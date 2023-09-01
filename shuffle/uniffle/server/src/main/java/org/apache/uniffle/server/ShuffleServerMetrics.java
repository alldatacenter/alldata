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

package org.apache.uniffle.server;

import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.commons.lang3.StringUtils;

import org.apache.uniffle.common.metrics.MetricsManager;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.storage.common.LocalStorage;

public class ShuffleServerMetrics {

  private static final String TOTAL_RECEIVED_DATA = "total_received_data";
  private static final String TOTAL_WRITE_DATA = "total_write_data";
  private static final String TOTAL_WRITE_BLOCK = "total_write_block";
  private static final String TOTAL_WRITE_TIME = "total_write_time";
  private static final String TOTAL_WRITE_HANDLER = "total_write_handler";
  private static final String TOTAL_WRITE_EXCEPTION = "total_write_exception";
  private static final String TOTAL_WRITE_SLOW = "total_write_slow";
  private static final String TOTAL_WRITE_NUM = "total_write_num";
  private static final String APP_NUM_WITH_NODE = "app_num_with_node";
  private static final String PARTITION_NUM_WITH_NODE = "partition_num_with_node";
  private static final String EVENT_SIZE_THRESHOLD_LEVEL1 = "event_size_threshold_level1";
  private static final String EVENT_SIZE_THRESHOLD_LEVEL2 = "event_size_threshold_level2";
  private static final String EVENT_SIZE_THRESHOLD_LEVEL3 = "event_size_threshold_level3";
  private static final String EVENT_SIZE_THRESHOLD_LEVEL4 = "event_size_threshold_level4";
  private static final String EVENT_QUEUE_SIZE = "event_queue_size";
  private static final String TOTAL_READ_DATA = "total_read_data";
  private static final String TOTAL_READ_LOCAL_DATA_FILE = "total_read_local_data_file";
  private static final String TOTAL_READ_LOCAL_INDEX_FILE = "total_read_local_index_file";
  private static final String TOTAL_READ_MEMORY_DATA = "total_read_memory_data";
  private static final String TOTAL_READ_TIME = "total_read_time";
  private static final String TOTAL_REQUIRE_READ_MEMORY = "total_require_read_memory_num";
  private static final String TOTAL_REQUIRE_READ_MEMORY_RETRY = "total_require_read_memory_retry_num";
  private static final String TOTAL_REQUIRE_READ_MEMORY_FAILED = "total_require_read_memory_failed_num";

  private static final String LOCAL_STORAGE_TOTAL_DIRS_NUM = "local_storage_total_dirs_num";
  private static final String LOCAL_STORAGE_CORRUPTED_DIRS_NUM = "local_storage_corrupted_dirs_num";
  private static final String LOCAL_STORAGE_TOTAL_SPACE = "local_storage_total_space";
  private static final String LOCAL_STORAGE_USED_SPACE = "local_storage_used_space";
  private static final String LOCAL_STORAGE_USED_SPACE_RATIO = "local_storage_used_space_ratio";

  private static final String IS_HEALTHY = "is_healthy";
  private static final String ALLOCATED_BUFFER_SIZE = "allocated_buffer_size";
  private static final String IN_FLUSH_BUFFER_SIZE = "in_flush_buffer_size";
  private static final String USED_BUFFER_SIZE = "used_buffer_size";
  private static final String READ_USED_BUFFER_SIZE = "read_used_buffer_size";
  private static final String TOTAL_FAILED_WRITTEN_EVENT_NUM = "total_failed_written_event_num";
  private static final String TOTAL_DROPPED_EVENT_NUM = "total_dropped_event_num";
  private static final String TOTAL_HDFS_WRITE_DATA = "total_hdfs_write_data";
  private static final String TOTAL_LOCALFILE_WRITE_DATA = "total_localfile_write_data";
  private static final String TOTAL_REQUIRE_BUFFER_FAILED = "total_require_buffer_failed";
  private static final String TOTAL_REQUIRE_BUFFER_FAILED_FOR_HUGE_PARTITION =
      "total_require_buffer_failed_for_huge_partition";
  private static final String TOTAL_REQUIRE_BUFFER_FAILED_FOR_REGULAR_PARTITION =
      "total_require_buffer_failed_for_regular_partition";
  private static final String STORAGE_TOTAL_WRITE_LOCAL = "storage_total_write_local";
  private static final String STORAGE_RETRY_WRITE_LOCAL = "storage_retry_write_local";
  private static final String STORAGE_FAILED_WRITE_LOCAL = "storage_failed_write_local";
  private static final String STORAGE_SUCCESS_WRITE_LOCAL = "storage_success_write_local";
  public static final String STORAGE_TOTAL_WRITE_REMOTE_PREFIX = "storage_total_write_remote_";
  public static final String STORAGE_RETRY_WRITE_REMOTE_PREFIX = "storage_retry_write_remote_";
  public static final String STORAGE_FAILED_WRITE_REMOTE_PREFIX = "storage_failed_write_remote_";
  public static final String STORAGE_SUCCESS_WRITE_REMOTE_PREFIX = "storage_success_write_remote_";

