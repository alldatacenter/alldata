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

import com.netease.arctic.optimizer.OptimizerToucher;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.iceberg.common.DynFields;

public class FlinkToucher extends RichParallelSourceFunction<String> {

  private static final DynFields.UnboundField<Environment> FLINK_TASK_RUNTIME_FIELDS =
      DynFields.builder().hiddenImpl(StreamingRuntimeContext.class, "taskEnvironment").build();

  private final OptimizerToucher toucher;



  public FlinkToucher(OptimizerToucher toucher) {
    this.toucher = toucher;
  }

  @Override
  public void run(SourceContext<String> sourceContext) {
    String jobId = FLINK_TASK_RUNTIME_FIELDS.bind(getRuntimeContext()).get().getJobID().toString();
    toucher.withTokenChangeListener(sourceContext::collect)
        .withRegisterProperty(FlinkOptimizer.JOB_ID_PROPERTY, jobId)
        .start();
  }

  @Override
  public void cancel() {
    toucher.stop();
  }
}
