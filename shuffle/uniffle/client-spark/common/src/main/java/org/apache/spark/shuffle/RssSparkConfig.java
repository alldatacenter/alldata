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

package org.apache.spark.shuffle;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.spark.SparkConf;
import org.apache.spark.internal.config.ConfigBuilder;
import org.apache.spark.internal.config.ConfigEntry;
import org.apache.spark.internal.config.TypedConfigBuilder;
import scala.Tuple2;
import scala.runtime.AbstractFunction1;

import org.apache.uniffle.client.util.RssClientConfig;
import org.apache.uniffle.common.config.ConfigUtils;
import org.apache.uniffle.common.config.RssConf;

public class RssSparkConfig {

  public static final String SPARK_RSS_CONFIG_PREFIX = "spark.";

  public static final ConfigEntry<Integer> RSS_PARTITION_NUM_PER_RANGE = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_PARTITION_NUM_PER_RANGE))
      .createWithDefault(RssClientConfig.RSS_PARTITION_NUM_PER_RANGE_DEFAULT_VALUE);

  public static final ConfigEntry<String> RSS_WRITER_BUFFER_SIZE = createStringBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_WRITER_BUFFER_SIZE)
          .doc("Buffer size for single partition data"))
      .createWithDefault("3m");

  public static final ConfigEntry<String> RSS_WRITER_SERIALIZER_BUFFER_SIZE = createStringBuilder(
      new ConfigBuilder("spark.rss.writer.serializer.buffer.size"))
      .createWithDefault("3k");

  public static final ConfigEntry<String> RSS_WRITER_BUFFER_SEGMENT_SIZE = createStringBuilder(
      new ConfigBuilder("spark.rss.writer.buffer.segment.size"))
      .createWithDefault("3k");

  public static final ConfigEntry<String> RSS_WRITER_BUFFER_SPILL_SIZE = createStringBuilder(
      new ConfigBuilder("spark.rss.writer.buffer.spill.size")
          .doc("Buffer size for total partition data"))
      .createWithDefault("128m");

  public static final ConfigEntry<String> RSS_WRITER_PRE_ALLOCATED_BUFFER_SIZE = createStringBuilder(
      new ConfigBuilder("spark.rss.writer.pre.allocated.buffer.size"))
      .createWithDefault("16m");

  public static final ConfigEntry<Integer> RSS_WRITER_REQUIRE_MEMORY_RETRY_MAX = createIntegerBuilder(
      new ConfigBuilder("spark.rss.writer.require.memory.retryMax"))
      .createWithDefault(1200);

  public static final ConfigEntry<Long> RSS_WRITER_REQUIRE_MEMORY_INTERVAL = createLongBuilder(
      new ConfigBuilder("spark.rss.writer.require.memory.interval"))
      .createWithDefault(1000L);

  public static final ConfigEntry<Long> RSS_CLIENT_SEND_CHECK_TIMEOUT_MS = createLongBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS))
      .createWithDefault(RssClientConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS_DEFAULT_VALUE);

  public static final ConfigEntry<Long> RSS_CLIENT_SEND_CHECK_INTERVAL_MS = createLongBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS))
      .createWithDefault(RssClientConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS_DEFAULT_VALUE);

  public static final ConfigEntry<Boolean> RSS_TEST_FLAG = createBooleanBuilder(
      new ConfigBuilder("spark.rss.test"))
      .createWithDefault(false);

  public static final ConfigEntry<Boolean> RSS_TEST_MODE_ENABLE = createBooleanBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_TEST_MODE_ENABLE)
          .doc("Whether enable test mode for the Spark Client"))
      .createWithDefault(false);

  public static final ConfigEntry<String> RSS_REMOTE_STORAGE_PATH = createStringBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_REMOTE_STORAGE_PATH))
      .createWithDefault("");

  public static final ConfigEntry<Integer> RSS_INDEX_READ_LIMIT = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_INDEX_READ_LIMIT))
      .createWithDefault(RssClientConfig.RSS_INDEX_READ_LIMIT_DEFAULT_VALUE);

  public static final ConfigEntry<String> RSS_CLIENT_TYPE = createStringBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_TYPE))
      .createWithDefault(RssClientConfig.RSS_CLIENT_TYPE_DEFAULT_VALUE);

  public static final ConfigEntry<String> RSS_STORAGE_TYPE = createStringBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_STORAGE_TYPE)
          .doc("Supports MEMORY_LOCALFILE, MEMORY_HDFS, MEMORY_LOCALFILE_HDFS"))
      .createWithDefault("");

  public static final ConfigEntry<Integer> RSS_CLIENT_RETRY_MAX = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_RETRY_MAX))
      .createWithDefault(RssClientConfig.RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE);

  public static final ConfigEntry<Long> RSS_CLIENT_RETRY_INTERVAL_MAX = createLongBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_RETRY_INTERVAL_MAX))
      .createWithDefault(RssClientConfig.RSS_CLIENT_RETRY_INTERVAL_MAX_DEFAULT_VALUE);

  public static final ConfigEntry<Integer> RSS_CLIENT_HEARTBEAT_THREAD_NUM = createIntegerBuilder(
      new ConfigBuilder("spark.rss.client.heartBeat.threadNum"))
      .createWithDefault(4);

  public static final ConfigEntry<String> RSS_CLIENT_SEND_SIZE_LIMIT = createStringBuilder(
      new ConfigBuilder("spark.rss.client.send.size.limit")
          .doc("The max data size sent to shuffle server"))
      .createWithDefault("16m");

  public static final ConfigEntry<Integer> RSS_CLIENT_UNREGISTER_THREAD_POOL_SIZE = createIntegerBuilder(
      new ConfigBuilder("spark.rss.client.unregister.thread.pool.size"))
      .createWithDefault(10);

  public static final ConfigEntry<Integer> RSS_CLIENT_UNREGISTER_REQUEST_TIMEOUT_SEC = createIntegerBuilder(
      new ConfigBuilder("spark.rss.client.unregister.request.timeout.sec"))
      .createWithDefault(10);

  // When the size of read buffer reaches the half of JVM region (i.e., 32m),
  // it will incur humongous allocation, so we set it to 14m.
  public static final ConfigEntry<String> RSS_CLIENT_READ_BUFFER_SIZE = createStringBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_READ_BUFFER_SIZE)
          .doc("The max data size read from storage"))
      .createWithDefault(RssClientConfig.RSS_CLIENT_READ_BUFFER_SIZE_DEFAULT_VALUE);

  public static final ConfigEntry<Long> RSS_HEARTBEAT_INTERVAL = createLongBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_HEARTBEAT_INTERVAL))
      .createWithDefault(RssClientConfig.RSS_HEARTBEAT_INTERVAL_DEFAULT_VALUE);

  public static final ConfigEntry<Long> RSS_HEARTBEAT_TIMEOUT = createLongBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_HEARTBEAT_TIMEOUT))
      .createWithDefault(5 * 1000L);

  public static final ConfigEntry<Integer> RSS_CLIENT_SEND_THREAD_POOL_SIZE = createIntegerBuilder(
      new ConfigBuilder("spark.rss.client.send.threadPool.size")
          .doc("The thread size for send shuffle data to shuffle server"))
      .createWithDefault(10);

  public static final ConfigEntry<Integer> RSS_CLIENT_SEND_THREAD_POOL_KEEPALIVE = createIntegerBuilder(
      new ConfigBuilder("spark.rss.client.send.threadPool.keepalive"))
      .createWithDefault(60);

  public static final ConfigEntry<Integer> RSS_DATA_REPLICA = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA)
          .doc("The max server number that each block can be send by client in quorum protocol"))
      .createWithDefault(RssClientConfig.RSS_DATA_REPLICA_DEFAULT_VALUE);

  public static final ConfigEntry<Integer> RSS_DATA_REPLICA_WRITE = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA_WRITE)
          .doc("The min server number that each block should be send by client successfully"))
      .createWithDefault(RssClientConfig.RSS_DATA_REPLICA_WRITE_DEFAULT_VALUE);

  public static final ConfigEntry<Integer> RSS_DATA_REPLICA_READ = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA_READ)
          .doc("The min server number that metadata should be fetched by client successfully"))
      .createWithDefault(RssClientConfig.RSS_DATA_REPLICA_READ_DEFAULT_VALUE);

  public static final ConfigEntry<Boolean> RSS_DATA_REPLICA_SKIP_ENABLED = createBooleanBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA_SKIP_ENABLED))
      .createWithDefault(RssClientConfig.RSS_DATA_REPLICA_SKIP_ENABLED_DEFAULT_VALUE);

  public static final ConfigEntry<Integer> RSS_DATA_TRANSFER_POOL_SIZE = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_TRANSFER_POOL_SIZE))
      .createWithDefault(RssClientConfig.RSS_DATA_TRANFER_POOL_SIZE_DEFAULT_VALUE);

  public static final ConfigEntry<Integer> RSS_DATA_COMMIT_POOL_SIZE = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_COMMIT_POOL_SIZE)
          .doc("The thread size for sending commit to shuffle servers"))
      .createWithDefault(RssClientConfig.RSS_DATA_COMMIT_POOL_SIZE_DEFAULT_VALUE);

  public static final ConfigEntry<Boolean> RSS_OZONE_DFS_NAMENODE_ODFS_ENABLE = createBooleanBuilder(
      new ConfigBuilder("spark.rss.ozone.dfs.namenode.odfs.enable"))
      .createWithDefault(false);

  public static final ConfigEntry<String> RSS_OZONE_FS_HDFS_IMPL = createStringBuilder(
      new ConfigBuilder("spark.rss.ozone.fs.hdfs.impl"))
      .createWithDefault("org.apache.hadoop.odfs.HdfsOdfsFilesystem");

  public static final ConfigEntry<String> RSS_OZONE_FS_ABSTRACT_FILE_SYSTEM_HDFS_IMPL = createStringBuilder(
      new ConfigBuilder("spark.rss.ozone.fs.AbstractFileSystem.hdfs.impl"))
      .createWithDefault("org.apache.hadoop.odfs.HdfsOdfs");

  public static final ConfigEntry<Integer> RSS_CLIENT_BITMAP_SPLIT_NUM = createIntegerBuilder(
      new ConfigBuilder("spark.rss.client.bitmap.splitNum"))
      .createWithDefault(1);

  public static final ConfigEntry<String> RSS_ACCESS_ID = createStringBuilder(
      new ConfigBuilder("spark.rss.access.id"))
      .createWithDefault("");

  public static final ConfigEntry<Integer> RSS_ACCESS_TIMEOUT_MS = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_ACCESS_TIMEOUT_MS))
      .createWithDefault(RssClientConfig.RSS_ACCESS_TIMEOUT_MS_DEFAULT_VALUE);

  public static final ConfigEntry<Boolean> RSS_ENABLED = createBooleanBuilder(
      new ConfigBuilder("spark.rss.enabled"))
      .createWithDefault(false);

  public static final ConfigEntry<Boolean> RSS_DYNAMIC_CLIENT_CONF_ENABLED = createBooleanBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED))
      .createWithDefault(RssClientConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED_DEFAULT_VALUE);

  public static final ConfigEntry<String> RSS_CLIENT_ASSIGNMENT_TAGS = createStringBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_ASSIGNMENT_TAGS)
          .doc("The comma-separated list of tags for deciding assignment shuffle servers. "
              + "Notice that the SHUFFLE_SERVER_VERSION will always as the assignment tag "
              + "whether this conf is set or not"))
      .createWithDefault("");

  public static final ConfigEntry<Integer> RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER))
      .createWithDefault(RssClientConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER_DEFAULT_VALUE);

  public static final ConfigEntry<Long> RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL = createLongBuilder(
          new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL))
          .createWithDefault(RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL_DEFAULT_VALUE);

  public static final ConfigEntry<Integer> RSS_CLIENT_ASSIGNMENT_RETRY_TIMES = createIntegerBuilder(
          new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_TIMES))
          .createWithDefault(RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_TIMES_DEFAULT_VALUE);

  public static final ConfigEntry<Long> RSS_CLIENT_ACCESS_RETRY_INTERVAL_MS = createLongBuilder(
      new ConfigBuilder("spark.rss.client.access.retry.interval.ms")
          .doc("Interval between retries fallback to SortShuffleManager"))
      .createWithDefault(20000L);

  public static final ConfigEntry<Integer> RSS_CLIENT_ACCESS_RETRY_TIMES = createIntegerBuilder(
      new ConfigBuilder("spark.rss.client.access.retry.times")
          .doc("Number of retries fallback to SortShuffleManager"))
      .createWithDefault(0);

  public static final ConfigEntry<String> RSS_COORDINATOR_QUORUM = createStringBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_COORDINATOR_QUORUM)
          .doc("Coordinator quorum"))
      .createWithDefault("");

  public static final ConfigEntry<Double> RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR = createDoubleBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX +  RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR)
          .doc("Between 0 and 1, used to estimate task concurrency, how likely is this part of the resource between"
                   + " spark.dynamicAllocation.minExecutors and spark.dynamicAllocation.maxExecutors"
                   + " to be allocated"))
      .createWithDefault(RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR_DEFAULT_VALUE);

  public static final ConfigEntry<Boolean> RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED = createBooleanBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX + RssClientConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED)
          .doc("Whether to estimate the number of ShuffleServers to be allocated based on the number"
                   + " of concurrent tasks."))
      .createWithDefault(RssClientConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED_DEFAULT_VALUE);

  public static final ConfigEntry<Integer> RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER = createIntegerBuilder(
      new ConfigBuilder(SPARK_RSS_CONFIG_PREFIX +  RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER)
          .doc("How many tasks concurrency to allocate a ShuffleServer, you need to enable"
                   + " spark.rss.estimate.server.assignment.enabled"))
      .createWithDefault(RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER_DEFAULT_VALUE);

  // spark2 doesn't have this key defined
  public static final String SPARK_SHUFFLE_COMPRESS_KEY = "spark.shuffle.compress";

  public static final boolean SPARK_SHUFFLE_COMPRESS_DEFAULT = true;

  public static final Set<String> RSS_MANDATORY_CLUSTER_CONF =
      ImmutableSet.of(RSS_STORAGE_TYPE.key(), RSS_REMOTE_STORAGE_PATH.key());

  public static final boolean RSS_USE_RSS_SHUFFLE_MANAGER_DEFAULT_VALUE = false;

  public static TypedConfigBuilder<Integer> createIntegerBuilder(ConfigBuilder builder) {
    scala.Function1<String, Integer> f = new AbstractFunction1<String, Integer>() {
      @Override
      public Integer apply(String in) {
        return ConfigUtils.convertValue(in, Integer.class);
      }
    };
    return new TypedConfigBuilder<>(builder, f);
  }

  public static TypedConfigBuilder<Long> createLongBuilder(ConfigBuilder builder) {
    scala.Function1<String, Long> f = new AbstractFunction1<String, Long>() {
      @Override
      public Long apply(String in) {
        return ConfigUtils.convertValue(in, Long.class);
      }
    };
    return new TypedConfigBuilder<>(builder, f);
  }

  public static TypedConfigBuilder<Boolean> createBooleanBuilder(ConfigBuilder builder) {
    scala.Function1<String, Boolean> f = new AbstractFunction1<String, Boolean>() {
      @Override
      public Boolean apply(String in) {
        return ConfigUtils.convertValue(in, Boolean.class);
      }
    };
    return new TypedConfigBuilder<>(builder, f);
  }

  public static TypedConfigBuilder<Double> createDoubleBuilder(ConfigBuilder builder) {
    scala.Function1<String, Double> f = new AbstractFunction1<String, Double>() {
      @Override
      public Double apply(String in) {
        return ConfigUtils.convertValue(in, Double.class);
      }
    };
    return new TypedConfigBuilder<>(builder, f);
  }

  public static TypedConfigBuilder<String> createStringBuilder(ConfigBuilder builder) {
    return builder.stringConf();
  }

  public static RssConf toRssConf(SparkConf sparkConf) {
    RssConf rssConf = new RssConf();
    for (Tuple2<String, String> tuple : sparkConf.getAll()) {
      String key = tuple._1;
      if (!key.startsWith(SPARK_RSS_CONFIG_PREFIX)) {
        continue;
      }
      key = key.substring(SPARK_RSS_CONFIG_PREFIX.length());
      rssConf.setString(key, tuple._2);
    }
    return rssConf;
  }
}
