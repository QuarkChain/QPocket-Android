package com.quarkonium.qpocket.model.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.wallet.viewmodel.CreateWalletViewModel;
import com.quarkonium.qpocket.model.wallet.viewmodel.CreateWalletViewModelFactory;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

//备份钱包助记词提示界面
public class BackupPhraseHintActivity extends BaseActivity {

    private String mWalletKey;

    private String mMnemonic;
    private String mPassword;
    private String mPasswordHint;

    private boolean mIsExport;
    private boolean mIsResultBackup;

    private View mProgressLayout;

    private boolean mIsColdMode;

    @Inject
    CreateWalletViewModelFactory mWalletViewModelFactory;
    CreateWalletViewModel mViewModel;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_home_backup_hint;
    }

    @Override
    public int getActivityTitle() {
        return R.string.backup_wallet_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mWalletKey = getIntent().getStringExtra(Constant.WALLET_KEY);

        mIsColdMode = getIntent().getBooleanExtra(Constant.KEY_COLD_MODE, false);

        mMnemonic = getIntent().getStringExtra(Constant.KEY_MNEMONIC);
        mPassword = getIntent().getStringExtra(Constant.KEY_PASSWORD);
        mPasswordHint = getIntent().getStringExtra(Constant.KEY_PASSWORD_HINT);

        mIsExport = getIntent().getBooleanExtra(Constant.IS_EXPORT_PHRASE, false);
        mIsResultBackup = getIntent().getBooleanExtra(Constant.IS_RESULT_BACKUP_PHRASE, false);

        mTopBarView.setTitle(R.string.backup_wallet_title);
        mTopBarView.setRightText(R.string.backup_wallet_skip);
        View skip = mTopBarView.getRightTextView();
        skip.setOnClickListener(v -> {
            createWallet();
            UmengStatistics.topBarCreateWalletSkipClickCount(getApplicationContext(), QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
        });
        findViewById(R.id.account_action_next).setOnClickListener(v -> goNext());

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));

        if (mIsExport || mIsResultBackup) {
            skip.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this, mWalletViewModelFactory)
                .get(CreateWalletViewModel.class);
        mViewModel.createdWallet().observe(this, v -> goMain());
        mViewModel.error().observe(this, v -> onError());
        mViewModel.progress().observe(this, this::showProgress);
    }

    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void goNext() {
        Intent intent = new Intent(this, BackupPhraseActivity.class);
        intent.putExtra(Constant.WALLET_KEY, mWalletKey);

        intent.putExtra(Constant.KEY_MNEMONIC, mMnemonic);
        intent.putExtra(Constant.KEY_PASSWORD, mPassword);
        intent.putExtra(Constant.KEY_PASSWORD_HINT, mPasswordHint);

        intent.putExtra(Constant.IS_EXPORT_PHRASE, mIsExport);
        intent.putExtra(Constant.IS_RESULT_BACKUP_PHRASE, mIsResultBackup);

        intent.putExtra(Constant.KEY_COLD_MODE, mIsColdMode);

        startActivityForResult(intent, Constant.RESULT_CODE_BACKUP_PHRASE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.RESULT_CODE_BACKUP_PHRASE && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private void createWallet() {
        mViewModel.newWallet(mMnemonic, mPassword, mPasswordHint);
    }

    private void onError() {
        showProgress(false);
        MyToast.showSingleToastShort(this, R.string.create_password_fail);
    }

    private void showProgress(boolean isShow) {
        if (isShow) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }
}
