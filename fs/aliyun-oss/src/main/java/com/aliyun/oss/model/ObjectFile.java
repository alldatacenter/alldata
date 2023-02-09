package com.aliyun.oss.model;

public class ObjectFile {
    private String filename;
    private long size;
    private String fileModifiedTime;
    private String fileCreateTime;
    private String fileAccessTime;
    private String ossObjectType;
    private String ossStorageClass;
    private String objectACL;
    private String eTag;
    private String ossCRC64;
    private int ossTaggingCount;
    private OSSTagging ossTagging;
    private OSSUserMeta ossUserMeta;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFileModifiedTime() {
        return fileModifiedTime;
    }

    public void setFileModifiedTime(String fileModifiedTime) {
        this.fileModifiedTime = fileModifiedTime;
    }

    public String getFileCreateTime() {
        return fileCreateTime;
    }

    public void setFileCreateTime(String fileCreateTime) {
        this.fileCreateTime = fileCreateTime;
    }

    public String getFileAccessTime() {
        return fileAccessTime;
    }

    public void setFileAccessTime(String fileAccessTime) {
        this.fileAccessTime = fileAccessTime;
    }

    public String getOssObjectType() {
        return ossObjectType;
    }

    public void setOssObjectType(String ossObjectType) {
        this.ossObjectType = ossObjectType;
    }

    public String getOssStorageClass() {
        return ossStorageClass;
    }

    public void setOssStorageClass(String ossStorageClass) {
        this.ossStorageClass = ossStorageClass;
    }

    public String getObjectACL() {
        return objectACL;
    }

    public void setObjectACL(String objectACL) {
        this.objectACL = objectACL;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getOssCRC64() {
        return ossCRC64;
    }

    public void setOssCRC64(String ossCRC64) {
        this.ossCRC64 = ossCRC64;
    }

    public int getOssTaggingCount() {
        return ossTaggingCount;
    }

    public void setOssTaggingCount(int ossTaggingCount) {
        this.ossTaggingCount = ossTaggingCount;
    }

    public OSSTagging getOssTagging() {
        return ossTagging;
    }

    public void setOssTagging(OSSTagging ossTagging) {
        this.ossTagging = ossTagging;
    }

    public OSSUserMeta getOssUserMeta() {
        return ossUserMeta;
    }

    public void setOssUserMeta(OSSUserMeta ossUserMeta) {
        this.ossUserMeta = ossUserMeta;
    }
}
