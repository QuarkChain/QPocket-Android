package com.quarkonium.qpocket.model.unlock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatImageView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.R;

public class UnlockManagerActivity extends BaseActivity {

    public static void startSettingActivity(Activity context) {
        Intent intent = new Intent(context, UnlockManagerActivity.class);
        context.startActivity(intent);
    }

    public static final int APP_LOCK_STATE_ALL = 100;
    public static final int APP_LOCK_STATE_WALLET = 101;
    public static final int APP_LOCK_STATE_NONE = 102;

    private ViewGroup mLockOpenView;
    private ViewGroup mLockWalletView;
    private ViewGroup mLockNoneView;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_settings_unlock;
    }

    @Override
    public int getActivityTitle() {
        return R.string.setting_unlock;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.setting_unlock);

        {
            mLockOpenView = findViewById(R.id.lock_open);
            TextView title = mLockOpenView.findViewById(R.id.name);
            title.setText(R.string.setting_open_app);
            TextView message = mLockOpenView.findViewById(R.id.message);
            message.setText(R.string.setting_open_app_info);
            mLockOpenView.setOnClickListener(v -> changeLockAll());
        }
        {
            mLockWalletView = findViewById(R.id.lock_wallet);
            TextView title = mLockWalletView.findViewById(R.id.name);
            title.setText(R.string.setting_wallet_related);
            TextView message = mLockWalletView.findViewById(R.id.message);
            message.setText(R.string.setting_wallet_related_info);
            mLockWalletView.setOnClickListener(v -> changeLockWallet());
        }
        {
            mLockNoneView = findViewById(R.id.lock_none);
            TextView title = mLockNoneView.findViewById(R.id.name);
            title.setText(R.string.setting_none);
            TextView message = mLockNoneView.findViewById(R.id.message);
            message.setText(R.string.setting_none_info);
            mLockNoneView.setOnClickListener(v -> changeLockNone());
        }

        //指纹开启
        boolean isSupportFingerprint = SharedPreferencesUtils.isSupportFingerprint(getApplicationContext());
        if (!isSupportFingerprint) {
            //未开启指纹，所有按钮处于锁定状态
            mLockOpenView.setEnabled(false);
            mLockWalletView.setEnabled(false);
            mLockNoneView.setEnabled(false);
            //UI锁定
            unselectedEnableView(mLockOpenView);
            unselectedEnableView(mLockWalletView);
            View lockTitle = mLockNoneView.findViewById(R.id.name);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) lockTitle.getLayoutParams();
            lp.topMargin = (int) UiUtils.dpToPixel(15);
            lockTitle.setLayoutParams(lp);
            mLockNoneView.findViewById(R.id.info).setVisibility(View.VISIBLE);
        } else {
            //获取当前锁定模式
            int index = SharedPreferencesUtils.getAppLockState(getApplicationContext());
            if (index == -1) {
                index = APP_LOCK_STATE_ALL;
                SharedPreferencesUtils.setAppLockState(getApplicationContext(), APP_LOCK_STATE_ALL);
            }
            //恢复选中态
            switch (index) {
                case APP_LOCK_STATE_ALL:
                    selectedView(mLockOpenView);
                    unSelectedView(mLockWalletView);
                    unSelectedView(mLockNoneView);
                    break;
                case APP_LOCK_STATE_WALLET:
                    unSelectedView(mLockOpenView);
                    selectedView(mLockWalletView);
                    unSelectedView(mLockNoneView);
                    break;
                case APP_LOCK_STATE_NONE:
                    unSelectedView(mLockOpenView);
                    unSelectedView(mLockWalletView);
                    selectedView(mLockNoneView);
                    break;
            }
        }
    }

    private void selectedView(ViewGroup viewGroup) {
        ImageView selected = viewGroup.findViewById(R.id.right_img);
        selected.setVisibility(View.VISIBLE);
    }

    private void unSelectedView(ViewGroup viewGroup) {
        ImageView selected = viewGroup.findViewById(R.id.right_img);
        selected.setVisibility(View.INVISIBLE);
    }

    private void unselectedEnableView(ViewGroup viewGroup) {
        AppCompatImageView selected = viewGroup.findViewById(R.id.right_img);
        selected.setVisibility(View.INVISIBLE);

        TextView title = viewGroup.findViewById(R.id.name);
        title.setTextColor(getResources().getColor(R.color.color_cccccc));
        TextView message = viewGroup.findViewById(R.id.message);
        message.setTextColor(getResources().getColor(R.color.color_cccccc));
    }

    private void changeLockAll() {
        selectedView(mLockOpenView);
        unSelectedView(mLockWalletView);
        unSelectedView(mLockNoneView);

        //设置值
        SharedPreferencesUtils.setAppLockState(getApplicationContext(), APP_LOCK_STATE_ALL);
    }

    private void changeLockWallet() {
        unSelectedView(mLockOpenView);
        selectedView(mLockWalletView);
        unSelectedView(mLockNoneView);

        //设置值
        SharedPreferencesUtils.setAppLockState(getApplicationContext(), APP_LOCK_STATE_WALLET);
    }

    private void changeLockNone() {
        unSelectedView(mLockOpenView);
        unSelectedView(mLockWalletView);
        selectedView(mLockNoneView);

        //设置值
        SharedPreferencesUtils.setAppLockState(getApplicationContext(), APP_LOCK_STATE_NONE);
    }
}
