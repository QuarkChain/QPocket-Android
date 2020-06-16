package com.quarkonium.qpocket.api.db.wealth.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.wealth.WealthDBHelper;
import com.quarkonium.qpocket.api.db.wealth.table.QWBanner2;

import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWBanner2Dao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWBanner2, Integer> dao;

    public QWBanner2Dao(Context context) {
        try {
            this.dao = WealthDBHelper.getInstance(context).getDao(QWBanner2.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clear(int type, String language) {
        //清除旧数据
        DeleteBuilder<QWBanner2, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder
                    .where()
                    .eq(QWBanner2.COLUMN_NAME_LOCALIZATION, language)
                    .and()
                    .eq(QWBanner2.COLUMN_NAME_TYPE, type);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clear(int type) {
        //清除旧数据
        DeleteBuilder<QWBanner2, Integer> deleteBuilder = dao.deleteBuilder();
        try {
            deleteBuilder
                    .where()
                    .eq(QWBanner2.COLUMN_NAME_TYPE, type);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public boolean insert(QWBanner2 data) {
        try {
            dao.createOrUpdate(data);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //批处理
    public void installAll(List<QWBanner2> list) {
        try {
            dao.callBatchTasks(() -> {
                for (QWBanner2 banner : list) {
                    insert(banner);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<QWBanner2> queryAll(int type, String language) {
        QueryBuilder<QWBanner2, Integer> queryBuilder = dao.queryBuilder();
        try {
            queryBuilder
                    .where()
                    .eq(QWBanner2.COLUMN_NAME_LOCALIZATION, language)
                    .and()
                    .eq(QWBanner2.COLUMN_NAME_TYPE, type);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWBanner2> queryAll(int type) {
        QueryBuilder<QWBanner2, Integer> queryBuilder = dao.queryBuilder();
        try {
            queryBuilder
                    .where()
                    .eq(QWBanner2.COLUMN_NAME_TYPE, type);
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
