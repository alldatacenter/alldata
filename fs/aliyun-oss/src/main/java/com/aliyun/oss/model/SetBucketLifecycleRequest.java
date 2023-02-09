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

import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.model.LifecycleRule.StorageTransition;

public class SetBucketLifecycleRequest extends GenericRequest {

    public static final int MAX_LIFECYCLE_RULE_LIMIT = 1000;
    public static final int MAX_RULE_ID_LENGTH = 255;

    private List<LifecycleRule> lifecycleRules = new ArrayList<LifecycleRule>();

    public SetBucketLifecycleRequest(String bucketName) {
        super(bucketName);
    }

    public List<LifecycleRule> getLifecycleRules() {
        return lifecycleRules;
    }

    public void setLifecycleRules(List<LifecycleRule> lifecycleRules) {
        if (lifecycleRules == null || lifecycleRules.isEmpty()) {
            throw new IllegalArgumentException("lifecycleRules should not be null or empty.");
        }

        if (lifecycleRules.size() > MAX_LIFECYCLE_RULE_LIMIT) {
            throw new IllegalArgumentException("One bucket not allow exceed one thousand items of LifecycleRules.");
        }

        this.lifecycleRules.clear();
        this.lifecycleRules.addAll(lifecycleRules);
    }

    public void clearLifecycles() {
        this.lifecycleRules.clear();
    }

    public void AddLifecycleRule(LifecycleRule lifecycleRule) {
        if (lifecycleRule == null) {
            throw new IllegalArgumentException("lifecycleRule should not be null or empty.");
        }

        if (this.lifecycleRules.size() >= MAX_LIFECYCLE_RULE_LIMIT) {
            throw new IllegalArgumentException("One bucket not allow exceed one thousand items of LifecycleRules.");
        }

        if (lifecycleRule.getId() != null && lifecycleRule.getId().length() > MAX_RULE_ID_LENGTH) {
            throw new IllegalArgumentException("Length of lifecycle rule id exceeds max limit " + MAX_RULE_ID_LENGTH);
        }

        int expirationTimeFlag = lifecycleRule.hasExpirationTime() ? 1 : 0;
        int expirationDaysFlag = lifecycleRule.hasExpirationDays() ? 1 : 0;
        int createdBeforeDateFlag = lifecycleRule.hasCreatedBeforeDate() ? 1 : 0;
        int expiredDeleteMarkerFlag = lifecycleRule.hasExpiredDeleteMarker() ? 1: 0;
        int flagSum = expirationTimeFlag + expirationDaysFlag + createdBeforeDateFlag + expiredDeleteMarkerFlag;
        if (flagSum > 1 ) {
            throw new IllegalArgumentException("Only one expiration property should be specified.");
        }

        if (flagSum == 0 && !lifecycleRule.hasAbortMultipartUpload()
                && !lifecycleRule.hasStorageTransition() && !lifecycleRule.hasNoncurrentVersionStorageTransitions()
                && !lifecycleRule.hasNoncurrentVersionExpiration()) {
            throw new IllegalArgumentException("Rule has none expiration or transition specified.");
        }

        if (lifecycleRule.getStatus() == LifecycleRule.RuleStatus.Unknown) {
            throw new IllegalArgumentException("RuleStatus property should be specified with 'Enabled' or 'Disabled'.");
        }

        if (lifecycleRule.hasAbortMultipartUpload()) {
            LifecycleRule.AbortMultipartUpload abortMultipartUpload = lifecycleRule.getAbortMultipartUpload();
            expirationDaysFlag = abortMultipartUpload.hasExpirationDays() ? 1 : 0;
            createdBeforeDateFlag = abortMultipartUpload.hasCreatedBeforeDate() ? 1 : 0;
            flagSum = expirationDaysFlag + createdBeforeDateFlag;
            if (flagSum != 1) {
                throw new IllegalArgumentException(
                        "Only one expiration property for AbortMultipartUpload should be specified.");
            }
        }

        if (lifecycleRule.hasStorageTransition()) {
            for (StorageTransition storageTransition : lifecycleRule.getStorageTransition()) {
                expirationDaysFlag = storageTransition.hasExpirationDays() ? 1 : 0;
                createdBeforeDateFlag = storageTransition.hasCreatedBeforeDate() ? 1 : 0;
                flagSum = expirationDaysFlag + createdBeforeDateFlag;
                if (flagSum != 1) {
                    throw new IllegalArgumentException(
                            "Only one expiration property for StorageTransition should be specified.");
                }
            }
        }

        if (lifecycleRule.getFilter() != null) {
            for(LifecycleNot lifecycleNot : lifecycleRule.getFilter().getNotList()){
                if(lifecycleNot == null){
                    throw new IllegalArgumentException("Not node cannot be empty.");
                }
                if(lifecycleNot.getPrefix() == null){
                    throw new IllegalArgumentException("The prefix node under the not node cannot be empty.");
                }
                if(lifecycleNot.getTag() == null && lifecycleNot.getPrefix().equals(lifecycleRule.getPrefix())){
                    throw new IllegalArgumentException("If there is no tag node under the not node, the prefix of the not node cannot be the same as that of the rule.");
                }
                if(!lifecycleNot.getPrefix().contains(lifecycleRule.getPrefix())){
                    lifecycleNot.setPrefix(lifecycleRule.getPrefix() + lifecycleNot.getPrefix());
                }
            }
        }
        this.lifecycleRules.add(lifecycleRule);
    }
}
