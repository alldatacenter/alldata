package com.alibaba.sreworks.job.master.params;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import com.alibaba.sreworks.job.master.event.JobEventConf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreateParam {

    private String creator;

    private String operator;

    private String appId;

    private String name;

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

    public SreworksJob job() {
        return SreworksJob.builder()
            .gmtCreate(System.currentTimeMillis())
            .gmtModified(System.currentTimeMillis())
            .creator(getCreator())
            .operator(getOperator())
            .appId(getAppId())
            .name(getName())
            .alias(getAlias())
            .tags(JSONObject.toJSONString(getTags()))
            .description(getDescription())
            .options(JSONObject.toJSONString(getOptions()))
            .triggerType(getTriggerType())
            .triggerConf(JSONObject.toJSONString(getTriggerConf()))
            .scheduleType(getScheduleType())
            .sceneType(getSceneType())
            .varConf(JSONObject.toJSONString(getVarConf()))
            .notifyConf(JSONObject.toJSONString(getNotifyConf()))
            .eventConf(JSONObject.toJSONString(getEventConf()))
            .build();

    }

}
