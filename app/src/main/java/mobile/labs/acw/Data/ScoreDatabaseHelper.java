package mobile.labs.acw.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ryan on 28/02/2018.
 */

public class ScoreDatabaseHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Scores.db";

    private static final String SQL_CREATE_DB =
            "CREATE TABLE " + PuzzleDbContract.Score.TABLE_NAME + " (" + PuzzleDbContract.Score._ID + " INTEGER PRIMARY KEY, " + PuzzleDbContract.Score.COLUMN_NAME_PUZZLE_NAME + " TEXT, " + PuzzleDbContract.Score.COLUMN_NAME_COMPLETION_TIME + " INT, " + PuzzleDbContract.Score.COLUMN_NAME_MOVES + " INT, " + PuzzleDbContract.Score.COLUMN_NAME_NAME + " TEXT, " + PuzzleDbContract.Score.COLUMN_NAME_TIMESTAMP + " INT);";

    private static final String SQL_DELETE_DB =
            "DROP TABLE IF EXISTS " + PuzzleDbContract.Score.TABLE_NAME + ";";

    public ScoreDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_DB);
        onCreate(db);
    }
}
