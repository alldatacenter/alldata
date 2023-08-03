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

package com.netease.arctic.flink.util;

import com.netease.arctic.flink.metric.MetricsGenerator;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.table.descriptors.ArcticValidator;
import com.netease.arctic.flink.write.ArcticLogWriter;
import com.netease.arctic.flink.write.AutomaticLogWriter;
import com.netease.arctic.flink.write.hidden.HiddenLogWriter;
import com.netease.arctic.flink.write.hidden.kafka.HiddenKafkaFactory;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.Schema;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil.fetchLogstorePrefixProperties;
import static com.netease.arctic.table.TableProperties.ENABLE_LOG_STORE;
import static com.netease.arctic.table.TableProperties.LOG_STORE_ADDRESS;
import static com.netease.arctic.table.TableProperties.LOG_STORE_DATA_VERSION;
import static com.netease.arctic.table.TableProperties.LOG_STORE_DATA_VERSION_DEFAULT;
import static com.netease.arctic.table.TableProperties.LOG_STORE_MESSAGE_TOPIC;
import static com.netease.arctic.table.TableProperties.LOG_STORE_STORAGE_TYPE_DEFAULT;
import static com.netease.arctic.table.TableProperties.LOG_STORE_STORAGE_TYPE_KAFKA;
import static com.netease.arctic.table.TableProperties.LOG_STORE_TYPE;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

/**
 * An util that loads arctic table, build arctic log writer and so on.
 */
public class ArcticUtils {

  public static final Logger LOG = LoggerFactory.getLogger(ArcticUtils.class);

