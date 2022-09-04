package com.alibaba.sreworks.job.master.domain.DTO;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SreworksJobTaskDTO {

    private Long id;

    private Long gmtCreate;

    private Long gmtModified;

    private String creator;

    private String operator;

    private String appId;

    private String name;

    private String alias;

    private Long execTimeout;

    private String execType;

    private String execContent;

    private Long execRetryTimes;

    private Long execRetryInterval;

    private JSONObject varConf;

    private String sceneType;

    private JSONObject sceneConf;

    public SreworksJobTaskDTO(SreworksJobTask jobTask) {
        id = jobTask.getId();
        gmtCreate = jobTask.getGmtCreate();
        gmtModified = jobTask.getGmtModified();
        creator = jobTask.getCreator();
        operator = jobTask.getOperator();
        appId = jobTask.getAppId();
        name = jobTask.getName();
        alias = jobTask.getAlias();
        execTimeout = jobTask.getExecTimeout();
        execType = jobTask.getExecType();
        execContent = jobTask.getExecContent();
        execRetryTimes = jobTask.getExecRetryTimes() == null ? 0 : jobTask.getExecRetryTimes();
        execRetryInterval = jobTask.getExecRetryInterval() == null ? 0 : jobTask.getExecRetryInterval();
        varConf = jobTask.varConf();
        sceneType = jobTask.getSceneType();
        sceneConf = jobTask.sceneConf();
    }

}
