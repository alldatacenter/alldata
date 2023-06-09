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
package org.apache.drill.exec.work;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.fragment.Wrapper;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.work.foreman.ForemanSetupException;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class QueryWorkUnit {

  /**
   * Definition of a minor fragment that contains the (unserialized) fragment operator
   * tree and the (partially built) fragment. Allows the resource manager to apply
   * memory allocations before serializing the fragments to JSON.
   */

  public static class MinorFragmentDefn {
    private PlanFragment fragment;
    private final FragmentRoot root;
    private final OptionList options;

    public MinorFragmentDefn(final PlanFragment fragment, final FragmentRoot root, OptionList options) {
      this.fragment = fragment;
      this.root = root;
      this.options = options;
    }

    public FragmentRoot root() { return root; }
    public PlanFragment fragment() { return fragment; }
    public OptionList options() { return options; }

    public PlanFragment applyPlan(PhysicalPlanReader reader) throws ForemanSetupException {
      // get plan as JSON
      try {
        final String plan = reader.writeJson(root);
        final String optionsData = reader.writeJson(options);
        return PlanFragment.newBuilder(fragment)
            .setFragmentJson(plan)
            .setOptionsJson(optionsData)
            .build();
      } catch (JsonProcessingException e) {
        throw new ForemanSetupException("Failure while trying to convert fragment into json.", e);
      }
    }
  }

  private PlanFragment rootFragment; // for local
  private final MinorFragmentDefn rootFragmentDefn;
  private final FragmentRoot rootOperator; // for local
  private List<PlanFragment> fragments = new ArrayList<>();
  private final List<MinorFragmentDefn> minorFragmentDefns;
  private final Wrapper rootWrapper;

  public QueryWorkUnit(final FragmentRoot rootOperator, final MinorFragmentDefn rootFragmentDefn,
      final List<MinorFragmentDefn> minorFragmentDefns, final Wrapper rootWrapper) {
    Preconditions.checkNotNull(rootOperator);
    Preconditions.checkNotNull(rootFragmentDefn);
    Preconditions.checkNotNull(minorFragmentDefns);

    this.rootFragmentDefn = rootFragmentDefn;
    this.rootOperator = rootOperator;
    this.minorFragmentDefns = minorFragmentDefns;
    this.rootWrapper = rootWrapper;
  }

  public PlanFragment getRootFragment() {
    return rootFragment;
  }

  public MinorFragmentDefn getRootFragmentDefn() {
    return rootFragmentDefn;
  }

  public List<PlanFragment> getFragments() {
    return fragments;
  }

  public List<MinorFragmentDefn> getMinorFragmentDefns() {
    return minorFragmentDefns;
  }

  public FragmentRoot getRootOperator() {
    return rootOperator;
  }

  public void applyPlan(PhysicalPlanReader reader) throws ForemanSetupException {
    assert rootFragment == null;
    rootFragment = rootFragmentDefn.applyPlan(reader);
    assert fragments.isEmpty();
    for (MinorFragmentDefn defn : minorFragmentDefns) {
      fragments.add(defn.applyPlan(reader));
    }
  }

  /**
   * Converts list of stored fragments into their string representation,
   * in case of exception returns text indicating that string was malformed.
   * Is used for debugging purposes.
   *
   * @return fragments information
   */
  public String stringifyFragments() {
    StringBuilder stringBuilder = new StringBuilder();
    final int fragmentCount = fragments.size();
    int fragmentIndex = 0;
    for (final PlanFragment planFragment : fragments) {
      final ExecProtos.FragmentHandle fragmentHandle = planFragment.getHandle();
      stringBuilder.append("PlanFragment(");
      stringBuilder.append(++fragmentIndex);
      stringBuilder.append('/');
      stringBuilder.append(fragmentCount);
      stringBuilder.append(") major_fragment_id ");
      stringBuilder.append(fragmentHandle.getMajorFragmentId());
      stringBuilder.append(" minor_fragment_id ");
      stringBuilder.append(fragmentHandle.getMinorFragmentId());
      stringBuilder.append('\n');

      final CoordinationProtos.DrillbitEndpoint endpointAssignment = planFragment.getAssignment();
      stringBuilder.append("  DrillbitEndpoint address ");
      stringBuilder.append(endpointAssignment.getAddress());
      stringBuilder.append('\n');

      String jsonString = "<<malformed JSON>>";
      stringBuilder.append("  fragment_json: ");
      final ObjectMapper objectMapper = new ObjectMapper();
      try {
        final Object json = objectMapper.readValue(planFragment.getFragmentJson(), Object.class);
        jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
      } catch (final Exception e) {
        // we've already set jsonString to a fallback value
      }
      stringBuilder.append(jsonString);
    }
    return stringBuilder.toString();
  }


  public Wrapper getRootWrapper() {
    return rootWrapper;
  }

}
