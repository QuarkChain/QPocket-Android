package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;

//钱包对应的token
public class QWWalletToken implements Parcelable {
    public static final Creator<QWWalletToken> CREATOR = new Creator<QWWalletToken>() {
        @Override
        public QWWalletToken createFromParcel(Parcel in) {
            return new QWWalletToken(in);
        }

        @Override
        public QWWalletToken[] newArray(int size) {
            return new QWWalletToken[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(accountAddress);
        parcel.writeString(tokenAddress);
        parcel.writeInt(isOpen);
    }

    // 表中各个字段的名称
    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_ACCOUNT_ADDRESS = "accountAddress";
    public static final String COLUMN_NAME_TOKEN_ADDRESS = "tokenAddress";
    public static final String COLUMN_NAME_IS_OPEN = "isOpen";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_ACCOUNT_ADDRESS)
    private String accountAddress;

    @DatabaseField(columnName = COLUMN_NAME_TOKEN_ADDRESS)
    private String tokenAddress;

    @DatabaseField(columnName = COLUMN_NAME_IS_OPEN)
    private int isOpen;

    public QWWalletToken() {
    }

    private QWWalletToken(Parcel in) {
        id = in.readInt();
        accountAddress = in.readString();
        tokenAddress = in.readString();
        isOpen = in.readInt();
    }

    public QWWalletToken(String walletAddress, String tokenAddress, int isOpen) {
        this.accountAddress = walletAddress;
        this.tokenAddress = tokenAddress;
        this.isOpen = isOpen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccountAddress() {
        return accountAddress;
    }

    public void setAccountAddress(String walletAddress) {
        this.accountAddress = walletAddress;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public int getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(int isOpen) {
        this.isOpen = isOpen;
    }

    @Override
    public String toString() {
        return "QWWalletToken{" +
                "id=" + id +
                ", walletAddress='" + accountAddress + '\'' +
                ", tokenAddress='" + tokenAddress + '\'' +
                '}';
    }
}
