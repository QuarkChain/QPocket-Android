package com.quarkonium.qpocket.api.db.wealth.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "qwWealth")
public class QWWealth implements Parcelable {

    public static final Creator<QWWealth> CREATOR = new Creator<QWWealth>() {
        @Override
        public QWWealth createFromParcel(Parcel in) {
            return new QWWealth(in);
        }

        @Override
        public QWWealth[] newArray(int size) {
            return new QWWealth[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);

        dest.writeString(objectId);
        dest.writeString(category);
        dest.writeString(name);
        dest.writeString(icon);
        dest.writeString(label);
        dest.writeString(url);
        dest.writeInt(order);
        dest.writeInt(coinType);
        dest.writeString(yearly);

        dest.writeString(description);
        dest.writeFloat(rates);
    }

    private static final String COLUMN_NAME_ID = "id";

    public static final String COLUMN_NAME_OBJECT_ID = "objectId";
    public static final String COLUMN_NAME_CATEGORY = "category";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_ICON = "icon";
    public static final String COLUMN_NAME_LABEL = "label";
    public static final String COLUMN_NAME_URL = "url";
    public static final String COLUMN_NAME_ORDER = "order";
    public static final String COLUMN_NAME_COIN_TYPE = "coinType";
    public static final String COLUMN_NAME_COIN_YEARLY = "yearly";

    public static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_RATES = "rates";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    protected int id;

    @DatabaseField(columnName = COLUMN_NAME_OBJECT_ID)
    protected String objectId;

    @DatabaseField(columnName = COLUMN_NAME_CATEGORY)
    protected String category;

    @DatabaseField(columnName = COLUMN_NAME_NAME)
    protected String name;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    protected String icon;

    @DatabaseField(columnName = COLUMN_NAME_LABEL)
    protected String label;

    @DatabaseField(columnName = COLUMN_NAME_URL)
    protected String url;

    @DatabaseField(columnName = COLUMN_NAME_ORDER)
    protected int order;

    @DatabaseField(columnName = COLUMN_NAME_COIN_TYPE)
    protected int coinType;

    @DatabaseField(columnName = COLUMN_NAME_COIN_YEARLY)
    protected String yearly;

    @DatabaseField(columnName = COLUMN_NAME_DESCRIPTION)
    protected String description;

    @DatabaseField(columnName = COLUMN_NAME_RATES)
    protected float rates;

    public QWWealth() {

    }

    public QWWealth(Parcel in) {
        id = in.readInt();

        objectId = in.readString();
        category = in.readString();
        name = in.readString();
        icon = in.readString();
        label = in.readString();
        url = in.readString();
        order = in.readInt();
        coinType = in.readInt();
        yearly = in.readString();

        description = in.readString();
        rates = in.readFloat();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getCoinType() {
        return coinType;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public void setRates(float rates) {
        this.rates = rates;
    }

    public float getRates() {
        return rates;
    }

    public void setYearly(String yearly) {
        this.yearly = yearly;
    }

    public String getYearly() {
        return yearly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QWWealth)) return false;

        QWWealth wealth = (QWWealth) o;

        if (getCoinType() != wealth.getCoinType()) return false;
        if (getCategory() != null ? !getCategory().equals(wealth.getCategory()) : wealth.getCategory() != null)
            return false;
        if (getName() != null ? !getName().equals(wealth.getName()) : wealth.getName() != null)
            return false;
        if (getLabel() != null ? !getLabel().equals(wealth.getLabel()) : wealth.getLabel() != null)
            return false;
        if (getUrl() != null ? !getUrl().equals(wealth.getUrl()) : wealth.getUrl() != null)
            return false;
        return getYearly() != null ? !getYearly().equals(wealth.getYearly()) : wealth.getYearly() != null;
    }

    @Override
    public int hashCode() {
        int result = getCategory() != null ? getCategory().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getLabel() != null ? getLabel().hashCode() : 0);
        result = 31 * result + (getUrl() != null ? getUrl().hashCode() : 0);
        result = 31 * result + getCoinType();
        result = 31 * result + (getYearly() != null ? getYearly().hashCode() : 0);
        return result;
    }
}
