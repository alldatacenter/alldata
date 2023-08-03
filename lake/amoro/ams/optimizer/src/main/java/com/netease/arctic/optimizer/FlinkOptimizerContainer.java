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

package com.netease.arctic.optimizer;

import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.resource.Resource;
import com.netease.arctic.optimizer.flink.FlinkOptimizer;
import com.netease.arctic.optimizer.util.PropertyUtil;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlinkOptimizerContainer extends AbstractResourceContainer {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkOptimizerContainer.class);

  public static final String FLINK_HOME_PROPERTY = "flink-home";
  public static final String TASK_MANAGER_MEMORY_PROPERTY = "taskmanager.memory";
  public static final String JOB_MANAGER_MEMORY_PROPERTY = "jobmanager.memory";
  public static final String YARN_APPLICATION_ID_PROPERTY = "yarn-application-id";

  private static final Pattern APPLICATION_ID_PATTERN = Pattern.compile("(.*)application_(\\d+)_(\\d+)");
  private static final int MAX_READ_APP_ID_TIME = 600000; //10 min


  public String getFlinkHome() {
    return PropertyUtil.checkAndGetProperty(getContainerProperties(), FLINK_HOME_PROPERTY);
  }

  @Override
  protected Map<String, String> doScaleOut(String startUpArgs) {
    Runtime runtime = Runtime.getRuntime();
    try {
      String exportCmd = String.join(" && ", exportSystemProperties());
      String startUpCmd = String.format("%s && %s", exportCmd, startUpArgs);
      String[] cmd = {"/bin/sh", "-c", startUpCmd};
      LOG.info("Starting flink optimizer using command:" + startUpCmd);
      Process exec = runtime.exec(cmd);
      Map<String, String> startUpStatesMap = Maps.newHashMap();
      String applicationId = readApplicationId(exec);
      if (applicationId != null) {
        startUpStatesMap.put(YARN_APPLICATION_ID_PROPERTY, applicationId);
      }
      return startUpStatesMap;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to scale out flink optimizer.", e);
    }
  }

  @Override
  protected String buildOptimizerStartupArgsString(Resource resource) {
    String taskManagerMemory = PropertyUtil.checkAndGetProperty(resource.getProperties(),
        TASK_MANAGER_MEMORY_PROPERTY);
    String jobManagerMemory = PropertyUtil.checkAndGetProperty(resource.getProperties(), JOB_MANAGER_MEMORY_PROPERTY);
    String jobPath = getAMSHome() + "/plugin/optimize/OptimizeJob.jar";
    long memory = Long.parseLong(jobManagerMemory) + Long.parseLong(taskManagerMemory) * resource.getThreadCount();
    return String.format("%s/bin/flink run -m yarn-cluster -ytm %s -yjm %s -c %s %s -m %s %s",
        getFlinkHome(), taskManagerMemory, jobManagerMemory,
        FlinkOptimizer.class.getName(), jobPath, memory,
        super.buildOptimizerStartupArgsString(resource));
  }

  private String readApplicationId(Process exec) {
    StringBuilder outputBuilder = new StringBuilder();
    String applicationId = null;
    try (InputStreamReader inputStreamReader = new InputStreamReader(exec.getInputStream())) {
      try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MAX_READ_APP_ID_TIME) {
          String readLine = bufferedReader.readLine();
          outputBuilder.append(readLine).append("\n");
          Matcher matcher = APPLICATION_ID_PATTERN.matcher(readLine);
          if (matcher.matches()) {
            applicationId = String.format("application_%s_%s", matcher.group(2), matcher.group(3));
            break;
          }
        }
        LOG.info("Started flink optimizer log:\n{}", outputBuilder);
        return applicationId;
      }
    } catch (IOException e) {
      LOG.error("Read application id from output failed", e);
      return null;
    }
  }

  @Override
  public void releaseOptimizer(Resource resource) {
    Preconditions.checkArgument(resource.getProperties().containsKey(YARN_APPLICATION_ID_PROPERTY),
        "Cannot find {} from optimizer start up stats", YARN_APPLICATION_ID_PROPERTY);
    Preconditions.checkArgument(resource.getProperties().containsKey(FlinkOptimizer.JOB_ID_PROPERTY),
        "Cannot find {} from optimizer properties", FlinkOptimizer.JOB_ID_PROPERTY);
    String applicationId = resource.getProperties().get(YARN_APPLICATION_ID_PROPERTY);
    String jobId = resource.getProperties().get(FlinkOptimizer.JOB_ID_PROPERTY);
    try {
      String exportCmd = String.join(" && ", exportSystemProperties());
      String releaseCmd = String.format("%s && %s/bin/flink cancel -t yarn-per-job -Dyarn.application.id=%s %s",
          exportCmd, getFlinkHome(), applicationId, jobId);
      String[] cmd = {"/bin/sh", "-c", releaseCmd};
      LOG.info("Releasing flink optimizer using command:" + releaseCmd);
      Runtime.getRuntime().exec(cmd);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to release flink optimizer.", e);
    }
  }
}
