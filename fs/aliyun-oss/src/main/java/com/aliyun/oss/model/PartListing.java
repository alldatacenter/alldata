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
 * The entity class wraps the result of the list parts request.
 *
 */
public class PartListing extends GenericResult {

    private String bucketName;

    private String key;

    private String uploadId;

    private Integer maxParts;

    private Integer partNumberMarker;

    private String storageClass;

    private boolean isTruncated;

    private Integer nextPartNumberMarker;

    private List<PartSummary> parts = new ArrayList<PartSummary>();

    /**
     * Gets the {@link Bucket} name.
     * 
     * @return Bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the {@link Bucket} name.
     * 
     * @param bucketName
     *            Bucket name.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the {@link OSSObject} key.
     * 
     * @return Object key。
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the {@link OSSObject} key.
     * 
     * @param key
     *            Object key.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the multipart Upload ID.
     * 
     * @return The multipart upload Id.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Sets the multipart upload Id.
     * 
     * @param uploadId
     *            The multipart upload Id.
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    /**
     * Gets the {@link ListPartsRequest#getPartNumberMarker()}.
     * 
     * @return Part number marker.
     */
    public Integer getPartNumberMarker() {
        return partNumberMarker;
    }

    /**
     * Sets the part number marker with
     * {@link ListPartsRequest#getPartNumberMarker()}.
     * 
     * @param partNumberMarker
     *            Part number marker.
     */
    public void setPartNumberMarker(int partNumberMarker) {
        this.partNumberMarker = partNumberMarker;
    }

    /**
     * Gets the next part number maker if there's remaining data left in the
     * server.
     * 
     * @return The next part number.
     */
    public Integer getNextPartNumberMarker() {
        return nextPartNumberMarker;
    }

    /**
     * Sets the next part number maker if there's remaining data left in the
     * server.
     * 
     * @param nextPartNumberMarker
     *            The next part number marker.
     */
    public void setNextPartNumberMarker(int nextPartNumberMarker) {
        this.nextPartNumberMarker = nextPartNumberMarker;
    }

    /**
     * Gets the max parts count to return, the value comes from
     * {@link ListPartsRequest#getMaxParts()}.
     * 
     * @return The max parts count.
     */
    public Integer getMaxParts() {
        return maxParts;
    }

    /**
     * Sets the max parts count. The value comes from （
     * {@link ListPartsRequest#getMaxParts()}).
     * 
     * @param maxParts
     *            The max parts count.
     */
    public void setMaxParts(int maxParts) {
        this.maxParts = maxParts;
    }

    /**
     * Gets the flag of if the result is truncated. If there's data remaining in
     * the server side, the flag is true.
     * 
     * @return true: the result is truncated; false the result is not truncated.
     */
    public boolean isTruncated() {
        return isTruncated;
    }

    /**
     * Sets the flag of if the result is truncated. If there's data remaining in
     * the server side, the flag is true.
     * 
     * @param isTruncated
     *            true: the result is truncated; false the result is not
     *            truncated.
     */
    public void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    /**
     * Gets the list of {@link PartSummary}.
     * 
     * @return The list of {@link PartSummary}.
     */
    public List<PartSummary> getParts() {
        return parts;
    }

    /**
     * Sets the list of {@link PartSummary}.
     * 
     * @param parts
     *            The list of {@link PartSummary}.
     */
    public void setParts(List<PartSummary> parts) {
        this.parts.clear();
        if (parts != null && !parts.isEmpty()) {
            this.parts.addAll(parts);
        }
    }

    /**
     * Adds a {@link PartSummary} instance.
     * 
     * @param partSummary
     *            A {@link PartSummary} instance.
     */
    public void addPart(PartSummary partSummary) {
        this.parts.add(partSummary);
    }

}
