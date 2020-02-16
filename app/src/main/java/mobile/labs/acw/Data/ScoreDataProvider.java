package mobile.labs.acw.Data;

import mobile.labs.acw.Data.Model.Score;

/**
 * Created by ryan on 17/03/2018.
 */

public interface ScoreDataProvider {
    enum SortMode {
        TIME, MOVES
    }
    Score[] getScores(String puzzleName, SortMode sortMode, int limit);
    long setScore(Score score);
}
