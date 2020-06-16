package com.quarkonium.qpocket.model.transaction.bean;

import com.quarkonium.qpocket.api.db.table.QWBalance;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

public class MergeBean {
    public QWBalance balance;

    public BigInteger amount;
    public BigInteger gasPrice;
    public BigInteger gasLimit;

    public String gasTokenId;
    public ArrayList<QWBalance> gasTokenList;

    public BigInteger normalGasPrice;
    public BigDecimal refundPercentage;
    public BigDecimal reserveTokenBalance;

    public boolean isSelected;
}
