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

public class SetBucketResourceGroupRequest extends GenericRequest{

    // The id of resource group
    private String resourceGroupId;

    public SetBucketResourceGroupRequest(String bucketName) {
        super(bucketName);
    }

    public SetBucketResourceGroupRequest(String bucketName, String resourceGroupId) {
        super(bucketName);
        this.resourceGroupId = resourceGroupId;
    }

	/**
     * Set the id of resource group.
     *
     * @param resourceGroupId
     *            id of resource group.
    */
    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

	/**
     * Get the id of resource group.
     *
     * @return  resource group id.
    */
    public String getResourceGroupId() {
        return resourceGroupId;
    }

}
