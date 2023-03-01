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

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.client.factory.ShuffleClientFactory;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.Constants;

public class RssMRUtils {

  private static final Logger LOG = LoggerFactory.getLogger(RssMRUtils.class);
  private static final int MAX_ATTEMPT_LENGTH = 6;
  private static final long MAX_ATTEMPT_ID = (1 << MAX_ATTEMPT_LENGTH) - 1;

  // Class TaskAttemptId have two field id and mapId, rss taskAttemptID have 21 bits,
  // mapId is 19 bits, id is 2 bits. MR have a trick logic, taskAttemptId will increase
  // 1000 * (appAttemptId - 1), so we will decrease it.
  public static long convertTaskAttemptIdToLong(TaskAttemptID taskAttemptID, int appAttemptId) {
    long lowBytes = taskAttemptID.getTaskID().getId();
    if (lowBytes > Constants.MAX_TASK_ATTEMPT_ID) {
      throw new RssException("TaskAttempt " + taskAttemptID + " low bytes " + lowBytes + " exceed");
    }
    if (appAttemptId < 1) {
      throw new RssException("appAttemptId " + appAttemptId + " is wrong");
    }
    long highBytes = (long)taskAttemptID.getId() - (appAttemptId - 1) * 1000;
    if (highBytes > MAX_ATTEMPT_ID || highBytes < 0) {
      throw new RssException("TaskAttempt " + taskAttemptID + " high bytes " + highBytes + " exceed");
    }
    return (highBytes << (Constants.TASK_ATTEMPT_ID_MAX_LENGTH + Constants.PARTITION_ID_MAX_LENGTH)) + lowBytes;
  }

  public static TaskAttemptID createMRTaskAttemptId(
      JobID jobID,
      TaskType taskType,
      long rssTaskAttemptId,
      int appAttemptId) {
    if (appAttemptId < 1) {
      throw new RssException("appAttemptId " + appAttemptId + " is wrong");
    }
    TaskID taskID = new TaskID(jobID, taskType, (int)(rssTaskAttemptId & Constants.MAX_TASK_ATTEMPT_ID));
    return new TaskAttemptID(taskID, (int)(rssTaskAttemptId
        >> (Constants.TASK_ATTEMPT_ID_MAX_LENGTH + Constants.PARTITION_ID_MAX_LENGTH)) + 1000 * (appAttemptId - 1));
  }

  public static ShuffleWriteClient createShuffleClient(JobConf jobConf) {
    int heartBeatThreadNum = jobConf.getInt(RssMRConfig.RSS_CLIENT_HEARTBEAT_THREAD_NUM,
        RssMRConfig.RSS_CLIENT_HEARTBEAT_THREAD_NUM_DEFAULT_VALUE);
    int retryMax = jobConf.getInt(RssMRConfig.RSS_CLIENT_RETRY_MAX,
        RssMRConfig.RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE);
    long retryIntervalMax = jobConf.getLong(RssMRConfig.RSS_CLIENT_RETRY_INTERVAL_MAX,
        RssMRConfig.RSS_CLIENT_RETRY_INTERVAL_MAX_DEFAULT_VALUE);
    String clientType = jobConf.get(RssMRConfig.RSS_CLIENT_TYPE,
        RssMRConfig.RSS_CLIENT_TYPE_DEFAULT_VALUE);
    int replicaWrite = jobConf.getInt(RssMRConfig.RSS_DATA_REPLICA_WRITE,
        RssMRConfig.RSS_DATA_REPLICA_WRITE_DEFAULT_VALUE);
    int replicaRead = jobConf.getInt(RssMRConfig.RSS_DATA_REPLICA_READ,
        RssMRConfig.RSS_DATA_REPLICA_READ_DEFAULT_VALUE);
    int replica = jobConf.getInt(RssMRConfig.RSS_DATA_REPLICA,
        RssMRConfig.RSS_DATA_REPLICA_DEFAULT_VALUE);
    boolean replicaSkipEnabled = jobConf.getBoolean(RssMRConfig.RSS_DATA_REPLICA_SKIP_ENABLED,
        RssMRConfig.RSS_DATA_REPLICA_SKIP_ENABLED_DEFAULT_VALUE);
    int dataTransferPoolSize = jobConf.getInt(RssMRConfig.RSS_DATA_TRANSFER_POOL_SIZE,
        RssMRConfig.RSS_DATA_TRANSFER_POOL_SIZE_DEFAULT_VALUE);
    int dataCommitPoolSize = jobConf.getInt(RssMRConfig.RSS_DATA_COMMIT_POOL_SIZE,
        RssMRConfig.RSS_DATA_COMMIT_POOL_SIZE_DEFAULT_VALUE);
    ShuffleWriteClient client = ShuffleClientFactory
        .getInstance()
        .createShuffleWriteClient(clientType, retryMax, retryIntervalMax,
            heartBeatThreadNum, replica, replicaWrite, replicaRead, replicaSkipEnabled,
            dataTransferPoolSize, dataCommitPoolSize);
    return client;
  }

