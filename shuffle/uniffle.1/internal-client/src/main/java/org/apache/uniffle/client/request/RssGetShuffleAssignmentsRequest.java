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

package org.apache.uniffle.client.request;

import java.util.Set;

import com.google.common.annotations.VisibleForTesting;

public class RssGetShuffleAssignmentsRequest {

  private String appId;
  private int shuffleId;
  private int partitionNum;
  private int partitionNumPerRange;
  private int dataReplica;
  private Set<String> requiredTags;
  private int assignmentShuffleServerNumber;
  private int estimateTaskConcurrency;

  @VisibleForTesting
  public RssGetShuffleAssignmentsRequest(String appId, int shuffleId, int partitionNum,
      int partitionNumPerRange, int dataReplica, Set<String> requiredTags) {
    this(appId, shuffleId, partitionNum, partitionNumPerRange, dataReplica, requiredTags, -1, -1);
  }

  public RssGetShuffleAssignmentsRequest(String appId, int shuffleId, int partitionNum,
      int partitionNumPerRange, int dataReplica, Set<String> requiredTags, int assignmentShuffleServerNumber,
      int estimateTaskConcurrency) {
    this.appId = appId;
    this.shuffleId = shuffleId;
    this.partitionNum = partitionNum;
    this.partitionNumPerRange = partitionNumPerRange;
    this.dataReplica = dataReplica;
    this.requiredTags = requiredTags;
    this.assignmentShuffleServerNumber = assignmentShuffleServerNumber;
    this.estimateTaskConcurrency = estimateTaskConcurrency;
  }

  public String getAppId() {
    return appId;
  }

  public int getShuffleId() {
    return shuffleId;
  }

  public int getPartitionNum() {
    return partitionNum;
  }

  public int getPartitionNumPerRange() {
    return partitionNumPerRange;
  }

  public int getDataReplica() {
    return dataReplica;
  }

  public Set<String> getRequiredTags() {
    return requiredTags;
  }

  public int getAssignmentShuffleServerNumber() {
    return assignmentShuffleServerNumber;
  }

  public int getEstimateTaskConcurrency() {
    return estimateTaskConcurrency;
  }
}
