package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWDApp;
import com.quarkonium.qpocket.api.db.table.QWRecentDApp;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * DApp的DAO类
 */
public class QWRecentDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWRecentDApp, Integer> recentDao;


    public QWRecentDao(Context context) {
        try {
            this.recentDao = WalletDBHelper.getInstance(context).getDao(QWRecentDApp.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void insert(QWRecentDApp data) {
        try {
            recentDao.createOrUpdate(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void update(QWRecentDApp data) {
        try {
            recentDao.update(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWRecentDApp data) {
        try {
            recentDao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //删除使用列表的DApp
    public void deleteRecent(String url) {
        DeleteBuilder<QWRecentDApp, Integer> deleteBuilder = recentDao.deleteBuilder();
        try {
            deleteBuilder.where()
                    .eq(QWDApp.COLUMN_NAME_URL, url);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<QWRecentDApp> queryAll(int coinType) {
        List<QWRecentDApp> list = queryRecentAll(coinType);
        if (list != null) {
            Collections.reverse(list);
        }
        return list;
    }

    public QWRecentDApp queryByBrl(String url) {
        QueryBuilder<QWRecentDApp, Integer> queryBuilder = recentDao.queryBuilder();
        try {
            queryBuilder
                    .where()
                    .eq(QWRecentDApp.COLUMN_NAME_URL, url);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //使用列表不区分国籍
    private List<QWRecentDApp> queryRecentAll(int coinType) {
        QueryBuilder<QWRecentDApp, Integer> queryBuilder = recentDao.queryBuilder();
        try {
            queryBuilder
                    .where()
                    .eq(QWDApp.COLUMN_NAME_COIN_TYPE, coinType);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
