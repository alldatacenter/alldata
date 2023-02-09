package com.qcloud.cos.model.ciModel.queue;

import com.qcloud.cos.model.ciModel.common.MediaCommonResponse;

import java.util.ArrayList;
import java.util.List;
/**
 * 媒体处理 队列列表响应实体 https://cloud.tencent.com/document/product/460/48234
 */
public class MediaListQueueResponse extends MediaCommonResponse {
    private List<MediaQueueObject> queueList;
    private List<String> nonExistPIDs;

    public MediaListQueueResponse() {
        queueList = new ArrayList<>();
        nonExistPIDs = new ArrayList<>();
    }

    public List<MediaQueueObject> getQueueList() {
        return queueList;
    }

    public void setQueueList(List<MediaQueueObject> queueList) {
        this.queueList = queueList;
    }

    public List<String> getNonExistPIDs() {
        return nonExistPIDs;
    }

    public void setNonExistPIDs(List<String> nonExistPIDs) {
        this.nonExistPIDs = nonExistPIDs;
    }

    @Override
    public String toString() {
        return "MediaQueueResponse{" +
                "queueList=" + queueList +
                ", nonExistPIDs=" + nonExistPIDs +
                '}';
    }
}
