package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenTransactionDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.utils.Numeric;
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
import com.quarkonium.qpocket.util.ApiHelper;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.SlidingTabLayout;
import com.quarkonium.qpocket.view.WheelPopWindow;
import com.quarkonium.qpocket.view.recycler.MyAnimator;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.yqritc.recyclerviewflexibledivider.VerticalDividerItemDecoration;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * 交易详情列表界面
 */
public class TransactionDetailActivity extends BaseActivity implements OnItemClickListener {

    private static class ShardBalanceAdapter extends BaseQuickAdapter<QWBalance, BaseViewHolder> {

        private BigInteger mainShardId;
        private BigInteger mainChainId;
        private QWToken mToken;

        private ShardBalanceAdapter(QWToken token, int layoutId, List<QWBalance> datas) {
            super(layoutId, datas);
            mToken = token;
        }

        @Override
        public void convert(BaseViewHolder holder, QWBalance balance) {
            Context context = holder.itemView.getContext().getApplicationContext();

            TextView tokenTitle = holder.getView(R.id.balance_token_title);
            TextView tokenBalance = holder.getView(R.id.balance_token);
            TextView tokenPrice = holder.getView(R.id.balance_token_price);
            ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            if (getItemCount() > 1) {
                float width = context.getResources().getDisplayMetrics().widthPixels - UiUtils.dpToPixel(80);
                layoutParams.width = (int) (width / 2.3f);

                tokenTitle.setGravity(Gravity.LEFT);
                tokenTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.dp_13));
                tokenBalance.setGravity(Gravity.LEFT);
                tokenBalance.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.dp_15));
                tokenPrice.setGravity(Gravity.LEFT);
                tokenPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.dp_13));
            } else {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                tokenTitle.setGravity(Gravity.RIGHT);
                tokenTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.dp_15));
                tokenBalance.setGravity(Gravity.RIGHT);
                tokenBalance.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.dp_20));
                tokenPrice.setGravity(Gravity.RIGHT);
                tokenPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.dp_17));
            }
            holder.itemView.setLayoutParams(layoutParams);

            BigInteger shardId = Numeric.toBigInt(balance.getQWShard().getShard());
            BigInteger chainId = Numeric.toBigInt(balance.getChain().getChain());

            TextView shardText = holder.getView(R.id.shard_id);
            String shard = String.format(context.getResources().getString(R.string.wallet_shard_id), shardId.toString());
            shardText.setText(shard);

            TextView chainText = holder.getView(R.id.chain_id);
            String chain = String.format(context.getResources().getString(R.string.wallet_chain_id), chainId.toString());
            chainText.setText(chain);

            String token = QWWalletUtils.getIntTokenFromWei16(balance.getBalance());
            tokenBalance.setText(token);

            String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(context, mToken == null ? QWTokenDao.QKC_SYMBOL : mToken.getSymbol(), token);
            tokenPrice.setText(priceStr);

            TextView balanceTitle = holder.getView(R.id.balance_token_title);

            ImageView flag = holder.getView(R.id.switch_chain);
            if (chainId.equals(mainChainId) && shardId.equals(mainShardId)) {
                if (ToolUtils.isZh(context)) {
                    flag.setImageResource(R.drawable.chain_current_item_cn);
                } else {
                    flag.setImageResource(R.drawable.chain_current_item_en);
                }
                flag.setVisibility(View.VISIBLE);
                if (getItemCount() > 1) {
                    holder.getView(R.id.bottom_wallet_token_layout).setBackgroundResource(R.drawable.item_balance_cs_small_select_bg);
                } else {
                    holder.getView(R.id.bottom_wallet_token_layout).setBackgroundResource(R.drawable.item_balance_cs_select_bg);
                }

                shardText.setTextColor(Color.WHITE);
                chainText.setTextColor(Color.WHITE);

                balanceTitle.setTextColor(Color.WHITE);

                tokenBalance.setTextColor(Color.WHITE);
                tokenPrice.setTextColor(Color.WHITE);
            } else {
                flag.setVisibility(View.GONE);
                if (getItemCount() > 1) {
                    holder.getView(R.id.bottom_wallet_token_layout).setBackgroundResource(R.drawable.item_balance_cs_small_bg);
                } else {
                    holder.getView(R.id.bottom_wallet_token_layout).setBackgroundResource(R.drawable.item_balance_cs_bg);
                }

                shardText.setTextColor(context.getResources().getColor(R.color.text_title));
                chainText.setTextColor(context.getResources().getColor(R.color.text_title));

                balanceTitle.setTextColor(context.getResources().getColor(R.color.text_message));

                tokenBalance.setTextColor(context.getResources().getColor(R.color.text_title));
                tokenPrice.setTextColor(context.getResources().getColor(R.color.text_title));
            }
        }

        private void setData(BigInteger mainShard, BigInteger mainChain, List<QWBalance> balances) {
            List<QWBalance> list = getData();
            list.clear();
            list.addAll(balances);
            mainShardId = mainShard;
            mainChainId = mainChain;
            notifyDataSetChanged();
        }

        private void setCurrentChainAndShard(BigInteger chain, BigInteger shard) {
            int oldPosition = -1;
            int newPosition = -1;
            int size = getItemCount();
            for (int i = 0; i < size; i++) {
                QWBalance balance = getItem(i);
                if (balance != null) {
                    BigInteger shardId = Numeric.toBigInt(balance.getQWShard().getShard());
                    BigInteger chainId = Numeric.toBigInt(balance.getChain().getChain());
                    if (mainChainId != null && mainShardId != null
                            && mainChainId.equals(chainId) && mainShardId.equals(shardId)) {
                        oldPosition = i;
                    }

                    if (chainId.equals(chain) && shardId.equals(shard)) {
                        newPosition = i;
                    }
                }
            }

            mainChainId = chain;
            mainShardId = shard;
            if (oldPosition != -1) {
                notifyItemChanged(oldPosition);
            }
            if (newPosition != -1) {
                notifyItemChanged(newPosition);
            }
        }

        boolean isEquals(BigInteger chain, BigInteger shard) {
            return mainChainId != null && mainShardId != null
                    && mainChainId.equals(chain) && mainShardId.equals(shard);
        }
    }

    public static void startTokenActivity(Activity activity, QWWallet wallet, TokenBean bean) {
        Intent intent = new Intent(activity, TransactionDetailActivity.class);
        intent.putExtra(Constant.KEY_WALLET, wallet);
        intent.putExtra(Constant.KEY_TOKEN, bean.getToken());
        activity.startActivity(intent);
    }

    private static final int DURATION_ADD = 150;
    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    private View mMergeView;
    private TextView mTotalQKCTokenView;
    private TextView mTotalPriceView;
    private ShardBalanceAdapter mBalanceAdapter;
    private RecyclerView mBalanceRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SmartRefreshLayout mTxSmartRefreshLayout;
    private ViewPager mViewPage;
    private View mEmptyView;
    private int mMaxHeight;

    private QWWallet mDefaultWallet;
    private QWToken mToken;

    private boolean mIsFirst;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_transaction_detail;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_transaction_detail_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mDefaultWallet = getIntent().getParcelableExtra(Constant.KEY_WALLET);
        mToken = getIntent().getParcelableExtra(Constant.KEY_TOKEN);

        mTopBarView.setTitle(R.string.wallet_transaction_detail_title);

        mMergeView = findViewById(R.id.transaction_token_merge);
        mMergeView.setOnClickListener(v -> onMerge());
        findViewById(R.id.transaction_switch_shard).setOnClickListener(v -> onSwitchShard());
        findViewById(R.id.transaction_receive).setOnClickListener(v -> onReceive());
        findViewById(R.id.transaction_send).setOnClickListener(v -> onSend());

        mTotalQKCTokenView = findViewById(R.id.transaction_total_token);
        mTotalQKCTokenView.setText("0");
        mTotalPriceView = findViewById(R.id.transaction_total_token_price);
        String currentPriceType = SharedPreferencesUtils.getCurrentMarketCoin(getApplicationContext());
        String coinSymbol = ToolUtils.getCoinSymbol(currentPriceType);
        String priceStr = Constant.PRICE_ABOUT + coinSymbol + "0";
        mTotalPriceView.setText(priceStr);

        mSwipeRefreshLayout = findViewById(R.id.transaction_swipe_view);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        mBalanceAdapter = new ShardBalanceAdapter(mToken, R.layout.holder_recycler_blance_item, new ArrayList<>());
        mBalanceAdapter.setOnItemClickListener(this);
        mBalanceRecyclerView = findViewById(R.id.transaction_shard_recycler_view);
        mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mBalanceRecyclerView.setLayoutManager(mLinearLayoutManager);
        mBalanceRecyclerView.setAdapter(mBalanceAdapter);
        VerticalDividerItemDecoration itemDecoration = new VerticalDividerItemDecoration.Builder(this)
                .color(Color.TRANSPARENT)
                .size((int) UiUtils.dpToPixel(10))
                .build();
        mBalanceRecyclerView.addItemDecoration(itemDecoration);
        //自定义defaultItemAnimator是因为部分手机使用默认的要崩溃的，而代码提出来后只是小概率崩溃了，所以还是要屏蔽动画
        if (ApiHelper.AFTER_JELLY_BEAN_MR1) {
            mBalanceRecyclerView.setItemAnimator(new MyAnimator());
            mBalanceRecyclerView.getItemAnimator().setAddDuration(DURATION_ADD);
            mBalanceRecyclerView.getItemAnimator().setRemoveDuration(DURATION_ADD);
            mBalanceRecyclerView.getItemAnimator().setMoveDuration(DURATION_ADD);
            mBalanceRecyclerView.getItemAnimator().setChangeDuration(DURATION_ADD);
        }

        mTxSmartRefreshLayout = findViewById(R.id.detail_tx_swipe);
        mTxSmartRefreshLayout.setEnableRefresh(false);
        mTxSmartRefreshLayout.setEnableAutoLoadMore(true);
        mTxSmartRefreshLayout.setEnableLoadMoreWhenContentNotFull(true);
        mTxSmartRefreshLayout.setFooterHeight(30);
        mTxSmartRefreshLayout.setOnLoadMoreListener(v -> onLoadMoreRequested());

        mEmptyView = findViewById(R.id.tx_empty_layout);
        mViewPage = findViewById(R.id.wallet_transaction_view_page);
        mViewPage.setOffscreenPageLimit(1);
        String[] titles = getResources().getStringArray(R.array.wallet_transaction_tag);
        TransactionPagerAdapter mAdapter = new TransactionPagerAdapter(getSupportFragmentManager(), titles, mToken);
        mViewPage.setAdapter(mAdapter);
        SlidingTabLayout mTabLayout = findViewById(R.id.transaction_tab_view);
        mTabLayout.setEndHasMargin(false);
        mTabLayout.setViewPager(mViewPage, titles);
        mTabLayout.setCurrentTab(0);
        float height = getResources().getDisplayMetrics().heightPixels - getStatusBarHeight() - getResources().getDimension(R.dimen.appbar_top_height);
        height = height - UiUtils.dpToPixel(20 + 50);//mSwipeRefreshLayout内容区高度
        mMaxHeight = (int) (height - UiUtils.dpToPixel(10 + 60));//减tab栏高度

        TextView textView = findViewById(R.id.transaction_total_token_title);
        textView.setText(mToken == null ? QWTokenDao.QKC_NAME : mToken.getSymbol().toUpperCase());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        mTransactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                .get(TransactionViewModel.class);
        super.onCreate(savedInstanceState);

        mIsFirst = true;
        mTransactionViewModel.findDefaultWalletObserve().observe(this, this::findWalterSuccess);
        mTransactionViewModel.tokenAccountAndTrans().observe(this, this::tokenAccountSuccess);
        mTransactionViewModel.dataObserve().observe(this, this::accountSuccess);
        mTransactionViewModel.dataError().observe(this, v -> onError());

        mTransactionViewModel.coinPrice().observe(this, v -> onCoinPriceSuccess());

        mTransactionViewModel.firstRefreshTransaction().observe(this, this::onFirstRefreshTransaction);

        //上拉加载
        mTransactionViewModel.transactionObserve().observe(this, v -> loadMoreFinish());
        mTransactionViewModel.loadMoreFailObserve().observe(this, v -> loadMoreFinish());
        //监听
        RxBus.get().register(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mIsFirst) {
            //获取当前主钱包
            mTransactionViewModel.findWallet();
        }
        mIsFirst = false;
    }

    private void onError() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void findWalterSuccess(QWWallet wallet) {
        if (wallet == null) {
            finish();
            return;
        }
        mDefaultWallet = wallet;

        //更新分片UI
        updateBalanceUi();
        //定位分片位置
        String mainChain = SharedPreferencesUtils.getCurrentChain(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        BigInteger mainChainId = Numeric.toBigInt(mainChain);
        //获取主分片
        String mainShard = SharedPreferencesUtils.getCurrentShard(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        BigInteger mainShardId = Numeric.toBigInt(mainShard);
        scrollPosition(mainChainId, mainShardId);

        //更新交易记录UI
        if (mToken != null) {
            //Native Token
            //重置分页下标
            String name = mDefaultWallet.getCurrentShareAddress() + mToken.getAddress();
            Constant.sTransactionNext = SharedPreferencesUtils.getCurrentTransactionNext(getApplicationContext(), name);

            //更新Transaction UI
            QWTokenTransactionDao dao = new QWTokenTransactionDao(getApplicationContext());
            List<QWTokenTransaction> list = dao.queryByToken(getApplicationContext(), wallet.getCurrentAccount(), mToken.getAddress());
            List<QWTransaction> transactionList = parseTokenTransaction(list);
            mTransactionViewModel.updateTransactionObserve(new ArrayList<>(transactionList));
            //更新UI
            updateTransactionUi(transactionList);

            //获取新数据
            mSwipeRefreshLayout.setRefreshing(true);
            mTransactionViewModel.getFirstQKCNativeToken(mDefaultWallet, mToken);

            //拉取价格
            mTransactionViewModel.coinPrice(mToken.getSymbol());
        } else {
            //QKC
            //重置分页下标
            Constant.sTransactionNext = SharedPreferencesUtils.getCurrentTransactionNext(getApplicationContext(), mDefaultWallet.getCurrentShareAddress());

            //更新Transaction
            ArrayList<QWTransaction> list = new ArrayList<>(wallet.getCurrentAccount().getTransactions());
            mTransactionViewModel.updateTransactionObserve(list);
            //更新UI
            updateTransactionUi(list);

            //获取新数据
            mSwipeRefreshLayout.setRefreshing(true);
            mTransactionViewModel.getFirstBalanceAndTrans(mDefaultWallet);

            //拉取价格
            mTransactionViewModel.coinPrice(QWTokenDao.QKC_SYMBOL);
        }
    }

    private List<QWTransaction> parseTokenTransaction(List<QWTokenTransaction> list) {
        List<QWTransaction> transactionList = new ArrayList<>();
        for (QWTokenTransaction transaction : list) {
            transactionList.add(transaction.parseTransactionList());
        }
        return transactionList;
    }

    //下拉刷新QKC成功
    private void accountSuccess(QWAccount account) {
        mSwipeRefreshLayout.setRefreshing(false);
        mDefaultWallet.setCurrentAccount(account);

        //更新UI
        List<QWTransaction> list = new ArrayList<>(mDefaultWallet.getCurrentAccount().getTransactions());
        updateUi(list);
        mTransactionViewModel.updateTransactionObserve(list);

        sendEventWallet();
    }

    //下拉刷新Token成功
    private void tokenAccountSuccess(List<QWTransaction> list) {
        mSwipeRefreshLayout.setRefreshing(false);
        //更新余额
        QWAccountDao dao = new QWAccountDao(getApplication());
        QWAccount account = dao.queryByAddress(mDefaultWallet.getCurrentAddress());
        mDefaultWallet.setCurrentAccount(account);

        //  更新UI
        updateUi(list);
        mTransactionViewModel.updateTransactionObserve(list);

        sendEventWallet();
    }

    //切分片后获取交易记录成功
    private void onFirstRefreshTransaction(QWAccount account) {
        mSwipeRefreshLayout.setRefreshing(false);

        List<QWTransaction> list;
        if (account != null) {
            mDefaultWallet.setCurrentAccount(account);
            list = new ArrayList<>(account.getTransactions());
        } else {
            mDefaultWallet.getCurrentAccount().getTransactions().clear();
            list = new ArrayList<>();
        }

        //刷新ViewPage高度
        updateTransactionUi(list);
        mTransactionViewModel.updateTransactionObserve(list);

        //更新QKC交易记录
        if (mToken == null) {
            sendEventWallet();
        }
    }

    private void onCoinPriceSuccess() {
        CharSequence totalToken = mTotalQKCTokenView.getText();
        if (!TextUtils.isEmpty(totalToken)) {
            String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), mToken != null ? mToken.getSymbol() : QWTokenDao.QKC_SYMBOL, totalToken.toString());
            mTotalPriceView.setText(priceStr);
        }
        mBalanceAdapter.notifyDataSetChanged();
    }

    private void loadMoreFinish() {
        mTxSmartRefreshLayout.postDelayed(() -> mTxSmartRefreshLayout.finishLoadMore(), 100);
    }

    private void updateUi(List<QWTransaction> list) {
        updateBalanceUi();

        updateTransactionUi(list);
    }

    //更新分片UI
    private void updateBalanceUi() {
        //获取主链
        String mainChain = SharedPreferencesUtils.getCurrentChain(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        BigInteger mainChainId = Numeric.toBigInt(mainChain);
        //获取主分片
        String mainShard = SharedPreferencesUtils.getCurrentShard(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        BigInteger mainShardId = Numeric.toBigInt(mainShard);

        ForeignCollection<QWBalance> collection = mDefaultWallet.getCurrentAccount().getBalances();
        //分片
        QWBalance mainBalance = null;
        ArrayList<QWBalance> list = new ArrayList<>();
        String symbol = mToken == null ? QWTokenDao.QKC_SYMBOL : mToken.getSymbol();
        //qkc 列表
        if (collection != null && !collection.isEmpty()) {
            BigInteger total = BigInteger.ZERO;
            for (QWBalance balance : collection) {
                if (balance.getQWToken() != null && TextUtils.equals(symbol, balance.getQWToken().getSymbol())) {
                    String value = QWWalletUtils.getIntTokenFromWei16(balance.getBalance(), balance.getQWToken().getTokenUnit());
                    if ("0".equals(value)) {
                        continue;
                    }
                    total = total.add(Numeric.toBigInt(balance.getBalance()));

                    QWChain chainT = balance.getChain();
                    QWShard shard = balance.getQWShard();
                    BigInteger id = Numeric.toBigInt(shard.getShard());
                    BigInteger chainTId = Numeric.toBigInt(chainT.getChain());
                    if (id.equals(mainShardId) && chainTId.equals(mainChainId)) {
                        mainBalance = balance;
                    }
                    list.add(balance);
                }
            }

            if (mToken == null) {
                String totalToken = QWWalletUtils.getIntTokenFromWei10(total.toString());
                mTotalQKCTokenView.setText(totalToken);

                String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.QKC_SYMBOL, totalToken);
                mTotalPriceView.setText(priceStr);
            } else {
                String totalToken = QWWalletUtils.getIntTokenFromWei10(total.toString(), mToken.getTokenUnit());
                mTotalQKCTokenView.setText(totalToken);

                String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), mToken.getSymbol(), totalToken);
                mTotalPriceView.setText(priceStr);
            }
        }
        if (mainBalance == null) {
            mainBalance = new QWBalance();
            mainBalance.setBalance("0x0");
            QWChain chain = new QWChain();
            chain.setChain(mainChain);
            mainBalance.setChain(chain);
            QWShard shard = new QWShard();
            shard.setShard(mainShard);
            mainBalance.setQWShard(shard);

            list.add(mainBalance);
        }
        //排序
        Collections.sort(list, (QWBalance p1, QWBalance p2) -> {
            /*
             * int compare(Person p1, Person p2) 返回一个基本类型的整型，
             * 返回负数表示：p1 小于p2，
             * 返回0 表示：p1和p2相等，
             * 返回正数表示：p1大于p2
             */
            int value = Numeric.toBigInt(p1.getChain().getChain()).compareTo(Numeric.toBigInt(p2.getChain().getChain()));
            if (value == 0) {
                return Numeric.toBigInt(p1.getQWShard().getShard()).compareTo(Numeric.toBigInt(p2.getQWShard().getShard()));
            }
            return value;
        });

        mBalanceAdapter.setData(mainShardId, mainChainId, list);

        if (list.size() > 1) {
            mMergeView.setVisibility(View.VISIBLE);
        } else {
            mMergeView.setVisibility(View.GONE);
        }
    }

    //更新事务UI
    private void updateTransactionUi(List<QWTransaction> list) {
        //当前版本token交易记录屏蔽
        if (mDefaultWallet == null) {
            return;
        }
        //事务
        //获取主链
        String mainChain = SharedPreferencesUtils.getCurrentChain(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        BigInteger mainChainId = Numeric.toBigInt(mainChain);
        //获取主分片
        String mainShard = SharedPreferencesUtils.getCurrentShard(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        BigInteger mainShardId = Numeric.toBigInt(mainShard);
        int size = 0;
        if (list != null && !list.isEmpty()) {
            for (QWTransaction transaction : list) {
                QWToken token = transaction.getToken();
                QWChain chain = transaction.getChain();
                QWShard shard = transaction.getShard();
                BigInteger id = Numeric.toBigInt(shard.getShard());
                BigInteger chainTId = Numeric.toBigInt(chain.getChain());
                if (id.equals(mainShardId) && chainTId.equals(mainChainId)) {
                    if (mToken != null) {
                        if (TextUtils.equals(mToken.getSymbol(), token.getSymbol())) {
                            size++;
                        }
                    } else if (TextUtils.equals(QWTokenDao.QKC_SYMBOL, token.getSymbol())) {
                        size++;
                    }
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

    private void onRefresh() {
        if (!ConnectionUtil.isInternetConnection(this)) {
            mSwipeRefreshLayout.setRefreshing(false);
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        //获取token数量
        //获取交易记录
        if (mToken == null) {
            mTransactionViewModel.getFirstBalanceAndTrans(mDefaultWallet);
            //拉取价格
            mTransactionViewModel.coinPrice(QWTokenDao.QKC_SYMBOL);
        } else {
            mTransactionViewModel.getFirstQKCNativeToken(mDefaultWallet, mToken);
            //拉取价格
            mTransactionViewModel.coinPrice(mToken.getSymbol());
        }
    }

    private void onLoadMoreRequested() {
        if (mDefaultWallet.getCurrentAccount().isQKC()) {
            if (mToken != null && mToken.isNative()) {
                mTransactionViewModel.getTokenTransactionsByNext(mDefaultWallet.getCurrentAccount(), mToken, Constant.sTransactionNext);
            } else {
                mTransactionViewModel.getTransactionsByNext(mDefaultWallet.getCurrentAccount(), Constant.sTransactionNext);
            }
        } else if (mDefaultWallet.getCurrentAccount().isEth()) {
            if (mToken != null) {
                //erc20
                mTransactionViewModel.getTokenTransactionsByNext(mDefaultWallet.getCurrentAccount(), mToken, Constant.sTransactionNext);
            } else {
                //eth
                mTransactionViewModel.getTransactionsByNext(mDefaultWallet.getCurrentAccount(), Constant.sTransactionNext);
            }
        } else if (mDefaultWallet.getCurrentAccount().isTRX()) {
            if (mToken != null && !QWTokenDao.TRX_SYMBOL.equals(mToken.getSymbol()) && TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress())) {
                //trc10
                mTransactionViewModel.getTokenTransactionsByNext(mDefaultWallet.getCurrentAccount(), mToken, Constant.sTransactionNext);
            } else {
                mTransactionViewModel.getTransactionsByNext(mDefaultWallet.getCurrentAccount(), Constant.sTransactionNext);
            }
        }
    }

    private void onMerge() {
        if (mDefaultWallet == null) {
            return;
        }
        MergeActivity.startMergeActivity(this, mToken);
        UmengStatistics.mergeToken(getApplicationContext(), mToken == null ? "qkc" : mToken.getSymbol());
    }

    private void onReceive() {
        if (mDefaultWallet == null) {
            return;
        }
        if (mToken != null) {
            WalletBitmapAddressActivity.startTokenActivity(this, mDefaultWallet, mToken.getAddress(), Constant.sNetworkId.intValue());
            UmengStatistics.qrReceiveTokenClick(getApplicationContext(), mToken.getSymbol());
        } else {
            WalletBitmapAddressActivity.startActivity(this, mDefaultWallet);
            UmengStatistics.qrReceiveTokenClick(getApplicationContext(), "qkc");
        }
        UmengStatistics.walletDetailReceiveTokenClickCount(getApplicationContext(), "qkc", mDefaultWallet.getCurrentAddress());
    }

    private void onSend() {
        if (mDefaultWallet == null) {
            return;
        }
        Intent intent = new Intent(this, TransactionCreateActivity.class);
        intent.putExtra(Constant.CURRENT_ACCOUNT_ADDRESS, mDefaultWallet.getCurrentAddress());
        if (mToken != null) {
            intent.putExtra(Constant.KEY_TOKEN, mToken);
        }
        startActivityForResult(intent, Constant.REQUEST_CODE_SEND_TRANSACTIONS);
        UmengStatistics.walletDetailSendTokenClickCount(getApplicationContext(), "qkc", mDefaultWallet.getCurrentAddress());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTIONS && resultCode == Activity.RESULT_OK && data != null) {
            onRefresh();
        } else if (requestCode == Constant.REQUEST_CODE_QCK_MERGE && resultCode == Activity.RESULT_OK) {
            MyToast.showSingleToastShort(this, R.string.transaction_balance_token_switch_title);
            //发送事件
            sendEventWallet();
        }
    }

    private void sendEventWallet() {
        ChangeWalletEvent message = new ChangeWalletEvent(mDefaultWallet.getCurrentAddress());
        EventBus.getDefault().post(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.get().unRegister(this);
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
    public void onItemClick(@NotNull BaseQuickAdapter adapter, @NotNull View view, int position) {
        RecyclerView.ItemAnimator anim = mBalanceRecyclerView.getItemAnimator();
        if (anim != null && anim.isRunning()) {
            return;
        }

        QWBalance balance = mBalanceAdapter.getData().get(position);
        String chainId = balance.getChain().getChain();
        String shardId = balance.getQWShard().getShard();
        //切换chain
        BigInteger chainInteger = Numeric.toBigInt(chainId);
        SharedPreferencesUtils.setCurrentChain(getApplicationContext(), mDefaultWallet.getCurrentAddress(), Numeric.toHexStringWithPrefix(chainInteger));
        //切换分片
        BigInteger integer = Numeric.toBigInt(shardId);
        SharedPreferencesUtils.setCurrentShard(getApplicationContext(), mDefaultWallet.getCurrentAddress(), Numeric.toHexStringWithPrefix(integer));
        if (mBalanceAdapter.isEquals(chainInteger, integer)) {
            return;
        }

        //刷新balance选中数据
        mBalanceAdapter.setCurrentChainAndShard(chainInteger, integer);
        //定位
        mLinearLayoutManager.scrollToPositionWithOffset(position, (int) ((mBalanceRecyclerView.getWidth() - view.getWidth()) / 2f));

        //是否显示merge
        if (mBalanceAdapter.getItemCount() > 1) {
            if (mBalanceAdapter.getItemCount() > 2) {
                mMergeView.setVisibility(View.VISIBLE);
            } else {
                mMergeView.setVisibility(View.GONE);
                //只有两个分片时
                QWBalance b1 = mBalanceAdapter.getData().get(0);
                QWBalance b2 = mBalanceAdapter.getData().get(1);
                if (Numeric.toBigInt(b1.getChain().getChain()).equals(chainInteger)
                        && Numeric.toBigInt(b1.getQWShard().getShard()).equals(integer)) {
                    //1卡片是选中分片
                    BigInteger value = Numeric.toBigInt(b2.getBalance());
                    if (BigInteger.ZERO.compareTo(value) < 0) {
                        mMergeView.setVisibility(View.VISIBLE);
                    }
                } else if (Numeric.toBigInt(b2.getChain().getChain()).equals(chainInteger)
                        && Numeric.toBigInt(b2.getQWShard().getShard()).equals(integer)) {
                    //2是选中分片
                    BigInteger value = Numeric.toBigInt(b1.getBalance());
                    if (BigInteger.ZERO.compareTo(value) < 0) {
                        mMergeView.setVisibility(View.VISIBLE);
                    }
                }
            }
        } else {
            mMergeView.setVisibility(View.GONE);
        }


        //清空transaction数据
        mTransactionViewModel.updateTransactionObserve(new ArrayList<>());
        //获取新分片下交易记录
        if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
            mBalanceRecyclerView.post(() -> {
                mSwipeRefreshLayout.setRefreshing(true);
                if (mToken == null) {
                    mTransactionViewModel.getFirstRefreshTransaction(mDefaultWallet.getCurrentAccount());
                } else {
                    mTransactionViewModel.getFirstRefreshNativeTokenTransaction(mDefaultWallet.getCurrentAccount(), mToken);
                }
            });
        } else {
            mViewPage.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }

        //发送事件
        sendEventWallet();
    }

    //切换分片
    private void onSwitchShard() {
        if (mDefaultWallet == null) {
            return;
        }
        String currentChain = SharedPreferencesUtils.getCurrentChain(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        int chain = Numeric.toBigInt(currentChain).intValue();
        String currentShard = SharedPreferencesUtils.getCurrentShard(getApplicationContext(), mDefaultWallet.getCurrentAddress());
        int shard = Numeric.toBigInt(currentShard).intValue();

        WheelPopWindow picker = new WheelPopWindow(this);
        picker.setSelected(chain, shard);
        picker.setOnNumberPickListener((int chainId, int shardId) -> {
            if (chain == chainId && shard == shardId) {
                return;
            }

            //切换chain
            BigInteger chainInteger = new BigInteger(String.valueOf(chainId));
            SharedPreferencesUtils.setCurrentChain(getApplicationContext(), mDefaultWallet.getCurrentAddress(), Numeric.toHexStringWithPrefix(chainInteger));
            //切换分片
            BigInteger integer = new BigInteger(String.valueOf(shardId));
            SharedPreferencesUtils.setCurrentShard(getApplicationContext(), mDefaultWallet.getCurrentAddress(), Numeric.toHexStringWithPrefix(integer));

            //刷新数据
            updateSwitchShardUI();
            scrollPosition(chainInteger, integer);

            //获取新分片下交易记录
            if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
                mSwipeRefreshLayout.setRefreshing(true);
                if (mToken == null) {
                    mTransactionViewModel.getFirstRefreshTransaction(mDefaultWallet.getCurrentAccount());
                } else {
                    mTransactionViewModel.getFirstRefreshNativeTokenTransaction(mDefaultWallet.getCurrentAccount(), mToken);
                }
                mTransactionViewModel.getFirstRefreshTransaction(mDefaultWallet.getCurrentAccount());
            } else {
                mViewPage.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            }


            //发送事件
            sendEventWallet();
        });
        picker.show();
    }

    //选择分片
    private void updateSwitchShardUI() {
        //更新balance
        updateBalanceUi();

        //清空transaction数据
        mTransactionViewModel.updateTransactionObserve(new ArrayList<>());
    }

    private void scrollPosition(BigInteger chainInteger, BigInteger integer) {
        //定位
        int index = -1;
        int size = mBalanceAdapter.getItemCount();
        for (int i = 0; i < size; i++) {
            QWBalance balance = mBalanceAdapter.getItem(i);
            if (balance != null) {
                BigInteger shardTempID = Numeric.toBigInt(balance.getQWShard().getShard());
                BigInteger chainTempId = Numeric.toBigInt(balance.getChain().getChain());
                if (chainTempId.equals(chainInteger) && shardTempID.equals(integer)) {
                    index = i;
                    break;
                }
            }
        }

        if (index != -1) {
            int i = index;
            float width = getResources().getDisplayMetrics().widthPixels - UiUtils.dpToPixel(80);
            width = width / 2.3f;
            int itemWitdh = (int) width;
            mBalanceRecyclerView.post(() -> mLinearLayoutManager.scrollToPositionWithOffset(i, (int) ((mBalanceRecyclerView.getWidth() - itemWitdh) / 2f)));
        }
    }
}
