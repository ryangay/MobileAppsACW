package mobile.labs.acw.Data;

import android.graphics.Bitmap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import mobile.labs.acw.Data.Model.Puzzle;

/**
 * Created by ryan on 28/02/2018.
 */

public interface DataRepository {
    void getPuzzleList(boolean forceRefresh, DataCallback<Puzzle[]> callback) throws IOException;
    void getPuzzleList(DataCallback<Puzzle[]> callback, int rows, int columns);
    void getAvailableDimensions(DataCallback<Collection<Puzzle.Layout.Dimensions>> callback) throws IOException;
    void downloadPuzzle(Puzzle puzzle, DataCallback<Boolean> callback);
    void getImageSetForPuzzle(String puzzleName, DataCallback<Map<String, Bitmap>> callback);

    interface DataCallback<T> {
        void onDataReady(T data);
        void onUpdateProgress(Progress progress);
        void onError(ErrorType error);
    }

    interface LocalPictureSetCallback {
        void onLocalPictureSetFound();
    }

    class Progress {
        public ProgressState state;
        public ErrorType type;
        String message;
        public int percent;
    }

    enum ProgressState {
        DOWNLOAD_PUZZLE_LIST,
        DOWNLOAD_PUZZLE,
        DOWNLOAD_PUZZLE_LAYOUT,
        DOWNLOAD_PUZZLE_IMAGE,
        ERROR
    }

    enum ErrorType {
        INCORRECT_PUZZLE_FORMAT,
        CONNECTION_ERROR
    }
}
