package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.entity.ServiceException;
import com.quarkonium.qpocket.api.service.AccountKeystoreService;
import com.quarkonium.qpocket.crypto.Hash;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.Web3jFactory;
import com.quarkonium.qpocket.jsonrpc.protocol.core.DefaultBlockParameterName;
import com.quarkonium.qpocket.jsonrpc.protocol.http.HttpService;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.GasLimitForBuyRequest;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.GasLimitRequest;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.Transaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthSendTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetAccountData;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetTransactions;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCNetworkInfo;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCSendRawTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TransactionDetail;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TransactionReceipt;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

//交易事务处理
public class TransactionRepository implements TransactionRepositoryType {

    private final AccountKeystoreService mAccountKeystoreService;

    public TransactionRepository(AccountKeystoreService accountKeystoreService) {
        mAccountKeystoreService = accountKeystoreService;
    }

    @Override
    public Single<QKCNetworkInfo.NetworkInfo> networkInfoSuccess() {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sQKCNetworkPath, false))
                .networkInfoSuccess()
                .send()
                .getQKCNetworkInfo());
    }

    @Override
    public Single<QKCGetAccountData.AccountData> getAccountData(String address) {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sQKCNetworkPath, false))
                .getAccountData(address)
                .send()
                .getQKCGetAccountData())
                .subscribeOn(Schedulers.io());

    }

    @Override
    public Single<QKCGetTransactions.TransactionData> fetchTransaction(String address, String start, String limit) {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sQKCNetworkPath, false))
                .getTransactionsByAddress(address, start, limit)
                .send()
                .getTransactionData())
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<QKCGetTransactions.TransactionData> fetchTransaction(String address, String tokenId, String start, String limit) {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sQKCNetworkPath, false))
                .getTransactionsByAddress(address, tokenId, start, limit)
                .send()
                .getTransactionData())
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<TransactionDetail> findTransaction(String transactionId) {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sQKCNetworkPath, false))
                .getTransactionById(transactionId)
                .send()
                .getTransaction())
                .subscribeOn(Schedulers.newThread());
    }

    @Override
    public Single<TransactionReceipt> findTransactionReceipt(String transactionId) {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sQKCNetworkPath, false))
                .getTransactionReceipt(transactionId)
                .send()
                .getTransaction())
                .subscribeOn(Schedulers.newThread());
    }

    @Override
    public Single<BigInteger> getQKCNonce(String fromAddress) {
        return Single.fromCallable(() -> {
            //获取nonce
            return Web3jFactory.build(new HttpService(Constant.sQKCNetworkPath))
                    .getTransactionCount(fromAddress)
                    .send()
                    .getTransactionCount();
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String[]> createTransaction(String fromAddress, String signerPassword,
                                              BigInteger gasPrice, BigInteger gasLimit,
                                              String toAddress, BigInteger amount,
                                              String data,
                                              BigInteger networkId,
                                              BigInteger transferToken, BigInteger gasToken,
                                              BigInteger nonce) {
        return mAccountKeystoreService.signTransaction(fromAddress, signerPassword,
                nonce,
                gasPrice, gasLimit,
                toAddress, amount,
                data, networkId,
                transferToken, gasToken
        ).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> sendTransaction(String hash) {
        return Single.fromCallable(() -> {
            QKCSendRawTransaction raw = Web3jFactory.build(new HttpService(Constant.sQKCNetworkPath, true))
                    .sendRawTransaction(hash)
                    .send();
            if (raw.hasError()) {
                throw new ServiceException(raw.getError().getMessage());
            }
            return raw.getTransactionHash();
        });
    }

    //获取gas价格
    @Override
    public Single<String> gasPrice(String shard) {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sQKCNetworkPath, false))
                .gasPrice(shard)
                .send()
                .getValue())
                .subscribeOn(Schedulers.io());
    }

    //获取gas上限
    @Override
    public Single<String> gasLimit(String fromAddress, String toAddress, String transferTokenId, String gasTokenId) {
        return Single.fromCallable(() -> {
                    GasLimitRequest request = new GasLimitRequest();
                    request.setTo(toAddress);
                    request.setFrom(fromAddress);
                    request.setTransferTokenId(transferTokenId);
                    request.setGasTokenId(gasTokenId);
                    request.setData("0x");

                    return Web3jFactory
                            .build(new HttpService(Constant.sQKCNetworkPath, false))
                            .gasLimit(request)
                            .send()
                            .getValue();
                }
        );
    }

    public Single<String> gasLimitSendToken(Context context, String fromAddress, String toAddress, String contractAddress, BigInteger amount,
                                            String transferTokenId, String gasTokenId) {
        return Single.fromCallable(() -> {
                    GasLimitRequest request = new GasLimitRequest();
                    request.setTo(contractAddress);

                    request.setFrom(fromAddress);

                    request.setTransferTokenId(transferTokenId);
                    request.setGasTokenId(gasTokenId);

                    String to = Numeric.parseAddressToEth(toAddress);
                    final byte[] temp = TokenRepository.createTokenTransferData(to, amount);
                    String data = Numeric.toHexStringWithPrefix(temp);

                    request.setData(data);

                    return Web3jFactory
                            .build(new HttpService(Constant.sQKCNetworkPath, false))
                            .gasLimit(request)
                            .send()
                            .getValue();
                }
        );
    }

    @Override
    public Single<String> gasLimitForBuy(Context context, String fromAddress, String toAddress, BigInteger amount, String transferTokenId, String gasTokenId) {
        return Single.fromCallable(() -> {
                    GasLimitForBuyRequest request = new GasLimitForBuyRequest();
                    request.setTo(toAddress);

                    request.setFrom(fromAddress);

                    request.setValue(Numeric.toHexStringWithPrefix(amount));
                    request.setTransferTokenId(transferTokenId);
                    request.setGasTokenId(gasTokenId);

                    final byte[] buyData = TokenRepository.createBuyTokenTransferData();
                    String data = Numeric.toHexStringWithPrefix(buyData);
                    request.setData(data);

                    return Web3jFactory
                            .build(new HttpService(Constant.sQKCNetworkPath, false))
                            .gasLimitForBuy(request)
                            .send()
                            .getValue();

                }
        );
    }

    @Override
    public Single<BigInteger> getETHNonce(String fromAddress) {
        return Single.fromCallable(() -> {
            //获取nonce
            return Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false))
                    .ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                    .send()
                    .getTransactionCount();
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String[]> createEthTransaction(String from, String toAddress,
                                                 BigInteger subunitAmount,
                                                 BigInteger gasPrice, BigInteger gasLimit,
                                                 byte[] data, String password,
                                                 BigInteger nonce) {
        return mAccountKeystoreService.signEthTransaction(from, password, toAddress,
                subunitAmount, gasPrice, gasLimit,
                nonce.longValue(), data, Constant.sETHNetworkId)
                .map(encoded -> {
                            byte[] hashed = Hash.sha3(encoded);
                            return new String[]{Numeric.toHexString(hashed), Numeric.toHexString(encoded)};
                        }
                ).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> sendEthTransaction(String hash) {
        return Single.fromCallable(() -> {
            EthSendTransaction raw = Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false))
                    .ethSendRawTransaction(hash)
                    .send();
            if (raw.hasError()) {
                throw new ServiceException(raw.getError().getMessage());
            }
            return raw.getTransactionHash();
        });
    }

    @Override
    public Single<String> ethEstimateGas(String fromAddress, String toAddress) {
        return Single.fromCallable(() -> {
            Transaction transaction = Transaction.createEthCallTransaction(fromAddress, toAddress, "0x");
            BigInteger gas = Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false))
                    .ethEstimateGas(transaction)
                    .send()
                    .getAmountUsed();
            return Numeric.toHexStringWithPrefix(gas);
        });
    }

    public Single<String> ethGasLimitSendToken(String fromAddress, String toAddress, String contractAddress, BigInteger amount) {
        return Single.fromCallable(() -> {
                    final byte[] temp = TokenRepository.createTokenTransferData(toAddress, amount);
                    String data = Numeric.toHexStringWithPrefix(temp);

                    Transaction transaction = Transaction.createEthCallTransaction(fromAddress, contractAddress, data);
                    BigInteger gas = Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false))
                            .ethEstimateGas(transaction)
                            .send()
                            .getAmountUsed();
                    return Numeric.toHexStringWithPrefix(gas);
                }
        );
    }

    @Override
    public Single<String> ethGasLimitForBuy(String fromAddress, String toAddress, BigInteger amount) {
        return Single.fromCallable(() -> {
                    final byte[] buyData = TokenRepository.createBuyTokenTransferData();
                    String data = Numeric.toHexStringWithPrefix(buyData);

                    Transaction transaction = Transaction.createEthBuyCallTransaction(fromAddress, toAddress, amount, data);
                    BigInteger gas = Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false))
                            .ethEstimateGas(transaction)
                            .send()
                            .getAmountUsed();
                    return Numeric.toHexStringWithPrefix(gas);
                }
        );
    }
}
