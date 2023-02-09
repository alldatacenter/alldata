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

public class UserQos extends GenericResult {

    public UserQos() {
    }

    public UserQos(int storageCapacity) {
        this.storageCapacity = storageCapacity;
    }

    /**
     * Gets the bucket capacity in GB.
     * 
     * @return Bucket capacity
     */
    public int getStorageCapacity() {
        return storageCapacity;
    }

    /**
     * Sets the bucket capacity in GB. -1 means unlimited capacity. Other
     * negative number is not allowed.
     * 
     * @param storageCapacity
     *            Storage capacity in GB.
     */
    public void setStorageCapacity(int storageCapacity) {
        this.storageCapacity = storageCapacity;
    }

    /**
     * Checks if the storage capacity is set.
     * 
     * @return True:the storage capacity is set.
     */
    public boolean hasStorageCapacity() {
        return storageCapacity != null;
    }

    private Integer storageCapacity;

}
