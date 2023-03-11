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
import com.netease.arctic.optimizer.operator.BaseTaskReporter;
import com.netease.arctic.optimizer.operator.BaseToucher;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FlinkReporter extends AbstractStreamOperator<Void>
    implements OneInputStreamOperator<OptimizeTaskStat, Void> {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkReporter.class);
  public static final String STATE_JOB_ID = "flink-job-id";

  private final BaseTaskReporter taskReporter;
  private final BaseToucher toucher;
  private final long heartBeatInterval;
  private volatile boolean stopped = false;
  private Thread thread;

  public FlinkReporter(BaseTaskReporter taskReporter, BaseToucher toucher, OptimizerConfig optimizerConfig) {
    this.taskReporter = taskReporter;
    this.toucher = toucher;
    this.heartBeatInterval = optimizerConfig.getHeartBeat();
  }

  public FlinkReporter(OptimizerConfig config) {
    this.taskReporter = new BaseTaskReporter(config);
    this.toucher = new BaseToucher(config);
    this.heartBeatInterval = config.getHeartBeat();
  }

  @Override
  public void open() throws Exception {
    super.open();
    this.thread = new Thread(() -> {
      while (!stopped) {
        try {
          Thread.sleep(heartBeatInterval);
          Map<String, String> state = Maps.newHashMap();
          try {
            String jobId = getContainingTask().getEnvironment().getJobID().toString();
            state.put(STATE_JOB_ID, jobId);
          } catch (Exception e) {
            LOG.error("failed to get joId, ignore", e);
          }
          toucher.touch(state);
        } catch (InterruptedException t) {
          break;
        } catch (Throwable t) {
          LOG.error("toucher get unexpected exception", t);
          continue;
        }
      }
    });
    this.thread.start();
  }

  @Override
  public void close() throws Exception {
    super.close();
    stopped = true;
    if (thread != null) {
      thread.interrupt();
    }
  }

  @Override
  public void processElement(StreamRecord<OptimizeTaskStat> element) throws Exception {
    if (element.getValue() != null) {
      taskReporter.report(element.getValue());
      LOG.info("report success {}", element.getValue() == null ? null : element.getValue().getTaskId());
    } else {
      LOG.warn("get empty task stat");
    }
  }
}
