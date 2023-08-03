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

import com.google.common.base.Preconditions;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.graphdb.tinkerpop.query.expr.AndCondition;
import org.apache.atlas.repository.graphdb.tinkerpop.query.expr.HasPredicate;
import org.apache.atlas.repository.graphdb.tinkerpop.query.expr.InPredicate;
import org.apache.atlas.repository.graphdb.tinkerpop.query.expr.OrCondition;
import org.apache.atlas.repository.graphdb.tinkerpop.query.expr.OrderByPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Abstract implementation of AtlasGraphQuery that is used by JanusGraph
 * <p>
 * Represents a graph query as an OrConditions which consists of
 * 1 or more AndConditions.  The query is executed by converting
 * the AndConditions to native GraphQuery instances that can be executed
 * directly against Tinkerpop GraphDB.  The overall result is obtained by
 * unioning together the results from those individual GraphQueries.
 * <p>
 * Here is a pictoral view of what is going on here.  Conceptually,
 * the query being executed can be though of as the where clause
 * in a query
 *
 * <pre>
 * where (a =1 and b=2) or (a=2 and b=3)
 *
 *                ||
 *               \||/
 *                \/
 *
 *           OrCondition
 *                 |
 *       +---------+--------+
 *       |                  |
 *   AndCondition     AndCondition
 *   (a=1 and b=2)     (a=2 and b=3)
 *
 *       ||                 ||
 *      \||/               \||/
 *       \/                 \/
 *
 *   GraphQuery          GraphQuery
 *
 *       ||                 ||
 *      \||/               \||/
 *       \/                 \/
 *
 *     vertices          vertices
 *           \            /
 *           _\/        \/_
 *               (UNION)
 *
 *                 ||
 *                \||/
 *                 \/
 *
 *               result
 * </pre>
 *
 *
 */
public abstract class TinkerpopGraphQuery<V, E> implements AtlasGraphQuery<V, E> {

    private static final Logger LOG = LoggerFactory.getLogger(TinkerpopGraphQuery.class);
    protected final AtlasGraph<V, E> graph;
    private final OrCondition queryCondition = new OrCondition();
    private final boolean isChildQuery;
    protected abstract NativeTinkerpopQueryFactory<V, E> getQueryFactory();

    /**
     * Creates a TinkerpopGraphQuery.
     *
     * @param graph
     */
    public TinkerpopGraphQuery(AtlasGraph<V, E> graph) {
        this.graph = graph;
        this.isChildQuery = false;
    }

    /**
     * Creates a TinkerpopGraphQuery.
     *
     * @param graph
     * @param isChildQuery
     */
    public TinkerpopGraphQuery(AtlasGraph<V, E> graph, boolean isChildQuery) {
        this.graph = graph;
        this.isChildQuery = isChildQuery;
    }

    @Override
    public AtlasGraphQuery<V, E> has(String propertyKey, Object value) {
        queryCondition.andWith(new HasPredicate(propertyKey, ComparisionOperator.EQUAL, value));
        return this;
    }

