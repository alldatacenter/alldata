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
 * This is the request that is used to initiate a multipart upload.
 *
 */
public class InitiateMultipartUploadRequest extends GenericRequest {

    private ObjectMetadata objectMetadata;
    private Boolean sequentialMode;

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name.
     * @param key
     *            Target object's key (which is the combined file after all
     *            parts are uploaded).
     */
    public InitiateMultipartUploadRequest(String bucketName, String key) {
        this(bucketName, key, null);
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name.
     * @param key
     *            Target object's key (which is the combined file after all
     *            parts are uploaded).
     * @param objectMetadata
     *            Object's metadata.
     */
    public InitiateMultipartUploadRequest(String bucketName, String key, ObjectMetadata objectMetadata) {
        super(bucketName, key);
        this.objectMetadata = objectMetadata;
    }

    /**
     * Gets the object's metadata.
     * 
     * @return The metadata of the object to create.
     */
    public ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    /**
     * Sets the object's metadata.
     * 
     * @param objectMetadata
     *            The metadata of the object to create.
     */
    public void setObjectMetadata(ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }


    /**
     * Gets the sequential mode setting.
     * @return the sequential mode flag.
     */
    public Boolean getSequentialMode() {
        return sequentialMode;
    }

    /**
     * Sets upload in sequential mode or not.
     * @param sequentialMode
     *            the sequential mode flag.
     */
    public void setSequentialMode(Boolean sequentialMode) {
        this.sequentialMode = sequentialMode;
    }

}
