package com.quarkonium.qpocket.model.wallet.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.api.interact.ImportWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;

import javax.inject.Inject;

public class ImportWalletViewModelFactory implements ViewModelProvider.Factory {

    private final ImportWalletInteract mImportWalletInteract;
    private final SetDefaultWalletInteract mSetDefaultWalletInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;
    private MainApplication mApplication;

    @Inject
    public ImportWalletViewModelFactory(MainApplication application, ImportWalletInteract importWalletInteract,
                                        SetDefaultWalletInteract setDefaultWalletInteract,
                                        WalletTransactionInteract walletTransactionInteract) {
        mApplication = application;
        this.mImportWalletInteract = importWalletInteract;
        this.mSetDefaultWalletInteract = setDefaultWalletInteract;
        this.mWalletTransactionInteract = walletTransactionInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportWalletViewModel(mApplication, mImportWalletInteract, mSetDefaultWalletInteract, mWalletTransactionInteract);
    }
}
