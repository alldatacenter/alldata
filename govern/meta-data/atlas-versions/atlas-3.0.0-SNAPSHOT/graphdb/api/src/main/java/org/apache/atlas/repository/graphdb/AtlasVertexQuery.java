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

/**
 * A query against a particular vertex.
 *
 * @param <V> vertex class used by the graph
 * @param <E> edge class used by the graph
 */
public interface AtlasVertexQuery<V, E> {

    /**
     * Specifies the edge direction that should be query.
     *
     * @param queryDirection
     * @return
     */
    AtlasVertexQuery<V, E> direction(AtlasEdgeDirection queryDirection);

    /**
     * Returns the vertices that satisfy the query condition.
     *
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices();

    /**
     * Returns the vertices that satisfy the query condition.
     *
     * @param limit Max number of vertices
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices(int limit);

    /**
     * Returns the incident edges that satisfy the query condition.
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges();

    /**
     * Returns the incident edges that satisfy the query condition.
     * @param limit Max number of edges
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges(int limit);

    /**
     * Returns the number of elements that match the query.
     * @return
     */
    long count();

    /**
     * Specifies the edge label that should be queried.
     *
     * @param label
     * @return
     */
    AtlasVertexQuery<V, E> label(String label);

    /**
     * Returns edges that matches property key and value.
     *
     * @param key
     * @param value
     * @return
     */
    AtlasVertexQuery<V, E> has(String key, Object value);
}
