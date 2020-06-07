package org.owwlo.watchcat.model;

import android.graphics.Bitmap;

public class Camera {
    private String ip;
    private int port;
    private Bitmap preview = null;

    public Camera(String ip, int port, Bitmap preview) {
        this.ip = ip;
        this.port = port;
        this.preview = preview;
    }

    public Camera(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Bitmap getPreview() {
        return preview;
    }

    public void setPreview(Bitmap preview) {
        this.preview = preview;
    }
}
