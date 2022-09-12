/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.core.impl;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.AuditQuerySource;
import org.apache.inlong.manager.common.pojo.audit.AuditInfo;
import org.apache.inlong.manager.common.pojo.audit.AuditRequest;
import org.apache.inlong.manager.common.pojo.audit.AuditVO;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.mapper.AuditEntityMapper;
import org.apache.inlong.manager.service.core.AuditService;
import org.apache.inlong.manager.service.resource.es.ElasticsearchApi;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.ParsedSum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Audit service layer implementation
 */
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);

    @Value("${audit.query.source}")
    private String auditQuerySource = AuditQuerySource.MYSQL.name();

    @Autowired
    private AuditEntityMapper auditEntityMapper;
    @Autowired
    private ElasticsearchApi elasticsearchApi;

    @Override
    public List<AuditVO> listByCondition(AuditRequest request) throws IOException {
        LOGGER.info("begin query audit list request={}", request);
        Preconditions.checkNotNull(request, "request is null");
        List<AuditVO> result = new ArrayList<>();
        AuditQuerySource querySource = AuditQuerySource.valueOf(auditQuerySource);
        for (String auditId : request.getAuditIds()) {
            if (AuditQuerySource.MYSQL == querySource) {
                String format = "%Y-%m-%d %H:%i:00";
                // Support min agg at now
                DateTimeFormatter forPattern = DateTimeFormat.forPattern("yyyy-MM-dd");
                DateTime dtDate = forPattern.parseDateTime(request.getDt());
                String eDate = dtDate.plusDays(1).toString(forPattern);
                List<Map<String, Object>> sumList = auditEntityMapper
                        .sumByLogTs(request.getInlongGroupId(), request.getInlongStreamId(),
                                auditId, request.getDt(), eDate, format);
                List<AuditInfo> auditSet = sumList.stream().map(s -> {
                    AuditInfo vo = new AuditInfo();
                    vo.setLogTs((String) s.get("logTs"));
                    vo.setCount(((BigDecimal) s.get("total")).longValue());
                    return vo;
                }).collect(Collectors.toList());
                result.add(new AuditVO(auditId, auditSet));
            } else if (AuditQuerySource.ELASTICSEARCH == querySource) {
                String index = String.format("%s_%s",
                        request.getDt().replaceAll("-", ""), auditId);
                if (elasticsearchApi.indexExists(index)) {
                    SearchResponse response = elasticsearchApi
                            .search(toAuditSearchRequest(index, request.getInlongGroupId(),
                                    request.getInlongStreamId()));
                    final List<Aggregation> aggregations = response.getAggregations().asList();
                    if (CollectionUtils.isNotEmpty(aggregations)) {
                        ParsedTerms terms = (ParsedTerms) aggregations.get(0);
                        if (CollectionUtils.isNotEmpty(terms.getBuckets())) {
                            List<AuditInfo> auditSet = terms.getBuckets().stream().map(bucket -> {
                                AuditInfo vo = new AuditInfo();
                                vo.setLogTs(bucket.getKeyAsString());
                                vo.setCount((long) ((ParsedSum) bucket.getAggregations().asList().get(0)).getValue());
                                return vo;
                            }).collect(Collectors.toList());
                            result.add(new AuditVO(auditId, auditSet));
                        }
                    }
                } else {
                    LOGGER.warn("Elasticsearch index={} not exists", index);
                }
            }
        }
        LOGGER.info("success to query audit list for request={}", request);
        return result;
    }

    /**
     * Convert to elasticsearch search request
     *
     * @param index The index of elasticsearch
     * @param groupId The groupId of inlong
     * @param streamId The streamId of inlong
     * @return The search request of elasticsearch
     */
    private SearchRequest toAuditSearchRequest(String index, String groupId, String streamId) {
        TermsAggregationBuilder aggrBuilder = AggregationBuilders.terms("log_ts").field("log_ts")
                .size(Integer.MAX_VALUE).subAggregation(AggregationBuilders.sum("count").field("count"));
        BoolQueryBuilder filterBuilder = new BoolQueryBuilder();
        filterBuilder.must(termQuery("inlong_group_id", groupId));
        filterBuilder.must(termQuery("inlong_stream_id", streamId));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(aggrBuilder);
        sourceBuilder.query(filterBuilder);
        sourceBuilder.from(0);
        sourceBuilder.size(0);
        sourceBuilder.sort("log_ts", SortOrder.ASC);
        return new SearchRequest(new String[]{index}, sourceBuilder);
    }

}