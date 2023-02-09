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

import java.io.InputStream;

/**
 * The result of a multipart upload.
 *
 */
public class CompleteMultipartUploadResult extends GenericResult implements CallbackResult {

    /** The name of the bucket containing the completed multipart upload. */
    private String bucketName;

    /** The key by which the object is stored. */
    private String key;

    /** The URL identifying the new multipart object. */
    private String location;

    private String eTag;
    
    /** Object Version Id. */
    private String versionId;

    /** The callback request's response body */
    private InputStream callbackResponseBody;

    /**
     * Gets the url of the target file of this multipart upload.
     * 
     * @return The url of the target file of this multipart upload.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the url of the target file of this multipart upload.
     * 
     * @param location
     *            The url of the target file of this multipart upload.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the bucket name of the target file of this multipart upload.
     * 
     * @return Bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the bucket name of the target file of this multipart upload.
     * 
     * @param bucketName
     *            Bucket name.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the target file's key.
     * 
     * @return The target file's key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the target file's key.
     * 
     * @param key
     *            The target file's key.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the ETag of the target file.
     * 
     * @return ETag of the target file.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the ETag of the target file.
     * 
     * @param etag
     *            ETag of the target file.
     */
    public void setETag(String etag) {
        this.eTag = etag;
    }
    
    /**
     * Gets version id.
     * 
     * @return version id.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets version id.
     * 
     * @param versionId version id
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Deprecated. Gets the callback response body. The caller needs to close it
     * after usage.
     * 
     * @return The response body.
     */
    @Override
    @Deprecated
    public InputStream getCallbackResponseBody() {
        return callbackResponseBody;
    }

    /**
     * Sets the callback response body.
     * 
     * @param callbackResponseBody
     *            The callback response body.
     */
    @Override
    public void setCallbackResponseBody(InputStream callbackResponseBody) {
        this.callbackResponseBody = callbackResponseBody;
    }

}
