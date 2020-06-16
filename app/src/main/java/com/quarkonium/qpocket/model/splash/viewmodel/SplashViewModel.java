package com.quarkonium.qpocket.model.splash.viewmodel;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.interact.FetchWalletsInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SplashViewModel extends BaseAndroidViewModel {

    private final MutableLiveData<QWWallet[]> mWallets = new MutableLiveData<>();

    SplashViewModel(Application application, FetchWalletsInteract fetchWalletsInteract, WalletTransactionInteract walletTransactionInteract) {
        super(application);

        Disposable disposable = fetchWalletsInteract
                .fetch(application)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mWallets::postValue, this::onError);

        //获取分片数量
        Disposable disposableCount = walletTransactionInteract
                .getNetworkInfo(getApplication())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> onGetNetworkInfoSuccess(), v -> {
                });
    }

    public void onError(Throwable throwable) {
        mWallets.postValue(null);
    }

    public LiveData<QWWallet[]> wallets() {
        return mWallets;
    }

    private void onGetNetworkInfoSuccess() {

    }
}
