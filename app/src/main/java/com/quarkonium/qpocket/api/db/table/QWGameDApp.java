package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.table.DatabaseTable;

//游戏DApp
@DatabaseTable(tableName = "qwgame")
public class QWGameDApp extends QWDApp implements Parcelable {

    public static final Creator<QWGameDApp> CREATOR = new Creator<QWGameDApp>() {
        @Override
        public QWGameDApp createFromParcel(Parcel in) {
            return new QWGameDApp(in);
        }

        @Override
        public QWGameDApp[] newArray(int size) {
            return new QWGameDApp[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(url);

        parcel.writeString(name);
        parcel.writeString(iconUrl);
        parcel.writeString(description);
        parcel.writeString(localization);

        parcel.writeInt(coinType);
        parcel.writeInt(chainId);
        parcel.writeInt(order);
        parcel.writeString(region);
    }

    public QWGameDApp() {
    }

    public QWGameDApp(Parcel in) {
        id = in.readInt();
        url = in.readString();
        name = in.readString();
        iconUrl = in.readString();
        description = in.readString();
        localization = in.readString();
        coinType = in.readInt();
        chainId = in.readInt();
        order = in.readInt();
        region = in.readString();
    }
}
