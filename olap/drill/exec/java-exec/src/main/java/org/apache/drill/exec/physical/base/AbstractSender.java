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
package org.apache.drill.exec.physical.base;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;

import java.util.List;

public abstract class AbstractSender extends AbstractSingle implements Sender {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractSender.class);

  protected final int oppositeMajorFragmentId;
  //
  protected final List<MinorFragmentEndpoint> destinations;

  /**
   * @param oppositeMajorFragmentId MajorFragmentId of fragments that are receiving data sent by this sender.
   * @param child Child PhysicalOperator which is providing data to this Sender.
   * @param destinations List of receiver MinorFragmentEndpoints each containing MinorFragmentId and Drillbit endpoint
   *                     where it is running.
   */
  public AbstractSender(int oppositeMajorFragmentId, PhysicalOperator child, List<MinorFragmentEndpoint> destinations) {
    super(child);
    this.oppositeMajorFragmentId = oppositeMajorFragmentId;
    this.destinations = ImmutableList.copyOf(destinations);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSender(this, value);
  }

  @Override
  @JsonProperty("receiver-major-fragment")
  public int getOppositeMajorFragmentId() {
    return oppositeMajorFragmentId;
  }

  @Override
  @JsonProperty("destinations")
  public List<MinorFragmentEndpoint> getDestinations() {
    return destinations;
  }
}
