package com.quarkonium.qpocket.rx;

public class FlashShareEvent {
    private String message;

    public FlashShareEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
