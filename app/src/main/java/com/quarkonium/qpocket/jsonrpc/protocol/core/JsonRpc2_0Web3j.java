package com.quarkonium.qpocket.jsonrpc.protocol.core;

import com.quarkonium.qpocket.jsonrpc.protocol.Web3j;
import com.quarkonium.qpocket.jsonrpc.protocol.Web3jService;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.GasLimitForBuyRequest;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.GasLimitRequest;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.TokenBalance;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.Transaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthCall;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthEstimateGas;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthGasPrice;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthGetBalance;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthGetTransactionCount;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthSendTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetAccountData;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetTransactionCount;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetTransactionReceipt;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetTransactions;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCNetworkInfo;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCSendRawTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QuarkCall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/**
 * JSON-RPC 2.0 factory implementation.
 */
public class JsonRpc2_0Web3j implements Web3j {

    private final Web3jService web3jService;

    public JsonRpc2_0Web3j(Web3jService web3jService) {
        this.web3jService = web3jService;
    }

    //分片总数
    @Override
    public Request<?, QKCNetworkInfo> networkInfoSuccess() {
        return new Request<>(
                "networkInfo",
                new ArrayList<>(),
                web3jService,
                QKCNetworkInfo.class);
    }

    //获取所有分片下qkc数量 @YES
    @Override
    public Request<?, QKCGetAccountData> getAccountData(String address) {
        return new Request<>(
                "getAccountData",
                Arrays.asList(address, "latest", true),
                web3jService,
                QKCGetAccountData.class);
    }


    //获取该地址发送交易数量
    @Override
    public Request<?, QKCGetTransactionCount> getTransactionCount(String address) {
        return new Request<>(
                "getTransactionCount",
                Arrays.asList(address),
                web3jService,
                QKCGetTransactionCount.class);
    }

    @Override
    public Request<?, QKCSendRawTransaction> sendRawTransaction(String signedTransactionData) {
        return new Request<>(
                "sendRawTransaction",
                Arrays.asList(signedTransactionData),
                web3jService,
                QKCSendRawTransaction.class);
    }

    //通过地址获得所有交易详情
    @Override
    public Request<?, QKCGetTransactions> getTransactionsByAddress(String address, String start, String limit) {
        return new Request<>(
                "getTransactionsByAddress",
                Arrays.asList(address, start, limit),
                web3jService,
                QKCGetTransactions.class);
    }

    //通过地址获得所有交易详情
    @Override
    public Request<?, QKCGetTransactions> getTransactionsByAddress(String address, String tokenId, String start, String limit) {
        return new Request<>(
                "getTransactionsByAddress",
                Arrays.asList(address, start, limit, tokenId),
                web3jService,
                QKCGetTransactions.class);
    }

    //根据id获取交易记录
    @Override
    public Request<?, QKCGetTransaction> getTransactionById(String transactionId) {
        return new Request<>(
                "getTransactionById",
                Arrays.asList(transactionId),
                web3jService,
                QKCGetTransaction.class);
    }

    //获取收据
    @Override
    public Request<?, QKCGetTransactionReceipt> getTransactionReceipt(String transactionId) {
        return new Request<>(
                "getTransactionReceipt",
                Arrays.asList(transactionId),
                web3jService,
                QKCGetTransactionReceipt.class);
    }

    //获取toke代表符号 如ADA，AE
//    @Override
//    public CallRequest<?, QuarkCall> call(TokenBalance signedTransactionData) {
//        return new CallRequest<Object, QuarkCall>(
//                "call",
//                signedTransactionData,
//                web3jService,
//                QuarkCall.class);
//    }
    @Override
    public Request<?, QuarkCall> call(TokenBalance signedTransactionData) {
        return new Request<>(
                "call",
                Arrays.asList(signedTransactionData, "latest"),
                web3jService,
                QuarkCall.class);
    }

    @Override
    public Request<?, QuarkCall> gasPrice(String shard) {
        return new Request<>(
                "gasPrice",
                Arrays.asList(shard),
                web3jService,
                QuarkCall.class);
    }

    @Override
    public Request<?, QuarkCall> gasLimit(GasLimitRequest bean) {
        return new Request<>(
                "estimateGas",
                Arrays.asList(bean),
                web3jService,
                QuarkCall.class);
    }

    @Override
    public Request<?, QuarkCall> gasLimitForBuy(GasLimitForBuyRequest request) {
        return new Request<>(
                "estimateGas",
                Arrays.asList(request),
                web3jService,
                QuarkCall.class);
    }


    //*******************eth********************
    //获取eth余额
    @Override
    public Request<?, EthGetBalance> ethGetBalance(
            String address, DefaultBlockParameter defaultBlockParameter) {
        return new Request<>(
                "eth_getBalance",
                Arrays.asList(address, defaultBlockParameter.getValue()),
                web3jService,
                EthGetBalance.class);
    }

    @Override
    public Request<?, EthCall> ethCall(Transaction transaction, DefaultBlockParameter defaultBlockParameter) {
        return new Request<>(
                "eth_call",
                Arrays.asList(transaction, defaultBlockParameter),
                web3jService,
                EthCall.class);
    }

    @Override
    public Request<?, EthGetTransactionCount> ethGetTransactionCount(String address, DefaultBlockParameter defaultBlockParameter) {
        return new Request<>(
                "eth_getTransactionCount",
                Arrays.asList(address, defaultBlockParameter.getValue()),
                web3jService,
                EthGetTransactionCount.class);
    }

    @Override
    public Request<?, EthSendTransaction>
    ethSendRawTransaction(
            String signedTransactionData) {
        return new Request<>(
                "eth_sendRawTransaction",
                Arrays.asList(signedTransactionData),
                web3jService,
                EthSendTransaction.class);
    }

    @Override
    public Request<?, EthGasPrice> ethGasPrice() {
        return new Request<String, EthGasPrice>(
                "eth_gasPrice",
                Collections.emptyList(),
                web3jService,
                EthGasPrice.class);
    }

    @Override
    public Request<?, EthEstimateGas> ethEstimateGas(Transaction transaction) {
        return new Request<>(
                "eth_estimateGas",
                Arrays.asList(transaction),
                web3jService,
                EthEstimateGas.class);
    }
}
