package com.quarkonium.qpocket.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.DrawableRes;

import com.quarkonium.qpocket.api.Constant;

import java.util.Random;

public class WalletIconUtils {

    //获取随机头像路径
    public static String randomIconPath(Context context) {
        Random random = new Random();
        int randomIndex = random.nextInt(Constant.WALLET_ICON_IDS.length);
        randomIndex = randomIndex >= 0 && randomIndex < Constant.WALLET_ICON_IDS.length ? randomIndex : 0;
        int id = Constant.WALLET_ICON_IDS[randomIndex];
        return getResourcesUri(context, id);
    }


    //res图片ID转为绝对路径
    public static String getResourcesUri(Context context, @DrawableRes int id) {
        Resources resources = context.getResources();
        return ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                resources.getResourcePackageName(id) + "/" +
                resources.getResourceTypeName(id) + "/" +
                resources.getResourceEntryName(id);
    }
}
