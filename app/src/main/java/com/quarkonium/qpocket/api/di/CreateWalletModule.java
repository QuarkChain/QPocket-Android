package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.api.interact.CreateWalletInteract;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.model.wallet.viewmodel.CreateWalletViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class CreateWalletModule {

    @Provides
    CreateWalletViewModelFactory provideCreateWalletViewModelFactory(MainApplication application,
                                                                     CreateWalletInteract createWalletInteract,
                                                                     SetDefaultWalletInteract setDefaultWalletInteract,
                                                                     FindDefaultWalletInteract findDefaultWalletInteract) {
        return new CreateWalletViewModelFactory(application, createWalletInteract, setDefaultWalletInteract, findDefaultWalletInteract);
    }

    @Provides
    CreateWalletInteract provideCreateAccountInteract(WalletRepositoryType accountRepository, PasswordStore passwordStore) {
        return new CreateWalletInteract(accountRepository, passwordStore);
    }

    @Provides
    SetDefaultWalletInteract provideSetDefaultWalletInteract(WalletRepositoryType accountRepository) {
        return new SetDefaultWalletInteract(accountRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType accountRepository, PasswordStore passwordStore) {
        return new FindDefaultWalletInteract(accountRepository, passwordStore);
    }
}
