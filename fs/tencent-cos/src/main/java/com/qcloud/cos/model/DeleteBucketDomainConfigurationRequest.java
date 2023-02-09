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
 * Request object for the parameters to delete a bucket's domain configuration.
 */
public class DeleteBucketDomainConfigurationRequest extends CosServiceRequest implements Serializable {

    /** The name of the bucket whose domain configuration is being deleted. */
    private String bucketName;

    /**
     * Creates a new request object, ready to be executed to delete the specified
     * bucket's domain configuration.
     *
     * @param bucketName
     *            The name of the bucket whose domain configuration is being deleted.
     */
    public DeleteBucketDomainConfigurationRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket whose domain configuration is to be deleted.
     *
     * @param bucketName
     *            The name of the bucket whose domain configuration is being deleted.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Returns the name of the bucket whose domain configuration is being deleted.
     *
     * @return The name of the bucket whose domain configuration is being deleted.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket whose domain configuration is being deleted and
     * returns this deleted request object so that additional method calls can
     * be chained together.
     *
     * @param bucketName
     *            The name of the bucket whose domain configuration is being
     *            deleted.
     * @return This updated request object so that additional method calls can
     *         be chained together.
     */
    public DeleteBucketDomainConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }
}