  public static Set<ShuffleServerInfo> getAssignedServers(JobConf jobConf, int reduceID) {
    String servers = jobConf.get(RssMRConfig.RSS_ASSIGNMENT_PREFIX
        + String.valueOf(reduceID));
    String[] splitServers = servers.split(",");
    Set<ShuffleServerInfo> assignServers = Sets.newHashSet();
    for (String splitServer : splitServers) {
      String[] serverInfo = splitServer.split(":");
      if (serverInfo.length != 2) {
        throw new RssException("partition " + reduceID + " server info isn't right");
      }
      ShuffleServerInfo sever = new ShuffleServerInfo(StringUtils.join(serverInfo, "-"),
          serverInfo[0], Integer.parseInt(serverInfo[1]));
      assignServers.add(sever);
    }
    return assignServers;
  }

  public static ApplicationAttemptId getApplicationAttemptId() {
    String containerIdStr =
        System.getenv(ApplicationConstants.Environment.CONTAINER_ID.name());
    ContainerId containerId = ContainerId.fromString(containerIdStr);
    return containerId.getApplicationAttemptId();
  }

  public static void applyDynamicClientConf(JobConf jobConf, Map<String, String> confItems) {
    if (jobConf == null) {
      LOG.warn("Job conf is null");
      return;
    }

    if (confItems == null || confItems.isEmpty()) {
      LOG.warn("Empty conf items");
      return;
    }

    for (Map.Entry<String, String> kv : confItems.entrySet()) {
      String mrConfKey = kv.getKey();
      if (!mrConfKey.startsWith(RssMRConfig.MR_RSS_CONFIG_PREFIX)) {
        mrConfKey = RssMRConfig.MR_RSS_CONFIG_PREFIX + mrConfKey;
      }
      String mrConfVal = kv.getValue();
      if (StringUtils.isEmpty(jobConf.get(mrConfKey, ""))
          || RssMRConfig.RSS_MANDATORY_CLUSTER_CONF.contains(mrConfKey)) {
        LOG.warn("Use conf dynamic conf {} = {}", mrConfKey, mrConfVal);
        jobConf.set(mrConfKey, mrConfVal);
      }
    }
  }

  public static int getInt(JobConf rssJobConf, JobConf mrJobCOnf, String key, int defaultValue) {
    return rssJobConf.getInt(key,  mrJobCOnf.getInt(key, defaultValue));
  }

  public static long getLong(JobConf rssJobConf, JobConf mrJobConf, String key, long defaultValue) {
    return rssJobConf.getLong(key, mrJobConf.getLong(key, defaultValue));
  }

  public static boolean getBoolean(JobConf rssJobConf, JobConf mrJobConf, String key, boolean defaultValue) {
    return rssJobConf.getBoolean(key, mrJobConf.getBoolean(key, defaultValue));
  }

  public static double getDouble(JobConf rssJobConf, JobConf mrJobConf, String key, double defaultValue) {
    return rssJobConf.getDouble(key, mrJobConf.getDouble(key, defaultValue));
  }

  public static String getString(JobConf rssJobConf, JobConf mrJobConf, String key) {
    return rssJobConf.get(key, mrJobConf.get(key));
  }

  public static String getString(JobConf rssJobConf, JobConf mrJobConf, String key, String defaultValue) {
    return rssJobConf.get(key, mrJobConf.get(key, defaultValue));
  }

