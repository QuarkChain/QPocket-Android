package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWBannerApp;

import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWBannerDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWBannerApp, Integer> dao;

    public QWBannerDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWBannerApp.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void insert(QWBannerApp data) {
        try {
            dao.createOrUpdate(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void update(QWBannerApp data) {
        try {
            dao.update(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWBannerApp data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearDataByType(int coinType, String language) {
        DeleteBuilder deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder
                    .where()
                    .eq(QWBannerApp.COLUMN_NAME_COIN_TYPE, coinType)
                    .and()
                    .eq(QWBannerApp.COLUMN_NAME_LOCALIZATION, language);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<QWBannerApp> queryAll(int coinType, String language) {
        QueryBuilder<QWBannerApp, Integer> queryBuilder = dao.queryBuilder();
        try {
            queryBuilder
                    .orderBy("order", true)
                    .where()
                    .eq(QWBannerApp.COLUMN_NAME_COIN_TYPE, coinType)
                    .and()
                    .eq(QWBannerApp.COLUMN_NAME_LOCALIZATION, language);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteAll() {
        try {
            dao.deleteBuilder().delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
