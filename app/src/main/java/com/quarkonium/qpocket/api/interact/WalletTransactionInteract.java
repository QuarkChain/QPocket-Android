package com.quarkonium.qpocket.api.interact;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWBalanceDao;
import com.quarkonium.qpocket.api.db.dao.QWChainDao;
import com.quarkonium.qpocket.api.db.dao.QWPublicTokenTransactionDao;
import com.quarkonium.qpocket.api.db.dao.QWShardDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenTransactionDao;
import com.quarkonium.qpocket.api.db.dao.QWTransactionDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.api.repository.ETHTransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TokenRepository;
import com.quarkonium.qpocket.api.repository.TokenRepositoryType;
import com.quarkonium.qpocket.api.repository.TransactionRepositoryType;
import com.quarkonium.qpocket.api.repository.TrxTransactionRepositoryType;
import com.quarkonium.qpocket.crypto.RawTransaction;
import com.quarkonium.qpocket.crypto.Sign;
import com.quarkonium.qpocket.crypto.TransactionDecoder;
import com.quarkonium.qpocket.crypto.TransactionEncoder;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetTransactions;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Transaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TransactionDetail;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TransactionReceipt;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxAllTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxTransaction;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.transaction.bean.EthGas;
import com.quarkonium.qpocket.model.transaction.bean.MergeBean;
import com.quarkonium.qpocket.model.transaction.bean.NativeGasBean;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.tron.utils.Utils;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.http.HttpUtils;

