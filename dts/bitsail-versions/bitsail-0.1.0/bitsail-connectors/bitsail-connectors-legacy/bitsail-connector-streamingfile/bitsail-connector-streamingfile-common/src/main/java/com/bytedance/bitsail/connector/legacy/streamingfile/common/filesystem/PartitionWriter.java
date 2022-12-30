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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.RollingPolicy;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_VALUE_INDEX;

/**
 * Partition writer to write records with partition.
 *
 * <p>See {@link SingleDirectoryWriter}.
 * See {@link DynamicPartitionWriter}.
 *
 * @param <T> The type of the consumed records.
 */
@Internal
public abstract class PartitionWriter<T> {
  private static final Logger LOG = LoggerFactory.getLogger(PartitionWriter.class);

  protected final PartitionComputer<T> computer;
  protected final Context<T> context;
  protected final RollingPolicy<T> rollingPolicy;
  protected final PartitionTempFileManager manager;
  protected final MetricManager metrics;
  protected final FileSystemMetaManager fileSystemMetaManager;

  private final List<PartFileInfo> partFileInfoCreations;

  public PartitionWriter(Context<T> context,
                         PartitionComputer<T> computer,
                         PartitionTempFileManager manager,
                         RollingPolicy<T> rollingPolicy,
                         FileSystemMetaManager fileSystemMetaManager) {
    this.context = context;
    this.computer = computer;
    this.manager = manager;
    this.rollingPolicy = rollingPolicy;
    this.fileSystemMetaManager = fileSystemMetaManager;
    this.partFileInfoCreations = Lists.newArrayList();
    this.metrics = MetricsFactory.getInstanceMetricsManager(context.getJobConf(),
        context.getConf().getInteger(StreamingFileSystemMetricsNames.SUB_TASK_ID, StreamingFileSystemMetricsNames.INVALID_TASK_ID));
  }

  /**
   * use an outputFormat to write a record, integrated with rolling policy.
   */
  public void write(T in) throws Exception {
    //1. select corresponding format
    Tuple2<LinkedHashMap<String, String>, T> partitionAndRow = computer.generatePartitionAndRowData(in);
    String partition = generatePartitionFromRecord(partitionAndRow.f0);
    Tuple2<OutputFormat<T>, PartFileInfo> currentFormatInfo = getOrCreateFormatForPartition(partition);

    //2. check whether format could be rebuild
    currentFormatInfo = rebuildIfPossible(in, partition, currentFormatInfo);

    //3. write record and update file meta info
    writeRecordAndUpdateFileInfo(in, currentFormatInfo, partitionAndRow.f1);
  }

  private Tuple2<OutputFormat<T>, PartFileInfo> rebuildIfPossible(T in, String partition, Tuple2<OutputFormat<T>,
      PartFileInfo> currentFormatInfo) throws Exception {
    OutputFormat<T> outputFormat = currentFormatInfo.f0;
    PartFileInfo partFileInfo = currentFormatInfo.f1;

    if (Objects.nonNull(outputFormat)) {
      if (rollingPolicy.shouldRollOnEvent(partFileInfo, in)
          || fileSystemMetaManager.shouldUpdate()) {

        if (fileSystemMetaManager.shouldUpdate()) {
          LOG.info("closing in-progress part file due to meta manager " +
                  "(in-progress file created @ {}, current size @ {}).", partFileInfo.getCreationTime(),
              partFileInfo.getSize());

        } else if (rollingPolicy.shouldRollOnEvent(partFileInfo, in)) {
          LOG.info("closing in-progress part file due to event time rolling policy " +
                  "(in-progress file created @ {}, current size @ {}).", partFileInfo.getCreationTime(),
              partFileInfo.getSize());
        }

        closeFormatForPartition(partition);
        currentFormatInfo = getOrCreateFormatForPartition(partition);
        fileSystemMetaManager.setUpdateValue(false);
      }
    }
    return currentFormatInfo;
  }

