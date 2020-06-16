package com.quarkonium.qpocket.model.main.viewmodel;

import androidx.lifecycle.MutableLiveData;

import android.content.Context;
import android.text.TextUtils;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWBalanceDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.interact.ExportWalletInteract;
import com.quarkonium.qpocket.api.interact.SetDefaultWalletInteract;
import com.quarkonium.qpocket.api.repository.SharedPreferenceRepository;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.viewmodel.BaseViewModel;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class EditWalletViewModel extends BaseViewModel {

    private ExportWalletInteract mExportWalletInteract;
    private MutableLiveData<String> mKeystoreData = new MutableLiveData<>();
    private MutableLiveData<String> mPrivateKeyData = new MutableLiveData<>();
    private MutableLiveData<String> mPhraseData = new MutableLiveData<>();
    private MutableLiveData<QWWallet> mDeleteData = new MutableLiveData<>();
    private MutableLiveData<Boolean> mWalletHasBalance = new MutableLiveData<>();
    private MutableLiveData<Boolean> mAccountHasBalance = new MutableLiveData<>();

    private SetDefaultWalletInteract mSetDefaultWalletInteract;

    EditWalletViewModel(ExportWalletInteract exportWalletInteract, SetDefaultWalletInteract setDefaultWalletInteract) {
        mExportWalletInteract = exportWalletInteract;
        mSetDefaultWalletInteract = setDefaultWalletInteract;
    }

    //导出keystore
    public void exportKeystore(QWAccount qwAccount, String password) {
        progress.setValue(true);
        QWAccount account = new QWAccount(qwAccount.getAddress());
        account.setType(qwAccount.getType());
        disposable = mExportWalletInteract
                .exportKeystore(account, password, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccessKeystore, this::onError);
    }

    //导出私钥
    public void exportPrivateKey(QWAccount qwAccount, String password, String newPassword) {
        progress.setValue(true);
        QWAccount account = new QWAccount(qwAccount.getAddress());
        account.setType(qwAccount.getType());
        disposable = mExportWalletInteract
                .exportPrivateKey(account, password, newPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccessPrivateKey, this::onError);
    }

    //导出助记词
    public void exportPhrase(QWWallet wallet, String password, String newPassword) {
        progress.setValue(true);
        disposable = mExportWalletInteract
                .exportPhrase(wallet, password, newPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccessPhrase, this::onError);
    }

    private void onSuccessKeystore(String keystore) {
        progress.setValue(false);
        mKeystoreData.postValue(keystore);
    }

    public MutableLiveData<String> getKeystoreData() {
        return mKeystoreData;
    }

    private void onSuccessPrivateKey(String keystore) {
        progress.setValue(false);
        mPrivateKeyData.postValue(keystore);
    }

    public MutableLiveData<String> getPrivateKeyData() {
        return mPrivateKeyData;
    }

    private void onSuccessPhrase(String keystore) {
        progress.setValue(false);
        mPhraseData.postValue(keystore);
    }

    public MutableLiveData<String> getPhraseData() {
        return mPhraseData;
    }

    //删除钱包
    public void deleteWatchWalletData(Context context, QWWallet wallet) {
        String defaultKey = wallet.getKey();
        String currentAddress = wallet.getCurrentAddress();
        progress.setValue(true);
        disposable = mExportWalletInteract
                .deleteWatchWalletData(context, wallet)
                .flatMap(state -> {
                    if (state) {
                        SharedPreferenceRepository mRepository = new SharedPreferenceRepository(context);
                        String key = mRepository.getCurrentWalletKey();
                        if (TextUtils.equals(key, defaultKey)) {
                            //删除的是选中钱包，则需要切换钱包
                            return mSetDefaultWalletInteract.setDefaultWallet(context, defaultKey, currentAddress);
                        }
                    }
                    return Single.fromCallable(QWWallet::new);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDeleteSuccess, this::onError);
    }

    //删除钱包
    public void deleteData(Context context, QWWallet wallet, String password) {
        String defaultKey = wallet.getKey();
        String currentAddress = wallet.getCurrentAddress();
        progress.setValue(true);
        disposable = mExportWalletInteract
                .deleteData(context, wallet, password)
                .flatMap(state -> {
                    if (state) {
                        SharedPreferenceRepository mRepository = new SharedPreferenceRepository(context);
                        String key = mRepository.getCurrentWalletKey();
                        if (TextUtils.equals(key, defaultKey)) {
                            //删除的是选中钱包，则需要切换钱包
                            return mSetDefaultWalletInteract.setDefaultWallet(context, defaultKey, currentAddress);
                        }
                    }
                    return Single.fromCallable(QWWallet::new);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDeleteSuccess, this::onError);
    }

    //删除钱包
    public void deleteAccountData(Context context, QWAccount account, String walletCurrentAddress, String password) {
        String defaultKey = account.getKey();
        String accountAddress = account.getAddress();
        progress.setValue(true);
        disposable = mExportWalletInteract
                .deleteAccount(context, account, password)
                .flatMap(state -> {
                    if (state) {
                        SharedPreferenceRepository mRepository = new SharedPreferenceRepository(context);
                        String key = mRepository.getCurrentWalletKey();
                        if (TextUtils.equals(accountAddress, walletCurrentAddress)) {
                            if (TextUtils.equals(key, defaultKey)) {
                                //删除的是当前默认wallet钱包中的选中account，则需要切换一个新的account或者wallet钱包
                                return mSetDefaultWalletInteract.setDefaultWallet(context, defaultKey, accountAddress);
                            } else {
                                //删除的是wallet钱包中的选中account，则需要更新wallet的current address
                                return mSetDefaultWalletInteract.updateWalletCurrentAccount(context, defaultKey);
                            }
                        }
                    }
                    return Single.fromCallable(QWWallet::new);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDeleteSuccess, this::onError);
    }

    private void onDeleteSuccess(QWWallet wallet) {
        mDeleteData.postValue(wallet);
    }

    public MutableLiveData<QWWallet> getDeleteData() {
        return mDeleteData;
    }

    //************************校验金额************************
    private boolean otherTokenHasBalance(Context context, QWAccount account) {
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
                    if (getQKCNativeBalance(balanceDao, account, token)) {
                        return true;
                    }
                } else {
                    QWBalance balance = balanceDao.queryByWT(account, token);
                    if (balance != null) {
                        if (!TextUtils.isEmpty(balance.getBalance()) && Numeric.toBigInt(balance.getBalance()).compareTo(BigInteger.ZERO) > 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    //获取QKC native Token总余额
    private boolean getQKCNativeBalance(QWBalanceDao balanceDao, QWAccount account, QWToken token) {
        List<QWBalance> list = balanceDao.queryTokenAllByWT(account, token);
        if (list != null && !list.isEmpty()) {
            for (QWBalance balance : list) {
                if (!TextUtils.isEmpty(balance.getBalance()) && Numeric.toBigInt(balance.getBalance()).compareTo(BigInteger.ZERO) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public void checkBalance(Context context, QWWallet wallet) {
        disposable2 = Single.fromCallable(() -> {
            QWAccountDao dao = new QWAccountDao(context);
            List<QWAccount> list = dao.queryByKey(wallet.getKey());
            if (list != null) {
                for (QWAccount account : list) {
                    //主币是否有余额
                    if (account.getBalances() != null) {
                        for (QWBalance balance : account.getBalances()) {
                            if (!TextUtils.isEmpty(balance.getBalance()) && Numeric.toBigInt(balance.getBalance()).compareTo(BigInteger.ZERO) > 0) {
                                return true;
                            }
                        }
                    }

                    //erc trc 等是否有余额
                    boolean otherHasBalance = otherTokenHasBalance(context, account);
                    if (otherHasBalance) {
                        return true;
                    }
                }
            }
            return false;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onWalletBalanceSuccess, v -> onWalletBalanceError());
    }

    private void onWalletBalanceSuccess(boolean state) {
        mWalletHasBalance.postValue(state);
    }

    private void onWalletBalanceError() {
        mWalletHasBalance.postValue(false);
    }

    public MutableLiveData<Boolean> walletHasBalance() {
        return mWalletHasBalance;
    }

    //获取余额
    public void checkBalance(Context context, QWAccount qwAccount) {
        disposable = Single.fromCallable(() -> {
            QWAccountDao dao = new QWAccountDao(context);
            QWAccount account = dao.queryByAddress(qwAccount.getAddress());
            //主币是否有余额
            if (account.getBalances() != null) {
                for (QWBalance balance : account.getBalances()) {
                    if (!TextUtils.isEmpty(balance.getBalance()) && Numeric.toBigInt(balance.getBalance()).compareTo(BigInteger.ZERO) > 0) {
                        return true;
                    }
                }
            }
            //erc20 trc20 等是否有余额
            if (otherTokenHasBalance(context, account)) {
                return true;
            }
            return false;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAccountBalanceSuccess, v -> onAccountBalanceError());
    }

    private void onAccountBalanceSuccess(boolean state) {
        mAccountHasBalance.postValue(state);
    }

    private void onAccountBalanceError() {
        mAccountHasBalance.postValue(false);
    }

    public MutableLiveData<Boolean> accountHasBalance() {
        return mAccountHasBalance;
    }
    //************************校验金额************************

}
