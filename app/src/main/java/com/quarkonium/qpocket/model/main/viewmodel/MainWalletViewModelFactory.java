package com.quarkonium.qpocket.model.main.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.api.interact.AppInstallInteract;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.MainApplication;

import javax.inject.Inject;

public class MainWalletViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultWalletInteract mFindDefaultWalletInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;
    private final AppInstallInteract mAppInstallInteract;

    private MainApplication mApplication;

    @Inject
    public MainWalletViewModelFactory(MainApplication application,
                                      FindDefaultWalletInteract findDefaultWalletInteract,
                                      WalletTransactionInteract transactionInteract,
                                      AppInstallInteract installInteract) {
        mApplication = application;
        mFindDefaultWalletInteract = findDefaultWalletInteract;
        mWalletTransactionInteract = transactionInteract;
        mAppInstallInteract = installInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new MainWallerViewModel(mApplication, mFindDefaultWalletInteract, mWalletTransactionInteract, mAppInstallInteract);
    }
}
