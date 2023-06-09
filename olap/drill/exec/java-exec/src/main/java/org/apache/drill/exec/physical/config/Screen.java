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

import java.util.Collections;
import java.util.List;

import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.physical.base.AbstractStore;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.Store;
import org.apache.drill.exec.planner.fragment.DistributionAffinity;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("screen")
public class Screen extends AbstractStore {

  public static final String OPERATOR_TYPE = "SCREEN";

  private final DrillbitEndpoint endpoint;

  public Screen(@JsonProperty("child") PhysicalOperator child, @JacksonInject DrillbitEndpoint endpoint) {
    super(child);
    assert (endpoint!=null);
    this.endpoint = endpoint;
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    return Collections.singletonList(new EndpointAffinity(endpoint, 1, true, /* maxWidth = */ 1));
  }

  @Override
  public int getMaxWidth() {
    return 1;
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> endpoints) throws PhysicalOperatorSetupException {
    // we actually don't have to do anything since nothing should have changed. we'll check just check that things
    // didn't get screwed up.
    if (endpoints.size() != 1) {
      throw new PhysicalOperatorSetupException("A Screen operator can only be assigned to a single node.");
    }
    DrillbitEndpoint endpoint = endpoints.iterator().next();
//    logger.debug("Endpoint this: {}, assignment: {}", this.endpoint, endpoint);
    if (!endpoint.equals(this.endpoint)) {
      throw new PhysicalOperatorSetupException(String.format(
          "A Screen operator can only be assigned to its home node.  Expected endpoint %s, Actual endpoint: %s",
          this.endpoint, endpoint));
    }
  }

  @Override
  public Store getSpecificStore(PhysicalOperator child, int minorFragmentId) {
    return new Screen(child, endpoint);
  }

  @JsonIgnore
  public DrillbitEndpoint getEndpoint() {
    return endpoint;
  }

  @Override
  public String toString() {
    return "Screen [endpoint=" + endpoint + ", getChild()=" + getChild() + "]";
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new Screen(child, endpoint);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitScreen(this, value);
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public DistributionAffinity getDistributionAffinity() {
    return DistributionAffinity.HARD;
  }
}
