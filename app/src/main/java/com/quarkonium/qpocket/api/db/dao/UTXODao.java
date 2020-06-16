package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.UTXO;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class UTXODao {

    private Dao<UTXO, Integer> dao;

    public UTXODao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(UTXO.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    private void insert(UTXO account) {
        try {
            dao.createOrUpdate(account);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void insertList(List<UTXO> list) {
        try {
            dao.callBatchTasks(() -> {
                for (UTXO utxo : list) {
                    insert(utxo);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void deleteAll(String key) {
        try {
            DeleteBuilder<UTXO, Integer> queryBuilder = dao.deleteBuilder();
            queryBuilder.where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key);
            queryBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public List<UTXO> queryByKey(String key) {
        try {
            QueryBuilder<UTXO, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder
                    .where()
                    .eq(QWAccount.COLUMN_NAME_KEY, key);
            List<UTXO> list = queryBuilder.query();
            if (list != null && !list.isEmpty()) {
                //随机排序
                Collections.shuffle(list);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
