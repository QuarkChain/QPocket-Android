package com.quarkonium.qpocket.model.lock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.finger.FingerprintDialogFragment;
import com.quarkonium.qpocket.model.main.viewmodel.MainWallerViewModel;
import com.quarkonium.qpocket.model.main.viewmodel.MainWalletViewModelFactory;
import com.quarkonium.qpocket.util.AppLanguageUtils;
import com.quarkonium.qpocket.util.ConstantLanguages;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.R;

import java.util.Date;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class LockPatternActivity extends AppCompatActivity {

    public static void startActivity(Activity activity) {
        Intent lockIntent = new Intent(activity, LockPatternActivity.class);
        activity.startActivity(lockIntent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public static void startActivity(Context activity) {
        Intent lockIntent = new Intent(activity, LockPatternActivity.class);
        activity.startActivity(lockIntent);
    }

    public static void startActivityForResult(Activity activity, int code) {
        Intent lockIntent = new Intent(activity, LockPatternActivity.class);
        activity.startActivityForResult(lockIntent, code);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private ImageView mWalletIconView;
    private QWWallet mWallet;

    private boolean mIsStop;

    @Inject
    MainWalletViewModelFactory mMainWallerFragmentFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        findViewById(R.id.lock_layout_id).setOnClickListener(v -> showFingerDialog());
        mWalletIconView = findViewById(R.id.wallet_icon);

        MainWallerViewModel mMainWalletFragmentViewModel = new ViewModelProvider(this, mMainWallerFragmentFactory)
                .get(MainWallerViewModel.class);
        mMainWalletFragmentViewModel.findDefaultWalletObserve().observe(this, this::findWalterSuccess);
        mMainWalletFragmentViewModel.error().observe(this, v -> finish());
        mMainWalletFragmentViewModel.findWallet(false);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String language = SharedPreferencesUtils.getCurrentLanguages(newBase);
        if (ConstantLanguages.AUTO.equals(language)) {
            AppLanguageUtils.applyChange(newBase);
            super.attachBaseContext(newBase);
            return;
        }
        super.attachBaseContext(AppLanguageUtils.attachBaseContext(newBase, language));
    }

    @Override
    public void onBackPressed() {
        //屏蔽返回键
    }

    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    private void findWalterSuccess(QWWallet wallet) {
        mWallet = wallet;
        //当前主钱包是观察钱包，则获取第一个非观察钱包
        if (wallet.getIsWatch() == 1) {
            QWWalletDao dao = new QWWalletDao(getApplicationContext());
            mWallet = dao.queryNormalWallet();
            if (mWallet == null) {
                finish();
            }
        }
        mWalletIconView.postDelayed(this::showFingerDialog, 100);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsStop = false;
    }

    @Override
    protected void onStop() {
        mIsStop = true;
        super.onStop();
    }

    private void showFingerDialog() {
        if (mWallet != null && !mIsStop) {
            QWAccountDao dao = new QWAccountDao(getApplicationContext());
            QWAccount account = dao.queryAllParamsByAddress(mWallet.getCurrentAddress());
            FingerprintDialogFragment fragment = new FingerprintDialogFragment();
            fragment.setQuarkWallet(mWallet);
            fragment.setDialogTitle(String.format(getString(R.string.lock_wallet_name), account.getName()));
            fragment.setPasswordListener(new SystemUtils.OnCheckPassWordListenerImp() {
                @Override
                public void onPasswordSuccess(String password) {
                    finish();
                }
            });
            fragment.setLockListener(this::finish);
            fragment.show(getSupportFragmentManager(), "DIALOG_FRAGMENT_TAG");
        }
    }

    @Override
    public void finish() {
        Constant.sLockAppWait = false;
        Constant.sUnlockAppTime = new Date().getTime();

        setResult(Activity.RESULT_OK);
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
