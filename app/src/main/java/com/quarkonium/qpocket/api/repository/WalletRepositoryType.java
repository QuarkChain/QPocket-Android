package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;

import io.reactivex.Completable;
import io.reactivex.Single;


public interface WalletRepositoryType {
    Single<QWWallet[]> fetchWallets(Context context);

    Single<QWWallet> fetchBadWallet(Context context);

    Single<QWWallet[]> fetchManagerWallets(Context context);

    Single<QWWallet> findWallet(Context context, String address);

    Single<QWWallet> createWallet(Context context, String mnemonic, String password);

    Single<QWWallet> importPhraseToWallet(Context context, boolean segWit, String phrase, String newPassword, int type);

    Single<QWWallet> importPrivateKeyToWallet(Context context, boolean segWit, String privateKey, String password, int type);

    Single<QWWallet> importKeystoreToWallet(Context context, boolean segWit, String store, String password, int type);

    Single<QWAccount> createTrxWallet(String mnemonic, String password);

    Single<String> exportKeystoreWallet(QWAccount wallet, String password, String newPassword);

    Single<String> exportPrivateKeyWallet(QWAccount wallet, String password, String newPassword);

    Completable deleteWallet(Context context, String key, String password);

    Single<Boolean> deleteAccount(Context context, QWAccount account, String password);

    Completable setDefaultWallet(QWWallet wallet);

    Single<QWWallet> setDefaultWallet(Context context, String walletKey, String address);

    Single<QWWallet> updateWalletCurrentAccount(Context context, String walletKey);

    Single<QWWallet> getDefaultWallet(Context context);

    Single<QWAccount> createETHChildAccount(String password, String mnemonic, int accountIndex);

    Single<QWAccount> createTRXChildAccount(String password, String mnemonic, int accountIndex);

    Single<QWAccount> createQKCChildAccount(Context context, String password, String mnemonic, int accountIndex);

    double getAccountBalance(Context context, QWAccount account);
}
