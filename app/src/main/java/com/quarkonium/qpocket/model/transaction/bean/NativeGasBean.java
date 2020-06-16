package com.quarkonium.qpocket.model.transaction.bean;

import java.math.BigDecimal;

public class NativeGasBean {
    private String mAddress;
    private BigDecimal mRefundPercentage;

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public BigDecimal getRefundPercentage() {
        return mRefundPercentage;
    }

    public void setRefundPercentage(BigDecimal mRefundPercentage) {
        this.mRefundPercentage = mRefundPercentage;
    }
}
