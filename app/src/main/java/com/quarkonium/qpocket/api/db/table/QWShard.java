package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * QWShard实体类，分片id相关类
 */
@DatabaseTable(tableName = "shard")
public class QWShard implements Parcelable {
    public static final Creator<QWShard> CREATOR = new Creator<QWShard>() {
        @Override
        public QWShard createFromParcel(Parcel in) {
            return new QWShard(in);
        }

        @Override
        public QWShard[] newArray(int size) {
            return new QWShard[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(shard);
    }

    // 表中各个字段的名称
    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_SHARD = "shard";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_SHARD, canBeNull = false, unique = true)
    private String shard;

    public QWShard() {
    }

    private QWShard(Parcel in) {
        id = in.readInt();
        shard = in.readString();
    }

    public QWShard(String shard) {
        this.shard = shard;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getShard() {
        return shard;
    }

    public void setShard(String id) {
        this.shard = id;
    }

    @Override
    public String toString() {
        return "QWShard{" +
                "id=" + id +
                ", shard='" + shard + '\'' +
                '}';
    }
}
