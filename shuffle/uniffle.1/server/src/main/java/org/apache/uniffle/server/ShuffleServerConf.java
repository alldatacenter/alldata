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

import java.util.List;

import org.apache.hadoop.conf.Configuration;

import org.apache.uniffle.common.config.ConfigOption;
import org.apache.uniffle.common.config.ConfigOptions;
import org.apache.uniffle.common.config.ConfigUtils;
import org.apache.uniffle.common.config.RssBaseConf;

public class ShuffleServerConf extends RssBaseConf {

  public static final String PREFIX_HADOOP_CONF = "rss.server.hadoop";

  public static final ConfigOption<Long> SERVER_BUFFER_CAPACITY = ConfigOptions
      .key("rss.server.buffer.capacity")
      .longType()
      .noDefaultValue()
      .withDescription("Max memory of buffer manager for shuffle server");

  public static final ConfigOption<Long> SERVER_READ_BUFFER_CAPACITY = ConfigOptions
      .key("rss.server.read.buffer.capacity")
      .longType()
      .defaultValue(10000L)
      .withDescription("Max size of buffer for reading data");

  public static final ConfigOption<Long> SERVER_HEARTBEAT_DELAY = ConfigOptions
      .key("rss.server.heartbeat.delay")
      .longType()
      .defaultValue(10 * 1000L)
      .withDescription("rss heartbeat initial delay ms");

  public static final ConfigOption<Integer> SERVER_HEARTBEAT_THREAD_NUM = ConfigOptions
      .key("rss.server.heartbeat.threadNum")
      .intType()
      .defaultValue(2)
      .withDescription("rss heartbeat thread number");

  public static final ConfigOption<Long> SERVER_HEARTBEAT_INTERVAL = ConfigOptions
      .key("rss.server.heartbeat.interval")
      .longType()
      .defaultValue(10 * 1000L)
      .withDescription("Heartbeat interval to Coordinator (ms)");

  public static final ConfigOption<Long> SERVER_HEARTBEAT_TIMEOUT = ConfigOptions
      .key("rss.server.heartbeat.timeout")
      .longType()
      .defaultValue(60 * 1000L)
      .withDescription("rss heartbeat interval ms");

  public static final ConfigOption<Integer> SERVER_FLUSH_THREAD_POOL_SIZE = ConfigOptions
      .key("rss.server.flush.threadPool.size")
      .intType()
      .defaultValue(10)
      .withDescription("thread pool for flush data to file");

  public static final ConfigOption<Integer> SERVER_FLUSH_THREAD_POOL_QUEUE_SIZE = ConfigOptions
      .key("rss.server.flush.threadPool.queue.size")
      .intType()
      .defaultValue(Integer.MAX_VALUE)
      .withDescription("size of waiting queue for thread pool");

  public static final ConfigOption<Long> SERVER_FLUSH_THREAD_ALIVE = ConfigOptions
      .key("rss.server.flush.thread.alive")
      .longType()
      .defaultValue(120L)
      .withDescription("thread idle time in pool (s)");

  public static final ConfigOption<Long> SERVER_COMMIT_TIMEOUT = ConfigOptions
      .key("rss.server.commit.timeout")
      .longType()
      .defaultValue(600000L)
      .withDescription("Timeout when commit shuffle data (ms)");

  public static final ConfigOption<Integer> SERVER_WRITE_RETRY_MAX = ConfigOptions
      .key("rss.server.write.retry.max")
      .intType()
      .defaultValue(10)
      .withDescription("Retry times when write fail");

  public static final ConfigOption<Long> SERVER_APP_EXPIRED_WITHOUT_HEARTBEAT = ConfigOptions
      .key("rss.server.app.expired.withoutHeartbeat")
      .longType()
      .defaultValue(60 * 1000L)
      .withDescription("Expired time (ms) for application which has no heartbeat with coordinator");

  public static final ConfigOption<Integer> SERVER_MEMORY_REQUEST_RETRY_MAX = ConfigOptions
      .key("rss.server.memory.request.retry.max")
      .intType()
      .defaultValue(50)
      .withDescription("Max times to retry for memory request");

  public static final ConfigOption<Long> SERVER_PRE_ALLOCATION_EXPIRED = ConfigOptions
      .key("rss.server.preAllocation.expired")
      .longType()
      .defaultValue(20 * 1000L)
      .withDescription("Expired time (ms) for pre allocated buffer");

