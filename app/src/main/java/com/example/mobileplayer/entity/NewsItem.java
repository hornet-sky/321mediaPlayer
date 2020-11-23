package com.example.mobileplayer.entity;

import com.alibaba.fastjson.annotation.JSONField;

public class NewsItem {
    /**
     * PLAYTIME : 2020-11-21 18:42:02
     * DRETITLE : [共同关注]美国大选 佐治亚州正式认证拜登赢得该州选举人票
     * PAGELINK : https://tv.cctv.com/2020/11/21/VIDEoSrREFeWzmbiyVNrgQD9201121.shtml
     * IMAGELINK : https://p2.img.cctvpic.com/fmspic/2020/11/21/82a9a50224734b3194e33e54cc7166c7-31105778-1.jpg
     * DRECONTENT : 美国大选：佐治亚州正式认证拜登赢得该州选举人票。
     * PUBTIME : 2020-11-21 18:42:02
     * SOURCE : NEWCMS_video
     * DURATION : 151
     * DETAILSID : 82a9a50224734b3194e33e54cc7166c7
     * ALBUMID :
     * VIDEO_TYPE : 0
     */
    @JSONField(name = "PLAYTIME")
    private String playTime;
    @JSONField(name = "DRETITLE")
    private String dreTitle;
    @JSONField(name = "PAGELINK")
    private String pageLink;
    @JSONField(name = "IMAGELINK")
    private String imageLink;
    @JSONField(name = "DRECONTENT")
    private String dreContent;
    @JSONField(name = "PUBTIME")
    private String pubTime;
    @JSONField(name = "SOURCE")
    private String source;
    @JSONField(name = "DURATION")
    private Integer duration;
    @JSONField(name = "DETAILSID")
    private String detailsId;
    @JSONField(name = "ALBUMID")
    private String albumId;
    @JSONField(name = "VIDEO_TYPE")
    private Integer videoType;

    public String getPlayTime() {
        return playTime;
    }

    public void setPlayTime(String playTime) {
        this.playTime = playTime;
    }

    public String getDreTitle() {
        return dreTitle;
    }

    public void setDreTitle(String dreTitle) {
        this.dreTitle = dreTitle;
    }

    public String getPageLink() {
        return pageLink;
    }

    public void setPageLink(String pageLink) {
        this.pageLink = pageLink;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public String getDreContent() {
        return dreContent;
    }

    public void setDreContent(String dreContent) {
        this.dreContent = dreContent;
    }

    public String getPubTime() {
        return pubTime;
    }

    public void setPubTime(String pubTime) {
        this.pubTime = pubTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getDetailsId() {
        return detailsId;
    }

    public void setDetailsId(String detailsId) {
        this.detailsId = detailsId;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public Integer getVideoType() {
        return videoType;
    }

    public void setVideoType(Integer videoType) {
        this.videoType = videoType;
    }
}
