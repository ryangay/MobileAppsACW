package mobile.labs.acw.PuzzleDetail;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.R;
import mobile.labs.acw.Data.Model.Score;
import mobile.labs.acw.PuzzleDetail.PuzzleDetailContract.Presenter;

public class PuzzleDetailFragment extends Fragment implements PuzzleDetailContract.View {

    private Presenter mPresenter;
    private OnFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        //return inflater.inflate(R.layout.fragment_puzzle_detail, container, false);
        return inflater.inflate(android.R.layout.activity_list_item, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.puzzle_detail_menu, menu);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void setPresenter(Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void setTitle(String title) {

    }

    @Override
    public void setPuzzleImages(Bitmap[][] images, int imageSize, Bitmap.Config config) {

    }

    @Override
    public void setHighScores(Score[] highScores) {

    }

    @Override
    public void play(Puzzle puzzle) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onPlay(Puzzle puzzle);
        void setTitle(String title);
    }
}
