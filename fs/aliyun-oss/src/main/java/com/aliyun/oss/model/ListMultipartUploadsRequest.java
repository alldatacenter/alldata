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

/**
 * This is the request class to list executing multipart uploads under a bucket.
 *
 */
public class ListMultipartUploadsRequest extends GenericRequest {

    private String delimiter;

    private String prefix;

    private Integer maxUploads;

    private String keyMarker;

    private String uploadIdMarker;

    private String encodingType;

    /**
     * Constructor.
     * 
     * @param bucketName
     *            Bucket name.
     */
    public ListMultipartUploadsRequest(String bucketName) {
        super(bucketName);
    }

    /**
     * Gets the max number of uploads to return.
     * 
     * @return The max number of uploads.
     */
    public Integer getMaxUploads() {
        return maxUploads;
    }

    /**
     * Sets the max number of uploads to return. The both max and default value
     * is 1000。
     * 
     * @param maxUploads
     *            The max number of uploads.
     */
    public void setMaxUploads(Integer maxUploads) {
        this.maxUploads = maxUploads;
    }

    /**
     * Gets the key marker filter---all uploads returned whose target file's key
     * must be greater than the marker filter.
     * 
     * @return The key marker filter.
     */
    public String getKeyMarker() {
        return keyMarker;
    }

    /**
     * Sets the key marker filter---all uploads returned whose target file's key
     * must be greater than the marker filter.
     * 
     * @param keyMarker
     *            The key marker.
     */
    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    /**
     * Gets the upload id marker--all uploads returned whose upload id must be
     * greater than the marker filter.
     * 
     * @return The upload Id marker.
     */
    public String getUploadIdMarker() {
        return uploadIdMarker;
    }

    /**
     * Sets the upload id marker--all uploads returned whose upload id must be
     * greater than the marker filter.
     * 
     * @param uploadIdMarker
     *            The upload Id marker.
     */
    public void setUploadIdMarker(String uploadIdMarker) {
        this.uploadIdMarker = uploadIdMarker;
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

    /**
     * Gets the encoding type of the object in the response body.
     * 
     * @return The encoding type of the object in the response body.
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Sets the encoding type of the object in the response body.
     * 
     * @param encodingType
     *            The encoding type of the object in the response body. Valid
     *            value is either 'null' or 'url'.
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

}
