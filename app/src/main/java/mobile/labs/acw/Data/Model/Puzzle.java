package mobile.labs.acw.Data.Model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ryan on 16/02/2018.
 */

public class Puzzle implements Parcelable {
    private static final Pattern puzzleNumberPattern = Pattern.compile("puzzle(\\d+).json");

    private final String mPuzzleFileName;
    private String mLayoutName;
    private Layout mLayout;
    private String mPictureSetName;
    private boolean mIsDownloaded;

    public Puzzle(String puzzleFileName){
        this(puzzleFileName, null, (Layout) null);
    }

    public Puzzle(String puzzleFileName, String pictureSetName, String layoutName){
        mPuzzleFileName = puzzleFileName;
        mPictureSetName = pictureSetName;
        mLayoutName = layoutName;
    }

    public Puzzle(String mPuzzleFileName, String pictureSet, String[][] layout) throws Layout.PuzzleFormatException{
        this(mPuzzleFileName, pictureSet, new Layout(layout));
    }

    public Puzzle(String puzzleFileName, String pictureSet, Layout layout){
        mPuzzleFileName = puzzleFileName;
        mPictureSetName = pictureSet;
        mLayout = layout;
    }

    protected Puzzle(Parcel in) {
        mPuzzleFileName = in.readString();
        mLayoutName = in.readString();
        mPictureSetName = in.readString();
        mIsDownloaded = in.readByte() != 0;
        mLayout = in.readParcelable(Puzzle.class.getClassLoader());

    }

    public static final Creator<Puzzle> CREATOR = new Creator<Puzzle>() {
        @Override
        public Puzzle createFromParcel(Parcel in) {
            return new Puzzle(in);
        }

        @Override
        public Puzzle[] newArray(int size) {
            return new Puzzle[size];
        }
    };

    public String getFriendlyName() {
        Matcher matcher = puzzleNumberPattern.matcher(mPuzzleFileName);
        if(matcher.find())
            return "Puzzle " + matcher.group(1);
        else return getFileName();
    }

    public String getFileName(){
        return mPuzzleFileName;
    }

    public String getLayoutName() {return mLayoutName; }

    public Layout getLayout(){
        return mLayout;
    }

    public void setPictureSetName(String name){
        mPictureSetName = name;
    }

    public String getPictureSetName(){
        return mPictureSetName;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Puzzle cmp = (Puzzle)obj;
            return cmp.getFileName().equals(mPuzzleFileName);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return getFriendlyName();
    }

    public void setLayout(Layout layout) {
        mLayout = layout;
    }

    public boolean isDownloaded() {
        return mIsDownloaded;
    }

    public void isDownloaded(boolean isDownloaded){
        mIsDownloaded = isDownloaded;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(mPuzzleFileName);
        dest.writeString(mLayoutName);
        dest.writeString(mPictureSetName);
        dest.writeByte((byte) (mIsDownloaded ? 1 : 0));
        dest.writeParcelable(mLayout, 0);
    }

    public static class Layout implements Parcelable {

        public static final String EMPTY_CELL = "empty";

        private String[][] mLayout;
        private Dimensions mDimensions;

        public Layout(String[][] layout) throws PuzzleFormatException {
            setLayout(layout);
        }

        protected Layout(Parcel in) throws PuzzleFormatException {
            int rows = in.readInt();
            int columns = in.readInt();
            String[][] layout = new String[rows][];
            for(int i = 0; i < rows; i++){
                String[] row = new String[columns];
                in.readStringArray(row);
                layout[i] = row;
            }
            setLayout(layout);
        }

        public static final Creator<Layout> CREATOR = new Creator<Layout>() {
            @Override
            public Layout createFromParcel(Parcel in) {
                try {
                    return new Layout(in);
                } catch (PuzzleFormatException e){
                    return null;
                }
            }

            @Override
            public Layout[] newArray(int size) {
                return new Layout[size];
            }
        };

        public void setLayout(String[][] layout) throws PuzzleFormatException {
            if(layout.length == 0) throw new PuzzleFormatException("The Puzzle layout is empty.");
            int columns = layout[0].length;
            for (int i = 1; i < layout.length; i++) {
                if(columns != layout[1].length)
                    throw new PuzzleFormatException("There is an unequal number of columns in each row of the puzzle. A puzzle must be rectangular.");
            }
            mLayout = layout;
            mDimensions = new Dimensions(layout.length, columns);
        }

        public String[][] getLayout() {
            return mLayout;
        }

        /***
         * Gets the row at the 1-indexed position
         * @param row The row number
         * @return Array consisting of the picture ID numbers of each column within the row
         */
        public String[] getRow(int row) throws ArrayIndexOutOfBoundsException {
            return mLayout[row-1];
        }

        public String getCell(int row, int column) throws ArrayIndexOutOfBoundsException {
            return mLayout[row][column];
        }

        public Dimensions getDimensions() {
            return mDimensions;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            int rows = mDimensions.rows();
            dest.writeInt(rows);
            dest.writeInt(mDimensions.columns());
            for(int i = 0; i < rows; i++){
                dest.writeStringArray(mLayout[i]);
            }
        }

        public static class Dimensions {
            private final int mRows;
            private final int mColumns;

            public Dimensions(int rows, int columns){
                mRows = rows;
                mColumns = columns;
            }

            public int rows() {
                return mRows;
            }

            public int columns() {
                return mColumns;
            }

            @Override
            public String toString() {
                return rows() + " x " + columns();
            }

            @Override
            public boolean equals(Object obj) {
                if(this == obj) return true;
                if(!(obj instanceof Dimensions)) return false;
                Dimensions other = ((Dimensions) obj);
                Boolean rowsEqual = mRows == other.mRows;
                Boolean colsEqual = mColumns == other.mColumns;
                return rowsEqual && colsEqual;
            }


            // Technique on overriding hash code found here:
            // https://www.mkyong.com/java/java-how-to-overrides-equals-and-hashcode/
            @Override
            public int hashCode() {
                int result = 17;
                result = 31 * result + rows();
                return 31 * result + columns();
            }
        }

        public static class PuzzleFormatException extends Exception {

            public PuzzleFormatException(String message) {
                super(message);
            }
        }
    }
}
