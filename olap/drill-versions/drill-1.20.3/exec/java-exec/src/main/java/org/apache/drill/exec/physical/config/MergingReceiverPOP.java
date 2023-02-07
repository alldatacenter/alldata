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

import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.base.AbstractReceiver;
import org.apache.drill.exec.physical.base.PhysicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

// The goal of this operator is to produce outgoing batches with records
// ordered according to the supplied expression.  Each incoming batch
// is guaranteed to be in order, so the operator simply merges the incoming
// batches.  This is accomplished by building and depleting a priority queue.
@JsonTypeName("merging-receiver")
public class MergingReceiverPOP extends AbstractReceiver {

  public static final String OPERATOR_TYPE = "MERGING_RECEIVER";

  private final List<Ordering> orderings;

  @JsonCreator
  public MergingReceiverPOP(@JsonProperty("sender-major-fragment") int oppositeMajorFragmentId,
                            @JsonProperty("senders") List<MinorFragmentEndpoint> senders,
                            @JsonProperty("orderings") List<Ordering> orderings,
                            @JsonProperty("spooling") boolean spooling) {
    super(oppositeMajorFragmentId, senders, spooling);
    this.orderings = orderings;
  }

  @Override
  public boolean supportsOutOfOrderExchange() {
    return false;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitMergingReceiver(this, value);
  }

  public List<Ordering> getOrderings() {
    return orderings;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
