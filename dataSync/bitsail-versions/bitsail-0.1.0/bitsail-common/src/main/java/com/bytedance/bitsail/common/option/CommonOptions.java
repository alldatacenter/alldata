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

package com.bytedance.bitsail.common.option;

import com.bytedance.bitsail.common.annotation.Essential;

import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;

/**
 * The set of configuration options relating to common config.
 */
public interface CommonOptions {
  String JOB_COMMON = "job.common";
  String COMMON_PREFIX = JOB_COMMON + ".";

  ConfigOption<String> STATIC_LIB_CONF_FILE =
      key(COMMON_PREFIX + "static_lib_conf_file")
          .defaultValue("static_libs/static_lib_conf.json");

  ConfigOption<String> STATIC_LIB_DIR =
      key(COMMON_PREFIX + "static_lib_dir")
          .defaultValue("static_libs");

  ConfigOption<String> JOB_PLUGIN_LIB_PATH =
      key(COMMON_PREFIX + "job_plugin_lib_dir")
          .defaultValue("connectors");

  ConfigOption<String> JOB_PLUGIN_CONF_PATH =
      key(COMMON_PREFIX + "job_plugin_conf_dir")
          .defaultValue("connectors/mapping");

  ConfigOption<String> JOB_PLUGIN_ROOT_PATH =
      key(COMMON_PREFIX + "job_plugin_root_path")
          .noDefaultValue(String.class);

  ConfigOption<Integer> GLOBAL_PARALLELISM_NUM =
      key(COMMON_PREFIX + "global_parallelism_num")
          .defaultValue(-1);

  /**
   * an optional option to store user-defined common parameters
   * key -> user-defined parameter key, value -> user-defined parameter value
   */
  ConfigOption<Map<String, Object>> OPTIONAL =
      key(COMMON_PREFIX + "optional")
          .onlyReference(new TypeReference<Map<String, Object>>() {
          });

  /**
   * BATCH, STREAMING
   */
  ConfigOption<String> JOB_TYPE =
      key(COMMON_PREFIX + "job_type")
          .defaultValue("batch");

  @Essential
  ConfigOption<Long> JOB_ID =
      key(COMMON_PREFIX + "job_id")
          .noDefaultValue(Long.class);

  @Essential
  ConfigOption<String> JOB_NAME =
      key(COMMON_PREFIX + "job_name")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> JOB_CONFIG_SKIP =
      key(COMMON_PREFIX + "job.config_skip")
          .defaultValue(true);

  ConfigOption<String> DEPLOY_TYPE =
      key(COMMON_PREFIX + "deploy_type")
          .defaultValue("yarn");

  ConfigOption<Boolean> SHOW_JOB_PROGRESS =
      key(COMMON_PREFIX + "show_job_progress")
          .defaultValue(false);

  ConfigOption<String> JOB_PROGRESS_TYPE =
      key(COMMON_PREFIX + "job_progress_type")
          .defaultValue("no_op");

  /**
   * Job instance id.
   */
  @Essential
  ConfigOption<Long> INSTANCE_ID =
      key(COMMON_PREFIX + "instance_id")
          .noDefaultValue(Long.class);

  ConfigOption<String> INTERNAL_INSTANCE_ID =
      key(COMMON_PREFIX + "internal_instance_id")
          .noDefaultValue(String.class);

  ConfigOption<Long> SLEEP_TIME =
      key(COMMON_PREFIX + "sleep_time")
          .noDefaultValue(Long.class);

  ConfigOption<String> USER_NAME =
      key(COMMON_PREFIX + "user_name")
          .noDefaultValue(String.class);

  @Essential
  ConfigOption<Long> READER_TRANSPORT_CHANNEL_SPEED_BYTE =
      key(COMMON_PREFIX + "reader_transport_channel_speed_byte")
          .defaultValue(-1L);

  @Essential
  ConfigOption<Long> READER_TRANSPORT_CHANNEL_SPEED_RECORD =
      key(COMMON_PREFIX + "reader_transport_channel_speed_record")
          .defaultValue(-1L);

