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

import org.apache.drill.exec.physical.base.AbstractExchange;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalOperatorUtil;
import org.apache.drill.exec.physical.base.Receiver;
import org.apache.drill.exec.physical.base.Sender;
import org.apache.drill.exec.planner.physical.PartitionFunction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("range-partition-exchange")
public class RangePartitionExchange extends AbstractExchange {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RangePartitionExchange.class);

  private final PartitionFunction partitionFunction;

  @JsonCreator
  public RangePartitionExchange(@JsonProperty("child") PhysicalOperator child,
      @JsonProperty("partition-function") PartitionFunction partitionFunction) {
    super(child);
    this.partitionFunction = partitionFunction;
  }

  @Override
  public Sender getSender(int minorFragmentId, PhysicalOperator child) {
    return new RangePartitionSender(receiverMajorFragmentId, child,
        PhysicalOperatorUtil.getIndexOrderedEndpoints(receiverLocations),
        partitionFunction);
  }

  @Override
  public Receiver getReceiver(int minorFragmentId) {
    return new UnorderedReceiver(senderMajorFragmentId, PhysicalOperatorUtil.getIndexOrderedEndpoints(senderLocations), false);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new RangePartitionExchange(child, partitionFunction);
  }
}
