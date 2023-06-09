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

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import java.util.List;
import java.util.Map;

/**
 * Captures parallelization parameters for a given operator/fragments. It consists of min and max width of
 * parallelization and affinity to drillbit endpoints.
 */
public class ParallelizationInfo {

  /* Default parallelization width is [1, Integer.MAX_VALUE] and no endpoint affinity. */
  public static final ParallelizationInfo UNLIMITED_WIDTH_NO_ENDPOINT_AFFINITY =
      ParallelizationInfo.create(1, Integer.MAX_VALUE);

  private final Map<DrillbitEndpoint, EndpointAffinity> affinityMap;
  private final int minWidth;
  private final int maxWidth;

  private ParallelizationInfo(int minWidth, int maxWidth, Map<DrillbitEndpoint, EndpointAffinity> affinityMap) {
    this.minWidth = minWidth;
    this.maxWidth = maxWidth;
    this.affinityMap = ImmutableMap.copyOf(affinityMap);
  }

  public static ParallelizationInfo create(int minWidth, int maxWidth) {
    return create(minWidth, maxWidth, ImmutableList.<EndpointAffinity>of());
  }

  public static ParallelizationInfo create(int minWidth, int maxWidth, List<EndpointAffinity> endpointAffinities) {
    Map<DrillbitEndpoint, EndpointAffinity> affinityMap = Maps.newHashMap();

    for(EndpointAffinity epAffinity : endpointAffinities) {
      affinityMap.put(epAffinity.getEndpoint(), epAffinity);
    }

    return new ParallelizationInfo(minWidth, maxWidth, affinityMap);
  }

  public int getMinWidth() {
    return minWidth;
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public Map<DrillbitEndpoint, EndpointAffinity> getEndpointAffinityMap() {
    return affinityMap;
  }

  @Override
  public String toString() {
    return getDigest(minWidth, maxWidth, affinityMap);
  }

  private static String getDigest(int minWidth, int maxWidth, Map<DrillbitEndpoint, EndpointAffinity> affinityMap) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("[minWidth = %d, maxWidth=%d, epAff=[", minWidth, maxWidth));
    sb.append(Joiner.on(",").join(affinityMap.values()));
    sb.append("]]");

    return sb.toString();
  }

  /**
   * Collects/merges one or more ParallelizationInfo instances.
   */
  public static class ParallelizationInfoCollector {
    private int minWidth = 1;
    private int maxWidth = Integer.MAX_VALUE;
    private final Map<DrillbitEndpoint, EndpointAffinity> affinityMap = Maps.newHashMap();

    public void add(ParallelizationInfo parallelizationInfo) {
      minWidth = Math.max(minWidth, parallelizationInfo.minWidth);
      maxWidth = Math.min(maxWidth, parallelizationInfo.maxWidth);

      Map<DrillbitEndpoint, EndpointAffinity> affinityMap = parallelizationInfo.getEndpointAffinityMap();
      for(Map.Entry<DrillbitEndpoint, EndpointAffinity> epAff : affinityMap.entrySet()) {
        addEndpointAffinity(epAff.getValue());
      }
    }

    public void addMaxWidth(int newMaxWidth) {
      maxWidth = Math.min(maxWidth, newMaxWidth);
    }

    public void addMinWidth(int newMinWidth) {
      minWidth = Math.max(minWidth, newMinWidth);
    }

    public void addEndpointAffinities(List<EndpointAffinity> endpointAffinities) {
      for(EndpointAffinity epAff : endpointAffinities) {
        addEndpointAffinity(epAff);
      }
    }

    // Helper method to add the given EndpointAffinity to the global affinity map
    private void addEndpointAffinity(EndpointAffinity epAff) {
      final EndpointAffinity epAffAgg = affinityMap.get(epAff.getEndpoint());
      if (epAffAgg != null) {
        epAffAgg.addAffinity(epAff.getAffinity());
        if (epAff.isAssignmentRequired()) {
          epAffAgg.setAssignmentRequired();
        }
        epAffAgg.setMaxWidth(epAff.getMaxWidth());
      } else {
        affinityMap.put(epAff.getEndpoint(), epAff);
      }
    }

    /**
     * Get a ParallelizationInfo instance based on the current state of collected info.
     */
    public ParallelizationInfo get() {
      return new ParallelizationInfo(minWidth, maxWidth, affinityMap);
    }

    @Override
    public String toString() {
      return getDigest(minWidth, maxWidth, affinityMap);
    }
  }
}
