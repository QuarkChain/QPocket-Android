package com.quarkonium.qpocket.api.repository;

import android.content.Context;
import android.text.TextUtils;

import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWBalanceDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWTransactionDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.service.AccountKeystoreService;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.tron.TronKeystoreAccountService;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

//钱包管理
public class WalletRepository implements WalletRepositoryType {

    private final PreferenceRepositoryType preferenceRepositoryType;
    private final AccountKeystoreService accountKeystoreService;
    private final AccountKeystoreService tronAccountKeystoreService;

    public WalletRepository(
            PreferenceRepositoryType preferenceRepositoryType,
            AccountKeystoreService accountKeystoreService,
            TronKeystoreAccountService tronAccountKeystoreService) {
        this.preferenceRepositoryType = preferenceRepositoryType;
        this.accountKeystoreService = accountKeystoreService;
        this.tronAccountKeystoreService = tronAccountKeystoreService;
    }

    @Override
    public Single<QWWallet[]> fetchWallets(Context context) {
        return Single.fromCallable(() -> {
            //获取所有钱包
            QWWalletDao dao = new QWWalletDao(context);
            List<QWWallet> list = dao.queryAll();
            if (list != null) {
                Collections.reverse(list);//对钱包数据进行倒叙排序
                QWWallet[] arrays = new QWWallet[list.size()];
                arrays = list.toArray(arrays);
                return arrays;
            }
            return new QWWallet[0];
        });
    }

    @Override
    public Single<QWWallet> fetchBadWallet(Context context) {
        return Single.fromCallable(() -> {
            SharedPreferenceRepository repository = new SharedPreferenceRepository(context);
            String key = repository.getCurrentWalletKey();
            if (!TextUtils.isEmpty(key) && !repository.isClearBadWallet()) {
                QWWalletDao dao = new QWWalletDao(context);
                QWAccountDao accountDao = new QWAccountDao(context);
                boolean hasExit = accountDao.hasExistAccount(key);
                if (!hasExit) {
                    //当前钱包不存在,脏数据,删除
                    dao.delete(key);
                    //更新当前钱包
                    repository.setCurrentWalletKey("");

                    List<QWWallet> list = dao.queryAll();
                    if (list != null) {
                        for (QWWallet wallet : list) {
                            hasExit = accountDao.hasExistAccount(wallet.getKey());
                            if (hasExit) {
                                repository.setCurrentWalletKey(wallet.getKey());
                            } else {
                                dao.delete(wallet);
                            }
                        }
                    }
                    //如果当前已经没有钱包，则默认选中页
                    if (TextUtils.isEmpty(repository.getCurrentWalletKey())) {
                        SharedPreferencesUtils.setMainTabIndex(context, -1);
                    }
                } else {
                    List<QWWallet> list = dao.queryAll();
                    if (list != null) {
                        for (QWWallet wallet : list) {
                            if (!accountDao.hasExistAccount(wallet.getKey())) {
                                dao.delete(wallet);
                            }
                        }
                    }
                }
            }
            repository.setClearBadWallet();
            return null;
        });
    }

    @Override
    public Single<QWWallet[]> fetchManagerWallets(Context context) {
        return Single.fromCallable(() -> {
            //获取所有钱包
            QWWalletDao dao = new QWWalletDao(context);
            List<QWWallet> list = dao.queryAll();
            if (list != null) {
                //获取钱包对应的account
                QWAccountDao accountDao = new QWAccountDao(context);
                QWWallet[] arrays = new QWWallet[list.size()];
                int size = list.size() - 1;
                for (int i = 0; i <= size; i++) {
                    QWWallet wallet = list.get(i);
                    List<QWAccount> accounts = accountDao.queryByKey(wallet.getKey());
                    //按币种进行归类
                    List<QWAccount> accountList = new ArrayList<>();
                    List<QWAccount> QKCList = new ArrayList<>();
                    List<QWAccount> TRXList = new ArrayList<>();
                    List<QWAccount> ETHList = new ArrayList<>();
                    List<QWAccount> BTCList = new ArrayList<>();
                    for (QWAccount account : accounts) {
                        if (account.isBTCSegWit()
                                && wallet.isShowBTCSegWit(account.getPathAccountIndex())) {
                            BTCList.add(account);
                        } else if (account.isBTC()
                                && !wallet.isShowBTCSegWit(account.getPathAccountIndex())) {
                            BTCList.add(account);
                        } else if (account.isEth()) {
                            ETHList.add(account);
                        } else if (account.isTRX()) {
                            TRXList.add(account);
                        } else if (account.isQKC()) {
                            QKCList.add(account);
                        }
                    }
                    accountList.addAll(QKCList);
                    accountList.addAll(TRXList);
                    accountList.addAll(ETHList);
                    accountList.addAll(BTCList);
                    wallet.setAccountList(accountList);

                    arrays[size - i] = wallet;
                }
                return arrays;
            }
            return new QWWallet[0];
        });
    }

