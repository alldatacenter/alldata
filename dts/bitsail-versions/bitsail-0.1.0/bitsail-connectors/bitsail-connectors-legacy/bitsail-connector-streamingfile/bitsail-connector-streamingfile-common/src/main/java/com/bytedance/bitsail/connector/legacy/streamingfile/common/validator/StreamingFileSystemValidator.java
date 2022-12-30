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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.validator;

import com.google.common.collect.ImmutableSet;
import org.apache.flink.annotation.Internal;
import org.apache.flink.table.descriptors.ConnectorDescriptorValidator;
import org.apache.flink.table.descriptors.DescriptorProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.apache.flink.table.descriptors.DescriptorProperties.noValidation;
import static org.apache.flink.table.descriptors.FileSystemValidator.CONNECTOR_PATH;
import static org.apache.flink.table.descriptors.FileSystemValidator.CONNECTOR_TYPE_VALUE;
import static org.apache.flink.table.descriptors.FormatDescriptorValidator.FORMAT_TYPE;
import static org.apache.flink.table.descriptors.OldCsvValidator.FORMAT_TYPE_VALUE;

/**
 * The validator for {@link StreamingFileSystemValidator}.
 */
@Internal
public class StreamingFileSystemValidator extends ConnectorDescriptorValidator {
  public static final String CONNECTOR_TYPE_VALUE_FILE_SYSTEM = "filesystem";
  public static final String CONNECTOR_NAME_VALUE_FILE_SYSTEM_SINK = "hdfs_file_sink";
  public static final String CONNECTOR_TYPE_VALUE_EMR_FILE_SYSTEM = "emr_filesystem";

  /**
   * Job Plugin configuration perfix.
   */
  public static final String JOB_READER_PREFIX = "job.reader.";
  public static final String JOB_WRITER_PREFIX = "job.writer.";
  public static final String JOB_COMMON_PREFIX = "job.common.";

  public static final String INITIAL_JOB_COMMIT_TIME_ENABLED = "initial_job_commit_time_enabled";
  public static final String INITIAL_JOB_COMMIT_TIME = "initial_job_commit_time";
  public static final String INITIAL_JOB_COMMIT_TIME_MAX_LOOKBACK_DAY = "initial_job_commit_time_max_lookback_day";
  public static final String INITIAL_JOB_COMMIT_TIME_MAX_LOOKBACK_DEFAULT = "7";

  public static final String METRICS_TYPE = "metrics_type";
  public static final String METRICS_TYPE_BYTEDANCE = "metrics_type.bytedance";
  public static final String METRICS_TYPE_NOP = "metrics_type.nop";

  public static final String EVENT_TIME = "event_time";
  public static final String EVENT_TIME_FIELDS = "event_time_fields";
  public static final String EVENT_TIME_PATTERN = "event_time_pattern";
  public static final String EVENT_TIME_CONJECTURE_ENABLED = "event_time_conjecture_enabled";
  public static final String STRICT_ARCHIVE_MODE = "strict_archive_mode";

  public static final String PARTITION_STRATEGY = "partition_strategy";
  public static final String PARTITION_STRATEGY_PARTITION_FIRST = "partition_first";
  public static final String PARTITION_STRATEGY_PARTITION_LAST = "partition_last";
  public static final String COMMIT_PARTITION_SIZE = "commit_partition_size";
  public static final String DEFAULT_COMMIT_PARTITION_SIZE = "10";
  public static final String COMMIT_PARTITION_TIME = "commit_partition_time";
  public static final String DEFAULT_COMMIT_PARTITION_TIME = "30";

  public static final String CUSTOM_EXTRACTOR_CLASSPATH = "custom_extractor_classpath";
  public static final String EVENT_TIME_SAFE_MODE_ENABLED = "event_time.safe_mode.enabled";
  public static final String EVENT_TIME_SAFE_MODE_DEFAULT = Boolean.TRUE.toString();

  public static final String EVENT_TIME_TAG_DURATION = "event_time.tag_duration";
  public static final long EVENT_TIME_TAG_DURATION_DEFAULT = 15 * 60 * 1000L;
  public static final String EVENT_TIME_TAG_MID_NIGHT_DURATION = "event_time.mid_tag_duration";
  public static final String EVENT_TIME_TAG_MID_NIGHT_DURATION_DEFAULT = String.valueOf(15 * 60 * 1000);
  public static final String EVENT_TIME_THRESHOLD = "event_time_threshold";
  public static final String EVENT_TIME_THRESHOLD_VALUE_DEFAULT = "7200000";
  public static final String IGNORE_MID_NIGHT_FAST = "ignore_midnight_fast";
  public static final String DUMP_SKIP_BYTES = "dump.skip_bytes_length";
  public static final String DUMP_SKIP_BYTES_DEFAULT = "0";

