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

package org.apache.hadoop.mapreduce;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.hadoop.mapred.JobConf;

import org.apache.uniffle.client.util.RssClientConfig;
import org.apache.uniffle.common.config.RssConf;

public class RssMRConfig {

  public static final String MR_RSS_CONFIG_PREFIX = "mapreduce.";
  public static final String RSS_CLIENT_HEARTBEAT_THREAD_NUM =
      MR_RSS_CONFIG_PREFIX + "rss.client.heartBeat.threadNum";
  public static final int RSS_CLIENT_HEARTBEAT_THREAD_NUM_DEFAULT_VALUE = 4;
  public static final String RSS_CLIENT_TYPE = MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_TYPE;
  public static final String RSS_CLIENT_TYPE_DEFAULT_VALUE = RssClientConfig.RSS_CLIENT_TYPE_DEFAULT_VALUE;
  public static final String RSS_CLIENT_RETRY_MAX = MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_RETRY_MAX;
  public static final int RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE = RssClientConfig.RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE;
  public static final String RSS_CLIENT_RETRY_INTERVAL_MAX =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_RETRY_INTERVAL_MAX;
  public static final long RSS_CLIENT_RETRY_INTERVAL_MAX_DEFAULT_VALUE =
      RssClientConfig.RSS_CLIENT_RETRY_INTERVAL_MAX_DEFAULT_VALUE;
  public static final String RSS_COORDINATOR_QUORUM =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_COORDINATOR_QUORUM;
  public static final String RSS_DATA_REPLICA = MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA;
  public static final int RSS_DATA_REPLICA_DEFAULT_VALUE = RssClientConfig.RSS_DATA_REPLICA_DEFAULT_VALUE;
  public static final String RSS_DATA_REPLICA_WRITE =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA_WRITE;
  public static final int RSS_DATA_REPLICA_WRITE_DEFAULT_VALUE =
      RssClientConfig.RSS_DATA_REPLICA_WRITE_DEFAULT_VALUE;
  public static final String RSS_DATA_REPLICA_READ =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA_READ;
  public static final int RSS_DATA_REPLICA_READ_DEFAULT_VALUE =
      RssClientConfig.RSS_DATA_REPLICA_READ_DEFAULT_VALUE;
  public static final String RSS_DATA_REPLICA_SKIP_ENABLED =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_REPLICA_SKIP_ENABLED;
  public static final String RSS_DATA_TRANSFER_POOL_SIZE =
          MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_TRANSFER_POOL_SIZE;
  public static final int RSS_DATA_TRANSFER_POOL_SIZE_DEFAULT_VALUE =
          RssClientConfig.RSS_DATA_TRANFER_POOL_SIZE_DEFAULT_VALUE;
  public static final String RSS_DATA_COMMIT_POOL_SIZE =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DATA_COMMIT_POOL_SIZE;
  public static final int RSS_DATA_COMMIT_POOL_SIZE_DEFAULT_VALUE =
      RssClientConfig.RSS_DATA_COMMIT_POOL_SIZE_DEFAULT_VALUE;

