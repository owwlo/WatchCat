package org.owwlo.watchcat.utils.EventBus;

public class IncomingAuthorizationRequestEvent {
    private final String incomingId;
    private final String name;
    private final String passcode;

    public String getIncomingId() {
        return incomingId;
    }

    public String getPasscode() {
        return passcode;
    }

    public String getName() {
        return name;
    }

    public IncomingAuthorizationRequestEvent(String name, String incomingId, String passcode) {
        this.name = name;
        this.incomingId = incomingId;
        this.passcode = passcode;
    }
}
