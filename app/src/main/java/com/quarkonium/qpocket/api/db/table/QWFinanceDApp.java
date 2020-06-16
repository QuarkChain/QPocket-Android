package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.table.DatabaseTable;

//游戏DApp
@DatabaseTable(tableName = "qwfinancedapp")
public class QWFinanceDApp extends QWDApp implements Parcelable {

    public static final Creator<QWFinanceDApp> CREATOR = new Creator<QWFinanceDApp>() {
        @Override
        public QWFinanceDApp createFromParcel(Parcel in) {
            return new QWFinanceDApp(in);
        }

        @Override
        public QWFinanceDApp[] newArray(int size) {
            return new QWFinanceDApp[size];
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

    public QWFinanceDApp() {
    }

    public QWFinanceDApp(QWBannerApp app) {
        url = app.getUrl();
        name = app.getName();
        iconUrl = app.getBackgroundURL();
        description = app.getDescription();
        localization = app.getLocalization();
        coinType = app.getCoinType();
        chainId = app.getChainId();
        order = app.getOrder();
    }

    public QWFinanceDApp(Parcel in) {
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
