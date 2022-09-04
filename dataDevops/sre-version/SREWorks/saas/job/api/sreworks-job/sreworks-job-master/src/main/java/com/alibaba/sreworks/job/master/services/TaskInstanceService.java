package com.alibaba.sreworks.job.master.services;

import com.alibaba.sreworks.job.taskinstance.*;
import com.alibaba.sreworks.job.utils.StringUtil;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.sreworks.job.utils.PageUtil.pageable;

@Slf4j
@Service
public class TaskInstanceService {

    @Autowired
    ElasticTaskInstanceRepository elasticTaskInstanceRepository;

    @Autowired
    ElasticTaskInstanceWithBlobsRepository elasticTaskInstanceWithBlobsRepository;

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    public Page<ElasticTaskInstance> list(
        String taskIdListString, String statusListString, Integer page, Integer pageSize) {

        List<String> statusList;
        if (StringUtil.isEmpty(statusListString)) {
            statusList = Arrays.stream(TaskInstanceStatus.values()).map(Enum::name).collect(Collectors.toList());
        } else {
            statusList = Arrays.stream(statusListString.split(",")).collect(Collectors.toList());
        }

        Pageable pageable = pageable(page, pageSize);
        if (StringUtil.isEmpty(taskIdListString)) {
            return elasticTaskInstanceRepository.findAllByStatusInOrderByGmtCreateDesc(statusList, pageable);
        } else {
            List<Long> taskIdList = Arrays.stream(taskIdListString.split(",")).map(Long::valueOf)
                .collect(Collectors.toList());
            return elasticTaskInstanceRepository
                .findAllByTaskIdInAndStatusInOrderByGmtCreateDesc(taskIdList, statusList, pageable);
        }

    }

    public SearchHits<ElasticTaskInstance> list2(
        String taskIdListString, String statusListString, Integer page, Integer pageSize, String id) {

        List<String> statusList;
        if (StringUtil.isEmpty(statusListString)) {
            statusList = Arrays.stream(TaskInstanceStatus.values()).map(Enum::name).collect(Collectors.toList());
        } else {
            statusList = Arrays.stream(statusListString.split(",")).collect(Collectors.toList());
        }

        Pageable pageable = pageable(page, pageSize);

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotEmpty(taskIdListString)) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("taskId", taskIdListString));
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
            ElasticTaskInstance.class
        );

    }

    public ElasticTaskInstanceDTO get(String id) {
        return new ElasticTaskInstanceDTO(elasticTaskInstanceRepository.findFirstById(id));
    }

    public ElasticTaskInstanceWithBlobsDTO getWithBlobs(String id) {
        return new ElasticTaskInstanceWithBlobsDTO(elasticTaskInstanceWithBlobsRepository.findFirstById(id));
    }

}
