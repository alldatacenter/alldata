package com.alibaba.sreworks.job.master.domain.repository;

import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElasticJobInstanceRepository extends ElasticsearchRepository<ElasticJobInstance, String> {

    ElasticJobInstance findFirstById(String id);

    Page<ElasticJobInstance> findAllByStatusInOrderByGmtCreateDesc(List<String> status, Pageable pageable);

    Page<ElasticJobInstance> findAllByStatusInAndJobIdOrderByGmtCreateDesc(
        List<String> status, Long jobId, Pageable pageable);

    ElasticJobInstance findFirstByScheduleInstanceId(Long scheduleInstanceId);

    Page<ElasticJobInstance> findAllByJobIdAndTagsAndGmtCreateBetweenOrderByGmtCreateDesc(
        Long jobId, String tag, Long stime, Long etime, Pageable pageable);

    Page<ElasticJobInstance> findAllByJobIdAndTraceIdsAndGmtCreateBetweenOrderByGmtCreateDesc(
        Long jobId, String traceIds, Long stime, Long etime, Pageable pageable);

    Page<ElasticJobInstance> findAllByJobLike(String sceneType, Pageable pageable);

}
