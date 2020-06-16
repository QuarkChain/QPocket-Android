package com.quarkonium.qpocket.model.transaction.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;

import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.api.repository.TokenRepository;
import com.quarkonium.qpocket.base.SingleLiveEvent;
import com.quarkonium.qpocket.crypto.utils.Convert;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.main.bean.TransactionLoadBean;
import com.quarkonium.qpocket.model.market.bean.Coin;
import com.quarkonium.qpocket.model.market.bean.Price;
import com.quarkonium.qpocket.model.transaction.bean.EthGas;
import com.quarkonium.qpocket.model.transaction.bean.MergeBean;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.http.HttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tron.protos.Contract;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class TransactionViewModel extends BaseAndroidViewModel {

    private final FindDefaultWalletInteract mFindDefaultWalletInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;

    private final MutableLiveData<QWWallet> mFindWalletObserve = new MutableLiveData<>();

    TransactionViewModel(Application application,
                         FindDefaultWalletInteract findDefaultWalletInteract,
                         WalletTransactionInteract transactionInteract) {
        super(application);
        this.mFindDefaultWalletInteract = findDefaultWalletInteract;
        this.mWalletTransactionInteract = transactionInteract;
    }


    //***********************************************************
    //************获取默认钱包***********
    //***********************************************************
    public MutableLiveData<QWWallet> findDefaultWalletObserve() {
        return mFindWalletObserve;
    }

    public void findWallet() {
        findWallet(true);
    }

    public void findWallet(boolean loadDB) {
        cancelDisposable("findWallet");
        Disposable disposable = mFindDefaultWalletInteract
                .find(getApplication())
                .map(wallet -> queryDB(wallet, loadDB))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDefaultWalletChanged, this::onError);
        addDisposable("findWallet", disposable);
    }

    //获取数据库信息
    private QWWallet queryDB(QWWallet wallet, boolean loadDB) {
        if (loadDB) {
            QWAccountDao dao = new QWAccountDao(getApplication());
            QWAccount account = dao.queryByAddress(wallet.getCurrentAddress());
            wallet.setCurrentAccount(account);
        }
        return wallet;
    }

    private void onDefaultWalletChanged(QWWallet wallet) {
        mFindWalletObserve.postValue(wallet);
    }
    //************获取默认钱包***********


    //***********************************************************
    //***************获取钱包data*token数量 transaction交易清单*****************
    //***********************************************************
    private final MutableLiveData<Boolean> mAccountDataObserve = new MutableLiveData<>();
    private final MutableLiveData<QWAccount> mAccountAndTransDataObserve = new MutableLiveData<>();
    private final MutableLiveData<Throwable> mDataErrorObserve = new MutableLiveData<>();
    private int mCount = 0;

    //获取分片数量
    public void getAccountData(QWWallet wallet) {
        cancelDisposable("getAccountData");
        Disposable disposable = mWalletTransactionInteract
                .getAccountData(getApplication(), wallet.getCurrentAccount(), wallet.isLedger())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> onGetAccountDataSuccess(), v -> onGetAccountDataFail());
        addDisposable("getAccountData", disposable);
    }

    public void getTokenAndAccountData(QWWallet wallet, QWToken token) {
        cancelDisposable("getTokenAndAccountData");
        QWAccount account = wallet.getCurrentAccount();
        boolean getRawData = wallet.isLedger();
        Disposable disposable = mWalletTransactionInteract
                .findTokenSingleBalance(getApplication(), account, token)
                .flatMap(tokenBean -> mWalletTransactionInteract.getAccountData(getApplication(), account, getRawData))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> onGetAccountDataSuccess(), v -> onGetAccountDataFail());
        addDisposable("getTokenAndAccountData", disposable);
    }

    private void onGetAccountDataSuccess() {
        mAccountDataObserve.setValue(true);
    }

    private void onGetAccountDataFail() {
        mAccountDataObserve.setValue(false);
    }

    public MutableLiveData<Boolean> accountDataObserve() {
        return mAccountDataObserve;
    }

    //同时获取分片数量，交易记录
    public void getFirstBalanceAndTrans(QWWallet wallet) {
        cancelDisposable("getFirstBalanceAndTrans");
        Disposable disposable;
        QWAccount account = wallet.getCurrentAccount();
        mCount = 0;
        disposable = Single.merge(
                mWalletTransactionInteract.getAccountData(getApplication(), account),
                mWalletTransactionInteract.getTransactions(getApplication(), account)
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> onGetDataSuccess(account), this::onDataError);
        addDisposable("getFirstBalanceAndTrans", disposable);
    }

    private void onGetDataSuccess(QWAccount account) {
        mCount++;
        if (mCount == 2) {
            QWAccountDao dao = new QWAccountDao(getApplication());
            account = dao.queryByAddress(account.getAddress());
            mAccountAndTransDataObserve.setValue(account);
        }
    }

    private void onDataError(Throwable throwable) {
        mDataErrorObserve.setValue(throwable);
    }

    public MutableLiveData<QWAccount> dataObserve() {
        return mAccountAndTransDataObserve;
    }

    public MutableLiveData<Throwable> dataError() {
        return mDataErrorObserve;
    }


    //同时获取Token数量，交易记录
    private final MutableLiveData<List<QWTransaction>> mTokenAccountAndTrans = new MutableLiveData<>();

    public void getTokenFirstBalanceAndTrans(QWAccount account, QWToken token, int start) {
        cancelDisposable("getTokenFirstBalanceAndTrans");
        Disposable disposable = Single.zip(
                mWalletTransactionInteract.getTokenBalance(getApplication(), account, token),
                mWalletTransactionInteract.getTokenTransactions(getApplication(), account, token, start),
                (TokenBean tokenBean, List<QWTransaction> list) -> list
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGetTokenDataSuccess, this::onDataError);
        addDisposable("getTokenFirstBalanceAndTrans", disposable);
    }

    private void onGetTokenDataSuccess(List<QWTransaction> list) {
        mTokenAccountAndTrans.setValue(list);
    }

    public MutableLiveData<List<QWTransaction>> tokenAccountAndTrans() {
        return mTokenAccountAndTrans;
    }
    //***************获取钱包data******************


    //***********************************************************
    //***************分页 交易记录******************
    //***********************************************************
    private final MutableLiveData<QWAccount> mRefreshObserve = new MutableLiveData<>();

    //获取分片ID下最新数据
    public void getFirstRefreshTransaction(QWAccount wallet) {
        cancelDisposable("getFirstTrans");
        Disposable disposable = mWalletTransactionInteract
                .getQKCTransactions(getApplication(), wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRefreshSuccess, v -> onRefreshFail());
        addDisposable("getFirstTrans", disposable);
    }

    private void onRefreshSuccess(QWAccount wallet) {
        mRefreshObserve.postValue(wallet);
    }

    private void onRefreshFail() {
        mRefreshObserve.postValue(null);
    }

    public MutableLiveData<QWAccount> firstRefreshTransaction() {
        return mRefreshObserve;
    }

    private final MutableLiveData<TransactionLoadBean> mTransactionObserve = new MutableLiveData<>();
    private final SingleLiveEvent<Throwable> mLoadMoreFailObserve = new SingleLiveEvent<>();

    public void updateTransactionObserve(List<QWTransaction> list) {
        TransactionLoadBean bean = new TransactionLoadBean();
        bean.setList(list);
        mTransactionObserve.postValue(bean);
    }

    public void updateTransactionObserve(TransactionLoadBean bean) {
        mTransactionObserve.postValue(bean);
    }

    //分页获取交易记录
    public void getTransactionsByNext(QWAccount wallet, String next) {
        Disposable disposable = mWalletTransactionInteract
                .getTransactions(getApplication(), wallet, next)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoadMoreSuccess, this::onLoadMoreFail);
        addDisposable(disposable);
    }

    public void getTokenTransactionsByNext(QWAccount wallet, QWToken token, String next) {
        Disposable disposable = mWalletTransactionInteract
                .getTokenTransactionsByNext(getApplication(), wallet, token, next)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoadMoreSuccess, this::onLoadMoreFail);
        addDisposable(disposable);
    }

    private void onLoadMoreSuccess(List<QWTransaction> list) {
        TransactionLoadBean bean = mTransactionObserve.getValue();
        if (bean == null) {
            bean = new TransactionLoadBean();
            bean.setList(new ArrayList<>());
        }
        bean.addAll(list);
        mTransactionObserve.postValue(bean);
    }

    public MutableLiveData<TransactionLoadBean> transactionObserve() {
        return mTransactionObserve;
    }

    private void onLoadMoreFail(Throwable throwable) {
        mLoadMoreFailObserve.postValue(throwable);
    }

    public MutableLiveData<Throwable> loadMoreFailObserve() {
        return mLoadMoreFailObserve;
    }
    //***************分页 交易记录******************


    //***********************************************************
    //***************交易记录详情 cost******************
    //***********************************************************
    private final MutableLiveData<String> mFindTransByIdObserve = new MutableLiveData<>();

    //获取交易详情
    public void getTransactionCostById(String txId, boolean isTrx, String from, String to) {
        Disposable disposable = mWalletTransactionInteract
                .getTransactionCostById(getApplication(), txId, isTrx, from, to)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onFindSuccess, this::onError);
        addDisposable(disposable);
    }

    private void onFindSuccess(String cost) {
        mFindTransByIdObserve.setValue(cost);
    }

    public MutableLiveData<String> findObserve() {
        return mFindTransByIdObserve;
    }
    //***************交易记录详情 cost******************


    //***********************************************************
    //***************创建，发送交易******************
    //***********************************************************
    private final MutableLiveData<String[]> createTransaction = new MutableLiveData<>();
    private final MutableLiveData<Throwable> createTransactionFail = new MutableLiveData<>();
    //***************发送交易******************
    private final MutableLiveData<String> sendTransaction = new MutableLiveData<>();

    private void onCreateTransaction(String[] transaction) {
        progress.postValue(false);
        createTransaction.postValue(transaction);
    }


    private void onCreateTransactionFail(Throwable throwable) {
        createTransactionFail.postValue(throwable);
    }

    private void onSendSuccess(String hashId) {
        sendTransaction.postValue(hashId);
        progress.setValue(false);
    }

    public MutableLiveData<String[]> createSendObserve() {
        return createTransaction;
    }

    public MutableLiveData<Throwable> createSendFailObserve() {
        return createTransactionFail;
    }

    public MutableLiveData<String> sendObserve() {
        return sendTransaction;
    }

    //*******QKC******
    //创建交易单，转QKC和native Token
    public void createTransaction(String password,
                                  String from, String to,
                                  BigInteger amount,
                                  BigInteger gasPrice,
                                  BigInteger gasLimit,
                                  BigInteger networkId,
                                  String transferTokenId, String gasTokenId) {
        BigInteger transferToken = Numeric.toBigInt(transferTokenId);
        BigInteger gasToken = Numeric.toBigInt(gasTokenId);
        Disposable disposable = mWalletTransactionInteract
                .createTransaction(from, password, gasPrice, gasLimit, to, amount, "", networkId, transferToken, gasToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCreateTransaction, this::onCreateTransactionFail);
        addDisposable(disposable);
    }

    //创建Token交易单 QRC20转账清单,可以跨片扣取Gas
    public void createTokenTransfer(String password,
                                    String from, String to,
                                    String contractAddress,
                                    BigInteger amount,
                                    BigInteger gasPrice,
                                    BigInteger gasLimit,
                                    BigInteger networkId,
                                    String transferTokenId, String gasTokenId) {
        BigInteger transferToken = Numeric.toBigInt(transferTokenId);
        BigInteger gasToken = Numeric.toBigInt(gasTokenId);
        String toAddress = Numeric.parseAddressToEth(to);
        final byte[] data = TokenRepository.createTokenTransferData(toAddress, amount);
        Disposable disposable = mWalletTransactionInteract
                .createTransaction(from, password, gasPrice, gasLimit, contractAddress, BigInteger.valueOf(0), Numeric.toHexStringNoPrefix(data), networkId, transferToken, gasToken)
                .subscribe(this::onCreateTransaction, this::onCreateTransactionFail);
        addDisposable(disposable);
    }


    //购买ERC20 Token
    public void createBuyTokenTransfer(String password,
                                       String fromAddress, String contractAddress,
                                       BigInteger amount,
                                       BigInteger gasPrice,
                                       BigInteger gasLimit,
                                       BigInteger networkId,
                                       String transferTokenId, String gasTokenId) {
        BigInteger transferToken = Numeric.toBigInt(transferTokenId);
        BigInteger gasToken = Numeric.toBigInt(gasTokenId);
        final byte[] data = TokenRepository.createBuyTokenTransferData();
        Disposable disposable = mWalletTransactionInteract
                .createTransaction(fromAddress, password, gasPrice, gasLimit, contractAddress, amount, Numeric.toHexStringNoPrefix(data), networkId, transferToken, gasToken)
                .subscribe(this::onCreateTransaction, this::onCreateTransactionFail);
        addDisposable(disposable);
    }

    //发送交易单
    public void senTransaction(String transaction) {
        progress.setValue(true);
        Disposable disposable = mWalletTransactionInteract
                .sendTransaction(transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendSuccess, this::onError);
        addDisposable(disposable);
    }

    public void sendTransactions(JSONArray jsonArray, String txData) {
        progress.setValue(true);
        Disposable disposable = mWalletTransactionInteract
                .sendTransactions(jsonArray, txData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendSuccess, v -> onSendSuccess(""));
        addDisposable(disposable);
    }
    //***************创建交易******************


    //***********************************************************
    //******************获取TOKEN BALANCE************************
    //***********************************************************
    private MutableLiveData<TokenBean> mTokenBean = new MutableLiveData<>();
    private MutableLiveData<Throwable> mTokenBeanFail = new MutableLiveData<>();

    public void findTokenBalance(QWAccount account, QWToken token) {
        Disposable disposable = mWalletTransactionInteract
                .findTokenBalance(getApplication(), account, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokenBeanSuccess, this::onTokenBeanFail);
        addDisposable(disposable);
    }

    private void onTokenBeanSuccess(TokenBean token) {
        mTokenBean.postValue(token);
    }

    private void onTokenBeanFail(Throwable throwable) {
        mTokenBeanFail.postValue(throwable);
    }

    public MutableLiveData<TokenBean> tokenBeanObserve() {
        return mTokenBean;
    }

    public MutableLiveData<Throwable> tokenBeanFailObserve() {
        return mTokenBeanFail;
    }
    //******************获取TOKEN BALANCE************************


    //***********************************************************
    //******************merge 所有分片token************************
    //***********************************************************
    private MutableLiveData<ArrayList<MergeBean>> mMergeStateObserve = new MutableLiveData<>();

    public void mergeTransaction(String toAddress, ArrayList<MergeBean> list, String password, String transferTokenId) {
        BigInteger transferToken = Numeric.toBigInt(transferTokenId);
        Disposable disposable = mWalletTransactionInteract
                .mergeBalance(toAddress, list, password, transferToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMergeSuccess, v -> onMergeFail());
        addDisposable(disposable);
    }

    private void onMergeSuccess(ArrayList<MergeBean> result) {
        mMergeStateObserve.postValue(result);
    }

    private void onMergeFail() {
        ArrayList<MergeBean> list = new ArrayList<>();
        list.add(new MergeBean());
        mMergeStateObserve.postValue(list);
    }

    public MutableLiveData<ArrayList<MergeBean>> mergeStateObserve() {
        return mMergeStateObserve;
    }
    //******************merge 所有分片token************************

    //***********************************************************
    //***************获取手续费******************
    //***********************************************************
    private MutableLiveData<String> mGasPrice = new MutableLiveData<>();

    public void gasPrice(String shard) {
        shard = Numeric.toHexStringWithPrefix(Numeric.toBigInt(shard));
        cancelDisposable("gasPrice");
        Disposable disposable = mWalletTransactionInteract
                .gasPrice(shard)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGasPriceSuccess, v -> onGasFail());
        addDisposable("gasPrice", disposable);
    }

    private void onGasPriceSuccess(String price) {
        if (Constant.sNetworkId.intValue() == Constant.QKC_PUBLIC_DEVNET_INDEX && Numeric.toBigInt(price).intValue() == 0) {
            mGasPrice.postValue("1");
            return;
        }
        mGasPrice.postValue(QWWalletUtils.getIntTokenFromWei16(price, Convert.Unit.GWEI));
    }

    public MutableLiveData<String> gasPriceObserve() {
        return mGasPrice;
    }

    private MutableLiveData<BigInteger> mGasLimit = new MutableLiveData<>();

    public void gasLimit(String fromAddress, String toAddress, String transferTokenId, String gasTokenId) {
        cancelDisposable("gasLimit");
        Disposable disposable = mWalletTransactionInteract
                .gasLimit(fromAddress, toAddress, transferTokenId, gasTokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGasLimitSuccess, v -> onGasLimitDefault(new BigInteger(Constant.DEFAULT_GAS_LIMIT)));
        addDisposable("gasLimit", disposable);
    }

    public void gasLimitSendToken(String fromAddress, String toAddress, String contractAddress, BigInteger amount, String transferTokenId, String gasTokenId) {
        cancelDisposable("gasLimitSendToken");
        Disposable disposable = mWalletTransactionInteract
                .gasLimitSendToken(getApplication(), fromAddress, toAddress, contractAddress, amount, transferTokenId, gasTokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGasLimitSuccess, v -> onGasLimitDefault(new BigInteger(Constant.DEFAULT_GAS_TOKEN_LIMIT)));
        addDisposable("gasLimitSendToken", disposable);
    }

    public void gasLimitForBuy(String fromAddress, String toAddress, BigInteger amount, String transferTokenId, String gasTokenId) {
        cancelDisposable("gasLimitForBuy");
        Disposable disposable = mWalletTransactionInteract
                .gasLimitForBuy(getApplication(), fromAddress, toAddress, amount, transferTokenId, gasTokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGasLimitSuccess, v -> onGasLimitDefault(new BigInteger(Constant.DEFAULT_GAS_TOKEN_LIMIT)));
        addDisposable("gasLimitForBuy", disposable);
    }

    private void onGasLimitSuccess(String limit) {
        mGasLimit.postValue(Numeric.toBigInt(limit));
    }

    private void onGasLimitDefault(BigInteger limit) {
        mGasLimit.postValue(limit);
    }

    public MutableLiveData<BigInteger> gasLimitObserve() {
        return mGasLimit;
    }

    private void onGasFail() {
    }
    //***************获取手续费******************


    //*****************************************************
    //**********************ETH*******************************
    //*****************************************************
    //************************创建交易transaction*****************************
    //创建交易单
    public void createEthTransaction(String password,
                                     String from, String to,
                                     BigInteger amount,
                                     BigInteger gasPrice,
                                     BigInteger gasLimit) {
        Disposable disposable = mWalletTransactionInteract
                .createEthTransaction(from, password, gasPrice, gasLimit, to, amount, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCreateTransaction, this::onCreateTransactionFail);
        addDisposable(disposable);
    }

    //创建Token交易单 ERC20转账清单
    public void createEthTokenTransfer(String password,
                                       String from, String to,
                                       String contractAddress,
                                       BigInteger amount,
                                       BigInteger gasPrice,
                                       BigInteger gasLimit) {
        final byte[] data = TokenRepository.createTokenTransferData(to, amount);
        Disposable disposable = mWalletTransactionInteract
                .createEthTransaction(from, password, gasPrice, gasLimit, contractAddress, BigInteger.ZERO, data)
                .subscribe(this::onCreateTransaction, this::onCreateTransactionFail);
        addDisposable(disposable);
    }

    //发送交易单
    public void senEthTransaction(String transaction) {
        progress.setValue(true);
        Disposable disposable = mWalletTransactionInteract
                .sendEthTransaction(transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendSuccess, this::onError);
        addDisposable(disposable);
    }

    public void ethGasLimit(String fromAddress, String toAddress) {
        cancelDisposable("ethGasLimit");
        Disposable disposable = mWalletTransactionInteract
                .ethGasLimit(fromAddress, toAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGasLimitSuccess, v -> onGasFail());
        addDisposable("ethGasLimit", disposable);
    }

    public void ethGasLimitSendToken(String fromAddress, String toAddress, String contractAddress, BigInteger amount) {
        cancelDisposable("ethGasLimitSendToken");
        Disposable disposable = mWalletTransactionInteract
                .ethGasLimitSendToken(fromAddress, toAddress, contractAddress, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGasLimitSuccess, v -> onGasFail());
        addDisposable("ethGasLimitSendToken", disposable);
    }

    public void ethGasLimitForBuy(String fromAddress, String toAddress, BigInteger amount) {
        cancelDisposable("ethGasLimitForBuy");
        Disposable disposable = mWalletTransactionInteract
                .ethGasLimitForBuy(fromAddress, toAddress, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGasLimitSuccess, v -> onGasFail());
        addDisposable("ethGasLimitForBuy", disposable);
    }

    private MutableLiveData<EthGas> mEthGas = new MutableLiveData<>();

    public void ethSlowFastGas() {
        cancelDisposable("ethSlowFastGas");
        Disposable disposable = mWalletTransactionInteract
                .ethSlowFastGas()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onEthSlowFastGasSuccess, v -> onEthSlowFastGasFail());
        addDisposable("ethSlowFastGas", disposable);
    }

    private void onEthSlowFastGasSuccess(EthGas gas) {
        mEthGas.postValue(gas);
    }

    private void onEthSlowFastGasFail() {
        mEthGas.postValue(null);
    }

    public MutableLiveData<EthGas> ethGas() {
        return mEthGas;
    }

    //*****************************************************
    //**********************TRX*******************************
    //*****************************************************
    //创建交易单
    public void createTrxTransaction(String password,
                                     String from, String to,
                                     double amount,
                                     String tokenAsset) {
        Disposable disposable = mWalletTransactionInteract
                .createTrxTransaction(getApplication(), password, from, to, amount, tokenAsset)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCreateTransaction, this::onCreateTransactionFail);
        addDisposable(disposable);
    }

    //发送交易单
    public void senTrxTransaction(String transaction) {
        progress.setValue(true);
        Disposable disposable = mWalletTransactionInteract
                .sendTrxTransaction(transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendSuccess, this::onError);
        addDisposable(disposable);
    }
    //************************

    //获取需要消耗的带宽
    private final MutableLiveData<Integer> costBandWidth = new MutableLiveData<>();

    //获取需要消耗的带宽
    public void getCostBandWidth(byte[] fromRaw, String to, String asset, double amount) {
        cancelDisposable("getCostBandWidth");
        Disposable disposable = mWalletTransactionInteract
                .getCostBandWidth(getApplication(), fromRaw, to, asset, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCostSuccess, this::onError);
        addDisposable("getCostBandWidth", disposable);
    }

    private void onCostSuccess(Integer bindWidth) {
        costBandWidth.postValue(bindWidth);
    }

    public MutableLiveData<Integer> costBandWidth() {
        return costBandWidth;
    }

    private final MutableLiveData<Integer> mFirstCostBandWidth = new MutableLiveData<>();

    //获取需要消耗的带宽
    public void getFirstCostBandWidth(byte[] fromRaw, String to, String asset, double amount) {
        cancelDisposable("getFirstCostBandWidth");
        Disposable disposable = mWalletTransactionInteract
                .getCostBandWidth(getApplication(), fromRaw, to, asset, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onFirstCostSuccess, v -> {
                });
        addDisposable("getFirstCostBandWidth", disposable);
    }

    private void onFirstCostSuccess(Integer bindWidth) {
        mFirstCostBandWidth.postValue(bindWidth);
    }

    public MutableLiveData<Integer> firstCostBandWidth() {
        return mFirstCostBandWidth;
    }
    //************************

    //************************
    public void getFreezeTrxCost(String key, String address, double amount, Contract.ResourceCode resource) {
        cancelDisposable("getFreezeTrxCost");
        Disposable disposable = mWalletTransactionInteract
                .getFreezeCostBandWidth(key, address, amount, resource)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCostSuccess, this::onSendError);
        addDisposable("getFreezeTrxCost", disposable);
    }

    private final SingleLiveEvent<Integer> unfreezeCostBandWidth = new SingleLiveEvent<>();

    public void getUnfreezeTrxCost(String key, String address, Contract.ResourceCode resource) {
        cancelDisposable("getUnfreezeTrxCost");
        Disposable disposable = mWalletTransactionInteract
                .getUnfreezeCostBandWidth(key, address, resource)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnfreezeCostSuccess, this::onSendError);
        addDisposable("getUnfreezeTrxCost", disposable);
    }

    private void onUnfreezeCostSuccess(Integer v) {
        unfreezeCostBandWidth.postValue(v);
    }

    public SingleLiveEvent<Integer> unfreezeCostBandWidth() {
        return unfreezeCostBandWidth;
    }
    //************************


    //冻结
    //冻结带宽 能量
    private final SingleLiveEvent<Throwable> trxSendTransactionFail = new SingleLiveEvent<>();

    public void freezeTrx(String password, String address, double amount, Contract.ResourceCode resource) {
        cancelDisposable("freezeTrx");
        Disposable disposable = mWalletTransactionInteract
                .trxFreeze(getApplication(), password, address, amount, resource)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendSuccess, this::onSendError);
        addDisposable("freezeTrx", disposable);
    }

    //解冻
    public void trxUnfreeze(String password, String address, Contract.ResourceCode resource) {
        cancelDisposable("trxUnfreeze");
        Disposable disposable = mWalletTransactionInteract
                .trxUnfreeze(getApplication(), password, address, resource)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendSuccess, this::onSendError);
        addDisposable("trxUnfreeze", disposable);
    }

    private void onSendError(Throwable throwable) {
        trxSendTransactionFail.postValue(throwable);
    }

    public SingleLiveEvent<Throwable> onTrxSendError() {
        return trxSendTransactionFail;
    }
    //*****************************************************
    //**********************TRX**END*****************************
    //*****************************************************


    //*****************QKC native Token*********************
    //同时获取分片数量，交易记录
    public void getFirstQKCNativeToken(QWWallet wallet, QWToken token) {
        cancelDisposable("getFirstQKCNativeToken");
        Disposable disposable = Single.zip(
                mWalletTransactionInteract.getAccountData(getApplication(), wallet.getCurrentAccount()),
                mWalletTransactionInteract.getTokenTransactions(getApplication(), wallet.getCurrentAccount(), token, 0),
                (Boolean b, List<QWTransaction> list) -> list
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGetTokenDataSuccess, this::onDataError);
        addDisposable("getFirstQKCNativeToken", disposable);
    }

    //获取分片ID下最新数据
    public void getFirstRefreshNativeTokenTransaction(QWAccount account, QWToken token) {
        cancelDisposable("getFirstRefreshNativeTokenTransaction");
        Disposable disposable = mWalletTransactionInteract
                .getTokenTransactions(getApplication(), account, token, 0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGetTokenDataSuccess, v -> onRefreshFail());
        addDisposable("getFirstRefreshNativeTokenTransaction", disposable);
    }


    //***********************************************************
    //***************获取Coin价格*********************************
    //***********************************************************
    private SingleLiveEvent<Boolean> mCoinPrice = new SingleLiveEvent<>();

    private Price getCoinPrice(String coinID) throws IOException, JSONException {
        String currencies = SharedPreferencesUtils.getCurrentMarketCoin(getApplication());
        String path = Constant.MARKET_API_PATH_COIN_PRICE;
        path = String.format(path, coinID, currencies);

        OkHttpClient okHttpClient = HttpUtils.getOkHttp();
        final okhttp3.Request request = new okhttp3.Request.Builder()
                .url(path)
                .build();
        final Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        if (response.body() != null) {
            String value = response.body().string();
            JSONObject jsonObject = new JSONObject(value);
            if (jsonObject.has(coinID)) {
                Price price = new Price();
                price.setCoinID(coinID);

                JSONObject priceObject = jsonObject.getJSONObject(coinID);
                Iterator<String> it = priceObject.keys();
                while (it.hasNext()) {
                    // 获得key
                    String key = it.next();
                    price.setPriceType(key);

                    float p = (float) priceObject.getDouble(key);
                    price.setPrice(p);
                }
                return price;
            }
        }
        return null;
    }

    public void coinPrice(String symbol) {
        cancelDisposable("coinPrice");
        Disposable disposable = Single.fromCallable(() -> {
            List<AVObject> queryList = null;
            AVQuery<AVObject> avQuery = new AVQuery<>("MarketSearch");
            avQuery.whereEqualTo("symbol", symbol.toLowerCase());
            try {
                queryList = avQuery.find();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            //重复Coin 不获取价格
            if (queryList != null && queryList.size() == 1) {
                AVObject object = queryList.get(0);
                Coin coin = new Coin();
                coin.setId(object.getString("id"));
                coin.setName(object.getString("name"));
                coin.setSymbol(object.getString("symbol"));

                Price price = getCoinPrice(coin.getId());
                //价格存缓存
                if (price != null) {
                    SharedPreferencesUtils.putCoinPrice(getApplication(), coin.getSymbol(), price.getPriceType(), price.getPrice());
                    return true;
                }
            }
            return false;
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::coinPriceSuccess, v -> {
                });

        addDisposable("coinPrice", disposable);
    }

    private void coinPriceSuccess(boolean value) {
        if (value) {
            mCoinPrice.postValue(true);
        }
    }

    public SingleLiveEvent<Boolean> coinPrice() {
        return mCoinPrice;
    }
    //***************获取Coin价格*********************************

    //***************校验token是否支持手续费*********************************
    private final SingleLiveEvent<QWToken> mCheckGasToken = new SingleLiveEvent<>();

    public void checkGasTokenByChain(QWToken token, String chain) {
        cancelDisposable("checkGasTokenByChain");
        progress.setValue(true);
        Disposable disposable = mWalletTransactionInteract
                .checkGasReserveToken(getApplication(), token, null, chain)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCheckGasTokenSuccess, v -> onCheckGasTokenFail());
        addDisposable("checkGasTokenByChain", disposable);
    }

    public void checkGasToken(QWToken token, String from) {
        cancelDisposable("checkGasToken");
        progress.setValue(true);
        Disposable disposable = mWalletTransactionInteract
                .checkGasReserveToken(getApplication(), token, from)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCheckGasTokenSuccess, v -> onCheckGasTokenFail());
        addDisposable("checkGasToken", disposable);
    }

    private void onCheckGasTokenSuccess(QWToken token) {
        progress.setValue(false);
        mCheckGasToken.postValue(token);
    }

    private void onCheckGasTokenFail() {
        progress.setValue(false);
        mCheckGasToken.postValue(null);
    }

    public MutableLiveData<QWToken> checkGasToken() {
        return mCheckGasToken;
    }

    //进入交易界面默认查询
    private final SingleLiveEvent<QWToken> mCheckGasTokenFirst = new SingleLiveEvent<>();

    public void checkGasTokenFirst(QWToken token, String from) {
        cancelDisposable("checkGasTokenFirst");
        Disposable disposable = mWalletTransactionInteract
                .checkGasReserveToken(getApplication(), token, from)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCheckGasTokenFirst, v -> {
                });
        addDisposable("checkGasTokenFirst", disposable);
    }

    private void onCheckGasTokenFirst(QWToken token) {
        mCheckGasTokenFirst.postValue(token);
    }

    public MutableLiveData<QWToken> checkFirstGasToken() {
        return mCheckGasTokenFirst;
    }

    //***************获取merge数据，并校验gas token是否支持手续费*********************************
    private Observable<List<MergeBean>> getMergeData(QWWallet wallet, String mergeSymbol,
                                                     String mainChain, String mainShard,
                                                     BigDecimal amountGWei) {
        return Observable.create((e) -> {
            List<MergeBean> list = new ArrayList<>();
            ForeignCollection<QWBalance> balances = wallet.getCurrentAccount().getBalances();
            if (balances != null && !balances.isEmpty()) {
                HashMap<String, MergeBean> map = new HashMap<>();
                BigInteger gas = QWWalletUtils.toGWeiFrom10(Constant.DEFAULT_GAS_PRICE_TO_GWEI);
                BigInteger gasLimit = Constant.sGasLimit;
                BigInteger mGasCost = gas.multiply(gasLimit);

                //一、获取数据
                //1、对数据按分片进行提取 归类
                for (QWBalance b : balances) {
                    QWToken token = b.getQWToken();
                    //1、进行过滤
                    if (token == null) {
                        continue;
                    }
                    //非QKC Native token进行过滤
                    if (!token.isNative() || token.getType() != Constant.ACCOUNT_TYPE_QKC) {
                        continue;
                    }
                    //当前分片不进行merge
                    QWShard shard = b.getQWShard();
                    QWChain chain = b.getChain();
                    if (shard == null || chain == null) {
                        continue;
                    }
                    if (mainChain.equals(chain.getChain()) && mainShard.equals(shard.getShard())) {
                        continue;
                    }
                    //金额太小，也过滤。小数点前4位都是0
                    String value = QWWalletUtils.getIntTokenFromWei16(b.getBalance(), b.getQWToken().getTokenUnit());
                    if ("0".equals(value)) {
                        continue;
                    }

                    //2、生成数据
                    //同一个链/分片下的所有native token进行归类
                    BigInteger qwBalance = Numeric.toBigInt(b.getBalance());
                    String key = chain.getChain() + "_" + shard.getShard();
                    MergeBean bean = map.get(key);
                    if (bean == null) {
                        bean = new MergeBean();
                        bean.gasTokenList = new ArrayList<>();
                    }
                    if (TextUtils.equals(mergeSymbol, token.getSymbol())) {
                        //当前币种
                        bean.balance = b;
                        bean.gasPrice = gas;
                        bean.normalGasPrice = gas;
                        bean.gasLimit = gasLimit;
                        bean.gasTokenId = QWTokenDao.TQKC_ADDRESS;//初始都用QKC作为手续费
                        bean.amount = qwBalance;
                    }
                    //所有native token都当做手续费
                    bean.gasTokenList.add(b);
                    map.put(key, bean);
                }
                //2、map转为list集合
                for (Map.Entry<String, MergeBean> balanceEntry : map.entrySet()) {
                    if (balanceEntry.getValue().balance != null) {
                        MergeBean bean = balanceEntry.getValue();
                        checkGasTokenList(bean);
                        list.add(bean);
                    }
                }
                //3、排序
                softList(list);

                //4、如果是QKC转账，确定每个分片待转账数量
                if (TextUtils.equals(mergeSymbol, QWTokenDao.QKC_SYMBOL)) {
                    BigInteger total = BigInteger.ZERO;
                    for (MergeBean bean : list) {
                        BigInteger qwBalance = Numeric.toBigInt(bean.balance.getBalance());
                        BigInteger amount = qwBalance.subtract(mGasCost);
                        bean.amount = BigInteger.ZERO.compareTo(amount) > 0 ? BigInteger.ZERO : amount;
                        bean.isSelected = false;
                        if (amountGWei != null) {
                            if (total.compareTo(amountGWei.toBigInteger()) < 0) {
                                //金额不够交易费，则不选中
                                if (bean.amount.compareTo(BigInteger.ZERO) > 0) {
                                    total = total.add(amount);
                                    bean.isSelected = true;
                                }
                            }
                        }
                    }
                }
                //显示数据
                e.onNext(list);

                //二、有网条件下获取gas相关信息
                //拉取gasPrice，避免拉取回来adapter无数据
                if (ConnectionUtil.isInternetConnection(getApplication())) {
                    //1、拉取gas费
                    String shard = QWWalletUtils.parseFullShardForAddress(wallet.getCurrentShareAddress());
                    String price = mWalletTransactionInteract.gasPrice(shard).blockingGet();
                    if (Numeric.toBigInt(price).intValue() == 0) {
                        price = "1";
                    } else {
                        price = QWWalletUtils.getIntTokenFromWei16(price, Convert.Unit.GWEI);
                    }

                    //2、拉取native token详情
                    if (!TextUtils.equals(mergeSymbol, QWTokenDao.QKC_SYMBOL)) {
                        //转非QKC的Native token，需要判定其本身在当前分片是否支持作为手续费使用
                        final Scheduler schedulers = Schedulers.from(Executors.newFixedThreadPool(6));
                        list = Observable.fromIterable(list)
                                .flatMap(mergeBean -> mWalletTransactionInteract
                                        .checkGasReserveToken(getApplication(), mergeBean)
                                        .subscribeOn(schedulers)
                                ).toList()
                                .blockingGet();

                        softList(list);
                    }

                    //3、更新数据
                    gas = QWWalletUtils.toGWeiFrom10(price);
                    BigInteger total = BigInteger.ZERO;
                    for (MergeBean bean : list) {
                        //新的gas费
                        bean.normalGasPrice = gas;
                        bean.gasPrice = bean.refundPercentage != null ?
                                bean.refundPercentage.multiply(new BigDecimal(gas)).toBigInteger() :
                                gas;
                        //确定默认可转账金额
                        //花费手续费
                        mGasCost = bean.gasPrice.multiply(gasLimit);
                        BigInteger amount = Numeric.toBigInt(bean.balance.getBalance());
                        QWToken token = bean.balance.getQWToken();
                        if (TextUtils.equals(token.getAddress(), bean.gasTokenId)) {
                            //转账token和手续费token是同一个时，可转账金额需要减去手续费
                            amount = amount.subtract(mGasCost);
                        }
                        bean.amount = BigInteger.ZERO.compareTo(amount) > 0 ? BigInteger.ZERO : amount;
                        bean.isSelected = false;
                        if (bean.refundPercentage != null && bean.gasTokenList != null) {
                            //成功拿取到兑换比例
                            for (QWBalance balance : bean.gasTokenList) {
                                QWToken qwToken = balance.getQWToken();
                                if (TextUtils.equals(qwToken.getAddress(), bean.gasTokenId)) {
                                    qwToken.setRefundPercentage(bean.refundPercentage);
                                    qwToken.setReserveTokenBalance(bean.reserveTokenBalance);
                                    break;
                                }
                            }
                        }
                        if (amountGWei != null) {
                            if (total.compareTo(amountGWei.toBigInteger()) < 0) {
                                if (bean.amount.compareTo(BigInteger.ZERO) > 0) {
                                    total = total.add(amount);
                                    bean.isSelected = true;
                                }
                            }
                        }
                    }
                    e.onNext(list);
                }
            }
            e.onComplete();
        });
    }

    private void checkGasTokenList(MergeBean bean) {
        QWToken defaultGasToken = QWTokenDao.getTQKCToken();
        boolean hasDefault = false;
        for (QWBalance balance : bean.gasTokenList) {
            QWToken token = balance.getQWToken();
            if (TextUtils.equals(token.getSymbol(), defaultGasToken.getSymbol())) {
                hasDefault = true;
                break;
            }
        }

        if (!hasDefault) {
            QWBalance balance = new QWBalance();
            balance.setQWToken(defaultGasToken);
            balance.setBalance("0x0");
            bean.gasTokenList.add(balance);
        }
    }

    private void softList(List<MergeBean> list) {
        Collections.sort(list, (MergeBean p1, MergeBean p2) -> {
            /*
             * int compare(Person p1, Person p2) 返回一个基本类型的整型，
             * 返回负数表示：p1 小于p2，
             * 返回0 表示：p1和p2相等，
             * 返回正数表示：p1大于p2
             */
            int value = Numeric.toBigInt(p2.balance.getBalance()).compareTo(Numeric.toBigInt(p1.balance.getBalance()));
            if (value == 0) {
                value = Numeric.toBigInt(p1.balance.getChain().getChain()).compareTo(Numeric.toBigInt(p2.balance.getChain().getChain()));
                if (value == 0) {
                    value = Numeric.toBigInt(p1.balance.getQWShard().getShard()).compareTo(Numeric.toBigInt(p2.balance.getQWShard().getShard()));
                }
            }
            return value;
        });
    }

    private final SingleLiveEvent<List<MergeBean>> mMergerList = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> mMergerDataFinish = new SingleLiveEvent<>();

    public void feachMergeData(QWWallet wallet, String mergeSymbol,
                               String mainChain, String mainShard,
                               BigDecimal amountGWei) {
        cancelDisposable("feachMergeData");
        Disposable disposable = getMergeData(wallet, mergeSymbol, mainChain, mainShard, amountGWei)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::feachMergeDataSuccess, v -> {
                }, this::feachMergeDataFinish);
        addDisposable("feachMergeData", disposable);
    }

    private void feachMergeDataSuccess(List<MergeBean> list) {
        mMergerList.setValue(list);
    }

    private void feachMergeDataFinish() {
        mMergerDataFinish.setValue(true);
    }

    public SingleLiveEvent<List<MergeBean>> feachMergeData() {
        return mMergerList;
    }

    public SingleLiveEvent<Boolean> feachMergeFinsh() {
        return mMergerDataFinish;
    }
}
