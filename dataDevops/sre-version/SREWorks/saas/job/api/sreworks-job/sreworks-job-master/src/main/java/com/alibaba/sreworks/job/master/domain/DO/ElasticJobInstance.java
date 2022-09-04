package com.alibaba.sreworks.job.master.domain.DO;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Slf4j
@Document(indexName = "sreworks-job-instance")
public class ElasticJobInstance {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String operator;

    @Field(type = FieldType.Long)
    private Long gmtCreate;

    @Field(type = FieldType.Long)
    private Long gmtExecute;

    @Field(type = FieldType.Long)
    private Long gmtStop;

    @Field(type = FieldType.Long)
    private Long gmtEnd;

    @Field(type = FieldType.Long)
    private Long jobId;

    @Field(type = FieldType.Text)
    private String job;

    @Field(type = FieldType.Text)
    private String varConf;

    @Field(type = FieldType.Long)
    private Long scheduleInstanceId;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private List<String> traceIds;

}
