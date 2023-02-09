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
 * This is the request class to list parts of a ongoing multipart upload.
 *
 */
public class ListPartsRequest extends GenericRequest {

    private String uploadId;

    private Integer maxParts;

    private Integer partNumberMarker;

    private String encodingType;

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name.
     * @param key
     *            Object key.
     * @param uploadId
     *            Mutlipart upload Id.
     */
    public ListPartsRequest(String bucketName, String key, String uploadId) {
        super(bucketName, key);
        this.uploadId = uploadId;
    }

    /**
     * Gets the upload Id.
     * 
     * @return The upload Id.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Sets the upload Id for the multipart upload.
     * 
     * @param uploadId
     *            The upload Id.
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    /**
     * The max parts count to return. By default it's 1000.
     * 
     * @return The max parts count.
     */
    public Integer getMaxParts() {
        return maxParts;
    }

    /**
     * Sets the max parts count (optional). The default and max value is 1000.
     * 
     * @param maxParts
     *            The max parts count.
     */
    public void setMaxParts(int maxParts) {
        this.maxParts = maxParts;
    }

    /**
     * Gets the part number marker---the parts to return whose part number must
     * be greater than this marker.
     * 
     * @return The part number marker.
     */
    public Integer getPartNumberMarker() {
        return partNumberMarker;
    }

    /**
     * Sets the part number marker---the parts to return whose part number must
     * be greater than this marker.
     * 
     * @param partNumberMarker
     *            The part number marker.
     */
    public void setPartNumberMarker(Integer partNumberMarker) {
        this.partNumberMarker = partNumberMarker;
    }

    /**
     * Gets the encoding type of object names in response.
     * 
     * @return The encoding type of object names.
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Sets the encoding type of object names in response.
     * 
     * @param encodingType
     *            The encoding type of object names. The valid values are 'null'
     *            or 'url'.
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

}
