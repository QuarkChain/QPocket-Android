package com.quarkonium.qpocket.api.interact;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;

import io.reactivex.Single;

//获取所有钱包
public class FetchWalletsInteract {

    private final WalletRepositoryType mAccountRepository;

    public FetchWalletsInteract(WalletRepositoryType accountRepository) {
        this.mAccountRepository = accountRepository;
    }

    public Single<QWWallet[]> fetch(Context context) {
        return mAccountRepository.fetchWallets(context);
    }

    public Single<QWWallet> fetchBadWallet(Context context) {
        return mAccountRepository.fetchBadWallet(context);
    }

    public Single<QWWallet[]> fetchManagerWallets(Context context) {
        return mAccountRepository.fetchManagerWallets(context);
    }

    public double getAccountBalance(Context context, QWAccount account) {
        return mAccountRepository.getAccountBalance(context, account);
    }
}
