package com.quarkonium.qpocket.model.transaction;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Convert;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.main.CaptureActivity;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.main.view.SpinnerPopWindow;
import com.quarkonium.qpocket.model.permission.PermissionHelper;
import com.quarkonium.qpocket.model.transaction.bean.EthGas;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.tron.utils.Utils;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.JustifyTextView;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.view.WheelBalancePopWindow;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.bean.EditDecimalInputFilter;
import com.quarkonium.qpocket.model.book.AddressBookActivity;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.quarkonium.qpocket.view.SwitchButton;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.jetbrains.annotations.NotNull;
import org.tron.api.GrpcAPI;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.disposables.Disposable;

/**
 * 交易详情列表界面
 */
public class TransactionCreateActivity extends BaseActivity {
    private class MyTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            check();
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mFromAddressByte != null) {
                checkBandwidth(true);
            } else {
                checkGasLimit(true);
            }
        }
    }

    private class MyGasPriceWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            check();
            if (mEthGas != null) {
                mGasSlowLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
                mGasSlowLayoutView.setSelected(false);
                mGasAvgLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
                mGasAvgLayoutView.setSelected(false);
                mGasFastLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
                mGasFastLayoutView.setSelected(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class MyGasLimitWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(s)) {
                mSendView.setEnabled(false);
            }
            check();

            //更新GasLimit时同步刷新ETH gas 卡片
            if (mEthGas != null && mDefaultWallet != null && WalletUtils.isValidAddress(mDefaultWallet.getCurrentAddress())) {
                updateGasPriceByLimit(s.toString());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class MyAmountTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(s)) {
                mSendView.setEnabled(false);
                mAmountPriceView.setText("");
            } else {
                String str = s.toString().trim();
                if (str.startsWith(".")) {
                    s = "0.";
                    mAmountText.setText(s);
                    mAmountText.setSelection(2);
                } else if (str.startsWith(",")) {
                    s = "0,";
                    mAmountText.setText(s);
                    mAmountText.setSelection(2);
                }

                if (str.contains(".") && s.length() - 1 - s.toString().indexOf(".") > mDecimalDigits) {
                    s = s.toString().subSequence(0, s.toString().indexOf(".") + mDecimalDigits + 1);
                    mAmountText.setText(s);
                    mAmountText.setSelection(s.length());
                } else if (str.contains(",") && s.length() - 1 - s.toString().indexOf(",") > mDecimalDigits) {
                    s = s.toString().subSequence(0, s.toString().indexOf(",") + mDecimalDigits + 1);
                    mAmountText.setText(s);
                    mAmountText.setSelection(s.length());
                }

                String address = !TextUtils.isEmpty(mFromString) ? mFromString : getCurrentAddress();
                if (!TextUtils.isEmpty(address)) {
                    String priceStr = ToolUtils.getTokenCurrentCoinPriceText(getApplicationContext(), address, mSymbol, s.toString());
                    mAmountPriceView.setText(priceStr);
                }

                check();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mFromAddressByte != null) {
                checkBandwidth(false);
            } else {
                checkGasLimit(false);
            }
        }
    }

    private String getCurrentAddress() {
        if (mDefaultWallet != null) {
            return mDefaultWallet.getCurrentAddress();
        }
        return "";
    }

    private void check() {
        if (mIsTrx) {
            //trx
            if (TextUtils.isEmpty(mAmountText.getText())
                    || TextUtils.isEmpty(mToAddressText.getText())) {
                mSendView.setEnabled(false);
            } else {
                mSendView.setEnabled(true);
            }
        } else {
            CharSequence gas = mGasEditText.getText();
            CharSequence gasLimit = mGasLimitText.getText();
            if (TextUtils.isEmpty(mAmountText.getText())
                    || TextUtils.isEmpty(mToAddressText.getText())
                    || TextUtils.isEmpty(gasLimit)
                    || TextUtils.isEmpty(gas)
                    || !isNumber(gas)
                    || Double.parseDouble(gas.toString()) <= 0) {
                mSendView.setEnabled(false);
            } else {
                mSendView.setEnabled(true);
            }

            if (mDefaultWallet != null) {
                if (QWWalletUtils.isQKCValidAddress(mDefaultWallet.getCurrentAddress())) {
                    String gasToken = mGasToken != null ? mGasToken.getSymbol() : QWTokenDao.QKC_SYMBOL;
                    //QKC
                    if (!TextUtils.isEmpty(gas) && !TextUtils.isEmpty(gasLimit)) {
                        BigInteger gasBig = QWWalletUtils.toGWeiFrom10(gas.toString());
                        BigInteger gasLimitBig = new BigInteger(gasLimit.toString());
                        BigInteger cost = gasBig.multiply(gasLimitBig);
                        String costStr = QWWalletUtils.getIntTokenFromWei10(cost.toString(), true, Constant.QKC_DECIMAL_NUMBER);
                        mGasCostTextView.setText(costStr);
                        mGasCostPriceTextView.setText(ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), gasToken, costStr));
                    } else {
                        mGasCostTextView.setText("0");
                        mGasCostPriceTextView.setText(ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), gasToken, "0"));
                    }
                } else {
                    //ETH
                    if (!TextUtils.isEmpty(gas) && !TextUtils.isEmpty(gasLimit)) {
                        BigInteger gasBig = QWWalletUtils.toGWeiFrom10(gas.toString());
                        BigInteger gasLimitBig = new BigInteger(gasLimit.toString());
                        BigInteger cost = gasBig.multiply(gasLimitBig);
                        String costStr = QWWalletUtils.getIntTokenFromWei10(cost.toString(), true);
                        float count = Float.parseFloat(costStr);

                        mGasCostTextView.setText(ToolUtils.format8Number(count));
                        mGasCostPriceTextView.setText(ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, costStr));
                    } else {
                        mGasCostTextView.setText("0");
                        mGasCostPriceTextView.setText(ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, "0"));
                    }
                }
            } else {
                mGasCostTextView.setText("");
                mGasCostPriceTextView.setText("");
            }
        }
    }

    private boolean isNumber(CharSequence value) {
        try {
            Double.parseDouble(value.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static class MyHandler extends Handler {
        WeakReference<TransactionCreateActivity> mActivity;

        MyHandler(TransactionCreateActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            TransactionCreateActivity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                if (msg.what == HANDLER_WHAT) {
                    activity.checkGasLimit(false);
                } else if (msg.what == HANDLER_WHAT_TRX) {
                    activity.checkBandwidth(false);
                }
            }
        }
    }

    private static class ViewHolder {
        private ImageView iv;
        private TextView tv;
    }

    public static class SpinnerAdapter extends BaseAdapter {

        private ArrayList<QWBalance> mList;
        private String mGasToken;

        public SpinnerAdapter(ArrayList<QWBalance> list, String token) {
            this.mList = list;
            this.mGasToken = token;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.spinner_gas_symbol_item, null);

                holder = new ViewHolder();
                holder.iv = convertView.findViewById(R.id.gas_symbol_selected);
                holder.tv = convertView.findViewById(R.id.gas_symbol_text);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            QWToken token = mList.get(position).getQWToken();
            holder.tv.setText(token.getSymbol().toUpperCase());
            if (TextUtils.equals(mGasToken, token.getSymbol().toUpperCase())) {
                holder.iv.setVisibility(View.VISIBLE);
            } else {
                holder.iv.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    private static final int DECIMAL_DIGITS = 14;
    private static final int HANDLER_WHAT = 1;
    private static final int HANDLER_WHAT_TRX = 2;
    private static final int HANDLER_TIME = 1000;
    private static final String CURRENT_ADDRESS = "current_address";

    private int mDecimalDigits = DECIMAL_DIGITS;

    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    private QWWallet mDefaultWallet;
    private QWToken mToken;

    private TextView mTotalText;
    private JustifyTextView mFromAddressText;

    private EditText mAmountText;
    private EditText mToAddressText;
    private EditText mGasEditText;
    private EditText mGasLimitText;
    private String mToTempAddress;
    private String mTempAmount;

    private TextView mGasCostTextView;
    private TextView mGasCostPriceTextView;

    private View mGasSlowFastLayout;
    //slow
    private View mGasSlowLayoutView;
    private TextView mGasSlowTimeView;
    private TextView mGasSlowCountView;
    private TextView mGasSlowPriceView;
    //avg
    private View mGasAvgLayoutView;
    private TextView mGasAvgTimeView;
    private TextView mGasAvgCountView;
    private TextView mGasAvgPriceView;
    //fast
    private View mGasFastLayoutView;
    private TextView mGasFastTimeView;
    private TextView mGasFastCountView;
    private TextView mGasFastPriceView;

    private View mGasFailLayout;

    private View mGasAdjustLayout;

    private View mGasProgressView;
    private EthGas mEthGas;


    private TextView mAmountPriceView;

    private TextView mGasSymbolText;
    private QWToken mGasToken;

    private View mSendView;
    private View mProgressLayout;

    private SwipeRefreshLayout mSwipeLayout;

    private BigInteger mMainQKCTokenCount;
    private BigInteger mTokenCount;
    private ArrayList<QWBalance> mGasTokenList;
    private SpinnerPopWindow mMenuPopWindow;

    private String mFromString;
    private boolean mShowToast;

    private MyHandler mHandler;
    private String mCurrentGas = Constant.DEFAULT_GAS_PRICE_TO_GWEI;

    private String mCurrentAddress;

    private boolean mIsTrx;
    private TextView mTotalBandWidthView;
    private TextView mCostBandWidthView;
    private double mCostBandWidth;
    private byte[] mFromAddressByte;

    private String mSymbol;

    private double mSendAllCostBandWidth = -1;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_transaction_create;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_transaction_send_create_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        String toAddress = getIntent().getStringExtra(Constant.WALLET_ADDRESS);
        String amount = getIntent().getStringExtra(Constant.KEY_BALANCE);
        mFromString = getIntent().getStringExtra(Constant.KEY_FROM);
        mShowToast = getIntent().getBooleanExtra(Constant.KEY_NOT_TOAST, false);

        mToken = getIntent().getParcelableExtra(Constant.KEY_TOKEN);
        String tokenAddress = getIntent().getStringExtra(Constant.KEY_TOKEN_ADDRESS);
        initToken(tokenAddress);

        mGasCostTextView = findViewById(R.id.transaction_send_gas_cost);
        mGasCostPriceTextView = findViewById(R.id.transaction_send_gas_cost_price);

        mGasSlowFastLayout = findViewById(R.id.transaction_gas_fast_low_layout);
        //slow
        mGasSlowLayoutView = findViewById(R.id.transaction_gas_slow);
        mGasSlowLayoutView.setOnClickListener(v -> onClickSlow());
        mGasSlowTimeView = findViewById(R.id.transaction_safe_low_time);
        mGasSlowCountView = findViewById(R.id.transaction_safe_low_gas);
        mGasSlowPriceView = findViewById(R.id.transaction_safe_low_price);
        //avg
        mGasAvgLayoutView = findViewById(R.id.transaction_gas_avg);
        mGasAvgLayoutView.setOnClickListener(v -> onClickAvg());
        mGasAvgTimeView = findViewById(R.id.transaction_avg_time);
        mGasAvgCountView = findViewById(R.id.transaction_avg_gas);
        mGasAvgPriceView = findViewById(R.id.transaction_avg_price);
        //fast
        mGasFastLayoutView = findViewById(R.id.transaction_gas_fast);
        mGasFastLayoutView.setOnClickListener(v -> onClickFast());
        mGasFastTimeView = findViewById(R.id.transaction_fast_time);
        mGasFastCountView = findViewById(R.id.transaction_fast_gas);
        mGasFastPriceView = findViewById(R.id.transaction_fast_price);

        mGasProgressView = findViewById(R.id.gas_progress_layout);
        mGasFailLayout = findViewById(R.id.gas_progress_fail_layout);
        findViewById(R.id.gas_fail_reload).setOnClickListener(v -> {
            if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
                MyToast.showSingleToastShort(this, R.string.network_error);
                return;
            }
            mGasProgressView.setVisibility(View.VISIBLE);
            mGasFailLayout.setVisibility(View.GONE);
            mTransactionViewModel.ethSlowFastGas();
        });

        mGasAdjustLayout = findViewById(R.id.transaction_gas_adjust_layout);
        SwitchButton mAdjustGasButton = findViewById(R.id.transaction_gas_adjust_toogle);
        mAdjustGasButton.setChecked(false);
        mAdjustGasButton.setOnCheckedChangeListener((SwitchButton buttonView, boolean on) -> {
            if (on) {
                mGasAdjustLayout.setVisibility(View.VISIBLE);
                mGasAdjustLayout.post(() -> {
                    NestedScrollView scrollView = findViewById(R.id.transaction_scroll);
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                });
            } else {
                mGasAdjustLayout.setVisibility(View.GONE);
            }
        });

        mTopBarView.setTitle(R.string.wallet_transaction_send_create_title);
        mTopBarView.setRightImage(R.drawable.ic_action_scan);
        mTopBarView.setRightImageClickListener(v -> onQRScan());

        findViewById(R.id.transaction_send_address_book).setOnClickListener(v -> openAddressBook());
        mSendView = findViewById(R.id.account_action_next);
        mSendView.setOnClickListener(v -> onPreCreate());
        mSendView.setEnabled(false);

        mTotalText = findViewById(R.id.transaction_send_total_token);
        mFromAddressText = findViewById(R.id.transaction_from_address);

        mAmountPriceView = findViewById(R.id.transaction_send_amount_price);
        mAmountText = findViewById(R.id.transaction_send_amount);
        mAmountText.addTextChangedListener(new MyAmountTextWatcher());

        mToAddressText = findViewById(R.id.transaction_send_address);
        mToAddressText.addTextChangedListener(new MyTextWatcher());

        mGasEditText = findViewById(R.id.transaction_send_gas);
        mGasEditText.addTextChangedListener(new MyGasPriceWatcher());

        mGasLimitText = findViewById(R.id.transaction_send_gas_limit);
        mGasLimitText.addTextChangedListener(new MyGasLimitWatcher());

        mGasSymbolText = findViewById(R.id.tx_gas_token_symbol);
        mGasSymbolText.setText(getString(R.string.qkc));
        mGasSymbolText.setOnClickListener(this::changeGasToken);
        //默认都用QKC做转账手续费
        mGasToken = QWTokenDao.getTQKCToken();

        mSymbol = QWTokenDao.QKC_SYMBOL;
        mGasEditText.setText(QWWalletUtils.getIntTokenFromWei10(Constant.DEFAULT_GAS_PRICE, Convert.Unit.GWEI, 0));
        if (isSendToken()) {
            if (!isQKCNativeToken()) {
                mGasLimitText.setText(Constant.DEFAULT_GAS_TOKEN_LIMIT);
            }

            TextView textView = findViewById(R.id.transaction_send_token_title);
            textView.setText(mToken.getSymbol().toUpperCase());
            mSymbol = mToken.getSymbol().toLowerCase();
        }
        mToAddressText.setText(toAddress);
        mFromAddressText.setText(mFromString);
        if (!TextUtils.isEmpty(amount)) {
            mAmountText.setText(amount);
            mAmountText.setSelection(amount.length());
        }
        if (!TextUtils.isEmpty(mToTempAddress)) {
            mToAddressText.setText(mToTempAddress);
            mToTempAddress = "";
        }
        if (!TextUtils.isEmpty(mTempAmount)) {
            mAmountText.setText(mTempAmount);
            mTempAmount = "";
        }

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));

        mSwipeLayout = findViewById(R.id.transaction_swipe_view);
        mSwipeLayout.setOnRefreshListener(this::onRefresh);

        findViewById(R.id.transaction_send_all).setOnClickListener(v -> sendAll());

        //是否为trx币种
        if (!TextUtils.isEmpty(toAddress)) {
            mIsTrx = TronWalletClient.isTronAddressValid(toAddress);
        } else if (!TextUtils.isEmpty(mFromString)) {
            mIsTrx = TronWalletClient.isTronAddressValid(mFromString);
        } else {
            String address = getIntent().getStringExtra(Constant.CURRENT_ACCOUNT_ADDRESS);
            if (!TextUtils.isEmpty(address)) {
                mIsTrx = TronWalletClient.isTronAddressValid(address);
            }
        }
        if (mIsTrx) {
            //更新trx ui
            updateBandWidthUi();
        }
    }

    private void initToken(String tokenAddress) {
        if (mToken == null && !TextUtils.isEmpty(tokenAddress)) {
            QWTokenDao dao = new QWTokenDao(getApplicationContext());
            mToken = dao.queryTokenByAddress(tokenAddress);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentAddress = savedInstanceState.getString(CURRENT_ADDRESS);
        }
        mHandler = new MyHandler(this);

        mTransactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                .get(TransactionViewModel.class);
        mTransactionViewModel.findDefaultWalletObserve().observe(this, this::findWalterSuccess);
        mTransactionViewModel.createSendObserve().observe(this, this::createSuccess);
        mTransactionViewModel.progress().observe(this, this::showProgress);
        mTransactionViewModel.error().observe(this, v -> showProgress(false));
        mTransactionViewModel.createSendFailObserve().observe(this, v -> {
            showProgress(false);
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_send_fail);
        });

        mTransactionViewModel.gasPriceObserve().observe(this, this::onGasSuccess);
        mTransactionViewModel.gasLimitObserve().observe(this, this::onGasLimitSuccess);
        mTransactionViewModel.costBandWidth().observe(this, this::onBandWidthSuccess);
        mTransactionViewModel.ethGas().observe(this, this::onEthGasSuccess);

        mTransactionViewModel.checkFirstGasToken().observe(this, this::checkFirstGasToken);
        mTransactionViewModel.checkGasToken().observe(this, this::onCheckGasToken);

        mTransactionViewModel.firstCostBandWidth().observe(this, this::onFirstBandWidthSuccess);

        mTransactionViewModel.accountDataObserve().observe(this, this::accountSuccess);

        //获取当前主钱包
        mTransactionViewModel.findWallet();
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
                            MyToast.showSingleToastLong(this, PermissionHelper.getPermissionToast(getApplicationContext(), name));
                        }
                    });
            return;
        }

        startCamera();
    }

    private void startCamera() {
        String contractAddress = isSendToken() ? mToken.getAddress() : "";
        CaptureActivity.startForResultActivity(this, mDefaultWallet, contractAddress, mDefaultWallet.getCurrentAccount().getType());

        UmengStatistics.topBarScanQRCodeClickCount(this, mDefaultWallet.getCurrentAddress());
        String symbol = isSendToken() ? mToken.getSymbol() : getWalletSymbol();
        UmengStatistics.qrSendTokenClick(this, symbol);
    }

    private String getWalletSymbol() {
        if (mDefaultWallet.getCurrentAccount().isEth()) {
            return "eth";
        } else if (mDefaultWallet.getCurrentAccount().isTRX()) {
            return "trx";
        } else {
            return "qkc";
        }
    }

    private void openAddressBook() {
        if (mDefaultWallet == null) {
            return;
        }
        AddressBookActivity.startActivity(this, mDefaultWallet.getCurrentAccount().getType());
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_ADDRESS, mCurrentAddress);
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentAddress = savedInstanceState.getString(CURRENT_ADDRESS);
    }

    private void findWalterSuccess(QWWallet wallet) {
        mDefaultWallet = wallet;
        mCurrentAddress = wallet.getCurrentAddress();

        if (QWWalletUtils.isQKCValidAddress(mCurrentAddress)) {
            mGasEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
            //过滤小数点 保持9位
            ArrayList<InputFilter> filters = new ArrayList<>(Arrays.asList(mGasEditText.getFilters()));
            filters.add(new EditDecimalInputFilter());
            InputFilter[] filter = new InputFilter[filters.size()];
            mGasEditText.setFilters(filters.toArray(filter));
        }
        //从服务器同步数据
        updateUi();
        check();
        if (mDefaultWallet.getCurrentAccount().isEth()) {
            mGasProgressView.setBackgroundColor(Color.WHITE);
            mGasSlowFastLayout.setVisibility(View.VISIBLE);
            mGasAdjustLayout.setVisibility(View.GONE);
            //获取eth gas
            mTransactionViewModel.ethSlowFastGas();
        } else if (mDefaultWallet.getCurrentAccount().isQKC()) {
            View view = findViewById(R.id.transaction_send_all);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = 1;
            view.setLayoutParams(lp);
        }

        onRefresh();

        //拉取手续费
        if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
            if (mDefaultWallet.getCurrentAccount().isEth()) {
                if (!checkGasLimit(false)) {
                    //用默认值提前获取gasLimit
                    checkFirstGasLimit();
                }
            } else if (mDefaultWallet.getCurrentAccount().isQKC()) {
                //获取gas价格
                String shard = QWWalletUtils.parseFullShardForAddress(mFromString);
                mTransactionViewModel.gasPrice(shard);
                if (isSendToken() && isQKCNativeToken()) {
                    //如果是转native token，则判断该token是否支持作为手续费
                    mTransactionViewModel.checkGasTokenFirst(mToken, mFromString);
                }
            } else if (mDefaultWallet.getCurrentAccount().isTRX()) {
                //获取带宽
                checkBandwidth(false);
            }
        }
    }

    private void updateUi() {
        mMainQKCTokenCount = BigInteger.ZERO;
        mTokenCount = BigInteger.ZERO;
        //ETH
        if (mDefaultWallet.getCurrentAccount().isEth()) {
            if (TextUtils.isEmpty(mFromString)) {
                mFromString = mDefaultWallet.getCurrentAddress();
            }
            mFromAddressText.setText(Keys.toChecksumHDAddress(mFromString));

            String symbol = isSendToken() ? mToken.getSymbol() : QWTokenDao.ETH_SYMBOL;
            ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
            if (balances != null && !balances.isEmpty()) {
                for (QWBalance balance : balances) {
                    //获取所有token
                    if (isSendToken()) {
                        if (balance.getQWToken() != null && TextUtils.equals(mToken.getAddress(), balance.getQWToken().getAddress())) {
                            mTokenCount = Numeric.toBigInt(balance.getBalance());
                        }
                    } else {
                        if (balance.getQWToken() != null && TextUtils.equals(symbol, balance.getQWToken().getSymbol())) {
                            mTokenCount = Numeric.toBigInt(balance.getBalance());
                        }
                    }

                    if (balance.getQWToken() != null && QWTokenDao.ETH_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        mMainQKCTokenCount = Numeric.toBigInt(balance.getBalance());
                    }
                }
            }

            String totalToken = QWWalletUtils.getIntTokenFromWei10(mTokenCount.toString(), isSendToken() ? mToken.getTokenUnit() : Convert.Unit.ETHER);
            mTotalText.setText(totalToken);

            if (!isSendToken()) {
                TextView textView = findViewById(R.id.transaction_send_token_title);
                textView.setText(getString(R.string.eth));
                mSymbol = QWTokenDao.ETH_SYMBOL;
            }
            return;
        }

        //TRX
        if (mDefaultWallet.getCurrentAccount().isTRX()) {
            if (TextUtils.isEmpty(mFromString)) {
                mFromString = mDefaultWallet.getCurrentAddress();
            }
            mFromAddressText.setText(mFromString);

            //刷新UI
            updateBandWidthUi();
            //初始化常量
            mFromAddressByte = TronWalletClient.decodeFromBase58Check(mFromString);

            String symbol = isSendToken() ? mToken.getSymbol() : QWTokenDao.TRX_SYMBOL;
            ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
            if (balances != null && !balances.isEmpty()) {
                for (QWBalance balance : balances) {
                    //获取所有token
                    if (isSendToken()) {
                        if (balance.getQWToken() != null && TextUtils.equals(mToken.getAddress(), balance.getQWToken().getAddress())) {
                            mTokenCount = Numeric.toBigInt(balance.getBalance());
                        }
                    } else {
                        if (balance.getQWToken() != null && TextUtils.equals(symbol, balance.getQWToken().getSymbol())) {
                            mTokenCount = Numeric.toBigInt(balance.getBalance());
                        }
                    }

                    if (balance.getQWToken() != null && QWTokenDao.TRX_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        mMainQKCTokenCount = Numeric.toBigInt(balance.getBalance());
                    }
                }
            }

            String totalToken = QWWalletUtils.getIntTokenFromWei10(mTokenCount.toString(), isSendToken() ? mToken.getTokenUnit() : Convert.Unit.SUN);
            mTotalText.setText(totalToken);

            if (!isSendToken()) {
                TextView textView = findViewById(R.id.transaction_send_token_title);
                textView.setText(getString(R.string.trx));
                mSymbol = QWTokenDao.TRX_SYMBOL;
            }
            return;
        }


        //QKC
        mGasTokenList = new ArrayList<>();
        if (TextUtils.isEmpty(mFromString)) {
            mFromString = mDefaultWallet.getCurrentShareAddress();
        }
        //chain
        BigInteger currentChain = QWWalletUtils.getChainByAddress(this, mFromString);
        //shared
        BigInteger currentShard = QWWalletUtils.getShardByAddress(this, mFromString, currentChain);
        //转账from地址
        String text = String.format(getString(R.string.wallet_transaction_address_chain_shard), Keys.toChecksumHDAddress(mFromString), currentChain.toString(), currentShard.toString());
        mFromAddressText.setText(text);

        ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
        if (balances != null && !balances.isEmpty()) {
            String symbol = isSendToken() ? mToken.getSymbol() : QWTokenDao.QKC_SYMBOL;
            BigInteger total = BigInteger.ZERO;
            for (QWBalance balance : balances) {
                QWToken token = balance.getQWToken();
                if (token == null) {
                    continue;
                }

                //获取token chain,分片
                QWChain chain = balance.getChain();
                QWShard shard = balance.getQWShard();

                //获取支持gas的token
                if (Constant.ACCOUNT_TYPE_QKC == token.getType() && token.isNative()) {
                    //当前分片余额大于0的token
                    boolean isCurrentChain = Numeric.toBigInt(chain.getChain()).equals(currentChain) && Numeric.toBigInt(shard.getShard()).equals(currentShard);
                    if (isCurrentChain && BigInteger.ZERO.compareTo(Numeric.toBigInt(balance.getBalance())) < 0) {
                        mGasTokenList.add(balance);
                    }
                }

                //获取当前Token总balance余额
                if (symbol.equals(token.getSymbol())) {
                    total = total.add(Numeric.toBigInt(balance.getBalance()));
                    mTokenCount = total;
                    if (token.isNative() && Numeric.toBigInt(chain.getChain()).equals(currentChain) && Numeric.toBigInt(shard.getShard()).equals(currentShard)) {
                        //获取当前链，分片QKC balance余额
                        mMainQKCTokenCount = mMainQKCTokenCount.add(Numeric.toBigInt(balance.getBalance()));
                    }
                }
            }

            String totalToken = QWWalletUtils.getIntTokenFromWei10(mTokenCount.toString(), isSendToken() ? mToken.getTokenUnit() : Convert.Unit.ETHER);
            mTotalText.setText(totalToken);
        }

        checkGasTokenList();
        mGasSymbolText.setVisibility(View.VISIBLE);
    }

    private void updateBandWidthUi() {
        findViewById(R.id.gas_layout).setVisibility(View.GONE);
        findViewById(R.id.trx_band_width_layout).setVisibility(View.VISIBLE);

        mTotalBandWidthView = findViewById(R.id.trx_band_width_total);
        mCostBandWidthView = findViewById(R.id.ttrx_band_width_cost);

        onBandWidthSuccess((int) (mCostBandWidth * 100000D));
    }

    private void checkGasTokenList() {
        QWToken defaultGasToken = QWTokenDao.getTQKCToken();
        boolean hasDefault = false;
        for (QWBalance balance : mGasTokenList) {
            QWToken token = balance.getQWToken();
            if (TextUtils.equals(token.getSymbol(), defaultGasToken.getSymbol())) {
                hasDefault = true;
                break;
            }
        }

        if (!hasDefault) {
            QWBalance balance = new QWBalance();
            balance.setQWToken(defaultGasToken);
            balance.setBalance("0x0");
            mGasTokenList.add(balance);
        }
    }

    private boolean isSendToken() {
        return mToken != null;
    }

    private boolean isQKCNativeToken() {
        return mToken != null && mToken.isNative() && Constant.ACCOUNT_TYPE_QKC == mToken.getType();
    }


    private void onClickSlow() {
        if (mGasSlowLayoutView.isSelected()) {
            return;
        }
        if (mEthGas == null) {
            return;
        }
        String gasText = (int) mEthGas.getSafeLow() + "";
        mGasEditText.setText(gasText);
        if (mGasEditText.hasFocus()) {
            mGasEditText.setSelection(gasText.length());
        }

        mGasSlowLayoutView.setBackgroundResource(R.drawable.text_title_round_bg);
        mGasSlowLayoutView.setSelected(true);
        //avg
        mGasAvgLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
        mGasAvgLayoutView.setSelected(false);
        //fast
        mGasFastLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
        mGasFastLayoutView.setSelected(false);
    }

    private void onClickAvg() {
        if (mGasAvgLayoutView.isSelected()) {
            return;
        }
        if (mEthGas == null) {
            return;
        }
        String gasText = (int) mEthGas.getAverage() + "";
        mGasEditText.setText(gasText);
        if (mGasEditText.hasFocus()) {
            mGasEditText.setSelection(gasText.length());
        }

        mGasSlowLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
        mGasSlowLayoutView.setSelected(false);
        //avg
        mGasAvgLayoutView.setBackgroundResource(R.drawable.text_title_round_bg);
        mGasAvgLayoutView.setSelected(true);
        //fast
        mGasFastLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
        mGasFastLayoutView.setSelected(false);
    }

    private void onClickFast() {
        if (mGasFastLayoutView.isSelected()) {
            return;
        }
        if (mEthGas == null) {
            return;
        }

        String gasText = (int) mEthGas.getFast() + "";
        mGasEditText.setText(gasText);
        if (mGasEditText.hasFocus()) {
            mGasEditText.setSelection(gasText.length());
        }

        mGasSlowLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
        mGasSlowLayoutView.setSelected(false);
        //avg
        mGasAvgLayoutView.setBackgroundResource(R.drawable.text_hint_round_bg);
        mGasAvgLayoutView.setSelected(false);
        //fast
        mGasFastLayoutView.setBackgroundResource(R.drawable.text_title_round_bg);
        mGasFastLayoutView.setSelected(true);
    }

    private void changeGasToken(View view) {
        if (mGasTokenList == null || mGasTokenList.isEmpty()) {
            return;
        }
        showSymbolWindow(view);
    }

    private boolean isSoftShowing() {
        //获取当屏幕内容的高度
        int screenHeight = this.getWindow().getDecorView().getHeight();
        //获取View可见区域的bottom
        Rect rect = new Rect();
        //DecorView即为activity的顶级view
        this.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        //考虑到虚拟导航栏的情况（虚拟导航栏情况下：screenHeight = rect.bottom + 虚拟导航栏高度）
        //选取screenHeight*2/3进行判断
        return screenHeight * 2 / 3 > rect.bottom;
    }

    private void showSymbolWindow(View view) {
        if (mMenuPopWindow == null) {
            mMenuPopWindow = new SpinnerPopWindow(this);
            mMenuPopWindow.setOnItemClickListener((int position, Object o) -> {
                //校验该token是否支持作为gas费消耗
                QWBalance balance = (QWBalance) o;
                QWToken gasToken = balance.getQWToken();
                if (mGasToken != null && TextUtils.equals(mGasToken.getAddress(), gasToken.getAddress())) {
                    //和当前选中是同一个token，不作处理
                    return;
                }
                if (gasToken.getRefundPercentage() != null) {
                    //已经获取过，直接切换
                    onCheckGasToken(gasToken);
                    return;
                }
                mTransactionViewModel.checkGasToken(gasToken, mFromString);
            });
        }
        mMenuPopWindow.setAdapter(new SpinnerAdapter(mGasTokenList, mGasToken.getSymbol()));
        if (isSoftShowing()) {
            hideSoftwareKeyboard(mGasEditText);
            mGasEditText.postDelayed(() -> mMenuPopWindow.showWidth(view, (int) UiUtils.dpToPixel(175)), 100);
        } else {
            mMenuPopWindow.showWidth(view, (int) UiUtils.dpToPixel(175));
        }
    }

    private void hideSoftwareKeyboard(EditText input) {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onPreCreate() {
        if (!ConnectionUtil.isInternetConnection(this)) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        //ETH校验
        if (mDefaultWallet.getCurrentAccount().isEth()) {
            onPreCreateEth();
            return;
        }

        //TRX校验
        if (mDefaultWallet.getCurrentAccount().isTRX()) {
            onPreCreateTRX();
            return;
        }

        //QKC校验
        onPreCreateQkc();
    }

    //准备创建qkc交易单
    private void onPreCreateQkc() {
        String toAddress = mToAddressText.getText().toString().trim();
        if (!QWWalletUtils.isQKCValidAddress(toAddress)) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_address_error);
            mToAddressText.requestFocus();
            return;
        }

        //检测amount是否大于0
        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal amount = Convert.toWei(amountStr, Convert.Unit.ETHER);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_amount_error);
            mAmountText.requestFocus();
            return;
        }

        BigInteger gas = QWWalletUtils.toGWeiFrom10(mGasEditText.getText().toString());
        BigInteger gasLimit = new BigInteger(mGasLimitText.getText().toString().trim());
        BigInteger cost = gas.multiply(gasLimit);
        //检测余额
        if (isSendToken() && !isQKCNativeToken()) {
            //ERC20转账
            //余额是否足够
            if (amount.toBigInteger().compareTo(mTokenCount) > 0) {
                MyToast.showSingleToastShort(this, R.string.transaction_balance_token_amount_error);
                mAmountText.requestFocus();
                return;
            }

            //判断当前链/分片gasToken余额是否够支付手续费
            boolean hasGasToken = false;
            for (QWBalance balance : mGasTokenList) {
                if (TextUtils.equals(mGasToken.getSymbol(), balance.getQWToken().getSymbol())) {
                    String b = balance.getBalance();
                    BigInteger value = BigInteger.ZERO;
                    if (!TextUtils.isEmpty(b)) {
                        value = Numeric.toBigInt(balance.getBalance());
                    }
                    if (value.compareTo(cost) >= 0) {
                        hasGasToken = true;
                    }
                    break;
                }
            }
            if (!hasGasToken) {
                //手续费不够
                String message = getString(R.string.wallet_qrc20_transaction_gas_enough);
                showDialog(message);
                return;
            }
        } else {
            //native转账
            //手续费token和待转账token是同一token
            boolean isSendGasEquals = (mToken == null && TextUtils.equals(QWTokenDao.QKC_SYMBOL, mGasToken.getSymbol())
                    || (mToken != null && TextUtils.equals(mToken.getSymbol(), mGasToken.getSymbol())));

            String fromAddress = mFromString;
            BigInteger fromChain = QWWalletUtils.getChainByAddress(getApplicationContext(), fromAddress);
            BigInteger fromShard = QWWalletUtils.getShardByAddress(getApplicationContext(), fromAddress, fromChain);
            BigInteger amountInt = amount.toBigInteger();
            //转账数量 < 当前分片总数
            BigInteger totalSendToken = isSendGasEquals ? cost.add(amountInt) : amount.toBigInteger();
            if (totalSendToken.compareTo(mMainQKCTokenCount) > 0) {
                //1 获取其他分片余额，是否有分片余额大于该转账总数
                //切分片，merge预估手续费都用待转账token作为gas来计算
                ArrayList<QWBalance> list = hasEnoughQkcShards(amountInt);
                if (!list.isEmpty()) {
                    String to = mToAddressText.getText().toString().trim();
                    String title = String.format(getString(R.string.transaction_balance_enough_message_switch),
                            fromChain.toString(), fromShard.toString(),
                            amountStr + (mToken == null ? QWTokenDao.QKC_NAME : mToken.getSymbol().toUpperCase()));
                    showSwitchShard(list, to, amountStr, title);
                    return;
                }

                //2 所有分片token加起来是否大于该转账数量
                //切分片，merge预估手续费都用待转账token作为gas来计算
                if (hasEnoughQkcMerge(amountInt)) {
                    QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                    dialog.setTitle(R.string.transaction_balance_error_title);
                    dialog.setMessage(String.format(getString(R.string.transaction_balance_enough_message_merge),
                            isQKCNativeToken() ? mToken.getSymbol().toUpperCase() : QWTokenDao.QKC_NAME));
                    dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
                    dialog.setPositiveBtn(R.string.ok, v -> {
                        Intent intent = new Intent(this, MergeActivity.class);
                        //需要merge多少token
                        intent.putExtra(Constant.KEY_BALANCE, totalSendToken.subtract(mMainQKCTokenCount).toString());
                        if (isQKCNativeToken()) {
                            intent.putExtra(Constant.KEY_TOKEN, mToken);
                        }
                        startActivityForResult(intent, Constant.REQUEST_CODE_SEND_QCK_MERGE);
                        dialog.dismiss();
                    });
                    dialog.show();
                    return;
                }

                //3 都不足够，则弹提示
                MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
                return;
            }

            //手续费token和待转账token不同
            if (!isSendGasEquals) {
                //判断gasToken余额是否够支付手续费
                for (QWBalance balance : mGasTokenList) {
                    if (TextUtils.equals(mGasToken.getSymbol(), balance.getQWToken().getSymbol())) {
                        String b = balance.getBalance();
                        BigInteger value = BigInteger.ZERO;
                        if (!TextUtils.isEmpty(b)) {
                            value = Numeric.toBigInt(balance.getBalance());
                        }
                        if (value.compareTo(cost) < 0) {
                            String message = getString(R.string.wallet_native_transaction_gas_enough);
                            showDialog(message);
                            return;
                        }
                        break;
                    }
                }
            }
        }

        //不是QKC,需要判定该gas token是否支持手续费
        if (!TextUtils.equals("QKC", mGasToken.getSymbol().toUpperCase())) {
            //充值金额
            BigDecimal reserveBalance = mGasToken.getReserveTokenBalance();
            //兑换比例
            BigDecimal percentage = mGasToken.getRefundPercentage();
            //转换成qkc手续费需要消耗多少
            BigDecimal qkcCost = new BigDecimal(cost).divide(percentage, Constant.QKC_DECIMAL_NUMBER, RoundingMode.CEILING);
            if (reserveBalance.compareTo(qkcCost) < 0) {
                //充值余额连手续费都不够
                QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setMessage(R.string.native_gas_token_balance_not_enough);
                dialog.setPositiveBtn(R.string.ok, (v) -> dialog.dismiss());
                dialog.show();
                return;
            }
        }

        onCheckPassword();
    }

    //准备创建eth交易单
    private void onPreCreateEth() {
        String toAddress = mToAddressText.getText().toString().trim();
        if (!WalletUtils.isValidAddress(toAddress)) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_address_error);
            mToAddressText.requestFocus();
            return;
        }

        //检测amount是否大于0
        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal amount = Convert.toWei(amountStr, isSendToken() ? mToken.getTokenUnit() : Convert.Unit.ETHER);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_amount_error);
            mAmountText.requestFocus();
            return;
        }

        BigInteger gas = QWWalletUtils.toGWeiFrom10(mGasEditText.getText().toString());
        BigInteger gasLimit = new BigInteger(mGasLimitText.getText().toString().trim());
        BigInteger cost = gas.multiply(gasLimit);
        BigInteger totalQKC;
        //检测余额
        if (isSendToken()) {
            if (amount.toBigInteger().compareTo(mTokenCount) > 0) {
                MyToast.showSingleToastShort(this, R.string.transaction_balance_token_amount_error);
                mAmountText.requestFocus();
                return;
            }

            if (cost.compareTo(mMainQKCTokenCount) > 0) {
                //余额不足
                MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
                return;
            }
        } else {
            totalQKC = cost.add(amount.toBigInteger());
            //转账数量+手续费 < 当前分片总数
            if (totalQKC.compareTo(mMainQKCTokenCount) > 0) {
                //余额不足
                MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
                return;
            }
        }
        onCheckPassword();
    }

    //准备创建trx交易单
    private void onPreCreateTRX() {
        String toAddress = mToAddressText.getText().toString().trim();
        if (!TronWalletClient.isTronAddressValid(toAddress)) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_address_error);
            mToAddressText.requestFocus();
            return;
        }

        if (TextUtils.equals(mFromString, toAddress)) {
            MyToast.showSingleToastShort(this, R.string.cant_send_to_own_address);
            mToAddressText.requestFocus();
            return;
        }

        //检测amount是否大于0
        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal amount = Convert.toWei(amountStr, isSendToken() ? mToken.getTokenUnit() : Convert.Unit.SUN);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_amount_error);
            mAmountText.requestFocus();
            return;
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        //校验带宽，能量
        BigDecimal cost = Convert.toWei(mCostBandWidth + "", Convert.Unit.SUN);
        //检测余额
        if (isSendToken()) {
            if (amount.toBigInteger().compareTo(mTokenCount) > 0) {
                MyToast.showSingleToastShort(this, R.string.transaction_balance_token_amount_error);
                mAmountText.requestFocus();
                return;
            }

            if (cost.toBigInteger().compareTo(mMainQKCTokenCount) > 0) {
                //余额不足
                MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
                return;
            }
        } else {
            BigDecimal totalCost = cost.add(amount);
            //转账数量+手续费 < 当前分片总数
            if (totalCost.toBigInteger().compareTo(mMainQKCTokenCount) > 0) {
                //余额不足
                MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
                return;
            }
        }
        onCheckPassword();
    }

    //获取所有余额大于该总数的分片
    private ArrayList<QWBalance> hasEnoughQkcShards(BigInteger total) {
        //转QKC，则加上QKC的手续费
        if (mToken == null || TextUtils.equals(mToken.getAddress(), QWTokenDao.TQKC_ADDRESS)) {
            BigInteger gasPrice = QWWalletUtils.toGWeiFrom10(mCurrentGas);
            BigInteger gasLimit = new BigInteger(Constant.DEFAULT_GAS_LIMIT);
            BigInteger gas = gasPrice.multiply(gasLimit);
            total = total.add(gas);
        }
        ArrayList<QWBalance> list = new ArrayList<>();
        ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
        if (balances != null) {
            String symbol = isQKCNativeToken() ? mToken.getSymbol() : QWTokenDao.QKC_SYMBOL;
            for (QWBalance balance : balances) {
                QWToken token = balance.getQWToken();
                if (token != null && TextUtils.equals(symbol, token.getSymbol())) {
                    BigInteger value = Numeric.toBigInt(balance.getBalance());
                    if (total.compareTo(value) <= 0) {
                        list.add(balance);
                    }
                }
            }
        }
        return list;
    }

    private boolean hasEnoughQkcMerge(BigInteger total) {
        //转QKC，则加上merge需要的手续费
        BigInteger gas = null;
        if (mToken == null || TextUtils.equals(mToken.getAddress(), QWTokenDao.TQKC_ADDRESS)) {
            BigInteger gasPrice = QWWalletUtils.toGWeiFrom10(mCurrentGas);
            BigInteger gasLimit = new BigInteger(Constant.DEFAULT_GAS_LIMIT);
            gas = gasPrice.multiply(gasLimit);
        }

        BigInteger totalShards = BigInteger.ZERO;
        ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
        if (balances != null) {
            String symbol = isQKCNativeToken() ? mToken.getSymbol() : QWTokenDao.QKC_SYMBOL;
            for (QWBalance balance : balances) {
                QWToken token = balance.getQWToken();
                if (token != null && TextUtils.equals(symbol, token.getSymbol())) {
                    BigInteger b = Numeric.toBigInt(balance.getBalance());
                    if (gas == null) {
                        totalShards = totalShards.add(b);
                    } else if (gas.compareTo(b) < 0) {
                        //减去手续费
                        b = b.subtract(gas);
                        totalShards = totalShards.add(b);
                    }
                }
            }
        }

        return totalShards.compareTo(total) >= 0;
    }

    private void onCheckPassword() {
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mDefaultWallet,
                new SystemUtils.OnCheckPassWordListenerImp() {
                    @Override
                    public void onPasswordSuccess(String password) {
                        onCreate(password);
                    }
                });
    }

    //**************转账******************
    private void onCreate(String password) {
        showProgress(true);
        if (mDefaultWallet.getCurrentAccount().isTRX()) {
            String toAddress = mToAddressText.getText().toString().trim();
            String fromAddress = mFromString;
            String tokenCount = mAmountText.getText().toString().trim();
            onSendTrx(password, fromAddress, toAddress, Double.parseDouble(tokenCount));
            return;
        }

        String tokenCount = mAmountText.getText().toString().trim();
        String gas = mGasEditText.getText().toString().trim();
        String gasLimitStr = mGasLimitText.getText().toString().trim();
        String toAddress = mToAddressText.getText().toString().trim();
        String fromAddress = mFromString;

        BigInteger gasPrice = QWWalletUtils.toGWeiFrom10(gas);

        BigInteger gasLimit = new BigInteger(gasLimitStr);

        BigInteger amount = QWWalletUtils.toWeiFrom10(tokenCount, isSendToken() ? mToken.getTokenUnit() : Convert.Unit.ETHER);

        if (mDefaultWallet.getCurrentAccount().isEth()) {
            onSendEth(password, gasPrice, gasLimit, fromAddress, toAddress, amount);
        } else {
            onSend(password, gasPrice, gasLimit, fromAddress, toAddress, amount);
        }
    }

    private void onSend(String password, BigInteger gasPrice, BigInteger gasLimit, String fromAddress, String toAddress, BigInteger amount) {
        if (isSendToken() && !isQKCNativeToken()) {
            mTransactionViewModel.createTokenTransfer(
                    password,
                    fromAddress, toAddress,
                    mToken.getAddress(),
                    amount,
                    gasPrice, gasLimit,
                    Constant.sNetworkId,
                    QWTokenDao.TQKC_ADDRESS, mGasToken.getAddress()
            );
            UmengStatistics.walletCreateTokenTransactionClickCount(getApplicationContext(), mToken.getSymbol(), fromAddress);
        } else {
            mTransactionViewModel.createTransaction(
                    password,
                    fromAddress, toAddress,
                    amount,
                    gasPrice, gasLimit,
                    Constant.sNetworkId,
                    isQKCNativeToken() ? mToken.getAddress() : QWTokenDao.TQKC_ADDRESS, mGasToken.getAddress()
            );
            UmengStatistics.walletCreateTokenTransactionClickCount(getApplicationContext(), "qkc", fromAddress);
        }
    }

    private void onSendEth(String password, BigInteger gasPrice, BigInteger gasLimit, String fromAddress, String toAddress, BigInteger amount) {
        if (isSendToken()) {
            mTransactionViewModel.createEthTokenTransfer(
                    password,
                    mDefaultWallet.getCurrentShareAddress(), toAddress,
                    mToken.getAddress(),
                    amount,
                    gasPrice, gasLimit
            );
            UmengStatistics.walletCreateTokenTransactionClickCount(getApplicationContext(), mToken.getSymbol(), fromAddress);
        } else {
            mTransactionViewModel.createEthTransaction(
                    password,
                    fromAddress, toAddress,
                    amount,
                    gasPrice, gasLimit
            );
            UmengStatistics.walletCreateTokenTransactionClickCount(getApplicationContext(), "eth", fromAddress);
        }
    }

    private void onSendTrx(String password, String fromAddress, String toAddress, double amount) {
        if (isSendToken()) {
            BigDecimal bigDecimal = Convert.toWei(amount + "", mToken.getTokenUnit());
            mTransactionViewModel.createTrxTransaction(
                    password,
                    mDefaultWallet.getCurrentAddress(), toAddress,
                    bigDecimal.doubleValue(),
                    mToken.getAddress()
            );
            UmengStatistics.walletCreateTokenTransactionClickCount(getApplicationContext(), mToken.getSymbol(), fromAddress);
        } else {
            mTransactionViewModel.createTrxTransaction(
                    password,
                    mDefaultWallet.getCurrentAddress(), toAddress,
                    amount,
                    null
            );
            UmengStatistics.walletCreateTokenTransactionClickCount(getApplicationContext(), "trx", fromAddress);
        }
    }

    //**************转账******************

    //创建交易条目成功
    private void createSuccess(String[] hash) {
        if (hash != null && hash.length == 2) {
            String costStr;
            if (mIsTrx) {
                costStr = String.valueOf(mCostBandWidth);
            } else {
                BigInteger gas = QWWalletUtils.toGWeiFrom10(mGasEditText.getText().toString());
                BigInteger gasLimit = new BigInteger(mGasLimitText.getText().toString().trim());
                BigInteger cost = gas.multiply(gasLimit);
                if (mDefaultWallet.getCurrentAccount().isQKC()) {
                    costStr = QWWalletUtils.getIntTokenFromWei10(cost.toString(), true, Constant.QKC_DECIMAL_NUMBER);
                } else {
                    costStr = QWWalletUtils.getIntTokenFromWei10(cost.toString(), true);
                }
            }

            String amount = mAmountText.getText().toString();

            String tokenName;
            String tokenAddress = "";
            if (mToken != null) {
                tokenName = mToken.getSymbol().toLowerCase();
                tokenAddress = mToken.getAddress();
            } else {
                if (mDefaultWallet.getCurrentAccount().isEth()) {
                    tokenName = "eth";
                } else if (mDefaultWallet.getCurrentAccount().isTRX()) {
                    tokenName = "trx";
                } else {
                    tokenName = "qkc";
                }
            }

            int accountType = mDefaultWallet.getCurrentAccount().getType();

            if (mDefaultWallet.getCurrentAccount().isQKC()) {
                String from = mFromString;
                String to = mToAddressText.getText().toString();
                if (isSendToken() && !isQKCNativeToken()) {
                    String conAddress = mToken.getAddress();
                    to = QWWalletUtils.changeChainShardToDes(getApplication(), to, conAddress);
                }
                TransactionSendActivity.startQKCTransactionSendActivity(this,
                        from,
                        to,
                        amount,
                        costStr,
                        hash[0],
                        hash[1],
                        mToken != null ? mToken.getSymbol().toUpperCase() : "",
                        accountType,
                        mShowToast,
                        TransactionSendActivity.KEY_FROM_NORMAL,
                        tokenName,
                        tokenAddress,
                        mGasToken.getSymbol().toUpperCase());
            } else {
                TransactionSendActivity.startTransactionSendActivity(this,
                        mFromString,
                        mToAddressText.getText().toString(),
                        amount,
                        costStr,
                        hash[0],
                        hash[1],
                        mToken != null ? mToken.getSymbol().toUpperCase() : "",
                        accountType,
                        mShowToast,
                        TransactionSendActivity.KEY_FROM_NORMAL,
                        tokenName,
                        tokenAddress);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTIONS) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                setResult(Activity.RESULT_OK, data);
            }
            finish();
        } else if ((requestCode == Constant.REQUEST_CODE_SEND_QCK_MERGE
                || requestCode == Constant.REQUEST_CODE_SEND_TOKEN_MERGE
                || requestCode == Constant.REQUEST_CODE_SEND_TOKEN_SWITCH)) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(Constant.KEY_RESULT, requestCode);
                startActivity(intent);
            } else {
                finish();
            }
        } else if (Constant.REQUEST_CODE_CAPTURE == requestCode && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String toAddress = data.getStringExtra(Constant.WALLET_ADDRESS);
                if (mToAddressText != null) {
                    mToAddressText.setText(toAddress);
                } else {
                    mToTempAddress = toAddress;
                }

                String amount = data.getStringExtra(Constant.KEY_BALANCE);
                if (!TextUtils.isEmpty(amount)) {
                    if (mAmountText != null) {
                        mAmountText.setText(amount);
                    } else {
                        mTempAmount = amount;
                    }
                }
            }
        } else if (Constant.REQUEST_CODE_ADDRESS_BOOK == requestCode && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String address = data.getStringExtra(AddressBookActivity.ACCOUNT_ADDRESS);
                mToAddressText.setText(address);
            }
        }
    }

    //选择转账的分片
    private void showSwitchShard(ArrayList<QWBalance> list, String toAddress, String total, String message) {
        WheelBalancePopWindow popWindow = new WheelBalancePopWindow(this);
        popWindow.setTitle(message);
        popWindow.setToken(mToken);
        popWindow.setItem(list);
        popWindow.setOnNumberPickListener(index -> {

            QWBalance balance = list.get(index);
            QWChain chain = balance.getChain();
            QWShard shard = balance.getQWShard();
            if (shard != null) {
                String fromAddress = Keys.toChecksumHDAddress(Numeric.selectChainAndShardAddress(mDefaultWallet.getCurrentAddress(), chain.getChain(), shard.getShard()));

                Intent intent = new Intent(TransactionCreateActivity.this, TransactionCreateActivity.class);
                intent.putExtra(Constant.KEY_FROM, fromAddress);
                intent.putExtra(Constant.WALLET_ADDRESS, toAddress);
                intent.putExtra(Constant.KEY_BALANCE, total);
                if (isQKCNativeToken()) {
                    intent.putExtra(Constant.KEY_TOKEN, mToken);
                }
                startActivityForResult(intent, Constant.REQUEST_CODE_SEND_TRANSACTIONS);
            }
        });
        popWindow.show();
    }

    private void onEthGasSuccess(EthGas gas) {
        mGasProgressView.setVisibility(View.GONE);
        if (gas == null) {
            mGasFailLayout.setVisibility(View.VISIBLE);
            //加载失败
            return;
        }

        mEthGas = gas;
        updateGasPriceByLimit(mGasLimitText.getText().toString().trim());

        onClickAvg();
    }

    private void updateGasPriceByLimit(String value) {
        value = TextUtils.isEmpty(value) ? "0" : value;
        BigInteger gasLimit = new BigInteger(value);
        //slow
        float safeLow = mEthGas.getSafeLow();
        BigInteger safeGas = QWWalletUtils.toGWeiFrom10(safeLow + "");
        BigInteger cost = safeGas.multiply(gasLimit);
        String costStr = QWWalletUtils.getIntTokenFromWei10(cost.toString(), true);
        String slowGas = costStr + " " + QWTokenDao.ETH_NAME;
        mGasSlowCountView.setText(slowGas);
        String slowGasPrice = ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, costStr);
        mGasSlowPriceView.setText(slowGasPrice);
        String lowTime = Constant.PRICE_ABOUT + mEthGas.getSafeLowWait() + "s";
        mGasSlowTimeView.setText(lowTime);

        //avg
        float avg = mEthGas.getAverage();
        BigInteger avgGas = QWWalletUtils.toGWeiFrom10(avg + "");
        BigInteger avgCost = avgGas.multiply(gasLimit);
        costStr = QWWalletUtils.getIntTokenFromWei10(avgCost.toString(), true);
        String avgGasStr = costStr + " " + QWTokenDao.ETH_NAME;
        mGasAvgCountView.setText(avgGasStr);
        String avgGasPrice = ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, costStr);
        mGasAvgPriceView.setText(avgGasPrice);
        String avgTime = Constant.PRICE_ABOUT + mEthGas.getAvgWait() + "s";
        mGasAvgTimeView.setText(avgTime);

        //fast
        float fast = mEthGas.getFast();
        BigInteger fastGas = QWWalletUtils.toGWeiFrom10(fast + "");
        BigInteger fastCost = fastGas.multiply(gasLimit);
        costStr = QWWalletUtils.getIntTokenFromWei10(fastCost.toString(), true);
        String fastGasStr = costStr + " " + QWTokenDao.ETH_NAME;
        mGasFastCountView.setText(fastGasStr);
        String fastGasPrice = ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, costStr);
        mGasFastPriceView.setText(fastGasPrice);
        String fastTime = Constant.PRICE_ABOUT + mEthGas.getFastWait() + "s";
        mGasFastTimeView.setText(fastTime);
    }

    private void onGasSuccess(String gas) {
        mGasEditText.setText(gas);
        mCurrentGas = gas;
    }

    private void onGasLimitSuccess(BigInteger gas) {
        String text = gas.toString();
        mGasLimitText.setText(text);
        if (mGasLimitText.hasFocus()) {
            mGasLimitText.setSelection(text.length());
        }
    }

    private void onCheckGasToken(QWToken token) {
        if (token == null) {
            //当前token不支持作为手续费
            QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMessage(R.string.transaction_gas_token_not_support);
            dialog.setPositiveBtn(R.string.ok, (v) -> dialog.dismiss());
            dialog.show();
            return;
        }
        checkFirstGasToken(token);
    }

    private void checkFirstGasToken(QWToken token) {
        if (token != null) {
            //切换gas token
            mGasToken = token;
            mGasSymbolText.setText(mGasToken.getSymbol().toUpperCase());
            //根据比例设置新的gas price
            BigDecimal currentGas = new BigDecimal(mCurrentGas);
            mGasEditText.setText(currentGas.multiply(token.getRefundPercentage()).stripTrailingZeros().toPlainString());
            //更新兑换比例
            if (mGasTokenList != null) {
                for (QWBalance balance : mGasTokenList) {
                    QWToken tok = balance.getQWToken();
                    if (TextUtils.equals(tok.getAddress(), token.getAddress())) {
                        tok.setRefundPercentage(token.getRefundPercentage());
                        tok.setReserveTokenBalance(token.getReserveTokenBalance());
                    }
                }
            }
        }
        //查询最新gas limit
        checkGasLimit(false);
    }


    private boolean checkGasLimit(boolean post) {
        if (mDefaultWallet == null) {
            return false;
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            return false;
        }

        if (mHandler != null && mHandler.hasMessages(HANDLER_WHAT)) {
            return false;
        }

        //检测amount是否大于0
        if (TextUtils.isEmpty(mAmountText.getText())) {
            return false;
        }
        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal amount = Convert.toWei(amountStr, Convert.Unit.ETHER);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (TextUtils.isEmpty(mToAddressText.getText()) && !mDefaultWallet.getCurrentAccount().isAllBTC()) {
            return false;
        }
        String toAddress = mToAddressText.getText().toString().trim();
        if (mDefaultWallet.getCurrentAccount().isEth()) {
            if (!WalletUtils.isValidAddress(toAddress)) {
                return false;
            }
        } else if (mDefaultWallet.getCurrentAccount().isQKC()) {
            if (!QWWalletUtils.isQKCValidAddress(toAddress)) {
                return false;
            }
        }

        if (post) {
            mHandler.sendEmptyMessageDelayed(HANDLER_WHAT, HANDLER_TIME);
        } else {
            if (mDefaultWallet.getCurrentAccount().isEth()) {
                if (isSendToken()) {
                    mTransactionViewModel.ethGasLimitSendToken(mFromString, toAddress, mToken.getAddress(), QWWalletUtils.toWeiFrom10(amountStr, mToken.getTokenUnit()));
                } else {
                    mTransactionViewModel.ethGasLimit(mFromString, toAddress);
                }
            } else {
                if (isSendToken() && !isQKCNativeToken()) {
                    mTransactionViewModel.gasLimitSendToken(mFromString, toAddress, mToken.getAddress(),
                            QWWalletUtils.toWeiFrom10(amountStr, mToken.getTokenUnit()),
                            QWTokenDao.TQKC_ADDRESS, mGasToken.getAddress());
                } else {
                    mTransactionViewModel.gasLimit(mFromString, toAddress, isQKCNativeToken() ? mToken.getAddress() : QWTokenDao.TQKC_ADDRESS, mGasToken.getAddress());
                }
            }
        }
        return true;
    }

    private void checkFirstGasLimit() {
        if (mDefaultWallet.getCurrentAccount().isEth()) {
            if (isSendToken()) {
                mTransactionViewModel.ethGasLimitSendToken(mFromString, Constant.ETH_FIRST_GAS_LIMIT_ADDRESS, mToken.getAddress(), new BigInteger(mTokenCount.toString()));
            } else {
                mTransactionViewModel.ethGasLimit(mFromString, Constant.ETH_FIRST_GAS_LIMIT_ADDRESS);
            }
        } else {
            if (isSendToken() && !isQKCNativeToken()) {
                mTransactionViewModel.gasLimitSendToken(mFromString, Constant.ETH_FIRST_GAS_LIMIT_ADDRESS, mToken.getAddress(),
                        new BigInteger(mTokenCount.toString()),
                        QWTokenDao.TQKC_ADDRESS, mGasToken.getAddress());
            } else {
                mTransactionViewModel.gasLimit(mFromString, Constant.ETH_FIRST_GAS_LIMIT_ADDRESS,
                        isQKCNativeToken() ? mToken.getAddress() : QWTokenDao.TQKC_ADDRESS,
                        mGasToken.getAddress());
            }
        }
    }

    private void onBandWidthSuccess(int bandwidthCost) {
        if (mDefaultWallet == null) {
            return;
        }
        GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(this, mDefaultWallet.getCurrentAddress());
        long bandwidthNormal = accountNetMessage.getNetLimit() - accountNetMessage.getNetUsed();
        long bandwidthFree = accountNetMessage.getFreeNetLimit() - accountNetMessage.getFreeNetUsed();

        long bandwidth = accountNetMessage.getNetLimit() + accountNetMessage.getFreeNetLimit();
        long bandwidthUsed = accountNetMessage.getNetUsed() + accountNetMessage.getFreeNetUsed();

        long currentBandwidth = bandwidth - bandwidthUsed;

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMaximumFractionDigits(6);

        mTotalBandWidthView.setText(numberFormat.format(currentBandwidth) + " BP");
        mCostBandWidthView.setTextColor(getResources().getColor(R.color.text_message));
        mCostBandWidthView.setText(bandwidthCost == 0 ? "-" : numberFormat.format(bandwidthCost) + " BP");

        boolean enoughBandwidth = bandwidthNormal >= bandwidthCost || bandwidthFree >= bandwidthCost;
        if (!enoughBandwidth) {
            mCostBandWidth = bandwidthCost / 100000D;
            mCostBandWidthView.setTextColor(Color.parseColor("#ff4c41"));
        } else {
            mCostBandWidth = 0D;
        }
    }

    private void onFirstBandWidthSuccess(int bandwidthCost) {
        showProgress(false);
        bandwidthCost = Math.max(bandwidthCost, 280);
        GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(this, mDefaultWallet.getCurrentAddress());
        long bandwidthNormal = accountNetMessage.getNetLimit() - accountNetMessage.getNetUsed();
        long bandwidthFree = accountNetMessage.getFreeNetLimit() - accountNetMessage.getFreeNetUsed();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMaximumFractionDigits(6);
        boolean enoughBandwidth = bandwidthNormal >= bandwidthCost || bandwidthFree >= bandwidthCost;
        if (!enoughBandwidth) {
            mSendAllCostBandWidth = bandwidthCost / 100000D;
        } else {
            mSendAllCostBandWidth = 0D;
        }


        BigInteger totalCount = new BigInteger(mTokenCount.toString());
        //检测余额
        if (!isSendToken()) {
            //校验带宽，能量
            BigDecimal cost = Convert.toWei(mSendAllCostBandWidth + "", Convert.Unit.SUN);
            totalCount = totalCount.subtract(cost.toBigInteger());
        }
        //TRX校验
        String totalToken = QWWalletUtils.getIntTokenFromWei10(totalCount.toString(), isSendToken() ? mToken.getTokenUnit() : Convert.Unit.SUN);
        if (!TextUtils.isEmpty(totalToken)) {
            mAmountText.requestFocus();
            mAmountText.setText(totalToken);
            mAmountText.setSelection(totalToken.length());
        }
    }

    //获取trx带宽
    private void checkBandwidth(boolean post) {
        if (mDefaultWallet == null) {
            return;
        }
        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            return;
        }
        if (mHandler != null && mHandler.hasMessages(HANDLER_WHAT_TRX)) {
            return;
        }

        //检测amount是否大于0
        if (TextUtils.isEmpty(mAmountText.getText())) {
            return;
        }
        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal amount = Convert.toWei(amountStr, isSendToken() ? mToken.getTokenUnit() : Convert.Unit.SUN);
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.toBigInteger().compareTo(isSendToken() ? mTokenCount : mMainQKCTokenCount) > 0) {
            return;
        }

        //检测to address
        if (TextUtils.isEmpty(mToAddressText.getText())) {
            return;
        }
        String toAddress = mToAddressText.getText().toString().trim();
        if (!TronWalletClient.isTronAddressValid(toAddress)) {
            return;
        }
        if (TextUtils.equals(mFromString, toAddress)) {
            return;
        }

        if (post) {
            mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_TRX, HANDLER_TIME);
        } else {
            mTransactionViewModel.getCostBandWidth(mFromAddressByte, toAddress,
                    isSendToken() ? mToken.getAddress() : "",
                    isSendToken() ? amount.doubleValue() : Double.parseDouble(amountStr));
        }
    }

    private void checkFirstBandwidth() {
        if (!isSendToken() && mTokenCount != null && !mTokenCount.equals(BigInteger.ZERO)) {
            BigInteger totalCount = mTokenCount.subtract(new BigInteger("300"));
            String count = QWWalletUtils.getIntTokenFromWei10(totalCount.toString(), Convert.Unit.SUN);
            mTransactionViewModel.getFirstCostBandWidth(mFromAddressByte, Constant.TRX_FIRST_BAND_WITH_ADDRESS, "", Double.parseDouble(count));
        }
    }

    private void sendAll() {
        if (mDefaultWallet != null && mTokenCount != null) {
            if (mDefaultWallet.getCurrentAccount().isTRX() && !isSendToken() && mSendAllCostBandWidth == -1) {
                showProgress(true);
                checkFirstBandwidth();
                return;
            }

            BigInteger totalCount = new BigInteger(mTokenCount.toString());
            String totalToken = "";
            if (mDefaultWallet.getCurrentAccount().isEth()) {
                //ETH
                if (!isSendToken()) {
                    BigInteger gas = QWWalletUtils.toGWeiFrom10(mGasEditText.getText().toString());
                    BigInteger gasLimit = new BigInteger(mGasLimitText.getText().toString().trim());
                    BigInteger cost = gas.multiply(gasLimit);
                    totalCount = totalCount.subtract(cost);
                }
                if (totalCount.compareTo(BigInteger.ZERO) < 0) {
                    totalCount = BigInteger.ZERO;
                }
                totalToken = QWWalletUtils.getIntTokenFromWei10(totalCount.toString(), isSendToken() ? mToken.getTokenUnit() : Convert.Unit.ETHER);
            } else if (mDefaultWallet.getCurrentAccount().isTRX()) {
                //检测余额
                if (!isSendToken()) {
                    //校验带宽，能量
                    BigDecimal cost = Convert.toWei(mSendAllCostBandWidth == -1 ? "0" : mSendAllCostBandWidth + "", Convert.Unit.SUN);
                    totalCount = totalCount.subtract(cost.toBigInteger());
                }
                //TRX校验
                if (totalCount.compareTo(BigInteger.ZERO) < 0) {
                    totalCount = BigInteger.ZERO;
                }
                totalToken = QWWalletUtils.getIntTokenFromWei10(totalCount.toString(), isSendToken() ? mToken.getTokenUnit() : Convert.Unit.SUN);
            } else if (mDefaultWallet.getCurrentAccount().isQKC()) {
                //手续费token和待转账token是同一token
                boolean isSendGasEquals = (mToken == null && TextUtils.equals(QWTokenDao.QKC_SYMBOL, mGasToken.getSymbol())
                        || (mToken != null && TextUtils.equals(mToken.getSymbol(), mGasToken.getSymbol())));
                if (isSendGasEquals) {
                    BigInteger gas = QWWalletUtils.toGWeiFrom10(mGasEditText.getText().toString());
                    BigInteger gasLimit = new BigInteger(mGasLimitText.getText().toString().trim());
                    BigInteger cost = gas.multiply(gasLimit);
                    totalCount = totalCount.subtract(cost);
                }
                if (totalCount.compareTo(BigInteger.ZERO) < 0) {
                    totalCount = BigInteger.ZERO;
                }
                totalToken = QWWalletUtils.getIntTokenFromWei10(totalCount.toString(), isSendToken() ? mToken.getTokenUnit() : Convert.Unit.ETHER);
            }

            if (!TextUtils.isEmpty(totalToken)) {
                mAmountText.requestFocus();
                mAmountText.setText(totalToken);
                mAmountText.setSelection(totalToken.length());
            }
        }
    }

    private void onRefresh() {
        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            mSwipeLayout.setRefreshing(false);
            return;
        }

        mSwipeLayout.post(() -> mSwipeLayout.setRefreshing(true));
        if (isSendToken() && !isQKCNativeToken()) {
            mTransactionViewModel.getTokenAndAccountData(mDefaultWallet, mToken);
        } else {
            mTransactionViewModel.getAccountData(mDefaultWallet);
        }
    }

    private void accountSuccess(boolean value) {
        mSwipeLayout.setRefreshing(false);

        //重新查询数据库 刷新数据
        QWAccountDao dao = new QWAccountDao(getApplicationContext());
        QWAccount account = dao.queryByAddress(mDefaultWallet.getCurrentAddress());
        mDefaultWallet.setCurrentAccount(account);

        updateUi();
    }

    private void showDialog(String message) {
        final QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage(message);
        dialog.setPositiveBtn(R.string.ok, (v) -> dialog.dismiss());
        dialog.show();
    }
}
