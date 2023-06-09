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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;

public abstract class AbstractReceiver extends AbstractBase implements Receiver {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractReceiver.class);

  private final int oppositeMajorFragmentId;
  private final List<MinorFragmentEndpoint> senders;
  private final boolean spooling;

  /**
   * @param oppositeMajorFragmentId MajorFragmentId of fragments that are sending data to this receiver.
   * @param senders List of sender MinorFragmentEndpoints each containing sender MinorFragmentId and Drillbit endpoint
   *                where it is running.
   */
  public AbstractReceiver(int oppositeMajorFragmentId, List<MinorFragmentEndpoint> senders, boolean spooling) {
    this.oppositeMajorFragmentId = oppositeMajorFragmentId;
    this.senders = ImmutableList.copyOf(senders);
    this.spooling = spooling;
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitReceiver(this, value);
  }

  @Override
  public final PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    //rewriting is unnecessary since the inputs haven't changed.
    return this;
  }

  @JsonProperty("sender-major-fragment")
  public int getOppositeMajorFragmentId() {
    return oppositeMajorFragmentId;
  }

  @JsonProperty("senders")
  public List<MinorFragmentEndpoint> getProvidingEndpoints() {
    return senders;
  }

  @JsonIgnore
  public int getNumSenders() {
    return senders.size();
  }

  @Override
  public boolean isSpooling() {
    return spooling;
  }
}

