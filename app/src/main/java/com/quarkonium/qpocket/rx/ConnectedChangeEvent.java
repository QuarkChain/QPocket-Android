package com.quarkonium.qpocket.rx;

public class ConnectedChangeEvent {
    private boolean message;

    public ConnectedChangeEvent(boolean message) {
        this.message = message;
    }

    public boolean getMessage() {
        return message;
    }

    public void setMessage(boolean message) {
        this.message = message;
    }
}
