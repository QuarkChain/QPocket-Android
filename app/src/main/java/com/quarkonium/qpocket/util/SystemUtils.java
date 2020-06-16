package com.quarkonium.qpocket.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.StatFs;
import androidx.fragment.app.FragmentManager;
import android.text.TextUtils;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.finger.FingerprintDialogFragment;
import com.quarkonium.qpocket.finger.FingerprintIdentify;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkPasswordDialog;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.view.listener.CommonCallbackListener;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by huowaa on 13-7-13.
 */
public class SystemUtils {

    // Orientation hysteresis amount used in rounding, in degrees
    public static final int ORIENTATION_HYSTERESIS = 20;


//    public static String getMCC(Context context) {
//        String imsiStr = Utils.getIMSI(context);
//        String mcc = "";
//        if (imsiStr != null && imsiStr.length() >= 3) {
//            mcc = imsiStr.substring(0, 3);
//        }
//
//        return mcc;
//    }
//
//    public static String getMNC(Context context) {
//        String imsiStr = Utils.getIMSI(context);
//        String mnc = "";
//        if (imsiStr != null && imsiStr.length() >= 5) {
//            mnc = imsiStr.substring(3, 5);
//        }
//
//        return mnc;
//    }

//    /**
//     * 是否是中国大陆用户
//     * 1、MCC = 460 or
//     * 2、Locale = zh_CN
//     * <p/>
//     * IMSI共有15位，其结构如下：
//     * MCC+MNC+MIN
//     * MCC：Mobile Country Code，移动国家码，共3位，中国为460;
//     * MNC:Mobile Network Code，移动网络码，共2位，电信03，移动02，联通GSM 01，一个典型的IMSI号码为460030912121001;
//     *
//     * @return
//     */
//    public static boolean isMainLandUser(Context context) {
//        String imsiStr = Utils.getIMSI(context);
//        String mcc = "";
//        if (imsiStr != null && imsiStr.length() >= 3) {
//            mcc = imsiStr.substring(0, 3);
//        }
//
//        if (mcc.equals("460")
//                || Locale.getDefault().equals(Locale.SIMPLIFIED_CHINESE)) {
//            return true;
//        } else {
//            return false;
//        }
//    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    /**
     * 是否有网.
     *
     * @param context
     * @return
     */
    public static boolean hasNet(Context context) {

        boolean b = false;
        ConnectivityManager mConnectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mConnectivity.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) { // 注意，这个判断一定要的哦，要不然会出错
            b = true;
        }
        return b;
    }

    /**
     * 网络名称.
     *
     * @param context
     * @return
     * @author lizhipeng
     */
    public static String getNetworkType(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null) {
            if (activeNetInfo.isAvailable()) {
                if (activeNetInfo.getExtraInfo() == null) {
                    return "wifi";
                }
                return activeNetInfo.getExtraInfo();
            }
        }
        return null;
    }

    /**
     * @param context     地球人都知道
     * @param packageName 需要检查的应用程序package名，如 com.google.android.gm ，已封装常量 PKG_GOOGLE_PLAY
     * @return
     * @author tangsong
     */
    public static boolean checkApkExist(Context context, String packageName) {
        if (packageName == null || "".equals(packageName)) {
            return false;
        }

        if (null == context.getPackageManager().getLaunchIntentForPackage(packageName)) {
            return false;
        }

        try {
            context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    /**
     * 获取CPU核心数
     *
     * @return
     */
    public static int getCpuCoresNum() {
        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                return Pattern.matches("cpu[0-9]", pathname.getName());
            }
        }
        try {
            // Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            e.printStackTrace();
            // Default to return 1 core
            return 1;
        }
    }


    // --------------------------------- 储存空间相关

    /**
     * 计算剩余空间
     *
     * @param path
     * @return
     */
    private static long getAvailableSize(String path) {
        StatFs fileStats = new StatFs(path);
        fileStats.restat(path);
        return (long) fileStats.getAvailableBlocks() * fileStats.getBlockSize(); // 注意与fileStats.getFreeBlocks()的区别
    }


    /**
     * 计算系统的剩余空间
     *
     * @return 剩余空间
     */
    public static long getSystemAvailableSize() {
        // context.getFilesDir().getAbsolutePath();
        return getAvailableSize("/data");
    }

    /**
     * 检查是否是正确的email格式
     */
    public static boolean checkEmailFormat(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        String reg = "^[0-9a-z_-][_.0-9a-z-]{0,31}@([0-9a-z][0-9a-z-]{0,30}\\.){1,4}[a-z]{2,4}$";
        Pattern pattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * 检查是否是正确的大陆手机号
     * 大陆手机号的格式判断：以1开头，后面跟10为数字
     */
    public static boolean checkPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }
        String reg = "^1[0-9]{10}$";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    /**
     * 获得APP版本信息（versionCode）
     */
    private static String getVersionName() {
        PackageInfo manager = null;
        try {
            Context context = MainApplication.getContext();
            manager = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (manager != null) {
            return String.valueOf(manager.versionName);
        } else {
            return "";
        }
    }

    /**
     * 版本号比较
     */
    private static int compareVersion(String version1, String version2) {
        try {
            if (version1.equals(version2)) {
                return 0;
            }
            String[] version1Array = version1.split("\\.");//转义
            String[] version2Array = version2.split("\\.");

            int index = 0;
            // 获取最小长度值
            int minLen = Math.min(version1Array.length, version2Array.length);
            int diff = 0;
            // 循环判断每位的大小
            while (index < minLen && (diff = Integer.parseInt(version1Array[index]) -
                    Integer.parseInt(version2Array[index])) == 0) {
                index++;
            }
            if (diff == 0) {
                // 如果位数不一致，比较多余位数
                for (int i = index; i < version1Array.length; i++) {
                    if (Integer.parseInt(version1Array[i]) > 0) {
                        return 1;
                    }
                }

                for (int i = index; i < version2Array.length; i++) {
                    if (Integer.parseInt(version2Array[i]) > 0) {
                        return -1;
                    }
                }
                return 0;
            } else {
                return diff > 0 ? 1 : -1;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean sIsCheckingUpdate = false;

    public static void checkForUpdates(Activity activity, boolean tipIfUpToDate, CommonCallbackListener completionHandler) {
        if (sIsCheckingUpdate) {
            return;
        }
        sIsCheckingUpdate = true;

        WeakReference<Activity> mActivity = new WeakReference<>(activity);
        AVQuery<AVObject> query = new AVQuery<>("Application");
        query.orderByAscending("createdAt");
        query.whereEqualTo("platform", "Android");
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                sIsCheckingUpdate = false;

                if (completionHandler != null) {
                    completionHandler.onCallback();
                }
                if (list == null || list.isEmpty()) {
                    return;
                }

                if (mActivity.get() != null) {
                    Activity context = mActivity.get();
                    if (context.isFinishing()) {
                        return;
                    }

                    AVObject result = list.get(list.size() - 1);
                    String releaseNotesKey = "releaseNotes";
                    if (ToolUtils.isZh(context)) {
                        releaseNotesKey = releaseNotesKey + "Cn";
                    } else if (ToolUtils.isKo(context)) {
                        releaseNotesKey = releaseNotesKey + "Ko";
                    }
                    String newVersion = (String) result.get("version");
                    if (compareVersion(getVersionName(), newVersion) < 0) { // has new version

                        if (!tipIfUpToDate) { // called from main activity when app start
                            SharedPreferences sharedPreferences = context.getSharedPreferences("QuarkWallet_CheckedUpdateVersion", Context.MODE_PRIVATE);
                            String checkedVersion = sharedPreferences.getString("version", "1.0.0");
                            if (newVersion.equals(checkedVersion)) {
                                return;
                            }
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("version", newVersion);
                            editor.apply();
                        }

                        QuarkSDKDialog dialog = new QuarkSDKDialog(context);
                        dialog.setTitle(context.getResources().getString(R.string.check_update_new) + " v" + newVersion);
                        dialog.setMessage(((String) result.get(releaseNotesKey)).replace("\\n", "\n"));
                        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
                        dialog.setPositiveBtn(R.string.update, v -> {
                            dialog.dismiss();
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.VIEW");
                            AVFile file = (AVFile) result.get("package");
                            Uri content_url = Uri.parse(file.getUrl());
                            intent.setData(content_url);
                            context.startActivity(intent);
                        });
                        dialog.show();
                    } else if (tipIfUpToDate) {
                        MyToast.showSingleToastShort(context, R.string.check_update_up_to_date);
                    }
                }
            }
        });
    }

    public interface OnCheckPasswordListener {
        void onPasswordSuccess(String password);

        void onCancel();
    }

    public static abstract class OnCheckPassWordListenerImp implements OnCheckPasswordListener {

        @Override
        public void onCancel() {

        }
    }

    //密码校验
    public static void checkPassword(Activity activity, FragmentManager fragmentManager, QWWallet wallet, OnCheckPasswordListener listener) {
        if (SharedPreferencesUtils.isNewInputFingerApp(activity.getApplicationContext())) {
            SharedPreferencesUtils.setNewInputFingerApp(activity.getApplicationContext());
            if (!SharedPreferencesUtils.isSupportFingerprint(activity.getApplicationContext())) {
                //如果支持指纹，则弹框进行指纹校验
                FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(activity.getApplicationContext());
                if (mFingerprintIdentify.isFingerprintEnable()) {
                    showFingerDialog(activity, fragmentManager, wallet, listener);
                    return;
                }
            }
        }

        if (SharedPreferencesUtils.isSupportFingerprint(activity.getApplicationContext())) {
            FingerprintDialogFragment fragment = new FingerprintDialogFragment();
            fragment.setQuarkWallet(wallet);
            fragment.setPasswordListener(listener);
            fragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
        } else {
            QuarkPasswordDialog textDialog = new QuarkPasswordDialog(activity);
            textDialog.setWallet(wallet);
            textDialog.setPasswordListener(listener);
            textDialog.setHint(Constant.sPasswordHintMap.get(wallet.getKey()));
            textDialog.show();
        }
    }

    private static void showFingerDialog(Activity activity, FragmentManager fragmentManager, QWWallet wallet, OnCheckPasswordListener listener) {
        QuarkSDKDialog dialog = new QuarkSDKDialog(activity);
        dialog.setTitle(R.string.finger_open_tip_title);
        dialog.setMessage(R.string.finger_open_tip_msg);
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.setPositiveBtn(R.string.ok, v -> {
            dialog.dismiss();
            openTouch(fragmentManager, wallet, listener);
        });
        dialog.show();
    }

    private static void openTouch(FragmentManager fragmentManager, QWWallet wallet, OnCheckPasswordListener listener) {
        FingerprintDialogFragment fragment = new FingerprintDialogFragment();
        fragment.setQuarkWallet(wallet);
        fragment.setPasswordListener(listener);
        fragment.setStage(FingerprintDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
        fragment.show(fragmentManager, "DIALOG_FRAGMENT_TAG");
    }
}
