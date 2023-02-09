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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class COSKeyFilter implements Serializable {

    /**
     * Allowable values for the name of a {@link FilterRule} for an {@link COSKeyFilter}
     */
    public enum FilterRuleName {
        Prefix;

        /**
         * Convenience factory method to create a new {@link FilterRule} with name initialized to
         * this {@link FilterRuleName}
         * 
         * <pre>
         * FilterRule prefixRule = FilterRuleName.Prefix.newRule().withValue(&quot;prefix-value&quot;);
         * </pre>
         * 
         * @return New {@link FilterRule} with name initialized to this {@link FilterRuleName}
         */
        public FilterRule newRule() {
            return new FilterRule().withName(this.toString());
        }

        /**
         * Convenience factory method to create a new {@link FilterRule} with name initialized to
         * this {@link FilterRuleName} and value initialized to {@code value}
         * 
         * <pre>
         * FilterRule prefixRule = FilterRuleName.Prefix.newRule(&quot;prefix-value&quot;);
         * </pre>
         * 
         * @return New {@link FilterRule} with name initialized to this {@link FilterRuleName}
         */
        public FilterRule newRule(String value) {
            return newRule().withValue(value);
        }
    }

    private List<FilterRule> filterRules;

    public COSKeyFilter() {
        filterRules = new ArrayList<FilterRule>();
    }

    /**
     * @return The list of {@link FilterRule}s for this {@link COSKeyFilter}
     */
    public List<FilterRule> getFilterRules() {
        return Collections.unmodifiableList(filterRules);
    }

    /**
     * Set the list of {@link FilterRule}s for this {@link COSKeyFilter}
     * 
     * @param filterRules New list of {@link FilterRule}s
     */
    public void setFilterRules(List<FilterRule> filterRules) {
        this.filterRules = new ArrayList<FilterRule>(filterRules);
    }

    /**
     * Set the list of {@link FilterRule}s for this {@link COSKeyFilter} and returns this object for
     * method chaining
     * 
     * @param filterRules New List of {@link FilterRule}s
     * @return This object for method chaining
     */
    public COSKeyFilter withFilterRules(List<FilterRule> filterRules) {
        setFilterRules(filterRules);
        return this;
    }

    /**
     * Convenience varargs method to set the list of {@link FilterRule}s for this
     * {@link COSKeyFilter} and returns this object for method chaining
     * 
     * @param filterRules New {@link FilterRule}s for this {@link COSKeyFilter}
     * @return This object for method chaining
     */
    public COSKeyFilter withFilterRules(FilterRule... filterRules) {
        setFilterRules(Arrays.asList(filterRules));
        return this;
    }

    /**
     * Append a {@link FilterRule} to the list of {@link FilterRule}s for this {@link COSKeyFilter}
     * 
     * @param filterRule New {@link FilterRule} to append
     */
    public void addFilterRule(FilterRule filterRule) {
        this.filterRules.add(filterRule);
    }

}