  protected Tuple2<OutputFormat<T>, PartFileInfo> createFormatForPath(Path path, long timestamp, String partition) throws IOException {
    OutputFormat<T> format = context.createNewOutputFormat(path);
    PartFileInfo partFileInfo = new PartFileInfo(timestamp, 0);
    partFileInfo.setPath(path);
    partFileInfo.setPartition(partition);
    partFileInfoCreations.add(partFileInfo);
    return Tuple2.of(format, partFileInfo);
  }

  private void writeRecordAndUpdateFileInfo(T in, Tuple2<OutputFormat<T>, PartFileInfo> currentFormatInfo, T parsed) throws Exception {
    OutputFormat<T> outputFormat;
    PartFileInfo partFileInfo;

    outputFormat = currentFormatInfo.f0;
    partFileInfo = currentFormatInfo.f1;

    //record value is stored in 2nd row index, we only computer the size
    Object value = ((Row) in).getField(DUMP_ROW_VALUE_INDEX);
    if (value instanceof byte[]) {
      partFileInfo.setSize(partFileInfo.getSize() + ((byte[]) value).length);
    }
    partFileInfo.setLastUpdateTime(System.currentTimeMillis());

    outputFormat.writeRecord(parsed);
  }

  /**
   * generate partition from record, default we do not need to generate partition.
   *
   * @param partSpec partition spec
   * @return partition
   */
  public String generatePartitionFromRecord(LinkedHashMap<String, String> partSpec) {
    return PartitionPathUtils.generatePartitionPath(partSpec);
  }

  public List<PartFileInfo> getPartFileInfoCreations() {
    return partFileInfoCreations;
  }

  /**
   * when triggering processingTimeService, do some check and clean work, eg. close the inactive file.
   */
  public abstract void onProcessingTime(long timestamp) throws Exception;

  /**
   * offer the corresponding outputFormat and format info based on specific write strategy.
   *
   * @param partition provide the partition if needed.
   */
  abstract Tuple2<OutputFormat<T>, PartFileInfo> getOrCreateFormatForPartition(String partition) throws Exception;

  /**
   * recycle the corresponding outputFormat based on specific write strategy.
   */
  abstract void closeFormatForPartition(String partition) throws Exception;

  /**
   * get write opened formats info.
   */
  public abstract Map<String, OutputFormat<T>> getOutputFormats();

  /**
   * End a transaction.
   */
  public void close(long jonMinTimestamp, long checkpointId) throws Exception {
    close(jonMinTimestamp, checkpointId, true);
  }

  public void close(long jonMinTimestamp, long checkpointId, boolean clearPartFileInfo) throws Exception {
    LOG.info("Subtask {} create {}(s) files in checkpoint {}.",
        context.getConf().getInteger(StreamingFileSystemMetricsNames.SUB_TASK_ID, StreamingFileSystemMetricsNames.INVALID_TASK_ID),
        CollectionUtils.size(partFileInfoCreations), checkpointId);
    if (clearPartFileInfo) {
      clearPartFileInfo();
    }
  }

  public void clearPartFileInfo() {
    partFileInfoCreations.clear();
  }

  /**
   * Context for partition writer, provide some information and generation utils.
   */
  public static class Context<T> {
    @Getter
    private final Configuration conf;
    private final OutputFormatFactory<T> factory;
    @Getter
    private final BitSailConfiguration jobConf;

    private final FileSystemMetaManager manager;

    public Context(Configuration conf,
                   OutputFormatFactory<T> factory,
                   BitSailConfiguration jobConf,
                   FileSystemMetaManager manager) {
      this.conf = conf;
      this.factory = factory;
      this.jobConf = jobConf;
      this.manager = manager;
    }

    /**
     * Create a new output format with path, configure it and open it.
     */
    OutputFormat<T> createNewOutputFormat(Path path) throws IOException {
      OutputFormat<T> format = factory.createOutputFormat(path, manager);
      format.configure(conf);
      // Here we just think of it as a single file format, so there can only be a single task.
      format.open(0, 1);
      return format;
    }
  }
}
