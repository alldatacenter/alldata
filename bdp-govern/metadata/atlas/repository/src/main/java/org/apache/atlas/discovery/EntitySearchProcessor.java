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
import org.apache.atlas.model.discovery.SearchParameters.FilterCriteria;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasIndexQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.util.SearchPredicateUtil;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.StreamSupport;

import static org.apache.atlas.SortOrder.ASCENDING;
import static org.apache.atlas.discovery.SearchContext.*;
import static org.apache.atlas.repository.Constants.*;
import static org.apache.atlas.repository.graphdb.AtlasGraphQuery.ComparisionOperator.EQUAL;
import static org.apache.atlas.repository.graphdb.AtlasGraphQuery.ComparisionOperator.NOT_EQUAL;
import static org.apache.atlas.repository.graphdb.AtlasGraphQuery.SortOrder.ASC;
import static org.apache.atlas.repository.graphdb.AtlasGraphQuery.SortOrder.DESC;

public class EntitySearchProcessor extends SearchProcessor {
    private static final Logger LOG      = LoggerFactory.getLogger(EntitySearchProcessor.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("EntitySearchProcessor");

    private final AtlasIndexQuery indexQuery;
    private final AtlasGraphQuery graphQuery;
    private       Predicate       graphQueryPredicate;
    private       Predicate       filterGraphQueryPredicate;

    public EntitySearchProcessor(SearchContext context) {
        super(context);

        final Set<AtlasEntityType> entityTypes      = context.getEntityTypes();
        final FilterCriteria  filterCriteria  = context.getSearchParameters().getEntityFilters();
        final Set<String>     indexAttributes = new HashSet<>();
        final Set<String>     graphAttributes = new HashSet<>();
        final Set<String>     allAttributes   = new HashSet<>();
        final Set<String>     typeAndSubTypes       = context.getEntityTypeNames();
        final String          typeAndSubTypesQryStr = context.getEntityTypesQryStr();
        final String          sortBy                = context.getSearchParameters().getSortBy();
        final SortOrder       sortOrder             = context.getSearchParameters().getSortOrder();

        final Set<AtlasClassificationType> classificationTypes           = context.getClassificationTypes();
        final Set<String>                  classificationTypeAndSubTypes = context.getClassificationTypeNames();
        final boolean                      filterClassification;

        if (CollectionUtils.isNotEmpty(classificationTypes)) {
            filterClassification = !context.needClassificationProcessor();
        } else {
            filterClassification = false;
        }

        final Predicate typeNamePredicate;
        final Predicate traitPredicate    = buildTraitPredict(classificationTypes);
        final Predicate activePredicate   = SearchPredicateUtil.getEQPredicateGenerator()
                                                               .generatePredicate(Constants.STATE_PROPERTY_KEY, "ACTIVE", String.class);


        if (!isEntityRootType()) {
            typeNamePredicate = SearchPredicateUtil.getINPredicateGenerator().generatePredicate(TYPE_NAME_PROPERTY_KEY, typeAndSubTypes, String.class);
        } else {
            typeNamePredicate = SearchPredicateUtil.generateIsEntityVertexPredicate(context.getTypeRegistry());
        }

        processSearchAttributes(entityTypes, filterCriteria, indexAttributes, graphAttributes, allAttributes);

        final boolean typeSearchByIndex = !filterClassification && typeAndSubTypesQryStr.length() <= MAX_QUERY_STR_LENGTH_TYPES;
        final boolean attrSearchByIndex = !filterClassification && CollectionUtils.isNotEmpty(indexAttributes) && canApplyIndexFilter(entityTypes, filterCriteria, false);

        StringBuilder indexQuery = new StringBuilder();

        // TypeName check to be done in-memory as well to address ATLAS-2121 (case sensitivity)
        inMemoryPredicate = typeNamePredicate;

        if (typeSearchByIndex) {
            graphIndexQueryBuilder.addTypeAndSubTypesQueryFilter(indexQuery, typeAndSubTypesQryStr);
        }

        if (attrSearchByIndex) {
            constructFilterQuery(indexQuery, entityTypes, filterCriteria, indexAttributes);

            Predicate attributePredicate = constructInMemoryPredicate(entityTypes, filterCriteria, indexAttributes);
            if (attributePredicate != null) {
                inMemoryPredicate = PredicateUtils.andPredicate(inMemoryPredicate, attributePredicate);
            }
        } else {
            graphAttributes.addAll(indexAttributes);
        }

        if (indexQuery.length() > 0) {

            graphIndexQueryBuilder.addActiveStateQueryFilter(indexQuery);

            String indexQueryString = STRAY_AND_PATTERN.matcher(indexQuery).replaceAll(")");

            indexQueryString = STRAY_OR_PATTERN.matcher(indexQueryString).replaceAll(")");
            indexQueryString = STRAY_ELIPSIS_PATTERN.matcher(indexQueryString).replaceAll("");

            this.indexQuery = context.getGraph().indexQuery(Constants.VERTEX_INDEX, indexQueryString);
        } else {
            this.indexQuery = null;
        }

        if (CollectionUtils.isNotEmpty(graphAttributes) || !typeSearchByIndex) {
            AtlasGraphQuery query = context.getGraph().query();

            if (!typeSearchByIndex && !isEntityRootType()) {
                query.in(TYPE_NAME_PROPERTY_KEY, typeAndSubTypes);
            }

            // If we need to filter on the trait names then we need to build the query and equivalent in-memory predicate
            if (filterClassification) {
                AtlasClassificationType classificationType = classificationTypes.iterator().next();
                List<AtlasGraphQuery> orConditions = new LinkedList<>();

                if (classificationType == MATCH_ALL_WILDCARD_CLASSIFICATION || classificationType == MATCH_ALL_CLASSIFIED || classificationType == MATCH_ALL_CLASSIFICATION_TYPES) {
                    orConditions.add(query.createChildQuery().has(TRAIT_NAMES_PROPERTY_KEY, NOT_EQUAL, null));
                    orConditions.add(query.createChildQuery().has(PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, NOT_EQUAL, null));
                } else if (classificationType == MATCH_ALL_NOT_CLASSIFIED) {
                    orConditions.add(query.createChildQuery().has(TRAIT_NAMES_PROPERTY_KEY, EQUAL, null)
                                                             .has(PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, EQUAL, null));
                } else {
                    orConditions.add(query.createChildQuery().in(TRAIT_NAMES_PROPERTY_KEY, classificationTypeAndSubTypes));
                    orConditions.add(query.createChildQuery().in(PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, classificationTypeAndSubTypes));
                }

                query.or(orConditions);

                // Construct a parallel in-memory predicate
                if (isEntityRootType()) {
                    inMemoryPredicate = typeNamePredicate;
                }

                if (graphQueryPredicate != null) {
                    graphQueryPredicate = PredicateUtils.andPredicate(graphQueryPredicate, traitPredicate);
                } else {
                    graphQueryPredicate = traitPredicate;
                }
            }

            graphQuery = toGraphFilterQuery(entityTypes, filterCriteria, graphAttributes, query);

            // Prepare in-memory predicate for attribute filtering
            Predicate attributePredicate = constructInMemoryPredicate(entityTypes, filterCriteria, graphAttributes);

            if (attributePredicate != null) {
                if (graphQueryPredicate != null) {
                    graphQueryPredicate = PredicateUtils.andPredicate(graphQueryPredicate, attributePredicate);
                } else {
                    graphQueryPredicate = attributePredicate;
                }
            }

            // Filter condition for the STATUS
            if (context.getSearchParameters().getExcludeDeletedEntities() && this.indexQuery == null) {
                graphQuery.has(Constants.STATE_PROPERTY_KEY, "ACTIVE");
                if (graphQueryPredicate != null) {
                    graphQueryPredicate = PredicateUtils.andPredicate(graphQueryPredicate, activePredicate);
                } else {
                    graphQueryPredicate = activePredicate;
                }
            }
            if (sortBy != null && !sortBy.isEmpty()) {
                final AtlasEntityType entityType = context.getEntityTypes().iterator().next();
                AtlasAttribute sortByAttribute = entityType.getAttribute(sortBy);

                if (sortByAttribute != null) {
                    AtlasGraphQuery.SortOrder qrySortOrder = sortOrder == SortOrder.ASCENDING ? ASC : DESC;

                    graphQuery.orderBy(sortByAttribute.getVertexPropertyName(), qrySortOrder);
                }
            }
        } else {
            graphQuery = null;
            graphQueryPredicate = null;
        }

        // Prepare the graph query and in-memory filter for the filtering phase
        filterGraphQueryPredicate = typeNamePredicate;

        Predicate attributesPredicate = constructInMemoryPredicate(entityTypes, filterCriteria, allAttributes);

        if (attributesPredicate != null) {
            filterGraphQueryPredicate = filterGraphQueryPredicate == null ? attributesPredicate :
                                        PredicateUtils.andPredicate(filterGraphQueryPredicate, attributesPredicate);
        }

        if (filterClassification) {
            filterGraphQueryPredicate = filterGraphQueryPredicate == null ? traitPredicate :
                                        PredicateUtils.andPredicate(filterGraphQueryPredicate, traitPredicate);
        }

        // Filter condition for the STATUS
        if (context.getSearchParameters().getExcludeDeletedEntities()) {
            filterGraphQueryPredicate = filterGraphQueryPredicate == null ? activePredicate :
                                        PredicateUtils.andPredicate(filterGraphQueryPredicate, activePredicate);
        }

    }

    @Override
    public List<AtlasVertex> execute() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntitySearchProcessor.execute({})", context);
        }

