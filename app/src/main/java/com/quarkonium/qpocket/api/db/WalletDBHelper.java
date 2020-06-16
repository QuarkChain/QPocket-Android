package com.quarkonium.qpocket.api.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.quarkonium.qpocket.api.db.table.DAppFavorite;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWBannerApp;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWExChangeDApp;
import com.quarkonium.qpocket.api.db.table.QWFinanceDApp;
import com.quarkonium.qpocket.api.db.table.QWGameDApp;
import com.quarkonium.qpocket.api.db.table.QWHighRiskDApp;
import com.quarkonium.qpocket.api.db.table.QWHotDApp;
import com.quarkonium.qpocket.api.db.table.QWPublicScale;
import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWRecentDApp;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTokenListOrder;
import com.quarkonium.qpocket.api.db.table.QWTokenTransaction;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.api.db.table.QWUtilitiesDApp;
import com.quarkonium.qpocket.api.db.table.QWUtilitiesTestNetDApp;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.db.table.QWWalletToken;
import com.quarkonium.qpocket.api.db.table.UTXO;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 * 数据库操作管理工具类
 * <p>
 * 我们需要自定义一个类继承自ORMlite给我们提供的OrmLiteSqliteOpenHelper，创建一个构造方法，重写两个方法onCreate()和onUpgrade()
 * 在onCreate()方法中使用TableUtils类中的createTable()方法初始化数据表
 * 在onUpgrade()方法中我们可以先删除所有表，然后调用onCreate()方法中的代码重新创建表
 * <p>
 * 我们需要对这个类进行单例，保证整个APP中只有一个SQLite Connection对象
 * <p>
 * 这个类通过一个Map集合来管理APP中所有的DAO，只有当第一次调用这个DAO类时才会创建这个对象（并存入Map集合中）
 * 其他时候都是直接根据实体类的路径从Map集合中取出DAO对象直接调用
 */
public class WalletDBHelper extends OrmLiteSqliteOpenHelper {

    private static WalletDBHelper instance;

    // 获取本类单例对象的方法
    public static synchronized WalletDBHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (WalletDBHelper.class) {
                if (instance == null) {
                    instance = new WalletDBHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private static final String DB_NAME = "wallet.db";
    private static final int VERSION = 11;

    private WalletDBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override // 创建数据库时调用的方法
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, QWShard.class);
            TableUtils.createTable(connectionSource, QWToken.class);
            TableUtils.createTable(connectionSource, QWAccount.class);
            TableUtils.createTable(connectionSource, QWWallet.class);
            TableUtils.createTable(connectionSource, QWBalance.class);
            TableUtils.createTable(connectionSource, QWTransaction.class);
            TableUtils.createTable(connectionSource, QWPublicScale.class);
            TableUtils.createTable(connectionSource, QWWalletToken.class);
            TableUtils.createTable(connectionSource, QWTokenListOrder.class);
            TableUtils.createTable(connectionSource, QWPublicTokenTransaction.class);

            TableUtils.createTable(connectionSource, QWExChangeDApp.class);
            TableUtils.createTable(connectionSource, QWGameDApp.class);
            TableUtils.createTable(connectionSource, QWBannerApp.class);

            TableUtils.createTable(connectionSource, QWTokenTransaction.class);

            TableUtils.createTable(connectionSource, QWChain.class);
            TableUtils.createTable(connectionSource, DAppFavorite.class);
            TableUtils.createTable(connectionSource, QWRecentDApp.class);
            TableUtils.createTable(connectionSource, QWHotDApp.class);
            TableUtils.createTable(connectionSource, QWHighRiskDApp.class);
            TableUtils.createTable(connectionSource, QWFinanceDApp.class);
            TableUtils.createTable(connectionSource, QWUtilitiesDApp.class);

            TableUtils.createTable(connectionSource, QWUtilitiesTestNetDApp.class);

            TableUtils.createTable(connectionSource, UTXO.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override // 数据库版本更新时调用的方法
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    }

    //根据注解添加字段
    private void addColumns(SQLiteDatabase database, String tableName, Class clazz) {
        Cursor mCursor = database.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
        for (Field field : clazz.getDeclaredFields()) {
            try {
                DatabaseField annotationField = field.getAnnotation(DatabaseField.class);
                if (annotationField != null && !annotationField.foreign()) {
                    String columnName = field.getName();
                    boolean hasColumn = mCursor.getColumnIndex(columnName) != -1;
                    if (!hasColumn) {
                        String columnType = field.getType().getSimpleName();
                        if (columnType.equals(String.class.getSimpleName())) {
                            columnType = "TEXT";
                        } else {
                            columnType = columnType.toUpperCase();
                        }
                        database.execSQL(MessageFormat.format("ALTER TABLE `{0}` ADD COLUMN {1} {2};", tableName, columnName, columnType));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mCursor.close();
    }
}
