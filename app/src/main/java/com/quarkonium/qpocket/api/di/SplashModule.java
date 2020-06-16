package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.api.interact.FetchWalletsInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.api.repository.ETHTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TokenRepositoryType;
import com.quarkonium.qpocket.api.repository.TransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TrxTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.model.splash.viewmodel.SplashViewModelFactory;
import com.quarkonium.qpocket.MainApplication;

import dagger.Module;
import dagger.Provides;

@Module
public class SplashModule {

    @Provides
    SplashViewModelFactory provideHomeViewModelFactory(MainApplication application, FetchWalletsInteract fetchWalletsInteract, WalletTransactionInteract walletTransactionInteract) {
        return new SplashViewModelFactory(application, fetchWalletsInteract, walletTransactionInteract);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
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
