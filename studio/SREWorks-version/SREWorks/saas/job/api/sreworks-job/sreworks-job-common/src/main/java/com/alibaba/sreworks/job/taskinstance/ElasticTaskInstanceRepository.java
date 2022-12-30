package com.alibaba.sreworks.job.taskinstance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElasticTaskInstanceRepository extends ElasticsearchRepository<ElasticTaskInstance, String> {

    ElasticTaskInstance findFirstById(String id);

    Page<ElasticTaskInstance> findAllByTaskIdInAndStatusInOrderByGmtCreateDesc(
        List<Long> taskId, List<String> status, Pageable pageable);

    Page<ElasticTaskInstance> findAllByStatusInOrderByGmtCreateDesc(List<String> status, Pageable pageable);

}
