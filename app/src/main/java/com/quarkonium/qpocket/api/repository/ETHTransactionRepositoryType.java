package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthTransaction;

import java.math.BigInteger;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface ETHTransactionRepositoryType {

    //获取地址下eth余额
    Single<BigInteger> balanceInWei(QWAccount wallet);

    Observable<BigInteger> balanceInWei(String address);

    //获取交易纪录
    Single<EthTransaction[]> fetchTransaction(Context context, String address, int start, int limit);

    Single<EthTransaction[]> fetchErc20Transaction(Context context, String address, String tokenAddress, int start, int limit);
}
