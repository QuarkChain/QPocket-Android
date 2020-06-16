package com.quarkonium.qpocket.model.main.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.api.interact.ExportWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;

public class EditWalletViewModelFactory implements ViewModelProvider.Factory {

    private final ExportWalletInteract mExportWalletInteract;
    private final SetDefaultWalletInteract mSetDefaultWalletInteract;

    public EditWalletViewModelFactory(ExportWalletInteract exportWalletInteract, SetDefaultWalletInteract setDefaultWalletInteract) {
        this.mExportWalletInteract = exportWalletInteract;
        this.mSetDefaultWalletInteract = setDefaultWalletInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new EditWalletViewModel(mExportWalletInteract, mSetDefaultWalletInteract);
    }

}
