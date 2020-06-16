package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.api.repository.ETHTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.TokenRepositoryType;
import com.quarkonium.qpocket.api.repository.TransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TrxTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.MainApplication;

import dagger.Module;
import dagger.Provides;

@Module
public class TransactionModule {

    @Provides
    TransactionModelFactory provideTransactionModelFactory(MainApplication application,
                                                           FindDefaultWalletInteract fetchWalletsInteract,
                                                           WalletTransactionInteract transactionInteract) {
        return new TransactionModelFactory(application, fetchWalletsInteract, transactionInteract);
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
}
