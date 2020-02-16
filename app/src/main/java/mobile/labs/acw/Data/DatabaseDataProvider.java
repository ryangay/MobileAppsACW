/*
package mobile.labs.acw.Data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mobile.labs.acw.Data.Model.Puzzle;

*/
/**
 * Created by ryan on 28/02/2018.
 *//*


public class DatabaseDataProvider implements DataProvider {

    private final ScoreDatabaseHelper mDbHelper;

    public DatabaseDataProvider(ScoreDatabaseHelper dbHelper){
        mDbHelper = dbHelper;
    }


    public void getPuzzleList(final DataRepository.PuzzleListCallback callback, final int rows, final int columns) throws IOException, Puzzle.Layout.PuzzleFormatException {
        new AsyncTask<Void, Void, Puzzle[]>() {
            @Override
            protected Puzzle[] doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getReadableDatabase();
                final String where = PuzzleDbContract.Dimensions.COLUMN_NAME_ROWS + " = ? AND " + PuzzleDbContract.Dimensions.COLUMN_NAME_COLUMNS + " = ?";
                final String[] whereArgs = new String[] {Integer.toString(rows), Integer.toString(columns)};
                Cursor dimCursor = db.query(PuzzleDbContract.Dimensions.TABLE_NAME, new String[] {PuzzleDbContract.Dimensions.COLUMN_NAME_ROWS, PuzzleDbContract.Dimensions.COLUMN_NAME_COLUMNS}, where, whereArgs, null, null, null);

                // There should be zero or one row returned due to unique constraint
                if(!dimCursor.moveToNext()) {
                    return null;
                }
                int dimensionsId = dimCursor.getInt(dimCursor.getColumnIndexOrThrow(PuzzleDbContract.Dimensions._ID));

                final String layWhere = PuzzleDbContract.Layout.COLUMN_NAME_DIMENSIONS_ID + " = ?";
                final String[] layWhereArgs = new String[] {Integer.toString(dimensionsId)};
                Cursor layCursor = db.query(PuzzleDbContract.Layout.TABLE_NAME, new String[] {PuzzleDbContract.Layout.COLUMN_NAME_FILE_NAME} ,layWhere, layWhereArgs, null, null, null);

                List<String> layoutNames = new ArrayList<>();
                while(layCursor.moveToNext()){
                    layoutNames.add(layCursor.getString(layCursor.getColumnIndexOrThrow(PuzzleDbContract.Layout.COLUMN_NAME_FILE_NAME)));
                }
                if(layoutNames.size() < 1){
                    return null;
                }

                final String[] cellOutColumns = new String[] {PuzzleDbContract.LayoutCell.COLUMN_NAME_COLUMN, PuzzleDbContract.LayoutCell.COLUMN_NAME_ROW};
                final StringBuilder cellWhere = new StringBuilder();
                final String[] cellWhereArgs = new String[layoutNames.size()];
                cellWhere.append(PuzzleDbContract.LayoutCell.COLUMN_NAME_LAYOUT_ID + " = ?");
                cellWhereArgs[0] = layoutNames.get(0);
                if(layoutNames.size() > 1) {
                    for(int i = 1; i < layoutNames.size(); i++){
                        cellWhere.append(" OR " + PuzzleDbContract.LayoutCell.COLUMN_NAME_LAYOUT_ID + " = ?");
                        cellWhereArgs[i] = layoutNames.get(i);
                    }
                }
                Cursor cellCursor = db.query(PuzzleDbContract.LayoutCell.TABLE_NAME, cellOutColumns, cellWhere.toString(), cellWhereArgs, null, null, null);

                List<Puzzle> list = new ArrayList<>();

                */
/*while(cursor.moveToNext()) {
                    list.add(new Puzzle(cursor.getString(cursor.getColumnIndexOrThrow(PuzzleDbContract.Puzzle.COLUMN_NAME_FILE_NAME))));
                }*//*


                Puzzle[] array = new Puzzle[list.size()];
                return list.toArray(array);
            }

            @Override
            protected void onPostExecute(Puzzle[] puzzles) {
                callback.onPuzzleListReady(puzzles, null);
            }
        }.execute();
    }

    @Override
    public void getPuzzleIndex(JsonDataCallback callback) {

    }

    @Override
    public void getPuzzleInfo(String puzzleFile, JsonDataCallback callback) {

    }

    @Override
    public void getPuzzleLayout(String layoutFile, JsonDataCallback callback) {

    }

    @Override
    public void getPicture(String pictureSet, String imageName, BitmapDataCallback callback) {

    }
}
*/
