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

import org.apache.tinkerpop.gremlin.process.traversal.Order;

import java.util.Iterator;

/**
 * A graph query that runs directly against a particular index.
 *
 * @param <V> vertex class used by the graph
 * @param <E> edge class used by the graph
 */
public interface AtlasIndexQuery<V, E> {

    /**
     * Gets the query results.
     *
     * @return
     */
    Iterator<Result<V, E>> vertices();

    /**
     * Gets the sorted query results
     * @param offset starting offset
     * @param limit max number of results
     * @return
     */
    Iterator<Result<V, E>> vertices(int offset, int limit, String sortBy, Order sortOrder);

    /**
     * Gets the query results
     * @param offset starting offset
     * @param limit max number of results
     * @return
     */
    Iterator<Result<V, E>> vertices(int offset, int limit);

    /**
     * Gets the total count of query results
     * @return
     */
    Long vertexTotals();

    /**
     * Query result from an index query.
     *
     * @param <V>
     * @param <E>
     */
    interface Result<V, E> {

        /**
         * Gets the vertex for this result.
         */
        AtlasVertex<V, E> getVertex();

        /**
         * Gets the score for this result.
         *
         */
        double getScore();

    }

}
