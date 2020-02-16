package mobile.labs.acw.Arena;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mobile.labs.acw.DI;
import mobile.labs.acw.Data.DataRepository;
import mobile.labs.acw.Data.DataRepositoryImpl;
import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.R;

public class ArenaActivity extends AppCompatActivity {

    public static final String TAG = ArenaActivity.class.getName();

    public static final String PUZZLE_EXTRA = "puzzle";
    public static final String PUZZLE_ROWS_EXTRA = "rows";
    public static final String PUZZLE_COLS_EXTRA = "columns";

    public static final String GAME_TIME_RESULT = "gametime";
    public static final String GAME_MOVES_RESULT = "gamemoves";

    private static final String STATE_TIME = "stateTime";
    private static final String STATE_MOVES = "stateMoves";
    private static final String STATE_PIECES = "statePieces";

    private Chronometer mChrono;
    private TextView mMovesText;
    private Map<String, PuzzleView.PuzzleGridDrawable> mDrawables;

    private DataRepository mDataRepository = DI.provideDataRepository(this);

    private int moves;

    private long mPauseTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_arena);

        final Puzzle puzzle = getIntent().getParcelableExtra(PUZZLE_EXTRA);
        final int rows = puzzle.getLayout().getDimensions().rows();
        final int columns = puzzle.getLayout().getDimensions().columns();

        if(rows == 0 || columns == 0){
            setResult(RESULT_CANCELED);
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
        }

        Bundle prevPositionsNonFinal = null;
        if(savedInstanceState != null){
            mPauseTime = savedInstanceState.getLong(STATE_TIME);
            moves = savedInstanceState.getInt(STATE_MOVES);
            prevPositionsNonFinal = savedInstanceState.getBundle(STATE_PIECES);
        }
        final Bundle prevPositions = prevPositionsNonFinal;
        mChrono = (Chronometer)findViewById(R.id.chrono);
        mMovesText = (TextView)findViewById(R.id.movesText);
        mMovesText.setText(String.valueOf(moves));

        // Setup the view
        final PuzzleView puzzleView = (PuzzleView)findViewById(R.id.puzzleView);
        final RelativeLayout sizeContainer = (RelativeLayout)findViewById(R.id.fillFrame);

        // Determine the size of the container at runtime
        // Reference: https://stackoverflow.com/questions/3779173/determining-the-size-of-an-android-view-at-runtime
        ViewTreeObserver viewTreeObserver = sizeContainer.getViewTreeObserver();
        if(viewTreeObserver.isAlive()){
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    sizeContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    puzzleView.setDimensions(rows, columns, sizeContainer.getWidth(), sizeContainer.getHeight());
                    Log.d(TAG, String.format("Dimensions set as %d, %d", sizeContainer.getWidth(), sizeContainer.getHeight()));
                }
            });
        }
        try {
            String hexString = PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.pref_colour_key), null);
            if(hexString != null && (hexString.length() == 3 || hexString.length() == 6)) {
                int shift = hexString.charAt(0) == '#' ? 1 : 0;
                int offset = hexString.length() / 3;
                int red = Integer.valueOf(hexString.substring(shift, shift + offset), 16);
                int green = Integer.valueOf(hexString.substring(1*offset + shift, 1*offset + shift + offset), 16);
                int blue = Integer.valueOf(hexString.substring(2*offset + shift, 2*offset + shift + offset), 16);
                puzzleView.setBackgroundColour(red, green, blue);
            }
        } catch(NumberFormatException e){
            Log.e(TAG, "Invalid hexadecimal colour string");
        } catch(IllegalArgumentException e){
            Log.wtf(TAG, "Each colour value needs to be a value between 0 and 255, but it should've been impossible to get here anyway.", e);
        }
        mDataRepository.getImageSetForPuzzle(puzzle.getFileName(), new DataRepository.DataCallback<Map<String, Bitmap>>() {
            @Override
            public void onDataReady(Map<String, Bitmap> data) {
                mDrawables = new HashMap<>(data.size());
                for (String key : data.keySet()) {
                    if(key.equals(Puzzle.Layout.EMPTY_CELL)) continue;
                    int finalX = Character.getNumericValue(key.charAt(0)) - 1;
                    int finalY = Character.getNumericValue(key.charAt(1)) - 1;
                    int startY;
                    int startX = startY = 0;

                    boolean found = false;

                    if(prevPositions != null){
                        int[] pos = prevPositions.getIntArray(key);
                        if(pos != null){
                            startX = pos[0];
                            startY = pos[1];
                            found = true;
                        }
                    }

                    for(int y = 0; !found && y < puzzle.getLayout().getDimensions().rows(); y++){
                        for(int x = 0; !found && x < puzzle.getLayout().getDimensions().columns(); x++){
                            if((found = key.equals(puzzle.getLayout().getCell(y, x)))){
                                startX = x;
                                startY = y;
                            }
                        }
                    }
                    mDrawables.put(key, new PuzzleView.PuzzleGridDrawable(data.get(key), startX, startY, finalX, finalY));
                }
                PuzzleView.PuzzleGridDrawable[] array = new PuzzleView.PuzzleGridDrawable[mDrawables.size()];
                mDrawables.values().toArray(array);
                puzzleView.setPieces(array, array[0].image.getWidth());
            }

            @Override
            public void onUpdateProgress(DataRepository.Progress progress) {

            }

            @Override
            public void onError(DataRepository.ErrorType error) {

            }
        });
        puzzleView.setCallback(new PuzzleView.PuzzleViewCallback(){

            public void onSetupFinished(){
                puzzleView.start();
                Log.d(TAG, "PuzzleView started");
            }

            @Override
            public void onMove(){
                mMovesText.setText(String.valueOf(++moves));
            }

            @Override
            public void onFinish(){
                mChrono.stop();
                Intent data = new Intent();
                data.putExtra(GAME_TIME_RESULT, getChronoElapsedTime());
                data.putExtra(GAME_MOVES_RESULT, moves);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mChrono.setBase(SystemClock.elapsedRealtime() - mPauseTime);
        mChrono.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPauseTime = getChronoElapsedTime();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(STATE_TIME, getChronoElapsedTime());
        outState.putInt(STATE_MOVES, moves);

        Bundle pieces = new Bundle();
        for(String key : mDrawables.keySet()){
            PuzzleView.PuzzleGridDrawable piece = mDrawables.get(key);
            pieces.putIntArray(key, new int[]{piece.pos.x, piece.pos.y});
        }
        outState.putBundle(STATE_PIECES, pieces);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((PuzzleView)findViewById(R.id.puzzleView)).stop();
    }

    private long getChronoElapsedTime(){
        return SystemClock.elapsedRealtime() - mChrono.getBase();
    }
}
