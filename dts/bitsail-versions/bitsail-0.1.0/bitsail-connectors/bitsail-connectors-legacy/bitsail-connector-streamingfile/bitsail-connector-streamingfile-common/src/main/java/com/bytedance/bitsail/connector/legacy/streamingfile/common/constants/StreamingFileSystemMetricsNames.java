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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.constants;

/**
 * Created 2020/3/15.
 */
public class StreamingFileSystemMetricsNames {

  public static final String SUB_TASK_ID = "sub_task_id";
  public static final Integer INVALID_TASK_ID = -1;

  /**
   * Checkpoint sync parse consume time.
   */
  public static final String CHECKPOINT_SNAPSHOT_LATENCY = "checkpoint.snapshot";
  public static final String CHECKPOINT_NOTIFY_LATENCY = "checkpoint.notify";

  /**
   * global aggregate consume time.
   */
  public static final String GLOBAL_AGGREGATE_LATENCY = "global.aggregate";
  public static final String SCHEMA_UPDATE_SUCCESS = "schema_update.success.count";

  /**
   * archive status statistics
   */
  public static final String ARCHIVE_SUCCESS = "archive.success.count";
  public static final String ARCHIVE_INVALID_EVENT_TIME = "archive.invalid_event_time.count";
  public static final String ARCHIVE_FASTER_THAN_SYSTEM_TIME = "archive.faster_than_system_time.count";
  public static final String ARCHIVE_LESS_THAN_COMMIT_TIME = "archive.less_than_commit_time.count";
  public static final String ARCHIVE_PRE_EVENT_TIME = "archive.pre_event_time.count";

  /**
   * hdfs client operate latency.
   */
  public static final String HDFS_FILE_WRITE_LATENCY = "hdfs.write";
  public static final String HDFS_FILE_CLOSE_LATENCY = "hdfs.close";
  public static final String HDFS_FILE_MOVE_LATENCY = "hdfs.move";
  public static final String HDFS_FILE_FLUSH_LATENCY = "hdfs.flush";

  public static final String DIRTY_HDFS_FILE_WRITE_LATENCY = "dirty.hdfs.write";
  public static final String DIRTY_HDFS_FILE_FLUSH_LATENCY = "dirty.hdfs.flush";

  /**
   * filesystem committer statistics
   */
  public static final String HDFS_REPLACED_COUNT = "hdfs.replaced.count";
  public static final String HDFS_REPLACED_COUNT_FAILED = "hdfs.replaced_failed.count";

  public static final String HDFS_CREATE_FILE_COUNT = "hdfs.create_file.count";
  public static final String HDFS_RENAME_SKIP_COUNT = "hdfs.rename_skip.count";
  public static final String HDFS_RENAME_FAILED_COUNT = "hdfs.rename_failed.count";
  public static final String HDFS_RENAME_SUCCESS_COUNT = "hdfs.rename_success.count";


  /**
   * task min timestamp metrics & job min timestamp metrics
   */
  public static final String TASK_MIN_TIMESTAMP = "task.min.timestamp";
  public static final String JOB_MIN_TIMESTAMP = "job.min.timestamp";

  /**
   * mq metric
   */
  public static final String MQ_OFFSET_INTERRUPT = "mq.offset.interrupt";
  public static final String MQ_OFFSET_DUPLICATE = "mq.offset.duplicate";
  public static final String MQ_OFFSET_OUT_OF_ORDER = "mq.offset.out_of_order";
}
