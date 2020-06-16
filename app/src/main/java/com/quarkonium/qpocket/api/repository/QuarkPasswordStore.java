package com.quarkonium.qpocket.api.repository;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.entity.ServiceErrorException;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.util.KS;
import com.quarkonium.qpocket.util.PasswordManager;

import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;

public class QuarkPasswordStore implements PasswordStore {

    private final Context context;

    public QuarkPasswordStore(Context context) {
        this.context = context;
    }

    //读取助记词
    @Override
    public Single<String> getMnemonic(QWWallet wallet) {
        return Single.fromCallable(() -> {
            try {
                byte[] data = KS.get(context, wallet.getKey());
                return new String(data);
            } catch (Exception e) {
                throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
            }
        });
    }

    @Override
    public Single<String> getPassword(QWWallet wallet) {
        return Single.fromCallable(() -> {
            try {
                byte[] data = KS.getPD(context, wallet.getKey());
                return new String(data);
            } catch (Exception e) {
                throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
            }
        });
    }

    //存储钱包助记词和地址
    @Override
    public Completable setMnemonic(QWWallet wallet, String mnemonic, String password) {
        return Completable.fromAction(() -> {
            if (!TextUtils.isEmpty(mnemonic)) {
                KS.put(context, wallet.getKey(), mnemonic);
            }
            KS.putPD(context, wallet.getKey(), password);
        });
    }

    //生成初始助记词
    @Override
    public Single<String> generateMnemonic() {
        return Single.fromCallable(() -> WalletUtils.createNewMnemonic(context));
    }

    @Override
    public Single<String> changeMnemonic(Context context, String mnemonic, Locale locale) {
        return Single.fromCallable(() -> WalletUtils.changeMnemonic(context, mnemonic, locale));
    }

    @Override
    public void importKeyStore(String keyStore, String address, String password, String newPassword) throws ServiceErrorException {
        KS.putKeystore(context, address, keyStore);
    }

    @Override
    public String exportKeyStore(String address, String password) throws ServiceErrorException {
        try {
            byte[] data = KS.getKeystore(context, address, password);
            return new String(data);
        } catch (Exception e) {
            throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
        }
    }

    @Override
    public void importPrivateKey(String key, String pv, String password) throws ServiceErrorException {
        KS.putPrivateKey(context, key, pv);
    }

    @Override
    public String exportPrivateKey(String key, String password) throws ServiceErrorException {
        try {
            byte[] data = KS.getPrivateKey(context, key, password);
            return new String(data);
        } catch (Exception e) {
            throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
        }
    }

    //删除钱包
    @Override
    public void deleteAccount(String address, String password) throws ServiceErrorException {
        //校验密码
        String keystore = exportKeyStore(address, password);
        if (TextUtils.isEmpty(keystore)) {
            throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KS.removeKeystoreWallet(context, address);
        } else {
            PasswordManager.removeKeystoreWallet(context, address);
        }
    }
}