  public static final String CHECK_INTERVAL = "check_interval";
  public static final String CHECK_INTERVAL_DEFAULT = "30000";

  public static final String CHECKPOINT_ENABLE = "checkpoint_enable";
  public static final String CHECKPOINT_ENABLE_DEFAULT = "true";
  public static final String CHECKPOINT_INTERVAL = "checkpoint_interval";
  public static final String CHECKPOINT_INTERVAL_DEFAULT = "900000";
  public static final String CHECKPOINT_TIMEOUT = "checkpoint_timeout";
  public static final String CHECKPOINT_TIMEOUT_DEFAULT = "600000";
  public static final String CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY = "checkpoint.tolerable_failure_number";
  public static final Integer CHECKPOINT_TOLERABLE_FAILURE_NUMBER_DEFAULT = 10;

  public static final String GLOBAL_PARALLELISM_NUM = "global_parallelism_num";
  public static final String JOB_WRITER_PARALLELISM_NUM = JOB_WRITER_PREFIX + "parallelism_num";
  public static final String JOB_READER_PARALLELISM_NUM = JOB_READER_PREFIX + "parallelism_num";
  public static final String MAX_PARALLELISM_NUM = "max_parallelism_num";

  public static final String ENABLE_GLOBAL_TRAFFIC_COMPENSATION = "enable_global_traffic_compensation";

  public static final String SOURCE_OPERATOR_DESC = "source_operator.desc";
  public static final String SINK_OPERATOR_DESC = "sink_operator.desc";

  public static final String MULTI_SOURCE_ENABLED = "multi_source_enable";
  public static final String MULTI_SOURCE_PARALLELISM = "multi_source_parallelism";
  public static final String MULTI_SOURCE_MARKER = "multi_source_marker";

  public static final String FLINK_SQL = "flink_sql";

  public static final String PARTITION_INFOS = "partition_infos";
  public static final String PARTITION_KEYS = "partition.keys";
  public static final String PARTITION_DEFAULT_NAME = "partition.default-name";
  public static final String PARTITION_LOOKBACK_DAY = "partition.lookback_day";
  public static final String PARTITION_LOOKBACK_HOUR = "partition.lookback_hour";
  public static final String PARTITION_LOOKBACK_DAY_DEFAULT = "2";
  public static final String PARTITION_LOOKBACK_HOUR_DEFAULT = "6";

  /**
   * kakfa discover partitions config;
   */

  public static final String COUNT_MODE = "count_mode";
  public static final String COUNT_MODE_RECORD_THRESHOLD = "count_mode_record_threshold";
  public static final String DEFAULT_COUNT_MODE_RECORD_THRESHOLD = "10000";
  public static final String COUNT_MODE_RUN_TIME_THRESHOLD = "count_mode_run_time_threshold";
  public static final String DEFAULT_COUNT_MODE_RUN_TIME_THRESHOLD = "600";

  /**
   * Time partition info
   */
  public static final String PARTITION_DATE_FORMAT = "partition.date_format";
  public static final String PARTITION_HOUR_FORMAT = "partition.hour_format";

  public static final String PARTITION_DATE_FORMAT_DEFAULT = "yyyyMMdd";
  public static final String PARTITION_HOUR_FORMAT_DEFAULT = "HH";

  public static final String PARTITION_FREQUENCY = "partition.frequency";
  public static final String PARTITION_FREQUENCY_VALUE_NONE = "none";
  public static final String PARTITION_FREQUENCY_VALUE_DAILY = "daily";
  public static final String PARTITION_FREQUENCY_VALUE_HOURLY = "hourly";

  public static final String HIVE_FORMAT_TYPE_VALUE = "hive";
  public static final String HIVE_DB_NAME = "db_name";
  public static final String HIVE_TABLE_NAME = "table_name";
  public static final String HIVE_METASTORE_CONSUL = "metastore_consul";
  public static final String HIVE_METASTORE_NAMESPACE = "metastore_namespace";
  public static final String HIVE_METASTORE_PROPERTIES = "metastore_properties";
  public static final String HIVE_OUTPUTFORMAT_PROPERTIES = "hive.outputformat.properties";
  public static final String HIVE_VERSION = "hive.version";
  public static final String EMR_HIVE_CONF = "emr_hive_conf";

