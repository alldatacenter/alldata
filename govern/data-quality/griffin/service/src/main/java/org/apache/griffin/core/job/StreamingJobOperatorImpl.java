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

import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_JOB_NAME;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.STREAMING_JOB_IS_RUNNING;
import static org.apache.griffin.core.job.JobServiceImpl.START;
import static org.apache.griffin.core.job.JobServiceImpl.STOP;
import static org.apache.griffin.core.job.entity.LivySessionStates.State;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.STOPPED;
import static org.apache.griffin.core.job.entity.LivySessionStates.convert2QuartzState;
import static org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType.STREAMING;
import static org.quartz.TriggerKey.triggerKey;

import java.util.List;
import javax.annotation.PostConstruct;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobHealth;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.JobState;
import org.apache.griffin.core.job.entity.StreamingJob;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.job.repo.StreamingJobRepo;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.util.YarnNetUtil;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Service
public class StreamingJobOperatorImpl implements JobOperator {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(StreamingJobOperatorImpl.class);
    @Autowired
    private StreamingJobRepo streamingJobRepo;
    @Autowired
    private Environment env;
    @Autowired
    private JobServiceImpl jobService;
    @Autowired
    private JobInstanceRepo instanceRepo;
    @Autowired
    @Qualifier("schedulerFactoryBean")
    private SchedulerFactoryBean factory;
    @Autowired
    private LivyTaskSubmitHelper livyTaskSubmitHelper;

    private String livyUri;

