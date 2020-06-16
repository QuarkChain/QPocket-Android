package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetAccountData;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetTransactions;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCNetworkInfo;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TransactionDetail;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TransactionReceipt;

import java.math.BigInteger;

import io.reactivex.Single;

public interface TransactionRepositoryType {

    //分片总数
    Single<QKCNetworkInfo.NetworkInfo> networkInfoSuccess();

    //获取地址下qkc数量 @YES
    Single<QKCGetAccountData.AccountData> getAccountData(String address);


    //获取地址下交易记录
    Single<QKCGetTransactions.TransactionData> fetchTransaction(String address, String start, String limit);

    Single<QKCGetTransactions.TransactionData> fetchTransaction(String address, String tokenId, String start, String limit);

    Single<TransactionDetail> findTransaction(String transactionId);

    Single<TransactionReceipt> findTransactionReceipt(String transactionId);

    Single<BigInteger> getQKCNonce(String fromAddress);

    //创建交易transaction
    Single<String[]> createTransaction(String fromAddress, String signerPassword,
                                       BigInteger gasPrice, BigInteger gasLimit,
                                       String toAddress, BigInteger amount,
                                       String data,
                                       BigInteger networkId,
                                       BigInteger transferToken, BigInteger gasToken,
                                       BigInteger nonce);

    //发送交易transaction
    Single<String> sendTransaction(String hash);

    //获取gas价格
    Single<String> gasPrice(String shard);

    //获取gas上限
    Single<String> gasLimit(String fromAddress, String toAddress, String transferTokenId, String gasTokenId);

    //获取gas上限 转账Token
    Single<String> gasLimitSendToken(Context context, String fromAddress, String toAddress, String contractAddress,
                                     BigInteger amount, String transferTokenId, String gasTokenId);

    //获取gas上限 购买Token
    Single<String> gasLimitForBuy(Context context, String fromAddress, String toAddress, BigInteger amount, String transferTokenId, String gasTokenId);


    //*************eth*********************
    Single<BigInteger> getETHNonce(String fromAddress);

    Single<String[]> createEthTransaction(String from, String toAddress, BigInteger subunitAmount,
                                          BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password, BigInteger nonce);

    Single<String> sendEthTransaction(String hash);

    Single<String> ethEstimateGas(String fromAddress, String toAddress);

    Single<String> ethGasLimitSendToken(String fromAddress, String toAddress, String contractAddress, BigInteger amount);

    Single<String> ethGasLimitForBuy(String fromAddress, String toAddress, BigInteger amount);
}
