package com.alibaba.sreworks.job.master.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobTask;
import com.alibaba.sreworks.job.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateParam {

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

    private String sceneType() {
        return StringUtil.isEmpty(getSceneType()) ? "normal" : getSceneType();
    }

    public SreworksJobTask task() {
        return SreworksJobTask.builder()
            .gmtCreate(System.currentTimeMillis())
            .gmtModified(System.currentTimeMillis())
            .creator(getCreator())
            .operator(getOperator())
            .appId(getAppId())
            .name(getName())
            .alias(getAlias())
            .execTimeout(getExecTimeout())
            .execType(getExecType())
            .execContent(getExecContent())
            .execRetryTimes(getExecRetryTimes())
            .execRetryInterval(getExecRetryInterval())
            .varConf(JSONObject.toJSONString(getVarConf()))
            .sceneType(sceneType())
            .sceneConf(JSONObject.toJSONString(getSceneConf()))
            .build();
    }

}
