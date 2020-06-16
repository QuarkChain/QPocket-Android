package com.quarkonium.qpocket.api.interact;

import android.content.Context;

import com.quarkonium.qpocket.api.Constants;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

//应用升级
public class AppInstallInteract {

    public AppInstallInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
    }

    //安装apk需要的数据
    public Observable<Integer> install(Context context) {
        return Observable.create(e -> {
            int appVersion = Integer.parseInt(Constants.getAppVersion(context));
            //是否为全新安装
            boolean isNew = SharedPreferencesUtils.isNewApp(context);
            if (isNew) {
                //全新安装
                SharedPreferencesUtils.setNewApp(context, false);
                //更新版本号
                SharedPreferencesUtils.setAppVersion(context, appVersion);

                //********************
                //安装数据
                //********************
                installData(e, context);
                e.onComplete();
                return;
            }


            //升级
            //201版本开始添加升级
            int formalVersion = SharedPreferencesUtils.getAppVersion(context);
            if (formalVersion < appVersion) {
                //更新版本号
                SharedPreferencesUtils.setAppVersion(context, appVersion);

                //********************
                //安装数据
                //********************
                installData(e, context);
            }
            e.onComplete();
        });
    }

    ///***************
    //***************安装数据*****************
    ///***************
    private void installData(ObservableEmitter<Integer> e, Context context) {
        e.onNext(100);
    }
}
