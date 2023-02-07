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
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.base.AbstractSender;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@JsonTypeName("OrderedPartitionSender")
public class OrderedPartitionSender extends AbstractSender {

  public static final String OPERATOR_TYPE = "ORDERED_PARTITION_SENDER";

  private final List<Ordering> orderings;
  private final FieldReference ref;
  private final int sendingWidth;

  private final int recordsToSample;
  private final int samplingFactor;
  private final float completionFactor;

  @JsonCreator
  public OrderedPartitionSender(@JsonProperty("orderings") List<Ordering> orderings,
                                @JsonProperty("ref") FieldReference ref,
                                @JsonProperty("child") PhysicalOperator child,
                                @JsonProperty("destinations") List<MinorFragmentEndpoint> endpoints,
                                @JsonProperty("receiver-major-fragment") int oppositeMajorFragmentId,
                                @JsonProperty("sending-fragment-width") int sendingWidth,
                                @JsonProperty("recordsToSample") int recordsToSample,
                                @JsonProperty("samplingFactor") int samplingFactor,
                                @JsonProperty("completionFactor") float completionFactor) {
    super(oppositeMajorFragmentId, child, endpoints);
    if (orderings == null) {
      this.orderings = Lists.newArrayList();
    } else {
      this.orderings = orderings;
    }
    this.ref = ref;
    this.sendingWidth = sendingWidth;
    this.recordsToSample = recordsToSample;
    this.samplingFactor = samplingFactor;
    this.completionFactor = completionFactor;
  }

  public int getSendingWidth() {
    return sendingWidth;
  }

  public List<Ordering> getOrderings() {
    return orderings;
  }

  public FieldReference getRef() {
    return ref;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E{
    return physicalVisitor.visitOrderedPartitionSender(this, value);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new OrderedPartitionSender(orderings, ref, child, destinations, oppositeMajorFragmentId,
        sendingWidth, recordsToSample, samplingFactor, completionFactor);
  }

  public int getRecordsToSample() {
    return recordsToSample;
  }

  public int getSamplingFactor() {
    return samplingFactor;
  }

  public float getCompletionFactor() {
    return completionFactor;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public String toString() {
    return "OrderedPartitionSender[orderings=" + orderings
        + ", ref=" + ref
        + ", sendingWidth=" + sendingWidth
        + ", recordsToSample=" + recordsToSample
        + ", samplingFactor=" + samplingFactor
        + ", completionFactor=" + completionFactor
        + "]";
  }
}
