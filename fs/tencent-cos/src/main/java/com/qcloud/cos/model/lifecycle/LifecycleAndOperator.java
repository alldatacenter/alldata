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


package com.qcloud.cos.model.lifecycle;

import java.util.List;

/**
 * A logical AND of two or more predicates of type {@link LifecycleFilterPredicate}.
 * The Lifecycle Rule will apply to any object matching all of the predicates configured inside the And operator.
 *
 * The {@link LifecycleAndOperator} can contain at most one {@link LifecyclePrefixPredicate} and any number of {@link LifecycleTagPredicate}s.
 */
public final class LifecycleAndOperator extends LifecycleNAryOperator {

    public LifecycleAndOperator(List<LifecycleFilterPredicate> operands) {
        super(operands);
    }

    @Override
    public void accept(LifecyclePredicateVisitor lifecyclePredicateVisitor) {
        lifecyclePredicateVisitor.visit(this);
    }
}