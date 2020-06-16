package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.quarkonium.qpocket.api.db.wealth.table.QWWealth;
import com.quarkonium.qpocket.api.db.wealth.table.QWBanner2;

//游戏DApp
@DatabaseTable(tableName = "qwrecentdapp")
public class QWRecentDApp implements Parcelable {

    public static final Creator<QWRecentDApp> CREATOR = new Creator<QWRecentDApp>() {
        @Override
        public QWRecentDApp createFromParcel(Parcel in) {
            return new QWRecentDApp(in);
        }

        @Override
        public QWRecentDApp[] newArray(int size) {
            return new QWRecentDApp[size];
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
        parcel.writeString(descriptionCn);
        parcel.writeString(descriptionKo);

        parcel.writeInt(coinType);
    }

    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_URL = "url";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_NAME_ICON = "iconURL";
    private static final String COLUMN_NAME_DESCRIPTION = "description";
    private static final String COLUMN_NAME_DESCRIPTION_CN = "descriptionCn";
    private static final String COLUMN_NAME_DESCRIPTION_KO = "descriptionKo";
    public static final String COLUMN_NAME_COIN_TYPE = "coinType";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    protected int id;

    @DatabaseField(columnName = COLUMN_NAME_URL, canBeNull = false, unique = true)
    protected String url;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    protected String name;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    protected String iconUrl;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION)
    protected String description = "";

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_CN)
    protected String descriptionCn = "";

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_KO)
    protected String descriptionKo = "";

    @DatabaseField(columnName = COLUMN_NAME_COIN_TYPE, defaultValue = "99999999")
    protected int coinType;

    public QWRecentDApp() {
    }

    public QWRecentDApp(QWBannerApp app) {
        url = app.getUrl();
        name = app.getName();
        coinType = app.getCoinType();

        if ("zh-Hans".equals(app.getLocalization())) {
            descriptionCn = app.getDescription();
        } else if ("ko".equals(app.getLocalization())) {
            descriptionKo = app.getDescription();
        } else {
            description = app.getDescription();
        }
    }

    public QWRecentDApp(QWBanner2 app) {
        url = app.getUrl();
        name = app.getName();
        coinType = app.getCoinType();
    }

    public QWRecentDApp(QWDApp app) {
        url = app.getUrl();
        iconUrl = app.getIconUrl();
        name = app.getName();
        coinType = app.getCoinType();

        if ("zh-Hans".equals(app.getLocalization())) {
            descriptionCn = app.getDescription();
        } else if ("ko".equals(app.getLocalization())) {
            descriptionKo = app.getDescription();
        } else {
            description = app.getDescription();
        }
    }

    public QWRecentDApp(QWWealth app) {
        url = app.getUrl();
        iconUrl = app.getIcon();
        name = app.getName();
        coinType = app.getCoinType();
    }

    public QWRecentDApp(DAppFavorite favorite) {
        url = favorite.getUrl();
        name = favorite.getName();
        iconUrl = favorite.getIconUrl();
        coinType = favorite.getCoinType();

        descriptionCn = favorite.getDescriptionCn();
        descriptionKo = favorite.getDescriptionKo();
        description = favorite.getDescription();
    }

    public QWRecentDApp(Parcel in) {
        id = in.readInt();
        url = in.readString();
        name = in.readString();
        iconUrl = in.readString();
        description = in.readString();
        descriptionCn = in.readString();
        descriptionKo = in.readString();
        coinType = in.readInt();
    }

    public static QWRecentDApp copy(QWRecentDApp app) {
        QWRecentDApp newApp = new QWRecentDApp();
        newApp.setUrl(app.getUrl());
        newApp.setName(app.getName());
        newApp.setIconUrl(app.getIconUrl());
        newApp.setDescription(app.getDescription());
        newApp.setDescriptionCn(app.getDescriptionCn());
        newApp.setDescriptionKo(app.getDescriptionKo());
        newApp.setCoinType(app.getCoinType());
        return newApp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionCn() {
        return descriptionCn;
    }

    public void setDescriptionCn(String descriptionCn) {
        this.descriptionCn = descriptionCn;
    }

    public String getDescriptionKo() {
        return descriptionKo;
    }

    public void setDescriptionKo(String descriptionKo) {
        this.descriptionKo = descriptionKo;
    }

    public int getCoinType() {
        return coinType;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
    }

    @Override
    public int hashCode() {
        return getUrl() != null ? getUrl().hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QWRecentDApp)) return false;

        QWRecentDApp favorite = (QWRecentDApp) o;

        return getUrl() != null ? getUrl().equals(favorite.getUrl()) : favorite.getUrl() == null;
    }

    @Override
    public String toString() {
        return "QWRecentDApp{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", description='" + description + '\'' +
                ", descriptionCn='" + descriptionCn + '\'' +
                ", descriptionKo='" + descriptionKo + '\'' +
                ", coinType=" + coinType +
                '}';
    }
}
