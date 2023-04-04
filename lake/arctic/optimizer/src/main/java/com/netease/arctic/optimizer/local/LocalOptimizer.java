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

package com.netease.arctic.optimizer.local;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.api.properties.OptimizerProperties;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.StatefulOptimizer;
import com.netease.arctic.optimizer.TaskWrapper;
import com.netease.arctic.optimizer.operator.BaseTaskConsumer;
import com.netease.arctic.optimizer.operator.BaseTaskExecutor;
import com.netease.arctic.optimizer.operator.BaseTaskReporter;
import com.netease.arctic.optimizer.operator.BaseToucher;
import com.netease.arctic.optimizer.operator.DefaultOperatorFactory;
import com.netease.arctic.optimizer.operator.OperatorFactory;
import com.netease.arctic.optimizer.util.OptimizerUtil;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * An optimizer running locally.
 */
public class LocalOptimizer implements StatefulOptimizer {
  private static final Logger LOG = LoggerFactory.getLogger(LocalOptimizer.class);
  private static final long serialVersionUID = 1L;
  private static final long POLL_INTERVAL = 5000; // 5s
  
  private final OperatorFactory operatorFactory;

  private OptimizerConfig config;

  private Semaphore pollTaskSemaphore;

  private Consumer consumer;

  private Map<String, String> properties;

  private static final String STATE_JOB_ID = "local-job-id";
  private static final String HADOOP_CONF_DIR = "HADOOP_CONF_DIR";
  private static final String HADOOP_USER_NAME = "HADOOP_USER_NAME";

  private ExecutorService executeThreadPool;

  private ScheduledExecutorService toucherService;

  private volatile boolean stopped = false;

  public LocalOptimizer() {
    this(new DefaultOperatorFactory());
  }

  public LocalOptimizer(OperatorFactory operatorFactory) {
    this.operatorFactory = operatorFactory;
  }

  public LocalOptimizer(Map<String, String> properties) {
    this();
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
    int memory = groupProperties.getInteger("memory");

    // start compact job
    String arcticHome = systemInfo.getString(OptimizerProperties.ARCTIC_HOME);
    String cmd = String.format("%s/bin/localOptimize.sh -m %s -a %s -q %s -p %s -hb %s -id %s",
        arcticHome, memory, amsUrl, groupInfo.get("id"),
        parallelism, heartBeatInterval, jobInfo.get(OptimizerProperties.OPTIMIZER_JOB_ID)) + spillMapCmd;
    LOG.info("starting compact job use command:" + cmd);
    Runtime runtime = Runtime.getRuntime();
    try {
      if (containerProperties != null && containerProperties.containsKey("hadoop_home")) {
        String[] tmpCmd = {"/bin/sh", "-c", "export HADOOP_HOME=" + containerProperties.getString("hadoop_home")};
        runtime.exec(tmpCmd);
      }
      if (containerProperties != null && containerProperties.containsKey("java_home")) {
        String[] tmpCmd = {"/bin/sh", "-c", "export JAVA_HOME=" + containerProperties.getString("java_home")};
        runtime.exec(tmpCmd);
      }
      if (containerProperties.containsKey(HADOOP_CONF_DIR)) {
        String[] tmpCmd = {"/bin/sh", "-c", "export HADOOP_CONF_DIR=" + containerProperties.get(HADOOP_CONF_DIR)};
        runtime.exec(tmpCmd);
      }
      if (containerProperties.containsKey(HADOOP_USER_NAME)) {
        String[] tmpCmd = {"/bin/sh", "-c", "export HADOOP_USER_NAME=" + containerProperties.get(HADOOP_USER_NAME)};
        runtime.exec(tmpCmd);
      }
      String[] finalCmd = {"/bin/sh", "-c", cmd};
      runtime.exec(finalCmd);
    } catch (Exception e) {
      LOG.error("LocalOptimizer start", e);
    }
  }

  @Override
  public void stop() throws IOException {
    JSONObject jobInfo = JSONObject.parseObject(properties.get(OptimizerProperties.OPTIMIZER_JOB_INFO));
    if (!properties.containsKey(OptimizerProperties.OPTIMIZER_LAUNCHER_INFO)) {
      LOG.warn("there is no launcher report for optimizer: " + jobInfo.get(OptimizerProperties.OPTIMIZER_JOB_ID));
      return;
    }
    JSONObject launcherInfo = JSONObject.parseObject(properties.get(OptimizerProperties.OPTIMIZER_LAUNCHER_INFO));
    if (!launcherInfo.containsKey(STATE_JOB_ID)) {
      LOG.warn("there is no real job running for optimizer: " + jobInfo.get(OptimizerProperties.OPTIMIZER_JOB_ID));
      return;
    }
    String cmd = "kill -9 " + launcherInfo.getString(STATE_JOB_ID);
    //stop compact job
    LOG.info("stop compact job use command:" + cmd);
    Runtime runtime = Runtime.getRuntime();
    try {
      String[] finalCmd = {"/bin/sh", "-c", cmd};
      runtime.exec(finalCmd);
    } catch (Exception e) {
      LOG.error("LocalOptimizer stop", e);
    }
  }

