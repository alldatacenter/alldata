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


import com.qcloud.cos.internal.CosServiceRequest;

import java.io.Serializable;

/**
 * Request object to retrieve an inventory configuration.
 */
public class GetBucketInventoryConfigurationRequest extends CosServiceRequest implements Serializable {

    private String bucketName;

    private String id;

    public GetBucketInventoryConfigurationRequest() {
    }

    public GetBucketInventoryConfigurationRequest(String bucketName, String id) {
        this.bucketName = bucketName;
        this.id = id;
    }

    /**
     * Returns the name of the bucket containing the inventory configuration to retrieve.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket containing the inventory configuration to retrieve.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket containing the inventory configuration to retrieve
     * and returns {@link GetBucketInventoryConfigurationRequest} object for
     * method chaining.
     */
    public GetBucketInventoryConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Returns the ID used to identify the inventory configuration.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID used to identify the inventory configuration.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the ID used to identify the inventory configuration and
     * returns {@link GetBucketInventoryConfigurationRequest} object
     * for method chaining.
     */
    public GetBucketInventoryConfigurationRequest withId(String id) {
        setId(id);
        return this;
    }
}
