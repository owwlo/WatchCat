package org.owwlo.watchcat.model;

import org.owwlo.watchcat.utils.Utils;

public class Camera {
    private final String ip;
    private final CameraInfo info;

    public Camera(String ip, CameraInfo info) {
        this.ip = ip;
        this.info = info;
    }

    public String getIp() {
        return ip;
    }

    public CameraInfo getInfo() {
        return info;
    }

    public Utils.Urls getUrls() {
        return Utils.Urls.getTarget(ip, info.getControlPort());
    }
}
