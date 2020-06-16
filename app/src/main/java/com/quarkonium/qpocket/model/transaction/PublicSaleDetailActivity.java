package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWPublicScale;
import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.transaction.bean.PublicTokenLoadBean;
import com.quarkonium.qpocket.model.transaction.viewmodel.PublicSaleDetailViewModel;
import com.quarkonium.qpocket.model.transaction.viewmodel.PublicSaleDetailViewModelFactory;
import com.quarkonium.qpocket.rx.RxBus;
import com.quarkonium.qpocket.rx.Subscribe;
import com.quarkonium.qpocket.rx.ThreadMode;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.SlidingTabLayout;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class PublicSaleDetailActivity extends BaseActivity {

    public static class PublicTokenTransactionPagerAdapter extends FragmentStatePagerAdapter {

        private String[] mTitles;
        private String mTokenAddress;

        PublicTokenTransactionPagerAdapter(FragmentManager fm, String[] titles, String address) {
            super(fm);
            mTitles = titles;
            mTokenAddress = address;
        }

        @Override
        public Fragment getItem(int position) {
            return PublicTokenTransactionFragment.getInstance(position, mTokenAddress);
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

    public static void startPublicSaleDetailActivity(Activity activity, QWPublicScale bean) {
        Intent intent = new Intent(activity, PublicSaleDetailActivity.class);
        intent.putExtra(Constant.KEY_TOKEN, bean);
        activity.startActivity(intent);
    }

    @Inject
    PublicSaleDetailViewModelFactory mPublicSaleFragmentFactory;
    private PublicSaleDetailViewModel mPublicSaleFragmentViewModel;

    private TextView mBalanceView;
    private TextView mAvailableView;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SmartRefreshLayout mTxSmartRefreshLayout;
    private ViewPager mViewPage;
    private View mEmptyView;
    private int mMaxHeight;

    private String mAddress;
    private String mBuyRate;
    private String mSymbol;

    private QWPublicScale mPublicScale;
    private QWWallet mDefaultWallet;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_public_sale_token_detail;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_transaction_detail_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        findViewById(R.id.public_sale_buy).setOnClickListener(v -> onBuy());

        mPublicScale = getIntent().getParcelableExtra(Constant.KEY_TOKEN);
        String startTime = mPublicScale.getStartTime();
        TextView startView = findViewById(R.id.public_sale_start_time);
        startView.setText(QWWalletUtils.parseTimeFor10(startTime, QWWalletUtils.FORMAT_END));

        String endTime = mPublicScale.getEndTime();
        TextView endView = findViewById(R.id.public_sale_end_time);
        endView.setText(QWWalletUtils.parseTimeFor10(endTime, QWWalletUtils.FORMAT_END));


        mSymbol = mPublicScale.getToken().getSymbol();
        mBuyRate = mPublicScale.getBuyRate();
        QWToken tokenItem = mPublicScale.getToken();
        String mSymbol = tokenItem.getSymbol();
        TextView priceView = findViewById(R.id.public_sale_price);
        if (mPublicScale.getToken().getType() == Constant.ACCOUNT_TYPE_ETH) {
            priceView.setText(String.format(getString(R.string.public_sale_eth_price), mBuyRate, mSymbol.toUpperCase()));
        } else {
            priceView.setText(String.format(getString(R.string.public_sale_price), mBuyRate, mSymbol.toUpperCase()));
        }

        String name = tokenItem.getSymbol().toUpperCase();
        TextView titleTextView = mTopBarView.getTitleView();
        titleTextView.setText(name);
        setTitleString(name);

        String path = tokenItem.getIconPath();
        ImageView icon = findViewById(R.id.public_sale_img);
        Glide.with(icon)
                .asBitmap()
                .load(path)
                .into(icon);

        TextView nameView = findViewById(R.id.public_sale_name);
        nameView.setText(name);

        TextView desView = findViewById(R.id.token_detail_des);
        if (ToolUtils.isZh(this)) {
            String tokenCount = tokenItem.getDescriptionCn();
            desView.setText(tokenCount);
        } else {
            String tokenCount = tokenItem.getDescriptionEn();
            desView.setText(tokenCount);
        }

        TextView webSiteView = findViewById(R.id.public_sale_website);
        webSiteView.setText(tokenItem.getUrl());

        mAddress = tokenItem.getAddress();
        TextView addressView = findViewById(R.id.public_sale_address);
        addressView.setText(mAddress);

        mSwipeRefreshLayout = findViewById(R.id.transaction_swipe_view);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        mBalanceView = findViewById(R.id.public_sale_balance);
        mAvailableView = findViewById(R.id.public_sale_available);
        String defaultText = QWWalletUtils.getIntTokenFromWei16(mPublicScale.getAvailability()) + " " + mSymbol.toUpperCase();
        mAvailableView.setText(defaultText);

        mEmptyView = findViewById(R.id.tx_empty_layout);
        mViewPage = findViewById(R.id.public_token_transaction_page);
        mViewPage.setOffscreenPageLimit(1);
        String[] titles = getResources().getStringArray(R.array.public_sale_transaction_tag);
        PublicTokenTransactionPagerAdapter mAdapter = new PublicTokenTransactionPagerAdapter(getSupportFragmentManager(), titles, mAddress);
        mViewPage.setAdapter(mAdapter);
        SlidingTabLayout mTabLayout = findViewById(R.id.public_token_transaction_tab);
        mTabLayout.setEndHasMargin(false);
        mTabLayout.setViewPager(mViewPage, titles);
        mTabLayout.setCurrentTab(0);
        float height = getResources().getDisplayMetrics().heightPixels - getStatusBarHeight() - getResources().getDimension(R.dimen.appbar_top_height);
        height = height - UiUtils.dpToPixel(20 + 50);//mSwipeRefreshLayout内容区高度
        mMaxHeight = (int) (height - UiUtils.dpToPixel(10 + 60));//减tab栏高度

        mTxSmartRefreshLayout = findViewById(R.id.detail_tx_swipe);
        mTxSmartRefreshLayout.setEnableRefresh(false);
        mTxSmartRefreshLayout.setEnableAutoLoadMore(true);
        mTxSmartRefreshLayout.setEnableLoadMoreWhenContentNotFull(true);
        mTxSmartRefreshLayout.setFooterHeight(30);
        mTxSmartRefreshLayout.setOnLoadMoreListener(v -> onLoadMoreRequested());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        //获取当前主钱包
        mPublicSaleFragmentViewModel = new ViewModelProvider(this, mPublicSaleFragmentFactory)
                .get(PublicSaleDetailViewModel.class);
        mPublicSaleFragmentViewModel.findDefaultWalletObserve().observe(this, this::findWalterSuccess);
        mPublicSaleFragmentViewModel.tokenBeanObserve().observe(this, this::onTokenBeanSuccess);

        mPublicSaleFragmentViewModel.transaction().observe(this, this::onTokenTransactionSuccess);
        mPublicSaleFragmentViewModel.publicBalanceObserve().observe(this, this::onPublicBalanceSuccess);

        //上拉加载
        mPublicSaleFragmentViewModel.transactionObserve().observe(this, v -> loadMoreFinish());
        mPublicSaleFragmentViewModel.loadMoreFailObserve().observe(this, v -> loadMoreFinish());

        mPublicSaleFragmentViewModel.findWallet();

        RxBus.get().register(this);
    }

    private void findWalterSuccess(QWWallet wallet) {
        mDefaultWallet = wallet;

        mSwipeRefreshLayout.setRefreshing(true);
        Constant.sTransactionNext = SharedPreferencesUtils.getCurrentTransactionNext(getApplicationContext(), mAddress);
        //获取token balance
        mPublicSaleFragmentViewModel.findTokenBalance(mDefaultWallet.getCurrentAccount(), mPublicScale.getToken());
        //获取交易记录
        mPublicSaleFragmentViewModel.getPublicTokenTransaction(mAddress, true);
    }

    private void onTokenBeanSuccess(TokenBean bean) {
        QWBalance balance = bean.getBalance();
        if (balance != null) {
            String tokenCount = QWWalletUtils.getIntTokenFromWei16(balance.getBalance()) + " " + bean.getToken().getSymbol().toUpperCase();
            mBalanceView.setText(tokenCount);
        } else {
            String defaultText = "0 " + bean.getToken().getSymbol().toUpperCase();
            mBalanceView.setText(defaultText);
        }
    }

    private void onTokenTransactionSuccess(List<QWPublicTokenTransaction> list) {
        if (list != null && !list.isEmpty()) {
            int size = list.size();
            int height = Math.min((int) (UiUtils.dpToPixel(56) * size), mMaxHeight);
            if (mViewPage.getMeasuredHeight() != height) {
                ViewGroup.LayoutParams layoutParams = mViewPage.getLayoutParams();
                layoutParams.height = height;
                mViewPage.setLayoutParams(layoutParams);
            }
            mViewPage.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        } else {
            mViewPage.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
        PublicTokenLoadBean bean = new PublicTokenLoadBean();
        bean.setList(list);
        bean.setHasLoadMore(list != null && list.size() == Constant.QKC_TRANSACTION_LIMIT_INT);
        mPublicSaleFragmentViewModel.updateTransactionObserve(bean);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void onPublicBalanceSuccess(QWBalance[] balances) {
        if (balances.length == 2) {
            QWBalance token = balances[0];
            if (token != null) {
                String tokenCount = QWWalletUtils.getIntTokenFromWei16(token.getBalance());
                QWToken qwToken = mPublicScale.getToken();
                if (qwToken != null) {
                    tokenCount = tokenCount + " " + qwToken.getSymbol().toUpperCase();
                }
                mBalanceView.setText(tokenCount);
            }

            QWBalance available = balances[1];
            if (available != null) {
                String tokenCount = QWWalletUtils.getIntTokenFromWei16(available.getBalance());
                QWToken qwToken = mPublicScale.getToken();
                if (qwToken != null) {
                    tokenCount = tokenCount + " " + qwToken.getSymbol().toUpperCase();
                }
                mAvailableView.setText(tokenCount);
            }

            //同步
            RxBus.get().send(Constant.RX_BUS_CODE_PUBLIC_SALE, "");
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }


    private void loadMoreFinish() {
        mTxSmartRefreshLayout.finishLoadMore();
    }

    private void onRefresh() {
        if (mDefaultWallet == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            mSwipeRefreshLayout.setRefreshing(false);
            MyToast.showSingleToastShort(getApplicationContext(), R.string.network_error);
            return;
        }

        mPublicSaleFragmentViewModel.findPublicTokenBalance(mDefaultWallet.getCurrentAccount(), mPublicScale.getToken());
        //获取交易记录
        mPublicSaleFragmentViewModel.getPublicTokenTransaction(mAddress, false);
    }

    private void onLoadMoreRequested() {
        mPublicSaleFragmentViewModel.getTransactionsByNext(mAddress, Constant.sTransactionNext);
    }

    private void onBuy() {
        if (mDefaultWallet == null) {
            return;
        }
        Intent intent = new Intent(this, PublicSaleTransactionCreateActivity.class);
        intent.putExtra(Constant.WALLET_ADDRESS, mAddress);
        intent.putExtra(Constant.KEY_TOKEN_SCALE, mBuyRate);
        intent.putExtra(Constant.KEY_TOKEN_SYMBOL, mSymbol);
        startActivity(intent);
        UmengStatistics.walletPublicSaleBuyClickCount(getApplicationContext(), mSymbol, mAddress);
    }

    @Subscribe(code = Constant.RX_BUS_CODE_TRANS_COST, threadMode = ThreadMode.MAIN)
    public void rxBusEventToken(String[] value) {
        if (value != null && mDefaultWallet.getCurrentAccount().getTransactions() != null) {
            PublicTokenLoadBean bean = mPublicSaleFragmentViewModel.transactionObserve().getValue();
            if (bean != null) {
                for (QWPublicTokenTransaction transaction : bean.getList()) {
                    if (transaction.getTxId().equals(value[0])) {
                        transaction.setCost(value[1]);
                    }
                }
                //更新Transaction
                mPublicSaleFragmentViewModel.updateTransactionObserve(bean);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.get().unRegister(this);
    }
}
