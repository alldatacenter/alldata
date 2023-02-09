package com.qcloud.cos.model.ciModel.job;

import java.util.ArrayList;
import java.util.List;

/**
 * 媒体处理拼接模板实体类
 */
public class MediaConcatTemplateObject {
    private List<MediaConcatFragmentObject> concatFragmentList;
    private MediaAudioObject audio;
    private MediaVideoObject video;
    private MediaContainerObject container;
    private String index;

    public List<MediaConcatFragmentObject> getConcatFragmentList() {
        if (concatFragmentList == null) {
            concatFragmentList = new ArrayList<>();
        }
        return concatFragmentList;
    }

    public void setConcatFragmentList(List<MediaConcatFragmentObject> concatFragmentList) {
        this.concatFragmentList = concatFragmentList;
    }

    public MediaAudioObject getAudio() {
        if (audio == null) {
            audio = new MediaAudioObject();
        }
        return audio;
    }

    public void setAudio(MediaAudioObject audio) {
        this.audio = audio;
    }

    public MediaVideoObject getVideo() {
        if (video == null) {
            video = new MediaVideoObject();
        }
        return video;
    }

    public void setVideo(MediaVideoObject video) {
        this.video = video;
    }

    public MediaContainerObject getContainer() {
        if (container == null){
            container = new MediaContainerObject();
        }
        return container;
    }

    public void setContainer(MediaContainerObject container) {
        this.container = container;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediaConcatTemplateObject{");
        sb.append("concatFragmentList=").append(concatFragmentList);
        sb.append(", audio=").append(audio);
        sb.append(", video=").append(video);
        sb.append(", container=").append(container);
        sb.append(", index='").append(index).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
