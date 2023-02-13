package com.alibaba.sreworks.job.master.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.common.JobTriggerType;
import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import com.alibaba.sreworks.job.master.domain.DTO.JobInstanceStatus;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobDTO;
import com.alibaba.sreworks.job.master.domain.repository.ElasticJobInstanceRepository;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobRepository;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobTaskRepository;
import com.alibaba.sreworks.job.master.jobscene.JobSceneService;
import com.alibaba.sreworks.job.master.jobschedule.JobScheduleService;
import com.alibaba.sreworks.job.master.jobtrigger.JobTriggerService;
import com.alibaba.sreworks.job.master.params.JobCreateParam;
import com.alibaba.sreworks.job.master.params.JobEditScheduleParam;
import com.alibaba.sreworks.job.master.params.JobModifyParam;
import com.alibaba.sreworks.job.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JobService {

    @Autowired
    SreworksJobRepository jobRepository;

    @Autowired
    SreworksJobTaskRepository taskRepository;

    @Autowired
    ElasticJobInstanceRepository jobInstanceRepository;

    @Autowired
    JobSceneService jobSceneService;

    @Autowired
    JobTriggerService jobTriggerService;

    @Autowired
    JobScheduleService jobScheduleService;

    public List<SreworksJobDTO> list(String sceneType, String scheduleType, String triggerType, List<String> jobNameList) throws Exception {
        List<SreworksJob> jobs = jobRepository.findAll().stream()
                .filter(job -> StringUtil.isEmpty(sceneType) || StringUtils.contains(sceneType, job.getSceneType()))
                .filter(job -> StringUtil.isEmpty(scheduleType) || StringUtils.contains(scheduleType, job.getScheduleType()))
                .filter(job -> StringUtil.isEmpty(triggerType) || StringUtils.contains(triggerType, job.getTriggerType()))
                .filter(job -> CollectionUtils.isEmpty(jobNameList) || jobNameList.contains(job.getName()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(jobs)) {
            return new ArrayList<>();
        }

        Map<Long, SreworksJobDTO> jobDTOMap = jobs.stream().collect(Collectors.toMap(SreworksJob::getId, SreworksJobDTO::new));

        Set<Long> cronJobIds = jobs.stream()
                .filter(job -> StringUtils.isNotEmpty(job.getTriggerType()) && job.getTriggerType().equals(JobTriggerType.CRON.getType()))
                .map(SreworksJob::getId)
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(cronJobIds)) {
            Map<Long, Boolean> cronTriggerStateMap = jobTriggerService.getJobTrigger(JobTriggerType.CRON.getType())
                    .getStateBatch(cronJobIds);
            cronTriggerStateMap.keySet().forEach(id -> {
                JSONObject triggerConf = jobDTOMap.get(id).getTriggerConf();
                triggerConf.put("enabled", cronTriggerStateMap.get(id));
                jobDTOMap.get(id).setTriggerConf(triggerConf);
            });
        }

        return jobDTOMap.values().stream().sorted(Comparator.comparingLong(SreworksJobDTO::getId).reversed())
                .collect(Collectors.toList());
    }

    public SreworksJobDTO get(Long id) throws Exception {
        SreworksJob job = jobRepository.findFirstById(id);
        if (job == null) {
            throw new Exception("id is not exists");
        }
        SreworksJobDTO jobDTO = new SreworksJobDTO(job);
        if (!StringUtil.isEmpty(job.getSceneType())) {
            jobDTO.setSceneConf(jobSceneService.getJobScene(job.getSceneType()).getConf(job.getId()));
        }
        if (!StringUtil.isEmpty(job.getScheduleType())) {
            jobDTO.setScheduleConf(jobScheduleService.getJobSchedule(job.getScheduleType()).getConf(job.getId()));
        }
        if (!StringUtil.isEmpty(job.getTriggerType()) && job.getTriggerType().equals(JobTriggerType.CRON.getType())) {
            JSONObject triggerConf = jobDTO.getTriggerConf();
            triggerConf.put("enabled", jobTriggerService.getJobTrigger(job.getTriggerType()).getState(id));
            jobDTO.setTriggerConf(triggerConf);
        }
        return jobDTO;
    }

    public SreworksJobDTO getByName(String jobName) throws Exception {
        SreworksJob sreworksJob = new SreworksJob();
        sreworksJob.setName(jobName);
        Example<SreworksJob> example = Example.of(sreworksJob);
        Optional<SreworksJob> optional = jobRepository.findOne(example);
        if (optional.isEmpty()) {
            throw new Exception("name is not exists");
        }
        SreworksJob job = optional.get();
        SreworksJobDTO jobDTO = new SreworksJobDTO(job);
        if (!StringUtil.isEmpty(job.getSceneType())) {
            jobDTO.setSceneConf(jobSceneService.getJobScene(job.getSceneType()).getConf(job.getId()));
        }
        if (!StringUtil.isEmpty(job.getScheduleType())) {
            jobDTO.setScheduleConf(jobScheduleService.getJobSchedule(job.getScheduleType()).getConf(job.getId()));
        }
        if (!StringUtil.isEmpty(job.getTriggerType()) && job.getTriggerType().equals(JobTriggerType.CRON.getType())) {
            JSONObject triggerConf = jobDTO.getTriggerConf();
            triggerConf.put("enabled", jobTriggerService.getJobTrigger(job.getTriggerType()).getState(job.getId()));
            jobDTO.setTriggerConf(triggerConf);
        }
        return jobDTO;
    }

    public SreworksJob create(JobCreateParam param) throws Exception {
        SreworksJob job = param.job();
        job = jobRepository.saveAndFlush(job);
        if (param.getSceneType() != null) {
            jobSceneService.getJobScene(param.getSceneType()).create(job.getId(), param.getSceneConf());
        }
        if (param.getScheduleType() != null) {
            jobScheduleService.getJobSchedule(param.getScheduleType()).create(job.getId(), param.getScheduleConf());
        }
        if (param.getTriggerType() != null) {
            jobTriggerService.getJobTrigger(param.getTriggerType()).create(job.getId(), param.getTriggerConf());
        }
        return job;
    }

    public void delete(Long id) throws Exception {
        SreworksJob job = jobRepository.findFirstById(id);
        if (job != null) {
            deleteJob(job);
        }
    }

    public void deleteWithTask(Long id) throws Exception {
        SreworksJob job = jobRepository.findFirstById(id);
        if (job != null) {
            deleteJob(job);
            JSONObject scheduleConf = jobScheduleService.getJobSchedule(job.getScheduleType()).getConf(job.getId());
            Set<Long> taskIds = scheduleConf.getJSONArray("taskIdList").stream()
                    .map(jobTask -> ((JSONObject)jobTask).getLong("id")).collect(Collectors.toSet());
            taskIds.forEach(taskId -> taskRepository.deleteById(taskId));
        }
    }

    private void deleteJob(SreworksJob job) throws Exception {
        if (job.getSceneType() != null) {
            jobSceneService.getJobScene(job.getSceneType()).delete(job.getId());
        }
        if (job.getScheduleType() != null) {
            jobScheduleService.getJobSchedule(job.getScheduleType()).delete(job.getId());
        }
        if (job.getTriggerType() != null) {
            jobTriggerService.getJobTrigger(job.getTriggerType()).delete(job.getId());
        }
        jobRepository.deleteById(job.getId());
    }

    public void modify(Long id, JobModifyParam param) throws Exception {
        SreworksJob job = jobRepository.findFirstById(id);

        String sceneType1 = job.getSceneType();
        String sceneType2 = param.getSceneType();
        JSONObject sceneConf = param.getSceneConf();
        if (sceneType2 != null) {
            if (sceneType2.equals(sceneType1)) {
                jobSceneService.getJobScene(sceneType1).modify(id, sceneConf);
            } else {
                if (sceneType1 != null) {
                    jobSceneService.getJobScene(sceneType1).delete(id);
                }
                jobSceneService.getJobScene(sceneType2).create(id, sceneConf);
            }
        }


        String scheduleType1 = job.getScheduleType();
        String scheduleType2 = param.getScheduleType();
        JSONObject scheduleConf = param.getScheduleConf();
        if (scheduleType2 != null) {
            if (scheduleType2.equals(scheduleType1)) {
                jobScheduleService.getJobSchedule(scheduleType1).modify(id, scheduleConf);
            } else {
                if (scheduleType1 != null) {
                    jobScheduleService.getJobSchedule(scheduleType1).delete(id);
                }
                jobScheduleService.getJobSchedule(scheduleType2).create(id, scheduleConf);
            }
        }

        String triggerType1 = job.getTriggerType();
        String triggerType2 = param.getTriggerType();
        JSONObject triggerConfParam = param.getTriggerConf();
        JSONObject triggerConfDb = JSONObject.parseObject(job.getTriggerConf());
        if (StringUtils.isNotEmpty(triggerType2)) {
            if (triggerType2.equals(JobTriggerType.CRON.getType())) {
                // cron修改成cron作业
                if (JobTriggerType.CRON.getType().equals(triggerType1)) {
                    // 定时表达式修改, 先删除后新建定时器
                    if (!triggerConfDb.getString("cronExpression").equals(triggerConfParam.getString("cronExpression"))) {
                        jobTriggerService.getJobTrigger(triggerType1).delete(id);
                        jobTriggerService.getJobTrigger(triggerType2).create(id, triggerConfParam);
                    }
                } else {
                    // normal修改成cron作业新建定时器
                    jobTriggerService.getJobTrigger(triggerType2).create(id, triggerConfParam);
                }
            } else {
                // cron修改成normal作业, 删除定时器
                if (JobTriggerType.CRON.getType().equals(triggerType1)) {
                    jobTriggerService.getJobTrigger(triggerType1).delete(id);
                }
            }
        }

        param.patchJob(job);
        jobRepository.saveAndFlush(job);

    }

    public void editSchedule(Long id, JobEditScheduleParam param) throws Exception {

        SreworksJob job = jobRepository.findFirstById(id);
        if (job == null) {
            throw new Exception("id is not exists");
        }
        if (StringUtil.isEmpty(job.getScheduleType())) {
            jobScheduleService.getJobSchedule(param.getScheduleType()).create(job.getId(), param.getScheduleConf());
        } else {
            String scheduleType1 = job.getScheduleType();
            String scheduleType2 = param.getScheduleType();
            JSONObject scheduleConf = param.getScheduleConf();
            if (scheduleType2.equals(scheduleType1)) {
                jobScheduleService.getJobSchedule(scheduleType1).modify(id, scheduleConf);
            } else {
                jobScheduleService.getJobSchedule(scheduleType1).delete(id);
                jobScheduleService.getJobSchedule(scheduleType2).create(id, scheduleConf);
            }

        }
        param.patchJob(job);
        jobRepository.saveAndFlush(job);

    }

    public void toggleCronJobState(Long id, Boolean state) throws Exception  {
        SreworksJob job = jobRepository.findFirstById(id);
        if (job == null) {
            throw new Exception(String.format("job[id:%s] not exists", id));
        }
        if (job.getTriggerType().equals(JobTriggerType.CRON.getType())) {
            jobTriggerService.getJobTrigger(job.getTriggerType()).toggleState(id, state);
        }
    }

    public ElasticJobInstance start(
        Long id, JSONObject varConf, List<String> tags, List<String> traceIds, String operator) throws Exception {

        varConf = varConf == null ? new JSONObject() : varConf;
        tags = tags == null ? new ArrayList<>() : tags;
        traceIds = traceIds == null ? new ArrayList<>() : traceIds;

        SreworksJob job = jobRepository.findFirstById(id);
        JSONObject jobVarConf = job.varConf();
        if (operator.equals("event")) {
            JSONObject recordOptions = (JSONObject)varConf.remove("options");
            if (!CollectionUtils.isEmpty(recordOptions)) {
                jobVarConf.putAll(recordOptions);
            }
            JSONObject eventObject = new JSONObject();
            eventObject.put("options", jobVarConf);
            eventObject.putAll(varConf);
            jobVarConf = eventObject;
        } else {
            jobVarConf.putAll(varConf);
        }

        tags.addAll(job.tags());
        tags = tags.stream().distinct().collect(Collectors.toList());
        Long scheduleInstanceId = jobScheduleService.getJobSchedule(job.getScheduleType()).start(id, jobVarConf);
        ElasticJobInstance jobInstance = new ElasticJobInstance();
        jobInstance.setId(UUID.randomUUID().toString().replace("-", ""));
        jobInstance.setOperator(operator);
        jobInstance.setGmtCreate(System.currentTimeMillis());
        jobInstance.setGmtExecute(System.currentTimeMillis());
        jobInstance.setJobId(id);
        jobInstance.setJob(JSONObject.toJSONString(new SreworksJobDTO(job)));
        jobInstance.setVarConf(JSONObject.toJSONString(jobVarConf));
        jobInstance.setScheduleInstanceId(scheduleInstanceId);
        jobInstance.setStatus(JobInstanceStatus.INIT.name());
        jobInstance.setTags(tags);
        jobInstance.setTraceIds(traceIds);
        return jobInstanceRepository.save(jobInstance);
    }
}
