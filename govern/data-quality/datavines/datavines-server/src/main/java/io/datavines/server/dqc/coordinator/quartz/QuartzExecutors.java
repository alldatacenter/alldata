/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.dqc.coordinator.quartz;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import io.datavines.common.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import com.google.common.collect.Maps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.datavines.core.constant.DataVinesConstants;

/**
 * single Quartz executors instance
 */
@Slf4j
@Service
public class QuartzExecutors {

  @Autowired
  private Scheduler scheduler;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private QuartzExecutors() {
  }

  /**
   * add task trigger , if this task already exists, return this task with updated trigger
   *
   * @param clazz job class name
   * @param schedule schedule job info
   * @param schedule schedule
   */
    public void addJob(Class<? extends Job> clazz, final ScheduleJobInfo schedule) throws ParseException {

        String jobGroupName = buildJobGroupName(schedule);
        String jobName = buildJobName(schedule);

        Map<String, Object> jobDataMap = buildDataMap(schedule);
        String cronExpression = schedule.getCronExpression();

        /*
         * transform from server default timezone to schedule timezone
         * e.g. server default timezone is `UTC`
         * user set a schedule with startTime `2022-04-28 10:00:00`, timezone is `Asia/Shanghai`,
         * api skip to transform it and save into databases directly, startTime `2022-04-28 10:00:00`, timezone is `UTC`, which actually added 8 hours,
         * so when add job to quartz, it should recover by transform timezone
         */

        Date startDate = buildDate(schedule.getStartTime());
        Date endDate = buildDate(schedule.getEndTime());

        lock.writeLock().lock();
        try {

            JobKey jobKey = new JobKey(jobName, jobGroupName);
            JobDetail jobDetail;
            //add a task (if this task already exists, return this task directly)
            if (scheduler.checkExists(jobKey)) {
                jobDetail = scheduler.getJobDetail(jobKey);
                jobDetail.getJobDataMap().putAll(jobDataMap);
            } else {
                jobDetail = newJob(clazz).withIdentity(jobKey).build();
                jobDetail.getJobDataMap().putAll(jobDataMap);
                scheduler.addJob(jobDetail, false, true);
                log.info("Add job, job name: {}, group name: {}", jobName, jobGroupName);
            }

            TriggerKey triggerKey = new TriggerKey(jobName, jobGroupName);

            /*
             * Instructs the Scheduler that upon a mis-fire
             * situation, the CronTrigger wants to have it's
             * next-fire-time updated to the next time in the schedule after the
             * current time (taking into account any associated Calendar),
             * but it does not want to be fired now.
             */
            CronTrigger cronTrigger = newTrigger()
                  .withIdentity(triggerKey)
                  .startAt(startDate)
                  .endAt(endDate)
                  .withSchedule(
                          cronSchedule(cronExpression)
                                  .withMisfireHandlingInstructionDoNothing())
                  .forJob(jobDetail).build();

            if (scheduler.checkExists(triggerKey)) {
                // updateProcessInstance scheduler trigger when scheduler cycle changes
                CronTrigger oldCronTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
                String oldCronExpression = oldCronTrigger.getCronExpression();

                if (!StringUtils.equalsIgnoreCase(cronExpression, oldCronExpression)) {
                // reschedule job trigger
                    scheduler.rescheduleJob(triggerKey, cronTrigger);
                    log.info("reschedule job trigger, triggerName: {}, triggerGroupName: {}, cronExpression: {}, startDate: {}, endDate: {}",
                      jobName, jobGroupName, cronExpression, startDate, endDate);
                }
            } else {
                scheduler.scheduleJob(cronTrigger);
                log.info("schedule job trigger, triggerName: {}, triggerGroupName: {}, cronExpression: {}, startDate: {}, endDate: {}",
                    jobName, jobGroupName, cronExpression, startDate, endDate);
            }
        } catch (Exception e) {
            log.error("add job failed", e);
            throw new RuntimeException("add job failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

  /**
   * delete job
   *
   * @param schedule ScheduleJobInfo
   * @return true if the Job was found and deleted.
   */
    public boolean deleteJob(ScheduleJobInfo schedule) {
        lock.writeLock().lock(); String jobName = buildJobName(schedule);
        String jobGroupName = buildJobGroupName(schedule);
        JobKey jobKey = new JobKey(jobName,jobGroupName);
        try {
            if(scheduler.checkExists(jobKey)){
                log.info("try to delete job, job name: {}, job group name: {},", jobName, jobGroupName);
                return scheduler.deleteJob(jobKey);
            } else {
                return true;
            }
        } catch (SchedulerException e) {
            log.error("delete job : {} failed", jobName, e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * delete all jobs in job group
     *
     * @param jobGroupName job group name
     *
     * @return true if all of the Jobs were found and deleted, false if
     *      one or more were not deleted.
     */
    public boolean deleteAllJobs(Scheduler scheduler, String jobGroupName) {
        lock.writeLock().lock();
        try {
            log.info("try to delete all jobs in job group: {}", jobGroupName);
            List<JobKey> jobKeys =
              new ArrayList<>(scheduler.getJobKeys(GroupMatcher.groupEndsWith(jobGroupName)));
            return scheduler.deleteJobs(jobKeys);
        } catch (SchedulerException e) {
            log.error("delete all jobs in job group: {} failed",jobGroupName, e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * build job name
     * @param schedule ScheduleJobInfo
     * @return job name
     */
    private static String buildJobName(ScheduleJobInfo schedule) {
        return schedule.getType().getDescription() + "_job" + DataVinesConstants.UNDERLINE + schedule.getId();
    }

    /**
     * build job group name
     * @param schedule ScheduleJobInfo
     * @return job group name
     */
    private static String buildJobGroupName(ScheduleJobInfo schedule) {
        return schedule.getType().getDescription() + "_job_group" + DataVinesConstants.UNDERLINE + schedule.getDatasourceId();
    }

    /**
     * @param schedule  schedule
     * @return map
     */
    private static Map<String, Object> buildDataMap( ScheduleJobInfo schedule) {
        Map<String, Object> dataMap = Maps.newHashMap();
        dataMap.put(DataVinesConstants.JOB_ID, schedule.getId());
        dataMap.put(DataVinesConstants.DATASOURCE_ID, schedule.getDatasourceId());
        dataMap.put(DataVinesConstants.SCHEDULE, JSONUtils.toJsonString(schedule));
        return dataMap;
    }

    private static Date buildDate(LocalDateTime localDateTime){
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        Instant instant = zonedDateTime.toInstant();
        return Date.from(instant);
    }

    public  boolean isValid(String cronExpression){
        return CronExpression.isValidExpression(cronExpression);
    }
}