  public static void main(String[] args) throws CmdLineException {
    OptimizerConfig optimizerConfig = new OptimizerConfig(args);
    if (optimizerConfig.getOptimizerId() == null || optimizerConfig.getOptimizerId().isEmpty() ||
        "unknown".equals(optimizerConfig.getOptimizerId())) {
      OptimizerUtil.register(optimizerConfig);
    }
    new LocalOptimizer().init(optimizerConfig);
    LOG.info("init LocalOptimizer with {}", optimizerConfig);
  }

  public synchronized void init(OptimizerConfig config) {
    if (this.config != null) {
      throw new IllegalArgumentException("already init");
    }
    this.config = config;
    this.consumer = new Consumer();
    this.pollTaskSemaphore = new Semaphore(1);

    ThreadFactory executorFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Executor %d").build();
    executeThreadPool = Executors.newFixedThreadPool(config.getExecutorParallel(), executorFactory);

    ThreadFactory toucherFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Toucher %d").build();
    toucherService =
        Executors.newScheduledThreadPool(config.getExecutorParallel(), toucherFactory);

    toucherService.scheduleAtFixedRate(new Toucher(), 3000, config.getHeartBeat(), TimeUnit.MILLISECONDS);
    executeThreadPool.execute(new Executor());
  }

  public void release() {
    this.stopped = true;
    if (executeThreadPool != null) {
      executeThreadPool.shutdownNow();
    }
    if (toucherService != null) {
      toucherService.shutdownNow();
    }
  }

  @Override
  public Map<String, String> getState() {
    return properties;
  }

  @Override
  public void updateState(Map<String, String> state) {
    this.properties = state;
  }

  private class Consumer {

    private final BaseTaskConsumer baseTaskConsumer;

    public Consumer() {
      this.baseTaskConsumer = operatorFactory.buildTaskConsumer(config);
    }

    public TaskWrapper pollTask() throws InterruptedException {
      while (!stopped) {
        try {
          TaskWrapper task = baseTaskConsumer.pollTask(0);
          if (task != null) {
            LOG.info("poll task {}", task);
            return task;
          } else {
            LOG.info("poll no task and wait for {} ms", POLL_INTERVAL);
            Thread.sleep(POLL_INTERVAL);
          }
        } catch (Throwable e) {
          if (stopped) {
            break;
          }
          LOG.error("failed to poll task", e);
          Thread.sleep(POLL_INTERVAL);
        }
      }
      return null;
    }
  }

  private class Executor implements Runnable {

    private final BaseTaskExecutor baseTaskExecutor;

    private final BaseTaskReporter baseTaskReporter;

    public Executor() {
      this.baseTaskExecutor = operatorFactory.buildTaskExecutor(config);
      this.baseTaskReporter = operatorFactory.buildTaskReporter(config);
    }

    @Override
    public void run() {
      while (!stopped) {
        try {
          TaskWrapper task;
          pollTaskSemaphore.acquire();
          try {
            task = consumer.pollTask();
            if (task == null) {
              continue;
            }
          } finally {
            pollTaskSemaphore.release();
          }
          LOG.info("get task to execute {}", task.getTask().getTaskId());
          OptimizeTaskStat result = baseTaskExecutor.execute(task);
          LOG.info("execute {} {}", result.getStatus(), task.getTask().getTaskId());
          baseTaskReporter.report(result);
          LOG.info("report success {}", result.getTaskId());
        } catch (InterruptedException e) {
          LOG.warn("execute interrupted");
          break;
        } catch (Throwable t) {
          LOG.error("execute error, ignore", t);
        }
      }
      LOG.info("execute thread exit");
    }
  }

  private class Toucher implements Runnable {

    private final BaseToucher toucher;

    public Toucher() {
      this.toucher = operatorFactory.buildToucher(config);
    }

    @Override
    public void run() {
      LOG.info("touching");
      RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
      String processId = runtimeMXBean.getName().split("@")[0];
      Map<String, String> state = new HashMap<>();
      state.put(STATE_JOB_ID, processId);
      boolean success = toucher.touch(state);
      LOG.info("touch {}", success ? "success" : "failed");
    }
  }
}
