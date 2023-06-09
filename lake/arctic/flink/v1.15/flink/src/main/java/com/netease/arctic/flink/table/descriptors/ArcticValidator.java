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

package com.netease.arctic.flink.table.descriptors;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.description.Description;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.descriptors.ConnectorDescriptorValidator;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Preconditions;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.flink.configuration.description.TextElement.text;

/**
 * Validate arctic table properties.
 */
public class ArcticValidator extends ConnectorDescriptorValidator {

  public static final String ARCTIC_EMIT_LOG = "log";
  public static final String ARCTIC_EMIT_FILE = "file";

  public static final String ARCTIC_EMIT_AUTO = "auto";

  public static final String ARCTIC_READ_FILE = "file";
  public static final String ARCTIC_READ_LOG = "log";

  public static final String ARCTIC_READ_MODE = "arctic.read.mode";
  public static final String ARCTIC_READ_MODE_DEFAULT = ARCTIC_READ_FILE;

  public static final String ARCTIC_LATENCY_METRIC_ENABLE = "metrics.event-latency.enabled";
  public static final boolean ARCTIC_LATENCY_METRIC_ENABLE_DEFAULT = false;
  @Deprecated
  public static final String ARCTIC_LATENCY_METRIC_ENABLE_LEGACY = "metrics.event-latency.enable";

  public static final String ARCTIC_THROUGHPUT_METRIC_ENABLE = "metrics.enabled";
  public static final boolean ARCTIC_THROUGHPUT_METRIC_ENABLE_DEFAULT = false;
  @Deprecated
  public static final String ARCTIC_THROUGHPUT_METRIC_ENABLE_LEGACY = "metrics.enable";

  public static final String BASE_WRITE_LOCATION = "base.write.location";
  public static final String BASE_WRITE_LOCATION_SUFFIX = "/init";

  public static final String ARCTIC_WRITE_MAX_OPEN_FILE_SIZE = "write.open-files.size.max";
  public static final long ARCTIC_WRITE_MAX_OPEN_FILE_SIZE_DEFAULT = 671088640L; // 640M = 5 * 128M

  // log.consumer.changelog.mode
  public static final String LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY = "append-only";
  public static final String LOG_CONSUMER_CHANGELOG_MODE_ALL_KINDS = "all-kinds";

  // file scan startup mode
  public static final String SCAN_STARTUP_MODE_EARLIEST = "earliest";
  public static final String SCAN_STARTUP_MODE_LATEST = "latest";
  public static final String SCAN_STARTUP_MODE_TIMESTAMP = "timestamp";

  public static final ConfigOption<Boolean> ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE =
      ConfigOptions.key("log-store.consistency-guarantee.enabled")
          .booleanType()
          .defaultValue(false)
          .withDescription("Flag hidden kafka read retraction enable or not.");

  @Deprecated
  public static final ConfigOption<Boolean> ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE_LEGACY =
      ConfigOptions.key("log-store.consistency-guarantee.enable")
          .booleanType()
          .defaultValue(false)
          .withDescription("Flag hidden kafka read retraction enable or not.");

  @Deprecated
  public static final ConfigOption<Boolean> ARCTIC_LOG_KAFKA_COMPATIBLE_ENABLE =
      ConfigOptions.key("log-store.kafka.compatible.enabled")
          .booleanType()
          .defaultValue(false)
          .withDescription("Flag hidden kafka reading compatible with legacy Kafka Source API enable or not" +
              " when restoring Flink application from a checkpoint." +
              " If a task should be restored from checkpoint which made in Arctic version less than 0.4.1");
  
  public static final ConfigOption<String> ARCTIC_LOG_CONSUMER_CHANGELOG_MODE =
      ConfigOptions.key("log.consumer.changelog.modes")
          .stringType()
          .defaultValue("all-kinds")
          .withDescription(
              Description.builder()
                  .text("Describe what changelog modes does the log consumer support ")
                  .list(
                      text("'all-kinds' (log consumer support +I/-D/-U/+U)"),
                      text("'append-only' (log consumer only support +I)")
                  )
                  .build())
          .withDescription("Describe what changelog modes does the log consumer support.");

  public static final ConfigOption<Integer> SOURCE_READER_FETCH_BATCH_RECORD_COUNT = ConfigOptions
      .key("table.exec.iceberg.fetch-batch-record-count")
      .intType()
      .defaultValue(2048)
      .withDescription("The target number of records for Iceberg reader fetch batch.");

  public static final ConfigOption<String> SCAN_STARTUP_MODE = ConfigOptions
      .key("scan.startup.mode")
      .stringType()
      .defaultValue(SCAN_STARTUP_MODE_LATEST)
      .withDescription(String.format("Optional startup mode for arctic source, valid values are " +
              "\"earliest\" or \"latest\", \"timestamp\". If %s values %s, \"earliest\":" +
              " read earliest table data including base and change files from" +
              " the current snapshot, \"latest\": read all incremental data in the change table starting from the" +
              " current snapshot (the current snapshot will be excluded), \"timestamp\" has not supported yet." +
              " If %s values %s, \"earliest\": start from the earliest offset possible." +
              " \"latest\": start from the latest offset," +
              " \"timestamp\": start from user-supplied timestamp for each partition.",
          ARCTIC_READ_MODE, ARCTIC_READ_FILE, ARCTIC_READ_MODE, ARCTIC_READ_LOG));

