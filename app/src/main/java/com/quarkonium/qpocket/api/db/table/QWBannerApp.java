package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;

public class QWBannerApp implements Parcelable {

    public static final Creator<QWBannerApp> CREATOR = new Creator<QWBannerApp>() {
        @Override
        public QWBannerApp createFromParcel(Parcel in) {
            return new QWBannerApp(in);
        }

        @Override
        public QWBannerApp[] newArray(int size) {
            return new QWBannerApp[size];
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
        parcel.writeString(backgroundURL);
        parcel.writeString(description);
        parcel.writeString(localization);

        parcel.writeInt(coinType);
        parcel.writeInt(chainId);
        parcel.writeInt(order);

        parcel.writeString(category);
    }

    private static final String COLUMN_NAME_ID = "id";
    private static final String COLUMN_NAME_URL = "url";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_NAME_BACKGROUND = "backgroundURL";
    private static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_LOCALIZATION = "localization";
    public static final String COLUMN_NAME_CATEGORY = "category";
    public static final String COLUMN_NAME_COIN_TYPE = "coinType";
    public static final String COLUMN_NAME_COIN_CHAIN_ID = "chainId";
    public static final String COLUMN_NAME_COIN_ORDER = "order";


    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_URL)
    private String url;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    private String name;

    @DatabaseField(columnName = COLUMN_NAME_BACKGROUND)
    private String backgroundURL;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION)
    private String description;

    @DatabaseField(columnName = COLUMN_NAME_LOCALIZATION)
    private String localization;

    @DatabaseField(columnName = COLUMN_NAME_CATEGORY)
    private String category;//类型 game，tools

    @DatabaseField(columnName = COLUMN_NAME_COIN_TYPE, defaultValue = "99999999")
    private int coinType;

    @DatabaseField(columnName = COLUMN_NAME_COIN_CHAIN_ID, defaultValue = "1")
    private int chainId;

    @DatabaseField(columnName = COLUMN_NAME_COIN_ORDER, defaultValue = "0")
    private int order;

    public QWBannerApp() {

    }

    public QWBannerApp(Parcel in) {
        id = in.readInt();
        url = in.readString();
        name = in.readString();
        backgroundURL = in.readString();
        description = in.readString();
        localization = in.readString();
        coinType = in.readInt();
        chainId = in.readInt();
        order = in.readInt();

        category = in.readString();
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

    public String getBackgroundURL() {
        return backgroundURL;
    }

    public void setBackgroundURL(String backgroundURL) {
        this.backgroundURL = backgroundURL;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocalization() {
        return localization;
    }

    public void setLocalization(String localization) {
        this.localization = localization;
    }

    public int getCoinType() {
        return coinType;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "QWDapp{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", backgroundURL='" + backgroundURL + '\'' +
                ", description='" + description + '\'' +
                ", localization='" + localization + '\'' +
                ", coinType=" + coinType +
                ", chainId=" + chainId +
                ", order=" + order +
                '}';
    }
}
