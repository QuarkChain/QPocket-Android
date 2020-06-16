package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;
import android.provider.Settings;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.R;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * 操作钱包表的DAO类
 */
public class QWWalletDao {

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWWallet, Integer> dao;

    private String[] mDefaultNameHD;
    private String[] mDefaultNameQKC;
    private String[] mDefaultNameETH;
    private String[] mDefaultNameTRX;
    private String[] mDefaultNameBTC;

    public QWWalletDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWWallet.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        mDefaultNameHD = new String[]{
                context.getString(R.string.create_wallet_first_name),
                context.getString(R.string.create_wallet_other_name)
        };
        mDefaultNameQKC = new String[]{
                context.getString(R.string.create_wallet_first_name_qkc),
                context.getString(R.string.create_wallet_other_name_qkc)
        };
        mDefaultNameETH = new String[]{
                context.getString(R.string.create_wallet_first_name_eth),
                context.getString(R.string.create_wallet_other_name_eth)
        };
        mDefaultNameTRX = new String[]{
                context.getString(R.string.create_wallet_first_name_trx),
                context.getString(R.string.create_wallet_other_name_trx)
        };
        mDefaultNameBTC = new String[]{
                context.getString(R.string.create_wallet_first_name_btc),
                context.getString(R.string.create_wallet_other_name_btc)
        };
    }

    // 添加数据
    public void insert(QWWallet data) {
        try {
            dao.createIfNotExists(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWWallet data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(String key) {
        try {
            DeleteBuilder<QWWallet, Integer> queryBuilder = dao.deleteBuilder();
            queryBuilder.where()
                    .eq(QWWallet.COLUMN_NAME_KEY, key);
            queryBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QWWallet queryByKey(String key) {
        try {
            QueryBuilder<QWWallet, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(QWWallet.COLUMN_NAME_KEY, key);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWWallet> queryAll() {
        try {
            QueryBuilder<QWWallet, Integer> queryBuilder = dao.queryBuilder();
            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    //改变钱包备份状态
    public void updateWalletBackup(boolean isBackup, String key) {
        try {
            UpdateBuilder<QWWallet, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.updateColumnValue(QWWallet.COLUMN_NAME_BACKUP, isBackup ? 1 : 0)
                    .where()
                    .eq(QWWallet.COLUMN_NAME_KEY, key);
            updateBuilder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //改变钱包默认选中币种
    public void updateCurrentAddress(String address, String key) {
        try {
            UpdateBuilder<QWWallet, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.updateColumnValue(QWWallet.COLUMN_NAME_CURRENT_ADDRESS, address)
                    .where()
                    .eq(QWWallet.COLUMN_NAME_KEY, key);
            updateBuilder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //改变钱包头像
    public void updateWalletIcon(String icon, String key) {
        try {
            UpdateBuilder<QWWallet, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.updateColumnValue(QWWallet.COLUMN_NAME_ICON, icon)
                    .where()
                    .eq(QWWallet.COLUMN_NAME_KEY, key);
            updateBuilder.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(QWWallet wallet) {
        try {
            dao.update(wallet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getName(int type) {
        switch (type) {
            case Constant.WALLET_TYPE_HD: {
                int count = queryWalletMaxCount(type);
                return count == 0 ? mDefaultNameHD[0] : String.format(mDefaultNameHD[1], count);
            }
            case Constant.WALLET_TYPE_QKC: {
                int count = queryWalletMaxCount(type);
                return count == 0 ? mDefaultNameQKC[0] : String.format(mDefaultNameQKC[1], count);
            }
            case Constant.WALLET_TYPE_ETH: {
                int count = queryWalletMaxCount(type);
                return count == 0 ? mDefaultNameETH[0] : String.format(mDefaultNameETH[1], count);
            }
            case Constant.WALLET_TYPE_TRX: {
                int count = queryWalletMaxCount(type);
                return count == 0 ? mDefaultNameTRX[0] : String.format(mDefaultNameTRX[1], count);
            }
        }
        return mDefaultNameHD[0];
    }

    public String getBtcName(int count) {
        return count == 0 ? mDefaultNameBTC[0] : String.format(mDefaultNameBTC[1], count);
    }

    private int queryWalletMaxCount(int type) {
        try {
            QueryBuilder<QWWallet, Integer> builder = dao.queryBuilder();
            builder.where().eq(QWWallet.COLUMN_NAME_TYPE, type);
            builder.orderBy(QWWallet.COLUMN_NAME_ID, false);
            List<QWWallet> list = builder.query();
            if (list != null && !list.isEmpty()) {
                return list.get(0).getId();
            }
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //获取最先创建的非观察钱包
    public QWWallet queryNormalWallet() {
        try {
            QueryBuilder<QWWallet, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(QWWallet.COLUMN_NAME_WATCH_ACCOUNT, "0");
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Dao<QWWallet, Integer> getDao() {
        return dao;
    }


    public static String getRandomKey(Context context) {
        String androidID = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidID + System.currentTimeMillis() + UUID.randomUUID().toString();
    }

    //获取数量
    public int querySize() {
        try {
            QueryBuilder<QWWallet, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(QWWallet.COLUMN_NAME_ID);
            return queryBuilder.query().size();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
