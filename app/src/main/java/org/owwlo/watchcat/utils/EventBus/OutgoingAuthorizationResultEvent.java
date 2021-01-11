package org.owwlo.watchcat.utils.EventBus;

import org.owwlo.watchcat.model.Camera;

public class OutgoingAuthorizationResultEvent {

    public int getResult() {
        return result;
    }

    public Camera getCamera() {
        return camera;
    }

    private final Camera camera;
    private final int result;

    public OutgoingAuthorizationResultEvent(Camera camera, int result) {
        this.camera = camera;
        this.result = result;
    }
}
