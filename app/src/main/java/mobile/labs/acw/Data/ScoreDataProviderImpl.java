package mobile.labs.acw.Data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobile.labs.acw.Data.Model.Score;

/**
 * Created by ryan on 17/03/2018.
 */

public class ScoreDataProviderImpl implements ScoreDataProvider {

    private final ScoreDatabaseHelper mDbHelper;

    public ScoreDataProviderImpl(ScoreDatabaseHelper mDbHelper) {
        this.mDbHelper = mDbHelper;
    }

    @Override
    public Score[] getScores(String puzzleName, SortMode sortMode, int limit) {
        final String ASC = " ASC";
        String sort;
        switch (sortMode){
            case MOVES:
                sort = PuzzleDbContract.Score.COLUMN_NAME_MOVES + ASC;
                break;
            case TIME:
                sort = PuzzleDbContract.Score.COLUMN_NAME_COMPLETION_TIME + ASC;
                break;
            default:
                sort = null;
                break;
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        final String where = PuzzleDbContract.Score.COLUMN_NAME_PUZZLE_NAME + " = ?";
        final String[] whereArgs = new String[]{puzzleName};
        Cursor cursor = db.query(PuzzleDbContract.Score.TABLE_NAME,
                new String[] {
                        PuzzleDbContract.Score.COLUMN_NAME_NAME,
                        PuzzleDbContract.Score.COLUMN_NAME_COMPLETION_TIME,
                        PuzzleDbContract.Score.COLUMN_NAME_MOVES,
                        PuzzleDbContract.Score.COLUMN_NAME_TIMESTAMP
                },
                where,
                whereArgs,
                null,
                null,
                sort,
                String.valueOf(limit));

        List<Score> scores = new ArrayList<>(cursor.getCount());
        while(cursor.moveToNext()){
            scores.add(
                    new Score(
                            cursor.getLong(cursor.getColumnIndex(PuzzleDbContract.Score.COLUMN_NAME_COMPLETION_TIME)),
                            cursor.getInt(cursor.getColumnIndex(PuzzleDbContract.Score.COLUMN_NAME_MOVES)),
                            cursor.getString(cursor.getColumnIndex(PuzzleDbContract.Score.COLUMN_NAME_NAME)),
                            cursor.getLong(cursor.getColumnIndex(PuzzleDbContract.Score.COLUMN_NAME_TIMESTAMP)),
                            puzzleName));
        }
        cursor.close();
        Score[] array = new Score[scores.size()];
        return scores.toArray(array);
    }

    @Override
    public long setScore(Score score) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PuzzleDbContract.Score.COLUMN_NAME_PUZZLE_NAME, score.getPuzzleName());
        values.put(PuzzleDbContract.Score.COLUMN_NAME_MOVES, score.getMoves());
        values.put(PuzzleDbContract.Score.COLUMN_NAME_COMPLETION_TIME, score.getTime());
        values.put(PuzzleDbContract.Score.COLUMN_NAME_NAME, score.getName());
        values.put(PuzzleDbContract.Score.COLUMN_NAME_TIMESTAMP, score.getTimestamp());
        return db.insert(PuzzleDbContract.Score.TABLE_NAME, null, values);
    }
}
