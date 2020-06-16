package com.quarkonium.qpocket.model.wallet;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.SlidingTabLayout;
import com.quarkonium.qpocket.R;

public class ImportWalletActivity extends BaseActivity {

    private static class ImportWalletPagerAdapter extends FragmentStatePagerAdapter {

        private String[] mTitles;
        private boolean mColdMode;

        ImportWalletPagerAdapter(FragmentManager fm, String[] titles, boolean coldMode) {
            super(fm);
            mTitles = titles;
            mColdMode = coldMode;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return ImportWalletFragment.getInstance(TAGS[position], mColdMode);
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

    public final static int IMPORT_WALLET_TAG_PHRASE = 0;
    public final static int IMPORT_WALLET_TAG_PRIVATE_KEY = 1;
    public final static int IMPORT_WALLET_TAG_KEYSTORE = 2;
    public final static int IMPORT_WALLET_TAG_WATCH = 3;
    private static final int[] TAGS = new int[]{
            IMPORT_WALLET_TAG_PHRASE,//助记词
            IMPORT_WALLET_TAG_PRIVATE_KEY,//私钥
            IMPORT_WALLET_TAG_KEYSTORE,//keystore
            IMPORT_WALLET_TAG_WATCH,//观察钱包
    };

    private View mProgressLayout;


    @Override
    protected int getLayoutResource() {
        return R.layout.activity_home_import_wallet;
    }

    @Override
    public int getActivityTitle() {
        return R.string.import_wallet_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.import_wallet_title);

        boolean isColdMode = getIntent().getBooleanExtra(Constant.KEY_COLD_MODE, false);
        ViewPager mViewPager = findViewById(R.id.import_wallet_view_page);
        mViewPager.setOffscreenPageLimit(1);
        SlidingTabLayout mTabLayout = findViewById(R.id.import_wallet_tab_view);

        String[] titles = isColdMode ?
                getResources().getStringArray(R.array.import_wallet_tag_cold) :
                getResources().getStringArray(R.array.import_wallet_tag);
        ImportWalletPagerAdapter mAdapter = new ImportWalletPagerAdapter(getSupportFragmentManager(), titles, isColdMode);
        mViewPager.setAdapter(mAdapter);
        mTabLayout.setEndHasMargin(false);
        mTabLayout.setCurrentTab(0);
        if (!isColdMode && ToolUtils.isRu(getApplicationContext())) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mTabLayout.getLayoutParams();
            lp.leftMargin = (int) getResources().getDimension(R.dimen.dp_15);
            lp.rightMargin = (int) getResources().getDimension(R.dimen.dp_15);
            mTabLayout.setLayoutParams(lp);
            mTabLayout.setTabPadding(getResources().getDimension(R.dimen.dp_6));
            mTabLayout.setTabSpaceEqual(false);
        }
        mTabLayout.setViewPager(mViewPager, titles);

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    public void showProgress(boolean isShow) {
        if (isShow) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }
}
