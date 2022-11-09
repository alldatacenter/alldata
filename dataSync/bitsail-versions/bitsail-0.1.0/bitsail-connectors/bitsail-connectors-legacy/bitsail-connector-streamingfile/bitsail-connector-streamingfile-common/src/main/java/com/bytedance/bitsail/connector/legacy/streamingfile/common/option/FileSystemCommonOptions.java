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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.option;

import com.bytedance.bitsail.common.option.ConfigOption;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

/**
 * Created 2022/8/16
 */
public interface FileSystemCommonOptions {

  ConfigOption<Integer> DUMP_SKIP_BYTES =
      key(WRITER_PREFIX + "dump.skip_bytes_length")
          .defaultValue(0);

  ConfigOption<String> DUMP_FORMAT_TYPE =
      key(WRITER_PREFIX + "dump.format.type")
          .noDefaultValue(String.class);

  ConfigOption<Long> CHECK_INTERVAL =
      key(WRITER_PREFIX + "check_interval")
          .defaultValue(30000L);

  ConfigOption<String> DUMP_DEFAULT_TIMEZONE_REGION =
      key(WRITER_PREFIX + "dump.timezone_region")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> MQ_OFFSET_CHECK =
      key(WRITER_PREFIX + "mq.offset.check")
          .defaultValue(true);

