package com.quarkonium.qpocket.crypto;

public class CreateWalletException extends Exception {

    private int mErrorSrc;
    private int mAccountIndex;

    public CreateWalletException(int src) {
        mErrorSrc = src;
    }

    public int getErrorSrc() {
        return mErrorSrc;
    }

    public void setAccountIndex(int mAccountIndex) {
        this.mAccountIndex = mAccountIndex;
    }

    public int getAccountIndex() {
        return mAccountIndex;
    }
}
