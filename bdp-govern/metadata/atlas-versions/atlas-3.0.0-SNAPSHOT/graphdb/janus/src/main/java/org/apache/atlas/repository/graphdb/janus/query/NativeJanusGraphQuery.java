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
package org.apache.atlas.repository.graphdb.janus.query;

import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.janusgraph.core.JanusGraphEdge;
import org.janusgraph.core.JanusGraphQuery;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.attribute.Contain;
import org.janusgraph.core.attribute.Text;
import org.janusgraph.graphdb.internal.ElementCategory;
import org.janusgraph.graphdb.query.JanusGraphPredicate;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery.ComparisionOperator;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery.MatchingOperator;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery.QueryOperator;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.graphdb.tinkerpop.query.NativeTinkerpopGraphQuery;
import org.apache.atlas.repository.graphdb.janus.AtlasJanusEdge;
import org.apache.atlas.repository.graphdb.janus.AtlasJanusGraph;
import org.apache.atlas.repository.graphdb.janus.AtlasJanusGraphDatabase;
import org.apache.atlas.repository.graphdb.janus.AtlasJanusVertex;
import org.apache.tinkerpop.gremlin.process.traversal.Compare;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.graphdb.query.JanusGraphPredicateUtils;
import org.janusgraph.graphdb.query.graph.GraphCentricQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Janus implementation of NativeTinkerpopGraphQuery.
 */
public class NativeJanusGraphQuery implements NativeTinkerpopGraphQuery<AtlasJanusVertex, AtlasJanusEdge> {
    private static final Logger LOG = LoggerFactory.getLogger(NativeJanusGraphQuery.class);

    private AtlasJanusGraph graph;
    private JanusGraphQuery<?> query;

    public NativeJanusGraphQuery(AtlasJanusGraph graph) {
        this.query = graph.getGraph().query();
        this.graph = graph;
    }

    @Override
    public Iterable<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>> vertices() {
        Iterable<JanusGraphVertex> it = query.vertices();
        return graph.wrapVertices(it);
    }

    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> edges() {
        Iterable<JanusGraphEdge> it = query.edges();
        return graph.wrapEdges(it);
    }

    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> edges(int limit) {
        Iterable<JanusGraphEdge> it = query.limit(limit).edges();

        if (LOG.isDebugEnabled()) {
            if (query instanceof GraphCentricQueryBuilder) {
                LOG.debug("NativeJanusGraphQuery.vertices({}): resultSize={}, {}", limit, getCountForDebugLog(it), ((GraphCentricQueryBuilder) query).constructQuery(ElementCategory.EDGE));
            } else {
                LOG.debug("NativeJanusGraphQuery.vertices({}): resultSize={}, {}", limit, getCountForDebugLog(it), query);
            }
        }

        return graph.wrapEdges(it);
    }

    @Override
    public Iterable<AtlasEdge<AtlasJanusVertex, AtlasJanusEdge>> edges(int offset, int limit) {
        List<Edge>               result = new ArrayList<>(limit);
        Iterable<? extends Edge> it     = query.limit(offset + limit).edges();

        if (LOG.isDebugEnabled()) {
            if (query instanceof GraphCentricQueryBuilder) {
                LOG.debug("NativeJanusGraphQuery.vertices({}, {}): resultSize={}, {}", offset, limit, getCountForDebugLog(it), ((GraphCentricQueryBuilder) query).constructQuery(ElementCategory.EDGE));
            } else {
                LOG.debug("NativeJanusGraphQuery.vertices({}, {}): resultSize={}, {}", offset, limit, getCountForDebugLog(it), query);
            }
        }

        Iterator<? extends Edge> iter = it.iterator();

        for (long resultIdx = 0; iter.hasNext() && result.size() < limit; resultIdx++) {
            Edge e = iter.next();

            if (resultIdx < offset) {
                continue;
            }

            result.add(e);
        }

        return graph.wrapEdges(result);
    }

    @Override
    public Iterable<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>> vertices(int limit) {
        Iterable<JanusGraphVertex> it = query.limit(limit).vertices();

        if (LOG.isDebugEnabled()) {
            if (query instanceof GraphCentricQueryBuilder) {
                LOG.debug("NativeJanusGraphQuery.vertices({}): resultSize={}, {}", limit, getCountForDebugLog(it), ((GraphCentricQueryBuilder) query).constructQuery(ElementCategory.VERTEX));
            } else {
                LOG.debug("NativeJanusGraphQuery.vertices({}): resultSize={}, {}", limit, getCountForDebugLog(it), query);
            }
        }

        return graph.wrapVertices(it);
    }

