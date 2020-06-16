package com.quarkonium.qpocket.api.db.address.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.OtherDBHelper;
import com.quarkonium.qpocket.api.db.address.table.QWAddressBook;

import java.sql.SQLException;
import java.util.List;

/**
 * 地址簿的DAO类
 */
public class QWAddressBookDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWAddressBook, Integer> dao;

    public QWAddressBookDao(Context context) {
        try {
            this.dao = OtherDBHelper.getInstance(context).getDao(QWAddressBook.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<QWAddressBook> queryAll() {
        try {
            return dao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWAddressBook> queryAll(int coinType) {
        QueryBuilder<QWAddressBook, Integer> builder = dao.queryBuilder();
        try {
            builder.where().eq(QWAddressBook.COLUMN_NAME_COIN_TYPE, coinType);
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 添加数据
    public boolean insert(QWAddressBook data) {
        try {
            dao.create(data);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(QWAddressBook data) {
        try {
            dao.update(data);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void remove(QWAddressBook data) {
        //清除旧数据
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
