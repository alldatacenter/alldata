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

import com.netease.arctic.ams.api.OptimizingTaskResult;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestOptimizer extends OptimizerTestBase {

  @BeforeClass
  public static void reduceCallAmsInterval() {
    OptimizerTestHelpers.setCallAmsIntervalForTest();
  }

  @Test
  public void testStartOptimizer() throws InterruptedException {
    OptimizerConfig  optimizerConfig = OptimizerTestHelpers.buildOptimizerConfig(TEST_AMS.getServerUrl());
    Optimizer optimizer = new Optimizer(optimizerConfig);
    new Thread(optimizer::startOptimizing).start();
    TimeUnit.SECONDS.sleep(1);
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getRegisteredOptimizers().size());
    TEST_AMS.getOptimizerHandler().offerTask(TestOptimizerExecutor.TestOptimizingInput.successInput(1).toTask(0, 0));
    TEST_AMS.getOptimizerHandler().offerTask(TestOptimizerExecutor.TestOptimizingInput.successInput(2).toTask(0, 1));
    TimeUnit.MILLISECONDS.sleep(OptimizerTestHelpers.CALL_AMS_INTERVAL * 10);
    String token = optimizer.getToucher().getToken();
    List<OptimizingTaskResult> taskResults = TEST_AMS.getOptimizerHandler().getCompletedTasks().get(token);
    Assert.assertEquals(2, taskResults.size());
    optimizer.stopOptimizing();
  }
}
