package com.quarkonium.qpocket.api.service;

import com.quarkonium.qpocket.api.db.table.QWAccount;

import java.math.BigInteger;

import io.reactivex.Single;


public interface AccountKeystoreService {
    /**
     * Create account in keystore
     *
     * @param mnemonic account mnemonic
     * @return new {@link QWAccount}
     */
    Single<QWAccount> createAccount(int HDCode, int accountIndex, String mnemonic, String password);

    /**
     * Include new existing keystore
     *
     * @param store    store to include
     * @param password store password
     * @return included {@link QWAccount} if success
     */
    Single<QWAccount> importKeystore(String store, String password, String newPassword);

    Single<QWAccount> importPrivateKey(String privateKey, String newPassword);

    /**
     * Export wallet to keystore
     *
     * @param wallet      wallet to export
     * @param password    password from wallet
     * @param newPassword new password to store
     * @return store data
     */
    Single<String> exportKeystore(QWAccount wallet, String password, String newPassword);

    //导出私钥
    Single<String> exportPrivateKey(QWAccount wallet, String password, String newPassword);

    /**
     * Delete account from keystore
     *
     * @param address  account address
     * @param password account password
     */
    Single<String> deleteAccount(String address, String password);

    /**
     * Sign transaction
     *
     * @param fromQWAddress  {@link QWAccount address}
     * @param signerPassword password from {@link QWAccount}
     * @param toQWAddress    transaction destination QuarkWallet address
     */
    Single<String[]> signTransaction(
            String fromQWAddress, String signerPassword,
            BigInteger nonce,
            BigInteger gasPrice, BigInteger gasLimit,
            String toQWAddress,
            BigInteger amount,
            String data,
            BigInteger chainId,
            BigInteger transferToken, BigInteger gasToken);

    /**
     * Check if there is an address in the keystore
     *
     * @param address {@link QWAccount} address
     */
    boolean hasAccount(String address);


    /**
     * Sign transaction
     *
     * @param toAddress transaction destination address
     * @return sign data
     */
    Single<byte[]> signEthTransaction(
            String fromAddress,
            String signerPassword,
            String toAddress,
            BigInteger amount,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            byte[] data,
            long chainId);
}
