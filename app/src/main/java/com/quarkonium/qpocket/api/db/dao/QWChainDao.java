package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.crypto.utils.Numeric;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWChainDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWChain, Integer> dao;

    public QWChainDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWChain.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //补齐到最大chain
    public void addTotal(int totalCount) {
        for (int i = 0; i < totalCount; i++) {
            String chainId = Numeric.toHexStringWithPrefix(new BigInteger(String.valueOf(i)));
            QWChain chain = queryChainByChain(chainId);
            if (chain == null) {
                chain = new QWChain();
                chain.setChain(chainId);
                insert(chain);
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
            String chainId = Numeric.toHexStringWithPrefix(new BigInteger(String.valueOf(i)));
            QWChain chain = new QWChain();
            chain.setChain(chainId);
            insert(chain);
        }
    }

    // 添加数据
    public void insert(QWChain data) {
        try {
            dao.createIfNotExists(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWChain data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QWChain queryChainByChain(String chain) {
        try {
            List<QWChain> list = dao.queryForEq(QWChain.COLUMN_NAME_CHAIN, chain);
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
