package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;
import android.text.TextUtils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Transaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.TrxTransaction;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作事务表的DAO类
 */
public class QWTokenTransactionDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWTokenTransaction, Integer> dao;

    public QWTokenTransactionDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWTokenTransaction.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(QWToken token) {
        DeleteBuilder<QWTokenTransaction, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder
                    .where()
                    .eq(QWTransaction.COLUMN_NAME_TOKEN, token);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertQKCNativeTransaction(Context context, QWAccount account, String tokenAddress, String chainID, String shardID, ArrayList<Transaction> data) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByAddress(tokenAddress);
        if (token == null) {
            return;
        }

        QWChainDao chainDao = new QWChainDao(context);
        QWChain chain = chainDao.queryChainByChain(chainID);
        if (chain == null) {
            chain = new QWChain(chainID);
            chainDao.insert(chain);
        }
        final QWChain finalChain = chain;

        QWShardDao shardDao = new QWShardDao(context);
        QWShard shard = shardDao.queryShardByShard(shardID);
        if (shard == null) {
            shard = new QWShard(shardID);
            shardDao.insert(shard);
        }
        final QWShard finalShard = shard;

        clearByWT(account, token);

        //批处理
        try {
            dao.callBatchTasks(() -> {
                for (Transaction transaction : data) {
                    insertQKCTransaction(account, token, finalChain, finalShard, transaction);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertQKCTransaction(QWAccount account, QWToken token, QWChain chain, QWShard shard, Transaction transaction) {
        QWTokenTransaction qwTransaction = new QWTokenTransaction();
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
        qwTransaction.setDirection(transaction.isPending() ?
                Constant.QKC_TRANSACTION_STATE_PENDING :
                qwTransaction.getDirectionByAddress(account.getShardAddress()));//是发送还是接受

        //多表关联
        //chain
        qwTransaction.setChain(chain);
        //shard
        qwTransaction.setShard(shard);
        //token
        qwTransaction.setToken(token);
        //钱包
        qwTransaction.setAccount(account); //钱包

        try {
            dao.createOrUpdate(qwTransaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertTrxTrc10Transaction(Context context, QWAccount account, String tokenAddress, TrxTransaction[] data) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByAddress(tokenAddress);
        if (token == null) {
            return;
        }

        clearByWT(account, token);

        //批处理
        try {
            dao.callBatchTasks(() -> {
                for (TrxTransaction transaction : data) {
                    insertTrxTrc10Transaction(account, token, transaction);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertTrxTrc10Transaction(QWAccount account, QWToken token, TrxTransaction transaction) {
        QWTokenTransaction qwTransaction = new QWTokenTransaction();
        //参数
        qwTransaction.setTxId(transaction.getTransactionHash());
        String amount = transaction.getAmount();
        qwTransaction.setAmount(Numeric.toHexStringWithPrefix(new BigInteger(amount)));

        qwTransaction.setFrom(transaction.getTransferFromAddress());
        qwTransaction.setTo(transaction.getTransferToAddress());

        String block = transaction.getBlock();
        qwTransaction.setBlock(Numeric.toHexStringWithPrefix(new BigInteger(block)));

        String time = transaction.getTimestamp();
        qwTransaction.setTimestamp(Numeric.toHexStringWithPrefix(new BigInteger(time)));

        // cost花费
        qwTransaction.setCost(transaction.getCost());

        qwTransaction.setStatus(String.valueOf(transaction.isConfirmed()));//状态 成功或者失败

        QWTokenTransaction t = hasExit(transaction.getTransactionHash());
        if (t != null && TextUtils.equals(Constant.QKC_TRANSACTION_STATE_SEND, t.getDirection())) {
            qwTransaction.setDirection(Constant.QKC_TRANSACTION_STATE_RECEIVE);//是发送还是接受
        } else {
            qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getAddress()));//是发送还是接受
        }

        //多表关联
        //token
        qwTransaction.setToken(token);
        //钱包
        qwTransaction.setAccount(account); //钱包

        try {
            dao.createOrUpdate(qwTransaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertErc20TokenTransaction(Context context, QWAccount account, String tokenAddress, EthTransaction[] data) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByAddress(tokenAddress);
        if (token == null) {
            return;
        }

        clearByWT(account, token);

        //批处理
        try {
            dao.callBatchTasks(() -> {
                for (EthTransaction transaction : data) {
                    insertERC20Transaction(account, token, transaction);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertERC20Transaction(QWAccount account, QWToken token, EthTransaction transaction) {
        QWTokenTransaction qwTransaction = new QWTokenTransaction();

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

        qwTransaction.setStatus("true");//状态 成功或者失败
        qwTransaction.setDirection(qwTransaction.getDirectionByAddress(account.getShardAddress()));//是发送还是接受

        //多表关联
        //token
        qwTransaction.setToken(token);
        //钱包
        qwTransaction.setAccount(account); //钱包

        try {
            dao.createOrUpdate(qwTransaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //清除改钱包下的所有记录
    private void clearByWT(QWAccount wallet, QWToken token) {
        try {
            DeleteBuilder<QWTokenTransaction, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWTransaction.COLUMN_NAME_ACCOUNT, wallet)
                    .and()
                    .eq(QWTransaction.COLUMN_NAME_TOKEN, token);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<QWTokenTransaction> queryByToken(Context context, QWAccount account, String tokenAddress) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByAddress(tokenAddress);
        if (token == null) {
            return null;
        }

        try {
            QueryBuilder<QWTokenTransaction, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where()
                    .eq(QWTransaction.COLUMN_NAME_ACCOUNT, account)
                    .and()
                    .eq(QWTransaction.COLUMN_NAME_TOKEN, token);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private QWTokenTransaction hasExit(String hashId) {
        try {
            return dao.queryBuilder().where().eq(QWTokenTransaction.COLUMN_NAME_TXID, hashId).queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void delete(QWAccount account) {
        try {
            DeleteBuilder<QWTokenTransaction, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWTransaction.COLUMN_NAME_ACCOUNT, account);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
