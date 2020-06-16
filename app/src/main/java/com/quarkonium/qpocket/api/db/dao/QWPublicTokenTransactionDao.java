package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthTransaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Transaction;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作事务表的DAO类
 */
public class QWPublicTokenTransactionDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWPublicTokenTransaction, Integer> dao;

    public QWPublicTokenTransactionDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWPublicTokenTransaction.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    //添加Transaction
    public void insertQKCTokenTransaction(QWToken token, ArrayList<Transaction> list) {
        delete(token);
        for (Transaction transaction : list) {
            insertQKCTokenTransaction(token, transaction);
        }
    }

    private void insertQKCTokenTransaction(QWToken token, Transaction transaction) {

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

        insert(qwTransaction);
    }

    public void insertETHTokenTransaction(QWToken token, EthTransaction[] data) {
        delete(token);
        for (EthTransaction transaction : data) {
            insertEthTokenTransaction(token, transaction);
        }
    }

    private void insertEthTokenTransaction(QWToken token, EthTransaction transaction) {
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

        insert(qwTransaction);
    }

    // 添加数据
    private void insert(QWPublicTokenTransaction data) {
        try {
            dao.createIfNotExists(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //清除改钱包下的所有记录
    private void delete(QWToken token) {
        try {
            DeleteBuilder<QWPublicTokenTransaction, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWPublicTokenTransaction.COLUMN_NAME_TOKEN, token);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<QWPublicTokenTransaction> queryByToken(QWToken token) {
        try {
            QueryBuilder<QWPublicTokenTransaction, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where()
                    .eq(QWPublicTokenTransaction.COLUMN_NAME_TOKEN, token);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public QWPublicTokenTransaction queryByID(int id) {
        try {
            return dao.queryForId(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(QWPublicTokenTransaction tokenTransaction) {
        try {
            dao.update(tokenTransaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllData() {
        DeleteBuilder<QWPublicTokenTransaction, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
