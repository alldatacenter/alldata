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

import org.apache.drill.shaded.guava.com.google.common.collect.HashMultiset;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.categories.PlannerTest;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static org.apache.drill.exec.ExecConstants.SLICE_TARGET_DEFAULT;
import static org.apache.drill.exec.planner.fragment.HardAffinityFragmentParallelizer.INSTANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(PlannerTest.class)
public class TestHardAffinityFragmentParallelizer extends BaseTest {

  // Create a set of test endpoints
  private static final DrillbitEndpoint N1_EP1 = newDrillbitEndpoint("node1", 30010);
  private static final DrillbitEndpoint N1_EP2 = newDrillbitEndpoint("node1", 30011);
  private static final DrillbitEndpoint N2_EP1 = newDrillbitEndpoint("node2", 30010);
  private static final DrillbitEndpoint N2_EP2 = newDrillbitEndpoint("node2", 30011);
  private static final DrillbitEndpoint N3_EP1 = newDrillbitEndpoint("node3", 30010);
  private static final DrillbitEndpoint N3_EP2 = newDrillbitEndpoint("node3", 30011);
  private static final DrillbitEndpoint N4_EP2 = newDrillbitEndpoint("node4", 30011);

  private static final DrillbitEndpoint newDrillbitEndpoint(String address, int port) {
    return DrillbitEndpoint.newBuilder().setAddress(address).setControlPort(port).build();
  }

  private static final ParallelizationParameters newParameters(final long threshold, final int maxWidthPerNode,
      final int maxGlobalWidth) {
    return new ParallelizationParameters() {
      @Override
      public long getSliceTarget() {
        return threshold;
      }

      @Override
      public int getMaxWidthPerNode() {
        return maxWidthPerNode;
      }

      @Override
      public int getMaxGlobalWidth() {
        return maxGlobalWidth;
      }

      /**
       * {@link HardAffinityFragmentParallelizer} doesn't use affinity factor.
       * @return
       */
      @Override
      public double getAffinityFactor() {
        return 0.0f;
      }
    };
  }

  private final Wrapper newWrapper(double cost, int minWidth, int maxWidth, List<EndpointAffinity> epAffs) {
    final Fragment fragment = mock(Fragment.class);
    final PhysicalOperator root = mock(PhysicalOperator.class);

    when(fragment.getRoot()).thenReturn(root);

    final Wrapper fragmentWrapper = new Wrapper(fragment, 1);
    final Stats stats = fragmentWrapper.getStats();
    stats.setDistributionAffinity(DistributionAffinity.HARD);
    stats.addCost(cost);
    stats.addMinWidth(minWidth);
    stats.addMaxWidth(maxWidth);
    stats.addEndpointAffinities(epAffs);

    return fragmentWrapper;
  }

  @Test
  public void simpleCase1() throws Exception {
    final Wrapper wrapper = newWrapper(200, 1, 20, Collections.singletonList(new EndpointAffinity(N1_EP1, 1.0, true, MAX_VALUE)));
    INSTANCE.parallelizeFragment(wrapper, newParameters(SLICE_TARGET_DEFAULT, 5, 20), null);

    // Expect the fragment parallelization to be just one because:
    // The cost (200) is below the threshold (SLICE_TARGET_DEFAULT) (which gives width of 200/10000 = ~1) and
    assertEquals(1, wrapper.getWidth());

    final List<DrillbitEndpoint> assignedEps = wrapper.getAssignedEndpoints();
    assertEquals(1, assignedEps.size());
    assertEquals(N1_EP1, assignedEps.get(0));
  }

  @Test
  public void simpleCase2() throws Exception {
    // Set the slice target to 1
    final Wrapper wrapper = newWrapper(200, 1, 20, Collections.singletonList(new EndpointAffinity(N1_EP1, 1.0, true, MAX_VALUE)));
    INSTANCE.parallelizeFragment(wrapper, newParameters(1, 5, 20), null);

    // Expect the fragment parallelization to be 5:
    // 1. the cost (200) is above the threshold (SLICE_TARGET_DEFAULT) (which gives 200/1=200 width) and
    // 2. Max width per node is 5 (limits the width 200 to 5)
    assertEquals(5, wrapper.getWidth());

    final List<DrillbitEndpoint> assignedEps = wrapper.getAssignedEndpoints();
    assertEquals(5, assignedEps.size());
    for (DrillbitEndpoint ep : assignedEps) {
      assertEquals(N1_EP1, ep);
    }
  }

