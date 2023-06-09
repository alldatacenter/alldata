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

import com.netease.arctic.ams.api.OptimizeTask;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.TaskWrapper;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FakeBaseConsumer extends BaseTaskConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(FakeBaseConsumer.class);

  private OptimizeTask nextTaskToConsume;

  public FakeBaseConsumer(OptimizerConfig config) {
    super(config);
  }

  public OptimizeTask feedTask() {
    synchronized (this) {
      OptimizeTask newTask = new OptimizeTask();
      newTask.setTaskId(new OptimizeTaskId(OptimizeType.Major, UUID.randomUUID().toString()));
      this.nextTaskToConsume = newTask;
      this.notifyAll();
      return newTask;
    }
  }

  @Override
  public TaskWrapper pollTask(long timeout) throws TException {
    synchronized (this) {
      try {
        if (this.nextTaskToConsume == null) {
          this.wait(timeout);
        }
        OptimizeTask compactTask = this.nextTaskToConsume;
        this.nextTaskToConsume = null;
        if (compactTask == null) {
          return null;
        }
        LOG.info("get task {}", compactTask.getTaskId());
        return new TaskWrapper(compactTask, Math.abs(ThreadLocalRandom.current().nextInt()));
      } catch (InterruptedException e) {
        return null;
      }
    }
  }
}
