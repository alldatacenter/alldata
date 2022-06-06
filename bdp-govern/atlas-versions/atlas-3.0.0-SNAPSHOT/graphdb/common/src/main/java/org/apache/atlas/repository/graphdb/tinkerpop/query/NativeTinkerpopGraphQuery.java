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
package org.apache.atlas.repository.graphdb.tinkerpop.query;

import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery.QueryOperator;
import org.apache.atlas.repository.graphdb.AtlasVertex;

import java.util.Collection;

/**
 * Interfaces that provides a thin wrapper around GraphQuery (used by Titan0) and
 * TitanGraphQuery (used by Titan 1).
 *
 * This abstraction  allows TitanGraphQuery to work on any version of Titan.
 *
 * @param <V>
 * @param <E>
 */
public interface NativeTinkerpopGraphQuery<V, E> {

    /**
     * Executes the graph query.
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices();

    /**
     * Executes the graph query.
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges();

    /**
     * Executes graph query
     * @param limit Max edges to return
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges(int limit);

    /**
     * Executes graph query
     * @param offset Starting offset
     * @param limit Max edges to return
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges(int offset, int limit);

    /**
     * Executes graph query
     * @param limit Max vertices to return
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices(int limit);

    /**
     * Executes graph query
     * @param offset Starting offset
     * @param limit Max vertices to return
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices(int offset, int limit);

    /**
     * Executes the graph query.
     * @return
     */
    Iterable<Object> vertexIds();

    /**
     * Executes graph query
     * @param limit Max vertices to return
     * @return
     */
    Iterable<Object> vertexIds(int limit);

    /**
     * Executes graph query
     * @param offset Starting offset
     * @param limit Max vertices to return
     * @return
     */
    Iterable<Object> vertexIds(int offset, int limit);


    /**
     * Adds an in condition to the query.
     *
     * @param propertyName
     * @param values
     */
    void in(String propertyName, Collection<?> values);

    /**
     * Adds a has condition to the query.
     *
     * @param propertyName
     * @param op
     * @param value
     */
    void has(String propertyName, QueryOperator op, Object value);

    /**
     * Add sort predicate for give property
     * @param propertyName
     * @param sortOrder
     */
    void orderBy(String propertyName, AtlasGraphQuery.SortOrder sortOrder);
}
