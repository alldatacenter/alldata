package com.aliyun.oss.model;

public class PutBucketImageRequest extends GenericRequest {
    private String bucketName;
    private boolean isForbidOrigPicAccess = false;
    private boolean isUseStyleOnly = false;
    private boolean isAutoSetContentType = false;
    private boolean isUseSrcFormat = false;
    private boolean isSetAttachName = false;
    private String default404Pic = "";
    private String styleDelimiters = "!"; // /,-,_,!

    public PutBucketImageRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    public void SetBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String GetBucketName() {
        return this.bucketName;
    }

    public void SetIsForbidOrigPicAccess(boolean isForbidOrigPicAccess) {
        this.isForbidOrigPicAccess = isForbidOrigPicAccess;
    }

    public boolean GetIsForbidOrigPicAccess() {
        return this.isForbidOrigPicAccess;
    }

    public void SetIsUseStyleOnly(boolean isUseStyleOnly) {
        this.isUseStyleOnly = isUseStyleOnly;
    }

    public boolean GetIsUseStyleOnly() {
        return this.isUseStyleOnly;
    }

    public void SetIsAutoSetContentType(boolean isAutoSetContentType) {
        this.isAutoSetContentType = isAutoSetContentType;
    }

    public boolean GetIsAutoSetContentType() {
        return this.isAutoSetContentType;
    }

    public void SetIsUseSrcFormat(boolean isUseSrcFormat) {
        this.isUseSrcFormat = isUseSrcFormat;
    }

    public boolean GetIsUseSrcFormat() {
        return this.isUseSrcFormat;
    }

    public void SetIsSetAttachName(boolean isSetAttachName) {
        this.isSetAttachName = isSetAttachName;
    }

    public boolean GetIsSetAttachName() {
        return this.isSetAttachName;
    }

    public void SetDefault404Pic(String default404Pic) {
        this.default404Pic = default404Pic;
    }

    public String GetDefault404Pic() {
        return this.default404Pic;
    }

    public void SetStyleDelimiters(String styleDelimiters) {
        this.styleDelimiters = styleDelimiters;
    }

    public String GetStyleDelimiters() {
        return this.styleDelimiters;
    }
}
