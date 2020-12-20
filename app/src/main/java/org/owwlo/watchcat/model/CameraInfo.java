package org.owwlo.watchcat.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.alibaba.fastjson.annotation.JSONField;

import org.owwlo.watchcat.services.ServiceDaemon;


public class CameraInfo implements Parcelable {

    @JSONField(name = "enabled")
    private boolean enabled;

    @JSONField(name = "width")
    private int width;

    @JSONField(name = "height")
    private int height;

    // TODO not used.
    @JSONField(name = "streamingPort")
    private int streamingPort;

    @JSONField(name = "controlPort")
    private int controlPort;

    public CameraInfo() {
    }


    protected CameraInfo(Parcel in) {
        enabled = (in.readInt() == 1);
        width = in.readInt();
        height = in.readInt();
        streamingPort = in.readInt();
        controlPort = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(streamingPort);
        dest.writeInt(controlPort);
    }

    public ServiceDaemon.RUNNING_MODE getRunningMode() {
        return isEnabled() ? ServiceDaemon.RUNNING_MODE.STREAMING : ServiceDaemon.RUNNING_MODE.STANDING_BY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CameraInfo> CREATOR = new Creator<CameraInfo>() {
        @Override
        public CameraInfo createFromParcel(Parcel in) {
            return new CameraInfo(in);
        }

        @Override
        public CameraInfo[] newArray(int size) {
            return new CameraInfo[size];
        }
    };

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getStreamingPort() {
        return streamingPort;
    }

    public void setStreamingPort(int streamingPort) {
        this.streamingPort = streamingPort;
    }

    public int getControlPort() {
        return controlPort;
    }

    public void setControlPort(int controlPort) {
        this.controlPort = controlPort;
    }
}