  public static long getBlockId(long partitionId, long taskAttemptId, int nextSeqNo) {
    long attemptId = taskAttemptId >> (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH);
    if (attemptId < 0 || attemptId > MAX_ATTEMPT_ID) {
      throw new RuntimeException("Can't support attemptId [" + attemptId
          + "], the max value should be " + MAX_ATTEMPT_ID);
    }
    long  atomicInt = (nextSeqNo << MAX_ATTEMPT_LENGTH) + attemptId;
    if (atomicInt < 0 || atomicInt > Constants.MAX_SEQUENCE_NO) {
      throw new RuntimeException("Can't support sequence [" + atomicInt
          + "], the max value should be " + Constants.MAX_SEQUENCE_NO);
    }
    if (partitionId < 0 || partitionId > Constants.MAX_PARTITION_ID) {
      throw new RuntimeException("Can't support partitionId["
          + partitionId + "], the max value should be " + Constants.MAX_PARTITION_ID);
    }
    long taskId = taskAttemptId - (attemptId
        << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH));
    if (taskId < 0 ||  taskId > Constants.MAX_TASK_ATTEMPT_ID) {
      throw new RuntimeException("Can't support taskId["
          + taskId + "], the max value should be " + Constants.MAX_TASK_ATTEMPT_ID);
    }
    return (atomicInt << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH))
        + (partitionId << Constants.TASK_ATTEMPT_ID_MAX_LENGTH) + taskId;
  }

  public static long getTaskAttemptId(long blockId) {
    long mapId = blockId & Constants.MAX_TASK_ATTEMPT_ID;
    long attemptId = (blockId >> (Constants.TASK_ATTEMPT_ID_MAX_LENGTH + Constants.PARTITION_ID_MAX_LENGTH))
        & MAX_ATTEMPT_ID;
    return (attemptId << (Constants.TASK_ATTEMPT_ID_MAX_LENGTH + Constants.PARTITION_ID_MAX_LENGTH)) + mapId;
  }

  public static int estimateTaskConcurrency(JobConf jobConf) {
    double dynamicFactor = jobConf.getDouble(RssMRConfig.RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR,
        RssMRConfig.RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR_DEFAULT_VALUE);
    double slowStart = jobConf.getDouble(Constants.MR_SLOW_START, Constants.MR_SLOW_START_DEFAULT_VALUE);
    int mapNum = jobConf.getNumMapTasks();
    int reduceNum = jobConf.getNumReduceTasks();
    int mapLimit = jobConf.getInt(Constants.MR_MAP_LIMIT, Constants.MR_MAP_LIMIT_DEFAULT_VALUE);
    int reduceLimit = jobConf.getInt(Constants.MR_REDUCE_LIMIT, Constants.MR_REDUCE_LIMIT_DEFAULT_VALUE);

    int estimateMapNum = mapLimit > 0 ? Math.min(mapNum, mapLimit) : mapNum;
    int estimateReduceNum = reduceLimit > 0 ? Math.min(reduceNum, reduceLimit) : reduceNum;
    if (slowStart == 1) {
      return (int) (Math.max(estimateMapNum, estimateReduceNum) * dynamicFactor);
    } else {
      return (int) (((1 - slowStart) * estimateMapNum + estimateReduceNum) * dynamicFactor);
    }
  }

  public static int getRequiredShuffleServerNumber(JobConf jobConf) {
    int requiredShuffleServerNumber = jobConf.getInt(
        RssMRConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER,
        RssMRConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER_DEFAULT_VALUE
    );
    boolean enabledEstimateServer = jobConf.getBoolean(
        RssMRConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED,
        RssMRConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED_DEFAULT_VALUE
    );
    if (!enabledEstimateServer || requiredShuffleServerNumber > 0) {
      return requiredShuffleServerNumber;
    }
    int taskConcurrency = estimateTaskConcurrency(jobConf);
    int taskConcurrencyPerServer = jobConf.getInt(RssMRConfig.RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER,
        RssMRConfig.RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER_DEFAULT_VALUE);
    return (int) Math.ceil(taskConcurrency * 1.0 / taskConcurrencyPerServer);
  }

  public static void validateRssClientConf(JobConf rssJobConf, JobConf mrJobConf) {
    int retryMax = getInt(rssJobConf, mrJobConf, RssMRConfig.RSS_CLIENT_RETRY_MAX,
        RssMRConfig.RSS_CLIENT_RETRY_MAX_DEFAULT_VALUE);
    long retryIntervalMax = getLong(rssJobConf, mrJobConf, RssMRConfig.RSS_CLIENT_RETRY_INTERVAL_MAX,
        RssMRConfig.RSS_CLIENT_RETRY_INTERVAL_MAX_DEFAULT_VALUE);
    long sendCheckTimeout = getLong(rssJobConf, mrJobConf, RssMRConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS,
        RssMRConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS_DEFAULT_VALUE);
    if (retryIntervalMax * retryMax > sendCheckTimeout) {
      throw new IllegalArgumentException(String.format("%s(%s) * %s(%s) should not bigger than %s(%s)",
          RssMRConfig.RSS_CLIENT_RETRY_MAX,
          retryMax,
          RssMRConfig.RSS_CLIENT_RETRY_INTERVAL_MAX,
          retryIntervalMax,
          RssMRConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS,
          sendCheckTimeout));
    }
  }
}
