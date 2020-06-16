package com.quarkonium.qpocket.model.transaction.bean;

import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;

import java.util.List;

public class PublicTokenLoadBean {
    private List<QWPublicTokenTransaction> mList;
    private boolean mHasLoadMore;

    public List<QWPublicTokenTransaction> getList() {
        return mList;
    }

    public void setList(List<QWPublicTokenTransaction> list) {
        this.mList = list;
    }

    public void addAll(List<QWPublicTokenTransaction> list) {
        mList.addAll(list);
    }

    public boolean isHasLoadMore() {
        return mHasLoadMore;
    }

    public void setHasLoadMore(boolean hasLoadMore) {
        this.mHasLoadMore = hasLoadMore;
    }
}
