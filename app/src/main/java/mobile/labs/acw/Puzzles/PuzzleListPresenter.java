package mobile.labs.acw.Puzzles;

import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mobile.labs.acw.Data.DataRepository;
import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.Data.Model.Puzzle.Layout.Dimensions;

/**
 * Created by ryan on 15/02/2018.
 */

public class PuzzleListPresenter implements PuzzleListContract.Presenter {

    private static final String TAG = "PuzzleListPresenter";

    private static final int ANY_DIMENS = 0;
    private static final String ANY_DIMENS_STRING = "ANY";

    private final PuzzleListContract.View mView;
    private final DataRepository mDataRepository;
    private final Boolean mDownloadedOnly;

    private Map<Integer, Dimensions> mFilterMap;
    private Set<Integer> mSelectedFilters;

    public PuzzleListPresenter(PuzzleListContract.View view, DataRepository repo, Boolean downloadedOnly){
        mView = view;
        mDataRepository = repo;
        mDownloadedOnly = downloadedOnly;

        view.setPresenter(this);
    }

    @Override
    public void start() {
        fillList();
    }

    private void fillList(){
        mView.setLoading(true);
        refreshPuzzleList(false);
        mView.setLoading(false);
    }

    @Override
    public void onPuzzleSelected(Puzzle puzzle) {
        Log.d(TAG, puzzle + " has been selected.");
        if(!puzzle.isDownloaded()){
            mView.displayError(PuzzleListContract.View.StringResource.PuzzleNotDownloaded);
            return;
        }
        mView.showPuzzleDetail(puzzle);
    }

    @Override
    public void downloadPuzzle(Puzzle puzzle, final ProgressBarUpdater progressUpdater) {
        mDataRepository.downloadPuzzle(puzzle, new DataRepository.DataCallback<Boolean>() {
            @Override
            public void onDataReady(Boolean data) {
                progressUpdater.onFinished(data.booleanValue());
                refreshPuzzleList(false);
            }

            @Override
            public void onUpdateProgress(DataRepository.Progress progress) {
                progressUpdater.onUpdateProgress(progress.percent);
            }

            @Override
            public void onError(DataRepository.ErrorType error) {
                if(error == DataRepository.ErrorType.CONNECTION_ERROR)
                    mView.displayError(PuzzleListContract.View.StringResource.CannotGetPuzzles);
            }
        });
    }

    @Override
    public void setFilter(Puzzle.Layout.Dimensions dimens) {

    }

    public void setFilters(Puzzle.Layout.Dimensions[] dimens){

    }

    @Override
    public void onFilterPuzzles() {
        if(mFilterMap == null){
            createFilterMap(new Runnable() {
                @Override
                public void run() {
                    showViewFilterOptions();
                }
            });
        } else {
            showViewFilterOptions();
        }
    }

    private void showViewFilterOptions(){
        // Create the id -> string map for the view (we don't want it to know about data models)
        Map<Integer, String> viewMap = new HashMap<>(mFilterMap.size());
        for (Integer key : mFilterMap.keySet()) {
            viewMap.put(key, key == ANY_DIMENS ? ANY_DIMENS_STRING : mFilterMap.get(key).toString());
        }

        Integer[] array = new Integer[mSelectedFilters.size()];
        mView.showFilterOptions(viewMap, (mSelectedFilters.toArray(array)));
    }

    @Override
    public void onFilterOptionSelected(int itemId) {
        if(itemId == ANY_DIMENS) {
            for (int key : mFilterMap.keySet()){
                mSelectedFilters.add(key);
                mView.setFilterSelected(key, true);
            }
        } else {
            try {
                mSelectedFilters.add(itemId);
                mView.setFilterSelected(itemId, true);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Tried to add filter that is already in use.", e);
            }
            if (mFilterMap.size() == mSelectedFilters.size()){
                mView.setFilterSelected(ANY_DIMENS, true);
            }
        }
        refreshPuzzleList(false);

    }

    @Override
    public void onFilterOptionDeselected(int itemId) {
        if(itemId == ANY_DIMENS) {
            mSelectedFilters.clear();
            for(int key : mFilterMap.keySet()){
                mView.setFilterSelected(key, false);
            }
            return;
        }
        mSelectedFilters.remove(itemId);
        mView.setFilterSelected(itemId, false);
        refreshPuzzleList(false);
    }

