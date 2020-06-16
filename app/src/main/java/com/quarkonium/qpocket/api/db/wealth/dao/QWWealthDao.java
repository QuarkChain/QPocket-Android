package com.quarkonium.qpocket.api.db.wealth.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.wealth.WealthDBHelper;
import com.quarkonium.qpocket.api.db.wealth.table.QWWealth;

import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWWealthDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWWealth, Integer> dao;

    public QWWealthDao(Context context) {
        try {
            this.dao = WealthDBHelper.getInstance(context).getDao(QWWealth.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //收藏不区分语言
    public List<QWWealth> queryAll() {
        QueryBuilder<QWWealth, Integer> queryBuilder = dao.queryBuilder();
        try {
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 添加数据
    public boolean insert(QWWealth data) {
        try {
            dao.createOrUpdate(data);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //批处理
    public void installAll(List<QWWealth> list) {
        try {
            dao.callBatchTasks(() -> {
                for (QWWealth flash : list) {
                    insert(flash);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        //清除旧数据
        try {
            dao.deleteBuilder().delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
