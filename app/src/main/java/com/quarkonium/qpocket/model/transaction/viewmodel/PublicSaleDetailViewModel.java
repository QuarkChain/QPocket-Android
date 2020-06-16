package com.quarkonium.qpocket.model.transaction.viewmodel;

import android.app.Application;
import androidx.lifecycle.MutableLiveData;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.base.SingleLiveEvent;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.transaction.bean.PublicTokenLoadBean;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PublicSaleDetailViewModel extends BaseAndroidViewModel {

    private final FindDefaultWalletInteract mFindDefaultWalletInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;

    private final MutableLiveData<QWWallet> mFindWalletObserve = new MutableLiveData<>();

    PublicSaleDetailViewModel(Application application,
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
        progress.setValue(true);
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
        progress.postValue(false);
        mFindWalletObserve.postValue(wallet);
    }
    //************获取默认钱包***********


    //***********************************************************
    //***************获取public sale transaction交易清单*****************
    //***********************************************************
    private final MutableLiveData<List<QWPublicTokenTransaction>> mTransactionObserve = new MutableLiveData<>();

    public void getPublicTokenTransaction(String tokenAddress, boolean onlyLoadDB) {
        cancelDisposable("getPublicTokenTransaction");
        Disposable disposable = mWalletTransactionInteract
                .getPublicTokenTransaction(getApplication(), tokenAddress, onlyLoadDB)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGetTransactionSuccess, v -> onGetTransactionFail());
        addDisposable("getPublicTokenTransaction", disposable);
    }

    private void onGetTransactionSuccess(List<QWPublicTokenTransaction> list) {
        mTransactionObserve.setValue(list);
    }

    private void onGetTransactionFail() {
        mTransactionObserve.setValue(new ArrayList<>());
    }

    public MutableLiveData<List<QWPublicTokenTransaction>> transaction() {
        return mTransactionObserve;
    }
    //***************获取public sale transaction交易清单******************


    //***********************************************************
    //***************分页 交易记录******************
    //***********************************************************
    private final MutableLiveData<PublicTokenLoadBean> mTransactionUpdateObserve = new MutableLiveData<>();
    private final SingleLiveEvent<Throwable> mLoadMoreFailObserve = new SingleLiveEvent<>();

    //分页获取交易记录
    public void getTransactionsByNext(String tokenAddress, String next) {
        cancelDisposable("getTransactionsByNext");
        Disposable disposable = mWalletTransactionInteract
                .getPublicTokenTransactionNext(getApplication(), tokenAddress, next)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoadMoreSuccess, this::onLoadMoreFail);
        addDisposable("getTransactionsByNext", disposable);
    }

    private void onLoadMoreSuccess(List<QWPublicTokenTransaction> list) {
        PublicTokenLoadBean bean = mTransactionUpdateObserve.getValue();
        if (bean == null) {
            bean = new PublicTokenLoadBean();
            bean.setList(new ArrayList<>());
        }
        bean.addAll(list);
        if (list.size() == Constant.QKC_TRANSACTION_LIMIT_INT) {
            bean.setHasLoadMore(true);
        } else {
            bean.setHasLoadMore(false);
        }

        updateTransactionObserve(bean);
    }

    private void onLoadMoreFail(Throwable throwable) {
        mLoadMoreFailObserve.postValue(throwable);
    }

    public MutableLiveData<Throwable> loadMoreFailObserve() {
        return mLoadMoreFailObserve;
    }

    public MutableLiveData<PublicTokenLoadBean> transactionObserve() {
        return mTransactionUpdateObserve;
    }

    public void updateTransactionObserve(PublicTokenLoadBean bean) {
        mTransactionUpdateObserve.postValue(bean);
    }
    //***************分页 交易记录******************

    //***********************************************************
    //******************获取PUBLIC TOKEN BALANCE余额************************
    //***********************************************************
    private MutableLiveData<TokenBean> mTokenBean = new MutableLiveData<>();

    public void findTokenBalance(QWAccount account, QWToken token) {
        Disposable disposable = mWalletTransactionInteract
                .findTokenBalance(getApplication(), account, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokenBeanSuccess, V -> onTokenBeanFail());
        addDisposable(disposable);
    }

    private void onTokenBeanSuccess(TokenBean token) {
        mTokenBean.postValue(token);
    }

    private void onTokenBeanFail() {
    }

    public MutableLiveData<TokenBean> tokenBeanObserve() {
        return mTokenBean;
    }
    //******************获取TOKEN BALANCE************************

    //***********************************************************
    //******************查询指定public scale token余额和可购买数量************************
    //***********************************************************
    private MutableLiveData<QWBalance[]> mPublicTokenBalance = new MutableLiveData<>();

    public void findPublicTokenBalance(QWAccount wallet, QWToken token) {
        Disposable disposable = mWalletTransactionInteract
                .findPublicTokenBalance(getApplication(), wallet, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokensSuccess, v -> onPublicBalanceFail());
        addDisposable(disposable);
    }

    private void onTokensSuccess(QWBalance[] tokens) {
        mPublicTokenBalance.postValue(tokens);
    }

    private void onPublicBalanceFail() {
    }

    public MutableLiveData<QWBalance[]> publicBalanceObserve() {
        return mPublicTokenBalance;
    }
    //******************获取TOKEN BALANCE余额和可购买数量************************
}
