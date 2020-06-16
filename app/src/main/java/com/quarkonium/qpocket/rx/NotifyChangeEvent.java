package com.quarkonium.qpocket.rx;

public class NotifyChangeEvent {
    private String message;

    public NotifyChangeEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
