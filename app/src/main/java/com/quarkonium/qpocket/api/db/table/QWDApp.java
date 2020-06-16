package com.quarkonium.qpocket.api.db.table;

import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;

import java.io.Serializable;

//DApp
public abstract class QWDApp implements Parcelable, Serializable {

    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_URL = "url";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_NAME_ICON = "iconURL";
    private static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_LOCALIZATION = "localization";
    public static final String COLUMN_NAME_COIN_TYPE = "coinType";
    public static final String COLUMN_NAME_COIN_CHAIN_ID = "chainId";
    public static final String COLUMN_NAME_COIN_ORDER = "order";
    public static final String COLUMN_NAME_REGION = "region";


    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    protected int id;

    @DatabaseField(columnName = COLUMN_NAME_URL)
    protected String url;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    protected String name;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    protected String iconUrl;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION)
    protected String description;

    @DatabaseField(columnName = COLUMN_NAME_LOCALIZATION)
    protected String localization;

    @DatabaseField(columnName = COLUMN_NAME_COIN_TYPE, defaultValue = "99999999")
    protected int coinType;

    @DatabaseField(columnName = COLUMN_NAME_COIN_CHAIN_ID, defaultValue = "1")
    protected int chainId;

    @DatabaseField(columnName = COLUMN_NAME_COIN_ORDER, defaultValue = "0")
    protected int order;

    @DatabaseField(columnName = COLUMN_NAME_REGION, defaultValue = "NONE")
    protected String region;

    private String objectId;

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

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return "QWDapp{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", description='" + description + '\'' +
                ", localization='" + localization + '\'' +
                ", coinType=" + coinType +
                ", chainId=" + chainId +
                ", order=" + order +
                ", objectId=" + objectId +
                ", region=" + region +
                '}';
    }
}
