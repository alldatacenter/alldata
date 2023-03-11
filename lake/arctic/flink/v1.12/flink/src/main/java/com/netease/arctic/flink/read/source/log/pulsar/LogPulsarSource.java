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

package com.netease.arctic.flink.read.source.log.pulsar;

import com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.connector.pulsar.source.PulsarSource;
import org.apache.flink.connector.pulsar.source.config.SourceConfiguration;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StopCursor;
import org.apache.flink.connector.pulsar.source.enumerator.subscriber.PulsarSubscriber;
import org.apache.flink.connector.pulsar.source.enumerator.topic.range.RangeGenerator;
import org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchema;
import org.apache.flink.connector.pulsar.source.split.PulsarPartitionSplit;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.Schema;
import org.apache.iceberg.flink.FlinkSchemaUtil;

import java.util.Map;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSUMER_CHANGELOG_MODE;

/**
 * The Source implementation of Log Pulsar. Please use a {@link LogPulsarSourceBuilder} to construct a
 * {@link LogPulsarSource}. The following example shows how to create a LogPulsarSource.
 *
 * <pre>{@code
 * LogPulsarSource<String> source = LogPulsarSource
 *     .builder(schema, arcticTable.properties())
 *     .setTopics(TOPIC1)
 *     .setServiceUrl(getServiceUrl())
 *     .setAdminUrl(getAdminUrl())
 *     .setSubscriptionName("test")
 *     .build();
 * }</pre>
 *
 * <p>See {@link LogPulsarSourceBuilder} for more details.
 */
@PublicEvolving
public final class LogPulsarSource extends PulsarSource<RowData> {

  /**
   * read schema, only contains the selected fields
   */
  private final Schema schema;
  private final boolean logRetractionEnable;
  private final String logConsumerChangelogMode;

  LogPulsarSource(
      SourceConfiguration sourceConfiguration,
      PulsarSubscriber subscriber,
      RangeGenerator rangeGenerator,
      StartCursor startCursor,
      StopCursor stopCursor,
      Boundedness boundedness,
      PulsarDeserializationSchema<RowData> deserializationSchema,
      Schema schema,
      Map<String, String> tableProperties) {
    super(sourceConfiguration, subscriber, rangeGenerator, startCursor, stopCursor, boundedness, deserializationSchema);
    logRetractionEnable = CompatibleFlinkPropertyUtil.propertyAsBoolean(tableProperties,
        ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(), ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.defaultValue());
    Preconditions.checkArgument(!logRetractionEnable, 
        "log-store.consistency-guarantee.enabled is not supported for now");
    logConsumerChangelogMode = CompatibleFlinkPropertyUtil.propertyAsString(tableProperties,
        ARCTIC_LOG_CONSUMER_CHANGELOG_MODE.key(), ARCTIC_LOG_CONSUMER_CHANGELOG_MODE.defaultValue());
    this.schema = schema;
  }

  /**
   * Get a LogPulsarSourceBuilder to builder a {@link LogPulsarSource}.
   *
   * @param schema          read schema, only contains the selected fields
   * @param tableProperties Arctic table properties
   * @return a Log Pulsar source builder.
   */
  @SuppressWarnings("java:S4977")
  public static LogPulsarSourceBuilder builder(Schema schema, Map<String, String> tableProperties) {
    return new LogPulsarSourceBuilder(schema, tableProperties);
  }

  @Override
  public SourceReader<RowData, PulsarPartitionSplit> createReader(SourceReaderContext readerContext) {
    return LogPulsarSourceReaderFactory.create(
        readerContext, sourceConfiguration, schema, logRetractionEnable, logConsumerChangelogMode);
  }

  @Override
  public TypeInformation<RowData> getProducedType() {
    RowType rowType = FlinkSchemaUtil.convert(schema);
    return InternalTypeInfo.of(rowType);
  }
}
