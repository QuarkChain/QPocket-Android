package com.quarkonium.qpocket.model.main;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.MySwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.google.android.material.appbar.AppBarLayout;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.TokenRepository;
import com.quarkonium.qpocket.base.SupportBaseFragment;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.main.viewmodel.MainWallerViewModel;
import com.quarkonium.qpocket.model.permission.PermissionHelper;
import com.quarkonium.qpocket.model.transaction.OtherTokenDetailActivity;
import com.quarkonium.qpocket.model.transaction.TransactionDetailActivity;
import com.quarkonium.qpocket.model.wallet.BackupPhraseHintActivity;
import com.quarkonium.qpocket.rx.ChangeWalletEvent;
import com.quarkonium.qpocket.rx.NetworkChangeEvent;
import com.quarkonium.qpocket.rx.RxBus;
import com.quarkonium.qpocket.rx.Subscribe;
import com.quarkonium.qpocket.rx.ThreadMode;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.tendcloud.tenddata.TCAgent;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * 主界面-钱包
 */
public class WalletFragment extends SupportBaseFragment implements OnItemClickListener {

    private class TokenAdapter extends BaseQuickAdapter<TokenBean, BaseViewHolder> {

        TokenAdapter(int layoutResId, @Nullable List<TokenBean> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(@NotNull BaseViewHolder holder, TokenBean item) {
            String symbol = "";
            QWToken token = item.getToken();
            if (token != null) {
                ImageView icon = holder.getView(R.id.token_img);
                String path = token.getIconPath();
                if (!TextUtils.isEmpty(path)) {
                    Glide.with(icon)
                            .asBitmap()
                            .load(path)
                            .into(icon);
                } else {
                    Glide.with(icon)
                            .asBitmap()
                            .load(R.drawable.token_default_icon)
                            .into(icon);
                }

                String name = token.getSymbol();
                holder.setText(R.id.token_name, name.toUpperCase());
                symbol = token.getSymbol();
            }

            String count = "0";
            QWBalance balance = item.getBalance();
            if (balance != null) {
                count = QWWalletUtils.getIntTokenFromWei16(balance.getBalance());
                if (token != null) {
                    count = QWWalletUtils.getIntTokenFromWei16(balance.getBalance(), token.getTokenUnit());
                }
                holder.setText(R.id.token_count, count);
            } else if (token != null) {
                holder.setText(R.id.token_count, count);
            }

            TextView priceView = holder.getView(R.id.token_price);
            priceView.setText(getPrice(count, symbol));

            if (holder.getAdapterPosition() == getItemCount() - 1) {
                holder.setVisible(R.id.line, false);
            } else {
                holder.setVisible(R.id.line, true);
            }
        }

        private String getPrice(String count, String symbol) {
            return ToolUtils.getTokenCurrentCoinPriceText(requireContext(), mDefaultWallet.getCurrentAddress(), symbol, count);
        }

        private int mFirstVisiblePosition;
        private int mFirstVisibleOff;

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            mFirstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
            View view = mLayoutManager.findViewByPosition(mFirstVisiblePosition);
            if (view != null) {
                mFirstVisibleOff = view.getTop();
            }
            super.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mLayoutManager.scrollToPositionWithOffset(mFirstVisiblePosition, mFirstVisibleOff);
        }
    }

