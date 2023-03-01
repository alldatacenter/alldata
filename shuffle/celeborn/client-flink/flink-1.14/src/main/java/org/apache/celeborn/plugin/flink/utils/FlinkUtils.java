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

import java.util.Map;

import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;

import org.apache.celeborn.common.CelebornConf;

public class FlinkUtils {

  public static CelebornConf toCelebornConf(Configuration configuration) {
    CelebornConf tmpCelebornConf = new CelebornConf();
    Map<String, String> confMap = configuration.toMap();
    for (Map.Entry<String, String> entry : confMap.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("celeborn.")) {
        tmpCelebornConf.set(entry.getKey(), entry.getValue());
      }
    }

    return tmpCelebornConf;
  }

  public static String toCelebornAppId(JobID jobID) {
    return jobID.toString();
  }

  public static String toShuffleId(JobID jobID, IntermediateDataSetID dataSetID) {
    return jobID.toString() + "-" + dataSetID.toString();
  }

  public static String toAttemptId(ExecutionAttemptID attemptID) {
    return attemptID.toString();
  }
}
