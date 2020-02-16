package mobile.labs.acw.Data;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mobile.labs.acw.Data.Model.Puzzle;

/**
 * Created by ryan on 26/02/2018.
 */

/**
 * Welcome to callback hell
 */
public class DataRepositoryImpl implements DataRepository {

    private static DataRepositoryImpl instance;

    private static final String TAG = DataRepositoryImpl.class.getName();
    private static final String INDEX_ARRAY_PROPERTY = "PuzzleIndex";
    private static final String PUZZLE_PICTURES_PROPERTY = "PictureSet";
    private static final String PUZZLE_LAYOUT_PROPERTY = "layout";

    private final DataProvider mLocalDataProvider;
    private final DataProvider mRemoteDataProvider;

    private Boolean canUseInMemory = false;
    private Puzzle[] mPuzzles;

    private DataRepositoryImpl(DataProvider localDataProvider, DataProvider remoteDataProvider) {
        mLocalDataProvider = localDataProvider;
        mRemoteDataProvider = remoteDataProvider;
    }

    public static DataRepositoryImpl getInstance(DataProvider localProvider, DataProvider remoteProvider){
        return instance == null ? new DataRepositoryImpl(localProvider, remoteProvider) : instance;
    }

    @Override
    public void getPuzzleList(boolean forceRefresh, final DataCallback<Puzzle[]> callback) throws IOException {
        if(canUseInMemory){
            callback.onDataReady(mPuzzles);
            return;
        }
        loadPuzzles(forceRefresh, new PuzzleLoadCallback() {
            @Override
            public void onPuzzlesLoaded() {
                callback.onDataReady(mPuzzles);
            }

            @Override
            public void onUpdateProgress(Progress progress) {
                callback.onUpdateProgress(progress);
            }

            @Override
            public void onError(ErrorType type) {
                callback.onError(type);
            }
        });
    }

    @Override
    public void getPuzzleList(final DataCallback<Puzzle[]> callback, int x, int y) {
        final Puzzle.Layout.Dimensions match = new Puzzle.Layout.Dimensions(x, y);

        if(canUseInMemory) {
            callback.onDataReady(filterPuzzlesByDimensions(match));
            return;
        }
        loadPuzzles(false, new PuzzleLoadCallback() {
            @Override
            public void onPuzzlesLoaded() {
                callback.onDataReady(filterPuzzlesByDimensions(match));
            }

            @Override
            public void onUpdateProgress(Progress progress) {

            }

            @Override
            public void onError(ErrorType type) {

            }
        });

    }


    private Puzzle[] filterPuzzlesByDimensions(Puzzle.Layout.Dimensions match){
        List<Puzzle> puzzles = new ArrayList<>();
        for(Puzzle puzzle : mPuzzles) {
            Puzzle.Layout layout = puzzle.getLayout();
            if(layout == null) continue;
            if(layout.getDimensions().equals(match)){
                puzzles.add(puzzle);
            }
        }
        Puzzle[] array = new Puzzle[puzzles.size()];
        return puzzles.toArray(array);
    }

    @Override
    public void getAvailableDimensions(final DataCallback<Collection<Puzzle.Layout.Dimensions>> callback) throws IOException {
        if(canUseInMemory){
            callback.onDataReady(collateDimensions());
        } else {
            loadPuzzles(false, new PuzzleLoadCallback() {
                @Override
                public void onPuzzlesLoaded() {
                    callback.onDataReady(collateDimensions());
                }

                @Override
                public void onUpdateProgress(Progress progress) {
                    callback.onUpdateProgress(progress);
                }

                @Override
                public void onError(ErrorType type) {

                }
            });
        }
    }