  public static final String HDFS_FORMAT_TYPE_VALUE = "hdfs";
  public static final String HDFS_REPLICATION = "hdfs.replication";
  public static final String HDFS_REPLICATION_DEFAULT = "3";
  public static final String EMR_HADOOP_CONF = "emr_hadoop_conf";

  public static final String HDFS_OVERWRITE = "hdfs.overwrite";
  public static final String HDFS_OVERWRITE_DEFAULT = "false";

  public static final String HDFS_COMPRESSION_CONFIG = "hdfs.compression.config";
  public static final String HDFS_COMPRESSION_CONFIG_DEFAULT = "true";
  public static final String HDFS_COMPRESSION_CODEC = "hdfs.compression_codec";
  public static final String HDFS_COMPRESSION_CODEC_NONE = "None";
  public static final String HDFS_COMPRESSION_CODEC_ZSTD = "zstd";

  public static final String FILE_NAME_STATE_TYPE = "dump.file_name_state.type";
  public static final String FILE_NAME_STATE_DEFAULT_TYPE = "file_name";
  public static final String DUMP_DELETE_TMP_PATH_BEFORE_CREATE = "dump.delete_tmp_path_before_create";
  public static final String DUMP_DEFAULT_TIMEZONE_REGION = "dump.timezone_region";

  public static final String DUMP_REMAIN_DIRECTORY_NUM = "dump.remain_directory_num";

  public static final String HDFS_DUMP_TYPE = "hdfs.dump_type";
  public static final String HDFS_DUMP_TYPE_TEXT = "hdfs.dump_type.text";
  public static final String HDFS_DUMP_TYPE_JSON = "hdfs.dump_type.json";
  public static final String HDFS_DUMP_TYPE_MSGPACK = "hdfs.dump_type.msgpack";
  public static final String HDFS_DUMP_TYPE_BINLOG = "hdfs.dump_type.binlog";
  public static final String HDFS_DUMP_TYPE_BINARY = "hdfs.dump_type.binary";
  public static final String HDFS_DUMP_TYPE_PB = "hdfs.dump_type.pb";
  public static final String HDFS_DUMP_TYPE_TFRECORD = "hdfs.dump_type.tfrecord";
  public static final String HDFS_DUMP_TYPE_DEBEZIUM_JSON = "hdfs.dump_type.debezium_json";
  public static final String HDFS_ADVANCED_ARGS_PREFIX = "dump.hdfs.advanced.";


  public static final String HDFS_DUMP_TYPE_PB_FEATURE_PREFIX = "hdfs.dump_type.pb.feature.";
  public static final String CASE_INSENSITIVE = "case_insensitive";
  public static final String JSON_SERIALIZER_FEATURES = "json_serializer_features";

  public static final String CONVERT_ERROR_COLUMN_AS_NULL = "convert_error_column_as_null";
  public static final String USE_ARRAY_MAP_COLUMN = "use_array_map_column";
  public static final String RAW_COLUMN = "raw_column";
  public static final String LAZY_PARSE = "lazy_parse";
  public static final String USE_STRING_TEXT = "use_string_text";
  public static final String NULL_STRING_AS_NULL = "null_string_as_null";

  public static final String DEFAULT_TIME_STRATEGY = "dump.default_time_strategy";
  public static final String DEFAULT_TIME_STRATEGY_ON_SYSTEM_TIME = "dump.default_time_strategy.on_system_time";
  public static final String DEFAULT_TIME_STRATEGY_ON_COMMIT_TIME = "dump.default_time_strategy.on_commit_time";

  public static final String DUMP_NO_TRAFFIC_INTERVAL = "dump.no_traffic_interval";
  public static final String DUMP_DBUS_CHANNEL_CHECK = "dump.dbus_channel_check";
  public static final String DUMP_DBUS_CHANNEL_DB_NAME = "dump.dbus_channel_db_name";
  public static final String DUMP_DBUS_CHANNEL_DB_REGION = "dump.dbus_channel_db_region";

  public static final String DUMP_NO_TRAFFIC_INTERVAL_DEFAULT = "-1";
  public static final String DUMP_DBUS_CHANNEL_CHECK_DEFAULT = "false";

  public static final String SOURCE_SCHEMA = "source_schema";
  public static final String SINK_SCHEMA = "sink_schema";

  public static final String RESTART_STRATEGY_ATTEMPTS_RATIO = "restart-strategy.task_attempts_ratio";
  public static final String RESTART_STRATEGY_RESTART_INTERVAL = "restart-strategy.task_restart_interval";
  public static final String RESTART_STRATEGY_RESTART_DELAY = "restart-strategy.task_restart_delay";

