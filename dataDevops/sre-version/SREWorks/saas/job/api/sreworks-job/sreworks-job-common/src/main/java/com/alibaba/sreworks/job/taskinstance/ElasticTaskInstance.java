package com.alibaba.sreworks.job.taskinstance;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Slf4j
@Document(indexName = "sreworks-job-task-instance", createIndex = false)
public class ElasticTaskInstance {

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
    private Long taskId;

    @Field(type = FieldType.Keyword)
    private String jobInstanceId;

    @Field(type = FieldType.Keyword)
    private String address;

    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Keyword)
    private String alias;

    @Field(type = FieldType.Long)
    private Long execTimeout;

    @Field(type = FieldType.Keyword)
    private String execType;

    @Field(type = FieldType.Text)
    private String execContent;

    @Field(type = FieldType.Long)
    private Long execRetryTimes;

    @Field(type = FieldType.Long)
    private Long execRetryInterval;

    @Field(type = FieldType.Long)
    private Long execTimes;

    @Field(type = FieldType.Text)
    private String varConf;

    @Field(type = FieldType.Text)
    private String outVarConf;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String statusDetail;

    @Field(type = FieldType.Keyword)
    private String sceneType;

    @Field(type = FieldType.Text)
    private String sceneConf;

}
