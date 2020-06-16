package com.quarkonium.qpocket.api.interact.operator;


import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;

import io.reactivex.CompletableOperator;
import io.reactivex.SingleTransformer;

public class Operators {

    //存储钱包助记词
    public static SingleTransformer<QWWallet, QWWallet> saveMnemonic(
            PasswordStore passwordStore, WalletRepositoryType walletRepository, String mnemonic, String password) {
        return new SavePasswordOperator(passwordStore, walletRepository, mnemonic, password);
    }

    public static CompletableOperator completableErrorProxy(Throwable throwable) {
        return new CompletableErrorProxyOperator(throwable);
    }
}
