package com.alibaba.sreworks.job.taskinstance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Document(indexName = "sreworks-job-task-instance")
public class ElasticTaskInstanceWithBlobs extends ElasticTaskInstance {

    @Field(type = FieldType.Text)
    private String stdout;

    @Field(type = FieldType.Text)
    private String stderr;

}
