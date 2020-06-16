package com.quarkonium.qpocket.model.wallet.viewmodel;

import androidx.lifecycle.LiveData;

import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.interact.ImportWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.base.SingleLiveEvent;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;
import com.quarkonium.qpocket.MainApplication;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ImportWalletViewModel extends BaseAndroidViewModel {

    private final ImportWalletInteract mImportInteract;
    private final SetDefaultWalletInteract mSetDefaultWalletInteract;
    private final SingleLiveEvent<QWWallet> mImportWalletObserve = new SingleLiveEvent<>();


    ImportWalletViewModel(MainApplication application, ImportWalletInteract importWalletInteract,
                          SetDefaultWalletInteract setDefaultWalletInteract,
                          WalletTransactionInteract walletTransactionInteract) {
        super(application);
        this.mImportInteract = importWalletInteract;
        mSetDefaultWalletInteract = setDefaultWalletInteract;
    }

    public LiveData<QWWallet> importWallet() {
        return mImportWalletObserve;
    }

    public void onPhrase(String phrase, String password, String passwordHint, int type) {
        progress.postValue(true);
        Disposable disposable = mImportInteract
                .importPhrase(getApplication(), phrase, password, type)
                .flatMap((wallet) -> mImportInteract.insertDB(getApplication(), wallet, passwordHint))//插入数据库
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setDefaultWallet, this::onError);
        addDisposable(disposable);
    }

    public void onKeystore(boolean segWit, String keystore, String password, int type) {
        progress.postValue(true);
        Disposable disposable = mImportInteract
                .importKeystore(getApplication(), segWit, keystore, password, type)
                .flatMap((wallet) -> mImportInteract.insertDB(getApplication(), wallet, ""))//插入数据库
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setDefaultWallet, this::onError);
        addDisposable(disposable);
    }

    public void onPrivateKey(boolean segWit, String key, String password, String passwordHint, int type) {
        progress.postValue(true);
        Disposable disposable = mImportInteract
                .importPrivateKey(getApplication(), segWit, key, password, type)
                .flatMap((wallet) -> mImportInteract.insertDB(getApplication(), wallet, passwordHint))//插入数据库
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setDefaultWallet, this::onError);
        addDisposable(disposable);
    }

    public void onWatch(String address, int type) {
        onWatch(address, type, null, null);
    }

    public void onWatch(String address, int type, String ledgerId, String ledgerPath) {
        //创建watch账户，watch账户不需要交涉GethKeystore，直接创建QuarkWallet即可
        progress.postValue(true);
        Disposable disposable = mImportInteract
                .insertWatchDB(getApplication(), address, type, ledgerId, ledgerPath)//插入数据库
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setDefaultWallet, this::onError);
        addDisposable(disposable);
    }

    private void setDefaultWallet(QWWallet wallet) {
        Disposable disposable = mSetDefaultWalletInteract
                .set(wallet)
                .subscribe(() -> onWallet(wallet), this::onError);
        addDisposable(disposable);
    }

    //导入钱包成功
    private void onWallet(QWWallet wallet) {
        progress.postValue(false);
        mImportWalletObserve.postValue(wallet);
    }

    //********************创建子钱包******************************
    //指定路径创建钱包
    public void createChildAccount(QWWallet wallet, int coinType, int start, boolean isFrist) {
        cancelDisposable("createChildAccount");
        progress.setValue(true);
        Disposable disposable = mImportInteract
                .queryChildAccountPathIndex(getApplication(), wallet, coinType, start)
                .flatMap(index -> mImportInteract
                        .createChildAccount(getApplication(), wallet, coinType, index, isFrist))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setDefaultWallet, this::onError);
        addDisposable("createChildAccount", disposable);
    }
}
