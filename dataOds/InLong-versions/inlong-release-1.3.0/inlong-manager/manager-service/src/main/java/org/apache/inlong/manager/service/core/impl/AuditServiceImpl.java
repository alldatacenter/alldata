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

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.AuditQuerySource;
import org.apache.inlong.manager.common.enums.TimeStaticsDim;
import org.apache.inlong.manager.pojo.audit.AuditInfo;
import org.apache.inlong.manager.pojo.audit.AuditRequest;
import org.apache.inlong.manager.pojo.audit.AuditVO;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.mapper.AuditEntityMapper;
import org.apache.inlong.manager.service.core.AuditService;
import org.apache.inlong.manager.service.resource.sink.es.ElasticsearchApi;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * Audit service layer implementation
 */
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);
    private static final String HOUR_FORMAT = "yyyy-MM-dd HH";
    private static final String DAY_FORMAT = "yyyy-MM-dd";

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

        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        List<AuditVO> result = new ArrayList<>();
        AuditQuerySource querySource = AuditQuerySource.valueOf(auditQuerySource);
        for (String auditId : request.getAuditIds()) {
            if (AuditQuerySource.MYSQL == querySource) {
                String format = "%Y-%m-%d %H:%i:00";
                // Support min agg at now
                DateTimeFormatter forPattern = DateTimeFormat.forPattern("yyyy-MM-dd");
                DateTime dtDate = forPattern.parseDateTime(request.getDt());
                String eDate = dtDate.plusDays(1).toString(forPattern);
                List<Map<String, Object>> sumList = auditEntityMapper.sumByLogTs(
                        groupId, streamId, auditId, request.getDt(), eDate, format);
                List<AuditInfo> auditSet = sumList.stream().map(s -> {
                    AuditInfo vo = new AuditInfo();
                    vo.setLogTs((String) s.get("logTs"));
                    vo.setCount(((BigDecimal) s.get("total")).longValue());
                    return vo;
                }).collect(Collectors.toList());
                result.add(new AuditVO(auditId, auditSet));
            } else if (AuditQuerySource.ELASTICSEARCH == querySource) {
                String index = String.format("%s_%s", request.getDt().replaceAll("-", ""), auditId);
                if (!elasticsearchApi.indexExists(index)) {
                    LOGGER.warn("elasticsearch index={} not exists", index);
                    continue;
                }
                SearchResponse response = elasticsearchApi.search(toAuditSearchRequest(index, groupId, streamId));
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
            }
        }
        LOGGER.info("success to query audit list for request={}", request);
        return aggregateByTimeDim(result, request.getTimeStaticsDim());
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
        TermsAggregationBuilder builder = AggregationBuilders.terms("log_ts").field("log_ts")
                .size(Integer.MAX_VALUE).subAggregation(AggregationBuilders.sum("count").field("count"));
        BoolQueryBuilder filterBuilder = new BoolQueryBuilder();
        filterBuilder.must(termQuery("inlong_group_id", groupId));
        filterBuilder.must(termQuery("inlong_stream_id", streamId));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(builder);
        sourceBuilder.query(filterBuilder);
        sourceBuilder.from(0);
        sourceBuilder.size(0);
        sourceBuilder.sort("log_ts", SortOrder.ASC);
        return new SearchRequest(new String[]{index}, sourceBuilder);
    }

    /**
     * Aggregate by time dim
     */
    private List<AuditVO> aggregateByTimeDim(List<AuditVO> auditVOList, TimeStaticsDim timeStaticsDim) {
        List<AuditVO> result;
        switch (timeStaticsDim) {
            case HOUR:
                result = doAggregate(auditVOList, HOUR_FORMAT);
                break;
            case DAY:
                result = doAggregate(auditVOList, DAY_FORMAT);
                break;
            default:
                result = auditVOList;
                break;
        }
        return result;
    }

    /**
     * Execute the aggregate by the given time format
     */
    private List<AuditVO> doAggregate(List<AuditVO> auditVOList, String format) {
        List<AuditVO> result = new ArrayList<>();
        for (AuditVO auditVO : auditVOList) {
            AuditVO statInfo = new AuditVO();
            ConcurrentHashMap<String, AtomicLong> countMap = new ConcurrentHashMap<>();
            statInfo.setAuditId(auditVO.getAuditId());
            for (AuditInfo auditInfo : auditVO.getAuditSet()) {
                String statKey = formatLogTime(auditInfo.getLogTs(), format);
                if (statKey == null) {
                    continue;
                }
                if (countMap.get(statKey) == null) {
                    countMap.put(statKey, new AtomicLong(0));
                }
                countMap.get(statKey).addAndGet(auditInfo.getCount());
            }

            List<AuditInfo> auditInfoList = new LinkedList<>();
            for (Map.Entry<String, AtomicLong> entry : countMap.entrySet()) {
                AuditInfo auditInfoStat = new AuditInfo();
                auditInfoStat.setLogTs(entry.getKey());
                auditInfoStat.setCount(entry.getValue().get());
                auditInfoList.add(auditInfoStat);
            }
            statInfo.setAuditSet(auditInfoList);
            result.add(statInfo);
        }
        return result;
    }

    /**
     * Format the log time
     */
    private String formatLogTime(String dateString, String format) {
        String formatDateString = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            Date date = formatter.parse(dateString);
            formatDateString = formatter.format(date);
        } catch (Exception e) {
            LOGGER.error("format lot time exception", e);
        }
        return formatDateString;
    }
}
