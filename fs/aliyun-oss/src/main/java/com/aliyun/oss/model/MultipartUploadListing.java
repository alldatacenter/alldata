/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The entity class that wraps all information about multipart upload.
 */
public class MultipartUploadListing extends GenericResult {

    private String bucketName;

    private String keyMarker;

    private String delimiter;

    private String prefix;

    private String uploadIdMarker;

    private int maxUploads;

    private boolean isTruncated;

    private String nextKeyMarker;

    private String nextUploadIdMarker;

    private List<MultipartUpload> multipartUploads = new ArrayList<MultipartUpload>();

    private List<String> commonPrefixes = new ArrayList<String>();

    public MultipartUploadListing() {
    }

    public MultipartUploadListing(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKeyMarker() {
        return keyMarker;
    }

    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    public String getUploadIdMarker() {
        return uploadIdMarker;
    }

    public void setUploadIdMarker(String uploadIdMarker) {
        this.uploadIdMarker = uploadIdMarker;
    }

    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    public void setNextKeyMarker(String nextKeyMarker) {
        this.nextKeyMarker = nextKeyMarker;
    }

    public String getNextUploadIdMarker() {
        return nextUploadIdMarker;
    }

    public void setNextUploadIdMarker(String nextUploadIdMarker) {
        this.nextUploadIdMarker = nextUploadIdMarker;
    }

    public int getMaxUploads() {
        return maxUploads;
    }

    public void setMaxUploads(int maxUploads) {
        this.maxUploads = maxUploads;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    public List<MultipartUpload> getMultipartUploads() {
        return multipartUploads;
    }

    public void setMultipartUploads(List<MultipartUpload> multipartUploads) {
        this.multipartUploads.clear();
        if (multipartUploads != null && !multipartUploads.isEmpty()) {
            this.multipartUploads.addAll(multipartUploads);
        }
    }

    public void addMultipartUpload(MultipartUpload multipartUpload) {
        this.multipartUploads.add(multipartUpload);
    }

    public List<String> getCommonPrefixes() {
        return commonPrefixes;
    }

    public void setCommonPrefixes(List<String> commonPrefixes) {
        this.commonPrefixes.clear();
        if (commonPrefixes != null && !commonPrefixes.isEmpty()) {
            this.commonPrefixes.addAll(commonPrefixes);
        }
    }

    public void addCommonPrefix(String commonPrefix) {
        this.commonPrefixes.add(commonPrefix);
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
