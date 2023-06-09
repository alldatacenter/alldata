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

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.physical.base.AbstractExchange;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalOperatorUtil;
import org.apache.drill.exec.physical.base.Receiver;
import org.apache.drill.exec.physical.base.Sender;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

@JsonTypeName("ordered-partition-exchange")
public class OrderedPartitionExchange extends AbstractExchange {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OrderedPartitionExchange.class);

  private final List<Ordering> orderings;
  private final FieldReference ref;
  private int recordsToSample = 10000; // How many records must be received before analyzing
  private int samplingFactor = 10; // Will collect SAMPLING_FACTOR * number of partitions to send to distributed cache
  private float completionFactor = .75f; // What fraction of fragments must be completed before attempting to build partition table

  @JsonCreator
  public OrderedPartitionExchange(@JsonProperty("orderings") List<Ordering> orderings, @JsonProperty("ref") FieldReference ref,
                                  @JsonProperty("child") PhysicalOperator child, @JsonProperty("recordsToSample") Integer recordsToSample,
                                  @JsonProperty("samplingFactor") Integer samplingFactor, @JsonProperty("completionFactor") Float completionFactor) {
    super(child);
    this.orderings = orderings;
    this.ref = ref;
    if (recordsToSample != null) {
      Preconditions.checkArgument(recordsToSample > 0, "recordsToSample must be greater than 0");
      this.recordsToSample = recordsToSample;
    }
    if (samplingFactor != null) {
      Preconditions.checkArgument(samplingFactor > 0, "samplingFactor must be greater than 0");
      this.samplingFactor = samplingFactor;
    }
    if (completionFactor != null) {
      Preconditions.checkArgument(completionFactor > 0, "completionFactor must be greater than 0");
      Preconditions.checkArgument(completionFactor <=  1.0, "completionFactor cannot be greater than 1.0");
      this.completionFactor = completionFactor;
    }
  }

  @Override
  public Sender getSender(int minorFragmentId, PhysicalOperator child) {
    return new OrderedPartitionSender(orderings, ref, child,
        PhysicalOperatorUtil.getIndexOrderedEndpoints(receiverLocations),
        receiverMajorFragmentId, senderLocations.size(), recordsToSample, samplingFactor, completionFactor);
  }

  @Override
  public Receiver getReceiver(int minorFragmentId) {
    return new UnorderedReceiver(senderMajorFragmentId, PhysicalOperatorUtil.getIndexOrderedEndpoints(senderLocations), false);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new OrderedPartitionExchange(orderings, ref, child, recordsToSample, samplingFactor, completionFactor);
  }
}
