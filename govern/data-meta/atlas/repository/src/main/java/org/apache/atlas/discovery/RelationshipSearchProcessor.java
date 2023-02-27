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

import org.apache.atlas.SortOrder;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasIndexQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasRelationshipType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.util.SearchPredicateUtil;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.atlas.repository.Constants.RELATIONSHIP_TYPE_PROPERTY_KEY;
import static org.apache.atlas.repository.graphdb.AtlasGraphQuery.SortOrder.ASC;
import static org.apache.atlas.repository.graphdb.AtlasGraphQuery.SortOrder.DESC;

public class RelationshipSearchProcessor extends SearchProcessor {
    private static final Logger LOG      = LoggerFactory.getLogger(RelationshipSearchProcessor.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("RelationshipSearchProcessor");

    private final AtlasIndexQuery indexQuery;
    private final AtlasGraphQuery graphQuery;

    RelationshipSearchProcessor(SearchContext context, Set<String> indexedKeys) {
        super(context);
        context.setEdgeIndexKeys(indexedKeys);

        final Set<AtlasRelationshipType> types               = context.getRelationshipTypes();
        final SearchParameters.FilterCriteria filterCriteria = context.getSearchParameters().getRelationshipFilters();
        final Set<String> indexAttributes                    = new HashSet<>();
        final Set<String> graphAttributes                    = new HashSet<>();
        final Set<String> allAttributes                      = new HashSet<>();
        final Set<String> typeNames                          = CollectionUtils.isNotEmpty(types) ? types.stream().map(AtlasRelationshipType::getTypeName).collect(Collectors.toSet()) : null;
        final String      typeAndSubTypesQryStr              = AtlasStructType.AtlasAttribute.escapeIndexQueryValue(typeNames, true);
        final Predicate   typeNamePredicate                  = SearchPredicateUtil.generateIsRelationshipEdgePredicate(context.getTypeRegistry());
        final String      sortBy                             = context.getSearchParameters().getSortBy();
        final SortOrder   sortOrder                          = context.getSearchParameters().getSortOrder();
        inMemoryPredicate = typeNamePredicate;

        processSearchAttributes(types, filterCriteria, indexAttributes, graphAttributes, allAttributes);

        final boolean typeSearchByIndex = typeAndSubTypesQryStr.length() <= MAX_QUERY_STR_LENGTH_TYPES;
        final boolean attrSearchByIndex = CollectionUtils.isNotEmpty(indexAttributes) && canApplyIndexFilter(types, filterCriteria, false);

        //index query
        StringBuilder indexQuery = new StringBuilder();

        if (typeSearchByIndex) {
            graphIndexQueryBuilder.addTypeAndSubTypesQueryFilterEdges(indexQuery, typeAndSubTypesQryStr);
        }

        if (attrSearchByIndex) {
            constructFilterQuery(indexQuery, types, filterCriteria, indexAttributes);

            Predicate attributePredicate = constructInMemoryPredicate(types, filterCriteria, indexAttributes);
            if (attributePredicate != null) {
                inMemoryPredicate = PredicateUtils.andPredicate(inMemoryPredicate, attributePredicate);
            }
        } else {
            graphAttributes.addAll(indexAttributes);
        }

        if (indexQuery.length() > 0) {

            String indexQueryString = STRAY_AND_PATTERN.matcher(indexQuery).replaceAll(")");
                   indexQueryString = STRAY_OR_PATTERN.matcher(indexQueryString).replaceAll(")");
                   indexQueryString = STRAY_ELIPSIS_PATTERN.matcher(indexQueryString).replaceAll("");

            this.indexQuery = context.getGraph().indexQuery(Constants.EDGE_INDEX, indexQueryString);
        } else {
            this.indexQuery = null;
        }

        //graph query
        if (CollectionUtils.isNotEmpty(graphAttributes) || !typeSearchByIndex) {
            AtlasGraphQuery query = context.getGraph().query();

            if (!typeSearchByIndex) {
                query.in(RELATIONSHIP_TYPE_PROPERTY_KEY, types);
            }
            graphQuery = toGraphFilterQuery(types, filterCriteria, graphAttributes, query);

            Predicate attributePredicate = constructInMemoryPredicate(types, filterCriteria, graphAttributes);
            if (attributePredicate != null) {
                inMemoryPredicate = PredicateUtils.andPredicate(inMemoryPredicate, attributePredicate);
            }

            if (StringUtils.isNotEmpty(sortBy)) {
                final AtlasRelationshipType relationshipType   = types.iterator().next();
                AtlasStructType.AtlasAttribute sortByAttribute = relationshipType.getAttribute(sortBy);

                if (sortByAttribute != null && StringUtils.isNotEmpty(sortByAttribute.getVertexPropertyName())) {
                    AtlasGraphQuery.SortOrder qrySortOrder = sortOrder == SortOrder.ASCENDING ? ASC : DESC;

                    graphQuery.orderBy(sortByAttribute.getVertexPropertyName(), qrySortOrder);
                }
            }
        } else {
            graphQuery = null;
        }

    }

    @Override
    public List<AtlasVertex> execute() {
        return null;
    }

    public List<AtlasEdge> executeEdges() {
        List<AtlasEdge> ret = new ArrayList<>();

        AtlasPerfTracer perf = null;
        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "RelationshipSearchProcessor.execute(" + context + ")");
        }

