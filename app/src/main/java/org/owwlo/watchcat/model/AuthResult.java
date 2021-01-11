package org.owwlo.watchcat.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.alibaba.fastjson.annotation.JSONField;

public class AuthResult implements Parcelable {
    public static final int kRESULT_GRANTED = 0;
    public static final int kRESULT_DENIED = 1;
    public static final int kRESULT_NEW_AUTH = 2;

    @JSONField(name = "result")
    private int result = kRESULT_DENIED;

    public AuthResult(int result) {
        this.result = result;
    }

    public AuthResult() {
    }

    protected AuthResult(Parcel in) {
        result = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(result);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AuthResult> CREATOR = new Creator<AuthResult>() {
        @Override
        public AuthResult createFromParcel(Parcel in) {
            return new AuthResult(in);
        }

        @Override
        public AuthResult[] newArray(int size) {
            return new AuthResult[size];
        }
    };

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
