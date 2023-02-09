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

/*
 * Contains options for setting the lifecycle configuration for a bucket.
 */

public class SetBucketLifecycleConfigurationRequest extends CosServiceRequest
        implements Serializable {
    /**
     * The bucket whose lifecycle configuration is being set.
     */
    private String bucketName;

    /**
     * The new lifecycle configuration for the specified bucket.
     */
    private BucketLifecycleConfiguration lifecycleConfiguration;

    public SetBucketLifecycleConfigurationRequest(String bucketName,
            BucketLifecycleConfiguration lifecycleConfiguration) {
        super();
        this.bucketName = bucketName;
        this.lifecycleConfiguration = lifecycleConfiguration;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public SetBucketLifecycleConfigurationRequest withBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public BucketLifecycleConfiguration getLifecycleConfiguration() {
        return lifecycleConfiguration;
    }

    public void setLifecycleConfiguration(BucketLifecycleConfiguration lifecycleConfiguration) {
        this.lifecycleConfiguration = lifecycleConfiguration;
    }

    public SetBucketLifecycleConfigurationRequest withLifecycleConfiguration(
            BucketLifecycleConfiguration lifecycleConfiguration) {
        this.lifecycleConfiguration = lifecycleConfiguration;
        return this;
    }

}
