package com.quarkonium.qpocket.tron;

import android.content.Context;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.QuarkPasswordStore;
import com.quarkonium.qpocket.api.service.AccountKeystoreService;
import com.quarkonium.qpocket.crypto.CreateWalletException;
import com.quarkonium.qpocket.crypto.ECKeyPair;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.tron.crypto.ECKey;
import com.quarkonium.qpocket.tron.keystore.CipherException;
import com.quarkonium.qpocket.tron.keystore.Wallet;
import com.quarkonium.qpocket.tron.keystore.WalletFile;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class TronKeystoreAccountService implements AccountKeystoreService {
    private static final int PRIVATE_KEY_RADIX = 16;
    private PasswordStore mPasswordStore;


    public TronKeystoreAccountService(Context context) {
        mPasswordStore = new QuarkPasswordStore(context);
    }

    //创建tron钱包
    @Override
    public Single<QWAccount> createAccount(int HDCode, int accountIndex, String mnemonic, String password) {
        return Single.fromCallable(() -> {
            //生成hd钱包私钥
            ECKeyPair childEcKeyPair = WalletUtils.generateKeyPair(HDCode, accountIndex, mnemonic);
            return Numeric.toHexStringNoPrefixZeroPadded(childEcKeyPair.getPrivateKey(), 64);
        }).flatMap(privateKey -> importPrivateKey(privateKey, password))
                .subscribeOn(Schedulers.newThread());
    }

    //导入私钥 并生成钱包
    @Override
    public Single<QWAccount> importPrivateKey(String privateKey, String newPassword) {
        return Single.fromCallable(() -> {
            //生成keystore
            String privateKeyEth = Numeric.cleanHexPrefix(privateKey);
            BigInteger key = new BigInteger(privateKeyEth, PRIVATE_KEY_RADIX);
            ECKey keypair = ECKey.fromPrivate(key);
            WalletFile walletFile = Wallet.createLight(newPassword, keypair);
            return new ObjectMapper().writeValueAsString(walletFile);
        }).flatMap(wallet -> importKeystore(wallet, newPassword, newPassword));
    }


    //导入keystore 并生成钱包
    @Override
    public Single<QWAccount> importKeystore(String store, String password, String newPassword) {
        //tron钱包
        return Single.fromCallable(() -> {
            //校验是否合法
            String address = checkKeyStore(store, password);
            //是否存在
            QWAccountDao dao = new QWAccountDao(MainApplication.getContext());
            if (dao.hasExist(address)) {
                throw new CreateWalletException(R.string.import_wallet_fail_exit);
            }

            //存储keystore
            mPasswordStore.importKeyStore(store, address, password, newPassword);
            return new QWAccount(address);
        });
    }

    //校验是否合法
    private String checkKeyStore(String store, String password) throws CipherException {
        //转为私钥
        String privateKey = TronWalletClient.getPrivateKeyByKeyStore(password, store);
        if (TextUtils.isEmpty(privateKey)) {
            throw new CipherException("Unable to deserialize params: " + store);
        }
        return TronWalletClient.privateKeyToAddress(privateKey);
    }

    //导出私钥
    @Override
    public Single<String> exportPrivateKey(QWAccount wallet, String password, String newPassword) {
        return exportKeystore(wallet, password, newPassword)
                .map(keystore -> TronWalletClient.getPrivateKeyByKeyStore(password, keystore))
                .subscribeOn(Schedulers.io());
    }

    //导出keystore
    @Override
    public Single<String> exportKeystore(QWAccount wallet, String password, String newPassword) {
        return Single.fromCallable(() ->
                mPasswordStore.exportKeyStore(wallet.getAddress(), password)
        ).subscribeOn(Schedulers.io());
    }


    //删除钱包
    @Override
    public Single<String> deleteAccount(String address, String password) {
        return Single.fromCallable(() -> {
            mPasswordStore.deleteAccount(address, password);
            return address;
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String[]> signTransaction(String fromQWAddress, String signerPassword,
                                            BigInteger nonce,
                                            BigInteger gasPrice, BigInteger gasLimit,
                                            String toQWAddress,
                                            BigInteger amount,
                                            String data,
                                            BigInteger networkId,
                                            BigInteger transferToken, BigInteger gasToken) {
        return null;
    }

    @Override
    public boolean hasAccount(String address) {
        return false;
    }

    @Override
    public Single<byte[]> signEthTransaction(String fromAddress,
                                             String signerPassword,
                                             String toAddress,
                                             BigInteger amount,
                                             BigInteger gasPrice,
                                             BigInteger gasLimit,
                                             long nonce,
                                             byte[] data,
                                             long chainId) {
        return null;
    }
}
