package com.quarkonium.qpocket.api.db.wealth.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "qwBanner2")
public class QWBanner2 implements Parcelable {

    public static final Creator<QWBanner2> CREATOR = new Creator<QWBanner2>() {
        @Override
        public QWBanner2 createFromParcel(Parcel in) {
            return new QWBanner2(in);
        }

        @Override
        public QWBanner2[] newArray(int size) {
            return new QWBanner2[size];
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

        parcel.writeInt(coinType);
        parcel.writeInt(type);
        parcel.writeInt(order);

        parcel.writeString(localization);
        parcel.writeString(description);
        parcel.writeString(descriptionCn);
        parcel.writeString(descriptionKo);

        parcel.writeString(descriptionRu);
        parcel.writeString(descriptionIn);
        parcel.writeString(descriptionVi);
    }

    private static final String COLUMN_NAME_ID = "id";

    private static final String COLUMN_NAME_URL = "url";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_NAME_BACKGROUND = "backgroundURL";
    public static final String COLUMN_NAME_COIN_TYPE = "coinType";
    public static final String COLUMN_NAME_TYPE = "type";
    public static final String COLUMN_NAME_COIN_ORDER = "order";
    public static final String COLUMN_NAME_LOCALIZATION = "localization";

    public static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_DESCRIPTION_CN = "descriptionCn";
    public static final String COLUMN_NAME_DESCRIPTION_KO = "descriptionKo";
    public static final String COLUMN_NAME_DESCRIPTION_RU = "descriptionRu";
    public static final String COLUMN_NAME_DESCRIPTION_IN = "descriptionIn";
    public static final String COLUMN_NAME_DESCRIPTION_VI = "descriptionVi";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_URL)
    private String url;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    private String name;

    @DatabaseField(columnName = COLUMN_NAME_BACKGROUND)
    private String backgroundURL;

    @DatabaseField(columnName = COLUMN_NAME_COIN_TYPE)
    private int coinType;

    @DatabaseField(columnName = COLUMN_NAME_TYPE)
    private int type;

    @DatabaseField(columnName = COLUMN_NAME_COIN_ORDER, defaultValue = "0")
    private int order;

    @DatabaseField(columnName = COLUMN_NAME_LOCALIZATION)
    private String localization;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION)
    private String description;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_CN)
    private String descriptionCn;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_KO)
    private String descriptionKo;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_RU)
    private String descriptionRu;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_IN)
    private String descriptionIn;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_VI)
    private String descriptionVi;

    public QWBanner2() {
    }

    public QWBanner2(Parcel in) {
        id = in.readInt();

        url = in.readString();
        name = in.readString();
        backgroundURL = in.readString();

        coinType = in.readInt();
        type = in.readInt();
        order = in.readInt();

        localization = in.readString();
        description = in.readString();
        descriptionCn = in.readString();
        descriptionKo = in.readString();

        descriptionRu = in.readString();
        descriptionIn = in.readString();
        descriptionVi = in.readString();
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

    public int getCoinType() {
        return coinType;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setLocalization(String localization) {
        this.localization = localization;
    }

    public String getLocalization() {
        return localization;
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

    public String getDescriptionRu() {
        return descriptionRu;
    }

    public void setDescriptionRu(String descriptionSu) {
        this.descriptionRu = descriptionSu;
    }

    public String getDescriptionIn() {
        return descriptionIn;
    }

    public void setDescriptionIn(String descriptionIn) {
        this.descriptionIn = descriptionIn;
    }

    public String getDescriptionVi() {
        return descriptionVi;
    }

    public void setDescriptionVi(String descriptionVi) {
        this.descriptionVi = descriptionVi;
    }

    @Override
    public String toString() {
        return "QWWealthBanner{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", backgroundURL='" + backgroundURL + '\'' +
                ", coinType=" + coinType +
                ", type=" + type +
                ", order=" + order +
                '}';
    }
}
