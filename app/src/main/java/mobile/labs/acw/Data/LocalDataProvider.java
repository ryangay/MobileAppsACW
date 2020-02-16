package mobile.labs.acw.Data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by ryan on 26/02/2018.
 */

public class LocalDataProvider implements DataProvider, DataConsumer {

    private static final String TAG = LocalDataProvider.class.getName();
    private static LocalDataProvider instance;

    private static final String INDEX_DIR = "index.json";
    private static final String PUZZLE_ROOT = "puzzles";
    private static final String PUZZLE_DIR = PUZZLE_ROOT + "/%s";
    private static final String LAYOUT_ROOT = "layouts";
    private static final String LAYOUT_DIR = LAYOUT_ROOT + "/%s";
    private static final String PICTURE_SET_ROOT_ROOT = "picture_sets";
    private static final String PICTURE_SET_ROOT = PICTURE_SET_ROOT_ROOT +  "/%s";
    private static final String PICTURE_SET_DIR = PICTURE_SET_ROOT + "/%s.jpg";

    // The context provides the file directory that we need to write and read from
    private final Context mContext;

    private File mRootFolder;
    private File mLayoutFolder;
    private File mPuzzleFolder;
    private File mPictureSetFolder;

    /**
     * Initialises the local data context
     * @param context The context in which the local data source will be used
     */
    private LocalDataProvider(@NonNull Context context) {
        mContext = context;
        createFolders();
    }

    public static LocalDataProvider getInstance(Context context){
        if(instance != null){
            return instance;
        }
        return instance = new LocalDataProvider(context);
    }

    private void createFolders(){
        mRootFolder = mContext.getFilesDir();
        mPuzzleFolder = new File(mRootFolder, PUZZLE_ROOT);
        mLayoutFolder = new File(mRootFolder, LAYOUT_ROOT);
        mPictureSetFolder = new File(mRootFolder, PICTURE_SET_ROOT_ROOT);

    }

    @Override
    public String getPuzzleIndex() {
        return getJson(mRootFolder, INDEX_DIR);
    }

    @Override
    public String getPuzzleInfo(String puzzleFile) {
        return getJson(mPuzzleFolder, puzzleFile);
    }

    @Override
    public String getPuzzleLayout(String layoutFile) {
        return getJson(mLayoutFolder, layoutFile);
    }

    @Override
    public Bitmap getPicture(String pictureSet, String imageName) {
        File specificFolder = new File(mPictureSetFolder, pictureSet);
        if(!specificFolder.exists()) return null;
        File file = new File(specificFolder, imageName + ".jpg");
        if(!file.exists()) return null;
        Bitmap bitmap = null;
        try {
            InputStream is = new FileInputStream(file);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot find image file at " + specificFolder + imageName, e);
        }
        return bitmap;
    }

    private String getJson(File root, String fileName){
        File file = new File(root, fileName);
        StringBuilder json = new StringBuilder();
        BufferedReader reader = null;
        if(!file.exists()) return null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(file));

