package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWTokenListOrder;

import java.sql.SQLException;

/**
 * 操作钱包表的DAO类
 */
public class QWTokenListOrderDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWTokenListOrder, Integer> dao;

    public QWTokenListOrderDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWTokenListOrder.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void insert(QWTokenListOrder data) {
        QWTokenListOrder token = queryTokenList(data.getType(), data.getChainId());
        if (token != null) {
            delete(token);
        }
        try {
            dao.create(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWTokenListOrder data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QWTokenListOrder queryTokenList(int type, int chainId) {
        try {
            QueryBuilder<QWTokenListOrder, Integer> builder = dao.queryBuilder();
            builder.where()
                    .eq(QWTokenListOrder.COLUMN_NAME_TYPE, type)
                    .and()
                    .eq(QWTokenListOrder.COLUMN_NAME_CHAIN_ID, chainId);
            return builder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteByType(int type) {
        try {
            DeleteBuilder<QWTokenListOrder, Integer> builder = dao.deleteBuilder();
            builder.where()
                    .eq(QWTokenListOrder.COLUMN_NAME_TYPE, type);
            builder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
