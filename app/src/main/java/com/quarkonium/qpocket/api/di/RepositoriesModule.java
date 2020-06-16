package com.quarkonium.qpocket.api.di;

import android.content.Context;

import com.quarkonium.qpocket.api.repository.ETHTransactionRepository;
import com.quarkonium.qpocket.api.repository.ETHTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.PreferenceRepositoryType;
import com.quarkonium.qpocket.api.repository.SharedPreferenceRepository;
import com.quarkonium.qpocket.api.repository.TokenRepository;
import com.quarkonium.qpocket.api.repository.TokenRepositoryType;
import com.quarkonium.qpocket.api.repository.TransactionRepository;
import com.quarkonium.qpocket.api.repository.TransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TrxTransactionRepository;
import com.quarkonium.qpocket.api.repository.TrxTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.WalletRepository;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.api.service.AccountKeystoreService;
import com.quarkonium.qpocket.api.service.GethKeystoreAccountService;
import com.quarkonium.qpocket.tron.TronKeystoreAccountService;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoriesModule {
    @Singleton
    @Provides
    PreferenceRepositoryType providePreferenceRepository(Context context) {
        return new SharedPreferenceRepository(context);
    }

    @Singleton
    @Provides
    AccountKeystoreService provideAccountKeyStoreService(Context context) {
        File file = new File(context.getFilesDir(), "keystore/keystore");
        return new GethKeystoreAccountService(file);
    }

    @Singleton
    @Provides
    TronKeystoreAccountService provideTronKeystoreAccountService(Context context) {
        return new TronKeystoreAccountService(context);
    }

    @Singleton
    @Provides
    WalletRepositoryType provideWalletRepository(
            PreferenceRepositoryType preferenceRepositoryType,
            AccountKeystoreService accountKeystoreService,
            TronKeystoreAccountService tronKeystoreAccountService) {
        return new WalletRepository(preferenceRepositoryType, accountKeystoreService, tronKeystoreAccountService);
    }

    @Singleton
    @Provides
    TransactionRepositoryType provideTransactionRepository(AccountKeystoreService accountKeystoreService) {
        return new TransactionRepository(accountKeystoreService);
    }

    @Singleton
    @Provides
    TokenRepositoryType provideTokenRepositoryType() {
        return new TokenRepository();
    }

    @Singleton
    @Provides
    ETHTransactionRepositoryType provideETHTransactionRepositoryType() {
        return new ETHTransactionRepository();
    }

    @Singleton
    @Provides
    TrxTransactionRepositoryType provideTronTransactionRepositoryType() {
        return new TrxTransactionRepository();
    }
}
