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

package com.netease.arctic.flink.read.source.log.kafka;

import com.netease.arctic.flink.read.internals.KafkaSource;
import com.netease.arctic.flink.read.source.log.LogSourceHelper;
import com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.enumerator.subscriber.KafkaSubscriber;
import org.apache.flink.connector.kafka.source.metrics.KafkaSourceReaderMetrics;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.connector.kafka.source.reader.fetcher.KafkaSourceFetcherManager;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.Schema;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSUMER_CHANGELOG_MODE;

/**
 * The Source implementation of LogKafka.
 *
 * <pre>{@code
 * LogKafkaSource source = LogKafkaSource.builder(arcticSchema, configuration)
 *    .setTopics(Arrays.asList(TOPIC1))
 *    .setStartingOffsets(OffsetsInitializer.earliest())
 *    .setProperties(properties)
 *    .build();
 * }</pre>
 *
 * <p>See {@link LogKafkaSourceBuilder} for more details.
 */
public class LogKafkaSource extends KafkaSource<RowData> {
  private static final long serialVersionUID = 1L;

  /**
   * read schema, only contains the selected fields
   */
  private final Schema schema;
  private final boolean logRetractionEnable;
  private final String logConsumerChangelogMode;

  LogKafkaSource(
      KafkaSubscriber subscriber,
      OffsetsInitializer startingOffsetsInitializer,
      @Nullable OffsetsInitializer stoppingOffsetsInitializer,
      Boundedness boundedness,
      KafkaRecordDeserializationSchema<RowData> deserializationSchema,
      Properties props,
      Schema schema,
      Map<String, String> tableProperties) {
    super(subscriber, startingOffsetsInitializer, stoppingOffsetsInitializer, boundedness, deserializationSchema,
        props);
    this.schema = schema;
    logRetractionEnable = CompatibleFlinkPropertyUtil.propertyAsBoolean(tableProperties,
        ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(), ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.defaultValue());
    logConsumerChangelogMode = CompatibleFlinkPropertyUtil.propertyAsString(tableProperties,
        ARCTIC_LOG_CONSUMER_CHANGELOG_MODE.key(), ARCTIC_LOG_CONSUMER_CHANGELOG_MODE.defaultValue());
  }

  /**
   * Get a logKafkaSourceBuilder to build a {@link LogKafkaSource}.
   *
   * @return a Log Kafka source builder.
   */
  public static LogKafkaSourceBuilder builder(Schema schema, Map<String, String> tableProperties) {
    return new LogKafkaSourceBuilder(schema, tableProperties);
  }

  @Override
  public SourceReader<RowData, KafkaPartitionSplit> createReader(SourceReaderContext readerContext) {
    FutureCompletingBlockingQueue<RecordsWithSplitIds<ConsumerRecord<byte[], byte[]>>> elementsQueue =
        new FutureCompletingBlockingQueue<>();
    LogSourceHelper logReadHelper = logRetractionEnable ? new LogSourceHelper() : null;

    final KafkaSourceReaderMetrics kafkaSourceReaderMetrics = new KafkaSourceReaderMetrics(readerContext.metricGroup());
    Supplier<LogKafkaPartitionSplitReader> splitReaderSupplier =
        () ->
            new LogKafkaPartitionSplitReader(
                props, readerContext, kafkaSourceReaderMetrics, schema, logRetractionEnable,
                logReadHelper, logConsumerChangelogMode);
    LogKafkaRecordEmitter recordEmitter = new LogKafkaRecordEmitter(null);

    return new LogKafkaSourceReader<>(
        elementsQueue,
        new KafkaSourceFetcherManager(elementsQueue, splitReaderSupplier::get, (ignore) -> {}),
        recordEmitter,
        toConfiguration(props),
        readerContext,
        kafkaSourceReaderMetrics,
        logReadHelper);
  }

  @Override
  public TypeInformation<RowData> getProducedType() {
    RowType rowType = FlinkSchemaUtil.convert(schema);
    return InternalTypeInfo.of(rowType);
  }

  // ----------- private helper methods ---------------

  private Configuration toConfiguration(Properties props) {
    Configuration config = new Configuration();
    props.stringPropertyNames().forEach(key -> config.setString(key, props.getProperty(key)));
    return config;
  }
}
