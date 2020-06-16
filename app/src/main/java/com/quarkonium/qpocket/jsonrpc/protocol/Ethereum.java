package com.quarkonium.qpocket.jsonrpc.protocol;

import com.quarkonium.qpocket.jsonrpc.protocol.core.DefaultBlockParameter;
import com.quarkonium.qpocket.jsonrpc.protocol.core.Request;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.Transaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthCall;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthEstimateGas;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthGasPrice;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthGetBalance;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthGetTransactionCount;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthSendTransaction;

public interface Ethereum {

    //获取eth余额
    Request<?, EthGetBalance> ethGetBalance(String address, DefaultBlockParameter defaultBlockParameter);

    Request<?, EthCall> ethCall(Transaction transaction, DefaultBlockParameter defaultBlockParameter);

    Request<?, EthGetTransactionCount> ethGetTransactionCount(String address, DefaultBlockParameter defaultBlockParameter);

    Request<?, EthSendTransaction> ethSendRawTransaction(String signedTransactionData);

    Request<?, EthGasPrice> ethGasPrice();

    Request<?, EthEstimateGas> ethEstimateGas(Transaction transaction);
}
