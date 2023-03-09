/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job;

import static java.util.TimeZone.getTimeZone;
import static org.apache.griffin.core.config.EnvConfig.ENV_BATCH;
import static org.apache.griffin.core.config.EnvConfig.ENV_STREAMING;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INSTANCE_ID_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_MEASURE_ID;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_ID_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_NAME_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_TYPE_DOES_NOT_SUPPORT;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.MEASURE_TYPE_DOES_NOT_SUPPORT;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.NO_SUCH_JOB_ACTION;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.QUARTZ_JOB_ALREADY_EXIST;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.BUSY;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.DEAD;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.IDLE;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.NOT_STARTED;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.RECOVERING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.RUNNING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.STARTING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.SUCCESS;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.UNKNOWN;
import static org.apache.griffin.core.job.entity.LivySessionStates.isActive;
import static org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType.BATCH;
import static org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType.STREAMING;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.event.GriffinEventManager;
import org.apache.griffin.core.event.JobEvent;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.BatchJob;
import org.apache.griffin.core.job.entity.JobHealth;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.JobState;
import org.apache.griffin.core.job.entity.JobType;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.apache.griffin.core.job.entity.LivySessionStates.State;
import org.apache.griffin.core.job.entity.StreamingJob;
import org.apache.griffin.core.job.repo.BatchJobRepo;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.job.repo.JobRepo;
import org.apache.griffin.core.job.repo.StreamingJobRepo;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType;
import org.apache.griffin.core.measure.repo.GriffinMeasureRepo;
import org.apache.griffin.core.util.JsonUtil;
import org.apache.griffin.core.util.YarnNetUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Service
public class JobServiceImpl implements JobService {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(JobServiceImpl.class);
    public static final String GRIFFIN_JOB_ID = "griffinJobId";
    private static final int MAX_PAGE_SIZE = 1024;
    private static final int DEFAULT_PAGE_SIZE = 10;
    static final String START = "start";
    static final String STOP = "stop";

    @Autowired
    @Qualifier("schedulerFactoryBean")
    private SchedulerFactoryBean factory;
    @Autowired
    private JobInstanceRepo instanceRepo;
    @Autowired
    private Environment env;
    @Autowired
    private GriffinMeasureRepo measureRepo;
    @Autowired
    private BatchJobRepo batchJobRepo;
    @Autowired
    private StreamingJobRepo streamingJobRepo;
    @Autowired
    private JobRepo<AbstractJob> jobRepo;
    @Autowired
    private BatchJobOperatorImpl batchJobOp;
    @Autowired
    private StreamingJobOperatorImpl streamingJobOp;
    @Autowired
    private GriffinEventManager eventManager;
    @Autowired
    private LivyTaskSubmitHelper livyTaskSubmitHelper;

    public JobServiceImpl() {

    }

    @Override
    public List<AbstractJob> getAliveJobs(String type) {
        List<? extends AbstractJob> jobs;
        if (JobType.BATCH.getName().equals(type)) {
            jobs = batchJobRepo.findByDeleted(false);
        } else if (JobType.STREAMING.getName().equals(type)) {
            jobs = streamingJobRepo.findByDeleted(false);
        } else {
            jobs = jobRepo.findByDeleted(false);
        }
        return getJobDataBeans(jobs);
    }

    private List<AbstractJob> getJobDataBeans(List<? extends AbstractJob> jobs) {
        List<AbstractJob> dataList = new ArrayList<>();
        try {
            for (AbstractJob job : jobs) {
                JobState jobState = genJobState(job);
                job.setJobState(jobState);
                dataList.add(job);
            }
        } catch (SchedulerException e) {
            LOGGER.error("Failed to get RUNNING jobs.", e);
            throw new GriffinException
                .ServiceException("Failed to get RUNNING jobs.", e);
        }
        return dataList;
    }

    @Override
    public AbstractJob addJob(AbstractJob job) throws Exception {
        JobEvent jobEvent = JobEvent.yieldJobEventBeforeCreation(job);
        eventManager.notifyListeners(jobEvent);
        Long measureId = job.getMeasureId();
        GriffinMeasure measure = getMeasureIfValid(measureId);
        JobOperator op = getJobOperator(measure.getProcessType());
        AbstractJob jobSaved = op.add(job, measure);
        jobEvent = JobEvent.yieldJobEventAfterCreation(jobSaved);
        eventManager.notifyListeners(jobEvent);
        return jobSaved;
    }

