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
import org.apache.atlas.AtlasException;
import org.apache.atlas.SortOrder;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.discovery.SearchParameters.FilterCriteria;
import org.apache.atlas.model.discovery.SearchParameters.FilterCriteria.Condition;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasIndexQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.*;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.util.AtlasGremlinQueryProvider;
import org.apache.atlas.util.SearchPredicateUtil;
import org.apache.atlas.util.SearchPredicateUtil.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.atlas.SortOrder.ASCENDING;
import static org.apache.atlas.discovery.SearchContext.MATCH_ALL_CLASSIFICATION_TYPES;
import static org.apache.atlas.discovery.SearchContext.MATCH_ALL_NOT_CLASSIFIED;
import static org.apache.atlas.repository.Constants.*;
import static org.apache.atlas.util.SearchPredicateUtil.*;

public abstract class SearchProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SearchProcessor.class);

    public static final Pattern STRAY_AND_PATTERN          = Pattern.compile("(AND\\s+)+\\)");
    public static final Pattern STRAY_OR_PATTERN           = Pattern.compile("(OR\\s+)+\\)");
    public static final Pattern STRAY_ELIPSIS_PATTERN      = Pattern.compile("(\\(\\s*)\\)");
    public static final int     MAX_RESULT_SIZE            = getApplicationProperty(Constants.INDEX_SEARCH_MAX_RESULT_SET_SIZE, 150);
    public static final int     MAX_QUERY_STR_LENGTH_TYPES = getApplicationProperty(Constants.INDEX_SEARCH_TYPES_MAX_QUERY_STR_LENGTH, 512);
    public static final int     MAX_QUERY_STR_LENGTH_TAGS  = getApplicationProperty(Constants.INDEX_SEARCH_TAGS_MAX_QUERY_STR_LENGTH, 512);
    public static final String  INDEX_SEARCH_PREFIX        = AtlasGraphUtilsV2.getIndexSearchPrefix();
    public static final String  AND_STR                    = " AND ";
    public static final String  EMPTY_STRING               = "";
    public static final String  SPACE_STRING               = " ";
    public static final String  BRACE_OPEN_STR             = "(";
    public static final String  BRACE_CLOSE_STR            = ")";
    public static final String  ALL_TYPE_QUERY             = "[* TO *]";
    public static final char    CUSTOM_ATTR_SEPARATOR      = '=';
    public static final String  CUSTOM_ATTR_SEARCH_FORMAT  = "\"\\\"%s\\\":\\\"%s\\\"\"";
    public static final String  CUSTOM_ATTR_SEARCH_FORMAT_GRAPH = "\"%s\":\"%s\"";
    private static final Map<SearchParameters.Operator, String>                            OPERATOR_MAP           = new HashMap<>();
    private static final Map<SearchParameters.Operator, ElementAttributePredicateGenerator> OPERATOR_PREDICATE_MAP = new HashMap<>();

    static
    {
        OPERATOR_MAP.put(SearchParameters.Operator.LT, INDEX_SEARCH_PREFIX + "\"%s\": [* TO %s}");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.LT, getLTPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.GT, INDEX_SEARCH_PREFIX + "\"%s\": {%s TO *]");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.GT, getGTPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.LTE, INDEX_SEARCH_PREFIX + "\"%s\": [* TO %s]");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.LTE, getLTEPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.GTE, INDEX_SEARCH_PREFIX + "\"%s\": [%s TO *]");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.GTE, getGTEPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.EQ, INDEX_SEARCH_PREFIX + "\"%s\": %s");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.EQ, getEQPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.NEQ, "(*:* -" + INDEX_SEARCH_PREFIX + "\"%s\": %s)");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.NEQ, getNEQPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.IN, INDEX_SEARCH_PREFIX + "\"%s\": (%s)"); // this should be a list of quoted strings
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.IN, getINPredicateGenerator()); // this should be a list of quoted strings

        OPERATOR_MAP.put(SearchParameters.Operator.LIKE, INDEX_SEARCH_PREFIX + "\"%s\": (%s)"); // this should be regex pattern
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.LIKE, getLIKEPredicateGenerator()); // this should be regex pattern

        OPERATOR_MAP.put(SearchParameters.Operator.STARTS_WITH, INDEX_SEARCH_PREFIX + "\"%s\": (%s*)");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.STARTS_WITH, getStartsWithPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.ENDS_WITH, INDEX_SEARCH_PREFIX + "\"%s\": (*%s)");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.ENDS_WITH, getEndsWithPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.CONTAINS, INDEX_SEARCH_PREFIX + "\"%s\": (*%s*)");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.CONTAINS, getContainsPredicateGenerator());

        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.NOT_CONTAINS, getNotContainsPredicateGenerator());

        // TODO: Add contains any, contains all mappings here

        OPERATOR_MAP.put(SearchParameters.Operator.IS_NULL, "(*:* NOT " + INDEX_SEARCH_PREFIX + "\"%s\":[* TO *])");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.IS_NULL, getIsNullPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.NOT_NULL, INDEX_SEARCH_PREFIX + "\"%s\":[* TO *]");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.NOT_NULL, getNotNullPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.NOT_EMPTY, INDEX_SEARCH_PREFIX + "\"%s\":[* TO *]");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.NOT_EMPTY, getNotEmptyPredicateGenerator());

        OPERATOR_MAP.put(SearchParameters.Operator.TIME_RANGE, INDEX_SEARCH_PREFIX + "\"%s\": [%s TO %s]");
        OPERATOR_PREDICATE_MAP.put(SearchParameters.Operator.TIME_RANGE, getInRangePredicateGenerator());
    }

    protected final SearchContext          context;
    protected       SearchProcessor        nextProcessor;
    protected       Predicate              inMemoryPredicate;
    protected       GraphIndexQueryBuilder graphIndexQueryBuilder;
    protected       Integer                nextOffset;

    protected SearchProcessor(SearchContext context) {
        this.context = context;
        this.graphIndexQueryBuilder = new GraphIndexQueryBuilder(context);
    }

    public void addProcessor(SearchProcessor processor) {
        if (nextProcessor == null) {
            nextProcessor = processor;
        } else {
            nextProcessor.addProcessor(processor);
        }
    }

    public String getNextMarker() {
        return SearchContext.MarkerUtil.getNextEncMarker(context.getSearchParameters(), nextOffset);
    }

    public abstract List<AtlasVertex> execute();
    public abstract long getResultCount();

    protected boolean isEntityRootType() {
        //always size will be one if in case of _ALL_ENTITY_TYPES
        if (CollectionUtils.isNotEmpty(context.getEntityTypes())) {
            return context.getEntityTypes().iterator().next() == SearchContext.MATCH_ALL_ENTITY_TYPES;
        }
        return false;
    }

    protected boolean isClassificationRootType() {
        //always size will be one if in case of _ALL_CLASSIFICATION_TYPES
        if (CollectionUtils.isNotEmpty(context.getClassificationTypes())) {
            return context.getClassificationTypes().iterator().next() == SearchContext.MATCH_ALL_CLASSIFICATION_TYPES;
        }
        return false;
    }

    protected boolean isSystemAttribute(String attrName) {
        return AtlasEntityType.getEntityRoot().hasAttribute(attrName) || AtlasClassificationType.getClassificationRoot().hasAttribute(attrName);
    }

    protected boolean isPipeSeparatedSystemAttribute(String attrName) {
        return StringUtils.equals(attrName, CLASSIFICATION_NAMES_KEY) ||
               StringUtils.equals(attrName, PROPAGATED_CLASSIFICATION_NAMES_KEY) ||
               StringUtils.equals(attrName, LABELS_PROPERTY_KEY) ||
               StringUtils.equals(attrName, CUSTOM_ATTRIBUTES_PROPERTY_KEY);
    }

    protected int collectResultVertices(final List<AtlasVertex> ret, final int startIdx, final int limit, int resultIdx, final Map<Integer, AtlasVertex> offsetEntityVertexMap, Integer marker) {
            int lastOffset = resultIdx;

            for (Map.Entry<Integer, AtlasVertex> offsetToEntity : offsetEntityVertexMap.entrySet()) {
                resultIdx++;

                if (resultIdx <= startIdx) {
                    continue;
                }

                lastOffset = offsetToEntity.getKey();
                ret.add(offsetToEntity.getValue());

                if (ret.size() == limit) {
                    break;
                }
            }
            return marker == null ? resultIdx : lastOffset;
    }

    public LinkedHashMap<Integer, AtlasVertex> filter(LinkedHashMap<Integer, AtlasVertex> offsetEntityVertexMap) {
        if (nextProcessor != null && MapUtils.isNotEmpty(offsetEntityVertexMap)) {
            return nextProcessor.filter(offsetEntityVertexMap);
        }
        return offsetEntityVertexMap;
    }

    public LinkedHashMap<Integer, AtlasVertex> filter(LinkedHashMap<Integer, AtlasVertex> offsetEntityVertexMap, Predicate predicate) {
        if (predicate != null) {
            offsetEntityVertexMap = offsetEntityVertexMap.entrySet()
                    .stream()
                    .filter(x -> predicate.evaluate(x.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        }
        return offsetEntityVertexMap;
    }

    protected Predicate buildTraitPredict(Set<AtlasClassificationType> classificationTypes) {
        Predicate traitPredicate;
        AtlasClassificationType classificationType = null;
        if (CollectionUtils.isNotEmpty(classificationTypes)) {
            classificationType = classificationTypes.iterator().next();
        }

        if (classificationType == MATCH_ALL_CLASSIFICATION_TYPES) {
            traitPredicate = PredicateUtils.orPredicate(SearchPredicateUtil.getNotEmptyPredicateGenerator().generatePredicate(TRAIT_NAMES_PROPERTY_KEY, null, List.class),
                SearchPredicateUtil.getNotEmptyPredicateGenerator().generatePredicate(PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, null, List.class));
        } else if (classificationType == MATCH_ALL_NOT_CLASSIFIED) {
            traitPredicate = PredicateUtils.andPredicate(SearchPredicateUtil.getIsNullOrEmptyPredicateGenerator().generatePredicate(TRAIT_NAMES_PROPERTY_KEY, null, List.class),
                SearchPredicateUtil.getIsNullOrEmptyPredicateGenerator().generatePredicate(PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, null, List.class));
        } else if (context.isWildCardSearch()) {
            //For wildcard search __classificationNames which of String type is taken instead of _traitNames which is of Array type
            Set<String> classificationNames = context.getClassificationNames();
            List<Predicate> predicates = new ArrayList<>();

            classificationNames.forEach(classificationName -> {
                //No need to escape, as classification Names only support letters,numbers,space and underscore
                String regexString = getRegexString("\\|" + classificationName + "\\|");
                predicates.add(SearchPredicateUtil.getRegexPredicateGenerator().generatePredicate(CLASSIFICATION_NAMES_KEY, regexString, String.class));
                predicates.add(SearchPredicateUtil.getRegexPredicateGenerator().generatePredicate(PROPAGATED_CLASSIFICATION_NAMES_KEY, regexString, String.class));
            });

            traitPredicate = PredicateUtils.anyPredicate(predicates);
        } else {
            traitPredicate = PredicateUtils.orPredicate(SearchPredicateUtil.getContainsAnyPredicateGenerator().generatePredicate(TRAIT_NAMES_PROPERTY_KEY, context.getClassificationTypeNames(), List.class),
                SearchPredicateUtil.getContainsAnyPredicateGenerator().generatePredicate(PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, context.getClassificationTypeNames(), List.class));
        }
        return traitPredicate;
    }


    protected void processSearchAttributes(Set<? extends AtlasStructType> structTypes, FilterCriteria filterCriteria, Set<String> indexFiltered, Set<String> graphFiltered, Set<String> allAttributes) {
        if (CollectionUtils.isEmpty(structTypes) || filterCriteria == null) {
            return;
        }

        Condition            filterCondition = filterCriteria.getCondition();
        List<FilterCriteria> criterion       = filterCriteria.getCriterion();

        if (filterCondition != null && CollectionUtils.isNotEmpty(criterion)) {
            for (SearchParameters.FilterCriteria criteria : criterion) {
                processSearchAttributes(structTypes, criteria, indexFiltered, graphFiltered, allAttributes);
            }
        } else if (StringUtils.isNotEmpty(filterCriteria.getAttributeName())) {
            String attributeName = filterCriteria.getAttributeName();

            if (StringUtils.equals(attributeName, Constants.IS_INCOMPLETE_PROPERTY_KEY)) {
                // when entity is incomplete (i.e. shell entity):
                //   vertex property IS_INCOMPLETE_PROPERTY_KEY will be set to INCOMPLETE_ENTITY_VALUE
                // when entity is not incomplete (i.e. not a shell entity):
                //   vertex property IS_INCOMPLETE_PROPERTY_KEY will not be set

                String attributeValue = filterCriteria.getAttributeValue();

                switch (filterCriteria.getOperator()) {
                    case EQ:
                        if (attributeValue == null || StringUtils.equals(attributeValue, "0") || StringUtils.equalsIgnoreCase(attributeValue, "false")) {
                            filterCriteria.setOperator(SearchParameters.Operator.IS_NULL);
                        } else {
                            filterCriteria.setOperator(SearchParameters.Operator.EQ);
                            filterCriteria.setAttributeValue(Constants.INCOMPLETE_ENTITY_VALUE.toString());
                        }
                    break;

                    case NEQ:
                        if (attributeValue == null || StringUtils.equals(attributeValue, "0") || StringUtils.equalsIgnoreCase(attributeValue, "false")) {
                            filterCriteria.setOperator(SearchParameters.Operator.EQ);
                            filterCriteria.setAttributeValue(Constants.INCOMPLETE_ENTITY_VALUE.toString());
                        } else {
                            filterCriteria.setOperator(SearchParameters.Operator.IS_NULL);
                        }
                    break;

                    case NOT_NULL:
                        filterCriteria.setOperator(SearchParameters.Operator.EQ);
                        filterCriteria.setAttributeValue(Constants.INCOMPLETE_ENTITY_VALUE.toString());
                    break;
                }
            }

            try {
                for (AtlasStructType structType : structTypes) {
                    String qualifiedName = structType.getVertexPropertyName(attributeName);
                    if (isIndexSearchable(filterCriteria, structType)) {
                        indexFiltered.add(qualifiedName);
                    } else {
                        LOG.warn("not using index-search for attribute '{}'; might cause poor performance", structType.getVertexPropertyName(attributeName));

                        graphFiltered.add(qualifiedName);
                    }

                    if (structType instanceof AtlasEntityType && !isSystemAttribute(attributeName)) {
                        // Capture the entity attributes
                        context.getEntityAttributes().add(attributeName);
                    }

                    allAttributes.add(qualifiedName);
                }
            } catch (AtlasBaseException e) {
                LOG.warn(e.getMessage());
            }
        }
    }

    //
    // If filterCriteria contains any non-indexed attribute inside OR condition:
    //    Index+Graph can't be used. Need to use only Graph query filter for all attributes. Examples:
    //    (OR idx-att1=x non-idx-attr=z)
    //    (AND idx-att1=x (OR idx-attr2=y non-idx-attr=z))
    // Else
    //    Index query can be used for indexed-attribute filtering and Graph query for non-indexed attributes. Examples:
    //      (AND idx-att1=x idx-attr2=y non-idx-attr=z)
    //      (AND (OR idx-att1=x idx-attr1=y) non-idx-attr=z)
    //      (AND (OR idx-att1=x idx-attr1=y) non-idx-attr=z (AND idx-attr2=xyz idx-attr2=abc))
    //
    protected boolean canApplyIndexFilter(Set<? extends AtlasStructType> structTypes, FilterCriteria filterCriteria, boolean insideOrCondition) {
        if (!context.hasAttributeFilter(filterCriteria)) {
            return true;
        }

        boolean              ret             = true;
        Condition            filterCondition = filterCriteria.getCondition();
        List<FilterCriteria> criterion       = filterCriteria.getCriterion();
        Set<String>          indexedKeys     = context.getIndexedKeys();


        if (filterCondition != null && CollectionUtils.isNotEmpty(criterion)) {
            insideOrCondition = insideOrCondition || filterCondition == Condition.OR;

            // If we have nested criterion let's find any nested ORs with non-indexed attr
            for (FilterCriteria criteria : criterion) {
                ret = canApplyIndexFilter(structTypes, criteria, insideOrCondition);

                if (!ret) {
                    break;
                }
            }
        } else if (StringUtils.isNotEmpty(filterCriteria.getAttributeName())) {
            try {
                for (AtlasStructType structType : structTypes) {
                    if (insideOrCondition && !isIndexSearchable(filterCriteria, structType)) {
                        ret = false;
                        break;
                    }
                }
            } catch (AtlasBaseException e) {
                LOG.warn(e.getMessage());
            }
        }

        return ret;
    }

    protected LinkedHashMap<Integer,AtlasVertex> filterWhiteSpaceClassification(LinkedHashMap<Integer,AtlasVertex> offsetEntityVertexMap) {
        if (offsetEntityVertexMap != null) {
            final Iterator<Map.Entry<Integer, AtlasVertex>> it = offsetEntityVertexMap.entrySet().iterator();
            final Set<String>                  typeAndSubTypes = context.getClassificationTypeNames();

            while (it.hasNext()) {
                AtlasVertex  entityVertex        = it.next().getValue();
                List<String> classificationNames = AtlasGraphUtilsV2.getClassificationNames(entityVertex);

                if (CollectionUtils.isNotEmpty(classificationNames)) {
                    if (typeAndSubTypes.isEmpty() || CollectionUtils.containsAny(classificationNames, typeAndSubTypes)) {
                        continue;
                    }
                }

                List<String> propagatedClassificationNames = AtlasGraphUtilsV2.getPropagatedClassificationNames(entityVertex);

                if (CollectionUtils.isNotEmpty(propagatedClassificationNames)) {
                    if (typeAndSubTypes.isEmpty() || CollectionUtils.containsAny(propagatedClassificationNames, typeAndSubTypes)) {
                        continue;
                    }
                }

                it.remove();
            }
        }
        return offsetEntityVertexMap;
    }

    protected void constructFilterQuery(StringBuilder indexQuery, Set<? extends AtlasStructType> structTypes, FilterCriteria filterCriteria, Set<String> indexAttributes) {
        if (filterCriteria != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing Filters");
            }

            String filterQuery = toIndexQuery(structTypes, filterCriteria, indexAttributes, 0);

            if (StringUtils.isNotEmpty(filterQuery)) {
                if (indexQuery.length() > 0) {
                    indexQuery.append(AND_STR);
                }

                indexQuery.append(filterQuery);
            }
        }
    }

    protected Predicate constructInMemoryPredicate(Set<? extends AtlasStructType> structTypes, FilterCriteria filterCriteria, Set<String> indexAttributes) {
        Predicate ret = null;
        if (filterCriteria != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing Filters");
            }

            ret = toInMemoryPredicate(structTypes, filterCriteria, indexAttributes);
        }
        return ret;
    }

    protected void constructGremlinFilterQuery(StringBuilder gremlinQuery, Map<String, Object> queryBindings, AtlasStructType structType, FilterCriteria filterCriteria) {
        if (filterCriteria != null) {
            FilterCriteria.Condition condition = filterCriteria.getCondition();

            if (condition != null) {
                StringBuilder orQuery = new StringBuilder();

                List<FilterCriteria> criterion = filterCriteria.getCriterion();

                for (int i = 0; i < criterion.size(); i++) {
                    FilterCriteria criteria = criterion.get(i);

                    if (condition == FilterCriteria.Condition.OR) {
                        StringBuilder nestedOrQuery = new StringBuilder("_()");

                        constructGremlinFilterQuery(nestedOrQuery, queryBindings, structType, criteria);

                        orQuery.append(i == 0 ? "" : ",").append(nestedOrQuery);
                    } else {
                        constructGremlinFilterQuery(gremlinQuery, queryBindings, structType, criteria);
                    }
                }

                if (condition == FilterCriteria.Condition.OR) {
                    gremlinQuery.append(".or(").append(orQuery).append(")");
                }
            } else {
                String         attributeName = filterCriteria.getAttributeName();
                AtlasAttribute attribute     = structType.getAttribute(attributeName);

                if (attribute != null) {
                    SearchParameters.Operator operator       = filterCriteria.getOperator();
                    String                    attributeValue = filterCriteria.getAttributeValue();

                    gremlinQuery.append(toGremlinComparisonQuery(attribute, operator, attributeValue, queryBindings));
                } else {
                    LOG.warn("Ignoring unknown attribute {}.{}", structType.getTypeName(), attributeName);
                }

            }
        }
    }

    private boolean isIndexSearchable(FilterCriteria filterCriteria, AtlasStructType structType) throws AtlasBaseException {
        String      attributeName  = filterCriteria.getAttributeName();
        String      attributeValue = filterCriteria.getAttributeValue();
        AtlasType   attributeType  = structType.getAttributeType(attributeName);
        String      typeName       = attributeType.getTypeName();
        String      qualifiedName  = structType.getVertexPropertyName(attributeName);
        Set<String> indexedKeys    = context.getIndexedKeys();
        boolean     ret            = indexedKeys != null && indexedKeys.contains(qualifiedName);

        SearchParameters.Operator                  operator  = filterCriteria.getOperator();
        AtlasStructDef.AtlasAttributeDef.IndexType indexType = structType.getAttributeDef(attributeName).getIndexType();

        if (ret) { // index exists
            // for string type attributes, don't use index query in the following cases:
            //   - operation is NEQ, as it might return fewer entries due to tokenization of vertex property value
            //   - value-to-compare has special characters
            if (AtlasBaseTypeDef.ATLAS_TYPE_STRING.equals(typeName)) {
                if (operator == SearchParameters.Operator.NEQ || operator == SearchParameters.Operator.NOT_CONTAINS) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} operator found for string attribute {}, deferring to in-memory or graph query (might cause poor performance)", operator, qualifiedName);
                    }

                    ret = false;
                } else if (operator == SearchParameters.Operator.CONTAINS && AtlasAttribute.hastokenizeChar(attributeValue) && indexType == null) { // indexType = TEXT
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} operator found for string (TEXT) attribute {} and special characters found in filter value {}, deferring to in-memory or graph query (might cause poor performance)", operator, qualifiedName, attributeValue);
                    }

                    ret = false;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            if (!ret) {
                LOG.debug("Not using index query for: attribute='{}', operator='{}', value='{}'", qualifiedName, operator, attributeValue);
            }
        }

        return ret;
    }

    private String toIndexQuery(Set<? extends AtlasStructType> structTypes, FilterCriteria criteria, Set<String> indexAttributes, int level) {
        return toIndexQuery(structTypes, criteria, indexAttributes, new StringBuilder(), level);
    }

    private String toIndexQuery(Set<? extends AtlasStructType> structTypes, FilterCriteria criteria, Set<String> indexAttributes, StringBuilder sb, int level) {
        Set<String> filterAttributes = new HashSet<>();
        filterAttributes.addAll(indexAttributes);

        Condition condition = criteria.getCondition();
        if (condition != null && CollectionUtils.isNotEmpty(criteria.getCriterion())) {
            StringBuilder nestedExpression = new StringBuilder();

            for (FilterCriteria filterCriteria : criteria.getCriterion()) {
                String nestedQuery = toIndexQuery(structTypes, filterCriteria, filterAttributes, level + 1);

                if (StringUtils.isNotEmpty(nestedQuery)) {
                    if (nestedExpression.length() > 0) {
                        nestedExpression.append(SPACE_STRING).append(condition).append(SPACE_STRING);
                    }
                    nestedExpression.append(nestedQuery);
                }
            }

            boolean needSurroundingBraces = level != 0 || (condition == Condition.OR && criteria.getCriterion().size() > 1);
            if (nestedExpression.length() > 0) {
                return sb.append(needSurroundingBraces ? BRACE_OPEN_STR : EMPTY_STRING)
                         .append(nestedExpression)
                         .append(needSurroundingBraces ? BRACE_CLOSE_STR : EMPTY_STRING)
                         .toString();
            } else {
                return EMPTY_STRING;
            }
        } else if (StringUtils.isNotEmpty(criteria.getAttributeName())) {
            try {
                if (criteria.getOperator() == SearchParameters.Operator.TIME_RANGE) {
                    criteria = processDateRange(criteria);
                }
                ArrayList<String> orExpQuery = new ArrayList<>();
                for (AtlasStructType structType : structTypes) {
                    String name = structType.getVertexPropertyName(criteria.getAttributeName());

                    if (filterAttributes.contains(name)) {
                        String nestedQuery = toIndexExpression(structType, criteria.getAttributeName(), criteria.getOperator(), criteria.getAttributeValue());
                        orExpQuery.add(nestedQuery);
                        filterAttributes.remove(name);
                    }
                }

                if (CollectionUtils.isNotEmpty(orExpQuery)) {
                    if (orExpQuery.size() > 1) {
                        String orExpStr = StringUtils.join(orExpQuery, " "+Condition.OR.name()+" ");
                        return BRACE_OPEN_STR + " " + orExpStr + " " + BRACE_CLOSE_STR;
                    } else {
                        return orExpQuery.iterator().next();
                    }
                } else {
                    return EMPTY_STRING;
                }
            } catch (AtlasBaseException e) {
                LOG.warn(e.getMessage());
            }
        }
        return EMPTY_STRING;
    }

    private Predicate toInMemoryPredicate(Set<? extends AtlasStructType> structTypes, FilterCriteria criteria, Set<String> indexAttributes) {
        Set<String> filterAttributes = new HashSet<>();
        filterAttributes.addAll(indexAttributes);

        if (criteria.getCondition() != null && CollectionUtils.isNotEmpty(criteria.getCriterion())) {
            List<Predicate> predicates = new ArrayList<>();

            for (FilterCriteria filterCriteria : criteria.getCriterion()) {
                Predicate predicate = toInMemoryPredicate(structTypes, filterCriteria, filterAttributes);

                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            if (CollectionUtils.isNotEmpty(predicates)) {
                if (criteria.getCondition() == Condition.AND) {
                    return PredicateUtils.allPredicate(predicates);
                } else {
                    return PredicateUtils.anyPredicate(predicates);
                }
            }
        } else if (StringUtils.isNotEmpty(criteria.getAttributeName())) {
            try {
                ArrayList<Predicate> predicates = new ArrayList<>();
                for (AtlasStructType structType : structTypes) {
                    String name = structType.getVertexPropertyName(criteria.getAttributeName());

                    if (filterAttributes.contains(name)) {
                        String attrName                    = criteria.getAttributeName();
                        String attrValue                   = criteria.getAttributeValue();
                        SearchParameters.Operator operator = criteria.getOperator();

                        if (operator == SearchParameters.Operator.TIME_RANGE) {
                            FilterCriteria processedRangeCriteria = processDateRange(criteria);
                            attrValue                             = processedRangeCriteria.getAttributeValue();
                        }
                        //process attribute value and attribute operator for pipeSeperated fields
                        if (isPipeSeparatedSystemAttribute(attrName)) {
                            FilterCriteria processedCriteria = processPipeSeperatedSystemAttribute(attrName, operator, attrValue);
                            attrValue                        = processedCriteria.getAttributeValue();
                            operator                         = processedCriteria.getOperator();
                        }

                        predicates.add(toInMemoryPredicate(structType, attrName, operator, attrValue));
                        filterAttributes.remove(name);
                    }
                }

                if (CollectionUtils.isNotEmpty(predicates)) {
                    if (predicates.size() > 1) {
                        return PredicateUtils.anyPredicate(predicates);
                    } else {
                        return predicates.iterator().next();
                    }
                }
            } catch (AtlasBaseException e) {
                LOG.warn(e.getMessage());
            }
        }
        return null;
    }

    @VisibleForTesting
    public FilterCriteria processDateRange(FilterCriteria criteria) {
        String attrName = criteria.getAttributeName();
        SearchParameters.Operator op = criteria.getOperator();
        String attrVal = criteria.getAttributeValue();
        FilterCriteria ret = new FilterCriteria();
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime startTime;
        final LocalDateTime endTime;

        switch (attrVal) {
            case "LAST_7_DAYS":
                startTime = now.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusDays(7).minusNanos(1);
                break;

            case "LAST_30_DAYS":
                startTime = now.minusDays(29).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusDays(30).minusNanos(1);
                break;

            case "LAST_MONTH":
                startTime = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusMonths(1).minusNanos(1);
                break;

            case "THIS_MONTH":
                startTime = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusMonths(1).minusNanos(1);
                break;

            case "TODAY":
                startTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusDays(1).minusNanos(1);
                break;
                
            case "YESTERDAY":
                startTime = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusDays(1).minusNanos(1);
                break;

            case "THIS_YEAR":
                startTime = now.withDayOfYear(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusYears(1).minusNanos(1);
                break;

            case "LAST_YEAR":
                startTime = now.minusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusYears(1).minusNanos(1);
                break;

            case "THIS_QUARTER":
                startTime = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusMonths(3).minusNanos(1);
                break;

            case "LAST_QUARTER":
                startTime = now.minusMonths(4).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusMonths(3).minusNanos(1);
                break;

            case "LAST_3_MONTHS":
                startTime = now.minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusMonths(3).minusNanos(1);
                break;

            case "LAST_6_MONTHS":
                startTime = now.minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusMonths(6).minusNanos(1);
                break;

            case "LAST_12_MONTHS":
                startTime = now.minusMonths(12).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endTime   = startTime.plusMonths(12).minusNanos(1);
                break;

            default:
                startTime = null;
                endTime   = null;
                break;
        }

        if (startTime == null || endTime == null) {
            String[] rangeAttr = attrVal.split(ATTRIBUTE_VALUE_DELIMITER);
            boolean numeric = true;
            if (rangeAttr.length != 2) {
                LOG.error("Separator invalid");
            } else {
                try {
                    Long parsestartTime = Long.parseLong(String.valueOf(rangeAttr[0]));
                    Long parseendTime = Long.parseLong(String.valueOf(rangeAttr[1]));
                } catch (NumberFormatException e) {
                    numeric = false;
                    if (!numeric) {
                        LOG.error("Attributes passed need to be LONG");
                    }
                }
            }
        }else {
            attrVal = Timestamp.valueOf(startTime).getTime() + ATTRIBUTE_VALUE_DELIMITER + Timestamp.valueOf(endTime).getTime();
        }

        ret.setAttributeName(attrName);
        ret.setOperator(op);
        ret.setAttributeValue(attrVal);

        return ret;
    }

    private FilterCriteria processPipeSeperatedSystemAttribute(String attrName, SearchParameters.Operator op, String attrVal) {

        FilterCriteria ret = new FilterCriteria();

        if (op != null && attrVal != null) {
            switch (op) {
                case STARTS_WITH:
                    attrVal = CLASSIFICATION_NAME_DELIMITER + attrVal;
                    op      = SearchParameters.Operator.CONTAINS;
                    break;
                case ENDS_WITH:
                    attrVal = attrVal + CLASSIFICATION_NAME_DELIMITER;
                    op      = SearchParameters.Operator.CONTAINS;
                    break;
                case EQ:
                    attrVal = GraphHelper.getDelimitedClassificationNames(Collections.singleton(attrVal));
                    op      = SearchParameters.Operator.CONTAINS;
                    break;
                case NEQ:
                    attrVal = GraphHelper.getDelimitedClassificationNames(Collections.singleton(attrVal));
                    op      = SearchParameters.Operator.NOT_CONTAINS;
                    break;
                case CONTAINS:
                case NOT_CONTAINS:
                    if (attrName.equals(CUSTOM_ATTRIBUTES_PROPERTY_KEY)) {
                        attrVal = getCustomAttributeIndexQueryValue(attrVal, true);
                    }
                    break;
            }
        }

        ret.setAttributeName(attrName);
        ret.setOperator(op);
        ret.setAttributeValue(attrVal);

        return ret;
    }

    private String toIndexExpression(AtlasStructType type, String attrName, SearchParameters.Operator op, String attrVal) throws AtlasBaseException{
        String ret = EMPTY_STRING;

        try {
            if (OPERATOR_MAP.get(op) != null) {
                String rangeStart = "";
                String rangeEnd = "";
                String qualifiedName = type.getVertexPropertyName(attrName);
                if (op == SearchParameters.Operator.TIME_RANGE) {
                    String[] parts = attrVal.split(ATTRIBUTE_VALUE_DELIMITER);
                    if (parts.length == 2) {
                        rangeStart = parts[0];
                        rangeEnd = parts[1];
                        String rangeStartIndexQueryValue = AtlasAttribute.escapeIndexQueryValue(rangeStart);
                        String rangeEndIndexQueryValue = AtlasAttribute.escapeIndexQueryValue(rangeEnd);
                        ret = String.format(OPERATOR_MAP.get(op), qualifiedName, rangeStartIndexQueryValue, rangeEndIndexQueryValue);
                    }
                } else {
                    String escapeIndexQueryValue;
                    boolean replaceWildcardChar = false;

                    AtlasStructDef.AtlasAttributeDef def = type.getAttributeDef(attrName);

                    //when wildcard search -> escape special Char, don't quote
                    //      when  tokenized characters + index field Type TEXT -> remove wildcard '*' from query
                    if (!isPipeSeparatedSystemAttribute(attrName)
                            && (op == SearchParameters.Operator.CONTAINS || op == SearchParameters.Operator.STARTS_WITH || op == SearchParameters.Operator.ENDS_WITH)
                            && def.getTypeName().equalsIgnoreCase(AtlasBaseTypeDef.ATLAS_TYPE_STRING)) {

                        if (def.getIndexType() == null && AtlasAttribute.hastokenizeChar(attrVal)) {
                            replaceWildcardChar = true;
                        }
                        escapeIndexQueryValue  = AtlasAttribute.escapeIndexQueryValue(attrVal, false, false);

                    } else {
                        escapeIndexQueryValue = AtlasAttribute.escapeIndexQueryValue(attrVal);
                    }

                    String operatorStr = OPERATOR_MAP.get(op);
                    if (replaceWildcardChar) {
                        operatorStr = operatorStr.replace("*", "");
                    }

                    // map '__customAttributes' value from 'key1=value1' to '\"key1\":\"value1\"' (escape special characters and surround with quotes)
                    if (attrName.equals(CUSTOM_ATTRIBUTES_PROPERTY_KEY) && op == SearchParameters.Operator.CONTAINS) {
                        ret = String.format(operatorStr, qualifiedName, getCustomAttributeIndexQueryValue(escapeIndexQueryValue, false));
                    } else {
                        ret = String.format(operatorStr, qualifiedName, escapeIndexQueryValue);
                    }
                }
            }
        } catch (AtlasBaseException ex) {
            LOG.warn(ex.getMessage());
        }

        return ret;
    }

    private String getCustomAttributeIndexQueryValue(String attrValue, boolean forGraphQuery) {
        String ret = null;

        if (StringUtils.isNotEmpty(attrValue)) {
            int    separatorIdx = attrValue.indexOf(CUSTOM_ATTR_SEPARATOR);
            String key          = separatorIdx != -1 ? attrValue.substring(0, separatorIdx) : null;
            String value        = key != null ? attrValue.substring(separatorIdx + 1) : null;

            if (key != null && value != null) {
                if (forGraphQuery) {
                    ret = String.format(CUSTOM_ATTR_SEARCH_FORMAT_GRAPH, key, value);
                } else {
                    ret = String.format(CUSTOM_ATTR_SEARCH_FORMAT, key, value);
                }
            } else {
                ret = attrValue;
            }
        }

        return ret;
    }

    private Predicate toInMemoryPredicate(AtlasStructType type, String attrName, SearchParameters.Operator op, String attrVal) {
        Predicate ret = null;
        AtlasAttribute                    attribute = type.getAttribute(attrName);
        ElementAttributePredicateGenerator predicate = OPERATOR_PREDICATE_MAP.get(op);

        if (attribute != null && predicate != null) {
            final AtlasType attrType = attribute.getAttributeType();
            final Class     attrClass;
            final Object    attrValue;
            Object attrValue2 = null;

            // Some operators support null comparison, thus the parsing has to be conditional
            switch (attrType.getTypeName()) {
                case AtlasBaseTypeDef.ATLAS_TYPE_STRING:
                    attrClass = String.class;
                    attrValue = attrVal;
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_SHORT:
                    attrClass = Short.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : Short.parseShort(attrVal);
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_INT:
                    attrClass = Integer.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : Integer.parseInt(attrVal);
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_BIGINTEGER:
                    attrClass = BigInteger.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : new BigInteger(attrVal);
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_BOOLEAN:
                    attrClass = Boolean.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : Boolean.parseBoolean(attrVal);
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_BYTE:
                    attrClass = Byte.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : Byte.parseByte(attrVal);
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_LONG:
                case AtlasBaseTypeDef.ATLAS_TYPE_DATE:
                    attrClass = Long.class;
                    String rangeStart = "";
                    String rangeEnd   = "";
                    if (op == SearchParameters.Operator.TIME_RANGE) {
                        String[] parts = attrVal.split(ATTRIBUTE_VALUE_DELIMITER);
                        if (parts.length == 2) {
                            rangeStart = parts[0];
                            rangeEnd   = parts[1];
                        }
                    }
                    if (StringUtils.isNotEmpty(rangeStart) && StringUtils.isNotEmpty(rangeEnd)) {
                        attrValue  = Long.parseLong(rangeStart);
                        attrValue2 = Long.parseLong(rangeEnd);
                    } else {
                        attrValue = StringUtils.isEmpty(attrVal) ? null : Long.parseLong(attrVal);
                    }
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_FLOAT:
                    attrClass = Float.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : Float.parseFloat(attrVal);
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_DOUBLE:
                    attrClass = Double.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : Double.parseDouble(attrVal);
                    break;
                case AtlasBaseTypeDef.ATLAS_TYPE_BIGDECIMAL:
                    attrClass = BigDecimal.class;
                    attrValue = StringUtils.isEmpty(attrVal) ? null : new BigDecimal(attrVal);
                    break;
                default:
                    if (attrType instanceof AtlasEnumType) {
                        attrClass = String.class;
                    } else if (attrType instanceof AtlasArrayType) {
                        attrClass = List.class;
                    } else {
                        attrClass = Object.class;
                    }

                    attrValue = attrVal;
                    break;
            }

            String vertexPropertyName = attribute.getVertexPropertyName();
            if (attrValue != null && attrValue2 != null) {
                ret = predicate.generatePredicate(StringUtils.isEmpty(vertexPropertyName) ? attribute.getQualifiedName() : vertexPropertyName,
                        attrValue, attrValue2, attrClass);
            } else {
                ret = predicate.generatePredicate(
                        StringUtils.isEmpty(vertexPropertyName) ? attribute.getQualifiedName() : vertexPropertyName,
                        attrValue, attrClass);
            }
        }

        return ret;
    }

    protected AtlasGraphQuery toGraphFilterQuery(Set<? extends AtlasStructType> structTypes, FilterCriteria criteria, Set<String> graphAttributes, AtlasGraphQuery query) {
        Set<String> filterAttributes = new HashSet<>();
        filterAttributes.addAll(graphAttributes);

        if (criteria != null) {
            if (criteria.getCondition() != null) {
                if (criteria.getCondition() == Condition.AND) {
                    for (FilterCriteria filterCriteria : criteria.getCriterion()) {
                        AtlasGraphQuery nestedQuery = toGraphFilterQuery(structTypes, filterCriteria, filterAttributes, context.getGraph().query());

                        query.addConditionsFrom(nestedQuery);
                    }
                } else {
                    List<AtlasGraphQuery> orConditions = new LinkedList<>();

                    for (FilterCriteria filterCriteria : criteria.getCriterion()) {
                        AtlasGraphQuery nestedQuery = toGraphFilterQuery(structTypes, filterCriteria, filterAttributes, context.getGraph().query());

                        orConditions.add(context.getGraph().query().createChildQuery().addConditionsFrom(nestedQuery));
                    }

                    if (!orConditions.isEmpty()) {
                        query.or(orConditions);
                    }
                }
            } else if (StringUtils.isNotEmpty(criteria.getAttributeName())) {
                try {
                    ArrayList<AtlasGraphQuery> queries = new ArrayList<>();
                    for (AtlasStructType structType : structTypes) {
                        String qualifiedName = structType.getVertexPropertyName(criteria.getAttributeName());
                        if (filterAttributes.contains(qualifiedName)) {

                            String attrName                    = criteria.getAttributeName();
                            String attrValue                   = criteria.getAttributeValue();
                            SearchParameters.Operator operator = criteria.getOperator();

                            //process attribute value and attribute operator for pipeSeperated fields
                            if (isPipeSeparatedSystemAttribute(attrName)) {
                                FilterCriteria processedCriteria = processPipeSeperatedSystemAttribute(attrName, operator, attrValue);
                                attrValue = processedCriteria.getAttributeValue();
                                operator = processedCriteria.getOperator();
                            }

                            AtlasGraphQuery innerQry = context.getGraph().query().createChildQuery();
                            switch (operator) {
                                case LT:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.LESS_THAN, attrValue);
                                    break;
                                case LTE:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.LESS_THAN_EQUAL, attrValue);
                                    break;
                                case GT:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.GREATER_THAN, attrValue);
                                    break;
                                case GTE:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.GREATER_THAN_EQUAL, attrValue);
                                    break;
                                case EQ:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.EQUAL, attrValue);
                                    break;
                                case NEQ:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.NOT_EQUAL, attrValue);
                                    break;
                                case LIKE:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.MatchingOperator.REGEX, attrValue);
                                    break;
                                case CONTAINS:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.MatchingOperator.REGEX, getContainsRegex(attrValue));
                                    break;
                                case STARTS_WITH:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.MatchingOperator.PREFIX, attrValue);
                                    break;
                                case ENDS_WITH:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.MatchingOperator.REGEX, getSuffixRegex(attrValue));
                                    break;
                                case IS_NULL:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.EQUAL, null);
                                    break;
                                case NOT_NULL:
                                    innerQry.has(qualifiedName, AtlasGraphQuery.ComparisionOperator.NOT_EQUAL, null);
                                    break;
                                case NOT_CONTAINS:
                                    break;
                                default:
                                    LOG.warn("{}: unsupported operator. Ignored", operator);
                                    break;
                            }
                            queries.add(context.getGraph().query().createChildQuery().addConditionsFrom(innerQry));
                            filterAttributes.remove(qualifiedName);
                        }
                    }

                    if (CollectionUtils.isNotEmpty(queries)) {
                        if (queries.size() > 1) {
                            return context.getGraph().query().createChildQuery().or(queries);
                        } else {
                            return queries.iterator().next();
                        }
                    }
                } catch (AtlasBaseException e) {
                    LOG.warn(e.getMessage());
                }
            }
        }

        return query;
    }

    private String toGremlinComparisonQuery(AtlasAttribute attribute, SearchParameters.Operator operator, String attrValue, Map<String, Object> queryBindings) {
        String bindName  = "__bind_" + queryBindings.size();
        Object bindValue = attribute.getAttributeType().getNormalizedValue(attrValue);

        AtlasGremlinQueryProvider queryProvider = AtlasGremlinQueryProvider.INSTANCE;
        String queryTemplate = null;
        switch (operator) {
            case LT:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_LT);
                break;
            case GT:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_GT);
                break;
            case LTE:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_LTE);
                break;
            case GTE:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_GTE);
                break;
            case EQ:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_EQ);
                break;
            case NEQ:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_NEQ);
                break;
            case LIKE:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_MATCHES);
                break;
            case STARTS_WITH:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_STARTS_WITH);
                break;
            case ENDS_WITH:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_ENDS_WITH);
                break;
            case CONTAINS:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_CONTAINS);
                break;
            case IS_NULL:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_IS_NULL);
                break;
            case NOT_NULL:
                queryTemplate = queryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.COMPARE_NOT_NULL);
                break;
        }

        if (org.apache.commons.lang3.StringUtils.isNotEmpty(queryTemplate)) {
            if (bindValue instanceof Date) {
                bindValue = ((Date)bindValue).getTime();
            }

            queryBindings.put(bindName, bindValue);

            return String.format(queryTemplate, attribute.getQualifiedName(), bindName);
        } else {
            return EMPTY_STRING;
        }
    }

    private static String getContainsRegex(String attributeValue) {
        return ".*" + escapeRegExChars(attributeValue) + ".*";
    }

    private static String getRegexString(String value) {
        return ".*" + value.replace("*", ".*") + ".*";
    }

    private static String getSuffixRegex(String attributeValue) {
        return ".*" + escapeRegExChars(attributeValue);
    }

    private static String escapeRegExChars(String val) {
        StringBuilder escapedVal = new StringBuilder();

        for (int i = 0; i < val.length(); i++) {
            final char c = val.charAt(i);

            if (isRegExSpecialChar(c)) {
                escapedVal.append('\\');
            }

            escapedVal.append(c);
        }

        return escapedVal.toString();
    }

    private static boolean isRegExSpecialChar(char c) {
        switch (c) {
            case '+':
            case '|':
            case '(':
            case '{':
            case '[':
            case '*':
            case '?':
            case '$':
            case '/':
            case '^':
                return true;
        }

        return false;
    }

    private static boolean hasIndexQuerySpecialChar(String attributeValue) {
        if (attributeValue == null) {
            return false;
        }

        for (int i = 0; i < attributeValue.length(); i++) {
            if (isIndexQuerySpecialChar(attributeValue.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isIndexQuerySpecialChar(char c) {
        switch (c) {
            case '#':
            case '$':
            case '%':
            case '@':
            case '=':
                return true;
        }

        return false;
    }

    protected Collection<AtlasVertex> getVerticesFromIndexQueryResult(Iterator<AtlasIndexQuery.Result> idxQueryResult, Collection<AtlasVertex> vertices) {
        if (idxQueryResult != null) {
            while (idxQueryResult.hasNext()) {
                AtlasVertex vertex = idxQueryResult.next().getVertex();

                vertices.add(vertex);
            }
        }

        return vertices;
    }

    protected LinkedHashMap<Integer, AtlasVertex> getVerticesFromIndexQueryResult(Iterator<AtlasIndexQuery.Result> idxQueryResult, LinkedHashMap<Integer, AtlasVertex> offsetEntityVertexMap, int qryOffset) {
        if (idxQueryResult != null) {
            while (idxQueryResult.hasNext()) {
                AtlasVertex vertex = idxQueryResult.next().getVertex();

                offsetEntityVertexMap.put(qryOffset++, vertex);
            }
        }

        return offsetEntityVertexMap;
    }

    protected Collection<AtlasVertex> getVertices(Iterator<AtlasVertex> iterator, Collection<AtlasVertex> vertices) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                AtlasVertex vertex = iterator.next();

                vertices.add(vertex);
            }
        }

        return vertices;
    }

    protected LinkedHashMap<Integer, AtlasVertex> getVertices(Iterator<AtlasVertex> iterator, LinkedHashMap<Integer, AtlasVertex> offsetEntityVertexMap, int qryOffset) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                AtlasVertex vertex = iterator.next();

                offsetEntityVertexMap.put(qryOffset++, vertex);
            }
        }

        return offsetEntityVertexMap;
    }

    protected Set<String> getGuids(List<AtlasVertex> vertices) {
        Set<String> ret = new HashSet<>();

        if (vertices != null) {
            for(AtlasVertex vertex : vertices) {
                String guid = AtlasGraphUtilsV2.getIdFromVertex(vertex);

                if (StringUtils.isNotEmpty(guid)) {
                    ret.add(guid);
                }
            }
        }

        return ret;
    }

    private static int getApplicationProperty(String propertyName, int defaultValue) {
        try {
            return ApplicationProperties.get().getInt(propertyName, defaultValue);
        } catch (AtlasException excp) {
            // ignore
        }

        return defaultValue;
    }

    private static String getSortByAttribute(SearchContext context) {
        if (CollectionUtils.isNotEmpty(context.getEntityTypes())) {
            final AtlasEntityType entityType = context.getEntityTypes().iterator().next();
            String sortBy = context.getSearchParameters().getSortBy();
            AtlasStructType.AtlasAttribute sortByAttribute = entityType.getAttribute(sortBy);
            if (sortByAttribute != null) {
                return sortByAttribute.getVertexPropertyName();
            }
        }
        return null;
    }

    private static Order getSortOrderAttribute(SearchContext context) {
        SortOrder sortOrder = context.getSearchParameters().getSortOrder();
        if (sortOrder == null) sortOrder = ASCENDING;

        return sortOrder == SortOrder.ASCENDING ? Order.asc : Order.desc;
    }

    protected static Iterator<AtlasIndexQuery.Result> executeIndexQuery(SearchContext context, AtlasIndexQuery indexQuery, int qryOffset, int limit) {
        String sortBy = getSortByAttribute(context);
        if (sortBy != null && !sortBy.isEmpty()) {
            Order sortOrder = getSortOrderAttribute(context);
            return indexQuery.vertices(qryOffset, limit, sortBy, sortOrder);
        }
        return indexQuery.vertices(qryOffset, limit);
    }
}
