package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.model.CiServiceResult;

/**
 * 媒体处理 任务响应实体 https://cloud.tencent.com/document/product/460/48234
 */
public class MediaJobResponse extends CiServiceResult {
    private MediaJobObject jobsDetail;

    public MediaJobObject getJobsDetail() {
        if (jobsDetail==null){
            jobsDetail = new MediaJobObject();
        }
        return jobsDetail;
    }

    @Override
    public String toString() {
        return "MediaJobResponse{" +
                "jobsDetail=" + jobsDetail +
                '}';
    }
}
