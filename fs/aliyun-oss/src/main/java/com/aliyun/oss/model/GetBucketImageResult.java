package com.aliyun.oss.model;

public class GetBucketImageResult extends PutBucketImageRequest {
    private String status;

    public GetBucketImageResult(String bucketName) {
        super(bucketName);
        // TODO Auto-generated constructor stub
    }

    public GetBucketImageResult() {
        super("");
    }

    public void SetStatus(String status) {
        this.status = status;
    }

    public String GetStatus() {
        return this.status;
    }
}
