package com.quarkonium.qpocket.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.quarkonium.qpocket.rx.FlashShareEvent;
import com.quarkonium.qpocket.util.ShareManager;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.greenrobot.eventbus.EventBus;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    private IWXAPI api;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 通过WXAPIFactory工厂，获取IWXAPI的实例
        api = WXAPIFactory.createWXAPI(this, ShareManager.WECHAT_APP_ID, false);
        //注意：
        //第三方开发者如果使用透明界面来实现WXEntryActivity，需要判断handleIntent的返回值，如果返回值为false，则说明入参不合法未被SDK处理，应finish当前透明界面，避免外部通过传递非法参数的Intent导致停留在透明界面，引起用户的疑惑
        try {
            api.handleIntent(getIntent(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {
        finish();
    }

    @Override
    public void onResp(BaseResp baseResp) {
        int result;
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                result = R.string.share_wechat_success;
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = R.string.share_wechat_cancel;
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                result = R.string.share_wechat_unsupport;
                break;
            default:
                result = R.string.share_wechat_fail;
                break;
        }
        MyToast.showSingleToastShort(getApplicationContext(), result);
        finish();

        //分享快讯成功
        if (baseResp.errCode == BaseResp.ErrCode.ERR_OK && !TextUtils.isEmpty(baseResp.transaction) && baseResp.transaction.startsWith("flash")) {
            FlashShareEvent messageEvent = new FlashShareEvent("");
            EventBus.getDefault().postSticky(messageEvent);
        }
    }
}