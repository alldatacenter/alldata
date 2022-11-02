package com.alibaba.sreworks.job.master.controllers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobDTO;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobTaskDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobTaskRepository;
import com.alibaba.sreworks.job.master.params.JobCreateParam;
import com.alibaba.sreworks.job.master.params.TaskCreateParam;
import com.alibaba.sreworks.job.master.services.JobService;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/imEx")
public class ImExportController extends BaseController {

    @Autowired
    JobService jobService;

    @Autowired
    SreworksJobTaskRepository taskRepository;

    @Autowired
    TaskController taskController;

    @Autowired
    JobController jobController;

    @RequestMapping(value = "im", method = RequestMethod.POST)
    public TeslaBaseResult im(@RequestBody List<SreworksJobDTO> jobDtoList) throws Exception {

        for (SreworksJobDTO jobDTO : jobDtoList) {

            List<SreworksJobTaskDTO> jobTaskDTOS = jobDTO.getScheduleConf()
                .getJSONArray("taskIdList")
                .toJavaList(SreworksJobTaskDTO.class);
            List<Long> taskIdList = new ArrayList<>();
            for (SreworksJobTaskDTO jobTaskDTO : jobTaskDTOS) {
                TaskCreateParam param = TaskCreateParam.builder()
                    .creator(jobTaskDTO.getCreator())
                    .operator(jobTaskDTO.getOperator())
                    .appId(jobTaskDTO.getAppId())
                    .name(jobTaskDTO.getName())
                    .alias(jobTaskDTO.getAlias())
                    .execTimeout(jobTaskDTO.getExecTimeout())
                    .execType(jobTaskDTO.getExecType())
                    .execContent(jobTaskDTO.getExecContent())
                    .execRetryTimes(jobTaskDTO.getExecRetryTimes())
                    .execRetryInterval(jobTaskDTO.getExecRetryInterval())
                    .varConf(jobTaskDTO.getVarConf())
                    .sceneType(jobTaskDTO.getSceneType())
                    .sceneConf(jobTaskDTO.getSceneConf())
                    .build();
                TeslaBaseResult result = taskController.create(param);
                taskIdList.add((Long)result.getData());
            }
            JobCreateParam param = JobCreateParam.builder()
                .creator(jobDTO.getCreator())
                .operator(jobDTO.getOperator())
                .appId(jobDTO.getAppId())
                .name(jobDTO.getName())
                .alias(jobDTO.getAlias())
                .tags(jobDTO.getTags())
                .description(jobDTO.getDescription())
                .options(jobDTO.getOptions())
                .triggerType(jobDTO.getTriggerType())
                .triggerConf(jobDTO.getTriggerConf())
                .scheduleType(jobDTO.getScheduleType())
                .scheduleConf(JsonUtil.map("taskIdList", taskIdList))
                .sceneType(jobDTO.getSceneType())
                .sceneConf(JSONObject.parseObject(JSONObject.toJSONString(jobDTO.getSceneConf())))
                .varConf(jobDTO.getVarConf())
                .notifyConf(jobDTO.getNotifyConf())
                .eventConf(jobDTO.getEventConf())
                .build();
            jobController.create(param);

        }
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "ex", method = RequestMethod.POST)
    public TeslaBaseResult ex(@RequestBody JSONArray jobIdList) throws Exception {

        List<SreworksJobDTO> jobDtoList = new ArrayList<>();
        for (Long jobId : jobIdList.toJavaList(Long.class)) {
            jobDtoList.add(jobService.get(jobId));
        }
        return buildSucceedResult(jobDtoList);

    }

}
