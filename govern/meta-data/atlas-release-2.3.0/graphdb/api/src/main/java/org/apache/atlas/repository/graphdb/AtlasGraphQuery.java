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

import java.util.Collection;
import java.util.List;

/**
 * Represents a query against the graph within the context of the
 * current transaction.
 *
 * @param <V> vertex class used by the graph
 * @param <E> edge class used by the graph
 */
public interface AtlasGraphQuery<V, E> {

    enum SortOrder { ASC, DESC }

    /**
     * Adds a predicate that the returned vertices must have the specified
     * property and that one of the values of the property must be the
     * given value.
     *
     * @param propertyKey
     * @param value
     * @return
     */
    AtlasGraphQuery<V, E> has(String propertyKey, Object value);

    /**
     * Adds a predicate that the returned vertices must have the specified
     * property and that one of the value of the property must be in
     * the specified list of values.
     *
     * @param propertyKey
     * @param values
     * @return
     */
    AtlasGraphQuery<V, E> in(String propertyKey, Collection<?> values);

    /**
     * Executes the query and returns the matching edges.
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges();

    /**
     * Executes the query and returns the matching edges till the max limit
     * @param limit max number of vertices
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges(int limit);

    /**
     * Executes the query and returns the matching edges from given offset till the max limit
     * @param offset starting offset
     * @param limit max number of vertices
     * @return
     */
    Iterable<AtlasEdge<V, E>> edges(int offset, int limit);

    /**
     * Executes the query and returns the matching vertices.
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices();

    /**
     * Executes the query and returns the matching vertices from given offset till the max limit
     * @param limit max number of vertices
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices(int limit);

    /**
     * Executes the query and returns the matching vertices from given offset till the max limit
     * @param offset starting offset
     * @param limit max number of vertices
     * @return
     */
    Iterable<AtlasVertex<V, E>> vertices(int offset, int limit);

    /**
     * Executes the query and returns IDs of matching vertices.
     * @return
     */
    Iterable<Object> vertexIds();

    /**
     * Executes the query and returns IDs of the matching vertices from given offset till the max limit
     * @param limit max number of vertices
     * @return
     */
    Iterable<Object> vertexIds(int limit);

    /**
     * Executes the query and returns IDs of the matching vertices from given offset till the max limit
     * @param offset starting offset
     * @param limit max number of vertices
     * @return
     */
    Iterable<Object> vertexIds(int offset, int limit);


    /**
     * Adds a predicate that the returned vertices must have the specified
     * property and that its value matches the criterion specified.
     *
     * @param propertyKey
     * @param op
     * @param values
     * @return
     */
    AtlasGraphQuery<V, E> has(String propertyKey, QueryOperator op, Object values);


    /**
     * Adds a sorting predicate
     * @param propertyKey property key to sort on
     * @param order ASC or DESC
     * @return
     */
    AtlasGraphQuery<V, E> orderBy(String propertyKey, SortOrder order);

    /**
     * Adds a predicate that the vertices returned must satisfy the
     * conditions in at least one of the child queries provided.
     *
     * @param childQueries
     * @return
     */
    AtlasGraphQuery<V, E> or(List<AtlasGraphQuery<V, E>> childQueries);

    /**
     * Creates a child query that can be used to add "or" conditions.
     *
     * @return
     */
    AtlasGraphQuery<V, E> createChildQuery();


    interface QueryOperator {}

    /**
     * Comparison operators that can be used in an AtlasGraphQuery.
     */
    enum ComparisionOperator implements QueryOperator {
        GREATER_THAN,
        GREATER_THAN_EQUAL,
        EQUAL,
        LESS_THAN,
        LESS_THAN_EQUAL,
        NOT_EQUAL
    }

    /**
     * String/text matching that can be used in AtlasGraphQuery
     */
    enum MatchingOperator implements QueryOperator {
        CONTAINS,
        PREFIX,
        SUFFIX,
        REGEX
    }

    /**
     * Adds all of the predicates that have been added to this query to the
     * specified query.
     * @param otherQuery
     * @return
     */
    AtlasGraphQuery<V, E> addConditionsFrom(AtlasGraphQuery<V, E> otherQuery);

    /**
     * Whether or not this is a child query.
     *
     * @return
     */
    boolean isChildQuery();


}
