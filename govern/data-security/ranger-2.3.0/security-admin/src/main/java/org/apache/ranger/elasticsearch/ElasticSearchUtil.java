/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.elasticsearch;

import org.apache.ranger.common.*;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ElasticSearchUtil {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchUtil.class);

    @Autowired
    StringUtil stringUtil;

    String dateFormateStr = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormateStr);

    public ElasticSearchUtil() {
        String timeZone = PropertiesUtil.getProperty("xa.elasticSearch.timezone");
        if (timeZone != null) {
            logger.info("Setting timezone to " + timeZone);
            try {
                dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
            } catch (Throwable t) {
                logger.error("Error setting timezone. TimeZone = " + timeZone);
            }
        }
    }

    public SearchResponse searchResources(SearchCriteria searchCriteria, List<SearchField> searchFields, List<SortField> sortFields, RestHighLevelClient client, String index) throws IOException {
        // See Also: https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-query-builders.html
        QueryAccumulator queryAccumulator = new QueryAccumulator(searchCriteria);
        if (searchCriteria.getParamList() != null) {
            searchFields.stream().forEach(queryAccumulator::addQuery);
            // For now assuming there is only date field where range query will
            // be done. If we there are more than one, then we should create a
            // hashmap for each field name
            if (queryAccumulator.fromDate != null || queryAccumulator.toDate != null) {
                queryAccumulator.queries.add(setDateRange(queryAccumulator.dateFieldName, queryAccumulator.fromDate, queryAccumulator.toDate));
            }
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        queryAccumulator.queries.stream().filter(x -> x != null).forEach(boolQueryBuilder::must);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        setSortClause(searchCriteria, sortFields, searchSourceBuilder);
        searchSourceBuilder.from(searchCriteria.getStartIndex());
        searchSourceBuilder.size(searchCriteria.getMaxRows());
        searchSourceBuilder.fetchSource(true);
        SearchRequest query = new SearchRequest();
        query.indices(index);
        query.source(searchSourceBuilder.query(boolQueryBuilder));
        return client.search(query, RequestOptions.DEFAULT);
    }

    public void setSortClause(SearchCriteria searchCriteria,
                              List<SortField> sortFields,
                              SearchSourceBuilder searchSourceBuilder) {

        // TODO: We are supporting single sort field only for now
        String sortBy = searchCriteria.getSortBy();
        String querySortBy = null;
        if (!stringUtil.isEmpty(sortBy)) {
            sortBy = sortBy.trim();
            for (SortField sortField : sortFields) {
                if (sortBy.equalsIgnoreCase(sortField.getParamName())) {
                    querySortBy = sortField.getFieldName();
                    // Override the sortBy using the normalized value
                    searchCriteria.setSortBy(sortField.getParamName());
                    break;
                }
            }
        }

        if (querySortBy == null) {
            for (SortField sortField : sortFields) {
                if (sortField.isDefault()) {
                    querySortBy = sortField.getFieldName();
                    // Override the sortBy using the default value
                    searchCriteria.setSortBy(sortField.getParamName());
                    searchCriteria.setSortType(sortField.getDefaultOrder().name());
                    break;
                }
            }
        }

        if (querySortBy != null) {
            // Add sort type
            String sortType = searchCriteria.getSortType();
            SortOrder order = SortOrder.ASC;
            if (sortType != null && "desc".equalsIgnoreCase(sortType)) {
                order = SortOrder.DESC;
            }
            searchSourceBuilder.sort(querySortBy, order);
        }
    }

    public QueryBuilder orList(String fieldName, Collection<?> valueList) {
        if (valueList == null || valueList.isEmpty()) {
            return null;
        }
        if (valueList.isEmpty()) {
            return null;
        } else {
            return QueryBuilders.queryStringQuery(valueList.stream()
                    .map(this::filterText)
                    .map(x -> "(" + x + ")")
                    .reduce((a, b) -> a + " OR " + b)
                    .get()
            ).defaultField(fieldName);
        }
    }


    private String filterText(Object value) {
        return ClientUtils.escapeQueryChars(value.toString().trim().toLowerCase());
    }

    public QueryBuilder setDateRange(String fieldName, Date fromDate, Date toDate) {
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(fieldName).format(dateFormateStr);
        if (fromDate != null) {
            rangeQueryBuilder.from(dateFormat.format(fromDate));
        }
        if (toDate != null) {
            rangeQueryBuilder.to(dateFormat.format(toDate));
        }
        return rangeQueryBuilder;
    }

    public MultiGetItemResponse[] fetch(RestHighLevelClient client, String index, SearchHit... hits) throws IOException {
        if(0 == hits.length) {
            return new MultiGetItemResponse[0];
        }
        MultiGetRequest multiGetRequest = new MultiGetRequest();
        for (SearchHit hit : hits) {
            MultiGetRequest.Item item = new MultiGetRequest.Item(index, null, hit.getId());
            item.fetchSourceContext(FetchSourceContext.FETCH_SOURCE);
            multiGetRequest.add(item);
        }
        return client.multiGet(multiGetRequest, RequestOptions.DEFAULT).getResponses();
    }

    private class QueryAccumulator {
        public final List<QueryBuilder> queries = new ArrayList<>();
        public final SearchCriteria searchCriteria;
        public Date fromDate;
        public Date toDate;
        public String dateFieldName;

        private QueryAccumulator(SearchCriteria searchCriteria) {
            this.searchCriteria = searchCriteria;
            this.fromDate = null;
            this.toDate = null;
            this.dateFieldName = null;
        }

        public QueryAccumulator addQuery(SearchField searchField) {
            QueryBuilder queryBuilder = getQueryBuilder(searchField);
            if (null != queryBuilder) {
                queries.add(queryBuilder);
            }
            return this;
        }

        public QueryBuilder getQueryBuilder(SearchField searchField) {
            String clientFieldName = searchField.getClientFieldName();
            String fieldName = searchField.getFieldName();
            SearchField.DATA_TYPE dataType = searchField.getDataType();
            SearchField.SEARCH_TYPE searchType = searchField.getSearchType();
            Object paramValue = searchCriteria.getParamValue(clientFieldName);
            return getQueryBuilder(dataType, searchType, fieldName, paramValue);
        }

        private QueryBuilder getQueryBuilder(SearchField.DATA_TYPE dataType, SearchField.SEARCH_TYPE searchType, String fieldName, Object paramValue) {
            if (paramValue == null || paramValue.toString().isEmpty()) {
                return null;
            }
            if (fieldName.startsWith("-")) {
                QueryBuilder negativeQuery = getQueryBuilder(dataType, searchType, fieldName.substring(1), paramValue);
                return null == negativeQuery ? null : QueryBuilders.boolQuery().mustNot(negativeQuery);
            }
            if (paramValue instanceof Collection) {
                Collection<?> valueList = (Collection<?>) paramValue;
                if (valueList.isEmpty()) {
                    return null;
                } else {
                    return QueryBuilders.queryStringQuery(valueList.stream()
                            .map(ElasticSearchUtil.this::filterText)
                            .map(x -> "(" + x + ")")
                            .reduce((a, b) -> a + " OR " + b)
                            .get()
                    ).defaultField(fieldName);
                }
            } else {
                if (dataType == SearchField.DATA_TYPE.DATE) {
                    if (!(paramValue instanceof Date)) {
                        logger.error(String.format(
                            "Search value is not a Java Date Object: %s %s %s",
                            fieldName, searchType, paramValue));
                    } else {
                        if (searchType == SearchField.SEARCH_TYPE.GREATER_EQUAL_THAN
                                || searchType == SearchField.SEARCH_TYPE.GREATER_THAN) {
                            fromDate = (Date) paramValue;
                            dateFieldName = fieldName;
                        } else if (searchType == SearchField.SEARCH_TYPE.LESS_EQUAL_THAN
                                || searchType == SearchField.SEARCH_TYPE.LESS_THAN) {
                            toDate = (Date) paramValue;
                            dateFieldName = fieldName;
                        }
                    }
                    return null;
                } else if (searchType == SearchField.SEARCH_TYPE.GREATER_EQUAL_THAN
                        || searchType == SearchField.SEARCH_TYPE.GREATER_THAN
                        || searchType == SearchField.SEARCH_TYPE.LESS_EQUAL_THAN
                        || searchType == SearchField.SEARCH_TYPE.LESS_THAN) { //NOPMD
                    logger.warn(String.format("Range Queries Not Implemented: %s %s %s",
                        fieldName, searchType, paramValue));
                    return null;
                } else {
                    if (searchType == SearchField.SEARCH_TYPE.PARTIAL) {
                        if (paramValue.toString().trim().length() == 0) {
                            return null;
                        } else {
                            return QueryBuilders.queryStringQuery("*" + filterText(paramValue) + "*").defaultField(fieldName);
                        }
                    } else {
                        if (paramValue.toString().trim().length() > 0) {
                            return QueryBuilders.matchPhraseQuery(fieldName, filterText(paramValue));
                        } else {
                            return null;
                        }
                    }
                }
            }
        }

    }
}