  ConfigOption<Map<String, String>> HDFS_ADVANCED_ARGS =
      key(WRITER_PREFIX + "dump.hdfs.advanced")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });

  ConfigOption<String> CONNECTOR_PATH =
      key(WRITER_PREFIX + "dump.output_dir")
          .noDefaultValue(String.class);

  interface CommitOptions {
    ConfigOption<Boolean> INITIAL_JOB_COMMIT_TIME_ENABLED =
        key(WRITER_PREFIX + "initial_job_commit_time_enabled")
            .defaultValue(false);

    ConfigOption<Long> INITIAL_JOB_COMMIT_TIME =
        key(WRITER_PREFIX + "initial_job_commit_time")
            .defaultValue(Long.MIN_VALUE);

    ConfigOption<Long> INITIAL_JOB_COMMIT_TIME_MAX_LOOKBACK_DAY =
        key(WRITER_PREFIX + "initial_job_commit_time_max_lookback_day")
            .defaultValue(7L);

    ConfigOption<String> DEFAULT_TIME_STRATEGY =
        key(WRITER_PREFIX + "dump.default_time_strategy")
            .noDefaultValue(String.class);

    /**
     * strategy for adding partitions, optional choice:
     * 1. partition_last: add partition after all data dumped
     * 2. partition_first: add partition first and then writer data
     */
    ConfigOption<String> COMMIT_PARTITION_STRATEGY =
        key(WRITER_PREFIX + "partition_strategy")
            .defaultValue("partition_last");

    /**
     * commit partition threshold
     */
    ConfigOption<Integer> COMMIT_PARTITION_SIZE =
        key(WRITER_PREFIX + "commit_partition_size")
            .defaultValue(10);

    /**
     * commit partition threshold (unit: second)
     */
    ConfigOption<Integer> COMMIT_PARTITION_TIME =
        key(WRITER_PREFIX + "commit_partition_time")
            .defaultValue(30);

    /**
     * rolling policy interval
     */
    ConfigOption<Long> ROLLING_INACTIVITY_INTERVAL =
        key(WRITER_PREFIX + "rolling.inactivity_interval")
            .noDefaultValue(Long.class);

    /**
     * rolling policy size
     */
    ConfigOption<Long> ROLLING_MAX_PART_SIZE =
        key(WRITER_PREFIX + "rolling.max_part_size")
            .noDefaultValue(Long.class);

    ConfigOption<String> DUMP_DIRECTORY_FREQUENCY =
        key(WRITER_PREFIX + "dump.directory_frequency")
            .defaultValue("dump.directory_frequency.day");

    ConfigOption<Boolean> HDFS_ADD_EMPTY_ON_DYNAMIC_ENABLE =
        key(WRITER_PREFIX + "hdfs.add_empty_on_dynamic.enable")
            .defaultValue(false);

    ConfigOption<String> HDFS_SPLIT_PARTITION_MAPPINGS =
        key(WRITER_PREFIX + "hdfs.split.partition_mappings")
            .noDefaultValue(String.class);
  }

  interface SnapshotOptions {
    ConfigOption<Double> DUMP_WRAPPER_SNAPSHOT_SUCCESS_RATE =
        key(WRITER_PREFIX + "dump.wrapper_snapshot_success_rate")
            .defaultValue(1.0);

    ConfigOption<Double> DUMP_WRAPPER_ASYNC_SNAPSHOT_SUCCESS_RATE =
        key(WRITER_PREFIX + "dump.wrapper_async_snapshot_success_rate")
            .defaultValue(1.0);

    ConfigOption<Integer> DUMP_WRAPPER_TASK_ID =
        key(WRITER_PREFIX + "dump.wrapper_task_id")
            .defaultValue(-1);

    ConfigOption<Boolean> DUMP_WRAPPER_SNAPSHOT_ENABLE =
        key(WRITER_PREFIX + "dump.wrapper_snapshot_enable")
            .defaultValue(false);
  }

  interface ArchiveOptions {
    /**
     * user defined extractor class
     */
    ConfigOption<String> CUSTOM_EXTRACTOR_CLASSPATH =
        key(WRITER_PREFIX + "custom_extractor_classpath")
            .noDefaultValue(String.class);

    ConfigOption<Boolean> STRICT_ARCHIVE_MODE =
        key(WRITER_PREFIX + "strict_archive_mode")
            .defaultValue(false);

    ConfigOption<Boolean> IGNORE_MID_NIGHT_FAST =
        key(WRITER_PREFIX + "ignore_midnight_fast")
            .defaultValue(false);

    ConfigOption<Boolean> ENABLE_EVENT_TIME =
        key(WRITER_PREFIX + "enable_event_time")
            .defaultValue(false);

    ConfigOption<String> EVENT_TIME_FIELDS =
        key(WRITER_PREFIX + "event_time_fields")
            .noDefaultValue(String.class);

    ConfigOption<String> EVENT_TIME_PATTERN =
        key(WRITER_PREFIX + "event_time_pattern")
            .noDefaultValue(String.class);

    ConfigOption<Boolean> EVENT_TIME_CONJECTURE_ENABLED =
        key(WRITER_PREFIX + "event_time_conjecture_enabled")
            .defaultValue(false);

    ConfigOption<Boolean> EVENT_TIME_SAFE_MODE_ENABLED =
        key(WRITER_PREFIX + "event_time.safe_mode.enabled")
            .defaultValue(true);

    ConfigOption<Long> EVENT_TIME_TAG_DURATION =
        key(WRITER_PREFIX + "event_time.tag_duration")
            .noDefaultValue(Long.class);

    ConfigOption<Long> EVENT_TIME_TAG_MID_NIGHT_DURATION =
        key(WRITER_PREFIX + "event_time.mid_tag_duration")
            .defaultValue(15 * 60 * 1000L);

    ConfigOption<Long> EVENT_TIME_THRESHOLD =
        key(WRITER_PREFIX + "event_time_threshold")
            .defaultValue(7200000L);
  }

  interface PartitionOptions {
    ConfigOption<String> PARTITION_INFOS =
        key(WRITER_PREFIX + "partition_infos")
            .noDefaultValue(String.class);

    ConfigOption<String> PARTITION_KEYS =
        key(WRITER_PREFIX + "partition.keys")
            .noDefaultValue(String.class);

    ConfigOption<String> PARTITION_DEFAULT_NAME =
        key(WRITER_PREFIX + "partition.default-name")
            .defaultValue("default");

    ConfigOption<Long> PARTITION_LOOKBACK_DAY =
        key(WRITER_PREFIX + "partition.lookback_day")
            .defaultValue(2L);

    ConfigOption<Long> PARTITION_LOOKBACK_HOUR =
        key(WRITER_PREFIX + "partition.lookback_hour")
            .defaultValue(6L);

    ConfigOption<String> PARTITION_DATE_FORMAT =
        key(WRITER_PREFIX + "partition.date_format")
            .defaultValue("yyyyMMdd");

    ConfigOption<String> PARTITION_HOUR_FORMAT =
        key(WRITER_PREFIX + "partition.hour_format")
            .defaultValue("HH");
  }

  interface FileStateOptions {

    ConfigOption<String> FILE_STATE_TYPE =
        key(WRITER_PREFIX + "file.state.type")
            .noDefaultValue(String.class);

    ConfigOption<String> FILE_NAME_STATE_TYPE =
        key(WRITER_PREFIX + "dump.file_name_state.type")
            .defaultValue("file_name");

    ConfigOption<Integer> DUMP_REMAIN_DIRECTORY_NUM =
        key(WRITER_PREFIX + "dump.remain_directory_num")
            .defaultValue(100);
  }
}
