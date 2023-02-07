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

import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Receiver;
import org.apache.drill.common.logical.data.Order.Ordering;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * OrderedMuxExchange is a version of MuxExchange where the incoming batches are sorted
 * merge operation is performed to produced a sorted stream as output.
 */
@JsonTypeName("ordered-mux-exchange")
public class OrderedMuxExchange extends AbstractMuxExchange {

  private final List<Ordering> orderings;

  public OrderedMuxExchange(@JsonProperty("child") PhysicalOperator child, @JsonProperty("orderings")List<Ordering> orderings) {
    super(child);
    this.orderings = orderings;
  }

  @Override
  public Receiver getReceiver(int minorFragmentId) {
    return new MergingReceiverPOP(senderMajorFragmentId, getSenders(minorFragmentId), orderings, false);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new OrderedMuxExchange(child, orderings);
  }
}