  public static final ConfigOption<Long> SERVER_COMMIT_CHECK_INTERVAL_MAX = ConfigOptions
      .key("rss.server.commit.check.interval.max.ms")
      .longType()
      .defaultValue(10000L)
      .withDescription("Max interval(ms) for check commit status");

  public static final ConfigOption<Long> SERVER_WRITE_SLOW_THRESHOLD = ConfigOptions
      .key("rss.server.write.slow.threshold")
      .longType()
      .defaultValue(10000L)
      .withDescription("Threshold for write slow defined");

  public static final ConfigOption<Long> SERVER_EVENT_SIZE_THRESHOLD_L1 = ConfigOptions
      .key("rss.server.event.size.threshold.l1")
      .longType()
      .defaultValue(200000L)
      .withDescription("Threshold for event size");

  public static final ConfigOption<Long> SERVER_EVENT_SIZE_THRESHOLD_L2 = ConfigOptions
      .key("rss.server.event.size.threshold.l2")
      .longType()
      .defaultValue(1000000L)
      .withDescription("Threshold for event size");

  public static final ConfigOption<Long> SERVER_EVENT_SIZE_THRESHOLD_L3 = ConfigOptions
      .key("rss.server.event.size.threshold.l3")
      .longType()
      .defaultValue(10000000L)
      .withDescription("Threshold for event size");

