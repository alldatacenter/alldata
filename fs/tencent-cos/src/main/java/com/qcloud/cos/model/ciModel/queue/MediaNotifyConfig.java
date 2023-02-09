package com.qcloud.cos.model.ciModel.queue;


/**
 * 媒体处理 回调通知渠道 实体 https://cloud.tencent.com/document/product/460/42324
 */
public class MediaNotifyConfig {
    /**
     * 回调配置
     */
    private String url;
    /**
     * 回调类型，普通回调：Url
     */
    private String type;
    /**
     * 回调事件 视频转码完成:TransCodingFinish
     */
    private String event;
    /**
     * 回调开关，Off，On
     */
    private String state;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MediaNotifyConfig{");
        sb.append("url='").append(url).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", event='").append(event).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
