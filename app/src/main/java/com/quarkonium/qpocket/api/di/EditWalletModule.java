package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.api.interact.ExportWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.model.main.viewmodel.EditWalletViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class EditWalletModule {

    @Provides
    EditWalletViewModelFactory provideEditWalletViewModelFactory(ExportWalletInteract exportWalletInteract, SetDefaultWalletInteract setDefaultWalletInteract) {
        return new EditWalletViewModelFactory(exportWalletInteract, setDefaultWalletInteract);
    }

    @Provides
    ExportWalletInteract provideExportWalletInteract(WalletRepositoryType accountRepository, PasswordStore passwordStore) {
        return new ExportWalletInteract(accountRepository, passwordStore);
    }

    @Provides
    SetDefaultWalletInteract provideSetDefaultWalletInteract(WalletRepositoryType accountRepository) {
        return new SetDefaultWalletInteract(accountRepository);
    }
}
