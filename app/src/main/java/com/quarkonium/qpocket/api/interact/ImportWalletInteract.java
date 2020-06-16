package com.quarkonium.qpocket.api.interact;

import android.content.Context;
import android.text.TextUtils;

import com.quarkonium.qpocket.api.Constant;
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
import com.quarkonium.qpocket.crypto.CreateWalletException;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.WalletIconUtils;
import com.quarkonium.qpocket.R;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class ImportWalletInteract {

    private final WalletRepositoryType walletRepository;
    private final PasswordStore passwordStore;

    public ImportWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        this.walletRepository = walletRepository;
        this.passwordStore = passwordStore;
    }

    //导入助记词
    public Single<QWWallet> importPhrase(Context context, String phrase, String password, int type) {
        return walletRepository
                .importPhraseToWallet(context, true, phrase, password, type)
                .compose(Operators.saveMnemonic(passwordStore, walletRepository, phrase, password));
    }

    //导入私钥
    public Single<QWWallet> importPrivateKey(Context context, boolean segWit, String privateKey, String password, int type) {
        return walletRepository.importPrivateKeyToWallet(context, segWit, privateKey, password, type)
                .compose(Operators.saveMnemonic(passwordStore, walletRepository, "", password));
    }

    //导入keystore
    public Single<QWWallet> importKeystore(Context context, boolean segWit, String keystore, String password, int type) {
        return walletRepository.importKeystoreToWallet(context, segWit, keystore, password, type)
                .compose(Operators.saveMnemonic(passwordStore, walletRepository, "", password));
    }

    //插入观察钱包数据库
    public Single<QWWallet> insertWatchDB(Context context, String checkAddress, int type, String ledgerId, String ledgerPath) {
        return Single.fromCallable(() -> {
            QWAccountDao dao = new QWAccountDao(context);

            //校验添加0x
            String address = checkAddress;
            String lowerCaseAddress = address;

            //是否存在
            if (type == Constant.WALLET_TYPE_QKC) {
                address = Numeric.prependHexPrefix(checkAddress).toLowerCase();
                String temp = Numeric.parseAddressToEth(address);
                if (dao.hasExist(temp)) {
                    throw new CreateWalletException(R.string.import_wallet_fail_exit);
                }
                lowerCaseAddress = Numeric.parseAddressToQuark(temp);
                if (dao.hasExist(lowerCaseAddress)) {
                    throw new CreateWalletException(R.string.import_wallet_fail_exit);
                }
            } else if (type == Constant.WALLET_TYPE_ETH) {
                lowerCaseAddress = Numeric.prependHexPrefix(checkAddress).toLowerCase();
                if (dao.hasExist(lowerCaseAddress)) {
                    throw new CreateWalletException(R.string.import_wallet_fail_exit);
                }
                String temp = Numeric.parseAddressToQuark(address);
                if (dao.hasExist(temp)) {
                    throw new CreateWalletException(R.string.import_wallet_fail_exit);
                }
            } else if (dao.hasExist(lowerCaseAddress)) {
                throw new CreateWalletException(R.string.import_wallet_fail_exit);
            }

            String key = QWWalletDao.getRandomKey(context);

            QWWalletDao walletDao = new QWWalletDao(context);
            String name = walletDao.getName(type);
            String icon = WalletIconUtils.randomIconPath(context);

            QWAccount account = new QWAccount(lowerCaseAddress);
            account.setName(name);
            account.setIcon(icon);
            account.setKey(key);
            account.setWalletType(type);
            dao.insert(account);

            ArrayList<QWAccount> list = new ArrayList<>();
            list.add(account);

            QWWallet wallet = new QWWallet();
            wallet.setKey(key);
            wallet.setType(type);
            wallet.setIsWatch(1);
            wallet.setIsBackup(1);
            wallet.setIcon(icon);
            wallet.setName(name);
            wallet.setCurrentAddress(lowerCaseAddress);

            if (!TextUtils.isEmpty(ledgerId)) {
                wallet.setLedgerDeviceId(ledgerId);
                wallet.setLedgerPath(ledgerPath);
            }

            wallet.setAccountList(list);
            walletDao.insert(wallet);

            if (type == Constant.WALLET_TYPE_QKC) {
                String totalChain = SharedPreferencesUtils.getTotalChainCount(context);
                String chain = Numeric.prependHexPrefix(QWWalletUtils.parseChainForAddress(address, Numeric.toBigInt(totalChain)));
                SharedPreferencesUtils.setCurrentChain(context, address, chain);
                List<String> allShared = SharedPreferencesUtils.getTotalSharedSizes(context);
                BigInteger totalShard = BigInteger.ONE;
                if (allShared != null && Numeric.toBigInt(chain).intValue() < allShared.size()) {
                    totalShard = Numeric.toBigInt(allShared.get(Numeric.toBigInt(chain).intValue()));
                }
                String shard = Numeric.prependHexPrefix(QWWalletUtils.parseShardForAddress(address, totalShard));
                SharedPreferencesUtils.setCurrentShard(context, address, shard);
            }
            return wallet;
        });
    }

    //插入数据库
    public Single<QWWallet> insertDB(Context context, QWWallet wallet, String passwordHint) {
        return Single.fromCallable(() -> {

            QWWalletDao dao = new QWWalletDao(context);
            String icon = WalletIconUtils.randomIconPath(context);
            String name = dao.getName(wallet.getType());
            wallet.setName(name);
            wallet.setIcon(icon);
            wallet.setIsBackup(1);
            wallet.setHint(passwordHint);
            dao.insert(wallet);

            QWAccountDao accountDao = new QWAccountDao(context);
            for (QWAccount account : wallet.getAccountList()) {
                account.setName(name);
                account.setIcon(icon);
                accountDao.insert(account);

                //ETH钱包默认显示ERC20 Token QKC
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

    //获取对应币种下钱包account最深的路径
    public Single<Integer> queryChildAccountPathIndex(Context context, QWWallet wallet, int coinType, int start) {
        return Single.fromCallable(() -> {
            //获取私钥路径下标
            QWAccountDao accountDao = new QWAccountDao(context);
            int accountIndex = -1;
            switch (coinType) {
                case Constant.HD_PATH_CODE_ETH:
                    accountIndex = accountDao.queryNextAccountPathIndex(wallet.getKey(), Constant.ACCOUNT_TYPE_ETH, start);
                    break;
                case Constant.HD_PATH_CODE_TRX:
                    accountIndex = accountDao.queryNextAccountPathIndex(wallet.getKey(), Constant.ACCOUNT_TYPE_TRX, start);
                    break;
                case Constant.HD_PATH_CODE_QKC:
                    accountIndex = accountDao.queryNextAccountPathIndex(wallet.getKey(), Constant.ACCOUNT_TYPE_QKC, start);
                    break;
            }
            return accountIndex;
        });
    }

    //创建对应币种下指定路径的钱包account
    public Single<QWWallet> createChildAccount(Context context, QWWallet wallet, int coinType, int accountIndex, boolean first) {
        return Single.fromCallable(() -> {
            String mnemonic = passwordStore.getMnemonic(wallet).blockingGet();
            String password = passwordStore.getPassword(wallet).blockingGet();

            String name = wallet.getName();
            QWAccountDao accountDao = new QWAccountDao(context);
            //创建子钱包QWAccount
            int index = accountIndex;
            QWAccount account = null;
            if (first) {
                account = createQWAccount(context, coinType, password, mnemonic, index, wallet);
            } else {
                //创建成功
                do {
                    try {
                        account = createQWAccount(context, coinType, password, mnemonic, index, wallet);
                    } catch (CreateWalletException e) {
                        //重复，跳过，获取下一个可创建下标
                        index = queryChildAccountPathIndex(context, wallet, coinType, ++index).blockingGet();
                    }
                } while (account == null);
            }
            if (account == null) {
                return null;
            }
            account.setKey(wallet.getKey());
            //确认名字
            account.setName(name);
            //确认icon
            List<QWAccount> list = accountDao.queryParamsByKey(wallet.getKey());
            if (list != null && !list.isEmpty()) {
                account.setIcon(list.get(0).getIcon());
            } else {
                account.setIcon(wallet.getIcon());
            }
            //添加钱包
            accountDao.insert(account);

            //ETH钱包默认显示ERC20 Token QKC
            if (account.isEth()) {
                QWWalletTokenDao tokenDao = new QWWalletTokenDao(context);
                QWWalletToken walletToken = new QWWalletToken();
                walletToken.setTokenAddress(QWTokenDao.QKC_ADDRESS);
                walletToken.setAccountAddress(account.getAddress());
                tokenDao.insert(walletToken);
            }

            //更新钱包
            wallet.setCurrentAddress(account.getAddress());
            wallet.setCurrentAccount(account);
            QWWalletDao walletDao = new QWWalletDao(context);
            walletDao.update(wallet);
            return wallet;
        });
    }

    private QWAccount createQWAccount(Context context, int coinType,
                                      String password, String mnemonic,
                                      int accountIndex,
                                      QWWallet wallet) throws CreateWalletException {
        QWAccount account = null;
        try {
            switch (coinType) {
                case Constant.HD_PATH_CODE_ETH:
                    account = walletRepository.createETHChildAccount(password, mnemonic, accountIndex).blockingGet();
                    account.setType(Constant.ACCOUNT_TYPE_ETH);
                    break;
                case Constant.HD_PATH_CODE_TRX:
                    account = walletRepository.createTRXChildAccount(password, mnemonic, accountIndex).blockingGet();
                    account.setType(Constant.ACCOUNT_TYPE_TRX);
                    break;
                case Constant.HD_PATH_CODE_QKC:
                    account = walletRepository.createQKCChildAccount(context, password, mnemonic, accountIndex).blockingGet();
                    account.setType(Constant.ACCOUNT_TYPE_QKC);
                    break;
            }
        } catch (Exception e) {
            if (CreateWalletException.class.getName().equals(e.getMessage())) {
                CreateWalletException exception = new CreateWalletException(R.string.import_wallet_fail_exit);
                exception.setAccountIndex(accountIndex);
                throw exception;
            }
        }
        return account;
    }
}
