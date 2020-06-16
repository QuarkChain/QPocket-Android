package com.quarkonium.qpocket.rx;

public class ChooseWalletEvent {
    private String message;

    public ChooseWalletEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
