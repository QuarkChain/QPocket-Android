package com.quarkonium.qpocket.model.main;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.Constants;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.SharedPreferenceRepository;
import com.quarkonium.qpocket.base.SupportBaseActivity;
import com.quarkonium.qpocket.base.SupportBaseFragment;
import com.quarkonium.qpocket.finger.FingerprintDialogFragment;
import com.quarkonium.qpocket.finger.FingerprintIdentify;
import com.quarkonium.qpocket.model.lock.LockPatternActivity;
import com.quarkonium.qpocket.model.main.viewmodel.MainWallerViewModel;
import com.quarkonium.qpocket.model.main.viewmodel.MainWalletViewModelFactory;
import com.quarkonium.qpocket.model.wallet.HomeActivity;
import com.quarkonium.qpocket.rx.ChooseWalletEvent;
import com.quarkonium.qpocket.rx.RxBus;
import com.quarkonium.qpocket.rx.Subscribe;
import com.quarkonium.qpocket.rx.ThreadMode;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.view.CircleProgressBar;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class MainActivity extends SupportBaseActivity {

    private final static String MAIN_CURRATN_TAG = "main_current_tag";
    public final static int MAIN_TAG_WALLET = 1002;
    public final static int MAIN_TAG_SETTING = 1004;
    private int mCurrentTag;


    private View mTagWalletView;
    private View mTagSettingView;

    private SupportBaseFragment mCurrentFragment;
    private WalletFragment mWalletFragment;
    private SettingFragment mSettingFragment;

    private View mInstallLayout;
    private CircleProgressBar mCircleProgressBar;

    @Inject
    public MainWalletViewModelFactory mMainWallerFragmentFactory;
    private MainWallerViewModel mMainWalletFragmentViewModel;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    public int getActivityTitle() {
        return 0;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTagWalletView = findViewById(R.id.menu_tag_wallet);
        mTagWalletView.setOnClickListener(v -> onClickWallet());
        mTagSettingView = findViewById(R.id.menu_tag_setting);
        mTagSettingView.setOnClickListener(v -> onClickSetting());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        //注册事件监听
        RxBus.get().register(this);
        EventBus.getDefault().register(this);

        mMainWalletFragmentViewModel = new ViewModelProvider(this, mMainWallerFragmentFactory)
                .get(MainWallerViewModel.class);

        //恢复数据
        SettingFragment firstFragment = findFragment(SettingFragment.class);
        if (firstFragment != null) {
            // 这里库已经做了Fragment恢复,所有不需要额外的处理了, 不会出现重叠问题
            // 这里我们需要拿到mFragments的引用
            mWalletFragment = findFragment(WalletFragment.class);
            mSettingFragment = firstFragment;
        }

        //冷启动
        if (savedInstanceState == null) {
            //切换语言
            if (Constant.sIsChangeLanguage) {
                enterSetting();
                Constant.sIsChangeLanguage = false;
                return;
            }

            //恢复上次选中位置
            int index = SharedPreferencesUtils.getMainTabIndex(getApplicationContext());
            if (index == -1) {
                //未记录过位置
                QWWallet wallet = initCurrentMainWallet();
                if (wallet != null) {
                    //默认打开钱包
                    enterWallet();
                } else {
                    //没有钱包
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                }
            } else {
                enterTab(index);
            }

            //版本升级校验
            install();
        } else {
            //恢复选中项
            mCurrentTag = savedInstanceState.getInt(MAIN_CURRATN_TAG);
            enterTab(mCurrentTag);
        }
    }

    private void enterTab(int index) {
        switch (index) {
            case MAIN_TAG_WALLET:
                enterWallet();
                break;
            case MAIN_TAG_SETTING:
                enterSetting();
                break;
        }
    }

    private void initInstallFinish() {
        //弹框提示
        //升级之前未做弹窗
        QWWallet wallet = initCurrentMainWallet();
        if (SharedPreferencesUtils.isNewInputFingerApp(getApplicationContext())
                && !SharedPreferencesUtils.isSupportFingerprint(getApplicationContext())
                && wallet != null && wallet.getIsWatch() != 1) {
            //指纹检测状态关闭
            SharedPreferencesUtils.setNewInputFingerApp(getApplicationContext());

            //如果支持指纹，则弹框进行指纹校验
            FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(getApplicationContext());
            if (mFingerprintIdentify.isFingerprintEnable()) {
                showFingerDialog();
            }
        } else {
            SystemUtils.checkForUpdates(this, false, null);
        }
    }

    private QWWallet initCurrentMainWallet() {
        SharedPreferenceRepository repository = new SharedPreferenceRepository(this);
        String key = repository.getCurrentWalletKey();
        //无钱包
        if (TextUtils.isEmpty(key)) {
            return null;
        }

        QWWalletDao dao = new QWWalletDao(this);
        return dao.queryByKey(key);
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存语言
        outState.putInt(MAIN_CURRATN_TAG, mCurrentTag);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            boolean isChangeShard = intent.getBooleanExtra(Constant.KEY_CHANGE_SHARD, false);
            int requestCode = intent.getIntExtra(Constant.KEY_RESULT, -1);
            if (requestCode == Constant.REQUEST_CODE_SEND_TOKEN_MERGE) {
                QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                dialog.setTitle(R.string.transaction_balance_token_merge_success_title);
                dialog.setMessage(isChangeShard ? R.string.transaction_balance_token_merge_success_message : R.string.transaction_balance_token_merge_success_message_nochange);
                dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
                dialog.show();
                refreshWallet();
            } else if (requestCode == Constant.REQUEST_CODE_SEND_QCK_MERGE) {
                QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                dialog.setTitle(R.string.transaction_balance_error_message_merge_success_title);
                dialog.setMessage(R.string.transaction_balance_error_message_merge_success_message);
                dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
                dialog.show();
                onClickWallet();
            } else if (requestCode == Constant.REQUEST_CODE_SEND_PUBLIC_MERGE) {
                QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                dialog.setTitle(R.string.transaction_balance_error_message_merge_success_title);
                dialog.setMessage(isChangeShard ? R.string.transaction_balance_public_merge_chain_success_message : R.string.transaction_balance_public_merge_success_message_nochange);
                dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
                dialog.show();
                refreshWallet();
            } else if (requestCode == Constant.REQUEST_CODE_SEND_TOKEN_SWITCH) {
                QuarkSDKDialog dialog = new QuarkSDKDialog(this);
                dialog.setTitle(R.string.transaction_balance_token_switch_title);
                dialog.setMessage(isChangeShard ? R.string.transaction_balance_public_switch_success_message : R.string.transaction_balance_public_switch_success_message_nochange);
                dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
                dialog.show();
                refreshWallet();
            } else {
                //创建钱包成功
                //使得数据能刷新
                updateFragment();
                //切换到钱包页面
                if (!mTagWalletView.isSelected()) {
                    enterWallet();
                }

                //获取当前钱包状态
                if (SharedPreferencesUtils.isNewInputFingerApp(getApplicationContext()) || SharedPreferencesUtils.isNewInstallApp(getApplicationContext())) {
                    QWWallet wallet = initCurrentMainWallet();
                    if (wallet != null) {
                        //如果是QKC，则弹出QKC引导页
//                        if (QWWalletUtils.isQKCValidAddress(wallet.getCurrentAddress()) && SharedPreferencesUtils.isNewInstallApp(getApplicationContext())) {
//                            showQKCGuide();
//                        } else
                        if (wallet.getIsWatch() != 1
                                && SharedPreferencesUtils.isNewInputFingerApp(getApplicationContext())
                                && !SharedPreferencesUtils.isSupportFingerprint(getApplicationContext())) {
                            //检测状态为关闭
                            SharedPreferencesUtils.setNewInputFingerApp(getApplicationContext());
                            //如果支持指纹，则弹框进行指纹校验
                            FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(getApplicationContext());
                            if (mFingerprintIdentify.isFingerprintEnable()) {
                                showFingerDialog();
                            }
                        }
                    }
                }
            }
        }
    }

    private void refreshWallet() {
        if (!mTagWalletView.isSelected()) {
            onClickWallet();
        }
        mWalletFragment.refreshBalance();
    }

    //*******Fragment 切换*******
    private boolean hasWallet() {
        QWWalletDao dao = new QWWalletDao(getApplication());
        List<QWWallet> list = dao.queryAll();
        return list != null && !list.isEmpty();
    }

    private boolean isNotInit(int position) {
        WalletFragment firstFragment = findFragment(WalletFragment.class);
        if (firstFragment == null) {
            mWalletFragment = WalletFragment.newInstance();
            mSettingFragment = SettingFragment.newInstance();
            loadMultipleRootFragment(R.id.main_frame_layout, position,
                    mWalletFragment,
                    mSettingFragment);
            return false;
        }
        return true;
    }

    //*******Wallet*******
    private void onClickWallet() {
        if (mTagWalletView.isSelected()) {
            return;
        }
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }

        //如果有创建钱包，则进入钱包页
        if (hasWallet()) {
            if (Constant.isCanLock(getApplicationContext())) {
                LockPatternActivity.startActivityForResult(this, Constant.REQUEST_CODE_LOCK_APP_WALLET);
            } else {
                enterWallet();
            }
        } else {
            //打开创建钱包页
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
        }
    }

    private void enterWallet() {
        mTagWalletView.setSelected(true);
        mTagSettingView.setSelected(false);

        if (isNotInit(0)) {
            showHideFragment(mWalletFragment, mCurrentFragment);
        }
        mCurrentFragment = mWalletFragment;

        mCurrentTag = MAIN_TAG_WALLET;
        SharedPreferencesUtils.setMainTabIndex(getApplicationContext(), mCurrentTag);

        UmengStatistics.mainWalletClickCount(getApplication(), QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
    }
    //*******Wallet*******

    //*******Setting*******
    private void onClickSetting() {
        if (mTagSettingView.isSelected()) {
            return;
        }
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }

        //如果有创建钱包，则进入设置页
        if (hasWallet()) {
            if (Constant.isCanLock(getApplicationContext())) {
                LockPatternActivity.startActivityForResult(this, Constant.REQUEST_CODE_LOCK_APP_SETTING);
            } else {
                enterSetting();
            }
        } else {
            //打开创建钱包页
            showCreateWalletDialog();
        }
    }

    private void enterSetting() {
        mTagWalletView.setSelected(false);
        mTagSettingView.setSelected(true);

        if (isNotInit(1)) {
            showHideFragment(mSettingFragment, mCurrentFragment);
        }
        mCurrentFragment = mSettingFragment;

        mCurrentTag = MAIN_TAG_SETTING;
        SharedPreferencesUtils.setMainTabIndex(getApplicationContext(), mCurrentTag);

        UmengStatistics.mainSettingClickCount(getApplication(), QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
    }
    //*******Setting*******
    //*******Fragment 切换*******

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_CHANGE_LANGUAGE && Constant.sIsChangeLanguage) {
            try {
                //避免重启太快 恢复旧数据
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.remove(mWalletFragment);
                fragmentTransaction.remove(mSettingFragment);
                fragmentTransaction.commitAllowingStateLoss();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        } else if (requestCode == Constant.REQUEST_CODE_LOCK_APP_WALLET && resultCode == Activity.RESULT_OK) {
            enterWallet();
        } else if (requestCode == Constant.REQUEST_CODE_LOCK_APP_SETTING && resultCode == Activity.RESULT_OK) {
            enterSetting();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //取消注册
        RxBus.get().unRegister(this);
        EventBus.getDefault().unregister(this);
    }

    private void showFingerDialog() {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.finger_open_tip_title);
        dialog.setMessage(R.string.finger_open_tip_msg);
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.setPositiveBtn(R.string.ok, v -> {
            dialog.dismiss();
            openTouch();
        });
        dialog.show();
    }

    private void openTouch() {
        FingerprintDialogFragment fragment = new FingerprintDialogFragment();
        fragment.setStage(FingerprintDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
        fragment.show(getSupportFragmentManager(), "DIALOG_FRAGMENT_TAG");
    }

    public void updateFragment() {
        updateWallet();
        mMainWalletFragmentViewModel.findWallet();
    }


    public void updateFragmentNoWallet() {
        mMainWalletFragmentViewModel.findWallet();
    }

    public void updateFragmentNoDApp() {
        updateWallet();
        mMainWalletFragmentViewModel.findWallet();
    }

    private void updateWallet() {
        if (mWalletFragment != null) {
            mWalletFragment.isNeedLoad = true;
        }
    }

    private void showCreateWalletDialog() {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.empty_create_wallet_title);
        dialog.setMessage(R.string.empty_create_wallet_msg);
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.setPositiveBtn(R.string.ok, v -> {
            //打开创建钱包页
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            dialog.dismiss();
        });
        dialog.show();
    }


    //*************升级************
    private void install() {
        //是否为全新安装
        boolean isNew = SharedPreferencesUtils.isNewApp(this);
        if (isNew) {
            //全新安装不做通知权限请求
            SharedPreferencesUtils.setAlertInstallNotifyPermission(getApplicationContext());
        }
        //是否为升级
        int appVersion = Integer.parseInt(Constants.getAppVersion(this));
        int formalVersion = SharedPreferencesUtils.getAppVersion(getApplicationContext());
        if (isNew || formalVersion < appVersion) {
            //全新安装 或 版本升级 进行数据升级
            ViewStub viewStub = findViewById(R.id.view_stub_install);
            viewStub.inflate();
            mInstallLayout = findViewById(R.id.install_progress_bar_layout);
            mCircleProgressBar = findViewById(R.id.install_progress_bar);
            mMainWalletFragmentViewModel.installProgress().observe(this, this::onUpdateProgress);
            mMainWalletFragmentViewModel.install();
        } else {
            initInstallFinish();
        }
    }

    public void onUpdateProgress(int progress) {
        if (progress == 100) {
            mCircleProgressBar.endProgress(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mInstallLayout.setVisibility(View.GONE);
                    initInstallFinish();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        } else {
            if (mInstallLayout.getVisibility() != View.VISIBLE) {
                mInstallLayout.setVisibility(View.VISIBLE);
            }
            //下一次动画到达时间
            long time = (long) (Math.random() * 200 + 200);
            //更新进度
            mCircleProgressBar.updateProgress(progress, time);
        }
    }
    //*************升级************

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void rxBusEventChooseWallet(ChooseWalletEvent event) {
        updateFragment();
    }

    @Subscribe(code = Constant.RX_BUS_CODE_PUBLIC_SALE, threadMode = ThreadMode.MAIN)
    public void rxBusEventPublicSale(String sale) {
        if (mWalletFragment != null) {
            mWalletFragment.rxBusEventToken("");
        }
    }

    @Override
    public void onBackPressedSupport() {
        super.onBackPressedSupport();
    }
}
