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

package com.bytedance.bitsail.connector.legacy.kafka.sink;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.connector.legacy.kafka.common.KafkaFormatErrorCode;
import com.bytedance.bitsail.connector.legacy.kafka.option.KafkaWriterOptions;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.plugins.OutputAdapter;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.connector.legacy.kafka.constants.KafkaConstants.MAX_PARALLELISM;
import static com.bytedance.bitsail.connector.legacy.kafka.constants.KafkaConstants.REQUEST_TIMEOUT_MS_CONFIG;

@Slf4j
public class KafkaOutputFormat extends OutputFormatPlugin<Row> implements ResultTypeQueryable<Row> {

  protected Map<String, Object> optionalConfig;
  /**
   * For handling error propagation or logging callbacks.
   */
  protected transient Callback callback;
  /**
   * Errors encountered in the async producer are stored here.
   */
  protected transient volatile Exception asyncException;
  /**
   * When encountering kafka send failures, log the exception or throw it anyway.
   */
  protected boolean logFailuresOnly;
  private String kafkaTopic;
  private String kafkaServers;
  private KafkaProducer kafkaProducer;
  /**
   * The INDEX list of partition fields in columns
   */
  private List<Integer> partitionFieldsIndices;
  private List<ColumnInfo> columns;
  /**
   * For transforming bitSail column to flink type in {@link OutputAdapter}
   */
  private RowTypeInfo rowTypeInfo;

  @Override
  public void initPlugin() {
    this.kafkaServers = outputSliceConfig.getNecessaryOption(KafkaWriterOptions.KAFKA_SERVERS, KafkaFormatErrorCode.REQUIRED_VALUE);
    this.kafkaTopic = outputSliceConfig.getNecessaryOption(KafkaWriterOptions.TOPIC_NAME, KafkaFormatErrorCode.REQUIRED_VALUE);

    columns = outputSliceConfig.getNecessaryOption(KafkaWriterOptions.COLUMNS, KafkaFormatErrorCode.REQUIRED_VALUE);
    logFailuresOnly = outputSliceConfig.get(KafkaWriterOptions.LOG_FAILURES_ONLY);

    optionalConfig = commonConfig.getUnNecessaryOption(CommonOptions.OPTIONAL, new HashMap<>());
    addDefaultProducerParams(optionalConfig);
    log.info("Kafka producer optional config is: " + optionalConfig);

    String partitionField = outputSliceConfig.get(KafkaWriterOptions.PARTITION_FIELD);
    if (StringUtils.isNotEmpty(partitionField)) {
      List<String> partitionFieldsNames = Arrays.asList(partitionField.split(",\\s*"));
      partitionFieldsIndices = getPartitionFieldsIndices(columns, partitionFieldsNames);
    }

    this.rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columns);
    log.info("Output Row Type Info: " + rowTypeInfo);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    this.kafkaProducer = new KafkaProducer(this.kafkaServers, this.kafkaTopic, optionalConfig);
    if (logFailuresOnly) {
      callback = (metadata, e) -> {
        if (e != null) {
          log.error("Error while sending record to Kafka: " + e.getMessage(), e);
        }
      };
    } else {
      callback = (metadata, exception) -> {
        if (exception != null && asyncException == null) {
          asyncException = exception;
        }
      };
    }
  }

  @Override
  public String getType() {
    return "Kafka";
  }

  protected void checkErroneous() throws IOException {
    Exception e = asyncException;
    if (e != null) {
      // in case double throwing
      asyncException = null;
      throw new IOException("Failed to send data to Kafka: " + e.getMessage(), e);
    }
  }

  private void addDefaultProducerParams(Map<String, Object> optionalConfig) {
    if (!optionalConfig.containsKey(ProducerConfig.RETRIES_CONFIG)) {
      optionalConfig.put(ProducerConfig.RETRIES_CONFIG, outputSliceConfig.get(KafkaWriterOptions.RETRIES));
    }
    if (!optionalConfig.containsKey(ProducerConfig.RETRY_BACKOFF_MS_CONFIG)) {
      optionalConfig.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, outputSliceConfig.get(KafkaWriterOptions.RETRY_BACKOFF_MS));
    }
    if (!optionalConfig.containsKey(ProducerConfig.LINGER_MS_CONFIG)) {
      optionalConfig.put(ProducerConfig.LINGER_MS_CONFIG, outputSliceConfig.get(KafkaWriterOptions.LINGER_MS));
    }
    if (!optionalConfig.containsKey(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG)) {
      optionalConfig.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, REQUEST_TIMEOUT_MS_CONFIG);
    }
  }

  @Override
  public void writeRecordInternal(Row record) throws Exception {
    checkErroneous();

    JSONObject jsonObject = new JSONObject();
    String columnName;
    Object columnValue;
    for (int i = 0; i < record.getArity(); i++) {
      columnName = columns.get(i).getName();
      columnValue = record.getField(i);
      jsonObject.put(columnName, columnValue);
    }

    // get partition id to insert if 'partitionFieldsIndices' is not empty
    if (CollectionUtils.isNotEmpty(partitionFieldsIndices)) {
      String[] partitionFieldsValues = new String[partitionFieldsIndices.size()];
      for (int i = 0; i < partitionFieldsIndices.size(); i++) {
        int partitionFieldIndex = partitionFieldsIndices.get(i);
        partitionFieldsValues[i] = String.valueOf(record.getField(partitionFieldIndex));
      }
      int partitionId = choosePartitionIdByFields(partitionFieldsValues);
      sendByPartitionId(jsonObject.toJSONString(), partitionId);
    } else {
      send(jsonObject.toJSONString());
    }
  }

  @Override
  public int getMaxParallelism() {
    return MAX_PARALLELISM;
  }

  @Override
  public void tryCleanupOnError() {
  }

  @Override
  public void close() throws IOException {
    super.close();
    closeProducer();
    checkErroneous();
  }

  @Override
  public TypeSystem getTypeSystem() {
    return TypeSystem.FLINK;
  }

  @Override
  public RowTypeInfo getProducedType() {
    return rowTypeInfo;
  }

  /**
   * get all partition fields indices according to their name
   */
  private List<Integer> getPartitionFieldsIndices(List<ColumnInfo> columns, List<String> partitionFieldsNames) {
    return partitionFieldsNames.stream()
        .map(partitionFieldsName -> {
          for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i).getName();
            if (columnName.equals(partitionFieldsName)) {
              return i;
            }
          }
          throw new IllegalArgumentException("partitionFieldsNames is not in columns");
        }).collect(Collectors.toList());
  }

  private int choosePartitionIdByFields(String[] fields) {
    return kafkaProducer.choosePartitionIdByFields(fields);
  }

  private void send(String value) {
    kafkaProducer.send(value, callback);
  }

  private void sendByPartitionId(String value, int partitionId) {
    kafkaProducer.send(value, partitionId, callback);
  }

  private void closeProducer() {
    if (Objects.nonNull(kafkaProducer)) {
      kafkaProducer.close();
    }
  }
}
