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
package org.apache.atlas.repository.graphdb.janus;

import java.util.Iterator;

import com.google.common.base.Preconditions;
import org.apache.atlas.repository.graphdb.AtlasIndexQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.janusgraph.core.JanusGraphIndexQuery;
import org.janusgraph.core.JanusGraphVertex;

/**
 * Janus implementation of AtlasIndexQuery.
 */
public class AtlasJanusIndexQuery implements AtlasIndexQuery<AtlasJanusVertex, AtlasJanusEdge> {
    private AtlasJanusGraph      graph;
    private JanusGraphIndexQuery query;

    public AtlasJanusIndexQuery(AtlasJanusGraph graph, JanusGraphIndexQuery query) {
        this.query = query;
        this.graph = graph;
    }

    @Override
    public Iterator<Result<AtlasJanusVertex, AtlasJanusEdge>> vertices() {
        Iterator<JanusGraphIndexQuery.Result<JanusGraphVertex>> results = query.vertices().iterator();

        Function<JanusGraphIndexQuery.Result<JanusGraphVertex>, Result<AtlasJanusVertex, AtlasJanusEdge>> function =
            new Function<JanusGraphIndexQuery.Result<JanusGraphVertex>, Result<AtlasJanusVertex, AtlasJanusEdge>>() {

                @Override
                public Result<AtlasJanusVertex, AtlasJanusEdge> apply(JanusGraphIndexQuery.Result<JanusGraphVertex> source) {
                    return new ResultImpl(source);
                }
            };

        return Iterators.transform(results, function);
    }

    @Override
    public Iterator<Result<AtlasJanusVertex, AtlasJanusEdge>> vertices(int offset, int limit) {
        Preconditions.checkArgument(offset >=0, "Index offset should be greater than or equals to 0");
        Preconditions.checkArgument(limit >=0, "Index limit should be greater than or equals to 0");
        Iterator<JanusGraphIndexQuery.Result<JanusGraphVertex>> results = query
                .offset(offset)
                .limit(limit)
                .vertices().iterator();

        Function<JanusGraphIndexQuery.Result<JanusGraphVertex>, Result<AtlasJanusVertex, AtlasJanusEdge>> function =
                new Function<JanusGraphIndexQuery.Result<JanusGraphVertex>, Result<AtlasJanusVertex, AtlasJanusEdge>>() {

                    @Override
                    public Result<AtlasJanusVertex, AtlasJanusEdge> apply(JanusGraphIndexQuery.Result<JanusGraphVertex> source) {
                        return new ResultImpl(source);
                    }
                };

        return Iterators.transform(results, function);
    }

    @Override
    public Iterator<Result<AtlasJanusVertex, AtlasJanusEdge>> vertices(int offset, int limit, String sortBy, Order sortOrder) {
        Preconditions.checkArgument(offset >=0, "Index offset should be greater than or equals to 0");
        Preconditions.checkArgument(limit >=0, "Index limit should be greater than or equals to 0");

        Iterator<JanusGraphIndexQuery.Result<JanusGraphVertex>> results = query
                .orderBy(sortBy, sortOrder)
                .offset(offset)
                .limit(limit)
                .vertices().iterator();

        Function<JanusGraphIndexQuery.Result<JanusGraphVertex>, Result<AtlasJanusVertex, AtlasJanusEdge>> function =
                new Function<JanusGraphIndexQuery.Result<JanusGraphVertex>, Result<AtlasJanusVertex, AtlasJanusEdge>>() {

                    @Override
                    public Result<AtlasJanusVertex, AtlasJanusEdge> apply(JanusGraphIndexQuery.Result<JanusGraphVertex> source) {
                        return new ResultImpl(source);
                    }
                };

        return Iterators.transform(results, function);
    }

    @Override
    public Long vertexTotals() {
        return query.vertexTotals();
    }

    /**
     * Janus implementation of AtlasIndexQuery.Result.
     */
    public final class ResultImpl implements AtlasIndexQuery.Result<AtlasJanusVertex, AtlasJanusEdge> {
        private JanusGraphIndexQuery.Result<JanusGraphVertex> source;

        public ResultImpl(JanusGraphIndexQuery.Result<JanusGraphVertex> source) {
            this.source = source;
        }

        @Override
        public AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> getVertex() {
            return GraphDbObjectFactory.createVertex(graph, source.getElement());
        }

        @Override
        public double getScore() {
            return source.getScore();
        }
    }
}
