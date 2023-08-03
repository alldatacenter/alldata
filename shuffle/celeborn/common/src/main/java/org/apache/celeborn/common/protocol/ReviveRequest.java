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

package org.apache.celeborn.common.protocol;

import org.apache.celeborn.common.protocol.message.StatusCode;

public class ReviveRequest {
  public int shuffleId;
  public int mapId;
  public int attemptId;
  public int partitionId;
  public int epoch;
  public PartitionLocation loc;
  public StatusCode cause;
  public volatile int reviveStatus;

  public ReviveRequest(
      int shuffleId,
      int mapId,
      int attemptId,
      int partitionId,
      int epoch,
      PartitionLocation loc,
      StatusCode cause) {
    this.shuffleId = shuffleId;
    this.mapId = mapId;
    this.attemptId = attemptId;
    this.partitionId = partitionId;
    this.epoch = epoch;
    this.loc = loc;
    this.cause = cause;
    reviveStatus = StatusCode.REVIVE_INITIALIZED.getValue();
  }
}
