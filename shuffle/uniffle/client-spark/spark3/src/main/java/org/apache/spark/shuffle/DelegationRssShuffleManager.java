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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkConf;
import org.apache.spark.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.CoordinatorClient;
import org.apache.uniffle.client.request.RssAccessClusterRequest;
import org.apache.uniffle.client.response.RssAccessClusterResponse;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.RetryUtils;

import static org.apache.uniffle.common.util.Constants.ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM;

public class DelegationRssShuffleManager implements ShuffleManager {

  private static final Logger LOG = LoggerFactory.getLogger(DelegationRssShuffleManager.class);

  private final ShuffleManager delegate;
  private final List<CoordinatorClient> coordinatorClients;
  private final int accessTimeoutMs;
  private final SparkConf sparkConf;
  private String user;
  private String uuid;

  public DelegationRssShuffleManager(SparkConf sparkConf, boolean isDriver) throws Exception {
    this.sparkConf = sparkConf;
    accessTimeoutMs = sparkConf.get(RssSparkConfig.RSS_ACCESS_TIMEOUT_MS);
    if (isDriver) {
      coordinatorClients = RssSparkShuffleUtils.createCoordinatorClients(sparkConf);
      delegate = createShuffleManagerInDriver();
    } else {
      coordinatorClients = Lists.newArrayList();
      delegate = createShuffleManagerInExecutor();
    }

    if (delegate == null) {
      throw new RssException("Fail to create shuffle manager!");
    }
  }

  private ShuffleManager createShuffleManagerInDriver() throws RssException {
    ShuffleManager shuffleManager;
    user = "user";
    try {
      user = UserGroupInformation.getCurrentUser().getShortUserName();
    } catch (Exception e) {
      LOG.error("Error on getting user from ugi." + e);
    }
    boolean canAccess = tryAccessCluster();
    if (uuid == null || "".equals(uuid)) {
      uuid = String.valueOf(System.currentTimeMillis());
    }
    if (canAccess) {
      try {
        sparkConf.set("spark.rss.quota.user", user);
        sparkConf.set("spark.rss.quota.uuid", uuid);
        shuffleManager = new RssShuffleManager(sparkConf, true);
        sparkConf.set(RssSparkConfig.RSS_ENABLED.key(), "true");
        sparkConf.set("spark.shuffle.manager", RssShuffleManager.class.getCanonicalName());
        LOG.info("Use RssShuffleManager");
        return shuffleManager;
      } catch (Exception exception) {
        LOG.warn("Fail to create RssShuffleManager, fallback to SortShuffleManager {}", exception.getMessage());
      }
    }

    try {
      shuffleManager = RssSparkShuffleUtils.loadShuffleManager(Constants.SORT_SHUFFLE_MANAGER_NAME, sparkConf, true);
      sparkConf.set(RssSparkConfig.RSS_ENABLED.key(), "false");
      sparkConf.set("spark.shuffle.manager", "sort");
      LOG.info("Use SortShuffleManager");
    } catch (Exception e) {
      throw new RssException(e.getMessage());
    }

    return shuffleManager;
  }

  private boolean tryAccessCluster() {
    String accessId = sparkConf.get(RssSparkConfig.RSS_ACCESS_ID.key(), "").trim();
    if (StringUtils.isEmpty(accessId)) {
      LOG.warn("Access id key is empty");
      return false;
    }
    long retryInterval = sparkConf.get(RssSparkConfig.RSS_CLIENT_ACCESS_RETRY_INTERVAL_MS);
    int retryTimes = sparkConf.get(RssSparkConfig.RSS_CLIENT_ACCESS_RETRY_TIMES);

    int assignmentShuffleNodesNum = sparkConf.get(RssSparkConfig.RSS_CLIENT_ASSIGNMENT_SHUFFLE_SERVER_NUMBER);
    Map<String, String> extraProperties = Maps.newHashMap();
    extraProperties.put(ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM, String.valueOf(assignmentShuffleNodesNum));

    for (CoordinatorClient coordinatorClient : coordinatorClients) {
      Set<String> assignmentTags = RssSparkShuffleUtils.getAssignmentTags(sparkConf);
      boolean canAccess;
      try {
        canAccess = RetryUtils.retry(() -> {
          RssAccessClusterResponse response = coordinatorClient.accessCluster(new RssAccessClusterRequest(
              accessId, assignmentTags, accessTimeoutMs, extraProperties, user));
          if (response.getStatusCode() == StatusCode.SUCCESS) {
            LOG.warn("Success to access cluster {} using {}", coordinatorClient.getDesc(), accessId);
            uuid = response.getUuid();
            return true;
          } else if (response.getStatusCode() == StatusCode.ACCESS_DENIED) {
            throw new RssException("Request to access cluster " + coordinatorClient.getDesc() + " is denied using "
                + accessId + " for " + response.getMessage());
          } else {
            throw new RssException("Fail to reach cluster " + coordinatorClient.getDesc()
                + " for " + response.getMessage());
          }
        }, retryInterval, retryTimes);
        return canAccess;
      } catch (Throwable e) {
        LOG.warn("Fail to access cluster {} using {} for {}",
            coordinatorClient.getDesc(), accessId, e.getMessage());
      }
    }

    return false;
  }

