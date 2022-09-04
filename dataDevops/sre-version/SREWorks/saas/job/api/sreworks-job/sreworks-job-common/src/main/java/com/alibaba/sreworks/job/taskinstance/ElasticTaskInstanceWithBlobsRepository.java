package com.alibaba.sreworks.job.taskinstance;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElasticTaskInstanceWithBlobsRepository
    extends ElasticsearchRepository<ElasticTaskInstanceWithBlobs, String> {

    ElasticTaskInstanceWithBlobs findFirstById(String id);

    List<ElasticTaskInstanceWithBlobs> findAllByJobInstanceId(String jobInstanceId);

}
