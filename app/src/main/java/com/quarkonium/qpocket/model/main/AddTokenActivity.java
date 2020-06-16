package com.quarkonium.qpocket.model.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.model.main.viewmodel.MainWallerViewModel;
import com.quarkonium.qpocket.model.main.viewmodel.MainWalletViewModelFactory;
import com.quarkonium.qpocket.model.transaction.TransactionCreateActivity;
import com.quarkonium.qpocket.rx.NetworkChangeEvent;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class AddTokenActivity extends BaseActivity {

    private class MyAddressTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(s)) {
                mActionView.setEnabled(false);
            } else {
                mActionView.setEnabled(true);
            }

            if (mToken != null && TextUtils.equals(mToken.getAddress(), s)) {
                mTokenInfoLayout.setVisibility(View.VISIBLE);
                mActionView.setText(R.string.guide_button_finish);
            } else {
                mTokenInfoLayout.setVisibility(View.GONE);
                mActionView.setText(R.string.add_token_button_detect);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private EditText mAddressView;

    private View mTokenInfoLayout;
    private TextView mNameView;
    private TextView mSymbolView;
    private TextView mDecimalView;

    private TextView mActionView;
    private View mProgressView;

    private QWToken mToken;
    private QWWallet mWallet;
    private int mAccountType;

    private String mToAddress;

    @Inject
    public MainWalletViewModelFactory mMainWallerFragmentFactory;
    private MainWallerViewModel mMainWalletFragmentViewModel;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_add_token_layout;
    }

    @Override
    public int getActivityTitle() {
        return R.string.add_token_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

        mTopBarView.setTitle(R.string.add_token_title);

        mProgressView = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressView, UiUtils.dpToPixel(4));

        mAddressView = findViewById(R.id.add_token_address);

        mTokenInfoLayout = findViewById(R.id.token_msg_layout);
        mNameView = findViewById(R.id.token_name);
        mSymbolView = findViewById(R.id.token_symbol);
        mDecimalView = findViewById(R.id.token_decimal);

        mActionView = findViewById(R.id.detect_action);
        mActionView.setEnabled(false);
        mActionView.setOnClickListener(v -> onClick());

        mAddressView.addTextChangedListener(new MyAddressTextWatcher());
    }

    private void initAccountType() {
        if (WalletUtils.isValidAddress(mWallet.getCurrentAddress())) {
            mAccountType = Constant.ACCOUNT_TYPE_ETH;
        } else if (TronWalletClient.isTronAddressValid(mWallet.getCurrentAddress())) {
            mAccountType = Constant.ACCOUNT_TYPE_TRX;
        } else {
            mAccountType = Constant.ACCOUNT_TYPE_QKC;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mWallet = getIntent().getParcelableExtra(Constant.KEY_WALLET);
        initAccountType();
        if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
            mAddressView.setHint(R.string.add_token_trx_contact);
        } else if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            mAddressView.setHint(R.string.add_token_qkc_contact);
        }

        mMainWalletFragmentViewModel = new ViewModelProvider(this, mMainWallerFragmentFactory)
                .get(MainWallerViewModel.class);
        mMainWalletFragmentViewModel.fetchAddTokensObserver().observe(this, this::onFetchSuccess);
        mMainWalletFragmentViewModel.error().observe(this, v -> onError());

        mMainWalletFragmentViewModel.addTokenStatus().observe(this, this::onAddTokenSuccess);

        String address = getIntent().getStringExtra(Constant.KEY_TOKEN_ADDRESS);
        if (!TextUtils.isEmpty(address)) {
            mAddressView.setText(address);

            if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
                //查询合约
                mProgressView.setVisibility(View.VISIBLE);
                mMainWalletFragmentViewModel.fetchAddToken(address.trim(), mAccountType);
            }

            mToAddress = getIntent().getStringExtra(Constant.WALLET_ADDRESS);
        }
    }

    private void onClick() {
        if (mTokenInfoLayout.getVisibility() == View.VISIBLE) {
            //添加数据库
            addToken();
        } else {
            String address = mAddressView.getText().toString();
            if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
                if (TextUtils.isEmpty(address) ||
                        !(TronWalletClient.isTronAddressValid(address.trim()) || TronWalletClient.isTronErc10TokenAddressValid(address.trim()))) {
                    MyToast.showSingleToastShort(this, R.string.add_token_trx_fail);
                    return;
                }
            } else if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
                if (TextUtils.isEmpty(address) || !WalletUtils.isValidAddress(address.trim())) {
                    MyToast.showSingleToastShort(this, R.string.add_token_fail);
                    return;
                }
            } else if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
                if (TextUtils.isEmpty(address) ||
                        !(QWWalletUtils.isQKCValidAddress(address.trim()) || QWWalletUtils.isQKCNativeTokenName(address.trim()))) {
                    MyToast.showSingleToastShort(this, R.string.add_token_qkc_fail);
                    return;
                }
            }

            if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
                MyToast.showSingleToastShort(this, R.string.network_error);
                return;
            }
            //查询合约
            mProgressView.setVisibility(View.VISIBLE);
            mMainWalletFragmentViewModel.fetchAddToken(address.trim(), mAccountType);
        }
    }

    private void onFetchSuccess(QWToken token) {
        mToken = token;
        mNameView.setText(mToken.getName());
        mSymbolView.setText(mToken.getSymbol());
        mDecimalView.setText(mToken.getDecimals());

        mTokenInfoLayout.setVisibility(View.VISIBLE);
        mActionView.setText(R.string.guide_button_finish);
        mProgressView.setVisibility(View.GONE);
    }

    private void onError() {
        showError();
        mProgressView.setVisibility(View.GONE);
    }

    //像数据库添加数据
    private void addToken() {
        if (mWallet != null) {
            mMainWalletFragmentViewModel.addToken(mWallet.getCurrentAddress(), mToken);
            UmengStatistics.addTokenClickCount(getApplicationContext(), mToken.getSymbol(), mWallet.getCurrentAddress());
        }
    }

    private void onAddTokenSuccess(String address) {
        if (TextUtils.isEmpty(address)) {
            showError();
        } else if (!TextUtils.isEmpty(mToAddress)) {
            Intent intent = new Intent(this, TransactionCreateActivity.class);
            intent.putExtra(Constant.WALLET_ADDRESS, mToAddress);
            intent.putExtra(Constant.KEY_TOKEN_ADDRESS, address);
            startActivity(intent);
            finish();
            NetworkChangeEvent messageEvent = new NetworkChangeEvent("");
            EventBus.getDefault().postSticky(messageEvent);
        } else {
            Intent intent = getIntent();
            intent.putExtra(Constant.KEY_TOKEN_ADDRESS, address);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    private void showError() {
        if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
            MyToast.showSingleToastShort(this, R.string.add_token_trx_fail);
        } else if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            MyToast.showSingleToastShort(this, R.string.add_token_qkc_fail);
        } else {
            MyToast.showSingleToastShort(this, R.string.add_token_fail);
        }
    }
}
