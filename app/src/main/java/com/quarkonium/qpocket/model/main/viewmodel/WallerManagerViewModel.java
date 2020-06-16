package com.quarkonium.qpocket.model.main.viewmodel;

import android.app.Application;

import androidx.lifecycle.MutableLiveData;

import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.interact.FetchWalletsInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.base.SingleLiveEvent;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class WallerManagerViewModel extends BaseAndroidViewModel {

    private final FetchWalletsInteract mFetchWalletsInteract;
    private final SetDefaultWalletInteract mSetDefaultWalletInteract;
    private final MutableLiveData<QWWallet[]> mFindWalletObserve = new MutableLiveData<>();
    private final MutableLiveData<QWWallet> mSetDefaultWalletObserve = new MutableLiveData<>();

    WallerManagerViewModel(Application application,
                           FetchWalletsInteract fetchWalletsInteract,
                           SetDefaultWalletInteract setDefaultWalletInteract) {
        super(application);
        this.mFetchWalletsInteract = fetchWalletsInteract;
        this.mSetDefaultWalletInteract = setDefaultWalletInteract;
    }

    private Observable<QWWallet[]> findAllWallets() {
        return Observable.create(e -> {
            //1、获取钱包
            QWWallet[] wallets = mFetchWalletsInteract.fetchManagerWallets(getApplication()).blockingGet();
            e.onNext(wallets);
            //2、非冷钱包模式，获取金额
            if (wallets != null) {
                for (QWWallet wallet : wallets) {
                    List<QWAccount> accounts = wallet.getAccountList();
                    for (QWAccount account : accounts) {
                        try {
                            double price = mFetchWalletsInteract.getAccountBalance(getApplication(), account);
                            account.setTotalPrice(price);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                e.onNext(wallets);
            }
            //3、完成
            e.onComplete();
        });
    }

    public void findWallets() {
        progress.setValue(true);
        Disposable disposable = findAllWallets()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onFindSuccess, this::onFindError);
        addDisposable(disposable);
    }

    public MutableLiveData<QWWallet[]> getWalletObserve() {
        return mFindWalletObserve;
    }

    private void onFindError(Throwable throwable) {
        onError(throwable);
    }

    private void onFindSuccess(QWWallet[] wallets) {
        progress.postValue(false);
        mFindWalletObserve.postValue(wallets);
    }

    public void setDefaultWallet(QWWallet wallet) {
        Disposable disposable = mSetDefaultWalletInteract
                .set(wallet)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> onDefaultWalletChanged(wallet), this::onError);
        addDisposable(disposable);
    }


    private void onDefaultWalletChanged(QWWallet wallet) {
        progress.postValue(false);
        mSetDefaultWalletObserve.postValue(wallet);
    }

    public MutableLiveData<QWWallet> getDefaultObserve() {
        return mSetDefaultWalletObserve;
    }

    //****************切换钱包币种********************
    private SingleLiveEvent<QWWallet> mChangeSymbol = new SingleLiveEvent<>();

    public void changeWalletSymbol(QWWallet wallet) {
        cancelDisposable("changeWalletSymbol");
        Disposable disposable = changeSymbol(wallet)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onChangeSymbolSuccess, v -> onChangeSymbolSuccess(wallet));
        addDisposable("changeWalletSymbol", disposable);
    }

    private Single<QWWallet> changeSymbol(QWWallet wallet) {
        return Single.fromCallable(() -> {
            QWWalletDao walletDao = new QWWalletDao(getApplication());
            walletDao.updateCurrentAddress(wallet.getCurrentAddress(), wallet.getKey());
            return wallet;
        });
    }

    private void onChangeSymbolSuccess(QWWallet wallet) {
        mChangeSymbol.postValue(wallet);
    }

    public SingleLiveEvent<QWWallet> changeSymbol() {
        return mChangeSymbol;
    }
    //****************切换钱包币种********************
}