  @Essential
  ConfigOption<Long> WRITER_TRANSPORT_CHANNEL_SPEED_BYTE =
      key(COMMON_PREFIX + "writer_transport_channel_speed_byte")
          .defaultValue(-1L);

  @Essential
  ConfigOption<Long> WRITER_TRANSPORT_CHANNEL_SPEED_RECORD =
      key(COMMON_PREFIX + "writer_transport_channel_speed_record")
          .defaultValue(-1L);

  ConfigOption<String> METRICS_REPORTER_TYPE =
      key(COMMON_PREFIX + "metrics_reporter_type")
          .defaultValue("log");

  ConfigOption<String> MESSENGER_COLLECTOR_TYPE =
      key(COMMON_PREFIX + "messenger_collector_type")
          .defaultValue("flink");

  /**
   * messenger
   */
  ConfigOption<String> MESSENGER_TYPE =
      key(COMMON_PREFIX + "messenger_type")
          .defaultValue("flink");

  /**
   * max number of dirty records that can be uploaded to somewhere
   */
  ConfigOption<Integer> MAX_DIRTY_RECORDS_STORED_NUM =
      key(COMMON_PREFIX + "max_dirty_records_stored_num")
          .defaultValue(50);

  /**
   * Threshold for the dirty collectors.
   * {@value -1} means disable this feature.
   */
  ConfigOption<Integer> DIRTY_RECORDS_COUNT_THRESHOLD =
      key(COMMON_PREFIX + "dirty_records_count_threshold")
          .defaultValue(0);

  /**
   * Percent threshold for the dirty collector.
   * {@value -1} means disable this feature.
   */
  ConfigOption<Double> DIRTY_RECORDS_PERCENTAGE_THRESHOLD =
      key(COMMON_PREFIX + "dirty_records_percentage_threshold")
          .defaultValue(0.00d);

  ConfigOption<Long> LOW_VOLUME_TEST_COUNT_THRESHOLD =
      key(COMMON_PREFIX + "low_volume_test_count_threshold")
          .defaultValue(-1L);

  ConfigOption<String> COLUMN_ALIGN_STRATEGY =
      key(COMMON_PREFIX + "column_align_strategy")
          .defaultValue("disable");

  /**
   * Whether enable the ddl sync feature.
   */
  ConfigOption<Boolean> SYNC_DDL =
      key(COMMON_PREFIX + "sync_ddl")
          .defaultValue(false);

  ConfigOption<Boolean> SYNC_DDL_SKIP_ERROR_COLUMNS =
      key(COMMON_PREFIX + "sync_ddl_skip_error_columns")
          .defaultValue(true);

  ConfigOption<Boolean> SYNC_DDL_PRE_EXECUTE =
      key(COMMON_PREFIX + "sync_ddl_pre_execute")
          .defaultValue(false);

  /**
   * Ignore ddl delete fields.
   */
  ConfigOption<Boolean> SYNC_DDL_IGNORE_DROP =
      key(COMMON_PREFIX + "sync_ddl_ignore_drop")
          .defaultValue(true);
  /**
   * Ignore ddl new added fields.
   */
  ConfigOption<Boolean> SYNC_DDL_IGNORE_ADD =
      key(COMMON_PREFIX + "sync_ddl_ignore_add")
          .defaultValue(false);

  /**
   * Ignore ddl updated fields.
   */
  ConfigOption<Boolean> SYNC_DDL_IGNORE_UPDATE =
      key(COMMON_PREFIX + "sync_ddl_ignore_update")
          .defaultValue(false);

  ConfigOption<Boolean> DRY_RUN =
      key(COMMON_PREFIX + "dry_run")
          .defaultValue(false);

  ConfigOption<Boolean> ENABLE_DYNAMIC_LOADER =
      key(COMMON_PREFIX + "enable_dynamic_loader")
          .defaultValue(true);

