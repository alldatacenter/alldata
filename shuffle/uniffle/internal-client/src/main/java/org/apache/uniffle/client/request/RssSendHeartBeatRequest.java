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


import java.util.Map;
import java.util.Set;

import org.apache.uniffle.common.storage.StorageInfo;

public class RssSendHeartBeatRequest {

  private final String shuffleServerId;
  private final String shuffleServerIp;
  private final int shuffleServerPort;
  private final long usedMemory;
  private final long preAllocatedMemory;
  private final long availableMemory;
  private final int eventNumInFlush;
  private final Set<String> tags;
  private final long timeout;
  private final boolean isHealthy;
  private final Map<String, StorageInfo> storageInfo;

  public RssSendHeartBeatRequest(
      String shuffleServerId,
      String shuffleServerIp,
      int shuffleServerPort,
      long usedMemory,
      long preAllocatedMemory,
      long availableMemory,
      int eventNumInFlush,
      long timeout,
      Set<String> tags,
      boolean isHealthy,
      Map<String, StorageInfo> storageInfo) {
    this.shuffleServerId = shuffleServerId;
    this.shuffleServerIp = shuffleServerIp;
    this.shuffleServerPort = shuffleServerPort;
    this.usedMemory = usedMemory;
    this.preAllocatedMemory = preAllocatedMemory;
    this.availableMemory = availableMemory;
    this.eventNumInFlush = eventNumInFlush;
    this.tags = tags;
    this.timeout = timeout;
    this.isHealthy = isHealthy;
    this.storageInfo = storageInfo;
  }

  public String getShuffleServerId() {
    return shuffleServerId;
  }

  public String getShuffleServerIp() {
    return shuffleServerIp;
  }

  public int getShuffleServerPort() {
    return shuffleServerPort;
  }

  public long getTimeout() {
    return timeout;
  }

  public long getUsedMemory() {
    return usedMemory;
  }

  public long getPreAllocatedMemory() {
    return preAllocatedMemory;
  }

  public long getAvailableMemory() {
    return availableMemory;
  }

  public int getEventNumInFlush() {
    return eventNumInFlush;
  }

  public Set<String> getTags() {
    return tags;
  }

  public boolean isHealthy() {
    return isHealthy;
  }

  public Map<String, StorageInfo> getStorageInfo() {
    return storageInfo;
  }
}
