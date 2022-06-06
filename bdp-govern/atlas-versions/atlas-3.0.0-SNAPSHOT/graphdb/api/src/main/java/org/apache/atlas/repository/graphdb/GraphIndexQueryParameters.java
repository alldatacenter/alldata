/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.graphdb;

import java.util.List;

/**
 * A class wrapping the parameters that need to create a graph index query.
 */
public class GraphIndexQueryParameters {
    final String indexName;
    final String graphQueryString;
    final int offset;
    final List<AtlasIndexQueryParameter> indexQueryParameters;

    /**
     *
     * @param indexName the name of the index on which the query is being made
     * @param graphQueryString the graph query string.
     * @param offset    the offset to be used for data fetch.
     * @param indexQueryParameters  any index system specific parameters for use in query.
     */
    public GraphIndexQueryParameters(String indexName, String graphQueryString, int offset, List<AtlasIndexQueryParameter> indexQueryParameters) {
        this.indexName = indexName;
        this.graphQueryString = graphQueryString;
        this.offset = offset;
        this.indexQueryParameters = indexQueryParameters;
    }


    public String getIndexName() {
        return indexName;
    }

    public String getGraphQueryString() {
        return graphQueryString;
    }

    public int getOffset() {
        return offset;
    }

    public List<AtlasIndexQueryParameter> getIndexQueryParameters() {
        return indexQueryParameters;
    }
}
