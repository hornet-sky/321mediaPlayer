package com.example.mobileplayer.invariable;

public enum MediaType {
    LOCAL_VIDEO("本地视频"), NET_VIDEO("网络视频"), LOCAL_AUDIO("本地音频"), NET_AUDIO("网络音频");
    private String desc;
    private MediaType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
