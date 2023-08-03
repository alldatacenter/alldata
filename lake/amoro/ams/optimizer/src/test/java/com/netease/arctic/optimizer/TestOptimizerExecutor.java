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
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import com.netease.arctic.ams.api.OptimizingTask;
import com.netease.arctic.ams.api.OptimizingTaskId;
import com.netease.arctic.ams.api.OptimizingTaskResult;
import com.netease.arctic.optimizing.BaseOptimizingInput;
import com.netease.arctic.optimizing.OptimizingExecutor;
import com.netease.arctic.optimizing.OptimizingExecutorFactory;
import com.netease.arctic.optimizing.OptimizingInputProperties;
import com.netease.arctic.optimizing.TableOptimizing;
import com.netease.arctic.utils.SerializationUtil;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestOptimizerExecutor extends OptimizerTestBase {

  private static final String FAILED_TASK_MESSAGE = "Execute Task failed";

  private OptimizerExecutor optimizerExecutor;

  @Before
  public void startOptimizer() {
    OptimizerConfig optimizerConfig = OptimizerTestHelpers.buildOptimizerConfig(TEST_AMS.getServerUrl());
    optimizerExecutor = new OptimizerExecutor(optimizerConfig, 0);
    new Thread(optimizerExecutor::start).start();
  }

  @After
  public void stopOptimizer() {
    optimizerExecutor.stop();
  }

  @Test
  public void testWaitForToken() throws InterruptedException {
    TEST_AMS.getOptimizerHandler().offerTask(TestOptimizingInput.successInput(1).toTask(0, 0));
    TimeUnit.MILLISECONDS.sleep(OptimizerTestHelpers.CALL_AMS_INTERVAL * 2);
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getPendingTasks().size());
  }

  @Test
  public void testExecuteTaskSuccess() throws InterruptedException, TException {
    TEST_AMS.getOptimizerHandler().authenticate(new OptimizerRegisterInfo());
    String token = TEST_AMS.getOptimizerHandler().getRegisteredOptimizers().keySet().iterator().next();
    TEST_AMS.getOptimizerHandler().offerTask(TestOptimizingInput.successInput(1).toTask(0, 0));
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getPendingTasks().size());
    optimizerExecutor.setToken(token);
    TimeUnit.MILLISECONDS.sleep(OptimizerTestHelpers.CALL_AMS_INTERVAL * 2);
    Assert.assertEquals(0, TEST_AMS.getOptimizerHandler().getPendingTasks().size());
    Assert.assertTrue(TEST_AMS.getOptimizerHandler().getCompletedTasks().containsKey(token));
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getCompletedTasks().get(token).size());
    OptimizingTaskResult taskResult = TEST_AMS.getOptimizerHandler().getCompletedTasks().get(token).get(0);
    Assert.assertEquals(new OptimizingTaskId(0, 0), taskResult.getTaskId());
    Assert.assertNull(taskResult.getErrorMessage());
    TestOptimizingOutput output = SerializationUtil.simpleDeserialize(taskResult.getTaskOutput());
    Assert.assertEquals(1, output.inputId());
  }

  @Test
  public void testExecuteTaskFailed() throws InterruptedException, TException {
    TEST_AMS.getOptimizerHandler().authenticate(new OptimizerRegisterInfo());
    String token = TEST_AMS.getOptimizerHandler().getRegisteredOptimizers().keySet().iterator().next();
    TEST_AMS.getOptimizerHandler().offerTask(TestOptimizingInput.failedInput(1).toTask(0, 0));
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getPendingTasks().size());
    optimizerExecutor.setToken(token);
    TimeUnit.MILLISECONDS.sleep(OptimizerTestHelpers.CALL_AMS_INTERVAL * 2);
    Assert.assertEquals(0, TEST_AMS.getOptimizerHandler().getPendingTasks().size());
    Assert.assertTrue(TEST_AMS.getOptimizerHandler().getCompletedTasks().containsKey(token));
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getCompletedTasks().get(token).size());
    OptimizingTaskResult taskResult = TEST_AMS.getOptimizerHandler().getCompletedTasks().get(token).get(0);
    Assert.assertEquals(new OptimizingTaskId(0, 0), taskResult.getTaskId());
    Assert.assertNull(taskResult.getTaskOutput());
    Assert.assertTrue(taskResult.getErrorMessage().contains(FAILED_TASK_MESSAGE));
  }

  public static class TestOptimizingInput extends BaseOptimizingInput {
    private final int inputId;
    private final boolean executeSuccess;

    private TestOptimizingInput(int inputId, boolean executeSuccess) {
      this.inputId = inputId;
      this.executeSuccess = executeSuccess;
    }

    public static TestOptimizingInput successInput(int inputId) {
      return new TestOptimizingInput(inputId, true);
    }

    public static TestOptimizingInput failedInput(int inputId) {
      return new TestOptimizingInput(inputId, false);
    }

    private int inputId() {
      return inputId;
    }

    public OptimizingTask toTask(long processId, int taskId) {
      OptimizingTask optimizingTask = new OptimizingTask(new OptimizingTaskId(processId, taskId));
      optimizingTask.setTaskInput(SerializationUtil.simpleSerialize(this));
      Map<String, String> inputProperties = Maps.newHashMap();
      inputProperties.put(OptimizingInputProperties.TASK_EXECUTOR_FACTORY_IMPL,
          TestOptimizingExecutorFactory.class.getName());
      optimizingTask.setProperties(inputProperties);
      return optimizingTask;
    }
  }

  public static class TestOptimizingExecutorFactory implements OptimizingExecutorFactory<TestOptimizingInput> {

    @Override
    public void initialize(Map<String, String> properties) {

    }

    @Override
    public OptimizingExecutor createExecutor(TestOptimizingInput input) {
      return new TestOptimizingExecutor(input);
    }
  }

  public static class TestOptimizingExecutor implements OptimizingExecutor<TestOptimizingOutput> {

    private final TestOptimizingInput input;

    private TestOptimizingExecutor(TestOptimizingInput input) {
      this.input = input;
    }

    @Override
    public TestOptimizingOutput execute() {
      if (input.executeSuccess) {
        return new TestOptimizingOutput(input.inputId());
      } else {
        throw new IllegalStateException(FAILED_TASK_MESSAGE);
      }

    }
  }

  public static class TestOptimizingOutput implements TableOptimizing.OptimizingOutput {

    private final int inputId;

    private TestOptimizingOutput(int inputId) {
      this.inputId = inputId;
    }

    @Override
    public Map<String, String> summary() {
      return null;
    }

    private int inputId() {
      return inputId;
    }
  }
}
