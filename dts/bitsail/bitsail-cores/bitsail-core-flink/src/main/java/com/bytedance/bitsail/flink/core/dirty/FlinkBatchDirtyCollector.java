/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.flink.core.dirty;

import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.messenger.context.MessengerContext;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.flink.core.runtime.RuntimeContextInjectable;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.common.accumulators.ListAccumulator;
import org.apache.flink.api.common.functions.RuntimeContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Created 2022/7/14
 */
public class FlinkBatchDirtyCollector extends AbstractDirtyCollector implements RuntimeContextInjectable {
  private RuntimeContext runtimeContext;

  private List<String> dirtyRecords;

  private int maxDirtyRecordCount;

  private MessengerContext messengerContext;

  public FlinkBatchDirtyCollector(BitSailConfiguration commonConfiguration,
                                  MessengerContext messengerContext) {
    this(commonConfiguration, messengerContext, 0);
  }

  public FlinkBatchDirtyCollector(BitSailConfiguration commonConfiguration,
                                  MessengerContext messengerContext,
                                  int taskId) {
    super(commonConfiguration, taskId);
    isRunning = true;

    this.dirtyRecords = Lists.newArrayList();
    this.messengerContext = messengerContext;
    this.maxDirtyRecordCount = commonConfiguration.get(CommonOptions.MAX_DIRTY_RECORDS_STORED_NUM);
  }

  @Override
  protected boolean shouldSample() {
    return CollectionUtils.size(dirtyRecords) < maxDirtyRecordCount;
  }

  @Override
  protected void collect(Object dirtyObj, Throwable e, long processingTime) throws IOException {
    if (CollectionUtils.size(dirtyRecords) < maxDirtyRecordCount) {
      dirtyRecords.add(String.format("Row: [%s]. message: [%s]", dirtyObj.toString(), e.getMessage()));
    }
  }

  @Override
  public void storeDirtyRecords() {
    ListAccumulator<String> localListAccumulator = new ListAccumulator<>();
    dirtyRecords.forEach(localListAccumulator::add);

    String accumulatorName = getAccumulatorName();
    Accumulator<Object, Serializable> accumulator = runtimeContext.getAccumulator(accumulatorName);
    if (Objects.isNull(accumulator)) {
      runtimeContext.addAccumulator(accumulatorName, localListAccumulator);
    } else {
      runtimeContext.getAccumulator(accumulatorName)
          .merge((Accumulator) localListAccumulator);
    }

    this.dirtyRecords.clear();
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public void restoreDirtyRecords(ProcessResult processResult) {
    if (processResult == null) {
      return;
    }
    String accumulatorName = getAccumulatorName();
    JobExecutionResult jobExecutionResult = (JobExecutionResult) processResult.getJobExecutionResult();
    List<String> dirtyRecords = jobExecutionResult.getAccumulatorResult(accumulatorName);
    switch (messengerContext.getMessengerGroup()) {
      case READER:
        processResult.addInputDirtyRecords(dirtyRecords);
        break;
      case WRITER:
        processResult.addOutputDirtyRecords(dirtyRecords);
        break;
      default:
    }
  }

  @Override
  public void setRuntimeContext(RuntimeContext runtimeContext) {
    this.runtimeContext = runtimeContext;
  }

  private String getAccumulatorName() {
    return String.format("%s_%s_LIST_DIRTY", messengerContext.getMessengerGroup().getName(), messengerContext.getInstanceId());
  }
}