  private static final String TOTAL_APP_NUM = "total_app_num";
  private static final String TOTAL_APP_WITH_HUGE_PARTITION_NUM = "total_app_with_huge_partition_num";
  private static final String TOTAL_PARTITION_NUM = "total_partition_num";
  private static final String TOTAL_HUGE_PARTITION_NUM = "total_huge_partition_num";

  private static final String HUGE_PARTITION_NUM = "huge_partition_num";
  private static final String APP_WITH_HUGE_PARTITION_NUM = "app_with_huge_partition_num";

  public static Counter counterTotalAppNum;
  public static Counter counterTotalAppWithHugePartitionNum;
  public static Counter counterTotalPartitionNum;
  public static Counter counterTotalHugePartitionNum;

  public static Counter counterTotalReceivedDataSize;
  public static Counter counterTotalWriteDataSize;
  public static Counter counterTotalWriteBlockSize;
  public static Counter counterTotalWriteTime;
  public static Counter counterWriteException;
  public static Counter counterWriteSlow;
  public static Counter counterWriteTotal;
  public static Counter counterEventSizeThresholdLevel1;
  public static Counter counterEventSizeThresholdLevel2;
  public static Counter counterEventSizeThresholdLevel3;
  public static Counter counterEventSizeThresholdLevel4;
  public static Counter counterTotalReadDataSize;
  public static Counter counterTotalReadLocalDataFileSize;
  public static Counter counterTotalReadLocalIndexFileSize;
  public static Counter counterTotalReadMemoryDataSize;
  public static Counter counterTotalReadTime;
  public static Counter counterTotalFailedWrittenEventNum;
  public static Counter counterTotalDroppedEventNum;
  public static Counter counterTotalHdfsWriteDataSize;
  public static Counter counterTotalLocalFileWriteDataSize;
  public static Counter counterTotalRequireBufferFailed;
  public static Counter counterTotalRequireBufferFailedForHugePartition;
  public static Counter counterTotalRequireBufferFailedForRegularPartition;

  public static Counter counterLocalStorageTotalWrite;
  public static Counter counterLocalStorageRetryWrite;
  public static Counter counterLocalStorageFailedWrite;
  public static Counter counterLocalStorageSuccessWrite;
  public static Counter counterTotalRequireReadMemoryNum;
  public static Counter counterTotalRequireReadMemoryRetryNum;
  public static Counter counterTotalRequireReadMemoryFailedNum;

  public static Gauge gaugeHugePartitionNum;
  public static Gauge gaugeAppWithHugePartitionNum;

  public static Gauge gaugeLocalStorageTotalDirsNum;
  public static Gauge gaugeLocalStorageCorruptedDirsNum;
  public static Gauge gaugeLocalStorageTotalSpace;
  public static Gauge gaugeLocalStorageUsedSpace;
  public static Gauge gaugeLocalStorageUsedSpaceRatio;

  public static Gauge gaugeIsHealthy;
  public static Gauge gaugeAllocatedBufferSize;
  public static Gauge gaugeInFlushBufferSize;
  public static Gauge gaugeUsedBufferSize;
  public static Gauge gaugeReadBufferUsedSize;
  public static Gauge gaugeWriteHandler;
  public static Gauge gaugeEventQueueSize;
  public static Gauge gaugeAppNum;
  public static Gauge gaugeTotalPartitionNum;
  public static Map<String, Counter> counterRemoteStorageTotalWrite;
  public static Map<String, Counter> counterRemoteStorageRetryWrite;
  public static Map<String, Counter> counterRemoteStorageFailedWrite;
  public static Map<String, Counter> counterRemoteStorageSuccessWrite;

  private static MetricsManager metricsManager;
  private static boolean isRegister = false;

  public static synchronized void register(CollectorRegistry collectorRegistry) {
    if (!isRegister) {
      counterRemoteStorageTotalWrite = Maps.newConcurrentMap();
      counterRemoteStorageRetryWrite = Maps.newConcurrentMap();
      counterRemoteStorageFailedWrite = Maps.newConcurrentMap();
      counterRemoteStorageSuccessWrite = Maps.newConcurrentMap();
      metricsManager = new MetricsManager(collectorRegistry);
      isRegister = true;
      setUpMetrics();
    }
  }

  @VisibleForTesting
  public static void register() {
    register(CollectorRegistry.defaultRegistry);
  }

  @VisibleForTesting
  public static void clear() {
    isRegister = false;
    CollectorRegistry.defaultRegistry.clear();
  }