    @Override
    public void downloadPuzzle(final Puzzle puzzle, final DataCallback<Boolean> callback) {

        new AsyncTask<Void, Progress, Boolean>(){

            @Override
            protected Boolean doInBackground(Void... params) {

                // A bit defensive, but we'll check what we have and what we don't have and go from there
                String layoutName = puzzle.getLayoutName();
                String pictureSetName = puzzle.getPictureSetName();

                Progress progress  = new Progress();

                DataConsumer localStore;
                if(mLocalDataProvider instanceof DataConsumer){
                    localStore = (DataConsumer)mLocalDataProvider;
                } else {
                    Log.w(TAG, "Local data provider needs to implement DataConsumer interface");
                    progress.state = ProgressState.ERROR;
                    publishProgress(progress);
                    return false;
                }

                progress.state = ProgressState.DOWNLOAD_PUZZLE;
                progress.percent = 10;
                publishProgress(progress);

                PuzzleInformation info;
                if(layoutName == null || pictureSetName == null){
                    String json;
                    if((json = mLocalDataProvider.getPuzzleInfo(puzzle.getFileName())) == null){
                        // Oh look, it actually isn't downloaded. Sorry, user.
                        if((json = mRemoteDataProvider.getPuzzleInfo(puzzle.getFileName())) == null){
                            // Lol
                            progress.state = ProgressState.ERROR;
                            progress.type = ErrorType.CONNECTION_ERROR;
                            publishProgress(progress);
                            return false;
                        }
                        localStore.savePuzzleInfo(puzzle.getFileName(), json);
                    }
                    // We actually have something now, this is good
                    info = getPuzzleInfoFromJson(getJsonReaderFromString(json));
                    layoutName = info.getLayoutFile();
                    pictureSetName = info.getPictureSet();
                }
                progress.state = ProgressState.DOWNLOAD_PUZZLE_LAYOUT;
                progress.percent = 20;
                publishProgress(progress);
                // Check to see if we have the layout (probably not but we'll still check)
                String layoutJson;
                if((layoutJson = mLocalDataProvider.getPuzzleLayout(layoutName)) == null){
                    if((layoutJson = mRemoteDataProvider.getPuzzleLayout(layoutName)) == null) {
                        // ERROR INTENSIFIES
                        progress.state = ProgressState.ERROR;
                        publishProgress(progress);
                        return false;
                    }
                    localStore.saveLayout(layoutName, layoutJson);
                }
                try {
                    puzzle.setLayout(getLayoutFromJson(getJsonReaderFromString(layoutJson)));
                } catch (Puzzle.Layout.PuzzleFormatException e) {
                    // FFS SIMON
                    progress.state = ProgressState.ERROR;
                    progress.type = ErrorType.INCORRECT_PUZZLE_FORMAT;
                    publishProgress(progress);
                    return false;
                }
                // Now we have the layout we can check for the picture set

                Map<String, Bitmap> missing = new HashMap<>();
                String pictureSet = puzzle.getPictureSetName();
                Puzzle.Layout layout = puzzle.getLayout();
                final int totalCells = layout.getDimensions().rows()*layout.getDimensions().columns();
                final int prePercent = progress.percent;
                int iterations = 0;
                for(int x = 0; x < layout.getDimensions().rows(); x++){
                    for(int y = 0; y < layout.getDimensions().columns(); y++){
                        progress.state = ProgressState.DOWNLOAD_PUZZLE_IMAGE;
                        progress.percent += (++iterations*prePercent)/(totalCells*prePercent);
                        publishProgress(progress);
                        Bitmap picture;
                        String name = layout.getCell(x, y);
                        if(!name.equals(Puzzle.Layout.EMPTY_CELL)) {
                            if (mLocalDataProvider.getPicture(pictureSetName, name) == null) {
                                if ((picture = mRemoteDataProvider.getPicture(pictureSetName, name)) == null) {
                                    progress.state = ProgressState.ERROR;
                                    progress.type = ErrorType.CONNECTION_ERROR;
                                    publishProgress(progress);
                                    return false;
                                }
                                missing.put(name, picture);
                            }
                        }
                    }
                }
                progress.percent = 100;
                if(localStore.savePictureSet(pictureSetName, missing)){
                    puzzle.isDownloaded(true);
                    return true;
                }
                return false;
            }

            @Override
            protected void onProgressUpdate(Progress... values) {
                callback.onUpdateProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Boolean value) {
                super.onPostExecute(value);
                if(value) {
                    callback.onDataReady(value);
                } else {
                    callback.onError(ErrorType.CONNECTION_ERROR);
                }
            }
        }.execute();

    }

