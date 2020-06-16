package com.quarkonium.qpocket.api;

import android.content.Context;

import com.quarkonium.qpocket.util.Utils;


/**
 * Created by hlf on 14-11-17.
 * 此类用来保存一些公共使用的参数
 */
public class Constants {

    public static String sChannel;

    public static String sAppVersion;

    public static String sAppVersionForNet;

    public static synchronized String getAppVersion(Context ctx) {
        if (sAppVersion == null && ctx != null) {
            sAppVersion = Utils.getVersionCode(ctx.getApplicationContext());
        }
        return sAppVersion;
    }

//    public static synchronized String getChannel(Context ctx) {
//        if (TextUtils.isEmpty(sChannel) && ctx != null) {
//            sChannel = Utils.getChannel(ctx);
//        }
//        return sChannel;
//    }

    public static int BATCH_TRY_COUNT = 3;

//    public static int BATCH_PHOTO_MAX_COUNT = 50;

    public static int GRAD_FILTER_TRY_COUNT = 3;

    public static long WATERMARK_TRY_TIME = 1000 * 60 * 60 * 24;
}
