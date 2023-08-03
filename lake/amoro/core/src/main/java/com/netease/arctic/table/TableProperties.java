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

package com.netease.arctic.table;

import java.util.HashSet;
import java.util.Set;

import static com.netease.arctic.table.WatermarkGenerator.EVENT_TIME_TIMESTAMP_MS;
import static com.netease.arctic.table.WatermarkGenerator.INGEST_TIME;
import static org.apache.iceberg.TableProperties.DEFAULT_NAME_MAPPING;
import static org.apache.iceberg.TableProperties.FORMAT_VERSION;

/**
 * Reserved Arctic table properties list.
 */
public class TableProperties {

  private TableProperties() {
  }

  public static final String TABLE_PARTITION_PROPERTIES = "table.partition-properties";

  public static final String BASE_TABLE_MAX_TRANSACTION_ID = "base.table.max-transaction-id";

  public static final String PARTITION_OPTIMIZED_SEQUENCE = "max-txId";
  public static final String PARTITION_BASE_OPTIMIZED_TIME = "base-op-time";

  public static final String LOCATION = "location";

  public static final String TABLE_CREATE_TIME = "table.create-timestamp";
  public static final long TABLE_CREATE_TIME_DEFAULT = 0L;

  /**
   * table watermark related properties
   */

  public static final String TABLE_EVENT_TIME_FIELD = "table.event-time-field";
  public static final String TABLE_EVENT_TIME_FIELD_DEFAULT = INGEST_TIME;

  public static final String TABLE_WATERMARK_ALLOWED_LATENESS = "table.watermark-allowed-lateness-second";
  public static final long TABLE_WATERMARK_ALLOWED_LATENESS_DEFAULT = 0L;

  public static final String TABLE_EVENT_TIME_STRING_FORMAT = "table.event-time-field.datetime-string-format";
  public static final String TABLE_EVENT_TIME_STRING_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss";

  public static final String TABLE_EVENT_TIME_NUMBER_FORMAT = "table.event-time-field.datetime-number-format";
  public static final String TABLE_EVENT_TIME_NUMBER_FORMAT_DEFAULT = EVENT_TIME_TIMESTAMP_MS;

  public static final String WATERMARK_TABLE = "watermark.table";

  public static final String WATERMARK_BASE_STORE = "watermark.base";

  /**
   * table optimize related properties
   */
  public static final String ENABLE_SELF_OPTIMIZING = "self-optimizing.enabled";
  public static final boolean ENABLE_SELF_OPTIMIZING_DEFAULT = true;

  public static final String SELF_OPTIMIZING_GROUP = "self-optimizing.group";
  public static final String SELF_OPTIMIZING_GROUP_DEFAULT = "default";

  public static final String SELF_OPTIMIZING_QUOTA = "self-optimizing.quota";
  public static final double SELF_OPTIMIZING_QUOTA_DEFAULT = 0.1;

  public static final String SELF_OPTIMIZING_EXECUTE_RETRY_NUMBER = "self-optimizing.execute.num-retries";
  public static final int SELF_OPTIMIZING_EXECUTE_RETRY_NUMBER_DEFAULT = 5;

  public static final String SELF_OPTIMIZING_TARGET_SIZE = "self-optimizing.target-size";
  public static final long SELF_OPTIMIZING_TARGET_SIZE_DEFAULT = 134217728; // 128 MB

  public static final String SELF_OPTIMIZING_MAX_FILE_CNT = "self-optimizing.max-file-count";
  public static final int SELF_OPTIMIZING_MAX_FILE_CNT_DEFAULT = 10000;

  public static final String SELF_OPTIMIZING_MAX_FILE_SIZE_BYTES = "self-optimizing.max-file-size-bytes";
  public static final long SELF_OPTIMIZING_MAX_FILE_SIZE_BYTES_DEFAULT = 8589934592L; // 8 GB

  public static final String SELF_OPTIMIZING_FRAGMENT_RATIO = "self-optimizing.fragment-ratio";
  public static final int SELF_OPTIMIZING_FRAGMENT_RATIO_DEFAULT = 8;

