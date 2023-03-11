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

package com.netease.arctic.ams.api.client;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;

/**
 * Provides ZooKeeper clients and operations.
 */
public class ZookeeperService {

  private CuratorFramework zkClient;
  private String zkServerAddress;
  private static volatile ZookeeperService instance;

  private ZookeeperService(String zkServerAddress) {
    this.zkServerAddress = zkServerAddress;
    this.zkClient = newClient();
  }

  private CuratorFramework newClient() {
    ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3, 5000);
    CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString(zkServerAddress)
        .sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000)
        .retryPolicy(retryPolicy)
        .build();
    client.start();
    return client;
  }

  public static ZookeeperService getInstance(String zkServerAddress) {
    if (instance == null) {
      synchronized (ZookeeperService.class) {
        if (instance == null) {
          instance = new ZookeeperService(zkServerAddress);
        }
      }
    }
    return instance;
  }

  public boolean exist(String path) throws Exception {
    Stat stat = zkClient.checkExists().forPath(path);
    return stat != null;
  }

  public void create(String path) throws Exception {
    StringBuilder tmpPath = new StringBuilder();
    for (String p : path.split("/")) {
      if (!p.isEmpty()) {
        tmpPath.append("/");
        tmpPath.append(p);
        if (!exist(tmpPath.toString())) {
          zkClient.create().withMode(CreateMode.PERSISTENT).forPath(tmpPath.toString());
        }
      }
    }
  }

  public void setData(String path, String data) throws Exception {
    zkClient.setData().forPath(path, data.getBytes(StandardCharsets.UTF_8));
  }

  public String getData(String path) throws Exception {
    return new String(zkClient.getData().forPath(path), StandardCharsets.UTF_8);
  }

  public void delete(String path) throws Exception {
    zkClient.delete().forPath(path);
  }
}