    private Set<Puzzle.Layout.Dimensions> collateDimensions(){
        Set<Puzzle.Layout.Dimensions> dimensions = new HashSet<>();
        for(Puzzle puzzle : mPuzzles){
            if(puzzle.isDownloaded()) {
                dimensions.add(puzzle.getLayout().getDimensions());
            }
        }
        return dimensions;
    }


    private Boolean pictureSetExistsLocally(Puzzle puzzle) {
        String[] row = puzzle.getLayout().getRow(1);
        String imageName = null;
        for(int i = 0; imageName == null && i < row.length; i++) {
            if(row[i] != Puzzle.Layout.EMPTY_CELL){
                imageName = row[i];
            }
        }
        return pictureSetExistsLocally(puzzle.getPictureSetName(), imageName);
    }

    private Boolean pictureSetExistsLocally(String pictureSet, String pictureName){
        return mLocalDataProvider.getPicture(pictureSet, pictureName) != null;
    }

    @Override
    public void getImageSetForPuzzle(final String puzzleName, final DataCallback<Map<String, Bitmap>> callback) {
        new AsyncTask<Void, Progress, Map<String, Bitmap>>(){

            @Override
            protected Map<String, Bitmap> doInBackground(Void... params) {
                PuzzleInformation info = getPuzzleInfoFromJson(new JsonReader(new StringReader(mLocalDataProvider.getPuzzleInfo(puzzleName))));
                Puzzle.Layout layout;
                try {
                    String layoutJson = mLocalDataProvider.getPuzzleLayout(info.getLayoutFile());
                    layout = getLayoutFromJson(new JsonReader(new StringReader(layoutJson)));
                } catch (Puzzle.Layout.PuzzleFormatException e){
                    return null;
                }
                String puzzleSet = info.getPictureSet();
                Map<String, Bitmap> data = new HashMap<>(layout.getDimensions().rows()*layout.getDimensions().columns());
                for(int y =0; y < layout.getDimensions().rows(); y++){
                    for(int x = 0; x < layout.getDimensions().columns(); x++){
                        String cell;
                        if((cell = layout.getCell(y, x)) != Puzzle.Layout.EMPTY_CELL){
                            data.put(cell, mLocalDataProvider.getPicture(puzzleSet, cell));
                        }
                    }
                }
                return data;
            }

            @Override
            protected void onPostExecute(Map<String, Bitmap> data) {
                callback.onDataReady(data);
            }
        }.execute();
    }

