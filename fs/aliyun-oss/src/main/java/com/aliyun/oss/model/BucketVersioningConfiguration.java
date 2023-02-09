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

import java.io.Serializable;

/**
 * Represents the versioning configuration for a bucket.
 * <p>
 * A bucket's versioning configuration can be in one of three possible states:
 *  <ul>
 *      <li>{@link BucketVersioningConfiguration#OFF}
 *      <li>{@link BucketVersioningConfiguration#ENABLED}
 *      <li>{@link BucketVersioningConfiguration#SUSPENDED}
 *  </ul>
 * <p>
 * By default, new buckets are in the
 * {@link BucketVersioningConfiguration#OFF off} state. Once versioning is
 * enabled for a bucket the status can never be reverted to
 * {@link BucketVersioningConfiguration#OFF off}.
 * </p>
 * <p>
 * The versioning configuration of a bucket has different implications for each
 * operation performed on that bucket or for objects within that bucket. For
 * instance, when versioning is enabled, a PutObject operation creates a unique
 * object version-id for the object being uploaded. The PutObject API guarantees
 * that, if versioning is enabled for a bucket at the time of the request, the
 * new object can only be permanently deleted using the DeleteVersion operation.
 * It can never be overwritten. Additionally, PutObject guarantees that, if
 * versioning is enabled for a bucket the request, no other object will be
 * overwritten by that request. Refer to the documentation sections for each API
 * for information on how versioning status affects the semantics of that
 * particular API.
 * <p>
 * OSS is eventually consistent. It may take time for the versioning status of a
 * bucket to be propagated throughout the system.
 * 
 * @see com.aliyun.oss.OSS#getBucketVersioning(String)
 * @see com.aliyun.oss.OSS#setBucketVersioning(SetBucketVersioningRequest)
 */
public class BucketVersioningConfiguration implements Serializable {

	private static final long serialVersionUID = -5015082031534990114L;

	/**
     * OSS bucket versioning status indicating that versioning is off for a
     * bucket. By default, all buckets start off with versioning off. Once you
     * enable versioning for a bucket, you can never set the status back to
     * "Off". You can only suspend versioning on a bucket once you've enabled.
     */
    public static final String OFF = "Off";

    /**
     * OSS bucket versioning status indicating that versioning is suspended for a
     * bucket. Use the "Suspended" status when you want to disable versioning on
     * a bucket that has versioning enabled.
     */
    public static final String SUSPENDED = "Suspended";

    /**
     * OSS bucket versioning status indicating that versioning is enabled for a
     * bucket.
     */
    public static final String ENABLED = "Enabled";
   
    
    /** The current status of versioning */
    private String status;
  
    /**
     * Creates a new bucket versioning configuration object which defaults to
     * {@link #OFF} status.
     */
    public BucketVersioningConfiguration() {
        setStatus(OFF);
    }

    /**
     * Creates a new bucket versioning configuration object with the specified
     * status.
     * <p>
     * Note that once versioning has been enabled for a bucket, its status can
     * only be {@link #SUSPENDED suspended} and can never be set back to
     * {@link #OFF off}.
     * 
     * @param status
     *            The desired bucket versioning status for the new configuration
     *            object.
     * 
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public BucketVersioningConfiguration(String status) {
        setStatus(status);
    }

    /**
     * Returns the current status of versioning for this bucket versioning
     * configuration object, indicating if versioning is enabled or not for a
     * bucket.
     * 
     * @return The current status of versioning for this bucket versioning
     *         configuration.
     * 
     * @see #OFF
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the desired status of versioning for this bucket versioning
     * configuration object.
     * <p>
     * Note that once versioning has been enabled for a bucket, its status can
     * only be {@link #SUSPENDED suspended} and can never be set back to
     * {@link #OFF off}.
     * 
     * @param status
     *            The desired status of versioning for this bucket versioning
     *            configuration.
     * 
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Sets the desired status of versioning for this bucket versioning
     * configuration object, and returns this object so that additional method
     * calls may be chained together.
     * <p>
     * Note that once versioning has been enabled for a bucket, its status can
     * only be {@link #SUSPENDED suspended} and can never be set back to
     * {@link #OFF off}.
     * 
     * @param status
     *            The desired status of versioning for this bucket versioning
     *            configuration.
     * 
     * @return The updated OSS BucketVersioningConfiguration object so that
     *         additional method calls may be chained together.
     * 
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public BucketVersioningConfiguration withStatus(String status) {
        setStatus(status);
        return this;
    }
    
}
