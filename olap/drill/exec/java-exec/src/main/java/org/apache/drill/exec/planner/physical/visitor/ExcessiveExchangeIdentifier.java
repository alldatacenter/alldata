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
package org.apache.drill.exec.planner.physical.visitor;

import java.util.Collections;
import java.util.List;
import org.apache.drill.exec.planner.fragment.DistributionAffinity;
import org.apache.drill.exec.planner.physical.LateralJoinPrel;
import org.apache.drill.exec.planner.physical.ExchangePrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.physical.ScreenPrel;
import org.apache.drill.exec.planner.physical.LimitPrel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.SingleMergeExchangePrel;
import org.apache.drill.exec.planner.physical.HashToMergeExchangePrel;
import org.apache.calcite.rel.RelNode;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.planner.physical.UnnestPrel;

public class ExcessiveExchangeIdentifier extends BasePrelVisitor<Prel, ExcessiveExchangeIdentifier.MajorFragmentStat, RuntimeException> {
  private final long targetSliceSize;
  private LateralJoinPrel topMostLateralJoin;

  public ExcessiveExchangeIdentifier(long targetSliceSize) {
    this.targetSliceSize = targetSliceSize;
  }

  public static Prel removeExcessiveExchanges(Prel prel, long targetSliceSize) {
    ExcessiveExchangeIdentifier exchange = new ExcessiveExchangeIdentifier(targetSliceSize);
    return prel.accept(exchange, exchange.getNewStat());
  }

  @Override
  public Prel visitExchange(ExchangePrel prel, MajorFragmentStat parent) throws RuntimeException {
    parent.add(prel);
    MajorFragmentStat newFrag = new MajorFragmentStat();
    newFrag.setRightSideOfLateral(parent.isRightSideOfLateral());

    if (prel instanceof SingleMergeExchangePrel) {
      newFrag.isSimpleRel = true;
    }

    Prel newChild = ((Prel) prel.getInput()).accept(this, newFrag);

    if (parent.isSimpleRel &&
        prel instanceof HashToMergeExchangePrel) {
      return newChild;
    }

    if (canRemoveExchange(parent, newFrag)) {
      return newChild;
    } else if (newChild != prel.getInput()) {
      return (Prel) prel.copy(prel.getTraitSet(), Collections.singletonList(newChild));
    } else {
      return prel;
    }
  }

  private boolean canRemoveExchange(MajorFragmentStat parentFrag, MajorFragmentStat childFrag) {
    if (childFrag.isSingular() && parentFrag.isSingular() &&
       (!childFrag.isDistributionStrict() || !parentFrag.isDistributionStrict())) {
      return true;
    }

    return parentFrag.isRightSideOfLateral();
  }

  @Override
  public Prel visitScreen(ScreenPrel prel, MajorFragmentStat s) throws RuntimeException {
    s.addScreen(prel);
    RelNode child = ((Prel) prel.getInput()).accept(this, s);
    if (child == prel.getInput()) {
      return prel;
    }
    return prel.copy(prel.getTraitSet(), Collections.singletonList(child));
  }

  @Override
  public Prel visitScan(ScanPrel prel, MajorFragmentStat s) throws RuntimeException {
    s.addScan(prel);
    return prel;
  }

  @Override
  public Prel visitLateral(LateralJoinPrel prel, MajorFragmentStat s) throws RuntimeException {
    List<RelNode> children = Lists.newArrayList();
    s.add(prel);

    for (Prel p : prel) {
      s.add(p);
    }

    // Traverse the left side of the Lateral join first. Left side of the
    // Lateral shouldn't have any restrictions on Exchanges.
    children.add(((Prel)prel.getInput(0)).accept(this, s));
    // Save the outermost Lateral join so as to unset the flag later.
    if (topMostLateralJoin == null) {
      topMostLateralJoin = prel;
    }

    // Right side of the Lateral shouldn't have any Exchanges. Hence set the
    // flag so that visitExchange removes the exchanges.
    s.setRightSideOfLateral(true);
    children.add(((Prel)prel.getInput(1)).accept(this, s));
    if (topMostLateralJoin == prel) {
      topMostLateralJoin = null;
      s.setRightSideOfLateral(false);
    }
    if (children.equals(prel.getInputs())) {
      return prel;
    }
    return (Prel) prel.copy(prel.getTraitSet(), children);
  }

  @Override
  public Prel visitUnnest(UnnestPrel prel, MajorFragmentStat s) throws RuntimeException {
    s.addUnnest(prel);
    return prel;
  }

  @Override
  public Prel visitPrel(Prel prel, MajorFragmentStat s) throws RuntimeException {
    List<RelNode> children = Lists.newArrayList();
    s.add(prel);

    // Add all children to MajorFragmentStat, before we visit each child.
    // Since MajorFramentStat keeps track of maxRows of Prels in MajorFrag, it's fine to add prel multiple times.
    // Doing this will ensure MajorFragmentStat is same when visit each individual child, in order to make
    // consistent decision.
    for (Prel p : prel) {
      s.add(p);
    }

    s.setHashDistribution(prel);

    for (Prel p : prel) {
      children.add(p.accept(this, s));
    }
    if (children.equals(prel.getInputs())) {
      return prel;
    }
    return (Prel) prel.copy(prel.getTraitSet(), children);
  }

  public MajorFragmentStat getNewStat() {
    return new MajorFragmentStat();
  }

  class MajorFragmentStat {
    private DistributionAffinity distributionAffinity = DistributionAffinity.NONE;
    private double maxRows;
    private int maxWidth = Integer.MAX_VALUE;
    private boolean isMultiSubScan;
    private boolean rightSideOfLateral;
    //This flag if true signifies that all the Rels thus far
    //are simple rels with no distribution requirement.
    private boolean isSimpleRel;

    public void add(Prel prel) {
      maxRows = Math.max(prel.estimateRowCount(prel.getCluster().getMetadataQuery()), maxRows);
    }

    public void addScreen(ScreenPrel screenPrel) {
      maxWidth = 1;
      distributionAffinity = screenPrel.getDistributionAffinity();
    }

    public void addScan(ScanPrel prel) {
      maxWidth = Math.min(maxWidth, prel.getGroupScan().getMaxParallelizationWidth());
      isMultiSubScan = prel.getGroupScan().getMinParallelizationWidth() > 1;
      distributionAffinity = prel.getDistributionAffinity();
      add(prel);
    }

    public void setHashDistribution(Prel prel) {
      isSimpleRel = isSimpleRel &&
                    (prel instanceof LimitPrel ||
                     prel instanceof ProjectPrel ||
                     prel instanceof FilterPrel);
    }

    public boolean isSingular() {
      // do not remove exchanges when a scan has more than one subscans (e.g. SystemTableScan)
      if (isMultiSubScan) {
        return false;
      }

      int suggestedWidth = (int) Math.ceil((maxRows + 1) / targetSliceSize);

      int w = Math.min(maxWidth, suggestedWidth);
      if (w < 1) {
        w = 1;
      }
      return w == 1;
    }

    public boolean isRightSideOfLateral() {
      return this.rightSideOfLateral;
    }

    public void addUnnest(UnnestPrel prel) {
      add(prel);
    }

    public void setRightSideOfLateral(boolean rightSideOfLateral) {
      this.rightSideOfLateral = rightSideOfLateral;
    }

    public boolean isDistributionStrict() {
      return distributionAffinity == DistributionAffinity.HARD;
    }
  }
}
