package mobile.labs.acw.PuzzleDetail;

import android.graphics.Bitmap;
import android.os.Handler;

import java.util.Map;

import mobile.labs.acw.Data.DataRepository;
import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.Data.Model.Score;
import mobile.labs.acw.Data.ScoreDataProvider;

import static android.R.attr.key;

/**
 * Created by ryan on 11/03/2018.
 */

public class PuzzleDetailPresenter implements PuzzleDetailContract.Presenter {

    private final PuzzleDetailContract.View mView;
    private final DataRepository mRepository;
    private final ScoreDataProvider mScoreData;
    private final Puzzle mPuzzle;

    private ScoreDataProvider.SortMode mSortMode = ScoreDataProvider.SortMode.values()[0];

    public PuzzleDetailPresenter(PuzzleDetailContract.View view, DataRepository repo, ScoreDataProvider scoreData, Puzzle puzzle){
        mView = view;
        mRepository = repo;
        mScoreData = scoreData;
        mPuzzle = puzzle;

        mView.setPresenter(this);
        mView.setTitle(mPuzzle.toString());
        refreshScores();
        setupImages();
    }

    void setupImages(){
        mRepository.getImageSetForPuzzle(mPuzzle.getFileName(), new DataRepository.DataCallback<Map<String, Bitmap>>() {
            @Override
            public void onDataReady(Map<String, Bitmap> data) {
                Bitmap[][] array = new Bitmap[mPuzzle.getLayout().getDimensions().rows()][mPuzzle.getLayout().getDimensions().columns()];
                int imageSize = 0;
                Bitmap.Config config = null;
                for(String key : data.keySet()){
                    if(key.equals(Puzzle.Layout.EMPTY_CELL)) continue;
                    int x = Character.getNumericValue(key.charAt(0)) - 1;
                    int y = Character.getNumericValue(key.charAt(1)) - 1;

                    Bitmap bitmap = data.get(key);
                    array[y][x] = bitmap;
                    imageSize = bitmap.getWidth();
                    config = bitmap.getConfig();
                }
                mView.setPuzzleImages(array, imageSize, config);
            }

            @Override
            public void onUpdateProgress(DataRepository.Progress progress) {

            }

            @Override
            public void onError(DataRepository.ErrorType error) {

            }
        });
    }

    void refreshScores(){
        mView.setHighScores(mScoreData.getScores(mPuzzle.getFileName(), mSortMode, 50));
    }

    @Override
    public void onPlayClicked() {
        mView.play(mPuzzle);
    }

    @Override
    public void storeResult(long time, int moves, String name) {
        mScoreData.setScore(new Score(time, moves, name, System.currentTimeMillis(), mPuzzle.getFileName()));
        refreshScores();
    }

    @Override
    public String getPuzzleFriendlyName() {
        return mPuzzle.getFriendlyName();
    }
}
