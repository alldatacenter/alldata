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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.plan.RelOptRuleCall;

/*
 * Preserves the context to be used for transforming join to rowkey join in order
 * to enable join filter pushdown.
 */
public class RowKeyJoinCallContext {

  public enum RowKey {NONE, LEFT, RIGHT, BOTH};
  // RelOptRule call
  private RelOptRuleCall call;
  // Row key present on which side of the join
  private RowKey rowKeyLoc;
  // 0-based index of the row-key column in the join input
  private int rowKeyPos;
  // swapping of row-key join inputs necessary
  private boolean swapInputs;
  private DrillJoin joinRel;
  // rels on the rowkey side of the join to be transformed
  private DrillProjectRel upperProjectRel;
  private DrillFilterRel filterRel;
  private DrillProjectRel lowerProjectRel;
  private DrillScanRel scanRel;

  public RowKeyJoinCallContext (RelOptRuleCall call, RowKey rowKeyLoc, int rowKeyPos, boolean swapInputs,
      DrillJoin joinRel, DrillProjectRel upperProjectRel, DrillFilterRel filterRel, DrillProjectRel lowerProjectRel,
          DrillScanRel scanRel) {
    this.call = call;
    this.rowKeyLoc = rowKeyLoc;
    this.rowKeyPos = rowKeyPos;
    this.swapInputs = swapInputs;
    this.joinRel = joinRel;
    this.upperProjectRel = upperProjectRel;
    this.filterRel = filterRel;
    this.lowerProjectRel = lowerProjectRel;
    this.scanRel = scanRel;
  }

  public RelOptRuleCall getCall() {
    return call;
  }

  public RowKey getRowKeyLocation() {
    return rowKeyLoc;
  }

  public int getRowKeyPosition() {
    return rowKeyPos;
  }

  public boolean mustSwapInputs() {
    return swapInputs;
  }

  public DrillJoin getJoinRel() {
    return joinRel;
  }

  public DrillProjectRel getUpperProjectRel() {
    return upperProjectRel;
  }

  public DrillFilterRel getFilterRel() {
    return filterRel;
  }

  public DrillProjectRel getLowerProjectRel() {
    return lowerProjectRel;
  }

  public DrillScanRel getScanRel() {
    return scanRel;
  }
}
