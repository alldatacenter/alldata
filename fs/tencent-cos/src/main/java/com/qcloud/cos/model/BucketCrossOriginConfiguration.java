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
import java.util.Arrays;
import java.util.List;

public class BucketCrossOriginConfiguration implements Serializable {

    
    private List<CORSRule> rules;

    /**
     * Returns the list of rules that comprise this configuration.
     */
    public List<CORSRule> getRules() {
        return rules;
    }

    /**
     * Sets the rules that comprise this configuration.
     */
    public void setRules(List<CORSRule> rules) {
        this.rules = rules;
    }

    /**
     * Sets the rules that comprise this configuration and returns a reference
     * to this object for easy method chaining.
     */
    public BucketCrossOriginConfiguration withRules(List<CORSRule> rules) {
        setRules(rules);
        return this;
    }

    /**
     * Convenience array style method for
     * {@link BucketCrossOriginConfiguration#withRules(List)}
     */
    public BucketCrossOriginConfiguration withRules(CORSRule... rules) {
        setRules(Arrays.asList(rules));
        return this;
    }

    /**
     * Constructs a new {@link BucketCrossOriginConfiguration} object with the
     * rules given.
     * 
     * @param rules
     */
    public BucketCrossOriginConfiguration(List<CORSRule> rules) {
        this.rules = rules;
    }

    public BucketCrossOriginConfiguration() {
        super();
    }
    
}