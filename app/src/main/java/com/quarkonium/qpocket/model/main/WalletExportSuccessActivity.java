package com.quarkonium.qpocket.model.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.view.MyToast;

public class WalletExportSuccessActivity extends BaseActivity {

    public static final String KEY_PASSWORD = "key_password";
    public static final String KEY_TYPE = "key_type";
    public static final String KEY_IS_HD = "key_is_hd";

    public static final int KEY_TYPE_KEYSTORE = 101;
    public static final int KEY_TYPE_PRIVATE_KEY = 102;

    private String mPassword;
    private int mType;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_export_wallet_success;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_edit_export_pk;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mPassword = getIntent().getStringExtra(KEY_PASSWORD);
        mType = getIntent().getIntExtra(KEY_TYPE, KEY_TYPE_KEYSTORE);

        TextView password = findViewById(R.id.export_password_text);
        password.setText(mPassword);
        password.setMovementMethod(ScrollingMovementMethod.getInstance());

        findViewById(R.id.copy_action).setOnClickListener((v) -> onCopy());

        mTopBarView.setTitle(R.string.wallet_edit_export_pk);
        if (mType == KEY_TYPE_KEYSTORE) {
            TextView title = mTopBarView.getTitleView();
            title.setText(R.string.wallet_edit_export_ks);
            setTitleString(getString(R.string.wallet_edit_export_ks));

            TextView tip = findViewById(R.id.phrase_title);
            tip.setText(R.string.wallet_export_keystore);

            TextView phraseTip = findViewById(R.id.phrase_tip);
            phraseTip.setText(R.string.wallet_export_success_phrase_ky_tip);

            TextView offlineTip = findViewById(R.id.offline_tip);
            offlineTip.setText(R.string.wallet_export_success_offline_ky_tip);
        }

        boolean isHD = getIntent().getBooleanExtra(KEY_IS_HD, false);
        if (isHD) {
            View hdView = findViewById(R.id.export_hd_pk_hint);
            hdView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void onCopy() {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            String label = mType == KEY_TYPE_KEYSTORE ? getString(R.string.wallet_export_keystore) : getString(R.string.wallet_export_private_key);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText(label, mPassword);
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            MyToast.showSingleToastShort(this, R.string.copy_success);
        }
    }
}
