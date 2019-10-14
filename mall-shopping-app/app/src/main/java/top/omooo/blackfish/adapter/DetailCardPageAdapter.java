package top.omooo.blackfish.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import top.omooo.blackfish.fragment.BillFragment;
import top.omooo.blackfish.fragment.PayHistoryFragment;

/**
 * Created by SSC on 2018/3/29.
 */

public class DetailCardPageAdapter extends FragmentPagerAdapter {

    private String[] titles = new String[]{"账单", "还款记录"};

    public DetailCardPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return BillFragment.newInstance();
        } else if (position == 1) {
            return PayHistoryFragment.newInstance();
        } else {
            return null;
        }
    }

    @Override
    public int getCount() {
        return titles.length;
    }
}
