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

package org.apache.celeborn.plugin.flink;

import java.io.Serializable;

public class ShuffleResourceDescriptor implements Serializable {

  private static final long serialVersionUID = -1251659747395561342L;

  private final int shuffleId;
  private final int mapId;
  private final int attemptId;
  private final int partitionId;

  public ShuffleResourceDescriptor(int shuffleId, int mapId, int attemptId, int partitionId) {
    this.shuffleId = shuffleId;
    this.mapId = mapId;
    this.attemptId = attemptId;
    this.partitionId = partitionId;
  }

  public int getShuffleId() {
    return shuffleId;
  }

  public int getMapId() {
    return mapId;
  }

  public int getAttemptId() {
    return attemptId;
  }

  public int getPartitionId() {
    return partitionId;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ShuffleResourceDescriptor{");
    sb.append("shuffleId=").append(shuffleId);
    sb.append(", mapId=").append(mapId);
    sb.append(", attemptId=").append(attemptId);
    sb.append(", partitionId=").append(partitionId);
    sb.append('}');
    return sb.toString();
  }
}
