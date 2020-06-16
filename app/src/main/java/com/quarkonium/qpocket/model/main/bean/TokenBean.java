package com.quarkonium.qpocket.model.main.bean;

import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;

public class TokenBean {

    private QWToken mToken;

    private QWBalance mBalance;

    public QWToken getToken() {
        return mToken;
    }

    public void setToken(QWToken mToken) {
        this.mToken = mToken;
    }

    public QWBalance getBalance() {
        return mBalance;
    }

    public void setBalance(QWBalance mBalance) {
        this.mBalance = mBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenBean)) return false;

        TokenBean bean = (TokenBean) o;

        return mToken != null ? mToken.equals(bean.mToken) : bean.mToken == null;
    }

    @Override
    public int hashCode() {
        return mToken != null ? mToken.hashCode() : 0;
    }
}
