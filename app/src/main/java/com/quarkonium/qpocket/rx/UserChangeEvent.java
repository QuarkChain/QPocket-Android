package com.quarkonium.qpocket.rx;

public class UserChangeEvent {
    private String message;

    public UserChangeEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