  public static final String RESTART_STRATEGY_ATTEMPTS_RATIO_DEFAULT = "0.02";
  public static final String RESTART_STRATEGY_RESTART_INTERVAL_DEFAULT = "60";
  public static final String RESTART_STRATEGY_RESTART_DELAY_DEFAULT = "1";

  /**
   * rolling policy
   */
  public static final String DIRTY_ROLLING_POLICY_ENABLED = "dirty.rolling_policy.enabled";
  public static final String DIRTY_ROLLING_POLICY_INTERVAL = "dirty.rolling_policy.interval";
  public static final String DIRTY_ROLLING_POLICY_INTERVAL_DEFAULT = String.valueOf(60 * 60 * 1000L);
  public static final String DIRTY_ROLLING_POLICY_SIZE = "dirty.rolling_policy.size";
  public static final String DIRTY_ROLLING_POLICY_SIZE_DEFAULT = String.valueOf(512 * 1024 * 1024L);
  public static final String DIRTY_COLLECTOR_SIZE = "dirty.collector_size";
  public static final String DIRTY_COLLECTOR_SIZE_DEFAULT = "50";
  public static final int DIRTY_COLLECTOR_SIZE_MAX = -1;

  public static final String DIRTY_SAMPLE_THRESHOLD = "dirty.sample_threshold";
  public static final String DIRTY_SAMPLE_THRESHOLD_DEFAULT = "1";

  public static final String DIRTY_COLLECTOR_TYPE = "dirty_collector.type";
  public static final String DIRTY_COLLECTOR_TYPE_SINGLE_FILE_HDFS = "dirty_collector.type.single_file_hdfs";
  public static final String DIRTY_COLLECTOR_TYPE_MULTI_FILE_HDFS = "dirty_collector.type.multi_file_hdfs";
  public static final String DIRTY_COLLECTOR_TYPE_NOP = "dirty_collector.type.nop";

  public static final String ROLLING_INACTIVITY_INTERVAL = "rolling.inactivity_interval";
  public static final String ROLLING_MAX_PART_SIZE = "rolling.max_part_size";
  public static final String ROLLING_ROLLOVER_INTERVAL = "rolling.rollover_interval";
  public static final long ROLLING_INACTIVITY_INTERVAL_DEFAULT = 15 * 60 * 1000L;
  public static final long ROLLING_ROLLOVER_INTERVAL_DEFAULT = 30 * 60 * 1000L;
  public static final long ROLLING_MAX_PART_SIZE_DEFAULT = 10 * 1024 * 1024 * 1024L;
  public static final double ROLLING_POLICY_INTERVAL_DEFAULT_RATIO = 1.5d;

  public static final String DIRTY_RECORD_SAMPLE_THRESHOLD = "dirty_record_sample_threshold";
  public static final String DIRTY_RECORD_SAMPLE_THRESHOLD_DEFAULT = "1000";
  public static final String DIRTY_RECORD_SKIP_ENABLED = "dirty_record_skip_enabled";
  public static final String DIRTY_RECORD_SKIP_ENABLED_DEFAULT = "false";
  public static final int DIRTY_RECORD_LOG_LENGTH = 500;

  public static final String SCHEMA_DISCOVER_ENABLED = "schema.discovery_enabled";
  public static final String SCHEMA_DISCOVER_INTERVAL = "schema.discovery_interval";
  public static final String SCHEMA_DISCOVER_INTERVAL_DEFAULT = String.valueOf(5 * 60 * 1000);
  public static final String SCHEMA_EXPIRE_INTERVAL = "schema.expire_interval";
  public static final String SCHEMA_EXPIRE_INTERVAL_DEFAULT = String.valueOf(15 * 60 * 1000);
  public static final String SCHEMA_DOMAIN = "schema.domain";

  public static final String MQ_OFFSET_CHECK = "mq.offset.check";
  public static final String MQ_OFFSET_CHECK_DEFAULT = "true";

  public static final String JOB_ID = "job_id";
  public static final Long JOB_ID_VALUE_DEFAULT = 111111111L;
  public static final String JOB_NAME = "job_name";
  public static final String JOB_RUN_MODE = "job_run_mode";
  public static final String JOB_RUN_MODE_STREAMING = "streaming";
  public static final String JOB_RUN_MODE_BATCH = "batch";
  public static final String JOB_RUN_REGION = "job_run_region";
  public static final String JOB_CONFIG_SKIP = "job.config_skip";
  public static final String JOB_HOST_IPS_MAPPING = "job.host_ips_mapping";

