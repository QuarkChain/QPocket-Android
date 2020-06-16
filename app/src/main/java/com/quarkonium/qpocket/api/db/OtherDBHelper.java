package com.quarkonium.qpocket.api.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.quarkonium.qpocket.api.db.address.table.QWAddressBook;

import java.sql.SQLException;

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
public class OtherDBHelper extends OrmLiteSqliteOpenHelper {

    private static OtherDBHelper instance;

    // 获取本类单例对象的方法
    public static synchronized OtherDBHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (OtherDBHelper.class) {
                if (instance == null) {
                    instance = new OtherDBHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private static final String DB_NAME = "other.db";
    private static final int VERSION = 1;

    private OtherDBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override // 创建数据库时调用的方法
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, QWAddressBook.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override // 数据库版本更新时调用的方法
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    }
}
