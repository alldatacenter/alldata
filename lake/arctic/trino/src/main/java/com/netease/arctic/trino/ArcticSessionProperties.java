
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

package com.netease.arctic.trino;

import com.google.common.collect.ImmutableList;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import io.trino.orc.OrcWriteValidation.OrcWriteValidationMode;
import io.trino.plugin.base.session.SessionPropertiesProvider;
import io.trino.plugin.hive.HiveCompressionCodec;
import io.trino.plugin.hive.parquet.ParquetReaderConfig;
import io.trino.plugin.hive.parquet.ParquetWriterConfig;
import io.trino.plugin.iceberg.IcebergConfig;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.session.PropertyMetadata;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkArgument;
import static io.trino.plugin.base.session.PropertyMetadataUtil.dataSizeProperty;
import static io.trino.plugin.base.session.PropertyMetadataUtil.durationProperty;
import static io.trino.spi.session.PropertyMetadata.booleanProperty;
import static io.trino.spi.session.PropertyMetadata.integerProperty;

/**
 * Arctic supporting session properties
 */
public final class ArcticSessionProperties
    implements SessionPropertiesProvider {
  private static final String COMPRESSION_CODEC = "compression_codec";
  private static final String USE_FILE_SIZE_FROM_METADATA = "use_file_size_from_metadata";
  private static final String ORC_BLOOM_FILTERS_ENABLED = "orc_bloom_filters_enabled";
  private static final String ORC_MAX_MERGE_DISTANCE = "orc_max_merge_distance";
  private static final String ORC_MAX_BUFFER_SIZE = "orc_max_buffer_size";
  private static final String ORC_STREAM_BUFFER_SIZE = "orc_stream_buffer_size";
  private static final String ORC_TINY_STRIPE_THRESHOLD = "orc_tiny_stripe_threshold";
  private static final String ORC_MAX_READ_BLOCK_SIZE = "orc_max_read_block_size";
  private static final String ORC_LAZY_READ_SMALL_RANGES = "orc_lazy_read_small_ranges";
  private static final String ORC_NESTED_LAZY_ENABLED = "orc_nested_lazy_enabled";
  private static final String ORC_STRING_STATISTICS_LIMIT = "orc_string_statistics_limit";
  private static final String ORC_WRITER_VALIDATE_PERCENTAGE = "orc_writer_validate_percentage";
  private static final String ORC_WRITER_VALIDATE_MODE = "orc_writer_validate_mode";
  private static final String ORC_WRITER_MIN_STRIPE_SIZE = "orc_writer_min_stripe_size";
  private static final String ORC_WRITER_MAX_STRIPE_SIZE = "orc_writer_max_stripe_size";
  private static final String ORC_WRITER_MAX_STRIPE_ROWS = "orc_writer_max_stripe_rows";
  private static final String ORC_WRITER_MAX_DICTIONARY_MEMORY = "orc_writer_max_dictionary_memory";
  private static final String PARQUET_MAX_READ_BLOCK_SIZE = "parquet_max_read_block_size";
  private static final String PARQUET_WRITER_BLOCK_SIZE = "parquet_writer_block_size";
  private static final String PARQUET_WRITER_PAGE_SIZE = "parquet_writer_page_size";
  private static final String PARQUET_WRITER_BATCH_SIZE = "parquet_writer_batch_size";
  private static final String DYNAMIC_FILTERING_WAIT_TIMEOUT = "dynamic_filtering_wait_timeout";
  private static final String STATISTICS_ENABLED = "statistics_enabled";
  private static final String PROJECTION_PUSHDOWN_ENABLED = "projection_pushdown_enabled";
  private static final String TARGET_MAX_FILE_SIZE = "target_max_file_size";
  private static final String SYNC_HIVE = "sync_hive";
  public static final String EXPIRE_SNAPSHOTS_MIN_RETENTION = "expire_snapshots_min_retention";
  public static final String DELETE_ORPHAN_FILES_MIN_RETENTION = "delete_orphan_files_min_retention";

  private final List<PropertyMetadata<?>> sessionProperties;

  @Inject
  public ArcticSessionProperties(
      IcebergConfig icebergConfig,
      ArcticConfig arcticConfig,
      ParquetReaderConfig parquetReaderConfig,
      ParquetWriterConfig parquetWriterConfig) {
    sessionProperties = ImmutableList.<PropertyMetadata<?>>builder()
        //                .add(enumProperty(
        //                        COMPRESSION_CODEC,
        //                        "Compression codec to use when writing files",
        //                        HiveCompressionCodec.class,
        //                        icebergConfig.getCompressionCodec(),
        //                        false))
        //                .add(booleanProperty(
        //                        USE_FILE_SIZE_FROM_METADATA,
        //                        "Use file size stored in Iceberg metadata",
        //                        icebergConfig.isUseFileSizeFromMetadata(),
        //                        false))
        .add(dataSizeProperty(
            PARQUET_MAX_READ_BLOCK_SIZE,
            "Parquet: Maximum size of a block to read",
            parquetReaderConfig.getMaxReadBlockSize(),
            false))
        .add(dataSizeProperty(
            PARQUET_WRITER_BLOCK_SIZE,
            "Parquet: Writer block size",
            parquetWriterConfig.getBlockSize(),
            false))
        .add(dataSizeProperty(
            PARQUET_WRITER_PAGE_SIZE,
            "Parquet: Writer page size",
            parquetWriterConfig.getPageSize(),
            false))
        .add(integerProperty(
            PARQUET_WRITER_BATCH_SIZE,
            "Parquet: Maximum number of rows passed to the writer in each batch",
            parquetWriterConfig.getBatchSize(),
            false))
        //                .add(durationProperty(
        //                        DYNAMIC_FILTERING_WAIT_TIMEOUT,
        //                        "Duration to wait for completion of dynamic filters during split generation",
        //                        icebergConfig.getDynamicFilteringWaitTimeout(),
        //                        false))
        .add(booleanProperty(
            STATISTICS_ENABLED,
            "Expose table statistics",
            icebergConfig.isTableStatisticsEnabled(),
            false))
        //                .add(booleanProperty(
        //                        PROJECTION_PUSHDOWN_ENABLED,
        //                        "Read only required fields from a struct",
        //                        icebergConfig.isProjectionPushdownEnabled(),
        //                        false))
        // .add(dataSizeProperty(
        //     TARGET_MAX_FILE_SIZE,
        //     "Target maximum size of written files; the actual size may be larger",
        //     hiveConfig.getTargetMaxFileSize(),
        //     false))
        .add(booleanProperty(SYNC_HIVE, "sync hive data to arctic", false, false))
        .add(durationProperty(
            EXPIRE_SNAPSHOTS_MIN_RETENTION,
            "Minimal retention period for expire_snapshot procedure",
            icebergConfig.getExpireSnapshotsMinRetention(),
            false))
        .add(durationProperty(
            DELETE_ORPHAN_FILES_MIN_RETENTION,
            "Minimal retention period for delete_orphan_files procedure",
            icebergConfig.getDeleteOrphanFilesMinRetention(),
            false))
        .build();
  }

  @Override
  public List<PropertyMetadata<?>> getSessionProperties() {
    return sessionProperties;
  }

  public static boolean isOrcBloomFiltersEnabled(ConnectorSession session) {
    return session.getProperty(ORC_BLOOM_FILTERS_ENABLED, Boolean.class);
  }

  public static DataSize getOrcMaxMergeDistance(ConnectorSession session) {
    return session.getProperty(ORC_MAX_MERGE_DISTANCE, DataSize.class);
  }

  public static DataSize getOrcMaxBufferSize(ConnectorSession session) {
    return session.getProperty(ORC_MAX_BUFFER_SIZE, DataSize.class);
  }

  public static DataSize getOrcStreamBufferSize(ConnectorSession session) {
    return session.getProperty(ORC_STREAM_BUFFER_SIZE, DataSize.class);
  }

  public static DataSize getOrcTinyStripeThreshold(ConnectorSession session) {
    return session.getProperty(ORC_TINY_STRIPE_THRESHOLD, DataSize.class);
  }

  public static DataSize getOrcMaxReadBlockSize(ConnectorSession session) {
    return session.getProperty(ORC_MAX_READ_BLOCK_SIZE, DataSize.class);
  }

  public static boolean getOrcLazyReadSmallRanges(ConnectorSession session) {
    return session.getProperty(ORC_LAZY_READ_SMALL_RANGES, Boolean.class);
  }

  public static boolean isOrcNestedLazy(ConnectorSession session) {
    return session.getProperty(ORC_NESTED_LAZY_ENABLED, Boolean.class);
  }

  public static DataSize getOrcStringStatisticsLimit(ConnectorSession session) {
    return session.getProperty(ORC_STRING_STATISTICS_LIMIT, DataSize.class);
  }

  public static boolean isOrcWriterValidate(ConnectorSession session) {
    double percentage = session.getProperty(ORC_WRITER_VALIDATE_PERCENTAGE, Double.class);
    if (percentage == 0.0) {
      return false;
    }

    checkArgument(percentage > 0.0 && percentage <= 100.0);

    return ThreadLocalRandom.current().nextDouble(100) < percentage;
  }

  public static OrcWriteValidationMode getOrcWriterValidateMode(ConnectorSession session) {
    return session.getProperty(ORC_WRITER_VALIDATE_MODE, OrcWriteValidationMode.class);
  }

  public static DataSize getOrcWriterMinStripeSize(ConnectorSession session) {
    return session.getProperty(ORC_WRITER_MIN_STRIPE_SIZE, DataSize.class);
  }

  public static DataSize getOrcWriterMaxStripeSize(ConnectorSession session) {
    return session.getProperty(ORC_WRITER_MAX_STRIPE_SIZE, DataSize.class);
  }

  public static int getOrcWriterMaxStripeRows(ConnectorSession session) {
    return session.getProperty(ORC_WRITER_MAX_STRIPE_ROWS, Integer.class);
  }

  public static DataSize getOrcWriterMaxDictionaryMemory(ConnectorSession session) {
    return session.getProperty(ORC_WRITER_MAX_DICTIONARY_MEMORY, DataSize.class);
  }

  public static HiveCompressionCodec getCompressionCodec(ConnectorSession session) {
    return session.getProperty(COMPRESSION_CODEC, HiveCompressionCodec.class);
  }

  public static boolean isUseFileSizeFromMetadata(ConnectorSession session) {
    return session.getProperty(USE_FILE_SIZE_FROM_METADATA, Boolean.class);
  }

  public static DataSize getParquetMaxReadBlockSize(ConnectorSession session) {
    return session.getProperty(PARQUET_MAX_READ_BLOCK_SIZE, DataSize.class);
  }

  public static DataSize getParquetWriterPageSize(ConnectorSession session) {
    return session.getProperty(PARQUET_WRITER_PAGE_SIZE, DataSize.class);
  }

  public static DataSize getParquetWriterBlockSize(ConnectorSession session) {
    return session.getProperty(PARQUET_WRITER_BLOCK_SIZE, DataSize.class);
  }

  public static int getParquetWriterBatchSize(ConnectorSession session) {
    return session.getProperty(PARQUET_WRITER_BATCH_SIZE, Integer.class);
  }

  public static Duration getDynamicFilteringWaitTimeout(ConnectorSession session) {
    return session.getProperty(DYNAMIC_FILTERING_WAIT_TIMEOUT, Duration.class);
  }

  public static boolean isStatisticsEnabled(ConnectorSession session) {
    return session.getProperty(STATISTICS_ENABLED, Boolean.class);
  }

  public static boolean isProjectionPushdownEnabled(ConnectorSession session) {
    return session.getProperty(PROJECTION_PUSHDOWN_ENABLED, Boolean.class);
  }

  public static long getTargetMaxFileSize(ConnectorSession session) {
    return session.getProperty(TARGET_MAX_FILE_SIZE, DataSize.class).toBytes();
  }

  public static boolean isSyncHive(ConnectorSession connectorSession) {
    return connectorSession.getProperty(SYNC_HIVE, Boolean.class);
  }

  public static Duration getExpireSnapshotMinRetention(ConnectorSession session) {
    return session.getProperty(EXPIRE_SNAPSHOTS_MIN_RETENTION, Duration.class);
  }

  public static Duration getDeleteOrphanFilesMinRetention(ConnectorSession session) {
    return session.getProperty(DELETE_ORPHAN_FILES_MIN_RETENTION, Duration.class);
  }
}
