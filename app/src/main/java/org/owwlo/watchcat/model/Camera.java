package org.owwlo.watchcat.model;

import androidx.annotation.Nullable;

import org.owwlo.watchcat.utils.Utils;

public class Camera {
    private final String ip;
    private final CameraInfo info;

    public Camera(String ip, CameraInfo info) {
        this.ip = ip;
        this.info = info;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Camera) {
            final Camera other = (Camera) obj;
            return ip.equals(other.ip)
                    && info.equals(other.info)
                    ;
        }
        return false;
    }

    public String getIp() {
        return ip;
    }

    public CameraInfo getInfo() {
        return info;
    }

    public Utils.Urls getUrls() {
        return Utils.Urls.getTarget(ip, info.getControlPort(), info.getStreamingPort());
    }
}
