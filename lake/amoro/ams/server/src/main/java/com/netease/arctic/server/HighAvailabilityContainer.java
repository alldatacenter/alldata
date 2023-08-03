/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.ams.api.client.AmsServerInfo;
import com.netease.arctic.ams.api.properties.AmsHAProperties;
import com.netease.arctic.server.utils.Configurations;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class HighAvailabilityContainer implements LeaderLatchListener {

  public static final Logger LOG = LoggerFactory.getLogger(HighAvailabilityContainer.class);

  private final LeaderLatch leaderLatch;
  private final CuratorFramework zkClient;
  private final String tableServiceMasterPath;
  private final String optimizingServiceMasterPath;
  private final AmsServerInfo tableServiceServerInfo;
  private final AmsServerInfo optimizingServiceServerInfo;
  private transient CountDownLatch followerLath;

  public HighAvailabilityContainer(Configurations serviceConfig) throws Exception {
    if (serviceConfig.getBoolean(ArcticManagementConf.HA_ENABLE)) {
      String zkServerAddress = serviceConfig.getString(ArcticManagementConf.HA_ZOOKEEPER_ADDRESS);
      String haClusterName = serviceConfig.getString(ArcticManagementConf.HA_CLUSTER_NAME);
      tableServiceMasterPath = AmsHAProperties.getTableServiceMasterPath(haClusterName);
      optimizingServiceMasterPath = AmsHAProperties.getOptimizingServiceMasterPath(haClusterName);
      ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3, 5000);
      this.zkClient = CuratorFrameworkFactory.builder()
          .connectString(zkServerAddress)
          .sessionTimeoutMs(5000)
          .connectionTimeoutMs(5000)
          .retryPolicy(retryPolicy)
          .build();
      zkClient.start();
      createPathIfNeeded(tableServiceMasterPath);
      createPathIfNeeded(optimizingServiceMasterPath);
      String leaderPath = AmsHAProperties.getLeaderPath(haClusterName);
      createPathIfNeeded(leaderPath);
      leaderLatch = new LeaderLatch(zkClient, leaderPath);
      leaderLatch.addListener(this);
      leaderLatch.start();
      this.tableServiceServerInfo = buildServerInfo(
          serviceConfig.getString(ArcticManagementConf.SERVER_EXPOSE_HOST),
          serviceConfig.getInteger(ArcticManagementConf.TABLE_SERVICE_THRIFT_BIND_PORT));
      this.optimizingServiceServerInfo = buildServerInfo(
          serviceConfig.getString(ArcticManagementConf.SERVER_EXPOSE_HOST),
          serviceConfig.getInteger(ArcticManagementConf.OPTIMIZING_SERVICE_THRIFT_BIND_PORT));
    } else {
      leaderLatch = null;
      zkClient = null;
      tableServiceMasterPath = null;
      optimizingServiceMasterPath = null;
      tableServiceServerInfo = null;
      optimizingServiceServerInfo = null;
      // block follower latch forever when ha is disabled
      followerLath = new CountDownLatch(1);
    }
  }

  public void waitLeaderShip() throws Exception {
    LOG.info("Waiting to become the leader of AMS");
    if (leaderLatch != null) {
      leaderLatch.await();
      if (leaderLatch.hasLeadership()) {
        zkClient.setData()
            .forPath(
                tableServiceMasterPath,
                JSONObject.toJSONString(tableServiceServerInfo).getBytes(StandardCharsets.UTF_8));
        zkClient.setData()
            .forPath(
                optimizingServiceMasterPath,
                JSONObject.toJSONString(optimizingServiceServerInfo).getBytes(StandardCharsets.UTF_8));
      }
    }
    LOG.info("Became the leader of AMS");
  }

  public void waitFollowerShip() throws Exception {
    LOG.info("Waiting to become the follower of AMS");
    if (followerLath != null) {
      followerLath.await();
    }
    LOG.info("Became the follower of AMS");
  }

  public void close() {
    if (leaderLatch != null) {
      try {
        this.zkClient.close();
        this.leaderLatch.close();
      } catch (IOException e) {
        LOG.error("Close high availability services failed", e);
      }
    }
  }

  @Override
  public void isLeader() {
    LOG.info("Table service server {} and optimizing service server {} got leadership",
        tableServiceServerInfo.toString(), optimizingServiceServerInfo.toString());
    followerLath = new CountDownLatch(1);
  }

  @Override
  public void notLeader() {
    LOG.info("Table service server {} and optimizing service server {} lost leadership",
        tableServiceServerInfo.toString(), optimizingServiceServerInfo.toString());
    followerLath.countDown();
  }

  private AmsServerInfo buildServerInfo(String host, int port) {
    AmsServerInfo amsServerInfo = new AmsServerInfo();
    amsServerInfo.setHost(host);
    amsServerInfo.setThriftBindPort(port);
    return amsServerInfo;
  }

  private void createPathIfNeeded(String path) throws Exception {
    try {
      zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
    } catch (KeeperException.NodeExistsException e) {
      // ignore
    }
  }
}
