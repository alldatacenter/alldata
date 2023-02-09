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

public class GetBucketInventoryConfigurationRequest extends GenericRequest {
    private String inventoryId;

    public GetBucketInventoryConfigurationRequest(String bucketName, String inventoryId) {
        super(bucketName);
        this.inventoryId = inventoryId;
    }

    /**
     * Gets the inventory id used to identify the inventory configuration.
     *  @return the inventory id.
     */
    public String getInventoryId() {
        return inventoryId;
    }

    /**
     * Sets the inventory id used to identify the inventory configuration.
     *
     * @param inventoryId
     *           the inventory id.
     */
    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }
}