        try {
            final int limit      = context.getSearchParameters().getLimit();
            final Integer marker = context.getMarker();
            final int startIdx   = marker != null ? marker : context.getSearchParameters().getOffset();

            // when subsequent filtering stages are involved, query should start at 0 even though startIdx can be higher
            // first 'startIdx' number of entries will be ignored
            // if marker is provided, start query with marker offset
            int qryOffset;
            if (marker != null) {
                qryOffset = marker;
            } else {
                qryOffset = (graphQuery != null && indexQuery != null) ? 0 : startIdx;
            }
            int resultIdx = qryOffset;

            LinkedHashMap<Integer, AtlasEdge> offsetEdgeMap = new LinkedHashMap<>();

            for (; ret.size() < limit; qryOffset += limit) {
                offsetEdgeMap.clear();

                if (context.terminateSearch()) {
                    LOG.warn("query terminated: {}", context.getSearchParameters());

                    break;
                }

                final boolean isLastResultPage;

                if (indexQuery != null) {
                    Iterator<AtlasIndexQuery.Result> idxQueryResult = executeIndexQueryForEdge(context, indexQuery, qryOffset, limit);
                    offsetEdgeMap = getEdgesFromIndexQueryResult(idxQueryResult, offsetEdgeMap, qryOffset);

                } else {
                    Iterator<AtlasEdge> queryResult = graphQuery.edges(qryOffset, limit).iterator();
                    offsetEdgeMap = getEdges(queryResult, offsetEdgeMap, qryOffset);
                }

                isLastResultPage = offsetEdgeMap.size() < limit;

                // Do in-memory filtering
                offsetEdgeMap = offsetEdgeMap.entrySet()
                        .stream()
                        .filter(x -> inMemoryPredicate.evaluate(x.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));

                resultIdx = collectResultEdges(ret, startIdx, limit, resultIdx, offsetEdgeMap, marker);

                if (isLastResultPage) {
                    resultIdx = SearchContext.MarkerUtil.MARKER_END - 1;
                    break;
                }
            }

            if (marker != null) {
                nextOffset = resultIdx + 1;
            }

        } finally {
            AtlasPerfTracer.log(perf);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RelationshipSearchProcessor.execute({}): ret.size()={}", context, ret.size());
        }
        return ret;
    }

    @Override
    public long getResultCount() {
        if (indexQuery != null) {
            return indexQuery.edgeTotals();
        } else {
            return -1L;
        }
    }
}