    @Override
    public Iterable<AtlasVertex<V, E>> vertices() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + queryCondition);
        }

        // Compute the overall result by combining the results of all the AndConditions (nested within OR) together.
        Set<AtlasVertex<V, E>> result = new LinkedHashSet<>();

        for(AndCondition andExpr : queryCondition.getAndTerms()) {
            NativeTinkerpopGraphQuery<V, E> andQuery = andExpr.create(getQueryFactory());
            for(AtlasVertex<V, E> vertex : andQuery.vertices()) {
                result.add(vertex);
            }
        }
        return result;
    }

    @Override
    public Iterable<AtlasEdge<V, E>> edges() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + queryCondition);
        }

        // Compute the overall result by combining the results of all the AndConditions (nested within OR) together.
        Set<AtlasEdge<V, E>> result = new HashSet<>();
        for(AndCondition andExpr : queryCondition.getAndTerms()) {
            NativeTinkerpopGraphQuery<V, E> andQuery = andExpr.create(getQueryFactory());
            for(AtlasEdge<V, E> edge : andQuery.edges()) {
                result.add(edge);
            }
        }
        return result;
    }

    @Override
    public Iterable<AtlasEdge<V, E>> edges(int limit) {
        return edges(0, limit);
    }

    @Override
    public Iterable<AtlasEdge<V, E>> edges(int offset, int limit) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + queryCondition);
        }

        Preconditions.checkArgument(offset >= 0, "Offset must be non-negative");
        Preconditions.checkArgument(limit >= 0, "Limit must be non-negative");

        // Compute the overall result by combining the results of all the AndConditions (nested within OR) together.
        Set<AtlasEdge<V, E>> result = new HashSet<>();
        long resultIdx = 0;
        for(AndCondition andExpr : queryCondition.getAndTerms()) {
            if (result.size() == limit) {
                break;
            }

            NativeTinkerpopGraphQuery<V, E> andQuery = andExpr.create(getQueryFactory());
            for(AtlasEdge<V, E> edge : andQuery.edges(offset + limit)) {
                if (resultIdx >= offset) {
                    result.add(edge);

                    if (result.size() == limit) {
                        break;
                    }
                }

                resultIdx++;
            }
        }

        return result;
    }

    @Override
    public Iterable<AtlasVertex<V, E>> vertices(int limit) {
        return vertices(0, limit);
    }

    @Override
    public Iterable<AtlasVertex<V, E>> vertices(int offset, int limit) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + queryCondition);
        }

        Preconditions.checkArgument(offset >= 0, "Offset must be non-negative");
        Preconditions.checkArgument(limit >= 0, "Limit must be non-negative");

        // Compute the overall result by combining the results of all the AndConditions (nested within OR) together.
        Set<AtlasVertex<V, E>> result = new LinkedHashSet<>();
        long resultIdx = 0;
        for(AndCondition andExpr : queryCondition.getAndTerms()) {
            if (result.size() == limit) {
                break;
            }

            NativeTinkerpopGraphQuery<V, E> andQuery = andExpr.create(getQueryFactory());
            for(AtlasVertex<V, E> vertex : andQuery.vertices(offset + limit)) {
                if (resultIdx >= offset) {
                    result.add(vertex);

                    if (result.size() == limit) {
                        break;
                    }
                }

                resultIdx++;
            }
        }

        return result;
    }

    @Override
    public Iterable<Object> vertexIds() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + queryCondition);
        }

        // Compute the overall result by combining the results of all the AndConditions (nested within OR) together.
        Set<Object> result = new HashSet<>();

        for(AndCondition andExpr : queryCondition.getAndTerms()) {
            NativeTinkerpopGraphQuery<V, E> andQuery = andExpr.create(getQueryFactory());
            for(Object vertexId : andQuery.vertexIds()) {
                result.add(vertexId);
            }
        }
        return result;
    }

    @Override
    public Iterable<Object> vertexIds(int limit) {
        return vertexIds(0, limit);
    }

    @Override
    public Iterable<Object> vertexIds(int offset, int limit) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + queryCondition);
        }

        Preconditions.checkArgument(offset >= 0, "Offset must be non-negative");
        Preconditions.checkArgument(limit >= 0, "Limit must be non-negative");

        // Compute the overall result by combining the results of all the AndConditions (nested within OR) together.
        Set<Object> result = new HashSet<>();
        long resultIdx = 0;
        for(AndCondition andExpr : queryCondition.getAndTerms()) {
            if (result.size() == limit) {
                break;
            }

            NativeTinkerpopGraphQuery<V, E> andQuery = andExpr.create(getQueryFactory());
            for(Object vertexId : andQuery.vertexIds(offset + limit)) {
                if (resultIdx >= offset) {
                    result.add(vertexId);

                    if (result.size() == limit) {
                        break;
                    }
                }

                resultIdx++;
            }
        }

        return result;
    }

    @Override
    public AtlasGraphQuery<V, E> has(String propertyKey, QueryOperator operator,
            Object value) {
        queryCondition.andWith(new HasPredicate(propertyKey, operator, value));
        return this;
    }


    @Override
    public AtlasGraphQuery<V, E> in(String propertyKey, Collection<?> values) {
        queryCondition.andWith(new InPredicate(propertyKey, values));
        return this;
    }

    @Override
    public AtlasGraphQuery<V, E> or(List<AtlasGraphQuery<V, E>> childQueries) {

        //Construct an overall OrCondition by combining all of the children for
        //the OrConditions in all of the childQueries that we passed in.  Then, "and" the current
        //query condition with this overall OrCondition.

        OrCondition overallChildQuery = new OrCondition(false);

        for(AtlasGraphQuery<V, E> atlasChildQuery : childQueries) {
            if (!atlasChildQuery.isChildQuery()) {
                throw new IllegalArgumentException(atlasChildQuery + " is not a child query");
            }
            TinkerpopGraphQuery<V, E> childQuery = (TinkerpopGraphQuery<V, E>)atlasChildQuery;
            overallChildQuery.orWith(childQuery.getOrCondition());
        }

        queryCondition.andWith(overallChildQuery);
        return this;
    }

    @Override
    public AtlasGraphQuery<V, E> orderBy(final String propertyKey, final SortOrder order) {
        queryCondition.andWith(new OrderByPredicate(propertyKey, order));
        return this;
    }

    private OrCondition getOrCondition() {
        return queryCondition;
    }

    @Override
    public AtlasGraphQuery<V, E> addConditionsFrom(AtlasGraphQuery<V, E> otherQuery) {

        TinkerpopGraphQuery<V, E> childQuery = (TinkerpopGraphQuery<V, E>)otherQuery;
        queryCondition.andWith(childQuery.getOrCondition());
        return this;
    }

    @Override
    public boolean isChildQuery() {
        return isChildQuery;
    }
}
