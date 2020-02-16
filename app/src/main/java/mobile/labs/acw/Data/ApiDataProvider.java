package mobile.labs.acw.Data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.Data.Model.Puzzle.Layout.Dimensions;

/**
 * Created by ryan on 15/02/2018.
 */

public class ApiDataProvider implements DataProvider {

    private static ApiDataProvider instance;

    private static final String TAG = "ApiDataProvider";

    private static final String BASE_URL = "http://www.simongrey.net/08027/slidingPuzzleAcw";
    private static final String INDEX_URL = BASE_URL + "/index.json";
    private static final String PUZZLE_URL = BASE_URL + "/puzzles/%s";
    private static final String LAYOUT_URL = BASE_URL + "/layouts/%s";
    private static final String PICTURE_URL = BASE_URL + "/images/%s/%s.jpg";

/*    final AsyncTask<JsonTaskParams, Void, JsonReader> getJSONTask = new AsyncTask<JsonTaskParams, Void, JsonReader>() {

        private final List<Throwable> errors = new ArrayList<>();
        private JsonDataCallback mCallback;

        @Override
        protected JsonReader doInBackground(JsonTaskParams... params) {
            mCallback = params[0].callback;
            URL url;
            try {
                url = new URL(params[0].path);
            } catch (MalformedURLException e) {
                errors.add(e);
                return null;
            }
            try {
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                InputStreamReader inReader = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(inReader);

                return new JsonReader(reader);
            } catch (IOException e) {
                errors.add(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JsonReader jsonReader) {
            if(errors.size() > 0){
                Throwable[] array = new Throwable[errors.size()];
                mCallback.onError(errors.toArray(array));
            }
            mCallback.onJSONReady(jsonReader);
        }
    };

    final AsyncTask<BitmapTaskParams, Void, Bitmap> getPictureTask = new AsyncTask<BitmapTaskParams, Void, Bitmap>() {
        public List<Throwable> errors = new ArrayList<>();
        public BitmapDataCallback mCallback;

        @Override
        protected Bitmap doInBackground(BitmapTaskParams... params) {
            mCallback = params[0].callback;
            URL url;
            try {
                url = new URL(params[0].path);
            } catch (MalformedURLException e) {
                errors.add(e);
                return null;
            }
            try {
                URLConnection conn = url.openConnection();
                InputStream input = new BufferedInputStream(conn.getInputStream());
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                errors.add(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(errors.size() > 0) {
                Throwable[] array = new Throwable[errors.size()];
                mCallback.onError(errors.toArray(array));
            }
            mCallback.onBitmapReady(bitmap);
        }
    };

    @Override
    public void getPuzzleIndex(final JsonDataCallback callback) {
        JsonTaskParams params = new JsonTaskParams(INDEX_URL, callback);
        getJSONTask.execute(params);
    }

    @Override
    public void getPuzzleInfo(String puzzleFile, final JsonDataCallback callback) {
        JsonTaskParams params = new JsonTaskParams(String.format(PUZZLE_URL, puzzleFile), callback);
        getJSONTask.execute(params);
    }

    @Override
    public void getPuzzleLayout(String layoutFile, final JsonDataCallback callback) {
        JsonTaskParams params = new JsonTaskParams(String.format(LAYOUT_URL, layoutFile), callback);
        getJSONTask.execute(params);
    }

    @Override
    public void getPicture(String pictureSet, String imageName, final BitmapDataCallback callback) {
        BitmapTaskParams params = new BitmapTaskParams(String.format(PICTURE_URL, pictureSet, imageName), callback);
        getPictureTask.execute(params);
    }*/

    public static ApiDataProvider getInstance(){
        if(instance == null)
            return instance = new ApiDataProvider();
        return instance;
    }

    @Override
    public String getPuzzleIndex() {
        try {
            return getJson(INDEX_URL);
        } catch (IOException e) {
            logError(INDEX_URL, e);
            return null;
        }
    }

    @Override
    public String getPuzzleInfo(String puzzleFile) {
        String formattedUrl = String.format(PUZZLE_URL, puzzleFile);
        try {
            return getJson(formattedUrl);
        } catch (IOException e){
            logError(formattedUrl, e);
            return null;
        }
    }

    @Override
    public String getPuzzleLayout(String layoutFile) {
        String formattedUrl = String.format(LAYOUT_URL, layoutFile);
        try {
            return getJson(formattedUrl);
        } catch (IOException e){
            logError(formattedUrl, e);
            return null;
        }
    }

    @Override
    public Bitmap getPicture(String pictureSet, String imageName) {
        String formattedUrl = String.format(PICTURE_URL, pictureSet, imageName);
        try {
            HttpURLConnection conn = (HttpURLConnection)(new URL(formattedUrl).openConnection());
            return BitmapFactory.decodeStream(conn.getInputStream());
        } catch(MalformedURLException e){
            logError(formattedUrl, e);
            return null;
        } catch (IOException e){
            logError(formattedUrl, e);
            return null;
        }
    }

    private static void logError(String url, Throwable e) {
        Log.e(TAG, "Could not get JSON at " + url, e);
    }

    private static String getJson(String path) throws IOException {
        URL url;
        url = new URL(path);

        StringBuilder json = new StringBuilder();
        String line;

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        InputStream in = new BufferedInputStream(conn.getInputStream());
        InputStreamReader inReader = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(inReader);

        while((line = reader.readLine()) != null) {
            json.append(line);
        }

        return json.length() == 0 ? null : json.toString();
    }
}
