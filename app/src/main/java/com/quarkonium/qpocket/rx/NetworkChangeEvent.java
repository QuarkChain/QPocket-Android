package com.quarkonium.qpocket.rx;

public class NetworkChangeEvent {
    private String message;

    public NetworkChangeEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
