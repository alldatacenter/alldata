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
package org.apache.atlas.query.executors;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AttributeSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AtlasQueryType;
import org.apache.atlas.query.AtlasDSL;
import org.apache.atlas.query.GremlinQuery;
import org.apache.atlas.query.QueryParams;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScriptEngineBasedExecutor implements DSLQueryExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptEngineBasedExecutor.class);

    private final AtlasTypeRegistry     typeRegistry;
    private final AtlasGraph            graph;
    private final EntityGraphRetriever  entityRetriever;

    public ScriptEngineBasedExecutor(AtlasTypeRegistry typeRegistry, AtlasGraph graph, EntityGraphRetriever entityRetriever) {
        this.typeRegistry    = typeRegistry;
        this.graph           = graph;
        this.entityRetriever = entityRetriever;
    }

    @Override
    public AtlasSearchResult execute(String dslQuery, int limit, int offset) throws AtlasBaseException {
        AtlasSearchResult ret          = new AtlasSearchResult(dslQuery, AtlasQueryType.DSL);
        GremlinQuery      gremlinQuery = toGremlinQuery(dslQuery, limit, offset);
        String            queryStr     = gremlinQuery.queryStr();
        Object            result       = graph.executeGremlinScript(queryStr, false);

        if (result instanceof List && CollectionUtils.isNotEmpty((List)result)) {
            List   queryResult  = (List) result;
            Object firstElement = queryResult.get(0);

            if (firstElement instanceof AtlasVertex) {
                for (Object element : queryResult) {
                    if (element instanceof AtlasVertex) {
                        ret.addEntity(entityRetriever.toAtlasEntityHeaderWithClassifications((AtlasVertex)element));
                    } else {
                        LOG.warn("searchUsingDslQuery({}): expected an AtlasVertex; found unexpected entry in result {}", dslQuery, element);
                    }
                }
            } else if (gremlinQuery.hasSelectList()) {
                ret.setAttributes(toAttributesResult(queryResult, gremlinQuery));
            } else if (firstElement instanceof Map) {
                for (Object element : queryResult) {
                    if (element instanceof Map) {
                        Map map = (Map)element;

                        for (Object key : map.keySet()) {
                            Object value = map.get(key);

                            if (value instanceof List && CollectionUtils.isNotEmpty((List)value)) {
                                for (Object o : (List) value) {
                                    Object entry = o;

                                    if (entry instanceof AtlasVertex) {
                                        ret.addEntity(entityRetriever.toAtlasEntityHeader((AtlasVertex) entry));
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LOG.warn("searchUsingDslQuery({}/{}): found unexpected entry in result {}", dslQuery, dslQuery, gremlinQuery.queryStr());
            }
        }

        return ret;
    }

    private GremlinQuery toGremlinQuery(String query, int limit, int offset) throws AtlasBaseException {
        QueryParams  params       = QueryParams.getNormalizedParams(limit, offset);
        GremlinQuery gremlinQuery = new AtlasDSL.Translator(query, typeRegistry, params.offset(), params.limit()).translate();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Translated Gremlin Query: {}", gremlinQuery.queryStr());
        }

        return gremlinQuery;
    }

    private AttributeSearchResult toAttributesResult(List results, GremlinQuery query) {
        AttributeSearchResult ret    = new AttributeSearchResult();
        List<String>          names  = (List<String>) results.get(0);
        List<List<Object>>    values = extractValues(results.subList(1, results.size()));

        ret.setName(names);
        ret.setValues(values);

        return ret;
    }

    private List<List<Object>> extractValues(List results) {
        List<List<Object>> values = new ArrayList<>();

        for (Object obj : results) {
            if (obj instanceof Map) {
                Map          map  = (Map) obj;
                List<Object> list = new ArrayList<>();

                if (MapUtils.isNotEmpty(map)) {
                    for (Object key : map.keySet()) {
                        Object vals = map.get(key);

                        if(vals instanceof List) {
                            List l = (List) vals;

                            list.addAll(l);
                        }
                    }

                    values.add(list);
                }
            } else if (obj instanceof List) {
                List list = (List) obj;

                if (CollectionUtils.isNotEmpty(list)) {
                    values.add(list);
                }
            }
        }

        return values;
    }
}