    private void loadPuzzles(final boolean force, final PuzzleLoadCallback callback){
/*        mLocalDataProvider.getPuzzleIndex(new DataProvider.JsonDataCallback() {
            @Override
            public void onJSONReady(JsonReader json) {
                if(json == null) {
                    // data doesn't exist
                    loadPuzzlesFromServer(callback);
                }
                final String[] puzzleNames = getPuzzleNamesFromIndexJSON(json);
                mPuzzles = new Puzzle[puzzleNames.length];

                InfoJsonCallback infoCallback = new InfoJsonCallback(puzzleNames, mPuzzles, true, true, callback);
                for(String name : puzzleNames){
                    mLocalDataProvider.getPuzzleInfo(name, infoCallback);
                }
            }

            @Override
            public void onError(Throwable[] errors) {

            }
        });
        */

        new AsyncTask<Void, Progress, Puzzle[]>() {

            @Override
            protected Puzzle[] doInBackground(Void... params) {
                String puzzleIndex;
                Progress progress = new Progress();
                if(force || (puzzleIndex = mLocalDataProvider.getPuzzleIndex()) == null) {
                    progress.state = ProgressState.DOWNLOAD_PUZZLE_LIST;
                    publishProgress(progress);
                    puzzleIndex = mRemoteDataProvider.getPuzzleIndex();
                    if(puzzleIndex == null){
                        return null;
                    }
                    ((DataConsumer)mLocalDataProvider).savePuzzleIndex(puzzleIndex);
                }
                String[] names = getPuzzleNamesFromIndexJSON(getJsonReaderFromString(puzzleIndex));
                // Try to get as much local information
                Puzzle[] puzzles = new Puzzle[names.length];
                for (int i = 0; i < names.length; i++) {
                    String json;
                    if((json = mLocalDataProvider.getPuzzleInfo(names[i])) != null){
                        PuzzleInformation info = getPuzzleInfoFromJson(getJsonReaderFromString(json));
                        Puzzle puzzle = new Puzzle(names[i], info.getPictureSet(), info.getLayoutFile());
                        String layout = mLocalDataProvider.getPuzzleLayout(info.getLayoutFile());
                        try {
                            puzzle.setLayout(getLayoutFromJson(getJsonReaderFromString(layout)));
                        } catch (Puzzle.Layout.PuzzleFormatException e){
                            progress.state = ProgressState.ERROR;
                            progress.type = ErrorType.INCORRECT_PUZZLE_FORMAT;
                            publishProgress(progress);
                        }
                        // We'll assume that the picture set exists if at least one image exists, to save time and memory
                        if(layout != null && pictureSetExistsLocally(puzzle)){
                            puzzle.isDownloaded(true);
                        }
                        puzzles[i] = puzzle;
                    } else {
                        Puzzle puzzle = new Puzzle(names[i]);
                        puzzle.isDownloaded(false);
                        puzzles[i] = puzzle;
                    }
                }
                return puzzles;
            }

            @Override
            protected void onProgressUpdate(Progress... values) {

                callback.onUpdateProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Puzzle[] puzzles) {
                if(puzzles == null) {
                    callback.onError(ErrorType.CONNECTION_ERROR);
                } else {
                    mPuzzles = puzzles;
                    callback.onPuzzlesLoaded();
                }
            }
        }.execute();

    }

    private static JsonReader getJsonReaderFromString(String json){
        return new JsonReader(new StringReader(json));
    }

    private Puzzle.Layout getLayoutFromJson(JsonReader layoutJson) throws Puzzle.Layout.PuzzleFormatException {
        List<String[]> layout = new ArrayList<>(4);
        try {
            layoutJson.beginObject();
            if (layoutJson.hasNext()) {
                if (layoutJson.nextName().equals(PUZZLE_LAYOUT_PROPERTY)) {
                    layoutJson.beginArray();
                    while (layoutJson.hasNext()) {
                        List<String> row = new ArrayList<>();
                        layoutJson.beginArray();
                        while (layoutJson.hasNext()) {
                            row.add(layoutJson.nextString());
                        }
                        layoutJson.endArray();
                        String[] array = new String[row.size()];
                        layout.add(row.toArray(array));
                    }
                    layoutJson.endArray();
                }
            }
            layoutJson.endObject();
        } catch(IOException e){
            Log.e(TAG, "Error reading layout JSON file:", e);
            return null;
        } catch(Throwable e){
            Log.e(TAG, "Unknown error:", e);
            return null;
        } finally {
            try {
                layoutJson.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing JSON reader:", e);
            }
        }
        String[][] array = new String[layout.size()][];
        Puzzle.Layout retVal = new Puzzle.Layout(layout.toArray(array));
        return retVal;
    }

    private static final class PuzzleInformation{
        private final String pictureSet;
        private final String layoutFile;

