package com.quarkonium.qpocket.model.splash;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.finger.FingerprintIdentify;
import com.quarkonium.qpocket.model.lock.LockPatternActivity;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.splash.viewmodel.SplashViewModel;
import com.quarkonium.qpocket.model.splash.viewmodel.SplashViewModelFactory;
import com.quarkonium.qpocket.model.wallet.HomeActivity;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.model.unlock.UnlockManagerActivity;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SplashActivity extends BaseActivity {

    private static class MyHandler extends Handler {
        WeakReference<SplashActivity> mActivityWptr;

        MyHandler(SplashActivity activity) {
            mActivityWptr = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            SplashActivity activity = mActivityWptr.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    /**
     * Duration of wait
     **/
    private static final int SPLASH_DISPLAY_LENGTH = 1500;
    private static final int WELCOME_FINISH = 100;


    @Inject
    SplashViewModelFactory mSplashViewModelFactory;
    SplashViewModel mSplashViewModel;

    private Handler mHandler = new MyHandler(this);

    private QWWallet[] mWallet;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_splash;
    }

    @Override
    public int getActivityTitle() {
        return 0;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }

        //获取当前创建钱包数量
        mSplashViewModel = new ViewModelProvider(this, mSplashViewModelFactory)
                .get(SplashViewModel.class);
        mSplashViewModel.wallets().observe(this, this::walletSuccess);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(WELCOME_FINISH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.sendEmptyMessageDelayed(WELCOME_FINISH, SPLASH_DISPLAY_LENGTH);
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case WELCOME_FINISH:
                if (mWallet == null || mWallet.length == 0) {
                    //打开创建钱包页
                    Intent intent = new Intent(this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    goMain();
                }
                break;
        }
    }

    private void walletSuccess(QWWallet[] wallet) {
        mWallet = wallet;
    }

    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        int index = SharedPreferencesUtils.getAppLockState(getApplicationContext());
        if (index == UnlockManagerActivity.APP_LOCK_STATE_WALLET || index == UnlockManagerActivity.APP_LOCK_STATE_ALL) {
            if (index == UnlockManagerActivity.APP_LOCK_STATE_WALLET) {
                //只锁定钱包tab
                int tabIndex = SharedPreferencesUtils.getMainTabIndex(getApplicationContext());
                if (tabIndex != MainActivity.MAIN_TAG_WALLET && tabIndex != MainActivity.MAIN_TAG_SETTING) {
                    Constant.sLockAppWait = true;
                    return;
                }
            }
            if (SharedPreferencesUtils.isSupportFingerprint(getApplicationContext())) {
                FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(getApplicationContext());
                if (mFingerprintIdentify.isFingerprintEnable() && MainApplication.havePasswordWallet()) {
                    LockPatternActivity.startActivity(this);
                }
            }
        }
    }

    protected void initStatusBar() {
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window win = getWindow();
            win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
            win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //设置状态栏颜色
            win.setStatusBarColor(Color.BLACK);
            //设置导航栏颜色
            win.setNavigationBarColor(Color.BLACK);
        }
    }
}