  public static final String RSS_CLIENT_SEND_THREAD_NUM =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_SEND_THREAD_NUM;
  public static final int RSS_CLIENT_DEFAULT_SEND_THREAD_NUM =
      RssClientConfig.RSS_CLIENT_DEFAULT_SEND_NUM;
  public static final String RSS_CLIENT_SEND_THRESHOLD = MR_RSS_CONFIG_PREFIX + "rss.client.send.threshold";
  public static final double RSS_CLIENT_DEFAULT_SEND_THRESHOLD = 0.2f;
  public static final boolean RSS_DATA_REPLICA_SKIP_ENABLED_DEFAULT_VALUE =
      RssClientConfig.RSS_DATA_REPLICA_SKIP_ENABLED_DEFAULT_VALUE;
  public static final String RSS_HEARTBEAT_INTERVAL =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_HEARTBEAT_INTERVAL;
  public static final long RSS_HEARTBEAT_INTERVAL_DEFAULT_VALUE =
      RssClientConfig.RSS_HEARTBEAT_INTERVAL_DEFAULT_VALUE;
  public static final String RSS_HEARTBEAT_TIMEOUT =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_HEARTBEAT_TIMEOUT;
  public static final String RSS_ASSIGNMENT_PREFIX = MR_RSS_CONFIG_PREFIX + "rss.assignment.partition.";
  public static final String RSS_CLIENT_BATCH_TRIGGER_NUM =
      MR_RSS_CONFIG_PREFIX + "rss.client.batch.trigger.num";
  public static final int RSS_CLIENT_DEFAULT_BATCH_TRIGGER_NUM = 50;
  public static final String RSS_CLIENT_SORT_MEMORY_USE_THRESHOLD =
      MR_RSS_CONFIG_PREFIX + "rss.client.sort.memory.use.threshold";
  public static final String RSS_WRITER_BUFFER_SIZE =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_WRITER_BUFFER_SIZE;
  public static final long RSS_WRITER_BUFFER_SIZE_DEFAULT_VALUE = 1024 * 1024 * 14;
  public static final double RSS_CLIENT_DEFAULT_SORT_MEMORY_USE_THRESHOLD = 0.9f;
  public static final String RSS_CLIENT_MEMORY_THRESHOLD =
      MR_RSS_CONFIG_PREFIX + "rss.client.memory.threshold";
  public static final double RSS_CLIENT_DEFAULT_MEMORY_THRESHOLD = 0.8f;
  public static final String RSS_CLIENT_SEND_CHECK_INTERVAL_MS =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS;
  public static final long RSS_CLIENT_SEND_CHECK_INTERVAL_MS_DEFAULT_VALUE =
      RssClientConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS_DEFAULT_VALUE;
  public static final String RSS_CLIENT_SEND_CHECK_TIMEOUT_MS =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS;
  public static final long RSS_CLIENT_SEND_CHECK_TIMEOUT_MS_DEFAULT_VALUE =
      RssClientConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS_DEFAULT_VALUE;
  public static final String RSS_CLIENT_BITMAP_NUM = MR_RSS_CONFIG_PREFIX + "rss.client.bitmap.num";
  public static final int RSS_CLIENT_DEFAULT_BITMAP_NUM = 1;
  public static final String RSS_CLIENT_MAX_SEGMENT_SIZE =
      MR_RSS_CONFIG_PREFIX + "rss.client.max.buffer.size";
  public static final long RSS_CLIENT_DEFAULT_MAX_SEGMENT_SIZE = 3 * 1024;
  public static final String RSS_STORAGE_TYPE = MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_STORAGE_TYPE;

  public static final String RSS_REDUCE_REMOTE_SPILL_ENABLED = MR_RSS_CONFIG_PREFIX
      + "rss.reduce.remote.spill.enable";
  public static final boolean RSS_REDUCE_REMOTE_SPILL_ENABLED_DEFAULT = false;
  public static final String RSS_REDUCE_REMOTE_SPILL_ATTEMPT_INC = MR_RSS_CONFIG_PREFIX
      + "rss.reduce.remote.spill.attempt.inc";
  public static final int RSS_REDUCE_REMOTE_SPILL_ATTEMPT_INC_DEFAULT = 1;
  public static final String RSS_REDUCE_REMOTE_SPILL_REPLICATION = MR_RSS_CONFIG_PREFIX
      + "rss.reduce.remote.spill.replication";
  public static final int RSS_REDUCE_REMOTE_SPILL_REPLICATION_DEFAULT = 1;
  public static final String RSS_REDUCE_REMOTE_SPILL_RETRIES = MR_RSS_CONFIG_PREFIX
      + "rss.reduce.remote.spill.retries";
  public static final int RSS_REDUCE_REMOTE_SPILL_RETRIES_DEFAULT = 5;


