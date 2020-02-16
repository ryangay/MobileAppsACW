package mobile.labs.acw.Puzzles;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import mobile.labs.acw.DI;
import mobile.labs.acw.Data.Model.Puzzle;
import mobile.labs.acw.PuzzleDetail.PuzzleDetailActivity;
import mobile.labs.acw.R;

public class PuzzleListActivity extends AppCompatActivity implements PuzzleListFragment.OnFragmentInteractionListener {

    private static final String ALL_FRAG_TAG_KEY = "ALL_TAG";
    private static final String DOWNLOAD_FRAG_TAG_KEY = "DOWNLOADED_TAG";
    private static final String FILTER_KEY = "PUZZLE_FILTER";

    private PuzzleListContract.Presenter mPresenter;
    private PuzzleListContract.Presenter mDownloadedPresenter;
    private String mAllViewTag;
    private String mDownloadedViewTag;
    private Toolbar mToolbar;
    private PuzzlePagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        final PuzzlePagerAdapter.PagerFragmentListener listener = new PuzzlePagerAdapter.PagerFragmentListener(){

            @Override
            public void onFragmentInstantiated(int position, String tag) {
                if(position == 0){
                    mAllViewTag = tag;
                } else if (position == 1){
                    mDownloadedViewTag = tag;
                }
            }

            @Override
            public void onFragmentDestroyed(int position) {
                if(position == 0){
                    mAllViewTag = null;
                } else if (position == 1){
                    mDownloadedViewTag = null;
                }
            }
        };

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new PuzzlePagerAdapter(getSupportFragmentManager(), listener);

        if(savedInstanceState != null){
            mAllViewTag = savedInstanceState.getString(ALL_FRAG_TAG_KEY);
            mDownloadedViewTag = savedInstanceState.getString(DOWNLOAD_FRAG_TAG_KEY);
        }
        setupPager(mPagerAdapter);
        mTabLayout = (TabLayout)findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        Puzzle.Layout.Dimensions filterLayout;
        if(savedInstanceState != null){
            mPresenter.setFilter((Puzzle.Layout.Dimensions) savedInstanceState.getSerializable(FILTER_KEY));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPuzzleSelected(Puzzle puzzle) {
        Intent intent = new Intent(this, PuzzleDetailActivity.class);
        intent.putExtra(PuzzleDetailActivity.ARG_PUZZLE, puzzle);
        startActivity(intent);
    }

    @Override
    public void onPuzzleDownloaded(Puzzle puzzle) {
        mDownloadedPresenter.refreshPuzzleList(false);
    }

    private void setupPager(PuzzlePagerAdapter adapter){
        PuzzleListFragment allFragment =  (PuzzleListFragment) getSupportFragmentManager().findFragmentByTag(mAllViewTag);
        PuzzleListFragment downloadedFragment = (PuzzleListFragment) getSupportFragmentManager().findFragmentByTag(mDownloadedViewTag);
        if(allFragment == null){
            allFragment = new PuzzleListFragment();
        }
        if(downloadedFragment == null){
            downloadedFragment = new PuzzleListFragment();
        }
        mPresenter = new PuzzleListPresenter(allFragment, DI.provideDataRepository(this), false);
        mDownloadedPresenter = new PuzzleListPresenter(downloadedFragment, DI.provideDataRepository(this), true);
        adapter.addFragment(allFragment, getResources().getString(R.string.allList));
        adapter.addFragment(downloadedFragment, getResources().getString(R.string.downloadedList));
        mViewPager.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ALL_FRAG_TAG_KEY, mAllViewTag);
        outState.putString(DOWNLOAD_FRAG_TAG_KEY, mDownloadedViewTag);
    }
}
