package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenTransactionDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.model.main.WalletBitmapAddressActivity;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.main.bean.TransactionLoadBean;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.rx.ChangeWalletEvent;
import com.quarkonium.qpocket.rx.RxBus;
import com.quarkonium.qpocket.rx.Subscribe;
import com.quarkonium.qpocket.rx.ThreadMode;
import com.quarkonium.qpocket.tron.TronWalletClient;
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

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class OtherTokenDetailActivity extends BaseActivity {

    public static void startOtherTokenDetailActivity(Activity activity, QWWallet wallet, TokenBean bean) {
        Intent intent = new Intent(activity, OtherTokenDetailActivity.class);
        intent.putExtra(Constant.KEY_WALLET, wallet);
        intent.putExtra(Constant.KEY_TOKEN, bean.getToken());
        intent.putExtra(Constant.KEY_BALANCE, bean.getBalance());
        activity.startActivity(intent);
    }

    public static void startOtherTokenDetailActivity(Activity activity, QWWallet wallet, QWToken token) {
        Intent intent = new Intent(activity, OtherTokenDetailActivity.class);
        intent.putExtra(Constant.KEY_WALLET, wallet);
        intent.putExtra(Constant.KEY_TOKEN, token);
        activity.startActivity(intent);
    }

    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    private QWWallet mQuarkWallet;
    private QWToken mToken;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mBalanceText;
    private TextView mPriceText;

    private SmartRefreshLayout mTxSmartRefreshLayout;
    private ViewPager mViewPage;
    private View mEmptyView;
    private int mMaxHeight;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_other_token_detail;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_transaction_detail_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.wallet_transaction_detail_title);
        findViewById(R.id.transaction_receive).setOnClickListener(v -> onReceive());
        findViewById(R.id.transaction_send).setOnClickListener(v -> onSend());

        mQuarkWallet = getIntent().getParcelableExtra(Constant.KEY_WALLET);
        mToken = getIntent().getParcelableExtra(Constant.KEY_TOKEN);

        String nameText = mToken.getSymbol().toUpperCase();
        if (!QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol())
                && !QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol())
                && !QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())) {
            TextView titleTextView = mTopBarView.getTitleView();
            titleTextView.setText(nameText);
            setTitleString(nameText);
        }

        //btc
        TextView mAddressView = findViewById(R.id.token_detail_address);
        if (QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol())) {
            findViewById(R.id.token_smart_address_title).setVisibility(View.GONE);
            mAddressView.setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mAddressView.getLayoutParams();
            lp.bottomMargin = 0;
            lp.topMargin = (int) UiUtils.dpToPixel(12);
            mAddressView.setLayoutParams(lp);
        }

        //trx
        if (QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())) {
            View trxFreeze = findViewById(R.id.trx_freeze);
            trxFreeze.setVisibility(View.VISIBLE);
            trxFreeze.setOnClickListener(v -> freezeTrx());

            TextView smartTitle = findViewById(R.id.token_smart_address_title);
            smartTitle.setText(R.string.trx_trc10_token_detail_address);
        } else if (TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress())) {
            TextView smartTitle = findViewById(R.id.token_smart_address_title);
            smartTitle.setText(R.string.trx_trc10_token_detail_address);
        } else if (TronWalletClient.isTronAddressValid(mToken.getAddress())) {
            View view = findViewById(R.id.trc20_transaction_empty);
            view.setVisibility(View.VISIBLE);
        }


        if (!TextUtils.isEmpty(mToken.getIconPath())) {
            ImageView icon = findViewById(R.id.token_detail_img);
            Glide.with(this)
                    .asBitmap()
                    .load(mToken.getIconPath())
                    .into(icon);
        }
        TextView name = findViewById(R.id.token_detail_name);
        name.setText(nameText);


        mBalanceText = findViewById(R.id.token_detail_count);
        mPriceText = findViewById(R.id.transaction_total_token_price);
        QWBalance mTokenBalance = getIntent().getParcelableExtra(Constant.KEY_BALANCE);
        if (mTokenBalance != null) {
            String tokenCount = QWWalletUtils.getIntTokenFromWei16(mTokenBalance.getBalance(), mToken.getTokenUnit());
            mBalanceText.setText(tokenCount);

            String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), mToken.getSymbol(), tokenCount);
            mPriceText.setText(priceStr);
        } else {
            mBalanceText.setText("0");

            String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), mToken.getSymbol(), "0");
            mPriceText.setText(priceStr);
        }


        TextView mWebSiteView = findViewById(R.id.token_detail_website);
        TextView mDesView = findViewById(R.id.token_detail_des);
        if (TextUtils.isEmpty(mToken.getUrl())) {
            mWebSiteView.setTextColor(getResources().getColor(R.color.text_message));
            mWebSiteView.setText(R.string.none);
            mWebSiteView.setAutoLinkMask(0);
        } else {
            mWebSiteView.setText(mToken.getUrl());
        }
        mAddressView.setText(mToken.getAddress());
        if (ToolUtils.isZh(this)) {
            mDesView.setText(mToken.getDescriptionCn());
        } else {
            mDesView.setText(mToken.getDescriptionEn());
        }
        if (TextUtils.isEmpty(mDesView.getText())) {
            findViewById(R.id.token_detail_line).setVisibility(View.GONE);
            mDesView.setVisibility(View.GONE);
        }

        mSwipeRefreshLayout = findViewById(R.id.transaction_swipe_view);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        //ETH、ETH下ERC20、BTC、BTC相关Token、TRX、TRX下TRC10显示交易记录
        if (QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol()) || mToken.getType() == Constant.ACCOUNT_TYPE_ETH
                || QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol()) || TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress())) {
            mEmptyView = findViewById(R.id.tx_empty_layout);
            mViewPage = findViewById(R.id.wallet_transaction_view_page);
            mViewPage.setOffscreenPageLimit(1);
            String[] titles = getResources().getStringArray(R.array.wallet_transaction_tag);
            if (!QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol()) && !QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())) {
                TransactionPagerAdapter mAdapter = new TransactionPagerAdapter(getSupportFragmentManager(), titles, mToken);
                mViewPage.setAdapter(mAdapter);
            } else {
                TransactionPagerAdapter mAdapter = new TransactionPagerAdapter(getSupportFragmentManager(), titles);
                mViewPage.setAdapter(mAdapter);
            }
            SlidingTabLayout mTabLayout = findViewById(R.id.transaction_tab_view);
            mTabLayout.setEndHasMargin(false);
            mTabLayout.setViewPager(mViewPage, titles);
            mTabLayout.setCurrentTab(0);

            float height = getResources().getDisplayMetrics().heightPixels - getStatusBarHeight() - getResources().getDimension(R.dimen.appbar_top_height);
            height = height - UiUtils.dpToPixel(20 + 50);//mSwipeRefreshLayout内容区高度
            mMaxHeight = (int) (height - UiUtils.dpToPixel(10 + 60));//减tab栏高度

            findViewById(R.id.other_token_transaction_layout).setVisibility(View.VISIBLE);

            mTxSmartRefreshLayout = findViewById(R.id.detail_tx_swipe);
            mTxSmartRefreshLayout.setEnableRefresh(false);
            mTxSmartRefreshLayout.setEnableAutoLoadMore(true);
            mTxSmartRefreshLayout.setEnableLoadMoreWhenContentNotFull(true);
            mTxSmartRefreshLayout.setFooterHeight(30);
            mTxSmartRefreshLayout.setOnLoadMoreListener(v -> onLoadMoreRequested());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        //获取当前主钱包
        mTransactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                .get(TransactionViewModel.class);
        mTransactionViewModel.findDefaultWalletObserve().observe(this, this::findWalterSuccess);

        if (QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol())
                || QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())
                || QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol())) {
            //主币
            mTransactionViewModel.dataObserve().observe(this, this::accountSuccess);
            mTransactionViewModel.dataError().observe(this, v -> onError());
        } else if ((mToken.getType() == Constant.ACCOUNT_TYPE_TRX && TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress()))
                || mToken.getType() == Constant.ACCOUNT_TYPE_ETH ) {
            //主币下Token
            mTransactionViewModel.tokenAccountAndTrans().observe(this, this::tokenAccountSuccess);
            mTransactionViewModel.dataError().observe(this, v -> onError());
        } else {
            //QKC QRC20 Token
            mTransactionViewModel.tokenBeanObserve().observe(this, this::onTokenBeanSuccess);
            mTransactionViewModel.tokenBeanFailObserve().observe(this, v -> onTokenBeanFail());
        }

        //拉取价格
        mTransactionViewModel.coinPrice().observe(this, v -> coinPriceSuccess());
        //上拉加载
        mTransactionViewModel.transactionObserve().observe(this, v -> loadMoreFinish());
        mTransactionViewModel.loadMoreFailObserve().observe(this, v -> loadMoreFinish());
        //获取当前主钱包
        mTransactionViewModel.findWallet();

        RxBus.get().register(this);
    }


    private void onReceive() {
        if (mQuarkWallet == null) {
            return;
        }
        if (QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol())) {
            //eth
            WalletBitmapAddressActivity.startActivity(this, mQuarkWallet);
            UmengStatistics.qrReceiveTokenClick(getApplicationContext(), "eth");
        } else if (QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())) {
            //trx
            WalletBitmapAddressActivity.startActivity(this, mQuarkWallet);
            UmengStatistics.qrReceiveTokenClick(getApplicationContext(), "trx");
        } else if (QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol())) {
            //trx
            WalletBitmapAddressActivity.startActivity(this, mQuarkWallet);
            UmengStatistics.qrReceiveTokenClick(getApplicationContext(), "btc");
        } else {
            //token
            WalletBitmapAddressActivity.startTokenActivity(this, mQuarkWallet, mToken.getAddress(), mToken.getChainId());
            UmengStatistics.qrReceiveTokenClick(getApplicationContext(), mToken.getSymbol());
        }
        UmengStatistics.walletDetailReceiveTokenClickCount(getApplicationContext(), mToken.getSymbol(), mQuarkWallet.getCurrentAddress());
    }

    private void onSend() {
        if (mQuarkWallet == null) {
            return;
        }
        Intent intent = new Intent(this, TransactionCreateActivity.class);
        intent.putExtra(Constant.CURRENT_ACCOUNT_ADDRESS, mQuarkWallet.getCurrentAddress());
        if (!QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol()) && !QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol()) && !QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())) {
            intent.putExtra(Constant.KEY_TOKEN, mToken);
        }
        startActivityForResult(intent, 0);
        UmengStatistics.walletDetailSendTokenClickCount(getApplicationContext(), mToken.getSymbol(), mQuarkWallet.getCurrentAddress());
    }

    private void freezeTrx() {
        Intent intent = new Intent(this, TRXFreezeActivity.class);
        startActivityForResult(intent, Constant.REQUEST_CODE_FREEZE_TRX);
    }

    private void onRefresh() {
        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            mSwipeRefreshLayout.setRefreshing(false);
            MyToast.showSingleToastShort(getApplicationContext(), R.string.network_error);
            return;
        }

        if (QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol())
                || QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol())
                || QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())) {
            mTransactionViewModel.getFirstBalanceAndTrans(mQuarkWallet);
        } else if ((mToken.getType() == Constant.ACCOUNT_TYPE_TRX && TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress()))) {
            //trc10交易记录
            mTransactionViewModel.getTokenFirstBalanceAndTrans(mQuarkWallet.getCurrentAccount(), mToken, 0);
        } else if (mToken.getType() == Constant.ACCOUNT_TYPE_ETH) {
            //ERC20交易记录
            mTransactionViewModel.getTokenFirstBalanceAndTrans(mQuarkWallet.getCurrentAccount(), mToken, 1);
        } else {
            //获取balance余额
            mTransactionViewModel.findTokenBalance(mQuarkWallet.getCurrentAccount(), mToken);
        }
        //刷新价格
        mTransactionViewModel.coinPrice(mToken.getSymbol());
    }

    private void onLoadMoreRequested() {
        if (mQuarkWallet.getCurrentAccount().isQKC()) {
            if (mToken != null && mToken.isNative()) {
                mTransactionViewModel.getTokenTransactionsByNext(mQuarkWallet.getCurrentAccount(), mToken, Constant.sTransactionNext);
            } else {
                mTransactionViewModel.getTransactionsByNext(mQuarkWallet.getCurrentAccount(), Constant.sTransactionNext);
            }
        } else if (mQuarkWallet.getCurrentAccount().isEth()) {
            if (mToken == null || QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol())) {
                //eth
                mTransactionViewModel.getTransactionsByNext(mQuarkWallet.getCurrentAccount(), Constant.sTransactionNext);
            } else {
                //erc20
                mTransactionViewModel.getTokenTransactionsByNext(mQuarkWallet.getCurrentAccount(), mToken, Constant.sTransactionNext);
            }
        } else if (mQuarkWallet.getCurrentAccount().isTRX()) {
            if (mToken != null && !QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol()) && TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress())) {
                //trc10
                mTransactionViewModel.getTokenTransactionsByNext(mQuarkWallet.getCurrentAccount(), mToken, Constant.sTransactionNext);
            } else {
                mTransactionViewModel.getTransactionsByNext(mQuarkWallet.getCurrentAccount(), Constant.sTransactionNext);
            }
        } else if (mQuarkWallet.getCurrentAccount().isAllBTC()) {
            if (mToken == null || QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol())) {
                //eth
                mTransactionViewModel.getTransactionsByNext(mQuarkWallet.getCurrentAccount(), Constant.sTransactionNext);
            } else {
                //erc20
                mTransactionViewModel.getTokenTransactionsByNext(mQuarkWallet.getCurrentAccount(), mToken, Constant.sTransactionNext);
            }
        }
    }

    private void onTokenBeanSuccess(TokenBean bean) {
        mToken = bean.getToken();
        QWBalance mTokenBalance = bean.getBalance();
        String currentPriceType = SharedPreferencesUtils.getCurrentMarketCoin(getApplicationContext());
        String coinSymbol = ToolUtils.getCoinSymbol(currentPriceType);
        if (mTokenBalance != null) {
            String tokenCount = QWWalletUtils.getIntTokenFromWei16(mTokenBalance.getBalance(), mToken.getTokenUnit());
            mBalanceText.setText(tokenCount);

            String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), mToken.getSymbol(), tokenCount);
            mPriceText.setText(priceStr);
        } else {
            mBalanceText.setText("0");

            String priceStr = Constant.PRICE_ABOUT + coinSymbol + "0";
            mPriceText.setText(priceStr);
        }
        mSwipeRefreshLayout.setRefreshing(false);

        //发送事件
        sendEventToken();
    }

    private void onTokenBeanFail() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void coinPriceSuccess() {
        CharSequence countStr = mBalanceText.getText();
        if (!TextUtils.isEmpty(countStr)) {
            String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), mToken.getSymbol(), countStr.toString());
            mPriceText.setText(priceStr);
        }
    }

    private void loadMoreFinish() {
        mTxSmartRefreshLayout.finishLoadMore();
    }

    private void findWalterSuccess(QWWallet wallet) {
        mQuarkWallet = wallet;

        if (QWTokenDao.BTC_SYMBOL.equals(mToken.getSymbol()) || QWTokenDao.ETH_SYMBOL.equals(mToken.getSymbol()) || QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol())) {
            //重置分页下标
            Constant.sTransactionNext = SharedPreferencesUtils.getCurrentTransactionNext(getApplicationContext(), mQuarkWallet.getCurrentAddress());
            //更新UI
            updateUi();
            //更新Transaction
            mTransactionViewModel.updateTransactionObserve(new ArrayList<>(wallet.getCurrentAccount().getTransactions()));

            if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
                mSwipeRefreshLayout.setRefreshing(true);
                mTransactionViewModel.getFirstBalanceAndTrans(mQuarkWallet);
                //刷新价格
                mTransactionViewModel.coinPrice(mToken.getSymbol());
            }
        } else if ((mToken.getType() == Constant.ACCOUNT_TYPE_TRX && TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress()))
                || mToken.getType() == Constant.ACCOUNT_TYPE_ETH) {
            //ERC20 | trc10交易记录
            //重置分页下标
            Constant.sTransactionNext = SharedPreferencesUtils.getCurrentTransactionNext(getApplicationContext(), mQuarkWallet.getCurrentAddress() + mToken.getAddress());
            //更新Transaction UI
            QWTokenTransactionDao dao = new QWTokenTransactionDao(getApplicationContext());
            List<QWTokenTransaction> list = dao.queryByToken(getApplicationContext(), wallet.getCurrentAccount(), mToken.getAddress());
            List<QWTransaction> transactionList = parseTokenTransaction(list);
            updateTransactionUi(transactionList);
            mTransactionViewModel.updateTransactionObserve(transactionList);

            //trc10 token首次进入没有记录时，触发拉取
            if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
                mSwipeRefreshLayout.setRefreshing(true);
                if ((mToken.getType() == Constant.ACCOUNT_TYPE_TRX)) {
                    //trc10交易记录
                    mTransactionViewModel.getTokenFirstBalanceAndTrans(mQuarkWallet.getCurrentAccount(), mToken, 0);
                } else if (mToken.getType() == Constant.ACCOUNT_TYPE_ETH) {
                    //ERC20交易记录
                    mTransactionViewModel.getTokenFirstBalanceAndTrans(mQuarkWallet.getCurrentAccount(), mToken, 1);
                }
                //刷新价格
                mTransactionViewModel.coinPrice(mToken.getSymbol());
            }
        }
    }

    private void updateUi() {
        updateBalanceUi();
        updateTransactionUi();
    }

    //更新分片UI
    private void updateBalanceUi() {
        ForeignCollection<QWBalance> collection = mQuarkWallet.getCurrentAccount().getBalances();
        if (collection != null && !collection.isEmpty()) {
            for (QWBalance balance : collection) {
                if (mQuarkWallet.getCurrentAccount().isEth() && balance.getQWToken() != null && QWTokenDao.ETH_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                    String mainToken = QWWalletUtils.getIntTokenFromWei16(balance.getBalance());
                    mBalanceText.setText(mainToken);

                    String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), QWTokenDao.ETH_SYMBOL, mainToken);
                    mPriceText.setText(priceStr);
                } else if (mQuarkWallet.getCurrentAccount().isTRX() && balance.getQWToken() != null && QWTokenDao.TRX_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                    String mainToken = QWWalletUtils.getIntTokenFromSun16(balance.getBalance());
                    mBalanceText.setText(mainToken);

                    String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), QWTokenDao.TRX_SYMBOL, mainToken);
                    mPriceText.setText(priceStr);
                } else if (mQuarkWallet.getCurrentAccount().isAllBTC() && balance.getQWToken() != null && QWTokenDao.BTC_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                    String mainToken = QWWalletUtils.getIntTokenFromCong16(balance.getBalance());
                    mBalanceText.setText(mainToken);

                    String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), QWTokenDao.BTC_SYMBOL, mainToken);
                    mPriceText.setText(priceStr);
                } else if (mToken != null && balance.getQWToken() != null && TextUtils.equals(mToken.getSymbol(), balance.getQWToken().getSymbol())) {
                    String mainToken = QWWalletUtils.getIntTokenFromWei16(balance.getBalance(), mToken.getTokenUnit());
                    mBalanceText.setText(mainToken);

                    String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), mQuarkWallet.getCurrentAddress(), mToken.getSymbol(), mainToken);
                    mPriceText.setText(priceStr);
                }
            }
        }
    }

    //更新事务UI
    private void updateTransactionUi() {
        if (mQuarkWallet == null) {
            return;
        }
        ForeignCollection<QWTransaction> transactions = mQuarkWallet.getCurrentAccount().getTransactions();
        updateTransactionUi(new ArrayList<>(transactions));
    }

    private void updateTransactionUi(List<QWTransaction> list) {
        if (mQuarkWallet == null) {
            return;
        }

        int size = 0;
        if (list != null && !list.isEmpty()) {
            for (QWTransaction transaction : list) {
                QWToken token = transaction.getToken();
                if (TextUtils.equals(mToken.getSymbol(), token.getSymbol())) {
                    size++;
                }
            }
        }
        if (size < 1) {
            mViewPage.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            int height = Math.min((int) (UiUtils.dpToPixel(60) * size), mMaxHeight);
            ViewGroup.LayoutParams layoutParams = mViewPage.getLayoutParams();
            layoutParams.height = height;
            mViewPage.setLayoutParams(layoutParams);
            mViewPage.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private void accountSuccess(QWAccount account) {
        mQuarkWallet.setCurrentAccount(account);

        updateUi();
        mSwipeRefreshLayout.setRefreshing(false);
        mTransactionViewModel.updateTransactionObserve(new ArrayList<>(mQuarkWallet.getCurrentAccount().getTransactions()));

        sendEventWallet();
    }

    private void tokenAccountSuccess(List<QWTransaction> list) {
        //更新余额
        QWAccountDao dao = new QWAccountDao(getApplication());
        QWAccount account = dao.queryByAddress(mQuarkWallet.getCurrentAddress());
        mQuarkWallet.setCurrentAccount(account);

        //  更新Transaction
        updateTransactionUi(list);
        mTransactionViewModel.updateTransactionObserve(list);

        //  更新balance
        updateBalanceUi();

        mSwipeRefreshLayout.setRefreshing(false);
        sendEventWallet();
    }

    private void onError() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.REQUEST_CODE_FREEZE_TRX && resultCode == Activity.RESULT_OK) {
            mSwipeRefreshLayout.setRefreshing(true);
            mTransactionViewModel.getFirstBalanceAndTrans(mQuarkWallet);
            return;
        }

        //发送事件
        sendEventToken();
    }

    private void sendEventToken() {
        RxBus.get().send(Constant.RX_BUS_CODE_TOKEN, "");
    }

    private void sendEventWallet() {
        ChangeWalletEvent message = new ChangeWalletEvent(mQuarkWallet.getCurrentAddress());
        EventBus.getDefault().post(message);
    }

    @Subscribe(code = Constant.RX_BUS_CODE_TRANS_COST, threadMode = ThreadMode.MAIN)
    public void rxBusEventToken(String[] value) {
        if (value != null) {
            TransactionLoadBean bean = mTransactionViewModel.transactionObserve().getValue();
            if (bean != null) {
                for (QWTransaction transaction : bean.getList()) {
                    if (transaction.getTxId().equals(value[0])) {
                        transaction.setCost(value[1]);
                    }
                }
                //更新Transaction
                mTransactionViewModel.updateTransactionObserve(bean);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.get().unRegister(this);
    }


    private List<QWTransaction> parseTokenTransaction(List<QWTokenTransaction> list) {
        List<QWTransaction> transactionList = new ArrayList<>();
        for (QWTokenTransaction transaction : list) {
            transactionList.add(transaction.parseTransactionList());
        }
        return transactionList;
    }
}
