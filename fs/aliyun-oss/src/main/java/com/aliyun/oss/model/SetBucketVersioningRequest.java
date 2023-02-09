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
 * <p>
 * Contains options for setting the versioning configuration for a bucket.
 * </p>
 * <p>
 * A bucket's versioning configuration can be in one of three possible states:
 * <ul>
 *  <li>{@link BucketVersioningConfiguration#OFF}
 *  <li>{@link BucketVersioningConfiguration#ENABLED}
 *  <li>{@link BucketVersioningConfiguration#SUSPENDED}
 * </ul>
 * <p>
 * By default, new buckets are created in the
 * {@link BucketVersioningConfiguration#OFF} state. Once versioning is
 * enabled for a bucket, its status can never be reverted to
 * {@link BucketVersioningConfiguration#OFF off}.
 * </p>
 * <p>
 * Objects created before versioning is enabled or while versioning is suspended
 * will be given the default <code>null</code> version ID (see
 * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID}). Note that the
 * <code>null</code> version ID is a valid version ID and is not the same
 * as having no version ID.
 * </p>
 * <p>
 * The versioning configuration of a bucket has different implications for each
 * operation performed on that bucket or for objects within that bucket. When 
 * versioning is enabled, a <code>PutObject</code> operation creates a unique
 * object version ID for the object being uploaded. The <code>PutObject</code> operation
 * guarantees that if versioning is enabled for a bucket at the time of the request, the
 * new object can only be permanently deleted by calling the <code>DeleteVersion</code> operation
 * and can never be overwritten.
 * </p>
 * <p>
 * Additionally, the <code>PutObject</code> operation guarantees that if
 * versioning is enabled for a bucket at the time of the request, no other object will be
 * overwritten by that request. Refer to the documentation sections for individual APIs
 * for information on how versioning status affects the semantics of that
 * particular API.
 * </p>
 * <p>
 * OSS is eventually consistent. It may take time for the versioning status of a
 * bucket to be propagated throughout the system.
 * </p>
 * 
 * @see SetBucketVersioningRequest#SetBucketVersioningRequest(String, BucketVersioningConfiguration)
 */
public class SetBucketVersioningRequest extends GenericRequest {

    /**
     * The new versioning configuration for the specified bucket.
     */
    private BucketVersioningConfiguration versioningConfiguration;
    
    /**
     * Constructs a new {@link SetBucketVersioningRequest}
     * to set the bucket versioning configuration of
     * the specified bucket.
     * 
     * @param bucketName
     *            The name of the bucket whose versioning configuration is being
     *            set.
     * @param configuration
     *            The new versioning configuration for the specified bucket.
     *            
     * @see SetBucketVersioningRequest#SetBucketVersioningRequest(String, BucketVersioningConfiguration)
     */
    public SetBucketVersioningRequest(String bucketName, BucketVersioningConfiguration configuration) {
        super(bucketName);
        this.versioningConfiguration = configuration;
    }

    /**
     * Gets the new versioning configuration for the specified bucket.
     * 
     * @return The new versioning configuration for the specified bucket.
     * 
     * @see SetBucketVersioningRequest#setVersioningConfiguration(BucketVersioningConfiguration)
     * @see SetBucketVersioningRequest#withVersioningConfiguration(BucketVersioningConfiguration)
     */
    public BucketVersioningConfiguration getVersioningConfiguration() {
        return versioningConfiguration;
    }

    /**
     * Sets the new versioning configuration for the specified bucket.
     * 
     * @param versioningConfiguration
     *            The new versioning configuration for the specified bucket.
     *            
     * @see SetBucketVersioningRequest#getVersioningConfiguration()
     * @see SetBucketVersioningRequest#withVersioningConfiguration(BucketVersioningConfiguration)
     */
    public void setVersioningConfiguration(
            BucketVersioningConfiguration versioningConfiguration) {
        this.versioningConfiguration = versioningConfiguration;
    }

    /**
     * Sets the new versioning configuration for the specified bucket and
     * returns this object, enabling additional method calls to be chained
     * together.
     * 
     * @param versioningConfiguration
     *            The new versioning configuration for the specified bucket.
     * 
     * @return This {@link SetBucketVersioningRequest} object, enabling that
     *         additional method calls may be chained together.
     *         
     * @see SetBucketVersioningRequest#getVersioningConfiguration()
     * @see SetBucketVersioningRequest#getVersioningConfiguration()
     */
    public SetBucketVersioningRequest withVersioningConfiguration(
            BucketVersioningConfiguration versioningConfiguration) {
        setVersioningConfiguration(versioningConfiguration);
        return this;
    }

}
