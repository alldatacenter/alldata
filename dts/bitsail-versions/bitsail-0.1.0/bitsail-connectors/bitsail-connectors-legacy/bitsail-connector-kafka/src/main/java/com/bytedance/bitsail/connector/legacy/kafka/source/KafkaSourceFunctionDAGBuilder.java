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

package com.bytedance.bitsail.connector.legacy.kafka.source;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.configuration.ConfigParser;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.kafka.constants.KafkaConstants;
import com.bytedance.bitsail.connector.legacy.kafka.deserialization.DeserializationSchemaFactory;
import com.bytedance.bitsail.connector.legacy.messagequeue.source.AbstractMessageQueueSourceFunctionDAGBuilder;
import com.bytedance.bitsail.connector.legacy.messagequeue.source.option.BaseMessageQueueReaderOptions;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.internal.KafkaPartitionDiscoverer;
import org.apache.flink.streaming.connectors.kafka.internals.AbstractPartitionDiscoverer;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicsDescriptor;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR;

public class KafkaSourceFunctionDAGBuilder<IN> extends AbstractMessageQueueSourceFunctionDAGBuilder<IN> {
  public static final String KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS_DEFAULT = String.valueOf(3 * 60 * 1000);
  private static final Logger LOG = LoggerFactory.getLogger(KafkaSourceFunctionDAGBuilder.class);
  private static final int KAFKA_PARALLELISM_THRESHOLD = 4;
  private long discoveryIntervalMillis;
  private String startUpMode;

  @Override
  public void configure(ExecutionEnviron execution,
                        BitSailConfiguration readerConfiguration) throws Exception {
    super.configure(execution, readerConfiguration);
    Map<String, String> connectorConf = jobConf.getUnNecessaryMap(BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES);
    topic = connectorConf.get(KafkaConstants.CONNECTOR_TOPIC);
    startUpMode = connectorConf.getOrDefault(KafkaConstants.CONNECTOR_STARTUP_MODE, KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_GROUP_OFFSETS);

    String discoveryIntervalMillisStr = jobConf.getUnnecessaryValue(
        FlinkKafkaConsumerBase.KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS,
        KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS_DEFAULT);
    discoveryIntervalMillisStr = ConfigParser.getUnnecessaryKeyFromExtraProp(jobConf,
        FlinkKafkaConsumerBase.KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS, discoveryIntervalMillisStr);
    discoveryIntervalMillis = Long.parseLong(discoveryIntervalMillisStr);
  }

  @Override
  public String getReaderName() {
    return "kafka_source";
  }

  @Override
  protected String getOperatorName() {
    boolean multiSourceEnabled = jobConf.get(CommonOptions.MULTI_SOURCE_ENABLED);
    if (multiSourceEnabled) {
      Map<String, String> connectorConf = jobConf.getUnNecessaryMap(BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES);
      return super.getOperatorName() + "_" + connectorConf.get(KafkaConstants.CONNECTOR_SOURCE_INDEX);
    } else {
      return super.getOperatorName();
    }
  }

  @Override
  public DataStreamSource<IN> getStreamSource(FlinkExecutionEnviron executionEnviron) {
    properties.put(FlinkKafkaConsumerBase.KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS, discoveryIntervalMillis);

    KafkaDeserializationSchema<IN> kafkaDeserializationSchema =
        (KafkaDeserializationSchema<IN>) DeserializationSchemaFactory.getDeserializationSchema(jobConf);
    FlinkKafkaConsumerBase<IN> kafkaConsumer = getFlinkKafkaConsumer(topic, kafkaDeserializationSchema, properties);

    switch (startUpMode) {
      case KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_EARLIEST:
        kafkaConsumer.setStartFromEarliest();
        break;
      case KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_LATEST:
        kafkaConsumer.setStartFromLatest();
        break;
      case KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_GROUP_OFFSETS:
        kafkaConsumer.setStartFromGroupOffsets();
        break;
      case KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_SPECIFIC_OFFSETS:
        kafkaConsumer.<IN>setStartFromSpecificOffsets(getKafkaSpecificOffsets());
        break;
      case KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_SPECIFIC_TIMESTAMP:
        Map<String, String> connectorConf = jobConf.getUnNecessaryMap(BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES);
        long timestamp = Long.parseLong(connectorConf.get(KafkaConstants.CONNECTOR_SPECIFIC_TIMESTAMP));
        kafkaConsumer.setStartFromTimestamp(timestamp);
        break;
      default:
        throw new RuntimeException("Unsupported startup mode. " + startUpMode);
    }

    return executionEnviron.getExecutionEnvironment().addSource(kafkaConsumer);
  }

  private FlinkKafkaConsumerBase<IN> getFlinkKafkaConsumer(
      String topic, KafkaDeserializationSchema<IN> kafkaDeserializationSchema, Properties properties) {
    return new FlinkKafkaConsumer<IN>(topic, kafkaDeserializationSchema, properties);
  }

