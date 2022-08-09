/**
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

package org.apache.ambari.view.pig.resources.jobs.utils;

import org.apache.ambari.view.pig.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.pig.persistence.utils.Indexed;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.resources.jobs.JobResourceManager;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Polling manager
 * Makes scheduled repeated polling of templeton to
 * be aware of happen events like job finished,
 * killed, changed progress and so on.
 */
public class JobPolling implements Runnable {
  private final static Logger LOG =
      LoggerFactory.getLogger(JobPolling.class);

  /**
   * We should limit count of concurrent calls to templeton
   * to avoid high load on component
   */
  private static final int WORKER_COUNT = 2;

  private static final int POLLING_DELAY = 60;  // 1 minutes

  /**
   * In LONG_JOB_THRESHOLD seconds job reschedules polling from POLLING_DELAY to LONG_POLLING_DELAY
   */
  private static final int LONG_POLLING_DELAY = 10*60; // 10 minutes
  private static final int LONG_JOB_THRESHOLD = 10*60; // 10 minutes

  private static final ScheduledExecutorService pollWorkersPool = Executors.newScheduledThreadPool(WORKER_COUNT);

  private static final Map<String, JobPolling> jobPollers = new HashMap<String, JobPolling>();

  private JobResourceManager resourceManager = null;
  private PigJob job;
  private volatile ScheduledFuture<?> thisFuture;

  private JobPolling(JobResourceManager resourceManager, PigJob job) {
    this.resourceManager = resourceManager;
    this.job = job;
  }

  /**
   * Do polling
   */
  public void run() {
    try {
      // Hack to make permission check work. It is based on
      // context.getUsername(), but it doesn't work in another thread. See BUG-27093.
      resourceManager.ignorePermissions(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          LOG.debug("Polling job status " + job.getJobId() + " #" + job.getId());
          try {
            job = resourceManager.read(job.getId());
          } catch (ItemNotFound itemNotFound) {
            LOG.error("Job " + job.getId() + " does not exist! Polling canceled");
            thisFuture.cancel(false);
            return null;
          }
          resourceManager.retrieveJobStatus(job);

          Long time = System.currentTimeMillis() / 1000L;
          if (time - job.getDateStarted() > LONG_JOB_THRESHOLD) {
            LOG.debug("Job becomes long.. Rescheduling polling to longer period");
            // If job running longer than LONG_JOB_THRESHOLD, reschedule
            // it to poll every LONG_POLLING_DELAY instead of POLLING_DELAY
            thisFuture.cancel(false);
            scheduleJobPolling(true);
          }

          if (job.getStatus().equals(PigJob.PIG_JOB_STATE_SUBMIT_FAILED) ||
              job.getStatus().equals(PigJob.PIG_JOB_STATE_COMPLETED) ||
              job.getStatus().equals(PigJob.PIG_JOB_STATE_FAILED) ||
              job.getStatus().equals(PigJob.PIG_JOB_STATE_KILLED)) {
            LOG.debug("Job finished. Polling canceled");
            thisFuture.cancel(false);

          } else {
          }
          return null;
        }
      });
    } catch (Exception e) {
      LOG.error("Exception during handling job polling: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void scheduleJobPolling(boolean longDelay) {
    if (!longDelay) {
      thisFuture = pollWorkersPool.scheduleWithFixedDelay(this,
          POLLING_DELAY, POLLING_DELAY, TimeUnit.SECONDS);
    } else {
      thisFuture = pollWorkersPool.scheduleWithFixedDelay(this,
          LONG_POLLING_DELAY, LONG_POLLING_DELAY, TimeUnit.SECONDS);
    }
  }

  private void scheduleJobPolling() {
    scheduleJobPolling(false);
  }

  /**
   * Schedule job polling
   * @param job job instance
   * @return returns false if already scheduled
   */
  public static boolean pollJob(JobResourceManager resourceManager, PigJob job) {
    if (jobPollers.get(job.getJobId()) == null) {
      LOG.debug("Setting up polling for " + job.getJobId());
      JobPolling polling = new JobPolling(resourceManager, job);
      polling.scheduleJobPolling();
      jobPollers.put(job.getJobId(), polling);
      return true;
    }
    return false;
  }
}
