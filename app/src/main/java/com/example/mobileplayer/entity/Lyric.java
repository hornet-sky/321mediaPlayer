package com.example.mobileplayer.entity;

public class Lyric {
    private long position;
    private String content;
    private int lineHeight;
    private int width;
    private long duration;

    public Lyric(long position, String content) {
        this.position = position;
        this.content = content;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public String toString() {
        return "Lyric{" +
                "position=" + position +
                ", content='" + content + '\'' +
                ", lineHeight=" + lineHeight +
                ", width=" + width +
                ", duration=" + duration +
                '}';
    }
}
