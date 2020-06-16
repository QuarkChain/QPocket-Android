package com.quarkonium.qpocket.model.market.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Coin implements Parcelable, Serializable {
    public static final Creator<Coin> CREATOR = new Creator<Coin>() {
        @Override
        public Coin createFromParcel(Parcel in) {
            return new Coin(in);
        }

        @Override
        public Coin[] newArray(int size) {
            return new Coin[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(symbol);
        dest.writeString(name);
        dest.writeString(market_cap_rank);
    }

    private String id;
    private String symbol;
    private String name;
    private String market_cap_rank;

    private Price price;

    public Coin() {

    }

    public Coin(Parcel in) {
        id = in.readString();
        symbol = in.readString();
        name = in.readString();
        market_cap_rank = in.readString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMarket_cap_rank() {
        return market_cap_rank;
    }

    public void setMarket_cap_rank(String market_cap_rank) {
        this.market_cap_rank = market_cap_rank;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public Price getPrice() {
        return price;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coin)) return false;

        Coin coin = (Coin) o;

        return getSymbol() != null ? getSymbol().equals(coin.getSymbol()) : coin.getSymbol() == null;
    }

    @Override
    public int hashCode() {
        return getSymbol() != null ? getSymbol().hashCode() : 0;
    }
}
