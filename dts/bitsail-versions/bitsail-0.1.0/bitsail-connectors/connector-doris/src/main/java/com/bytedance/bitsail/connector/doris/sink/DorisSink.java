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

package com.bytedance.bitsail.connector.doris.sink;

import com.bytedance.bitsail.base.connector.writer.v1.Sink;
import com.bytedance.bitsail.base.connector.writer.v1.Writer;
import com.bytedance.bitsail.base.connector.writer.v1.WriterCommitter;
import com.bytedance.bitsail.base.serializer.BinarySerializer;
import com.bytedance.bitsail.base.serializer.SimpleBinarySerializer;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;
import com.bytedance.bitsail.connector.doris.DorisConnectionHolder;
import com.bytedance.bitsail.connector.doris.committer.DorisCommittable;
import com.bytedance.bitsail.connector.doris.committer.DorisCommittableSerializer;
import com.bytedance.bitsail.connector.doris.committer.DorisCommitter;
import com.bytedance.bitsail.connector.doris.config.DorisExecutionOptions;
import com.bytedance.bitsail.connector.doris.config.DorisOptions;
import com.bytedance.bitsail.connector.doris.error.DorisErrorCode;
import com.bytedance.bitsail.connector.doris.option.DorisWriterOptions;
import com.bytedance.bitsail.connector.doris.partition.DorisPartition;
import com.bytedance.bitsail.connector.doris.partition.DorisPartitionManager;
import com.bytedance.bitsail.connector.doris.sink.ddl.DorisSchemaManagerGenerator;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class DorisSink<InputT> implements Sink<InputT, DorisCommittable, DorisWriterState> {
  private static final Logger LOG = LoggerFactory.getLogger(DorisSink.class);
  private DorisExecutionOptions.WRITE_MODE writeMode;
  private DorisOptions dorisOptions;
  private DorisExecutionOptions dorisExecutionOptions;
  private BitSailConfiguration writerConfiguration;

  @Override
  public String getWriterName() {
    return "doris";
  }

  @Override
  public void configure(BitSailConfiguration commonConfiguration, BitSailConfiguration writerConfiguration) throws IOException {
    this.writerConfiguration = writerConfiguration;
    this.writeMode =
        DorisExecutionOptions.WRITE_MODE.valueOf(writerConfiguration.get(DorisWriterOptions.SINK_WRITE_MODE).toUpperCase());
    initDorisExecutionOptions(writerConfiguration);
    initDorisOptions(writerConfiguration);
    if (this.dorisExecutionOptions.getEnableDelete()) {
      //add delete sign column
      this.dorisOptions.getColumnInfos().add(new ColumnInfo("__DORIS_DELETE_SIGN__", "tinyint"));
    }
    if (this.writeMode.equals(DorisExecutionOptions.WRITE_MODE.BATCH_REPLACE)) {
      DorisConnectionHolder dorisConnectionHolder = DorisSchemaManagerGenerator.getDorisConnection(new DorisConnectionHolder(), dorisOptions);
      DorisPartitionManager dorisPartitionManager = DorisSchemaManagerGenerator.openDorisPartitionManager(dorisConnectionHolder, dorisOptions);
      try {
        dorisPartitionManager.cleanTemporaryPartition();
        dorisPartitionManager.createTemporaryPartition();
      } catch (SQLException e) {
        throw new IOException("Failed to create temp table or partitions.", e);
      }
    }
  }

  @Override
  public Writer<InputT, DorisCommittable, DorisWriterState> createWriter(Writer.Context<DorisWriterState> context) {
    return new DorisWriter(writerConfiguration, dorisOptions, dorisExecutionOptions);
  }

  @Override
  public TypeInfoConverter createTypeInfoConverter() {
    return new FileMappingTypeInfoConverter(getWriterName());
  }

  @Override
  public Optional<WriterCommitter<DorisCommittable>> createCommitter() {
    return Optional.of(new DorisCommitter(dorisOptions, dorisExecutionOptions.getWriterMode()));
  }

  @Override
  public BinarySerializer<DorisCommittable> getCommittableSerializer() {
    return new DorisCommittableSerializer();
  }

  @Override
  public BinarySerializer<DorisWriterState> getWriteStateSerializer() {
    return new SimpleBinarySerializer<DorisWriterState>();
  }

  private void initDorisOptions(BitSailConfiguration writerConfiguration) {
    LOG.info("Start to init DorisOptions!");
    DorisOptions.DorisOptionsBuilder builder = DorisOptions.builder()
        .feNodes(writerConfiguration.getNecessaryOption(DorisWriterOptions.FE_HOSTS, DorisErrorCode.REQUIRED_VALUE))
        .mysqlNodes(writerConfiguration.get(DorisWriterOptions.MYSQL_HOSTS))
        .databaseName(writerConfiguration.getNecessaryOption(DorisWriterOptions.DB_NAME, DorisErrorCode.REQUIRED_VALUE))
        .tableName(writerConfiguration.getNecessaryOption(DorisWriterOptions.TABLE_NAME, DorisErrorCode.REQUIRED_VALUE))
        .username(writerConfiguration.getNecessaryOption(DorisWriterOptions.USER, DorisErrorCode.REQUIRED_VALUE))
        .password(writerConfiguration.getNecessaryOption(DorisWriterOptions.PASSWORD, DorisErrorCode.REQUIRED_VALUE))
        .fieldDelimiter(writerConfiguration.get(DorisWriterOptions.CSV_FIELD_DELIMITER))
        .lineDelimiter(writerConfiguration.get(DorisWriterOptions.CSV_LINE_DELIMITER))
        .tableHasPartitions(writerConfiguration.get(DorisWriterOptions.TABLE_HAS_PARTITION))
        .tableModel(DorisOptions.TableModel.valueOf(writerConfiguration.get(DorisWriterOptions.TABLE_MODEL).toUpperCase().trim()))
        .loadDataFormat(DorisOptions.LOAD_CONTENT_TYPE.valueOf(writerConfiguration.get(DorisWriterOptions.LOAD_CONTEND_TYPE).toUpperCase()));

    if (this.writeMode.name().startsWith("STREAM")) {
      builder.columnInfos(JSON.parseArray(writerConfiguration.getString(DorisWriterOptions.COLUMNS.key()), ColumnInfo.class));
    } else {
      builder.columnInfos(writerConfiguration.getNecessaryOption(DorisWriterOptions.COLUMNS, DorisErrorCode.REQUIRED_VALUE));
    }
    String tmpTableName = writerConfiguration.get(DorisWriterOptions.TABLE_NAME) + "_dts_tmp";
    builder.tmpTableName(tmpTableName);
    boolean isHasPartition = writerConfiguration.get(DorisWriterOptions.TABLE_HAS_PARTITION);
    // Need partition info in batch replace modes.
    if (isHasPartition && this.writeMode.equals(DorisExecutionOptions.WRITE_MODE.BATCH_REPLACE)) {
      //BATCH and REPLACE mode need the partition infos
      List<Map<String, Object>> partitionList = writerConfiguration.getNecessaryOption(DorisWriterOptions.PARTITIONS, CommonErrorCode.CONFIG_ERROR);
      builder.partitions(
          partitionList.stream()
              .map(partition -> JSON.parseObject(JSON.toJSONString(partition), DorisPartition.class))
              .collect(Collectors.toList())
      );
    }
    dorisOptions = builder.build();
  }

  private void initDorisExecutionOptions(BitSailConfiguration writerConfiguration) {
    LOG.info("Start to init DorisExecutionOptions!");
    final DorisExecutionOptions.DorisExecutionOptionsBuilder builder = DorisExecutionOptions.builder();
    builder.flushIntervalMs(writerConfiguration.get(DorisWriterOptions.SINK_FLUSH_INTERVAL_MS))
        .maxRetries(writerConfiguration.get(DorisWriterOptions.SINK_MAX_RETRIES))
        .bufferCount(writerConfiguration.get(DorisWriterOptions.SINK_BUFFER_COUNT))
        .bufferSize(writerConfiguration.get(DorisWriterOptions.SINK_BUFFER_SIZE))
        .labelPrefix(writerConfiguration.get(DorisWriterOptions.SINK_LABEL_PREFIX))
        .enableDelete(writerConfiguration.get(DorisWriterOptions.SINK_ENABLE_DELETE))
        .writerMode(this.writeMode)
        .isBatch(this.writeMode.name().startsWith("BATCH"));
    Map<String, String> streamProperties =
        writerConfiguration.getUnNecessaryOption(DorisWriterOptions.STREAM_LOAD_PROPERTIES, new HashMap<>());
    Properties streamLoadProp = new Properties();
    streamLoadProp.putAll(streamProperties);
    builder.streamLoadProp(streamLoadProp);
    dorisExecutionOptions = builder.build();
  }
}