  public static final String SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT = "self-optimizing.minor.trigger.file-count";
  public static final int SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT_DEFAULT = 12;

  public static final String SELF_OPTIMIZING_MINOR_TRIGGER_INTERVAL = "self-optimizing.minor.trigger.interval";
  public static final int SELF_OPTIMIZING_MINOR_TRIGGER_INTERVAL_DEFAULT = 3600000; // 1 h

  public static final String SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO =
      "self-optimizing.major.trigger.duplicate-ratio";
  public static final double SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO_DEFAULT = 0.5;

  public static final String SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL = "self-optimizing.full.trigger.interval";
  public static final int SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL_DEFAULT = -1; // not trigger

  public static final String SELF_OPTIMIZING_FULL_REWRITE_ALL_FILES = "self-optimizing.full.rewrite-all-files";
  public static final boolean SELF_OPTIMIZING_FULL_REWRITE_ALL_FILES_DEFAULT = true;


  /**
   * deprecated table optimize related properties
   */
  @Deprecated
  public static final String ENABLE_OPTIMIZE = "optimize.enable";

  @Deprecated
  public static final String OPTIMIZE_GROUP = "optimize.group";

  @Deprecated
  public static final String OPTIMIZE_RETRY_NUMBER = "optimize.num-retries";

  @Deprecated
  public static final String OPTIMIZE_MAX_FILE_COUNT = "optimize.max-file-count";

  @Deprecated
  public static final String FULL_OPTIMIZE_TRIGGER_MAX_INTERVAL = "optimize.full.trigger.max-interval";

  @Deprecated
  public static final String MINOR_OPTIMIZE_TRIGGER_MAX_INTERVAL = "optimize.minor.trigger.max-interval";

  @Deprecated
  public static final String MINOR_OPTIMIZE_TRIGGER_DELETE_FILE_COUNT = "optimize.minor.trigger.delete-file-count";

  @Deprecated
  public static final String OPTIMIZE_QUOTA = "optimize.quota";

  /**
   * table clean related properties
   */
  public static final String ENABLE_TABLE_EXPIRE = "table-expire.enabled";
  public static final boolean ENABLE_TABLE_EXPIRE_DEFAULT = true;
  @Deprecated
  public static final String ENABLE_TABLE_EXPIRE_LEGACY = "table-expire.enable";

  public static final String CHANGE_DATA_TTL = "change.data.ttl.minutes";
  public static final long CHANGE_DATA_TTL_DEFAULT = 10080; // 7 Days

  public static final String CHANGE_SNAPSHOT_KEEP_MINUTES = "snapshot.change.keep.minutes";
  public static final long CHANGE_SNAPSHOT_KEEP_MINUTES_DEFAULT = 10080; // 7 Days

  public static final String BASE_SNAPSHOT_KEEP_MINUTES = "snapshot.base.keep.minutes";
  public static final long BASE_SNAPSHOT_KEEP_MINUTES_DEFAULT = 720; // 12 Hours

  public static final String ENABLE_INDEPENDENT_CLEAN = "clean-independent-delete-files.enabled";
  public static final boolean ENABLE_INDEPENDENT_CLEAN_DEFAULT = true;

  public static final String ENABLE_ORPHAN_CLEAN = "clean-orphan-file.enabled";
  public static final boolean ENABLE_ORPHAN_CLEAN_DEFAULT = false;
  @Deprecated
  public static final String ENABLE_ORPHAN_CLEAN_LEGACY = "clean-orphan-file.enable";

  public static final String MIN_ORPHAN_FILE_EXISTING_TIME = "clean-orphan-file.min-existing-time-minutes";
  public static final long MIN_ORPHAN_FILE_EXISTING_TIME_DEFAULT = 2880; // 2 Days

  public static final String ENABLE_TABLE_TRASH = "table-trash.enabled";
  public static final boolean ENABLE_TABLE_TRASH_DEFAULT = false;

