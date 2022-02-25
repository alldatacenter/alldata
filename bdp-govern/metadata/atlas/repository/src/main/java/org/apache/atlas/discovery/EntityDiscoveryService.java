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

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.SortOrder;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.authorize.AtlasSearchResultScrubRequest;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasAggregationEntry;
import org.apache.atlas.model.discovery.AtlasQuickSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AtlasFullTextResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AtlasQueryType;
import org.apache.atlas.model.discovery.AtlasSuggestionsResult;
import org.apache.atlas.model.discovery.QuickSearchParameters;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.profile.AtlasUserSavedSearch;
import org.apache.atlas.query.QueryParams;
import org.apache.atlas.query.executors.DSLQueryExecutor;
import org.apache.atlas.query.executors.ScriptEngineBasedExecutor;
import org.apache.atlas.query.executors.TraversalBasedExecutor;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasIndexQuery;
import org.apache.atlas.repository.graphdb.AtlasIndexQuery.Result;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.repository.userprofile.UserProfileService;
import org.apache.atlas.type.AtlasArrayType;
import org.apache.atlas.type.AtlasBuiltInTypes.AtlasObjectIdType;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.util.AtlasGremlinQueryProvider;
import org.apache.atlas.util.AtlasGremlinQueryProvider.AtlasGremlinQuery;
import org.apache.atlas.util.SearchPredicateUtil;
import org.apache.atlas.util.SearchTracker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.AtlasErrorCode.*;
import static org.apache.atlas.SortOrder.ASCENDING;
import static org.apache.atlas.model.instance.AtlasEntity.Status.ACTIVE;
import static org.apache.atlas.model.instance.AtlasEntity.Status.DELETED;
import static org.apache.atlas.repository.Constants.ASSET_ENTITY_TYPE;
import static org.apache.atlas.repository.Constants.OWNER_ATTRIBUTE;
import static org.apache.atlas.util.AtlasGremlinQueryProvider.AtlasGremlinQuery.BASIC_SEARCH_STATE_FILTER;
import static org.apache.atlas.util.AtlasGremlinQueryProvider.AtlasGremlinQuery.TO_RANGE_LIST;

@Component
public class EntityDiscoveryService implements AtlasDiscoveryService {
    private static final Logger LOG = LoggerFactory.getLogger(EntityDiscoveryService.class);
    private static final String DEFAULT_SORT_ATTRIBUTE_NAME = "name";

    private final AtlasGraph                      graph;
    private final EntityGraphRetriever            entityRetriever;
    private final AtlasGremlinQueryProvider       gremlinQueryProvider;
    private final AtlasTypeRegistry               typeRegistry;
    private final GraphBackedSearchIndexer        indexer;
    private final SearchTracker                   searchTracker;
    private final int                             maxResultSetSize;
    private final int                             maxTypesLengthInIdxQuery;
    private final int                             maxTagsLengthInIdxQuery;
    private final String                          indexSearchPrefix;
    private final UserProfileService              userProfileService;
    private final SuggestionsProvider             suggestionsProvider;
    private final DSLQueryExecutor                dslQueryExecutor;

    @Inject
    EntityDiscoveryService(AtlasTypeRegistry typeRegistry,
                           AtlasGraph graph,
                           GraphBackedSearchIndexer indexer,
                           SearchTracker searchTracker,
                           UserProfileService userProfileService) throws AtlasException {
        this.graph                    = graph;
        this.entityRetriever          = new EntityGraphRetriever(this.graph, typeRegistry);
        this.indexer                  = indexer;
        this.searchTracker            = searchTracker;
        this.gremlinQueryProvider     = AtlasGremlinQueryProvider.INSTANCE;
        this.typeRegistry             = typeRegistry;
        this.maxResultSetSize         = ApplicationProperties.get().getInt(Constants.INDEX_SEARCH_MAX_RESULT_SET_SIZE, 150);
        this.maxTypesLengthInIdxQuery = ApplicationProperties.get().getInt(Constants.INDEX_SEARCH_TYPES_MAX_QUERY_STR_LENGTH, 512);
        this.maxTagsLengthInIdxQuery  = ApplicationProperties.get().getInt(Constants.INDEX_SEARCH_TAGS_MAX_QUERY_STR_LENGTH, 512);
        this.indexSearchPrefix        = AtlasGraphUtilsV2.getIndexSearchPrefix();
        this.userProfileService       = userProfileService;
        this.suggestionsProvider      = new SuggestionsProviderImpl(graph, typeRegistry);
        this.dslQueryExecutor         = AtlasConfiguration.DSL_EXECUTOR_TRAVERSAL.getBoolean()
                                            ? new TraversalBasedExecutor(typeRegistry, graph, entityRetriever)
                                            : new ScriptEngineBasedExecutor(typeRegistry, graph, entityRetriever);
        LOG.info("DSL Executor: {}", this.dslQueryExecutor.getClass().getSimpleName());
    }

