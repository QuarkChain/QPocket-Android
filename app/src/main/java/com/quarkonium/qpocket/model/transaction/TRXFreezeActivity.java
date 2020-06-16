package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.utils.Convert;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.rx.SendFinishEvent;
import com.quarkonium.qpocket.tron.utils.Utils;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.view.SlidingTabLayout;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class TRXFreezeActivity extends BaseActivity {

    public static class TRXFreezePagerAdapter extends PagerAdapter {

        private String[] mTitles;

        public TRXFreezePagerAdapter(String[] titles) {
            mTitles = titles;
        }

        @Override
        public int getCount() {
            return mTitles != null ? mTitles.length : 0;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            return new TextView(container.getContext());
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }

    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    private NestedScrollView mNestedScrollView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private View mProgressView;

    private ProgressBar mBandWidthProgressBar;
    private TextView mAvailableBandWidthView;
    private TextView mFrozenBandWidth;

    private ProgressBar mEnergyProgressBar;
    private TextView mAvailableEnergyView;
    private TextView mFrozenEnergy;

    private View mFreezeLayout;
    private TextView mAccountBalanceView;
    private EditText mFreezeAmount;
    private TextView mFreezeTypeBandWidth;
    private TextView mFreezeTypeEnergy;

    private View mUnfreezeView;
    private View mUnFreezeLayout;
    private View mUnfreezeTypeBP;
    private View mUnfreezeTypeEnergy;
    private TextView mUnfreezeBandTime;
    private TextView mUnfreezeEnergyTime;

    private QWWallet mDefaultWallet;
    private BigInteger mMainTrxTokenCount;

    private boolean isPull;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_trx_freeze_layout;
    }

    @Override
    public int getActivityTitle() {
        return R.string.trx_freeze_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.trx_freeze_title);

        mNestedScrollView = findViewById(R.id.trx_scroll_view);
        mProgressView = findViewById(R.id.progress_layout);
        mSwipeRefreshLayout = findViewById(R.id.freeze_swipe_view);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        //带宽
        mBandWidthProgressBar = findViewById(R.id.freeze_band_seekbar);
        mAvailableBandWidthView = findViewById(R.id.freeze_band_current);
        mFrozenBandWidth = findViewById(R.id.freeze_band_value);

        //能量
        mEnergyProgressBar = findViewById(R.id.freeze_energy_seekbar);
        mAvailableEnergyView = findViewById(R.id.freeze_energy_current);
        mFrozenEnergy = findViewById(R.id.freeze_energy_value);

        //冻结/解冻
        mFreezeLayout = findViewById(R.id.freeze_layout);
        mUnFreezeLayout = findViewById(R.id.unfreeze_layout);
        ViewPager mViewPager = findViewById(R.id.freeze_view_page);
        String[] titles = getResources().getStringArray(R.array.trx_freeze_tags);
        TRXFreezePagerAdapter mAdapter = new TRXFreezePagerAdapter(titles);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 0) {
                    mUnFreezeLayout.setVisibility(View.GONE);
                    mFreezeLayout.setVisibility(View.VISIBLE);
                } else {
                    mUnFreezeLayout.setVisibility(View.VISIBLE);
                    mFreezeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        SlidingTabLayout mSlidingTabLayout = findViewById(R.id.freeze_tab_view);
        mSlidingTabLayout.setEndHasMargin(false);
        mSlidingTabLayout.setViewPager(mViewPager, titles);
        mSlidingTabLayout.setCurrentTab(0);


        //冻结
        View mFreezeView = findViewById(R.id.freeze_action);
        mFreezeView.setOnClickListener(v -> onFreeze());
        mAccountBalanceView = findViewById(R.id.freeze_total_balance);
        mFreezeAmount = findViewById(R.id.freeze_amount);
        mFreezeTypeBandWidth = findViewById(R.id.freeze_band_type);
        mFreezeTypeBandWidth.setOnClickListener((v) -> onChangeFreezeTypeBand());
        mFreezeTypeBandWidth.setSelected(true);
        mFreezeTypeEnergy = findViewById(R.id.freeze_energy_type);
        mFreezeTypeEnergy.setOnClickListener(v -> onChangeFreezeTypeEnergy());

        //解冻
        mUnfreezeView = findViewById(R.id.unfreeze_action);
        mUnfreezeView.setEnabled(false);
        mUnfreezeView.setOnClickListener(v -> onUnFreeze());
        mUnfreezeTypeBP = findViewById(R.id.unfreeze_band_type);
        mUnfreezeTypeBP.setSelected(true);
        mUnfreezeTypeBP.setOnClickListener(v -> onChangeUnFreezeTypeBand());
        mUnfreezeTypeEnergy = findViewById(R.id.unfreeze_energy_type);
        mUnfreezeTypeEnergy.setOnClickListener(v -> onChangeUnFreezeTypeEnergy());
        mUnfreezeBandTime = findViewById(R.id.unfreeze_band_time_title);
        mUnfreezeEnergyTime = findViewById(R.id.unfreeze_energy_time_title);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        mMainTrxTokenCount = BigInteger.ZERO;

        mTransactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                .get(TransactionViewModel.class);
        mTransactionViewModel.findDefaultWalletObserve().observe(this, this::onFindWalletSuccess);

        mTransactionViewModel.unfreezeCostBandWidth().observe(this, this::onUnFreezeBandwidthSuccess);
        mTransactionViewModel.costBandWidth().observe(this, this::onBandWidthSuccess);
        mTransactionViewModel.sendObserve().observe(this, v -> onSendSuccess());
        mTransactionViewModel.onTrxSendError().observe(this, v -> onError());

        mTransactionViewModel.accountDataObserve().observe(this, v -> {
            //重新查询数据库 刷新数据
            QWAccountDao dao = new QWAccountDao(getApplicationContext());
            QWAccount account = dao.queryByAddress(mDefaultWallet.getCurrentAddress());
            mDefaultWallet.setCurrentAccount(account);
            onFindWalletSuccess(mDefaultWallet);
            mSwipeRefreshLayout.setRefreshing(false);
        });
        mTransactionViewModel.findWallet();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void onRefresh() {
        if (mDefaultWallet == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            mSwipeRefreshLayout.setRefreshing(false);
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }
        mTransactionViewModel.getAccountData(mDefaultWallet);
    }

    private void onChangeFreezeTypeBand() {
        mFreezeTypeBandWidth.setSelected(true);
        mFreezeTypeEnergy.setSelected(false);
    }

    private void onChangeFreezeTypeEnergy() {
        mFreezeTypeBandWidth.setSelected(false);
        mFreezeTypeEnergy.setSelected(true);
    }

    private void onChangeUnFreezeTypeBand() {
        mUnfreezeTypeBP.setSelected(true);
        mUnfreezeTypeEnergy.setSelected(false);

        if (mDefaultWallet == null) {
            return;
        }
        long time = getFreezeBandTime(mDefaultWallet.getCurrentAddress());
        if (time == 0) {
            mUnfreezeView.setEnabled(false);
        } else {
            mUnfreezeView.setEnabled(true);
        }
    }

    private void onChangeUnFreezeTypeEnergy() {
        mUnfreezeTypeBP.setSelected(false);
        mUnfreezeTypeEnergy.setSelected(true);

        if (mDefaultWallet == null) {
            return;
        }
        long time = getFreezeEnergyTime(mDefaultWallet.getCurrentAddress());
        if (time == 0) {
            mUnfreezeView.setEnabled(false);
        } else {
            mUnfreezeView.setEnabled(true);
        }
    }

    //*********************
    //解冻
    private void onUnFreeze() {
        if (mDefaultWallet == null) {
            return;
        }

        if (mUnfreezeTypeBP.isSelected()) {
            long time = getFreezeBandTime(mDefaultWallet.getCurrentAddress());
            if (time > System.currentTimeMillis()) {
                MyToast.showSingleToastShort(this, R.string.trx_unfreeze_end_time);
                return;
            }
        } else {
            long time = getFreezeEnergyTime(mDefaultWallet.getCurrentAddress());
            if (time > System.currentTimeMillis()) {
                MyToast.showSingleToastShort(this, R.string.trx_unfreeze_end_time);
                return;
            }
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        showProgress(true);
        mTransactionViewModel.getUnfreezeTrxCost(mDefaultWallet.getKey(), mDefaultWallet.getCurrentAddress(),
                mUnfreezeTypeBP.isSelected() ? Contract.ResourceCode.BANDWIDTH : Contract.ResourceCode.ENERGY);
    }

    private void onUnFreezeBandwidthSuccess(Integer bandwidthCost) {
        showProgress(false);
        if (mDefaultWallet == null) {
            return;
        }

        GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(this, mDefaultWallet.getCurrentAddress());
        long bandwidthNormal = accountNetMessage.getNetLimit() - accountNetMessage.getNetUsed();
        long bandwidthFree = accountNetMessage.getFreeNetLimit() - accountNetMessage.getFreeNetUsed();
        boolean enoughBandwidth = bandwidthNormal >= bandwidthCost || bandwidthFree >= bandwidthCost;
        if (!enoughBandwidth) {
            showUnfreezeCostInfo(String.valueOf(bandwidthCost / 100000D));
        } else {
            //普通钱包
            onCheckPassword();
        }
    }

    private void showUnfreezeCostInfo(String cost) {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.trx_freeze_cost_title);
        dialog.setMessage(String.format(getString(R.string.trx_unfreeze_cost_msg), cost));
        dialog.setPositiveBtn(R.string.ok, v -> {
            onCheckPassword();
            dialog.dismiss();
        });
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.show();
    }

    //*********************
    //冻结
    private void onFreeze() {
        if (mDefaultWallet == null) {
            return;
        }

        if (TextUtils.isEmpty(mFreezeAmount.getText())) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_amount_error);
            mFreezeAmount.requestFocus();
            return;
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        //检测amount是否大于0
        String amountStr = mFreezeAmount.getText().toString().trim();
        BigDecimal amount = Convert.toWei(amountStr, Convert.Unit.SUN);
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_amount_error);
            mFreezeAmount.requestFocus();
            return;
        }
        //检测余额
        //转账数量+手续费 < 当前分片总数
        if (amount.toBigInteger().compareTo(mMainTrxTokenCount) > 0) {
            //余额不足
            MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
            mFreezeAmount.requestFocus();
            return;
        }

        showProgress(true);
        String tokenCount = mFreezeAmount.getText().toString().trim();
        mTransactionViewModel.getFreezeTrxCost(mDefaultWallet.getKey(), mDefaultWallet.getCurrentAddress(), Double.parseDouble(tokenCount),
                mFreezeTypeBandWidth.isSelected() ? Contract.ResourceCode.BANDWIDTH : Contract.ResourceCode.ENERGY);
    }

    //获取带宽花费成功
    private void onBandWidthSuccess(Integer bandwidthCost) {
        showProgress(false);
        if (mDefaultWallet == null) {
            return;
        }
        GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(this, mDefaultWallet.getCurrentAddress());
        long bandwidthNormal = accountNetMessage.getNetLimit() - accountNetMessage.getNetUsed();
        long bandwidthFree = accountNetMessage.getFreeNetLimit() - accountNetMessage.getFreeNetUsed();

        boolean enoughBandwidth = bandwidthNormal >= bandwidthCost || bandwidthFree >= bandwidthCost;
        if (!enoughBandwidth) {
            String amountStr = mFreezeAmount.getText().toString().trim();
            BigDecimal amount = Convert.toWei(amountStr, Convert.Unit.SUN);
            //校验带宽，能量
            BigDecimal cost = new BigDecimal(bandwidthCost);
            BigDecimal totalCost = cost.add(amount);
            //转账数量+手续费 < 当前分片总数
            if (totalCost.toBigInteger().compareTo(mMainTrxTokenCount) > 0) {
                //余额不足
                MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
                mFreezeAmount.requestFocus();
                return;
            }
            showCostInfo(String.valueOf(bandwidthCost / 100000D));
        } else {
            //普通钱包
            onCheckPassword();
        }
    }

    private void showCostInfo(String cost) {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.trx_freeze_cost_title);
        dialog.setMessage(String.format(getString(R.string.trx_not_enough_cost_msg), cost));
        dialog.setPositiveBtn(R.string.ok, v -> {
            onCheckPassword();
            dialog.dismiss();
        });
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.show();
    }

    //**********提交清单***********
    private void onCheckPassword() {
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mDefaultWallet,
                new SystemUtils.OnCheckPassWordListenerImp() {
                    @Override
                    public void onPasswordSuccess(String password) {
                        onCreateTransaction(password);
                    }
                });
    }


    private void onCreateTransaction(String password) {
        showProgress(true);
        if (mFreezeLayout.getVisibility() == View.VISIBLE) {
            String tokenCount = mFreezeAmount.getText().toString().trim();
            mTransactionViewModel.freezeTrx(
                    password,
                    mDefaultWallet.getCurrentAddress(),
                    Double.parseDouble(tokenCount),
                    mFreezeTypeBandWidth.isSelected() ? Contract.ResourceCode.BANDWIDTH : Contract.ResourceCode.ENERGY
            );
        } else {
            mTransactionViewModel.trxUnfreeze(
                    password,
                    mDefaultWallet.getCurrentAddress(),
                    mUnfreezeTypeBP.isSelected() ? Contract.ResourceCode.BANDWIDTH : Contract.ResourceCode.ENERGY
            );
        }
    }

    private void onError() {
        showProgress(false);
        MyToast.showSingleToastShort(this, R.string.transaction_send_fail);
    }

    private void onSendSuccess() {
        isPull = true;
        showProgress(false);
        mNestedScrollView.smoothScrollTo(0, 0);
        mNestedScrollView.postDelayed(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            mFreezeAmount.setText("");
            mFreezeAmount.requestFocus();
            onRefresh();
            MyToast.showSingleToastShort(TRXFreezeActivity.this, R.string.transaction_send_success);
        }, 100);

        if (mFreezeLayout.getVisibility() == View.VISIBLE) {
            UmengStatistics.walletTrxFreezeClickCount(getApplicationContext());
        } else {
            UmengStatistics.walletTrxUnfreezeClickCount(getApplicationContext());
        }
    }
    //*********************

    //更新UI
    private void onFindWalletSuccess(QWWallet wallet) {
        if (wallet == null) {
            return;
        }

        mDefaultWallet = wallet;

        updateBandWidth(wallet.getCurrentAddress());
        updateEnergy(wallet.getCurrentAddress());

        //更新可用余额
        mAccountBalanceView.setText(String.format(getString(R.string.trx_balance), "0"));
        ForeignCollection<QWBalance> collection = mDefaultWallet.getCurrentAccount().getBalances();
        if (collection == null || collection.isEmpty()) {
            return;
        }
        for (QWBalance balance : collection) {
            if (balance.getQWToken() != null && QWTokenDao.TRX_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                mMainTrxTokenCount = Numeric.toBigInt(balance.getBalance());
                String mainToken = QWWalletUtils.getIntTokenFromSun16(balance.getBalance());
                mAccountBalanceView.setText(String.format(getString(R.string.trx_balance), mainToken));
            }
        }
    }

    private void updateBandWidth(String address) {
        GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(getApplicationContext(), address);
        long bandwidth = accountNetMessage.getNetLimit() + accountNetMessage.getFreeNetLimit();
        long bandwidthUsed = accountNetMessage.getNetUsed() + accountNetMessage.getFreeNetUsed();
        long currentBandwidth = bandwidth - bandwidthUsed;

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        mAvailableBandWidthView.setText(String.format(getString(R.string.trx_freeze_band_width_avaliable), currentBandwidth, bandwidth));
        int progress = bandwidth == 0 ? 0 : Math.round(currentBandwidth * 1f / bandwidth * 1000);
        mBandWidthProgressBar.setProgress(progress == 0 ? 0 : progress + 5);
        mBandWidthProgressBar.setSecondaryProgress(progress);

        List<Protocol.Account.Frozen> frozenList = Utils.getFrozenBandWidth(getApplicationContext(), address);
        long freezed = 0;
        long time = 0;
        if (frozenList != null && !frozenList.isEmpty()) {
            for (Protocol.Account.Frozen frozen : frozenList) {
                freezed += frozen.getFrozenBalance();
                if (time < frozen.getExpireTime()) {
                    time = frozen.getExpireTime();
                }
            }
        }
        mFrozenBandWidth.setText(String.format(getString(R.string.trx_freeze_value), numberFormat.format(freezed / 1000000L)));
        if (time == 0) {
            mUnfreezeBandTime.setText(String.format(getString(R.string.trx_unfreeze_band_time), Constant.NONE));
            mUnfreezeBandTime.setTextColor(getResources().getColor(R.color.text_hint));
            if (mUnfreezeTypeBP.isSelected()) {
                mUnfreezeView.setEnabled(false);
            }
        } else {
            String timeStr = QWWalletUtils.parseFullTimeFor16(Numeric.toHexStringWithPrefix(new BigInteger("" + time)));
            mUnfreezeBandTime.setText(String.format(getString(R.string.trx_unfreeze_band_time), timeStr));
            mUnfreezeBandTime.setTextColor(getResources().getColor(R.color.text_title));
            if (mUnfreezeTypeBP.isSelected()) {
                mUnfreezeView.setEnabled(true);
            }
        }
    }

    private void updateEnergy(String address) {
        GrpcAPI.AccountResourceMessage accountNetMessage = Utils.getAccountRes(getApplicationContext(), address);
        long energy = accountNetMessage.getEnergyLimit();
        long energyUsed = accountNetMessage.getEnergyUsed();
        long currentEnergy = energy - energyUsed;
        if (currentEnergy < 0) {
            currentEnergy = 0;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        mAvailableEnergyView.setText(String.format(getString(R.string.trx_freeze_energy_available), currentEnergy, energy));
        int progress = energy == 0 ? 0 : Math.round(currentEnergy * 1f / energy * 1000);
        mEnergyProgressBar.setProgress(progress == 0 ? 0 : progress + 5);
        mEnergyProgressBar.setSecondaryProgress(progress);

        Protocol.Account.Frozen frozen = Utils.getFrozenEnergy(getApplicationContext(), address);
        if (frozen != null && frozen.getFrozenBalance() > 0) {
            String value = String.format(getString(R.string.trx_freeze_value), numberFormat.format(frozen.getFrozenBalance() / 1000000L));
            mFrozenEnergy.setText(value);
            String time = QWWalletUtils.parseFullTimeFor16(Numeric.toHexStringWithPrefix(new BigInteger("" + frozen.getExpireTime())));
            mUnfreezeEnergyTime.setTextColor(getResources().getColor(R.color.text_title));
            mUnfreezeEnergyTime.setText(String.format(getString(R.string.trx_unfreeze_energy_time), time));
            if (mUnfreezeTypeEnergy.isSelected()) {
                mUnfreezeView.setEnabled(true);
            }
        } else {
            mFrozenEnergy.setText(R.string.trx_freeze_default_value);
            mUnfreezeEnergyTime.setTextColor(getResources().getColor(R.color.text_hint));
            mUnfreezeEnergyTime.setText(String.format(getString(R.string.trx_unfreeze_energy_time), Constant.NONE));
            if (mUnfreezeTypeEnergy.isSelected()) {
                mUnfreezeView.setEnabled(false);
            }
        }
    }

    private long getFreezeEnergyTime(String address) {
        Protocol.Account.Frozen frozen = Utils.getFrozenEnergy(getApplicationContext(), address);
        if (frozen != null && frozen.getFrozenBalance() > 0) {
            return frozen.getExpireTime();
        }
        return 0;
    }

    private long getFreezeBandTime(String address) {
        List<Protocol.Account.Frozen> frozenList = Utils.getFrozenBandWidth(getApplicationContext(), address);
        long time = 0;
        if (frozenList != null && !frozenList.isEmpty()) {
            for (Protocol.Account.Frozen frozen : frozenList) {
                if (time < frozen.getExpireTime()) {
                    time = frozen.getExpireTime();
                }
            }
        }
        return time;
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
        } else {
            mProgressView.setVisibility(View.GONE);
        }
    }

    @Override
    public void finish() {
        if (isPull) {
            setResult(Activity.RESULT_OK);
        }
        super.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void rxBusEventChange(SendFinishEvent event) {
        onSendSuccess();
    }
}
