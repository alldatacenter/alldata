package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.internal.CIServiceRequest;

/**
 * 文档预览任务发起请求类
 */
public class DocJobRequest extends CIServiceRequest {

    /**
     * cos桶名称
     */
    private String bucketName;

    /**
     * 文档预览任务对象
     */
    private DocJobObject docJobObject;

    /**
     * 任务ID
     */
    private String jobId;

    /**
     * 队列ID
     */
    private String queueId;

    /**
     * 查询类型
     */
    private String tag;

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public DocJobObject getDocJobObject() {
        if (docJobObject == null) {
            docJobObject = new DocJobObject();
        }
        return docJobObject;
    }

    public void setDocJobObject(DocJobObject docJobObject) {
        this.docJobObject = docJobObject;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
