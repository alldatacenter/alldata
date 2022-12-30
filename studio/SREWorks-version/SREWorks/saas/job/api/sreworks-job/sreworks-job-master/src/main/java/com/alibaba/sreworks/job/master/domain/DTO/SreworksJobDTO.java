package com.alibaba.sreworks.job.master.domain.DTO;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import com.alibaba.sreworks.job.master.event.JobEventConf;
import com.alibaba.sreworks.job.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class SreworksJobDTO {

    private Long id;

    private Long gmtCreate;

    private Long gmtModified;

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

    private Object sceneConf;

    private JSONObject varConf;

    private JSONObject notifyConf;

    private List<JobEventConf> eventConf;

    public SreworksJobDTO(SreworksJob job) {
        id = job.getId();
        gmtCreate = job.getGmtCreate();
        gmtModified = job.getGmtModified();
        appId = job.getAppId();
        name = job.getName();
        alias = job.getAlias();
        tags = JSONObject.parseArray(job.getTags());
        creator = job.getCreator();
        operator = job.getOperator();
        description = job.getDescription();
        options = JSONObject.parseObject(job.getOptions());
        sceneType = job.getSceneType();
        scheduleType = job.getScheduleType();
        triggerType = job.getTriggerType();
        triggerConf = JSONObject.parseObject(job.getTriggerConf());
        varConf = job.varConf();
        notifyConf = JSONObject.parseObject(job.getNotifyConf());
        eventConf = StringUtil.isEmpty(job.getEventConf()) ?
            new ArrayList<>() : JSONObject.parseArray(job.getEventConf()).toJavaList(JobEventConf.class);
    }

}
