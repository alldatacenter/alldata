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

import com.netease.arctic.ams.api.OptimizerDescriptor;

import java.sql.Timestamp;
import java.util.Map;

public class Optimizer {
  private int jobId;
  private String groupName;
  private TableTaskStatus jobStatus;
  private int coreNumber;
  private long memory;
  private int parallelism;
  private String jobmanagerUrl;
  private String queueId;
  private byte[] instance;
  private Map<String, String> stateInfo;
  private Timestamp updateTime;

  private String container;

  private String containerType;

  public OptimizerDescriptor convertToDescriptor() {
    return new OptimizerDescriptor(jobId, Integer.parseInt(queueId), groupName, coreNumber, memory, container,
        jobStatus.name(), updateTime.getTime());
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public int getJobId() {
    return jobId;
  }

  public void setJobId(int jobId) {
    this.jobId = jobId;
  }

  public TableTaskStatus getJobStatus() {
    return jobStatus;
  }

  public void setJobStatus(TableTaskStatus jobStatus) {
    this.jobStatus = jobStatus;
  }

  public int getCoreNumber() {
    return coreNumber;
  }

  public void setCoreNumber(int coreNumber) {
    this.coreNumber = coreNumber;
  }

  public long getMemory() {
    return memory;
  }

  public void setMemory(int memory) {
    this.memory = memory;
  }

  public int getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  public String getJobmanagerUrl() {
    return jobmanagerUrl;
  }

  public void setJobmanagerUrl(String jobmanagerUrl) {
    this.jobmanagerUrl = jobmanagerUrl;
  }

  public String getContainer() {
    return container;
  }

  public void setContainer(String container) {
    this.container = container;
  }

  public String getQueueId() {
    return queueId;
  }

  public void setQueueId(String queueId) {
    this.queueId = queueId;
  }

  public byte[] getInstance() {
    return instance;
  }

  public void setInstance(byte[] instance) {
    this.instance = instance;
  }

  public Map<String, String> getStateInfo() {
    return stateInfo;
  }

  public void setStateInfo(Map<String, String> stateInfo) {
    this.stateInfo = stateInfo;
  }

  public Timestamp getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Timestamp updateTime) {
    this.updateTime = updateTime;
  }

  public String getContainerType() {
    return containerType;
  }

  public void setContainerType(String containerType) {
    this.containerType = containerType;
  }
}