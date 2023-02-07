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
package org.apache.drill.exec.physical.config;

import java.util.List;

import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.base.AbstractSender;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.planner.physical.PartitionFunction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("range-partition-sender")
public class RangePartitionSender extends AbstractSender{

  // The number of records in the outgoing batch. This is overriding the default value in Partitioner
  public static final int RANGE_PARTITION_OUTGOING_BATCH_SIZE = (1 << 12) - 1;

  public static final String OPERATOR_TYPE = "RANGE_PARTITION_SENDER";

  @JsonProperty("partitionFunction")
  private final PartitionFunction partitionFunction;

  @JsonCreator
  public RangePartitionSender(@JsonProperty("receiver-major-fragment") int oppositeMajorFragmentId,
                              @JsonProperty("child") PhysicalOperator child,
                              @JsonProperty("destinations") List<MinorFragmentEndpoint> endpoints,
                              @JsonProperty("partitionFunction") PartitionFunction partitionFunction) {
    super(oppositeMajorFragmentId, child, endpoints);
    this.partitionFunction = partitionFunction;
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new RangePartitionSender(oppositeMajorFragmentId, child, destinations, partitionFunction);
  }

  @JsonProperty("partitionFunction")
  public PartitionFunction getPartitionFunction() {
    return partitionFunction;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitRangePartitionSender(this, value);
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

}
