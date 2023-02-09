package com.aliyun.oss.model;

public class PutImageStyleRequest extends GenericRequest {
    private String bucketName;
    private String styleName;
    private String style;

    public void SetBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String GetBucketName() {
        return this.bucketName;
    }

    public void SetStyleName(String styleName) {
        this.styleName = styleName;
    }

    public String GetStyleName() {
        return this.styleName;
    }

    public void SetStyle(String style) {
        this.style = style;
    }

    public String GetStyle() {
        return this.style;
    }
}
