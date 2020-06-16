package com.quarkonium.qpocket.model.wallet;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.entity.ErrorEnvelope;
import com.quarkonium.qpocket.base.BaseFragment;
import com.quarkonium.qpocket.crypto.KeystoreTypeException;
import com.quarkonium.qpocket.crypto.MnemonicUtils;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.model.main.CaptureActivity;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.permission.PermissionHelper;
import com.quarkonium.qpocket.model.wallet.viewmodel.ImportWalletViewModel;
import com.quarkonium.qpocket.model.wallet.viewmodel.ImportWalletViewModelFactory;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.PasswordUtils;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.PasswordLevelView;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.view.listener.ActionModeCallbackInterceptor;
import com.quarkonium.qpocket.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.reactivex.disposables.Disposable;


public class ImportWalletFragment extends BaseFragment {

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

    private final static String IMPORT_WALLET_TYPE = "import_wallet_type";
    private final static String IMPORT_WALLET_CURRENT_TYPE = "import_wallet_current_type";
    private final static String IMPORT_WALLET_COLD_MODE = "import_cold_mode";

    @Inject
    ImportWalletViewModelFactory mImportWalletViewModelFactory;
    private ImportWalletViewModel mImportWalletViewModel;

    private EditText mPrivateKeyEdit;//内容输入框

    private EditText mPasswordEdit;//密码框
    private EditText mConfirmEdit;//确认密码框
    private EditText mHintEdit;//hint密码提示框

    private PasswordLevelView mPassWordLevelView;

    private ImageView mShowPdView;
    private TextView mErrorView;

    private NestedScrollView mNestedScrollView;

    private View mSymbolLayout;
    private TextView mSymbolTextView;
    private PopupWindow mMenuPopWindow;
    private int mSymbolType = -1;

    private int mType;
    private boolean mIsShow;

    private View mBtcAccountTabLayout;
    private TextView mBtcAccountTabSegWit;
    private TextView mBtcAccountTabNormal;

    private TextView mPrivateTipView;

