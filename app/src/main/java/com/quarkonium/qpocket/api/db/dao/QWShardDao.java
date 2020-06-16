package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.crypto.utils.Numeric;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWShardDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWShard, Integer> dao;

    public QWShardDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWShard.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //补齐到最大shard
    public void addTotal(int totalCount) {
        for (int i = 0; i < totalCount; i++) {
            String shardId = Numeric.toHexStringWithPrefix(new BigInteger(String.valueOf(i)));
            QWShard shard = queryShardByShard(shardId);
            if (shard == null) {
                shard = new QWShard();
                shard.setShard(shardId);
                insert(shard);
            }
        }
    }

    public void insertTotal(int totalCount) {
        //删除旧数据
        try {
            dao.deleteBuilder().delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < totalCount; i++) {
            String shardId = Numeric.toHexStringWithPrefix(new BigInteger(String.valueOf(i)));
            QWShard shard = new QWShard();
            shard.setShard(shardId);
            insert(shard);
        }
    }

    // 添加数据
    public void insert(QWShard data) {
        try {
            dao.createIfNotExists(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWShard data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QWShard queryShardByShard(String shard) {
        try {
            List<QWShard> list = dao.queryForEq(QWShard.COLUMN_NAME_SHARD, shard);
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
