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


package com.qcloud.cos.model;

import java.io.Serializable;


/**
 * CAS related prameters pertaining to a job.
 */
public class CASJobParameters implements Serializable {

    private String tier;

    /**
     * @return CAS retrieval tier at which the restore will be processed.
     */
    public String getTier() {
        return tier;
    }

    /**
     * Sets CAS retrieval tier at which the restore will be processed.
     *
     * @param tier New tier value
     */
    public void setTier(String tier) {
        this.tier = tier;
    }

    /**
     * Sets CAS retrieval tier at which the restore will be processed.
     *
     * @param tier New tier enum value.
     */
    public void setTier(Tier tier) {
        setTier(tier == null ? null : tier.toString());
    }

    /**
     * Sets CAS retrieval tier at which the restore will be processed.
     *
     * @param tier New tier value.
     * @return This object for method chaining.
     */
    public CASJobParameters withTier(String tier) {
        setTier(tier);
        return this;
    }

    /**
     * Sets CAS retrieval tier at which the restore will be processed.
     *
     * @param tier New tier enum value.
     * @return This object for method chaining.
     */
    public CASJobParameters withTier(Tier tier) {
        setTier(tier);
        return this;
    }
}