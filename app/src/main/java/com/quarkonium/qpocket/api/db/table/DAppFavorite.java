package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "dappfavorite") // 指定数据表的名称
public class DAppFavorite implements Parcelable {
    public static final Creator<DAppFavorite> CREATOR = new Creator<DAppFavorite>() {
        @Override
        public DAppFavorite createFromParcel(Parcel in) {
            return new DAppFavorite(in);
        }

        @Override
        public DAppFavorite[] newArray(int size) {
            return new DAppFavorite[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(coinType);


        parcel.writeString(url);
        parcel.writeString(name);
        parcel.writeString(iconUrl);
        parcel.writeString(description);
        parcel.writeString(descriptionCn);
        parcel.writeString(descriptionKo);

        parcel.writeLong(modifyTime);
    }

    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_COIN_TYPE = "coinType";

    public static final String COLUMN_NAME_URL = "url";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_NAME_ICON = "iconURL";
    private static final String COLUMN_NAME_DESCRIPTION = "description";
    private static final String COLUMN_NAME_DESCRIPTION_CN = "descriptionCn";
    private static final String COLUMN_NAME_DESCRIPTION_KO = "descriptionKo";

    public static final String COLUMN_NAME_MODIFY_TIME = "modifyTime";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    protected int id;

    @DatabaseField(columnName = COLUMN_NAME_COIN_TYPE, defaultValue = "99999999")
    protected int coinType;

    @DatabaseField(columnName = COLUMN_NAME_URL, canBeNull = false, unique = true)
    protected String url;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    protected String name;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    protected String iconUrl;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION)
    protected String description;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_CN)
    protected String descriptionCn;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION_KO)
    protected String descriptionKo;

    @DatabaseField(columnName = COLUMN_NAME_MODIFY_TIME)
    protected long modifyTime;

    public DAppFavorite() {

    }

    public DAppFavorite(QWDApp app) {
        url = app.getUrl();
        coinType = app.getCoinType();
        name = app.getName();
        iconUrl = app.getIconUrl();

        if ("zh-Hans".equals(app.getLocalization())) {
            descriptionCn = app.getDescription();
        } else if ("ko".equals(app.getLocalization())) {
            descriptionKo = app.getDescription();
        } else {
            description = app.getDescription();
        }

        modifyTime = System.currentTimeMillis();
    }

    public DAppFavorite(QWRecentDApp app) {
        url = app.getUrl();
        coinType = app.getCoinType();
        name = app.getName();
        iconUrl = app.getIconUrl();
        descriptionCn = app.getDescriptionCn();
        descriptionKo = app.getDescriptionKo();
        description = app.getDescription();

        modifyTime = System.currentTimeMillis();
    }

    public DAppFavorite(Parcel in) {
        id = in.readInt();
        coinType = in.readInt();

        url = in.readString();
        name = in.readString();
        iconUrl = in.readString();
        description = in.readString();
        descriptionCn = in.readString();
        descriptionKo = in.readString();

        modifyTime = in.readLong();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCoinType() {
        return coinType;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
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

    public long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(long modifyTime) {
        this.modifyTime = modifyTime;
    }

    @Override
    public String toString() {
        return "DAppFavorite{" +
                "id=" + id +
                ", coinType=" + coinType +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", description='" + description + '\'' +
                ", descriptionCn='" + descriptionCn + '\'' +
                ", descriptionKo='" + descriptionKo + '\'' +
                ", modifyTime=" + modifyTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DAppFavorite)) return false;

        DAppFavorite favorite = (DAppFavorite) o;

        return getUrl() != null ? getUrl().equals(favorite.getUrl()) : favorite.getUrl() == null;
    }

    @Override
    public int hashCode() {
        return getUrl() != null ? getUrl().hashCode() : 0;
    }

    public static DAppFavorite fromString(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, DAppFavorite.class);
    }
}
