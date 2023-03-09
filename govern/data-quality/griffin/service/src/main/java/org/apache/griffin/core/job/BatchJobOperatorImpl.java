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

import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_CONNECTOR_NAME;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_CRON_EXPRESSION;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_JOB_NAME;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_IS_NOT_IN_PAUSED_STATUS;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_IS_NOT_SCHEDULED;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_KEY_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.MISSING_BASELINE_CONFIG;
import static org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType.BATCH;
import static org.quartz.CronExpression.isValidExpression;
import static org.quartz.JobKey.jobKey;
import static org.quartz.Trigger.TriggerState;
import static org.quartz.Trigger.TriggerState.BLOCKED;
import static org.quartz.Trigger.TriggerState.NORMAL;
import static org.quartz.Trigger.TriggerState.PAUSED;
import static org.quartz.TriggerKey.triggerKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.BatchJob;
import org.apache.griffin.core.job.entity.JobDataSegment;
import org.apache.griffin.core.job.entity.JobHealth;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.JobState;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.apache.griffin.core.job.repo.BatchJobRepo;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.measure.entity.DataSource;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class BatchJobOperatorImpl implements JobOperator {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(BatchJobOperatorImpl.class);

    @Autowired
    @Qualifier("schedulerFactoryBean")
    private SchedulerFactoryBean factory;
    @Autowired
    private JobInstanceRepo instanceRepo;
    @Autowired
    private BatchJobRepo batchJobRepo;
    @Autowired
    private JobServiceImpl jobService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AbstractJob add(AbstractJob job, GriffinMeasure measure)
        throws Exception {
        validateParams(job, measure);
        String qName = jobService.getQuartzName(job);
        String qGroup = jobService.getQuartzGroup();
        TriggerKey triggerKey = jobService.getTriggerKeyIfValid(qName, qGroup);
        BatchJob batchJob = genBatchJobBean(job, qName, qGroup);
        batchJob = batchJobRepo.save(batchJob);
        jobService.addJob(triggerKey, batchJob, BATCH);
        return job;
    }

    private BatchJob genBatchJobBean(AbstractJob job,
                                     String qName,
                                     String qGroup) {
        BatchJob batchJob = (BatchJob) job;
        batchJob.setMetricName(job.getJobName());
        batchJob.setGroup(qGroup);
        batchJob.setName(qName);
        return batchJob;
    }

    /**
     * all states: BLOCKED  COMPLETE ERROR NONE  NORMAL   PAUSED
     * to start states: PAUSED
     * to stop states: BLOCKED   NORMAL
     *
     * @param job streaming job
     */
    @Override
    public void start(AbstractJob job) {
        String name = job.getName();
        String group = job.getGroup();
        TriggerState state = getTriggerState(name, group);
        if (state == null) {
            throw new GriffinException.BadRequestException(
                JOB_IS_NOT_SCHEDULED);
        }
        /* If job is not in paused state,we can't start it
        as it may be RUNNING.*/
        if (state != PAUSED) {
            throw new GriffinException.BadRequestException
                (JOB_IS_NOT_IN_PAUSED_STATUS);
        }
        JobKey jobKey = jobKey(name, group);
        try {
            factory.getScheduler().resumeJob(jobKey);
        } catch (SchedulerException e) {
            throw new GriffinException.ServiceException(
                "Failed to start job.", e);
        }
    }

    @Override
    public void stop(AbstractJob job) {
        pauseJob((BatchJob) job, false);
    }

    @Override
    @Transactional
    public void delete(AbstractJob job) {
        pauseJob((BatchJob) job, true);
    }


    @Override
    public JobHealth getHealth(JobHealth jobHealth, AbstractJob job)
        throws SchedulerException {
        List<? extends Trigger> triggers = jobService
            .getTriggers(job.getName(), job.getGroup());
        if (!CollectionUtils.isEmpty(triggers)) {
            jobHealth.setJobCount(jobHealth.getJobCount() + 1);
            if (jobService.isJobHealthy(job.getId())) {
                jobHealth.setHealthyJobCount(
                    jobHealth.getHealthyJobCount() + 1);
            }
        }
        return jobHealth;
    }

    @Override
    public JobState getState(AbstractJob job, String action)
        throws SchedulerException {
        JobState jobState = new JobState();
        Scheduler scheduler = factory.getScheduler();
        if (job.getGroup() == null || job.getName() == null) {
            return null;
        }
        TriggerKey triggerKey = triggerKey(job.getName(), job.getGroup());
        TriggerState triggerState = scheduler.getTriggerState(triggerKey);
        jobState.setState(triggerState.toString());
        jobState.setToStart(getStartStatus(triggerState));
        jobState.setToStop(getStopStatus(triggerState));
        setTriggerTime(job, jobState);
        return jobState;
    }

    private void setTriggerTime(AbstractJob job, JobState jobState)
        throws SchedulerException {
        List<? extends Trigger> triggers = jobService
            .getTriggers(job.getName(), job.getGroup());
        // If triggers are empty, in Griffin it means job is completed whose
        // trigger state is NONE or not scheduled.
        if (CollectionUtils.isEmpty(triggers)) {
            return;
        }
        Trigger trigger = triggers.get(0);
        Date nextFireTime = trigger.getNextFireTime();
        Date previousFireTime = trigger.getPreviousFireTime();
        jobState.setNextFireTime(nextFireTime != null ?
            nextFireTime.getTime() : -1);
        jobState.setPreviousFireTime(previousFireTime != null ?
            previousFireTime.getTime() : -1);
    }

    /**
     * only PAUSED state of job can be started
     *
     * @param state job state
     * @return true: job can be started, false: job is running which cannot be
     * started
     */
    private boolean getStartStatus(TriggerState state) {
        return state == PAUSED;
    }

    /**
     * only NORMAL or  BLOCKED state of job can be started
     *
     * @param state job state
     * @return true: job can be stopped, false: job is running which cannot be
     * stopped
     */
    private boolean getStopStatus(TriggerState state) {
        return state == NORMAL || state == BLOCKED;
    }


    private TriggerState getTriggerState(String name, String group) {
        try {
            List<? extends Trigger> triggers = jobService.getTriggers(name,
                group);
            if (CollectionUtils.isEmpty(triggers)) {
                return null;
            }
            TriggerKey key = triggers.get(0).getKey();
            return factory.getScheduler().getTriggerState(key);
        } catch (SchedulerException e) {
            LOGGER.error("Failed to delete job", e);
            throw new GriffinException
                .ServiceException("Failed to delete job", e);
        }

    }


    /**
     * @param job    griffin job
     * @param delete if job needs to be deleted,set isNeedDelete true,otherwise
     *               it just will be paused.
     */
    private void pauseJob(BatchJob job, boolean delete) {
        try {
            pauseJob(job.getGroup(), job.getName());
            pausePredicateJob(job);
            job.setDeleted(delete);
            batchJobRepo.save(job);
        } catch (Exception e) {
            LOGGER.error("Job schedule happens exception.", e);
            throw new GriffinException.ServiceException("Job schedule " +
                "happens exception.", e);
        }
    }

    private void pausePredicateJob(BatchJob job) throws SchedulerException {
        List<JobInstanceBean> instances = instanceRepo.findByJobId(job.getId());
        for (JobInstanceBean instance : instances) {
            if (!instance.isPredicateDeleted()) {
                deleteJob(instance.getPredicateGroup(), instance
                    .getPredicateName());
                instance.setPredicateDeleted(true);
                if (instance.getState().equals(LivySessionStates.State.FINDING)) {
                    instance.setState(LivySessionStates.State.NOT_FOUND);
                }
            }
        }
    }

    public void deleteJob(String group, String name) throws SchedulerException {
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = new JobKey(name, group);
        if (!scheduler.checkExists(jobKey)) {
            LOGGER.info("Job({},{}) does not exist.", jobKey.getGroup(), jobKey
                .getName());
            return;
        }
        scheduler.deleteJob(jobKey);

    }

    private void pauseJob(String group, String name) throws SchedulerException {
        if (StringUtils.isEmpty(group) || StringUtils.isEmpty(name)) {
            return;
        }
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = new JobKey(name, group);
        if (!scheduler.checkExists(jobKey)) {
            LOGGER.warn("Job({},{}) does not exist.", jobKey.getGroup(), jobKey
                .getName());
            throw new GriffinException.NotFoundException
                (JOB_KEY_DOES_NOT_EXIST);
        }
        scheduler.pauseJob(jobKey);
    }

    public boolean pauseJobInstances(List<JobInstanceBean> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return true;
        }
        List<JobInstanceBean> deletedInstances = new ArrayList<>();
        boolean pauseStatus = true;
        for (JobInstanceBean instance : instances) {
            boolean status = pauseJobInstance(instance, deletedInstances);
            pauseStatus = pauseStatus && status;
        }
        instanceRepo.saveAll(deletedInstances);
        return pauseStatus;
    }

    private boolean pauseJobInstance(JobInstanceBean instance,
                                     List<JobInstanceBean> deletedInstances) {
        String pGroup = instance.getPredicateGroup();
        String pName = instance.getPredicateName();
        try {
            if (!instance.isPredicateDeleted()) {
                deleteJob(pGroup, pName);
                instance.setPredicateDeleted(true);
                deletedInstances.add(instance);
            }
        } catch (SchedulerException e) {
            LOGGER.error("Failed to pause predicate job({},{}).", pGroup,
                    pName);
            return false;
        }
        return true;
    }

    private void validateParams(AbstractJob job, GriffinMeasure measure) {
        if (!jobService.isValidJobName(job.getJobName())) {
            throw new GriffinException.BadRequestException(INVALID_JOB_NAME);
        }
        if (!isValidCronExpression(job.getCronExpression())) {
            throw new GriffinException.BadRequestException
                (INVALID_CRON_EXPRESSION);
        }
        if (!isValidBaseLine(job.getSegments())) {
            throw new GriffinException.BadRequestException
                (MISSING_BASELINE_CONFIG);
        }
        List<String> names = getConnectorNames(measure);
        if (!isValidConnectorNames(job.getSegments(), names)) {
            throw new GriffinException.BadRequestException
                (INVALID_CONNECTOR_NAME);
        }
    }

    private boolean isValidCronExpression(String cronExpression) {
        if (StringUtils.isEmpty(cronExpression)) {
            LOGGER.warn("Cron Expression is empty.");
            return false;
        }
        if (!isValidExpression(cronExpression)) {
            LOGGER.warn("Cron Expression is invalid: {}", cronExpression);
            return false;
        }
        return true;
    }

    private boolean isValidBaseLine(List<JobDataSegment> segments) {
        assert segments != null;
        for (JobDataSegment jds : segments) {
            if (jds.isAsTsBaseline()) {
                return true;
            }
        }
        LOGGER.warn("Please set segment timestamp baseline " +
            "in as.baseline field.");
        return false;
    }

    private boolean isValidConnectorNames(List<JobDataSegment> segments,
                                          List<String> names) {
        assert segments != null;
        Set<String> sets = new HashSet<>();
        for (JobDataSegment segment : segments) {
            String dcName = segment.getDataConnectorName();
            sets.add(dcName);
            boolean exist = names.stream().anyMatch(name -> name.equals
                (dcName));
            if (!exist) {
                LOGGER.warn("Param {} is a illegal string. " +
                    "Please input one of strings in {}.", dcName, names);
                return false;
            }
        }
        if (sets.size() < segments.size()) {
            LOGGER.warn("Connector names in job data segment " +
                "cannot duplicate.");
            return false;
        }
        return true;
    }

    private List<String> getConnectorNames(GriffinMeasure measure) {
        Set<String> sets = new HashSet<>();
        List<DataSource> sources = measure.getDataSources();
        for (DataSource source : sources) {
            sets.add(source.getConnector().getName());
        }
        if (sets.size() < sources.size()) {
            LOGGER.warn("Connector names cannot be repeated.");
            return Collections.emptyList();
        }
        return new ArrayList<>(sets);
    }
}
