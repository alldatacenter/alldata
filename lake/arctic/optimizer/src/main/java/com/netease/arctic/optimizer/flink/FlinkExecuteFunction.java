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

import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.TaskWrapper;
import com.netease.arctic.optimizer.operator.BaseTaskExecutor;
import com.netease.arctic.optimizer.util.CircularArray;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public class FlinkExecuteFunction extends ProcessFunction<TaskWrapper, OptimizeTaskStat>
    implements BaseTaskExecutor.ExecuteListener {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkExecuteFunction.class);
  private static final String INFLUXDB_TAG_NAME = "arctic_task_id";

  private final BaseTaskExecutor executor;
  private final OptimizerConfig config;

  private final CircularArray<TaskStat> latestTaskStats = new CircularArray<>(10);
  private final ArrayBlockingQueue<TaskStat> completedTasks = new ArrayBlockingQueue<>(256);
  private volatile TaskStat currentTaskStat;

  private Meter inputFlowRateMeter;
  private Meter outputFlowRateMeter;
  private Meter inputFileCntMeter;
  private Meter outputFileCntMeter;

  private volatile long lastUsageCheckTime = 0;

  FlinkExecuteFunction(OptimizerConfig config) {
    this.config = config;
    this.executor = new BaseTaskExecutor(config, this);
  }

  public FlinkExecuteFunction(BaseTaskExecutor executor,
                              OptimizerConfig config) {
    this.executor = executor;
    this.config = config;
  }

  @Override
  public void processElement(TaskWrapper compactTaskTaskWrapper,
                             ProcessFunction<TaskWrapper, OptimizeTaskStat>.Context context,
                             Collector<OptimizeTaskStat> collector) throws Exception {
    OptimizeTaskStat result = executor.execute(compactTaskTaskWrapper);
    collector.collect(result);
  }

  @Override
  public void onTaskStart(Iterable<ContentFile<?>> inputFiles) {
    this.currentTaskStat = new TaskStat();
    this.currentTaskStat.recordInputFiles(inputFiles);
    int size = Iterables.size(inputFiles);
    long sum = 0;
    for (ContentFile<?> inputFile : inputFiles) {
      sum += inputFile.fileSizeInBytes();
    }
    // file cnt rate /min
    this.inputFileCntMeter.markEvent(size * 60L);
    this.inputFlowRateMeter.markEvent(sum);
    LOG.info("record metrics inputFlowRate={}, InputFileCnt={}", sum, size);
  }

  @Override
  public void onTaskFinish(Iterable<ContentFile<?>> outputFiles) {
    this.currentTaskStat.recordOutFiles(outputFiles);
    this.currentTaskStat.finish();
    this.latestTaskStats.add(currentTaskStat);
    TaskStat taskStat = this.currentTaskStat;
    this.currentTaskStat = null;
    try {
      this.completedTasks.add(taskStat);
    } catch (IllegalStateException e) {
      LOG.warn("completed queue may be full, poll the first one and retry add", e);
      this.completedTasks.poll();
      this.completedTasks.add(taskStat);
    }

    int size = 0;
    long sum = 0;
    if (outputFiles != null) {
      size = Iterables.size(outputFiles);
      for (ContentFile<?> outputFile : outputFiles) {
        sum += outputFile.fileSizeInBytes();
      }
    }
    // file cnt rate /min
    this.outputFileCntMeter.markEvent(size * 60L);
    this.outputFlowRateMeter.markEvent(sum);
    LOG.info("record metrics outputFlowRate={}, outputFileCnt={}", sum, size);
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    ExecutionConfig.GlobalJobParameters globalJobParameters =
        getRuntimeContext().getExecutionConfig().getGlobalJobParameters();

    String taskId = Objects.nonNull(globalJobParameters.toMap().get(INFLUXDB_TAG_NAME)) ?
        globalJobParameters.toMap().get(INFLUXDB_TAG_NAME) : config.getOptimizerId();
    getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .gauge("last-5-input-file-size", () -> getLastNFileSize(5, true));
    LOG.info("add Gauge metrics last-5-input-file-size");

    getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .gauge("last-10-input-file-size", () -> getLastNFileSize(10, true));
    LOG.info("add Gauge metrics last-10-input-file-size");

    getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .gauge("last-5-output-file-size", () -> getLastNFileSize(5, false));
    LOG.info("add Gauge metrics last-5-output-file-size");

    getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .gauge("last-10-output-file-size", () -> getLastNFileSize(10, false));
    LOG.info("add Gauge metrics last-10-output-file-size");

    getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .gauge("last-5-average-time", () -> getLastNAverageTime(5));
    LOG.info("add Gauge metrics last-5-average-time");

    getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .gauge("last-10-average-time", () -> getLastNAverageTime(10));
    LOG.info("add Gauge metrics last-10-average-time");

    getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .gauge("usage-percentage", this::getUsagePercentage);
    LOG.info("add Gauge metrics usage-percentage");

    final int flowRateTimeSpanInSeconds = 5;
    this.inputFlowRateMeter = getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .meter("input-flow-rate", new MeterView(flowRateTimeSpanInSeconds));
    LOG.info("add Meter metrics input-flow-rate with timeSpanInSeconds = {}s", flowRateTimeSpanInSeconds);

    this.outputFlowRateMeter = getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .meter("output-flow-rate", new MeterView(flowRateTimeSpanInSeconds));
    LOG.info("add Meter metrics output-flow-rate with timeSpanInSeconds = {}s", flowRateTimeSpanInSeconds);

    final int fileCntRateTimeSpanInSeconds = 5;
    this.inputFileCntMeter = getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .meter("input-file-cnt-rate", new MeterView(fileCntRateTimeSpanInSeconds));
    LOG.info("add Meter metrics input-file-cnt-rate with timeSpanInSeconds = {}s", fileCntRateTimeSpanInSeconds);

    this.outputFileCntMeter = getRuntimeContext()
        .getMetricGroup()
        .addGroup(INFLUXDB_TAG_NAME, taskId)
        .meter("output-file-cnt-rate", new MeterView(fileCntRateTimeSpanInSeconds));
    LOG.info("add Meter metrics output-file-cnt-rate with timeSpanInSeconds = {}s", fileCntRateTimeSpanInSeconds);
  }

  private long getLastNFileSize(int n, boolean input) {
    if (n <= 0) {
      return 0;
    }
    int cnt = 0;
    int fileCnt = 0;
    long totalFileSize = 0;
    for (TaskStat taskStat : latestTaskStats) {
      if (taskStat == null) {
        break;
      }
      if (input) {
        fileCnt += taskStat.getInputFileCnt();
        totalFileSize += taskStat.getInputTotalSize();
      } else {
        fileCnt += taskStat.getOutputFileCnt();
        totalFileSize += taskStat.getOutputTotalSize();
      }
      cnt++;
      if (cnt == n) {
        break;
      }
    }
    return fileCnt == 0 ? 0 : totalFileSize / fileCnt;
  }

  private long getLastNAverageTime(int n) {
    if (n <= 0) {
      return 0;
    }
    int cnt = 0;
    int taskCnt = 0;
    long totalTime = 0;
    for (TaskStat taskStat : latestTaskStats) {
      if (taskStat == null) {
        break;
      }
      totalTime += taskStat.getDuration();
      taskCnt++;
      cnt++;
      if (cnt == n) {
        break;
      }
    }
    return taskCnt == 0 ? 0 : totalTime / taskCnt;
  }

  private double getUsagePercentage() {
    if (lastUsageCheckTime == 0) {
      this.lastUsageCheckTime = System.currentTimeMillis();
      LOG.info("get usage = 0.0, init lastUsageCheckTime");
      return 0.0;
    }
    long duration = 0;
    // get completed tasks execute duration
    while (true) {
      TaskStat task = completedTasks.poll();
      long taskDuration;
      if (task == null) {
        // no task in queue
        break;
      }
      if (task.finished()) {
        if (task.getEndTime() < lastUsageCheckTime) {
          // case1: end before lastUsageCheckTime, ignore
          taskDuration = 0;
        } else {
          if (task.getStartTime() < lastUsageCheckTime) {
            // case2: start before lastUsageCheckTime, calculate duration from lastUsageCheckTime
            taskDuration = task.getEndTime() - lastUsageCheckTime;
          } else {
            // case3: start after lastUsageCheckTime, get total duration by end - start
            taskDuration = task.getDuration();
          }
        }
      } else {
        LOG.warn("should not get not finished task, ignore {}", task);
        taskDuration = 0;
      }
      duration += taskDuration;
    }
    // get current task execute duration
    TaskStat current = this.currentTaskStat;
    long now = System.currentTimeMillis();
    if (current != null) {
      if (current.getStartTime() < lastUsageCheckTime) {
        duration += (now - lastUsageCheckTime);
      } else {
        duration += (now - current.getStartTime());
      }
    }
    long totalDuration = now - lastUsageCheckTime;
    this.lastUsageCheckTime = now;
    double usage;
    if (duration > totalDuration) {
      LOG.warn("duration {} is bigger than total duration {}", duration, totalDuration);
      usage = 1.0;
    } else if (duration == totalDuration) {
      usage = 1.0;
    } else {
      usage = (double) duration / totalDuration;
    }
    LOG.info("get usage = {}%, execute duration = {}, totalDuration = {}", usage * 100, duration, totalDuration);
    return usage * 100;
  }
}