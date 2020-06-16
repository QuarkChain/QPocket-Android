package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "utxo") // 指定数据表的名称
public class UTXO implements Parcelable {

    public static final Parcelable.Creator<UTXO> CREATOR = new Parcelable.Creator<UTXO>() {
        @Override
        public UTXO createFromParcel(Parcel in) {
            return new UTXO(in);
        }

        @Override
        public UTXO[] newArray(int size) {
            return new UTXO[size];
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
        parcel.writeString(txHash);
        parcel.writeInt(vout);
        parcel.writeLong(amount);
        parcel.writeString(address);
        parcel.writeString(scriptPubKey);
        parcel.writeString(derivedPath);
        parcel.writeLong(sequence);
        parcel.writeLong(blockId);

        parcel.writeString(rawData);
    }


    // 定义字段在数据库中的字段名
    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_BLOCK_ID = "blockId";
    public static final String COLUMN_NAME_KEY = "key";
    public static final String COLUMN_NAME_HASH_ID = "txHash";
    public static final String COLUMN_NAME_OUT_INDEX = "vout";
    public static final String COLUMN_NAME_AMOUNT = "amount";
    public static final String COLUMN_NAME_ADDRESS = "address";
    public static final String COLUMN_NAME_SCRIPT = "scriptPubKey";
    public static final String COLUMN_NAME_PATH_INDEX = "derivedPath";
    public static final String COLUMN_NAME_SEQUENCE = "sequence";
    public static final String COLUMN_NAME_TX_RAW_DATA = "rawData";


    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_KEY, canBeNull = false)
    private String key;

    @DatabaseField(columnName = COLUMN_NAME_HASH_ID, canBeNull = false)
    private String txHash;

    @DatabaseField(columnName = COLUMN_NAME_OUT_INDEX, canBeNull = false)
    private int vout;

    @DatabaseField(columnName = COLUMN_NAME_BLOCK_ID)
    private long blockId;

    @DatabaseField(columnName = COLUMN_NAME_AMOUNT)
    private long amount;

    @DatabaseField(columnName = COLUMN_NAME_ADDRESS)
    private String address;

    @DatabaseField(columnName = COLUMN_NAME_SCRIPT)
    private String scriptPubKey;

    @DatabaseField(columnName = COLUMN_NAME_PATH_INDEX)
    private String derivedPath;

    @DatabaseField(columnName = COLUMN_NAME_TX_RAW_DATA)
    private String rawData;

    @DatabaseField(columnName = COLUMN_NAME_SEQUENCE)
    private long sequence = 4294967295L;

    public UTXO() {

    }

    private UTXO(Parcel in) {
        id = in.readInt();
        key = in.readString();
        txHash = in.readString();
        vout = in.readInt();
        amount = in.readLong();
        address = in.readString();
        scriptPubKey = in.readString();
        derivedPath = in.readString();
        sequence = in.readLong();
        blockId = in.readLong();

        rawData = in.readString();
    }

    public UTXO(String key, String txHash, int vout, long amount, String address,
                String scriptPubKey, String derivedPath, long blockId, String rawData) {
        this.key = key;
        this.txHash = txHash;
        this.vout = vout;
        this.amount = amount;
        this.address = address;
        this.scriptPubKey = scriptPubKey;
        this.derivedPath = derivedPath;
        this.blockId = blockId;
        this.rawData = rawData;
    }

    public UTXO(String key, String txHash, int vout, long amount, String address,
                String scriptPubKey, String derivedPath, long blockId, long sequence, String rawData) {
        this.key = key;
        this.txHash = txHash;
        this.vout = vout;
        this.amount = amount;
        this.address = address;
        this.scriptPubKey = scriptPubKey;
        this.derivedPath = derivedPath;
        this.sequence = sequence;
        this.blockId = blockId;
        this.rawData = rawData;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setKeyAddress(String key) {
        this.key = key;
    }

    public String getKeyAddress() {
        return key;
    }

    public int getVout() {
        return vout;
    }

    public void setVout(int vout) {
        this.vout = vout;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getScriptPubKey() {
        return scriptPubKey;
    }

    public void setScriptPubKey(String scriptPubKey) {
        this.scriptPubKey = scriptPubKey;
    }

    public String getDerivedPath() {
        return derivedPath;
    }

    public void setDerivedPath(String derivedPath) {
        this.derivedPath = derivedPath;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getBlockId() {
        return blockId;
    }

    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    @Override
    public String toString() {
        return "UTXO{" +
                "txHash='" + txHash + '\'' +
                ", vout=" + vout +
                ", amount=" + amount +
                ", address='" + address + '\'' +
                ", scriptPubKey='" + scriptPubKey + '\'' +
                ", derivedPath='" + derivedPath + '\'' +
                ", sequence=" + sequence +
                '}';
    }
}
