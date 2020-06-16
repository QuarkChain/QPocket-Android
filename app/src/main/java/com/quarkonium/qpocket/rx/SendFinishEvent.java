package com.quarkonium.qpocket.rx;

public class SendFinishEvent {
    private boolean state = true;

    public void setState(boolean state) {
        this.state = state;
    }

    public boolean isState() {
        return state;
    }
}