    @PostConstruct
    public void init() {
        livyUri = env.getProperty("livy.uri");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AbstractJob add(AbstractJob job, GriffinMeasure measure) throws
        Exception {
        validateParams(job);
        String qName = jobService.getQuartzName(job);
        String qGroup = jobService.getQuartzGroup();
        TriggerKey triggerKey = jobService.getTriggerKeyIfValid(qName, qGroup);
        StreamingJob streamingJob = genStreamingJobBean(job, qName, qGroup);
        streamingJob = streamingJobRepo.save(streamingJob);
        jobService.addJob(triggerKey, streamingJob, STREAMING);
        return streamingJob;
    }

    private StreamingJob genStreamingJobBean(AbstractJob job, String qName,
                                             String qGroup) {
        StreamingJob streamingJob = (StreamingJob) job;
        streamingJob.setMetricName(job.getJobName());
        streamingJob.setGroup(qGroup);
        streamingJob.setName(qName);
        return streamingJob;
    }

    /**
     * active states: NOT_STARTED, STARTING, RECOVERING, IDLE, RUNNING, BUSY
     * inactive states: SHUTTING_DOWN, ERROR, DEAD, SUCCESS
     *
     * @param job streaming job
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(AbstractJob job) throws Exception {
        StreamingJob streamingJob = (StreamingJob) job;
        verifyJobState(streamingJob);
        streamingJob = streamingJobRepo.save(streamingJob);
        String qName = jobService.getQuartzName(job);
        String qGroup = jobService.getQuartzGroup();
        TriggerKey triggerKey = triggerKey(qName, qGroup);
        jobService.addJob(triggerKey, streamingJob, STREAMING);
    }

    private void verifyJobState(AbstractJob job) throws SchedulerException {
        /* Firstly you should check whether job is scheduled.
        If it is scheduled, triggers are empty. */
        List<? extends Trigger> triggers = jobService.getTriggers(
            job.getName(),
            job.getGroup());
        if (!CollectionUtils.isEmpty(triggers)) {
            throw new GriffinException.BadRequestException
                (STREAMING_JOB_IS_RUNNING);
        }
        /* Secondly you should check whether job instance is running. */
        List<JobInstanceBean> instances = instanceRepo.findByJobId(job.getId());
        instances.stream().filter(instance -> !instance.isDeleted()).forEach
            (instance -> {
                State state = instance.getState();
                String quartzState = convert2QuartzState(state);
                if (!getStartStatus(quartzState)) {
                    throw new GriffinException.BadRequestException
                        (STREAMING_JOB_IS_RUNNING);
                }
                instance.setDeleted(true);
            });
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stop(AbstractJob job) throws SchedulerException {
        StreamingJob streamingJob = (StreamingJob) job;
        stop(streamingJob, false);
    }

    @Override
    public void delete(AbstractJob job) throws SchedulerException {
        StreamingJob streamingJob = (StreamingJob) job;
        stop(streamingJob, true);
    }


    @Override
    public JobHealth getHealth(JobHealth jobHealth, AbstractJob job) {
        jobHealth.setJobCount(jobHealth.getJobCount() + 1);
        if (jobService.isJobHealthy(job.getId())) {
            jobHealth.setHealthyJobCount(jobHealth.getHealthyJobCount() + 1);
        }
        return jobHealth;
    }

    @Override
    public JobState getState(AbstractJob job, String action) {
        JobState jobState = new JobState();
        List<JobInstanceBean> instances = instanceRepo
            .findByJobId(job.getId());
        for (JobInstanceBean instance : instances) {
            State state = instance.getState();
            if (!instance.isDeleted() && state != null) {
                String quartzState = convert2QuartzState(state);
                jobState.setState(quartzState);
                jobState.setToStart(getStartStatus(quartzState));
                jobState.setToStop(getStopStatus(quartzState));
                break;
            }
        }
        setStateIfNull(action, jobState);
        return jobState;
    }

    private void setStateIfNull(String action, JobState jobState) {
        if (jobState.getState() == null && START.equals(action)) {
            jobState.setState("NORMAL");
            jobState.setToStop(true);
        } else if (jobState.getState() == null || STOP.equals(action)) {
            jobState.setState("NONE");
            jobState.setToStart(true);
        }

    }

    /**
     * NORMAL or BLOCKED state of job cannot be started
     *
     * @param state job state
     * @return true: job can be started, false: job is running which cannot be
     * started
     */
    private boolean getStartStatus(String state) {
        return !"NORMAL".equals(state) && !"BLOCKED".equals(state);
    }

    /**
     * COMPLETE or ERROR state of job cannot be stopped
     *
     * @param state job state
     * @return true: job can be stopped, false: job is running which cannot be
     * stopped
     */
    private boolean getStopStatus(String state) {
        return !"COMPLETE".equals(state) && !"ERROR".equals(state);
    }

    private void deleteByLivy(JobInstanceBean instance) {
        Long sessionId = instance.getSessionId();
        if (sessionId == null) {
            LOGGER.warn("Session id of instance({},{}) is null.", instance
                .getPredicateGroup(), instance.getPredicateName
                ());
            return;
        }
        String url = livyUri + "/" + instance.getSessionId();
        try {
            // Use livy helper to interact with livy
            livyTaskSubmitHelper.deleteByLivy(url);

            LOGGER.info("Job instance({}) has been deleted. {}", instance
                .getSessionId(), url);
        } catch (ResourceAccessException e) {
            LOGGER.error("Your url may be wrong. Please check {}.\n {}",
                livyUri, e.getMessage());
        } catch (RestClientException e) {
            LOGGER.warn("sessionId({}) appId({}) {}.", instance.getSessionId(),
                instance.getAppId(), e.getMessage());
            YarnNetUtil.delete(env.getProperty("yarn.uri"),
                instance.getAppId());
        }
    }


    /**
     * @param job    streaming job
     * @param delete true: delete job, false: only stop instance, but not delete
     *               job
     */
    private void stop(StreamingJob job, boolean delete) throws
        SchedulerException {
        pauseJob(job);
        /* to prevent situation that streaming job is submitted
        before pause or when pausing. */
        List<JobInstanceBean> instances = instanceRepo
            .findByJobId(job.getId());
        instances.stream().filter(instance -> !instance.isDeleted())
            .forEach(instance -> {
                State state = instance.getState();
                String quartzState = convert2QuartzState(state);
                if (getStopStatus(quartzState)) {
                    deleteByLivy(instance);

                }
                instance.setState(STOPPED);
                instance.setDeleted(true);
            });
        job.setDeleted(delete);
        streamingJobRepo.save(job);
    }

    private void pauseJob(StreamingJob job) throws SchedulerException {
        String name = job.getName();
        String group = job.getGroup();
        List<? extends Trigger> triggers = jobService.getTriggers(name, group);
        if (!CollectionUtils.isEmpty(triggers)) {
            factory.getScheduler().pauseJob(JobKey.jobKey(name, group));
        }
    }


    private void validateParams(AbstractJob job) {
        if (!jobService.isValidJobName(job.getJobName())) {
            throw new GriffinException.BadRequestException(INVALID_JOB_NAME);
        }
    }

}
