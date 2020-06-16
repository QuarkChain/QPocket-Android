package com.quarkonium.qpocket.api.interact.operator;

import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.observers.DisposableCompletableObserver;

public class SavePasswordOperator implements SingleTransformer<QWWallet, QWWallet> {

    private final PasswordStore passwordStore;
    private final String mnemonic;
    private final WalletRepositoryType walletRepository;
    private String password;

    SavePasswordOperator(
            PasswordStore passwordStore, WalletRepositoryType walletRepository, String mnemonic, String password) {
        this.passwordStore = passwordStore;
        this.mnemonic = mnemonic;
        this.walletRepository = walletRepository;
        this.password = password;
    }

    @Override
    public SingleSource<QWWallet> apply(Single<QWWallet> upstream) {
        return upstream.flatMap(wallet ->
                passwordStore
                        .setMnemonic(wallet, mnemonic, password)
                        .onErrorResumeNext(err -> walletRepository.deleteWallet(MainApplication.getContext(), wallet.getKey(), password)
                                .lift(observer -> new DisposableCompletableObserver() {
                                    @Override
                                    public void onComplete() {
                                        observer.onError(err);
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        observer.onError(e);
                                    }
                                }))
                        .toSingle(() -> wallet)
        );
    }
}
