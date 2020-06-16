package com.quarkonium.qpocket.rx;

public class RecentChangeEvent {
    private String message;

    public RecentChangeEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