            while((line = reader.readLine()) != null) {
                json.append(line);
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File at " + root + fileName + " not found.", e);
            return null;
        } catch (IOException e){
            Log.e(TAG, "Error reading file at " + root + fileName , e);
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing file reader:",e );
            }
        }
        return json.length() == 0 ? null : json.toString();
    }

    private Bitmap getBitmap(String path){
        File file = new File(mContext.getFilesDir(), path);
        if(!file.exists()){
            return null;
        }
        try {
            InputStream is = new FileInputStream(file);
            return BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot find image file at " + path, e);
            return null;
        }
    }

    @Override
    public Boolean savePuzzleIndex(String json) {
        try {
            return saveJson(json, mRootFolder, INDEX_DIR);
        } catch (IOException e) {
            Log.e(TAG, "Error saving puzzle information", e);
        }
        return false;
    }

    @Override
    public Boolean savePuzzleInfo(String fileName, String json) {
        try {
            return saveJson(json, mPuzzleFolder, fileName);
        } catch (IOException e) {
            Log.e(TAG, "Error saving puzzle information", e);
        }
        return false;
    }

    @Override
    public Boolean saveLayout(String fileName, String json) {
        try {
            return saveJson(json, mLayoutFolder, fileName);
        } catch (IOException e) {
            Log.e(TAG, "Error saving puzzle information", e);
        }
        return false;
    }

    @Override
    public Boolean savePictureSet(String name, Map<String, Bitmap> set) {
        File dir = new File(mPictureSetFolder, name);
        if(!dir.mkdirs() && !dir.exists()){
            // We can't create the directory for some reason..
            Log.w(TAG, "Couldn't create save directory for puzzle set + \"" + name + "\"");
            return false;
        }
        for(String picture : set.keySet()) {
            File newPic = new File(dir, picture + ".jpg");
            try {
                if(!newPic.createNewFile()){
                    newPic.delete();
                    newPic.createNewFile();
                }
                set.get(picture).compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(newPic));
            } catch (IOException e){
                Log.e(TAG, "Could not save picture set, failure at Picture Set: " + name + ", " + picture, e);
                return false;
            }
        }
        return true;
    }

    /**
     * If this seems terribly inefficient, that's because it is
     * @param json The string of JSON
     * @param root The path at which to save the JSON
     * @param fileName
     * @return {@code true} if the save is successful, {@code false} if the file cannot be opened
     * @throws IOException
     */
    private Boolean saveJson(String json, File root, String fileName) throws IOException{
        File file = new File(root, fileName);
        if(!file.mkdirs() && !file.exists()){
            // We can't create the directory for some reason..
            Log.w(TAG, "Couldn't create save directory for JSON");
            return false;
        }
        try {
            if(!file.createNewFile()){
                file.delete();
                file.createNewFile();
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot create file at " + root + "/" + fileName, e);
            return false;
        }
        FileWriter writer = new FileWriter(file);
        writer.write(json);
        writer.close();
        return true;
    }

/*    final AsyncTask<JsonTaskParams, Void, JsonReader> getJsonTask = new AsyncTask<JsonTaskParams, Void, JsonReader>() {

        private List<Throwable> mErrors = new ArrayList<>();
        private JsonDataCallback mCallback;

        @Override
        protected JsonReader doInBackground(JsonTaskParams... params) {
            mCallback = params[0].callback;
            File file = new File(mContext.getFilesDir(), params[0].path);
            if(!file.exists()) {
                return null;
            }
            try {
                return new JsonReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                mErrors.add(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JsonReader jsonReader) {
            if(mErrors.size() > 0) {
                Throwable[] array = new Throwable[mErrors.size()];
                mCallback.onError(array);
            }
            mCallback.onJSONReady(jsonReader);
        }
    };

    final AsyncTask<BitmapTaskParams, Void, Bitmap> getPictureTask = new AsyncTask<BitmapTaskParams, Void, Bitmap>() {
        private List<Throwable> mErrors = new ArrayList<>();
        private BitmapDataCallback mCallback;

        @Override
        protected Bitmap doInBackground(BitmapTaskParams... params) {
            mCallback = params[0].callback;
            File file = new File(mContext.getFilesDir(), params[0].path);
            if(!file.exists()) {
                return null;
            }
            try {
                InputStream stream = new FileInputStream(file);
                return BitmapFactory.decodeStream(stream);
            } catch (FileNotFoundException e){
                mErrors.add(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(mErrors.size() > 0){
                Throwable[] array = new Throwable[mErrors.size()];
                mCallback.onError(mErrors.toArray(array));
            }
            mCallback.onBitmapReady(bitmap);
        }
    };

    @Override
    public void getPuzzleIndex(JsonDataCallback callback) {
        JsonTaskParams params = new JsonTaskParams(INDEX_DIR, callback);
        getJsonTask.execute(params);
    }

    @Override
    public void getPuzzleInfo(String puzzleFile, JsonDataCallback callback) {
        JsonTaskParams params = new JsonTaskParams(String.format(PUZZLE_DIR, puzzleFile), callback);
        getJsonTask.execute(params);
    }

    @Override
    public void getPuzzleLayout(String layoutFile, JsonDataCallback callback) {
        JsonTaskParams params = new JsonTaskParams(String.format(LAYOUT_DIR, layoutFile), callback);
        getJsonTask.execute(params);
    }

    @Override
    public void getPicture(String pictureSet, String imageName, BitmapDataCallback callback) {
        BitmapTaskParams params = new BitmapTaskParams(String.format(PICTURE_SET_DIR, pictureSet, imageName), callback);
        getPictureTask.execute(params);
    }*/
}
