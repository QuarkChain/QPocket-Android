package com.quarkonium.qpocket.api.repository;

import android.content.Context;
import android.text.TextUtils;

import com.quarkonium.qpocket.abi.FunctionEncoder;
import com.quarkonium.qpocket.abi.datatypes.Type;
import com.quarkonium.qpocket.abi.datatypes.generated.Uint168;
import com.quarkonium.qpocket.abi.datatypes.generated.Uint256;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.entity.ServiceException;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.http.HttpService;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxAllTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxTransaction;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.tron.TrxRequest;
import com.quarkonium.qpocket.tron.crypto.ECKey;
import com.quarkonium.qpocket.tron.crypto.Hash;
import com.quarkonium.qpocket.tron.keystore.Wallet;
import com.quarkonium.qpocket.tron.utils.ByteArray;
import com.quarkonium.qpocket.tron.utils.TransactionUtils;
import com.quarkonium.qpocket.tron.utils.Utils;
import com.quarkonium.qpocket.util.http.HttpUtils;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

//交易事务处理
public class TrxTransactionRepository implements TrxTransactionRepositoryType {

    //获取余额
    @Override
    public Single<Protocol.Account> balanceInWei(Context context, QWAccount account) {
        return Single.fromCallable(() -> {
            byte[] address = TronWalletClient.decodeFromBase58Check(account.getAddress());

            TronWalletClient client = new TronWalletClient();
            //获取带宽
            GrpcAPI.AccountNetMessage accountNetMessage = client.getAccountNet(address);
            Utils.saveAccountNet(context, account.getAddress(), accountNetMessage);
            //获取能量
            GrpcAPI.AccountResourceMessage accountResMessage = client.getAccountRes(address);
            Utils.saveAccountRes(context, account.getAddress(), accountResMessage);
            //获取banlance
            return client.queryAccount(address, false);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<TrxAllTransaction[]> fetchTransaction(Context context, String address, int start, int limit) {
        return Single.fromCallable(() -> {
//            @param sort：定义记录返回的顺序;
//            @param限制：分页的页面大小;
//            @param start：查询分页索引;
//            @param count：记录总数;
//            @param start_timestamp：查询日期范围;
//            @param end_timestamp：查询日期范围;
//            @param标记：'_'仅显示TRX转移;
//            @param地址：转移相关地址;
//            https://apilist.tronscan.org/api/transaction?sort=-timestamp&count=true&limit=50&start=50&address=TYUtLiT54e6uyHsgHTenUNSk27vpSD8Nje
            String apiPath = Constant.TRON_PUBLIC_PATH_SCAN_MAIN;
            String url = apiPath + "api/transaction?sort=-timestamp&count=true&" +
                    "limit=%s&" +
                    "start=%s&" +
                    "address=%s";
            String path = String.format(url, limit, start, address);
            OkHttpClient okHttpClient = HttpUtils.getOkHttp();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(path)
                    .build();
            final Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response.body() != null) {
                String value = response.body().string();
                JSONObject jsonObject = new JSONObject(value);
                if (jsonObject.has("data")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    int size = jsonArray.length();
                    TrxAllTransaction[] list = new TrxAllTransaction[size];
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        TrxAllTransaction trxTransaction = new TrxAllTransaction();
                        int contractType = object.getInt("contractType");
                        trxTransaction.setBlock(object.getString("block"));
                        trxTransaction.setHash(object.getString("hash"));
                        trxTransaction.setTimestamp(object.getString("timestamp"));
                        trxTransaction.setConfirmed(object.getBoolean("confirmed"));
                        trxTransaction.setFee(object.getString("fee"));
                        trxTransaction.setContractType(contractType);

                        trxTransaction.setOwnerAddress(object.getString("ownerAddress"));
                        trxTransaction.setToAddress(object.getString("toAddress"));

                        JSONObject contractData = object.getJSONObject("contractData");
                        switch (contractType) {
                            case Constant.TRX_CONTRACT_TYPE_TRANSFER:
                                //trx转账
                                if (contractData.has("amount")) {
                                    trxTransaction.setAmount(contractData.getString("amount"));
                                }
                                break;
                            case Constant.TRX_CONTRACT_TYPE_TRANSFER_TOKEN:
                                //trc10转账
//                                if (contractData.has("amount")) {
//                                    trxTransaction.setAmount(contractData.getString("amount"));
//                                }
                                if (contractData.has("asset_name")) {
                                    trxTransaction.setContractAddress(contractData.getString("asset_name"));
                                }
                                break;
                            case Constant.TRX_CONTRACT_TYPE_FROZEN:
                                //冻结
//                                if (contractData.has("frozen_balance")) {
//                                    trxTransaction.setAmount(contractData.getString("frozen_balance"));
//                                }
                                if (contractData.has("frozen_duration")) {
                                    trxTransaction.setFrozenDuration(contractData.getString("frozen_duration"));
                                }
                                break;
                            case Constant.TRX_CONTRACT_TYPE_UNFROZEN:
                                //解冻
                                if (contractData.has("resource")) {
                                    trxTransaction.setResource(contractData.getString("resource"));
                                }
                                break;
                            case Constant.TRX_CONTRACT_TYPE_SMART:
                                //智能合约
                                if (contractData.has("contract_address")) {
                                    trxTransaction.setContractAddress(contractData.getString("contract_address"));
                                }
                                if (contractData.has("call_value")) {
                                    trxTransaction.setAmount(contractData.getString("call_value"));
                                }
                                break;
                            case Constant.TRX_CONTRACT_TYPE_VOTE_ASSET:
                                break;
                            case Constant.TRX_CONTRACT_TYPE_VOTE_WITNESS:
                                //投票
                                //"votes":[
                                //{
                                //"vote_address":"TTcYhypP8m4phDhN6oRexz2174zAerjEWP",
                                //"vote_count":12985
                                //}
                                break;
                        }
                        list[i] = trxTransaction;
                    }
                    return list;
                }
            }
            return null;
        });
    }

    @Override
    public Single<TrxTransaction[]> fetchTrc10Transaction(Context context, String address, String tokenName, int start, int limit) {
        return Single.fromCallable(() -> {
//            @param sort：定义记录返回的顺序;
//            @param限制：分页的页面大小;
//            @param start：查询分页索引;
//            @param count：记录总数;
//            @param start_timestamp：查询日期范围;
//            @param end_timestamp：查询日期范围;
//            @param标记：'_'仅显示TRX转移;
//            @param地址：转移相关地址;
//            https://apilist.tronscan.org/api/transfer?sort=-timestamp&count=true&limit=20&start=0&total=70&token=&address=TYtbsav5XCuwT1agJHPuoYp6bHvk3GL9x2
            String apiPath = Constant.TRON_PUBLIC_PATH_SCAN_MAIN;
            String url = apiPath + "api/transfer?sort=-timestamp&count=true&" +
                    "limit=%s&" +
                    "start=%s&" +
                    "token=%s&" +
                    "address=%s";
            String path = String.format(url, limit, start, tokenName, address);
            OkHttpClient okHttpClient = HttpUtils.getOkHttp();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(path)
                    .build();
            final Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response.body() != null) {
                String value = response.body().string();
                JSONObject jsonObject = new JSONObject(value);
                if (jsonObject.has("data")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    int size = jsonArray.length();
                    TrxTransaction[] list = new TrxTransaction[size];
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);

                        TrxTransaction trxTransaction = new TrxTransaction();
                        trxTransaction.setAmount(object.getString("amount"));
                        trxTransaction.setTransferFromAddress(object.getString("transferFromAddress"));
                        trxTransaction.setTransferToAddress(object.getString("transferToAddress"));
                        trxTransaction.setData(object.getString("data"));
                        trxTransaction.setTokenName(object.getString("tokenName"));
                        trxTransaction.setBlock(object.getString("block"));
                        trxTransaction.setConfirmed(object.getBoolean("confirmed"));
                        trxTransaction.setTransactionHash(object.getString("transactionHash"));
                        trxTransaction.setId(object.getString("id"));
                        trxTransaction.setTimestamp(object.getString("timestamp"));

                        list[i] = trxTransaction;
                    }
                    return list;
                }
            }
            return null;
        });
    }

    @Override
    public Single<TrxTransaction> getTransactionInfo(Context context, String txId) {
        return Single.fromCallable(() ->
                new TronWalletClient().getTransactionInfo(txId)
        ).map(transactionInfo -> {
            TrxTransaction trxTransaction = new TrxTransaction();
            trxTransaction.setCost(String.valueOf(transactionInfo.getFee()));
            return trxTransaction;
        }).subscribeOn(Schedulers.io());
    }

    private synchronized Protocol.Transaction createTransactionAndSign(Context context, ECKey ecKeyPair, byte[] fromRaw, String to, String asset, double amount) {
        //生成transaction
        Protocol.Transaction transactionUnsigned = createTransaction(context, fromRaw, to, asset, amount);
        //签名
        return TransactionUtils.sign(transactionUnsigned, ecKeyPair);
    }

    private synchronized Protocol.Transaction createTransaction(Context context, byte[] fromRaw, String to, String asset, double amount) {
        //生成transaction
        byte[] toRaw = TronWalletClient.decodeFromBase58Check(to);
        Protocol.Transaction transactionUnsigned;
        if (TextUtils.isEmpty(asset)) {
            Contract.TransferContract contract = TronWalletClient.createTransferContract(toRaw, fromRaw, (long) (amount * 1000000.0d));
            transactionUnsigned = new TronWalletClient().createTransaction4Transfer(contract);
        } else {
            Contract.TransferAssetContract contract = TronWalletClient.createTransferAssetContract(toRaw, asset.getBytes(), fromRaw, (long) amount);
            transactionUnsigned = new TronWalletClient().createTransferAssetTransaction(contract);
        }
        return TransactionUtils.setTimestamp(transactionUnsigned);
    }

    //创建交易
    @Override
    public Single<String[]> createTransaction(Context context, String password, String from, String to, String asset, double amount) {
        return Single.fromCallable(() -> {
            //获取私钥
            final ECKey ecKeyPair = getEcKey(context, password, from);
            final byte[] fromRaw = TronWalletClient.decodeFromBase58Check(from);
            //生成transaction
            Protocol.Transaction mTransactionSigned = createTransactionAndSign(context, ecKeyPair, fromRaw, to, asset, amount);
            //获取HASH
            String hashed = Hex.toHexString(Hash.sha256(mTransactionSigned.getRawData().toByteArray()));
            return new String[]{hashed, Numeric.toHexString(mTransactionSigned.toByteArray())};
        });
    }

    //创建合约token data字段 转账Token
    private GrpcAPI.TransactionExtention triggerSmartContractFunction(HttpService httpService,
                                                                      String toHexString,
                                                                      String tokenAmount,
                                                                      String contractAddress,
                                                                      String fromAddress) throws Exception {
        List<Type> params = Arrays.asList(new Uint168(Numeric.toBigInt(toHexString)), new Uint256(new BigInteger(tokenAmount)));
        String encodedFunction = FunctionEncoder.encodeConstructor(params);
        TrxRequest request = TrxRequest.createContractTransaction(fromAddress, contractAddress, "transfer(address,uint256)",
                encodedFunction, new BigInteger("100000000"), BigInteger.ZERO);
        return httpService.triggerSmartContractFunction(request);
    }

    private synchronized Protocol.Transaction createTrc20TransactionAndSign(ECKey ecKeyPair, String fromHexString, String to, String asset, double amount) throws Exception {
        //生成transaction
        Protocol.Transaction transactionUnSigned = createTrc20Transaction(fromHexString, to, asset, amount);
        if (transactionUnSigned == null) {
            return null;
        }
        //签名
        return TransactionUtils.sign(transactionUnSigned, ecKeyPair);
    }

    private synchronized Protocol.Transaction createTrc20Transaction(String fromHexString, String to, String asset, double amount) throws Exception {
        //生成transaction
        String toHexString = TronWalletClient.decodeBase58checkToHexString(to);
        String contractHexString = TronWalletClient.decodeBase58checkToHexString(asset);

        HttpService httpService = new HttpService(Constant.TRON_MAIN_NET_PATH + Constant.TRON_MAIN_NET_METHOD_TRIGGER_SMART, false);
        GrpcAPI.TransactionExtention transactionExtention = triggerSmartContractFunction(httpService, toHexString, String.valueOf((long) amount), contractHexString, fromHexString);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            return null;
        }
        return TransactionUtils.setTimestamp(transactionExtention.getTransaction());
    }

    @Override
    public Single<String[]> createTrc20Transaction(Context context, String password, String from, String to, String asset, double amount) {
        return Single.fromCallable(() -> {
                    //获取私钥
                    ECKey ecKeyPair = getEcKey(context, password, from);

                    String fromHexString = TronWalletClient.decodeBase58checkToHexString(from);
                    //生成transaction
                    Protocol.Transaction mTransactionSigned = createTrc20TransactionAndSign(ecKeyPair, fromHexString, to, asset, amount);
                    if (mTransactionSigned == null) {
                        return null;
                    }
                    //获取HASH
                    String hashed = Hex.toHexString(Hash.sha256(mTransactionSigned.getRawData().toByteArray()));
                    return new String[]{hashed, Numeric.toHexString(mTransactionSigned.toByteArray())};
                }
        );
    }

    //获取当前交易需要花费的带宽
    @Override
    public Single<Integer> getCostBandWidth(Context context, byte[] fromRaw, String to, String asset, double amount) {
        final byte[] fromTempRaw = fromRaw;
        return Single.fromCallable(() -> {
            //生成transaction
            ECKey ecKeyPair = getTestEcKey();
            Protocol.Transaction mTransactionSigned = createTransactionAndSign(context, ecKeyPair, fromTempRaw, to, asset, amount);
            //获取带宽
            return mTransactionSigned.getSerializedSize();
        });
    }

    @Override
    public Single<Integer> getCost20BandWidth(Context context, byte[] fromRaw, String to, String asset, double amount) {
        return Single.fromCallable(() -> {
            //生成transaction
            String fromHexString = ByteArray.toHexString(fromRaw);

            ECKey ecKeyPair = getTestEcKey();
            Protocol.Transaction mTransactionSigned = createTrc20TransactionAndSign(ecKeyPair, fromHexString, to, asset, amount);
            if (mTransactionSigned == null) {
                return null;
            }
            //获取带宽
            return mTransactionSigned.getSerializedSize();
        });
    }

    //发送交易
    @Override
    public Single<String> sendTrxTransaction(String hash) {
        return Single.fromCallable(() -> {
            Protocol.Transaction transaction = Protocol.Transaction.parseFrom(Numeric.hexStringToByteArray(hash));
            GrpcAPI.Return result = new TronWalletClient().broadcastTransaction(transaction);
            if (!result.getResult()) {
                throw new ServiceException(result.getMessage().toStringUtf8());
            }
            return Hex.toHexString(Hash.sha256(transaction.getRawData().toByteArray()));
        });
    }

    @Override
    public Single<Integer> getFreezeCostBandWidth(String key, String address, double amount, long frozenDuration, Contract.ResourceCode resource) {
        return Single.fromCallable(() -> {
            byte[] addressByte = TronWalletClient.decodeFromBase58Check(address);

            //获取私钥
            ECKey ecKey = getTestEcKey();

            //创建清单
            Protocol.Transaction transactionUnsigned = new TronWalletClient().createFreezeBalanceTransaction(addressByte, (long) (amount * 1000000.0d), frozenDuration, resource);
            //签名
            Protocol.Transaction mTransactionSigned = TransactionUtils.setTimestamp(transactionUnsigned);
            mTransactionSigned = TransactionUtils.sign(mTransactionSigned, ecKey);

            return mTransactionSigned.getSerializedSize();
        });
    }

    @Override
    public Single<String> freeze(Context context, String password, String address, double amount, long frozenDuration, Contract.ResourceCode resource) {
        return Single.fromCallable(() -> {
            byte[] addressByte = TronWalletClient.decodeFromBase58Check(address);
            Protocol.Transaction transactionUnsigned = new TronWalletClient().createFreezeBalanceTransaction(addressByte, (long) (amount * 1000000.0d), frozenDuration, resource);

            //获取私钥
            ECKey ecKeyPair = getEcKey(context, password, address);

            //签名
            Protocol.Transaction mTransactionSigned = TransactionUtils.setTimestamp(transactionUnsigned);
            mTransactionSigned = TransactionUtils.sign(mTransactionSigned, ecKeyPair);

            GrpcAPI.Return result = new TronWalletClient().broadcastTransaction(mTransactionSigned);
            if (!result.getResult()) {
                throw new ServiceException(result.getMessage().toStringUtf8());
            }
            return Hex.toHexString(Hash.sha256(mTransactionSigned.getRawData().toByteArray()));
        });
    }

    @Override
    public Single<Integer> getUnfreezeCostBandWidth(String key, String address, Contract.ResourceCode resource) {
        return Single.fromCallable(() -> {
            byte[] addressByte = TronWalletClient.decodeFromBase58Check(address);

            //获取私钥
            ECKey ecKey = getTestEcKey();
            //创建清单
            Protocol.Transaction transactionUnsigned = new TronWalletClient().createUnfreezeBalanceTransaction(addressByte, resource);
            //签名
            Protocol.Transaction mTransactionSigned = TransactionUtils.setTimestamp(transactionUnsigned);
            mTransactionSigned = TransactionUtils.sign(mTransactionSigned, ecKey);

            return mTransactionSigned.getSerializedSize();
        });
    }

    @Override
    public Single<String> unfreeze(Context context, String password, String address, Contract.ResourceCode resource) {
        return Single.fromCallable(() -> {
            byte[] addressByte = TronWalletClient.decodeFromBase58Check(address);
            Protocol.Transaction transactionUnsigned = new TronWalletClient().createUnfreezeBalanceTransaction(addressByte, resource);

            //获取私钥
            ECKey ecKeyPair = getEcKey(context, password, address);

            //签名
            Protocol.Transaction mTransactionSigned = TransactionUtils.setTimestamp(transactionUnsigned);
            mTransactionSigned = TransactionUtils.sign(mTransactionSigned, ecKeyPair);

            GrpcAPI.Return result = new TronWalletClient().broadcastTransaction(mTransactionSigned);
            if (!result.getResult()) {
                throw new ServiceException(result.getMessage().toStringUtf8());
            }
            return Hex.toHexString(Hash.sha256(mTransactionSigned.getRawData().toByteArray()));
        });
    }

    private final ECKey getEcKey(Context context, String password, String address) throws Exception {
        //获取私钥
        PasswordStore mPasswordStore = new QuarkPasswordStore(context);
        String keystore = mPasswordStore.exportKeyStore(address, password);
        return Wallet.decrypt(password, keystore);
    }

    private ECKey getTestEcKey() {
        //获取私钥
        BigInteger key = new BigInteger("665d94b36ff56823b8eac0242f0402f2121d4ac41d2ab12d56d0f6c3e660ad8d", 16);
        return ECKey.fromPrivate(key);
    }
}
