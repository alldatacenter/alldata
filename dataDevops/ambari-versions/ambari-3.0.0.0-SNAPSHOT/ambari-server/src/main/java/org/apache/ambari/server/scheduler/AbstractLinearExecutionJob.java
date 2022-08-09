/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.scheduler;

import static org.apache.ambari.server.state.scheduler.BatchRequestJob.BATCH_REQUEST_BATCH_ID_KEY;
import static org.apache.ambari.server.state.scheduler.BatchRequestJob.BATCH_REQUEST_CLUSTER_NAME_KEY;
import static org.apache.ambari.server.state.scheduler.BatchRequestJob.BATCH_REQUEST_EXECUTION_ID_KEY;
import static org.apache.ambari.server.state.scheduler.RequestExecution.Status.ABORTED;
import static org.apache.ambari.server.state.scheduler.RequestExecution.Status.PAUSED;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.scheduler.BatchRequestJob;
import org.quartz.DateBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job that knows how to get the job name and group out of the JobDataMap using
 * pre-defined keys (constants) and contains code to schedule the identified job.
 * This abstract Job's implementation of execute() delegates to an abstract
 * template method "doWork()" (where the extending Job class's real work goes)
 * and then it schedules the follow-up job.
 */
public abstract class AbstractLinearExecutionJob implements ExecutionJob {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractLinearExecutionJob.class);
  protected ExecutionScheduleManager executionScheduleManager;

  public AbstractLinearExecutionJob(ExecutionScheduleManager executionScheduleManager) {
    this.executionScheduleManager = executionScheduleManager;
  }

  /**
   * Do the actual work of the fired job.
   * @throws AmbariException
   * @param properties
   */
  protected abstract void doWork(Map<String, Object> properties)
    throws AmbariException;

  protected abstract void finalizeExecution(Map<String, Object> properties)
    throws AmbariException;

  /**
   * Get the next job id from context and create a trigger to fire the next
   * job.
   * @param context
   * @throws JobExecutionException
   */
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    JobKey jobKey = context.getJobDetail().getKey();
    LOG.debug("Executing linear job: {}", jobKey);
    JobDataMap jobDataMap = context.getMergedJobDataMap();

    if (!executionScheduleManager.continueOnMisfire(context)) {
      throw new JobExecutionException("Canceled execution based on misfire"
        + " toleration threshold, job: " + jobKey
        + ", scheduleTime = " + context.getScheduledFireTime());
    }

    Map<String, Object> properties = jobDataMap.getWrappedMap();
    boolean finalize = false;

    // Perform work and exit if failure reported
    try {
      doWork(properties);
    } catch (AmbariException e) {
      LOG.error("Exception caught on execution of job " + jobKey +
        ". Exiting linear chain...", e);

      finalize = true;
      throw new JobExecutionException(e);

    } catch (RuntimeException e) {
      LOG.error("Unexpected exception caught on execution of job " + jobKey + ". " +
          "Exiting linear chain...", e);

      finalize = true;
      throw e;

    } finally {
      // Finalize before exiting chain
      if (finalize) {
        try {
          finalizeExecution(properties);
        } catch (AmbariException e) {
          LOG.warn("Unable to finalize execution for job: " + jobKey);
        }
      }
    }

    LOG.debug("Finished linear job: {}", jobKey);

    String nextJobName = jobDataMap.getString(NEXT_EXECUTION_JOB_NAME_KEY);
    String nextJobGroup = jobDataMap.getString(NEXT_EXECUTION_JOB_GROUP_KEY);

    // If no more jobs left, finalize and return
    if (nextJobName == null || nextJobName.isEmpty()) {
      LOG.debug("End of linear job chain. Returning with success.");
      try {
        finalizeExecution(properties);
      } catch (AmbariException e) {
        LOG.warn("Unable to finalize execution for job: " + jobKey);
      }
      return;
    }

    try {
      executionScheduleManager.pauseAfterBatchIfNeeded(jobDataMap.getLong(BATCH_REQUEST_EXECUTION_ID_KEY),
          jobDataMap.getLong(BATCH_REQUEST_BATCH_ID_KEY), jobDataMap.getString(BATCH_REQUEST_CLUSTER_NAME_KEY));
    } catch (AmbariException e) {
      LOG.warn("Received exception while trying to auto pause the scheduled request execution :", e);
    }

    String status = null;
    try {
      status = executionScheduleManager.getBatchRequestStatus(jobDataMap.getLong(BATCH_REQUEST_EXECUTION_ID_KEY), jobDataMap.getString(BATCH_REQUEST_CLUSTER_NAME_KEY));
    } catch (AmbariException e) {
      LOG.warn("Unable to define the status of batch request : ", e);
    }

    if(ABORTED.name().equals(status) || PAUSED.name().equals(status)) {
      LOG.info("The linear job chain was paused or aborted, not triggering the next one");
      return;
    }


    int separationSeconds = jobDataMap.getIntValue(NEXT_EXECUTION_SEPARATION_SECONDS);
    Object failedCount = properties.get(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY);
    Object totalCount = properties.get(BatchRequestJob.BATCH_REQUEST_TOTAL_TASKS_KEY);

    // Create trigger for next job execution
    // Persist counts with trigger, so that they apply to current batch only
    Trigger trigger = newTrigger()
      .forJob(nextJobName, nextJobGroup)
      .withIdentity("TriggerForJob-" + nextJobName, LINEAR_EXECUTION_TRIGGER_GROUP)
      .withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow())
      .startAt(futureDate(separationSeconds, DateBuilder.IntervalUnit.SECOND))
      .usingJobData(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY,
        failedCount != null ? (Integer) failedCount : 0)
      .usingJobData(BatchRequestJob.BATCH_REQUEST_TOTAL_TASKS_KEY,
        totalCount != null ? (Integer) totalCount : 0)
      .build();

    executionScheduleManager.scheduleJob(trigger);
  }
}