  public static final String TABLE_TRASH_CUSTOM_ROOT_LOCATION = "table-trash.custom-root-location";

  public static final String TABLE_TRASH_KEEP_DAYS = "table-trash.keep.days";
  public static final int TABLE_TRASH_KEEP_DAYS_DEFAULT = 7; // 7 Days

  public static final String TABLE_TRASH_FILE_PATTERN = "table-trash.file-pattern";
  public static final String TABLE_TRASH_FILE_PATTERN_DEFAULT = ".+\\.parquet" +
      "|.*snap-[0-9]+-[0-9]+-.+\\.avro" + // snap-1515213806302741636-1-UUID.avro
      "|.*version-hint.text" + // version-hint.text
      "|.*v[0-9]+\\.metadata\\.json" + // v123.metadata.json
      "|.*[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}-m[0-9]+\\.avro"; // UUID-m0.avro

  /**
   * table write related properties
   */
  public static final String BASE_FILE_FORMAT = "base.write.format";
  public static final String BASE_FILE_FORMAT_DEFAULT = "parquet";

  public static final String CHANGE_FILE_FORMAT = "change.write.format";
  public static final String CHANGE_FILE_FORMAT_DEFAULT = "parquet";

  public static final String DEFAULT_FILE_FORMAT = org.apache.iceberg.TableProperties.DEFAULT_FILE_FORMAT;
  public static final String DEFAULT_FILE_FORMAT_DEFAULT =
      org.apache.iceberg.TableProperties.DEFAULT_FILE_FORMAT_DEFAULT;

  public static final String BASE_FILE_INDEX_HASH_BUCKET = "base.file-index.hash-bucket";
  public static final int BASE_FILE_INDEX_HASH_BUCKET_DEFAULT = 4;

  public static final String CHANGE_FILE_INDEX_HASH_BUCKET = "change.file-index.hash-bucket";
  public static final int CHANGE_FILE_INDEX_HASH_BUCKET_DEFAULT = 4;

  public static final String WRITE_TARGET_FILE_SIZE_BYTES =
      org.apache.iceberg.TableProperties.WRITE_TARGET_FILE_SIZE_BYTES;
  public static final long WRITE_TARGET_FILE_SIZE_BYTES_DEFAULT = 134217728; // 128 MB

  public static final String UPSERT_ENABLED = "write.upsert.enabled";
  public static final boolean UPSERT_ENABLED_DEFAULT = false;

  public static final String WRITE_DISTRIBUTION_MODE = org.apache.iceberg.TableProperties.WRITE_DISTRIBUTION_MODE;
  public static final String WRITE_DISTRIBUTION_MODE_NONE =
      org.apache.iceberg.TableProperties.WRITE_DISTRIBUTION_MODE_NONE;
  public static final String WRITE_DISTRIBUTION_MODE_HASH =
      org.apache.iceberg.TableProperties.WRITE_DISTRIBUTION_MODE_HASH;
  public static final String WRITE_DISTRIBUTION_MODE_RANGE =
      org.apache.iceberg.TableProperties.WRITE_DISTRIBUTION_MODE_RANGE;
  public static final String WRITE_DISTRIBUTION_MODE_DEFAULT = WRITE_DISTRIBUTION_MODE_HASH;

  public static final String WRITE_DISTRIBUTION_HASH_MODE = "write.distribution.hash-mode";
  public static final String WRITE_DISTRIBUTION_HASH_PARTITION = "partition-key";
  public static final String WRITE_DISTRIBUTION_HASH_PRIMARY = "primary-key";
  public static final String WRITE_DISTRIBUTION_HASH_PRIMARY_PARTITION = "primary-partition-key";
  public static final String WRITE_DISTRIBUTION_HASH_AUTO = "auto";
  public static final String WRITE_DISTRIBUTION_HASH_MODE_DEFAULT = WRITE_DISTRIBUTION_HASH_AUTO;

  public static final String BASE_REFRESH_INTERVAL = "base.refresh-interval";
  public static final long BASE_REFRESH_INTERVAL_DEFAULT = -1L;

