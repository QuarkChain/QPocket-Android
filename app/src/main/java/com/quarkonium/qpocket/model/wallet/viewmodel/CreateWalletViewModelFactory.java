package com.quarkonium.qpocket.model.wallet.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.api.interact.CreateWalletInteract;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;

import javax.inject.Inject;

public class CreateWalletViewModelFactory implements ViewModelProvider.Factory {

    private final CreateWalletInteract mCreateWalletInteract;
    private final SetDefaultWalletInteract mSetDefaultWalletInteract;
    private final FindDefaultWalletInteract mFindDefaultWalletInteract;
    private MainApplication mApplication;

    @Inject
    public CreateWalletViewModelFactory(MainApplication application,
                                        CreateWalletInteract createWalletInteract,
                                        SetDefaultWalletInteract setDefaultWalletInteract,
                                        FindDefaultWalletInteract findDefaultWalletInteract) {
        mApplication = application;
        this.mCreateWalletInteract = createWalletInteract;
        mSetDefaultWalletInteract = setDefaultWalletInteract;
        mFindDefaultWalletInteract = findDefaultWalletInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new CreateWalletViewModel(mApplication, mCreateWalletInteract, mSetDefaultWalletInteract, mFindDefaultWalletInteract);
    }
}
