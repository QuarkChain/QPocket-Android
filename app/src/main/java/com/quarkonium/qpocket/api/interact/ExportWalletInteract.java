package com.quarkonium.qpocket.api.interact;

import android.content.Context;

import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWBalanceDao;
import com.quarkonium.qpocket.api.db.dao.QWTransactionDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

//钱包管理
public class ExportWalletInteract {

    private final WalletRepositoryType walletRepository;
    private final PasswordStore passwordStore;

    public ExportWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        this.walletRepository = walletRepository;
        this.passwordStore = passwordStore;
    }

    public Single<String> exportKeystore(QWAccount account, String password, String newPassword) {
        return walletRepository.exportKeystoreWallet(account, password, newPassword);
    }

    //导出私钥
    public Single<String> exportPrivateKey(QWAccount account, String password, String newPassword) {
        return walletRepository.exportPrivateKeyWallet(account, password, newPassword);
    }

    //导出助记词
    public Single<String> exportPhrase(QWWallet wallet, String password, String newPassword) {
        QWAccount account = new QWAccount(wallet.getCurrentAddress());
        account.setType(wallet.getType());
        return walletRepository.exportKeystoreWallet(account, password, newPassword)
                .flatMap(keystore -> passwordStore.getMnemonic(wallet))
                .subscribeOn(Schedulers.io());
    }

    //删除account
    public Single<Boolean> deleteAccount(Context context, QWAccount account, String password) {
        return walletRepository.deleteAccount(context, account, password);
    }

    //删除观察钱包
    public Single<Boolean> deleteWatchWalletData(Context context, QWWallet wallet) {
        return Single.fromCallable(() -> {
            QWAccountDao accountDao = new QWAccountDao(context);
            List<QWAccount> list = accountDao.queryByKey(wallet.getKey());
            if (list != null && !list.isEmpty()) {
                QWBalanceDao balanceDao = new QWBalanceDao(context);
                QWTransactionDao transactionDao = new QWTransactionDao(context);
                for (QWAccount account : list) {
                    balanceDao.delete(account);
                    transactionDao.delete(account);
                }
            }
            accountDao.deleteByKey(wallet.getKey());

            QWWalletDao dao = new QWWalletDao(context);
            dao.delete(wallet.getKey());
            return true;
        });
    }

    //删除钱包
    public Single<Boolean> deleteData(Context context, QWWallet wallet, String password) {
        //正常钱包
        return walletRepository.deleteWallet(context, wallet.getKey(), password)
                .andThen(deleteWatchWalletData(context, wallet));
    }
}
