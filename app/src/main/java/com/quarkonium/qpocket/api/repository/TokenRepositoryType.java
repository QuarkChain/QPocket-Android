package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWPublicScale;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.transaction.bean.NativeGasBean;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;

public interface TokenRepositoryType {

    ArrayList<TokenBean> fetchTokenDB(Context context, QWAccount account);

    ArrayList<TokenBean> fetchTokenCloud(ObservableEmitter e, Context context, QWAccount account, boolean needRefresh);

    Single<TokenBean> getTokenBalance(Context context, QWAccount account, QWToken token);

    Observable<TokenBean> findTokenBalance(Context context, QWAccount account, QWToken token);

    Single<TokenBean> findTokenSingleBalance(Context context, QWAccount account, QWToken token);

    Observable<List<QWPublicScale>> fetchPublicSale(Context context, int accountType, boolean isLoadSQL);

    Single<QWBalance[]> findPublicSaleBalance(Context context, QWAccount account, QWToken token);

    Single<QWToken> fetchAddToken(Context context, String address, int accountType);

    Single<String> addToken(Context context, String currentAddress, QWToken token);

    Single<Boolean> deleteToken(Context context, QWToken token);

    Single<List<QWToken>> fetchTokenList(Context context, QWAccount account, int loadType);

    Single<List<QWToken>> searchTokenList(QWAccount account, String key);

    NativeGasBean gasReserveToken(Context context, BigInteger tokenID, String contractAddress);

    BigDecimal gasReserveTokenBalance(Context context, String address, BigInteger tokenID, String contractAddress);
}
