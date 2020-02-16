package mobile.labs.acw.Puzzles;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import mobile.labs.acw.Data.Model.Puzzle;

/**
 * Created by ryan on 15/02/2018.
 */

public class PuzzleListContract {

    public interface View {
        void setPresenter(Presenter presenter);
        void setLoading(boolean isLoading);
        void setPuzzleList(Puzzle[] list);
        void showFilterOptions(Map<Integer, String> filterMap, Integer[] checked);
        void setFilterSelected(int id, Boolean selected);
        void displayError(String error);
        void displayError(StringResource res);
        void showPuzzleDetail(Puzzle puzzle);

        enum StringResource {
            PuzzleNotDownloaded,
            CannotGetPuzzles
        }
    }

    public interface Presenter {
        void start();
        void onPuzzleSelected(Puzzle name);
        void downloadPuzzle(Puzzle puzzle, ProgressBarUpdater progressUpdater);
        void refreshPuzzleList(boolean force);
        void setFilter(Puzzle.Layout.Dimensions dimens);
        void onFilterPuzzles();
        void onFilterOptionSelected(int itemId);
        void onFilterOptionDeselected(int itemId);

        int getMenu(int allMenuResource, int dlMenuResource);

        interface ProgressBarUpdater {
            void onUpdateProgress(int percent);
            void onFinished(boolean success);
        }
    }
}
