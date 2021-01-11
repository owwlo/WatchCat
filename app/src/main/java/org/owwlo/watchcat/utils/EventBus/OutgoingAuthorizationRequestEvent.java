package org.owwlo.watchcat.utils.EventBus;

import org.owwlo.watchcat.model.Camera;

public class OutgoingAuthorizationRequestEvent {
    public Camera getCamera() {
        return camera;
    }

    public OutgoingAuthorizationRequestEvent(Camera camera) {
        this.camera = camera;
    }

    final Camera camera;

}
