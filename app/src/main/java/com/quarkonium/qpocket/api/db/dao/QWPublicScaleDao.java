package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWPublicScale;
import com.quarkonium.qpocket.api.db.table.QWToken;

import java.sql.SQLException;
import java.util.List;

/**
 * 操作PublicScale表的DAO类
 */
public class QWPublicScaleDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWPublicScale, Integer> dao;

    public QWPublicScaleDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWPublicScale.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void insert(QWPublicScale data) {
        try {
            dao.createOrUpdate(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void deleteAll(int accountType, int chainId) {
        try {
            DeleteBuilder<QWPublicScale, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWPublicScale.COLUMN_NAME_TYPE, accountType)
                    .and()
                    .eq(QWPublicScale.COLUMN_NAME_CHAIN_ID, chainId);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void deleteAll(int accountType) {
        try {
            DeleteBuilder<QWPublicScale, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWPublicScale.COLUMN_NAME_TYPE, accountType);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<QWPublicScale> queryAll(int accountType, int chainId) {
        try {
            QueryBuilder<QWPublicScale, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where()
                    .eq(QWPublicScale.COLUMN_NAME_TYPE, accountType)
                    .and()
                    .eq(QWPublicScale.COLUMN_NAME_CHAIN_ID, chainId);
            queryBuilder.orderBy(QWPublicScale.COLUMN_NAME_ORDER, false);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public QWPublicScale queryByToken(QWToken token) {
        try {
            QueryBuilder<QWPublicScale, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(QWPublicScale.COLUMN_NAME_KEY_ADDRESS, token.getAddress());
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(QWPublicScale publicScale) {
        try {
            dao.update(publicScale);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