  public static final String FILE_STATE_TYPE = "file.state.type";
  public static final String FILE_STATE_TYPE_ABASE = "file.state.type.abase";

  public static final String PROTO_DESCRIPTOR = "proto.descriptor";
  public static final String PROTO_CLASS_NAME = "proto.class_name";

  public static final String DUMP_DIRECTORY_FREQUENCY = "dump.directory_frequency";
  public static final String DUMP_DIRECTORY_FREQUENCY_DAY = "dump.directory_frequency.day";
  public static final String DUMP_DIRECTORY_FREQUENCY_HOUR = "dump.directory_frequency.hour";

  public static final String DUMP_WRAPPER_SNAPSHOT_SUCCESS_RATE = "dump.wrapper_snapshot_success_rate";
  public static final String DUMP_WRAPPER_ASYNC_SNAPSHOT_SUCCESS_RATE = "dump.wrapper_async_snapshot_success_rate";
  public static final String DUMP_WRAPPER_TASK_ID = "dump.wrapper_task_id";
  public static final int DUMP_DEFAULT_WRAPPER_TASK_ID = -1;
  public static final String DUMP_WRAPPER_SNAPSHOT_SUCCESS_RATE_DEFAULT = "1";
  public static final String DUMP_WRAPPER_SNAPSHOT_ENABLE = "dump.wrapper_snapshot_enable";

  public static final String HDFS_ADD_EMPTY_ON_DYNAMIC_ENABLE = "hdfs.add_empty_on_dynamic.enable";

  public static final String DEFAULT_PARTITION = "__bitsail_dump_default_partition__";
  public static final String HDFS_SPLIT_PARTITION_MAPPINGS = "hdfs.split.partition_mappings";
  /**
   * Support dynamic hdfs partition type
   */
  public static final Set<String> HDFS_SUPPORT_DYNAMIC_PARTITIONS = ImmutableSet.of(
      HDFS_DUMP_TYPE_JSON,
      HDFS_DUMP_TYPE_MSGPACK, HDFS_DUMP_TYPE_BINARY);

  public static final String DUMP_DIRTY_DIR = "_DUMP_DIRTY_DIR";
  public static final String DUMP_TMP_SUFFIX = "_DUMP_TEMPORARY";


  public static final Set<String> DUMP_DEFAULT_KEYWORD = ImmutableSet.of(
      DUMP_DIRTY_DIR,
      DUMP_TMP_SUFFIX
  );

  @Override
  public void validate(DescriptorProperties properties) {
    super.validate(properties);
    properties.validateValue(CONNECTOR_TYPE, CONNECTOR_TYPE_VALUE, false);
    properties.validateBoolean(EVENT_TIME, true);
    properties.validateLong(EVENT_TIME_THRESHOLD, true);
    properties.validateLong(CHECK_INTERVAL, true);
    properties.validateString(PARTITION_KEYS, true, 0);
    properties.validateString(PARTITION_DEFAULT_NAME, true, 0);

    validatePartitionFrequency(properties);
    validateFormatType(properties);
  }

  private void validatePartitionFrequency(DescriptorProperties properties) {
    final Map<String, Consumer<String>> frequencyValidation = new HashMap<>();
    frequencyValidation.put(PARTITION_FREQUENCY_VALUE_NONE, noValidation());
    frequencyValidation.put(PARTITION_FREQUENCY_VALUE_DAILY, noValidation());
    frequencyValidation.put(PARTITION_FREQUENCY_VALUE_HOURLY, noValidation());
    properties.validateEnum(PARTITION_FREQUENCY, true, frequencyValidation);
  }

  private void validateFormatType(DescriptorProperties properties) {
    final Map<String, Consumer<String>> formatValidation = new HashMap<>();
    formatValidation.put(
        FORMAT_TYPE_VALUE,
        key -> properties.validateString(CONNECTOR_PATH, false, 1));
    formatValidation.put(
        HDFS_FORMAT_TYPE_VALUE,
        key -> properties.validateString(CONNECTOR_PATH, false, 1));
    formatValidation.put(
        HIVE_FORMAT_TYPE_VALUE,
        key -> validateHiveFormatType(properties));
    properties.validateEnum(FORMAT_TYPE, false, formatValidation);
  }

  private void validateHiveFormatType(DescriptorProperties properties) {
    properties.validateString(HIVE_DB_NAME, false, 1);
    properties.validateString(HIVE_TABLE_NAME, false, 1);
    properties.validateString(HIVE_METASTORE_CONSUL, false, 1);
    properties.validateString(HIVE_VERSION, true, 1);
  }
}
