package com.quarkonium.qpocket.api.repository;

import android.content.Context;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.jsonrpc.protocol.Web3jFactory;
import com.quarkonium.qpocket.jsonrpc.protocol.core.DefaultBlockParameterName;
import com.quarkonium.qpocket.jsonrpc.protocol.http.HttpService;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthTransaction;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.http.HttpUtils;
import com.quarkonium.qpocket.MainApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

//交易事务处理
public class ETHTransactionRepository implements ETHTransactionRepositoryType {

    @Override
    public Single<BigInteger> balanceInWei(QWAccount wallet) {
        return Single.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sEthNetworkPath, false))
                .ethGetBalance(wallet.getAddress(), DefaultBlockParameterName.LATEST)
                .send()
                .getBalance()
        ).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<BigInteger> balanceInWei(String address) {
        return Observable.fromCallable(() -> Web3jFactory
                .build(new HttpService(Constant.sEthNetworkPath, false))
                .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .send()
                .getBalance()
        ).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<EthTransaction[]> fetchTransaction(Context context, String address, int start, int limit) {
        return Single.fromCallable(() -> {
            String apiPath = Constant.ETH_PUBLIC_PATH_SCAN_MAIN;
            int index = (int) SharedPreferencesUtils.getEthNetworkIndex(MainApplication.getContext());
            switch (index) {
                case Constant.ETH_PUBLIC_PATH_MAIN_INDEX:
                    if (ToolUtils.isZh(context)) {
                        apiPath = Constant.ETH_PUBLIC_PATH_SCAN_MAIN_CN;
                    }
                    break;
                case Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX:
                    apiPath = Constant.ETH_PUBLIC_PATH_SCAN_ROPSTEN;
                    break;
                case Constant.ETH_PUBLIC_PATH_KOVAN_INDEX:
                    apiPath = Constant.ETH_PUBLIC_PATH_SCAN_KOVAN;
                    break;
                case Constant.ETH_PUBLIC_PATH_RINKBY_INDEX:
                    apiPath = Constant.ETH_PUBLIC_PATH_SCAN_RINKBY;
                    break;
            }
            String url = apiPath + "api?module=account&action=txlist&" +
                    "address=%s&startblock=0&endblock=latest&" +
                    "page=%s&" +
                    "offset=%s&sort=desc&" +
                    "apikey=%s";
            String path = String.format(url, address, start, limit, Constant.ETH_SCAN_API_KEY);
            OkHttpClient okHttpClient = HttpUtils.getOkHttp();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(path)
                    .build();
            final Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response.body() != null) {
                String value = response.body().string();
                JSONObject jsonObject = new JSONObject(value);
                if (jsonObject.has("result")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("result");
                    int size = jsonArray.length();
                    EthTransaction[] list = new EthTransaction[size];
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);

                        EthTransaction ethTransaction = new EthTransaction();
                        ethTransaction.setBlockNumber(object.getString("blockNumber"));
                        ethTransaction.setTimeStamp(object.getString("timeStamp"));
                        ethTransaction.setHash(object.getString("hash"));
                        ethTransaction.setNonce(object.getString("nonce"));
                        ethTransaction.setBlockHash(object.getString("blockHash"));
                        ethTransaction.setTransactionIndex(object.getString("transactionIndex"));
                        ethTransaction.setFrom(object.getString("from"));
                        ethTransaction.setTo(object.getString("to"));
                        ethTransaction.setValue(object.getString("value"));
                        ethTransaction.setGas(object.getString("gas"));
                        ethTransaction.setGasPrice(object.getString("gasPrice"));
                        ethTransaction.setIsError(object.getString("isError"));
                        ethTransaction.setTxreceipt_status(object.getString("txreceipt_status"));
                        ethTransaction.setInput(object.getString("input"));
                        ethTransaction.setContractAddress(object.getString("contractAddress"));
                        ethTransaction.setCumulativeGasUsed(object.getString("cumulativeGasUsed"));
                        ethTransaction.setGasUsed(object.getString("gasUsed"));
                        ethTransaction.setConfirmations(object.getString("confirmations"));

                        list[i] = ethTransaction;
                    }
                    return list;
                }
            }
            return null;
        });
    }

    @Override
    public Single<EthTransaction[]> fetchErc20Transaction(Context context, String address, String tokenAddress, int start, int limit) {
        return Single.fromCallable(() -> {
            String apiPath = Constant.ETH_PUBLIC_PATH_SCAN_MAIN;
            int index = (int) SharedPreferencesUtils.getEthNetworkIndex(MainApplication.getContext());
            switch (index) {
                case Constant.ETH_PUBLIC_PATH_MAIN_INDEX:
                    if (ToolUtils.isZh(context)) {
                        apiPath = Constant.ETH_PUBLIC_PATH_SCAN_MAIN_CN;
                    }
                    break;
                case Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX:
                    apiPath = Constant.ETH_PUBLIC_PATH_SCAN_ROPSTEN;
                    break;
                case Constant.ETH_PUBLIC_PATH_KOVAN_INDEX:
                    apiPath = Constant.ETH_PUBLIC_PATH_SCAN_KOVAN;
                    break;
                case Constant.ETH_PUBLIC_PATH_RINKBY_INDEX:
                    apiPath = Constant.ETH_PUBLIC_PATH_SCAN_RINKBY;
                    break;
            }
            String url = apiPath + "api?module=account&action=tokentx&" +
                    "contractaddress=%s&" +
                    "address=%s&" +
                    "page=%s&" +
                    "offset=%s&sort=desc&" +
                    "apikey=%s";
            String path = String.format(url, tokenAddress, address, start, limit, Constant.ETH_SCAN_API_KEY);
            OkHttpClient okHttpClient = HttpUtils.getOkHttp();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(path)
                    .build();
            final Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response.body() != null) {
                String value = response.body().string();
                JSONObject jsonObject = new JSONObject(value);
                if (jsonObject.has("result")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("result");
                    int size = jsonArray.length();
                    EthTransaction[] list = new EthTransaction[size];
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);

                        EthTransaction ethTransaction = new EthTransaction();
                        ethTransaction.setBlockNumber(object.getString("blockNumber"));
                        ethTransaction.setTimeStamp(object.getString("timeStamp"));
                        ethTransaction.setHash(object.getString("hash"));
                        ethTransaction.setNonce(object.getString("nonce"));
                        ethTransaction.setBlockHash(object.getString("blockHash"));
                        ethTransaction.setTransactionIndex(object.getString("transactionIndex"));
                        ethTransaction.setFrom(object.getString("from"));
                        ethTransaction.setTo(object.getString("to"));
                        ethTransaction.setValue(object.getString("value"));
                        ethTransaction.setGas(object.getString("gas"));
                        ethTransaction.setGasPrice(object.getString("gasPrice"));
                        ethTransaction.setInput(object.getString("input"));
                        ethTransaction.setContractAddress(object.getString("contractAddress"));
                        ethTransaction.setCumulativeGasUsed(object.getString("cumulativeGasUsed"));
                        ethTransaction.setGasUsed(object.getString("gasUsed"));
                        ethTransaction.setConfirmations(object.getString("confirmations"));
                        list[i] = ethTransaction;
                    }
                    return list;
                }
            }
            return null;
        });
    }

    //    @Override
//    public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password) {
//        final Web3j web3j = Web3jFactory.build(new HttpService(networkRepository.getDefaultNetwork().rpcServerUrl));
//
//        return Single.fromCallable(() -> {
//            EthGetTransactionCount ethGetTransactionCount = web3j
//                    .ethGetTransactionCount(from.address, DefaultBlockParameterName.LATEST)
//                    .send();
//            return ethGetTransactionCount.getTransactionCount();
//        })
//                .flatMap(nonce -> accountKeystoreService.signTransaction(from, password, toAddress, subunitAmount, gasPrice, gasLimit, nonce.longValue(), data, networkRepository.getDefaultNetwork().chainId))
//                .flatMap(signedMessage -> Single.fromCallable( () -> {
//                    EthSendTransaction raw = web3j
//                            .ethSendRawTransaction(Numeric.toHexString(signedMessage))
//                            .send();
//                    if (raw.hasError()) {
//                        throw new ServiceException(raw.getError().getMessage());
//                    }
//                    return raw.getTransactionHash();
//                })).subscribeOn(Schedulers.io());
//    }
}
