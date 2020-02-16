package mobile.labs.acw.Data;

import android.provider.BaseColumns;

/**
 * Created by ryan on 28/02/2018.
 */

public final class PuzzleDbContract {

    private PuzzleDbContract() {}

    public static class Dimensions implements BaseColumns {
        public static final String TABLE_NAME = "dimensions";
        public static final String COLUMN_NAME_ROWS = "rows";
        public static final String COLUMN_NAME_COLUMNS = "columns";
    }

    public static class PictureSet {
        public static final String TABLE_NAME = "pictureset";
        public static final String COLUMN_NAME_NAME = "name";
    }

    public static class Picture implements BaseColumns {
        public static final String TABLE_NAME = "picture";
        public static final String COLUMN_NAME_DATA = "data";
        public static final String COLUMN_NAME_PICTURE_SET_NAME = "picturesetname";
    }

    public static class Layout {
        public static final String TABLE_NAME = "layout";
        public static final String COLUMN_NAME_FILE_NAME = "filename";
        public static final String COLUMN_NAME_DIMENSIONS_ID = "dimensionsid";
    }

    public static class LayoutCell implements BaseColumns {
        public static final String TABLE_NAME = "layoutcell";
        public static final String COLUMN_NAME_ROW = "row";
        public static final String COLUMN_NAME_COLUMN = "column";
        public static final String COLUMN_NAME_PICTURE_ID = "pictureid";
        public static final String COLUMN_NAME_LAYOUT_ID = "layoutid";
    }

    public static class Puzzle {
        public static final String TABLE_NAME = "puzzle";
        public static final String COLUMN_NAME_FILE_NAME = "filename";
        public static final String COLUMN_NAME_LAYOUT_ID = "layoutid";
        public static final String COLUMN_NAME_PICTURE_SET_NAME = "picturesetname";
    }

    public static class Score implements BaseColumns {
        public static final String TABLE_NAME = "score";
        public static final String COLUMN_NAME_PUZZLE_NAME = "puzzlefilename";
        public static final String COLUMN_NAME_COMPLETION_TIME = "completionmilliseconds";
        public static final String COLUMN_NAME_MOVES = "moves";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }
}