    @Override
    public int getMenu(int allMenuResource, int dlMenuResource) {
        return mDownloadedOnly ? dlMenuResource : allMenuResource;
    }

    public void refreshPuzzleList(boolean force){

        mView.setLoading(true);

        final DataRepository.DataCallback<Puzzle[]> noFilterCallback = new DataRepository.DataCallback<Puzzle[]>() {

            @Override
            public void onDataReady(Puzzle[] puzzles) {
                if(puzzles == null) {
                    mView.displayError(PuzzleListContract.View.StringResource.CannotGetPuzzles);
                }
                if(!mDownloadedOnly) {
                    mView.setPuzzleList(puzzles);
                } else {
                    final List<Puzzle> puzzlesToShow = new ArrayList<>();
                    for (Puzzle puzzle : puzzles) {
                        if (puzzle.isDownloaded()) {
                            puzzlesToShow.add(puzzle);
                        }
                    }
                    Puzzle[] array = new Puzzle[puzzlesToShow.size()];
                    mView.setPuzzleList(puzzlesToShow.toArray(array));
                }

                mView.setLoading(false);
            }

            @Override
            public void onUpdateProgress(DataRepository.Progress progress) {

            }

            @Override
            public void onError(DataRepository.ErrorType error) {
                if(error == DataRepository.ErrorType.CONNECTION_ERROR) {
                    mView.displayError(PuzzleListContract.View.StringResource.CannotGetPuzzles);
                }
                mView.setLoading(false);
            }
        };

        final List<Puzzle> puzzlesToShow = new ArrayList<>();
        class FilterCallback implements DataRepository.DataCallback<Puzzle[]> {

            private int maxIterations;
            private int iterations;

            public FilterCallback(int maxIterations){
                this.maxIterations = maxIterations;
            }

            @Override
            public void onDataReady(Puzzle[] puzzles) {
                for (Puzzle puzzle : puzzles) {
                    puzzlesToShow.add(puzzle);
                }

                if((++iterations+1) == maxIterations) {
                    Puzzle[] array = new Puzzle[puzzlesToShow.size()];
                    mView.setPuzzleList((puzzlesToShow.toArray(array)));
                }
            }

            @Override
            public void onUpdateProgress(DataRepository.Progress progress) {

            }

            @Override
            public void onError(DataRepository.ErrorType error) {

            }
        };

        if(mSelectedFilters == null || mSelectedFilters == mFilterMap.keySet()){
            try {
                mDataRepository.getPuzzleList(force, noFilterCallback);
            } catch (IOException e){
                Log.e(TAG, "Failed to get puzzles", e);
            }
        } else {
            final List<Dimensions> selectedDimensions = new ArrayList<>(mSelectedFilters.size());

            for (Integer dimensKey : mSelectedFilters) {
                selectedDimensions.add(mFilterMap.get(dimensKey));
            }

            DataRepository.DataCallback<Puzzle[]> callback = new FilterCallback(selectedDimensions.size());
            for (Dimensions dimens : selectedDimensions) {
                if(dimens == null) continue;
                mDataRepository.getPuzzleList(callback, dimens.rows(), dimens.columns());
            }
        }
    }

    private void createFilterMap(final Runnable after) {
        try {
            mDataRepository.getAvailableDimensions(new DataRepository.DataCallback<Collection<Dimensions>>() {
                @Override
                public void onDataReady(Collection<Dimensions> dimensions) {
                    mFilterMap = new HashMap<>();
                    mFilterMap.put(ANY_DIMENS, null);
                    int key = ANY_DIMENS + 1;
                    for(Puzzle.Layout.Dimensions dimens : dimensions) {
                        mFilterMap.put(key++, dimens);
                    }
                    if(mSelectedFilters == null) {
                        // Assume that all filters are selected at first instance
                        mSelectedFilters = new HashSet<>(mFilterMap.keySet());
                    }
                    after.run();
                }

                @Override
                public void onUpdateProgress(DataRepository.Progress progress) {

                }

                @Override
                public void onError(DataRepository.ErrorType error) {

                }
            });
        } catch (IOException e){
            Log.e(TAG, "Error creating filter map..", e);
        }
    }
}
