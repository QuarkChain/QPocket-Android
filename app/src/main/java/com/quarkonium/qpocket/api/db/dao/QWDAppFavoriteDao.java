package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.DAppFavorite;

import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWDAppFavoriteDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<DAppFavorite, Integer> dao;

    public QWDAppFavoriteDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(DAppFavorite.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //收藏不区分语言
    public List<DAppFavorite> queryAll(int type) {
        QueryBuilder<DAppFavorite, Integer> queryBuilder = dao.queryBuilder();
        try {
            queryBuilder
                    .orderBy(DAppFavorite.COLUMN_NAME_MODIFY_TIME, false)
                    .where()
                    .eq(DAppFavorite.COLUMN_NAME_COIN_TYPE, type);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DAppFavorite queryByBrl(String url) {
        QueryBuilder<DAppFavorite, Integer> queryBuilder = dao.queryBuilder();
        try {
            queryBuilder
                    .where()
                    .eq(DAppFavorite.COLUMN_NAME_URL, url);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //批处理
    public void updateAll(List<DAppFavorite> list) {
        try {
            dao.callBatchTasks(() -> {
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    try {
                        dao.update(list.get(i));
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

    public boolean update(DAppFavorite data) {
        try {
            dao.update(data);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 添加数据
    public boolean insert(DAppFavorite data) {
        try {
            dao.createOrUpdate(data);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(String url) {
        DeleteBuilder<DAppFavorite, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder.where()
                    .eq(DAppFavorite.COLUMN_NAME_URL, url);
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 删除数据
    public void delete(DAppFavorite data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
