package mobile.labs.acw.Data;

import android.graphics.Bitmap;
import android.util.JsonReader;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;

import mobile.labs.acw.Data.Model.Puzzle;

/**
 * Created by ryan on 26/02/2018.
 */

public interface DataProvider {
    String getPuzzleIndex();//JsonDataCallback callback);
    String getPuzzleInfo(String puzzleFile);//, JsonDataCallback callback);
    String getPuzzleLayout(String layoutFile);//, JsonDataCallback callback);
    Bitmap getPicture(String pictureSet, String imageName);//, BitmapDataCallback callback);

    interface JsonDataCallback {
        void onJSONReady(JsonReader json);
        void onError(Throwable[] errors);
    }

    interface BitmapDataCallback {
        void onBitmapReady(Bitmap bitmap);
        void onError(Throwable[] errors);
    }

    final class JsonTaskParams {
        final String path;
        final JsonDataCallback callback;

        public JsonTaskParams(String path, JsonDataCallback callback) {
            this.path = path;
            this.callback = callback;
        }
    }

    final class BitmapTaskParams {
        final String path;
        final BitmapDataCallback callback;

        BitmapTaskParams(String path, BitmapDataCallback callback) {
            this.path = path;
            this.callback = callback;
        }
    }
}
