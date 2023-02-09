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
 * Request object to set an inventory configuration to a bucket.
 */
public class SetBucketInventoryConfigurationRequest extends GenericRequest implements Serializable {
    private InventoryConfiguration inventoryConfiguration;

    public SetBucketInventoryConfigurationRequest(String bucketName, InventoryConfiguration inventoryConfiguration) {
        super(bucketName);
        this.inventoryConfiguration = inventoryConfiguration;
    }

    /**
     * Gets the inventory configuration.
     * @return The {@link InventoryConfiguration} instance.
     */
    public InventoryConfiguration getInventoryConfiguration() {
        return inventoryConfiguration;
    }

    /**
     * Sets the inventory configuration.
     *
     * @param inventoryConfiguration
     *            The {@link InventoryConfiguration} instance.
     */
    public void setInventoryConfiguration(InventoryConfiguration inventoryConfiguration) {
        this.inventoryConfiguration = inventoryConfiguration;
    }
}
