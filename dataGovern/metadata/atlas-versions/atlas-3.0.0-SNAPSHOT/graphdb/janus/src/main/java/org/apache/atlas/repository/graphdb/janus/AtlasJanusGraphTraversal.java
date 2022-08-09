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
package org.apache.atlas.repository.graphdb.janus;

import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphTraversal;
import org.apache.commons.collections.CollectionUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.attribute.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public class AtlasJanusGraphTraversal extends AtlasGraphTraversal<AtlasJanusVertex, AtlasJanusEdge> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasJanusGraphTraversal.class);

    private List resultList;
    private Set  resultSet;

    public AtlasJanusGraphTraversal() {
    }

    public AtlasJanusGraphTraversal(final AtlasGraph<AtlasJanusVertex, AtlasJanusEdge> atlasGraph,
                                    final GraphTraversalSource traversalSource) {
        super(atlasGraph, traversalSource);
    }

    public AtlasJanusGraphTraversal(final AtlasGraph atlasGraph, final Graph graph) {
        super(atlasGraph, graph);
    }

    @Override
    public AtlasGraphTraversal startAnonymousTraversal() {
        return new AtlasJanusGraphTraversal();
    }

    @Override
    public List<AtlasJanusVertex> getAtlasVertexList() {
        List                   list = getResultList();
        List<AtlasJanusVertex> ret;

        if (CollectionUtils.isNotEmpty(list)) {
            // toList called after groupBy will return a single map element list
            if (list.size() == 1 && list.get(0) instanceof Map) {
                ret = Collections.emptyList();
            } else {
                ret = new ArrayList<>(list.size());
                for (Object o : list) {
                    if (o instanceof Vertex) {
                        ret.add(GraphDbObjectFactory.createVertex((AtlasJanusGraph) atlasGraph, (Vertex) o));
                    }
                }
            }
        } else {
            ret = Collections.emptyList();
        }

        return ret;
    }

    @Override
    public Set<AtlasJanusVertex> getAtlasVertexSet() {
        Set                   set = getResultSet();
        Set<AtlasJanusVertex> ret;

        if (CollectionUtils.isNotEmpty(set)) {
            ret = new HashSet<>(set.size());
            for (Object o : set) {
                if (o instanceof Vertex) {
                    ret.add(GraphDbObjectFactory.createVertex((AtlasJanusGraph) atlasGraph, (Vertex) o));
                }
            }
        } else {
            ret = Collections.emptySet();
        }

        return ret;
    }

    @Override
    public Map<String, Collection<AtlasJanusVertex>> getAtlasVertexMap() {
        List                                      list = getResultList();
        Map<String, Collection<AtlasJanusVertex>> ret;

        if (CollectionUtils.isNotEmpty(list) && (list.get(0) instanceof Map)) {
            Map map = (Map) list.get(0);

            ret = new HashMap<>(map.size());

            for (Object key : map.keySet()) {
                if (!(key instanceof String)) {
                    continue;
                }

                Object value = map.get(key);

                if (value instanceof List) {
                    Collection<AtlasJanusVertex> values = new ArrayList<>();

                    for (Object o : (List) value) {
                        if (o instanceof Vertex) {
                            values.add(GraphDbObjectFactory.createVertex((AtlasJanusGraph) atlasGraph, (Vertex) o));
                        } else {
                            LOG.warn("{} is not a vertex.", o.getClass().getSimpleName());
                        }
                    }

                    ret.put((String) key, values);
                }
            }
        } else {
            ret = Collections.emptyMap();
        }

        return ret;
    }

    @Override
    public Set<AtlasJanusEdge> getAtlasEdgeSet() {
        Set                 set = getResultSet();
        Set<AtlasJanusEdge> ret;

        if (CollectionUtils.isNotEmpty(set)) {
            ret = new HashSet<>(set.size());
            for (Object o : set) {
                if (o instanceof Edge) {
                    ret.add(GraphDbObjectFactory.createEdge((AtlasJanusGraph) atlasGraph, (Edge) o));
                }
            }
        } else {
            ret = Collections.emptySet();
        }

        return ret;
    }

    @Override
    public Map<String, AtlasJanusEdge> getAtlasEdgeMap() {
        return null;
    }

    @Override
    public TextPredicate textPredicate() {
        return new JanusGraphPredicate();
    }

    @Override
    public AtlasGraphTraversal textRegEx(String key, String value) {
        return (AtlasGraphTraversal) this.has(key, Text.textRegex(value));
    }

    @Override
    public AtlasGraphTraversal textContainsRegEx(String key, String value) {
        return (AtlasGraphTraversal) this.has(key, Text.textContainsRegex(value));
    }

    public static class JanusGraphPredicate implements TextPredicate {
        @Override
        public BiPredicate<Object, Object> contains() {
            return Text.CONTAINS;
        }

        @Override
        public BiPredicate<Object, Object> containsPrefix() {
            return Text.CONTAINS_PREFIX;
        }

        @Override
        public BiPredicate<Object, Object> containsRegex() {
            return Text.CONTAINS_REGEX;
        }

        @Override
        public BiPredicate<Object, Object> prefix() {
            return Text.PREFIX;
        }

        @Override
        public BiPredicate<Object, Object> regex() {
            return Text.REGEX;
        }
    }

    private List getResultList() {
        if (resultList == null) {
            resultList = toList();
        }
        return resultList;
    }

    private Set getResultSet() {
        if (resultSet == null) {
            resultSet = toSet();
        }
        return resultSet;
    }

}
