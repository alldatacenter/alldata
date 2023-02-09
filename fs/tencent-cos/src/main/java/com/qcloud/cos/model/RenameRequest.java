package com.qcloud.cos.model;

import com.qcloud.cos.internal.CosServiceRequest;

import java.io.Serializable;

// this rename request only used in merge bucket
public class RenameRequest extends CosServiceRequest implements Serializable {
    // src object name
    private String srcObject;
    // dst object name
    private String dstObject;
    // bucket name
    private String bucketName;

    public RenameRequest(String bucketName, String srcObject, String dstObject) {
        this.bucketName = bucketName;
        this.srcObject = srcObject;
        this.dstObject = dstObject;
    }

    public String getSrcObject() {
        return srcObject;
    }

    public String getDstObject() {
        return dstObject;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setSrcObject(String srcObject) {
        this.srcObject = srcObject;
    }

    public void setDstObject(String dstObject) {
        this.dstObject = dstObject;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
