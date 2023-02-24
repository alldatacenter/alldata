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

import org.apache.atlas.model.discovery.AtlasAggregationEntry;

import java.util.List;
import java.util.Map;

/**
 * Represents a graph client work with indices used by Jansgraph.
 */
public interface AtlasGraphIndexClient {

    /**
     * Gets aggregated metrics for the given query string and aggregation field names.
     * @return A map of aggregation field to value-count pairs.
     */
    Map<String, List<AtlasAggregationEntry>> getAggregatedMetrics(AggregationContext aggregationContext);

    /**
     * Returns top 5 suggestions for the given prefix string.
     * @param prefixString the prefix string whose value needs to be retrieved.
     * @param indexFieldName the indexed field name from which to retrieve suggestions
     * @return top 5 suggestion strings with prefix String
     */
    List<String> getSuggestions(String prefixString, String indexFieldName);

    /**
     *  The implementers should apply the search weights for the passed in properties.
     *  @param collectionName                the name of the collection for which the search weight needs to be applied
     *  @param indexFieldName2SearchWeightMap the map containing search weights from index field name to search weights.
     */
    void applySearchWeight(String collectionName, Map<String, Integer> indexFieldName2SearchWeightMap);

    /**
     * The implementors should take the passed in list of suggestion properties for suggestions functionality.
     * @param collectionName the name of the collection to which the suggestions properties should be applied to.
     * @param suggestionProperties the list of suggestion properties.
     */
    void applySuggestionFields(String collectionName, List<String> suggestionProperties);

    /**
     * Returns status of index client
     * @return returns true if index client is active
     */
    boolean isHealthy();
}
