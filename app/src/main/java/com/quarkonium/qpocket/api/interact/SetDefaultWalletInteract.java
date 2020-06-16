package com.quarkonium.qpocket.api.interact;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

//设置主钱包
public class SetDefaultWalletInteract {

    private WalletRepositoryType accountRepository;

    public SetDefaultWalletInteract(WalletRepositoryType walletRepositoryType) {
        this.accountRepository = walletRepositoryType;
    }

    public Completable set(QWWallet wallet) {
        return accountRepository
                .setDefaultWallet(wallet)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<QWWallet> setDefaultWallet(Context context, String walletKey, String address) {
        return accountRepository.setDefaultWallet(context, walletKey, address);
    }

    public Single<QWWallet> updateWalletCurrentAccount(Context context, String walletKey) {
        return accountRepository.updateWalletCurrentAccount(context, walletKey);
    }
}
