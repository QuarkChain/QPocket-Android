package com.quarkonium.qpocket.api.db.table;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "publicscale")
public class QWPublicScale implements Parcelable {

    public static final Creator<QWPublicScale> CREATOR = new Creator<QWPublicScale>() {
        @Override
        public QWPublicScale createFromParcel(Parcel in) {
            return new QWPublicScale(in);
        }

        @Override
        public QWPublicScale[] newArray(int size) {
            return new QWPublicScale[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(key);
        parcel.writeString(availability);
        parcel.writeString(backgroundImageURL);
        parcel.writeString(buyRate);
        parcel.writeString(startTime);
        parcel.writeString(endTime);
        parcel.writeInt(chainId);
        parcel.writeInt(type);
        parcel.writeInt(order);

        parcel.writeParcelable(token, i);
    }

    private static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_KEY_ADDRESS = "key";
    private static final String COLUMN_NAME_AVAILABILITY = "availability";
    private static final String COLUMN_NAME_ICON = "backgroundImageURL";
    private static final String COLUMN_NAME_RATE = "buyRate";
    private static final String COLUMN_NAME_START_TIME = "startTime";
    private static final String COLUMN_NAME_END_TIME = "endTime";
    public static final String COLUMN_NAME_CHAIN_ID = "chainId";//属于币种的那个网络节点
    public static final String COLUMN_NAME_TYPE = "type";
    public static final String COLUMN_NAME_ORDER = "order";//顺序


    private static final String COLUMN_NAME_TOKEN = "token_id";

    @DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
    private int id;

    @DatabaseField(columnName = COLUMN_NAME_KEY_ADDRESS)
    private String key;

    @DatabaseField(columnName = COLUMN_NAME_AVAILABILITY)
    private String availability;

    @DatabaseField(columnName = COLUMN_NAME_ICON)
    private String backgroundImageURL;

    @DatabaseField(columnName = COLUMN_NAME_RATE)
    private String buyRate;

    @DatabaseField(columnName = COLUMN_NAME_START_TIME)
    private String startTime;

    @DatabaseField(columnName = COLUMN_NAME_END_TIME)
    private String endTime;

    @DatabaseField(columnName = COLUMN_NAME_CHAIN_ID, defaultValue = "1")
    private int chainId;

    @DatabaseField(columnName = COLUMN_NAME_TYPE, defaultValue = "0")
    private int type;

    @DatabaseField(columnName = COLUMN_NAME_ORDER)
    private int order;

    @DatabaseField(columnName = COLUMN_NAME_TOKEN, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private QWToken token;


    public QWPublicScale() {

    }

    public QWPublicScale(Parcel in) {
        id = in.readInt();
        key = in.readString();
        availability = in.readString();
        backgroundImageURL = in.readString();
        buyRate = in.readString();
        startTime = in.readString();
        endTime = in.readString();
        chainId = in.readInt();
        type = in.readInt();
        order = in.readInt();

        token = in.readParcelable(QWToken.class.getClassLoader());
    }

    public QWPublicScale(String key, String availability, String backgroundImageURL,
                         String buyRate, String startTime, String endTime, int chainId, int type, int order) {
        this.key = key;
        this.availability = availability;
        this.backgroundImageURL = backgroundImageURL;
        this.buyRate = buyRate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.chainId = chainId;
        this.type = type;
        this.order = order;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getBackgroundImageURL() {
        return backgroundImageURL;
    }

    public void setBackgroundImageURL(String backgroundImageURL) {
        this.backgroundImageURL = backgroundImageURL;
    }

    public String getBuyRate() {
        return buyRate;
    }

    public void setBuyRate(String buyRate) {
        this.buyRate = buyRate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public QWToken getToken() {
        return token;
    }

    public void setToken(QWToken token) {
        this.token = token;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "QWPublicScale{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", availability='" + availability + '\'' +
                ", backgroundImageURL='" + backgroundImageURL + '\'' +
                ", buyRate='" + buyRate + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", chainId='" + chainId + '\'' +
                ", type='" + type + '\'' +
                ", order='" + order + '\'' +
                ", token=" + token +
                '}';
    }
}
