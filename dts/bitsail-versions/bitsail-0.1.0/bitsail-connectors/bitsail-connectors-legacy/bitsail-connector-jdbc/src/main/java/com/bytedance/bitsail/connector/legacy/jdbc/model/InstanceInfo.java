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

import com.bytedance.bitsail.common.util.Pair;

/**
 * @desc:
 */
public class InstanceInfo {
  private String host;
  private int port;
  private Pair<Integer, Integer> shardNumRange;
  private boolean isSharded;
  private boolean isMaster;

  public InstanceInfo(String host, int port, boolean isSharded, boolean isMaster) {
    this.host = host;
    this.port = port;
    this.isSharded = isSharded;
    this.isMaster = isMaster;
  }

  public InstanceInfo(String host, int port, boolean isSharded, Pair<Integer, Integer> shardNumRange, boolean isMaster) {
    this.host = host;
    this.port = port;
    this.isSharded = isSharded;
    this.shardNumRange = shardNumRange;
    this.isMaster = isMaster;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public Pair<Integer, Integer> getShardNumRange() {
    return shardNumRange;
  }

  public boolean isSharded() {
    return isSharded;
  }

  public boolean isMaster() {
    return isMaster;
  }

  @Override
  public boolean equals(Object obj) {
    InstanceInfo other = (InstanceInfo) obj;
    return this.host == other.host
        && this.port == other.port
        && this.shardNumRange == other.shardNumRange
        && this.isSharded == other.isSharded
        && this.isMaster == other.isMaster;
  }

  @Override
  public int hashCode() {
    int result = host.hashCode();
    result = 31 * result + port;
    result = 31 * result + shardNumRange.hashCode();
    result = 31 * result + (isSharded ? 1 : 0);
    result = 31 * result + (isMaster ? 1 : 0);
    return result;
  }
}