    @Override
    public AbstractJob getJobConfig(Long jobId) {
        AbstractJob job = jobRepo.findByIdAndDeleted(jobId, false);
        if (job == null) {
            LOGGER.warn("Job id {} does not exist.", jobId);
            throw new GriffinException
                .NotFoundException(JOB_ID_DOES_NOT_EXIST);
        }
        return job;
    }

    /**
     * @param jobId  job id
     * @param action job operation: start job, stop job
     */
    @Override
    public AbstractJob onAction(Long jobId, String action) throws Exception {
        AbstractJob job = jobRepo.findByIdAndDeleted(jobId, false);
        validateJobExist(job);
        JobOperator op = getJobOperator(job);
        doAction(action, job, op);
        JobState jobState = genJobState(job, action);
        job.setJobState(jobState);
        return job;
    }

    private void doAction(String action, AbstractJob job, JobOperator op)
        throws Exception {
        switch (action) {
            case START:
                op.start(job);
                break;
            case STOP:
                op.stop(job);
                break;
            default:
                throw new GriffinException
                    .NotFoundException(NO_SUCH_JOB_ACTION);
        }
    }


    /**
     * logically delete
     * 1. pause these jobs
     * 2. set these jobs as deleted status
     *
     * @param jobId griffin job id
     */
    @Override
    public void deleteJob(Long jobId) throws SchedulerException {
        AbstractJob job = jobRepo.findByIdAndDeleted(jobId, false);
        validateJobExist(job);
        JobEvent event = JobEvent.yieldJobEventBeforeRemoval(job);
        eventManager.notifyListeners(event);
        JobOperator op = getJobOperator(job);
        op.delete(job);
        event = JobEvent.yieldJobEventAfterRemoval(job);
        eventManager.notifyListeners(event);
    }

    /**
     * logically delete
     *
     * @param name griffin job name which may not be unique.
     */
    @Override
    public void deleteJob(String name) throws SchedulerException {
        List<AbstractJob> jobs = jobRepo.findByJobNameAndDeleted(name, false);
        if (CollectionUtils.isEmpty(jobs)) {
            LOGGER.warn("There is no job with '{}' name.", name);
            throw new GriffinException
                .NotFoundException(JOB_NAME_DOES_NOT_EXIST);
        }
        for (AbstractJob job : jobs) {
            JobEvent event = JobEvent.yieldJobEventBeforeRemoval(job);
            eventManager.notifyListeners(event);
            JobOperator op = getJobOperator(job);
            op.delete(job);
            event = JobEvent.yieldJobEventAfterRemoval(job);
            eventManager.notifyListeners(event);
        }
    }

    @Override
    public List<JobInstanceBean> findInstancesOfJob(
        Long jobId,
        int page,
        int size) {
        AbstractJob job = jobRepo.findByIdAndDeleted(jobId, false);
        if (job == null) {
            LOGGER.warn("Job id {} does not exist.", jobId);
            throw new GriffinException
                .NotFoundException(JOB_ID_DOES_NOT_EXIST);
        }
        size = size > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : size;
        size = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        Pageable pageable = new PageRequest(page, size,
            Sort.Direction.DESC, "tms");
        List<JobInstanceBean> instances = instanceRepo.findByJobId(jobId,
            pageable);
        return updateState(instances);
    }

    @Override
    public JobInstanceBean findInstance(Long id) {
        JobInstanceBean bean = instanceRepo.findByInstanceId(id);
        if (bean == null) {
            LOGGER.warn("Instance id {} does not exist.", id);
            throw new GriffinException
                .NotFoundException(INSTANCE_ID_DOES_NOT_EXIST);
        }
        return bean;
    }

    private List<JobInstanceBean> updateState(List<JobInstanceBean> instances) {
        for (JobInstanceBean instance : instances) {
            State state = instance.getState();
            if (state.equals(UNKNOWN) || isActive(state)) {
                syncInstancesOfJob(instance);
            }
        }
        return instances;
    }

    @Override
    public List<JobInstanceBean> findInstancesByTriggerKey(String triggerKey) {
        return instanceRepo.findByTriggerKey(triggerKey);
    }