import org.json.JSONArray;
import org.tron.protos.Contract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class WalletTransactionInteract {

    private final TransactionRepositoryType transaction;
    private final TokenRepositoryType tokenRepositoryType;
    private final ETHTransactionRepositoryType ethTransactionRepository;
    private final TrxTransactionRepositoryType trxTransactionRepositoryType;

    public WalletTransactionInteract(TransactionRepositoryType transactionRepositoryType,
                                     TokenRepositoryType token,
                                     ETHTransactionRepositoryType ethTransactionRepository,
                                     TrxTransactionRepositoryType trxTransactionRepositoryType) {
        this.transaction = transactionRepositoryType;
        this.tokenRepositoryType = token;
        this.ethTransactionRepository = ethTransactionRepository;
        this.trxTransactionRepositoryType = trxTransactionRepositoryType;
    }

    //获取分片总数
    public Single<BigInteger> getNetworkInfo(Context context) {
        return transaction
                .networkInfoSuccess()
                .map(networkInfo -> {
                    String nowTotalCount = SharedPreferencesUtils.getTotalChainCount(context);
                    List<String> nowTotalShard = SharedPreferencesUtils.getTotalSharedSizes(context);
                    if (!TextUtils.equals(nowTotalCount, networkInfo.getChainSize()) || isShardChange(nowTotalShard, networkInfo.getShardSizes())) {
                        //存储数据
                        SharedPreferencesUtils.setTotalChainCount(context, networkInfo.getChainSize());
                        SharedPreferencesUtils.setTotalSharedSizes(context, networkInfo.getShardSizes());

                        //增加chain
                        QWChainDao chainDao = new QWChainDao(context);
                        chainDao.addTotal(Numeric.toBigInt(networkInfo.getChainSize()).intValue());

                        //增加分片
                        int max = getMaxShard(networkInfo.getShardSizes());
                        QWShardDao shardDao = new QWShardDao(context);
                        shardDao.addTotal(max);
                    }
                    return Constant.sNetworkId;
                });
    }

    //分片有变化
    private boolean isShardChange(List<String> nowShards, List<String> networkShards) {
        if (networkShards == null) {
            return false;
        }

        if (nowShards == null) {
            return true;
        }

        if (networkShards.size() != nowShards.size()) {
            return true;
        }

        int size = nowShards.size();
        for (int i = 0; i < size; i++) {
            if (!TextUtils.equals(nowShards.get(i), networkShards.get(i))) {
                return true;
            }
        }

        return false;
    }

    private int getMaxShard(List<String> shared) {
        int max = 1;
        if (shared != null && !shared.isEmpty()) {
            for (String s : shared) {
                int sharedId = Numeric.toBigInt(s).intValue();
                if (sharedId > max) {
                    max = sharedId;
                }
            }
        }
        return max;
    }

    public Single<Boolean> getAccountData(Context context, QWAccount account) {
        return getAccountData(context, account, false);
    }

    //获取钱包对应币种balance
    public Single<Boolean> getAccountData(Context context, QWAccount account, boolean getRawData) {
        //eth
        if (account.isEth()) {
            return ethTransactionRepository
                    .balanceInWei(account)
                    .flatMap(balance -> Single.fromCallable(() -> {
                        synchronized (transaction) {
                            QWBalanceDao dao = new QWBalanceDao(context);
                            dao.insertEthBalance(context, account, balance);
                        }
                        return true;
                    })).subscribeOn(Schedulers.newThread());
        }

        //trx
        if (account.isTRX()) {
            return trxTransactionRepositoryType
                    .balanceInWei(context, account)
                    .flatMap(balance -> Single.fromCallable(() -> {
                        synchronized (transaction) {
                            //插入TRX余额
                            QWBalanceDao dao = new QWBalanceDao(context);
                            dao.insertTrxBalance(context, account, new BigInteger(balance.getBalance() + ""));

                            //保存冻结数量
                            Utils.saveAccount(context, account.getAddress(), balance);
                        }
                        return true;
                    })).subscribeOn(Schedulers.newThread());
        }

        return transaction
                .getAccountData(account.getShardAddress())
                .flatMap(balance -> Single.fromCallable(() -> {
                    synchronized (transaction) {
                        QWBalanceDao dao = new QWBalanceDao(context);
                        dao.insertQKCBalance(context, account, balance);
                    }
                    return true;
                })).subscribeOn(Schedulers.newThread());
    }

    //获取并存储当前最新10条数据+pending状态数据
    public Single<QWAccount> getTransactions(Context context, QWAccount account) {
        //eth
        if (account.isEth()) {
            return ethTransactionRepository
                    .fetchTransaction(context, account.getAddress(), 1, Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        //存储数据
                        QWTransactionDao dao = new QWTransactionDao(context);
                        dao.insertEthTransaction(context, account, data);

                        //存储分页下标
                        SharedPreferencesUtils.setCurrentTransactionNext(context, account.getAddress(), "0x2");
                        Constant.sTransactionNext = "0x2";

                        //获取新数据
                        QWAccountDao walletDao = new QWAccountDao(context);
                        return walletDao.queryByAddress(account.getAddress());
                    }).subscribeOn(Schedulers.newThread());
        }

        if (account.isTRX()) {
            return trxTransactionRepositoryType
                    .fetchTransaction(context, account.getAddress(), 0, Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        //存储数据
                        QWTransactionDao dao = new QWTransactionDao(context);
                        dao.insertTrxAllTransaction(context, account, data);

                        //存储分页下标
                        SharedPreferencesUtils.setCurrentTransactionNext(context, account.getAddress(), Integer.toHexString(data.length));
                        Constant.sTransactionNext = Integer.toHexString(data.length);

                        //获取新数据
                        QWAccountDao walletDao = new QWAccountDao(context);
                        return walletDao.queryByAddress(account.getAddress());
                    }).subscribeOn(Schedulers.newThread());

        }

        //qkc
        return getQKCTransactions(context, account);
    }

    public Single<QWAccount> getQKCTransactions(Context context, QWAccount account) {
        return Single.zip(
                getQKCNativeTokenTransactions(account, QWTokenDao.TQKC_ADDRESS, "0x", Constant.QKC_TRANSACTION_LIMIT),
                getQKCNativeTokenTransactions(account, QWTokenDao.TQKC_ADDRESS, "0x00", Constant.QKC_TRANSACTION_LIMIT),
                (QKCGetTransactions.TransactionData data, QKCGetTransactions.TransactionData pendingData) -> {
                    //合并数据
                    ArrayList<Transaction> list = new ArrayList<>();
                    if (pendingData.getTxList() != null && !pendingData.getTxList().isEmpty()) {
                        for (Transaction transaction : pendingData.getTxList()) {
                            transaction.setPending(true);
                            list.add(transaction);
                        }
                    }
                    if (data.getTxList() != null) {
                        list.addAll(data.getTxList());
                    }

                    final String shardId = SharedPreferencesUtils.getCurrentShard(context, account.getShardAddress());
                    final String chainId = SharedPreferencesUtils.getCurrentChain(context, account.getAddress());
                    //存储数据
                    QWTransactionDao dao = new QWTransactionDao(context);
                    dao.insertQWTransaction(context, account, shardId, chainId, list);

                    //存储分页下标
                    SharedPreferencesUtils.setCurrentTransactionNext(context, account.getShardAddress(), data.getNext());
                    Constant.sTransactionNext = data.getNext();

                    //获取新数据
                    QWAccountDao walletDao = new QWAccountDao(context);
                    return walletDao.queryByAddress(account.getAddress());
                }
        ).subscribeOn(Schedulers.newThread());
    }

    public Single<List<QWTransaction>> getTokenTransactions(Context context, QWAccount account, QWToken token, int start) {
        if (account.isTRX()) {
            return trxTransactionRepositoryType
                    .fetchTrc10Transaction(context, account.getAddress(), token.getName(), start, Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        //存储数据
                        QWTokenTransactionDao dao = new QWTokenTransactionDao(context);
                        dao.insertTrxTrc10Transaction(context, account, token.getAddress(), data);

                        //存储分页下标
                        Constant.sTransactionNext = Integer.toHexString(data.length);
                        SharedPreferencesUtils.setCurrentTransactionNext(context, account.getAddress() + token.getAddress(), Constant.sTransactionNext);

                        List<QWTokenTransaction> list = dao.queryByToken(context, account, token.getAddress());
                        return parseTokenTransaction(list);
                    }).subscribeOn(Schedulers.newThread());
        } else if (account.isEth()) {
            return ethTransactionRepository
                    .fetchErc20Transaction(context, account.getAddress(), token.getAddress(), start, Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        //存储数据
                        QWTokenTransactionDao dao = new QWTokenTransactionDao(context);
                        dao.insertErc20TokenTransaction(context, account, token.getAddress(), data);

                        //存储分页下标
                        Constant.sTransactionNext = Numeric.toHexStringWithPrefix(new BigInteger((start + 1) + ""));
                        SharedPreferencesUtils.setCurrentTransactionNext(context, account.getAddress() + token.getAddress(), Constant.sTransactionNext);

                        List<QWTokenTransaction> list = dao.queryByToken(context, account, token.getAddress());
                        return parseTokenTransaction(list);
                    }).subscribeOn(Schedulers.newThread());
        } else if (account.isQKC()) {
            if (token.isNative()) {
                //native token交易记录
                return Single.zip(
                        getQKCNativeTokenTransactions(account, token.getAddress(), "0x", Constant.QKC_TRANSACTION_LIMIT),
                        getQKCNativeTokenTransactions(account, token.getAddress(), "0x00", Constant.QKC_TRANSACTION_LIMIT),
                        (QKCGetTransactions.TransactionData data, QKCGetTransactions.TransactionData pendingData) -> {
                            //合并数据
                            ArrayList<Transaction> list = new ArrayList<>();
                            if (pendingData.getTxList() != null && !pendingData.getTxList().isEmpty()) {
                                for (Transaction transaction : pendingData.getTxList()) {
                                    transaction.setPending(true);
                                    list.add(transaction);
                                }
                            }
                            if (data.getTxList() != null) {
                                list.addAll(data.getTxList());
                            }

                            final String chainId = SharedPreferencesUtils.getCurrentChain(context, account.getAddress());
                            final String shardId = SharedPreferencesUtils.getCurrentShard(context, account.getAddress());
                            //存储数据
                            QWTokenTransactionDao dao = new QWTokenTransactionDao(context);
                            dao.insertQKCNativeTransaction(context, account, token.getAddress(), chainId, shardId, list);

                            //存储分页下标
                            SharedPreferencesUtils.setCurrentTransactionNext(context, account.getShardAddress() + token.getAddress(), data.getNext());
                            Constant.sTransactionNext = data.getNext();

                            //获取新数据
                            List<QWTokenTransaction> tokenList = dao.queryByToken(context, account, token.getAddress());
                            return parseTokenTransaction(tokenList);
                        }
                ).subscribeOn(Schedulers.newThread());
            }
        }
        return null;
    }

    public Single<List<QWTransaction>> getTokenTransactionsByNext(Context context, QWAccount account, QWToken token, String next) {
        if (account.isTRX()) {
            int start = Numeric.toBigInt(next).intValue();
            return trxTransactionRepositoryType
                    .fetchTrc10Transaction(context, account.getAddress(), token.getName(), start, Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        List<QWTokenTransaction> list = new ArrayList<>();
                        for (TrxTransaction trxTransaction : data) {
                            QWTokenTransaction qwTransaction = new QWTokenTransaction();
                            //参数
                            qwTransaction.setTxId(trxTransaction.getTransactionHash());
                            String amount = trxTransaction.getAmount();
                            qwTransaction.setAmount(Numeric.toHexStringWithPrefix(new BigInteger(amount)));

                            qwTransaction.setFrom(trxTransaction.getTransferFromAddress());
                            qwTransaction.setTo(trxTransaction.getTransferToAddress());

                            String block = trxTransaction.getBlock();
                            qwTransaction.setBlock(Numeric.toHexStringWithPrefix(new BigInteger(block)));

                            String time = trxTransaction.getTimestamp();
                            qwTransaction.setTimestamp(Numeric.toHexStringWithPrefix(new BigInteger(time)));

                            // cost花费
                            qwTransaction.setCost(trxTransaction.getCost());

                            qwTransaction.setStatus(String.valueOf(trxTransaction.isConfirmed()));//状态 成功或者失败

                            qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getAddress()));//是发送还是接受

                            //多表关联
                            //token
                            qwTransaction.setToken(token);
                            //钱包
                            qwTransaction.setAccount(account); //钱包
                            list.add(qwTransaction);
                        }
                        //存储分页下标
                        Constant.sTransactionNext = Integer.toHexString(data.length);
                        return parseTokenTransaction(list);
                    }).subscribeOn(Schedulers.newThread());
        } else if (account.isEth()) {
            int start = Numeric.toBigInt(next).intValue();
            return ethTransactionRepository
                    .fetchErc20Transaction(context, account.getAddress(), token.getAddress(), start, Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        List<QWTokenTransaction> list = new ArrayList<>();
                        for (EthTransaction ethTransaction : data) {
                            QWTokenTransaction qwTransaction = new QWTokenTransaction();

                            //参数
                            qwTransaction.setTxId(ethTransaction.getHash());

                            String amount = ethTransaction.getValue();
                            qwTransaction.setAmount(Numeric.toHexStringWithPrefix(new BigInteger(amount)));

                            qwTransaction.setFrom(ethTransaction.getFrom());
                            qwTransaction.setTo(ethTransaction.getTo());

                            String block = ethTransaction.getBlockNumber();
                            qwTransaction.setBlock(Numeric.toHexStringWithPrefix(new BigInteger(block)));

                            String time = ethTransaction.getTimeStamp();
                            qwTransaction.setTimestamp(Numeric.toHexStringWithPrefix(new BigInteger(time)));

                            BigInteger gas = new BigInteger(ethTransaction.getGasUsed());
                            BigInteger gasPrice = new BigInteger(ethTransaction.getGasPrice());
                            BigInteger cost = gas.multiply(gasPrice);
                            String costStr = Numeric.toHexStringWithPrefix(cost);
                            qwTransaction.setCost(costStr);

                            qwTransaction.setStatus("true");//状态 成功或者失败
                            qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getShardAddress()));//是发送还是接受

                            //多表关联
                            //token
                            qwTransaction.setToken(token);
                            //钱包
                            qwTransaction.setAccount(account); //钱包
                            list.add(qwTransaction);
                        }
                        //存储分页下标
                        Constant.sTransactionNext = Numeric.toHexStringWithPrefix(new BigInteger((start + 1) + ""));
                        return parseTokenTransaction(list);
                    }).subscribeOn(Schedulers.newThread());
        } else if (account.isQKC()) {
            if (token.isNative()) {
                //native token交易记录
                return getQKCNativeTokenTransactions(account, token.getAddress(), next, Constant.QKC_TRANSACTION_LIMIT)
                        .map(data -> {
                            final String shardId = SharedPreferencesUtils.getCurrentShard(context, account.getShardAddress());
                            final String chainId = SharedPreferencesUtils.getCurrentChain(context, account.getAddress());
                            QWChainDao chainDao = new QWChainDao(context);
                            QWChain chain = chainDao.queryChainByChain(chainId);
                            if (chain == null) {
                                chain = new QWChain(chainId);
                                chainDao.insert(chain);
                            }

                            QWShardDao shardDao = new QWShardDao(context);
                            QWShard shard = shardDao.queryShardByShard(shardId);
                            if (shard == null) {
                                shard = new QWShard(shardId);
                                shardDao.insert(shard);
                            }

                            List<QWTokenTransaction> tokenList = new ArrayList<>();
                            ArrayList<Transaction> list = data.getTxList();
                            for (Transaction qkcTransaction : list) {
                                QWTokenTransaction qwTransaction = new QWTokenTransaction();
                                //参数
                                qwTransaction.setTxId(qkcTransaction.getTxId());
                                qwTransaction.setAmount(qkcTransaction.getValue());
                                qwTransaction.setFrom(qkcTransaction.getFromAddress());
                                qwTransaction.setTo(qkcTransaction.getToAddress());
                                qwTransaction.setBlock(qkcTransaction.getBlockHeight());
                                qwTransaction.setTimestamp(qkcTransaction.getTimestamp());

                                qwTransaction.setTransferTokenId(qkcTransaction.getTransferTokenId());
                                qwTransaction.setTransferTokenStr(qkcTransaction.getTransferTokenStr());
                                qwTransaction.setGasTokenId(qkcTransaction.getGasTokenId());
                                qwTransaction.setGasTokenStr(qkcTransaction.getGasTokenStr());

                                qwTransaction.setStatus(String.valueOf(qkcTransaction.isSuccess()));//状态 成功或者失败
                                qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getShardAddress()));//是发送还是接受

                                //多表关联
                                //chain
                                qwTransaction.setChain(chain);
                                //shard
                                qwTransaction.setShard(shard);
                                //token
                                qwTransaction.setToken(token);
                                //钱包
                                qwTransaction.setAccount(account); //钱包
                                tokenList.add(qwTransaction);
                            }

                            //存储分页下标
                            Constant.sTransactionNext = data.getNext();
                            return parseTokenTransaction(tokenList);
                        }).subscribeOn(Schedulers.newThread());
            }
        }
        return null;
    }

    private List<QWTransaction> parseTokenTransaction(List<QWTokenTransaction> list) {
        List<QWTransaction> transactionList = new ArrayList<>();
        for (QWTokenTransaction transaction : list) {
            transactionList.add(transaction.parseTransactionList());
        }
        return transactionList;
    }

    //分页获取
    public Single<ArrayList<QWTransaction>> getTransactions(Context context, QWAccount account, String start) {
        if (account.isEth()) {
            return ethTransactionRepository
                    .fetchTransaction(context, account.getAddress(), Numeric.toBigInt(start).intValue(), Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        ArrayList<QWTransaction> transactions = new ArrayList<>();
                        QWTokenDao tokenDao = new QWTokenDao(context);
                        QWToken token = tokenDao.queryTokenByName(QWTokenDao.ETH_NAME);
                        if (token == null) {
                            token = QWTokenDao.getDefaultETHToken();
                            tokenDao.insert(token);
                        }

                        for (EthTransaction transaction : data) {
                            QWTransaction qwTransaction = new QWTransaction();

                            //参数
                            qwTransaction.setTxId(transaction.getHash());

                            String amount = transaction.getValue();
                            qwTransaction.setAmount(Numeric.toHexStringWithPrefix(new BigInteger(amount)));

                            qwTransaction.setFrom(transaction.getFrom());
                            qwTransaction.setTo(transaction.getTo());

                            String block = transaction.getBlockNumber();
                            qwTransaction.setBlock(Numeric.toHexStringWithPrefix(new BigInteger(block)));

                            String time = transaction.getTimeStamp();
                            qwTransaction.setTimestamp(Numeric.toHexStringWithPrefix(new BigInteger(time)));

                            BigInteger gas = new BigInteger(transaction.getGasUsed());
                            BigInteger gasPrice = new BigInteger(transaction.getGasPrice());
                            BigInteger cost = gas.multiply(gasPrice);
                            String costStr = Numeric.toHexStringWithPrefix(cost);
                            qwTransaction.setCost(costStr);

                            qwTransaction.setStatus(String.valueOf("0".equals(transaction.getIsError())));//状态 成功或者失败
                            qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getShardAddress()));//是发送还是接受

                            //多表关联
                            //token
                            qwTransaction.setToken(token);
                            //钱包
                            qwTransaction.setAccount(account); //钱包

                            transactions.add(qwTransaction);
                        }
                        Constant.sTransactionNext = Numeric.toHexStringWithPrefix(Numeric.toBigInt(start).add(BigInteger.ONE));
                        return transactions;
                    });
        }

        if (account.isTRX()) {
            return trxTransactionRepositoryType
                    .fetchTransaction(context, account.getAddress(), Numeric.toBigInt(start).intValue(), Constant.QKC_TRANSACTION_LIMIT_INT)
                    .map(data -> {
                        ArrayList<QWTransaction> transactions = new ArrayList<>();
                        QWTokenDao tokenDao = new QWTokenDao(context);
                        QWToken token = tokenDao.queryTokenByName(QWTokenDao.TRX_NAME);
                        if (token == null) {
                            token = QWTokenDao.getDefaultTRXToken();
                            tokenDao.insert(token);
                        }

                        for (TrxAllTransaction transaction : data) {
                            QWTransaction qwTransaction = new QWTransaction();

                            //参数
                            qwTransaction.setTxId(transaction.getHash());
                            String amount = transaction.getAmount();
                            if (TextUtils.isEmpty(amount)) {
                                qwTransaction.setAmount("0x0");
                            } else {
                                qwTransaction.setAmount(Numeric.toHexStringWithPrefix(new BigInteger(amount)));
                            }

                            qwTransaction.setFrom(transaction.getOwnerAddress());
                            String to = transaction.getToAddress();
                            if (TextUtils.isEmpty(to)) {
                                qwTransaction.setTo(transaction.getOwnerAddress());
                            } else {
                                qwTransaction.setTo(transaction.getToAddress());
                            }

                            String block = transaction.getBlock();
                            qwTransaction.setBlock(Numeric.toHexStringWithPrefix(new BigInteger(block)));

                            String time = transaction.getTimestamp();
                            qwTransaction.setTimestamp(Numeric.toHexStringWithPrefix(new BigInteger(time)));

                            qwTransaction.setStatus(String.valueOf(transaction.isConfirmed()));//状态 成功或者失败

                            switch (transaction.getContractType()) {
                                case Constant.TRX_CONTRACT_TYPE_TRANSFER:
                                    qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getAddress()));//是发送还是接受
                                    break;
                                case Constant.TRX_CONTRACT_TYPE_TRANSFER_TOKEN:
                                    //trc10转账
                                    qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getAddress()));//是发送还是接受
                                    break;
                                case Constant.TRX_CONTRACT_TYPE_FROZEN:
                                    //冻结
                                    qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_FREEZE);
                                    break;
                                case Constant.TRX_CONTRACT_TYPE_UNFROZEN:
                                    //解冻
                                    qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_UNFREEZE);
                                    break;
                                case Constant.TRX_CONTRACT_TYPE_VOTE_ASSET:
                                case Constant.TRX_CONTRACT_TYPE_VOTE_WITNESS:
                                    qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_VOTE);
                                    break;
                                case Constant.TRX_CONTRACT_TYPE_SMART:
                                    //智能合约
                                    qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_CONTRACT);
                                    break;
                                default:
                                    //trx转账
                                    qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getAddress()));//是发送还是接受
                                    break;
                            }

                            //多表关联
                            //token
                            qwTransaction.setToken(token);
                            //钱包
                            qwTransaction.setAccount(account); //钱包

                            transactions.add(qwTransaction);
                        }
                        Constant.sTransactionNext = Integer.toHexString(Numeric.toBigInt(start).intValue() + data.length);
                        return transactions;
                    });
        }

        return getQKCNativeTokenTransactions(account, QWTokenDao.TQKC_ADDRESS, start, Constant.QKC_TRANSACTION_LIMIT)
                .flatMap(data -> Single.fromCallable(() -> {
                    ArrayList<QWTransaction> transactions = new ArrayList<>();
                    if (data.getTxList() != null && !data.getTxList().isEmpty()) {
                        QWTokenDao tokenDao = new QWTokenDao(context);
                        QWToken token = tokenDao.queryTokenByAddress(QWTokenDao.TQKC_ADDRESS);
                        if (token == null) {
                            token = QWTokenDao.getTQKCToken();
                            tokenDao.insert(token);
                        }

                        final String chainId = SharedPreferencesUtils.getCurrentChain(context, account.getAddress());
                        QWChainDao chainDao = new QWChainDao(context);
                        QWChain chain = chainDao.queryChainByChain(chainId);
                        if (chain == null) {
                            chain = new QWChain(chainId);
                            chainDao.insert(chain);
                        }

                        final String shardId = SharedPreferencesUtils.getCurrentShard(context, account.getShardAddress());
                        QWShardDao shardDao = new QWShardDao(context);
                        QWShard qwShard = shardDao.queryShardByShard(shardId);
                        if (qwShard == null) {
                            qwShard = new QWShard(shardId);
                            shardDao.insert(qwShard);
                        }

                        ArrayList<Transaction> list = data.getTxList();
                        for (Transaction transaction : list) {
                            QWTransaction qwTransaction = new QWTransaction();

                            //参数
                            qwTransaction.setTxId(transaction.getTxId());
                            qwTransaction.setAmount(transaction.getValue());
                            qwTransaction.setFrom(transaction.getFromAddress());
                            qwTransaction.setTo(transaction.getToAddress());
                            qwTransaction.setBlock(transaction.getBlockHeight());
                            qwTransaction.setTimestamp(transaction.getTimestamp());

                            qwTransaction.setTransferTokenId(transaction.getTransferTokenId());
                            qwTransaction.setTransferTokenStr(transaction.getTransferTokenStr());
                            qwTransaction.setGasTokenId(transaction.getGasTokenId());
                            qwTransaction.setGasTokenStr(transaction.getGasTokenStr());

                            qwTransaction.setStatus(String.valueOf(transaction.isSuccess()));//状态 成功或者失败
                            qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getShardAddress()));//是发送还是接受

                            //多表关联
                            //chain
                            qwTransaction.setChain(chain);
                            //shard
                            qwTransaction.setShard(qwShard);
                            //token
                            qwTransaction.setToken(token);
                            //钱包
                            qwTransaction.setAccount(account); //钱包

                            transactions.add(qwTransaction);
                        }
                    }
                    Constant.sTransactionNext = data.getNext();
                    return transactions;
                }));
    }

    private Single<QKCGetTransactions.TransactionData> getQKCNativeTokenTransactions(QWAccount wallet, String tokenId, String start, String limit) {
        return transaction.fetchTransaction(wallet.getShardAddress(), tokenId, start, limit).subscribeOn(Schedulers.newThread());
    }

    //获取实际gas费用
    public Single<String> getTransactionCostById(Context context, String txId, boolean isTrx, String from, String to) {
        if (isTrx) {
            return trxTransactionRepositoryType
                    .getTransactionInfo(context, txId)
                    .map(trxTransaction -> {
                        String cost = trxTransaction.getCost();
                        return Numeric.toHexStringWithPrefix(new BigInteger(cost));
                    });
        }
        BigInteger fromChain = QWWalletUtils.getChainByAddress(context, from);
        BigInteger fromShard = QWWalletUtils.getShardByAddress(context, from, fromChain);
        BigInteger toChain = QWWalletUtils.getChainByAddress(context, to);
        BigInteger toShard = QWWalletUtils.getShardByAddress(context, to, toChain);
        if (fromChain.equals(toChain) && fromShard.equals(toShard)) {
            //同链同分片
            return Single.zip(
                    transaction.findTransaction(txId),
                    transaction.findTransactionReceipt(txId),
                    (TransactionDetail transactionDetail, TransactionReceipt transactionReceipt) -> {
                        BigInteger gas = Numeric.toBigInt(transactionReceipt.getGasUsed());
                        BigInteger gasPrice = Numeric.toBigInt(transactionDetail.getGasPrice());
                        BigInteger cost = gas.multiply(gasPrice);
                        return Numeric.toHexStringWithPrefix(cost);
                    });
        } else {
            //跨链跨片
            String fullId = to.substring(to.length() - 8);
            String toTxId = txId.substring(0, txId.length() - 8) + fullId;
            return Single.zip(
                    transaction.findTransaction(txId),
                    transaction.findTransactionReceipt(txId),
                    transaction.findTransactionReceipt(toTxId),
                    (TransactionDetail tsDetail, TransactionReceipt tsReceipt, TransactionReceipt toTsReceipt) -> {
                        BigInteger gasPrice = Numeric.toBigInt(tsDetail.getGasPrice());

                        BigInteger gasUsed = Numeric.toBigInt(tsReceipt.getGasUsed());
                        BigInteger cost = gasUsed.multiply(gasPrice);

                        BigInteger toGasUsed = Numeric.toBigInt(toTsReceipt.getGasUsed());
                        BigInteger toCost = toGasUsed.multiply(gasPrice);
                        return Numeric.toHexStringWithPrefix(cost.add(toCost));
                    });
        }
    }

    //创建交易
    public Single<String[]> createTransaction(String fromAddress, String signerPassword,
                                              BigInteger gasPrice, BigInteger gasLimit,
                                              String toAddress, BigInteger amount,
                                              String data,
                                              BigInteger networkId,
                                              BigInteger transferToken, BigInteger gasToken) {
        return transaction.getQKCNonce(fromAddress)
                .flatMap(nonce -> transaction.createTransaction(
                        fromAddress, signerPassword, gasPrice, gasLimit, toAddress,
                        amount, data, networkId, transferToken, gasToken, nonce)
                );
    }

    //发送交易
    public Single<String> sendTransaction(String hash) {
        return transaction.sendTransaction(hash);
    }

    //发送交易单
    public Single<String> sendTransactions(JSONArray transactions, String txData) {
        return Single.fromCallable(() -> {
            JSONArray txArray = new JSONArray(txData);
            int size = transactions.length();
            if (size != txArray.length()) {
                return "";
            }

            boolean success = true;
            String result = "";
            for (int i = 0; i < size; i++) {
                String mTxData = txArray.getString(i);
                String hash = transactions.getString(i);
                try {
                    byte[] signed = Numeric.hexStringToByteArray(hash);
                    Sign.SignatureData signatureData = parseSignatureData(signed, Numeric.toBigInt(Constant.DEFAULT_CHINA_ID));
                    //在根据SignatureData做hash
                    RawTransaction rawTransaction = TransactionDecoder.decode(mTxData);
                    byte[] encoded = TransactionEncoder.encode(rawTransaction, BigInteger.ZERO, signatureData);
                    result = transaction.sendTransaction(Numeric.toHexString(encoded)).blockingGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                }
                if (TextUtils.isEmpty(result)) {
                    success = false;
                }
            }

            if (success) {
                return result;
            } else {
                return "";
            }
        });
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

    //merge
    public Single<ArrayList<MergeBean>> mergeBalance(String toAddress, ArrayList<MergeBean> list, String signerPassword, BigInteger transferToken) {
        return Single.fromCallable(() -> {

            ArrayList<MergeBean> failList = new ArrayList<>();
            //获取不带分片id的地址
            for (MergeBean bean : list) {

                QWBalance balance = bean.balance;
                //确定from地址
                String fromAddress = Numeric.selectChainAndShardAddress(toAddress, balance.getChain().getChain(), balance.getQWShard().getShard());
                //gas费用
                BigInteger gasPrice = bean.gasPrice;
                BigInteger gasLimit = bean.gasLimit;

                //转出数量
                BigInteger amount = bean.amount;
                //使用手续费的token
                BigInteger gasToken = Numeric.toBigInt(bean.gasTokenId);
                String hash = "";
                try {
                    String[] sign = createTransaction(fromAddress, signerPassword, gasPrice, gasLimit, toAddress, amount, "", Constant.sNetworkId, transferToken, gasToken).blockingGet();
                    if (sign != null && sign.length == 2) {
                        hash = sendTransaction(sign[1]).blockingGet();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                if ("".equals(hash)) {
                    failList.add(bean);
                }
            }
            return failList;
        });
    }

    public Observable<ArrayList<TokenBean>> fetchAllToken(Context context, QWAccount account, boolean onlyLoadDB, boolean needRefresh, boolean getRawData) {
        if (onlyLoadDB) {
            return getDBToken(context, account)
                    .subscribeOn(Schedulers.newThread());
        } else {
            return getCloudToken(context, account, needRefresh, getRawData)
                    .subscribeOn(Schedulers.newThread());
        }
    }

    //获取数据库缓存
    private Observable<ArrayList<TokenBean>> getDBToken(Context context, QWAccount account) {
        return Observable.create(e -> {
            //获取本地数据
            ArrayList<TokenBean> list = tokenRepositoryType.fetchTokenDB(context, account);
            e.onNext(list);
            e.onComplete();
        });
    }

    //刷新云端数据
    private Observable<ArrayList<TokenBean>> getCloudToken(Context context, QWAccount account, boolean needRefresh, boolean getRawData) {
        return Observable.create(e -> {
            //获取本地数据
            ArrayList<TokenBean> list = tokenRepositoryType.fetchTokenDB(context, account);
            e.onNext(list);
            if (e.isDisposed()) {
                e.onComplete();
                return;
            }
            if (!ConnectionUtil.isInternetConnection(context)) {
                e.onComplete();
                return;
            }

            //刷新ETH QKC TRX BTC数据
            try {
                boolean value = getAccountData(context, account, getRawData).blockingGet();
                TokenBean bean = TokenRepository.getMainTokenBean(context, account, true);
                list.remove(0);
                list.add(0, bean);
                e.onNext(list);
                if (e.isDisposed()) {
                    e.onComplete();
                    return;
                }

                //获取云数据
                list = tokenRepositoryType.fetchTokenCloud(e, context, account, needRefresh);
                if (list != null) {
                    list.add(0, bean);
                    e.onNext(list);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.onComplete();
        });
    }

    public Single<TokenBean> getTokenBalance(Context context, QWAccount account, QWToken token) {
        return tokenRepositoryType.getTokenBalance(context, account, token);
    }

    public Observable<TokenBean> findTokenBalance(Context context, QWAccount account, QWToken token) {
        return tokenRepositoryType.findTokenBalance(context, account, token);
    }

    public Single<TokenBean> findTokenSingleBalance(Context context, QWAccount account, QWToken token) {
        return tokenRepositoryType.findTokenSingleBalance(context, account, token);
    }

    public Single<QWBalance[]> findPublicTokenBalance(Context context, QWAccount account, QWToken token) {
        return tokenRepositoryType.findPublicSaleBalance(context, account, token);
    }

    public Single<QWToken> fetchAddToken(Context context, String address, int accountType) {
        return tokenRepositoryType.fetchAddToken(context, address, accountType);
    }

    public Single<String> addToken(Context context, String currentAddress, QWToken token) {
        return tokenRepositoryType.addToken(context, currentAddress, token);
    }

    public Single<Boolean> deleteToken(Context context, QWToken token) {
        return tokenRepositoryType.deleteToken(context, token);
    }

    public Single<String> gasPrice(String shard) {
        return transaction.gasPrice(shard);
    }

    public Single<String> gasLimit(String fromAddress, String toAddress, String transferTokenId, String gasTokenId) {
        return transaction.gasLimit(fromAddress, toAddress, transferTokenId, gasTokenId);
    }

    public Single<String> gasLimitSendToken(Context context, String fromAddress, String toAddress, String contractAddress, BigInteger amount,
                                            String transferTokenId, String gasTokenId) {
        return transaction.gasLimitSendToken(context, fromAddress, toAddress, contractAddress, amount, transferTokenId, gasTokenId);
    }

    public Single<String> gasLimitForBuy(Context context, String fromAddress, String toAddress, BigInteger amount, String transferTokenId, String gasTokenId) {
        return transaction.gasLimitForBuy(context, fromAddress, toAddress, amount, transferTokenId, gasTokenId);
    }

    public Single<List<QWToken>> fetchTokenList(Context context, QWAccount account, int loadType) {
        return tokenRepositoryType.fetchTokenList(context, account, loadType);
    }

    public Single<List<QWToken>> searchTokenList(QWAccount account, String key) {
        return tokenRepositoryType.searchTokenList(account, key);
    }


    //获取公募token交易记录
    public Observable<List<QWPublicTokenTransaction>> getPublicTokenTransaction(Context context, String address, boolean onlyLoadDB) {
        return Observable.create(e -> {
            QWTokenDao tokenDao = new QWTokenDao(context);
            QWToken token = tokenDao.queryTokenByAddress(address);
            if (token == null) {
                e.onNext(new ArrayList<>());
                e.onComplete();
                return;
            }

            QWPublicTokenTransactionDao transactionDao = new QWPublicTokenTransactionDao(context);
            List<QWPublicTokenTransaction> list;
            if (onlyLoadDB) {
                list = transactionDao.queryByToken(token);
                if ((list != null && !list.isEmpty()) || !ConnectionUtil.isInternetConnection(context)) {
                    e.onNext(list);
                    e.onComplete();
                    return;
                }
            }

            if (WalletUtils.isQKCValidAddress(address)) {
                QKCGetTransactions.TransactionData data = transaction.fetchTransaction(address, "0x", Constant.QKC_TRANSACTION_LIMIT).blockingGet();
                //存储数据
                transactionDao.insertQKCTokenTransaction(token, data.getTxList());
                //存储分页下标
                SharedPreferencesUtils.setCurrentTransactionNext(context, address, data.getNext());
                Constant.sTransactionNext = data.getNext();

                //获取新数据
                list = transactionDao.queryByToken(token);
                e.onNext(list);
            } else if (WalletUtils.isValidAddress(address)) {
                EthTransaction[] data = ethTransactionRepository.fetchTransaction(context, address, 1, Constant.QKC_TRANSACTION_LIMIT_INT).blockingGet();
                transactionDao.insertETHTokenTransaction(token, data);

                Constant.sTransactionNext = "0x2";
                SharedPreferencesUtils.setCurrentTransactionNext(context, address, Constant.sTransactionNext);

                list = transactionDao.queryByToken(token);
                e.onNext(list);
            }
            e.onComplete();
        });
    }

    //获取公募token交易记录
    public Single<List<QWPublicTokenTransaction>> getPublicTokenTransactionNext(Context context, String address, String next) {
        return Single.fromCallable(() -> {
            QWTokenDao tokenDao = new QWTokenDao(context);
            QWToken token = tokenDao.queryTokenByAddress(address);
            if (token == null) {
                return new ArrayList<>();
            }

            if (WalletUtils.isQKCValidAddress(address)) {
                QKCGetTransactions.TransactionData data = transaction.fetchTransaction(address, next, Constant.QKC_TRANSACTION_LIMIT).blockingGet();

                ArrayList<QWPublicTokenTransaction> transactions = new ArrayList<>();
                if (data.getTxList() != null && !data.getTxList().isEmpty()) {
                    ArrayList<Transaction> list = data.getTxList();
                    for (Transaction transaction : list) {
                        QWPublicTokenTransaction qwTransaction = new QWPublicTokenTransaction();

                        //参数
                        qwTransaction.setTxId(transaction.getTxId());
                        qwTransaction.setAmount(transaction.getValue());
                        qwTransaction.setFrom(transaction.getFromAddress());
                        qwTransaction.setTo(transaction.getToAddress());
                        qwTransaction.setBlock(transaction.getBlockHeight());
                        qwTransaction.setTimestamp(transaction.getTimestamp());
                        qwTransaction.setGasTokenId(transaction.getGasTokenId());
                        qwTransaction.setGasTokenName(transaction.getGasTokenStr());

                        qwTransaction.setStatus(String.valueOf(transaction.isSuccess()));//状态 成功或者失败

                        if (!"0x0".equals(qwTransaction.getAmount())) {
                            qwTransaction.setDirection(Constant.TOKEN_TRANSACTION_STATE_BUY);//购买
                        } else {
                            qwTransaction.setDirection(Constant.TOKEN_TRANSACTION_STATE_SEND);//转账
                        }

                        //多表关联
                        //token
                        qwTransaction.setToken(token);
                        transactions.add(qwTransaction);
                    }
                }
                Constant.sTransactionNext = data.getNext();
                return transactions;
            } else if (WalletUtils.isValidAddress(address)) {
                EthTransaction[] data = ethTransactionRepository.fetchTransaction(context, address, 1, Constant.QKC_TRANSACTION_LIMIT_INT).blockingGet();

                ArrayList<QWPublicTokenTransaction> transactions = new ArrayList<>();
                for (EthTransaction transaction : data) {
                    QWPublicTokenTransaction qwTransaction = new QWPublicTokenTransaction();

                    //参数
                    qwTransaction.setTxId(transaction.getHash());

                    String amount = transaction.getValue();
                    qwTransaction.setAmount(Numeric.toHexStringWithPrefix(new BigInteger(amount)));

                    qwTransaction.setFrom(transaction.getFrom());
                    qwTransaction.setTo(transaction.getTo());

                    String block = transaction.getBlockNumber();
                    qwTransaction.setBlock(Numeric.toHexStringWithPrefix(new BigInteger(block)));

                    String time = transaction.getTimeStamp();
                    qwTransaction.setTimestamp(Numeric.toHexStringWithPrefix(new BigInteger(time)));

                    BigInteger gas = new BigInteger(transaction.getGasUsed());
                    BigInteger gasPrice = new BigInteger(transaction.getGasPrice());
                    BigInteger cost = gas.multiply(gasPrice);
                    String costStr = Numeric.toHexStringWithPrefix(cost);
                    qwTransaction.setCost(costStr);

                    qwTransaction.setStatus(String.valueOf("0".equals(transaction.getIsError())));//状态 成功或者失败

                    if (!"0x0".equals(qwTransaction.getAmount())) {
                        qwTransaction.setDirection(Constant.TOKEN_TRANSACTION_STATE_BUY);//购买
                    } else {
                        qwTransaction.setDirection(Constant.TOKEN_TRANSACTION_STATE_SEND);//转账
                    }

                    //多表关联
                    //token
                    qwTransaction.setToken(token);
                    transactions.add(qwTransaction);
                }
                Constant.sTransactionNext = Numeric.toHexStringWithPrefix(Numeric.toBigInt(next).add(BigInteger.ONE));
                return transactions;
            }
            return new ArrayList<>();
        });
    }

    //**************eth***********************
    //创建ETH交易
    public Single<String[]> createEthTransaction(String fromAddress, String signerPassword,
                                                 BigInteger gasPrice, BigInteger gasLimit,
                                                 String toAddress, BigInteger amount,
                                                 byte[] data) {
        return transaction.getETHNonce(fromAddress)
                .flatMap(nonce -> transaction.createEthTransaction(
                        fromAddress, toAddress, amount,
                        gasPrice, gasLimit, data, signerPassword, nonce)
                );
    }

    //发送ETH交易
    public Single<String> sendEthTransaction(String hash) {
        return transaction.sendEthTransaction(hash);
    }

    public Single<EthGas> ethSlowFastGas() {
        return Single.fromCallable(() -> {
                    OkHttpClient okHttpClient = HttpUtils.getOkHttp();
                    final okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(Constant.ETH_GAS_API)
                            .build();
                    final Call call = okHttpClient.newCall(request);
                    Response response = call.execute();
                    if (response.body() != null) {
                        String value = response.body().string();
                        return new Gson().fromJson(value, EthGas.class);
                    }
                    return null;
                }
        );
    }

    public Single<String> ethGasLimit(String fromAddress, String toAddress) {
        return transaction.ethEstimateGas(fromAddress, toAddress);
    }

    public Single<String> ethGasLimitSendToken(String fromAddress, String toAddress, String contractAddress, BigInteger amount) {
        return transaction.ethGasLimitSendToken(fromAddress, toAddress, contractAddress, amount);
    }

    public Single<String> ethGasLimitForBuy(String fromAddress, String toAddress, BigInteger amount) {
        return transaction.ethGasLimitForBuy(fromAddress, toAddress, amount);
    }
    //**************eth***********************

    //**************trx交易***********************
    //创建TRX交易
    public Single<String[]> createTrxTransaction(Context context,
                                                 String password,
                                                 String fromAddress,
                                                 String toAddress,
                                                 double amount,
                                                 String tokenAsset) {
        if (!TextUtils.isEmpty(tokenAsset) && !TronWalletClient.isTronErc10TokenAddressValid(tokenAsset)) {
            return trxTransactionRepositoryType.createTrc20Transaction(context, password, fromAddress, toAddress, tokenAsset, amount);
        }
        return trxTransactionRepositoryType.createTransaction(context, password, fromAddress, toAddress, tokenAsset, amount);
    }

    public Single<String> sendTrxTransaction(String hash) {
        return trxTransactionRepositoryType.sendTrxTransaction(hash);
    }

    public Single<Integer> getCostBandWidth(Context context, byte[] fromRaw, String to, String asset, double amount) {
        if (!TextUtils.isEmpty(asset) && !TronWalletClient.isTronErc10TokenAddressValid(asset)) {
            return trxTransactionRepositoryType.getCost20BandWidth(context, fromRaw, to, asset, amount);
        }
        return trxTransactionRepositoryType.getCostBandWidth(context, fromRaw, to, asset, amount);
    }

    //获取冻结需要的能量
    public Single<Integer> getFreezeCostBandWidth(String key, String address, double amount, Contract.ResourceCode resource) {
        return trxTransactionRepositoryType.getFreezeCostBandWidth(key, address, amount, 3, resource);
    }

    //冻结带宽 能量
    public Single<String> trxFreeze(Context context, String password, String address, double amount, Contract.ResourceCode resource) {
        return trxTransactionRepositoryType.freeze(context, password, address, amount, 3, resource);
    }

    //获取解冻需要的能量
    public Single<Integer> getUnfreezeCostBandWidth(String key, String address, Contract.ResourceCode resource) {
        return trxTransactionRepositoryType.getUnfreezeCostBandWidth(key, address, resource);
    }

    public Single<String> trxUnfreeze(Context context, String password, String address, Contract.ResourceCode resource) {
        return trxTransactionRepositoryType.unfreeze(context, password, address, resource);
    }
    //**************trx***********************

    public Single<QWToken> checkGasReserveToken(Context context, QWToken token, String from) {
        return checkGasReserveToken(context, token, from, null);
    }

    public Single<QWToken> checkGasReserveToken(Context context, QWToken token, String from, String chainStr) {
        return Single.fromCallable(() -> {
            if (TextUtils.equals(token.getAddress(), QWTokenDao.TQKC_ADDRESS)) {
                //QKC不作查询
                token.setRefundPercentage(BigDecimal.ONE);
                return token;
            }
            BigInteger tokenID = Numeric.toBigInt(token.getAddress());
            //合约地址
            BigInteger chain;
            if (!TextUtils.isEmpty(chainStr)) {
                chain = Numeric.toBigInt(chainStr);
            } else {
                chain = QWWalletUtils.getChainByAddress(context, from);
            }
            String chainCode = Numeric.toHexStringNoPrefixZeroPadded(chain, 4);
            String contractAddress = Constant.QKC_NATIVE_TOKEN_RESERVE + chainCode + "0000";
            //获取token详情 是否注册过
            NativeGasBean nativeGasBean = tokenRepositoryType.gasReserveToken(context, tokenID, contractAddress);
            if (nativeGasBean == null || Constant.EMPTY_ADDRESS.equals(Numeric.prependHexPrefix(nativeGasBean.getAddress()))) {
                return null;
            }
            //获取token所在分片是否充值
            BigDecimal balance = tokenRepositoryType.gasReserveTokenBalance(context, nativeGasBean.getAddress(), tokenID, contractAddress);
            if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
                token.setRefundPercentage(nativeGasBean.getRefundPercentage());
                token.setReserveTokenBalance(balance);
                return token;
            }
            return null;
        }).subscribeOn(Schedulers.newThread());
    }

    //多线程并发获取token详情
    public Observable<MergeBean> checkGasReserveToken(Context context, MergeBean bean) {
        return Observable.create(e -> {
            //获取rawData
            if (e.isDisposed()) {
                return;
            }
            try {
                QWBalance balance = bean.balance;
                //当做手续费的token
                String tokenId = balance.getQWToken().getAddress();
                BigInteger token = Numeric.toBigInt(tokenId);
                //确定合约地址分片
                BigInteger chain = Numeric.toBigInt(balance.getChain().getChain());
                String chainCode = Numeric.toHexStringNoPrefixZeroPadded(chain, 4);
                String contractAddress = Constant.QKC_NATIVE_TOKEN_RESERVE + chainCode + "0000";

                //1、获取token详情，返回同QKC兑换比例
                //获取token详情
                NativeGasBean nativeGasBean = tokenRepositoryType.gasReserveToken(context, token, contractAddress);
                if (nativeGasBean == null || Constant.EMPTY_ADDRESS.equals(Numeric.prependHexPrefix(nativeGasBean.getAddress()))) {
                    //获取不到信息
                    e.onNext(bean);
                    e.onComplete();
                    return;
                }

                //2、获取token所在分片是否充值，充值金额大于0就认为支持作为手续费使用
                BigDecimal value = tokenRepositoryType.gasReserveTokenBalance(context, nativeGasBean.getAddress(), token, contractAddress);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    //切换gas token
                    bean.gasTokenId = tokenId;
                    //设置同QKC的兑换比例
                    bean.refundPercentage = nativeGasBean.getRefundPercentage();
                    bean.reserveTokenBalance = value;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.onNext(bean);
            e.onComplete();
        });
    }
}
