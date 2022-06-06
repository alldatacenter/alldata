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
package org.apache.atlas.util;

import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graphdb.GremlinVersion;

/**
 * Generic Gremlin query provider which is agnostic of the Gremlin/TinkerPop version being used in Atlas
 */
public abstract class AtlasGremlinQueryProvider {
    public static final AtlasGremlinQueryProvider INSTANCE =
            AtlasGraphProvider.getGraphInstance().getSupportedGremlinVersion() == GremlinVersion.THREE ?
                    new AtlasGremlin3QueryProvider() : new AtlasGremlin2QueryProvider();

    abstract public String getQuery(final AtlasGremlinQuery gremlinQuery);

    public enum AtlasGremlinQuery {
        // Metrics related Queries
        TYPE_COUNT_METRIC,
        TYPE_UNUSED_COUNT_METRIC,
        TAG_COUNT_METRIC,
        ENTITY_ACTIVE_METRIC,
        ENTITY_DELETED_METRIC,
        ENTITIES_PER_TYPE_METRIC,
        TAGGED_ENTITIES_METRIC,
        ENTITIES_FOR_TAG_METRIC,

        // Import Export related Queries
        EXPORT_BY_GUID_FULL,
        EXPORT_BY_GUID_CONNECTED_IN_EDGE,
        EXPORT_BY_GUID_CONNECTED_OUT_EDGE,
        EXPORT_TYPE_ALL_FOR_TYPE,
        EXPORT_TYPE_STARTS_WITH,
        EXPORT_TYPE_ENDS_WITH,
        EXPORT_TYPE_CONTAINS,
        EXPORT_TYPE_MATCHES,
        EXPORT_TYPE_DEFAULT,

        // Lineage Queries
        FULL_LINEAGE_DATASET,
        FULL_LINEAGE_PROCESS,
        PARTIAL_LINEAGE_DATASET,
        PARTIAL_LINEAGE_PROCESS,

        // Discovery Queries
        BASIC_SEARCH_TYPE_FILTER,
        BASIC_SEARCH_CLASSIFICATION_FILTER,
        BASIC_SEARCH_STATE_FILTER,
        TO_RANGE_LIST,
        GUID_PREFIX_FILTER,
        RELATIONSHIP_SEARCH,
        RELATIONSHIP_SEARCH_ASCENDING_SORT,
        RELATIONSHIP_SEARCH_DESCENDING_SORT,

        // Discovery test queries
        GREMLIN_SEARCH_RETURNS_VERTEX_ID,
        GREMLIN_SEARCH_RETURNS_EDGE_ID,

        // Comparison clauses
        COMPARE_LT,
        COMPARE_LTE,
        COMPARE_GT,
        COMPARE_GTE,
        COMPARE_EQ,
        COMPARE_NEQ,
        COMPARE_MATCHES,
        COMPARE_STARTS_WITH,
        COMPARE_ENDS_WITH,
        COMPARE_CONTAINS,
        COMPARE_IS_NULL,
        COMPARE_NOT_NULL,
    }
}
