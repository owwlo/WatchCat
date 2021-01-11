package org.owwlo.watchcat.utils.EventBus;

public class IncomingAuthorizationCancelEvent {
    public String getIncomingId() {
        return incomingId;
    }

    private final String incomingId;

    public IncomingAuthorizationCancelEvent(String incomingId) {
        this.incomingId = incomingId;
    }
}
