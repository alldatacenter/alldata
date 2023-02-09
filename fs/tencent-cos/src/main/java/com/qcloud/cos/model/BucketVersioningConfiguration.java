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

public class BucketVersioningConfiguration implements Serializable {

    /**
     * bucket versioning status indicating that versioning is off for a
     * bucket. By default, all buckets start off with versioning off. Once you
     * enable versioning for a bucket, you can never set the status back to
     * "Off". You can only suspend versioning on a bucket once you've enabled.
     */
    public static final String OFF = "Off";

    /**
     * bucket versioning status indicating that versioning is suspended for a
     * bucket. Use the "Suspended" status when you want to disable versioning on
     * a bucket that has versioning enabled.
     */
    public static final String SUSPENDED = "Suspended";

    /**
     * bucket versioning status indicating that versioning is enabled for a
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
     * @return The updated BucketVersioningConfiguration object so that
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