  @Test
  public void multiNodeCluster1() throws Exception {
    final Wrapper wrapper = newWrapper(200, 1, 20,
        ImmutableList.of(
            new EndpointAffinity(N1_EP1, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N1_EP2, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N2_EP1, 0.10, true, MAX_VALUE),
            new EndpointAffinity(N3_EP2, 0.20, true, MAX_VALUE),
            new EndpointAffinity(N4_EP2, 0.20, true, MAX_VALUE)
        ));
    INSTANCE.parallelizeFragment(wrapper, newParameters(SLICE_TARGET_DEFAULT, 5, 20), null);

    // Expect the fragment parallelization to be 5 because:
    // 1. The cost (200) is below the threshold (SLICE_TARGET_DEFAULT) (which gives width of 200/10000 = ~1) and
    // 2. Number of mandoatory node assignments are 5 which overrides the cost based width of 1.
    assertEquals(5, wrapper.getWidth());

    // As there are 5 required eps and the width is 5, everyone gets assigned 1.
    final List<DrillbitEndpoint> assignedEps = wrapper.getAssignedEndpoints();
    assertEquals(5, assignedEps.size());
    assertTrue(assignedEps.contains(N1_EP1));
    assertTrue(assignedEps.contains(N1_EP2));
    assertTrue(assignedEps.contains(N2_EP1));
    assertTrue(assignedEps.contains(N3_EP2));
    assertTrue(assignedEps.contains(N4_EP2));
  }

  @Test
  public void multiNodeCluster2() throws Exception {
    final Wrapper wrapper = newWrapper(200, 1, 20,
        ImmutableList.of(
            new EndpointAffinity(N1_EP2, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N2_EP2, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N3_EP1, 0.10, true, MAX_VALUE),
            new EndpointAffinity(N4_EP2, 0.20, true, MAX_VALUE),
            new EndpointAffinity(N1_EP1, 0.20, true, MAX_VALUE)
        ));
    INSTANCE.parallelizeFragment(wrapper, newParameters(1, 5, 20), null);

    // Expect the fragment parallelization to be 20 because:
    // 1. the cost (200) is above the threshold (SLICE_TARGET_DEFAULT) (which gives 200/1=200 width) and
    // 2. Number of mandatory node assignments are 5 (current width 200 satisfies the requirement)
    // 3. max fragment width is 20 which limits the width
    assertEquals(20, wrapper.getWidth());

    final List<DrillbitEndpoint> assignedEps = wrapper.getAssignedEndpoints();
    assertEquals(20, assignedEps.size());
    final HashMultiset<DrillbitEndpoint> counts = HashMultiset.create();
    for(final DrillbitEndpoint ep : assignedEps) {
      counts.add(ep);
    }
    // Each node gets at max 5.
    assertTrue(counts.count(N1_EP2) <= 5);
    assertTrue(counts.count(N2_EP2) <= 5);
    assertTrue(counts.count(N3_EP1) <= 5);
    assertTrue(counts.count(N4_EP2) <= 5);
    assertTrue(counts.count(N1_EP1) <= 5);
  }

  @Test
  public void multiNodeClusterNegative1() throws Exception {
    final Wrapper wrapper = newWrapper(200, 1, 20,
        ImmutableList.of(
            new EndpointAffinity(N1_EP2, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N2_EP2, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N3_EP1, 0.10, true, MAX_VALUE),
            new EndpointAffinity(N4_EP2, 0.20, true, MAX_VALUE),
            new EndpointAffinity(N1_EP1, 0.20, true, MAX_VALUE)
        ));

    try {
      INSTANCE.parallelizeFragment(wrapper, newParameters(1, 2, 2), null);
      fail("Expected an exception, because max global query width (2) is less than the number of mandatory nodes (5)");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  public void multiNodeClusterNegative2() throws Exception {
    final Wrapper wrapper = newWrapper(200, 1, 3,
        ImmutableList.of(
            new EndpointAffinity(N1_EP2, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N2_EP2, 0.15, true, MAX_VALUE),
            new EndpointAffinity(N3_EP1, 0.10, true, MAX_VALUE),
            new EndpointAffinity(N4_EP2, 0.20, true, MAX_VALUE),
            new EndpointAffinity(N1_EP1, 0.20, true, MAX_VALUE)
        ));

    try {
      INSTANCE.parallelizeFragment(wrapper, newParameters(1, 2, 2), null);
      fail("Expected an exception, because max fragment width (3) is less than the number of mandatory nodes (5)");
    } catch (Exception e) {
      // ok
    }
  }
}
