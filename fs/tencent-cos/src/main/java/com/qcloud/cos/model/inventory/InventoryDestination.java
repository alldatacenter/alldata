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

package com.qcloud.cos.model.inventory;

import java.io.Serializable;

/**
 * Information about where to publish the inventory results.
 */
public class InventoryDestination implements Serializable {

    /**
     * Contains the COS destination information of where inventory results are published.
     */
    private InventoryCosBucketDestination cosBucketDestination;

    /**
     * Returns the {@link InventoryCosBucketDestination} which contains COS bucket destination information
     * of where inventory results are published.
     */
    public InventoryCosBucketDestination getCosBucketDestination() {
        return cosBucketDestination;
    }

    /**
     * Sets the {@link InventoryCosBucketDestination} which contains COS bucket destination information
     * of where inventory results are published.
     */
    public void setCosBucketDestination(InventoryCosBucketDestination cosBucketDestination) {
        this.cosBucketDestination = cosBucketDestination;
    }

    /**
     * Sets the {@link InventoryCosBucketDestination} which contains COS bucket destination information
     * of where inventory results are published.
     * This {@link InventoryDestination} object is returned for method chaining.
     */
    public InventoryDestination withCosBucketDestination(InventoryCosBucketDestination cosBucketDestination) {
        setCosBucketDestination(cosBucketDestination);
        return this;
    }
}
