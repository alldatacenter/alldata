package com.alibaba.sreworks.job.master.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobTask;
import com.alibaba.sreworks.job.utils.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TaskModifyParam {

    private String operator;

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

    public void patchTask(SreworksJobTask task) {
        task.setGmtModified(System.currentTimeMillis());
        task.setOperator(getOperator());
        task.setAlias(getAlias());
        task.setExecTimeout(getExecTimeout());
        task.setExecType(getExecType());
        task.setExecContent(getExecContent());
        task.setExecRetryTimes(getExecRetryTimes());
        task.setExecRetryInterval(getExecRetryInterval());
        task.setVarConf(JSONObject.toJSONString(getVarConf()));
        task.setSceneType(sceneType());
        task.setSceneConf(JSONObject.toJSONString(getSceneConf()));
    }

}