  public static ArcticTable loadArcticTable(ArcticTableLoader tableLoader) {
    tableLoader.open();
    ArcticTable table = tableLoader.loadArcticTable();
    try {
      tableLoader.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return table;
  }

  public static List<String> getPrimaryKeys(ArcticTable table) {
    if (table.isUnkeyedTable()) {
      return Collections.emptyList();
    }
    return table.asKeyedTable().primaryKeySpec().fields()
        .stream()
        .map(PrimaryKeySpec.PrimaryKeyField::fieldName)
        .collect(Collectors.toList());
  }

  public static MetricsGenerator getMetricsGenerator(boolean metricsEventLatency, boolean metricsEnable,
                                                     ArcticTable arcticTable, RowType flinkSchemaRowType,
                                                     Schema writeSchema) {
    MetricsGenerator metricsGenerator;
    if (metricsEventLatency) {
      String modifyTimeColumn = arcticTable.properties().get(TableProperties.TABLE_EVENT_TIME_FIELD);
      metricsGenerator = MetricsGenerator.newGenerator(arcticTable.schema(), flinkSchemaRowType,
          modifyTimeColumn, metricsEnable);
    } else {
      metricsGenerator = MetricsGenerator.empty(metricsEnable);
    }
    return metricsGenerator;
  }

  public static boolean arcticWALWriterEnable(Map<String, String> properties, String arcticEmitMode) {
    boolean streamEnable = CompatiblePropertyUtil.propertyAsBoolean(properties, ENABLE_LOG_STORE,
        TableProperties.ENABLE_LOG_STORE_DEFAULT);

    if (arcticEmitMode.contains(ArcticValidator.ARCTIC_EMIT_LOG)) {
      if (!streamEnable) {
        throw new ValidationException(
            "emit to kafka was set, but no kafka config be found, please set kafka config first");
      }
      return true;
    } else if (arcticEmitMode.equals(ArcticValidator.ARCTIC_EMIT_AUTO)) {
      LOG.info("arctic emit mode is auto, and the arctic table {} is {}",
          ENABLE_LOG_STORE,
          streamEnable);
      return streamEnable;
    }

    return false;
  }

  /**
   * only when {@link ArcticValidator#ARCTIC_EMIT_MODE} contains {@link ArcticValidator#ARCTIC_EMIT_FILE}
   * and enable {@link TableProperties#ENABLE_LOG_STORE}
   * create logWriter according to {@link TableProperties#LOG_STORE_DATA_VERSION}
   *
   * @param properties        arctic table properties
   * @param producerConfig
   * @param topic
   * @param tableSchema
   * @param tableLoader       arctic table loader
   * @param watermarkWriteGap watermark gap that triggers automatic writing to log storage
   * @return ArcticLogWriter
   */
  public static ArcticLogWriter buildArcticLogWriter(Map<String, String> properties,
                                                     @Nullable Properties producerConfig,
                                                     @Nullable String topic,
                                                     TableSchema tableSchema,
                                                     String arcticEmitMode,
                                                     ShuffleHelper helper,
                                                     ArcticTableLoader tableLoader,
                                                     Duration watermarkWriteGap) {
    if (!arcticWALWriterEnable(properties, arcticEmitMode)) {
      return null;
    }

    if (topic == null) {
      topic = CompatibleFlinkPropertyUtil.propertyAsString(properties, LOG_STORE_MESSAGE_TOPIC, null);
    }
    Preconditions.checkNotNull(topic, String.format("Topic should be specified. It can be set by '%s'",
        LOG_STORE_MESSAGE_TOPIC));

    producerConfig = combineTableAndUnderlyingLogstoreProperties(properties, producerConfig);

    String version = properties.getOrDefault(LOG_STORE_DATA_VERSION, LOG_STORE_DATA_VERSION_DEFAULT);
    if (LOG_STORE_DATA_VERSION_DEFAULT.equals(version)) {
      if (arcticEmitMode.equals(ArcticValidator.ARCTIC_EMIT_AUTO)) {
        LOG.info("arctic emit mode is auto, and we will build automatic log writer: AutomaticLogWriter(v1)");
        return new AutomaticLogWriter(
            FlinkSchemaUtil.convert(tableSchema),
            producerConfig,
            topic,
            new HiddenKafkaFactory<>(),
            LogRecordV1.fieldGetterFactory,
            IdGenerator.generateUpstreamId(),
            helper,
            tableLoader,
            watermarkWriteGap
        );
      }

      LOG.info("build log writer: HiddenLogWriter(v1)");
      return new HiddenLogWriter(
          FlinkSchemaUtil.convert(tableSchema),
          producerConfig,
          topic,
          new HiddenKafkaFactory<>(),
          LogRecordV1.fieldGetterFactory,
          IdGenerator.generateUpstreamId(),
          helper);
    }
    throw new UnsupportedOperationException("don't support log version '" + version +
        "'. only support 'v1' or empty");
  }

  /**
   * Extract and combine the properties for underlying log store queue.
   * @param tableProperties arctic table properties
   * @param producerConfig can be set by java API
   * @return properties with tableProperties and producerConfig which has higher priority.
   */
  private static Properties combineTableAndUnderlyingLogstoreProperties(Map<String, String> tableProperties,
                                                    Properties producerConfig) {
    Properties finalProp;
    Properties underlyingLogStoreProps = fetchLogstorePrefixProperties(tableProperties);
    if (producerConfig == null) {
      finalProp = underlyingLogStoreProps;
    } else {
      underlyingLogStoreProps.stringPropertyNames()
          .forEach(k -> producerConfig.putIfAbsent(k, underlyingLogStoreProps.get(k)));
      finalProp = producerConfig;
    }

    String logStoreAddress = CompatibleFlinkPropertyUtil.propertyAsString(tableProperties,
        LOG_STORE_ADDRESS, null);

    String logType = CompatibleFlinkPropertyUtil.propertyAsString(tableProperties, LOG_STORE_TYPE,
        LOG_STORE_STORAGE_TYPE_DEFAULT);
    if (logType.equals(LOG_STORE_STORAGE_TYPE_KAFKA)) {
      finalProp.putIfAbsent("key.serializer",
          "org.apache.kafka.common.serialization.ByteArraySerializer");
      finalProp.putIfAbsent("value.serializer",
          "org.apache.kafka.common.serialization.ByteArraySerializer");
      finalProp.putIfAbsent("key.deserializer",
          "org.apache.kafka.common.serialization.ByteArrayDeserializer");
      finalProp.putIfAbsent("value.deserializer",
          "org.apache.kafka.common.serialization.ByteArrayDeserializer");

      if (logStoreAddress != null) {
        finalProp.putIfAbsent(BOOTSTRAP_SERVERS_CONFIG, logStoreAddress);
      }

      Preconditions.checkArgument(finalProp.containsKey(BOOTSTRAP_SERVERS_CONFIG), String.format("%s should be set",
          LOG_STORE_ADDRESS));
    }

    return finalProp;
  }

  public static boolean arcticFileWriterEnable(String arcticEmitMode) {
    return arcticEmitMode.contains(ArcticValidator.ARCTIC_EMIT_FILE) ||
        arcticEmitMode.equals(ArcticValidator.ARCTIC_EMIT_AUTO);
  }

  public static boolean isToBase(boolean overwrite) {
    boolean toBase = overwrite;
    LOG.info("is write to base:{}", toBase);
    return toBase;
  }

  public static RowData removeArcticMetaColumn(RowData rowData, int columnSize) {
    GenericRowData newRowData = new GenericRowData(rowData.getRowKind(), columnSize);
    if (rowData instanceof GenericRowData) {
      GenericRowData before = (GenericRowData) rowData;
      for (int i = 0; i < newRowData.getArity(); i++) {
        newRowData.setField(i, before.getField(i));
      }
      return newRowData;
    }
    throw new UnsupportedOperationException(
        String.format(
            "Can't remove arctic meta column from this RowData %s",
            rowData.getClass().getSimpleName()));
  }

}