    public static WalletFragment newInstance() {
        Bundle args = new Bundle();
        WalletFragment fragment = new WalletFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private static final String COORDINATOR_OFFSET = "coordinator_off";

    private MySwipeRefreshLayout mSwipeRefreshLayout;
    //定义为全局变量，fragment销毁后还能记录还原位置
    private AppBarLayout mAppbarLayout;

    private ImageView mManagerView;
    private View mQrCameraView;

    //钱包card
    private View mMainCardView;
    private ImageView mCircleIcon;
    private View mBackupInfoView;

    private TextView mNameTextView;
    private TextView mHDAccountIndexView;
    private TextView mChainIdTextView;
    private TextView mSharedIdTextView;
    private TextView mAddressTextView;

    private ImageView mWatchWalletFlag;

    private TextView mPriceSymbolView;
    private TextView mPriceTextView;

    //token card
    private RecyclerView mTokenRecycleView;
    private TokenAdapter mTokenAdapter;
    private LinearLayoutManager mLayoutManager;
    private View mEmptyView;

    private View mAddTokenView;

    private View mProgressView;

    private MainWallerViewModel mMainWalletFragmentViewModel;


    private QWWallet mDefaultWallet;
    private String mUpdateWalletIcon;
    //是否需要加载
    boolean isNeedLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //注册
        RxBus.get().register(this);
        EventBus.getDefault().register(this);

        if (mTokenAdapter == null) {
            mTokenAdapter = new TokenAdapter(R.layout.holder_recycler_token_item, new ArrayList<>());
            mTokenAdapter.setOnItemClickListener(this);
            isNeedLoad = true;
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_main_wallet;
    }

    @Override
    public int getFragmentTitle() {
        return R.string.main_menu_tag_wallet;
    }

    @Override
    protected void onInitView(Bundle savedInstanceState, View rootView) {
        mSwipeRefreshLayout = rootView.findViewById(R.id.main_wallet_swipe);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        mQrCameraView = rootView.findViewById(R.id.wallet_qr_camera);
        mQrCameraView.setOnClickListener(v -> onQRScan());

        mManagerView = rootView.findViewById(R.id.wallet_manager);
        rootView.findViewById(R.id.wallet_manager_layout).setOnClickListener(v -> goManager());

        View qrAddress = rootView.findViewById(R.id.wallet_qr_address);
        qrAddress.setOnClickListener(v -> onGetAddress());

        //wallet card
        View card = rootView.findViewById(R.id.middle_wallet_layout);
        card.setOnClickListener(v -> goEdit());

        mMainCardView = rootView.findViewById(R.id.main_card_bg_view);

        View copyAddress = rootView.findViewById(R.id.wallet_copy_view);
        copyAddress.setOnClickListener(v -> onCopy());

        mCircleIcon = rootView.findViewById(R.id.wallet_icon);
        mCircleIcon.setOnClickListener(v -> onIconClick());
        mBackupInfoView = rootView.findViewById(R.id.wallet_backup_info);

        mNameTextView = rootView.findViewById(R.id.wallet_name_text);
        mHDAccountIndexView = rootView.findViewById(R.id.wallet_hd_count_text);
        mChainIdTextView = rootView.findViewById(R.id.wallet_chain_text);
        mSharedIdTextView = rootView.findViewById(R.id.wallet_shard_text);
        mAddressTextView = rootView.findViewById(R.id.wallet_address_text);

        mPriceSymbolView = rootView.findViewById(R.id.main_wallet_price_symbol);
        mPriceTextView = rootView.findViewById(R.id.main_wallet_price);

        mWatchWalletFlag = rootView.findViewById(R.id.wallet_watch_flag);

        //token card
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mTokenRecycleView = rootView.findViewById(R.id.main_wallet_token_layout);
        mLayoutManager = new LinearLayoutManager(requireContext());
        mTokenRecycleView.setLayoutManager(mLayoutManager);
        mTokenRecycleView.setAdapter(mTokenAdapter);

        mAddTokenView = rootView.findViewById(R.id.wallet_add_token);
        mAddTokenView.setOnClickListener(v -> addToken());


        mAppbarLayout = rootView.findViewById(R.id.appbar);
        mAppbarLayout.addOnOffsetChangedListener((AppBarLayout appBarLayout, int verticalOffset) -> {
                    if (verticalOffset >= 0) {
                        mSwipeRefreshLayout.setEnabled(true);
                    } else {
                        mSwipeRefreshLayout.setEnabled(false);
                    }
                }
        );

        mProgressView = rootView.findViewById(R.id.progress_layout);

        checkTokenAdapter();

        if (savedInstanceState != null) {
            //恢复位置
            int mCoordinatorOffset = savedInstanceState.getInt(COORDINATOR_OFFSET);
            mAppbarLayout.post(() -> {
                CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) mAppbarLayout.getLayoutParams();
                if (layoutParams != null && layoutParams.getBehavior() instanceof AppBarLayout.Behavior) {
                    AppBarLayout.Behavior appBarLayoutBehavior = (AppBarLayout.Behavior) layoutParams.getBehavior();
                    appBarLayoutBehavior.setTopAndBottomOffset(mCoordinatorOffset);
                }
            });
        }
    }

