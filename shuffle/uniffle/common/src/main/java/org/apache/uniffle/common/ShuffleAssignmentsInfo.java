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

package org.apache.uniffle.common;

import java.util.List;
import java.util.Map;

public class ShuffleAssignmentsInfo {

  private Map<Integer, List<ShuffleServerInfo>> partitionToServers;
  private Map<ShuffleServerInfo, List<PartitionRange>> serverToPartitionRanges;

  public ShuffleAssignmentsInfo(
      Map<Integer, List<ShuffleServerInfo>> partitionToServers,
      Map<ShuffleServerInfo, List<PartitionRange>> serverToPartitionRanges) {
    this.partitionToServers = partitionToServers;
    this.serverToPartitionRanges = serverToPartitionRanges;
  }

  public Map<Integer, List<ShuffleServerInfo>> getPartitionToServers() {
    return partitionToServers;
  }

  public Map<ShuffleServerInfo, List<PartitionRange>> getServerToPartitionRanges() {
    return serverToPartitionRanges;
  }
}
