/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.kafka;

import org.apache.drill.common.FunctionNames;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.PlanStringBuilder;

import java.util.Objects;

public class KafkaPartitionScanSpec {
  private final String topicName;
  private final int partitionId;
  private long startOffset;
  private long endOffset;

  @JsonCreator
  public KafkaPartitionScanSpec(@JsonProperty("topicName") String topicName,
                                @JsonProperty("partitionId") int partitionId,
                                @JsonProperty("startOffset") long startOffset,
                                @JsonProperty("endOffset") long endOffset) {
    this.topicName = topicName;
    this.partitionId = partitionId;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  public String getTopicName() {
    return topicName;
  }

  public int getPartitionId() {
    return partitionId;
  }

  public long getStartOffset() {
    return startOffset;
  }

  public long getEndOffset() {
    return endOffset;
  }

  public void mergeScanSpec(String functionName, KafkaPartitionScanSpec scanSpec) {
    switch (functionName) {
      case FunctionNames.AND:
        //Reduce the scan range
        if (startOffset < scanSpec.startOffset) {
          startOffset = scanSpec.startOffset;
        }

        if (endOffset > scanSpec.endOffset) {
          endOffset = scanSpec.endOffset;
        }
        break;
      case FunctionNames.OR:
        //Increase the scan range
        if (scanSpec.startOffset < startOffset) {
          startOffset = scanSpec.startOffset;
        }

        if (scanSpec.endOffset > endOffset) {
          endOffset = scanSpec.endOffset;
        }
        break;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KafkaPartitionScanSpec) {
      KafkaPartitionScanSpec that = ((KafkaPartitionScanSpec)obj);
      return this.topicName.equals(that.topicName) && this.partitionId == that.partitionId
                 && this.startOffset == that.startOffset && this.endOffset == that.endOffset;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(topicName, partitionId, startOffset, endOffset);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("topicName", topicName)
      .field("partitionId", partitionId)
      .field("startOffset", startOffset)
      .field("endOffset", endOffset)
      .toString();
  }

  @Override
  public KafkaPartitionScanSpec clone() {
    return new KafkaPartitionScanSpec(topicName, partitionId, startOffset, endOffset);
  }
}
