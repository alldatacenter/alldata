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
import com.qcloud.cos.model.inventory.InventoryConfiguration;

import java.io.Serializable;

/**
 * Request object to set an inventory configuration to a bucket.
 */
public class SetBucketInventoryConfigurationRequest extends CosServiceRequest implements Serializable {

    private String bucketName;

    private InventoryConfiguration inventoryConfiguration;

    public SetBucketInventoryConfigurationRequest() {
    }

    public SetBucketInventoryConfigurationRequest(String bucketName, InventoryConfiguration inventoryConfiguration) {
        this.bucketName = bucketName;
        this.inventoryConfiguration = inventoryConfiguration;
    }

    /**
     * Returns the name of the bucket where the inventory configuration will be stored.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket where the inventory configuration will be stored.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket where the inventory configuration will be stored
     * and returns {@link SetBucketInventoryConfigurationRequest} object for
     * method chaining.
     */
    public SetBucketInventoryConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Returns the inventory configuration.
     */
    public InventoryConfiguration getInventoryConfiguration() {
        return inventoryConfiguration;
    }

    /**
     * Sets the inventory configuration.
     */
    public void setInventoryConfiguration(InventoryConfiguration inventoryConfiguration) {
        this.inventoryConfiguration = inventoryConfiguration;
    }

    /**
     * Sets the inventory configuration and returns the
     * {@link SetBucketInventoryConfigurationRequest} object
     * for method chaining.
     */
    public SetBucketInventoryConfigurationRequest withInventoryConfiguration(InventoryConfiguration inventoryConfiguration) {
        setInventoryConfiguration(inventoryConfiguration);
        return this;
    }
}
