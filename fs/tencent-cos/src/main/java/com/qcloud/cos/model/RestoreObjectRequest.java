/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.model;

import java.io.Serializable;

import com.qcloud.cos.internal.CosServiceRequest;

public class RestoreObjectRequest extends CosServiceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The time, in days, between when an object is restored to the bucket and when it expires.
     */
    private int expirationInDays;

    /**
     * The name of the bucket containing the reference to the object to restore which is now stored
     * in CAS.
     */
    private String bucketName;

    /**
     * The key, the name of the reference to the object to restore, which is now stored in CAS.
     */
    private String key;
    /**
     * Optional version ID specifying which version of the object to restore. If not specified, the
     * most recent version will be restored.
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link COS#setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}. For
     * more information about enabling lifecycle versioning for a bucket, see
     * {@link COS#setBucketLifecycleConfiguration(SetBucketLifecycleConfigurationRequest)}.
     */
    private String versionId;

    /**
     * CAS Job Parameters
     */
    private CASJobParameters casJobParameters;

    /**
     * <p>
     * Constructs a new RestoreObjectRequest.
     * </p>
     *
     * @param bucketName The name of the bucket containing the reference to the object to restore
     *        which is now stored in CAS.
     * @param key The key, the name of the reference to the object to restore, which is now stored
     *        in CAS.
     *
     * @see RestoreObjectRequest#RestoreObjectRequest(String, String, int)
     */
    public RestoreObjectRequest(String bucketName, String key) {
        this(bucketName, key, -1);
    }

    /**
     * <p>
     * Constructs a new RestoreObjectRequest.
     * </p>
     *
     * @param bucketName The name of the bucket containing the reference to the object to restore
     *        which is now stored in CAS.
     * @param key The key, the name of the reference to the object to restore, which is now stored
     *        in CAS.
     * @param expirationInDays The time, in days, between when an object is restored to the bucket
     *        and when it expires
     *
     * @see RestoreObjectRequest#RestoreObjectRequest(String, String)
     */
    public RestoreObjectRequest(String bucketName, String key, int expirationInDays) {
        this.bucketName = bucketName;
        this.key = key;
        this.expirationInDays = expirationInDays;
    }

    /**
     * Returns the name of the bucket containing the reference to the object to restore which is now
     * stored in CAS.
     *
     * @see RestoreObjectRequest#setBucketName(String)
     * @see RestoreObjectRequest#withBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket containing the reference to the object to restore which is now
     * stored in CAS, and returns a reference to this object(RestoreObjectRequest) for method
     * chaining.
     *
     * @see RestoreObjectRequest#setBucketName(String)
     * @see RestoreObjectRequest#getBucketName()
     */
    public RestoreObjectRequest withBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    /**
     * Sets the name of the bucket containing the reference to the object to restore which is now
     * stored in CAS.
     *
     * @see RestoreObjectRequest#getBucketName()
     * @see RestoreObjectRequest#withBucketName(String)
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the key, the name of the reference to the object to restore, which is now stored in CAS.
     *
     * @see RestoreObjectRequest#setKey(String)
     * @see RestoreObjectRequest#withKey(String)
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key, the name of the reference to the object to restore, which is now stored in CAS.
     *
     * @see RestoreObjectRequest#getKey()
     * @see RestoreObjectRequest#withKey(String)
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the key, the name of the reference to the object to restore, which is now stored in CAS.
     * returns a reference to this object(RestoreObjectRequest) for method chaining.
     *
     * @see RestoreObjectRequest#getKey()
     * @see RestoreObjectRequest#setKey(String)
     */
    public RestoreObjectRequest withKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Returns the id of the version to be restored.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the id of the version to be restored.
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Sets the id of the version to be restored and returns a reference to this object for method
     * chaining.
     */
    public RestoreObjectRequest withVersionId(String versionId) {
        this.versionId = versionId;
        return this;
    }

    /**
     * Sets the time, in days, between when an object is uploaded to the bucket and when it expires.
     */
    public void setExpirationInDays(int expirationInDays) {
        this.expirationInDays = expirationInDays;
    }

    /**
     * Returns the time in days from an object's creation to its expiration.
     */
    public int getExpirationInDays() {
        return expirationInDays;
    }

    /**
     * Sets the time, in days, between when an object is uploaded to the bucket and when it expires,
     * and returns a reference to this object(RestoreObjectRequest) for method chaining.
     */
    public RestoreObjectRequest withExpirationInDays(int expirationInDays) {
        this.expirationInDays = expirationInDays;
        return this;
    }


    /**
     * @return CAS related prameters pertaining to this job.
     */
    public CASJobParameters getCasJobParameters() {
        return casJobParameters;
    }

    /**
     * Sets CAS related prameters pertaining to this job.
     * 
     * @param casJobParameters New value for CAS job parameters.
     */
    public void setCASJobParameters(CASJobParameters casJobParameters) {
        this.casJobParameters = casJobParameters;
    }

    /**
     * Sets CAS related prameters pertaining to this job.
     *
     * @param casJobParameters New value for CAS job parameters.
     * @return This object for method chaining.
     */
    public RestoreObjectRequest withCASJobParameters(CASJobParameters casJobParameters) {
        setCASJobParameters(casJobParameters);
        return this;
    }
}