  public static CollectorRegistry getCollectorRegistry() {
    return metricsManager.getCollectorRegistry();
  }

  public static synchronized void addDynamicCounterForRemoteStorage(String storageHost) {
    if (!StringUtils.isEmpty(storageHost)) {
      String totalWriteMetricName = STORAGE_TOTAL_WRITE_REMOTE_PREFIX
          + RssUtils.getMetricNameForHostName(storageHost);
      if (!counterRemoteStorageTotalWrite.containsKey(storageHost)) {
        counterRemoteStorageTotalWrite.putIfAbsent(storageHost,
            metricsManager.addCounter(totalWriteMetricName));
      }
      String retryWriteMetricName = STORAGE_RETRY_WRITE_REMOTE_PREFIX
          + RssUtils.getMetricNameForHostName(storageHost);
      if (!counterRemoteStorageRetryWrite.containsKey(storageHost)) {
        counterRemoteStorageRetryWrite.putIfAbsent(storageHost,
            metricsManager.addCounter(retryWriteMetricName));
      }
      String failedWriteMetricName = STORAGE_FAILED_WRITE_REMOTE_PREFIX
          + RssUtils.getMetricNameForHostName(storageHost);
      if (!counterRemoteStorageFailedWrite.containsKey(storageHost)) {
        counterRemoteStorageFailedWrite.putIfAbsent(storageHost,
            metricsManager.addCounter(failedWriteMetricName));
      }
      String successWriteMetricName = STORAGE_SUCCESS_WRITE_REMOTE_PREFIX
          + RssUtils.getMetricNameForHostName(storageHost);
      if (!counterRemoteStorageSuccessWrite.containsKey(storageHost)) {
        counterRemoteStorageSuccessWrite.putIfAbsent(storageHost,
            metricsManager.addCounter(successWriteMetricName));
      }
    }
  }

  public static void incStorageRetryCounter(String storageHost) {
    if (LocalStorage.STORAGE_HOST.equals(storageHost)) {
      counterLocalStorageTotalWrite.inc();
      counterLocalStorageRetryWrite.inc();
    } else {
      if (!StringUtils.isEmpty(storageHost)) {
        counterRemoteStorageTotalWrite.get(storageHost).inc();
        counterRemoteStorageRetryWrite.get(storageHost).inc();
      }
    }
  }

  public static void incStorageSuccessCounter(String storageHost) {
    if (LocalStorage.STORAGE_HOST.equals(storageHost)) {
      counterLocalStorageTotalWrite.inc();
      counterLocalStorageSuccessWrite.inc();
    } else {
      if (!StringUtils.isEmpty(storageHost)) {
        counterRemoteStorageTotalWrite.get(storageHost).inc();
        counterRemoteStorageSuccessWrite.get(storageHost).inc();
      }
    }
  }

  public static void incStorageFailedCounter(String storageHost) {
    if (LocalStorage.STORAGE_HOST.equals(storageHost)) {
      counterLocalStorageTotalWrite.inc();
      counterLocalStorageFailedWrite.inc();
    } else {
      if (!StringUtils.isEmpty(storageHost)) {
        counterRemoteStorageTotalWrite.get(storageHost).inc();
        counterRemoteStorageFailedWrite.get(storageHost).inc();
      }
    }
  }

