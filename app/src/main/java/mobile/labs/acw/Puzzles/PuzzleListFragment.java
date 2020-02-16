package mobile.labs.acw.Puzzles;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Map;

import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.R;
import mobile.labs.acw.Settings.SettingsActivity;

/**
 * Created by ryan on 15/02/2018.
 */

public class PuzzleListFragment extends Fragment implements PuzzleListContract.View {

    private static final String TAG = "PuzzleListFragment";
    private static final String PUZZLE_LIST_STATE = "listState";

    private PuzzleListContract.Presenter mPresenter;
    private OnFragmentInteractionListener mListener;

    private ListView mPuzzleList;
    private PopupMenu mFilterPopup;
    private View mView;

    private Parcelable mListViewState;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        mView = inflater.inflate(R.layout.puzzle_list_fragment, container, false);
        setHasOptionsMenu(true);
        mPuzzleList = (ListView) mView.findViewById(R.id.puzzleList);
        if(savedInstanceState != null) {
            mListViewState = savedInstanceState.getParcelable(PUZZLE_LIST_STATE);
        }
        mPuzzleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPresenter.onPuzzleSelected((Puzzle)mPuzzleList.getAdapter().getItem(position));
            }
        });
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener){
            mListener = (OnFragmentInteractionListener)context;
        } else {
            throw new RuntimeException(context.toString() +
                    " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "OnResume called");
        mPresenter.start();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG, "onCreateOptionsMenu called.");
        inflater.inflate(mPresenter.getMenu(R.menu.puzzle_list_all_menu, R.menu.puzzle_list_dl_menu), menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.puzzle_filter_item:
                mPresenter.onFilterPuzzles();
                return true;
            case R.id.puzzle_refresh_item:
                mPresenter.refreshPuzzleList(true);
                return true;
            case R.id.settings_item:
                startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mPuzzleList != null){
            outState.putParcelable(PUZZLE_LIST_STATE, mPuzzleList.onSaveInstanceState());
        }
    }

    public void showFilterOptions(Map<Integer, String> options, Integer[] checked) {
        if(mFilterPopup == null){
            mFilterPopup = new PopupMenu(getContext(), getActivity().findViewById(R.id.puzzle_filter_item));
            mFilterPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if(item.isChecked()) {
                        mPresenter.onFilterOptionDeselected(item.getItemId());
                    }
                    else {
                        mPresenter.onFilterOptionSelected(item.getItemId());
                    }
                    return true;
                }
            });

            mFilterPopup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                @Override
                public void onDismiss(PopupMenu menu) {
                    //mPresenter.refreshPuzzleList();
                }
            });
        }
        mFilterPopup.getMenu().clear();
        for(Integer option : options.keySet()) {
            MenuItem item = mFilterPopup.getMenu().add(Menu.NONE, option, Menu.NONE, options.get(option));
            item.setCheckable(true);
            for(Integer id : checked){
                if(id == option){
                    item.setChecked(true);
                    break;
                }
            }
        }
        mFilterPopup.show();
    }

    @Override
    public void setFilterSelected(int id, Boolean selected) {
        if(mFilterPopup == null) return;

        mFilterPopup.getMenu().findItem(id).setChecked(selected);
    }

    @Override
    public void setPresenter(PuzzleListContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void setLoading(boolean isLoading) {
        View loader = mView.findViewById(R.id.listLoader);
        loader.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setPuzzleList(Puzzle[] list) {
        final View.OnClickListener downloadButtonListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                final Button downloadButton = (Button)v;
                View item = (View)v.getTag();
                final ProgressBar progress = (ProgressBar)item.findViewById(R.id.downloadProgress);

                downloadButton.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);

                final Puzzle puzzle = (Puzzle)item.getTag();
                final int returnPos = mPuzzleList.getFirstVisiblePosition();
                mPresenter.downloadPuzzle(puzzle, new PuzzleListContract.Presenter.ProgressBarUpdater(){

                    @Override
                    public void onUpdateProgress(int percent) {
                        progress.setProgress(percent);
                    }

                    @Override
                    public void onFinished(boolean success) {
                        if(success) {
                            mListener.onPuzzleDownloaded(puzzle);
                            mPuzzleList.setSelection(returnPos);
                        } else {
                            downloadButton.setVisibility(View.VISIBLE);
                            progress.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };
        ArrayAdapter<Puzzle> arrayAdapter =
                new PuzzleListArrayAdapter(this.getContext(), R.layout.puzzle_list_item, list, downloadButtonListener);
        mPuzzleList.setAdapter(arrayAdapter);
        if(mListViewState != null) {
            mPuzzleList.onRestoreInstanceState(mListViewState);
        }
        mListViewState = null;
    }

    @Override
    public void displayError(String error) {
        Toast toast = Toast.makeText(getContext(), error, Toast.LENGTH_LONG);
        toast.show();
    }

    public void displayError(StringResource res){
        int resId;
        switch(res){
            case PuzzleNotDownloaded:
                resId = R.string.puzzle_not_downloaded;
                break;
            case CannotGetPuzzles:
                resId = R.string.connection_error;
                break;
            default:
                resId = R.string.unknown_error;
                break;
        }
        Toast toast = Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void showPuzzleDetail(Puzzle puzzle) {
        mListener.onPuzzleSelected(puzzle);
    }

    public interface OnFragmentInteractionListener{
        void onPuzzleSelected(Puzzle puzzle);
        void onPuzzleDownloaded(Puzzle puzzle);
    }
}
