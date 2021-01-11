package org.owwlo.watchcat.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.alibaba.fastjson.annotation.JSONField;

public class ViewerPasscode implements Parcelable {

    @JSONField(name = "id")
    private String id;
    @JSONField(name = "passcode")
    private String passcode;

    public ViewerPasscode() {
    }

    public ViewerPasscode(String id, String passcode) {
        this.id = id;
        this.passcode = passcode;
    }

    protected ViewerPasscode(Parcel in) {
        id = in.readString();
        passcode = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(passcode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ViewerPasscode> CREATOR = new Creator<ViewerPasscode>() {
        @Override
        public ViewerPasscode createFromParcel(Parcel in) {
            return new ViewerPasscode(in);
        }

        @Override
        public ViewerPasscode[] newArray(int size) {
            return new ViewerPasscode[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }
}
