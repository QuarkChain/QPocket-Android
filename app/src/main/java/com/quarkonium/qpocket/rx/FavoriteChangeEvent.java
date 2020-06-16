package com.quarkonium.qpocket.rx;

public class FavoriteChangeEvent {
    private String message;

    public FavoriteChangeEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