    /**
     * a job is regard as healthy job when its latest instance is in healthy
     * state.
     *
     * @return job healthy statistics
     */
    @Override
    public JobHealth getHealthInfo() {
        JobHealth jobHealth = new JobHealth();
        List<AbstractJob> jobs = jobRepo.findByDeleted(false);
        for (AbstractJob job : jobs) {
            JobOperator op = getJobOperator(job);
            try {
                jobHealth = op.getHealth(jobHealth, job);
            } catch (SchedulerException e) {
                LOGGER.error("Job schedule exception. {}", e);
                throw new GriffinException
                    .ServiceException("Fail to Get HealthInfo", e);
            }

        }
        return jobHealth;
    }

    @Scheduled(fixedDelayString = "${jobInstance.expired.milliseconds}")
    public void deleteExpiredJobInstance() {
        Long timeMills = System.currentTimeMillis();
        List<JobInstanceBean> instances = instanceRepo
            .findByExpireTmsLessThanEqual
                (timeMills);
        if (!batchJobOp.pauseJobInstances(instances)) {
            LOGGER.error("Pause job failure.");
            return;
        }
        int count = instanceRepo.deleteByExpireTimestamp(timeMills);
        LOGGER.info("Delete {} expired job instances.", count);
    }

    private void validateJobExist(AbstractJob job) {
        if (job == null) {
            LOGGER.warn("Griffin job does not exist.");
            throw new GriffinException.NotFoundException(JOB_ID_DOES_NOT_EXIST);
        }
    }

    private JobOperator getJobOperator(AbstractJob job) {
        if (job instanceof BatchJob) {
            return batchJobOp;
        } else if (job instanceof StreamingJob) {
            return streamingJobOp;
        }
        throw new GriffinException.BadRequestException
            (JOB_TYPE_DOES_NOT_SUPPORT);
    }

    private JobOperator getJobOperator(ProcessType type) {
        if (type == BATCH) {
            return batchJobOp;
        } else if (type == STREAMING) {
            return streamingJobOp;
        }
        throw new GriffinException.BadRequestException
            (MEASURE_TYPE_DOES_NOT_SUPPORT);
    }

    TriggerKey getTriggerKeyIfValid(String qName, String qGroup) throws
        SchedulerException {
        TriggerKey triggerKey = triggerKey(qName, qGroup);
        if (factory.getScheduler().checkExists(triggerKey)) {
            throw new GriffinException.ConflictException
                (QUARTZ_JOB_ALREADY_EXIST);
        }
        return triggerKey;
    }

    List<? extends Trigger> getTriggers(String name, String group) throws
        SchedulerException {
        if (name == null || group == null) {
            return null;
        }
        JobKey jobKey = new JobKey(name, group);
        Scheduler scheduler = factory.getScheduler();
        return scheduler.getTriggersOfJob(jobKey);
    }

    private JobState genJobState(AbstractJob job, String action) throws
        SchedulerException {
        JobOperator op = getJobOperator(job);
        JobState state = op.getState(job, action);
        job.setJobState(state);
        return state;
    }

    private JobState genJobState(AbstractJob job) throws SchedulerException {
        return genJobState(job, null);
    }

    void addJob(TriggerKey tk, AbstractJob job, ProcessType type) throws
        Exception {
        JobDetail jobDetail = addJobDetail(tk, job);
        Trigger trigger = genTriggerInstance(tk, jobDetail, job, type);
        factory.getScheduler().scheduleJob(trigger);
    }

    String getQuartzName(AbstractJob job) {
        return job.getJobName() + "_" + System.currentTimeMillis();
    }

    String getQuartzGroup() {
        return "BA";
    }

    boolean isValidJobName(String jobName) {
        if (StringUtils.isEmpty(jobName)) {
            LOGGER.warn("Job name cannot be empty.");
            return false;
        }
        int size = jobRepo.countByJobNameAndDeleted(jobName, false);
        if (size > 0) {
            LOGGER.warn("Job name already exits.");
            return false;
        }
        return true;
    }


    private GriffinMeasure getMeasureIfValid(Long measureId) {
        GriffinMeasure measure = measureRepo.findByIdAndDeleted(measureId,
            false);
        if (measure == null) {
            LOGGER.warn("The measure id {} isn't valid. Maybe it doesn't " +
                    "exist or is external measure type.",
                measureId);
            throw new GriffinException.BadRequestException(INVALID_MEASURE_ID);
        }
        return measure;
    }

