package mobile.labs.acw.Data;

import android.graphics.Bitmap;
import android.util.JsonReader;

import java.util.Map;

/**
 * Created by ryan on 03/03/2018.
 */

public interface DataConsumer {
    Boolean savePuzzleIndex(String reader);
    Boolean savePuzzleInfo(String fileName, String reader);
    Boolean saveLayout(String fileName, String reader);
    Boolean savePictureSet(String name, Map<String, Bitmap> set);
}
