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

package com.bytedance.bitsail.connector.legacy.jdbc.model;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.JDBCPluginErrorCode;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Desc:
 */
@Slf4j
public class DbClusterInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private int maxIdle;
  private int maxActive;
  private int initConn;
  private long evictableIdleTime;

  private Map<Integer, List<DbShardInfo>> shardsInfo = new TreeMap<>();
  private Map<Integer, List<String>> tablesInfo = new TreeMap<>();

  //todo support the difference schema
  private String schema;

  /**
   * Single instance primary key, only support for numeric type
   */
  private String splitPK;

  /**
   * Uniq key for global table
   */
  private List<String> shardKey;

  private String username;
  private String password;

  public DbClusterInfo(Map<Integer, List<DbShardInfo>> shardsInfo, String username, String password,
                       String splitPK, List<String> shardKey,
                       int maxIdle, int maxActive, int initConn, long evictableIdleTime) {
    this.shardsInfo = shardsInfo;
    this.username = username;
    this.password = password;
    this.splitPK = splitPK;
    this.shardKey = shardKey;
    this.maxIdle = maxIdle;
    this.maxActive = maxActive;
    this.initConn = initConn;
    this.evictableIdleTime = evictableIdleTime;
  }

  public DbClusterInfo(String username, String password, String schema,
                       String splitPK, List<String> shardKey,
                       int maxIdle, int maxActive, int initConn, long evictableIdleTime) {
    this.username = username;
    this.password = password;
    this.schema = schema;
    this.splitPK = splitPK;
    this.shardKey = shardKey;
    this.maxIdle = maxIdle;
    this.maxActive = maxActive;
    this.initConn = initConn;
    this.evictableIdleTime = evictableIdleTime;
  }

  public DbClusterInfo() {

  }

  public DbClusterInfo(String userName,
                       String password,
                       String schema,
                       String splitPK,
                       List<String> shardKey,
                       int maxIdle,
                       int maxActive,
                       int initConn,
                       long evictableIdleTime,
                       List<ClusterInfo> clusterInfos) {

    this(userName, password, schema, splitPK, shardKey, maxIdle, maxActive, initConn, evictableIdleTime);

    for (ClusterInfo clusterInfo : clusterInfos) {

      int shardNum = clusterInfo.getShardNumber();
      List<ConnectionInfo> slaves = clusterInfo.getSlaves();
      if (StringUtils.isNotEmpty(clusterInfo.getTableNames())) {
        addShardTables(shardNum, Lists.newArrayList(
            StringUtils.split(clusterInfo.getTableNames(), ",")));
      }

      if (CollectionUtils.isEmpty(slaves)) {
        throw BitSailException.asBitSailException(JDBCPluginErrorCode.CONNECTION_ERROR,
            "Get DB topology information error, the slaves info is null! Connection Json string is " + JsonSerializer.serialize(
                clusterInfo
            ));
      }
      for (ConnectionInfo connectionInfo : slaves) {
        addShard(shardNum, new DbShardInfo(
            connectionInfo.getHost(),
            connectionInfo.getPort(),
            connectionInfo.getUrl(),
            shardNum
        ));
      }
    }

  }

  public Map<Integer, List<DbShardInfo>> getShardsInfo() {
    return shardsInfo;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getSplitPK() {
    return splitPK;
  }

  public void setSplitPK(String splitPK) {
    this.splitPK = splitPK;
  }

  public int getMaxIdle() {
    return maxIdle;
  }

  public void setMaxIdle(int maxIdle) {
    this.maxIdle = maxIdle;
  }

  public int getMaxActive() {
    return maxActive;
  }

  public void setMaxActive(int maxActive) {
    this.maxActive = maxActive;
  }

  public int getInitConn() {
    return initConn;
  }

  public void setInitConn(int initConn) {
    this.initConn = initConn;
  }

  public long getEvictableIdleTime() {
    return evictableIdleTime;
  }

  public void setEvictableIdleTime(long evictableIdleTime) {
    this.evictableIdleTime = evictableIdleTime;
  }

  public int getShardNum() {
    return shardsInfo.size();
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public synchronized void addShard(int shardNum, DbShardInfo shardInfo) {
    List<DbShardInfo> slaves = shardsInfo.get(shardNum);
    if (null == slaves) {
      slaves = new ArrayList<>();
      shardsInfo.put(shardNum, slaves);
    }

    slaves.add(shardInfo);
  }

  public synchronized void addShardTables(int shardNum, List<String> tableList) {
    tablesInfo.put(shardNum, tableList);
  }

  public List<DbShardInfo> getSlaves(int shardNum) {
    return shardsInfo.get(shardNum);
  }

  public List<String> getShardTables(int shardNum) {
    return tablesInfo.get(shardNum);
  }

  public int getDistinctHostsNumber() {
    final Set<String> distinctHosts = shardsInfo.values().stream()
        .flatMap(shardList ->
            shardList.stream().map(DbShardInfo::getHost))
        .collect(Collectors.toSet());

    log.info("Distinct hosts in DbClusterInfo: {}", distinctHosts);
    return distinctHosts.size();
  }
}
