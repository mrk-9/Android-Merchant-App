package info.blockchain.merchant.tabsswipe;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class TabsPagerAdapter extends FragmentPagerAdapter {

    private String[] tabs = null;

    public TabsPagerAdapter(FragmentManager fm, String[] tabs) {
        super(fm);
        this.tabs = tabs;
    }

    @Override
    public int getCount() {
        return tabs.length;
    }

    public CharSequence getPageTitle(int position){
        return tabs[position];
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new PaymentFragment();
            case 1:
                return new TransactionsFragment();
            default:
                return null;
        }
    }
}