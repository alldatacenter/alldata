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

import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Ordering;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of {@link FragmentParallelizer} where fragment has zero or
 * more endpoints with affinities. Width per node is depended on the affinity to
 * the endpoint and total width (calculated using costs). Based on various
 * factors endpoints which have no affinity can be assigned to run the
 * fragments.
 */
public class SoftAffinityFragmentParallelizer implements FragmentParallelizer {
  public static final SoftAffinityFragmentParallelizer INSTANCE = new SoftAffinityFragmentParallelizer();

  private static final Ordering<EndpointAffinity> ENDPOINT_AFFINITY_ORDERING =
      Ordering.from(new Comparator<EndpointAffinity>() {
        @Override
        public int compare(EndpointAffinity o1, EndpointAffinity o2) {
          // Sort in descending order of affinity values
          return Double.compare(o2.getAffinity(), o1.getAffinity());
        }
      });

  @Override
  public void parallelizeFragment(final Wrapper fragmentWrapper, final ParallelizationParameters parameters,
      final Collection<DrillbitEndpoint> activeEndpoints) throws PhysicalOperatorSetupException {

    // Find the parallelization width of fragment
    final Stats stats = fragmentWrapper.getStats();
    final ParallelizationInfo parallelizationInfo = stats.getParallelizationInfo();

    // 1. Find the parallelization based on cost. Use max cost of all operators in this fragment; this is consistent
    //    with the calculation that ExcessiveExchangeRemover uses.
    int width = (int) Math.ceil(stats.getMaxCost() / parameters.getSliceTarget());

    // 2. Cap the parallelization width by fragment level width limit and system level per query width limit
    width = Math.min(width, Math.min(parallelizationInfo.getMaxWidth(), parameters.getMaxGlobalWidth()));

    // 3. Cap the parallelization width by system level per node width limit
    width = Math.min(width, parameters.getMaxWidthPerNode() * activeEndpoints.size());

    // 4. Make sure width is at least the min width enforced by operators
    width = Math.max(parallelizationInfo.getMinWidth(), width);

    // 4. Make sure width is at most the max width enforced by operators
    width = Math.min(parallelizationInfo.getMaxWidth(), width);

    // 5 Finally make sure the width is at least one
    width = Math.max(1, width);

    fragmentWrapper.setWidth(width);

    final List<DrillbitEndpoint> assignedEndpoints = findEndpoints(activeEndpoints,
        parallelizationInfo.getEndpointAffinityMap(), fragmentWrapper.getWidth(), parameters);

    fragmentWrapper.assignEndpoints(assignedEndpoints);
  }

  // Assign endpoints based on the given endpoint list, affinity map and width.
  private List<DrillbitEndpoint> findEndpoints(final Collection<DrillbitEndpoint> activeEndpoints,
      final Map<DrillbitEndpoint, EndpointAffinity> endpointAffinityMap, final int width,
      final ParallelizationParameters parameters)
    throws PhysicalOperatorSetupException {

    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();

    if (endpointAffinityMap.size() > 0) {
      // Get EndpointAffinity list sorted in descending order of affinity values
      List<EndpointAffinity> sortedAffinityList = ENDPOINT_AFFINITY_ORDERING.immutableSortedCopy(endpointAffinityMap.values());

      // Find the number of mandatory nodes (nodes with +infinity affinity).
      int numRequiredNodes = 0;
      for(EndpointAffinity ep : sortedAffinityList) {
        if (ep.isAssignmentRequired()) {
          numRequiredNodes++;
        } else {
          // As the list is sorted in descending order of affinities, we don't need to go beyond the first occurrance
          // of non-mandatory node
          break;
        }
      }

      if (width < numRequiredNodes) {
        throw new PhysicalOperatorSetupException("Can not parallelize the fragment as the parallelization width (" + width + ") is " +
            "less than the number of mandatory nodes (" + numRequiredNodes + " nodes with +INFINITE affinity).");
      }

      // Find the maximum number of slots which should go to endpoints with affinity (See DRILL-825 for details)
      int affinedSlots =
          Math.max(1, (int) (Math.ceil(parameters.getAffinityFactor() * width / activeEndpoints.size()) * sortedAffinityList.size()));

      // Make sure affined slots is at least the number of mandatory nodes
      affinedSlots = Math.max(affinedSlots, numRequiredNodes);

      // Cap the affined slots to max parallelization width
      affinedSlots = Math.min(affinedSlots, width);

      Iterator<EndpointAffinity> affinedEPItr = Iterators.cycle(sortedAffinityList);

      // Keep adding until we have selected "affinedSlots" number of endpoints.
      while(endpoints.size() < affinedSlots) {
        EndpointAffinity ea = affinedEPItr.next();
        endpoints.add(ea.getEndpoint());
      }
    }

    // add remaining endpoints if required
    if (endpoints.size() < width) {
      // Get a list of endpoints that are not part of the affinity endpoint list
      List<DrillbitEndpoint> endpointsWithNoAffinity;
      final Set<DrillbitEndpoint> endpointsWithAffinity = endpointAffinityMap.keySet();

      if (endpointAffinityMap.size() > 0) {
        endpointsWithNoAffinity = Lists.newArrayList();
        for (DrillbitEndpoint ep : activeEndpoints) {
          if (!endpointsWithAffinity.contains(ep)) {
            endpointsWithNoAffinity.add(ep);
          }
        }
      } else {
        endpointsWithNoAffinity = Lists.newArrayList(activeEndpoints); // Need to create a copy instead of an
        // immutable copy, because we need to shuffle the list (next statement) and Collections.shuffle() doesn't
        // support immutable copy as input.
      }

      // round robin with random start.
      Collections.shuffle(endpointsWithNoAffinity, ThreadLocalRandom.current());
      Iterator<DrillbitEndpoint> otherEPItr =
          Iterators.cycle(endpointsWithNoAffinity.size() > 0 ? endpointsWithNoAffinity : endpointsWithAffinity);
      while (endpoints.size() < width) {
        endpoints.add(otherEPItr.next());
      }
    }

    return endpoints;
  }
}
