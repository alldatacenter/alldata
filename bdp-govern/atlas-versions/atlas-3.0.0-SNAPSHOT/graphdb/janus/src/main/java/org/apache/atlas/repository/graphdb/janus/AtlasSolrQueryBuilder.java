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

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.SearchParameters.FilterCriteria;
import org.apache.atlas.model.discovery.SearchParameters.Operator;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.atlas.repository.Constants.CUSTOM_ATTRIBUTES_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.CLASSIFICATION_NAMES_KEY;
import static org.apache.atlas.repository.Constants.PROPAGATED_CLASSIFICATION_NAMES_KEY;
import static org.apache.atlas.repository.Constants.LABELS_PROPERTY_KEY;

public class AtlasSolrQueryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasSolrQueryBuilder.class);

    private Set<AtlasEntityType> entityTypes;
    private String               queryString;
    private FilterCriteria       criteria;
    private boolean              excludeDeletedEntities;
    private boolean              includeSubtypes;
    private Map<String, String>  indexFieldNameCache;
    public static final char     CUSTOM_ATTR_SEPARATOR      = '=';
    public static final String   CUSTOM_ATTR_SEARCH_FORMAT  = "\"\\\"%s\\\":\\\"%s\\\"\"";


    public AtlasSolrQueryBuilder() {
    }

    public AtlasSolrQueryBuilder withEntityTypes(Set<AtlasEntityType> searchForEntityTypes) {
        this.entityTypes = searchForEntityTypes;

        return this;
    }

    public AtlasSolrQueryBuilder withQueryString(String queryString) {
        this.queryString = queryString;

        return this;
    }

    public AtlasSolrQueryBuilder withCriteria(FilterCriteria criteria) {
        this.criteria = criteria;

        return this;
    }

    public AtlasSolrQueryBuilder withExcludedDeletedEntities(boolean excludeDeletedEntities) {
        this.excludeDeletedEntities = excludeDeletedEntities;

        return this;
    }

    public AtlasSolrQueryBuilder withIncludeSubTypes(boolean includeSubTypes) {
        this.includeSubtypes = includeSubTypes;

        return this;
    }

    public AtlasSolrQueryBuilder withCommonIndexFieldNames(Map<String, String> indexFieldNameCache) {
        this.indexFieldNameCache = indexFieldNameCache;

        return this;
    }

    public String build() throws AtlasBaseException {
        StringBuilder queryBuilder = new StringBuilder();
        boolean       isAndNeeded  = false;

        if (StringUtils.isNotEmpty(queryString)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Initial query string is {}.", queryString);
            }

            queryBuilder.append("+").append(queryString.trim()).append(" ");

            isAndNeeded = true;
        }

        if (excludeDeletedEntities) {
            if (isAndNeeded) {
                queryBuilder.append(" AND ");
            }

            dropDeletedEntities(queryBuilder);

            isAndNeeded = true;
        }

        if (CollectionUtils.isNotEmpty(entityTypes)) {
            if (isAndNeeded) {
                queryBuilder.append(" AND ");
            }

            buildForEntityType(queryBuilder);

            isAndNeeded = true;
        }

        if (criteria != null) {
            StringBuilder attrFilterQueryBuilder = new StringBuilder();

            withCriteria(attrFilterQueryBuilder, criteria);

            if (attrFilterQueryBuilder.length() != 0) {
                if (isAndNeeded) {
                    queryBuilder.append(" AND ");
                }

                queryBuilder.append(" ").append(attrFilterQueryBuilder.toString());
            }
        }

        return queryBuilder.toString();
    }

    private void buildForEntityType(StringBuilder queryBuilder) {

        String typeIndexFieldName = indexFieldNameCache.get(Constants.ENTITY_TYPE_PROPERTY_KEY);

        queryBuilder.append(" +")
                    .append(typeIndexFieldName)
                    .append(":(");

        Set<String> typesToSearch = new HashSet<>();
        for (AtlasEntityType type : entityTypes) {

            if (includeSubtypes) {
                typesToSearch.addAll(type.getTypeAndAllSubTypes());
            } else {
                typesToSearch.add(type.getTypeName());
            }
        }

        queryBuilder.append(StringUtils.join(typesToSearch, " ")).append(" ) ");

    }

    private void dropDeletedEntities(StringBuilder queryBuilder) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("excluding the deleted entities.");
        }

        String indexFieldName = indexFieldNameCache.get(Constants.STATE_PROPERTY_KEY);

        if (indexFieldName == null) {
            String msg = String.format("There is no index field name defined for attribute '%s'",
                                       Constants.STATE_PROPERTY_KEY);

            LOG.error(msg);

            throw new AtlasBaseException(msg);
        }

        queryBuilder.append(" -").append(indexFieldName).append(":").append(AtlasEntity.Status.DELETED.name());
    }

    private  AtlasSolrQueryBuilder withCriteria(StringBuilder queryBuilder, FilterCriteria criteria) throws AtlasBaseException {
        List<FilterCriteria> criterion = criteria.getCriterion();
        Set<String> indexAttributes    = new HashSet<>();
        if (StringUtils.isNotEmpty(criteria.getAttributeName()) && CollectionUtils.isEmpty(criterion)) { // no child criterion

            String   attributeName  = criteria.getAttributeName();
            String   attributeValue = criteria.getAttributeValue();
            Operator operator       = criteria.getOperator();

            ArrayList<StringBuilder> orExpQuery = new ArrayList<>();

            for (AtlasEntityType type : entityTypes) {
                String indexAttributeName = getIndexAttributeName(type, attributeName);

                //check to remove duplicate attribute query (for eg. name)
                if (!indexAttributes.contains(indexAttributeName)) {
                    StringBuilder sb   = new StringBuilder();

                    if (attributeName.equals(CUSTOM_ATTRIBUTES_PROPERTY_KEY)) {
                        // CustomAttributes stores key value pairs in String format, so ideally it should be 'contains' operator to search for one pair,
                        // for use-case, E1 having key1=value1 and E2 having key1=value2, searching key1=value1 results both E1,E2
                        // surrounding inverted commas to attributeValue works
                        if (operator.equals(Operator.CONTAINS)) {
                            operator   = Operator.EQ;
                        } else if (operator.equals(Operator.NOT_CONTAINS)) {
                            operator   = Operator.NEQ;
                        }
                        attributeValue = getIndexQueryAttributeValue(attributeValue);
                    }

                    if (attributeValue != null) {
                        attributeValue = attributeValue.trim();
                    }

                    //when wildcard search -> escape special Char, don't quote
                    //      when  tokenized characters + index field Type TEXT -> remove wildcard '*' from query

                    //'CONTAINS and NOT_CONTAINS' operator with tokenize char in attributeValue doesn't guarantee correct results
                    // for aggregationMetrics
                    boolean replaceWildcardChar = false;

                    AtlasStructDef.AtlasAttributeDef def = type.getAttributeDef(attributeName);
                    if (!isPipeSeparatedSystemAttribute(attributeName)
                            && isWildCardOperator(operator)
                            && def.getTypeName().equalsIgnoreCase(AtlasBaseTypeDef.ATLAS_TYPE_STRING)) {

                        if (def.getIndexType() == null && AtlasAttribute.hastokenizeChar(attributeValue)) {
                            replaceWildcardChar = true;
                        }
                        attributeValue  = AtlasAttribute.escapeIndexQueryValue(attributeValue, false, false);

                    } else {
                        attributeValue  = AtlasAttribute.escapeIndexQueryValue(attributeValue);
                    }

                    withPropertyCondition(sb, indexAttributeName, operator, attributeValue, replaceWildcardChar);
                    indexAttributes.add(indexAttributeName);
                    orExpQuery.add(sb);
                }
            }

            if (CollectionUtils.isNotEmpty(orExpQuery)) {
                if (orExpQuery.size() > 1) {
                    String orExpStr = StringUtils.join(orExpQuery, FilterCriteria.Condition.OR.name());
                    queryBuilder.append(" ( ").append(orExpStr).append(" ) ");
                } else {
                    queryBuilder.append(orExpQuery.iterator().next());
                }
            }

        } else if (CollectionUtils.isNotEmpty(criterion)) {
            beginCriteria(queryBuilder);

            for (Iterator<FilterCriteria> iterator = criterion.iterator(); iterator.hasNext(); ) {
                FilterCriteria childCriteria = iterator.next();

                withCriteria(queryBuilder, childCriteria);

                if (iterator.hasNext()) {
                    withCondition(queryBuilder, criteria.getCondition().name());
                }
            }

            endCriteria(queryBuilder);
        }

        return this;
    }

    private void withPropertyCondition(StringBuilder queryBuilder, String indexFieldName, Operator operator, String attributeValue, boolean replaceWildCard) throws AtlasBaseException {
        if (StringUtils.isNotEmpty(indexFieldName) && operator != null) {
            beginCriteria(queryBuilder);

            switch (operator) {
                case EQ:
                    withEqual(queryBuilder, indexFieldName, attributeValue);
                    break;
                case NEQ:
                    withNotEqual(queryBuilder, indexFieldName, attributeValue);
                    break;
                case STARTS_WITH:
                    withStartsWith(queryBuilder, indexFieldName, attributeValue, replaceWildCard);
                    break;
                case ENDS_WITH:
                    withEndsWith(queryBuilder, indexFieldName, attributeValue, replaceWildCard);
                    break;
                case CONTAINS:
                    withContains(queryBuilder, indexFieldName, attributeValue, replaceWildCard);
                    break;
                case NOT_CONTAINS:
                    withNotContains(queryBuilder, indexFieldName, attributeValue, replaceWildCard);
                    break;
                case IS_NULL:
                    withIsNull(queryBuilder, indexFieldName);
                    break;
                case NOT_NULL:
                    withIsNotNull(queryBuilder, indexFieldName);
                    break;
                case LT:
                    withLessthan(queryBuilder, indexFieldName, attributeValue);
                    break;
                case GT:
                    withGreaterThan(queryBuilder, indexFieldName, attributeValue);
                    break;
                case LTE:
                    withLessthanOrEqual(queryBuilder, indexFieldName, attributeValue);
                    break;
                case GTE:
                    withGreaterThanOrEqual(queryBuilder, indexFieldName, attributeValue);
                    break;
                case IN:
                case LIKE:
                case CONTAINS_ANY:
                case CONTAINS_ALL:
                default:
                    String msg = String.format("%s is not supported operation.", operator.getSymbol());
                    LOG.error(msg);
                    throw new AtlasBaseException(msg);
            }

            endCriteria(queryBuilder);
        }
    }

    private boolean isPipeSeparatedSystemAttribute(String attrName) {
        return StringUtils.equals(attrName, CLASSIFICATION_NAMES_KEY) ||
                StringUtils.equals(attrName, PROPAGATED_CLASSIFICATION_NAMES_KEY) ||
                StringUtils.equals(attrName, LABELS_PROPERTY_KEY) ||
                StringUtils.equals(attrName, CUSTOM_ATTRIBUTES_PROPERTY_KEY);
    }

    private boolean isWildCardOperator(Operator operator) {
        return (operator == Operator.CONTAINS ||
                operator == Operator.STARTS_WITH ||
                operator == Operator.ENDS_WITH ||
                operator == Operator.NOT_CONTAINS);
    }

    private String getIndexQueryAttributeValue(String attributeValue) {

        if (StringUtils.isNotEmpty(attributeValue)) {
            int    separatorIdx = attributeValue.indexOf(CUSTOM_ATTR_SEPARATOR);
            String key          = separatorIdx != -1 ? attributeValue.substring(0, separatorIdx) : null;
            String value        = key != null ? attributeValue.substring(separatorIdx + 1) : null;

            if (key != null && value != null) {
                return String.format(CUSTOM_ATTR_SEARCH_FORMAT, key, value);
            }
        }

        return attributeValue;
    }

    private String getIndexAttributeName(AtlasEntityType type, String attrName) throws AtlasBaseException {
        AtlasAttribute ret = type.getAttribute(attrName);

        if (ret == null) {
            String msg = String.format("Received unknown attribute '%s' for type '%s'.", attrName, type.getTypeName());

            LOG.error(msg);

            throw new AtlasBaseException(msg);
        }

        String indexFieldName = ret.getIndexFieldName();

        if (indexFieldName == null) {
            String msg = String.format("Received non-index attribute %s for type %s.", attrName, type.getTypeName());

            LOG.error(msg);

            throw new AtlasBaseException(msg);
        }

        return indexFieldName;
    }

    private void beginCriteria(StringBuilder queryBuilder) {
        queryBuilder.append("( ");
    }

    private void endCriteria(StringBuilder queryBuilder) {
        queryBuilder.append(" )");
    }

    private void withEndsWith(StringBuilder queryBuilder, String indexFieldName, String attributeValue, boolean replaceWildCard) {
        String attrValuePrefix = replaceWildCard ? ":" : ":*";
        queryBuilder.append("+").append(indexFieldName).append(attrValuePrefix).append(attributeValue).append(" ");
    }

    private void withStartsWith(StringBuilder queryBuilder, String indexFieldName, String attributeValue, boolean replaceWildCard) {
        String attrValuePostfix = replaceWildCard ? " " : "* ";
        queryBuilder.append("+").append(indexFieldName).append(":").append(attributeValue).append(attrValuePostfix);
    }

    private void withNotEqual(StringBuilder queryBuilder, String indexFieldName, String attributeValue) {
        queryBuilder.append("*:* -").append(indexFieldName).append(":").append(attributeValue).append(" ");
    }

    private void withEqual(StringBuilder queryBuilder, String indexFieldName, String attributeValue) {
        queryBuilder.append("+").append(indexFieldName).append(":").append(attributeValue).append(" ");
    }

    private void withGreaterThan(StringBuilder queryBuilder, String indexFieldName, String attributeValue) {
        //{ == exclusive
        //] == inclusive
        //+__timestamp_l:{<attributeValue> TO *]
        queryBuilder.append("+").append(indexFieldName).append(":{ ").append(attributeValue).append(" TO * ] ");
    }

    private void withGreaterThanOrEqual(StringBuilder queryBuilder, String indexFieldName, String attributeValue) {
        //[ == inclusive
        //] == inclusive
        //+__timestamp_l:[<attributeValue> TO *]
        queryBuilder.append("+").append(indexFieldName).append(":[ ").append(attributeValue).append(" TO * ] ");
    }

    private void withLessthan(StringBuilder queryBuilder, String indexFieldName, String attributeValue) {
        //[ == inclusive
        //} == exclusive
        //+__timestamp_l:[* TO <attributeValue>}
        queryBuilder.append("+").append(indexFieldName).append(":[ * TO ").append(attributeValue).append("} ");
    }

    private void withLessthanOrEqual(StringBuilder queryBuilder, String indexFieldName, String attributeValue) {
        //[ == inclusive
        //[ == inclusive
        //+__timestamp_l:[* TO <attributeValue>]
        queryBuilder.append("+").append(indexFieldName).append(":[ * TO ").append(attributeValue).append(" ] ");
    }

    private void withContains(StringBuilder queryBuilder, String indexFieldName, String attributeValue, boolean replaceWildCard) {
        String attrValuePrefix  = replaceWildCard ? ":" : ":*";
        String attrValuePostfix = replaceWildCard ? " " : "* ";

        queryBuilder.append("+").append(indexFieldName).append(attrValuePrefix).append(attributeValue).append(attrValuePostfix);
    }

    private void withNotContains(StringBuilder queryBuilder, String indexFieldName, String attributeValue, boolean replaceWildCard) {
        String attrValuePrefix  = replaceWildCard ? ":" : ":*";
        String attrValuePostfix = replaceWildCard ? " " : "* ";

        queryBuilder.append("*:* -").append(indexFieldName).append(attrValuePrefix).append(attributeValue).append(attrValuePostfix);

    }

    private void withIsNull(StringBuilder queryBuilder, String indexFieldName) {
        queryBuilder.append("-").append(indexFieldName).append(":*").append(" ");
    }

    private void withIsNotNull(StringBuilder queryBuilder, String indexFieldName) {
        queryBuilder.append("+").append(indexFieldName).append(":*").append(" ");
    }

    private void withCondition(StringBuilder queryBuilder, String condition) {
        queryBuilder.append(" ").append(condition).append(" ");
    }
}