    public static ImportWalletFragment getInstance(int type, boolean coldMode) {
        ImportWalletFragment fragment = new ImportWalletFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(IMPORT_WALLET_TYPE, type);
        bundle.putBoolean(IMPORT_WALLET_COLD_MODE, coldMode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected int getLayoutResource() {
        if (mType == ImportWalletActivity.IMPORT_WALLET_TAG_KEYSTORE) {
            return R.layout.fragment_import_keystore_wallet;
        }
        if (mType == ImportWalletActivity.IMPORT_WALLET_TAG_WATCH) {
            return R.layout.fragment_import_watch;
        }
        return R.layout.fragment_import_wallet;
    }

    @Override
    public int getFragmentTitle() {
        return 0;
    }

    @Override
    protected void onInitView(Bundle savedInstanceState, View rootView) {
        if (savedInstanceState != null) {
            mSymbolType = savedInstanceState.getInt(IMPORT_WALLET_CURRENT_TYPE, -1);
        }
        if (mType == ImportWalletActivity.IMPORT_WALLET_TAG_WATCH) {
            initWatchView(rootView);
            return;
        }
        initView(rootView);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mType = bundle.getInt(IMPORT_WALLET_TYPE);
            String[] titles = getResources().getStringArray(R.array.import_wallet_tag);
            if (mType >= 0 && mType < titles.length) {
                setFragmentTitle(titles[mType]);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImportWalletViewModel = new ViewModelProvider(this, mImportWalletViewModelFactory)
                .get(ImportWalletViewModel.class);
        mImportWalletViewModel.importWallet().observe(this, v -> onImportSuccess());
        mImportWalletViewModel.error().observe(this, this::onError);
        mImportWalletViewModel.progress().observe(this, this::onShowProgress);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(IMPORT_WALLET_CURRENT_TYPE, mSymbolType);
    }

    private void initView(View view) {
        mSymbolLayout = view.findViewById(R.id.import_symbol_layout);
        mSymbolTextView = view.findViewById(R.id.import_token_symbol);
        mSymbolTextView.setOnClickListener(this::showSymbolWindow);

        mPrivateKeyEdit = view.findViewById(R.id.phrase_edit_text);
        //改变默认的单行模式
        mPrivateKeyEdit.setSingleLine(false);
        //水平滚动设置为False
        mPrivateKeyEdit.setHorizontallyScrolling(false);
        mPrivateKeyEdit.setOnTouchListener((View v, MotionEvent event) -> {
            // 解决scrollView中嵌套EditText导致不能上下滑动的问题
            v.getParent().requestDisallowInterceptTouchEvent(true);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

        mPrivateTipView = view.findViewById(R.id.import_private_tip);

        mPasswordEdit = view.findViewById(R.id.pd_edit_text);
        mConfirmEdit = view.findViewById(R.id.pd_confirm_edit_text);
        mHintEdit = view.findViewById(R.id.pd_hint_edit_text);
        enablePaste(mPasswordEdit);
        enablePaste(mConfirmEdit);

        View mImportBtn = view.findViewById(R.id.new_account_action);
        mImportBtn.setOnClickListener((v) -> importWallet());

        mErrorView = view.findViewById(R.id.error_layout);

        mNestedScrollView = view.findViewById(R.id.import_nested_scroll_view);

        mShowPdView = view.findViewById(R.id.show_pd);
        mShowPdView.setOnClickListener((v) -> onShowPD());

        mPassWordLevelView = rootView.findViewById(R.id.show_pd_strong);
        if (mPassWordLevelView != null) {
            mPassWordLevelView.setOnClickListener(v -> onCheckPassword());
        }
        if (mPasswordEdit != null) {
            mPasswordEdit.addTextChangedListener(new PasswordTextWatcher());
        }

        mBtcAccountTabLayout = rootView.findViewById(R.id.btc_type_layout);
        if (mBtcAccountTabLayout != null) {
            mBtcAccountTabSegWit = rootView.findViewById(R.id.btc_type_segwit);
            mBtcAccountTabSegWit.setOnClickListener(v -> {
                mBtcAccountTabSegWit.setSelected(true);
                mBtcAccountTabNormal.setSelected(false);
            });

            mBtcAccountTabNormal = rootView.findViewById(R.id.btc_type_normal);
            mBtcAccountTabNormal.setOnClickListener(v -> {
                mBtcAccountTabSegWit.setSelected(false);
                mBtcAccountTabNormal.setSelected(true);
            });
            mBtcAccountTabSegWit.setSelected(true);
        }

        switch (mType) {
            case ImportWalletActivity.IMPORT_WALLET_TAG_PHRASE:
                mPrivateKeyEdit.setHint(R.string.import_wallet_input_phrase_hint);
                mPrivateTipView.setText(R.string.import_phrase_tip);
                mSymbolLayout.setVisibility(View.GONE);
                break;
            case ImportWalletActivity.IMPORT_WALLET_TAG_PRIVATE_KEY:
                mPrivateKeyEdit.setHint(R.string.import_wallet_input_private_hint);
                mPrivateTipView.setText(R.string.import_private_tip);
                break;
        }

        switch (mSymbolType) {
            case Constant.WALLET_TYPE_QKC:
                updateType();
                mSymbolTextView.setText(R.string.qkc);
                break;
            case Constant.WALLET_TYPE_ETH:
                updateType();
                mSymbolTextView.setText(R.string.eth);
                break;
            case Constant.WALLET_TYPE_TRX:
                updateType();
                mSymbolTextView.setText(R.string.trx);
                break;
        }
    }

    private void initWatchView(View view) {
        mSymbolLayout = view.findViewById(R.id.import_symbol_layout);
        mSymbolTextView = view.findViewById(R.id.import_token_symbol);
        mSymbolTextView.setOnClickListener(this::showSymbolWindow);
        mConfirmEdit = view.findViewById(R.id.pd_confirm_edit_text);

        View mImportBtn = view.findViewById(R.id.new_account_action);
        mImportBtn.setOnClickListener((v) -> importWallet());

        mErrorView = view.findViewById(R.id.error_layout);
        mNestedScrollView = view.findViewById(R.id.import_nested_scroll_view);

        mConfirmEdit.setHint(getWatchText(R.string.import_symbol_watch_address));

        switch (mSymbolType) {
            case Constant.WALLET_TYPE_QKC:
                updateType();
                mSymbolTextView.setText(R.string.qkc);
                break;
            case Constant.WALLET_TYPE_ETH:
                updateType();
                mSymbolTextView.setText(R.string.eth);
                break;
            case Constant.WALLET_TYPE_TRX:
                updateType();
                mSymbolTextView.setText(R.string.trx);
                break;
        }

        view.findViewById(R.id.address_scan).setOnClickListener(v -> onScan());
    }

    private void onCheckPassword() {
        PasswordLevelView.Level level = mPassWordLevelView.getLevel();
        if (level == PasswordLevelView.Level.DANGER) {
            QuarkSDKDialog dialog = new QuarkSDKDialog(requireActivity());
            dialog.setTitle(R.string.password_low_title);
            dialog.setMessage(R.string.password_easy_message);
            dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
            dialog.show();
        } else if (level == PasswordLevelView.Level.LOW) {
            QuarkSDKDialog dialog = new QuarkSDKDialog(requireActivity());
            dialog.setTitle(R.string.password_medium_title);
            dialog.setMessage(R.string.password_easy_message);
            dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
            dialog.show();
        } else if (level == PasswordLevelView.Level.MID) {
            MyToast.showSingleToastShort(requireActivity(), R.string.password_high_title);
        } else if (level == PasswordLevelView.Level.STRONG) {
            MyToast.showSingleToastShort(requireActivity(), R.string.password_high_good_title);
        }
    }

    private void onShowPD() {
        if (mIsShow) {
            mShowPdView.setImageResource(R.drawable.hide_password);
            if (mPasswordEdit != null) {
                mPasswordEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            mConfirmEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            mShowPdView.setImageResource(R.drawable.show_password);
            if (mPasswordEdit != null) {
                mPasswordEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            mConfirmEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }

        if (mPasswordEdit != null && !TextUtils.isEmpty(mPasswordEdit.getText())) {
            mPasswordEdit.setSelection(mPasswordEdit.getText().length());
        }
        if (!TextUtils.isEmpty(mConfirmEdit.getText())) {
            mConfirmEdit.setSelection(mConfirmEdit.getText().length());
        }

        mIsShow = !mIsShow;
    }

    private void importWallet() {
        int symbolType = mSymbolType;
        String key = mType == ImportWalletActivity.IMPORT_WALLET_TAG_WATCH ? mConfirmEdit.getText().toString().trim() : mPrivateKeyEdit.getText().toString().trim();
        switch (mType) {
            case ImportWalletActivity.IMPORT_WALLET_TAG_PHRASE:
                if (!MnemonicUtils.validateMnemonic(requireContext(), key)) {
                    showError(getString(R.string.import_wallet_fail_phrase));
                    return;
                }

                String[] words = key.split(" ");
                if (words.length < 12 || words.length > 24) {
                    showError(getString(R.string.import_wallet_fail_phrase));
                    return;
                }

                String passWord = mPasswordEdit.getText().toString().trim();
                String confirmPassword = mConfirmEdit.getText().toString().trim();
                PasswordLevelView.Level level = mPassWordLevelView.getLevel();
                if (level == null || level == PasswordLevelView.Level.DANGER) {
                    QuarkSDKDialog dialog = new QuarkSDKDialog(requireActivity());
                    dialog.setTitle(R.string.password_low_title);
                    dialog.setMessage(R.string.password_easy_message);
                    dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
                    dialog.show();
                    return;
                }

                //确认密码
                if (!passWord.equals(confirmPassword)) {
                    showError(getString(R.string.create_password_not_equals));
                    return;
                }

                String phrasePdHint = mHintEdit.getText().toString().trim();
                mImportWalletViewModel.onPhrase(key, passWord, phrasePdHint, Constant.WALLET_TYPE_ETH);
                break;
            case ImportWalletActivity.IMPORT_WALLET_TAG_PRIVATE_KEY:
                if (symbolType == -1) {
                    showError(getString(R.string.import_symbol_type_null));
                    return;
                }

                if (!WalletUtils.isValidPrivateKey(key)) {
                    showError(getString(R.string.import_wallet_fail_private_key));
                    return;
                }

                PasswordLevelView.Level privateKeyLevel = mPassWordLevelView.getLevel();
                if (privateKeyLevel == null || privateKeyLevel == PasswordLevelView.Level.DANGER) {
                    QuarkSDKDialog dialog = new QuarkSDKDialog(requireActivity());
                    dialog.setTitle(R.string.password_low_title);
                    dialog.setMessage(R.string.password_easy_message);
                    dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
                    dialog.show();
                    return;
                }

                //确认密码
                String privateKeyWord = mPasswordEdit.getText().toString().trim();
                String privateConfirmPassword = mConfirmEdit.getText().toString().trim();
                if (!privateKeyWord.equals(privateConfirmPassword)) {
                    showError(getString(R.string.create_password_not_equals));
                    return;
                }

                String privatePdHint = mHintEdit.getText().toString().trim();
                boolean isSegWit = false;
                if (mBtcAccountTabSegWit != null) {
                    isSegWit = mBtcAccountTabSegWit.isSelected();
                }
                mImportWalletViewModel.onPrivateKey(isSegWit, key, privateKeyWord, privatePdHint, symbolType);
                break;
            case ImportWalletActivity.IMPORT_WALLET_TAG_KEYSTORE:
                if (symbolType == -1) {
                    showError(getString(R.string.import_symbol_type_null));
                    return;
                }
                if (!key.startsWith("{") || !key.endsWith("}") || !key.contains("address") || !key.contains("crypto")) {
                    showError(getString(R.string.import_wallet_fail_keystore));
                    return;
                }

                String keystorePassword = mConfirmEdit.getText().toString().trim();

                boolean isSegWitK = false;
                if (mBtcAccountTabSegWit != null) {
                    isSegWitK = mBtcAccountTabSegWit.isSelected();
                }
                mImportWalletViewModel.onKeystore(isSegWitK, key, keystorePassword, symbolType);
                break;
            case ImportWalletActivity.IMPORT_WALLET_TAG_WATCH:
                if (symbolType == -1) {
                    showError(getString(R.string.import_symbol_type_null));
                    return;
                }
                if (symbolType == Constant.WALLET_TYPE_ETH) {
                    if (!WalletUtils.isValidAddress(key)) {
                        showError(getWatchText(R.string.import_wallet_fail_watch));
                        return;
                    }
                } else if (symbolType == Constant.ACCOUNT_TYPE_TRX) {
                    if (!TronWalletClient.isTronAddressValid(key)) {
                        showError(getWatchText(R.string.import_wallet_fail_watch));
                        return;
                    }
                } else if (symbolType == Constant.ACCOUNT_TYPE_QKC) {
                    if (!WalletUtils.isQKCValidAddress(key)) {
                        showError(getWatchText(R.string.import_wallet_fail_watch));
                        return;
                    }
                }
                mImportWalletViewModel.onWatch(key, symbolType);
                break;
        }
    }


    private void updateType() {
        if (getActivity() != null && mType == ImportWalletActivity.IMPORT_WALLET_TAG_WATCH && mConfirmEdit != null) {
            mConfirmEdit.setHint(getWatchText(R.string.import_wallet_input_watch_hint));
        }

        if (mType == ImportWalletActivity.IMPORT_WALLET_TAG_PRIVATE_KEY) {
            mPrivateTipView.setText(R.string.import_private_tip);
        }
    }

    private void onImportSuccess() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        onShowProgress(false);

        if (errorEnvelope.code == Constant.ErrorCode.WALLET_EXIT) {
            showError(getString(R.string.import_wallet_fail_exit));
            return;
        }
        if (errorEnvelope.code == Constant.ErrorCode.PRIVATE_NOT_WIF_COM) {
            showError(getString(R.string.import_wallet_btc_wif_not_com));
            return;
        }
        if (errorEnvelope.code == Constant.ErrorCode.KEYSTORE_ERROR) {
            KeystoreTypeException exception = (KeystoreTypeException) errorEnvelope.throwable;
            switch (exception.getErrorSrc()) {
                case KeystoreTypeException.KEYSTORE_ERROR_BTC_NORMAL:
                    //keystore类型错误
                    showError(String.format(getString(R.string.keystore_type_error), getString(R.string.btc_change_normal)));
                    break;
                case KeystoreTypeException.KEYSTORE_ERROR_BTC_SEGWIT:
                    //keystore类型错误
                    showError(String.format(getString(R.string.keystore_type_error), getString(R.string.btc_change_segwit)));
                    break;
            }
            return;
        }
        switch (mType) {
            case ImportWalletActivity.IMPORT_WALLET_TAG_PHRASE:
                showError(getString(R.string.import_wallet_fail_phrase));
                break;
            case ImportWalletActivity.IMPORT_WALLET_TAG_PRIVATE_KEY:
                showError(getString(R.string.import_wallet_fail_private_key));
                break;
            case ImportWalletActivity.IMPORT_WALLET_TAG_KEYSTORE:
                showError(getString(R.string.import_wallet_fail_keystore));
                break;
            case ImportWalletActivity.IMPORT_WALLET_TAG_WATCH:
                showError(getWatchText(R.string.import_wallet_fail_watch));
                break;
        }
    }

    private void showError(String text) {
        mErrorView.setVisibility(View.VISIBLE);
        mErrorView.setText(text);

        mNestedScrollView.post(() -> {
            mNestedScrollView.fullScroll(ScrollView.FOCUS_DOWN);

            mErrorView.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(100)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mErrorView.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(100)
                                    .setListener(null);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        });
    }

    private void onShowProgress(boolean show) {
        ImportWalletActivity activity = (ImportWalletActivity) getActivity();
        if (activity != null) {
            activity.showProgress(show);
        }
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

    private String getWatchText(int id) {
        if (getActivity() == null) {
            return "";
        }
        int stringId = R.string.import_wallet_qkc;
        switch (mSymbolType) {
            case Constant.WALLET_TYPE_ETH:
                stringId = R.string.import_wallet_eth;
                break;
            case Constant.ACCOUNT_TYPE_TRX:
                stringId = R.string.import_wallet_trx;
                break;
        }
        return String.format(getString(id), getString(stringId));
    }

    private void showSymbolWindow(View view) {
        if (mMenuPopWindow != null) {
            if (mMenuPopWindow.isShowing()) {
                mMenuPopWindow.dismiss();
            }
            mMenuPopWindow = null;
        }

        View contentView = View.inflate(requireContext().getApplicationContext(), R.layout.import_token_menu_layout, null);
        mMenuPopWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        //********QKC********
        ViewGroup qkc = contentView.findViewById(R.id.symbol_qkc);
        qkc.setOnClickListener((View v) -> {
            mMenuPopWindow.dismiss();

            mSymbolType = Constant.WALLET_TYPE_QKC;
            updateType();
            mSymbolTextView.setText(R.string.qkc);

            if (mBtcAccountTabLayout != null) {
                mBtcAccountTabLayout.setVisibility(View.GONE);
            }
        });

        //***********ETH***********
        ViewGroup eth = contentView.findViewById(R.id.symbol_eth);
        eth.setOnClickListener((View v) -> {
            mMenuPopWindow.dismiss();

            mSymbolType = Constant.WALLET_TYPE_ETH;
            updateType();
            mSymbolTextView.setText(R.string.eth);

            if (mBtcAccountTabLayout != null) {
                mBtcAccountTabLayout.setVisibility(View.GONE);
            }
        });

        //***********TRX***********
        ViewGroup trx = contentView.findViewById(R.id.symbol_trx);
        trx.setOnClickListener((View v) -> {
            mMenuPopWindow.dismiss();

            mSymbolType = Constant.WALLET_TYPE_TRX;
            updateType();
            mSymbolTextView.setText(R.string.trx);

            if (mBtcAccountTabLayout != null) {
                mBtcAccountTabLayout.setVisibility(View.GONE);
            }
        });

        switch (mSymbolType) {
            case Constant.WALLET_TYPE_QKC:
                qkc.getChildAt(1).setVisibility(View.VISIBLE);
                eth.getChildAt(1).setVisibility(View.GONE);
                trx.getChildAt(1).setVisibility(View.GONE);
                break;
            case Constant.WALLET_TYPE_ETH:
                qkc.getChildAt(1).setVisibility(View.GONE);
                eth.getChildAt(1).setVisibility(View.VISIBLE);
                trx.getChildAt(1).setVisibility(View.GONE);
                break;
            case Constant.WALLET_TYPE_TRX:
                qkc.getChildAt(1).setVisibility(View.GONE);
                eth.getChildAt(1).setVisibility(View.GONE);
                trx.getChildAt(1).setVisibility(View.VISIBLE);
                break;
            default:
                qkc.getChildAt(1).setVisibility(View.GONE);
                eth.getChildAt(1).setVisibility(View.GONE);
                trx.getChildAt(1).setVisibility(View.GONE);
                break;
        }

        mMenuPopWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mMenuPopWindow.setTouchable(true);
        mMenuPopWindow.setTouchInterceptor((View v, MotionEvent event) -> false);
        showPopWindowPos(view, contentView);
    }

    /**
     * 计算出来的位置，y方向就在anchorView的上面和下面对齐显示，x方向就是与屏幕右边对齐显示
     * 如果anchorView的位置有变化，就可以适当自己额外加入偏移来修正
     *
     * @param anchorView  呼出window的view
     * @param contentView window的内容布局
     */
    private void showPopWindowPos(final View anchorView, final View contentView) {
        final int windowPos[] = new int[2];
        final int anchorLoc[] = new int[2];
        // 获取锚点View在屏幕上的左上角坐标位置
        anchorView.getLocationOnScreen(anchorLoc);
        final int anchorHeight = anchorView.getHeight();
        // 获取屏幕的高宽
        final int screenHeight = anchorView.getContext().getResources().getDisplayMetrics().heightPixels;
        // 测量contentView
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // 计算contentView的高宽
        final int windowHeight = contentView.getMeasuredHeight();
        // 判断需要向上弹出还是向下弹出显示
        final boolean isNeedShowUp = (screenHeight - anchorLoc[1] - anchorHeight < windowHeight - 20);
        if (isNeedShowUp) {
            windowPos[0] = anchorLoc[0];
            windowPos[1] = anchorLoc[1] - windowHeight - 20;
        } else {
            windowPos[0] = anchorLoc[0];
            windowPos[1] = anchorLoc[1] + anchorHeight + 20;
        }
        mMenuPopWindow.showAtLocation(anchorView, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);
    }

    private void onScan() {
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
                            String[] name = {
                                    Manifest.permission.CAMERA
                            };
                            MyToast.showSingleToastLong(getActivity(), PermissionHelper.getPermissionToast(requireContext().getApplicationContext(), name));
                        }
                    });
            return;
        }
        startCamera();
    }

    private void startCamera() {
        int accountType = getAccountType();
        CaptureActivity.startForResultActivity(this, requireActivity(), accountType);
    }

    private int getAccountType() {
        switch (mSymbolType) {
            case Constant.WALLET_TYPE_TRX:
                return Constant.ACCOUNT_TYPE_TRX;
            case Constant.WALLET_TYPE_QKC:
                return Constant.ACCOUNT_TYPE_QKC;
            case Constant.WALLET_TYPE_ETH:
                return Constant.ACCOUNT_TYPE_ETH;
        }
        return -1;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Constant.REQUEST_CODE_CAPTURE == requestCode && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String address = data.getStringExtra(Constant.WALLET_ADDRESS);
                mConfirmEdit.setText(address);
                if (mSymbolType == -1) {
                    mConfirmEdit.setHint(getWatchText(R.string.import_wallet_input_watch_hint));
                    if (QWWalletUtils.isQKCValidAddress(address)) {
                        mSymbolType = Constant.WALLET_TYPE_QKC;
                        mSymbolTextView.setText(R.string.qkc);
                    } else if (TronWalletClient.isTronAddressValid(address)) {
                        mSymbolType = Constant.WALLET_TYPE_TRX;
                        mSymbolTextView.setText(R.string.trx);
                    } else if (WalletUtils.isValidAddress(address)) {
                        mSymbolType = Constant.WALLET_TYPE_ETH;
                        mSymbolTextView.setText(R.string.eth);
                    }
                }
            }
        }
    }
}