        PuzzleInformation(String pictureSet, String layoutFile){
            this.pictureSet = pictureSet;
            this.layoutFile = layoutFile;
        }

        public String getPictureSet() {
            return pictureSet;
        }

        public String getLayoutFile() {
            return layoutFile;
        }
    }

    private PuzzleInformation getPuzzleInfoFromJson(JsonReader info){
        try {
            info.beginObject();
            String pictureSet = null;
            String layoutName = null;
            while (info.hasNext()) {
                switch (info.nextName()) {
                    case PUZZLE_PICTURES_PROPERTY: {
                        pictureSet = info.nextString();
                        break;
                    }
                    case PUZZLE_LAYOUT_PROPERTY: {
                        layoutName = info.nextString();
                    }
                }
            }
            if(pictureSet == null || layoutName == null) return null;
            return new PuzzleInformation(pictureSet, layoutName);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns an array of file names of Puzzles
     * @param json
     * @return Array of strings
     */
    private String[] getPuzzleNamesFromIndexJSON(JsonReader json){
        List<String> names = new ArrayList<>();
        try {
            json.beginObject();
            if (json.hasNext()) {
                if (json.nextName().equals(INDEX_ARRAY_PROPERTY)) {
                    json.beginArray();
                    while (json.hasNext()) {
                        String val = json.nextString();
                        names.add(val);
                    }
                    json.endArray();
                }
            }
            json.endObject();
        } catch (IOException e){
            Log.e(TAG, "Error getting API JSON response", e);
            return null;
        }
        String[] array = new String[names.size()];
        return names.toArray(array);
    }

    private interface PuzzleLoadCallback {
        void onPuzzlesLoaded(); // Aka cached in the mPuzzles object
        void onUpdateProgress(Progress progress);
        void onError(ErrorType type);
    }




   /* private class LayoutJsonCallback implements DataProvider.JsonDataCallback {

        private final String[] mLayoutNames;
        private final Puzzle[] mPuzzles;
        private final PuzzleLoadCallback mLoadCallback;
        private Boolean mSourceIsLocal;
        final JsonReader[] layoutReaders;
        int counter = 0;

        private static final String PUZZLE_LAYOUT_PROPERTY = "layout";

        LayoutJsonCallback(String[] layoutNames, Puzzle[] puzzles, Boolean local, PuzzleLoadCallback callback) {
            mLayoutNames = layoutNames;
            mPuzzles = puzzles;
            layoutReaders = new JsonReader[puzzles.length];
            mSourceIsLocal = local;
            mLoadCallback = callback;
        }

        @Override
        public void onJSONReady(JsonReader json) {

            if(mSourceIsLocal && json == null) {
                mSourceIsLocal = false;
                mRemoteDataProvider.getPuzzleLayout(mLayoutNames[counter], this);
                try {
                    wait();
                } catch(InterruptedException e) {
                    return;
                }
                return;
            }

            layoutReaders[counter++] = json;

            if(counter == layoutReaders.length){
                for (int i = 0; i < layoutReaders.length; i++) {
                    try {
                        mPuzzles[i].setLayout(getLayoutFromJson(json));
                    } catch (Puzzle.Layout.PuzzleFormatException e){

                    }
                    mLoadCallback.onPuzzlesLoaded();
                }
            }
            if(!mSourceIsLocal){
                exitRemoteSource();
            }
        }

        @Override
        public void onError(Throwable[] errors) {
            counter++;
            if(!mSourceIsLocal){
                exitRemoteSource();
            }
        }

        private Puzzle.Layout getLayoutFromJson(JsonReader layoutJson) throws Puzzle.Layout.PuzzleFormatException {
            try {
                layoutJson.beginObject();
                if (layoutJson.hasNext()) {
                    if (layoutJson.nextName().equals(PUZZLE_LAYOUT_PROPERTY)) {
                        layoutJson.beginArray();
                        List<String[]> layout = new ArrayList<>(4);
                        while (layoutJson.hasNext()) {
                            List<String> row = new ArrayList<>();
                            layoutJson.beginArray();
                            while (layoutJson.hasNext()) {
                                row.add(layoutJson.nextString());
                            }
                            layoutJson.endArray();
                            String[] array = new String[row.size()];
                            layout.add(row.toArray(array));
                        }
                        layoutJson.endArray();

                        String[][] array = new String[layout.size()][];
                        return new Puzzle.Layout(layout.toArray(array));
                    }
                }
                layoutJson.endObject();
            } catch(IOException e){
                return null;
            }
            return null;
        }

        private void exitRemoteSource() {
            notify();
            mSourceIsLocal = true;
        }
    };

    private class InfoJsonCallback implements DataProvider.JsonDataCallback {

        private static final String PUZZLE_PICTURES_PROPERTY = "PictureSet";
        private static final String PUZZLE_LAYOUT_PROPERTY = "layout";

        private final String[] mPuzzleNames;
        private final Puzzle[] mPuzzles;
        private final PuzzleLoadCallback mCallback;
        final JsonReader[] infoReaders;

        private Boolean mLoadLayout;
        private Boolean mSourceIsLocal;
        private int counter = 0;


        InfoJsonCallback(String[] puzzleNames, Puzzle[] puzzles, Boolean sourceIsLocal, Boolean loadLayout, PuzzleLoadCallback loadCallback){
            mPuzzleNames = puzzleNames;
            mPuzzles = puzzles;
            infoReaders = new JsonReader[puzzles.length];
            mSourceIsLocal = sourceIsLocal;
            mLoadLayout = loadLayout;
            mCallback = loadCallback;
        }

        @Override
        public void onJSONReady(JsonReader json) {

            if(mSourceIsLocal && json == null){
                mSourceIsLocal = false;
                mRemoteDataProvider.getPuzzleInfo(mPuzzleNames[counter], this);
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }

            infoReaders[counter++] = json;

            if(counter == infoReaders.length){

                String[] layoutNames = new String[mPuzzles.length];

                for (int i = 0; i < infoReaders.length; i++) {
                    Puzzle puzzle = getPuzzleInfoFromJson(mPuzzleNames[i], infoReaders[i]);
                    mPuzzles[i] = puzzle;
                    layoutNames[i] = puzzle.getLayoutName();
                }
                DataRepositoryImpl.this.mPuzzles = mPuzzles;
                if(mLoadLayout) {
                    LayoutJsonCallback mLayoutCallback = new LayoutJsonCallback(layoutNames, mPuzzles, true, mCallback);
                    for (String layout : layoutNames) {
                        mLocalDataProvider.getPuzzleLayout(layout, mLayoutCallback);
                    }
                } else {
                    mCallback.onPuzzlesLoaded();
                }
            }
            if(!mSourceIsLocal){
                exitRemoteSource();
            }
        }

        @Override
        public void onError(Throwable[] errors) {
            counter++;
            if(!mSourceIsLocal){
                exitRemoteSource();
            }
        }

        private Puzzle getPuzzleInfoFromJson(String puzzleName, JsonReader info) {

            String pictureSet = "";
            String layoutName = "";
            Puzzle puzzle;

            try {
                info.beginObject();
                while (info.hasNext()) {
                    switch (info.nextName()) {
                        case PUZZLE_PICTURES_PROPERTY: {
                            pictureSet = info.nextString();
                            break;
                        }
                        case PUZZLE_LAYOUT_PROPERTY: {
                            layoutName = info.nextString();
                        }
                    }
                }
            } catch (IOException e) {
                return null;
            }

            puzzle = new Puzzle(puzzleName, pictureSet, layoutName);
            return puzzle;
        }

        private void exitRemoteSource() {
            notify();
            mSourceIsLocal = true;
        }
    };*/
}
