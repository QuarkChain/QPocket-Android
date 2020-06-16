package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.crypto.utils.Convert;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.main.view.SpinnerPopWindow;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.JustifyTextView;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.view.WheelBalancePopWindow;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.bean.EditDecimalInputFilter;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class PublicSaleTransactionCreateActivity extends BaseActivity {

    private class MyGasWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(s) || !isNumber(s) || Double.parseDouble(s.toString()) <= 0) {
                mSendView.setEnabled(false);
            } else {
                check();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

        private boolean isNumber(CharSequence value) {
            try {
                Double.parseDouble(value.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private class MyAmountTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mAmountText.removeTextChangedListener(mAmountTextWatcher);
            mPriceText.removeTextChangedListener(mPriceTextWatcher);

            if (TextUtils.isEmpty(s)) {
                mSendView.setEnabled(false);
                mPriceText.setText(null);
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

                if (str.contains(".") && s.length() - 1 - s.toString().indexOf(".") > DECIMAL_DIGITS) {
                    s = s.toString().subSequence(0, s.toString().indexOf(".") + DECIMAL_DIGITS + 1);
                    mAmountText.setText(s);
                    mAmountText.setSelection(s.length());
                } else if (str.contains(",") && s.length() - 1 - s.toString().indexOf(",") > DECIMAL_DIGITS) {
                    s = s.toString().subSequence(0, s.toString().indexOf(",") + DECIMAL_DIGITS + 1);
                    mAmountText.setText(s);
                    mAmountText.setSelection(s.length());
                }

                try {
                    BigDecimal value = new BigDecimal(s.toString().trim());
                    BigDecimal cost = value.divide(new BigDecimal(mTokenBuyRate), 4, BigDecimal.ROUND_UP);
                    if (cost.compareTo(BigDecimal.ONE) < 0) {
                        BigDecimal costTemp = value.divide(new BigDecimal(mTokenBuyRate), 18, BigDecimal.ROUND_HALF_UP);
                        if (costTemp.compareTo(new BigDecimal(MIN_QKC)) < 0) {
                            mPriceText.setText("0");
                        } else {
                            mPriceText.setText(cost.stripTrailingZeros().toPlainString());
                        }
                    } else {
                        mPriceText.setText(cost.stripTrailingZeros().toPlainString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                check();

                checkGasLimit(true);
            }

            mPriceText.addTextChangedListener(mPriceTextWatcher);
            mAmountText.addTextChangedListener(mAmountTextWatcher);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class PriceTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mAmountText.removeTextChangedListener(mAmountTextWatcher);
            mPriceText.removeTextChangedListener(mPriceTextWatcher);

            if (TextUtils.isEmpty(s)) {
                mSendView.setEnabled(false);
                mAmountText.setText(null);
            } else {
                String str = s.toString().trim();
                if (str.startsWith(".")) {
                    s = "0.";
                    mPriceText.setText(s);
                    mPriceText.setSelection(2);
                } else if (str.startsWith(",")) {
                    s = "0,";
                    mPriceText.setText(s);
                    mPriceText.setSelection(2);
                }

                if (str.contains(".") && s.length() - 1 - s.toString().indexOf(".") > DECIMAL_DIGITS) {
                    s = s.toString().subSequence(0, s.toString().indexOf(".") + DECIMAL_DIGITS + 1);
                    mPriceText.setText(s);
                    mPriceText.setSelection(s.length());
                } else if (str.contains(",") && s.length() - 1 - s.toString().indexOf(",") > DECIMAL_DIGITS) {
                    s = s.toString().subSequence(0, s.toString().indexOf(",") + DECIMAL_DIGITS + 1);
                    mPriceText.setText(s);
                    mPriceText.setSelection(s.length());
                }

                try {
                    BigDecimal value = new BigDecimal(s.toString().trim());
                    BigDecimal amount = value.multiply(new BigDecimal(mTokenBuyRate));
                    if (amount.compareTo(BigDecimal.ONE) < 0) {
                        amount = amount.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
                        if (amount.compareTo(new BigDecimal(MIN_QKC)) < 0) {
                            mAmountText.setText("0");
                        } else {
                            mAmountText.setText(amount.stripTrailingZeros().toPlainString());
                        }
                    } else {
                        mAmountText.setText(amount.stripTrailingZeros().toPlainString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                check();

                checkGasLimit(true);
            }

            mPriceText.addTextChangedListener(mPriceTextWatcher);
            mAmountText.addTextChangedListener(mAmountTextWatcher);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private void check() {
        if (TextUtils.isEmpty(mAmountText.getText())
                || TextUtils.isEmpty(mGasLimitText.getText())
                || TextUtils.isEmpty(mGasEditText.getText())) {
            mSendView.setEnabled(false);
        } else {
            mSendView.setEnabled(true);
        }
    }

    private static class MyHandler extends Handler {
        WeakReference<PublicSaleTransactionCreateActivity> mActivity;

        MyHandler(PublicSaleTransactionCreateActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            PublicSaleTransactionCreateActivity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                activity.checkGasLimit(false);
            }
        }
    }

    private static final int DECIMAL_DIGITS = 4;
    private static final int HANDLER_WHAT = 1;
    private static final int HANDLER_TIME = 1000;
    private static final String MIN_QKC = "0.0001";
    private static final String CURRENT_ADDRESS = "current_address";

    private MyHandler mHandler;

    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    private QWWallet mDefaultWallet;
    private String mFromAddress;
    private String mContractAddress;
    private String mTokenBuyRate;

    private JustifyTextView mFromAddressText;

    private EditText mAmountText;
    private EditText mPriceText;
    private EditText mGasEditText;
    private EditText mGasLimitText;

    private View mSendView;
    private View mProgressLayout;

    private BigInteger mMainQKCTokenCount;

    private TextView mGasSymbolText;
    private QWToken mGasToken;
    private ArrayList<QWBalance> mGasTokenList;
    private SpinnerPopWindow mMenuPopWindow;
    private String mCurrentGas = Constant.DEFAULT_GAS_PRICE_TO_GWEI;

    private PriceTextWatcher mPriceTextWatcher;
    private MyAmountTextWatcher mAmountTextWatcher;


    private String mCurrentAddress;
    private String mSymbol;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_transaction_public_sacle_create;
    }

    @Override
    public int getActivityTitle() {
        return R.string.public_sale_buy;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mContractAddress = getIntent().getStringExtra(Constant.WALLET_ADDRESS);
        mTokenBuyRate = getIntent().getStringExtra(Constant.KEY_TOKEN_SCALE);
        mFromAddress = getIntent().getStringExtra(Constant.KEY_FROM);
        String amount = getIntent().getStringExtra(Constant.KEY_BALANCE);

        mTopBarView.setTitle(R.string.public_sale_buy);
        mSendView = findViewById(R.id.account_action_next);
        mSendView.setOnClickListener(v -> onPreCreate());
        mSendView.setEnabled(false);

        TextView mBuyTokenSym = findViewById(R.id.transaction_send_token);
        mSymbol = getIntent().getStringExtra(Constant.KEY_TOKEN_SYMBOL);
        mBuyTokenSym.setText(mSymbol);

        mFromAddressText = findViewById(R.id.transaction_from_address);
        JustifyTextView mToAddressText = findViewById(R.id.transaction_send_address);

        //转账地址
        String address = Keys.toChecksumHDAddress(mContractAddress);
        //chain
        BigInteger currentChain = QWWalletUtils.getChainByAddress(this, mContractAddress);
        //shared
        BigInteger currentShard = QWWalletUtils.getShardByAddress(this, mContractAddress, currentChain);
        String text = String.format(getString(R.string.wallet_transaction_address_chain_shard), address, currentChain.toString(), currentShard.toString());
        mToAddressText.setText(text);

        mGasSymbolText = findViewById(R.id.tx_gas_token_symbol);
        mGasSymbolText.setText(getString(R.string.qkc));
        mGasSymbolText.setOnClickListener(this::changeGasToken);
        mGasToken = QWTokenDao.getTQKCToken();

        mPriceTextWatcher = new PriceTextWatcher();
        mPriceText = findViewById(R.id.transaction_send_total_cost);
        mPriceText.addTextChangedListener(mPriceTextWatcher);

        mAmountTextWatcher = new MyAmountTextWatcher();
        mAmountText = findViewById(R.id.transaction_send_amount);
        mAmountText.addTextChangedListener(mAmountTextWatcher);

        mGasEditText = findViewById(R.id.transaction_send_gas);
        mGasEditText.addTextChangedListener(new MyGasWatcher());
        //过滤小数点 保持9位
        ArrayList<InputFilter> filters = new ArrayList<>(Arrays.asList(mGasEditText.getFilters()));
        filters.add(new EditDecimalInputFilter());
        InputFilter[] filter = new InputFilter[filters.size()];
        mGasEditText.setFilters(filters.toArray(filter));

        mGasLimitText = findViewById(R.id.transaction_send_gas_limit);
        mGasLimitText.addTextChangedListener(new MyGasWatcher());
        mGasEditText.setText(QWWalletUtils.getIntTokenFromWei10(Constant.DEFAULT_GAS_PRICE, Convert.Unit.GWEI, 0));
        mGasLimitText.setText(Constant.DEFAULT_GAS_TOKEN_LIMIT);

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));

        if (!TextUtils.isEmpty(amount)) {
            mAmountText.setText(amount);
            mAmountText.setSelection(amount.length());
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
        mTransactionViewModel.error().observe(this, v -> error());
        mTransactionViewModel.createSendFailObserve().observe(this, v -> {
            showProgress(false);
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_send_fail);
        });

        mTransactionViewModel.gasPriceObserve().observe(this, this::onGasSuccess);
        mTransactionViewModel.gasLimitObserve().observe(this, this::onGasLimitSuccess);
        mTransactionViewModel.checkGasToken().observe(this, this::onCheckGasToken);

        //获取当前主钱包
        mTransactionViewModel.findWallet();
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
        mCurrentAddress = mDefaultWallet.getCurrentShareAddress();

        //从服务器同步数据
        updateUi();

        if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
            String shard = QWWalletUtils.parseFullShardForAddress(mContractAddress);
            mTransactionViewModel.gasPrice(shard);
            checkGasLimit(false);
        }
    }

    private void updateUi() {
        mGasTokenList = new ArrayList<>();
        if (TextUtils.isEmpty(mFromAddress)) {
            mFromAddress = mDefaultWallet.getCurrentShareAddress();
        }
        //chain
        BigInteger currentChain = QWWalletUtils.getChainByAddress(this, mFromAddress);
        //shared
        BigInteger currentShard = QWWalletUtils.getShardByAddress(this, mFromAddress, currentChain);
        //转账地址
        String address = Keys.toChecksumHDAddress(mFromAddress);
        String text = String.format(getString(R.string.wallet_transaction_address_chain_shard), address, currentChain.toString(), currentShard.toString());
        mFromAddressText.setText(text);

        mMainQKCTokenCount = BigInteger.ZERO;
        ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
        if (balances != null && !balances.isEmpty()) {
            for (QWBalance balance : balances) {
                QWToken token = balance.getQWToken();
                if (token == null) {
                    continue;
                }
                //获取分片QKC Token数量
                if (Constant.ACCOUNT_TYPE_QKC == token.getType() && token.isNative()) {
                    QWChain chain = balance.getChain();
                    QWShard shard = balance.getQWShard();
                    BigInteger chainId = Numeric.toBigInt(chain.getChain());
                    BigInteger shardId = Numeric.toBigInt(shard.getShard());
                    //同一chain同一分片
                    if (chainId.equals(currentChain) && shardId.equals(currentShard)) {
                        //QKC数量
                        if (QWTokenDao.QKC_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                            mMainQKCTokenCount = mMainQKCTokenCount.add(Numeric.toBigInt(balance.getBalance()));
                        }
                        //不为0的native Token
                        if (BigInteger.ZERO.compareTo(Numeric.toBigInt(balance.getBalance())) < 0) {
                            mGasTokenList.add(balance);
                        }
                    }
                }
            }
        }
        checkGasTokenList();
        mGasSymbolText.setVisibility(View.VISIBLE);
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

    private void showProgress(boolean show) {
        if (show) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }

    private void error() {
        mProgressLayout.setVisibility(View.GONE);

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

    private void hideSoftwareKeyboard(EditText input) {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                mTransactionViewModel.checkGasToken(gasToken, mFromAddress);
            });
        }
        mMenuPopWindow.setAdapter(new TransactionCreateActivity.SpinnerAdapter(mGasTokenList, mGasToken.getSymbol()));
        if (isSoftShowing()) {
            hideSoftwareKeyboard(mGasEditText);
            mGasEditText.postDelayed(() -> mMenuPopWindow.showWidth(view, (int) UiUtils.dpToPixel(175)), 100);
        } else {
            mMenuPopWindow.showWidth(view, (int) UiUtils.dpToPixel(175));
        }
    }

    private void onPreCreate() {
        if (!ConnectionUtil.isInternetConnection(this)) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        //检测amount是否大于0
        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal value = new BigDecimal(amountStr);
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_amount_error);
            mAmountText.requestFocus();
            return;
        }

        String costStr = mPriceText.getText().toString();
        if (Float.parseFloat(costStr) == 0) {
            MyToast.showSingleToastShort(this, R.string.wallet_transaction_to_amount_error);
            mAmountText.requestFocus();
            return;
        }

        //gas
        BigInteger gas = QWWalletUtils.toGWeiFrom10(mGasEditText.getText().toString());
        BigInteger gasLimit = new BigInteger(mGasLimitText.getText().toString().trim());
        BigInteger cost = gas.multiply(gasLimit);
        //购买数量
        BigDecimal costAmount = value.divide(new BigDecimal(mTokenBuyRate), 4, BigDecimal.ROUND_UP);
        BigDecimal amountF = Convert.toWei(costAmount, Convert.Unit.ETHER);
        BigInteger amount = amountF.toBigInteger();

        boolean isSendGasEquals = TextUtils.equals(QWTokenDao.QKC_SYMBOL, mGasToken.getSymbol());

        //当前分片
        BigInteger currChainId = QWWalletUtils.getChainByAddress(getApplicationContext(), mFromAddress);
        BigInteger currShardId = QWWalletUtils.getShardByAddress(getApplicationContext(), mFromAddress, currChainId);
        //token所在分片
        BigInteger tokenChain = QWWalletUtils.getChainByAddress(getApplicationContext(), mContractAddress);
        BigInteger tokenShard = QWWalletUtils.getShardByAddress(getApplicationContext(), mContractAddress, tokenChain);

        //转账数量 < 当前分片总数
        BigInteger totalSendToken = isSendGasEquals ? cost.add(amount) : amount;
        //当前分片约不够
        if (totalSendToken.compareTo(mMainQKCTokenCount) > 0) {
            //1。 获取其他分片余额，是否有分片余额大于该转账总数
            ArrayList<QWBalance> list = hasEnoughQkcShards(amount);
            if (!list.isEmpty()) {
                //如果有，则用该分片购买
                String message = String.format(getString(R.string.transaction_balance_public_buy_chain_switch),
                        currChainId.toString(), currShardId.toString());
                showSwitchShard(list, amountStr, message);
                return;
            }

            //2。 判断所有分片token加起来是否大于该转账数量
            BigInteger total = new BigInteger(amount.toString());
            //2.1 扣除token所在分片的余额
            BigInteger tokenBalance = getTokenChainBalance(tokenChain, tokenShard);
            if (tokenBalance.compareTo(BigInteger.ZERO) > 0) {
                total = total.subtract(tokenBalance);
            }
            //2.2 估算
            if (hasEnoughQkcMerge(total, tokenChain, tokenShard)) {
                String to = QWWalletUtils.changeChainShardToDes(getApplicationContext(), mFromAddress, mContractAddress);
                String valueStr = total.toString();
                QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                dialog.setTitle(R.string.transaction_balance_error_title);
                dialog.setMessage(String.format(getString(R.string.transaction_balance_token_chain_merge), QWTokenDao.QKC_NAME));
                dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
                dialog.setPositiveBtn(R.string.ok, v -> {
                    Intent intent = new Intent(this, MergeActivity.class);
                    intent.putExtra(Constant.KEY_BALANCE, valueStr);
                    intent.putExtra(Constant.WALLET_ADDRESS, to);
                    startActivityForResult(intent, Constant.REQUEST_CODE_SEND_PUBLIC_MERGE);
                    dialog.dismiss();
                    UmengStatistics.transferTokenClick(getApplicationContext(), mDefaultWallet.getCurrentAddress(), mSymbol);
                });
                dialog.show();
                return;
            }

            //余额不足
            MyToast.showSingleToastShort(this, R.string.transaction_balance_error_title);
            return;
        }

        //手续费token不是QKC时
        if (!isSendGasEquals) {
            //判断gasToken余额是否够支付手续费
            for (QWBalance balance : mGasTokenList) {
                if (TextUtils.equals(mGasToken.getSymbol(), balance.getQWToken().getSymbol())) {
                    String b = balance.getBalance();
                    BigInteger v = BigInteger.ZERO;
                    if (!TextUtils.isEmpty(b)) {
                        v = Numeric.toBigInt(balance.getBalance());
                    }
                    if (v.compareTo(cost) < 0) {
                        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                        dialog.setCancelable(false);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setMessage(R.string.wallet_native_transaction_gas_enough);
                        dialog.setPositiveBtn(R.string.ok, (view) -> dialog.dismiss());
                        dialog.show();
                        return;
                    }
                    break;
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

        if (mDefaultWallet.isWatch()) {
            onCreateBuyWatchTransaction();
        } else {
            onCheckPassword();
        }
    }

    //获取所有余额大于该总数的分片
    private ArrayList<QWBalance> hasEnoughQkcShards(BigInteger amount) {
        //默认gas
        BigInteger gas = QWWalletUtils.toGWeiFrom10(mCurrentGas);
        BigInteger gasLimit = new BigInteger(Constant.DEFAULT_GAS_LIMIT);
        BigInteger gasCost = gas.multiply(gasLimit);
        BigInteger total = amount.add(gasCost);

        ArrayList<QWBalance> list = new ArrayList<>();
        ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
        if (balances != null) {
            for (QWBalance balance : balances) {
                if (balance.getQWToken() != null && QWTokenDao.QKC_SYMBOL.equals(balance.getQWToken().getSymbol())
                        && total.compareTo(Numeric.toBigInt(balance.getBalance())) <= 0) {
                    list.add(balance);
                }
            }
        }
        return list;
    }

    private boolean hasEnoughQkcMerge(BigInteger total, BigInteger tokenChain, BigInteger tokenShard) {
        //转账需要的手续费
        BigInteger gasPrice = QWWalletUtils.toGWeiFrom10(mCurrentGas);
        BigInteger gasLimit = new BigInteger(Constant.DEFAULT_GAS_LIMIT);
        BigInteger gas = gasPrice.multiply(gasLimit);

        BigInteger totalShards = BigInteger.ZERO;
        ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
        if (balances != null) {
            for (QWBalance balance : balances) {
                if (balance.getQWToken() != null && QWTokenDao.QKC_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                    BigInteger b = Numeric.toBigInt(balance.getBalance());
                    //屏蔽Token分片本身
                    QWChain chain = balance.getChain();
                    QWShard shard = balance.getQWShard();
                    if (Numeric.toBigInt(chain.getChain()).equals(tokenChain) && Numeric.toBigInt(shard.getShard()).equals(tokenShard)) {
                        continue;
                    }

                    if (gas.compareTo(b) < 0) {
                        //减去手续费
                        b = b.subtract(gas);
                        totalShards = totalShards.add(b);
                    }
                }
            }
        }

        return totalShards.compareTo(total) >= 0;
    }

    private BigInteger getTokenChainBalance(BigInteger tokenChain, BigInteger tokenShard) {
        //转账需要的手续费
        ForeignCollection<QWBalance> balances = mDefaultWallet.getCurrentAccount().getBalances();
        if (balances != null) {
            for (QWBalance balance : balances) {
                if (balance.getQWToken() != null && QWTokenDao.QKC_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                    QWChain chain = balance.getChain();
                    QWShard shard = balance.getQWShard();
                    if (Numeric.toBigInt(chain.getChain()).equals(tokenChain) && Numeric.toBigInt(shard.getShard()).equals(tokenShard)) {
                        return Numeric.toBigInt(balance.getBalance());
                    }
                }
            }
        }

        return BigInteger.ZERO;
    }

    private void onCheckPassword() {
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mDefaultWallet,
                new SystemUtils.OnCheckPassWordListenerImp() {
                    @Override
                    public void onPasswordSuccess(String password) {
                        onCreateBuyTransaction(password);
                    }
                });
    }

    private void onCreateBuyTransaction(String password) {
        showProgress(true);
        String gas = mGasEditText.getText().toString().trim();
        String gasLimitStr = mGasLimitText.getText().toString().trim();
        BigInteger gasPrice = QWWalletUtils.toGWeiFrom10(gas);
        BigInteger gasLimit = new BigInteger(gasLimitStr);

        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal value = new BigDecimal(amountStr);
        BigDecimal costAmount = value.divide(new BigDecimal(mTokenBuyRate), 18, BigDecimal.ROUND_UP);
        BigInteger amount = QWWalletUtils.toWeiFrom10(costAmount.toString());

        String txId = QWTokenDao.TQKC_ADDRESS;
        mTransactionViewModel.createBuyTokenTransfer(
                password,
                mFromAddress, mContractAddress,
                amount,
                gasPrice, gasLimit,
                Constant.sNetworkId,
                txId, mGasToken.getAddress()
        );
        UmengStatistics.walletPublicSaleCreateTransactionClickCount(getApplicationContext(), mSymbol, mFromAddress);
    }

    private void onCreateBuyWatchTransaction() {
        showProgress(true);
        String gas = mGasEditText.getText().toString().trim();
        String gasLimitStr = mGasLimitText.getText().toString().trim();
        BigInteger gasPrice = QWWalletUtils.toGWeiFrom10(gas);
        BigInteger gasLimit = new BigInteger(gasLimitStr);

        String amountStr = mAmountText.getText().toString().trim();
        BigDecimal value = new BigDecimal(amountStr);
        BigDecimal costAmount = value.divide(new BigDecimal(mTokenBuyRate), 18, BigDecimal.ROUND_UP);
        BigInteger amount = QWWalletUtils.toWeiFrom10(costAmount.toString());

        String txId = QWTokenDao.TQKC_ADDRESS;
        UmengStatistics.walletPublicSaleCreateTransactionClickCount(getApplicationContext(), mSymbol, mFromAddress);
    }

    //创建交易条目成功
    private void createSuccess(String[] hash) {
        if (hash != null && hash.length == 2) {
            BigInteger gas = QWWalletUtils.toGWeiFrom10(mGasEditText.getText().toString());
            BigInteger gasLimit = new BigInteger(mGasLimitText.getText().toString().trim());
            BigInteger cost = gas.multiply(gasLimit);
            String costStr = QWWalletUtils.getIntTokenFromWei10(cost.toString(), true, Constant.QKC_DECIMAL_NUMBER);

            String amountStr = mAmountText.getText().toString().trim();
            BigDecimal value = new BigDecimal(amountStr);
            BigDecimal costAmount = value.divide(new BigDecimal(mTokenBuyRate), 9, BigDecimal.ROUND_UP);
            String amount = costAmount.stripTrailingZeros().toPlainString();

            TransactionSendActivity.startQKCTransactionSendActivity(this,
                    mFromAddress,
                    mContractAddress,
                    amount,
                    costStr,
                    hash[0],
                    hash[1],
                    "",
                    mDefaultWallet.getCurrentAccount().getType(),
                    false,
                    TransactionSendActivity.KEY_FROM_PUBLICSALE,
                    mSymbol,
                    mContractAddress,
                    mGasToken.getSymbol().toUpperCase());

            UmengStatistics.payTokenClick(getApplicationContext(), mDefaultWallet.getCurrentAddress(), mSymbol);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constant.REQUEST_CODE_SEND_PUBLIC_SWITCH) {
                finish();
            } else if (requestCode == Constant.REQUEST_CODE_SEND_PUBLIC_MERGE) {

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(Constant.KEY_RESULT, requestCode);

                if (!TextUtils.isEmpty(mContractAddress) && !TextUtils.isEmpty(mCurrentAddress)) {
                    //切换分片
                    BigInteger chain = QWWalletUtils.getChainByAddress(getApplicationContext(), mContractAddress);
                    BigInteger shard = QWWalletUtils.getShardByAddress(getApplicationContext(), mContractAddress, chain);
                    BigInteger currChain = QWWalletUtils.getChainByAddress(getApplicationContext(), mCurrentAddress);
                    BigInteger currShard = QWWalletUtils.getShardByAddress(getApplicationContext(), mCurrentAddress, currChain);
                    if (!chain.equals(currChain) || !shard.equals(currShard)) {
                        SharedPreferencesUtils.setCurrentChain(getApplicationContext(), mCurrentAddress, Numeric.toHexStringWithPrefix(chain));
                        SharedPreferencesUtils.setCurrentShard(getApplicationContext(), mCurrentAddress, Numeric.toHexStringWithPrefix(shard));
                        intent.putExtra(Constant.KEY_CHANGE_SHARD, true);
                    }
                }

                startActivity(intent);
            } else if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTIONS) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        }
    }

    //选择转账的分片
    private void showSwitchShard(ArrayList<QWBalance> list, String total, String message) {
        WheelBalancePopWindow popWindow = new WheelBalancePopWindow(this);
        popWindow.setItem(list);
        popWindow.setTitle(message);
        popWindow.setOnNumberPickListener(index -> {

            QWBalance balance = list.get(index);
            QWChain chain = balance.getChain();
            QWShard shard = balance.getQWShard();
            if (chain != null && shard != null) {
                String fromAddress = Keys.toChecksumHDAddress(Numeric.selectChainAndShardAddress(mDefaultWallet.getCurrentAddress(), chain.getChain(), shard.getShard()));
                Intent intent = new Intent(PublicSaleTransactionCreateActivity.this, PublicSaleTransactionCreateActivity.class);
                intent.putExtra(Constant.KEY_FROM, fromAddress);
                intent.putExtra(Constant.WALLET_ADDRESS, mContractAddress);
                intent.putExtra(Constant.KEY_TOKEN_SCALE, mTokenBuyRate);
                intent.putExtra(Constant.KEY_TOKEN_SYMBOL, mSymbol);
                intent.putExtra(Constant.KEY_BALANCE, total);
                startActivityForResult(intent, Constant.REQUEST_CODE_SEND_PUBLIC_SWITCH);
                UmengStatistics.transferTokenClick(getApplicationContext(), mDefaultWallet.getCurrentAddress(), mSymbol);
            }
        });
        popWindow.show();
    }


    private void onGasSuccess(String gas) {
        mGasEditText.setText(gas);
        mCurrentGas = gas;
    }

    private void onGasLimitSuccess(BigInteger gas) {
        String text = gas.toString();
        mGasLimitText.setText(text);
    }

    private void checkGasLimit(boolean post) {
        if (mDefaultWallet == null) {
            return;
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            return;
        }

        if (mHandler != null && mHandler.hasMessages(HANDLER_WHAT)) {
            return;
        }

        if (TextUtils.isEmpty(mPriceText.getText())) {
            return;
        }

        //检测amount是否大于0
        String costStr = mPriceText.getText().toString();
        BigInteger cost = QWWalletUtils.toWeiFrom10(costStr);
        if (post) {
            mHandler.sendEmptyMessageDelayed(HANDLER_WHAT, HANDLER_TIME);
        } else {
            if (mDefaultWallet.getCurrentAccount().isEth()) {
                mTransactionViewModel.ethGasLimitForBuy(mDefaultWallet.getCurrentAddress(), mContractAddress, cost);
            } else {
                mTransactionViewModel.gasLimitForBuy(
                        (TextUtils.isEmpty(mFromAddress) ? mDefaultWallet.getCurrentShareAddress() : mFromAddress),
                        mContractAddress, cost, QWTokenDao.TQKC_ADDRESS, mGasToken.getAddress()
                );
            }
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
        //切换gas token
        mGasToken = token;
        mGasSymbolText.setText(mGasToken.getSymbol().toUpperCase());
        //根据比例设置新的gas price
        BigDecimal currentGas = new BigDecimal(mCurrentGas);
        mGasEditText.setText(currentGas.multiply(token.getRefundPercentage()).stripTrailingZeros().toPlainString());
        //设置gas limit
        checkGasLimit(false);
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
}
