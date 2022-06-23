/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.discovery;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.SortOrder;
import org.apache.atlas.model.discovery.SearchParameters.FilterCriteria;

import java.io.Serializable;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * This is the root class representing the input for quick search puroposes.
 */
public class QuickSearchParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    private String         query;
    private String         typeName;
    private FilterCriteria entityFilters;
    private boolean        includeSubTypes;
    private boolean        excludeDeletedEntities;
    private int            offset;
    private int            limit;
    private Set<String>    attributes;
    private String         sortBy;
    private SortOrder      sortOrder;

    /**
     * for framework use.
     */
    public QuickSearchParameters() {
    }

    public QuickSearchParameters(String         query,
                                 String         typeName,
                                 FilterCriteria entityFilters,
                                 boolean        includeSubTypes,
                                 boolean        excludeDeletedEntities,
                                 int            offset,
                                 int            limit,
                                 Set<String>    attributes,
                                 String         sortBy,
                                 SortOrder      sortOrder) {
        this.query                  = query;
        this.typeName               = typeName;
        this.entityFilters          = entityFilters;
        this.includeSubTypes        = includeSubTypes;
        this.excludeDeletedEntities = excludeDeletedEntities;
        this.offset                 = offset;
        this.limit                  = limit;
        this.attributes             = attributes;
        this.sortBy                 = sortBy;
        this.sortOrder              = sortOrder;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public FilterCriteria getEntityFilters() {
        return entityFilters;
    }

    public void setEntityFilters(FilterCriteria entityFilters) {
        this.entityFilters = entityFilters;
    }

    public boolean getIncludeSubTypes() {
        return includeSubTypes;
    }

    public void setIncludeSubTypes(boolean includeSubTypes) {
        this.includeSubTypes = includeSubTypes;
    }

    public boolean getExcludeDeletedEntities() {
        return excludeDeletedEntities;
    }

    public void setExcludeDeletedEntities(boolean excludeDeletedEntities) {
        this.excludeDeletedEntities = excludeDeletedEntities;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
