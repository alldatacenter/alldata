package com.qcloud.cos.model.ciModel.mediaInfo;

/**
 * MediaInfo 格式详情实体类 详情见：https://cloud.tencent.com/document/product/460/38935
 */
public class MediaStream {
    /**
     * 视频信息
     */
    private MediaInfoVideo video;
    /**
     * 音频信息
     */
    private MediaInfoAudio audio;
    /**
     * 字幕信息
     */
    private MediaInfoSubtitle subtitle;

    public MediaInfoVideo getVideo() {
        if (video==null){
            video = new MediaInfoVideo();
        }
        return video;
    }

    public void setVideo(MediaInfoVideo video) {
        this.video = video;
    }

    public MediaInfoAudio getAudio() {
        if (audio == null){
            audio = new MediaInfoAudio();
        }
        return audio;
    }

    public void setAudio(MediaInfoAudio audio) {
        this.audio = audio;
    }

    public MediaInfoSubtitle getSubtitle() {
        if (subtitle==null){
            subtitle = new MediaInfoSubtitle();
        }
        return subtitle;
    }

    public void setSubtitle(MediaInfoSubtitle subtitle) {
        this.subtitle = subtitle;
    }

    @Override
    public String toString() {
        return "MediaStream{" +
                "video=" + video +
                ", audio=" + audio +
                ", subtitle=" + subtitle +
                '}';
    }
}
