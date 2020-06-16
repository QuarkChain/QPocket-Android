package com.quarkonium.qpocket.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.DAppFavorite;
import com.quarkonium.qpocket.api.db.table.QWRecentDApp;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class Utils {

    public static String getDeviceId(Context context) {
        StringBuilder sbDeviceId = new StringBuilder();
        //获得AndroidId（无需权限）
        String androidid = getAndroidId(context);
        //获得设备序列号（无需权限）
        String serial = getSERIAL();
        //获得硬件uuid（根据硬件相关属性，生成uuid）（无需权限）
        String uuid = getDeviceUUID().replace("-", "");
        //追加androidid
        if (androidid != null && androidid.length() > 0) {
            sbDeviceId.append(androidid);
            sbDeviceId.append("|");
        }
        //追加serial
        if (serial != null && serial.length() > 0) {
            sbDeviceId.append(serial);
            sbDeviceId.append("|");
        }
        //追加硬件uuid
        if (uuid.length() > 0) {
            sbDeviceId.append(uuid);
        }

        //生成SHA1，统一DeviceId长度
        if (sbDeviceId.length() > 0) {
            try {
                byte[] hash = getHashByString(sbDeviceId.toString());
                String sha1 = bytesToHex(hash);
                if (sha1.length() > 0) {
                    //返回最终的DeviceId
                    return sha1;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //如果以上硬件标识数据均无法获得，
        //则DeviceId默认使用系统随机数，这样保证DeviceId不为空
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获得设备的AndroidId
     *
     * @param context 上下文
     * @return 设备的AndroidId
     */
    private static String getAndroidId(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * 获得设备序列号（如：WTK7N16923005607）, 个别设备无法获取
     *
     * @return 设备序列号
     */
    private static String getSERIAL() {
        try {
            return Build.SERIAL;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * 获得设备硬件uuid
     * 使用硬件信息，计算出一个随机数
     *
     * @return 设备硬件uuid
     */
    private static String getDeviceUUID() {
        try {
            String dev = "3883756" +
                    Build.BOARD.length() % 10 +
                    Build.BRAND.length() % 10 +
                    Build.DEVICE.length() % 10 +
                    Build.HARDWARE.length() % 10 +
                    Build.ID.length() % 10 +
                    Build.MODEL.length() % 10 +
                    Build.PRODUCT.length() % 10 +
                    Build.SERIAL.length() % 10;
            return new UUID(dev.hashCode(),
                    Build.SERIAL.hashCode()).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * 取SHA1
     *
     * @param data 数据
     * @return 对应的hash值
     */
    private static byte[] getHashByString(String data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.reset();
            messageDigest.update(data.getBytes("UTF-8"));
            return messageDigest.digest();
        } catch (Exception e) {
            return "".getBytes();
        }
    }

    /**
     * 转16进制字符串
     *
     * @param data 数据
     * @return 16进制字符串
     */
    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        String stmp;
        for (int n = 0; n < data.length; n++) {
            stmp = (Integer.toHexString(data[n] & 0xFF));
            if (stmp.length() == 1)
                sb.append("0");
            sb.append(stmp);
        }
        return sb.toString().toUpperCase(Locale.CHINA);
    }

    public static String getVersionCode(Context context) {
        PackageInfo manager = null;
        try {
            manager = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        return manager != null ? String.valueOf(manager.versionCode) : "0";
    }

    /**
     * 产生0~99999999的随机数,不足的用0补充
     *
     * @return 产生0~99999999的随机数,不足的用0补充
     */
    private static String getRandomString() {
        final int max = 100000000;
        int random = new Random().nextInt(max);
        String str = String.format(Locale.ENGLISH, "%8d", random);
        return str.replace(' ', '0');
    }

    public static boolean isServiceRunning(Context context, String service_Name) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }

        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service_Name.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public static String getChannel(Context context) {
        String channel;
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            channel = appInfo.metaData.get("UMENG_CHANNEL") + "";
            if (TextUtils.isEmpty(channel)) {
                channel = context.getResources().getString(R.string.channel);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            channel = context.getResources().getString(R.string.channel);
        }
        return channel;
    }

    //获取网址host
    public static String getHost(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        try {
            URL uri = new URL(url);
            return uri.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String getLanguageByFavorite(Context context, DAppFavorite favorite) {
        String language = ToolUtils.isZh(context) ? "zh-Hans" : ToolUtils.isKo(context) ? "ko" : "en";
        String msg = "";
        if ("ko".equals(language)) {
            msg = favorite.getDescriptionKo();
        } else if ("zh-Hans".equals(language)) {
            msg = favorite.getDescriptionCn();
        } else {
            msg = favorite.getDescription();
        }

        if (TextUtils.isEmpty(msg)) {
            msg = favorite.getUrl();
        }
        return msg;
    }

    public static String getLanguageByRecent(Context context, QWRecentDApp dApp) {
        String language = ToolUtils.isZh(context) ? "zh-Hans" : ToolUtils.isKo(context) ? "ko" : "en";
        String msg = "";
        if ("ko".equals(language)) {
            msg = dApp.getDescriptionKo();
        } else if ("zh-Hans".equals(language)) {
            msg = dApp.getDescriptionCn();
        } else {
            msg = dApp.getDescription();
        }

        if (TextUtils.isEmpty(msg)) {
            msg = dApp.getUrl();
        }
        return msg;
    }

    public static String getCharIcon(Context context, String url) {
        String tempUrl = url;
        //去掉http头
        if (tempUrl.startsWith(Constant.HTTP)) {
            tempUrl = tempUrl.substring(Constant.HTTP.length());
        } else if (tempUrl.startsWith(Constant.HTTPS)) {
            tempUrl = tempUrl.substring(Constant.HTTPS.length());
        }
        //去掉www
        if (tempUrl.startsWith("www.")) {
            tempUrl = tempUrl.substring(4);
        }
        if (tempUrl.length() >= 1) {
            char c = tempUrl.charAt(0);
            if (Character.isDigit(c)) {
                int id = context.getResources().getIdentifier("a", "drawable", context.getPackageName());
                return WalletIconUtils.getResourcesUri(context, id);
            } else {
                int id = context.getResources().getIdentifier(String.valueOf(c), "drawable", context.getPackageName());
                return WalletIconUtils.getResourcesUri(context, id);
            }
        } else {
            int id = context.getResources().getIdentifier("a", "drawable", context.getPackageName());
            return WalletIconUtils.getResourcesUri(context, id);
        }
    }
}