    @Override
    public void onLazyInitView(@Nullable Bundle savedInstanceState) {
        super.onLazyInitView(savedInstanceState);
        if (mMainWalletFragmentViewModel == null) {
            mMainWalletFragmentViewModel = new ViewModelProvider(requireActivity(), ((MainActivity) requireActivity()).mMainWallerFragmentFactory).get(MainWallerViewModel.class);
            mMainWalletFragmentViewModel.findDefaultWalletObserve().observe(requireActivity(), this::findWalterSuccess);
        }
        mMainWalletFragmentViewModel.tokensObserver().observe(this, this::tokenSuccess);
        mMainWalletFragmentViewModel.coinPrice().observe(this, v -> onCoinPriceSuccess());

        if (isNeedLoad) {
            mMainWalletFragmentViewModel.findWallet();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //位置
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) mAppbarLayout.getLayoutParams();
        if (layoutParams != null && layoutParams.getBehavior() instanceof AppBarLayout.Behavior) {
            AppBarLayout.Behavior appBarLayoutBehavior = (AppBarLayout.Behavior) layoutParams.getBehavior();
            int coordinatorOffset = appBarLayoutBehavior.getTopAndBottomOffset();
            outState.putInt(COORDINATOR_OFFSET, coordinatorOffset);
        }
    }