    @Override
    public Iterable<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>> vertices(int offset, int limit) {
        List<Vertex>               result = new ArrayList<>(limit);
        Iterable<JanusGraphVertex> it     = query.limit(offset + limit).vertices();

        if (LOG.isDebugEnabled()) {
            if (query instanceof GraphCentricQueryBuilder) {
                LOG.debug("NativeJanusGraphQuery.vertices({}, {}): resultSize={}, {}", offset, limit, getCountForDebugLog(it), ((GraphCentricQueryBuilder) query).constructQuery(ElementCategory.VERTEX));
            } else {
                LOG.debug("NativeJanusGraphQuery.vertices({}, {}): resultSize={}, {}", offset, limit, getCountForDebugLog(it), query);
            }
        }

        Iterator<? extends Vertex> iter = it.iterator();

        for (long resultIdx = 0; iter.hasNext() && result.size() < limit; resultIdx++) {
            Vertex v = iter.next();

            if (resultIdx < offset) {
                continue;
            }

            result.add(v);
        }

        return graph.wrapVertices(result);
    }

    @Override
    public Iterable<Object> vertexIds() {
        Set<Object>                result = new HashSet<>();
        Iterable<JanusGraphVertex> it     = query.vertices();

        for (Iterator<? extends Vertex> iter = it.iterator(); iter.hasNext(); ) {
            result.add(iter.next().id());
        }

        return result;
    }

    @Override
    public Iterable<Object> vertexIds(int limit) {
        Set<Object>                result = new HashSet<>(limit);
        Iterable<JanusGraphVertex> it     = query.limit(limit).vertices();

        if (LOG.isDebugEnabled()) {
            if (query instanceof GraphCentricQueryBuilder) {
                LOG.debug("NativeJanusGraphQuery.vertices({}): resultSize={}, {}", limit, getCountForDebugLog(it), ((GraphCentricQueryBuilder) query).constructQuery(ElementCategory.VERTEX));
            } else {
                LOG.debug("NativeJanusGraphQuery.vertices({}): resultSize={}, {}", limit, getCountForDebugLog(it), query);
            }
        }

        for (Iterator<? extends Vertex> iter = it.iterator(); iter.hasNext(); ) {
            result.add(iter.next().id());
        }

        return result;
    }

    @Override
    public Iterable<Object> vertexIds(int offset, int limit) {
        Set<Object>                result = new HashSet<>(limit);
        Iterable<JanusGraphVertex> it     = query.limit(offset + limit).vertices();

        if (LOG.isDebugEnabled()) {
            if (query instanceof GraphCentricQueryBuilder) {
                LOG.debug("NativeJanusGraphQuery.vertices({}, {}): resultSize={}, {}", offset, limit, getCountForDebugLog(it), ((GraphCentricQueryBuilder) query).constructQuery(ElementCategory.VERTEX));
            } else {
                LOG.debug("NativeJanusGraphQuery.vertices({}, {}): resultSize={}, {}", offset, limit, getCountForDebugLog(it), query);
            }
        }

        Iterator<? extends Vertex> iter = it.iterator();

        for (long resultIdx = 0; iter.hasNext() && result.size() < limit; resultIdx++) {
            if (resultIdx < offset) {
                continue;
            }

            result.add(iter.next().id());
        }

        return result;
    }

    @Override
    public void in(String propertyName, Collection<? extends Object> values) {
        query.has(propertyName, Contain.IN, values);

    }

    @Override
    public void has(String propertyName, QueryOperator op, Object value) {
        JanusGraphPredicate pred;
        if (op instanceof ComparisionOperator) {
            Compare c = getGremlinPredicate((ComparisionOperator) op);
            pred = JanusGraphPredicateUtils.convert(c);
        } else {
            pred = getGremlinPredicate((MatchingOperator)op);
        }
        query.has(propertyName, pred, value);
    }

    @Override
    public void orderBy(final String propertyName, final AtlasGraphQuery.SortOrder sortOrder) {
        Order order = sortOrder == AtlasGraphQuery.SortOrder.ASC ? Order.asc : Order.desc;
        query.orderBy(propertyName, order);
    }

    private Text getGremlinPredicate(MatchingOperator op) {
        switch (op) {
            case CONTAINS:
                return Text.CONTAINS;
            case PREFIX:
                return Text.PREFIX;
            case SUFFIX:
                return Text.CONTAINS_REGEX;
            case REGEX:
                return Text.REGEX;
            default:
                throw new RuntimeException("Unsupported matching operator:" + op);
        }
    }

    private Compare getGremlinPredicate(ComparisionOperator op) {
        switch (op) {
            case EQUAL:
                return Compare.eq;
            case GREATER_THAN:
                return Compare.gt;
            case GREATER_THAN_EQUAL:
                return Compare.gte;
            case LESS_THAN:
                return Compare.lt;
            case LESS_THAN_EQUAL:
                return Compare.lte;
            case NOT_EQUAL:
                return Compare.neq;

            default:
                throw new RuntimeException("Unsupported comparison operator:" + op);
        }
    }

    private int getCountForDebugLog(Iterable it) {
        int ret = 0;

        if (LOG.isDebugEnabled()) {
            if (it != null) {
                for (Iterator iter = it.iterator(); iter.hasNext(); iter.next()) {
                    ret++;
                }
            }
        }

        return ret;
    }
}
