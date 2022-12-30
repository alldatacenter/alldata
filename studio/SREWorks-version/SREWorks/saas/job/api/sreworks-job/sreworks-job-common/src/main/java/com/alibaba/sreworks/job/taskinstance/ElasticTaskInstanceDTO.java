package com.alibaba.sreworks.job.taskinstance;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ElasticTaskInstanceDTO {

    private String id;

    private Long gmtCreate;

    private Long gmtExecute;

    private Long gmtStop;

    private Long gmtEnd;

    private Long taskId;

    private String jobInstanceId;

    private String address;

    private String name;

    private String alias;

    private Long execTimeout;

    private String execType;

    private String execContent;

    private Long execRetryTimes;

    private Long execRetryInterval;

    private Long execTimes;

    private JSONObject varConf;

    private JSONObject outVarConf;

    private String status;

    private String statusDetail;

    public ElasticTaskInstanceDTO(ElasticTaskInstance elasticTaskInstance) {
        this.id = elasticTaskInstance.getId();
        this.gmtCreate = elasticTaskInstance.getGmtCreate();
        this.gmtExecute = elasticTaskInstance.getGmtExecute();
        this.gmtStop = elasticTaskInstance.getGmtStop();
        this.gmtEnd = elasticTaskInstance.getGmtEnd();
        this.taskId = elasticTaskInstance.getTaskId();
        this.jobInstanceId = elasticTaskInstance.getJobInstanceId();
        this.address = elasticTaskInstance.getAddress();
        this.name = elasticTaskInstance.getName();
        this.alias = elasticTaskInstance.getAlias();
        this.execTimeout = elasticTaskInstance.getExecTimeout();
        this.execType = elasticTaskInstance.getExecType();
        this.execContent = elasticTaskInstance.getExecContent();
        this.execRetryTimes = elasticTaskInstance.getExecRetryTimes();
        this.execRetryInterval = elasticTaskInstance.getExecRetryInterval();
        this.execTimes = elasticTaskInstance.getExecTimes();
        this.varConf = JSONObject.parseObject(elasticTaskInstance.getVarConf());
        this.outVarConf = JSONObject.parseObject(elasticTaskInstance.getOutVarConf());
        this.status = elasticTaskInstance.getStatus();
        this.statusDetail = elasticTaskInstance.getStatusDetail();
    }

}
