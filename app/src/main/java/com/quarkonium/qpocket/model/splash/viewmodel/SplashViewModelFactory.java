package com.quarkonium.qpocket.model.splash.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.api.interact.FetchWalletsInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;

public class SplashViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract mFetchWalletsInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;
    private MainApplication mApplication;

    public SplashViewModelFactory(MainApplication application, FetchWalletsInteract fetchWalletsInteract, WalletTransactionInteract walletTransactionInteract) {
        mApplication = application;
        this.mFetchWalletsInteract = fetchWalletsInteract;
        mWalletTransactionInteract = walletTransactionInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SplashViewModel(mApplication, mFetchWalletsInteract, mWalletTransactionInteract);
    }

}
