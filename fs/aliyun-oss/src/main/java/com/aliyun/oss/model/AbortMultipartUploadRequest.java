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
 * The request class which is used to abort a multipart upload.
 *
 */
public class AbortMultipartUploadRequest extends GenericRequest {

    /** The ID of the multipart upload to abort */
    private String uploadId;

    /**
     * Constructor.
     * 
     * @param bucketName
     *            Bucket name.
     * @param key
     *            Object key。
     * @param uploadId
     *            The multipart upload Id.
     */
    public AbortMultipartUploadRequest(String bucketName, String key, String uploadId) {
        super(bucketName, key);
        this.uploadId = uploadId;
    }

    /**
     * Gets the upload Id.
     * 
     * @return The upload Id
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Sets the upload Id.
     * 
     * @param uploadId
     *            The upload Id.
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
}
