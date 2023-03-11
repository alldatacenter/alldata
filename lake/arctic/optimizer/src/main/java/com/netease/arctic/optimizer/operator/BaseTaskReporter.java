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

import com.netease.arctic.ams.api.OptimizeManager;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.api.client.OptimizeManagerClientPools;
import com.netease.arctic.optimizer.OptimizerConfig;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Report task execute result.
 */
public class BaseTaskReporter implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(BaseTaskReporter.class);

  private static final int DEFAULT_RETRY = 20;
  private static final int DEFAULT_RETRY_INTERVAL = 15000;

  private final OptimizerConfig config;

  public BaseTaskReporter(OptimizerConfig config) {
    this.config = config;
  }

  /**
   * Report task execute result.
   *
   * @param optimizeTaskStat -
   * @return - true if success
   */
  public boolean report(OptimizeTaskStat optimizeTaskStat) throws InterruptedException {
    return report(optimizeTaskStat, DEFAULT_RETRY, DEFAULT_RETRY_INTERVAL);
  }

  /**
   * Report task execute result with retry.
   *
   * @param optimizeTaskStat -
   * @return - true if success
   */
  public boolean report(OptimizeTaskStat optimizeTaskStat, int maxRetry, long retryInterval)
      throws InterruptedException {
    int retry = 0;
    while (true) {
      try {
        reportResult(config.getAmsUrl(), optimizeTaskStat);
        return true;
      } catch (Throwable t) {
        LOG.error("failed to sending result, task: {}, ", optimizeTaskStat.getTaskId(), t);
        if (retry++ < maxRetry) {
          Thread.sleep(retryInterval);
        } else {
          return false;
        }
      }
    }
  }

  private static void reportResult(String thriftUrl, OptimizeTaskStat optimizeTaskStat)
      throws TException {
    LOG.info("start reporting result: {}", printOptimizeTaskStat(optimizeTaskStat));
    try {
      OptimizeManager.Iface compactManager = OptimizeManagerClientPools.getClient(thriftUrl);
      compactManager.reportOptimizeResult(optimizeTaskStat);
    } catch (Throwable t) {
      LOG.error("failed to sending result, task: {} ", optimizeTaskStat.getTaskId(), t);
      throw t;
    }
  }

  public static String printOptimizeTaskStat(OptimizeTaskStat optimizeTaskStat) {
    if (optimizeTaskStat == null) {
      return null;
    }
    return String.format("%s, table=%s, status=%s, attemptId=%s, newFileSize=%d, reportTime=%d, costTime=%d",
        optimizeTaskStat.getTaskId(), optimizeTaskStat.getTableIdentifier(), optimizeTaskStat.getStatus(),
        optimizeTaskStat.getAttemptId(), optimizeTaskStat.getNewFileSize(), optimizeTaskStat.getReportTime(),
        optimizeTaskStat.getCostTime());
  }
}
