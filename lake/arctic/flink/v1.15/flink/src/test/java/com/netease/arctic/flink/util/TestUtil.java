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

package com.netease.arctic.flink.util;

import org.apache.flink.api.common.JobStatus;
import org.apache.flink.core.execution.JobClient;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtil {

  public static final Logger LOG = LoggerFactory.getLogger(TestUtil.class);

  /**
   * get ut method name without parameters.
   */
  public static String getUtMethodName(TestName testName) {
    int i = testName.getMethodName().indexOf("[");
    if (i == -1) {
      return testName.getMethodName();
    }
    return testName.getMethodName().substring(0, i);
  }
  
  public static void cancelJob(JobClient jobClient) {
    if (isJobTerminated(jobClient)) {
      return;
    }
    try {
      jobClient.cancel();
    } catch (Exception e) {
      LOG.warn("cancel job exception.", e);
    }
  }

  public static boolean isJobTerminated(JobClient jobClient) {
    try {
      JobStatus status = jobClient.getJobStatus().get();
      return status.isGloballyTerminalState();
    } catch (Exception e) {
      // TODO
      //  This is sort of hack.
      //  Currently different execution environment will have different behaviors
      //  when fetching a finished job status.
      //  For example, standalone session cluster will return a normal FINISHED,
      //  while mini cluster will throw IllegalStateException,
      //  and yarn per job will throw ApplicationNotFoundException.
      //  We have to assume that job has finished in this case.
      //  Change this when these behaviors are unified.
      LOG.warn(
          "Failed to get job status so we assume that the job has terminated. Some data might be lost.",
          e);
      return true;
    }
  }

}
