package com.quarkonium.qpocket.model.transaction;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.quarkonium.qpocket.api.db.table.QWToken;

public class TransactionPagerAdapter extends FragmentStatePagerAdapter {

    private String[] mTitles;
    private QWToken mToken;

    TransactionPagerAdapter(FragmentManager fm, String[] titles) {
        super(fm);
        mTitles = titles;
    }

    TransactionPagerAdapter(FragmentManager fm, String[] titles, QWToken token) {
        super(fm);
        mTitles = titles;
        mToken = token;
    }

    @Override
    public Fragment getItem(int position) {
        if (mToken != null) {
            return TransactionFragment.getInstance(mToken, position);
        }
        return TransactionFragment.getInstance(position);
    }

    @Override
    public int getCount() {
        return mTitles != null ? mTitles.length : 0;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

}
