package org.owwlo.watchcat.model;

import android.graphics.Bitmap;

public class Camera {
    private String ip;
    private int streamingPort;
    private int controlPort;
    private Bitmap preview = null;

    public Camera(String ip, int streamingPort, Bitmap preview) {
        this.ip = ip;
        this.streamingPort = streamingPort;
        this.preview = preview;
    }

    public Camera(String ip, int streamingPort, int controlPort) {
        this.ip = ip;
        this.streamingPort = streamingPort;
        this.controlPort = controlPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getStreamingPort() {
        return streamingPort;
    }

    public int getControlPort() {
        return controlPort;
    }

    public void setPort(int streamingPort) {
        this.streamingPort = streamingPort;
    }

    public Bitmap getPreview() {
        return preview;
    }

    public void setPreview(Bitmap preview) {
        this.preview = preview;
    }
}
