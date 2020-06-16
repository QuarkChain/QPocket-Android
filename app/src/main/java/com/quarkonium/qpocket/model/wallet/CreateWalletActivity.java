package com.quarkonium.qpocket.model.wallet;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.model.wallet.viewmodel.CreateWalletViewModel;
import com.quarkonium.qpocket.model.wallet.viewmodel.CreateWalletViewModelFactory;
import com.quarkonium.qpocket.util.PasswordUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.PasswordLevelView;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.view.listener.ActionModeCallbackInterceptor;
import com.quarkonium.qpocket.R;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class CreateWalletActivity extends BaseActivity {

    public class PasswordTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(s)) {
                mPassWordLevelView.showLevel(null);
            } else {
                mPassWordLevelView.showLevel(PasswordUtils.getPasswordLevel(s.toString().trim()));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    @Inject
    CreateWalletViewModelFactory mWalletViewModelFactory;
    CreateWalletViewModel mViewModel;

    private EditText mPasswordEdit;
    private EditText mPasswordConfirmEdit;
    private EditText mPasswordHintEdit;

    private PasswordLevelView mPassWordLevelView;
    private ImageView mShowPdView;
    private View mProgressLayout;

    private TextView mErrorView;

    private boolean mIsShow;

    private String mPassword, mPasswordHint;

    private boolean mIsColdMode;//冷钱包模式

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_home_create_wallte;
    }

    @Override
    public int getActivityTitle() {
        return R.string.create_wallet_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mIsColdMode = getIntent().getBooleanExtra(Constant.KEY_COLD_MODE, false);

        mPasswordEdit = findViewById(R.id.id_password_edittext);
        mPasswordConfirmEdit = findViewById(R.id.confirm_password_edittext);
        mPasswordHintEdit = findViewById(R.id.hint_password_edittext);
        enablePaste(mPasswordEdit);
        enablePaste(mPasswordConfirmEdit);

        mPassWordLevelView = findViewById(R.id.show_pd_strong);
        mPassWordLevelView.setOnClickListener(v -> onCheckPassword());

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));

        View createWalletBtn = findViewById(R.id.new_account_action);
        createWalletBtn.setOnClickListener((view) -> createWallet());

        mTopBarView.setTitle(R.string.create_wallet_title);

        mShowPdView = findViewById(R.id.show_pd);
        mShowPdView.setOnClickListener((v) -> onShowPD());

        mErrorView = findViewById(R.id.error_layout);
        mPasswordEdit.addTextChangedListener(new PasswordTextWatcher());
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this, mWalletViewModelFactory)
                .get(CreateWalletViewModel.class);
        mViewModel.error().observe(this, v -> onError());
        mViewModel.progress().observe(this, this::showProgress);
        mViewModel.mnemonicObserve().observe(this, this::createdWalletSuccess);
    }

    //显示隐藏密码
    private void onShowPD() {
        if (mIsShow) {
            mShowPdView.setImageResource(R.drawable.hide_password);
            mPasswordEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
            mPasswordConfirmEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            mShowPdView.setImageResource(R.drawable.show_password);
            mPasswordEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mPasswordConfirmEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }

        if (!TextUtils.isEmpty(mPasswordEdit.getText())) {
            mPasswordEdit.setSelection(mPasswordEdit.getText().length());
        }
        if (!TextUtils.isEmpty(mPasswordConfirmEdit.getText())) {
            mPasswordConfirmEdit.setSelection(mPasswordConfirmEdit.getText().length());
        }

        mIsShow = !mIsShow;
    }

    private void onCheckPassword() {
        PasswordLevelView.Level level = mPassWordLevelView.getLevel();
        if (level == PasswordLevelView.Level.DANGER) {
            QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setTitle(R.string.password_low_title);
            dialog.setMessage(R.string.password_easy_message);
            dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
            dialog.show();
        } else if (level == PasswordLevelView.Level.LOW) {
            QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setTitle(R.string.password_medium_title);
            dialog.setMessage(R.string.password_easy_message);
            dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
            dialog.show();
        } else if (level == PasswordLevelView.Level.MID) {
            MyToast.showSingleToastShort(this, R.string.password_high_title);
        } else if (level == PasswordLevelView.Level.STRONG) {
            MyToast.showSingleToastShort(this, R.string.password_high_good_title);
        }
    }

    //************创建钱包*************
    //创建钱包密码校验
    private void createWallet() {
        PasswordLevelView.Level level = mPassWordLevelView.getLevel();
        if (level == null || level == PasswordLevelView.Level.DANGER) {
            QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setTitle(R.string.password_low_title);
            dialog.setMessage(R.string.password_easy_message);
            dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
            dialog.show();
            return;
        }

        String password = mPasswordEdit.getText().toString().trim();
        String confirmPassword = mPasswordConfirmEdit.getText().toString().trim();
        if (!password.equals(confirmPassword)) {
            showMessage(getString(R.string.create_password_not_equals));
            return;
        }

        String passwordHint = mPasswordHintEdit.getText().toString().trim();
        create(password, passwordHint);
    }

    //创建钱包
    private void create(String password, String passwordHint) {
        mPassword = password;
        mPasswordHint = passwordHint;
        mViewModel.generateMnemonic();
    }

    //创建钱包成功
    private void createdWalletSuccess(String mnemonic) {
        Intent intent = new Intent(this, BackupPhraseHintActivity.class);
        intent.putExtra(Constant.KEY_MNEMONIC, mnemonic);
        intent.putExtra(Constant.KEY_PASSWORD, mPassword);
        intent.putExtra(Constant.KEY_PASSWORD_HINT, mPasswordHint);
        intent.putExtra(Constant.KEY_COLD_MODE, mIsColdMode);
        startActivity(intent);
    }

    private void onError() {
        showProgress(false);
        showMessage(getString(R.string.create_password_fail));
    }
    //************创建钱包*************

    private void showProgress(boolean isShow) {
        if (isShow) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }

    private void showMessage(String message) {
        mErrorView.setVisibility(View.VISIBLE);
        mErrorView.setText(message);
    }

    //禁止粘贴
    private void enablePaste(EditText text) {
        text.setLongClickable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // call that method
            text.setCustomInsertionActionModeCallback(new ActionModeCallbackInterceptor());
        }
    }
}
