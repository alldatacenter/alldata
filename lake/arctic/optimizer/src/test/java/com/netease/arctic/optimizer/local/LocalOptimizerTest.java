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

import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.api.OptimizeTask;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.operator.FakeBaseConsumer;
import com.netease.arctic.optimizer.operator.FakeBaseReporter;
import com.netease.arctic.optimizer.operator.FakeOperatorFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalOptimizerTest {
  private static final OptimizerConfig CONFIG = new OptimizerConfig();

  static {
    CONFIG.setExecutorParallel(1);
  }

  private FakeOperatorFactory operatorFactory;
  private LocalOptimizer localOptimizer;

  @Before
  public void before() {
    this.operatorFactory = new FakeOperatorFactory(CONFIG);
    this.localOptimizer = new LocalOptimizer(this.operatorFactory);
  }

  @Test
  public void testCommonCase() throws InterruptedException {
    this.localOptimizer.init(CONFIG);
    FakeBaseConsumer baseConsumer = operatorFactory.getBaseConsumer();
    FakeBaseReporter reporter = operatorFactory.getBaseReporter();
    OptimizeTask optimizeTask;
    OptimizeTaskStat optimizeTaskStat;

    for (int i = 0; i < 5; i++) {
      optimizeTask = baseConsumer.feedTask();
      optimizeTaskStat = reporter.waitReportResult();
      Assert.assertEquals(optimizeTask.getTaskId(), optimizeTaskStat.getTaskId());
      Assert.assertEquals(OptimizeStatus.Prepared, optimizeTaskStat.getStatus());
    }

  }

  @After
  public void after() {
    if (this.localOptimizer != null) {
      this.localOptimizer.release();
    }
  }
}