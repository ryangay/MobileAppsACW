package mobile.labs.acw.PuzzleDetail;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import mobile.labs.acw.Data.Model.Score;
import mobile.labs.acw.R;
import mobile.labs.acw.Util;

/**
 * Created by ryan on 18/03/2018.
 */

public class PuzzleScoresArrayAdapter extends ArrayAdapter<Score> {

    private final Score[] mScores;

    public PuzzleScoresArrayAdapter(@NonNull Context context, Score[] scores) {
        super(context, R.layout.score_list_item, scores);
        mScores = scores;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.score_list_item, parent, false);
        }

        Score score = mScores[position];

        Date date = new Date(mScores[position].getTimestamp());
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        ((TextView)convertView.findViewById(R.id.scoreDate)).setText(format.format(date));
        ((TextView)convertView.findViewById(R.id.scoreName)).setText(score.getName());
        ((TextView)convertView.findViewById(R.id.scoreMoves)).setText(String.valueOf(score.getMoves()));
        ((TextView)convertView.findViewById(R.id.scoreTime)).setText(Util.getMinutesSecondsString(score.getTime()));

        return convertView;
    }
}
