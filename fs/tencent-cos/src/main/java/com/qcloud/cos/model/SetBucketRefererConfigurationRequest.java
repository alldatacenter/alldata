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

/**
 * Request object for the parameters to set a bucket's referer configuration.
 */
public class SetBucketRefererConfigurationRequest extends CosServiceRequest implements Serializable {

    /** The name of the bucket whose referer configuration is being set. */
    private String bucketName;

    /** The referer configuration for the specified bucket. */
    private BucketRefererConfiguration configuration;

    /**
     * Creates a new request object, ready to be executed to set the specified
     * bucket's referer configuration.
     *
     * @param bucketName
     *            The name of the bucket whose referer configuration is being set.
     * @param configuration
     *            The new referer configuration for the specified bucket.
     */
    public SetBucketRefererConfigurationRequest(String bucketName, BucketRefererConfiguration configuration) {
        this.bucketName = bucketName;
        this.configuration = configuration;
    }

    /**
     * Sets the name of the bucket whose referer configuration is to be updated.
     *
     * @param bucketName
     *            The name of the bucket whose referer configuration is being set.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Returns the name of the bucket whose referer configuration is being set.
     *
     * @return The name of the bucket whose referer configuration is being set.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket whose referer configuration is being set and
     * returns this updated request object so that additional method calls can
     * be chained together.
     *
     * @param bucketName
     *            The name of the bucket whose referer configuration is being
     *            set.
     * @return This updated request object so that additional method calls can
     *         be chained together.
     */
    public SetBucketRefererConfigurationRequest withBucketName(String bucektName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Sets the referer configuration to send as part of this request.
     *
     * @param configuration
     *            The referer configuration to set for the specified bucket.
     */
    public void setConfiguration(BucketRefererConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the referer configuration to send as part of this request.
     *
     * @return The referer configuration to set for the specified bucket.
     */
    public BucketRefererConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Sets the referer configuration to send as part of this request, and
     * returns this updated request object so that additional method calls can
     * be chained together.
     *
     * @param configuration
     *            The referer configuration to set for the specified bucket.
     *
     * @return This updated request object so that additional method calls can
     *         be chained together.
     */
    public SetBucketRefererConfigurationRequest withConfiguration(BucketRefererConfiguration configuration) {
        setConfiguration(configuration);
        return this;
    }
}
