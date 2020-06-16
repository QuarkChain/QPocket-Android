package com.quarkonium.qpocket.model.transaction.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;

import javax.inject.Inject;

public class PublicSaleDetailViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultWalletInteract mFindDefaultWalletInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;

    private MainApplication mApplication;

    @Inject
    public PublicSaleDetailViewModelFactory(MainApplication application,
                                            FindDefaultWalletInteract findDefaultWalletInteract,
                                            WalletTransactionInteract transactionInteract) {
        mApplication = application;
        mFindDefaultWalletInteract = findDefaultWalletInteract;
        mWalletTransactionInteract = transactionInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new PublicSaleDetailViewModel(mApplication, mFindDefaultWalletInteract, mWalletTransactionInteract);
    }
}
