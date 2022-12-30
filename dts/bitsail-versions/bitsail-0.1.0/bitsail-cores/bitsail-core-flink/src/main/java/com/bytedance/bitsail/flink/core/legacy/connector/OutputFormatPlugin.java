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

package com.bytedance.bitsail.flink.core.legacy.connector;

import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.dirty.DirtyCollectorFactory;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.messenger.Messenger;
import com.bytedance.bitsail.base.messenger.MessengerFactory;
import com.bytedance.bitsail.base.messenger.checker.DirtyRecordChecker;
import com.bytedance.bitsail.base.messenger.common.MessengerGroup;
import com.bytedance.bitsail.base.messenger.context.MessengerContext;
import com.bytedance.bitsail.base.messenger.context.SimpleMessengerContext;
import com.bytedance.bitsail.base.messenger.impl.NoOpMessenger;
import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.BitSailMetricManager;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.base.ratelimit.Channel;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.sink.SinkEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.flink.core.runtime.RuntimeContextInjectable;
import com.bytedance.bitsail.flink.core.util.RowUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.io.CleanupWhenUnsuccessful;
import org.apache.flink.api.common.io.FinalizeOnMaster;
import org.apache.flink.api.common.io.InitializeOnMaster;
import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.bytedance.bitsail.base.constants.BaseMetricsNames.RECORD_CHANNEL_FLOW_CONTROL;
import static com.bytedance.bitsail.base.constants.BaseMetricsNames.RECORD_INVOKE_LATENCY;
import static com.bytedance.bitsail.base.messenger.common.MessageType.FAILED;
import static com.bytedance.bitsail.base.messenger.common.MessageType.SUCCESS;

/**
 * @desc:
 */
public abstract class OutputFormatPlugin<E extends Row> extends RichOutputFormat<E> implements
    InitializeOnMaster, CleanupWhenUnsuccessful, Pluggable, FinalizeOnMaster {

  private static final Logger LOG = LoggerFactory.getLogger(OutputFormatPlugin.class);

  protected Channel channel;

  protected String instanceId;

  protected Long jobId;

  protected BitSailConfiguration commonConfig = null;

  protected BitSailConfiguration outputSliceConfig = null;

  protected Messenger<Row> messenger;
  protected MessengerContext messengerContext;

  protected AbstractDirtyCollector dirtyCollector;
  protected transient MetricManager metricManager;
  /**
   * whether the speculation is enabled
   */
  protected boolean speculation = false;
  protected String authType;
  private DirtyRecordChecker dirtyRecordChecker;

  @Override
  public void configure(Configuration parameters) {

  }

  public boolean isSpeculationEnabled() {
    return speculation;
  }

  private void buildMessenger() {
    messengerContext = SimpleMessengerContext
        .builder()
        .messengerGroup(MessengerGroup.WRITER)
        .instanceId(instanceId)
        .build();

    messenger = MessengerFactory.initMessenger(commonConfig, messengerContext);
    dirtyCollector = DirtyCollectorFactory.initDirtyCollector(commonConfig, messengerContext);
  }

  @Override
  public void initializeGlobal(int parallelism) throws IOException {

  }

  @Override
  public void finalizeGlobal(int parallelism) throws IOException {
    if (isSpeculationEnabled()) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "run with speculation enabled, but flink doesn't support speculation");
    }
  }

  public void initFromConf(BitSailConfiguration commonConf, BitSailConfiguration outputConf) throws Exception {
    this.commonConfig = commonConf;
    this.instanceId = commonConf.getNecessaryOption(CommonOptions.INTERNAL_INSTANCE_ID, CommonErrorCode.CONFIG_ERROR);
    this.jobId = commonConf.getNecessaryOption(CommonOptions.JOB_ID, CommonErrorCode.CONFIG_ERROR);
    this.outputSliceConfig = outputConf;
    this.initPlugin();

    long recordSpeed = commonConf.get(CommonOptions.WRITER_TRANSPORT_CHANNEL_SPEED_RECORD);
    long byteSpeed = commonConf.get(CommonOptions.WRITER_TRANSPORT_CHANNEL_SPEED_BYTE);
    LOG.info("Init Output Flow Control: ");
    this.channel = new Channel(recordSpeed, byteSpeed);

    this.dirtyRecordChecker = new DirtyRecordChecker(commonConf);
    buildMessenger();
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    ColumnCast.initColumnCast(commonConfig);

    if (messenger instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) messenger).setRuntimeContext(getRuntimeContext());
    }
    if (dirtyCollector instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) dirtyCollector).setRuntimeContext(getRuntimeContext());
    }
    messenger.open();

    int taskId = getRuntimeContext().getIndexOfThisSubtask();
    this.metricManager = new BitSailMetricManager(commonConfig,
        "batch.output",
        false,
        ImmutableList.of(
            Pair.newPair("instance", String.valueOf(commonConfig.get(CommonOptions.INSTANCE_ID))),
            Pair.newPair("type", getType()),
            Pair.newPair("task", String.valueOf(taskId))
        ));
    metricManager.start();
  }

  @Override
  public void close() throws IOException {
    messenger.commit();
    dirtyCollector.storeDirtyRecords();

    messenger.close();
    dirtyCollector.close();

    if (metricManager != null) {
      metricManager.close();
    }
  }

  /**
   * When job finished, sometimes we should do some other things, such as add hive partition etc.
   */
  @Override
  public void onSuccessComplete(ProcessResult result) throws Exception {
    messenger.restoreMessengerCounter(result);
    dirtyCollector.restoreDirtyRecords(result);
    LOG.info("Checking dirty records during output...");
    dirtyRecordChecker.check(result, MessengerGroup.WRITER);
  }

  @Override
  public void onDestroy() throws Exception {

  }

  @Override
  public void writeRecord(E record) throws IOException {
    try (CallTracer ignore = metricManager.recordTimer(RECORD_INVOKE_LATENCY).get()) {
      try {
        writeRecordInternal(record);
        messenger.addSuccessRecord(record);
        metricManager.reportRecord(RowUtil.getRowBytesSize(record), SUCCESS);
      } catch (BitSailException e) {
        messenger.addFailedRecord(record, e);
        dirtyCollector.collectDirty(record, e, System.currentTimeMillis());
        LOG.debug("Write one record failed. - " + record.toString(), e);
        metricManager.reportRecord(0, FAILED);
      }
    } catch (Exception e) {
      metricManager.reportRecord(0, FAILED);
      throw new IOException("Couldn't write data - " + record.toString(), e);
    }

    try (CallTracer ignore = metricManager.recordTimer(RECORD_CHANNEL_FLOW_CONTROL).get()) {
      channel.checkFlowControl(
          messenger.getSuccessRecords(),
          messenger.getSuccessRecordBytes(),
          messenger.getFailedRecords()
      );
    }
  }

  @PublicEvolving
  public DataStream<Row> transform(DataStream<Row> source) {
    return source;
  }

  public abstract void writeRecordInternal(E record) throws Exception;

  public abstract int getMaxParallelism();

  public int getMinParallelism() {
    return 1;
  }

  public boolean uniformedParallelism() {
    return false;
  }

  public SinkEngineConnector initSinkSchemaManager(BitSailConfiguration commonConf, BitSailConfiguration writerConf) throws Exception {
    return null;
  }

  @VisibleForTesting
  public void setEmptyMessenger() {
    this.messenger = new NoOpMessenger<>();
  }

  @VisibleForTesting
  public void setDirtyCollector(AbstractDirtyCollector dirtyCollector) {
    this.dirtyCollector = dirtyCollector;
  }
}
