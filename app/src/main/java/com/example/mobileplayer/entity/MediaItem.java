package com.example.mobileplayer.entity;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class MediaItem implements Parcelable {
    private String name;
    private Uri uri;
    private long duration;
    private long size;

    public MediaItem(String name, Uri uri, long duration, long size) {
        this.name = name;
        this.uri = uri;
        this.duration = duration;
        this.size = size;
    }

    public MediaItem() {
    }

    protected MediaItem(Parcel in) {
        name = in.readString();
        uri = in.readParcelable(Uri.class.getClassLoader());
        duration = in.readLong();
        size = in.readLong();
    }

    public static final Creator<MediaItem> CREATOR = new Creator<MediaItem>() {
        @Override
        public MediaItem createFromParcel(Parcel in) {
            return new MediaItem(in);
        }

        @Override
        public MediaItem[] newArray(int size) {
            return new MediaItem[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeParcelable(uri, flags);
        dest.writeLong(duration);
        dest.writeLong(size);
    }
}
