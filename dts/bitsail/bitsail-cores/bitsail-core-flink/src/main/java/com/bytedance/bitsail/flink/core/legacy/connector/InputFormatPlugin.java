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
import com.bytedance.bitsail.base.messenger.checker.LowVolumeTestChecker;
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
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.flink.core.runtime.RuntimeContextInjectable;
import com.bytedance.bitsail.flink.core.util.RowUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import lombok.Setter;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.commons.lang.SerializationUtils;
import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.bytedance.bitsail.base.constants.BaseMetricsNames.RECORD_CHANNEL_FLOW_CONTROL;
import static com.bytedance.bitsail.base.constants.BaseMetricsNames.RECORD_INVOKE_LATENCY;
import static com.bytedance.bitsail.base.messenger.common.MessageType.FAILED;
import static com.bytedance.bitsail.base.messenger.common.MessageType.SUCCESS;

/**
 * @desc:
 */
public abstract class InputFormatPlugin<OT extends Row, T extends InputSplit> extends RichInputFormat<OT, T> implements
    Pluggable {

  private static final Logger LOG = LoggerFactory.getLogger(InputFormatPlugin.class);

  protected Channel channel;

  protected BitSailConfiguration commonConfig;
  protected BitSailConfiguration inputSliceConfig;

  protected Messenger<Row> messenger;
  protected MessengerContext messengerContext;

  protected transient MetricManager metricManager;
  /**
   * Used to tell flink framework whether has more records to fetch
   */
  protected transient boolean hasNext;
  protected String instanceId;
  protected Long jobId;

  // ------------------------------------ Runtime States ------------------------------------
  protected String mandatoryEncoding;
  protected transient T[] cachedInputSplits;
  protected Configuration parameters = new Configuration();
  protected transient Counter completedSplitsCounter;
  protected transient Gauge totalSplitsCounter;
  @Setter
  int totalSplitsNum = -1;
  private AbstractDirtyCollector dirtyCollector;
  private DirtyRecordChecker dirtyRecordChecker;
  private LowVolumeTestChecker lowVolumeTestChecker;
  private byte[] cachedInputSplitsCompressedBytes;

  @Override
  public void configure(Configuration parameters) {

  }

  private void buildMessenger() {
    messengerContext = SimpleMessengerContext
        .builder()
        .messengerGroup(MessengerGroup.READER)
        .instanceId(instanceId)
        .build();

    messenger = MessengerFactory.initMessenger(commonConfig, messengerContext);
    dirtyCollector = DirtyCollectorFactory.initDirtyCollector(commonConfig, messengerContext);
  }

  @Override
  public void openInputFormat() throws IOException {
    ColumnCast.initColumnCast(commonConfig);

    super.openInputFormat();
    if (messenger instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) messenger).setRuntimeContext(getRuntimeContext());
    }
    if (dirtyCollector instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) dirtyCollector).setRuntimeContext(getRuntimeContext());
    }
    messenger.open();

    int taskId = 0;
    try {
      completedSplitsCounter = getRuntimeContext().getMetricGroup().addGroup("bitsail").counter("completedSplits");
      totalSplitsCounter = getRuntimeContext().getMetricGroup().addGroup("bitsail").gauge("totalSplits", (Gauge<Integer>) () -> totalSplitsNum);
      taskId = getRuntimeContext().getIndexOfThisSubtask();
    } catch (Exception e) {
      // ignore
    }
    this.metricManager = new BitSailMetricManager(commonConfig,
        "batch.input",
        false,
        ImmutableList.of(
            Pair.newPair("instance", String.valueOf(commonConfig.get(CommonOptions.INSTANCE_ID))),
            Pair.newPair("type", getType()),
            Pair.newPair("task", String.valueOf(taskId))
        ));
    this.metricManager.start();
  }

  @Override
  public void closeInputFormat() throws IOException {
    super.closeInputFormat();
    messenger.commit();
    dirtyCollector.storeDirtyRecords();

    messenger.close();
    dirtyCollector.close();

    if (metricManager != null) {
      metricManager.close();
    }
  }

  @Override
  public void onSuccessComplete(ProcessResult result) throws Exception {
    LOG.info("Checking dirty records during input...");
    messenger.restoreMessengerCounter(result);
    dirtyCollector.restoreDirtyRecords(result);
    dirtyRecordChecker.check(result, MessengerGroup.READER);
  }

  @Override
  public void onDestroy() throws Exception {

  }

  @Override
  public final T[] createInputSplits(int minNumSplits) throws IOException {
    // return if cached in memory
    if (cachedInputSplits != null) {
      LOG.info("Input splits got from cache. Length: {}", cachedInputSplits.length);
      return cachedInputSplits;
    }

    // return if recovered from compressed bytes
    cachedInputSplits = recoverInputSplitsFromBytes();
    if (cachedInputSplits != null) {
      return cachedInputSplits;
    }

    // create input splits (expensive operation)
    LOG.info("Creating Input splits.");
    cachedInputSplits = createSplits(minNumSplits);
    cachedInputSplits = restrictSplitsNumber(cachedInputSplits);

    cachedInputSplitsCompressedBytes = compressToBytes(cachedInputSplits);
    LOG.info("Input splits compressed to bytes. Length: {}", cachedInputSplitsCompressedBytes.length);

    if (totalSplitsNum < 0) {
      setTotalSplitsNum(cachedInputSplits.length);
    }
    LOG.info("Input splits size: {}", totalSplitsNum);

    return cachedInputSplits;
  }

  private byte[] compressToBytes(Serializable object) throws IOException {
    final byte[] bytes = SerializationUtils.serialize(object);
    final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    final GZIPOutputStream gzOut = new GZIPOutputStream(bOut);
    gzOut.write(bytes);
    gzOut.close();
    return bOut.toByteArray();
  }

  private T[] restrictSplitsNumber(T[] cachedInputSplits) {
    if (lowVolumeTestChecker != null) {
      return lowVolumeTestChecker.restrictSplitsNumber(cachedInputSplits);
    } else {
      return cachedInputSplits;
    }
  }

  @SuppressWarnings("unchecked")
  private T[] recoverInputSplitsFromBytes() {
    if (cachedInputSplitsCompressedBytes == null || cachedInputSplitsCompressedBytes.length == 0) {
      LOG.info("Input splits not found in bytes cache.");
      return null;
    }

    try (val bIn = new ByteArrayInputStream(cachedInputSplitsCompressedBytes);
         val zipIn = new GZIPInputStream(bIn)) {
      val deserializeBytes = IOUtils.toByteArray(zipIn);
      final T[] result = (T[]) deserializeWithCL(deserializeBytes, Thread.currentThread().getContextClassLoader());
      LOG.info("Input splits recovered from bytes cache. Splits number: {}", result.length);
      return result;
    } catch (Exception e) {
      LOG.error("Error while recovering input splits from bytes cache.", e);
      return null;
    }
  }

  private Object deserializeWithCL(byte[] serialized, ClassLoader loader) {
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
      Object ret;
      if (loader != null) {
        ClassLoaderObjectInputStream cis = new ClassLoaderObjectInputStream(loader, bis);
        ret = cis.readObject();
        cis.close();
      } else {
        ObjectInputStream ois = new ObjectInputStream(bis);
        ret = ois.readObject();
        ois.close();
      }
      return ret;
    } catch (IOException | ClassNotFoundException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public void initFromConf(BitSailConfiguration commonConf, BitSailConfiguration inputConf)
      throws Exception {
    this.commonConfig = commonConf;
    this.instanceId = commonConf.getNecessaryOption(CommonOptions.INTERNAL_INSTANCE_ID, CommonErrorCode.CONFIG_ERROR);
    this.jobId = commonConf.getNecessaryOption(CommonOptions.JOB_ID, CommonErrorCode.CONFIG_ERROR);
    this.inputSliceConfig = inputConf;
    this.mandatoryEncoding = inputSliceConfig.get(ReaderOptions.BaseReaderOptions.CHARSET_NAME);
    this.initPlugin();

    LOG.info("Init Input Flow Control: ");
    long recordSpeed = commonConf.get(CommonOptions.READER_TRANSPORT_CHANNEL_SPEED_RECORD);
    long byteSpeed = commonConf.get(CommonOptions.READER_TRANSPORT_CHANNEL_SPEED_BYTE);
    this.channel = new Channel(recordSpeed, byteSpeed);

    this.dirtyRecordChecker = new DirtyRecordChecker(commonConf);

    this.lowVolumeTestChecker = new LowVolumeTestChecker(commonConf);
    this.configure(new Configuration());
    buildMessenger();
  }

  /**
   * Flink framework use this method to fetch record
   */
  @Override
  public final OT nextRecord(OT reuse) throws IOException {
    try {
      if (!hasNext) {
        return null;
      }
      try (CallTracer ignore = metricManager.recordTimer(RECORD_INVOKE_LATENCY).get()) {
        try {
          resetReuse(reuse);
          buildRow(reuse, mandatoryEncoding);
          messenger.addSuccessRecord(reuse);
          metricManager.reportRecord(RowUtil.getRowBytesSize(reuse), SUCCESS);
        } catch (BitSailException de) {
          messenger.addFailedRecord(reuse, de);
          dirtyCollector.collectDirty(reuse, de, System.currentTimeMillis());
          LOG.debug("Read one record failed.", de);
          metricManager.reportRecord(0, FAILED);
          return null;
        }
      }
      /*
       * Flow control
       */
      try (CallTracer ignore = metricManager.recordTimer(RECORD_CHANNEL_FLOW_CONTROL).get()) {
        channel.checkFlowControl(
            messenger.getSuccessRecords(),
            messenger.getSuccessRecordBytes(),
            messenger.getFailedRecords()
        );
      }

      return reuse;
    } catch (Exception e) {
      throw new IOException("Couldn't read data - " + e.getMessage(), e);
    }
  }

  protected void resetReuse(OT reuse) {
    int arity = reuse.getArity();
    for (int i = 0; i < arity; i++) {
      reuse.setField(i, null);
    }
  }

  @Override
  public final boolean reachedEnd() throws IOException {
    if (lowVolumeTestChecker != null && lowVolumeTestChecker.check(messenger.getSuccessRecords(),
        messenger.getFailedRecords())) {
      return true;
    }
    boolean splitEnd = isSplitEnd();
    if (splitEnd) {
      completeSplits();
    }

    return splitEnd;
  }

  public void completeSplits() {
    incCompletedSplits(1);
  }

  public void incCompletedSplits(long count) {
    if (completedSplitsCounter != null) {
      completedSplitsCounter.inc(count);
    }
  }

  /**
   * Build BitSail row from external storage engine
   *
   * @return row raw bytes
   */
  public abstract OT buildRow(OT reuse, String mandatoryEncoding) throws BitSailException;

  /**
   * Reach split or not.
   */
  public abstract boolean isSplitEnd() throws IOException;

  public abstract T[] createSplits(int minNumSplits) throws IOException;

  public SourceEngineConnector initSourceSchemaManager(BitSailConfiguration commonConf, BitSailConfiguration readerConf) throws Exception {
    return null;
  }

  public boolean supportSchemaCheck() {
    return false;
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
