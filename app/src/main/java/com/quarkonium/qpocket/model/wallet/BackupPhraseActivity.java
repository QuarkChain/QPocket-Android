package com.quarkonium.qpocket.model.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.quarkonium.qpocket.view.QuarkChooseLanDialog;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

//备份钱包助记词界面
public class BackupPhraseActivity extends BaseActivity {

    @Inject
    CreateWalletViewModelFactory mWalletViewModelFactory;
    CreateWalletViewModel mViewModel;

    private TextView mPhraseTextView;
    private View mProgressLayout;
    private TextView mLanguageView;

    private String mWalletKey;

    private String mMnemonic;
    private String mPassword;
    private String mPasswordHint;

    private boolean mIsExport;
    private boolean mIsResultBackup;

    private int mLanguageIndex = 0;
    private String mENMnemonic;
    private String mCNMnemonic;
    private String mTWMnemonic;

    private boolean mIsColdMode;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_home_backup_remember;
    }

    @Override
    public int getActivityTitle() {
        return R.string.backup_wallet_remember_title;
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

        findViewById(R.id.account_action_next).setOnClickListener((view) -> goNext());
        mPhraseTextView = findViewById(R.id.backup_phrase_label);

        mTopBarView.setTitle(R.string.backup_wallet_remember_title);
        mTopBarView.setRightText(R.string.backup_wallet_skip);
        View skip = mTopBarView.getRightTextView();
        skip.setOnClickListener(v -> {
            createWallet();
            UmengStatistics.topBarCreateWalletSkipClickCount(getApplicationContext(), QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
        });
        if (mIsExport || mIsResultBackup) {
            skip.setVisibility(View.GONE);
        } else {
            mLanguageView = findViewById(R.id.create_wallet_language);
            mLanguageView.setVisibility(View.VISIBLE);
            mLanguageView.setOnClickListener(v -> chooseLanguage());

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mPhraseTextView.getLayoutParams();
            lp.topMargin = (int) UiUtils.dpToPixel(30);
            mPhraseTextView.setLayoutParams(lp);
        }

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mViewModel = new ViewModelProvider(this, mWalletViewModelFactory)
                .get(CreateWalletViewModel.class);
        if (TextUtils.isEmpty(mMnemonic) && !TextUtils.isEmpty(mWalletKey)) {
            mViewModel.findObserve().observe(this, this::findWalterSuccess);
            mViewModel.findPhraseByKey(mWalletKey);
        } else {
            mViewModel.createdWallet().observe(this, v -> goMain());
            mViewModel.error().observe(this, v -> onError());
            mViewModel.progress().observe(this, this::showProgress);
            mViewModel.mnemonicObserve().observe(this, this::onChangeMnemonic);
            findWalterSuccess(mMnemonic);
            mENMnemonic = mMnemonic;
        }
    }

    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void goNext() {
        Intent intent = new Intent(this, BackupPhraseInputActivity.class);
        intent.putExtra(Constant.WALLET_KEY, mWalletKey);

        intent.putExtra(Constant.KEY_MNEMONIC, mMnemonic);
        intent.putExtra(Constant.KEY_PASSWORD, mPassword);
        intent.putExtra(Constant.KEY_PASSWORD_HINT, mPasswordHint);

        intent.putExtra(Constant.IS_EXPORT_PHRASE, mIsExport);
        intent.putExtra(Constant.IS_RESULT_BACKUP_PHRASE, mIsResultBackup);

        intent.putExtra(Constant.KEY_COLD_MODE, mIsColdMode);

        startActivityForResult(intent, Constant.RESULT_CODE_BACKUP_PHRASE);
    }

    private void findWalterSuccess(String phase) {
        mMnemonic = phase;
        if (!TextUtils.isEmpty(phase)) {
            phase = phase.replaceAll(" ", "  ");
        }
        mPhraseTextView.setText(phase);
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

    private void chooseLanguage() {
        QuarkChooseLanDialog dialog = new QuarkChooseLanDialog(this);
        dialog.setChooseIndex(mLanguageIndex);
        dialog.setOnChooseListener(index -> {
            if (index == mLanguageIndex) {
                return;
            }

            mLanguageIndex = index;
            changeLanguage();
        });
        dialog.show();
    }

    private void changeLanguage() {
        switch (mLanguageIndex) {
            case 0:
                mLanguageView.setText(R.string.phrase_language_en);
                findWalterSuccess(mENMnemonic);
                break;
            case 1:
                mLanguageView.setText(R.string.phrase_language_cn);
                if (!TextUtils.isEmpty(mCNMnemonic)) {
                    findWalterSuccess(mCNMnemonic);
                } else {
                    mViewModel.changeMnemonic(mENMnemonic, Locale.CHINESE);
                }
                break;
            case 2:
                mLanguageView.setText(R.string.phrase_language_tw);
                if (!TextUtils.isEmpty(mTWMnemonic)) {
                    findWalterSuccess(mTWMnemonic);
                } else {
                    mViewModel.changeMnemonic(mENMnemonic, Locale.TAIWAN);
                }
                break;
        }
    }

    private void onChangeMnemonic(String mnemonic) {
        if (mLanguageIndex == 1) {
            mCNMnemonic = mnemonic;
            findWalterSuccess(mCNMnemonic);
        } else if (mLanguageIndex == 2) {
            mTWMnemonic = mnemonic;
            findWalterSuccess(mTWMnemonic);
        }
    }
}
