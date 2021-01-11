package org.owwlo.watchcat.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.alibaba.fastjson.annotation.JSONField;

@Entity
public class Viewer implements Parcelable {
    @PrimaryKey
    @NonNull
    @JSONField(name = "id")
    private String id;
    @JSONField(name = "name")
    private String name;
    private boolean myself;

    public static Viewer createFromKey(String id) {
        return new Viewer(id, null, false);
    }

    public static Viewer createSelf(String id) {
        return new Viewer(id, null, true);
    }

    public static Viewer createClient(String id, String name) {
        return new Viewer(id, name, false);
    }

    public Viewer(String id, String name, boolean self) {
        this.id = id;
        this.name = name;
        this.myself = self;
    }

    public Viewer() {
    }

    public Viewer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMyself() {
        return myself;
    }

    public void setMyself(boolean myself) {
        this.myself = myself;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected Viewer(Parcel in) {
        id = in.readString();
        name = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
    }

    public static final Creator<Viewer> CREATOR = new Creator<Viewer>() {
        @Override
        public Viewer createFromParcel(Parcel in) {
            return new Viewer(in);
        }

        @Override
        public Viewer[] newArray(int size) {
            return new Viewer[size];
        }
    };
}
