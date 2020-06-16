package com.quarkonium.qpocket;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;

import androidx.core.view.ViewCompat;
import androidx.multidex.MultiDexApplication;

import com.avos.avoscloud.AVOSCloud;
import com.llew.huawei.verifier.LoadedApkHuaWei;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.Debug;
import com.quarkonium.qpocket.api.db.dao.QWChainDao;
import com.quarkonium.qpocket.api.db.dao.QWShardDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.di.DaggerAppComponent;
import com.quarkonium.qpocket.crash.AppCrashHandler;
import com.quarkonium.qpocket.crash.CrashReportActivity;
import com.quarkonium.qpocket.finger.FingerprintIdentify;
import com.quarkonium.qpocket.model.lock.LockPatternActivity;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.main.WalletManagerActivity;
import com.quarkonium.qpocket.model.splash.SplashActivity;
import com.quarkonium.qpocket.model.transaction.TransactionCreateActivity;
import com.quarkonium.qpocket.model.transaction.TransactionSendActivity;
import com.quarkonium.qpocket.util.PasswordUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.util.http.HttpUtils;
import com.quarkonium.qpocket.model.book.AddressBookActivity;
import com.quarkonium.qpocket.model.unlock.UnlockManagerActivity;
import com.quarkonium.qpocket.util.ConnectReceiver;
import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.footer.BallPulseFooter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.reactivex.plugins.RxJavaPlugins;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainApplication extends MultiDexApplication implements HasAndroidInjector {

    private static Context mContext;

    public int count = 0;//app栈中有多少个activity，用来判断是否在前台还是后台
    private long mTimeStart = 0;//记录切换到后台的毫秒值
    private HashSet<String> mLockActivity = new HashSet<>();

    private boolean mIsLockState = false;

    public static Context getContext() {
        return mContext;
    }

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        //修复华为部分机型regist too many Broadcast Receivers
        LoadedApkHuaWei.hookHuaWeiVerifier(this);

        //崩溃抓获
        Thread.setDefaultUncaughtExceptionHandler(new AppCrashHandler(this));

        //dagger注解 MVVP模式
        DaggerAppComponent
                .builder()
                .application(this)
                .build()
                .inject(this);

        //RxJava异常处理
        setRxJavaErrorHandler();

        //Log日志
        Logger.addLogAdapter(new AndroidLogAdapter() {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return Debug.DEBUG;
            }
        });

        //服务区数据
        // 初始化
        AVOSCloud.initialize(this, "xxxx-xxxx", "xxxx");
        AVOSCloud.setDebugLogEnabled(Debug.DEBUG);

        //主进程
        boolean isMainProcess = getPackageName().equals(getCurrentProcessName());
        if (isMainProcess) {
            SmartRefreshLayout.setDefaultRefreshHeaderCreator((Context context, RefreshLayout layout) -> {
                MaterialHeader header = new MaterialHeader(context);
                header.setColorSchemeResources(R.color.colorAccent);
                ViewCompat.setElevation(header, UiUtils.dpToPixel(3));
                return header;
            });
            //设置全局的Footer构建器
            SmartRefreshLayout.setDefaultRefreshFooterCreator((Context context, RefreshLayout layout) -> {
                //指定为经典Footer，默认是 BallPulseFooter
                BallPulseFooter footer = new BallPulseFooter(context);
                footer.setNormalColor(Color.parseColor("#d0d0d0"));
                footer.setAnimatingColor(context.getResources().getColor(R.color.text_title));
                return footer;
            });

            registerReceiver(new ConnectReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            initPrint();
            initQKCPath();
            initEthPath();
            init();

            try {
                new WebView(this).destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }

    /**
     * 获取当前进程名
     */
    private String getCurrentProcessName() {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) getSystemService
                (Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }

    /**
     * RxJava2 当取消订阅后(dispose())，RxJava抛出的异常后续无法接收(此时后台线程仍在跑，可能会抛出IO等异常),全部由RxJavaPlugin接收，需要提前设置ErrorHandler
     * 详情：http://engineering.rallyhealth.com/mobile/rxjava/reactive/2017/03/15/migrating-to-rxjava-2.html#Error Handling
     */
    private void setRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(throwable -> {
        });
    }

    //*************指纹检测*******************
    //指纹开启后，切换到后台时锁住app
    private void initPrint() {
        //如果指纹有开启，并未设置过锁屏模式，则锁屏模式为全部
        int index = SharedPreferencesUtils.getAppLockState(getApplicationContext());
        if (index == -1 && SharedPreferencesUtils.isSupportFingerprint(getApplicationContext())) {
            SharedPreferencesUtils.setAppLockState(getApplicationContext(), UnlockManagerActivity.APP_LOCK_STATE_ALL);
        }

        //应用前后台切换的判断
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStopped(Activity activity) {
                count--;
                if (count == 0) {
                    mTimeStart = new Date().getTime();
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                mIsLockState = false;
                if (count == 0) {
                    long timeEnd = new Date().getTime();
                    //切换到前台和切换到后台的时间差大于等于3分钟
                    if (mTimeStart != 0 && timeEnd - mTimeStart >= Constant.APP_LOCK_TIME) {
                        mIsLockState = true;
                        if (needLockActivity()) {
                            LockPatternActivity.startActivity(activity);
                        }
                    }
                }
                count++;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                String name = activity.getComponentName().getClassName();
                mLockActivity.remove(name);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                String name = activity.getComponentName().getClassName();
                if (TextUtils.equals(WalletManagerActivity.class.getName(), name)
                        || TextUtils.equals(AddressBookActivity.class.getName(), name)
                        || TextUtils.equals(TransactionCreateActivity.class.getName(), name)
                        || TextUtils.equals(TransactionSendActivity.class.getName(), name)) {
                    mLockActivity.add(name);
                }
            }
        });
    }

    public boolean isLockState() {
        int index = SharedPreferencesUtils.getAppLockState(getApplicationContext());
        if (index == UnlockManagerActivity.APP_LOCK_STATE_ALL || index == UnlockManagerActivity.APP_LOCK_STATE_WALLET) {
            return mIsLockState
                    && SharedPreferencesUtils.isSupportFingerprint(getApplicationContext())
                    && new FingerprintIdentify(getApplicationContext()).isFingerprintEnable()
                    && havePasswordWallet();
        }
        return false;
    }

    private boolean needLockActivity() {
        if (isTopActivity(LockPatternActivity.class.getSimpleName())
                || isTopActivity(SplashActivity.class.getSimpleName())
                || isTopActivity(CrashReportActivity.class.getSimpleName())
                || !SharedPreferencesUtils.isSupportFingerprint(getApplicationContext())
                || !new FingerprintIdentify(getApplicationContext()).isFingerprintEnable()
                || !havePasswordWallet()) {
            //闪屏 崩溃 锁屏页不处理
            //未开启指纹或者不支持指纹不处理
            //只有观察钱包不处理
            return false;
        }

        int index = SharedPreferencesUtils.getAppLockState(getApplicationContext());
        if (index == UnlockManagerActivity.APP_LOCK_STATE_ALL) {
            //锁定整个APP
            return true;
        } else if (index == UnlockManagerActivity.APP_LOCK_STATE_WALLET) {
            //锁定钱包相关界面
            //main tab页是钱包时都锁
            //钱包管理界面，转账交易,地址本界面在activity栈中
            int mainTabIndex = SharedPreferencesUtils.getMainTabIndex(getApplicationContext());
            if (mainTabIndex == MainActivity.MAIN_TAG_WALLET || mainTabIndex == MainActivity.MAIN_TAG_SETTING || mLockActivity.size() > 0) {
                return true;
            } else {
                Constant.sLockAppWait = true;
            }
        }
        return false;
    }

    public static boolean isTopActivity(String shortClassName) {
        String topClassName = getTopActivityShortClassName();
        return !TextUtils.isEmpty(topClassName) && topClassName.contains(shortClassName);
    }

    private static String getTopActivityShortClassName() {
        ComponentName cn = getTopActivity();
        if (cn == null) {
            return "";
        } else {
            return cn.getShortClassName();
        }
    }

    private static ComponentName getTopActivity() {
        try {
            ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            return am.getRunningTasks(1).get(0).topActivity;
        } catch (Exception e) {
            return null;
        }
    }

    //是否有正常钱包，观察钱包没有密码
    public static boolean havePasswordWallet() {
        //获取所有钱包
        QWWalletDao dao = new QWWalletDao(getContext());
        QWWallet wallet = dao.queryNormalWallet();
        return wallet != null;
    }
    //*************指纹检测*******************

    public static void initQKCPath() {
        int index = SharedPreferencesUtils.getQKCNetworkIndex(getContext());
        switch (index) {
            case Constant.QKC_PUBLIC_MAIN_INDEX:
                Constant.sNetworkId = new BigInteger(String.valueOf(Constant.QKC_PUBLIC_MAIN_INDEX));
                Constant.sQKCNetworkPath = Constant.QKC_PUBLIC_PATH_MAIN;
                break;
            case Constant.QKC_PUBLIC_DEVNET_INDEX:
                Constant.sNetworkId = new BigInteger(String.valueOf(Constant.QKC_PUBLIC_DEVNET_INDEX));
                Constant.sQKCNetworkPath = Constant.QKC_PUBLIC_PATH_DEVNET;
                break;
        }
    }

    public static void initEthPath() {
        int index = (int) SharedPreferencesUtils.getEthNetworkIndex(getContext());
        switch (index) {
            case Constant.ETH_PUBLIC_PATH_MAIN_INDEX:
                Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_MAIN_INDEX;
                Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_MAIN;
                break;
            case Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX:
                Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX;
                Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_ROPSTEN;
                break;
            case Constant.ETH_PUBLIC_PATH_KOVAN_INDEX:
                Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_KOVAN_INDEX;
                Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_KOVAN;
                break;
            case Constant.ETH_PUBLIC_PATH_RINKBY_INDEX:
                Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_RINKBY_INDEX;
                Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_RINKBY;
                break;
        }
    }

    private void init() {
        if (!SharedPreferencesUtils.isInstall(this)) {
            QWChainDao chainDao = new QWChainDao(this);
            chainDao.insertTotal(1);

            QWShardDao shardDao = new QWShardDao(this);
            shardDao.insertTotal(1);

            //插入默认
            QWTokenDao tokenDao = new QWTokenDao(this);
            tokenDao.insertDefault();

            SharedPreferencesUtils.setNewApp(this, true);
            SharedPreferencesUtils.setIsInstall(this, true);
        }

        //初始化密码加载控件
        new Thread(() -> PasswordUtils.getPasswordLevel("0")).start();

        initCountryByIp();

        //设置market默认币种
        String coin = SharedPreferencesUtils.getCurrentMarketCoin(this);
        if (TextUtils.isEmpty(coin)) {
            if (ToolUtils.isZh(this)) {
                SharedPreferencesUtils.setCurrentMarketCoin(this, "cny");
            } else {
                SharedPreferencesUtils.setCurrentMarketCoin(this, "usd");
            }
        }
    }

    //获取国家IP
    public void initCountryByIp() {
        String path = "https://ipapi.co/country/";
        final okhttp3.Request request = new okhttp3.Request.Builder()
                .url(path)
                .build();
        final Call call = HttpUtils.getOkHttp().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.body() != null) {
                    try {
                        SharedPreferencesUtils.setCountryCode(mContext, response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
