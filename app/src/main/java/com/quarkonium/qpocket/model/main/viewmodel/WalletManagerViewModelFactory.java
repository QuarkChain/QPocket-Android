package com.quarkonium.qpocket.model.main.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.interact.FetchWalletsInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.MainApplication;

import javax.inject.Inject;

public class WalletManagerViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract mFetchWalletsInteract;
    private final SetDefaultWalletInteract mSetDefaultWalletInteract;
    private MainApplication mApplication;

    @Inject
    public WalletManagerViewModelFactory(MainApplication application,
                                         FetchWalletsInteract fetchWalletsInteract,
                                         SetDefaultWalletInteract setDefaultWalletInteract) {
        mApplication = application;
        mFetchWalletsInteract = fetchWalletsInteract;
        mSetDefaultWalletInteract = setDefaultWalletInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WallerManagerViewModel(mApplication, mFetchWalletsInteract, mSetDefaultWalletInteract);
    }
}