    @Override
    @GraphTransaction
    public AtlasSearchResult searchUsingDslQuery(String dslQuery, int limit, int offset) throws AtlasBaseException {
        AtlasSearchResult ret = dslQueryExecutor.execute(dslQuery, limit, offset);

        scrubSearchResults(ret);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasSearchResult searchUsingFullTextQuery(String fullTextQuery, boolean excludeDeletedEntities, int limit, int offset)
                                                      throws AtlasBaseException {
        AtlasSearchResult ret      = new AtlasSearchResult(fullTextQuery, AtlasQueryType.FULL_TEXT);
        QueryParams       params   = QueryParams.getNormalizedParams(limit, offset);
        AtlasIndexQuery   idxQuery = toAtlasIndexQuery(fullTextQuery);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing Full text query: {}", fullTextQuery);
        }
        ret.setFullTextResult(getIndexQueryResults(idxQuery, params, excludeDeletedEntities));
        ret.setApproximateCount(idxQuery.vertexTotals());

        scrubSearchResults(ret);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasSearchResult searchUsingBasicQuery(String query, String typeName, String classification, String attrName,
                                                   String attrValuePrefix, boolean excludeDeletedEntities, int limit,
                                                   int offset) throws AtlasBaseException {

        AtlasSearchResult ret = new AtlasSearchResult(AtlasQueryType.BASIC);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing basic search query: {} with type: {} and classification: {}", query, typeName, classification);
        }

        final QueryParams params              = QueryParams.getNormalizedParams(limit, offset);
        Set<String>       typeNames           = null;
        Set<String>       classificationNames = null;
        String            attrQualifiedName   = null;

        if (StringUtils.isNotEmpty(typeName)) {
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);

            if (entityType == null) {
                throw new AtlasBaseException(UNKNOWN_TYPENAME, typeName);
            }

            typeNames = entityType.getTypeAndAllSubTypes();

            ret.setType(typeName);
        }

        if (StringUtils.isNotEmpty(classification)) {
            AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(classification);

            if (classificationType == null) {
                throw new AtlasBaseException(CLASSIFICATION_NOT_FOUND, classification);
            }

            classificationNames = classificationType.getTypeAndAllSubTypes();

            ret.setClassification(classification);
        }

        boolean isAttributeSearch  = StringUtils.isNotEmpty(attrName) || StringUtils.isNotEmpty(attrValuePrefix);
        boolean isGuidPrefixSearch = false;

        if (isAttributeSearch) {
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);

            ret.setQueryType(AtlasQueryType.ATTRIBUTE);

            if (entityType != null) {
                AtlasAttribute attribute = null;

                if (StringUtils.isNotEmpty(attrName)) {
                    attribute = entityType.getAttribute(attrName);

                    if (attribute == null) {
                        throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_ATTRIBUTE, attrName, typeName);
                    }

                } else {
                    // if attrName is null|empty iterate defaultAttrNames to get attribute value
                    final List<String> defaultAttrNames = new ArrayList<>(Arrays.asList("qualifiedName", "name"));
                    Iterator<String>   iter             = defaultAttrNames.iterator();

                    while (iter.hasNext() && attribute == null) {
                        attrName  = iter.next();
                        attribute = entityType.getAttribute(attrName);
                    }
                }

