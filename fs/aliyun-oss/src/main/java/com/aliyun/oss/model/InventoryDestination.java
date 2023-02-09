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
 * Information about where to publish the inventory results.
 */
public class InventoryDestination implements Serializable {
    private static final long serialVersionUID = 3910115592538153029L;

    /**
     * Contains the OSS destination information of where inventory results are published.
     */
    private InventoryOSSBucketDestination ossBucketDestination;

    /**
     * Gets the {@link InventoryOSSBucketDestination} which contains OSS bucket destination information
     * of where inventory results are published.
     *
     * @return  The {@link InventoryOSSBucketDestination} instance.
     */
    public InventoryOSSBucketDestination getOssBucketDestination() {
        return ossBucketDestination;
    }

    /**
     * Sets the {@link InventoryOSSBucketDestination} which contains OSS bucket destination information
     * of where inventory results are published.
     *
     * @param ossBucketDestination
     *            The {@link InventoryOSSBucketDestination} instance.
     */
    public void setOssBucketDestination(InventoryOSSBucketDestination ossBucketDestination) {
        this.ossBucketDestination = ossBucketDestination;
    }

    /**
     * Sets the {@link InventoryOSSBucketDestination} which contains OSS bucket destination information
     * of where inventory results are published.
     * And returns the {@link InventoryDestination} object itself.
     *
     * @param ossBucketDestination
     *            The {@link InventoryOSSBucketDestination} instance.
     *
     * @return  The {@link InventoryDestination} instance.
     */
    public InventoryDestination withOSSBucketDestination(InventoryOSSBucketDestination ossBucketDestination) {
        setOssBucketDestination(ossBucketDestination);
        return this;
    }
}
