package mobile.labs.acw.Data.Model;

import java.util.Date;

import mobile.labs.acw.Data.Model.Puzzle;

/**
 * Created by ryan on 20/02/2018.
 */

public class Score {
    private long mTimeTaken;
    private int mMoves;
    private String mName;
    private String mPuzzleName;
    private long mTimestamp;

    public Score(long timeTaken, int moves, String name, long timestamp, String puzzle){
        mTimeTaken = timeTaken;
        mMoves = moves;
        mName = name;
        mTimestamp = timestamp;
        mPuzzleName = puzzle;
    }

    public String getName() {
        return mName;
    }

    public String getPuzzleName(){
        return mPuzzleName;
    }

    public int getMoves() {
        return mMoves;
    }

    public long getTime() {
        return mTimeTaken;
    }

    public long getTimestamp() {
        return mTimestamp;
    }
}
