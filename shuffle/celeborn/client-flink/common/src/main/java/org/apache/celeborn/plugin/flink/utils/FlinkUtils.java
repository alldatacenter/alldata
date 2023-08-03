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

package org.apache.celeborn.plugin.flink.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;

import org.apache.celeborn.common.CelebornConf;

public class FlinkUtils {
  private static final JobID ZERO_JOB_ID = new JobID(0, 0);
  public static final Set<String> pluginConfNames =
      new HashSet<String>() {
        {
          add("remote-shuffle.job.min.memory-per-partition");
          add("remote-shuffle.job.min.memory-per-gate");
          add("remote-shuffle.job.concurrent-readings-per-gate");
          add("remote-shuffle.job.memory-per-partition");
          add("remote-shuffle.job.memory-per-gate");
          add("remote-shuffle.job.support-floating-buffer-per-input-gate");
          add("remote-shuffle.job.enable-data-compression");
          add("remote-shuffle.job.support-floating-buffer-per-output-gate");
          add("remote-shuffle.job.compression.codec");
        }
      };

  public static CelebornConf toCelebornConf(Configuration configuration) {
    CelebornConf tmpCelebornConf = new CelebornConf();
    Map<String, String> confMap = configuration.toMap();
    for (Map.Entry<String, String> entry : confMap.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("celeborn.") || pluginConfNames.contains(key)) {
        tmpCelebornConf.set(key, entry.getValue());
      }
    }

    return tmpCelebornConf;
  }

  public static String toCelebornAppId(long lifecycleManagerTimestamp, JobID jobID) {
    // Workaround for FLINK-19358, use first none ZERO_JOB_ID as celeborn shared appId for all
    // other flink jobs
    if (!ZERO_JOB_ID.equals(jobID)) {
      return lifecycleManagerTimestamp + "-" + jobID.toString();
    }

    return lifecycleManagerTimestamp + "-" + JobID.generate();
  }

  public static String toShuffleId(JobID jobID, IntermediateDataSetID dataSetID) {
    return jobID.toString() + "-" + dataSetID.toString();
  }

  public static String toAttemptId(ExecutionAttemptID attemptID) {
    return attemptID.toString();
  }
}
