package com.alibaba.sreworks.job.master.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import com.alibaba.sreworks.job.master.domain.DTO.ElasticJobInstanceDTO;
import com.alibaba.sreworks.job.master.domain.DTO.JobInstanceStatus;
import com.alibaba.sreworks.job.master.domain.repository.ElasticJobInstanceRepository;
import com.alibaba.sreworks.job.master.jobschedule.JobScheduleService;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.sreworks.job.utils.StringUtil;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery.Refresh;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JobInstanceService {

    @Autowired
    ElasticJobInstanceRepository jobInstanceRepository;

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    JobScheduleService jobScheduleService;

    public Page<ElasticJobInstance> list(Long jobId, String status, Pageable pageable) {
        List<String> statusList = StringUtil.isEmpty(status) ? JobInstanceStatus.nameValues() :
            Arrays.asList(status.split(","));

        if (jobId == null) {
            return jobInstanceRepository
                .findAllByStatusInOrderByGmtCreateDesc(statusList, pageable);
        } else {
            return jobInstanceRepository
                .findAllByStatusInAndJobIdOrderByGmtCreateDesc(statusList, jobId, pageable);
        }
    }

    public SearchHits<ElasticJobInstance> list2(Long jobId, String status, Pageable pageable, String id) {
        List<String> statusList = StringUtil.isEmpty(status) ? JobInstanceStatus.nameValues() :
            Arrays.asList(status.split(","));

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (jobId != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("jobId", jobId));
        }
        if (StringUtils.isNotEmpty(id)) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("id", id));
        }
        boolQueryBuilder.must(QueryBuilders.termsQuery("status", statusList));
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQueryBuilder)
            .withPageable(pageable)
            .withSort(new FieldSortBuilder("gmtCreate").order(SortOrder.DESC))
            .build();

        return elasticsearchRestTemplate.search(
            searchQuery,
            ElasticJobInstance.class
        );

    }

    public SearchHits<ElasticJobInstance> listByTraceId(String traceId, Pageable pageable) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termsQuery("traceIds", traceId));
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQueryBuilder)
            .withPageable(pageable)
            .withSort(new FieldSortBuilder("gmtCreate").order(SortOrder.DESC))
            .build();

        return elasticsearchRestTemplate.search(
            searchQuery,
            ElasticJobInstance.class
        );

    }

    public ElasticJobInstanceDTO get(String id) throws Exception {
        ElasticJobInstance jobInstance = jobInstanceRepository.findFirstById(id);
        ElasticJobInstanceDTO jobInstanceDTO = new ElasticJobInstanceDTO(jobInstance);
        jobInstanceDTO.setScheduleInstance(
            jobScheduleService
                .getJobSchedule(jobInstanceDTO.getJob().getScheduleType())
                .get(jobInstanceDTO.getScheduleInstanceId())
        );
        return jobInstanceDTO;
    }

    public void stop(String id) throws Exception {
        ElasticJobInstance jobInstance = jobInstanceRepository.findFirstById(id);
        ElasticJobInstanceDTO jobInstanceDTO = new ElasticJobInstanceDTO(jobInstance);
        jobInstance.setStatus(JobInstanceStatus.STOP.name());
        jobInstance.setGmtStop(System.currentTimeMillis());
        jobScheduleService
            .getJobSchedule(jobInstanceDTO.getJob().getScheduleType())
            .stop(jobInstanceDTO.getScheduleInstanceId());
        jobInstanceRepository.save(jobInstance);
    }

    public void update(String id, JSONObject jsonObject) {
        elasticsearchRestTemplate.update(
            UpdateQuery.builder(id)
                .withDocument(Document.from(jsonObject))
                .withRefresh(Refresh.True)
                .withRetryOnConflict(2)
                .build(),
            IndexCoordinates.of("sreworks-job-instance")
        );
    }

    public List<JSONObject> distinctOrderByMaxGmtCreate(
        Long jobId, Long stime, Long etime, String term, String field) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
            .must(QueryBuilders.rangeQuery("gmtCreate").gte(stime).lte(etime));
        if (jobId != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("jobId", jobId));
        }
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQueryBuilder)
            .addAggregation(AggregationBuilders
                .terms(term)
                .field(field)
                .size(Integer.MAX_VALUE)
                .order(BucketOrder.aggregation("maxGmtCreate.value", false))
                .subAggregation(AggregationBuilders
                    .max("maxGmtCreate")
                    .field("gmtCreate")
                )
            )
            //.withPageable(PageRequest.of(0, 10))
            .build();

        SearchHits<ElasticJobInstance> searchHits = elasticsearchRestTemplate.search(
            searchQuery,
            ElasticJobInstance.class
        );

        Aggregation aggregation = Objects.requireNonNull(searchHits.getAggregations()).get(term);
        return ((ParsedStringTerms)aggregation).getBuckets().stream().map(bucket -> JsonUtil.map(
            term, bucket.getKeyAsString(),
            "count", bucket.getDocCount(),
            "maxGmtCreate", (long)((ParsedMax)bucket.getAggregations().get("maxGmtCreate")).getValue()
        )).collect(Collectors.toList());

    }

    public List<JSONObject> groupByStatus(String sceneType, long stime, long etime) {

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("job", sceneType))
                .must(QueryBuilders.rangeQuery("gmtCreate").gte(stime).lte(etime))
            )
            .addAggregation(AggregationBuilders
                .terms("status")
                .field("status")
                .size(Integer.MAX_VALUE)
            )
            //.withPageable(PageRequest.of(0, 10))
            .build();
        if (elasticsearchRestTemplate.indexExists("sreworks-job-instance")) {
            SearchHits<ElasticJobInstance> searchHits = elasticsearchRestTemplate.search(
                searchQuery,
                ElasticJobInstance.class
            );
            Aggregation aggregation = Objects.requireNonNull(searchHits.getAggregations()).get("status");
            return ((ParsedStringTerms)aggregation).getBuckets().stream().map(bucket -> JsonUtil.map(
                "status", bucket.getKeyAsString(),
                "count", bucket.getDocCount()
            )).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }

    }

    public List<JSONObject> groupByTraceId(long stime, long etime) {

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("gmtCreate").gte(stime).lte(etime))
            )
            .addAggregation(AggregationBuilders
                .terms("traceId")
                .field("traceIds")
                .size(Integer.MAX_VALUE)
                .subAggregation(AggregationBuilders
                    .terms("status")
                    .field("status")
                    .size(Integer.MAX_VALUE)
                )
                .subAggregation(AggregationBuilders
                    .max("maxGmtCreate")
                    .field("gmtCreate")
                )
                .subAggregation(AggregationBuilders
                    .min("minGmtCreate")
                    .field("gmtCreate")
                )
            )
            .withPageable(PageRequest.of(0, 1))
            .build();
        if (elasticsearchRestTemplate.indexExists("sreworks-job-instance")) {
            SearchHits<ElasticJobInstance> searchHits = elasticsearchRestTemplate.search(
                searchQuery,
                ElasticJobInstance.class
            );
            Aggregation aggregation = Objects.requireNonNull(searchHits.getAggregations()).get("traceId");
            return ((ParsedStringTerms)aggregation).getBuckets().stream().map(bucket -> {
                Aggregation statusAgg = bucket.getAggregations().get("status");
                return JsonUtil.map(
                    "traceId", bucket.getKeyAsString(),
                    "count", bucket.getDocCount(),
                    "maxGmtCreate", (long)((ParsedMax)bucket.getAggregations().get("maxGmtCreate")).getValue(),
                    "minGmtCreate", (long)((ParsedMin)bucket.getAggregations().get("minGmtCreate")).getValue(),
                    "status", ((ParsedStringTerms)statusAgg).getBuckets().stream().map(statusBucket -> JsonUtil.map(
                        "status", statusBucket.getKeyAsString(),
                        "count", statusBucket.getDocCount()
                    )).collect(Collectors.toMap(x->x.getString("status"), x->x.getIntValue("count")))
                );
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }

    }
}
