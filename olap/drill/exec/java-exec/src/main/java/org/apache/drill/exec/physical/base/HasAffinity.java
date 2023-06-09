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

import java.util.List;

import org.apache.drill.exec.physical.EndpointAffinity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.drill.exec.planner.fragment.DistributionAffinity;

/**
 * Describes a physical operator that has affinity to particular nodes. Used for assignment decisions.
 */
public interface HasAffinity extends PhysicalOperator {

  /**
   * Get the list of Endpoints with associated affinities that this operator has preference for.
   * @return List of EndpointAffinity objects.
   */
  @JsonIgnore
  List<EndpointAffinity> getOperatorAffinity();

  /**
   * Get distribution affinity which describes the parallelization strategy of the operator.
   */
  @JsonIgnore
  DistributionAffinity getDistributionAffinity();
}
