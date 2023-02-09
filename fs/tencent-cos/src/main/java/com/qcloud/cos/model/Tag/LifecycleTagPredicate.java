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


package com.qcloud.cos.model.Tag;

import com.qcloud.cos.model.lifecycle.LifecycleFilterPredicate;
import com.qcloud.cos.model.lifecycle.LifecyclePredicateVisitor;

/**
 * A {@link LifecycleFilterPredicate} class to represent the {@link Tag} object
 * that must exist in the object's tag set in order for the
 * {@link com.qcloud.cos.model.BucketLifecycleConfiguration.Rule} to apply.
 */
public final class LifecycleTagPredicate extends LifecycleFilterPredicate {

    private final Tag tag;

    public LifecycleTagPredicate(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public void accept(LifecyclePredicateVisitor lifecyclePredicateVisitor) {
        lifecyclePredicateVisitor.visit(this);
    }
}