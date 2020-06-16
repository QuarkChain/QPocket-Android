package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.api.interact.FetchWalletsInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.model.main.viewmodel.WalletManagerViewModelFactory;
import com.quarkonium.qpocket.MainApplication;

import dagger.Module;
import dagger.Provides;

@Module
public class ManagerWalletModule {

    @Provides
    WalletManagerViewModelFactory provideWalletManagerViewModelFactory(MainApplication application,
                                                                       FetchWalletsInteract fetchWalletsInteract,
                                                                       SetDefaultWalletInteract setDefaultWalletInteract) {
        return new WalletManagerViewModelFactory(application, fetchWalletsInteract, setDefaultWalletInteract);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletsInteract(WalletRepositoryType accountRepository) {
        return new FetchWalletsInteract(accountRepository);
    }

    @Provides
    SetDefaultWalletInteract provideSetDefaultWalletInteract(WalletRepositoryType accountRepository) {
        return new SetDefaultWalletInteract(accountRepository);
    }
}
