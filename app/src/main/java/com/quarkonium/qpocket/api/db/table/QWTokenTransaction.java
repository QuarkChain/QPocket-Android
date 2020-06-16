package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.crypto.utils.Numeric;

/**
 * erc trc token交易记录相关表
 * foreignColumnName：外键约束指向的类中的属性名
 * foreign：当前字段是否是外键
 * foreignAutoRefresh：如果这个属性设置为true，在关联查询的时候就不需要再调用refresh()方法了
 */
@DatabaseTable(tableName = "tokenTransaction")
public class QWTokenTransaction implements Parcelable {

    public static final Creator<QWTokenTransaction> CREATOR = new Creator<QWTokenTransaction>() {
        @Override
        public QWTokenTransaction createFromParcel(Parcel in) {
            return new QWTokenTransaction(in);
        }

        @Override
        public QWTokenTransaction[] newArray(int size) {
            return new QWTokenTransaction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(txId);
        parcel.writeString(amount);
        parcel.writeString(from);
        parcel.writeString(to);
        parcel.writeString(block);
        parcel.writeString(timestamp);
        parcel.writeString(status);
        parcel.writeString(cost);
        parcel.writeString(direction);

        parcel.writeString(transferTokenId);
        parcel.writeString(transferTokenStr);
        parcel.writeString(gasTokenId);
        parcel.writeString(gasTokenStr);

        parcel.writeParcelable(token, i);
        parcel.writeParcelable(shard, i);
        parcel.writeParcelable(chain, i);
    }

    // 表中各个字段的名称
    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_TXID = "txId";
    private static final String COLUMN_NAME_AMOUNT = "amount";
    private static final String COLUMN_NAME_FROM = "from";
    private static final String COLUMN_NAME_TO = "to";
    private static final String COLUMN_NAME_BLOCK = "block";
    private static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    private static final String COLUMN_NAME_STATUS = "status";//状态 成功或者失败

    private static final String COLUMN_NAME_COST = "cost";

    private static final String COLUMN_NAME_DIRECTION = "direction";//是发送还是接受

    private static final String COLUMN_NAME_TRANSFER_TOKEN_ID = "transferTokenId";//转账token
    private static final String COLUMN_NAME_TRANSFER_TOKEN_STR = "transferTokenStr";
    private static final String COLUMN_NAME_GAS_TOKEN_ID = "gasTokenId";//手续费
    private static final String COLUMN_NAME_GAS_TOKEN_STR = "gasTokenStr";

    //关联查询
    public static final String COLUMN_NAME_ACCOUNT = "account_id";
    public static final String COLUMN_NAME_TOKEN = "token_id";
    public static final String COLUMN_NAME_SHARD = "shard_id";
    public static final String COLUMN_NAME_CHAIN = "chain_id";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_TXID)
    private String txId;

    @DatabaseField(columnName = COLUMN_NAME_AMOUNT)
    private String amount;

    @DatabaseField(columnName = COLUMN_NAME_FROM)
    private String from;

    @DatabaseField(columnName = COLUMN_NAME_TO)
    private String to;

    @DatabaseField(columnName = COLUMN_NAME_BLOCK)
    private String block;

    @DatabaseField(columnName = COLUMN_NAME_TIMESTAMP)
    private String timestamp;

    @DatabaseField(columnName = COLUMN_NAME_STATUS)
    private String status;

    @DatabaseField(columnName = COLUMN_NAME_COST)
    private String cost;

    @DatabaseField(columnName = COLUMN_NAME_DIRECTION)
    private String direction;

    @DatabaseField(columnName = COLUMN_NAME_TRANSFER_TOKEN_ID)
    private String transferTokenId;
    @DatabaseField(columnName = COLUMN_NAME_TRANSFER_TOKEN_STR)
    private String transferTokenStr;
    @DatabaseField(columnName = COLUMN_NAME_GAS_TOKEN_ID)
    private String gasTokenId;
    @DatabaseField(columnName = COLUMN_NAME_GAS_TOKEN_STR)
    private String gasTokenStr;

