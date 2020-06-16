package com.quarkonium.qpocket.model.wallet.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.interact.CreateWalletInteract;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;
import com.quarkonium.qpocket.MainApplication;

import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CreateWalletViewModel extends BaseAndroidViewModel {

    private final CreateWalletInteract mCreateWalletInteract;
    private final SetDefaultWalletInteract mSetDefaultWalletInteract;
    private final FindDefaultWalletInteract mFindDefaultWalletInteract;

    private final MutableLiveData<String> mCreatedMnemonicObserve = new MutableLiveData<>();
    private final MutableLiveData<QWWallet> mCreatedWalletObserve = new MutableLiveData<>();
    private final MutableLiveData<String> mFindWalletAddressObserve = new MutableLiveData<>();

    CreateWalletViewModel(MainApplication application,
                          CreateWalletInteract createWalletInteract,
                          SetDefaultWalletInteract setDefaultWalletInteract,
                          FindDefaultWalletInteract findDefaultWalletInteract) {
        super(application);
        this.mCreateWalletInteract = createWalletInteract;
        this.mSetDefaultWalletInteract = setDefaultWalletInteract;
        this.mFindDefaultWalletInteract = findDefaultWalletInteract;
    }

    //*****************创建助记词*******************
    public void generateMnemonic() {
        Disposable disposable = mCreateWalletInteract
                .generateMnemonic()//创建钱包
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMnemonicSuccess, this::onError);
        addDisposable(disposable);
    }

    public void changeMnemonic(String mnemonic, Locale locale) {
        cancelDisposable("changeMnemonic");
        Disposable disposable = mCreateWalletInteract
                .chooseMnemonic(getApplication(), mnemonic, locale)//创建钱包
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMnemonicSuccess, this::onError);
        addDisposable("changeMnemonic", disposable);
    }

    private void onMnemonicSuccess(String mnemonic) {
        mCreatedMnemonicObserve.postValue(mnemonic);
    }

    public MutableLiveData<String> mnemonicObserve() {
        return mCreatedMnemonicObserve;
    }
    //*****************创建助记词*******************

    //*****************创建钱包*******************
    public void newWallet(String mnemonic, String password, String passwordHint) {
        progress.setValue(true);
        Disposable disposable = mCreateWalletInteract
                .create(getApplication(), mnemonic, password, passwordHint, 0)//创建钱包
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setDefaultWallet, this::onError);
        addDisposable(disposable);
    }

    public void newWallet(String mnemonic, String password, String passwordHint, int isBackup) {
        progress.setValue(true);
        Disposable disposable = mCreateWalletInteract
                .create(getApplication(), mnemonic, password, passwordHint, isBackup)//创建钱包
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setDefaultWallet, this::onError);
        addDisposable(disposable);
    }

    private void setDefaultWallet(QWWallet wallet) {
        Disposable disposable = mSetDefaultWalletInteract
                .set(wallet)
                .subscribe(() -> onDefaultWalletChanged(wallet), this::onError);
        addDisposable(disposable);
    }

    private void onDefaultWalletChanged(QWWallet wallet) {
        progress.postValue(false);
        mCreatedWalletObserve.postValue(wallet);
    }

    public LiveData<QWWallet> createdWallet() {
        return mCreatedWalletObserve;
    }
    //*****************创建钱包*******************

    //*****************查找钱包*******************
    public void findPhraseByKey(String key) {
        progress.setValue(true);
        Disposable disposable = mFindDefaultWalletInteract
                .findPhraseByKey(key)
                .subscribe(this::onFindSuccess, this::onFindError);
        addDisposable(disposable);
    }

    private void onFindError(Throwable throwable) {
        onError(throwable);
    }

    private void onFindSuccess(String mnemonic) {
        progress.postValue(false);
        mFindWalletAddressObserve.postValue(mnemonic);
    }

    public MutableLiveData<String> findObserve() {
        return mFindWalletAddressObserve;
    }
    //*****************查找钱包*******************
}
