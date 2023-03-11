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

import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.TaskWrapper;
import com.netease.arctic.optimizer.operator.BaseTaskConsumer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlinkConsumer extends RichParallelSourceFunction<TaskWrapper> {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkConsumer.class);
  private static final long POLL_INTERVAL = 5000; // 5s

  private final BaseTaskConsumer taskConsumer;

  private volatile boolean running = true;

  public FlinkConsumer(OptimizerConfig config) {
    this.taskConsumer = new BaseTaskConsumer(config);
  }

  public FlinkConsumer(BaseTaskConsumer taskConsumer) {
    this.taskConsumer = taskConsumer;
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
  }

  @Override
  public void run(SourceContext<TaskWrapper> sourceContext) throws Exception {
    while (running) {
      try {
        TaskWrapper task = taskConsumer.pollTask(0);
        if (task != null) {
          sourceContext.collect(task);
        } else {
          LOG.info("poll no task and wait for {} ms", POLL_INTERVAL);
          Thread.sleep(POLL_INTERVAL);
        }
      } catch (Exception e) {
        if (!running) {
          break;
        }
        LOG.error("failed to poll task", e);
        Thread.sleep(POLL_INTERVAL);
      }
    }
  }

  @Override
  public void cancel() {
    running = false;
  }
}
