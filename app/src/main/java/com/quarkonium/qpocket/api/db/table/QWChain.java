package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * QWShard实体类，分片id相关类
 */
@DatabaseTable(tableName = "chain")
public class QWChain implements Parcelable {
    public static final Creator<QWChain> CREATOR = new Creator<QWChain>() {
        @Override
        public QWChain createFromParcel(Parcel in) {
            return new QWChain(in);
        }

        @Override
        public QWChain[] newArray(int size) {
            return new QWChain[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(chain);
    }

    // 表中各个字段的名称
    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_CHAIN = "chain";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_CHAIN, canBeNull = false, unique = true)
    private String chain;

    public QWChain() {
    }

    private QWChain(Parcel in) {
        id = in.readInt();
        chain = in.readString();
    }

    public QWChain(String chain) {
        this.chain = chain;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String id) {
        this.chain = id;
    }

    @Override
    public String toString() {
        return "QWShard{" +
                "id=" + id +
                ", chain='" + chain + '\'' +
                '}';
    }
}
