package com.quarkonium.qpocket.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.util.PasswordManager;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.view.listener.ActionModeCallbackInterceptor;
import com.quarkonium.qpocket.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class QuarkPasswordDialog extends Dialog {
    private Activity mActivity;
    private EditText mPassword;
    private SystemUtils.OnCheckPasswordListener mPasswordListener;
    private ImageView mShowPdView;
    private boolean mIsShow;
    private QWWallet mWallet;

    private Disposable mPasswordDisposable;

    private String mHint;

    public QuarkPasswordDialog(Activity context) {
        super(context, R.style.CompositeSDKFullScreenResizeDialog);
        mActivity = context;
    }

    public void setPasswordListener(SystemUtils.OnCheckPasswordListener listener) {
        mPasswordListener = listener;
    }

    public void setWallet(QWWallet wallet) {
        mWallet = wallet;
    }

    @Override
    public void show() {
        if (!PasswordManager.shouldRequestPasswordBySavingStateAndTipIfWrong(getContext(), mWallet)) {
            if (mPasswordListener != null) {
                mPasswordListener.onCancel();
            }
            dismiss();
            return;
        }
        super.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_password_layout);
        setCanceledOnTouchOutside(false);

        mShowPdView = findViewById(R.id.show_pd);
        mShowPdView.setOnClickListener(v -> onShowPD());

        View ok = findViewById(R.id.positive_btn);
        ok.setOnClickListener((v) -> onConfirmPassword());

        View cancel = findViewById(R.id.negative_btn);
        cancel.setOnClickListener((v) -> {
            if (mPasswordListener != null) {
                mPasswordListener.onCancel();
            }
            dismiss();
        });

        mPassword = findViewById(R.id.personal_nick_name);
        mPassword.requestFocus();
        if (!TextUtils.isEmpty(mHint)) {
            mPassword.setHint(mHint);
        }
        enablePaste(mPassword);
    }

    private void onConfirmPassword() {
        if (ToolUtils.isFastDoubleClick(800)) {
            return;
        }
        String password = getPassword();
        if (TextUtils.isEmpty(password)) {
            onConfirmFail();
            return;
        }
        if (mPasswordDisposable != null) {
            mPasswordDisposable.dispose();
        }

        mPasswordDisposable = PasswordManager.checkPassword(getContext(), mWallet, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> {
                    if (TextUtils.equals(v, password)) {
                        onConfirmSuccess();
                    } else {
                        onConfirmFail();
                    }
                }, v -> onConfirmFail());
    }

    private void onConfirmSuccess() {
        mPasswordDisposable = null;
        Constant.sPasswordHintMap.put(mWallet.getKey(), "");

        SharedPreferences sharedPreferences = getContext().getApplicationContext().getSharedPreferences("QuarkWallet_PasswordWrongState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("wrongCount" + mWallet.getId());
        editor.remove("wrongTime" + mWallet.getId());
        editor.apply();
        if (mPasswordListener != null) {
            mPasswordListener.onPasswordSuccess(getPassword());
        }
        dismiss();
    }

    private void onConfirmFail() {
        //更新错误次数
        mPasswordDisposable = null;
        Constant.sPasswordHintMap.put(mWallet.getKey(), mWallet.getHint());

        SharedPreferences sharedPreferences = getContext().getApplicationContext().getSharedPreferences("QuarkWallet_PasswordWrongState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("wrongCount" + mWallet.getId(), sharedPreferences.getInt("wrongCount" + mWallet.getId(), 0) + 1);
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timeStamp);
        String timeStampString = dateFormatter.format(date);
        editor.putString("wrongTime" + mWallet.getId(), timeStampString);
        editor.apply();

        if (!PasswordManager.shouldRequestPasswordBySavingStateAndTipIfWrong(getContext().getApplicationContext(), mWallet)) {
            //如果超過次數限制，則退出
            dismiss();
        } else {
            MyToast.showSingleToastShort(getContext(), R.string.password_error);
        }
    }


    //显示隐藏密码
    private void onShowPD() {
        if (mIsShow) {
            mShowPdView.setImageResource(R.drawable.hide_password);
            mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            mPassword.setSelection(TextUtils.isEmpty(mPassword.getText()) ? 0 : mPassword.getText().length());
        } else {
            mShowPdView.setImageResource(R.drawable.show_password);
            mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mPassword.setSelection(TextUtils.isEmpty(mPassword.getText()) ? 0 : mPassword.getText().length());
        }

        mIsShow = !mIsShow;
    }

    private void hideSoftwareKeyboard(EditText input) {
        try {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dismiss() {
        if (mPasswordDisposable != null) {
            mPasswordDisposable.dispose();
        }

        hideSoftwareKeyboard(mPassword);
        super.dismiss();
    }

    public void setHint(String hint) {
        mHint = hint;
    }

    public String getPassword() {
        return mPassword.getText().toString().trim();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
                if (mPasswordListener != null) {
                    mPasswordListener.onCancel();
                }
                dismiss();
            }
        }
        return super.onTouchEvent(event);
    }

    //禁止粘贴
    private void enablePaste(EditText text) {
        if (text == null) {
            return;
        }
        text.setLongClickable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // call that method
            text.setCustomInsertionActionModeCallback(new ActionModeCallbackInterceptor());
        }
    }
}
