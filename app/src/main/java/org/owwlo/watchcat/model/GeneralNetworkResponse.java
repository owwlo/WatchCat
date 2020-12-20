package org.owwlo.watchcat.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.alibaba.fastjson.annotation.JSONField;


public class GeneralNetworkResponse implements Parcelable {

    @JSONField(name = "succeed")
    private boolean succeed;

    public GeneralNetworkResponse(boolean succeed) {
        this.succeed = succeed;
    }

    protected GeneralNetworkResponse(Parcel in) {
        succeed = (in.readInt() == 1);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(succeed ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GeneralNetworkResponse> CREATOR = new Creator<GeneralNetworkResponse>() {
        @Override
        public GeneralNetworkResponse createFromParcel(Parcel in) {
            return new GeneralNetworkResponse(in);
        }

        @Override
        public GeneralNetworkResponse[] newArray(int size) {
            return new GeneralNetworkResponse[size];
        }
    };

    public boolean isSucceed() {
        return succeed;
    }

    public void setSucceed(boolean succeed) {
        this.succeed = succeed;
    }

}
