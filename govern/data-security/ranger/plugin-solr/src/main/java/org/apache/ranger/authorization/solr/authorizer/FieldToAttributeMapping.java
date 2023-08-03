/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.solr.authorizer;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Helper class to hold the configuration which maps fields in Solr to one or more
 * LDAP attributes. Includes details such as whether empty values in the doc should be permitted, whether there is a
 * default value for all users, whether any RegExs should be applied and any extra options that should be passed
 * to the fq
 */
public class FieldToAttributeMapping {

    private static final Splitter ATTR_NAME_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    private final String fieldName;
    private final Collection<String> attributes;
    private final FilterType filterType;
    private final boolean acceptEmpty;
    private final String allUsersValue;
    private final Pattern attrValueRegex;
    private final String extraOpts;

    /**
     * The four filter types currently supported, AND, OR, LessThanOrEqualTo (LTE), GreaterThanOrEqualTo (GTE)
     * Expected to be expanded in the future
     */
    enum FilterType {
        AND,
        OR,
        LTE,
        GTE
    }

    /**
     * @param fieldName The field being mapped
     * @param ldapAttributeNames comma delimited list of attributes which will be used to acquire values for this field
     * @param filterType filter type can be any one of {@link FilterType}
     * @param acceptEmpty true if an empty value in the Solr field should be counted as a match (i.e. doc returned)
     * @param allUsersValue the value which the field may contain that would indicate that all users should see this doc
     * @param valueFilterRegex String representation of a {@link Pattern} that will be applied to attributes retrieved
     *                         from the attribute source. Note: If match groups are used, the last non-null match-group
     *                         will be applied as the value for this filter
     * @param extraOpts Any extra options that should be passed to the filter as constructed before appending to the fq
     */
    public FieldToAttributeMapping(String fieldName, String ldapAttributeNames, String filterType, boolean acceptEmpty, String allUsersValue, String valueFilterRegex, String extraOpts) {
        this.fieldName = fieldName;
        this.attributes = Collections.unmodifiableSet(Sets.newHashSet(ATTR_NAME_SPLITTER.split(ldapAttributeNames)));
        this.filterType = FilterType.valueOf(filterType);
        this.acceptEmpty = acceptEmpty;
        this.allUsersValue = allUsersValue;
        this.attrValueRegex = Pattern.compile(valueFilterRegex);
        this.extraOpts = extraOpts;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Collection<String> getAttributes() {
        return attributes;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public boolean getAcceptEmpty() {
        return acceptEmpty;
    }

    public String getAllUsersValue() {
        return allUsersValue;
    }

    public String getExtraOpts() {
        return extraOpts;
    }

    public Pattern getAttrValueRegex() {
    	return attrValueRegex;
    }
}
