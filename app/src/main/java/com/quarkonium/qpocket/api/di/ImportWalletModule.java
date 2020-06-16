package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.api.interact.ImportWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.api.repository.ETHTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.TokenRepositoryType;
import com.quarkonium.qpocket.api.repository.TransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TrxTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.model.wallet.viewmodel.ImportWalletViewModelFactory;
import com.quarkonium.qpocket.MainApplication;

import dagger.Module;
import dagger.Provides;

@Module
public class ImportWalletModule {

    @Provides
    ImportWalletViewModelFactory provideImportWalletViewModelFactory(MainApplication application,
                                                                     ImportWalletInteract importWalletInteract,
                                                                     SetDefaultWalletInteract setDefaultWalletInteract,
                                                                     WalletTransactionInteract walletTransactionInteract) {
        return new ImportWalletViewModelFactory(application, importWalletInteract, setDefaultWalletInteract, walletTransactionInteract);
    }

    @Provides
    ImportWalletInteract provideImportWalletInteract(WalletRepositoryType accountRepository, PasswordStore passwordStore) {
        return new ImportWalletInteract(accountRepository, passwordStore);
    }

    @Provides
    SetDefaultWalletInteract provideSetDefaultWalletInteract(WalletRepositoryType accountRepository) {
        return new SetDefaultWalletInteract(accountRepository);
    }


    @Provides
    WalletTransactionInteract provideWalletTransactionInteract(TransactionRepositoryType transactionRepositoryType,
                                                               TokenRepositoryType tokenRepositoryType,
                                                               ETHTransactionRepositoryType ethTransactionRepositoryType,
                                                               TrxTransactionRepositoryType tronTransactionRepositoryType) {
        return new WalletTransactionInteract(transactionRepositoryType, tokenRepositoryType, ethTransactionRepositoryType,
                tronTransactionRepositoryType);
    }
}
