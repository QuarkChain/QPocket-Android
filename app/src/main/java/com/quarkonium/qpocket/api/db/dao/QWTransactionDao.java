package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;
import android.text.TextUtils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Transaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxAllTransaction;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作事务表的DAO类
 */
public class QWTransactionDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWTransaction, Integer> dao;

    public QWTransactionDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWTransaction.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(QWToken token) {
        DeleteBuilder<QWTransaction, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder
                    .where()
                    .eq(QWTransaction.COLUMN_NAME_TOKEN, token);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //添加Transaction
    public void insertQWTransaction(Context context, QWAccount wallet, String shardID, String chainID, ArrayList<Transaction> list) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByAddress(QWTokenDao.TQKC_ADDRESS);
        if (token == null) {
            token = QWTokenDao.getTQKCToken();
            tokenDao.insert(token);
        }

        QWChainDao chainDao = new QWChainDao(context);
        QWChain chain = chainDao.queryChainByChain(chainID);
        if (chain == null) {
            chain = new QWChain(chainID);
            chainDao.insert(chain);
        }

        QWShardDao shardDao = new QWShardDao(context);
        QWShard shard = shardDao.queryShardByShard(shardID);
        if (shard == null) {
            shard = new QWShard(shardID);
            shardDao.insert(shard);
        }

        clearByWT(wallet, token);

        for (Transaction transaction : list) {
            insertTransaction(wallet, token, shard, chain, transaction);
        }
    }

    private void insertTransaction(QWAccount wallet, QWToken token, QWShard shard, QWChain chain, Transaction transaction) {

        QWTransaction qwTransaction = new QWTransaction();

        //参数
//        private static final String COLUMN_NAME_COST = "cost";
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
        qwTransaction.setDirection(transaction.isPending() ?
                Constant.QKC_TRANSACTION_STATE_PENDING :
                qwTransaction.getDirectionByAddress(wallet.getShardAddress()));//是发送还是接受

        //多表关联
        //chain
        qwTransaction.setChain(chain);
        //shard
        qwTransaction.setShard(shard);
        //token
        qwTransaction.setToken(token);
        //钱包
        qwTransaction.setAccount(wallet); //钱包

        insert(qwTransaction);
    }

    public void insertEthTransaction(Context context, QWAccount account, EthTransaction[] data) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByName(QWTokenDao.ETH_NAME);
        if (token == null) {
            token = QWTokenDao.getDefaultETHToken();
            tokenDao.insert(token);
        }

        clearByWT(account, token);

        for (EthTransaction transaction : data) {
            insertEthTransaction(account, token, transaction);
        }
    }

    private void insertEthTransaction(QWAccount wallet, QWToken token, EthTransaction transaction) {
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

        QWTransaction t = hasExit(transaction.getHash());
        if (t != null && TextUtils.equals(Constant.QKC_TRANSACTION_STATE_SEND, t.getDirection())) {
            qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_RECEIVE);//是发送还是接受
        } else {
            //TODO pending状态
            qwTransaction.setDirection(
//                transaction.isPending() ? Constant.QKC_TRANSACTION_STATE_PENDING :
                    qwTransaction.getDirectionByAddress(wallet.getShardAddress()));//是发送还是接受
        }


        //多表关联
        //token
        qwTransaction.setToken(token);
        //钱包
        qwTransaction.setAccount(wallet); //钱包

        try {
            dao.createOrUpdate(qwTransaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertTrxAllTransaction(Context context, QWAccount account, TrxAllTransaction[] data) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByName(QWTokenDao.TRX_NAME);
        if (token == null) {
            token = QWTokenDao.getDefaultTRXToken();
            tokenDao.insert(token);
        }

        clearByWT(account, token);

        for (TrxAllTransaction transaction : data) {
            insertTrxAllTransaction(account, token, transaction);
        }
    }

    private void insertTrxAllTransaction(QWAccount wallet, QWToken token, TrxAllTransaction transaction) {
        QWTransaction qwTransaction = new QWTransaction();
        //参数
        qwTransaction.setTxId(transaction.getHash());

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

        String amount = transaction.getAmount();
        if (TextUtils.isEmpty(amount)) {
            qwTransaction.setAmount("0x0");
        } else {
            qwTransaction.setAmount(Numeric.toHexStringWithPrefix(new BigInteger(amount)));
        }

        // cost花费
        qwTransaction.setCost(transaction.getFee());

        qwTransaction.setStatus(String.valueOf(transaction.isConfirmed()));//状态 成功或者失败

        switch (transaction.getContractType()) {
            case Constant.TRX_CONTRACT_TYPE_TRANSFER:
                //trx转账
                qwTransaction.setDirection(qwTransaction.getDirectionByAddress(wallet.getAddress()));//是发送还是接受
                break;
            case Constant.TRX_CONTRACT_TYPE_TRANSFER_TOKEN:
                //trc10转账
                qwTransaction.setDirection(qwTransaction.getDirectionByAddress(wallet.getAddress()));//是发送还是接受
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
                //投票
                qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_VOTE);
                //投票
                qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_VOTE);
                break;
            case Constant.TRX_CONTRACT_TYPE_SMART:
                //智能合约
                qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_CONTRACT);
                break;
            default:
                //trx转账
                qwTransaction.setDirection(qwTransaction.getDirectionByAddress(wallet.getAddress()));//是发送还是接受
                break;
        }

        //多表关联
        //token
        qwTransaction.setToken(token);
        //钱包
        qwTransaction.setAccount(wallet); //钱包

        try {
            dao.createOrUpdate(qwTransaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    private void insert(QWTransaction data) {
        try {
            dao.createIfNotExists(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //清除该钱包下的所有记录
    private void clearByWT(QWAccount wallet, QWToken token) {
        try {
            DeleteBuilder<QWTransaction, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWTransaction.COLUMN_NAME_ACCOUNT, wallet)
                    .and()
                    .eq(QWTransaction.COLUMN_NAME_TOKEN, token);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QWTransaction queryByID(int id) {
        try {
            return dao.queryForId(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateTransaction(QWTransaction transaction) {
        try {
            dao.update(transaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private QWTransaction hasExit(String hashId) {
        try {
            return dao.queryBuilder().where().eq(QWTransaction.COLUMN_NAME_TXID, hashId).queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void delete(QWAccount account) {
        try {
            DeleteBuilder<QWTransaction, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWTransaction.COLUMN_NAME_ACCOUNT, account);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllQKC(List<QWAccount> list) {
        DeleteBuilder<QWTransaction, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            dao.callBatchTasks(() -> {
                for (QWAccount account : list) {
                    try {
                        deleteBuilder
                                .where()
                                .eq(QWTransaction.COLUMN_NAME_ACCOUNT, account);
                        deleteBuilder.delete();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
