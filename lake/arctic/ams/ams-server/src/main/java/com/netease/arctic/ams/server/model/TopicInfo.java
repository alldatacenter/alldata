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

package com.netease.arctic.ams.server.model;

import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.Objects;

public class TopicInfo {
  private String topicName;
  private int replication;
  private int partition;

  public TopicInfo() {
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public int getReplication() {
    return replication;
  }

  public void setReplication(int replication) {
    this.replication = replication;
  }

  public int getPartition() {
    return partition;
  }

  public void setPartition(int partition) {
    this.partition = partition;
  }

  @Override
  public String toString() {
    return "TopicInfo{" +
        "topicName='" + topicName + '\'' +
        ", replication=" + replication +
        ", partition=" + partition +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TopicInfo topicInfo = (TopicInfo) o;
    return replication == topicInfo.replication &&
        partition == topicInfo.partition &&
        Objects.equals(topicName, topicInfo.topicName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topicName, replication, partition);
  }

  public void validate() {
    Preconditions.checkArgument(StringUtils.isNotBlank(topicName), "topicName can not be blank");
    Preconditions.checkArgument(replication > 0, "replication can not be negative or 0");
    Preconditions.checkArgument(partition > 0, "partition can not be negative or 0");
  }
}
