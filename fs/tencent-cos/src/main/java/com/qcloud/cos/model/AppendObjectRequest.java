package com.qcloud.cos.model;

import java.io.File;
import java.io.InputStream;

public class AppendObjectRequest extends PutObjectRequest {

    private Long position;

    public AppendObjectRequest(String bucketName, String key, File file) {
        super(bucketName, key, file);
    }

    public AppendObjectRequest(String bucketName, String key, InputStream input) {
        this(bucketName, key, input, null);
    }

    public AppendObjectRequest(String bucketName, String key, InputStream input, ObjectMetadata metadata) {
        super(bucketName, key, input, metadata);
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public AppendObjectRequest withPosition(Long position) {
        setPosition(position);
        return this;
    }
}