    private Trigger genTriggerInstance(TriggerKey tk, JobDetail jd, AbstractJob
        job, ProcessType type) {
        TriggerBuilder builder = newTrigger().withIdentity(tk).forJob(jd);
        if (type == BATCH) {
            TimeZone timeZone = getTimeZone(job.getTimeZone());
            return builder.withSchedule(cronSchedule(job.getCronExpression())
                .inTimeZone(timeZone)).build();
        } else if (type == STREAMING) {
            return builder.startNow().withSchedule(simpleSchedule()
                .withRepeatCount(0)).build();
        }
        throw new GriffinException.BadRequestException
            (JOB_TYPE_DOES_NOT_SUPPORT);

    }

    private JobDetail addJobDetail(TriggerKey triggerKey, AbstractJob job)
        throws SchedulerException {
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = jobKey(triggerKey.getName(), triggerKey.getGroup());
        JobDetail jobDetail;
        Boolean isJobKeyExist = scheduler.checkExists(jobKey);
        if (isJobKeyExist) {
            jobDetail = scheduler.getJobDetail(jobKey);
        } else {
            jobDetail = newJob(JobInstance.class).storeDurably().withIdentity
                (jobKey).build();
        }
        setJobDataMap(jobDetail, job);
        scheduler.addJob(jobDetail, isJobKeyExist);
        return jobDetail;
    }

    private void setJobDataMap(JobDetail jd, AbstractJob job) {
        JobDataMap jobDataMap = jd.getJobDataMap();
        jobDataMap.put(GRIFFIN_JOB_ID, job.getId().toString());
    }


    /**
     * deleteJobsRelateToMeasure
     * 1. search jobs related to measure
     * 2. deleteJob
     *
     * @param measureId measure id
     */
    public void deleteJobsRelateToMeasure(Long measureId) throws
        SchedulerException {
        List<AbstractJob> jobs = jobRepo.findByMeasureIdAndDeleted(measureId,
            false);
        if (CollectionUtils.isEmpty(jobs)) {
            LOGGER.info("Measure id {} has no related jobs.", measureId);
            return;
        }
        for (AbstractJob job : jobs) {
            JobOperator op = getJobOperator(job);
            op.delete(job);
        }
    }

    @Scheduled(fixedDelayString = "${jobInstance.fixedDelay.in.milliseconds}")
    public void syncInstancesOfAllJobs() {
        LivySessionStates.State[] states = {STARTING, NOT_STARTED, RECOVERING,
            IDLE, RUNNING, BUSY};
        List<JobInstanceBean> beans = instanceRepo.findByActiveState(states);
        for (JobInstanceBean jobInstance : beans) {
            syncInstancesOfJob(jobInstance);
        }
    }

