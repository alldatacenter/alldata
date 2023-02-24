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
package org.apache.impala.hooks;

import java.util.Objects;

/**
 * {@link QueryCompleteContext} encapsulates immutable information sent from the
 * BE to a post-query hook.
 */
public class QueryCompleteContext {
    private final String lineageGraph_;

    public QueryCompleteContext(String lineageGraph) {
        lineageGraph_ = Objects.requireNonNull(lineageGraph);
    }

    /**
     * Returns the lineage graph sent from the backend during
     * {@link QueryEventHook#onQueryComplete(QueryCompleteContext)}.  This graph
     * object will generally contain more information than it did when it was
     * first constructed in the frontend, because the backend will have filled
     * in additional information.
     * <p>
     * The returned object is a JSON representation of the lineage graph object
     * for the query.  The details of the JSON translation are not provided here
     * as this is meant to be a temporary feature, and the String format will
     * be changed to something more strongly-typed in the future.
     * </p>
     *
     * @return lineage graph from the query that executed
     */
    public String getLineageGraph() { return lineageGraph_; }

    @Override
    public String toString() {
        return "QueryCompleteContext{" +
            "lineageGraph='" + lineageGraph_ + '\'' +
            '}';
    }
}
