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

package com.netease.arctic.optimizer.operator;

import com.netease.arctic.ams.api.JobId;
import com.netease.arctic.ams.api.JobType;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OptimizeManager;
import com.netease.arctic.ams.api.OptimizeTask;
import com.netease.arctic.ams.api.client.OptimizeManagerClientPools;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.TaskWrapper;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Consume task from Ams.
 */
public class BaseTaskConsumer implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(BaseTaskConsumer.class);

  private static final long DEFAULT_POLL_WAIT_TIMEOUT = 10000; // 10s

  private final OptimizerConfig config;
  private final JobId jobId;

  public BaseTaskConsumer(OptimizerConfig config) {
    this.config = config;
    this.jobId = new JobId();
    this.jobId.setId(config.getOptimizerId());
    this.jobId.setType(JobType.Optimize);
  }

  /**
   * poll task from Ams.
   *
   * @return - return null if got no task
   */
  public TaskWrapper pollTask() throws TException {
    return pollTask(DEFAULT_POLL_WAIT_TIMEOUT);
  }

  /**
   * poll task from Ams with timeout.
   *
   * @return - return null if got no task
   */
  public TaskWrapper pollTask(long timeout) throws TException {
    int attemptId = Math.abs(ThreadLocalRandom.current().nextInt());
    OptimizeTask task = pollTask(attemptId, timeout);
    return task == null ? null : new TaskWrapper(task, attemptId);
  }

  private OptimizeTask pollTask(int attemptId, long timeout) throws TException {
    try {
      OptimizeManager.Iface optimizeManager = OptimizeManagerClientPools.getClient(config.getAmsUrl());
      return optimizeManager.pollTask(config.getQueueId(), jobId, attemptId + "", timeout);
    } catch (NoSuchObjectException e) {
      return null;
    }
  }
}
