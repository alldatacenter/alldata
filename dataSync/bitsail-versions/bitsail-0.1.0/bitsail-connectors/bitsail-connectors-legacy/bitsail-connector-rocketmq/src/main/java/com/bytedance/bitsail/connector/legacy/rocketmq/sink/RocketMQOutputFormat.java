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

package com.bytedance.bitsail.connector.legacy.rocketmq.sink;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.connector.legacy.rocketmq.config.RocketMQSinkConfig;
import com.bytedance.bitsail.connector.legacy.rocketmq.constant.RocketMQConstants;
import com.bytedance.bitsail.connector.legacy.rocketmq.error.RocketMQPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.rocketmq.option.RocketMQWriterOptions;
import com.bytedance.bitsail.connector.legacy.rocketmq.sink.format.RocketMQSerializationFactory;
import com.bytedance.bitsail.connector.legacy.rocketmq.sink.format.RocketMQSerializationSchema;
import com.bytedance.bitsail.connector.legacy.rocketmq.sink.format.RocketMQSinkFormat;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RocketMQOutputFormat extends OutputFormatPlugin<Row> implements ResultTypeQueryable<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(RocketMQOutputFormat.class);

  private RocketMQProducer rocketmqProducer;

  private RocketMQSinkConfig sinkConfig;

  private String topic;
  private String tag;

  private List<Integer> partitionIndices;

  // transform bitsail row to flink row
  private RowTypeInfo rowTypeInfo;

  /* serialize row(s) based on user-defined format */
  private RocketMQSerializationSchema serializationSchema;

  @Override
  public void initPlugin() {
    this.sinkConfig = new RocketMQSinkConfig(this.outputSliceConfig);
    this.topic = sinkConfig.getTopic();
    this.tag = sinkConfig.getTag();

    List<ColumnInfo> columns = this.outputSliceConfig.getNecessaryOption(RocketMQWriterOptions.COLUMNS,
        RocketMQPluginErrorCode.REQUIRED_VALUE);

    // get partition fields
    String partitionFields = this.outputSliceConfig.get(RocketMQWriterOptions.PARTITION_FIELDS);
    this.partitionIndices = getIndicesByFieldNames(columns, partitionFields);

    // get key index
    String keyFields = this.outputSliceConfig.get(RocketMQWriterOptions.KEY_FIELDS);
    List<Integer> keyIndices = getIndicesByFieldNames(columns, keyFields);

    // get output format type (support only json now)
    String formatType = this.outputSliceConfig.get(RocketMQWriterOptions.FORMAT);
    RocketMQSinkFormat sinkFormat = RocketMQSinkFormat.valueOf(formatType.toUpperCase());

    LOG.info("RocketMQ producer settings: " + sinkConfig);
    LOG.info("RocketMQ partition fields indices: " + partitionIndices);
    LOG.info("RocketMQ key indices: " + keyIndices);
    LOG.info("RocketMQ sink format type: " + sinkFormat);

    // transform bitsail row to flink row
    this.rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columns);
    LOG.info("Output Row Type Info: " + rowTypeInfo);

    // get serialization schema
    RocketMQSerializationFactory factory = new RocketMQSerializationFactory(rowTypeInfo, partitionIndices, keyIndices);
    this.serializationSchema = factory.getSerializationSchemaByFormat(sinkFormat);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    this.rocketmqProducer = new RocketMQProducer(sinkConfig);

    if (partitionIndices != null && !partitionIndices.isEmpty()) {
      rocketmqProducer.setEnableQueueSelector(true);
    }
    rocketmqProducer.validateParams();

    try {
      this.rocketmqProducer.open();
    } catch (Exception e) {
      throw new IOException("failed to open rocketmq producer: " + e.getMessage(), e);
    }
  }

  @Override
  public void writeRecordInternal(Row record) throws Exception {
    Message message = prepareMessage(record);
    String partitionKeys = serializationSchema.getPartitionKey(record);
    try {
      rocketmqProducer.send(message, partitionKeys);
    } catch (Exception e) {
      throw new IOException("failed to send record to rocketmq: " + e.getMessage(), e);
    }
  }

  @Override
  public String getType() {
    return "rocketmq";
  }

  @Override
  public int getMaxParallelism() {
    return RocketMQConstants.MAX_PARALLELISM_OUTPUT_ROCKETMQ;
  }

  /**
   * do nothing when task fail
   */
  @Override
  public void tryCleanupOnError() {
  }

  @Override
  public void close() throws IOException {
    try {
      if (rocketmqProducer != null) {
        rocketmqProducer.close();
      }
    } catch (Exception e) {
      throw new IOException("failed to close rocketmq producer: " + e.getMessage(), e);
    }
    super.close();
  }

  @Override
  public TypeSystem getTypeSystem() {
    return TypeSystem.FLINK;
  }

  @Override
  public RowTypeInfo getProducedType() {
    return this.rowTypeInfo;
  }

  /**
   * get indices by field names
   */
  private List<Integer> getIndicesByFieldNames(List<ColumnInfo> columns, String fieldNames) {
    if (StringUtils.isEmpty(fieldNames)) {
      return null;
    }

    List<String> fields = Arrays.asList(fieldNames.split(",\\s*"));
    List<Integer> indices = fields.stream().map(field -> {
      for (int i = 0; i < columns.size(); ++i) {
        String columnName = columns.get(i).getName().trim();
        if (columnName.equals(field)) {
          return i;
        }
      }
      throw new IllegalArgumentException("Field " + field + " not found in columns! All fields are: " + fieldNames);
    }).collect(Collectors.toList());
    return indices.isEmpty() ? null : indices;
  }

  /**
   * transform row to message (value, topic, tag)
   */
  private Message prepareMessage(Row row) {
    byte[] k = serializationSchema.serializeKey(row);
    byte[] value = serializationSchema.serializeValue(row);
    String key = k != null ? new String(k, StandardCharsets.UTF_8) : "";

    Preconditions.checkNotNull(topic, "the message topic is null");
    Preconditions.checkNotNull(value, "the message body is null");

    Message msg = new Message(topic, value);
    msg.setKeys(key);
    msg.setTags(tag);

    return msg;
  }
}

