package com.quarkonium.qpocket.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.entity.ServiceException;
import com.quarkonium.qpocket.crypto.CreateWalletException;
import com.quarkonium.qpocket.crypto.ECKeyPair;
import com.quarkonium.qpocket.crypto.Hash;
import com.quarkonium.qpocket.crypto.RawTransaction;
import com.quarkonium.qpocket.crypto.Sign;
import com.quarkonium.qpocket.crypto.TransactionEncoder;
import com.quarkonium.qpocket.crypto.Wallet;
import com.quarkonium.qpocket.crypto.WalletFile;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;

import org.ethereum.geth.Accounts;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;
import org.json.JSONObject;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class GethKeystoreAccountService implements AccountKeystoreService {
    private static final int PRIVATE_KEY_RADIX = 16;
    /**
     * CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than 2^(128 * r / 8).
     */
    private static final int N = 1 << 9;
    /**
     * Parallelization parameter. Must be a positive integer less than or equal to Integer.MAX_VALUE / (128 * r * 8).
     */
    private static final int P = 1;

    private final KeyStore keyStore;

    public GethKeystoreAccountService(File keyStoreFile) {
        keyStore = new KeyStore(keyStoreFile.getAbsolutePath(), Geth.LightScryptN, Geth.LightScryptP);
    }

    //创建钱包并生成钱包地址
    @Override
    public Single<QWAccount> createAccount(int HDPathCode, int accountIndex, String mnemonic, String password) {
        return Single.fromCallable(() -> {
            ECKeyPair childEcKeyPair = WalletUtils.generateKeyPair(HDPathCode, accountIndex, mnemonic);
            //创建钱包
            WalletFile walletFile = Wallet.createLight(password, childEcKeyPair);
            return new ObjectMapper().writeValueAsString(walletFile);
        }).flatMap(keystore -> importKeystore(keystore, password, password))
                .subscribeOn(Schedulers.newThread());
    }

    //导入私钥 并生成钱包
    @Override
    public Single<QWAccount> importPrivateKey(String privateKey, String newPassword) {
        return Single.fromCallable(() -> {
            String privateKeyEth = Numeric.cleanHexPrefix(privateKey);
            BigInteger key = new BigInteger(privateKeyEth, PRIVATE_KEY_RADIX);
            ECKeyPair keypair = ECKeyPair.create(key);
            WalletFile walletFile = Wallet.create(newPassword, keypair, N, P);
            return new ObjectMapper().writeValueAsString(walletFile);
        }).flatMap(wallet -> importKeystore(wallet, newPassword, newPassword));
    }

    //导入keystore 并生成钱包
    @Override
    public Single<QWAccount> importKeystore(String store, String password, String newPassword) {
        return Single.fromCallable(() -> {
                    JSONObject jsonObject = new JSONObject(store);
                    String address = jsonObject.getString("address");

                    //是否存在观察钱包
                    String addressTemp = Numeric.prependHexPrefix(address).toLowerCase();
                    QWAccountDao dao = new QWAccountDao(MainApplication.getContext());
                    String temp = Numeric.parseAddressToEth(addressTemp);
                    if (dao.hasExist(temp)) {
                        throw new CreateWalletException(R.string.import_wallet_fail_exit);
                    }
                    temp = Numeric.parseAddressToQuark(addressTemp);
                    if (dao.hasExist(temp)) {
                        throw new CreateWalletException(R.string.import_wallet_fail_exit);
                    }

                    org.ethereum.geth.Account account = keyStore.importKey(store.getBytes(StandardCharsets.UTF_8), password, newPassword);
                    String walletAddress = account.getAddress().getHex().toLowerCase();
                    return new QWAccount(walletAddress);
                }
        );
    }

    //导出keystore
    @Override
    public Single<String> exportKeystore(QWAccount wallet, String password, String newPassword) {
        return Single.fromCallable(() -> findAccountByQuarkAddress(wallet.getAddress()))
                .map(account1 -> new String(keyStore.exportKey(account1, password, newPassword)))
                .subscribeOn(Schedulers.io());
    }

    //导出私钥
    @Override
    public Single<String> exportPrivateKey(QWAccount wallet, String password, String newPassword) {
        return exportKeystore(wallet, password, newPassword)
                .map(keystore -> WalletUtils.getPrivateKeyByKeyStore(password, keystore))
                .subscribeOn(Schedulers.io());
    }

    //删除钱包
    @Override
    public Single<String> deleteAccount(String address, String password) {
        return Single.fromCallable(() -> {
            org.ethereum.geth.Account account = findAccountByQuarkAddress(address);
            keyStore.deleteAccount(account, password);
            return address;
        })
                .subscribeOn(Schedulers.io());
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
        return Single.fromCallable(() -> {

            String fromAddress = fromQWAddress.substring(0, fromQWAddress.length() - 8);
            String fromShardStr = fromQWAddress.substring(fromQWAddress.length() - 8);
            BigInteger fromShard = Numeric.toBigInt(fromShardStr);

            String toAddress = toQWAddress.substring(0, toQWAddress.length() - 8);
            String toShardStr = toQWAddress.substring(toQWAddress.length() - 8);
            BigInteger toShard = Numeric.toBigInt(toShardStr);

            //做hash算法
            RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, amount, fromShard, toShard,
                    data, networkId, transferToken, gasToken);
            byte[] encoded = TransactionEncoder.encode(rawTransaction);
            byte[] hashed = Hash.sha3(encoded);

            org.ethereum.geth.Account gethAccount = findAccountByQuarkAddress(fromAddress);
            keyStore.unlock(gethAccount, signerPassword);
            byte[] signed = keyStore.signHash(gethAccount.getAddress(), hashed);
            keyStore.lock(gethAccount.getAddress());

            //通过signed生成SignatureData
            Sign.SignatureData signatureData = parseSignatureData(signed, Numeric.toBigInt(Constant.DEFAULT_CHINA_ID));

            //在根据SignatureData做hash
            encoded = TransactionEncoder.encode(rawTransaction, BigInteger.ZERO, signatureData);
            hashed = Hash.sha3(encoded);

            return new String[]{
                    Numeric.toHexString(hashed), Numeric.toHexString(encoded)
            };
        }).subscribeOn(Schedulers.io());
    }


    @Override
    public boolean hasAccount(String address) {
        String ethAddress = Numeric.parseAddressToEth(address);
        return keyStore.hasAddress(new Address(ethAddress));
    }

    private org.ethereum.geth.Account findAccountByQuarkAddress(String address) throws ServiceException {
        //转为eth地址
        address = Numeric.parseAddressToEth(address);

        Accounts accounts = keyStore.getAccounts();
        int len = (int) accounts.size();
        for (int i = 0; i < len; i++) {
            try {
                if (accounts.get(i).getAddress().getHex().equalsIgnoreCase(address)) {
                    return accounts.get(i);
                }
            } catch (Exception ex) {
                /* Quietly: interest only result, maybe next is ok. */
            }
        }
        throw new ServiceException("Wallet with address: " + address + " not found");
    }

    private Sign.SignatureData parseSignatureData(byte[] singed, BigInteger chainId) {
        byte[] r = Arrays.copyOfRange(singed, 0, 32);
        byte[] s = Arrays.copyOfRange(singed, 32, 64);
        byte v = (byte) (singed[64] + 27);
        if (chainId.longValue() > 0) {
            v = (byte) (singed[64] + 35 + chainId.intValue() + chainId.intValue());
        }
        return new Sign.SignatureData(v, r, s);
    }

    private org.ethereum.geth.Account findAccount(String address) throws ServiceException {
        Accounts accounts = keyStore.getAccounts();
        int len = (int) accounts.size();
        for (int i = 0; i < len; i++) {
            try {
                if (accounts.get(i).getAddress().getHex().equalsIgnoreCase(address)) {
                    return accounts.get(i);
                }
            } catch (Exception ex) {
                /* Quietly: interest only result, maybe next is ok. */
            }
        }
        throw new ServiceException("Wallet with address: " + address + " not found");
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

        return Single.fromCallable(() -> {
            BigInt value = new BigInt(0);
            value.setString(amount.toString(), 10);

            BigInt gasPriceBI = new BigInt(0);
            gasPriceBI.setString(gasPrice.toString(), 10);

            BigInt gasLimitBI = new BigInt(0);
            gasLimitBI.setString(gasLimit.toString(), 10);

            Transaction tx = new Transaction(
                    nonce,
                    new Address(toAddress),
                    value,
                    gasLimitBI,
                    gasPriceBI,
                    data);

            BigInt chain = new BigInt(chainId); // Chain identifier of the main net
            org.ethereum.geth.Account gethAccount = findAccount(fromAddress);
            keyStore.unlock(gethAccount, signerPassword);
            Transaction signed = keyStore.signTx(gethAccount, tx, chain);
            keyStore.lock(gethAccount.getAddress());

            return signed.encodeRLP();
        }).subscribeOn(Schedulers.io());
    }
}
