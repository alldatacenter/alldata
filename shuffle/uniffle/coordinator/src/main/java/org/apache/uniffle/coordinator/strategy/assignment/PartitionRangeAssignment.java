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

package org.apache.uniffle.coordinator.strategy.assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.google.common.base.Objects;

import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.coordinator.ServerNode;
import org.apache.uniffle.proto.RssProtos;

public class PartitionRangeAssignment {

  private final SortedMap<PartitionRange, List<ServerNode>> assignments;

  public PartitionRangeAssignment(SortedMap<PartitionRange, List<ServerNode>> assignments) {
    this.assignments = assignments;
  }

  public List<RssProtos.PartitionRangeAssignment> convertToGrpcProto() {
    List<RssProtos.PartitionRangeAssignment> praList = new ArrayList<>();
    if (assignments == null) {
      return praList;
    }

    for (Entry<PartitionRange, List<ServerNode>> entry : assignments.entrySet()) {
      final int start = entry.getKey().getStart();
      final int end = entry.getKey().getEnd();
      final RssProtos.PartitionRangeAssignment partitionRangeAssignment =
          RssProtos.PartitionRangeAssignment
              .newBuilder()
              .setStartPartition(start)
              .setEndPartition(end)
              .addAllServer(entry
                  .getValue()
                  .stream()
                  .map(ServerNode::convertToGrpcProto)
                  .collect(Collectors.toList()))
              .build();
      praList.add(partitionRangeAssignment);
    }

    return praList;
  }

  public SortedMap<PartitionRange, List<ServerNode>> getAssignments() {
    return assignments;
  }

  public boolean isEmpty() {
    return assignments == null || assignments.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PartitionRangeAssignment that = (PartitionRangeAssignment) o;
    return Objects.equal(assignments, that.assignments);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(assignments);
  }
}
