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

package com.bytedance.bitsail.flink.core.runtime.messenger;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.messenger.Messenger;
import com.bytedance.bitsail.base.messenger.common.MessengerCounterType;
import com.bytedance.bitsail.base.messenger.common.MessengerGroup;
import com.bytedance.bitsail.base.messenger.context.MessengerContext;
import com.bytedance.bitsail.flink.core.runtime.RuntimeContextInjectable;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.accumulators.LongCounter;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.dropwizard.metrics.DropwizardMeterWrapper;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;

import java.io.IOException;
import java.util.Map;

/**
 * Created 2022/7/14
 */
public abstract class AbstractBatchMessenger<T> extends Messenger<T> implements RuntimeContextInjectable {

  protected static final String GROUP_BITSAIL = "bitsail";
  protected static final String SUCCESS_RECORDS_RATE = "succ_records_rate";
  protected static final String FAILED_RECORDS_RATE = "failed_records_rate";
  protected static final String SUCCESS_BYTES_RATE = "succ_bytes_rate";
  protected static final String SUCCESS_RECORDS = "succ_records";
  protected static final String FAILED_RECORDS = "failed_records";
  protected static final String SUCCESS_BYTES = "succ_bytes";
  protected static final String INPUT_SPLITS = "splits";

  protected transient Meter successRecordsMeter;
  protected transient Meter successBytesMeter;
  protected transient Meter failedRecordsMeter;

  protected transient Counter successRecordsCounter;
  protected transient Counter successBytesCounter;
  protected transient Counter failedRecordsCounter;
  protected transient Counter finishedSplitCounter;

  protected transient RuntimeContext flinkRuntimeContext;

  protected boolean committed;

  public AbstractBatchMessenger(MessengerContext messengerContext) {
    super(messengerContext);
  }

  @Override
  public void setRuntimeContext(RuntimeContext runtimeContext) {
    this.flinkRuntimeContext = runtimeContext;
  }

  @Override
  public void open() {
    String messengerGroupName = messengerContext.getMessengerGroup().getName().toLowerCase();

    successRecordsMeter = initializeMeter(messengerGroupName, SUCCESS_RECORDS_RATE);
    failedRecordsMeter = initializeMeter(messengerGroupName, FAILED_RECORDS_RATE);
    successBytesMeter = initializeMeter(messengerGroupName, SUCCESS_BYTES_RATE);

    successRecordsCounter = initializeCounter(messengerGroupName, SUCCESS_RECORDS);
    failedRecordsCounter = initializeCounter(messengerGroupName, FAILED_RECORDS);
    successBytesCounter = initializeCounter(messengerGroupName, SUCCESS_BYTES);
    if (MessengerGroup.READER.equals(messengerContext.getMessengerGroup())) {
      finishedSplitCounter = initializeCounter(messengerGroupName, INPUT_SPLITS);
    }

    committed = false;
  }

  protected Meter initializeMeter(String groupName, String meterName) {
    return flinkRuntimeContext.getMetricGroup()
        .addGroup(GROUP_BITSAIL)
        .addGroup(groupName)
        .meter(meterName, new DropwizardMeterWrapper(new com.codahale.metrics.Meter()));
  }

  protected Counter initializeCounter(String groupName, String counterName) {
    return flinkRuntimeContext.getMetricGroup()
        .addGroup(GROUP_BITSAIL)
        .addGroup(groupName)
        .counter(counterName);
  }

  @Override
  public void recordSplitProgress() {
    finishedSplitCounter.inc();
  }

  @Override
  public long getSuccessRecords() {
    return successRecordsCounter.getCount();
  }

  @Override
  public long getSuccessRecordBytes() {
    return successBytesCounter.getCount();
  }

  @Override
  public long getFailedRecords() {
    return failedRecordsCounter.getCount();
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public void commit() {
    if (committed) {
      return;
    }
    uploadMessengerCounters(messengerContext.getCompleteCounterName(MessengerCounterType.SUCCESS_RECORDS_COUNT),
        new LongCounter(successRecordsCounter.getCount()));
    uploadMessengerCounters(messengerContext.getCompleteCounterName(MessengerCounterType.SUCCESS_RECORDS_BYTES),
        new LongCounter(successBytesCounter.getCount()));
    uploadMessengerCounters(messengerContext.getCompleteCounterName(MessengerCounterType.FAILED_RECORDS_COUNT),
        new LongCounter(failedRecordsCounter.getCount()));
    uploadMessengerCounters(messengerContext.getCompleteCounterName(MessengerCounterType.TASK_RETRY_COUNT),
        new LongCounter(flinkRuntimeContext.getAttemptNumber()));

    committed = true;
  }

  protected void uploadMessengerCounters(String name, LongCounter longCounter) {
    LongCounter exists = flinkRuntimeContext.getLongCounter(name);
    exists.add(longCounter.getLocalValue());
  }

  public void restoreMessengerCounter(ProcessResult processResult) {
    if (null == processResult) {
      return;
    }

    JobExecutionResult jobExecutionResult = (JobExecutionResult) processResult.getJobExecutionResult();
    Map<String, Object> allAccumulatorResults = jobExecutionResult.getAllAccumulatorResults();
    long retryCount = getAccumulatorValue(messengerContext.getCompleteCounterName(MessengerCounterType.TASK_RETRY_COUNT),
        allAccumulatorResults);
    processResult.setTaskRetryCount((processResult.getTaskRetryCount()) + (int) retryCount);

    switch (messengerContext.getMessengerGroup()) {
      case READER:
        processResult.setJobFailedInputRecordCount(getAccumulatorValue(messengerContext.getCompleteCounterName(MessengerCounterType.FAILED_RECORDS_COUNT),
            allAccumulatorResults));
        processResult.setJobSuccessInputRecordCount(getAccumulatorValue(messengerContext.getCompleteCounterName(MessengerCounterType.SUCCESS_RECORDS_COUNT),
            allAccumulatorResults));
        processResult.setJobSuccessInputRecordBytes(getAccumulatorValue(messengerContext.getCompleteCounterName(MessengerCounterType.SUCCESS_RECORDS_BYTES),
            allAccumulatorResults));
        break;
      case WRITER:
        processResult.setJobFailedOutputRecordCount(getAccumulatorValue(messengerContext.getCompleteCounterName(MessengerCounterType.FAILED_RECORDS_COUNT),
            allAccumulatorResults));
        processResult.setJobSuccessOutputRecordCount(getAccumulatorValue(messengerContext.getCompleteCounterName(MessengerCounterType.SUCCESS_RECORDS_COUNT),
            allAccumulatorResults));
        processResult.setJobSuccessOutputRecordBytes(getAccumulatorValue(messengerContext.getCompleteCounterName(MessengerCounterType.SUCCESS_RECORDS_BYTES),
            allAccumulatorResults));
        break;
      default:
        break;
    }
  }

  protected long getAccumulatorValue(String name, Map<String, Object> allAccumulatorResults) {
    return (long) allAccumulatorResults.get(name);
  }
}
