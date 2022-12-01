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
package org.apache.atlas.discovery;

import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TermSearchProcessor extends SearchProcessor {
    private static final Logger LOG      = LoggerFactory.getLogger(TermSearchProcessor.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("TermSearchProcessor");

    public static final String ATLAS_GLOSSARY_TERM_ENTITY_TYPE            = "AtlasGlossaryTerm";
    public static final String ATLAS_GLOSSARY_TERM_ATTR_QNAME             = "qualifiedName";
    public static final String ATLAS_GLOSSARY_TERM_ATTR_ASSIGNED_ENTITIES = "assignedEntities";
    public static final String ATLAS_GLOSSARY_TERM_ATTR_MEANINGS          = "meanings";

    final List<AtlasVertex> assignedEntities;

    public TermSearchProcessor(SearchContext context, List<AtlasVertex> assignedEntities) {
        super(context);

        this.assignedEntities = assignedEntities;
    }

    @Override
    public List<AtlasVertex> execute() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> TermSearchProcessor.execute({})", context);
        }

        List<AtlasVertex> ret  = new ArrayList<>();
        AtlasPerfTracer   perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "TermSearchProcessor.execute(" + context +  ")");
        }

        //marker functionality will not work when there is need to fetch Term vertices and get entities from it
        try {
            if (CollectionUtils.isNotEmpty(assignedEntities)) {
                LinkedHashMap<Integer, AtlasVertex> offsetEntityVertexMap = new LinkedHashMap<>();

                final int               startIdx = context.getSearchParameters().getOffset();
                final int               limit    = context.getSearchParameters().getLimit();
                final List<AtlasVertex> tmpList  = new ArrayList<>(assignedEntities);

                for (int i = 0; i < tmpList.size(); i++) {
                    offsetEntityVertexMap.put(i, tmpList.get(i));
                }

                offsetEntityVertexMap = super.filter(offsetEntityVertexMap);

                collectResultVertices(ret, startIdx, limit, 0, offsetEntityVertexMap, null);
            }
        } finally {
            AtlasPerfTracer.log(perf);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== TermSearchProcessor.execute({}): ret.size()={}", context, ret.size());
        }

        return ret;
    }

    //this filter is never used
    @Override
    public LinkedHashMap<Integer, AtlasVertex> filter(LinkedHashMap<Integer, AtlasVertex> offsetEntityVertexMap) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> TermSearchProcessor.filter({})", offsetEntityVertexMap.size());
        }

        if (MapUtils.isNotEmpty(offsetEntityVertexMap)) {
            if (CollectionUtils.isEmpty(assignedEntities)) {
                offsetEntityVertexMap.clear();
            } else {
                offsetEntityVertexMap.entrySet().stream().
                        filter(o -> {
                            if (o instanceof AtlasVertex) {
                                AtlasVertex entityVertex = (AtlasVertex) o;

                                for (AtlasVertex assignedEntity : assignedEntities) {
                                    if (assignedEntity.getId().equals(entityVertex.getId())) {
                                        return true;
                                    }
                                }
                            }

                    return false;
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
            }
        }

        offsetEntityVertexMap = super.filter(offsetEntityVertexMap);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== TermSearchProcessor.filter(): ret.size()={}", offsetEntityVertexMap.size());
        }

        return offsetEntityVertexMap;
    }

    @Override
    public long getResultCount() {
        return assignedEntities != null ? assignedEntities.size() : -1;
    }
}
