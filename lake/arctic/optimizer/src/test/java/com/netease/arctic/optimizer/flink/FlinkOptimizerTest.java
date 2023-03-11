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
import com.netease.arctic.optimizer.operator.FakeBaseConsumer;
import com.netease.arctic.optimizer.operator.FakeBaseExecutor;
import com.netease.arctic.optimizer.operator.FakeBaseReporter;
import com.netease.arctic.optimizer.operator.FakeBaseToucher;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlinkOptimizerTest {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkOptimizerTest.class);
  private static final String JOB_NAME = "arctic-major-optimizer-job";

  public static void main(String[] args) throws CmdLineException {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(new Configuration());
    OptimizerConfig optimizerConfig = new OptimizerConfig(args);

    env.addSource(new FlinkConsumer(new FakeBaseConsumer(optimizerConfig)))
        .setParallelism(optimizerConfig.getExecutorParallel())
        .process(new FlinkExecuteFunction(new FakeBaseExecutor(optimizerConfig),
            optimizerConfig))
        .setParallelism(optimizerConfig.getExecutorParallel())
        .name(FlinkExecuteFunction.class.getName())
        .transform(FlinkReporter.class.getName(), Types.VOID,
            new FlinkReporter(new FakeBaseReporter(optimizerConfig), new FakeBaseToucher(optimizerConfig), optimizerConfig))
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

}