  private static void setUpMetrics() {
    counterTotalReceivedDataSize = metricsManager.addCounter(TOTAL_RECEIVED_DATA);
    counterTotalWriteDataSize = metricsManager.addCounter(TOTAL_WRITE_DATA);
    counterTotalWriteBlockSize = metricsManager.addCounter(TOTAL_WRITE_BLOCK);
    counterTotalWriteTime = metricsManager.addCounter(TOTAL_WRITE_TIME);
    counterWriteException = metricsManager.addCounter(TOTAL_WRITE_EXCEPTION);
    counterWriteSlow = metricsManager.addCounter(TOTAL_WRITE_SLOW);
    counterWriteTotal = metricsManager.addCounter(TOTAL_WRITE_NUM);
    counterEventSizeThresholdLevel1 = metricsManager.addCounter(EVENT_SIZE_THRESHOLD_LEVEL1);
    counterEventSizeThresholdLevel2 = metricsManager.addCounter(EVENT_SIZE_THRESHOLD_LEVEL2);
    counterEventSizeThresholdLevel3 = metricsManager.addCounter(EVENT_SIZE_THRESHOLD_LEVEL3);
    counterEventSizeThresholdLevel4 = metricsManager.addCounter(EVENT_SIZE_THRESHOLD_LEVEL4);
    counterTotalReadDataSize = metricsManager.addCounter(TOTAL_READ_DATA);
    counterTotalReadLocalDataFileSize = metricsManager.addCounter(TOTAL_READ_LOCAL_DATA_FILE);
    counterTotalReadLocalIndexFileSize = metricsManager.addCounter(TOTAL_READ_LOCAL_INDEX_FILE);
    counterTotalReadMemoryDataSize = metricsManager.addCounter(TOTAL_READ_MEMORY_DATA);
    counterTotalReadTime = metricsManager.addCounter(TOTAL_READ_TIME);
    counterTotalDroppedEventNum = metricsManager.addCounter(TOTAL_DROPPED_EVENT_NUM);
    counterTotalFailedWrittenEventNum = metricsManager.addCounter(TOTAL_FAILED_WRITTEN_EVENT_NUM);
    counterTotalHdfsWriteDataSize = metricsManager.addCounter(TOTAL_HDFS_WRITE_DATA);
    counterTotalLocalFileWriteDataSize = metricsManager.addCounter(TOTAL_LOCALFILE_WRITE_DATA);
    counterTotalRequireBufferFailed = metricsManager.addCounter(TOTAL_REQUIRE_BUFFER_FAILED);
    counterTotalRequireBufferFailedForRegularPartition =
        metricsManager.addCounter(TOTAL_REQUIRE_BUFFER_FAILED_FOR_REGULAR_PARTITION);
    counterTotalRequireBufferFailedForHugePartition =
        metricsManager.addCounter(TOTAL_REQUIRE_BUFFER_FAILED_FOR_HUGE_PARTITION);
    counterLocalStorageTotalWrite = metricsManager.addCounter(STORAGE_TOTAL_WRITE_LOCAL);
    counterLocalStorageRetryWrite = metricsManager.addCounter(STORAGE_RETRY_WRITE_LOCAL);
    counterLocalStorageFailedWrite = metricsManager.addCounter(STORAGE_FAILED_WRITE_LOCAL);
    counterLocalStorageSuccessWrite = metricsManager.addCounter(STORAGE_SUCCESS_WRITE_LOCAL);
    counterTotalRequireReadMemoryNum = metricsManager.addCounter(TOTAL_REQUIRE_READ_MEMORY);
    counterTotalRequireReadMemoryRetryNum = metricsManager.addCounter(TOTAL_REQUIRE_READ_MEMORY_RETRY);
    counterTotalRequireReadMemoryFailedNum = metricsManager.addCounter(TOTAL_REQUIRE_READ_MEMORY_FAILED);

    counterTotalAppNum = metricsManager.addCounter(TOTAL_APP_NUM);
    counterTotalAppWithHugePartitionNum = metricsManager.addCounter(TOTAL_APP_WITH_HUGE_PARTITION_NUM);
    counterTotalPartitionNum = metricsManager.addCounter(TOTAL_PARTITION_NUM);
    counterTotalHugePartitionNum = metricsManager.addCounter(TOTAL_HUGE_PARTITION_NUM);

    gaugeLocalStorageTotalDirsNum = metricsManager.addGauge(LOCAL_STORAGE_TOTAL_DIRS_NUM);
    gaugeLocalStorageCorruptedDirsNum = metricsManager.addGauge(LOCAL_STORAGE_CORRUPTED_DIRS_NUM);
    gaugeLocalStorageTotalSpace = metricsManager.addGauge(LOCAL_STORAGE_TOTAL_SPACE);
    gaugeLocalStorageUsedSpace = metricsManager.addGauge(LOCAL_STORAGE_USED_SPACE);
    gaugeLocalStorageUsedSpaceRatio = metricsManager.addGauge(LOCAL_STORAGE_USED_SPACE_RATIO);

    gaugeIsHealthy = metricsManager.addGauge(IS_HEALTHY);
    gaugeAllocatedBufferSize = metricsManager.addGauge(ALLOCATED_BUFFER_SIZE);
    gaugeInFlushBufferSize = metricsManager.addGauge(IN_FLUSH_BUFFER_SIZE);
    gaugeUsedBufferSize = metricsManager.addGauge(USED_BUFFER_SIZE);
    gaugeReadBufferUsedSize = metricsManager.addGauge(READ_USED_BUFFER_SIZE);
    gaugeWriteHandler = metricsManager.addGauge(TOTAL_WRITE_HANDLER);
    gaugeEventQueueSize = metricsManager.addGauge(EVENT_QUEUE_SIZE);
    gaugeAppNum = metricsManager.addGauge(APP_NUM_WITH_NODE);
    gaugeTotalPartitionNum = metricsManager.addGauge(PARTITION_NUM_WITH_NODE);

    gaugeHugePartitionNum = metricsManager.addGauge(HUGE_PARTITION_NUM);
    gaugeAppWithHugePartitionNum = metricsManager.addGauge(APP_WITH_HUGE_PARTITION_NUM);
  }

}
