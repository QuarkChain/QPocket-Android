package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.table.DatabaseTable;

//交易所DApp
@DatabaseTable(tableName = "qwexchange")
public class QWExChangeDApp extends QWDApp implements Parcelable {

    public static final Creator<QWExChangeDApp> CREATOR = new Creator<QWExChangeDApp>() {
        @Override
        public QWExChangeDApp createFromParcel(Parcel in) {
            return new QWExChangeDApp(in);
        }

        @Override
        public QWExChangeDApp[] newArray(int size) {
            return new QWExChangeDApp[size];
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

    public QWExChangeDApp() {

    }

    public QWExChangeDApp(QWBannerApp app) {
        url = app.getUrl();
        name = app.getName();
        iconUrl = app.getBackgroundURL();
        description = app.getDescription();
        localization = app.getLocalization();
        coinType = app.getCoinType();
        chainId = app.getChainId();
        order = app.getOrder();
    }

    public QWExChangeDApp(Parcel in) {
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
