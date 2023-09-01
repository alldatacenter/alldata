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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.deploy.SparkHadoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.CoordinatorClient;
import org.apache.uniffle.client.factory.CoordinatorClientFactory;
import org.apache.uniffle.client.util.ClientUtils;
import org.apache.uniffle.common.ClientType;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.Constants;

public class RssSparkShuffleUtils {

  private static final Logger LOG = LoggerFactory.getLogger(RssSparkShuffleUtils.class);

  public static Configuration newHadoopConfiguration(SparkConf sparkConf) {
    SparkHadoopUtil util = new SparkHadoopUtil();
    Configuration conf = util.newConfiguration(sparkConf);

    boolean useOdfs = sparkConf.get(RssSparkConfig.RSS_OZONE_DFS_NAMENODE_ODFS_ENABLE);
    if (useOdfs) {
      final int OZONE_PREFIX_LEN = "spark.rss.ozone.".length();
      conf.setBoolean(RssSparkConfig.RSS_OZONE_DFS_NAMENODE_ODFS_ENABLE.key().substring(OZONE_PREFIX_LEN), useOdfs);
      conf.set(
          RssSparkConfig.RSS_OZONE_FS_HDFS_IMPL.key().substring(OZONE_PREFIX_LEN),
          sparkConf.get(RssSparkConfig.RSS_OZONE_FS_HDFS_IMPL));
      conf.set(
          RssSparkConfig.RSS_OZONE_FS_ABSTRACT_FILE_SYSTEM_HDFS_IMPL.key().substring(OZONE_PREFIX_LEN),
          sparkConf.get(RssSparkConfig.RSS_OZONE_FS_ABSTRACT_FILE_SYSTEM_HDFS_IMPL));
    }

    return conf;
  }

  public static ShuffleManager loadShuffleManager(String name, SparkConf conf, boolean isDriver) throws Exception {
    Class<?> klass = Class.forName(name);
    Constructor<?> constructor;
    ShuffleManager instance;
    try {
      constructor = klass.getConstructor(conf.getClass(), Boolean.TYPE);
      instance = (ShuffleManager) constructor.newInstance(conf, isDriver);
    } catch (NoSuchMethodException e) {
      constructor = klass.getConstructor(conf.getClass());
      instance = (ShuffleManager) constructor.newInstance(conf);
    }
    return instance;
  }

  public static List<CoordinatorClient> createCoordinatorClients(SparkConf sparkConf) {
    String clientType = sparkConf.get(RssSparkConfig.RSS_CLIENT_TYPE);
    String coordinators = sparkConf.get(RssSparkConfig.RSS_COORDINATOR_QUORUM);
    CoordinatorClientFactory coordinatorClientFactory = new CoordinatorClientFactory(ClientType.valueOf(clientType));
    return coordinatorClientFactory.createCoordinatorClient(coordinators);
  }

  public static void applyDynamicClientConf(SparkConf sparkConf, Map<String, String> confItems) {
    if (sparkConf == null) {
      LOG.warn("Spark conf is null");
      return;
    }

    if (confItems == null || confItems.isEmpty()) {
      LOG.warn("Empty conf items");
      return;
    }

    for (Map.Entry<String, String> kv : confItems.entrySet()) {
      String sparkConfKey = kv.getKey();
      if (!sparkConfKey.startsWith(RssSparkConfig.SPARK_RSS_CONFIG_PREFIX)) {
        sparkConfKey = RssSparkConfig.SPARK_RSS_CONFIG_PREFIX + sparkConfKey;
      }
      String confVal = kv.getValue();
      if (!sparkConf.contains(sparkConfKey) || RssSparkConfig.RSS_MANDATORY_CLUSTER_CONF.contains(sparkConfKey)) {
        LOG.warn("Use conf dynamic conf {} = {}", sparkConfKey, confVal);
        sparkConf.set(sparkConfKey, confVal);
      }
    }
  }

  public static void validateRssClientConf(SparkConf sparkConf) {
    String msgFormat = "%s must be set by the client or fetched from coordinators.";
    if (!sparkConf.contains(RssSparkConfig.RSS_STORAGE_TYPE.key())) {
      String msg = String.format(msgFormat, "Storage type");
      LOG.error(msg);
      throw new IllegalArgumentException(msg);
    }

    String storageType = sparkConf.get(RssSparkConfig.RSS_STORAGE_TYPE.key());
    boolean testMode = sparkConf.getBoolean(RssSparkConfig.RSS_TEST_MODE_ENABLE.key(), false);
    ClientUtils.validateTestModeConf(testMode, storageType);
    int retryMax = sparkConf.get(RssSparkConfig.RSS_CLIENT_RETRY_MAX);
    long retryIntervalMax = sparkConf.get(RssSparkConfig.RSS_CLIENT_RETRY_INTERVAL_MAX);
    long sendCheckTimeout = sparkConf.get(RssSparkConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS);
    if (retryIntervalMax * retryMax > sendCheckTimeout) {
      throw new IllegalArgumentException(String.format("%s(%s) * %s(%s) should not bigger than %s(%s)",
          RssSparkConfig.RSS_CLIENT_RETRY_MAX.key(),
          retryMax,
          RssSparkConfig.RSS_CLIENT_RETRY_INTERVAL_MAX.key(),
          retryIntervalMax,
          RssSparkConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS.key(),
          sendCheckTimeout));
    }
  }

