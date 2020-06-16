package com.quarkonium.qpocket.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;

import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.BuildConfig;
import com.quarkonium.qpocket.R;
import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;

/**
 * 分享滤镜、照片的调用类
 */
public class ShareManager {

    public static final int SHARE_WECHAT = 102;
    public static final int SHARE_WECHAT_FRIEND = 103;

    //public static final String WECHAT_APP_ID = "wx32eb29e8a7127fd2";
    public static final String WECHAT_APP_ID = "wxbe7f760944888fb6";

    //public static final String QQ_APP_KEY = "1105439340";//"1104541673";
    private static final String QQ_APP_KEY = "1103397731";

    //private static final String WEIBO_APP_KEY = "2960519055";//"3054314304";
    public static final String WEIBO_APP_KEY = "68456218";

    public static final int SHARE_PIC_TO_FACEBOOK = 100;
    public static final int LOCK_FILTER_MAX = 2;

    private static final String FACEBOOK_APP_PK_NAME = "com.facebook.katana";
    private static final String WECHAT_APP_PK_NAME = "com.tencent.mm";
    private static final String WECHAT_SHARE_PIC_LOCK = "wx_share_pic_lock";
    private static final String FACEBOOK_SHARE_PIC_LOCK = "facebook_share_pic_lock";

    private static ShareManager mInstance;

    private ShareManager() {
    }

    public static ShareManager instance() {
        if (mInstance == null) {
            mInstance = new ShareManager();
        }
        return mInstance;
    }

    /**
     * 照片微信分享
     */
    private boolean startSharePictureWechat(Activity activity, String path, int scene, String type) {
        IWXAPI mWechatAPI = WXAPIFactory.createWXAPI(activity, WECHAT_APP_ID, true);
        mWechatAPI.registerApp(WECHAT_APP_ID);
        boolean installed = mWechatAPI.isWXAppInstalled();
        if (!installed) {
            MyToast.showSingleToastShort(activity.getApplicationContext(), R.string.share_not_install_wechat);
            return false;
        }

        boolean isAppSupportAPI = mWechatAPI.getWXAppSupportAPI() >= Build.TIMELINE_SUPPORTED_SDK_INT;
        if (!isAppSupportAPI) {
            MyToast.showSingleToastShort(activity.getApplicationContext(), R.string.share_wechat_unsupport);
            return false;
        }


        WXMediaMessage msg = new WXMediaMessage();
        //分享图片
        WXImageObject imgObj = new WXImageObject();
        imgObj.imagePath = path;
        msg.mediaObject = imgObj;
        //缩略图
        Bitmap thumbNail = BitmapUtils.scalePicture(path, 320, true);
        if (thumbNail != null) {
            msg.thumbData = BitmapUtils.bitmap2Bytes(thumbNail);
            thumbNail.recycle();
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction(type);
        req.message = msg;
        req.scene = scene;

        return mWechatAPI.sendReq(req);
    }

    private static String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis())
                : type + System.currentTimeMillis();
    }

    /**
     * 分享图片
     */
    public static void sharePhotoItem(Activity activity, String path, int id, String type) {
        switch (id) {
            case SHARE_WECHAT:
                ShareManager.instance().startSharePictureWechat(activity,
                        path, SendMessageToWX.Req.WXSceneSession, type);
                break;
            case SHARE_WECHAT_FRIEND:
                ShareManager.instance().startSharePictureWechat(activity,
                        path, SendMessageToWX.Req.WXSceneTimeline, type);
                break;
        }
    }


    /**
     * 照片国外分享调用
     */
    public boolean startSharePicture(Activity mActivity, String destSitePackageName, String path) {
        File file = new File(path);
        Uri u;
        if (ApiHelper.AFTER_NOUGAT) {
            u = FileProvider.getUriForFile(mActivity,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file);
        } else {
            u = Uri.fromFile(file);
        }
        return startSharePicture(mActivity, destSitePackageName, u);
    }

    public boolean startSharePicture(Activity mActivity, String destSitePackageName, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setPackage(destSitePackageName);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            mActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            String packageGooglePlay = "com.android.vending";
            boolean installed = SystemUtils.checkApkExist(mActivity, packageGooglePlay);

            if (installed) {
                intent = mActivity.getPackageManager().getLaunchIntentForPackage(packageGooglePlay);
                intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + destSitePackageName));
                intent.setAction(Intent.ACTION_VIEW);
                mActivity.startActivity(intent);
            } else {
                return false;

            }
        }
        return true;
    }

    //facebook
    public boolean startSharePictureToFacebook(Activity mActivity, String destSitePackageName, String path) {
        boolean state = startSharePicture(mActivity, destSitePackageName, path);
        if (!state) {
            MyToast.showSingleToastShort(mActivity, R.string.share_facebook_uninstalled);
        }
        return state;
    }

    public boolean startSharePictureToFacebook(Activity mActivity, String destSitePackageName, Uri uri) {
        boolean state = startSharePicture(mActivity, destSitePackageName, uri);
        if (!state) {
            MyToast.showSingleToastShort(mActivity, R.string.share_facebook_uninstalled);
        }
        return state;
    }

    //twitter
    public boolean startSharePictureToTwitter(Activity mActivity, String destSitePackageName, String path) {
        boolean state = startSharePicture(mActivity, destSitePackageName, path);
        if (!state) {
            MyToast.showSingleToastShort(mActivity, R.string.share_twitter_uninstalled);
        }
        return state;
    }

    public boolean startSharePictureToTwitter(Activity mActivity, String destSitePackageName, Uri uri) {
        boolean state = startSharePicture(mActivity, destSitePackageName, uri);
        if (!state) {
            MyToast.showSingleToastShort(mActivity, R.string.share_twitter_uninstalled);
        }
        return state;
    }

    //telegram
    public boolean startSharePictureToTelegram(Activity mActivity, String destSitePackageName, String path) {
        boolean state = startSharePicture(mActivity, destSitePackageName, path);
        if (!state) {
            MyToast.showSingleToastShort(mActivity, R.string.share_telegram_uninstalled);
        }
        return state;
    }

    public boolean startSharePictureToTelegram(Activity mActivity, String destSitePackageName, Uri uri) {
        boolean state = startSharePicture(mActivity, destSitePackageName, uri);
        if (!state) {
            MyToast.showSingleToastShort(mActivity, R.string.share_telegram_uninstalled);
        }
        return state;
    }
}
