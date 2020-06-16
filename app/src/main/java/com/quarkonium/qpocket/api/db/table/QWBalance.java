package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * QWShard实体类，分片id相关类
 * 数据库中的QWBalance表和Wallet Token Shard表是关联的，因此我们需要在balance表中配置外键
 * <p>
 * foreignColumnName：外键约束指向的类中的属性名
 * foreign：当前字段是否是外键
 * foreignAutoRefresh：如果这个属性设置为true，在关联查询的时候就不需要再调用refresh()方法了
 */
@DatabaseTable(tableName = "balance")
public class QWBalance implements Parcelable {

    public static final Creator<QWBalance> CREATOR = new Creator<QWBalance>() {
        @Override
        public QWBalance createFromParcel(Parcel in) {
            return new QWBalance(in);
        }

        @Override
        public QWBalance[] newArray(int size) {
            return new QWBalance[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(balance);

        parcel.writeParcelable(account, i);
        parcel.writeParcelable(token, i);
        parcel.writeParcelable(shard, i);
        parcel.writeParcelable(chain, i);
    }

    // 表中各个字段的名称
    private static final String COLUMN_NAME_ID = "id";
    private static final String COLUMN_NAME_BALANCE = "balance";//token数量

    //关联查询
    public static final String COLUMN_NAME_ACCOUNT = "account_id";
    public static final String COLUMN_NAME_TOKEN = "token_id";
    public static final String COLUMN_NAME_SHARD = "shard_id";
    public static final String COLUMN_NAME_CHAIN = "chain_id";


    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_BALANCE)
    private String balance;

    @DatabaseField(columnName = COLUMN_NAME_ACCOUNT, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWAccount account;

    @DatabaseField(columnName = COLUMN_NAME_TOKEN, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWToken token;

    @DatabaseField(columnName = COLUMN_NAME_SHARD, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWShard shard;

    @DatabaseField(columnName = COLUMN_NAME_CHAIN, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWChain chain;

    public QWBalance() {
    }

    public QWBalance(Parcel in) {
        id = in.readInt();
        balance = in.readString();
        account = in.readParcelable(QWAccount.class.getClassLoader());
        token = in.readParcelable(QWToken.class.getClassLoader());
        shard = in.readParcelable(QWShard.class.getClassLoader());
        chain = in.readParcelable(QWChain.class.getClassLoader());
    }

    public QWBalance(String balance, QWAccount mWallet, QWToken mQWToken, QWShard mQWShard, QWChain chain) {
        this.balance = balance;
        this.account = mWallet;
        this.token = mQWToken;
        this.shard = mQWShard;
        this.chain = chain;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public QWAccount getAccount() {
        return account;
    }

    public void setAccount(QWAccount mWallet) {
        this.account = mWallet;
    }

    public QWToken getQWToken() {
        return token;
    }

    public void setQWToken(QWToken mQWToken) {
        this.token = mQWToken;
    }

    public QWShard getQWShard() {
        return shard;
    }

    public void setQWShard(QWShard mQWShard) {
        this.shard = mQWShard;
    }

    public QWChain getChain() {
        return chain;
    }

    public void setChain(QWChain chain) {
        this.chain = chain;
    }

    @Override
    public String toString() {
        return "QWBalance{" +
                "id=" + id +
                ", balance='" + balance + '\'' +
                ", mWallet=" + account +
                ", mQWToken=" + token +
                ", mQWShard=" + shard +
                ", mQWChain=" + chain +
                '}';
    }
}
