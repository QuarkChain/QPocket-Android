package com.quarkonium.qpocket.model.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.entity.ErrorEnvelope;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.CreateWalletException;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.wallet.viewmodel.ImportWalletViewModel;
import com.quarkonium.qpocket.model.wallet.viewmodel.ImportWalletViewModelFactory;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.R;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

//钱包编辑界面
public class CreateChildAccountActivity extends BaseActivity {

    private static final String KEY_WALLET = "key_wallet";

    public static void startActivity(Activity activity, QWWallet wallet) {
        Intent intent = new Intent(activity, CreateChildAccountActivity.class);
        intent.putExtra(KEY_WALLET, wallet);
        activity.startActivity(intent);
    }

    private QWWallet mQuarkWallet;

    @Inject
    ImportWalletViewModelFactory mImportWalletViewModelFactory;
    private ImportWalletViewModel mImportWalletViewModel;

    private View mProgressLayout;
    private int mCoinType;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_create_child_account;
    }

    @Override
    public int getActivityTitle() {
        return R.string.create_child_account_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        if (mQuarkWallet == null) {
            return;
        }
        mTopBarView.setTitle(R.string.create_child_account_title);

        TextView qkcText = findViewById(R.id.create_account_qkc_text);
        qkcText.setText(String.format(getString(R.string.create_child_account_btn), "QKC"));
        TextView ethText = findViewById(R.id.create_account_eth_text);
        ethText.setText(String.format(getString(R.string.create_child_account_btn), "ETH"));
        TextView trxText = findViewById(R.id.create_account_trx_text);
        trxText.setText(String.format(getString(R.string.create_child_account_btn), "TRX"));

        findViewById(R.id.create_account_qkc).setOnClickListener((v) -> onCreateQKCChildAccount());
        findViewById(R.id.create_account_eth).setOnClickListener((v) -> onCreateETHChildAccount());
        findViewById(R.id.create_account_trx).setOnClickListener((v) -> onCreateTRXChildAccount());

        mProgressLayout = findViewById(R.id.progress_layout);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        mQuarkWallet = getIntent().getParcelableExtra(KEY_WALLET);
        if (savedInstanceState != null) {
            mQuarkWallet = savedInstanceState.getParcelable(KEY_WALLET);
        }
        super.onCreate(savedInstanceState);

        mImportWalletViewModel = new ViewModelProvider(this, mImportWalletViewModelFactory)
                .get(ImportWalletViewModel.class);
        mImportWalletViewModel.importWallet().observe(this, this::onCreateAccountFinish);
        mImportWalletViewModel.error().observe(this, this::onError);
        mImportWalletViewModel.progress().observe(this, this::showProgress);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //重启时更新钱包数据
        outState.putParcelable(KEY_WALLET, mQuarkWallet);
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mQuarkWallet = savedInstanceState.getParcelable(KEY_WALLET);
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }

    private void onCreateQKCChildAccount() {
        mCoinType = Constant.HD_PATH_CODE_QKC;
        mImportWalletViewModel.createChildAccount(mQuarkWallet, Constant.HD_PATH_CODE_QKC, 0, true);
    }

    private void onCreateETHChildAccount() {
        mCoinType = Constant.HD_PATH_CODE_ETH;
        mImportWalletViewModel.createChildAccount(mQuarkWallet, Constant.HD_PATH_CODE_ETH, 0, true);
    }

    private void onCreateTRXChildAccount() {
        mCoinType = Constant.HD_PATH_CODE_TRX;
        mImportWalletViewModel.createChildAccount(mQuarkWallet, Constant.HD_PATH_CODE_TRX, 0, true);
    }

    private void onCreateAccountFinish(QWWallet wallet) {
        showProgress(false);
        //创建成功
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(-1, -1);
        MyToast.showSingleToastLong(this, R.string.create_child_account_success);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        showProgress(false);
        if (errorEnvelope.code == Constant.ErrorCode.WALLET_EXIT) {
            //已存在
            CreateWalletException error = (CreateWalletException) errorEnvelope.throwable;
            if (error != null) {
                int accountIndex = error.getAccountIndex();
                QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                String message = getString(R.string.create_child_account_exist, accountIndex + "");
                dialog.setMessage(message);
                dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
                dialog.setPositiveBtn(R.string.ok, (v) -> {
                    dialog.dismiss();
                    mImportWalletViewModel.createChildAccount(mQuarkWallet, mCoinType, accountIndex + 1, false);
                });
                dialog.show();
                return;
            }
        }
        MyToast.showSingleToastLong(this, R.string.create_child_account_fail);
    }
}
