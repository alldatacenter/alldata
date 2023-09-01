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

package org.apache.uniffle.storage.common;

import java.util.List;

public class StorageWriteMetrics {

  private final String appId;
  private final int shuffleId;
  private final long eventSize;
  private final long writeBlocks;
  private final long writeTime;
  private final long dataSize;
  private final List<Integer> partitions;

  public StorageWriteMetrics(
      long eventSize,
      long writeBlocks,
      long writeTime,
      long dataSize,
      List<Integer> partitions,
      String appId,
      int shuffleId) {
    this.writeBlocks = writeBlocks;
    this.eventSize = eventSize;
    this.writeTime = writeTime;
    this.dataSize = dataSize;
    this.partitions = partitions;
    this.appId = appId;
    this.shuffleId = shuffleId;
  }

  public long getEventSize() {
    return eventSize;
  }

  public long getWriteBlocks() {
    return writeBlocks;
  }

  public long getWriteTime() {
    return writeTime;
  }

  public long getDataSize() {
    return dataSize;
  }

  public List<Integer> getPartitions() {
    return partitions;
  }

  public String getAppId() {
    return appId;
  }

  public int getShuffleId() {
    return shuffleId;
  }
}
