package com.quarkonium.qpocket.api.db.table;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.MainApplication;

/**
 * QuarkWallet，存储数据库中钱包表中的数据
 * <p>
 * 注解：
 * DatabaseTable：通过其中的tableName属性指定数据库名称
 * DatabaseField：代表数据表中的一个字段
 * ForeignCollectionField：一对多关联，表示一个QuarkWallet关联着多个事务（必须使用ForeignCollection集合）
 * <p>
 * <p>
 * 属性：
 * id：当前字段是不是id字段（一个实体类中只能设置一个id字段）
 * columnName：表示当前属性在表中代表哪个字段
 * generatedId：设置属性值在数据表中的数据是否自增
 * useGetSet：是否使用Getter/Setter方法来访问这个字段
 * canBeNull：字段是否可以为空，默认值是true
 * unique：是否唯一
 * defaultValue：设置这个字段的默认值
 */
@DatabaseTable(tableName = "account") // 指定数据表的名称
public class QWAccount implements Parcelable {
    public static final Creator<QWAccount> CREATOR = new Creator<QWAccount>() {
        @Override
        public QWAccount createFromParcel(Parcel in) {
            return new QWAccount(in);
        }

        @Override
        public QWAccount[] newArray(int size) {
            return new QWAccount[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(address);
        parcel.writeString(key);
        parcel.writeInt(type);

        parcel.writeString(name);
        parcel.writeString(icon);

        parcel.writeInt(bitCoinIndex);
        parcel.writeString(bitCoinPubk);

        parcel.writeInt(pathAccountIndex);

        parcel.writeDouble(mTotalPrice);
    }

    // 定义字段在数据库中的字段名
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_KEY = "key";
    public static final String COLUMN_NAME_ADDRESS = "address";//钱包地址
    public static final String COLUMN_NAME_TYPE = "type";//钱包类型

    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_ICON = "icon";

    public static final String COLUMN_NAME_PATH_INDEX = "btcIndex";//收款子地址路径
    public static final String COLUMN_NAME_PARENT_PUBKEY = "btcPubK";//父公钥

    public static final String COLUMN_NAME_PATH_ACCOUNT_INDEX = "pathAccountIndex";//HD钱包路径标准中account字段下标 44'/60'/account'/0/0

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_ADDRESS, canBeNull = false, unique = true)
    private String address;

    @DatabaseField(columnName = COLUMN_NAME_KEY, canBeNull = false)
    private String key;

    @DatabaseField(columnName = COLUMN_NAME_TYPE, defaultValue = "1")
    private int type;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    private String name;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    private String icon;

    @DatabaseField(columnName = COLUMN_NAME_PATH_INDEX)
    private int bitCoinIndex;

    @DatabaseField(columnName = COLUMN_NAME_PARENT_PUBKEY)
    private String bitCoinPubk;

    @DatabaseField(columnName = COLUMN_NAME_PATH_ACCOUNT_INDEX, defaultValue = "0")
    private int pathAccountIndex;

    @ForeignCollectionField(eager = true)
    private ForeignCollection<QWBalance> balances;

    @ForeignCollectionField(eager = true)
    private ForeignCollection<QWTransaction> transactions;

    private double mTotalPrice;

    public QWAccount() {
    }

    public QWAccount(String address) {
        this.address = address;
    }


    private QWAccount(Parcel in) {
        id = in.readInt();
        address = in.readString();
        key = in.readString();
        type = in.readInt();

        name = in.readString();
        icon = in.readString();

        bitCoinIndex = in.readInt();
        bitCoinPubk = in.readString();

        pathAccountIndex = in.readInt();

        mTotalPrice = in.readDouble();
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public ForeignCollection<QWBalance> getBalances() {
        return balances;
    }

    public void setBalances(ForeignCollection<QWBalance> balances) {
        this.balances = balances;
    }

    public ForeignCollection<QWTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(ForeignCollection<QWTransaction> transactions) {
        this.transactions = transactions;
    }

    public void setTotalPrice(double mTotalPrice) {
        this.mTotalPrice = mTotalPrice;
    }

    public double getTotalPrice() {
        return mTotalPrice;
    }

    public void setBitCoinIndex(int bitCoinIndex) {
        this.bitCoinIndex = bitCoinIndex;
    }

    public int getBitCoinIndex() {
        return bitCoinIndex;
    }

    public void setBitCoinPubk(String bitCoinPubk) {
        this.bitCoinPubk = bitCoinPubk;
    }

    public String getBitCoinPubk() {
        return bitCoinPubk;
    }

    public void setPathAccountIndex(int pathAccountIndex) {
        this.pathAccountIndex = pathAccountIndex;
    }

    public int getPathAccountIndex() {
        return pathAccountIndex;
    }

    public void setWalletType(int walletType) {
        switch (walletType) {
            case Constant.WALLET_TYPE_QKC:
                type = Constant.ACCOUNT_TYPE_QKC;
                break;
            case Constant.WALLET_TYPE_ETH:
                type = Constant.ACCOUNT_TYPE_ETH;
                break;
            case Constant.WALLET_TYPE_TRX:
                type = Constant.ACCOUNT_TYPE_TRX;
                break;
        }
    }

    @Override
    public String toString() {
        return "QWAccount{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", key='" + key + '\'' +
                ", type=" + type +
                '}';
    }

    public String getShardAddress() {
        if (type == Constant.ACCOUNT_TYPE_QKC) {
            return getShardAddress(getAddress());
        }
        return getAddress();
    }

    //根据当前选中分片获取地址
    public static String getShardAddress(String address) {
        Context context = MainApplication.getContext();
        String chain = SharedPreferencesUtils.getCurrentChain(context, address);
        String shard = SharedPreferencesUtils.getCurrentShard(context, address);
        return Numeric.selectChainAndShardAddress(address, chain, shard);
    }

    public boolean isEth() {
        return type == Constant.ACCOUNT_TYPE_ETH;
    }

    public boolean isQKC() {
        return type == Constant.ACCOUNT_TYPE_QKC;
    }

    public boolean isTRX() {
        return type == Constant.ACCOUNT_TYPE_TRX;
    }

    public boolean isBTC() {
        return false;
    }

    public boolean isBTCSegWit() {
        return false;
    }

    public boolean isAllBTC() {
        return isBTC() || isBTCSegWit();
    }
}
