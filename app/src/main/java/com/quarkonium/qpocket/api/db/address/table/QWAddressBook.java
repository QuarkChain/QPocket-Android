package com.quarkonium.qpocket.api.db.address.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "qwAddressBook")
public class QWAddressBook implements Parcelable {

    public static final Creator<QWAddressBook> CREATOR = new Creator<QWAddressBook>() {
        @Override
        public QWAddressBook createFromParcel(Parcel in) {
            return new QWAddressBook(in);
        }

        @Override
        public QWAddressBook[] newArray(int size) {
            return new QWAddressBook[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);

        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(icon);
        dest.writeInt(coinType);
    }

    private static final String COLUMN_NAME_ID = "id";

    public static final String COLUMN_NAME_ADDRESS = "address";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_ICON = "icon";
    public static final String COLUMN_NAME_COIN_TYPE = "coinType";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    protected int id;

    @DatabaseField(columnName = COLUMN_NAME_ADDRESS)
    protected String address;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    protected String name;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    protected String icon;

    @DatabaseField(columnName = COLUMN_NAME_COIN_TYPE)
    protected int coinType;


    public QWAddressBook() {

    }

    public QWAddressBook(Parcel in) {
        id = in.readInt();

        address = in.readString();
        name = in.readString();
        icon = in.readString();
        coinType = in.readInt();
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getCoinType() {
        return coinType;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QWAddressBook)) return false;

        QWAddressBook that = (QWAddressBook) o;

        if (getCoinType() != that.getCoinType()) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null)
            return false;
        return getIcon() != null ? getIcon().equals(that.getIcon()) : that.getIcon() == null;
    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getIcon() != null ? getIcon().hashCode() : 0);
        result = 31 * result + getCoinType();
        return result;
    }
}
