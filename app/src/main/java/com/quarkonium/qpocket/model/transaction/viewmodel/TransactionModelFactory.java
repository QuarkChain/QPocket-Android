package com.quarkonium.qpocket.model.transaction.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.MainApplication;

import javax.inject.Inject;

public class TransactionModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultWalletInteract mFindDefaultWalletInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;

    private MainApplication mApplication;

    @Inject
    public TransactionModelFactory(MainApplication application,
                                   FindDefaultWalletInteract findDefaultWalletInteract,
                                   WalletTransactionInteract transactionInteract ) {
        mApplication = application;
        mFindDefaultWalletInteract = findDefaultWalletInteract;
        mWalletTransactionInteract = transactionInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransactionViewModel(mApplication, mFindDefaultWalletInteract, mWalletTransactionInteract );
    }
}
