package com.quarkonium.qpocket.model.main.bean;

import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;

public class WalletManagerBean {

    private int mGroupId;
    private QWWallet mWallet;
    private QWAccount mAccount;
    //是否带头部
    private boolean isShowAssets;

    public void setGroupId(int groupId) {
        this.mGroupId = groupId;
    }

    public int getGroupId() {
        return mGroupId;
    }

    public void setWallet(QWWallet mWallet) {
        this.mWallet = mWallet;
    }

    public QWWallet getWallet() {
        return mWallet;
    }

    public QWAccount getAccount() {
        return mAccount;
    }

    public void setAccount(QWAccount account) {
        this.mAccount = account;
    }

    public boolean isShowAssets() {
        return isShowAssets;
    }

    public void setShowAssets(boolean showAssets) {
        isShowAssets = showAssets;
    }
}
