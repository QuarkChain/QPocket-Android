package com.quarkonium.qpocket.crypto;

public class KeystoreTypeException extends Exception {

    public static final int KEYSTORE_ERROR_BTC_NORMAL = 0;
    public static final int KEYSTORE_ERROR_BTC_SEGWIT = 1;

    private int mErrorSrc;

    public KeystoreTypeException(int src) {
        mErrorSrc = src;
    }

    public int getErrorSrc() {
        return mErrorSrc;
    }
}
