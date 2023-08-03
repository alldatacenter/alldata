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

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasQuickSearchResult {
    private AtlasSearchResult                         searchResults;
    private Map<String, List<AtlasAggregationEntry>>  aggregationMetrics;

    public AtlasQuickSearchResult() {
    }

    public AtlasQuickSearchResult(AtlasSearchResult searchResults, Map<String, List<AtlasAggregationEntry>> aggregationMetrics) {
        this.searchResults      = searchResults;
        this.aggregationMetrics = aggregationMetrics;
    }

    public Map<String, List<AtlasAggregationEntry>> getAggregationMetrics() {
        return aggregationMetrics;
    }

    public void setAggregationMetrics(Map<String, List<AtlasAggregationEntry>> aggregationMetrics) {
        this.aggregationMetrics = aggregationMetrics;
    }

    public AtlasSearchResult getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(AtlasSearchResult searchResults) {
        this.searchResults = searchResults;
    }
}
