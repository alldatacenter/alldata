package com.alibaba.sreworks.job.master.controllers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobRepository;
import com.alibaba.sreworks.job.master.jobscene.JobSceneService;
import com.alibaba.sreworks.job.master.jobschedule.JobScheduleService;
import com.alibaba.sreworks.job.master.jobtrigger.JobTriggerService;
import com.alibaba.sreworks.job.master.params.*;
import com.alibaba.sreworks.job.master.services.JobService;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/job")
public class JobController extends BaseController {

    @Autowired
    JobService jobService;

    @Autowired
    JobSceneService jobSceneService;

    @Autowired
    JobScheduleService jobScheduleService;

    @Autowired
    JobTriggerService jobTriggerService;

    @Autowired
    SreworksJobRepository jobRepository;

    @RequestMapping(value = "listSceneType", method = RequestMethod.GET)
    public TeslaBaseResult listSceneType() {

        return buildSucceedResult(jobSceneService.listType());

    }

    @RequestMapping(value = "listScheduleType", method = RequestMethod.GET)
    public TeslaBaseResult listScheduleType() {

        return buildSucceedResult(jobScheduleService.listType());

    }

    @RequestMapping(value = "listTriggerType", method = RequestMethod.GET)
    public TeslaBaseResult listTriggerType() {

        return buildSucceedResult(jobTriggerService.listType());

    }

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(
        String sceneType, String scheduleType, String triggerType, String names, Integer page, Integer pageSize) throws Exception {

        if (page == null) {
            page = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        List<String> jobNameList = new ArrayList<>();
        if (StringUtils.isNotEmpty(names)) {
            jobNameList = Arrays.asList(names.split(","));
        }

        List<SreworksJobDTO> jobList = jobService.list(sceneType, scheduleType, triggerType, jobNameList);
        List<SreworksJobDTO> items = jobList.subList((page - 1) * pageSize, Math.min(page * pageSize, jobList.size()));
        for (SreworksJobDTO item : items) {
            try {
                JSONObject scheduleConf = jobScheduleService.getJobSchedule(item.getScheduleType())
                    .getConf(item.getId());
                item.setScheduleConf(scheduleConf);
            } catch (Exception ignored) {}

        }
        return buildSucceedResult(JsonUtil.map(
            "total", jobList.size(),
            "items", items
        ));

    }

    @RequestMapping(value = "getTaskIdList", method = RequestMethod.GET)
    public TeslaBaseResult getTaskIdList(Long jobId) throws Exception {

        try {
            SreworksJobDTO item = jobService.get(jobId);
            JSONObject scheduleConf = jobScheduleService.getJobSchedule(item.getScheduleType()).getConf(item.getId());
            return buildSucceedResult(scheduleConf.getJSONArray("taskIdList"));
        } catch (Exception e) {
            return buildSucceedResult(new JSONArray());
        }

    }

    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) throws Exception {
        return buildSucceedResult(jobService.get(id));
    }


    @RequestMapping(value = "getByName", method = RequestMethod.GET)
    public TeslaBaseResult getByName(String jobName) {
        SreworksJobDTO jobDTO = null;
        try {
            jobDTO = jobService.getByName(jobName);
        } catch (Exception ex) {
            log.warn(ex.getMessage());
        }
        return buildSucceedResult(jobDTO);
    }

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(@RequestBody JobCreateParam param) throws Exception {
        param.setCreator(getUserEmployeeId());
        param.setOperator(getUserEmployeeId());
        param.setAppId(getAppId());
        SreworksJob job = jobService.create(param);
        return buildSucceedResult(job.getId());

    }

    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) throws Exception {

        jobService.delete(id);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "deleteWithTask", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteWithTask(Long id) throws Exception {
        jobService.deleteWithTask(id);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "modify", method = RequestMethod.POST)
    public TeslaBaseResult modify(Long id, @RequestBody JobModifyParam param) throws Exception {

        param.setOperator(getUserEmployeeId());
        jobService.modify(id, param);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "editSchedule", method = RequestMethod.POST)
    public TeslaBaseResult editSchedule(Long id, @RequestBody JobEditScheduleParam param) throws Exception {

        jobService.editSchedule(id, param);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "toggleCronJobState", method = RequestMethod.POST)
    public TeslaBaseResult toggleCronJobState(Long id, @RequestBody JobToggleCronJobStateParam param) throws Exception {
        jobService.toggleCronJobState(id, param.isState());
        return buildSucceedResult("OK");
    }

    @RequestMapping(value = "start", method = RequestMethod.POST)
    public TeslaBaseResult start(Long id, String jobName, @RequestBody JobStartParam param) throws Exception {
        Long jobId;
        if(StringUtils.isNotBlank(jobName)){
            SreworksJobDTO jobInfo = jobService.getByName(jobName);
            jobId = jobInfo.getId();
        }else{
            jobId = id;
        }
        return buildSucceedResult(jobService.start(
            jobId, param.varConf(), param.tags(), param.traceIds(), getUserEmployeeId()
        ));

    }

}