  public static Configuration getRemoteStorageHadoopConf(
      SparkConf sparkConf, RemoteStorageInfo remoteStorageInfo) {
    Configuration readerHadoopConf = RssSparkShuffleUtils.newHadoopConfiguration(sparkConf);
    final Map<String, String> shuffleRemoteStorageConf = remoteStorageInfo.getConfItems();
    if (shuffleRemoteStorageConf != null && !shuffleRemoteStorageConf.isEmpty()) {
      for (Map.Entry<String, String> entry : shuffleRemoteStorageConf.entrySet()) {
        readerHadoopConf.set(entry.getKey(), entry.getValue());
      }
    }
    return readerHadoopConf;
  }

  public static Set<String> getAssignmentTags(SparkConf sparkConf) {
    Set<String> assignmentTags = new HashSet<>();
    String rawTags = sparkConf.get(RssSparkConfig.RSS_CLIENT_ASSIGNMENT_TAGS.key(), "");
    if (StringUtils.isNotEmpty(rawTags)) {
      rawTags = rawTags.trim();
      assignmentTags.addAll(Arrays.asList(rawTags.split(",")));
    }
    assignmentTags.add(Constants.SHUFFLE_SERVER_VERSION);
    return assignmentTags;
  }

  public static int estimateTaskConcurrency(SparkConf sparkConf) {
    int taskConcurrency;
    double dynamicAllocationFactor = sparkConf.get(RssSparkConfig.RSS_ESTIMATE_TASK_CONCURRENCY_DYNAMIC_FACTOR);
    if (dynamicAllocationFactor > 1 || dynamicAllocationFactor < 0) {
      throw new RssException("dynamicAllocationFactor is not valid: " + dynamicAllocationFactor);
    }
    int executorCores = sparkConf.getInt(Constants.SPARK_EXECUTOR_CORES, Constants.SPARK_EXECUTOR_CORES_DEFAULT_VALUE);
    int taskCpus = sparkConf.getInt(Constants.SPARK_TASK_CPUS, Constants.SPARK_TASK_CPUS_DEFAULT_VALUE);
    int taskConcurrencyPerExecutor = Math.floorDiv(executorCores, taskCpus);
    if (!sparkConf.getBoolean(Constants.SPARK_DYNAMIC_ENABLED, false)) {
      int executorInstances = sparkConf.getInt(Constants.SPARK_EXECUTOR_INSTANTS,
          Constants.SPARK_EXECUTOR_INSTANTS_DEFAULT_VALUE);
      taskConcurrency =  executorInstances > 0 ? executorInstances * taskConcurrencyPerExecutor : 0;
    } else {
      // Default is infinity
      int maxExecutors = Math.min(sparkConf.getInt(Constants.SPARK_MAX_DYNAMIC_EXECUTOR,
          Constants.SPARK_DYNAMIC_EXECUTOR_DEFAULT_VALUE), Constants.SPARK_MAX_DYNAMIC_EXECUTOR_LIMIT);
      int minExecutors = sparkConf.getInt(Constants.SPARK_MIN_DYNAMIC_EXECUTOR,
          Constants.SPARK_DYNAMIC_EXECUTOR_DEFAULT_VALUE);
      taskConcurrency = (int)((maxExecutors - minExecutors) * dynamicAllocationFactor + minExecutors)
                            * taskConcurrencyPerExecutor;
    }
    return taskConcurrency;
  }

  public static int getRequiredShuffleServerNumber(SparkConf sparkConf) {
    boolean enabledEstimateServer = sparkConf.get(RssSparkConfig.RSS_ESTIMATE_SERVER_ASSIGNMENT_ENABLED);
    int requiredShuffleServerNumber = sparkConf.get(RssSparkConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER);
    if (!enabledEstimateServer || requiredShuffleServerNumber > 0) {
      return requiredShuffleServerNumber;
    }
    int estimateTaskConcurrency = RssSparkShuffleUtils.estimateTaskConcurrency(sparkConf);
    int taskConcurrencyPerServer = sparkConf.get(RssSparkConfig.RSS_ESTIMATE_TASK_CONCURRENCY_PER_SERVER);
    return (int) Math.ceil(estimateTaskConcurrency * 1.0 / taskConcurrencyPerServer);
  }
}
