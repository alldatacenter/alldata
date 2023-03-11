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
 * Specifies an inventory filter.
 * The inventory only includes objects that meet the filter's criteria.
 */
public class InventoryFilter implements Serializable {

    private InventoryFilterPredicate predicate;

    public InventoryFilter() { }

    public InventoryFilter(InventoryFilterPredicate predicate) {
        this.predicate = predicate;
    }

    /**
     * Returns the {@link InventoryFilterPredicate} to be used when evaluating an inventory filter.
     *
     * The predicate should be of type {@link InventoryPrefixPredicate}.
     */
    public InventoryFilterPredicate getPredicate() {
        return predicate;
    }

    /**
     * Sets the {@link InventoryFilterPredicate} to be used when evaluating an inventory filter.
     *
     * The predicate should be of type {@link InventoryPrefixPredicate}.
     */
    public void setPredicate(InventoryFilterPredicate predicate) {
        this.predicate = predicate;
    }

    /**
     * Sets the {@link InventoryFilterPredicate} to be used when evaluating an inventory filter
     * and returns the {@link InventoryFilter} object for method chaining.
     *
     * The predicate should be of type {@link InventoryPrefixPredicate}.
     */
    public InventoryFilter withPredicate(InventoryFilterPredicate predicate) {
        setPredicate(predicate);
        return this;
    }
}
