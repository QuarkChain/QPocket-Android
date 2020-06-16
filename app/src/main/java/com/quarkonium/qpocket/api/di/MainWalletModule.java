package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.api.interact.AppInstallInteract;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.api.repository.ETHTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.TokenRepositoryType;
import com.quarkonium.qpocket.api.repository.TransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TrxTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.model.main.viewmodel.MainWalletViewModelFactory;
import com.quarkonium.qpocket.MainApplication;

import dagger.Module;
import dagger.Provides;

@Module
public class MainWalletModule {

    @Provides
    MainWalletViewModelFactory provideMainWalletFragmentViewModelFactory(MainApplication application,
                                                                         FindDefaultWalletInteract fetchWalletsInteract,
                                                                         WalletTransactionInteract transactionInteract,
                                                                         AppInstallInteract appInstallInteract) {
        return new MainWalletViewModelFactory(application, fetchWalletsInteract, transactionInteract, appInstallInteract);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType accountRepository, PasswordStore store) {
        return new FindDefaultWalletInteract(accountRepository, store);
    }

    @Provides
    WalletTransactionInteract provideWalletTransactionInteract(TransactionRepositoryType transactionRepositoryType,
                                                               TokenRepositoryType tokenRepositoryType,
                                                               ETHTransactionRepositoryType ethTransactionRepositoryType,
                                                               TrxTransactionRepositoryType tronTransactionRepositoryType) {
        return new WalletTransactionInteract(transactionRepositoryType, tokenRepositoryType, ethTransactionRepositoryType,
                tronTransactionRepositoryType);
    }

    @Provides
    AppInstallInteract provideAppInstallInteract(WalletRepositoryType accountRepository, PasswordStore store) {
        return new AppInstallInteract(accountRepository, store);
    }
}
