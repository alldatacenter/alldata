package com.aliyun.oss.model;

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

/**
 * A generic request that contains some basic request options, such as bucket
 * name, object key, costom headers, progress listener and so on.
 */
public class GenericRequest extends WebServiceRequest {

    private String bucketName;
    private String key;
    private String versionId;

    // The one who pays for the request
    private Payer payer;

    public GenericRequest() {}

    public GenericRequest(String bucketName) {
        this(bucketName, null);
    }

    public GenericRequest(String bucketName, String key) {
        this.bucketName = bucketName;
        this.key = key;
    }
    
    public GenericRequest(String bucketName, String key, String versionId) {
        this.bucketName = bucketName;
        this.key = key;
        this.versionId = versionId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public GenericRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GenericRequest withKey(String key) {
        setKey(key);
        return this;
    }
    
    /**
     * <p>
     * Gets the optional version ID specifying which version of the object to
     * operate. If not specified, the most recent version will be operated.
     * </p>
     * <p>
     * Objects created before versioning was enabled or when versioning is
     * suspended are given the default <code>null</code> version ID (see
     * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID}). Note that the
     * <code>null</code> version ID is a valid version ID and is not the
     * same as not having a version ID.
     * </p>
     *
     * @return The optional version ID specifying which version of the object to
     *         operate. If not specified, the most recent version will be operated.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the optional version ID specifying which version of the object to
     * operate. If not specified, the most recent version will be operated.
     * <p>
     * Objects created before versioning was enabled or when versioning is
     * suspended will be given the default <code>null</code> version ID (see
     * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID}). Note that the
     * <code>null</code> version ID is a valid version ID and is not the
     * same as not having a version ID.
     * </p>
     *
     * @param versionId
     *            The optional version ID specifying which version of the object
     *            to operate.
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
    
    /**
     * <p>
     * Sets the optional version ID specifying which version of the object to
     * download and returns this object, enabling additional method calls to be
     * chained together. If not specified, the most recent version will be
     * operated.
     * </p>
     * <p>
     * Objects created before versioning was enabled or when versioning is
     * suspended will be given the default or <code>null</code> version ID (see
     * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID}). Note that the
     * <code>null</code> version ID is a valid version ID and is not the
     * same as not having a version ID.
     * </p>
     *
     * @param versionId
     *            The optional version ID specifying which version of the object
     *            to operate.
     *
     * @return The updated request object.
     */
    public GenericRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

    /**
     * * <p>
     * Sets the one who pays for the request
     * The Third party should set request payer when requesting resources.
     * </p>
     * @param payer
     *            The one who pays for the request
     * */
    public void setRequestPayer(Payer payer) {
        this.payer = payer;
    }
    
    /**
     * * <p>
     * Gets the one who pays for the request
     * </p>
     * @return The one who pays for the request
     * */
    public Payer getRequestPayer() {
        return payer;
    }

    /**
     * * <p>
     * Sets the one who pays for the request
     * The Third party should set request payer when requesting resources.
     * </p>
     * @param payer
     *            The one who pays for the request
     * @return The updated request object.
     * */
    public GenericRequest withRequestPayer(Payer payer) {
        setRequestPayer(payer);
        return this;
    }

}