        List<AtlasVertex> ret = new ArrayList<>();

        AtlasPerfTracer perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntitySearchProcessor.execute(" + context +  ")");
        }

        try {
            final int limit       = context.getSearchParameters().getLimit();
            final Integer marker  = context.getMarker();
            final int startIdx    = marker != null ? marker : context.getSearchParameters().getOffset();

            // when subsequent filtering stages are involved, query should start at 0 even though startIdx can be higher
            //
            // first 'startIdx' number of entries will be ignored
            // if marker is provided, start query with marker offset
            int qryOffset;
            if (marker != null) {
                qryOffset = marker;
            } else {
                qryOffset = (nextProcessor != null || (graphQuery != null && indexQuery != null)) ? 0 : startIdx;
            }
            int resultIdx = qryOffset;

            LinkedHashMap<Integer, AtlasVertex> offsetEntityVertexMap = new LinkedHashMap<>();

            for (; ret.size() < limit; qryOffset += limit) {
                offsetEntityVertexMap.clear();

                if (context.terminateSearch()) {
                    LOG.warn("query terminated: {}", context.getSearchParameters());

                    break;
                }

                final boolean isLastResultPage;

                if (indexQuery != null) {
                    Iterator<AtlasIndexQuery.Result> idxQueryResult = executeIndexQuery(context, indexQuery, qryOffset, limit);
                    offsetEntityVertexMap = getVerticesFromIndexQueryResult(idxQueryResult, offsetEntityVertexMap, qryOffset);

                } else {
                    Iterator<AtlasVertex> queryResult = graphQuery.vertices(qryOffset, limit).iterator();
                    offsetEntityVertexMap = getVertices(queryResult, offsetEntityVertexMap, qryOffset);
                }

                isLastResultPage = offsetEntityVertexMap.size() < limit;
                // Do in-memory filtering
                offsetEntityVertexMap = super.filter(offsetEntityVertexMap, inMemoryPredicate);

                //incase when operator is NEQ in pipeSeperatedSystemAttributes
                if (graphQueryPredicate != null) {
                    offsetEntityVertexMap = super.filter(offsetEntityVertexMap, graphQueryPredicate);
                }

                offsetEntityVertexMap = super.filter(offsetEntityVertexMap);

                resultIdx = collectResultVertices(ret, startIdx, limit, resultIdx, offsetEntityVertexMap, marker);

                if (isLastResultPage) {
                    resultIdx = MarkerUtil.MARKER_END - 1;
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
            LOG.debug("<== EntitySearchProcessor.execute({}): ret.size()={}", context, ret.size());
        }

        return ret;
    }

    @Override
    public LinkedHashMap<Integer, AtlasVertex> filter(LinkedHashMap<Integer,AtlasVertex> offsetEntityVertexMap) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntitySearchProcessor.filter({})", offsetEntityVertexMap.size());
        }

        // Since we already have the entity vertices, a in-memory filter will be faster than fetching the same
        // vertices again with the required filtering
        if (filterGraphQueryPredicate != null) {
            LOG.debug("Filtering in-memory");
            offsetEntityVertexMap = super.filter(offsetEntityVertexMap, filterGraphQueryPredicate);
        }

        offsetEntityVertexMap = super.filter(offsetEntityVertexMap);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== EntitySearchProcessor.filter(): ret.size()={}", offsetEntityVertexMap.size());
        }

        return offsetEntityVertexMap;
    }

    @Override
    public long getResultCount() {
        if (indexQuery != null) {
            return indexQuery.vertexTotals();
        } else if (graphQuery != null) {
            return StreamSupport.stream(graphQuery.vertexIds().spliterator(), false).count();
        } else {
            return -1L;
        }
    }
}
