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

package com.netease.arctic.optimizer.flink;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.ams.api.properties.OptimizerProperties;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.StatefulOptimizer;
import com.netease.arctic.optimizer.operator.BaseToucher;
import com.netease.arctic.optimizer.util.OptimizerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.netease.arctic.optimizer.flink.FlinkReporter.STATE_JOB_ID;

public class FlinkOptimizer implements StatefulOptimizer {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkOptimizer.class);
  private static final String JOB_NAME = "arctic-major-optimizer-job";
  private static final String FLINK_HOME = "FLINK_HOME";
  private static final String HADOOP_CONF_DIR = "HADOOP_CONF_DIR";
  private static final String YARN_CONF_DIR = "YARN_CONF_DIR";
  private static final String HADOOP_USER_NAME = "HADOOP_USER_NAME";
  private static final String JVM_ARGS = "JVM_ARGS";
  private static final String FLINK_CONF_DIR = "FLINK_CONF_DIR";
  public static final String YARN_APP_ID = "yarn-app-id";
  private static final long serialVersionUID = 1L;

  private Map<String, String> properties;

  public FlinkOptimizer(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public void start() throws IOException {
    JSONObject systemInfo = JSONObject.parseObject(properties.get(OptimizerProperties.AMS_SYSTEM_INFO));
    JSONObject containerInfo = JSONObject.parseObject(properties.get(OptimizerProperties.CONTAINER_INFO));
    JSONObject groupInfo = JSONObject.parseObject(properties.get(OptimizerProperties.OPTIMIZER_GROUP_INFO));
    JSONObject jobInfo = JSONObject.parseObject(properties.get(OptimizerProperties.OPTIMIZER_JOB_INFO));
    JSONObject containerProperties = containerInfo.getJSONObject(OptimizerProperties.CONTAINER_PROPERTIES);
    JSONObject groupProperties = groupInfo.getJSONObject(OptimizerProperties.OPTIMIZER_GROUP_PROPERTIES);

    String flinkHome = containerProperties.getString(FLINK_HOME);
    if (StringUtils.isEmpty(flinkHome)) {
      throw new IllegalArgumentException("system environment variable FLINK_HOME can not be empty, please set " +
          "flink install home like '/opt/flink'");
    }
    String cmd = flinkHome + "/bin/flink run -m yarn-cluster ";

    // add flink executor config
    int tmMemory = groupProperties.getInteger("taskmanager.memory");
    cmd += " -ytm " + tmMemory;
    int jmMemory = groupProperties.getInteger("jobmanager.memory");
    cmd += " -yjm " + jmMemory;

    // spill map config
    Boolean enableSpillMap = groupProperties.getBoolean(OptimizerProperties.SPILLABLE_MAP_ENABLE);
    String backendBaseDir = groupProperties.getString(OptimizerProperties.SPILLABLE_MAP_DIR);
    Long maxDeleteMemorySize = groupProperties.getLong(OptimizerProperties.SPILLABLE_MEMORY_LIMIT);
    String spillMapCmd = "";
    if (enableSpillMap != null) {
      spillMapCmd = spillMapCmd + " -es " + enableSpillMap;
    }
    if (backendBaseDir != null) {
      spillMapCmd = spillMapCmd + " -rp " + backendBaseDir;
    }
    if (maxDeleteMemorySize != null) {
      spillMapCmd = spillMapCmd + " -mm " + maxDeleteMemorySize;
    }

    // add compact execute config
    String arcticHome = systemInfo.getString(OptimizerProperties.ARCTIC_HOME);
    String jarPath = " " + arcticHome + "/plugin/optimize/OptimizeJob.jar ";
    String entryClass = this.getClass().getName();
    cmd += " -c " + entryClass + jarPath;
    String amsUrl;
    if (systemInfo.containsKey(OptimizerProperties.HA_ENABLE) && systemInfo.getBoolean(OptimizerProperties.HA_ENABLE)) {
      amsUrl = String.format("zookeeper://%s/%s", systemInfo.getString(OptimizerProperties.ZOOKEEPER_SERVER).trim(),
          systemInfo.getString(OptimizerProperties.CLUSTER_NAME)).trim();
    } else {
      amsUrl =
          "thrift://" + systemInfo.getString(OptimizerProperties.THRIFT_BIND_HOST).trim() + ":" +
              systemInfo.getString(OptimizerProperties.THRIFT_BIND_PORT).trim();
    }
    int parallelism = jobInfo.getInteger(OptimizerProperties.OPTIMIZER_JOB_PARALLELISM);
    long heartBeatInterval = groupProperties.containsKey(OptimizerProperties.OPTIMIZER_GROUP_HEART_BEAT_INTERVAL) ?
        groupProperties.getLong(OptimizerProperties.OPTIMIZER_GROUP_HEART_BEAT_INTERVAL) :
        OptimizerProperties.OPTIMIZER_GROUP_HEART_BEAT_INTERVAL_DEFAULT;
    cmd +=
        " -a " + amsUrl + " -q " + groupInfo.get("id") + " -p " + parallelism + " --heart-beat " + heartBeatInterval +
            " -id " + jobInfo.get(OptimizerProperties.OPTIMIZER_JOB_ID) + spillMapCmd;

    String envCmd = "";
    if (containerProperties.containsKey(HADOOP_CONF_DIR)) {
      envCmd += "export HADOOP_CONF_DIR=" + containerProperties.get(HADOOP_CONF_DIR) + " && ";
    }
    if (containerProperties.containsKey(HADOOP_USER_NAME)) {
      envCmd += "export HADOOP_USER_NAME=" + containerProperties.get(HADOOP_USER_NAME) + " && ";
    }
    if (containerProperties.containsKey(JVM_ARGS)) {
      envCmd += "export JVM_ARGS=" + containerProperties.get(JVM_ARGS) + " && ";
    }
    if (containerProperties.containsKey(FLINK_CONF_DIR)) {
      envCmd += "export FLINK_CONF_DIR=" + containerProperties.get(FLINK_CONF_DIR) + " && ";
    }
    if (containerProperties.containsKey(YARN_CONF_DIR)) {
      envCmd += "export YARN_CONF_DIR=" + containerProperties.get(YARN_CONF_DIR) + " && ";
    }
    String finalCmd = envCmd + cmd;
    new Thread(() -> {
      try {
        // start compact job
        LOG.info("starting compact job use command:" + finalCmd);
        String[] commends = {"/bin/sh", "-c", finalCmd};
        Runtime runtime = Runtime.getRuntime();
        Process exec = null;
        try {
          exec = runtime.exec(commends);
        } catch (IOException e) {
          e.printStackTrace();
        }
        StringBuffer sb = new StringBuffer();
        InputStream inputStream = exec.getInputStream();
        InputStreamReader buInputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(buInputStreamReader);
        Pattern pattern = Pattern.compile("(.*)application_(\\d+)_(\\d+)");
        String str = null;
        boolean isTouched = false;
        long start = System.currentTimeMillis();
        while (true) {
          try {
            if ((str = bufferedReader.readLine()) == null) {
              break;
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          if ((System.currentTimeMillis() - start) > 600000) {
            LOG.error("FlinkOptimizer start:" + sb);
            return;
          }
          if (!isTouched) {
            Matcher m = pattern.matcher(str);
            if (m.matches()) {
              String yarnApp = String.format("application_%s_%s", m.group(2), m.group(3));
              LOG.info("YARN_APP_ID" + yarnApp);
              Map<String, String> launcherInfo = new HashMap<>();
              launcherInfo.put(YARN_APP_ID, yarnApp);
              OptimizerConfig config = new OptimizerConfig();
              config.setOptimizerId(jobInfo.getString(OptimizerProperties.OPTIMIZER_JOB_ID));
              config.setAmsUrl(amsUrl);
              config.setHeartBeat(heartBeatInterval);
              config.setExecutorParallel(parallelism);
              config.setQueueId(groupInfo.getInteger("id"));
              BaseToucher toucher = new BaseToucher(config);
              toucher.touch(launcherInfo);
              isTouched = true;
            }
          }
          sb.append(str);
          sb.append("\n");
        }
        LOG.info("start flink optimizer log:" + sb);
      } catch (Exception e) {
        LOG.error("FlinkOptimizer start", e);
      }
    }).start();
  }

  @Override
  public void stop() throws IOException {
    JSONObject jobInfo = JSONObject.parseObject(properties.get(OptimizerProperties.OPTIMIZER_JOB_INFO));
    if (!properties.containsKey(OptimizerProperties.OPTIMIZER_LAUNCHER_INFO)) {
      LOG.warn("there is no launcher report for optimizer: " + jobInfo.get(OptimizerProperties.OPTIMIZER_JOB_ID));
      return;
    }
    JSONObject containerInfo = JSONObject.parseObject(properties.get(OptimizerProperties.CONTAINER_INFO));
    Map<String, String> containerProperties = containerInfo.getObject("properties", Map.class);
    String flinkHome = containerProperties.get(FLINK_HOME);
    if (StringUtils.isEmpty(flinkHome)) {
      throw new IllegalArgumentException("system environment variable FLINK_HOME can not be empty, please set " +
          "flink install home like '/opt/flink'");
    }
    JSONObject launcherInfo = JSONObject.parseObject(properties.get(OptimizerProperties.OPTIMIZER_LAUNCHER_INFO));
    if (!launcherInfo.containsKey(STATE_JOB_ID)) {
      LOG.warn("there is no real job running for optimizer: " + jobInfo.get(OptimizerProperties.OPTIMIZER_JOB_ID));
      return;
    }
    String cmd = String.format(
        "%s/bin/flink cancel -t yarn-per-job -Dyarn.application.id=%s %s",
        flinkHome,
        launcherInfo.getString(YARN_APP_ID),
        launcherInfo.getString(STATE_JOB_ID));
    //stop compact job
    LOG.info("stop compact job use command:" + cmd);
    Runtime runtime = Runtime.getRuntime();
    String envCmd = "";
    try {
      if (containerProperties.containsKey(HADOOP_CONF_DIR)) {
        envCmd += "export HADOOP_CONF_DIR=" + containerProperties.get(HADOOP_CONF_DIR) + " && ";
      }
      String[] finalCmd = {"/bin/sh", "-c", envCmd + cmd};
      runtime.exec(finalCmd);
    } catch (Exception e) {
      LOG.error("FlinkOptimizer stop", e);
    }
  }

  public static void main(String[] args) throws CmdLineException {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(new Configuration());
    OptimizerConfig optimizerConfig = new OptimizerConfig(args);

    if (optimizerConfig.getOptimizerId() == null || optimizerConfig.getOptimizerId().isEmpty() ||
        "unknown".equals(optimizerConfig.getOptimizerId())) {
      OptimizerUtil.register(optimizerConfig);
    }

    env.addSource(new FlinkConsumer(optimizerConfig))
        .setParallelism(optimizerConfig.getExecutorParallel())
        .process(new FlinkExecuteFunction(optimizerConfig))
        .setParallelism(optimizerConfig.getExecutorParallel())
        .name(FlinkExecuteFunction.class.getName())
        .transform(FlinkReporter.class.getName(), Types.VOID, new FlinkReporter(optimizerConfig))
        .setParallelism(1)
        .addSink(new DiscardingSink<>())
        .name("Optimizer sink")
        .setParallelism(1);

    tryExecute(env);
  }

  private static void tryExecute(StreamExecutionEnvironment env) {
    try {
      env.execute(JOB_NAME);
    } catch (Exception e) {
      LOG.error("An error occurred during job start ", e);
    }
  }

  @Override
  public Map<String, String> getState() {
    return properties;
  }

  @Override
  public void updateState(Map<String, String> state) {
    this.properties.putAll(state);
  }
}