  public static final String SPLIT_SIZE = org.apache.iceberg.TableProperties.SPLIT_SIZE;
  public static final long SPLIT_SIZE_DEFAULT = 134217728; // 128 MB

  public static final String SPLIT_LOOKBACK = org.apache.iceberg.TableProperties.SPLIT_LOOKBACK;
  public static final int SPLIT_LOOKBACK_DEFAULT = 10;

  public static final String SPLIT_OPEN_FILE_COST = org.apache.iceberg.TableProperties.SPLIT_OPEN_FILE_COST;
  public static final long SPLIT_OPEN_FILE_COST_DEFAULT = 4 * 1024 * 1024; // 4MB
  /**
   * log store related properties
   */
  public static final String ENABLE_LOG_STORE = "log-store.enabled";
  public static final boolean ENABLE_LOG_STORE_DEFAULT = false;
  @Deprecated
  public static final String ENABLE_LOG_STORE_LEGACY = "log-store.enable";

  public static final String LOG_STORE_TYPE = "log-store.type";
  public static final String LOG_STORE_STORAGE_TYPE_KAFKA = "kafka";
  public static final String LOG_STORE_STORAGE_TYPE_PULSAR = "pulsar";
  public static final String LOG_STORE_STORAGE_TYPE_DEFAULT = LOG_STORE_STORAGE_TYPE_KAFKA;

  public static final String LOG_STORE_ADDRESS = "log-store.address";

  public static final String LOG_STORE_MESSAGE_TOPIC = "log-store.topic";

  public static final String LOG_STORE_DATA_FORMAT = "log-store.data-format";
  public static final String LOG_STORE_DATA_FORMAT_DEFAULT = "json";

  public static final String LOG_STORE_DATA_VERSION = "log-store.data-version";
  public static final String LOG_STORE_DATA_VERSION_DEFAULT = "v1";

  public static final String LOG_STORE_PROPERTIES_PREFIX = "properties.";

  public static final String OWNER = "owner";

  /**
   * Protected properties which should not be read by user.
   */
  public static final Set<String> READ_PROTECTED_PROPERTIES = new HashSet<>();

  /**
   * Protected properties which should not be written by user.
   */
  public static final Set<String> WRITE_PROTECTED_PROPERTIES = new HashSet<>();

  static {
    READ_PROTECTED_PROPERTIES.add(TableProperties.BASE_TABLE_MAX_TRANSACTION_ID);
    READ_PROTECTED_PROPERTIES.add(TableProperties.PARTITION_OPTIMIZED_SEQUENCE);
    READ_PROTECTED_PROPERTIES.add(TableProperties.LOCATION);
    READ_PROTECTED_PROPERTIES.add(TableProperties.TABLE_PARTITION_PROPERTIES);
    READ_PROTECTED_PROPERTIES.add(DEFAULT_NAME_MAPPING);
    READ_PROTECTED_PROPERTIES.add(FORMAT_VERSION);
    READ_PROTECTED_PROPERTIES.add("flink.max-continuous-empty-commits");


    WRITE_PROTECTED_PROPERTIES.add(TableProperties.BASE_TABLE_MAX_TRANSACTION_ID);
    WRITE_PROTECTED_PROPERTIES.add(TableProperties.PARTITION_OPTIMIZED_SEQUENCE);
    WRITE_PROTECTED_PROPERTIES.add(TableProperties.LOCATION);
    WRITE_PROTECTED_PROPERTIES.add(TableProperties.TABLE_PARTITION_PROPERTIES);
    WRITE_PROTECTED_PROPERTIES.add(DEFAULT_NAME_MAPPING);
    WRITE_PROTECTED_PROPERTIES.add(FORMAT_VERSION);
    WRITE_PROTECTED_PROPERTIES.add(WATERMARK_TABLE);
    WRITE_PROTECTED_PROPERTIES.add(WATERMARK_BASE_STORE);
    WRITE_PROTECTED_PROPERTIES.add("flink.max-continuous-empty-commits");
  }
}
