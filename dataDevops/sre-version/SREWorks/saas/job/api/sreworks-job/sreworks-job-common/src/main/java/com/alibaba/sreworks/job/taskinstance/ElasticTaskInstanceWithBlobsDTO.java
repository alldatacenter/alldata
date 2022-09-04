package com.alibaba.sreworks.job.taskinstance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ElasticTaskInstanceWithBlobsDTO extends ElasticTaskInstanceDTO {

    private String stdout;

    private String stderr;

    public ElasticTaskInstanceWithBlobsDTO(ElasticTaskInstanceWithBlobs elasticTaskInstanceWithBlobs) {
        super(elasticTaskInstanceWithBlobs);
        this.stdout = elasticTaskInstanceWithBlobs.getStdout();
        this.stderr = elasticTaskInstanceWithBlobs.getStderr();
    }

}