    //生成二维码地址
    private void onGetAddress() {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }
        if (mDefaultWallet == null) {
            return;
        }
        WalletBitmapAddressActivity.startActivity(requireActivity(), mDefaultWallet);
        UmengStatistics.topBarShowQRCodeClickCount(requireContext(), mDefaultWallet.getCurrentAddress());
        //友盟统计
        UmengStatistics.qrMainReceiveTokenClick(requireActivity());
    }

    //扫描二维码
    private void onQRScan() {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }

        final RxPermissions rxPermissions = new RxPermissions(this);
        if (!rxPermissions.isGranted(Manifest.permission.CAMERA)) {
            Disposable disposable = rxPermissions.request(Manifest.permission.CAMERA)
                    .subscribe(aBoolean -> {
                        if (aBoolean) {
                            startCamera();
                        } else {
                            String[] name = {Manifest.permission.CAMERA};
                            MyToast.showSingleToastLong(getActivity(), PermissionHelper.getPermissionToast(requireContext().getApplicationContext(), name));
                        }
                    });
            return;
        }

        startCamera();
    }

    private void startCamera() {
        if (mDefaultWallet == null) {
            return;
        }
        int accountType = mDefaultWallet.getCurrentAccount().getType();
        Intent intent = new Intent(requireActivity(), CaptureActivity.class);
        intent.putExtra(Constant.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(Constant.KEY_WALLET, mDefaultWallet);
        startActivity(intent);
        UmengStatistics.topBarScanQRCodeClickCount(requireContext(), mDefaultWallet.getCurrentAddress());
        //友盟统计
        UmengStatistics.qrMainSendTokenClick(requireActivity());
    }

    //钱包管理
    private void goManager() {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }
        if (mDefaultWallet == null) {
            return;
        }
        Intent intent = new Intent(requireActivity(), WalletManagerActivity.class);
        intent.putExtra(Constant.KEY_WALLET, mDefaultWallet);
        startActivityForResult(intent, Constant.REQUEST_CODE_EDIT_WALLET);
    }

    private void goEdit() {
        if (mDefaultWallet == null) {
            return;
        }
        showProgress(false);
        Intent intent = new Intent(requireActivity(), WalletEditActivity.class);
        intent.putExtra(WalletEditActivity.KEY_WALLET, mDefaultWallet);
        intent.putExtra(WalletEditActivity.KEY_ACCOUNT, mDefaultWallet.getCurrentAccount());
        startActivityForResult(intent, Constant.REQUEST_CODE_EDIT_WALLET);
    }

    private void onEditIcon() {
        if (mDefaultWallet == null) {
            return;
        }
        Intent intent = new Intent(requireActivity(), WalletEditSettingIconActivity.class);
        intent.putExtra(Constant.KEY_WALLET_ICON_PATH, mDefaultWallet.getCurrentAccount().getIcon());
        startActivityForResult(intent, Constant.REQUEST_CODE_EDIT_ICON);
    }

    private void goBackup() {
        if (getActivity() == null) {
            return;
        }
        Intent intent = new Intent(requireActivity(), BackupPhraseHintActivity.class);
        intent.putExtra(Constant.WALLET_KEY, mDefaultWallet.getKey());
        intent.putExtra(Constant.IS_RESULT_BACKUP_PHRASE, true);
        startActivity(intent);
    }

    private void addToken() {
        if (mDefaultWallet == null) {
            return;
        }

        Intent intent = new Intent(requireActivity(), TokenListActivity.class);
        startActivityForResult(intent, Constant.REQUEST_CODE_ADD_TOKEN);
    }

    //拷贝地址
    private void onCopy() {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }
        if (mDefaultWallet == null) {
            return;
        }

        //没有备份并且是第一次点击弹备份提示
        if (0 == mDefaultWallet.getIsBackup() && !SharedPreferencesUtils.isBackupByKey(requireContext(), mDefaultWallet.getKey())) {
            checkWalletBackup();
            SharedPreferencesUtils.setBackupByKey(requireContext(), mDefaultWallet.getKey());
            return;
        }

        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            String label = getString(R.string.wallet_copy_address_label);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText(label, mDefaultWallet.getCurrentShowAddress());
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            MyToast.showSingleToastShort(requireActivity(), R.string.copy_success);
        }
    }

    private void onIconClick() {
        if (mBackupInfoView.getVisibility() == View.VISIBLE) {
            checkWalletBackup();
        } else {
            onEditIcon();
        }
    }

    //做备份提示
    private void checkWalletBackup() {
        QuarkSDKDialog dialog = new QuarkSDKDialog(requireActivity());
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
        SystemUtils.checkPassword(requireActivity(), getFragmentManager(), mDefaultWallet,
                new SystemUtils.OnCheckPassWordListenerImp() {
                    @Override
                    public void onPasswordSuccess(String password) {
                        onPassword();
                    }
                });
    }

    private void findWalterSuccess(QWWallet wallet) {
        if (wallet == null) {
            return;
        }
        if (getContext() == null) {
            return;
        }

        if (WalletUtils.isQKCValidAddress(wallet.getCurrentAddress())) {
            TCAgent.setGlobalKV("CoinType", String.valueOf(Constant.HD_PATH_CODE_QKC));
        } else if (WalletUtils.isValidAddress(wallet.getCurrentAddress())) {
            TCAgent.setGlobalKV("CoinType", String.valueOf(Constant.HD_PATH_CODE_ETH));
        } else if (TronWalletClient.isTronAddressValid(wallet.getCurrentAddress())) {
            TCAgent.setGlobalKV("CoinType", String.valueOf(Constant.HD_PATH_CODE_TRX));
        }

        if (!TextUtils.isEmpty(mUpdateWalletIcon)) {
            //更新数据库
            QWAccountDao walletDao = new QWAccountDao(requireContext());
            walletDao.updateWalletIcon(mUpdateWalletIcon, wallet.getKey());
            wallet.getCurrentAccount().setIcon(mUpdateWalletIcon);
            mUpdateWalletIcon = null;
        }

        mDefaultWallet = wallet;
        //更新UI数据
        updateNormalWalletUi();
        checkTokenAdapter();

        if (isNeedLoad) {
            //同步数据 获取当前Token数量
            pullToTokenInfo();
        }
        isNeedLoad = false;
    }

    private void tokenSuccess(ArrayList<TokenBean> list) {
        mSwipeRefreshLayout.setRefreshing(false);

        if (mDefaultWallet == null) {
            return;
        }

        if (list.isEmpty()) {
            ArrayList<TokenBean> tokenBeans = new ArrayList<>();
            tokenBeans.add(TokenRepository.getMainTokenBean(requireContext(), mDefaultWallet.getCurrentAccount()));
            mTokenAdapter.setNewInstance(tokenBeans);
            checkTokenAdapter();
            updateCardPrice();
            return;
        }

        if (mDefaultWallet.getCurrentAccount().isTRX()) {
            ArrayList<TokenBean> trc10List = new ArrayList<>();
            ArrayList<TokenBean> trc20List = new ArrayList<>();
            for (TokenBean bean : list) {
                if (TronWalletClient.isTronErc10TokenAddressValid(bean.getToken().getAddress())) {
                    trc10List.add(bean);
                } else {
                    trc20List.add(bean);
                }
            }
            trc10List.addAll(trc20List);
            mTokenAdapter.setNewInstance(trc10List);
        } else if (mDefaultWallet.getCurrentAccount().isQKC()) {
            ArrayList<TokenBean> trc10List = new ArrayList<>();
            ArrayList<TokenBean> trc20List = new ArrayList<>();
            for (TokenBean bean : list) {
                if (bean.getToken().isNative()) {
                    trc10List.add(bean);
                } else {
                    trc20List.add(bean);
                }
            }
            trc10List.addAll(trc20List);
            mTokenAdapter.setNewInstance(trc10List);
        } else {
            mTokenAdapter.setNewInstance(list);
        }
        checkTokenAdapter();
        updateCardPrice();
    }

    private void checkTokenAdapter() {
        if (mTokenAdapter != null && mTokenAdapter.getItemCount() > 0) {
            mEmptyView.setVisibility(View.GONE);
            mTokenRecycleView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
            mTokenRecycleView.setVisibility(View.GONE);
        }
    }

    private void pullToTokenInfo() {
        if (getContext() == null) {
            return;
        }
        //获取Token列表
        mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));
        mMainWalletFragmentViewModel.fetchRefreshToken(mDefaultWallet);
    }

    //刷新钱包相基础数据
    private void updateNormalWalletUi() {
        if (getContext() == null) {
            return;
        }

        QWAccount account = mDefaultWallet.getCurrentAccount();
        mAddressTextView.setText(QWWalletUtils.parseAddressTo8Show(mDefaultWallet.getCurrentShowAddress()));

        String name = account.getName();
        mNameTextView.setText(TextUtils.isEmpty(name) ? mDefaultWallet.getName() : name);
        String icon = account.getIcon();
        Glide.with(this)
                .asBitmap()
                .load(TextUtils.isEmpty(icon) ? mDefaultWallet.getIcon() : icon)
                .into(mCircleIcon);
        if (mDefaultWallet.getType() == Constant.WALLET_TYPE_HD) {
            Drawable dra = getResources().getDrawable(R.drawable.wallet_hd);
            dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
            mNameTextView.setCompoundDrawables(null, null, dra, null);

            int index = account.getPathAccountIndex();
            String pathIndex = index + "";
            if (index < 10) {
                pathIndex = "0" + index;
            }
            mHDAccountIndexView.setText(pathIndex);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setGradientType(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(5);
            drawable.setColor(Color.parseColor("#FFAE33"));
//                if (account.isAllBTC()) {
//                }
            mHDAccountIndexView.setBackground(drawable);
            mHDAccountIndexView.setVisibility(View.VISIBLE);
        } else {
            mNameTextView.setCompoundDrawables(null, null, null, null);
            mHDAccountIndexView.setVisibility(View.GONE);
        }

        if (1 == mDefaultWallet.getIsBackup() || mDefaultWallet.getIsWatch() == 1) {
            mBackupInfoView.setVisibility(View.GONE);
        } else {
            mBackupInfoView.setVisibility(View.VISIBLE);
        }
        if (mDefaultWallet.isWatch()) {
            mWatchWalletFlag.setVisibility(View.VISIBLE);
            if (mDefaultWallet.isLedger()) {
                mWatchWalletFlag.setImageResource(R.drawable.wallet_ledger);
            } else {
                mWatchWalletFlag.setImageResource(R.drawable.wallet_watch);
            }
        } else {
            mWatchWalletFlag.setVisibility(View.GONE);
        }

        mAddTokenView.setVisibility(View.VISIBLE);
        if (account.isQKC()) {
            mChainIdTextView.setVisibility(View.VISIBLE);
            mSharedIdTextView.setVisibility(View.VISIBLE);
            mMainCardView.setBackgroundResource(R.drawable.main_card_bg);

            mManagerView.setImageResource(R.drawable.wallet_switch_qkc_selected);
        } else if (account.isTRX()) {
            mChainIdTextView.setVisibility(View.INVISIBLE);
            mSharedIdTextView.setVisibility(View.INVISIBLE);
            mMainCardView.setBackgroundResource(R.drawable.main_card_trx_bg);

            mManagerView.setImageResource(R.drawable.wallet_switch_trx_selected);
        } else {
            mChainIdTextView.setVisibility(View.INVISIBLE);
            mSharedIdTextView.setVisibility(View.INVISIBLE);
            mMainCardView.setBackgroundResource(R.drawable.main_card_eth_bg);

            mManagerView.setImageResource(R.drawable.wallet_switch_eth_selected);
        }

        mPriceTextView.setText("0");
        String coinSymbol = SharedPreferencesUtils.getCurrentMarketCoin(requireContext());
        mPriceSymbolView.setText(ToolUtils.getCoinSymbol(coinSymbol));

        //QKC
        //chain
        if (account.isQKC()) {
            String allChain = SharedPreferencesUtils.getTotalChainCount(mContext);
            BigInteger totalCount = Numeric.toBigInt(allChain);
            String chain = SharedPreferencesUtils.getCurrentChain(MainApplication.getContext(), mDefaultWallet.getCurrentAddress());
            BigInteger currentChain = Numeric.toBigInt(chain);
            BigInteger mainChainId = currentChain.mod(totalCount);
            String chainId = String.format(getString(R.string.wallet_chain_id), mainChainId);
            mChainIdTextView.setText(chainId);
            //shared
            BigInteger totalShard = BigInteger.ONE;
            List<String> allShared = SharedPreferencesUtils.getTotalSharedSizes(mContext);
            if (allShared != null && currentChain.intValue() < allShared.size() && currentChain.intValue() >= 0) {
                totalShard = Numeric.toBigInt(allShared.get(currentChain.intValue()));
            }
            String mainShard = SharedPreferencesUtils.getCurrentShard(getContext(), mDefaultWallet.getCurrentAddress());
            BigInteger mainShardId = Numeric.toBigInt(mainShard).mod(totalShard);
            String share = String.format(getString(R.string.wallet_shard_id), mainShardId.toString());
            mSharedIdTextView.setText(share);
        }
    }

    private void updateTokenUi() {
        if (getContext() == null) {
            return;
        }

        if (mTokenAdapter != null) {
            TokenBean bean = TokenRepository.getMainTokenBean(requireContext(), mDefaultWallet.getCurrentAccount());
            if (mTokenAdapter.getData().isEmpty()) {
                ArrayList<TokenBean> list = new ArrayList<>();
                list.add(bean);
                mTokenAdapter.setNewInstance(list);
            } else {
                mTokenAdapter.getData().remove(0);
                mTokenAdapter.getData().add(0, bean);
                mTokenAdapter.notifyDataSetChanged();
            }
        }
    }

    private void updateCardPrice() {
        if (ToolUtils.isTestNetwork(mDefaultWallet.getCurrentAddress())) {
            mPriceTextView.setText("0");
            return;
        }
        String currentPriceType = SharedPreferencesUtils.getCurrentMarketCoin(requireContext());
        List<TokenBean> list = mTokenAdapter.getData();
        float totalPrice = 0;
        for (TokenBean bean : list) {
            if (bean.getToken() == null || bean.getBalance() == null) {
                continue;
            }

            QWToken token = bean.getToken();
            QWBalance balance = bean.getBalance();
            String count = QWWalletUtils.getIntTokenFromWei16(balance.getBalance(), token.getTokenUnit());
            float price = SharedPreferencesUtils.getCoinPrice(requireContext(), token.getSymbol().toLowerCase(), currentPriceType);
            price = Float.parseFloat(count) * price;

            totalPrice += price;
        }
        mPriceTextView.setText(ToolUtils.format8Number(totalPrice));
    }

    private void onRefresh() {
        if (mDefaultWallet == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (!ConnectionUtil.isInternetConnection(requireContext())) {
            mSwipeRefreshLayout.setRefreshing(false);
            MyToast.showSingleToastShort(requireActivity(), R.string.network_error);
            return;
        }

        //同步数据 获取Token数量
        pullToTokenInfo();
    }

    void refreshBalance() {
        //刷新分片 地址等信息
        updateNormalWalletUi();
        //获取QKC数量并显示
        mMainWalletFragmentViewModel.fetchRefreshToken(mDefaultWallet);
    }

    private void onPassword() {
        showProgress(false);
        goBackup();
    }

    private void onCoinPriceSuccess() {
        mTokenAdapter.notifyDataSetChanged();
        updateCardPrice();
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
        } else {
            mProgressView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTIONS) {
                isNeedLoad = true;
                if (mDefaultWallet != null) {
                    findWalterSuccess(mDefaultWallet);
                }
                return;
            }

            if (requestCode == Constant.REQUEST_CODE_ADD_TOKEN) {
                if (mMainWalletFragmentViewModel != null && mDefaultWallet != null) {
                    mMainWalletFragmentViewModel.fetchRefreshToken(mDefaultWallet);
                }
                return;
            }

            if (data != null) {
                if (requestCode == Constant.REQUEST_CODE_EDIT_WALLET) {
                    boolean isChooseNewWallet = data.getBooleanExtra(Constant.KEY_CHOOSE_NEW_WALLET, false);
                    boolean isChangeBTCAccount = data.getBooleanExtra(Constant.KEY_CHANGE_BTC_ACCOUNT, false);
                    if (isChooseNewWallet || isChangeBTCAccount) {
                        //切换了主钱包，则重新获取
                        isNeedLoad = true;
                    }
                    if (mMainWalletFragmentViewModel != null) {
                        //拉取钱包数据
                        mMainWalletFragmentViewModel.findWallet();
                    }
                } else if (requestCode == Constant.REQUEST_CODE_EDIT_ICON) {
                    //更新头像
                    String icon = data.getStringExtra(Constant.KEY_WALLET_ICON_PATH);
                    if (mDefaultWallet != null) {
                        //更新数据库
                        QWAccountDao walletDao = new QWAccountDao(requireContext());
                        walletDao.updateWalletIcon(icon, mDefaultWallet.getKey());
                    } else {
                        mUpdateWalletIcon = icon;
                    }

                    if (mMainWalletFragmentViewModel != null) {
                        QWWallet wallet = mMainWalletFragmentViewModel.findDefaultWalletObserve().getValue();
                        if (wallet != null && wallet.getCurrentAccount() != null) {
                            wallet.getCurrentAccount().setIcon(icon);
                        }

                        if (mDefaultWallet != null && mDefaultWallet.getCurrentAccount() != null) {
                            mDefaultWallet.getCurrentAccount().setIcon(icon);
                            Glide.with(this)
                                    .asBitmap()
                                    .load(icon)
                                    .into(mCircleIcon);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onItemClick(@NotNull BaseQuickAdapter adapter, @NotNull View view, int position) {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }
        TokenBean bean = (TokenBean) adapter.getData().get(position);
        QWToken token = bean.getToken();
        if (TextUtils.equals(token.getAddress(), QWTokenDao.TQKC_ADDRESS)) {
            //QKC
            Intent intent = new Intent(requireActivity(), TransactionDetailActivity.class);
            startActivity(intent);
            UmengStatistics.walletHomeDetailClickCount(requireContext(), mDefaultWallet.getCurrentAddress());
            return;
        } else if (TextUtils.equals(token.getSymbol(), QWTokenDao.ETH_SYMBOL)) {
            //ETH
            OtherTokenDetailActivity.startOtherTokenDetailActivity(requireActivity(), mDefaultWallet, QWTokenDao.getDefaultETHToken());
            UmengStatistics.walletHomeDetailClickCount(requireContext(), mDefaultWallet.getCurrentAddress());
            return;
        } else if (TextUtils.equals(token.getSymbol(), QWTokenDao.TRX_SYMBOL)) {
            //TRX
            OtherTokenDetailActivity.startOtherTokenDetailActivity(requireActivity(), mDefaultWallet, QWTokenDao.getDefaultTRXToken());
            UmengStatistics.walletHomeDetailClickCount(requireContext(), mDefaultWallet.getCurrentAddress());
            return;
        }

        if (bean.getToken().isNative() && mDefaultWallet.getCurrentAccount().isQKC()) {
            TransactionDetailActivity.startTokenActivity(requireActivity(), mDefaultWallet, bean);
        } else {
            OtherTokenDetailActivity.startOtherTokenDetailActivity(requireActivity(), mDefaultWallet, bean);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //取消注册
        RxBus.get().unRegister(this);
        EventBus.getDefault().unregister(this);
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void rxBusEventWallet(ChangeWalletEvent event) {
        if (null != event && getContext() != null) {
            String address = event.getMessage();
            QWAccountDao dao = new QWAccountDao(requireContext());
            QWAccount account = dao.queryByAddress(address);

            if (mDefaultWallet != null) {
                mDefaultWallet.setCurrentAccount(account);
                mDefaultWallet.setCurrentAddress(address);
            }
            QWWallet quarkWallet = mMainWalletFragmentViewModel.findDefaultWalletObserve().getValue();
            if (quarkWallet != null) {
                quarkWallet.setCurrentAccount(account);
                quarkWallet.setCurrentAddress(address);
            }

            updateNormalWalletUi();
            updateTokenUi();
            //刷新token
            mMainWalletFragmentViewModel.fetchDBToken(mDefaultWallet);
        }
    }

    @Subscribe(code = Constant.RX_BUS_CODE_TOKEN, threadMode = ThreadMode.MAIN)
    public void rxBusEventToken(String msg) {
        if (mMainWalletFragmentViewModel != null) {
            mMainWalletFragmentViewModel.fetchToken(mDefaultWallet);
        }
    }

    @Subscribe(code = Constant.RX_BUS_CODE_BACKUP_PHRASE, threadMode = ThreadMode.MAIN)
    public void rxBusEventBackup(String msg) {
        mDefaultWallet.setIsBackup(1);
        mBackupInfoView.setVisibility(View.GONE);
        QWWallet wallet = mMainWalletFragmentViewModel.findDefaultWalletObserve().getValue();
        if (wallet != null) {
            wallet.setIsBackup(1);
        }
    }

    //切换网络
    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void rxBusEventRecent(NetworkChangeEvent messageEvent) {
        isNeedLoad = true;
        findWalterSuccess(mDefaultWallet);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.updateFragmentNoWallet();
        }
    }
}


