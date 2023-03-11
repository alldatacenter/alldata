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

package com.netease.arctic.ams.server;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.ams.api.client.AmsServerInfo;
import com.netease.arctic.ams.api.properties.AmsHAProperties;
import com.netease.arctic.ams.server.utils.ZookeeperUtils;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HighAvailabilityServices {

  public static final Logger LOG = LoggerFactory.getLogger(HighAvailabilityServices.class);

  private final LeaderLatch leaderLatch;
  private  volatile HighAvailabilityServices instance;
  private  final List<LeaderLatchListener> listeners = new ArrayList<>();
  private  ZookeeperUtils zkService;
  private final String namespace;

  public HighAvailabilityServices(String zkServerAddress, String namespace) {
    this.namespace = namespace;
    zkService = new ZookeeperUtils(zkServerAddress);
    String lockPath = AmsHAProperties.getLeaderPath(namespace);
    try {
      zkService.create(lockPath);
    } catch (Exception e) {
      e.printStackTrace();
    }
    leaderLatch = new LeaderLatch(zkService.getZkClient(), lockPath);
  }

  public void addListener(LeaderLatchListener listener) {
    listeners.add(listener);
  }

  public void leaderLatch() throws Exception {
    listeners.forEach(leaderLatch::addListener);
    leaderLatch.start();
    leaderLatch.await();
  }

  public AmsServerInfo getNodeInfo(String host, int port) {
    AmsServerInfo amsServerInfo = new AmsServerInfo();
    amsServerInfo.setHost(host);
    amsServerInfo.setThriftBindPort(port);
    return amsServerInfo;
  }

  public AmsServerInfo getMaster() throws Exception {
    return JSONObject.parseObject(
        zkService.getData(AmsHAProperties.getMasterPath(namespace)),
        AmsServerInfo.class);
  }

  public void close() {
    try {
      this.zkService.close();
      this.leaderLatch.close();
    } catch (IOException e) {
      LOG.error("close HighAvailabilityServices error");
    }
  }
}
