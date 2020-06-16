package com.quarkonium.qpocket.api.interact;

import android.content.Context;

import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.db.table.QWWalletToken;
import com.quarkonium.qpocket.api.interact.operator.Operators;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.WalletRepositoryType;
import com.quarkonium.qpocket.util.WalletIconUtils;

import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

//创建钱包
public class CreateWalletInteract {

    private final WalletRepositoryType walletRepository;
    private final PasswordStore passwordStore;

    public CreateWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        this.walletRepository = walletRepository;
        this.passwordStore = passwordStore;
    }

    //获取助记词
    public Single<String> generateMnemonic() {
        return passwordStore.generateMnemonic();
    }

    //替换助记词
    public Single<String> chooseMnemonic(Context context, String mnemonic, Locale locale) {
        return passwordStore.changeMnemonic(context, mnemonic, locale);
    }

    //创建钱包
    public Single<QWWallet> create(Context context, String mnemonic, String password, String passwordHint, int isBackup) {
        return walletRepository
                .createWallet(context, mnemonic, password)
                .compose(Operators.saveMnemonic(passwordStore, walletRepository, mnemonic, password))//存储助记词
                .flatMap((wallet) -> insertDB(context.getApplicationContext(), wallet, passwordHint, isBackup))//插入数据库
                .flatMap(wallet -> passwordVerification(context, wallet, password)); //校验
    }

    //进行校验
    private Single<QWWallet> passwordVerification(Context context, QWWallet wallet, String masterPassword) {
        return passwordStore
                .getMnemonic(wallet)//助记词校验
                .flatMap(phrase -> walletRepository.findWallet(context, wallet.getKey()))//钱包校验
                .onErrorResumeNext(throwable -> walletRepository
                        .deleteWallet(context, wallet.getKey(), masterPassword)
                        .lift(Operators.completableErrorProxy(throwable))
                        .toSingle(() -> wallet));
    }

    //插入数据库
    private Single<QWWallet> insertDB(Context context, QWWallet wallet, String passwordHint, int isBackUp) {
        return Single.fromCallable(() -> {

            QWWalletDao dao = new QWWalletDao(context);
            String name = dao.getName(wallet.getType());
            String icon = WalletIconUtils.randomIconPath(context);
            wallet.setName(name);
            wallet.setIcon(icon);
            wallet.setIsBackup(isBackUp);
            wallet.setHint(passwordHint);
            dao.insert(wallet);

            QWAccountDao accountDao = new QWAccountDao(context);
            for (QWAccount account : wallet.getAccountList()) {
                account.setName(name);
                account.setIcon(icon);
                accountDao.insert(account);
                //ETH钱包默认显示QKC
                if (account.isEth()) {
                    QWWalletTokenDao tokenDao = new QWWalletTokenDao(context);
                    QWWalletToken walletToken = new QWWalletToken();
                    walletToken.setTokenAddress(QWTokenDao.QKC_ADDRESS);
                    walletToken.setAccountAddress(account.getAddress());
                    tokenDao.insert(walletToken);
                }
            }
            return wallet;
        }).subscribeOn(Schedulers.io());
    }
}
