package com.example.mobileplayer.entity;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.alibaba.fastjson.annotation.JSONField;
import com.example.mobileplayer.invariable.MediaType;

public class MediaItem implements Parcelable {
    @JSONField(name = "movieName")
    private String name;
    private Uri uri;
    private long duration;
    private long size;
    @JSONField(name = "coverImg")
    private String coverImg;
    @JSONField(name = "videoTitle")
    private String title;
    private MediaType mediaType;

    public MediaItem(String name, Uri uri, long duration, long size, MediaType type) {
        this.name = name;
        this.uri = uri;
        this.duration = duration;
        this.size = size;
        this.mediaType = type;
    }

    public MediaItem() {
    }

    protected MediaItem(Parcel in) {
        name = in.readString();
        uri = in.readParcelable(Uri.class.getClassLoader());
        duration = in.readLong();
        size = in.readLong();
        coverImg = in.readString();
        title = in.readString();
        String _type = in.readString();
        if(_type != null) {
            mediaType = MediaType.valueOf(_type);
        }
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JSONField(name = "hightUrl")
    public void setUri(String uri) {
        this.uri = Uri.parse(uri);
    }

    @JSONField(name = "videoLength")
    public void setDuration(int duration) {
        this.duration = duration * 1000;
    }

    public String getCoverImg() {
        return coverImg;
    }

    public void setCoverImg(String coverImg) {
        this.coverImg = coverImg;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

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
        dest.writeString(coverImg);
        dest.writeString(title);
        dest.writeString(mediaType != null ? mediaType.name() : null);
    }
}