  public static final ConfigOption<Long> SCAN_STARTUP_TIMESTAMP_MILLIS =
      ConfigOptions.key("scan.startup.timestamp-millis")
          .longType()
          .noDefaultValue()
          .withDescription(
              "Optional timestamp used in case of \"timestamp\" startup mode");

  public static final ConfigOption<Boolean> SUBMIT_EMPTY_SNAPSHOTS = ConfigOptions
      .key("submit.empty.snapshots")
      .booleanType()
      .defaultValue(false)
      .withDescription("Optional submit empty snapshots to the arctic table, false means that writers will not emit" +
          " empty WriteResults to the committer operator, and reduce the number of snapshots in File Cache; true" +
          " means this job will submit empty snapshots to the table, it is suitable with some valid reasons, e.g." +
          " advance watermark metadata stored in the table(https://github.com/apache/iceberg/pull/5561).");

  public static final ConfigOption<String> ARCTIC_CATALOG =
      ConfigOptions.key("arctic.catalog")
          .stringType()
          .noDefaultValue()
          .withDescription("underlying arctic catalog name.");

  public static final ConfigOption<String> ARCTIC_DATABASE =
      ConfigOptions.key("arctic.database")
          .stringType()
          .noDefaultValue()
          .withDescription("underlying arctic database name.");

  public static final ConfigOption<String> ARCTIC_TABLE =
      ConfigOptions.key("arctic.table")
          .stringType()
          .noDefaultValue()
          .withDescription("underlying arctic table name.");

  public static final ConfigOption<Boolean> DIM_TABLE_ENABLE =
      ConfigOptions.key("dim-table.enabled")
          .booleanType()
          .defaultValue(false)
          .withDescription("If it is true, Arctic source will generate watermark after stock data being read");

  @Deprecated
  public static final ConfigOption<Boolean> DIM_TABLE_ENABLE_LEGACY =
      ConfigOptions.key("dim-table.enable")
          .booleanType()
          .defaultValue(false)
          .withDescription("If it is true, Arctic source will generate watermark after stock data being read");

  public static final ConfigOption<String> ARCTIC_EMIT_MODE =
      ConfigOptions.key("arctic.emit.mode")
          .stringType()
          .defaultValue(ARCTIC_EMIT_AUTO)
          .withDescription("file, log, auto. e.g.\n" +
              "'file' means only writing data into filestore.\n" +
              "'log' means only writing data into logstore.\n" +
              "'file,log' means writing data into both filestore and logstore.\n" +
              "'auto' means writing data into filestore if the logstore of the arctic table is disabled;" +
              " Also means writing data into both filestore and logstore if the logstore of the arctic table" +
              " is enabled.\n" +
              "'auto' is recommended.");


  public static final ConfigOption<Duration> AUTO_EMIT_LOGSTORE_WATERMARK_GAP =
      ConfigOptions.key("arctic.emit.auto-write-to-logstore.watermark-gap")
          .durationType()
          .noDefaultValue()
          .withDescription("Only enabled when 'arctic.emit.mode'='auto', if the watermark of the arctic writers" +
              " is greater than the current system timestamp subtracts the specific value, writers will also write" +
              " data into the logstore.\n" +
              "This value must be greater than 0.");

  public static final ConfigOption<Boolean> LOG_STORE_CATCH_UP =
      ConfigOptions.key("log-store.catch-up")
          .booleanType()
          .defaultValue(false)
          .withDescription("If it is true, Arctic source will emit data to filestore and logstore. If it is false," +
              " Arctic source will only emit data to filestore.");

  public static final ConfigOption<Long> LOG_STORE_CATCH_UP_TIMESTAMP = ConfigOptions
      .key("log-store.catch-up-timestamp")
      .longType()
      .defaultValue(0L)
      .withDescription("Mark the time to start double writing (the logstore of arctic table catches up with the" +
          " historical data).");

  @Override
  public void validate(DescriptorProperties properties) {
    String emitMode = properties.getString(ARCTIC_EMIT_MODE.key());
    if (StringUtils.isBlank(emitMode)) {
      throw new ValidationException(
          "None value for property '" +
              ARCTIC_EMIT_MODE.key());
    }

    String[] actualEmitModes = emitMode.split(",");
    List<String> modeList = Arrays.asList(ARCTIC_EMIT_FILE, ARCTIC_EMIT_LOG, ARCTIC_EMIT_AUTO);
    for (String mode : actualEmitModes) {
      if (!modeList.contains(mode)) {
        throw new ValidationException(
            "Unknown value for property '" +
                ARCTIC_EMIT_MODE.key() +
                "'.\n" +
                "Supported values are " +
                modeList.stream().collect(Collectors.toMap(v -> v, v -> DescriptorProperties.noValidation())).keySet() +
                " but was: " +
                mode);
      }

      Preconditions.checkArgument(
          !ARCTIC_EMIT_AUTO.equals(mode) || actualEmitModes.length == 1,
          "The value of property '" + ARCTIC_EMIT_MODE.key() + "' must be only 'auto' when it is included.");
    }
  }

  public static Configuration asConfiguration(Map<String, String> options) {
    final Configuration configuration = new Configuration();
    options.forEach(configuration::setString);
    return configuration;
  }

  private static RowType getRowType(CatalogBaseTable flinkTable) {
    return (RowType) flinkTable.getSchema().toRowDataType().getLogicalType();
  }
}
