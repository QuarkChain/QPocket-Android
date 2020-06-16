package com.quarkonium.qpocket.api.interact;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;

import io.reactivex.Flowable;
import io.reactivex.Single;

public class FindDefaultWalletInteract {

    private final WalletRepositoryType walletRepository;
    private final PasswordStore mPasswordStore;

    public FindDefaultWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        this.walletRepository = walletRepository;
        mPasswordStore = passwordStore;
    }

    public Single<QWWallet> find(Context context) {
        return walletRepository
                .getDefaultWallet(context)
                .onErrorResumeNext(walletRepository
                        .fetchWallets(context)
                        .to(single -> Flowable.fromArray(single.blockingGet()))
                        .firstOrError()
                        .flatMapCompletable(walletRepository::setDefaultWallet)
                        .andThen(walletRepository.getDefaultWallet(context)));
    }

    public Single<String> findPhraseByKey(String key) {
        return mPasswordStore.getMnemonic(new QWWallet(key));
    }

}
