package com.quarkonium.qpocket.model.main.bean;

import com.quarkonium.qpocket.api.db.table.QWTransaction;

import java.util.List;

public class TransactionLoadBean {
    private List<QWTransaction> mList;

    public List<QWTransaction> getList() {
        return mList;
    }

    public void setList(List<QWTransaction> list) {
        this.mList = list;
    }

    public void addAll(List<QWTransaction> list) {
        mList.addAll(list);
    }
}
