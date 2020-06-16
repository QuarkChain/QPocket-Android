package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.crypto.utils.Convert;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.main.view.SpinnerPopWindow;
import com.quarkonium.qpocket.model.transaction.TransactionCreateActivity;
import com.quarkonium.qpocket.model.transaction.bean.MergeBean;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.bean.EditDecimalInputFilter;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class MergeEditPopWindow extends DialogFragment {
    public interface MergePopWindowClickListener {
        void onClick(MergeBean bean);
    }

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
            if (TextUtils.isEmpty(s)) {
                mSendView.setEnabled(false);
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

                check();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private void check() {
        if (TextUtils.isEmpty(mAmountText.getText()) || TextUtils.isEmpty(mGasPriceText.getText())
                || Float.parseFloat(mAmountText.getText().toString()) == 0
                || Float.parseFloat(mGasPriceText.getText().toString()) == 0) {
            mSendView.setEnabled(false);
        } else {
            mSendView.setEnabled(true);
        }
    }

    private final static int MAX_GAS_LIMIT_VALUE = 100000;
    private final static int MIN_GAS_LIMIT_VALUE = 30000;
    private static final int DECIMAL_DIGITS = 14;

    private EditText mAmountText;
    private EditText mGasPriceText;
    private SeekBar mGasLimitSeekBar;
    private TextView mGasLimitValueText;
    private View mSendView;
    private View mProgressView;

    private TextView mGasSymbolText;
    private SpinnerPopWindow mMenuPopWindow;

    private MergePopWindowClickListener mListener;

    private MergeBean mMergeBean;
    private String mTitle;
    private String mSendToken;
    private BigInteger mTotalAmount;
    private QWToken mGasToken;

    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    @Override
    public void onAttach(@NotNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CompositeSDKFullScreenDialog);

        mTransactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                .get(TransactionViewModel.class);
        mTransactionViewModel.checkGasToken().observe(this, this::onCheckGasToken);
        mTransactionViewModel.progress().observe(this, this::showProgress);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.merge_edit_pop_layout, container, false);
        rootView.setFocusable(true);
        rootView.setFocusableInTouchMode(true);

        TextView mTitleTip = rootView.findViewById(R.id.merge_edit_pop_tip);
        mTitleTip.setText(mTitle);
        TextView mSendSymbolText = rootView.findViewById(R.id.merge_edit_token_symbol);
        mSendSymbolText.setText(mSendToken);

        mAmountText = rootView.findViewById(R.id.merge_edit_amount);
        String mAmount = QWWalletUtils.getIntTokenNotScaleFromWei16(mMergeBean.amount.toString(16));
        mAmountText.setText(mAmount);

        mGasPriceText = rootView.findViewById(R.id.merge_edit_gas_price);
        String mGasPrice = mMergeBean.gasPrice.toString(16);
        mGasPriceText.setText(QWWalletUtils.getIntTokenFromWei16(mGasPrice, Convert.Unit.GWEI, Constant.QKC_DECIMAL_NUMBER));
        //过滤小数点 保持9位
        ArrayList<InputFilter> filters = new ArrayList<>(Arrays.asList(mGasPriceText.getFilters()));
        filters.add(new EditDecimalInputFilter());
        InputFilter[] filter = new InputFilter[filters.size()];
        mGasPriceText.setFilters(filters.toArray(filter));

        mGasLimitValueText = rootView.findViewById(R.id.merge_edit_gas_limit_value);
        String mGasLimit = mMergeBean.gasLimit.toString();
        mGasLimitValueText.setText(mGasLimit);
        mGasLimitSeekBar = rootView.findViewById(R.id.merge_edit_gas_limit);
        mGasLimitSeekBar.setLineColor("#03c774");
        mGasLimitSeekBar.setSeekLength(0, MAX_GAS_LIMIT_VALUE - MIN_GAS_LIMIT_VALUE, 0, 1);
        mGasLimitSeekBar.setOnSeekChangeListener(new SeekBar.OnSeekChangeListener() {
            @Override
            public void onSeekStarted(float currentValue, float currDisplayValue) {

            }

            @Override
            public void onSeekChanged(float currentValue, float currDisplayValue) {
                String value = String.valueOf((int) (currentValue + MIN_GAS_LIMIT_VALUE));
                mGasLimitValueText.setText(value);
            }

            @Override
            public void onSeekStopped(float currentValue, float currDisplayValue) {

            }
        });
        mGasLimitSeekBar.setValue(Integer.parseInt(mGasLimit) - MIN_GAS_LIMIT_VALUE);

        mGasSymbolText = rootView.findViewById(R.id.merge_edit_gas_token_symbol);
        mGasSymbolText.setOnClickListener(this::showSymbolWindow);
        mGasSymbolText.setText(mGasToken.getSymbol().toUpperCase());

        mSendView = rootView.findViewById(R.id.positive_btn);
        mSendView.setOnClickListener(v -> onClick());
        rootView.findViewById(R.id.negative_btn).setOnClickListener(v -> dismiss());

        mProgressView = rootView.findViewById(R.id.progress_layout);

        mAmountText.addTextChangedListener(new MyAmountTextWatcher());
        mGasPriceText.addTextChangedListener(new MyGasWatcher());
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            Window window = getDialog().getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams p = window.getAttributes(); // 获取对话框当前的参数值
                p.gravity = Gravity.BOTTOM;
                p.height = ViewGroup.LayoutParams.WRAP_CONTENT; // 高度设置
                p.width = getResources().getDisplayMetrics().widthPixels; // 宽度设置为屏幕
                window.setAttributes(p);
            }
        }
    }

    public void setOnClickListener(MergePopWindowClickListener listener) {
        mListener = listener;
    }

    public void setData(MergeBean bean, String title, String sendToken) {
        mMergeBean = bean;
        mTitle = title;
        mSendToken = sendToken;
        mGasToken = getTokenSymbol(mMergeBean.gasTokenId, mMergeBean.gasTokenList);
        mTotalAmount = Numeric.toBigInt(bean.balance.getBalance());
    }

    private QWToken getTokenSymbol(String tokenId, ArrayList<QWBalance> list) {
        if (list != null && !list.isEmpty()) {
            for (QWBalance balance : list) {
                if (TextUtils.equals(tokenId, balance.getQWToken().getAddress())) {
                    return balance.getQWToken();
                }
            }
        }
        return QWTokenDao.getTQKCToken();
    }

    private void showSymbolWindow(View view) {
        if (mMergeBean == null || mMergeBean.gasTokenList.isEmpty()) {
            return;
        }
        if (mMenuPopWindow == null) {
            mMenuPopWindow = new SpinnerPopWindow(view.getContext());
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
                if (TextUtils.equals(gasToken.getAddress(), QWTokenDao.TQKC_ADDRESS)) {
                    //QKC
                    mTransactionViewModel.checkGasTokenByChain(gasToken, "");
                    return;
                }
                mTransactionViewModel.checkGasTokenByChain(gasToken, balance.getChain().getChain());
            });
        }
        mMenuPopWindow.setAdapter(new TransactionCreateActivity.SpinnerAdapter(mMergeBean.gasTokenList, mGasToken.getSymbol().toUpperCase()));

        hideSoftwareKeyboard(mAmountText);
        mGasSymbolText.postDelayed(() -> mMenuPopWindow.showWidth(view, (int) UiUtils.dpToPixel(175)), 100);
    }

    private void onCheckGasToken(QWToken token) {
        mProgressView.setVisibility(View.GONE);
        if (token == null) {
            //当前token不支持作为手续费
            QuarkSDKDialog dialog = new QuarkSDKDialog(getContext());
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
        String gasPrice = QWWalletUtils.getIntTokenFromWei16(mMergeBean.normalGasPrice.toString(16), Convert.Unit.GWEI);
        BigDecimal currentGas = new BigDecimal(gasPrice).multiply(token.getRefundPercentage());
        mGasPriceText.setText(currentGas.stripTrailingZeros().toPlainString());
        //更新兑换比例
        if (mMergeBean.gasTokenList != null) {
            for (QWBalance balance : mMergeBean.gasTokenList) {
                QWToken tok = balance.getQWToken();
                if (TextUtils.equals(tok.getAddress(), token.getAddress())) {
                    tok.setRefundPercentage(token.getRefundPercentage());
                    tok.setReserveTokenBalance(token.getReserveTokenBalance());
                }
            }
        }
    }

    private void showProgress(boolean v) {
        mProgressView.setVisibility(v ? View.VISIBLE : View.GONE);
    }

    private void hideSoftwareKeyboard(EditText input) {
        try {
            InputMethodManager imm = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onClick() {
        if (mListener != null) {
            String amountStr = mAmountText.getText().toString().trim();
            BigDecimal amount = Convert.toWei(amountStr, Convert.Unit.ETHER);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                MyToast.showSingleToastShort(mAmountText.getContext(), R.string.wallet_transaction_to_amount_error);
                mAmountText.requestFocus();
                return;
            }

            String gasPrice = mGasPriceText.getText().toString().trim();
            BigInteger gas = QWWalletUtils.toGWeiFrom10(gasPrice);
            BigInteger gasLimit = new BigInteger(String.valueOf((int) (mGasLimitSeekBar.getValue() + MIN_GAS_LIMIT_VALUE)));
            BigInteger cost = gas.multiply(gasLimit);

            //进行校验
            if (TextUtils.equals(mGasToken.getAddress(), mMergeBean.balance.getQWToken().getAddress())) {
                //用当前转账Token作为手续费
                BigInteger totalQKC = cost.add(amount.toBigInteger());
                if (totalQKC.compareTo(mTotalAmount) > 0) {
                    MyToast.showSingleToastShort(mAmountText.getContext(), R.string.transaction_balance_error_title);
                    return;
                }
            } else if (mMergeBean.gasTokenList != null && !mMergeBean.gasTokenList.isEmpty()) {
                //用非当前转账的其他Native Token作为手续费
                BigInteger totalQKC = amount.toBigInteger();
                if (totalQKC.compareTo(mTotalAmount) > 0) {
                    MyToast.showSingleToastShort(mAmountText.getContext(), R.string.transaction_balance_error_title);
                    return;
                }

                for (QWBalance balance : mMergeBean.gasTokenList) {
                    if (TextUtils.equals(mGasToken.getAddress(), balance.getQWToken().getAddress())) {
                        //gas费用不足
                        if (Numeric.toBigInt(balance.getBalance()).compareTo(cost) < 0) {
                            String text = getString(R.string.wallet_merge_transaction_gas_enough);
                            text = String.format(text, mGasToken.getSymbol().toUpperCase());
                            MyToast.showSingleToastShort(mAmountText.getContext(), text);
                            return;
                        }
                        break;
                    }
                }
            }

            //校验合格
            mMergeBean.amount = amount.toBigInteger();
            mMergeBean.gasPrice = gas;
            mMergeBean.gasLimit = gasLimit;
            mMergeBean.gasTokenId = mGasToken.getAddress();
            mMergeBean.refundPercentage = mGasToken.getRefundPercentage();
            mMergeBean.reserveTokenBalance = mGasToken.getReserveTokenBalance();
            mListener.onClick(mMergeBean);
        }
        dismiss();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mMenuPopWindow = null;
        mListener = null;
        mMergeBean = null;
        mGasToken = null;
    }
}
