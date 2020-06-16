package com.quarkonium.qpocket.rx;

public class ChangeWalletEvent {
    private String mMessage;

    public ChangeWalletEvent(String message) {
        this.mMessage = message;
    }

    public void setMessage(String mMessage) {
        this.mMessage = mMessage;
    }

    public String getMessage() {
        return mMessage;
    }
}
