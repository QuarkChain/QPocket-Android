package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWalletToken;
import com.quarkonium.qpocket.util.QWWalletUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWWalletTokenDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWWalletToken, Integer> dao;

    public QWWalletTokenDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWWalletToken.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void insert(QWWalletToken data) {
        QWWalletToken token = queryByWalletAndToken(data.getAccountAddress(), data.getTokenAddress());
        if (token != null) {
            token.setIsOpen(1);
            update(token);
            return;
        }

        try {
            data.setIsOpen(1);
            dao.create(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新数据
    private void update(QWWalletToken data) {
        try {
            dao.update(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(String accountAddress, String tokenAddress) {
        try {
            DeleteBuilder<QWWalletToken, Integer> builder = dao.deleteBuilder();
            builder.where()
                    .eq(QWWalletToken.COLUMN_NAME_ACCOUNT_ADDRESS, accountAddress)
                    .and()
                    .eq(QWWalletToken.COLUMN_NAME_TOKEN_ADDRESS, tokenAddress);
            builder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 关闭开关
    public void closeToken(String accountAddress, String tokenAddress) {
        QWWalletToken token = queryByWalletAndToken(accountAddress, tokenAddress);
        if (token != null) {
            token.setIsOpen(0);
            update(token);
        }
    }

    public void closeToken(QWToken token) {
        UpdateBuilder<QWWalletToken, Integer> updateBuilder = dao.updateBuilder();
        try {
            updateBuilder.updateColumnValue(QWWalletToken.COLUMN_NAME_IS_OPEN, 0)
                    .where()
                    .eq(QWWalletToken.COLUMN_NAME_TOKEN_ADDRESS, token.getAddress());
            updateBuilder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public List<QWWalletToken> queryByWallet(String walletAddress) {
        try {
            QueryBuilder<QWWalletToken, Integer> builder = dao.queryBuilder();
            builder.where()
                    .eq(QWWalletToken.COLUMN_NAME_ACCOUNT_ADDRESS, walletAddress)
                    .and()
                    .eq(QWWalletToken.COLUMN_NAME_IS_OPEN, 1);
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public QWWalletToken queryByWalletAndToken(String walletAddress, String tokenAddress) {
        try {
            QueryBuilder<QWWalletToken, Integer> builder = dao.queryBuilder();
            builder.where()
                    .eq(QWWalletToken.COLUMN_NAME_ACCOUNT_ADDRESS, walletAddress)
                    .and()
                    .eq(QWWalletToken.COLUMN_NAME_TOKEN_ADDRESS, tokenAddress);
            return builder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteQKC() {
        List<QWWalletToken> deleteList = new ArrayList<>();
        try {
            List<QWWalletToken> list = dao.queryForAll();
            for (QWWalletToken token : list) {
                if (QWWalletUtils.isQKCValidAddress(token.getAccountAddress())) {
                    deleteList.add(token);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!deleteList.isEmpty()) {
            try {
                dao.callBatchTasks(() -> {
                    int size = deleteList.size();
                    for (int i = 0; i < size; i++) {
                        try {
                            dao.delete(deleteList.get(i));
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
}
