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
package org.apache.drill.exec.planner.fragment;

import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.planner.fragment.ParallelizationInfo.ParallelizationInfoCollector;

import java.util.List;

public class Stats {
  private final ParallelizationInfoCollector collector = new ParallelizationInfoCollector();
  private double maxCost;
  private DistributionAffinity distributionAffinity = DistributionAffinity.NONE;

  public void addParallelizationInfo(ParallelizationInfo parallelizationInfo) {
    collector.add(parallelizationInfo);
  }

  public void addCost(double cost){
    maxCost = Math.max(maxCost, cost);
  }

  public void addMaxWidth(int maxWidth) {
    collector.addMaxWidth(maxWidth);
  }

  public void addMinWidth(int minWidth) {
    collector.addMinWidth(minWidth);
  }

  public void setDistributionAffinity(final DistributionAffinity distributionAffinity) {
    if (this.distributionAffinity.isLessRestrictiveThan(distributionAffinity)) {
      this.distributionAffinity = distributionAffinity;
    }
  }

  public void addEndpointAffinities(List<EndpointAffinity> endpointAffinityList) {
    collector.addEndpointAffinities(endpointAffinityList);
  }

  public ParallelizationInfo getParallelizationInfo() {
    return collector.get();
  }

  @Override
  public String toString() {
    return "Stats [maxCost=" + maxCost +", parallelizationInfo=" + collector.toString() + "]";
  }

  public double getMaxCost() {
    return maxCost;
  }

  public DistributionAffinity getDistributionAffinity() {
    return distributionAffinity;
  }
}
