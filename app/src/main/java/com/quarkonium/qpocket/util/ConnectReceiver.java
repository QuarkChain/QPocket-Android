package com.quarkonium.qpocket.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.quarkonium.qpocket.rx.ConnectedChangeEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * 网络监听
 */
public class ConnectReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        //联网状态
        NetworkInfo info = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
        boolean isConnecting = info != null && info.isConnectedOrConnecting();
        ConnectedChangeEvent messageEvent = new ConnectedChangeEvent(isConnecting);
        EventBus.getDefault().post(messageEvent);
    }
}
