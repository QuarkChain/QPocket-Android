package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWAccount;

import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWAccountDao {

    private Dao<QWAccount, Integer> dao;

    public QWAccountDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWAccount.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void insert(QWAccount account) throws Exception {
        dao.createIfNotExists(account);
    }

    public void deleteByKey(String key) {
        try {
            DeleteBuilder<QWAccount, Integer> queryBuilder = dao.deleteBuilder();
            queryBuilder.where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key);
            queryBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteByAddress(String address) {
        try {
            DeleteBuilder<QWAccount, Integer> queryBuilder = dao.deleteBuilder();
            queryBuilder.where()
                    .eq(QWAccount.COLUMN_NAME_ADDRESS, address);
            queryBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 通过用户address查找
    public QWAccount queryByAddress(String address) {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder
                    .where()
                    .eq(QWAccount.COLUMN_NAME_ADDRESS, address);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWAccount> queryByKey(String key) {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder
                    .orderBy(QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX, true)
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWAccount> queryAllQKC() {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(QWAccount.COLUMN_NAME_ID, QWAccount.COLUMN_NAME_KEY, QWAccount.COLUMN_NAME_ADDRESS, QWAccount.COLUMN_NAME_TYPE);
            queryBuilder
                    .where()
                    .eq(QWAccount.COLUMN_NAME_TYPE, Constant.ACCOUNT_TYPE_QKC);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public QWAccount queryAllParamsByAddress(String address) {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(QWAccount.COLUMN_NAME_ID, QWAccount.COLUMN_NAME_KEY,
                    QWAccount.COLUMN_NAME_ADDRESS, QWAccount.COLUMN_NAME_TYPE,
                    QWAccount.COLUMN_NAME_NAME, QWAccount.COLUMN_NAME_ICON,
                    QWAccount.COLUMN_NAME_PATH_INDEX, QWAccount.COLUMN_NAME_PARENT_PUBKEY,
                    QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX);
            queryBuilder
                    .where()
                    .eq(QWAccount.COLUMN_NAME_ADDRESS, address);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWAccount> queryParamsByKey(String key) {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(QWAccount.COLUMN_NAME_ID, QWAccount.COLUMN_NAME_KEY,
                    QWAccount.COLUMN_NAME_ADDRESS, QWAccount.COLUMN_NAME_TYPE,
                    QWAccount.COLUMN_NAME_NAME, QWAccount.COLUMN_NAME_ICON,
                    QWAccount.COLUMN_NAME_PATH_INDEX, QWAccount.COLUMN_NAME_PARENT_PUBKEY,
                    QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX);
            queryBuilder
                    .orderBy(QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX, true)
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWAccount> queryAllParams() {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(QWAccount.COLUMN_NAME_ID, QWAccount.COLUMN_NAME_KEY,
                    QWAccount.COLUMN_NAME_ADDRESS, QWAccount.COLUMN_NAME_TYPE,
                    QWAccount.COLUMN_NAME_NAME, QWAccount.COLUMN_NAME_ICON,
                    QWAccount.COLUMN_NAME_PATH_INDEX, QWAccount.COLUMN_NAME_PARENT_PUBKEY,
                    QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //获取数量
    public int querySizeByKey(String key) {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(QWAccount.COLUMN_NAME_ID);
            return queryBuilder
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key)
                    .query()
                    .size();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //是否存在
    public boolean hasExist(String address) {
        QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
        queryBuilder.selectColumns(QWAccount.COLUMN_NAME_ADDRESS);
        try {
            QWAccount wallet = queryBuilder
                    .where()
                    .eq(QWAccount.COLUMN_NAME_ADDRESS, address)
                    .queryForFirst();
            return wallet != null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasExistAccount(String key) {
        QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
        queryBuilder.selectColumns(QWAccount.COLUMN_NAME_KEY);
        try {
            QWAccount wallet = queryBuilder
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key)
                    .queryForFirst();
            return wallet != null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateAccount(List<QWAccount> list) {
        try {
            dao.callBatchTasks(() -> {
                for (QWAccount account : list) {
                    try {
                        dao.update(account);
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


    //改变钱包头像
    public void updateWalletIcon(String icon, String key) {
        try {
            UpdateBuilder<QWAccount, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.updateColumnValue(QWAccount.COLUMN_NAME_ICON, icon)
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key);
            updateBuilder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //改变HD钱包下所有币种名称
    public void updateWalletName(String name, String key) {
        try {
            UpdateBuilder<QWAccount, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.updateColumnValue(QWAccount.COLUMN_NAME_NAME, name)
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key)
                    .and()
                    .eq(QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX, 0);
            updateBuilder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //改变HD钱包指定币种名称
    public void updateAccountName(String name, String address) {
        try {
            UpdateBuilder<QWAccount, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.updateColumnValue(QWAccount.COLUMN_NAME_NAME, name)
                    .where()
                    .eq(QWAccount.COLUMN_NAME_ADDRESS, address);
            updateBuilder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //获取HD钱包对应币种下一个account的路径，如果数组中间某一个被删除，则补上
    public int queryNextAccountPathIndex(String key, int type, int start) {
        try {
            QueryBuilder<QWAccount, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX);
            queryBuilder
                    .orderBy(QWAccount.COLUMN_NAME_PATH_ACCOUNT_INDEX, true)
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key)
                    .and()
                    .eq(QWAccount.COLUMN_NAME_TYPE, type);
            List<QWAccount> list = queryBuilder.query();
            if (list != null && !list.isEmpty()) {
                int size = list.size();
                //获取最后一个account
                QWAccount lastAccount = list.get(size - 1);

                //一个都不缺
                if (lastAccount.getPathAccountIndex() == size - 1) {
                    if (start > size - 1) {
                        return start;
                    } else {
                        return size;
                    }
                }

                //有缺少
                if (start > lastAccount.getPathAccountIndex()) {
                    return start;
                }
                for (QWAccount account : list) {
                    if (account.getPathAccountIndex() < start) {
                    } else if (account.getPathAccountIndex() == start) {
                        start++;
                    } else {
                        return start;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return start;
    }
}
