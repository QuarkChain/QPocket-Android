package com.quarkonium.qpocket.model.market.bean;

import java.io.Serializable;

public class Price implements Serializable {

    private String coinID;
    private String priceType;
    private float price;

    public String getCoinID() {
        return coinID;
    }

    public void setCoinID(String coinID) {
        this.coinID = coinID;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}