  ConfigOption<Boolean> PRINT_LOADED_URLS =
      key(COMMON_PREFIX + "print_loaded_urls")
          .defaultValue(false);
  ConfigOption<Boolean> MULTI_SOURCE_ENABLED =
      key(COMMON_PREFIX + "multi_source_enable")
          .defaultValue(false);
  // todo: will be deprecated in the future
  ConfigOption<Map<String, String>> EXTRA_PROPERTIES =
      key(COMMON_PREFIX + "extra_properties")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });

  interface CheckPointOptions {
    ConfigOption<Boolean> CHECKPOINT_ENABLE =
        key(COMMON_PREFIX + "checkpoint_enable")
            .defaultValue(false);

    ConfigOption<Long> CHECKPOINT_INTERVAL =
        key(COMMON_PREFIX + "checkpoint_interval")
            .defaultValue(900000L);

    ConfigOption<Long> CHECKPOINT_TIMEOUT =
        key(COMMON_PREFIX + "checkpoint_timeout")
            .defaultValue(600000L);

    ConfigOption<Integer> CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY =
        key(COMMON_PREFIX + "checkpoint_tolerable_failure_number")
            .noDefaultValue(Integer.class);
  }

  interface RestartOptions {
    ConfigOption<String> RESTART_STRATEGY_ATTEMPTS_RATIO =
        key(COMMON_PREFIX + "task_restart_ratio")
            .noDefaultValue(String.class);

    ConfigOption<Integer> RESTART_STRATEGY_RESTART_INTERVAL =
        key(COMMON_PREFIX + "task_restart_interval")
            .defaultValue(60);

    ConfigOption<Integer> RESTART_STRATEGY_RESTART_DELAY =
        key(COMMON_PREFIX + "task_restart_delay")
            .defaultValue(1);
  }

  interface DirtyRecordOptions {
    ConfigOption<Long> DIRTY_ROLLING_POLICY_INTERVAL =
        key(COMMON_PREFIX + "dirty.rolling_policy.interval")
            .defaultValue(60 * 60 * 1000L);

    ConfigOption<Long> DIRTY_ROLLING_POLICY_SIZE =
        key(COMMON_PREFIX + "dirty.rolling_policy.size")
            .defaultValue(512 * 1024 * 1024L);

    ConfigOption<Integer> DIRTY_COLLECTOR_SIZE =
        key(COMMON_PREFIX + "dirty.collector_size")
            .defaultValue(50);

    ConfigOption<Double> DIRTY_SAMPLE_RATIO =
        key(COMMON_PREFIX + "dirty.sample_ratio")
            .defaultValue(1.0);

    ConfigOption<String> DIRTY_COLLECTOR_TYPE =
        key(COMMON_PREFIX + "dirty_collector.type")
            .noDefaultValue(String.class);

    ConfigOption<Long> DIRTY_RECORD_SAMPLE_THRESHOLD =
        key(COMMON_PREFIX + "dirty_record_sample_threshold")
            .defaultValue(1000L);

    ConfigOption<Boolean> DIRTY_RECORD_SKIP_ENABLED =
        key(COMMON_PREFIX + "dirty_record_skip_enabled")
            .defaultValue(true);
  }

  interface DateFormatOptions {
    ConfigOption<String> DATE_TIME_PATTERN = key(COMMON_PREFIX + "column.datetimeFormat")
        .defaultValue("yyyy-MM-dd HH:mm:ss");

    ConfigOption<String> DATE_PATTERN = key(COMMON_PREFIX + "column.dateFormat")
        .defaultValue("yyyy-MM-dd");

    ConfigOption<String> TIME_PATTERN = key(COMMON_PREFIX + "column.timeFormat")
        .defaultValue("HH:mm:ss");

    ConfigOption<List<String>> EXTRA_FORMATS = key(COMMON_PREFIX + "column.extraFormats")
        .defaultValue(ImmutableList.of("yyyyMMdd HH:mm:ss",
            "yyyyMMdd"));

    ConfigOption<String> TIME_ZONE = key(COMMON_PREFIX + "column.timeZone")
        .noDefaultValue(String.class);

    ConfigOption<String> COLUMN_ENCODING = key(COMMON_PREFIX + "column.encoding")
        .defaultValue("utf-8");
  }
}
