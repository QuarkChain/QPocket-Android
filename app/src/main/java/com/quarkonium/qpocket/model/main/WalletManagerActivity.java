package com.quarkonium.qpocket.model.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.SharedPreferenceRepository;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.model.lock.LockPatternActivity;
import com.quarkonium.qpocket.model.main.bean.WalletManagerBean;
import com.quarkonium.qpocket.model.main.view.SectionDecoration;
import com.quarkonium.qpocket.model.main.viewmodel.WallerManagerViewModel;
import com.quarkonium.qpocket.model.main.viewmodel.WalletManagerViewModelFactory;
import com.quarkonium.qpocket.model.wallet.BackupPhraseHintActivity;
import com.quarkonium.qpocket.model.wallet.CreateChildAccountActivity;
import com.quarkonium.qpocket.model.wallet.HomeActivity;
import com.quarkonium.qpocket.rx.ChooseWalletEvent;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class WalletManagerActivity extends BaseActivity {

    private class WalletAdapter extends BaseQuickAdapter<WalletManagerBean, BaseViewHolder>
            implements StickyRecyclerHeadersAdapter<BaseViewHolder> {

        private boolean mIsColdModel;

        WalletAdapter(int layoutResId, List<WalletManagerBean> datas, boolean isColdModel) {
            super(layoutResId, datas);
            mIsColdModel = isColdModel;
        }

        @Override
        public void convert(@NotNull BaseViewHolder holder, WalletManagerBean bean) {
            QWWallet wallet = bean.getWallet();
            QWAccount account = bean.getAccount();
            View view = holder.getView(R.id.wallet_card_layout);
            view.setBackgroundResource(getBackground(account));

            ImageView iconView = holder.getView(R.id.wallet_icon);
            Glide.with(WalletManagerActivity.this)
                    .asBitmap()
                    .load(account.getIcon())
                    .into(iconView);

            TextView name = holder.getView(R.id.wallet_name);
            name.setText(account.getName());
            //HD标识
            if (wallet.getType() == Constant.WALLET_TYPE_HD) {
                Drawable dra = getResources().getDrawable(R.drawable.wallet_hd);
                dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
                name.setCompoundDrawables(null, null, dra, null);
            } else {
                name.setCompoundDrawables(null, null, null, null);
            }

            //该account的所有token的价值
            TextView balance = holder.getView(R.id.wallet_balance);
            if (mIsColdModel) {
                balance.setVisibility(View.GONE);
            } else {
                //获取当前选定法币
                String currentPriceCoin = SharedPreferencesUtils.getCurrentMarketCoin(getApplication());
                //获取法币简称符号
                String coinSymbol = ToolUtils.getCoinSymbol(currentPriceCoin);
                String price = Constant.PRICE_ABOUT + coinSymbol + ToolUtils.format8Number((float) account.getTotalPrice());
                balance.setText(price);
                balance.setTextColor(getResources().getColor(R.color.text_title));
            }

            //是否选中
            View usedView = holder.getView(R.id.wallet_icon_used);
            String key = mRepository.getCurrentWalletKey();
            if (TextUtils.equals(key, wallet.getKey()) && TextUtils.equals(wallet.getCurrentAddress(), account.getAddress())) {
                usedView.setVisibility(View.VISIBLE);
            } else {
                usedView.setVisibility(View.GONE);
            }

            //编辑
            View edit = holder.getView(R.id.wallet_edit);
            edit.setTag(bean);
            edit.setOnClickListener(this::onEdit);

            //地址
            String address = account.getShardAddress();
            holder.setText(R.id.wallet_address, QWWalletUtils.parseAddressTo8Show(Keys.toChecksumHDAddress(address)));
            View copyView = holder.getView(R.id.wallet_copy);
            copyView.setTag(wallet);
            copyView.setOnClickListener(v -> onCopy(v, address));
            copyView.setVisibility(View.VISIBLE);

            //卡片点击
            View card = holder.getView(R.id.wallet_manager_layout);
            card.setOnClickListener(v -> onItemClick(holder.getAdapterPosition()));

            //观察钱包标识
            ImageView watchFlagView = holder.getView(R.id.watch_flag);
            if (wallet.isWatch()) {
                watchFlagView.setVisibility(View.VISIBLE);
                if (wallet.isLedger()) {
                    watchFlagView.setImageResource(R.drawable.wallet_list_ledger);
                } else {
                    watchFlagView.setImageResource(R.drawable.wallet_list_watch);
                }
            } else {
                watchFlagView.setVisibility(View.GONE);
            }

            //HD钱包子钱包标识
            TextView HDIndex = holder.getView(R.id.wallet_child_count_text);
            if (wallet.getType() == Constant.WALLET_TYPE_HD) {
                HDIndex.setVisibility(View.VISIBLE);
                int index = account.getPathAccountIndex();
                String pathIndex = index + "";
                if (index < 10) {
                    pathIndex = "0" + index;
                }
                HDIndex.setText(pathIndex);
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setGradientType(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(5);
                drawable.setColor(Color.parseColor("#FFAE33"));
//                if (account.isAllBTC()) {
//                }
                HDIndex.setBackground(drawable);
            } else {
                HDIndex.setVisibility(View.GONE);
            }

            //显示header 展示总金额
            View headerView = holder.getView(R.id.wallet_manager_header);
            if (bean.isShowAssets()) {
                if (mIsColdModel) {
                    //冷钱包HD标识
                    holder.setGone(R.id.wallet_manager_assets, true);
                    holder.setGone(R.id.wallet_manager_cold_hd, false);
                } else {
                    holder.setGone(R.id.wallet_manager_cold_hd, true);
                    //金额
                    float totalPrice = (mShowAccountType == -1 && wallet.getType() == Constant.WALLET_TYPE_HD) ?
                            getHDWalletTotalPrice(wallet) :
                            getTotalPrice();
                    //获取当前选定法币
                    String currentPriceCoin = SharedPreferencesUtils.getCurrentMarketCoin(getApplication());
                    //获取法币简称符号
                    String coinSymbol = ToolUtils.getCoinSymbol(currentPriceCoin);
                    String totalText = getString(R.string.wallet_manager_total_assets) + " " +
                            Constant.PRICE_ABOUT + coinSymbol + ToolUtils.format8Number(totalPrice);
                    TextView totalAssets = holder.getView(R.id.wallet_manager_assets);
                    totalAssets.setText(totalText);
                }

                //是否可以创建子钱包
                View createChildView = holder.getView(R.id.wallet_manager_create_child);
                if (wallet.getType() == Constant.WALLET_TYPE_HD && mShowAccountType == -1) {
                    createChildView.setTag(wallet);
                    createChildView.setOnClickListener(v -> {
                        QWWallet temp = (QWWallet) v.getTag();
                        CreateChildAccountActivity.startActivity(WalletManagerActivity.this, temp);
                    });
                    createChildView.setVisibility(View.VISIBLE);
                } else {
                    createChildView.setVisibility(View.GONE);
                }

                //是否显示时间轴线条
                if (mShowAccountType == -1) {
                    holder.setVisible(R.id.holder_circle, true);
                    holder.setVisible(R.id.holder_circle_line_height, true);
                    holder.setVisible(R.id.holder_circle_line_width, true);
                } else {
                    holder.setVisible(R.id.holder_circle, false);
                    holder.setVisible(R.id.holder_circle_line_height, false);
                    holder.setVisible(R.id.holder_circle_line_width, false);
                }
                headerView.setVisibility(View.VISIBLE);
            } else {
                headerView.setVisibility(View.GONE);
            }

            //显示左边时间轴线条
            View lineView = holder.getView(R.id.wallet_hd_line);
            if (mShowAccountType == -1 && wallet.getType() == Constant.WALLET_TYPE_HD) {
                //全部模式下，HD钱包显示包含线条
                lineView.setVisibility(View.VISIBLE);
                if (holder.getAdapterPosition() + 1 < getItemCount()) {
                    WalletManagerBean nextBean = getData().get(holder.getAdapterPosition() + 1);
                    if (TextUtils.equals(nextBean.getWallet().getKey(), wallet.getKey())) {
                        holder.setVisible(R.id.wallet_hd_line_clip, true);
                    } else {
                        //下一个是新的钱包，则收尾的线条要段一点
                        holder.setVisible(R.id.wallet_hd_line_clip, false);
                    }
                } else {
                    //最后一个钱包
                    holder.setVisible(R.id.wallet_hd_line_clip, false);
                }
            } else {
                lineView.setVisibility(View.GONE);
            }
        }

        private float getHDWalletTotalPrice(QWWallet wallet) {
            double totalPrice = 0;
            for (QWAccount account : wallet.getAccountList()) {
                totalPrice += account.getTotalPrice();
            }
            return (float) totalPrice;
        }

        private float getTotalPrice() {
            double totalPrice = 0;
            List<WalletManagerBean> list = getData();
            for (WalletManagerBean bean : list) {
                QWAccount account = bean.getAccount();
                //BTC钱包
                if (account.getType() == mShowAccountType) {
                    totalPrice += account.getTotalPrice();
                }
            }
            return (float) totalPrice;
        }

        private int getBackground(QWAccount account) {
            if (account.isEth()) {
                return R.drawable.card_eth_bg;
            } else if (account.isTRX()) {
                return R.drawable.card_trx_bg;
            } else if (account.isAllBTC()) {
                return R.drawable.card_btc_bg;
            }
            return R.drawable.card_bg;
        }

        private void onEdit(View view) {
            WalletManagerBean bean = (WalletManagerBean) view.getTag();
            WalletEditActivity.startActivity(WalletManagerActivity.this, bean);
        }

        private void onCopy(View view, String address) {
            QWWallet wallet = (QWWallet) view.getTag();

            //没有备份并且是第一次点击弹备份提示
            if (0 == wallet.getIsBackup() && !SharedPreferencesUtils.isBackupByKey(getApplicationContext(), wallet.getKey())) {
                mBackWallet = wallet;
                checkWalletBackup();
                SharedPreferencesUtils.setBackupByKey(WalletManagerActivity.this, wallet.getKey());
                return;
            }

            //获取剪贴板管理器：
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                String label = getString(R.string.wallet_copy_address_label);
                // 创建普通字符型ClipData
                address = Keys.toChecksumHDAddress(address);
                ClipData mClipData = ClipData.newPlainText(label, address);
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);

                MyToast.showSingleToastShort(WalletManagerActivity.this, R.string.copy_success);
            }
        }

        @Override
        public long getHeaderId(int position) {
            WalletManagerBean bean = getItem(Math.min(position, getItemCount() - 1));
            return bean.getGroupId();
        }

        @Override
        public BaseViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.holder_recycler_manager_header_item, parent, false);
            return new BaseViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(BaseViewHolder holder, int position) {
            WalletManagerBean bean = getItem(Math.min(position, getItemCount() - 1));
            int groupID = bean.getGroupId();
            TextView textView = holder.itemView.findViewById(R.id.header_title2);
            TextView otherTitle = holder.itemView.findViewById(R.id.header_other_title2);
            if (groupID == NON_HD_GROUP) {
                textView.setText(R.string.no_hd_name);
                otherTitle.setText(R.string.no_hd_name2);
                otherTitle.setVisibility(View.VISIBLE);
            } else if (groupID == HD_GROUP) {
                textView.setText(R.string.hd_name);
                otherTitle.setText(R.string.hd_name2);
                otherTitle.setVisibility(View.VISIBLE);
            } else {
                textView.setText(getWalletHeader(groupID));
                otherTitle.setVisibility(View.GONE);
            }
        }

        private String getWalletHeader(int groupId) {
            switch (groupId) {
                case NON_HD_GROUP:
                    return getString(R.string.no_hd_name);
                case Constant.WALLET_TYPE_ETH:
                    return getString(R.string.eth_name);
                case Constant.WALLET_TYPE_QKC:
                    return getString(R.string.qkc_name);
                case Constant.WALLET_TYPE_TRX:
                    return getString(R.string.trx_name);
            }
            return getString(R.string.hd_name);
        }
    }

    private static final String NAME = "tab";
    private static final String PARAM_TAB = "tab_info";

    public static int getWalletTab(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getInt(PARAM_TAB, -100);
    }

    public static void setWalletTab(Context context, int tab) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
                .putInt(PARAM_TAB, tab).apply();
    }

    private static final int HD_GROUP = 10001;
    private static final int NON_HD_GROUP = 10002;

    @Inject
    WalletManagerViewModelFactory mManagerViewModelFactory;
    WallerManagerViewModel mManagerViewModel;

    private ViewGroup mTabAllView;
    private ViewGroup mTabEthView;
    private ViewGroup mTabTrxView;
    private ViewGroup mTabQkcView;

    private SectionDecoration mSectionDecoration;
    private WalletAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private LinearLayoutManager mLinearLayoutManager;
    private int mAllPosition;
    private int mAllPositionOff;
    private int mETHPosition;
    private int mETHPositionOff;
    private int mTRXPosition;
    private int mTRXPositionOff;
    private int mQKCPosition;
    private int mQKCPositionOff;

    private QWWallet mDefaultWallet;
    private QWWallet mBackWallet;
    private SharedPreferenceRepository mRepository;

    private List<QWWallet> mAllQWWallets;

    private boolean mIsChange;//主钱包是否有改变过
    private boolean mIsChooseNewWallet;//是否选择了新主钱包

    private int mShowAccountType = -1;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_manager_wallet;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_manager_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mDefaultWallet = getIntent().getParcelableExtra(Constant.KEY_WALLET);
        mShowAccountType = getIntent().getIntExtra(Constant.KEY_CURRENT_TYPE, -1);
        boolean mShowOnlyType = getIntent().getBooleanExtra(Constant.KEY_SHOW_ONLY_TYPE, false);

        mRepository = new SharedPreferenceRepository(this);

        mTopBarView.setRightImage(R.drawable.import_create_wallet);
        View addWallet = mTopBarView.getRightImageView();
        if (mShowOnlyType) {
            addWallet.setVisibility(View.GONE);
        } else {
            addWallet.setOnClickListener((view) -> {
                onAddWallet();
                UmengStatistics.topBarAddWalletClickCount(getApplicationContext());
            });
        }

        mTabAllView = findViewById(R.id.wallet_all_tab);
        mTabAllView.setOnClickListener(v -> onClickTabAll());
        mTabEthView = findViewById(R.id.wallet_eth_tab);
        mTabEthView.setOnClickListener(v -> onClickTabETH());
        mTabTrxView = findViewById(R.id.wallet_trx_tab);
        mTabTrxView.setOnClickListener(v -> onClickTabTRX());
        mTabQkcView = findViewById(R.id.wallet_qkc_tab);
        mTabQkcView.setOnClickListener(v -> onClickTabQKC());
        //没有指定选中tab时，获取历史选中tab
        if (mShowAccountType == -1) {
            int index = getWalletTab(getApplicationContext());
            if (index >= -1) {
                mShowAccountType = index;
            }
        }
        selectedTab();
        //指定选中tab，且只选中一个币种，enable其他币种
        if (mShowOnlyType && mShowAccountType != -1) {
            switch (mShowAccountType) {
                case Constant.ACCOUNT_TYPE_ETH:
                    mTabAllView.setEnabled(false);
                    mTabAllView.getChildAt(0).setEnabled(false);
                    mTabTrxView.setEnabled(false);
                    mTabTrxView.getChildAt(0).setEnabled(false);
                    mTabQkcView.setEnabled(false);
                    mTabQkcView.getChildAt(0).setEnabled(false);
                    break;
                case Constant.ACCOUNT_TYPE_TRX:
                    mTabAllView.setEnabled(false);
                    mTabAllView.getChildAt(0).setEnabled(false);
                    mTabEthView.setEnabled(false);
                    mTabEthView.getChildAt(0).setEnabled(false);
                    mTabQkcView.setEnabled(false);
                    mTabQkcView.getChildAt(0).setEnabled(false);
                    break;
                case Constant.ACCOUNT_TYPE_QKC:
                    mTabAllView.setEnabled(false);
                    mTabAllView.getChildAt(0).setEnabled(false);
                    mTabEthView.setEnabled(false);
                    mTabEthView.getChildAt(0).setEnabled(false);
                    mTabTrxView.setEnabled(false);
                    mTabTrxView.getChildAt(0).setEnabled(false);
                    break;
            }
        }

        mRecyclerView = findViewById(R.id.wallet_manager_recycler_context);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setItemAnimator(null);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            if (Constant.isCanLock(getApplicationContext())) {
                LockPatternActivity.startActivity(this);
            }
        }

        mManagerViewModel = new ViewModelProvider(this, mManagerViewModelFactory)
                .get(WallerManagerViewModel.class);
        mManagerViewModel.getWalletObserve().observe(this, this::findSuccess);

        mManagerViewModel.getDefaultObserve().observe(this, this::setDefaultWalletSuccess);
        mManagerViewModel.changeSymbol().observe(this, this::onChangeSuccess);

        mManagerViewModel.findWallets();
    }

    //钱包获取成功
    private void findSuccess(QWWallet[] wallets) {
        mAllQWWallets = new ArrayList<>(Arrays.asList(wallets));
        checkWallet();
        scrollTOPosition();
    }

    private void scrollTOPosition() {
        List<WalletManagerBean> list = mAdapter.getData();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            QWAccount account = list.get(i).getAccount();
            if (TextUtils.equals(mDefaultWallet.getCurrentAddress(), account.getAddress())) {
                mLinearLayoutManager.scrollToPosition(i);
                return;
            }
        }
    }

    //遍历数据
    private void checkWallet() {
        //根据tab生成对应列表数据
        List<WalletManagerBean> list = new ArrayList<>();
        switch (mShowAccountType) {
            case -1:
                list = checkAllWallet();
                break;
            case Constant.ACCOUNT_TYPE_ETH:
                list = checkOtherWallet(Constant.ACCOUNT_TYPE_ETH);
                break;
            case Constant.ACCOUNT_TYPE_TRX:
                list = checkOtherWallet(Constant.ACCOUNT_TYPE_TRX);
                break;
            case Constant.ACCOUNT_TYPE_QKC:
                list = checkOtherWallet(Constant.ACCOUNT_TYPE_QKC);
                break;
        }

        mAdapter = new WalletAdapter(R.layout.holder_recycler_manager_wallet, list, false);
        //刷新header
        if (mSectionDecoration != null) {
            mRecyclerView.removeItemDecoration(mSectionDecoration);
        }
        mSectionDecoration = new SectionDecoration(mAdapter);
        mRecyclerView.addItemDecoration(mSectionDecoration);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mSectionDecoration.invalidateHeaders();
            }
        });
    }

    private List<WalletManagerBean> checkAllWallet() {
        List<WalletManagerBean> list = new ArrayList<>();
        List<WalletManagerBean> hdList = new ArrayList<>();
        List<WalletManagerBean> noHdList = new ArrayList<>();
        for (QWWallet wallet : mAllQWWallets) {
            List<QWAccount> accountList = wallet.getAccountList();
            int size = accountList.size();
            for (int i = 0; i < size; i++) {
                WalletManagerBean bean = new WalletManagerBean();
                bean.setWallet(wallet);
                bean.setAccount(accountList.get(i));
                //属于什么组
                if (wallet.getType() == Constant.WALLET_TYPE_HD) {
                    //是否显示金额
                    if (i == 0) {
                        bean.setShowAssets(true);
                    }
                    bean.setGroupId(HD_GROUP);
                    hdList.add(bean);
                } else {
                    bean.setGroupId(NON_HD_GROUP);
                    noHdList.add(bean);
                }
            }
        }
        list.addAll(hdList);
        list.addAll(noHdList);
        return list;
    }

    private List<WalletManagerBean> checkOtherWallet(int accountType) {
        List<WalletManagerBean> hdList = new ArrayList<>();
        List<WalletManagerBean> noHdList = new ArrayList<>();

        List<WalletManagerBean> list = new ArrayList<>();
        for (QWWallet wallet : mAllQWWallets) {
            List<QWAccount> accountList = wallet.getAccountList();
            int size = accountList.size();
            for (int i = 0; i < size; i++) {
                QWAccount account = accountList.get(i);
                if (accountType == Constant.WALLET_TYPE_ETH) {
                    if (!account.isEth()) {
                        continue;
                    }
                } else if (accountType == Constant.WALLET_TYPE_QKC) {
                    if (!account.isQKC()) {
                        continue;
                    }
                } else if (accountType == Constant.WALLET_TYPE_TRX) {
                    if (!account.isTRX()) {
                        continue;
                    }
                }

                WalletManagerBean bean = new WalletManagerBean();
                bean.setWallet(wallet);
                bean.setAccount(account);
                //带不带头部信息
                bean.setGroupId(accountType);

                //属于什么组
                if (wallet.getType() == Constant.WALLET_TYPE_HD) {
                    hdList.add(bean);
                } else {
                    noHdList.add(bean);
                }
            }
        }
        list.addAll(hdList);
        list.addAll(noHdList);
        if (list.size() > 0) {
            WalletManagerBean firstItem = list.get(0);
            firstItem.setShowAssets(true);
        }
        return list;
    }

    private void deleteWallet(QWWallet temp) {
        //该wallet是否还有account
        QWAccountDao qwAccountDao = new QWAccountDao(getApplicationContext());
        List<QWAccount> newList = qwAccountDao.queryParamsByKey(temp.getKey());
        if (newList == null || newList.isEmpty()) {
            //该钱包已经不存在account
            //移除wallet
            if (mAllQWWallets != null) {
                int walletSize = mAllQWWallets.size();
                for (int i = 0; i < walletSize; i++) {
                    if (TextUtils.equals(mAllQWWallets.get(i).getKey(), temp.getKey())) {
                        mAllQWWallets.remove(i);
                        break;
                    }
                }
            }
            //更新UI
            if (mAdapter != null) {
                Iterator<WalletManagerBean> iterator = mAdapter.getData().iterator();
                while (iterator.hasNext()) {
                    //多线程情况下加锁
                    WalletManagerBean item = iterator.next();
                    if (TextUtils.equals(item.getWallet().getKey(), temp.getKey())) {
                        iterator.remove();
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
            return;
        }

        //刷新数据，刷新UI
        updateAllWallet(temp, null);
    }

    //更新选中态钱包
    public void updateDefaultWallet(QWWallet temp) {
        if (mAllQWWallets != null) {
            for (QWWallet wallet : mAllQWWallets) {
                if (TextUtils.equals(wallet.getKey(), temp.getKey())) {
                    wallet.setCurrentAddress(temp.getCurrentAddress());
                    break;
                }
            }
        }

        if (mAdapter != null) {
            int size = mAdapter.getItemCount();
            for (int i = 0; i < size; i++) {
                WalletManagerBean bean = mAdapter.getItem(i);
                if (TextUtils.equals(bean.getWallet().getKey(), temp.getKey())) {
                    bean.getWallet().setCurrentAddress(temp.getCurrentAddress());
                    mAdapter.notifyItemChanged(i);
                    mRecyclerView.scrollToPosition(i);
                }
            }
        }
    }

    //更新数据，并刷新UI
    private void updateAllWallet(QWWallet temp, QWAccount editAccount) {
        if (mAllQWWallets == null) {
            return;
        }

        int index = 0;
        QWWallet wallet = null;
        int walletSize = mAllQWWallets.size();
        for (int i = 0; i < walletSize; i++) {
            if (TextUtils.equals(mAllQWWallets.get(i).getKey(), temp.getKey())) {
                //获取历史数据
                List<QWAccount> oldList = mAllQWWallets.get(i).getAccountList();
                //获取新的account
                QWAccountDao qwAccountDao = new QWAccountDao(getApplicationContext());
                List<QWAccount> newList = qwAccountDao.queryParamsByKey(temp.getKey());
                //按币种进行过滤
                List<QWAccount> list = new ArrayList<>();
                List<QWAccount> QKCList = new ArrayList<>();
                List<QWAccount> TRXList = new ArrayList<>();
                List<QWAccount> ETHList = new ArrayList<>();
                List<QWAccount> BTCList = new ArrayList<>();
                for (QWAccount account : newList) {
                    //更新金额
                    for (QWAccount oldAccount : oldList) {
                        if (TextUtils.equals(account.getAddress(), oldAccount.getAddress())) {
                            account.setTotalPrice(oldAccount.getTotalPrice());
                            break;
                        }
                    }
                    if (account.isBTCSegWit()
                            && temp.isShowBTCSegWit(account.getPathAccountIndex())) {
                        //切换币种后需要更新余额
                        if (editAccount != null && TextUtils.equals(editAccount.getAddress(), account.getAddress())) {
                            account.setTotalPrice(editAccount.getTotalPrice());
                        }
                        BTCList.add(account);
                    } else if (account.isBTC()
                            && !temp.isShowBTCSegWit(account.getPathAccountIndex())) {
                        //切换币种后需要更新余额
                        if (editAccount != null && TextUtils.equals(editAccount.getAddress(), account.getAddress())) {
                            account.setTotalPrice(editAccount.getTotalPrice());
                        }
                        BTCList.add(account);
                    } else if (account.isEth()) {
                        ETHList.add(account);
                    } else if (account.isTRX()) {
                        TRXList.add(account);
                    } else if (account.isQKC()) {
                        QKCList.add(account);
                    }
                }
                list.addAll(QKCList);
                list.addAll(TRXList);
                list.addAll(ETHList);
                list.addAll(BTCList);
                temp.setAccountList(list);

                index = i;
                wallet = temp;
                break;
            }
        }

        if (wallet != null) {
            //替换数据
            mAllQWWallets.remove(index);
            mAllQWWallets.add(index, wallet);
            //刷新UI
            if (mAdapter != null) {
                //根据tab生成对应列表数据
                List<WalletManagerBean> list = new ArrayList<>();
                switch (mShowAccountType) {
                    case -1:
                        list = checkAllWallet();
                        break;
                    case Constant.ACCOUNT_TYPE_ETH:
                        list = checkOtherWallet(Constant.ACCOUNT_TYPE_ETH);
                        break;
                    case Constant.ACCOUNT_TYPE_TRX:
                        list = checkOtherWallet(Constant.ACCOUNT_TYPE_TRX);
                        break;
                    case Constant.ACCOUNT_TYPE_QKC:
                        list = checkOtherWallet(Constant.ACCOUNT_TYPE_QKC);
                        break;
                }
                mAdapter.setNewInstance(list);
            }
        }
    }

    private void onChangeSuccess(QWWallet wallet) {
        //切换主钱包
        mManagerViewModel.setDefaultWallet(wallet);
    }

    //设置主钱包成功
    private void setDefaultWalletSuccess(QWWallet wallet) {
        if (mShowAccountType != -1) {
            EventBus.getDefault().postSticky(new ChooseWalletEvent(""));
        }

        mIsChange = true;
        mIsChooseNewWallet = true;
        mDefaultWallet = wallet;
        finish();
    }

    //做备份提示
    private void checkWalletBackup() {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.wallet_backup_title);
        dialog.setMessage(R.string.wallet_backup_error_message);
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.setPositiveBtn(R.string.ok, v -> {
            dialog.dismiss();
            checkPassWord();
        });
        dialog.show();
    }

    private void checkPassWord() {
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mBackWallet,
                new SystemUtils.OnCheckPassWordListenerImp() {
                    @Override
                    public void onPasswordSuccess(String password) {
                        goBackup();
                    }
                });
    }

    private void goBackup() {
        Intent intent = new Intent(this, BackupPhraseHintActivity.class);
        intent.putExtra(Constant.WALLET_KEY, mBackWallet.getKey());
        intent.putExtra(Constant.IS_RESULT_BACKUP_PHRASE, true);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_EDIT_WALLET && resultCode == RESULT_OK) {
            QWWallet mQuarkWallet = data.getParcelableExtra(Constant.KEY_WALLET);
            QWAccount editAccount = data.getParcelableExtra(Constant.KEY_ACCOUNT);
            if (mQuarkWallet == null) {
                return;
            }
            //删除钱包
            boolean isDelete = data.getBooleanExtra(Constant.KEY_DELETE_WALLET, false);
            if (isDelete) {
                //刷新数据,刷新UI
                deleteWallet(mQuarkWallet);
                //切换了选中钱包
                boolean isChange = data.getBooleanExtra(Constant.KEY_CHOOSE_NEW_WALLET, false);
                QWWallet defaultWallet = data.getParcelableExtra(Constant.KEY_CHOOSE_WALLET);
                if (isChange && defaultWallet != null) {
                    mIsChange = true;
                    mIsChooseNewWallet = true;
                    mDefaultWallet = defaultWallet;

                    //HD钱包 则更新数据
                    if (defaultWallet.getType() == Constant.WALLET_TYPE_HD) {
                        updateDefaultWallet(defaultWallet);
                    } else {
                        if (mAdapter != null) {
                            //刷新UI
                            List<WalletManagerBean> list = mAdapter.getData();
                            int size = list.size();
                            for (int i = 0; i < size; i++) {
                                WalletManagerBean bean = list.get(i);
                                if (TextUtils.equals(bean.getWallet().getKey(), defaultWallet.getKey())) {
                                    mAdapter.notifyItemChanged(i);
                                    mRecyclerView.scrollToPosition(i);
                                }
                            }
                        }
                    }
                }
                return;
            }

            //编辑钱包
            //刷新数据,刷新UI
            updateAllWallet(mQuarkWallet, editAccount);
            //修改了当前主钱包，则返回主界面时需要更新
            if (mDefaultWallet != null && TextUtils.equals(mQuarkWallet.getKey(), mDefaultWallet.getKey())) {
                mDefaultWallet = mQuarkWallet;
                mIsChange = true;
                boolean isChangeBTC = data.getBooleanExtra(Constant.KEY_CHANGE_BTC_ACCOUNT, false);
                if (isChangeBTC) {
                    mIsChooseNewWallet = true;
                }
            }
        }
    }

    @Override
    public void finish() {
        if (mIsChange) {
            Intent intent = getIntent();
            intent.putExtra(Constant.KEY_CHOOSE_NEW_WALLET, mIsChooseNewWallet);
            setResult(RESULT_OK, intent);
        }
        super.finish();
    }

    private void onAddWallet() {
        Intent intent = new Intent(WalletManagerActivity.this, HomeActivity.class);
        intent.putExtra(Constant.FROM_WALLET_MANAGER, true);
        startActivity(intent);
    }

    private void initCurrentPosition() {
        //记录当前位置
        int firstVisiblePosition = mLinearLayoutManager.findFirstVisibleItemPosition();
        int firstVisibleOff = 0;
        View view = mLinearLayoutManager.findViewByPosition(firstVisiblePosition);
        if (view != null) {
            firstVisibleOff = view.getTop();
        }
        switch (mShowAccountType) {
            case -1:
                mAllPosition = firstVisiblePosition;
                mAllPositionOff = firstVisibleOff;
                if (firstVisiblePosition == 0) {
                    mAllPositionOff = firstVisibleOff - mSectionDecoration.getHeaderView(mRecyclerView, 0).getHeight();
                }
                break;
            case Constant.ACCOUNT_TYPE_ETH:
                mETHPosition = firstVisiblePosition;
                mETHPositionOff = firstVisibleOff;
                if (firstVisiblePosition == 0) {
                    mETHPositionOff = firstVisibleOff - mSectionDecoration.getHeaderView(mRecyclerView, 0).getHeight();
                }
                break;
            case Constant.ACCOUNT_TYPE_TRX:
                mTRXPosition = firstVisiblePosition;
                mTRXPositionOff = firstVisibleOff;
                if (firstVisiblePosition == 0) {
                    mTRXPositionOff = firstVisibleOff - mSectionDecoration.getHeaderView(mRecyclerView, 0).getHeight();
                }
                break;
            case Constant.ACCOUNT_TYPE_QKC:
                mQKCPosition = firstVisiblePosition;
                mQKCPositionOff = firstVisibleOff;
                if (firstVisiblePosition == 0) {
                    mQKCPositionOff = firstVisibleOff - mSectionDecoration.getHeaderView(mRecyclerView, 0).getHeight();
                }
                break;
        }
    }

    private void onClickTabAll() {
        if (mAllQWWallets == null) {
            return;
        }
        //记录当前位置
        initCurrentPosition();

        mShowAccountType = -1;
        checkWallet();
        selectedTab();
        //恢复位置
        mLinearLayoutManager.scrollToPositionWithOffset(mAllPosition, mAllPositionOff);
        //记录tab位置
        setWalletTab(getApplicationContext(), mShowAccountType);
    }

    private void onClickTabETH() {
        if (mAllQWWallets == null) {
            return;
        }
        //记录当前位置
        initCurrentPosition();

        mShowAccountType = Constant.ACCOUNT_TYPE_ETH;
        checkWallet();
        selectedTab();
        //恢复位置
        mLinearLayoutManager.scrollToPositionWithOffset(mETHPosition, mETHPositionOff);
        //记录tab位置
        setWalletTab(getApplicationContext(), mShowAccountType);
    }

    private void onClickTabTRX() {
        if (mAllQWWallets == null) {
            return;
        }
        //记录当前位置
        initCurrentPosition();

        mShowAccountType = Constant.ACCOUNT_TYPE_TRX;
        checkWallet();
        selectedTab();
        //恢复位置
        mLinearLayoutManager.scrollToPositionWithOffset(mTRXPosition, mTRXPositionOff);
        //记录tab位置
        setWalletTab(getApplicationContext(), mShowAccountType);
    }

    private void onClickTabQKC() {
        if (mAllQWWallets == null) {
            return;
        }
        //记录当前位置
        initCurrentPosition();

        mShowAccountType = Constant.ACCOUNT_TYPE_QKC;
        checkWallet();
        selectedTab();
        //恢复位置
        mLinearLayoutManager.scrollToPositionWithOffset(mQKCPosition, mQKCPositionOff);
        //记录tab位置
        setWalletTab(getApplicationContext(), mShowAccountType);
    }

    private void selectedTab() {
        mTabAllView.setSelected(false);
        mTabEthView.setSelected(false);
        mTabTrxView.setSelected(false);
        mTabQkcView.setSelected(false);

        if (mShowAccountType == -1) {
            mTabAllView.setSelected(true);
        } else if (mShowAccountType == Constant.ACCOUNT_TYPE_ETH) {
            mTabEthView.setSelected(true);
        } else if (mShowAccountType == Constant.ACCOUNT_TYPE_TRX) {
            mTabTrxView.setSelected(true);
        } else if (mShowAccountType == Constant.ACCOUNT_TYPE_QKC) {
            mTabQkcView.setSelected(true);
        }
    }

    public void onItemClick(int position) {
        if (ToolUtils.isFastDoubleClick(400)) {
            return;
        }
        WalletManagerBean bean = mAdapter.getItem(position);
        if (bean == null || bean.getWallet() == null) {
            return;
        }

        QWAccount account = bean.getAccount();
        QWWallet wallet = bean.getWallet();

        if (mDefaultWallet != null &&
                TextUtils.equals(mDefaultWallet.getKey(), account.getKey()) &&
                TextUtils.equals(mDefaultWallet.getCurrentAddress(), account.getAddress())) {
            //当前钱包，当前币种
            finish();
            return;
        }

        if (wallet.getType() == Constant.WALLET_TYPE_HD) {
            //切换币种
            wallet.setCurrentAddress(account.getAddress());
            wallet.setCurrentAccount(account);
            mManagerViewModel.changeWalletSymbol(wallet);
        } else {
            //切换主钱包
            mManagerViewModel.setDefaultWallet(wallet);
        }
    }
}