    @DatabaseField(columnName = COLUMN_NAME_ACCOUNT, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWAccount account;

    @DatabaseField(columnName = COLUMN_NAME_TOKEN, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWToken token;

    @DatabaseField(columnName = COLUMN_NAME_SHARD, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWShard shard;

    @DatabaseField(columnName = COLUMN_NAME_CHAIN, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWChain chain;

    public QWTokenTransaction() {
    }

    private QWTokenTransaction(Parcel in) {
        id = in.readInt();

        txId = in.readString();
        amount = in.readString();
        from = in.readString();
        to = in.readString();
        block = in.readString();
        timestamp = in.readString();
        status = in.readString();
        cost = in.readString();
        direction = in.readString();

        transferTokenId = in.readString();
        transferTokenStr = in.readString();
        gasTokenId = in.readString();
        gasTokenStr = in.readString();

        token = in.readParcelable(QWToken.class.getClassLoader());
        shard = in.readParcelable(QWShard.class.getClassLoader());
        chain = in.readParcelable(QWChain.class.getClassLoader());
    }

    public QWTokenTransaction(String txId, String amount, String from, String to, String block, String timestamp, String status, String cost, String direction) {
        this.txId = txId;
        this.amount = amount;
        this.from = from;
        this.to = to;
        this.block = block;
        this.timestamp = timestamp;
        this.status = status;
        this.cost = cost;
        this.direction = direction;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public QWAccount getAccount() {
        return account;
    }

    public void setAccount(QWAccount wallet) {
        this.account = wallet;
    }

    public QWToken getToken() {
        return token;
    }

    public void setToken(QWToken token) {
        this.token = token;
    }

    public QWShard getShard() {
        return shard;
    }

    public void setShard(QWShard shard) {
        this.shard = shard;
    }

    public QWChain getChain() {
        return chain;
    }

    public void setChain(QWChain chain) {
        this.chain = chain;
    }


    public String getTransferTokenId() {
        return transferTokenId;
    }

    public void setTransferTokenId(String transferTokenId) {
        this.transferTokenId = transferTokenId;
    }

    public String getTransferTokenStr() {
        return transferTokenStr;
    }

    public void setTransferTokenStr(String transferTokenStr) {
        this.transferTokenStr = transferTokenStr;
    }

    public String getGasTokenId() {
        return gasTokenId;
    }

    public void setGasTokenId(String gasTokenId) {
        this.gasTokenId = gasTokenId;
    }

    public String getGasTokenStr() {
        return gasTokenStr;
    }

    public void setGasTokenStr(String gasTokenStr) {
        this.gasTokenStr = gasTokenStr;
    }

    @Override
    public String toString() {
        return "QWTransaction{" +
                "id=" + id +
                ", txId=" + txId +
                ", amount='" + amount + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", block='" + block + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", status='" + status + '\'' +
                ", cost='" + cost + '\'' +
                ", direction='" + direction + '\'' +
                ", account=" + account +
                ", token=" + token +
                ", shard=" + shard +
                '}';
    }

    public String getDirectionByAddress(String nowAddress) {
        String from = Numeric.cleanHexPrefix(getFrom().toLowerCase());
        nowAddress = Numeric.cleanHexPrefix(nowAddress.toLowerCase());
        return from.equals(nowAddress) ? Constant.QKC_TRANSACTION_STATE_SEND : Constant.QKC_TRANSACTION_STATE_RECEIVE;
    }

    public QWTransaction parseTransactionList() {
        QWTransaction transaction = new QWTransaction();
        transaction.setAccount(getAccount());
        transaction.setToken(getToken());
        transaction.setChain(getChain());
        transaction.setShard(getShard());

        transaction.setId(id);
        transaction.setTxId(txId);
        transaction.setAmount(amount);
        transaction.setFrom(from);
        transaction.setTo(to);
        transaction.setBlock(block);
        transaction.setTimestamp(timestamp);
        transaction.setStatus(status);
        transaction.setCost(cost);
        transaction.setDirection(direction);

        transaction.setTransferTokenStr(transferTokenStr);
        transaction.setTransferTokenId(transferTokenId);
        transaction.setGasTokenStr(gasTokenStr);
        transaction.setGasTokenId(gasTokenId);
        return transaction;
    }
}