    @Override
    public Single<QWWallet> findWallet(Context context, String key) {
        return fetchWallets(context)
                .flatMap(accounts -> {
                    for (QWWallet wallet : accounts) {
                        if (TextUtils.equals(key, wallet.getKey())) {
                            return Single.just(wallet);
                        }
                    }
                    return null;
                });
    }

    //获取QKC默认链和分片
    private void initCurrentChainByMaxBalance(Context context, String address) {
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

    //创建钱包
    @Override
    public Single<QWWallet> createWallet(Context context, String mnemonic, String password) {
        return Single.zip(
                accountKeystoreService.createAccount(Constant.HD_PATH_CODE_QKC, 0, mnemonic, password),//创建QKC钱包
                accountKeystoreService.createAccount(Constant.HD_PATH_CODE_ETH, 0, mnemonic, password),//创建ETH钱包
                tronAccountKeystoreService.createAccount(Constant.HD_PATH_CODE_TRX, 0, mnemonic, password),//创建TRX钱包
                (QWAccount qkc, QWAccount eth, QWAccount tron) -> {
                    if (qkc != null && eth != null && tron != null) {
                        return insertHDWallet(context, qkc, eth, tron, true);
                    }
                    return null;
                });
    }

    //导入助记词 并生成钱包
    @Override
    public Single<QWWallet> importPhraseToWallet(Context context, boolean segWit, String phrase, String newPassword, int type) {
        return Single.zip(
                accountKeystoreService.createAccount(Constant.HD_PATH_CODE_QKC, 0, phrase, newPassword),//创建QKC钱包
                accountKeystoreService.createAccount(Constant.HD_PATH_CODE_ETH, 0, phrase, newPassword),//创建ETH钱包
                tronAccountKeystoreService.createAccount(Constant.HD_PATH_CODE_TRX, 0, phrase, newPassword),//创建TRX钱包
                (QWAccount qkc, QWAccount eth, QWAccount trx) -> {
                    if (qkc != null && eth != null && trx != null) {
                        return insertHDWallet(context, qkc, eth, trx, segWit);
                    }
                    return null;
                });
    }

    private QWWallet insertHDWallet(Context context, QWAccount qkc, QWAccount eth, QWAccount trx, boolean segWit) {
        ArrayList<QWAccount> list = new ArrayList<>();
        QWWallet wallet = new QWWallet();
        wallet.setKey(QWWalletDao.getRandomKey(context));
        wallet.setType(Constant.WALLET_TYPE_HD);

        //设置比特币当前选择钱包格式
        wallet.setShowBTCSegWit(0, segWit);

        //转为QKC地址
        qkc.setAddress(Numeric.parseAddressToQuark(qkc.getAddress()));
        qkc.setKey(wallet.getKey());
        qkc.setType(Constant.ACCOUNT_TYPE_QKC);
        list.add(qkc);

        //eth钱包
        eth.setKey(wallet.getKey());
        eth.setType(Constant.ACCOUNT_TYPE_ETH);
        list.add(eth);

        //tron钱包
        trx.setKey(wallet.getKey());
        trx.setType(Constant.ACCOUNT_TYPE_TRX);
        list.add(trx);

        //默认钱包币种为QKC
        wallet.setCurrentAddress(qkc.getAddress());

        wallet.setAccountList(list);

        //设置QKC默认链 分片
        initCurrentChainByMaxBalance(context, qkc.getAddress());
        return wallet;
    }

    @Override
    public Single<QWWallet> importPrivateKeyToWallet(Context context, boolean segWit, String privateKey, String newPassword, int type) {
        //tron钱包
        if (type == Constant.WALLET_TYPE_TRX) {
            return tronAccountKeystoreService
                    .importPrivateKey(privateKey, newPassword)
                    .map(account -> {
                        QWWallet wallet = new QWWallet();
                        wallet.setKey(QWWalletDao.getRandomKey(context));
                        wallet.setType(type);

                        account.setKey(wallet.getKey());
                        account.setType(Constant.ACCOUNT_TYPE_TRX);

                        wallet.setCurrentAddress(account.getAddress());

                        ArrayList<QWAccount> list = new ArrayList<>();
                        list.add(account);
                        wallet.setAccountList(list);
                        return wallet;
                    });
        }

        return accountKeystoreService.importPrivateKey(privateKey, newPassword)
                .map(account -> {
                    QWWallet wallet = new QWWallet();
                    wallet.setKey(QWWalletDao.getRandomKey(context));
                    wallet.setType(type);

                    account.setKey(wallet.getKey());
                    switch (type) {
                        case Constant.WALLET_TYPE_QKC:
                            account.setAddress(Numeric.parseAddressToQuark(account.getAddress()));
                            account.setType(Constant.ACCOUNT_TYPE_QKC);

                            //设置QKC默认链 分片
                            initCurrentChainByMaxBalance(context, account.getAddress());
                            break;
                        case Constant.WALLET_TYPE_ETH:
                            account.setType(Constant.ACCOUNT_TYPE_ETH);
                            break;
                    }

                    wallet.setCurrentAddress(account.getAddress());

                    ArrayList<QWAccount> list = new ArrayList<>();
                    list.add(account);
                    wallet.setAccountList(list);
                    return wallet;
                });
    }

    @Override
    public Single<QWWallet> importKeystoreToWallet(Context context, boolean segWit, String store, String password, int type) {
        //tron钱包
        if (type == Constant.WALLET_TYPE_TRX) {
            return tronAccountKeystoreService.importKeystore(store, password, password)
                    .map(account -> {
                        QWWallet wallet = new QWWallet();
                        wallet.setKey(QWWalletDao.getRandomKey(context));
                        wallet.setType(type);

                        account.setKey(wallet.getKey());
                        account.setType(Constant.ACCOUNT_TYPE_TRX);

                        wallet.setCurrentAddress(account.getAddress());

                        ArrayList<QWAccount> list = new ArrayList<>();
                        list.add(account);
                        wallet.setAccountList(list);
                        return wallet;
                    });
        }

        return accountKeystoreService.importKeystore(store, password, password)
                .map(account -> {
                    QWWallet wallet = new QWWallet();
                    wallet.setKey(QWWalletDao.getRandomKey(context));
                    wallet.setType(type);

                    account.setKey(wallet.getKey());
                    switch (type) {
                        case Constant.WALLET_TYPE_QKC:
                            account.setAddress(Numeric.parseAddressToQuark(account.getAddress()));
                            account.setType(Constant.ACCOUNT_TYPE_QKC);

                            //设置QKC默认链 分片
                            initCurrentChainByMaxBalance(context, account.getAddress());
                            break;
                        case Constant.WALLET_TYPE_ETH:
                            account.setType(Constant.ACCOUNT_TYPE_ETH);
                            break;
                    }

                    wallet.setCurrentAddress(account.getAddress());

                    ArrayList<QWAccount> list = new ArrayList<>();
                    list.add(account);
                    wallet.setAccountList(list);
                    return wallet;
                });
    }

    @Override
    public Single<QWAccount> createTrxWallet(String mnemonic, String password) {
        return tronAccountKeystoreService.createAccount(Constant.HD_PATH_CODE_TRX, 0, mnemonic, password);
    }

    @Override
    public Single<String> exportKeystoreWallet(QWAccount wallet, String password, String newPassword) {
        //trx
        if (wallet != null && (wallet.getType() == Constant.WALLET_TYPE_TRX
                || (wallet.getType() == Constant.WALLET_TYPE_HD && TronWalletClient.isTronAddressValid(wallet.getAddress())))) {
            return tronAccountKeystoreService.exportKeystore(wallet, password, newPassword);
        }

        return accountKeystoreService.exportKeystore(wallet, password, newPassword);
    }

    @Override
    public Single<String> exportPrivateKeyWallet(QWAccount wallet, String password, String newPassword) {
        if (wallet != null && (wallet.getType() == Constant.WALLET_TYPE_TRX
                || (wallet.getType() == Constant.WALLET_TYPE_HD && TronWalletClient.isTronAddressValid(wallet.getAddress())))) {
            return tronAccountKeystoreService.exportPrivateKey(wallet, password, newPassword);
        }

        return accountKeystoreService.exportPrivateKey(wallet, password, newPassword);
    }

    @Override
    public Completable deleteWallet(Context context, String key, String password) {
        return Completable.fromAction(() -> {
            //删除account
            QWAccountDao accountDao = new QWAccountDao(context);
            List<QWAccount> list = accountDao.queryByKey(key);
            if (list != null) {
                QWBalanceDao balanceDao = new QWBalanceDao(context);
                QWTransactionDao transactionDao = new QWTransactionDao(context);
                for (QWAccount qwAccount : list) {
                    String address;
                    if (qwAccount.isTRX()) {
                        address = tronAccountKeystoreService
                                .deleteAccount(qwAccount.getAddress(), password)
                                .blockingGet();
                    } else {
                        address = accountKeystoreService
                                .deleteAccount(qwAccount.getAddress(), password)
                                .blockingGet();
                    }
                    if (TextUtils.equals(address, qwAccount.getAddress())) {
                        accountDao.deleteByAddress(address);
                        balanceDao.delete(qwAccount);
                        transactionDao.delete(qwAccount);
                    }
                }
            }

            //删除钱包
            QWWalletDao dao = new QWWalletDao(context);
            QWWallet wallet = dao.queryByKey(key);
            dao.delete(wallet);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Boolean> deleteAccount(Context context, QWAccount account, String password) {
        return Single.fromCallable(() -> {
            QWAccountDao accountDao = new QWAccountDao(context);
            QWBalanceDao balanceDao = new QWBalanceDao(context);
            QWTransactionDao transactionDao = new QWTransactionDao(context);

            //删除account
            String address = delete(account, password);
            //删除成功，则删除数据库数据
            if (!TextUtils.isEmpty(address)) {
                accountDao.deleteByAddress(account.getAddress());
                balanceDao.delete(account);
                transactionDao.delete(account);
            }

            //account全部都被删除
            List<QWAccount> list = accountDao.queryByKey(account.getKey());
            if (list.isEmpty()) {
                //删除钱包wallet
                QWWalletDao dao = new QWWalletDao(context);
                dao.delete(account.getKey());
            }
            return true;
        }).subscribeOn(Schedulers.io());
    }

    private String delete(QWAccount account, String password) {
        //删除account
        String address;
        if (account.isTRX()) {
            address = tronAccountKeystoreService
                    .deleteAccount(account.getAddress(), password)
                    .blockingGet();
        } else {
            address = accountKeystoreService
                    .deleteAccount(account.getAddress(), password)
                    .blockingGet();
        }
        return address;
    }

    //设置默认钱包
    @Override
    public Completable setDefaultWallet(QWWallet wallet) {
        return Completable.fromAction(() -> preferenceRepositoryType.setCurrentWalletKey(wallet.getKey()));
    }

    //根据删除钱包币种切换默认钱包
    @Override
    public Single<QWWallet> setDefaultWallet(Context context, String walletKey, String address) {
        return Single.fromCallable(() -> {
            //1、确定币种
            int accountType = Constant.WALLET_TYPE_QKC;
            if (WalletUtils.isValidAddress(address)) {
                accountType = Constant.WALLET_TYPE_ETH;
            } else if (TronWalletClient.isTronAddressValid(address)) {
                accountType = Constant.WALLET_TYPE_TRX;
            }

            //2、判断该钱包下是否存在与删除币种相同的其他子钱包
            //获取钱包
            QWWalletDao dao = new QWWalletDao(context);
            QWWallet editWallet = dao.queryByKey(walletKey);
            //判断该钱包下是否存在与删除币种相同的其他子钱包
            QWAccountDao accountDao = new QWAccountDao(context);
            List<QWAccount> accountList = accountDao.queryParamsByKey(walletKey);
            if (accountList != null && !accountList.isEmpty()) {
                for (QWAccount account : accountList) {
                    switch (accountType) {
                        case Constant.WALLET_TYPE_QKC:
                            if (account.isQKC()) {
                                dao.updateCurrentAddress(account.getAddress(), walletKey);
                                editWallet.setCurrentAddress(account.getAddress());
                                return editWallet;
                            }
                            break;
                        case Constant.WALLET_TYPE_ETH:
                            if (account.isEth()) {
                                dao.updateCurrentAddress(account.getAddress(), walletKey);
                                editWallet.setCurrentAddress(account.getAddress());
                                return editWallet;
                            }
                            break;
                        case Constant.WALLET_TYPE_TRX:
                            if (account.isTRX()) {
                                dao.updateCurrentAddress(account.getAddress(), walletKey);
                                editWallet.setCurrentAddress(account.getAddress());
                                return editWallet;
                            }
                            break;
                    }
                }
            }


            //3、判断其他钱包下是否有相同的币种
            List<QWWallet> list = dao.queryAll();
            //获取新默认钱包
            QWWallet defaultWallet = null;
            //根据创建顺序逆序 优先取HD钱包
            int size = list.size();
            for (int i = size - 1; i >= 0; i--) {
                QWWallet wallet = list.get(i);
                if (TextUtils.equals(wallet.getKey(), walletKey)) {
                    //是删除钱包，则跳过
                    continue;
                }
                if (wallet.getType() == Constant.WALLET_TYPE_HD) {
                    //判断该HD钱包是否有相同币种子钱包 切该钱包默认选中币种是别的币种
                    List<QWAccount> walletAccountList = accountDao.queryParamsByKey(wallet.getKey());
                    for (QWAccount account : walletAccountList) {
                        if (account.getType() == accountType) {
                            //切换币种
                            dao.updateCurrentAddress(account.getAddress(), wallet.getKey());
                            wallet.setCurrentAddress(account.getAddress());
                            defaultWallet = wallet;
                            break;
                        }
                    }
                    //如果已经找到，跳出循环
                    if (defaultWallet != null) {
                        break;
                    }
                }
            }
            if (defaultWallet == null) {
                //没有HD钱包
                //获取最近创建的同币种钱包
                for (int i = list.size() - 1; i >= 0; i--) {
                    QWWallet wallet = list.get(i);
                    if (TextUtils.equals(wallet.getKey(), walletKey)) {
                        //是删除钱包，则跳过
                        continue;
                    }
                    if (wallet.getType() == accountType) {
                        defaultWallet = wallet;
                        break;
                    }
                }
            }

            //如果所有钱包不都不存在含有相同币种的钱包
            if (defaultWallet == null) {
                if (accountList != null && !accountList.isEmpty()) {
                    //当前编辑钱包有其他币种
                    // 选择当前钱包的其他币种
                    defaultWallet = editWallet;
                } else {
                    //如果当前删除钱包不存在其他account账号
                    //则根据创建顺序选择最近的一个钱包
                    defaultWallet = list.get(size - 1);
                    accountList = accountDao.queryParamsByKey(defaultWallet.getKey());
                }

                //切换币种
                QWAccount qkcAccount = null;
                QWAccount trxAccount = null;
                QWAccount ethAccount = null;
                QWAccount btcAccount = null;
                for (QWAccount account : accountList) {
                    if (qkcAccount == null && account.isQKC()) {
                        qkcAccount = account;
                    }
                    if (ethAccount == null && account.isEth()) {
                        ethAccount = account;
                    }
                    if (trxAccount == null && account.isTRX()) {
                        trxAccount = account;
                    }
                    if (btcAccount == null && account.isAllBTC()) {
                        if ((account.isBTCSegWit() && defaultWallet.isShowBTCSegWit(account.getPathAccountIndex()))
                                || (account.isBTC() && !defaultWallet.isShowBTCSegWit(account.getPathAccountIndex()))) {
                            btcAccount = account;
                        }
                    }
                }
                //按币种顺序进行切换
                if (qkcAccount != null) {
                    dao.updateCurrentAddress(qkcAccount.getAddress(), defaultWallet.getKey());
                    defaultWallet.setCurrentAddress(qkcAccount.getAddress());
                } else if (trxAccount != null) {
                    dao.updateCurrentAddress(trxAccount.getAddress(), defaultWallet.getKey());
                    defaultWallet.setCurrentAddress(trxAccount.getAddress());
                } else if (ethAccount != null) {
                    dao.updateCurrentAddress(ethAccount.getAddress(), defaultWallet.getKey());
                    defaultWallet.setCurrentAddress(ethAccount.getAddress());
                } else if (btcAccount != null) {
                    dao.updateCurrentAddress(btcAccount.getAddress(), defaultWallet.getKey());
                    defaultWallet.setCurrentAddress(btcAccount.getAddress());
                }
            }

            preferenceRepositoryType.setCurrentWalletKey(defaultWallet.getKey());
            return defaultWallet;
        });
    }

    @Override
    public Single<QWWallet> updateWalletCurrentAccount(Context context, String walletKey) {
        return Single.fromCallable(() -> {
            //2、判断该钱包下是否存在与删除币种相同的其他子钱包
            //获取钱包
            QWWalletDao dao = new QWWalletDao(context);
            QWWallet editWallet = dao.queryByKey(walletKey);
            if (editWallet == null) {
                //钱包已经全部删除
                return new QWWallet();
            }

            //判断该钱包下是否存在与删除币种相同的其他子钱包
            QWAccountDao accountDao = new QWAccountDao(context);
            List<QWAccount> accountList = accountDao.queryParamsByKey(walletKey);
            if (accountList != null && !accountList.isEmpty()) {
                editWallet.setCurrentAddress(accountList.get(0).getAddress());
                dao.update(editWallet);
                return editWallet;
            }
            return new QWWallet();
        });
    }

    @Override
    public Single<QWWallet> getDefaultWallet(Context context) {
        return Single.fromCallable(preferenceRepositoryType::getCurrentWalletKey)
                .flatMap(v -> findWallet(context, v));
    }

    //创建ETH子钱包
    @Override
    public Single<QWAccount> createETHChildAccount(String password, String mnemonic, int accountIndex) {
        //创建ETH钱包
        return accountKeystoreService.createAccount(Constant.HD_PATH_CODE_ETH, accountIndex, mnemonic, password)
                .map(account -> {
                    account.setPathAccountIndex(accountIndex);
                    return account;
                });
    }

    //创建TRX子钱包
    @Override
    public Single<QWAccount> createTRXChildAccount(String password, String mnemonic, int accountIndex) {
        //创建TRX钱包
        return tronAccountKeystoreService.createAccount(Constant.HD_PATH_CODE_TRX, accountIndex, mnemonic, password)
                .map(account -> {
                    account.setPathAccountIndex(accountIndex);
                    return account;
                });
    }

    //创建QKC子钱包
    @Override
    public Single<QWAccount> createQKCChildAccount(Context context, String password, String mnemonic, int accountIndex) {
        //创建QKC钱包
        return accountKeystoreService.createAccount(Constant.HD_PATH_CODE_QKC, accountIndex, mnemonic, password)
                .map(account -> {
                    account.setPathAccountIndex(accountIndex);
                    account.setAddress(Numeric.parseAddressToQuark(account.getAddress()));
                    //初始化默认分片
                    initCurrentChainByMaxBalance(context, account.getAddress());
                    return account;
                });
    }

    //获取公链主网币的金额和公链上其他erc20等token的总金额
    public double getAccountBalance(Context context, QWAccount account) {
        if (ToolUtils.isTestNetwork(account.getAddress())) {
            return 0;
        }

        //获取当前选定法币
        String currentPriceCoin = SharedPreferencesUtils.getCurrentMarketCoin(context);

        double totalPrice = 0;
        double price = 0;
        String count = "0";
        ForeignCollection<QWBalance> collection = account.getBalances();
        BigInteger total = BigInteger.ZERO;
        if (collection != null && !collection.isEmpty()) {
            for (QWBalance balance : collection) {
                if (account.isAllBTC()) {
                    if (balance.getQWToken() != null && QWTokenDao.BTC_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        count = QWWalletUtils.getIntTokenFromCong16(balance.getBalance());

                        price = SharedPreferencesUtils.getCoinPrice(context, QWTokenDao.BTC_SYMBOL, currentPriceCoin);
                        break;
                    }
                } else if (account.isEth()) {
                    if (balance.getQWToken() != null && QWTokenDao.ETH_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        count = QWWalletUtils.getIntTokenFromWei16(balance.getBalance());

                        price = SharedPreferencesUtils.getCoinPrice(context, QWTokenDao.ETH_SYMBOL, currentPriceCoin);
                        break;
                    }
                } else if (account.isTRX()) {
                    if (balance.getQWToken() != null && QWTokenDao.TRX_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        count = QWWalletUtils.getIntTokenFromSun16(balance.getBalance());

                        price = SharedPreferencesUtils.getCoinPrice(context, QWTokenDao.TRX_SYMBOL, currentPriceCoin);
                        break;
                    }
                } else if (account.isQKC()) {
                    if (balance.getQWToken() != null && QWTokenDao.QKC_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        total = total.add(Numeric.toBigInt(balance.getBalance()));
                    }
                }
            }

            if (account.getType() == Constant.ACCOUNT_TYPE_QKC) {
                count = QWWalletUtils.getIntTokenFromWei10(total.toString());

                price = SharedPreferencesUtils.getCoinPrice(context, QWTokenDao.QKC_SYMBOL, currentPriceCoin);
            }
            totalPrice += Double.parseDouble(count) * price;
        }

        //其他Token
        totalPrice += getOtherTokenPrice(context, account);

        return totalPrice;
    }

    //获取公链上token的金额
    private double getOtherTokenPrice(Context context, QWAccount account) {
        //获取当前选定法币
        String currentPriceCoin = SharedPreferencesUtils.getCurrentMarketCoin(context);

        double totalPrice = 0;
        double price;
        QWTokenDao dao = new QWTokenDao(context);
        QWBalanceDao balanceDao = new QWBalanceDao(context);

        List<QWToken> list = dao.queryAllTokenByType(account.getType());
        if (list != null) {
            //服务器获取的数据
            for (QWToken token : list) {
                if (QWTokenDao.BTC_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.TRX_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.ETH_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.QKC_SYMBOL.equals(token.getSymbol()) && !account.isEth()) {
                    continue;
                }
                if (account.getType() == Constant.ACCOUNT_TYPE_ETH && token.getChainId() != Constant.ETH_PUBLIC_PATH_MAIN_INDEX) {
                    continue;
                }
                if (account.getType() == Constant.ACCOUNT_TYPE_QKC && token.getChainId() != Constant.QKC_PUBLIC_MAIN_INDEX) {
                    continue;
                }

                //获取balance
                if (token.isNative() && token.getType() == Constant.ACCOUNT_TYPE_QKC) {
                    QWBalance balance = getQKCNativeBalance(balanceDao, account, token);
                    if (balance != null) {
                        String count = QWWalletUtils.getIntTokenFromWei16(balance.getBalance());
                        price = SharedPreferencesUtils.getCoinPrice(context, token.getSymbol().toLowerCase(), currentPriceCoin);
                        totalPrice += Double.parseDouble(count) * price;
                    }
                } else {
                    QWBalance balance = balanceDao.queryByWT(account, token);
                    if (balance != null) {
                        String count = QWWalletUtils.getIntTokenFromWei16(balance.getBalance(), token.getTokenUnit());
                        price = SharedPreferencesUtils.getCoinPrice(context, token.getSymbol().toLowerCase(), currentPriceCoin);
                        totalPrice += Double.parseDouble(count) * price;
                    }
                }
            }
        }
        return totalPrice;
    }

    //获取QKC native Token总余额
    private QWBalance getQKCNativeBalance(QWBalanceDao balanceDao, QWAccount account, QWToken token) {
        List<QWBalance> list = balanceDao.queryTokenAllByWT(account, token);
        if (list != null && !list.isEmpty()) {
            BigInteger totalBalance = BigInteger.ZERO;
            QWBalance currentBalance = list.get(0);
            for (QWBalance balance : list) {
                String value = balance.getBalance();
                if (!TextUtils.isEmpty(value)) {
                    totalBalance = totalBalance.add(Numeric.toBigInt(balance.getBalance()));
                }
            }

            currentBalance.setBalance(Numeric.toHexStringWithPrefix(totalBalance));
            return currentBalance;
        }

        return balanceDao.queryByWT(account, token);
    }
}