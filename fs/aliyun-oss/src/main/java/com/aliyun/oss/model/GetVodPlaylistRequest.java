package com.aliyun.oss.model;

public class GetVodPlaylistRequest extends LiveChannelGenericRequest {

    public GetVodPlaylistRequest(String bucketName, String liveChannelName, long startTime,
             long endTime) {
        super(bucketName, liveChannelName);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    private Long startTime;
    private Long endTime;
}