  public static final String RSS_PARTITION_NUM_PER_RANGE =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_PARTITION_NUM_PER_RANGE;
  public static final int RSS_PARTITION_NUM_PER_RANGE_DEFAULT_VALUE =
      RssClientConfig.RSS_PARTITION_NUM_PER_RANGE_DEFAULT_VALUE;
  public static final String RSS_REMOTE_STORAGE_PATH =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_REMOTE_STORAGE_PATH;
  public static final String RSS_REMOTE_STORAGE_CONF =
      MR_RSS_CONFIG_PREFIX + "rss.remote.storage.conf";
  public static final String RSS_INDEX_READ_LIMIT =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_INDEX_READ_LIMIT;
  public static final int RSS_INDEX_READ_LIMIT_DEFAULT_VALUE =
      RssClientConfig.RSS_INDEX_READ_LIMIT_DEFAULT_VALUE;
  public static final String RSS_CLIENT_READ_BUFFER_SIZE =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_READ_BUFFER_SIZE;

  // When the size of read buffer reaches the half of JVM region (i.e., 32m),
  // it will incur humongous allocation, so we set it to 14m.
  public static final String RSS_CLIENT_READ_BUFFER_SIZE_DEFAULT_VALUE =
      RssClientConfig.RSS_CLIENT_READ_BUFFER_SIZE_DEFAULT_VALUE;

  public static final String RSS_DYNAMIC_CLIENT_CONF_ENABLED =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED;
  public static final boolean RSS_DYNAMIC_CLIENT_CONF_ENABLED_DEFAULT_VALUE =
      RssClientConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED_DEFAULT_VALUE;
  public static final String RSS_ACCESS_TIMEOUT_MS = MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_ACCESS_TIMEOUT_MS;
  public static final int RSS_ACCESS_TIMEOUT_MS_DEFAULT_VALUE = RssClientConfig.RSS_ACCESS_TIMEOUT_MS_DEFAULT_VALUE;

  public static final String RSS_CLIENT_ASSIGNMENT_TAGS =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_ASSIGNMENT_TAGS;

  public static final String RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER =
      RssClientConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER;
  public static final int RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER_DEFAULT_VALUE =
      RssClientConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER_DEFAULT_VALUE;

  public static final String RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL =
          MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL;
  public static final long RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL_DEFAULT_VALUE =
          RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL_DEFAULT_VALUE;
  public static final String RSS_CLIENT_ASSIGNMENT_RETRY_TIMES =
          MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_TIMES;
  public static final int RSS_CLIENT_ASSIGNMENT_RETRY_TIMES_DEFAULT_VALUE =
          RssClientConfig.RSS_CLIENT_ASSIGNMENT_RETRY_TIMES_DEFAULT_VALUE;

  public static final String RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED;
  public static final boolean RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED_DEFAULT_VALUE =
      RssClientConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED_DEFAULT_VALUE;

  public static final String RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR;

  public static final double RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR_DEFAULT_VALUE
      = RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR_DEFAULT_VALUE;

  public static final String RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER =
      MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER;
  public static final int RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER_DEFAULT_VALUE =
      RssClientConfig.RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER_DEFAULT_VALUE;

  public static final String RSS_CONF_FILE = "rss_conf.xml";

  public static final Set<String> RSS_MANDATORY_CLUSTER_CONF =
      ImmutableSet.of(RSS_STORAGE_TYPE, RSS_REMOTE_STORAGE_PATH);

  //Whether enable test mode for the MR Client
  public static final String RSS_TEST_MODE_ENABLE = MR_RSS_CONFIG_PREFIX + RssClientConfig.RSS_TEST_MODE_ENABLE;

  public static RssConf toRssConf(JobConf jobConf) {
    RssConf rssConf = new RssConf();
    for (Map.Entry<String, String> entry : jobConf) {
      String key = entry.getKey();
      if (!key.startsWith(MR_RSS_CONFIG_PREFIX)) {
        continue;
      }
      key = key.substring(MR_RSS_CONFIG_PREFIX.length());
      rssConf.setString(key, entry.getValue());
    }
    return rssConf;
  }
}
