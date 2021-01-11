package org.owwlo.watchcat.utils.EventBus;

import org.owwlo.watchcat.model.Camera;

public class PinInputDoneEvent {
    final Camera camera;

    public Camera getCamera() {
        return camera;
    }

    public String getPasscode() {
        return passcode;
    }

    public PinInputDoneEvent(Camera camera, String passcode) {
        this.camera = camera;
        this.passcode = passcode;
    }

    final String passcode;
}
