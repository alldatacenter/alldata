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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies;

import lombok.Data;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.functions.sink.filesystem.RollingPolicy;

/**
 * An interface exposing the information concerning the current (open) part file
 * that is necessary to the {@link RollingPolicy} in order to determine if it
 * should roll the part file or not.
 */
@PublicEvolving
@Data
public class PartFileInfo {
  private long creationTime;
  private long size;
  private long lastUpdateTime;
  private Path path;
  private String partition;

  public PartFileInfo(long creationTime, long size) {
    this.creationTime = creationTime;
    this.size = size;
    this.lastUpdateTime = creationTime;
  }

  /**
   * @return The creation time (in ms) of the currently open part file.
   */
  public long getCreationTime() {
    return creationTime;
  }

  /**
   * @return The size of the currently open part file.
   */
  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  /**
   * @return The last time (in ms) the currently open part file was written to.
   */
  public long getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setLastUpdateTime(long lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  public void update(int appendSize, long newUpdateTime) {
    this.size += appendSize;
    this.lastUpdateTime = newUpdateTime;
  }
}
