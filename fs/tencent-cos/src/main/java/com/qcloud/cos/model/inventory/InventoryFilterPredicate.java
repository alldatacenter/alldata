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
 * Base class to represent the root predicate in {@link InventoryFilter} class.
 *
 * @see InventoryPrefixPredicate
 */
public abstract class InventoryFilterPredicate implements Serializable {

    /**
     * Helper method that accepts an implemenation of {@link InventoryPredicateVisitor}
     * and invokes the most applicable visit method in the visitor.
     */
    public abstract void accept(InventoryPredicateVisitor inventoryPredicateVisitor);
}