                if (attribute == null) {
                    // for guid prefix search use gremlin and nullify query to avoid using fulltext
                    // (guids cannot be searched in fulltext)
                    isGuidPrefixSearch = true;
                    query              = null;

                } else {
                    attrQualifiedName = attribute.getQualifiedName();

                    String attrQuery = String.format("%s AND (%s *)", attrName, attrValuePrefix.replaceAll("\\.", " "));

                    query = StringUtils.isEmpty(query) ? attrQuery : String.format("(%s) AND (%s)", query, attrQuery);
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing attribute search attrName: {} and attrValue: {}", attrName, attrValuePrefix);
            }
        }

        // if query was provided, perform indexQuery and filter for typeName & classification in memory; this approach
        // results in a faster and accurate results than using CONTAINS/CONTAINS_PREFIX filter on entityText property
        if (StringUtils.isNotEmpty(query)) {
            final String idxQuery   = getQueryForFullTextSearch(query, typeName, classification);
            final int    startIdx   = params.offset();
            final int    resultSize = params.limit();
            int          resultIdx  = 0;

            for (int indexQueryOffset = 0; ; indexQueryOffset += getMaxResultSetSize()) {
                final AtlasIndexQuery        qry       = graph.indexQuery(Constants.FULLTEXT_INDEX, idxQuery, indexQueryOffset);
                final Iterator<Result<?, ?>> qryResult = qry.vertices();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("indexQuery: query=" + idxQuery + "; offset=" + indexQueryOffset);
                }

                if(!qryResult.hasNext()) {
                    break;
                }

                while (qryResult.hasNext()) {
                    AtlasVertex<?, ?> vertex         = qryResult.next().getVertex();
                    String            vertexTypeName = GraphHelper.getTypeName(vertex);

                    // skip non-entity vertices
                    if (StringUtils.isEmpty(vertexTypeName) || StringUtils.isEmpty(GraphHelper.getGuid(vertex))) {
                        continue;
                    }

                    if (typeNames != null && !typeNames.contains(vertexTypeName)) {
                        continue;
                    }

                    if (classificationNames != null) {
                        List<String> traitNames = GraphHelper.getTraitNames(vertex);

                        if (CollectionUtils.isEmpty(traitNames) ||
                                !CollectionUtils.containsAny(classificationNames, traitNames)) {
                            continue;
                        }
                    }

                    if (isAttributeSearch) {
                        String vertexAttrValue = vertex.getProperty(attrQualifiedName, String.class);

                        if (StringUtils.isNotEmpty(vertexAttrValue) && !vertexAttrValue.startsWith(attrValuePrefix)) {
                            continue;
                        }
                    }

                    if (skipDeletedEntities(excludeDeletedEntities, vertex)) {
                        continue;
                    }

                    resultIdx++;

                    if (resultIdx <= startIdx) {
                        continue;
                    }

                    AtlasEntityHeader header = entityRetriever.toAtlasEntityHeader(vertex);

                    ret.addEntity(header);

                    if (ret.getEntities().size() == resultSize) {
                        break;
                    }
                }

                if (ret.getApproximateCount() < 0) {
                    ret.setApproximateCount(qry.vertexTotals());
                }

                if (ret.getEntities() != null && ret.getEntities().size() == resultSize) {
                    break;
                }
            }
        } else {
            final Map<String, Object> bindings   = new HashMap<>();
            String                    basicQuery = "g.V()";

            if (classificationNames != null) {
                bindings.put("traitNames", classificationNames);

                basicQuery += gremlinQueryProvider.getQuery(AtlasGremlinQuery.BASIC_SEARCH_CLASSIFICATION_FILTER);
            }

            if (typeNames != null) {
                bindings.put("typeNames", typeNames);

                basicQuery += gremlinQueryProvider.getQuery(AtlasGremlinQuery.BASIC_SEARCH_TYPE_FILTER);
            }

            if (excludeDeletedEntities) {
                bindings.put("state", ACTIVE.toString());

                basicQuery += gremlinQueryProvider.getQuery(BASIC_SEARCH_STATE_FILTER);
            }

            if (isGuidPrefixSearch) {
                bindings.put("guid", attrValuePrefix + ".*");

                basicQuery += gremlinQueryProvider.getQuery(AtlasGremlinQuery.GUID_PREFIX_FILTER);
            }

            bindings.put("startIdx", params.offset());
            bindings.put("endIdx", params.offset() + params.limit());

            basicQuery += gremlinQueryProvider.getQuery(TO_RANGE_LIST);

            ScriptEngine scriptEngine = graph.getGremlinScriptEngine();

            try {
                Object result = graph.executeGremlinScript(scriptEngine, bindings, basicQuery, false);

                if (result instanceof List && CollectionUtils.isNotEmpty((List) result)) {
                    List queryResult = (List) result;
                    Object firstElement = queryResult.get(0);

                    if (firstElement instanceof AtlasVertex) {
                        for (Object element : queryResult) {
                            if (element instanceof AtlasVertex) {
                                ret.addEntity(entityRetriever.toAtlasEntityHeader((AtlasVertex) element));
                            } else {
                                LOG.warn("searchUsingBasicQuery({}): expected an AtlasVertex; found unexpected entry in result {}", basicQuery, element);
                            }
                        }
                    }
                }
            } catch (ScriptException e) {
                throw new AtlasBaseException(DISCOVERY_QUERY_FAILED, basicQuery);
            } finally {
                graph.releaseGremlinScriptEngine(scriptEngine);
            }
        }

        scrubSearchResults(ret);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasQuickSearchResult quickSearch(QuickSearchParameters quickSearchParameters) throws AtlasBaseException {
        String query = quickSearchParameters.getQuery();
        if (StringUtils.isNotEmpty(query) && !AtlasStructType.AtlasAttribute.hastokenizeChar(query)) {
                query = query + "*";
        }
        quickSearchParameters.setQuery(query);
        
        SearchContext searchContext = new SearchContext(createSearchParameters(quickSearchParameters),
                                                        typeRegistry,
                                                        graph,
                                                        indexer.getVertexIndexKeys());

        if(LOG.isDebugEnabled()) {
            LOG.debug("Generating the search results for the query {} .", searchContext.getSearchParameters().getQuery());
        }

        AtlasSearchResult searchResult = searchWithSearchContext(searchContext);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Generating the aggregated metrics for the query {} .", searchContext.getSearchParameters().getQuery());
        }

        // load the facet fields and attributes.
        Set<String>                              aggregationFields     = getAggregationFields();
        Set<AtlasAttribute>                      aggregationAttributes = getAggregationAtlasAttributes();
        SearchAggregator                         searchAggregator      = new SearchAggregatorImpl(searchContext);
        Map<String, List<AtlasAggregationEntry>> aggregatedMetrics     = searchAggregator.getAggregatedMetrics(aggregationFields, aggregationAttributes);
        AtlasQuickSearchResult                   ret                   = new AtlasQuickSearchResult(searchResult, aggregatedMetrics);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasSuggestionsResult getSuggestions(String prefixString, String fieldName) {
        return suggestionsProvider.getSuggestions(prefixString, fieldName);
    }

    @Override
    @GraphTransaction
    public AtlasSearchResult searchWithParameters(SearchParameters searchParameters) throws AtlasBaseException {
        return searchWithSearchContext(new SearchContext(searchParameters, typeRegistry, graph, indexer.getVertexIndexKeys()));
    }

    private AtlasSearchResult searchWithSearchContext(SearchContext searchContext) throws AtlasBaseException {
        SearchParameters  searchParameters = searchContext.getSearchParameters();
        AtlasSearchResult ret              = new AtlasSearchResult(searchParameters);
        final QueryParams params           = QueryParams.getNormalizedParams(searchParameters.getLimit(),searchParameters.getOffset());
        String            searchID         = searchTracker.add(searchContext); // For future cancellations

        searchParameters.setLimit(params.limit());
        searchParameters.setOffset(params.offset());

        try {
            List<AtlasVertex> resultList = searchContext.getSearchProcessor().execute();

            ret.setApproximateCount(searchContext.getSearchProcessor().getResultCount());

            String nextMarker = searchContext.getSearchProcessor().getNextMarker();
            if (StringUtils.isNotEmpty(nextMarker)) {
                ret.setNextMarker(nextMarker);
            }

            // By default any attribute that shows up in the search parameter should be sent back in the response
            // If additional values are requested then the entityAttributes will be a superset of the all search attributes
            // and the explicitly requested attribute(s)
            Set<String> resultAttributes = new HashSet<>();
            Set<String> entityAttributes = new HashSet<>();

            if (CollectionUtils.isNotEmpty(searchParameters.getAttributes())) {
                resultAttributes.addAll(searchParameters.getAttributes());
            }

            if (CollectionUtils.isNotEmpty(searchContext.getEntityAttributes())) {
                resultAttributes.addAll(searchContext.getEntityAttributes());
            }

            if (CollectionUtils.isNotEmpty(searchContext.getEntityTypes())) {

                AtlasEntityType entityType = searchContext.getEntityTypes().iterator().next();

               for (String resultAttribute : resultAttributes) {
                    AtlasAttribute  attribute  = entityType.getAttribute(resultAttribute);

                    if (attribute == null) {
                        attribute = entityType.getRelationshipAttribute(resultAttribute, null);
                    }

                    if (attribute != null) {
                        AtlasType attributeType = attribute.getAttributeType();

                        if (attributeType instanceof AtlasArrayType) {
                            attributeType = ((AtlasArrayType) attributeType).getElementType();
                        }

                        if (attributeType instanceof AtlasEntityType || attributeType instanceof AtlasObjectIdType) {
                            entityAttributes.add(resultAttribute);
                        }
                    }
                }
            }

            for (AtlasVertex atlasVertex : resultList) {
                AtlasEntityHeader entity = entityRetriever.toAtlasEntityHeader(atlasVertex, resultAttributes);

                if(searchParameters.getIncludeClassificationAttributes()) {
                    entity.setClassifications(entityRetriever.getAllClassifications(atlasVertex));
                }

                ret.addEntity(entity);

                // populate ret.referredEntities
                for (String entityAttribute : entityAttributes) {
                    Object attrValue = entity.getAttribute(entityAttribute);

                    if (attrValue instanceof AtlasObjectId) {
                        AtlasObjectId objId = (AtlasObjectId) attrValue;

                        if (ret.getReferredEntities() == null) {
                            ret.setReferredEntities(new HashMap<>());
                        }

                        if (!ret.getReferredEntities().containsKey(objId.getGuid())) {
                            ret.getReferredEntities().put(objId.getGuid(), entityRetriever.toAtlasEntityHeader(objId.getGuid()));
                        }
                    } else if (attrValue instanceof Collection) {
                        Collection objIds = (Collection) attrValue;

                        for (Object obj : objIds) {
                            if (obj instanceof AtlasObjectId) {
                                AtlasObjectId objId = (AtlasObjectId) obj;

                                if (ret.getReferredEntities() == null) {
                                    ret.setReferredEntities(new HashMap<>());
                                }

                                if (!ret.getReferredEntities().containsKey(objId.getGuid())) {
                                    ret.getReferredEntities().put(objId.getGuid(), entityRetriever.toAtlasEntityHeader(objId.getGuid()));
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            searchTracker.remove(searchID);
        }

        scrubSearchResults(ret);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasSearchResult searchRelatedEntities(String guid, String relation, boolean getApproximateCount, SearchParameters searchParameters) throws AtlasBaseException {
        AtlasSearchResult ret = new AtlasSearchResult(AtlasQueryType.RELATIONSHIP);

        if (StringUtils.isEmpty(guid) || StringUtils.isEmpty(relation)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "guid: '" + guid + "', relation: '" + relation + "'");
        }

        //validate entity
        AtlasVertex     entityVertex   = entityRetriever.getEntityVertex(guid);
        String          entityTypeName = GraphHelper.getTypeName(entityVertex);
        AtlasEntityType entityType     = typeRegistry.getEntityTypeByName(entityTypeName);

        if (entityType == null) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_RELATIONSHIP_TYPE, entityTypeName, guid);
        }

        //validate relation
        AtlasEntityType endEntityType = null;
        AtlasAttribute  attribute     = entityType.getAttribute(relation);

        if (attribute == null) {
            attribute = entityType.getRelationshipAttribute(relation, null);
        }

        if (attribute != null) {
            //get end entity type through relationship attribute
            endEntityType = attribute.getReferencedEntityType(typeRegistry);

            if (endEntityType == null) {
                throw new AtlasBaseException(AtlasErrorCode.INVALID_RELATIONSHIP_ATTRIBUTE, relation, attribute.getTypeName());
            }
            relation = attribute.getRelationshipEdgeLabel();
        } else {
            //get end entity type through label
            String endEntityTypeName = GraphHelper.getReferencedEntityTypeName(entityVertex, relation);

            if (StringUtils.isNotEmpty(endEntityTypeName)) {
                endEntityType = typeRegistry.getEntityTypeByName(endEntityTypeName);
            }
        }

        //validate sortBy attribute
        String    sortBy           = searchParameters.getSortBy();
        SortOrder sortOrder        = searchParameters.getSortOrder();
        int       offset           = searchParameters.getOffset();
        int       limit            = searchParameters.getLimit();
        String sortByAttributeName = DEFAULT_SORT_ATTRIBUTE_NAME;

        if (StringUtils.isNotEmpty(sortBy)) {
            sortByAttributeName = sortBy;
        }

        if (endEntityType != null) {
            AtlasAttribute sortByAttribute = endEntityType.getAttribute(sortByAttributeName);

            if (sortByAttribute == null) {
                sortByAttributeName = null;
                sortOrder           = null;

                if (StringUtils.isNotEmpty(sortBy)) {
                    LOG.info("Invalid sortBy Attribute {} for entityType {}, Ignoring Sorting", sortBy, endEntityType.getTypeName());
                } else {
                    LOG.info("Invalid Default sortBy Attribute {} for entityType {}, Ignoring Sorting", DEFAULT_SORT_ATTRIBUTE_NAME, endEntityType.getTypeName());
                }

            } else {
                sortByAttributeName = sortByAttribute.getVertexPropertyName();

                if (sortOrder == null) {
                    sortOrder = ASCENDING;
                }
            }
        } else {
            sortOrder = null;

            if (StringUtils.isNotEmpty(sortBy)) {
                LOG.info("Invalid sortBy Attribute {}, Ignoring Sorting", sortBy);
            }
        }

        //get relationship(end vertices) vertices
        GraphTraversal gt = graph.V(entityVertex.getId()).bothE(relation).otherV();

        if (searchParameters.getExcludeDeletedEntities()) {
            gt.has(Constants.STATE_PROPERTY_KEY, AtlasEntity.Status.ACTIVE.name());
        }

        if (sortOrder != null) {
            if (sortOrder == ASCENDING) {
                gt.order().by(sortByAttributeName, Order.asc);
            } else {
                gt.order().by(sortByAttributeName, Order.desc);
            }
        }

         gt.range(offset, offset + limit);

        List<AtlasEntityHeader> resultList = new ArrayList<>();
        while (gt.hasNext()) {
            Vertex v = (Vertex) gt.next();

            if (v != null && v.property(Constants.GUID_PROPERTY_KEY).isPresent()) {
                String endVertexGuid     = v.property(Constants.GUID_PROPERTY_KEY).value().toString();
                AtlasVertex vertex       = entityRetriever.getEntityVertex(endVertexGuid);
                AtlasEntityHeader entity = entityRetriever.toAtlasEntityHeader(vertex, searchParameters.getAttributes());

                if (searchParameters.getIncludeClassificationAttributes()) {
                    entity.setClassifications(entityRetriever.getAllClassifications(vertex));
                }
                resultList.add(entity);
            }
        }

        ret.setEntities(resultList);

        if (ret.getEntities() == null) {
            ret.setEntities(new ArrayList<>());
        }

        //set approximate count
        //state of the edge and endVertex will be same
        if (getApproximateCount) {
            Iterator<AtlasEdge> edges = GraphHelper.getAdjacentEdgesByLabel(entityVertex, AtlasEdgeDirection.BOTH, relation);

            if (searchParameters.getExcludeDeletedEntities()) {
                List<AtlasEdge> edgeList = new ArrayList<>();
                edges.forEachRemaining(edgeList::add);

                Predicate activePredicate = SearchPredicateUtil.getEQPredicateGenerator().generatePredicate
                        (Constants.STATE_PROPERTY_KEY, AtlasEntity.Status.ACTIVE.name(), String.class);

                CollectionUtils.filter(edgeList, activePredicate);
                ret.setApproximateCount(edgeList.size());

            } else {
                ret.setApproximateCount(IteratorUtils.size(edges));

            }
        }

        scrubSearchResults(ret);

        return ret;
    }

    public int getMaxResultSetSize() {
        return maxResultSetSize;
    }

    private String getQueryForFullTextSearch(String userKeyedString, String typeName, String classification) {
        String typeFilter           = getTypeFilter(typeRegistry, typeName, maxTypesLengthInIdxQuery);
        String classificationFilter = getClassificationFilter(typeRegistry, classification, maxTagsLengthInIdxQuery);

        StringBuilder queryText = new StringBuilder();

        if (! StringUtils.isEmpty(userKeyedString)) {
            queryText.append(userKeyedString);
        }

        if (! StringUtils.isEmpty(typeFilter)) {
            if (queryText.length() > 0) {
                queryText.append(" AND ");
            }

            queryText.append(typeFilter);
        }

        if (! StringUtils.isEmpty(classificationFilter)) {
            if (queryText.length() > 0) {
                queryText.append(" AND ");
            }

            queryText.append(classificationFilter);
        }

        return String.format(indexSearchPrefix + "\"%s\":(%s)", Constants.ENTITY_TEXT_PROPERTY_KEY, queryText.toString());
    }

    private List<AtlasFullTextResult> getIndexQueryResults(AtlasIndexQuery query, QueryParams params, boolean excludeDeletedEntities) throws AtlasBaseException {
        List<AtlasFullTextResult> ret  = new ArrayList<>();
        Iterator<Result>          iter = query.vertices();

        while (iter.hasNext() && ret.size() < params.limit()) {
            Result      idxQueryResult = iter.next();
            AtlasVertex vertex         = idxQueryResult.getVertex();

            if (skipDeletedEntities(excludeDeletedEntities, vertex)) {
                continue;
            }

            String guid = vertex != null ? vertex.getProperty(Constants.GUID_PROPERTY_KEY, String.class) : null;

            if (guid != null) {
                AtlasEntityHeader entity = entityRetriever.toAtlasEntityHeader(vertex);
                Double score = idxQueryResult.getScore();
                ret.add(new AtlasFullTextResult(entity, score));
            }
        }

        return ret;
    }

    private AtlasIndexQuery toAtlasIndexQuery(String fullTextQuery) {
        String graphQuery = String.format(indexSearchPrefix + "\"%s\":(%s)", Constants.ENTITY_TEXT_PROPERTY_KEY, fullTextQuery);
        return graph.indexQuery(Constants.FULLTEXT_INDEX, graphQuery);
    }

    private boolean skipDeletedEntities(boolean excludeDeletedEntities, AtlasVertex<?, ?> vertex) {
        return excludeDeletedEntities && GraphHelper.getStatus(vertex) == DELETED;
    }

    private static String getClassificationFilter(AtlasTypeRegistry typeRegistry, String classificationName, int maxTypesLengthInIdxQuery) {
        AtlasClassificationType type                  = typeRegistry.getClassificationTypeByName(classificationName);
        String                  typeAndSubTypesQryStr = type != null ? type.getTypeAndAllSubTypesQryStr() : null;

        if(StringUtils.isNotEmpty(typeAndSubTypesQryStr) && typeAndSubTypesQryStr.length() <= maxTypesLengthInIdxQuery) {
            return typeAndSubTypesQryStr;
        }

        return "";
    }

    @VisibleForTesting
    static String getTypeFilter(AtlasTypeRegistry typeRegistry, String typeName, int maxTypesLengthInIdxQuery) {
        AtlasEntityType type                  = typeRegistry.getEntityTypeByName(typeName);
        String          typeAndSubTypesQryStr = type != null ? type.getTypeAndAllSubTypesQryStr() : null;

        if(StringUtils.isNotEmpty(typeAndSubTypesQryStr) && typeAndSubTypesQryStr.length() <= maxTypesLengthInIdxQuery) {
            return typeAndSubTypesQryStr;
        }

        return "";
    }

    private Set<String> getEntityStates() {
        return new HashSet<>(Arrays.asList(ACTIVE.toString(), DELETED.toString()));
    }


    @Override
    public AtlasUserSavedSearch addSavedSearch(String currentUser, AtlasUserSavedSearch savedSearch) throws AtlasBaseException {
        try {
            if (StringUtils.isEmpty(savedSearch.getOwnerName())) {
                savedSearch.setOwnerName(currentUser);
            }

            checkSavedSearchOwnership(currentUser, savedSearch);

            return userProfileService.addSavedSearch(savedSearch);
        } catch (AtlasBaseException e) {
            LOG.error("addSavedSearch({})", savedSearch, e);
            throw e;
        }
    }


    @Override
    public AtlasUserSavedSearch updateSavedSearch(String currentUser, AtlasUserSavedSearch savedSearch) throws AtlasBaseException {
        try {
            if (StringUtils.isEmpty(savedSearch.getOwnerName())) {
                savedSearch.setOwnerName(currentUser);
            }

            checkSavedSearchOwnership(currentUser, savedSearch);

            return userProfileService.updateSavedSearch(savedSearch);
        } catch (AtlasBaseException e) {
            LOG.error("updateSavedSearch({})", savedSearch, e);
            throw e;
        }
    }

    @Override
    public List<AtlasUserSavedSearch> getSavedSearches(String currentUser, String userName) throws AtlasBaseException {
        try {
            if (StringUtils.isEmpty(userName)) {
                userName = currentUser;
            } else if (!StringUtils.equals(currentUser, userName)) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "invalid data");
            }

            return userProfileService.getSavedSearches(userName);
        } catch (AtlasBaseException e) {
            LOG.error("getSavedSearches({})", userName, e);
            throw e;
        }
    }

    @Override
    public AtlasUserSavedSearch getSavedSearchByGuid(String currentUser, String guid) throws AtlasBaseException {
        try {
            AtlasUserSavedSearch savedSearch = userProfileService.getSavedSearch(guid);

            checkSavedSearchOwnership(currentUser, savedSearch);

            return savedSearch;
        } catch (AtlasBaseException e) {
            LOG.error("getSavedSearchByGuid({})", guid, e);
            throw e;
        }
    }

    @Override
    public AtlasUserSavedSearch getSavedSearchByName(String currentUser, String userName, String searchName) throws AtlasBaseException {
        try {
            if (StringUtils.isEmpty(userName)) {
                userName = currentUser;
            } else if (!StringUtils.equals(currentUser, userName)) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "invalid data");
            }

            return userProfileService.getSavedSearch(userName, searchName);
        } catch (AtlasBaseException e) {
            LOG.error("getSavedSearchByName({}, {})", userName, searchName, e);
            throw e;
        }
    }

    @Override
    public void deleteSavedSearch(String currentUser, String guid) throws AtlasBaseException {
        try {
            AtlasUserSavedSearch savedSearch = userProfileService.getSavedSearch(guid);

            checkSavedSearchOwnership(currentUser, savedSearch);

            userProfileService.deleteSavedSearch(guid);
        } catch (AtlasBaseException e) {
            LOG.error("deleteSavedSearch({})", guid, e);
            throw e;
        }
    }

    @Override
    public String getDslQueryUsingTypeNameClassification(String query, String typeName, String classification) {
        String queryStr = query == null ? "" : query;

        if (StringUtils.isNotEmpty(typeName)) {
            queryStr = escapeTypeName(typeName) + " " + queryStr;
        }

        if (StringUtils.isNotEmpty(classification)) {
            // isa works with a type name only - like hive_column isa PII; it doesn't work with more complex query
            if (StringUtils.isEmpty(query)) {
                queryStr += (" isa " + classification);
            }
        }
        return queryStr;
    }

    public static SearchParameters createSearchParameters(QuickSearchParameters quickSearchParameters) {
        SearchParameters searchParameters = new SearchParameters();

        searchParameters.setQuery(quickSearchParameters.getQuery());
        searchParameters.setTypeName(quickSearchParameters.getTypeName());
        searchParameters.setExcludeDeletedEntities(quickSearchParameters.getExcludeDeletedEntities());
        searchParameters.setIncludeSubTypes(quickSearchParameters.getIncludeSubTypes());
        searchParameters.setLimit(quickSearchParameters.getLimit());
        searchParameters.setOffset(quickSearchParameters.getOffset());
        searchParameters.setEntityFilters(quickSearchParameters.getEntityFilters());
        searchParameters.setAttributes(quickSearchParameters.getAttributes());
        searchParameters.setSortBy(quickSearchParameters.getSortBy());
        searchParameters.setSortOrder(quickSearchParameters.getSortOrder());

        return searchParameters;
    }

    private String escapeTypeName(String typeName) {
        String ret;

        if (StringUtils.startsWith(typeName, "`") && StringUtils.endsWith(typeName, "`")) {
            ret = typeName;
        } else {
            ret = String.format("`%s`", typeName);
        }

        return ret;
    }

    private void checkSavedSearchOwnership(String claimedOwner, AtlasUserSavedSearch savedSearch) throws AtlasBaseException {
        // block attempt to delete another user's saved-search
        if (savedSearch != null && !StringUtils.equals(savedSearch.getOwnerName(), claimedOwner)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "invalid data");
        }
    }

    private void scrubSearchResults(AtlasSearchResult result) throws AtlasBaseException {
        AtlasAuthorizationUtils.scrubSearchResults(new AtlasSearchResultScrubRequest(typeRegistry, result));
    }

    private Set<String> getAggregationFields() {
        Set<String> ret = new HashSet<>(); // for non-modeled attributes.

        ret.add(Constants.ENTITY_TYPE_PROPERTY_KEY);
        ret.add(Constants.STATE_PROPERTY_KEY);

        return ret;
    }

    private Set<AtlasAttribute> getAggregationAtlasAttributes() {
        Set<AtlasAttribute> ret = new HashSet<>(); // for modeled attributes, like Asset.owner

        ret.add(getAtlasAttributeForAssetOwner());

        return ret;
    }

    private AtlasAttribute getAtlasAttributeForAssetOwner() {
        AtlasEntityType typeAsset = typeRegistry.getEntityTypeByName(ASSET_ENTITY_TYPE);
        AtlasAttribute  atttOwner = typeAsset != null ? typeAsset.getAttribute(OWNER_ATTRIBUTE) : null;

        if(atttOwner == null) {
            String msg = String.format("Unable to resolve the attribute %s.%s", ASSET_ENTITY_TYPE, OWNER_ATTRIBUTE);

            LOG.error(msg);

            throw new RuntimeException(msg);
        }

        return atttOwner;
    }
}
