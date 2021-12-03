package org.owwlo.watchcat.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.alibaba.fastjson.annotation.JSONField;

@Entity
public class HodorRequest implements Parcelable {
    @PrimaryKey
    @NonNull
    @JSONField(name = "id")
    private String id;

    @Ignore
    public HodorRequest(String id) {
        this.id = id;
    }

    public HodorRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected HodorRequest(Parcel in) {
        id = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
    }

    public static final Creator<HodorRequest> CREATOR = new Creator<HodorRequest>() {
        @Override
        public HodorRequest createFromParcel(Parcel in) {
            return new HodorRequest(in);
        }

        @Override
        public HodorRequest[] newArray(int size) {
            return new HodorRequest[size];
        }
    };
}