  @Override
  protected int getSourcePartitionNumber() {
    KafkaTopicsDescriptor descriptor = new KafkaTopicsDescriptor(Lists.newArrayList(topic), null);
    Properties properties = new Properties();
    properties.putAll(this.properties);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
    AbstractPartitionDiscoverer discover = getPartitionDiscoverer(descriptor, properties);
    try {
      discover.open();
      List<KafkaTopicPartition> discoverPartitions = discover.discoverPartitions();
      if (discoverPartitions == null) {
        throw new RuntimeException(String.format("Failed to get partition list, topic:[%s] maybe is not existed", topic));
      }

      if (discoverPartitions.isEmpty()) {
        throw new RuntimeException(String.format("Partition num is 0, topic:[%s] maybe is not correct", topic));
      }
      LOG.info("Discovery partition number is " + discoverPartitions.size());
      return CollectionUtils.size(discoverPartitions);
    } catch (Exception e) {
      LOG.error("Calculate kafka parallelism failed.", e);
      throw new IllegalStateException("Please check kafka properties for parallelism.");
    } finally {
      try {
        discover.close();
      } catch (Exception e) {
        LOG.error("Close discovery failed, skip exception.", e);
      }
    }
  }

  private AbstractPartitionDiscoverer getPartitionDiscoverer(KafkaTopicsDescriptor descriptor, Properties properties) {
    return new KafkaPartitionDiscoverer(descriptor, 0, 1, properties);
  }

  @Override
  public int getSingleTaskPartitionThreshold() {
    return KAFKA_PARALLELISM_THRESHOLD;
  }

  @Override
  protected Properties generateProperties() {
    return getKafkaProperties();
  }

  private Properties getKafkaProperties() {
    final Properties kafkaProperties = new Properties();
    DescriptorProperties descriptorProperties = new DescriptorProperties(true);
    descriptorProperties.putProperties(jobConf.getUnNecessaryMap(BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES));
    descriptorProperties.putProperties(jobConf.getUnNecessaryMap(CommonOptions.EXTRA_PROPERTIES));

    List<Map<String, String>> propsList = descriptorProperties.getFixedIndexedProperties(
        KafkaConstants.CONNECTOR_PROPERTIES,
        Arrays.asList(KafkaConstants.CONNECTOR_PROPERTIES_KEY, KafkaConstants.CONNECTOR_PROPERTIES_VALUE));
    propsList.forEach(kv -> kafkaProperties.put(
        descriptorProperties.getString(kv.get(KafkaConstants.CONNECTOR_PROPERTIES_KEY)),
        descriptorProperties.getString(kv.get(KafkaConstants.CONNECTOR_PROPERTIES_VALUE))
    ));

    List<String> mainProperties = getMainProperties();
    int prefixLength = (CONNECTOR + ".").length();
    mainProperties.forEach(p -> descriptorProperties.getOptionalString(p).ifPresent(
        v -> kafkaProperties.put(p.substring(prefixLength), v)));

    int dynamicPrefixLength = (KafkaConstants.CONNECTOR_KAFKA_PROPERTIES + ".").length();
    descriptorProperties.asMap().forEach((key, value) -> {
      if (key.startsWith(KafkaConstants.CONNECTOR_KAFKA_PROPERTIES + ".")) {
        kafkaProperties.put(key.substring(dynamicPrefixLength), value);
      }
    });
    return kafkaProperties;
  }

  private List<String> getMainProperties() {
    List<String> properties = new ArrayList<>();
    properties.add(KafkaConstants.CONNECTOR_GROUP_ID);
    properties.add(KafkaConstants.CONNECTOR_TOPIC);
    properties.add(KafkaConstants.CONNECTOR_SERVERS);
    return properties;
  }

  /**
   * the format of 'specific-offsets' should be like:
   * <p>"connector.specific-offsets" :
   * [
   * {
   * "partition":1,
   * "offset":100
   * },
   * {
   * "partition":2,
   * "offset":200
   * }
   * ]
   * </p>
   *
   * @return kafka partition info
   */
  public Map<KafkaTopicPartition, Long> getKafkaSpecificOffsets() {
    Map<KafkaTopicPartition, Long> specificOffsets = new HashMap<>();
    Map<String, String> connectorConf = jobConf.getUnNecessaryMap(BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES);
    String specificOffsetsString = connectorConf.get(KafkaConstants.CONNECTOR_SPECIFIC_OFFSETS);
    if (StringUtils.isEmpty(specificOffsetsString)) {
      return specificOffsets;
    }

    specificOffsetsString = specificOffsetsString.toLowerCase();
    List<KafkaPartitionInfo> partitionInfos = JsonSerializer.parseToList(
        specificOffsetsString,
        KafkaPartitionInfo.class);

    return partitionInfos.stream()
        .collect(Collectors.toMap(
            partitionInfo -> new KafkaTopicPartition(topic, partitionInfo.getPartition()),
            KafkaPartitionInfo::getOffset));
  }

  @Getter
  private static class KafkaPartitionInfo {
    Integer partition;
    Long offset;
  }
}
