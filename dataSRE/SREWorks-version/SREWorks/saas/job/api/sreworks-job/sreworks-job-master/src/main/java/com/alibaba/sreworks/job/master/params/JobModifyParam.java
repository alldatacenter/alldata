package com.alibaba.sreworks.job.master.params;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import com.alibaba.sreworks.job.master.event.JobEventConf;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

@Data
@Slf4j
public class JobModifyParam {

    private String operator;

    private String alias;

    private JSONArray tags;

    private String description;

    private JSONObject options;

    private String triggerType;

    private JSONObject triggerConf;

    private String scheduleType;

    private JSONObject scheduleConf;

    private String sceneType;

    private JSONObject sceneConf;

    private JSONObject varConf;

    private JSONObject notifyConf;

    private List<JobEventConf> eventConf;

    public void patchJob(SreworksJob job) {
        job.setGmtModified(System.currentTimeMillis());
        job.setOperator(getOperator());

        if (StringUtils.isNotBlank(getAlias())) {
            job.setAlias(getAlias());
        }
        if (getTags() != null) {
            job.setTags(JSONObject.toJSONString(getTags()));
        }

        if (StringUtils.isNotBlank(getDescription())) {
            job.setDescription(getDescription());
        }

        if (getOptions() != null) {
            job.setOptions(JSONObject.toJSONString(getOptions()));
        }

        if (StringUtils.isNotBlank(getTriggerType())) {
            job.setTriggerType(getTriggerType());
        }

        if (Objects.nonNull(getTriggerConf())) {
            job.setTriggerConf(JSONObject.toJSONString(getTriggerConf()));
        }

        if (StringUtils.isNotBlank(getScheduleType())) {
            job.setScheduleType(getScheduleType());
        }
        if (StringUtils.isNotBlank(getSceneType())) {
            job.setSceneType(getSceneType());
        }
        if (getVarConf() != null){
            job.setVarConf(JSONObject.toJSONString(getVarConf()));
        }
        if (getNotifyConf() != null){
            job.setNotifyConf(JSONObject.toJSONString(getNotifyConf()));
        }
        if (getEventConf() != null) {
            job.setEventConf(JSONObject.toJSONString(getEventConf()));
        }
    }

}
