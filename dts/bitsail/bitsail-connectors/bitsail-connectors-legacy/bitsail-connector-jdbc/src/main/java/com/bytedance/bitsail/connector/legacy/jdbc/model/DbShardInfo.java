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

import java.io.Serializable;

/**
 * Desc:
 */
public class DbShardInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String host;
  private int port;
  private String dbURL;
  private int shardNum;

  public DbShardInfo(String host, int port, String dbURL) {
    this.host = host;
    this.port = port;
    this.dbURL = dbURL;
  }

  public DbShardInfo(String host, int port, String dbURL, int shardNum) {
    this.host = host;
    this.port = port;
    this.dbURL = dbURL;
    this.shardNum = shardNum;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DbShardInfo shardInfo = (DbShardInfo) o;

    if (port != shardInfo.port) {
      return false;
    }
    if (shardNum != shardInfo.shardNum) {
      return false;
    }
    if (host != null ? !host.equals(shardInfo.host) : shardInfo.host != null) {
      return false;
    }
    return dbURL != null ? dbURL.equals(shardInfo.dbURL) : shardInfo.dbURL == null;
  }

  @Override
  public int hashCode() {
    int result = host != null ? host.hashCode() : 0;
    result = 31 * result + port;
    result = 31 * result + (dbURL != null ? dbURL.hashCode() : 0);
    result = 31 * result + shardNum;
    return result;
  }

  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

  public String getDbURL() {
    return dbURL;
  }

  public int getShardNum() {
    return shardNum;
  }

  @Override
  public String toString() {
    StringBuilder summary = new StringBuilder();
    summary.append("(")
        .append("shard num: ").append(shardNum)
        .append("\t")
        .append(host).append(":").append(port)
        .append("\t")
        .append(dbURL)
        .append(")");
    return summary.toString();
  }

}
