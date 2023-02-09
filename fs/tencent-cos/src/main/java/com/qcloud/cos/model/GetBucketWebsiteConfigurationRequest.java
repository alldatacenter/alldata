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
 * Request object for the parameters to retrieve a bucket's website
 * configuration.
 */
public class GetBucketWebsiteConfigurationRequest extends CosServiceRequest implements Serializable {

    /** The name of the bucket whose website configuration is being retrieved. */
    private String bucketName;


    /**
     * Creates a new request object, ready to be executed to retrieve the bucket
     * website configuration for the specified bucket.
     *
     * @param bucketName
     *            The name of the bucket whose website configuration is being
     *            retrieved.
     */
    public GetBucketWebsiteConfigurationRequest(String bucketName) {
        this.bucketName = bucketName;
    }
    /**
     * Sets the name of the bucket whose website configuration is being
     * retrieved.
     *
     * @param bucketName
     *            The name of the bucket whose website configuration is being
     *            retrieved.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Returns the name of the bucket whose website configuration is being
     * retrieved.
     *
     * @return The name of the bucket whose website configuration is being
     *         retrieved.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket whose website configuration is being
     * retrieved, and returns this updated request object so that additional
     * method calls can be chained together.
     *
     * @param bucketName
     *            The name of the bucket whose website configuration is being
     *            retrieved.
     *
     * @return This updated request object, so that additional method calls can
     *         be chained together.
     */
    public GetBucketWebsiteConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }
}
