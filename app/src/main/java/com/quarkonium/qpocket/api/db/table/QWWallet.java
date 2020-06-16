package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.util.QWWalletUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
@DatabaseTable(tableName = "wallet") // 指定数据表的名称
public class QWWallet implements Parcelable {
    public static final Creator<QWWallet> CREATOR = new Creator<QWWallet>() {
        @Override
        public QWWallet createFromParcel(Parcel in) {
            return new QWWallet(in);
        }

        @Override
        public QWWallet[] newArray(int size) {
            return new QWWallet[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(key);
        parcel.writeInt(type);
        parcel.writeString(name);
        parcel.writeString(icon);
        parcel.writeString(hint);
        parcel.writeInt(isBackup);
        parcel.writeInt(isWatch);
        parcel.writeString(currentAddress);

        parcel.writeInt(btcSupportSegWit ? 1 : 0);
        parcel.writeInt(btcSegWitState ? 1 : 0);
        parcel.writeString(btcSegWitPathList);

        parcel.writeString(ledgerDeviceId);
        parcel.writeString(ledgerPath);
    }

    // 定义字段在数据库中的字段名
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_KEY = "key";//钱包主键
    public static final String COLUMN_NAME_TYPE = "type";//钱包类型 hd eth qkc
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_ICON = "icon";
    private static final String COLUMN_NAME_PASSWORD_HINT = "hint";
    public static final String COLUMN_NAME_BACKUP = "isBackup";
    public static final String COLUMN_NAME_WATCH_ACCOUNT = "isWatch"; //是否为Watch Account
    public static final String COLUMN_NAME_CURRENT_ADDRESS = "currentAddress"; //当前默认币种钱包Account
    private static final String COLUMN_NAME_WATCH_LEDGER = "ledgeDeviceId"; //硬件钱包ID
    private static final String COLUMN_NAME_WATCH_LEDGER_PATH = "ledgePath"; //硬件钱包路径

    private static final String COLUMN_NAME_BTC_SUPPORT_SEGWIT = "btcSupportSegwit"; //钱包BTC币种是否支持segWit
    private static final String COLUMN_NAME_BTC_SHOW_SEGWIT = "btcSegwit"; //0号路径BTC钱包，是否显示隔离见证
    private static final String COLUMN_NAME_BTC_SHOW_SEGWIT_LIST = "btcSegWitPathList"; //Wallet下所有非0号路径子BTC钱包显示隔离见证的下标

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_KEY, canBeNull = false, unique = true)
    private String key;

    @DatabaseField(columnName = COLUMN_NAME_TYPE, defaultValue = "0")
    private int type;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    private String name;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    private String icon;

    @DatabaseField(columnName = COLUMN_NAME_PASSWORD_HINT)
    private String hint;

    @DatabaseField(columnName = COLUMN_NAME_BACKUP, defaultValue = "0")
    private int isBackup;

    @DatabaseField(columnName = COLUMN_NAME_WATCH_ACCOUNT, defaultValue = "0")
    private int isWatch;

    @DatabaseField(columnName = COLUMN_NAME_CURRENT_ADDRESS, canBeNull = false)
    private String currentAddress;

    @DatabaseField(columnName = COLUMN_NAME_BTC_SUPPORT_SEGWIT)
    private boolean btcSupportSegWit;

    @DatabaseField(columnName = COLUMN_NAME_BTC_SHOW_SEGWIT)
    private boolean btcSegWitState;

    @DatabaseField(columnName = COLUMN_NAME_BTC_SHOW_SEGWIT_LIST)
    private String btcSegWitPathList;

    @DatabaseField(columnName = COLUMN_NAME_WATCH_LEDGER)
    private String ledgerDeviceId;

    @DatabaseField(columnName = COLUMN_NAME_WATCH_LEDGER_PATH)
    private String ledgerPath;

    private List<QWAccount> mAccountList;

    private QWAccount mCurrentAccount;

    public QWWallet() {
    }

    public QWWallet(String key) {
        this.key = key;
    }

    public QWWallet(String key, int type, String name, String icon, String hint,
                    int isBackup, int isWatch, String currentAddress,
                    boolean supportSegWit, boolean segWitState,
                    String btcSegWitPathList,
                    String ledgerId, String ledgerPath) {
        this.key = key;
        this.type = type;
        this.name = name;
        this.icon = icon;
        this.hint = hint;
        this.isBackup = isBackup;
        this.isWatch = isWatch;
        this.currentAddress = currentAddress;
        this.btcSupportSegWit = supportSegWit;
        this.btcSegWitState = segWitState;
        this.btcSegWitPathList = btcSegWitPathList;
        this.ledgerDeviceId = ledgerId;
        this.ledgerPath = ledgerPath;
    }

    public QWWallet(Parcel in) {
        id = in.readInt();
        key = in.readString();
        type = in.readInt();
        name = in.readString();
        icon = in.readString();
        hint = in.readString();
        isBackup = in.readInt();
        isWatch = in.readInt();
        currentAddress = in.readString();

        btcSupportSegWit = in.readInt() == 1;
        btcSegWitState = in.readInt() == 1;
        btcSegWitPathList = in.readString();

        this.ledgerDeviceId = in.readString();
        this.ledgerPath = in.readString();
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public int getIsBackup() {
        return isBackup;
    }

    public void setIsBackup(int isBackup) {
        this.isBackup = isBackup;
    }

    public int getIsWatch() {
        return isWatch;
    }

    public void setIsWatch(int isWatch) {
        this.isWatch = isWatch;
    }

    public void setCurrentAddress(String currentAddress) {
        this.currentAddress = currentAddress;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public List<QWAccount> getAccountList() {
        return mAccountList;
    }

    public void setAccountList(List<QWAccount> mAccountList) {
        this.mAccountList = mAccountList;
    }

    public QWAccount getCurrentAccount() {
        return mCurrentAccount;
    }

    public void setCurrentAccount(QWAccount mCurrentAccount) {
        this.mCurrentAccount = mCurrentAccount;
    }

    //确定当前钱包btc币种显示隔离见证还是普通地址，兼容以前的钱包数据库
    @Deprecated
    private void setShowBTCSegWit(boolean btcSegWit) {
        this.btcSegWitState = btcSegWit;
    }

    //兼容以前的钱包数据库
    @Deprecated
    private boolean isShowBTCSegWit() {
        return btcSegWitState;
    }

    private List<Integer> parseBTCNormalList() {
        if (!TextUtils.isEmpty(btcSegWitPathList)) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Integer>>() {
            }.getType();
            List<Integer> list = gson.fromJson(btcSegWitPathList, type);
            return list == null ? new ArrayList<>() : list;
        }
        return new ArrayList<>();
    }

    //切换某个路径子钱包是否显示隔离见证
    public void setShowBTCSegWit(int pathDepth, boolean btcSegWit) {
        if (pathDepth == 0) {
            //0号路径走原来逻辑
            setShowBTCSegWit(btcSegWit);
            return;
        }

        HashSet<Integer> set = new HashSet<>();
        set.addAll(parseBTCNormalList());
        //更新数据
        if (btcSegWit) {
            set.add(pathDepth);
        } else {
            set.remove(pathDepth);
        }

        if (set.size() == 0) {
            btcSegWitPathList = "";
            return;
        }
        Gson gson = new Gson();
        btcSegWitPathList = gson.toJson(set);
    }

    //获取指定路径钱包是否展示隔离见证account
    public boolean isShowBTCSegWit(int pathDepth) {
        if (pathDepth == 0) {
            //0号走原来逻辑
            return isShowBTCSegWit();
        }

        List<Integer> list = parseBTCNormalList();
        for (int index : list) {
            if (index == pathDepth) {
                //隔离见证列表中存在该下标
                return true;
            }
        }
        return false;
    }

    public void setBtcSegWitPathList(String btcSegWitPathList) {
        this.btcSegWitPathList = btcSegWitPathList;
    }

    public String getBtcSegWitPathList() {
        return btcSegWitPathList;
    }

    public void setSupportSegWit(boolean btcSupportSegWit) {
        this.btcSupportSegWit = btcSupportSegWit;
    }

    public boolean isSupportSegWit() {
        return btcSupportSegWit;
    }

    public boolean isWatch() {
        return isWatch == 1;
    }

    public String getLedgerDeviceId() {
        return ledgerDeviceId;
    }

    public void setLedgerDeviceId(String ledgerDeviceId) {
        this.ledgerDeviceId = ledgerDeviceId;
    }

    public String getLedgerPath() {
        return ledgerPath;
    }

    public void setLedgerPath(String ledgerPath) {
        this.ledgerPath = ledgerPath;
    }

    public boolean isLedger() {
        return !TextUtils.isEmpty(ledgerDeviceId);
    }

    @Override
    public String toString() {
        return "QWWallet{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", hint='" + hint + '\'' +
                ", isBackup=" + isBackup +
                ", isWatch=" + isWatch +
                ", btcSupportSegWit=" + btcSupportSegWit +
                ", btcSegWit=" + btcSegWitState +
                ", currentAddress=" + currentAddress +
                '}';
    }

    public QWWallet copy() {
        QWWallet wallet = new QWWallet();
        wallet.setId(this.id);
        wallet.setKey(this.key);
        wallet.setType(this.type);
        wallet.setName(this.name);
        wallet.setIcon(this.icon);
        wallet.setHint(this.hint);
        wallet.setIsBackup(this.isBackup);
        wallet.setIsBackup(this.isWatch);
        wallet.setCurrentAddress(this.currentAddress);
        wallet.setSupportSegWit(this.btcSupportSegWit);
        wallet.setShowBTCSegWit(this.btcSegWitState);
        wallet.setBtcSegWitPathList(this.btcSegWitPathList);
        return wallet;
    }

    public String getCurrentShareAddress() {
        String address = getCurrentAddress();
        if (type == Constant.WALLET_TYPE_QKC || QWWalletUtils.isQKCValidAddress(address)) {
            address = QWAccount.getShardAddress(address);
        }
        return address;
    }

    public String getCurrentShowAddress() {
        return getCurrentShowAddress(getCurrentAddress());
    }

    public static String getCurrentShowAddress(String address) {
        if (QWWalletUtils.isQKCValidAddress(address)) {
            address = QWAccount.getShardAddress(address);
        }
        if (WalletUtils.isValidAddress(address)) {
            return Keys.toChecksumHDAddress(address);
        }
        return address;
    }
}
