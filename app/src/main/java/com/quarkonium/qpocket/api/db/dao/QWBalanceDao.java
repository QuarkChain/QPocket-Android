package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;
import android.text.TextUtils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Account;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Balance;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetAccountData;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWBalanceDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWBalance, Integer> dao;

    public QWBalanceDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWBalance.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertEthBalance(Context context, QWAccount account, BigInteger value) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByName(QWTokenDao.ETH_NAME);
        if (token == null) {
            token = QWTokenDao.getDefaultETHToken();
            tokenDao.insert(token);
        }

        QWBalance balance = queryByWT(account, token);
        if (balance == null) {
            balance = new QWBalance();
        }
        //token数量
        balance.setBalance(Numeric.toHexStringWithPrefix(value));

        //多表关联
        //token
        balance.setQWToken(token);
        //钱包
        balance.setAccount(account); //钱包
        insert(balance);
    }

    //Trx余额
    public void insertTrxBalance(Context context, QWAccount account, BigInteger value) {
        QWTokenDao tokenDao = new QWTokenDao(context);
        QWToken token = tokenDao.queryTokenByName(QWTokenDao.TRX_NAME);
        if (token == null) {
            token = QWTokenDao.getDefaultTRXToken();
            tokenDao.insert(token);
        }

        QWBalance balance = queryByWT(account, token);
        if (balance == null) {
            balance = new QWBalance();
        }
        //token数量
        balance.setBalance(Numeric.toHexStringWithPrefix(value));

        //多表关联
        //token
        balance.setQWToken(token);
        //钱包
        balance.setAccount(account); //钱包
        insert(balance);
    }

    //添加balances
    public void insertQKCBalance(Context context, QWAccount account, QKCGetAccountData.AccountData data) {
        if (data != null) {
            QWTokenDao tokenDao = new QWTokenDao(context);
            QWToken token = tokenDao.queryTokenByAddress(QWTokenDao.TQKC_ADDRESS);
            if (token == null) {
                token = QWTokenDao.getTQKCToken();
                tokenDao.insert(token);
            }
            //清除QKC旧数据
            clearByWT(account, token);
            //清除其余native token旧数据
            List<QWToken> tokens = tokenDao.queryAllNativeTokenByType(account.getType());
            if (tokens != null) {
                for (QWToken t : tokens) {
                    clearByWT(account, t);
                }
            }


            QWChainDao chainDao = new QWChainDao(context);
            QWShardDao shardDao = new QWShardDao(context);
            //主分片
            if (data.getPrimary() != null) {
                Account balance = data.getPrimary();
                //插入balance
                insertBalance(chainDao, shardDao, token, tokens, account, balance);
            }
            //其他分片
            ArrayList<Account> list = data.getShards();
            if (list != null && !list.isEmpty()) {
                for (Account balance : list) {
                    insertBalance(chainDao, shardDao, token, tokens, account, balance);
                }
            }
        }
    }

    private QWToken getTokenByAddress(String address, QWToken qkcToken, List<QWToken> tokens) {
        if (TextUtils.equals(address, qkcToken.getAddress())) {
            return qkcToken;
        }

        if (tokens != null) {
            for (QWToken token : tokens) {
                if (TextUtils.equals(address, token.getAddress())) {
                    return token;
                }
            }
        }
        return null;
    }

    private void insertBalance(QWChainDao chainDao, QWShardDao shardDao,
                               QWToken qkcToken, List<QWToken> tokens,
                               QWAccount wallet, Account account) {
        if (account.getBalances() == null || account.getBalances().isEmpty()) {
            return;
        }

        //插入余额
        for (Balance b : account.getBalances()) {
            QWToken token = getTokenByAddress(b.getTokenId(), qkcToken, tokens);
            if (token != null) {
                //QKC余额
                QWChain chain = chainDao.queryChainByChain(account.getChainId());
                if (chain == null) {
                    chain = new QWChain(account.getChainId());
                    chainDao.insert(chain);
                }

                QWShard shard = shardDao.queryShardByShard(account.getShardId());
                if (shard == null) {
                    shard = new QWShard(account.getShardId());
                    shardDao.insert(shard);
                }

                QWBalance balance = queryByWTCS(wallet, token, chain, shard);
                if (BigInteger.ZERO.equals(Numeric.toBigInt(b.getBalance()))) {
                    if (balance != null) {
                        delete(balance);
                    }
                    continue;
                }

                if (balance == null) {
                    balance = new QWBalance();
                }
                //token数量
                balance.setBalance(b.getBalance());

                //多表关联
                //chain
                balance.setChain(chain);
                //shard
                balance.setQWShard(shard);
                //token
                balance.setQWToken(token);
                //钱包
                balance.setAccount(wallet); //钱包

                insert(balance);
            }
        }
    }

    //添加token余额
    public void insertBalance(QWAccount account, QWToken token, String balanceStr) {
        if (TextUtils.isEmpty(balanceStr)) {
            return;
        }

        QWBalance balance = queryByWT(account, token);
        if (balance == null) {
            balance = new QWBalance();
        }
        //token数量
        balance.setBalance(balanceStr);

        //多表关联
        //token
        balance.setQWToken(token);
        //钱包
        balance.setAccount(account); //钱包

        insert(balance);
    }

    //插入qkc native token余额
    public void insertQKCTokenBalance(QWAccount account, QWToken token, String balanceStr, QWChain chain, QWShard shard) {
        if (TextUtils.isEmpty(balanceStr)) {
            return;
        }

        QWBalance balance = queryByWTCS(account, token, chain, shard);
        if (balance == null) {
            balance = new QWBalance();
        }
        //token数量
        balance.setBalance(balanceStr);

        //多表关联
        balance.setChain(chain);
        balance.setQWShard(shard);
        //token
        balance.setQWToken(token);
        //钱包
        balance.setAccount(account); //钱包

        insert(balance);
    }

    // 添加数据
    private void insert(QWBalance data) {
        try {
            dao.createOrUpdate(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWBalance data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(QWAccount account) {
        DeleteBuilder<QWBalance, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder
                    .where()
                    .eq(QWBalance.COLUMN_NAME_ACCOUNT, account);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void delete(QWToken token) {
        DeleteBuilder<QWBalance, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder
                    .where()
                    .eq(QWBalance.COLUMN_NAME_TOKEN, token);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllQKC(List<QWAccount> list) {
        DeleteBuilder<QWBalance, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            dao.callBatchTasks(() -> {
                for (QWAccount account : list) {
                    try {
                        deleteBuilder
                                .where()
                                .eq(QWBalance.COLUMN_NAME_ACCOUNT, account);
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

    public QWBalance queryByWTCS(QWAccount account, QWToken token, QWChain chain, QWShard shard) {
        try {
            return dao.queryBuilder()
                    .where()
                    .eq(QWBalance.COLUMN_NAME_ACCOUNT, account)
                    .and()
                    .eq(QWBalance.COLUMN_NAME_TOKEN, token)
                    .and()
                    .eq(QWBalance.COLUMN_NAME_CHAIN, chain)
                    .and()
                    .eq(QWBalance.COLUMN_NAME_SHARD, shard)
                    .queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public QWBalance queryByWT(QWAccount account, QWToken token) {
        try {
            return dao.queryBuilder()
                    .where()
                    .eq(QWBalance.COLUMN_NAME_ACCOUNT, account)
                    .and()
                    .eq(QWBalance.COLUMN_NAME_TOKEN, token)
                    .queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWBalance> queryTokenAllByWT(QWAccount account, QWToken token) {
        try {
            return dao.queryBuilder()
                    .where()
                    .eq(QWBalance.COLUMN_NAME_ACCOUNT, account)
                    .and()
                    .eq(QWBalance.COLUMN_NAME_TOKEN, token)
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //清除改钱包下的所有币
    private void clearByWT(QWAccount account, QWToken token) {
        try {
            DeleteBuilder<QWBalance, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWBalance.COLUMN_NAME_ACCOUNT, account)
                    .and()
                    .eq(QWBalance.COLUMN_NAME_TOKEN, token);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