  private ShuffleManager createShuffleManagerInExecutor() throws RssException {
    ShuffleManager shuffleManager;
    // get useRSS from spark conf
    boolean useRSS = sparkConf.get(RssSparkConfig.RSS_ENABLED);
    if (useRSS) {
      // Executor will not do any fallback
      shuffleManager = new RssShuffleManager(sparkConf, false);
      LOG.info("Use RssShuffleManager");
    } else {
      try {
        shuffleManager = RssSparkShuffleUtils.loadShuffleManager(
            Constants.SORT_SHUFFLE_MANAGER_NAME, sparkConf, false);
        LOG.info("Use SortShuffleManager");
      } catch (Exception e) {
        throw new RssException(e.getMessage());
      }
    }
    return shuffleManager;
  }

  public ShuffleManager getDelegate() {
    return delegate;
  }

  @Override
  public <K, V, C> ShuffleHandle registerShuffle(int shuffleId, ShuffleDependency<K, V, C> dependency) {
    return delegate.registerShuffle(shuffleId, dependency);
  }

  @Override
  public <K, V> ShuffleWriter<K, V> getWriter(
      ShuffleHandle handle,
      long mapId,
      TaskContext context,
      ShuffleWriteMetricsReporter metrics) {
    return delegate.getWriter(handle, mapId, context, metrics);
  }

  @Override
  public <K, C> ShuffleReader<K, C> getReader(
      ShuffleHandle handle,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    return delegate.getReader(handle,
        startPartition, endPartition, context, metrics);
  }

  // The interface is only used for compatibility with spark 3.1.2
  public <K, C> ShuffleReader<K, C> getReader(
      ShuffleHandle handle,
      int startMapIndex,
      int endMapIndex,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    ShuffleReader<K, C> reader = null;
    try {
      reader = (ShuffleReader<K, C>)delegate.getClass().getDeclaredMethod(
          "getReader",
          ShuffleHandle.class,
          int.class,
          int.class,
          int.class,
          int.class,
          TaskContext.class,
          ShuffleReadMetricsReporter.class).invoke(
          handle,
          startMapIndex,
          endMapIndex,
          startPartition,
          endPartition,
          context,
          metrics);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return reader;
  }

  // The interface is only used for compatibility with spark 3.0.1
  public <K, C> ShuffleReader<K, C> getReaderForRange(
      ShuffleHandle handle,
      int startMapIndex,
      int endMapIndex,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    ShuffleReader<K, C> reader = null;
    try {
      reader = (ShuffleReader<K, C>)delegate.getClass().getDeclaredMethod(
          "getReaderForRange",
          ShuffleHandle.class,
          int.class,
          int.class,
          int.class,
          int.class,
          TaskContext.class,
          ShuffleReadMetricsReporter.class).invoke(
          handle,
          startMapIndex,
          endMapIndex,
          startPartition,
          endPartition,
          context,
          metrics);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return reader;
  }

  @Override
  public boolean unregisterShuffle(int shuffleId) {
    return delegate.unregisterShuffle(shuffleId);
  }

  @Override
  public void stop() {
    delegate.stop();
    coordinatorClients.forEach(CoordinatorClient::close);
  }

  @Override
  public ShuffleBlockResolver shuffleBlockResolver() {
    return delegate.shuffleBlockResolver();
  }
}
