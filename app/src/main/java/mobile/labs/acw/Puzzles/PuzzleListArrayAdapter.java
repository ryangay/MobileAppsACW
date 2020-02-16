package mobile.labs.acw.Puzzles;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.R;

/**
 * Created by ryan on 01/03/2018.
 */

public class PuzzleListArrayAdapter extends ArrayAdapter<Puzzle> {

    private final int mResource;
    private Puzzle[] mPuzzles;
    private final View.OnClickListener mDownloadButtonListener;

    public PuzzleListArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull Puzzle[] objects, View.OnClickListener downloadButtonListener) {
        super(context, resource, objects);
        mResource = resource;
        mPuzzles = objects;
        mDownloadButtonListener = downloadButtonListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
        }

        Puzzle puzzle = mPuzzles[position];
        convertView.setTag(puzzle);
        ((TextView)convertView.findViewById(R.id.puzzleName)).setText(puzzle.toString());
        Button downloadButton = (Button)convertView.findViewById(R.id.downloadPuzzle);
        downloadButton.setTag(convertView);
        downloadButton.setVisibility(puzzle.isDownloaded() ? View.INVISIBLE : View.VISIBLE);
        downloadButton.setOnClickListener(mDownloadButtonListener);
        return convertView;
    }

    public void replaceAll(Puzzle[] puzzles) {
        super.clear();
        super.addAll(puzzles);
    }
}
