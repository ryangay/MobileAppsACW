package mobile.labs.acw.PuzzleDetail;

import android.graphics.Bitmap;

import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.Data.Model.Score;

/**
 * Created by ryan on 20/02/2018.
 */

public class PuzzleDetailContract {
    public interface View {
        void setPresenter(Presenter presenter);
        void setTitle(String title);
        void setPuzzleImages(Bitmap[][] images, int imageSize, Bitmap.Config config);
        void setHighScores(Score[] highScores);
        void play(Puzzle puzzle);
    }

    public interface Presenter {
        void onPlayClicked();

        /**
         * Stores the result of the game in the database
         * @param time Time taken to finish the game
         * @param moves Number of moves taken
         * @return The normalised score
         */
        void storeResult(long time, int moves, String name);
        String getPuzzleFriendlyName();
    }
}
