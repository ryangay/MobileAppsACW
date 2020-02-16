package mobile.labs.acw.Puzzles;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ryan on 23/02/2018.
 */

public class PuzzlePagerAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mFragmentList = new ArrayList<>();
    private final List<CharSequence> mTitleList = new ArrayList<>();
    private final PagerFragmentListener mListener;

    PuzzlePagerAdapter(FragmentManager fm, PagerFragmentListener listener) {
        super(fm);
        mListener = listener;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitleList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public void addFragment(Fragment fragment, CharSequence title) {
        mFragmentList.add(fragment);
        mTitleList.add(title);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment)super.instantiateItem(container, position);
        mFragmentList.remove(position);
        mFragmentList.add(position, fragment);
        mListener.onFragmentInstantiated(position, fragment.getTag());
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        mListener.onFragmentDestroyed(position);
    }

    interface PagerFragmentListener{
        void onFragmentInstantiated(int position, String tag);
        void onFragmentDestroyed(int position);
    }
}
