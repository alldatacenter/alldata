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
package org.apache.atlas.repository.graphdb;

import org.apache.atlas.model.discovery.SearchParameters.FilterCriteria;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;

import java.util.Map;
import java.util.Set;

public class AggregationContext {
    private final String               queryString;
    private final FilterCriteria       filterCriteria;
    private final Set<AtlasEntityType> searchForEntityTypes;
    private final Set<String>          aggregationFieldNames;
    private final Set<AtlasAttribute>  aggregationAttributes;
    private final Map<String, String>  indexFieldNameCache;
    private final boolean              excludeDeletedEntities;
    private final boolean              includeSubTypes;

    /**
     * @param queryString the query string whose aggregation metrics need to be retrieved.
     * @param searchForEntityType
     * @param aggregationFieldNames the set of aggregation fields.
     * @param indexFieldNameCache
     * @param excludeDeletedEntities a boolean flag to indicate if the deleted entities need to be excluded in search
     */
    public AggregationContext(String               queryString,
                              FilterCriteria       filterCriteria,
                              Set<AtlasEntityType> searchForEntityType,
                              Set<String>          aggregationFieldNames,
                              Set<AtlasAttribute>  aggregationAttributes,
                              Map<String, String>  indexFieldNameCache,
                              boolean              excludeDeletedEntities,
                              boolean              includeSubTypes) {
        this.queryString            = queryString;
        this.filterCriteria         = filterCriteria;
        this.searchForEntityTypes   = searchForEntityType;
        this.aggregationFieldNames  = aggregationFieldNames;
        this.aggregationAttributes  = aggregationAttributes;
        this.indexFieldNameCache    = indexFieldNameCache;
        this.excludeDeletedEntities = excludeDeletedEntities;
        this.includeSubTypes        = includeSubTypes;
    }

    public String getQueryString() {
        return queryString;
    }

    public FilterCriteria getFilterCriteria() {
        return filterCriteria;
    }

    public Set<AtlasEntityType> getSearchForEntityTypes() {
        return searchForEntityTypes;
    }

    public Set<String> getAggregationFieldNames() {
        return aggregationFieldNames;
    }

    public Set<AtlasAttribute> getAggregationAttributes() {
        return aggregationAttributes;
    }

    public Map<String, String> getIndexFieldNameCache() {
        return indexFieldNameCache;
    }

    public boolean isExcludeDeletedEntities() {
        return excludeDeletedEntities;
    }

    public boolean isIncludeSubTypes() {
        return includeSubTypes;
    }
}
