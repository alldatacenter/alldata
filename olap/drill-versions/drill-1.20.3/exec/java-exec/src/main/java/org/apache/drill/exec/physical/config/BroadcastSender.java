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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("broadcast-sender")
public class BroadcastSender extends AbstractSender {

  public static final String OPERATOR_TYPE = "BROADCAST_SENDER";

  @JsonCreator
  public BroadcastSender(@JsonProperty("receiver-major-fragment") int oppositeMajorFragmentId,
                         @JsonProperty("child") PhysicalOperator child,
                         @JsonProperty("destinations") List<MinorFragmentEndpoint> destinations) {
    super(oppositeMajorFragmentId, child, destinations);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new BroadcastSender(oppositeMajorFragmentId, child, destinations);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitBroadcastSender(this, value);
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }


}