    /**
     * call livy to update part of job instance table data associated with group
     * and jobName in mysql.
     *
     * @param instance job instance livy info
     */
    private void syncInstancesOfJob(JobInstanceBean instance) {
        if (instance.getSessionId() == null) {
            return;
        }
        String uri = env.getProperty("livy.uri") + "/"
            + instance.getSessionId();
        TypeReference<HashMap<String, Object>> type =
            new TypeReference<HashMap<String, Object>>() {
            };
        try {
            String resultStr = livyTaskSubmitHelper.getFromLivy(uri);
            LOGGER.info(resultStr);

            HashMap<String, Object> resultMap = JsonUtil.toEntity(resultStr,
                type);
            setJobInstanceIdAndUri(instance, resultMap);
        } catch (ResourceAccessException e) {
            LOGGER.error("Your url may be wrong. Please check {}.\n {}", uri, e
                .getMessage());
        } catch (HttpClientErrorException e) {
            LOGGER.warn("sessionId({}) appId({}) {}.", instance.getSessionId(),
                instance.getAppId(), e.getMessage());
            setStateByYarn(instance, e);
            livyTaskSubmitHelper.decreaseCurTaskNum(instance.getId());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

    }

    private void setStateByYarn(JobInstanceBean instance,
                                HttpClientErrorException e) {
        if (!checkStatus(instance, e)) {
            int code = e.getStatusCode().value();
            boolean match = (code == 400 || code == 404)
                && instance.getAppId() != null;
            //this means your url is correct,but your param is wrong or livy
            //session may be overdue.
            if (match) {
                setStateByYarn(instance);
            }
        }

    }

    /**
     * Check instance status in case that session id is overdue and app id is
     * null and so we cannot update instance state
     * .
     *
     * @param instance job instance bean
     * @param e        HttpClientErrorException
     * @return boolean
     */
    private boolean checkStatus(JobInstanceBean instance,
                                HttpClientErrorException e) {
        int code = e.getStatusCode().value();
        String appId = instance.getAppId();
        String responseBody = e.getResponseBodyAsString();
        Long sessionId = instance.getSessionId();
        sessionId = sessionId != null ? sessionId : -1;
        // If code is 404 and appId is null and response body is like 'Session
        // {id} not found',this means instance may not be scheduled for
        // a long time by spark for too many tasks. It may be dead.
        if (code == 404 && appId == null && (responseBody != null &&
            responseBody.contains(sessionId.toString()))) {
            instance.setState(DEAD);
            instance.setDeleted(true);
            instanceRepo.save(instance);
            return true;
        }
        return false;
    }

    private void setStateByYarn(JobInstanceBean instance) {
        LOGGER.warn("Spark session {} may be overdue! " +
            "Now we use yarn to update state.", instance.getSessionId());
        String yarnUrl = env.getProperty("yarn.uri");
        boolean success = YarnNetUtil.update(yarnUrl, instance);
        if (!success) {
            if (instance.getState().equals(UNKNOWN)) {
                return;
            }
            instance.setState(UNKNOWN);
        }
        instanceRepo.save(instance);
    }


    private void setJobInstanceIdAndUri(JobInstanceBean instance, HashMap<String
        , Object> resultMap) {
        if (resultMap != null) {
            Object state = resultMap.get("state");
            Object appId = resultMap.get("appId");
            instance.setState(state == null ? null : LivySessionStates.State
                .valueOf(state.toString().toUpperCase
                    ()));
            instance.setAppId(appId == null ? null : appId.toString());
            instance.setAppUri(appId == null ? null : env
                .getProperty("yarn.uri") + "/cluster/app/" + appId);
            instanceRepo.save(instance);
            // If Livy returns to success or dead, task execution completes one,TaskNum--
            if (instance.getState().equals(SUCCESS) || instance.getState().equals(DEAD)) {
                livyTaskSubmitHelper.decreaseCurTaskNum(instance.getSessionId());
            }
        }
    }

    public Boolean isJobHealthy(Long jobId) {
        Pageable pageable = new PageRequest(0, 1, Sort.Direction.DESC, "tms");
        List<JobInstanceBean> instances = instanceRepo.findByJobId(jobId,
            pageable);
        return !CollectionUtils.isEmpty(instances) && LivySessionStates
            .isHealthy(instances.get(0).getState());
    }

    @Override
    public String getJobHdfsSinksPath(String jobName, long timestamp) {
        List<AbstractJob> jobList = jobRepo.findByJobNameAndDeleted(
            jobName, false);
        if (jobList.size() == 0) {
            return null;
        }
        if (jobList.get(0).getType().toLowerCase().equals("batch")) {
            return getSinksPath(ENV_BATCH)
                + "/" + jobName + "/" + timestamp + "";
        }

        return getSinksPath(ENV_STREAMING)
            + "/" + jobName + "/" + timestamp + "";
    }

    private String getSinksPath(String jsonString) {
        try {
            JSONObject obj = new JSONObject(jsonString);
            JSONArray persistArray = obj.getJSONArray("sinks");
            for (int i = 0; i < persistArray.length(); i++) {
                Object type = persistArray.getJSONObject(i).get("type");
                if (type instanceof String
                    && "hdfs".equalsIgnoreCase(String.valueOf(type))) {
                    return persistArray.getJSONObject(i)
                        .getJSONObject("config").getString("path");
                }
            }

            return null;
        } catch (Exception ex) {
            LOGGER.error("Fail to get Persist path from {}", jsonString, ex);
            return null;
        }
    }

    @Override
    public String triggerJobById(Long id) throws SchedulerException {
        AbstractJob job = jobRepo.findByIdAndDeleted(id, false);
        validateJobExist(job);
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = jobKey(job.getName(), job.getGroup());
        if (scheduler.checkExists(jobKey)) {
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobKey)
                    .startNow()
                    .build();
            scheduler.scheduleJob(trigger);
            return trigger.getKey().toString();
        } else {
            throw new GriffinException.NotFoundException(JOB_ID_DOES_NOT_EXIST);
        }
    }
}
