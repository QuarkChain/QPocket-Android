package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.entity.ServiceErrorException;

import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;


public interface PasswordStore {
    Single<String> getMnemonic(QWWallet wallet);

    Single<String> getPassword(QWWallet wallet);

    Completable setMnemonic(QWWallet wallet, String phrase, String password);

    Single<String> generateMnemonic();

    Single<String> changeMnemonic(Context context, String mnemonic, Locale locale);

    void importKeyStore(String keyStore, String address, String password, String newPassword) throws ServiceErrorException;

    String exportKeyStore(String address, String password) throws ServiceErrorException;

    void importPrivateKey(String key, String pv, String password) throws ServiceErrorException;

    String exportPrivateKey(String key, String password) throws ServiceErrorException;

    void deleteAccount(String address, String password) throws ServiceErrorException;
}
