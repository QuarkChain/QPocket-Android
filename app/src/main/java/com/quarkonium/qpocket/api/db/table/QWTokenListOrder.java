package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;

//钱包对应的token
public class QWTokenListOrder implements Parcelable {
    public static final Creator<QWTokenListOrder> CREATOR = new Creator<QWTokenListOrder>() {
        @Override
        public QWTokenListOrder createFromParcel(Parcel in) {
            return new QWTokenListOrder(in);
        }

        @Override
        public QWTokenListOrder[] newArray(int size) {
            return new QWTokenListOrder[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(type);
        parcel.writeInt(chainId);
        parcel.writeString(tokenList);
    }

    // 表中各个字段的名称
    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_TYPE = "type";
    public static final String COLUMN_NAME_CHAIN_ID = "chainId";
    public static final String COLUMN_NAME_TOKEN_LIST = "tokenList";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_TYPE, defaultValue = "0")
    private int type;

    @DatabaseField(columnName = COLUMN_NAME_CHAIN_ID)
    private int chainId;

    @DatabaseField(columnName = COLUMN_NAME_TOKEN_LIST)
    private String tokenList;//json数组

    public QWTokenListOrder() {
    }

    private QWTokenListOrder(Parcel in) {
        id = in.readInt();
        type = in.readInt();
        chainId = in.readInt();
        tokenList = in.readString();
    }

    public QWTokenListOrder(int type, int chainId, String tokenList) {
        this.type = type;
        this.chainId = chainId;
        this.tokenList = tokenList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getTokenList() {
        return tokenList;
    }

    public void setTokenList(String tokenList) {
        this.tokenList = tokenList;
    }

    @Override
    public String toString() {
        return "QWTokenListOrder{" +
                "id=" + id +
                ", type=" + type +
                ", chainId=" + chainId +
                ", tokenList='" + tokenList + '\'' +
                '}';
    }
}
