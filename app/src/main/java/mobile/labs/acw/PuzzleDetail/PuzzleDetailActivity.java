package mobile.labs.acw.PuzzleDetail;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import mobile.labs.acw.Arena.ArenaActivity;
import mobile.labs.acw.DI;
import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.R;
import mobile.labs.acw.Data.Model.Score;
import mobile.labs.acw.Util;

public class PuzzleDetailActivity extends AppCompatActivity implements PuzzleDetailContract.View {

    public static final String ARG_PUZZLE = "puzzle";
    public static final int    GAME_FINISHED_REQUEST = 1;

    private PuzzleDetailContract.Presenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        new PuzzleDetailPresenter(this, DI.provideDataRepository(this), DI.provideScoreProvider(this), (Puzzle)getIntent().getParcelableExtra(ARG_PUZZLE));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.onPlayClicked();
            }
        });

        ViewCompat.setNestedScrollingEnabled(findViewById(R.id.highScoreList), true);
    }

    @Override
    public void setPresenter(PuzzleDetailContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void setTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void setPuzzleImages(Bitmap[][] images, int imageSize, Bitmap.Config imageConfig) {
        // Reference: https://stackoverflow.com/questions/4863518/combining-two-bitmap-image-side-by-side

        int rows = images.length;
        int columns = images[0].length;

        int width = columns*imageSize;
        int height = rows*imageSize;

        Bitmap template = Bitmap.createBitmap(width, height, imageConfig);

        Canvas canvas = new Canvas(template);
        Paint paint = new Paint();
        for(int y = 0; y < rows; y++){
            for(int x = 0; x < columns; x++){
                Bitmap bmp = images[y][x];
                if(bmp == null) continue;
                canvas.drawBitmap(images[y][x], x*imageSize, y*imageSize, paint);
            }
        }

        ((ImageView)findViewById(R.id.puzzleImage)).setImageBitmap(template);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void setHighScores(Score[] highScores) {
        LinearLayout highScoreList = ((LinearLayout)findViewById(R.id.highScoreList));
        ArrayAdapter adapter = new PuzzleScoresArrayAdapter(this, highScores);

        if(highScoreList.getChildCount() > adapter.getCount()){
            for(int i = adapter.getCount(); i < highScoreList.getChildCount(); i++){
                highScoreList.removeViewAt(i);
            }
        }

        for(int i = 0; i < adapter.getCount(); i++){
            View currentView = highScoreList.getChildAt(i);
            View inflated = adapter.getView(i, currentView, highScoreList);

            if(currentView == null){
                highScoreList.addView(inflated);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GAME_FINISHED_REQUEST){
            switch(resultCode){
                case RESULT_OK:
                    showNameEntryAndStore(data.getLongExtra(ArenaActivity.GAME_TIME_RESULT, 0), data.getIntExtra(ArenaActivity.GAME_MOVES_RESULT, 0));
                    break;
            }
        }
    }

    private void showNameEntryAndStore(final long timeElapsed, final int moves){
        final View main = findViewById(R.id.mainDetailView);
        final View scoreView = findViewById(R.id.scoreDetailView);
        final TextView movesMade = (TextView)findViewById(R.id.movesMade);
        final TextView timeTaken = (TextView)findViewById(R.id.timeTaken);
        final EditText nameText = ((EditText)findViewById(R.id.scoreNameEdit));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        movesMade.setText(String.valueOf(moves));
        timeTaken.setText(Util.getMinutesSecondsString(timeElapsed));
        if(prefs.contains(getResources().getString(R.string.pref_name_key))){
            nameText.setText(prefs.getString(getResources().getString(R.string.pref_name_key), null));
        }
        scoreView.setVisibility(View.VISIBLE);
        main.setVisibility(View.GONE);
        nameText.requestFocus();
        (findViewById(R.id.submitName)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.storeResult(timeElapsed, moves, nameText.getText().toString());
                Toast.makeText(PuzzleDetailActivity.this, R.string.score_saved, Toast.LENGTH_SHORT);
                AlertDialog.Builder builder = new AlertDialog.Builder(PuzzleDetailActivity.this);
                builder.setMessage(R.string.shareScoreQuestion);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT,
                                getResources().getString(R.string.shareMessage, mPresenter.getPuzzleFriendlyName(), moves, Util.getMinutesSecondsString(timeElapsed)));
                        dialogInterface.dismiss();
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
                main.setVisibility(View.VISIBLE);
                scoreView.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public void play(Puzzle puzzle) {
        Intent intent = new Intent(this, ArenaActivity.class);
        intent.putExtra(ArenaActivity.PUZZLE_EXTRA, puzzle);
        startActivityForResult(intent, GAME_FINISHED_REQUEST);
    }
}