  public static final ConfigOption<Double> CLEANUP_THRESHOLD = ConfigOptions
      .key("rss.server.cleanup.threshold")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR, "clean threshold must be between 0.0 and 100.0")
      .defaultValue(10.0)
      .withDescription("Threshold for disk cleanup");

  public static final ConfigOption<Double> HIGH_WATER_MARK_OF_WRITE = ConfigOptions
      .key("rss.server.high.watermark.write")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR, "high write watermark must be between 0.0 and 100.0")
      .defaultValue(95.0)
      .withDescription("If disk usage is bigger than this value, disk cannot been written");

  public static final ConfigOption<Double> LOW_WATER_MARK_OF_WRITE = ConfigOptions
      .key("rss.server.low.watermark.write")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR, "low write watermark must be between 0.0 and 100.0")
      .defaultValue(85.0)
      .withDescription("If disk usage is smaller than this value, disk can been written again");

  public static final ConfigOption<Long> PENDING_EVENT_TIMEOUT_SEC = ConfigOptions
      .key("rss.server.pending.event.timeout.sec")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_LONG_VALIDATOR, "pending event timeout must be positive")
      .defaultValue(600L)
      .withDescription("If disk cannot be written for timeout seconds, the flush data event will fail");

  public static final ConfigOption<Long> DISK_CAPACITY = ConfigOptions
      .key("rss.server.disk.capacity")
      .longType()
      .defaultValue(-1L)
      .withDescription("Disk capacity that shuffle server can use. "
          + "If it's negative, it will use the default whole space");

  public static final ConfigOption<Long> SHUFFLE_EXPIRED_TIMEOUT_MS = ConfigOptions
      .key("rss.server.shuffle.expired.timeout.ms")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_LONG_VALIDATOR, "shuffle expired timeout must be positive")
      .defaultValue(60L * 1000 * 2)
      .withDescription("If the shuffle is not read for the long time, and shuffle is uploaded totally,"
          + " , we can delete the shuffle");

  public static final ConfigOption<Long> SERVER_SHUFFLE_INDEX_SIZE_HINT = ConfigOptions
      .key("rss.server.index.size.hint")
      .longType()
      .defaultValue(2 * 1024L * 1024L)
      .withDescription("The index file size hint");

  public static final ConfigOption<Double> HEALTH_STORAGE_MAX_USAGE_PERCENTAGE = ConfigOptions
      .key("rss.server.health.max.storage.usage.percentage")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR,
          "The max usage percentage must be between 0.0 and 100.0")
      .defaultValue(90.0)
      .withDescription("The usage percentage of a storage exceed the value, the disk become unavailable");

  public static final ConfigOption<Double> HEALTH_STORAGE_RECOVERY_USAGE_PERCENTAGE = ConfigOptions
      .key("rss.server.health.storage.recovery.usage.percentage")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR,
          "The recovery usage percentage must be between 0.0 and 100.0")
      .defaultValue(80.0)
      .withDescription("The usage percentage of an unavailable storage decline the value, the disk"
          + " will become available");

  public static final ConfigOption<Long> HEALTH_CHECK_INTERVAL = ConfigOptions
      .key("rss.server.health.check.interval.ms")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_LONG_VALIDATOR,  "The interval for health check must be positive")
      .defaultValue(5000L)
      .withDescription("The interval for health check");

  public static final ConfigOption<Double> HEALTH_MIN_STORAGE_PERCENTAGE = ConfigOptions
      .key("rss.server.health.min.storage.percentage")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR,
          "The minimum for healthy storage percentage must be between 0.0 and 100.0")
      .defaultValue(80.0)
      .withDescription("The minimum fraction of storage that must pass the check mark the node as healthy");

  public static final ConfigOption<Boolean> HEALTH_CHECK_ENABLE = ConfigOptions
      .key("rss.server.health.check.enable")
      .booleanType()
      .defaultValue(false)
      .withDescription("The switch for the health check");

  public static final ConfigOption<List<String>> HEALTH_CHECKER_CLASS_NAMES = ConfigOptions
      .key("rss.server.health.checker.class.names")
      .stringType()
      .asList()
      .noDefaultValue()
      .withDescription("The list of the Checker's name");

  public static final ConfigOption<Double> SERVER_MEMORY_SHUFFLE_LOWWATERMARK_PERCENTAGE = ConfigOptions
      .key("rss.server.memory.shuffle.lowWaterMark.percentage")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR,
          "The lowWaterMark for memory percentage must be between 0.0 and 100.0")
      .defaultValue(25.0)
      .withDescription("LowWaterMark of memory in percentage style");

  public static final ConfigOption<Double> SERVER_MEMORY_SHUFFLE_HIGHWATERMARK_PERCENTAGE = ConfigOptions
      .key("rss.server.memory.shuffle.highWaterMark.percentage")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR,
          "The highWaterMark for memory percentage must be between 0.0 and 100.0")
      .defaultValue(75.0)
      .withDescription("HighWaterMark of memory in percentage style");

  public static final ConfigOption<Long> FLUSH_COLD_STORAGE_THRESHOLD_SIZE = ConfigOptions
      .key("rss.server.flush.cold.storage.threshold.size")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_LONG_VALIDATOR, "flush cold storage threshold must be positive")
      .defaultValue(64L * 1024L * 1024L)
      .withDescription("For multistorage, the event size exceed this value, flush data  to cold storage");

  public static final ConfigOption<String> MULTISTORAGE_FALLBACK_STRATEGY_CLASS = ConfigOptions
      .key("rss.server.multistorage.fallback.strategy.class")
      .stringType()
      .noDefaultValue()
      .withDescription("For multistorage, fallback strategy class");

  public static final ConfigOption<Long> FALLBACK_MAX_FAIL_TIMES = ConfigOptions
      .key("rss.server.multistorage.fallback.max.fail.times")
      .longType()
      .checkValue(ConfigUtils.NON_NEGATIVE_LONG_VALIDATOR, " fallback times must be non-negative")
      .defaultValue(0L)
      .withDescription("For multistorage, fail times exceed the number, will switch storage");

  public static final ConfigOption<List<String>> TAGS = ConfigOptions
      .key("rss.server.tags")
      .stringType()
      .asList()
      .noDefaultValue()
      .withDescription("Tags list supported by shuffle server");

  public static final ConfigOption<Long> LOCAL_STORAGE_INITIALIZE_MAX_FAIL_NUMBER = ConfigOptions
      .key("rss.server.localstorage.initialize.max.fail.number")
      .longType()
      .checkValue(ConfigUtils.NON_NEGATIVE_LONG_VALIDATOR, " max fail times must be non-negative")
      .defaultValue(0L)
      .withDescription("For localstorage, it will exit when the failed initialized local storage exceed the number");

  public static final ConfigOption<Boolean> SINGLE_BUFFER_FLUSH_ENABLED = ConfigOptions
       .key("rss.server.single.buffer.flush.enabled")
       .booleanType()
       .defaultValue(false)
       .withDescription("Whether single buffer flush when size exceeded rss.server.single.buffer.flush.threshold");

  public static final ConfigOption<Long> SINGLE_BUFFER_FLUSH_THRESHOLD = ConfigOptions
        .key("rss.server.single.buffer.flush.threshold")
        .longType()
        .defaultValue(64 * 1024 * 1024L)
        .withDescription("The threshold of single shuffle buffer flush");

  public static final ConfigOption<Long> STORAGEMANAGER_CACHE_TIMEOUT = ConfigOptions
      .key("rss.server.multistorage.storagemanager.cache.timeout")
      .longType()
      .defaultValue(60 * 1000L)
      .withDescription("The timeout of the cache which record the mapping information");

  public static final ConfigOption<Long> SERVER_LEAK_SHUFFLE_DATA_CHECK_INTERVAL = ConfigOptions
      .key("rss.server.leak.shuffledata.check.interval")
      .longType()
      .defaultValue(3600 * 1000L)
      .withDescription("the interval of leak shuffle data check");

  public static final ConfigOption<Integer> SERVER_MAX_CONCURRENCY_OF_ONE_PARTITION = ConfigOptions
      .key("rss.server.max.concurrency.of.single.partition.writer")
      .intType()
      .defaultValue(1)
      .withDescription("The max concurrency of single partition writer, the data partition file number is "
          + "equal to this value. Default value is 1.");

  public static final ConfigOption<Long> SERVER_TRIGGER_FLUSH_CHECK_INTERVAL = ConfigOptions
      .key("rss.server.shuffleBufferManager.trigger.flush.interval")
      .longType()
      .defaultValue(0L)
      .withDescription("The interval of trigger shuffle buffer manager to flush data to persistent storage. If <= 0"
          + ", then this flush check would be disabled.");

  public static final ConfigOption<Long> SERVER_SHUFFLE_FLUSH_THRESHOLD = ConfigOptions
      .key("rss.server.shuffle.flush.threshold")
      .longType()
      .checkValue(ConfigUtils.NON_NEGATIVE_LONG_VALIDATOR, "flush threshold must be non negative")
      .defaultValue(0L)
      .withDescription("Threshold when flushing shuffle data to persistent storage, recommend value would be 256K, "
          + "512K, or even 1M");

  public static final ConfigOption<String> STORAGE_MEDIA_PROVIDER_ENV_KEY = ConfigOptions
      .key("rss.server.storageMediaProvider.from.env.key")
      .stringType()
      .noDefaultValue()
      .withDescription("The env key to get json source of local storage media provider");

  public static final ConfigOption<Long> HUGE_PARTITION_SIZE_THRESHOLD = ConfigOptions
      .key("rss.server.huge-partition.size.threshold")
      .longType()
      .defaultValue(20 * 1024 * 1024 * 1024L)
      .withDescription("Threshold of huge partition size, once exceeding threshold, memory usage limitation and "
          + "huge partition buffer flushing will be triggered.");

  public static final ConfigOption<Double> HUGE_PARTITION_MEMORY_USAGE_LIMITATION_RATIO = ConfigOptions
      .key("rss.server.huge-partition.memory.limit.ratio")
      .doubleType()
      .defaultValue(0.2)
      .withDescription("The memory usage limit ratio for huge partition, it will only triggered when partition's "
          + "size exceeds the threshold of '" + HUGE_PARTITION_SIZE_THRESHOLD.key() + "'");

  public ShuffleServerConf() {
  }

  public ShuffleServerConf(String fileName) {
    super();
    boolean ret = loadConfFromFile(fileName);
    if (!ret) {
      throw new IllegalStateException("Fail to load config file " + fileName);
    }
  }

  public Configuration getHadoopConf() {
    Configuration hadoopConf = new Configuration();
    for (String key : getKeySet()) {
      if (key.startsWith(ShuffleServerConf.PREFIX_HADOOP_CONF)) {
        String value = getString(key, "");
        String hadoopKey = key.substring(ShuffleServerConf.PREFIX_HADOOP_CONF.length() + 1);
        hadoopConf.set(hadoopKey, value);
      }
    }
    return hadoopConf;
  }

  public boolean loadConfFromFile(String fileName) {
    return loadConfFromFile(fileName, ConfigUtils.getAllConfigOptions(ShuffleServerConf.class));
  }
}
