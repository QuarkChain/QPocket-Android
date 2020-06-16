package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxAllTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxTransaction;

import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import io.reactivex.Single;

public interface TrxTransactionRepositoryType {

    //获取地址下trx余额
    Single<Protocol.Account> balanceInWei(Context context, QWAccount account);

    //获取交易纪录
    Single<TrxAllTransaction[]> fetchTransaction(Context context, String address, int start, int limit);

    Single<TrxTransaction[]> fetchTrc10Transaction(Context context, String address, String tokenName, int start, int limit);

    Single<TrxTransaction> getTransactionInfo(Context context, String txId);

    Single<String[]> createTransaction(Context context, String password, String from, String to, String asset, double amount);

    Single<String[]> createTrc20Transaction(Context context, String password, String from, String to, String asset, double amount);

    Single<String> sendTrxTransaction(String hash);

    Single<Integer> getCostBandWidth(Context context, byte[] fromRaw, String to, String asset, double amount);

    Single<Integer> getCost20BandWidth(Context context, byte[] fromRaw, String to, String asset, double amount);

    //获取冻结需要的带宽
    Single<Integer> getFreezeCostBandWidth(String key, String address, double amount, long frozenDuration, Contract.ResourceCode resource);

    //冻结带宽 能量
    Single<String> freeze(Context context, String password, String address, double amount, long frozenDuration, Contract.ResourceCode resource);

    //获取解冻结需要的带宽
    Single<Integer> getUnfreezeCostBandWidth(String key, String address, Contract.ResourceCode resource);

    //解冻
    Single<String> unfreeze(Context context, String password, String address, Contract.ResourceCode resource);
}
