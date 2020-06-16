package com.quarkonium.qpocket.finger;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.QuarkPasswordStore;
import com.quarkonium.qpocket.finger.base.BaseFingerprint;
import com.quarkonium.qpocket.util.PasswordManager;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.listener.ActionModeCallbackInterceptor;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.model.unlock.UnlockManagerActivity;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FingerprintDialogFragment extends DialogFragment {

    public interface DismissListener {
        void onDismiss();
    }

    public interface OnLockPasswordListener {
        void onSuccess();
    }

    private static class FingerprintListener implements BaseFingerprint.IdentifyListener {

        private SoftReference<FingerprintDialogFragment> mFragment;

        FingerprintListener(FingerprintDialogFragment fingerprintDialogFragment) {
            mFragment = new SoftReference<>(fingerprintDialogFragment);
        }

        @Override
        public void onSucceed() {
            if (mFragment != null && mFragment.get() != null) {
                FingerprintDialogFragment fingerprintDialogFragment = mFragment.get();
                fingerprintDialogFragment.onSucceed();
            }
        }

        @Override
        public void onNotMatch(int availableTimes) {
            if (mFragment != null && mFragment.get() != null) {
                FingerprintDialogFragment fingerprintDialogFragment = mFragment.get();
                fingerprintDialogFragment.onNotMatch(availableTimes);
            }
        }

        @Override
        public void onFailed(boolean isDeviceLocked) {
            if (mFragment != null && mFragment.get() != null) {
                FingerprintDialogFragment fingerprintDialogFragment = mFragment.get();
                fingerprintDialogFragment.onFailed(isDeviceLocked);
            }
        }

        @Override
        public void onStartFailedByDeviceLocked() {
            if (mFragment != null && mFragment.get() != null) {
                FingerprintDialogFragment fingerprintDialogFragment = mFragment.get();
                fingerprintDialogFragment.onStartFailedByDeviceLocked();
            }
        }
    }

    /**
     * Enumeration to indicate which authentication method the user is trying to authenticate with.
     */
    public enum Stage {
        NEW_FINGERPRINT_ENROLLED,
        FINGERPRINT,
        PASSWORD
    }

    private View mFingerLayout;
    private View mPasswordLayout;

    private TextView mTouchTitleView;
    private TextView mInputPasswordView;

    private EditText mPassword;
    private ImageView mShowPdView;
    private TextView mTitleView;


    private FingerprintIdentify mFingerprintIdentify;

    private DismissListener mDismissListener;
    private SystemUtils.OnCheckPasswordListener mPasswordListener;
    private boolean mPasswordSuccess;
    private Disposable mDisposable;

    private QWWallet mQuarkWallet;
    private boolean mIsShow;

    private String mDialogTitle;

    private Stage mStage = Stage.FINGERPRINT;

    private OnLockPasswordListener mLockListener;

    private Disposable mPasswordDisposable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CompositeSDKFullScreenDialog);

        mFingerprintIdentify = new FingerprintIdentify(requireContext().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.dialog_fingerprint_container, container, false);
        //*****************指纹界面*****************
        View mCancelButton = v.findViewById(R.id.negative_btn);
        mCancelButton.setOnClickListener((view) -> dismiss());

        mInputPasswordView = v.findViewById(R.id.positive_btn);
        mInputPasswordView.setOnClickListener(view -> showPasswordDialog());

        mTouchTitleView = v.findViewById(R.id.fingerprint_title);

        mFingerLayout = v.findViewById(R.id.fingerprint_layout);
        mPasswordLayout = v.findViewById(R.id.password_inner);

        //*****************密码界面*****************
        mShowPdView = v.findViewById(R.id.show_pd);
        mShowPdView.setOnClickListener(view -> onShowPD());
        mPassword = v.findViewById(R.id.personal_nick_name);
        enablePaste(mPassword);

        mTitleView = v.findViewById(R.id.dialog_title);

        View ok = v.findViewById(R.id.password_positive_btn);
        ok.setOnClickListener((view) -> onConfirmPassword());

        View cancel = v.findViewById(R.id.password_negative_btn);
        cancel.setOnClickListener((view) -> dismiss());

        if (!mFingerprintIdentify.isFingerprintEnable()) {
            if (mStage == Stage.FINGERPRINT) {
                showPasswordDialog();
            } else if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
                MyToast.showSingleToastShort(requireActivity(), R.string.finger_open_fail);
                dismiss();
            }
        }
        return v;
    }

    private void onConfirmPassword() {
        if (ToolUtils.isFastDoubleClick(400)) {
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
        mPasswordDisposable = PasswordManager.checkPassword(requireContext(), mQuarkWallet, password)
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
        Constant.sPasswordHintMap.put(mQuarkWallet.getKey(), "");

        SharedPreferences sharedPreferences = requireContext().getApplicationContext().getSharedPreferences("QuarkWallet_PasswordWrongState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("wrongCount" + mQuarkWallet.getId());
        editor.remove("wrongTime" + mQuarkWallet.getId());
        editor.apply();
        if (mPasswordListener != null) {
            mPasswordListener.onPasswordSuccess(getPassword());
            mPasswordSuccess = true;
        }
        dismiss();
    }

    private void onConfirmFail() {
        //更新错误次数
        mPasswordDisposable = null;
        Constant.sPasswordHintMap.put(mQuarkWallet.getKey(), mQuarkWallet.getHint());

        SharedPreferences sharedPreferences = requireContext().getApplicationContext().getSharedPreferences("QuarkWallet_PasswordWrongState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("wrongCount" + mQuarkWallet.getId(), sharedPreferences.getInt("wrongCount" + mQuarkWallet.getId(), 0) + 1);
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timeStamp);
        String timeStampString = dateFormatter.format(date);
        editor.putString("wrongTime" + mQuarkWallet.getId(), timeStampString);
        editor.apply();

        if (!PasswordManager.shouldRequestPasswordBySavingStateAndTipIfWrong(requireContext().getApplicationContext(), mQuarkWallet)) {
            //如果超過次數限制，則退出
            dismiss();
        } else {
            MyToast.showSingleToastShort(getContext(), R.string.password_error);
        }
    }

    @Override
    public void onStart() { //在onStart()中
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.82F);
            window.setAttributes(windowParams);
        }
        getDialog().setCanceledOnTouchOutside(false);
        this.getDialog().setOnKeyListener((DialogInterface arg0, int keyCode, KeyEvent arg2) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismiss();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStage == Stage.NEW_FINGERPRINT_ENROLLED || mStage == Stage.FINGERPRINT) {
            mFingerprintIdentify.startIdentify(Constant.MAX_AVAILABLE_TIMES, new FingerprintListener(this));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mStage == Stage.NEW_FINGERPRINT_ENROLLED || mStage == Stage.FINGERPRINT) {
            mFingerprintIdentify.cancelIdentify();
        }
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }

    public void setQuarkWallet(QWWallet wallet) {
        mQuarkWallet = wallet;
    }

    public void setDialogTitle(String title) {
        mDialogTitle = title;
    }

    public void setPasswordListener(SystemUtils.OnCheckPasswordListener listener) {
        mPasswordListener = listener;
    }

    public void setLockListener(OnLockPasswordListener lockListener) {
        mLockListener = lockListener;
    }

    public void setDismissListener(DismissListener listener) {
        mDismissListener = listener;
    }

    //指纹支付
    public void onSucceed() {
        if (mStage == Stage.FINGERPRINT) {
            PasswordManager.clearPasswordWrongTimeStatesIfExist(requireContext(), mQuarkWallet);
            if (mLockListener != null) {
                dismiss();
                mLockListener.onSuccess();
                return;
            }
            if (mPasswordListener != null) {
                PasswordStore passwordStore = new QuarkPasswordStore(requireContext().getApplicationContext());
                mDisposable = passwordStore.getPassword(mQuarkWallet)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onPasswordSuccess, v -> onPasswordFail());
                return;
            }
        } else if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
            Context context = requireContext().getApplicationContext();
            SharedPreferencesUtils.setSupportFingerprint(context, true);
            MyToast.showSingleToastShort(requireActivity(), R.string.finger_open_success);

            int index = SharedPreferencesUtils.getAppLockState(context);
            if (index == -1) {
                //第一次开启指纹，则锁屏模式为全部
                SharedPreferencesUtils.setAppLockState(context, UnlockManagerActivity.APP_LOCK_STATE_ALL);
            }
            //第一次输入密码时启动指纹，则启动成功后触发密码校验
            if (mPasswordListener != null) {
                PasswordStore passwordStore = new QuarkPasswordStore(context);
                mDisposable = passwordStore.getPassword(mQuarkWallet)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onPasswordSuccess, v -> onPasswordFail());
                return;
            }
        }
        dismiss();
    }

    public void onNotMatch(int availableTimes) {
        mTouchTitleView.setText(R.string.finger_touch_again);
        nope(mTouchTitleView);

        if (mStage == Stage.FINGERPRINT) {
            mInputPasswordView.setVisibility(View.VISIBLE);
        }
    }

    public void onFailed(boolean isDeviceLocked) {
        onStartFailedByDeviceLocked();
    }

    public void onStartFailedByDeviceLocked() {
        if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
            MyToast.showSingleToastShort(requireActivity(), R.string.finger_open_fail);
            if (mPasswordListener != null) {
                showPasswordDialog();
            } else {
                dismiss();
            }
        } else if (mStage == Stage.FINGERPRINT) {
            showPasswordDialog();
        }
    }

    private void onPasswordSuccess(String password) {
        mPasswordListener.onPasswordSuccess(password);
        mPasswordSuccess = true;
        dismiss();
    }

    private void onPasswordFail() {
        showPasswordDialog();
    }

    public static void nope(View view) {
        int delta = view.getResources().getDimensionPixelOffset(R.dimen.dp_5);
        PropertyValuesHolder pvhTranslateX = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                Keyframe.ofFloat(0f, 0),
                Keyframe.ofFloat(.10f, -delta),
                Keyframe.ofFloat(.26f, delta),
                Keyframe.ofFloat(.42f, -delta),
                Keyframe.ofFloat(.58f, delta),
                Keyframe.ofFloat(.74f, -delta),
                Keyframe.ofFloat(.90f, delta),
                Keyframe.ofFloat(1f, 0f));
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, pvhTranslateX).setDuration(500);
        animator.start();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mDismissListener != null) {
            mDismissListener.onDismiss();
        }

        if (mPasswordListener != null && !mPasswordSuccess) {
            mPasswordListener.onCancel();
        }

        if (mDisposable != null) {
            mDisposable.dispose();
        }

        if (mPasswordDisposable != null) {
            mPasswordDisposable.dispose();
        }

        hideSoftwareKeyboard(mPassword);
    }

    private void showPasswordDialog() {
        if (getContext() == null) {
            return;
        }
        if (!PasswordManager.shouldRequestPasswordBySavingStateAndTipIfWrong(getContext(), mQuarkWallet)) {
            dismiss();
            return;
        }

        mFingerprintIdentify.cancelIdentify();

        mFingerLayout.setVisibility(View.GONE);
        mPasswordLayout.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(mDialogTitle)) {
            mTitleView.setText(mDialogTitle);
        }

        mPassword.setHint(Constant.sPasswordHintMap.get(mQuarkWallet.getKey()));
        showInput(mPassword);

        mStage = Stage.PASSWORD;
    }

    public String getPassword() {
        return mPassword.getText().toString().trim();
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

    private void hideSoftwareKeyboard(EditText input) {
        try {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showInput(final EditText et) {
        et.requestFocus();
        try {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
