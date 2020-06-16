package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.table.DatabaseTable;

//游戏DApp
@DatabaseTable(tableName = "qwutilitiesdapp")
public class QWUtilitiesDApp extends QWDApp implements Parcelable {

    public static final Creator<QWUtilitiesDApp> CREATOR = new Creator<QWUtilitiesDApp>() {
        @Override
        public QWUtilitiesDApp createFromParcel(Parcel in) {
            return new QWUtilitiesDApp(in);
        }

        @Override
        public QWUtilitiesDApp[] newArray(int size) {
            return new QWUtilitiesDApp[size];
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

    public QWUtilitiesDApp() {
    }

    public QWUtilitiesDApp(QWBannerApp app) {
        url = app.getUrl();
        name = app.getName();
        iconUrl = app.getBackgroundURL();
        description = app.getDescription();
        localization = app.getLocalization();
        coinType = app.getCoinType();
        chainId = app.getChainId();
        order = app.getOrder();
    }

    public QWUtilitiesDApp(Parcel in) {
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
