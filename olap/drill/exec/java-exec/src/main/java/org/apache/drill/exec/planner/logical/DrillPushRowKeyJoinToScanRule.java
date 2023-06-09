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


import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.Pair;
import org.apache.drill.exec.planner.logical.RowKeyJoinCallContext.RowKey;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.planner.index.rules.MatchFunction;
import org.apache.drill.exec.planner.physical.PrelUtil;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * This rule implements the run-time filter pushdown via the rowkey join for queries with row-key filters. Row-key
 * filters are filters on primary-keys which appears in database groupscans {@link DbGroupScan}.
 *
 * Consider the following query:
 * SELECT L.LINEITEM_ID FROM LINEITEM L WHERE L._ID IN (SELECT O.LID FROM ORDERS O WHERE O.ORDER_DATE > '2019-01-01');
 * With this rule the logical plan on the left would transform to the logical plan on the right:
 * Project                                                Project
 *   Join (L._ID = O.LID)                                   RowKeyJoin (L._ID = O.LID)
 *     LineItem L                                ====>>       Lineitem L
 *     Filter (ORDER_DATE > '2019-01-01')                     Filter (ORDER_DATE > '2019-01-01')
 *       Orders O                                               Orders O
 *
 * During physical planning, the plan on the left would end up with e.g. HashJoin whereas the transformed plan would
 * have a RowKeyJoin along with a Restricted GroupScan instead.
 * Project                                                Project
 *   HashJoin (L._ID = O.LID)                               RowKeyJoin (L._ID = O.LID)
 *     Scan (LineItem L)                                      RestrictedScan (Lineitem L)
 *     Filter (ORDER_DATE > '2019-01-01')                     Filter (ORDER_DATE > '2019-01-01')
 *       Scan (Orders O)                                        Scan (Orders O)
 *
 * The row-key join pushes the `row-keys` for rows satisfying the filter into the Lineitem restricted groupscan. So
 * we only fetch these rowkeys instead of fetching all rows into the Hash Join.
 */
public class DrillPushRowKeyJoinToScanRule extends RelOptRule {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillPushRowKeyJoinToScanRule.class);
  final public MatchFunction match;

  private DrillPushRowKeyJoinToScanRule(RelOptRuleOperand operand, String description, MatchFunction match) {
    super(operand, description);
    this.match = match;
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    return match.match(call);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    doOnMatch((RowKeyJoinCallContext) match.onMatch(call));
  }

  public static DrillPushRowKeyJoinToScanRule JOIN = new DrillPushRowKeyJoinToScanRule(
      RelOptHelper.any(DrillJoin.class), "DrillPushRowKeyJoinToScanRule_Join", new MatchRelJ());

  public static class MatchRelJ implements MatchFunction<RowKeyJoinCallContext> {
    /*
     * Returns the rels matching the specified sequence relSequence. The match is executed
     * beginning from startingRel. An example of such a sequence is Join->Filter->Project->Scan
     */
    private List<RelNode> findRelSequence(Class[] relSequence, RelNode startingRel) {
      List<RelNode> matchingRels = new ArrayList<>();
      findRelSequenceInternal(relSequence, 0, startingRel, matchingRels);
      return matchingRels;
    }
    /*
     * Recursively match until the sequence is satisfied. Otherwise return. Recurse down intermediate nodes
     * such as RelSubset/HepRelVertex.
     */
    private void findRelSequenceInternal(Class[] classes, int idx, RelNode rel, List<RelNode> matchingRels) {
      if (rel instanceof HepRelVertex) {
        findRelSequenceInternal(classes, idx, ((HepRelVertex) rel).getCurrentRel(), matchingRels);
      } else if (rel instanceof RelSubset) {
        if (((RelSubset) rel).getBest() != null) {
          findRelSequenceInternal(classes, idx, ((RelSubset) rel).getBest(), matchingRels);
        } else {
          findRelSequenceInternal(classes, idx, ((RelSubset) rel).getOriginal(), matchingRels);
        }
      } else if (classes[idx].isInstance(rel)) {
        matchingRels.add(rel);
        if (idx + 1 < classes.length && rel.getInputs().size() > 0) {
          findRelSequenceInternal(classes, idx + 1, rel.getInput(0), matchingRels);
        }
      } else {
        if (logger.isDebugEnabled()) {
          String sequence, matchingSequence;
          StringBuffer sb = new StringBuffer();
          for (int i = 0; i < classes.length; i++) {
            if (i == classes.length - 1) {
              sb.append(classes[i].getCanonicalName().toString());
            } else {
              sb.append(classes[i].getCanonicalName().toString() + "->");
            }
          }
          sequence = sb.toString();
          sb.delete(0, sb.length());
          for (int i = 0; i < matchingRels.size(); i++) {
            if (i == matchingRels.size() - 1) {
              sb.append(matchingRels.get(i).getClass().getCanonicalName().toString());
            } else {
              sb.append(matchingRels.get(i).getClass().getCanonicalName().toString() + "->");
            }
          }
          matchingSequence = sb.toString();
          logger.debug("FindRelSequence: ABORT: Unexpected Rel={}, After={}, CurSeq={}",
              rel.getClass().getCanonicalName().toString(), matchingSequence, sequence);
        }
        matchingRels.clear();
      }
    }

    /*
     * Generate the rowkeyjoin call context. This context is useful when generating the transformed
     * plan nodes. It tries to identify some RelNode sequences e.g. Filter-Project-Scan and generates
     * the context based on the identified sequence.
     */
    private RowKeyJoinCallContext generateContext(RelOptRuleCall call, DrillJoin joinRel,
      RelNode joinChildRel, RowKey rowKeyLoc, int rowKeyPos, boolean swapInputs) {
      List<RelNode> matchingRels;
      // Sequence of rels (PFPS, FPS, PS, FS, S) matched for this rule
      Class[] PFPS = new Class[] {DrillProjectRel.class, DrillFilterRel.class, DrillProjectRel.class, DrillScanRel.class};
      Class[] FPS = new Class[] {DrillFilterRel.class, DrillProjectRel.class, DrillScanRel.class};
      Class[] PS = new Class[] {DrillProjectRel.class, DrillScanRel.class};
      Class[] FS = new Class[] {DrillFilterRel.class, DrillScanRel.class};
      Class[] S = new Class[] {DrillScanRel.class};
      logger.debug("GenerateContext(): Primary-key: Side={}, RowTypePos={}, SwapInputs={}",
          rowKeyLoc.name(), rowKeyPos, swapInputs);
      matchingRels = findRelSequence(PFPS, joinChildRel);
      if (matchingRels.size() > 0) {
        logger.debug("Matched rel sequence : Project->Filter->Project->Scan");
        return new RowKeyJoinCallContext(call, rowKeyLoc, rowKeyPos, swapInputs, joinRel,
            (DrillProjectRel) matchingRels.get(0), (DrillFilterRel) matchingRels.get(1),
            (DrillProjectRel) matchingRels.get(2), (DrillScanRel) matchingRels.get(3));
      }
      matchingRels = findRelSequence(FPS, joinChildRel);
      if (matchingRels.size() > 0) {
        logger.debug("Matched rel sequence : Filter->Project->Scan");
        return new RowKeyJoinCallContext(call, rowKeyLoc, rowKeyPos, swapInputs, joinRel,
            null, (DrillFilterRel) matchingRels.get(0), (DrillProjectRel) matchingRels.get(1),
            (DrillScanRel) matchingRels.get(2));
      }
      matchingRels = findRelSequence(PS, joinChildRel);
      if (matchingRels.size() > 0) {
        logger.debug("Matched rel sequence : Project->Scan");
        return new RowKeyJoinCallContext(call, rowKeyLoc, rowKeyPos, swapInputs, joinRel, null,
            null, (DrillProjectRel) matchingRels.get(0), (DrillScanRel) matchingRels.get(1));
      }
      matchingRels = findRelSequence(FS, joinChildRel);
      if (matchingRels.size() > 0) {
        logger.debug("Matched rel sequence : Filter->Scan");
        return new RowKeyJoinCallContext(call, rowKeyLoc, rowKeyPos, swapInputs, joinRel, null,
            (DrillFilterRel) matchingRels.get(0), null, (DrillScanRel) matchingRels.get(1));
      }
      matchingRels = findRelSequence(S, joinChildRel);
      if (matchingRels.size() > 0) {
        logger.debug("Matched rel sequence : Scan");
        return new RowKeyJoinCallContext(call, rowKeyLoc, rowKeyPos, swapInputs, joinRel, null, null,
            null, (DrillScanRel) matchingRels.get(0));
      }
      logger.debug("Matched rel sequence : None");
      return new RowKeyJoinCallContext(call, RowKey.NONE, -1, false, null, null, null, null, null);
    }

    @Override
    public boolean match(RelOptRuleCall call) {
      DrillJoin joinRel = call.rel(0);
      //Perform validity checks
      logger.debug("DrillPushRowKeyJoinToScanRule begin()");
      return canPushRowKeyJoinToScan(joinRel, call.getPlanner()).left;
    }

    @Override
    public RowKeyJoinCallContext onMatch(RelOptRuleCall call) {
      DrillJoin joinRel = call.rel(0);
      /*
       * Find which side of the join (left/right) has the primary-key column. Then find which sequence of rels
       * is present on that side of the join. We will need this sequence to correctly transform the left
       * side of the join.
       */
      Pair<Boolean, Pair<RowKey, Integer>> res = canPushRowKeyJoinToScan(joinRel, call.getPlanner());
      if (res.left) {
        if (res.right.left == RowKey.LEFT) {
          return generateContext(call, joinRel, joinRel.getLeft(), res.right.left, res.right.right, false);
        } else if (res.right.left == RowKey.RIGHT) {
          // If the primary-key column is present on the right, swapping of inputs is required. Find out if possible!
          if (canSwapJoinInputs(joinRel, res.right.left)) {
            return generateContext(call, joinRel, joinRel.getRight(), res.right.left, res.right.right, true);
          }
        } else if (res.right.left == RowKey.BOTH) {
          // Create row key join without swapping inputs, since either side of the join is eligible.
          return generateContext(call, joinRel, joinRel.getLeft(), res.right.left, res.right.right, false);
        }
      }
      return new RowKeyJoinCallContext(call, RowKey.NONE, -1, false, null, null, null, null, null);
    }
  }

  /* Assumption : Only the non-rowkey side needs to be checked. The row-key side does not have
   * any blocking operators for the transformation to work
   */
  private static boolean canSwapJoinInputs(DrillJoin joinRel, RowKey rowKeyLocation) {
    // We cannot swap the join inputs if the join is a semi-join. We determine it indirectly, by
    // checking for the presence of a aggregating Aggregate Rel (computes aggregates e.g. sum).
    if (rowKeyLocation == RowKey.LEFT
        || rowKeyLocation == RowKey.BOTH) {
      return canSwapJoinInputsInternal(joinRel.getRight());
    } else if (rowKeyLocation == RowKey.RIGHT) {
      // If the rowkey occurs on the right side, don't swap since it can potentially cause
      // wrong results unless we make additional changes to fix-up column ordinals for the
      // join condition as well as the parent/ancestors of the Join.

      // return canSwapJoinInputsInternal(joinRel.getLeft());
      return false;
    }
    return false;
  }

  /* Recurse down to find an aggregate (DrillAggRel). For semi-joins Calcite adds an aggregate
   * without any agg expressions.
   */
  private static boolean canSwapJoinInputsInternal(RelNode rel) {
    if (rel instanceof DrillAggregateRel &&
        ((DrillAggregateRel) rel).getAggCallList().size() > 0) {
      return false;
    } else if (rel instanceof HepRelVertex) {
      return canSwapJoinInputsInternal(((HepRelVertex) rel).getCurrentRel());
    } else if (rel instanceof RelSubset) {
      if (((RelSubset) rel).getBest() != null) {
        return canSwapJoinInputsInternal(((RelSubset) rel).getBest());
      } else {
        return canSwapJoinInputsInternal(((RelSubset) rel).getOriginal());
      }
    } else {
      for (RelNode child : rel.getInputs()) {
        if (!canSwapJoinInputsInternal(child)) {
          return false;
        }
      }
    }
    return true;
  }

  /*
   * Returns whether the join condition can be pushed (via rowkeyjoin mechanism). It returns true/false alongwith
   * whether the rowkey is present on the left/right side of the join and its 0-based index in the projection of that
   * side.
   */
  private static Pair<Boolean, Pair<RowKey, Integer>> canPushRowKeyJoinToScan(DrillJoin joinRel, RelOptPlanner planner) {
    RowKey rowKeyLoc = RowKey.NONE;
    logger.debug("canPushRowKeyJoinToScan(): Check: Rel={}", joinRel);

    if (joinRel instanceof RowKeyJoinRel) {
      logger.debug("SKIP: Join is a RowKeyJoin");
      return Pair.of(false, Pair.of(rowKeyLoc, -1));
    }

    if (joinRel.getJoinType() != JoinRelType.INNER) {
      logger.debug("SKIP: JoinType={} - NOT an INNER join", joinRel.getJoinType());
      return Pair.of(false, Pair.of(rowKeyLoc, -1));
    }

    // Single column equality condition
    if (joinRel.getCondition().getKind() != SqlKind.EQUALS
        || joinRel.getLeftKeys().size() != 1
        || joinRel.getRightKeys().size() != 1) {
      logger.debug("SKIP: #LeftKeys={}, #RightKeys={} - NOT single predicate join condition",
          joinRel.getLeftKeys().size(), joinRel.getRightKeys().size());
      return Pair.of(false, Pair.of(rowKeyLoc, -1));
    }

    // Join condition is of type primary-key = Col
    boolean hasLeftRowKeyCol = false;
    boolean hasRightRowKeyCol = false;
    int leftRowKeyPos = -1;
    int rightRowKeyPos = -1;
    if (joinRel.getCondition() instanceof RexCall) {
      for (RexNode op : ((RexCall) joinRel.getCondition()).getOperands()) {
        // Only support rowkey column (no expressions involving rowkey column)
        if (op instanceof RexInputRef) {
          //Check the left/right sides of the join to find the primary-key column
          int pos = ((RexInputRef)op).getIndex();
          if (pos < joinRel.getLeft().getRowType().getFieldList().size()) {
            if (isRowKeyColumn(((RexInputRef) op).getIndex(), joinRel.getLeft())) {
              logger.debug("FOUND Primary-key: Side=LEFT, RowType={}", joinRel.getLeft().getRowType());
              hasLeftRowKeyCol = true;
              leftRowKeyPos = pos;
              break;
            }
          } else {
            if (isRowKeyColumn(pos - joinRel.getLeft().getRowType().getFieldList().size(), joinRel.getRight())) {
              logger.debug("FOUND Primary-key: Side=RIGHT, RowType={}", joinRel.getRight().getRowType());
              hasRightRowKeyCol = true;
              rightRowKeyPos = pos;
              break;
            }
          }
        }
      }
    }
    if (!hasLeftRowKeyCol && !hasRightRowKeyCol) {
      logger.debug("SKIP: Primary-key = column condition NOT found");
      return Pair.of(false, Pair.of(rowKeyLoc, -1));
    }
    /* Get the scan rel on left/right side of the join (at least one of them should be non-null for us
     * to proceed). This would be the side with the primary-key column and would be later transformed to restricted
     * group scan.
     */
    RelNode leftScan = getValidJoinInput(joinRel.getLeft());
    RelNode rightScan = getValidJoinInput(joinRel.getRight());

    if (leftScan == null && rightScan == null) {
      logger.debug("SKIP: Blocking operators between join and scans");
      return Pair.of(false, Pair.of(rowKeyLoc, -1));
    }
    // Only valid if the side with the primary-key column doesn't not have any blocking operations e.g. aggregates
    if (leftScan != null && hasLeftRowKeyCol) {
      rowKeyLoc = RowKey.LEFT;
    }
    if (rightScan != null && hasRightRowKeyCol) {
      if (rowKeyLoc == RowKey.LEFT) {
        rowKeyLoc = RowKey.BOTH;
      } else {
        rowKeyLoc = RowKey.RIGHT;
      }
    }
    // Heuristic : only generate such plans if selectivity less than RKJ conversion selectivity threshold.
    // Rowkey join plans do random scans, hence are expensive. Since this transformation takes place in
    // the HEP planner, it is not costed. Hence, the heuristic to potentially prevent an expensive plan!
    RelMetadataQuery mq = RelMetadataQuery.instance();
    double ncSel = PrelUtil.getPlannerSettings(planner).getRowKeyJoinConversionSelThreshold();
    double sel;
    if (rowKeyLoc == RowKey.NONE) {
      return Pair.of(false, Pair.of(rowKeyLoc, -1));
    } else if (rowKeyLoc == RowKey.LEFT) {
      sel = computeSelectivity(joinRel.getRight().estimateRowCount(mq), leftScan.estimateRowCount(mq));
      if (sel > ncSel) {
        logger.debug("SKIP: SEL= {}/{} = {}\\%, THRESHOLD={}\\%",
            joinRel.getRight().estimateRowCount(mq), leftScan.estimateRowCount(mq), sel*100.0, ncSel*100.0);
        return Pair.of(false, Pair.of(rowKeyLoc, -1));
      }
    } else {
      sel = computeSelectivity(joinRel.getLeft().estimateRowCount(mq), rightScan.estimateRowCount(mq));
      if (sel > ncSel) {
        logger.debug("SKIP: SEL= {}/{} = {}\\%, THRESHOLD={}\\%",
            joinRel.getLeft().estimateRowCount(mq), rightScan.estimateRowCount(mq), sel*100.0, ncSel*100.0);
        return Pair.of(false, Pair.of(rowKeyLoc, -1));
      }
    }
    int rowKeyPos = rowKeyLoc == RowKey.RIGHT ? rightRowKeyPos : leftRowKeyPos;
    logger.info("FOUND Primary-key: Side={}, RowTypePos={}, Sel={}, Threshold={}",
        rowKeyLoc.name(), rowKeyPos, sel, ncSel);
    return Pair.of(true, Pair.of(rowKeyLoc, rowKeyPos));
  }

  /*
   * Computes the selectivity given the number of rows selected from the total rows
   */
  private static double computeSelectivity(double selectRows, double totalRows) {
    if (totalRows <= 0) {
      return 1.0;
    }
    return Math.min(1.0, Math.max(0.0, selectRows/totalRows));
  }

  /* Finds the scan rel underlying the given rel. No blocking operators should
   * be present in between. Currently, the rowkeyjoin operator cannot send rowkeys
   * across major fragment boundaries. The presence of blocking operators can
   * lead to creation of a fragment boundary, hence the limitation. Once, we can
   * send rowkeys across fragment boundaries, we can remove this restriction.
   */
  public static RelNode getValidJoinInput(RelNode rel) {
    if (rel instanceof DrillScanRel) {
      return rel;
    } else if (rel instanceof DrillProjectRel
        || rel instanceof DrillFilterRel
        || rel instanceof DrillLimitRel) {
      for (RelNode child : rel.getInputs()) {
        RelNode tgt = getValidJoinInput(child);
        if (tgt != null) {
          return tgt;
        }
      }
    } else if (rel instanceof HepRelVertex) {
      return getValidJoinInput(((HepRelVertex) rel).getCurrentRel());
    } else if (rel instanceof RelSubset) {
      if (((RelSubset) rel).getBest() != null) {
        return getValidJoinInput(((RelSubset) rel).getBest());
      } else {
        return getValidJoinInput(((RelSubset) rel).getOriginal());
      }
    }
    return null;
  }

  /* Finds whether the given column reference is for the rowkey col(also known as primary-key col).
   * We need to recurse down the operators looking at their references down to the scan
   * to figure out whether the reference is a rowkey col. Projections can rearrange the
   * incoming columns. We also need to handle HepRelVertex/RelSubset while handling the rels.
   */
  private static boolean isRowKeyColumn(int index, RelNode rel) {
    RelNode curRel = rel;
    int curIndex = index;
    while (curRel != null && !(curRel instanceof DrillScanRel)) {
      logger.debug("IsRowKeyColumn: Rel={}, RowTypePos={}, RowType={}", curRel.toString(), curIndex,
          curRel.getRowType().toString());
      if (curRel instanceof HepRelVertex) {
        curRel = ((HepRelVertex) curRel).getCurrentRel();
      } else if (curRel instanceof RelSubset) {
        if (((RelSubset) curRel).getBest() != null) {
          curRel = ((RelSubset) curRel).getBest();
        } else {
          curRel = ((RelSubset) curRel).getOriginal();
        }
      } else {
        RelNode child = null;
        // For multi-input parent rels, found out the 0-based index in the child rel,
        // before recursing down that child rel.
        for (RelNode input : curRel.getInputs()) {
          if (input.getRowType().getFieldList().size() <= curIndex) {
            curIndex -= input.getRowType().getFieldList().size();
          } else {
            child = input;
            break;
          }
        }
        curRel = child;
      }
      // If no exprs present in projection the column index remains the same in the child.
      // Otherwise, the column index is the `RexInputRef` index.
      if (curRel != null && curRel instanceof DrillProjectRel) {
        List<RexNode> childExprs = curRel.getChildExps();
        if (childExprs != null && childExprs.size() > 0) {
          if (childExprs.get(curIndex) instanceof RexInputRef) {
            curIndex = ((RexInputRef) childExprs.get(curIndex)).getIndex();
          } else {
            // Currently do not support expressions on rowkey col. So if an expr is present,
            // return false
            logger.debug("IsRowKeyColumn: ABORT: Primary-key EXPR$={}", childExprs.get(curIndex).toString());
            return false;
          }
        }
      }
    }
    logger.debug("IsRowKeyColumn:Primary-key Col={} ",
        curRel != null ? curRel.getRowType().getFieldNames().get(curIndex) : "??");
    // Get the primary-key col name from the scan and match with the column being referenced.
    if (curRel != null && curRel instanceof DrillScanRel) {
      if (((DrillScanRel) curRel).getGroupScan() instanceof DbGroupScan) {
        DbGroupScan dbGroupScan = (DbGroupScan) ((DrillScanRel) curRel).getGroupScan();
        String rowKeyName = dbGroupScan.getRowKeyName();
        DbGroupScan restrictedGroupScan = dbGroupScan.getRestrictedScan(((DrillScanRel)curRel).getColumns());
        // Also verify this scan supports restricted groupscans(random seeks)
        if (restrictedGroupScan != null &&
            curRel.getRowType().getFieldNames().get(curIndex).equalsIgnoreCase(rowKeyName)) {
          logger.debug("IsRowKeyColumn: FOUND: Rel={}, RowTypePos={}, RowType={}",
              curRel.toString(), curIndex, curRel.getRowType().toString());
          return true;
        }
      }
    }
    logger.debug("IsRowKeyColumn: NOT FOUND");
    return false;
  }

  protected void doOnMatch(RowKeyJoinCallContext rkjCallContext) {
    if (rkjCallContext.getRowKeyLocation() != RowKey.NONE) {
      doOnMatch(rkjCallContext.getCall(), rkjCallContext.getRowKeyPosition(), rkjCallContext.mustSwapInputs(),
          rkjCallContext.getJoinRel(), rkjCallContext.getUpperProjectRel(), rkjCallContext.getFilterRel(),
          rkjCallContext.getLowerProjectRel(), rkjCallContext.getScanRel());
    }
  }

  private void doOnMatch(RelOptRuleCall call, int rowKeyPosition, boolean swapInputs, DrillJoin joinRel,
      DrillProjectRel upperProjectRel, DrillFilterRel filterRel, DrillProjectRel lowerProjectRel, DrillScanRel scanRel) {
    // Swap the inputs, when necessary (i.e. when the primary-key col is on the right-side of the join)
    logger.debug("Transforming: Swapping of join inputs is required!");
    RelNode right = swapInputs ? joinRel.getLeft() : joinRel.getRight();
    // The join condition is primary-key = COL similarly to PK-FK relationship in relational DBs
    // where primary-key is PK and COL is FK
    List<Integer> leftJoinKeys = ImmutableList.of(rowKeyPosition);
    List<Integer> rightJoinKeys = swapInputs ? joinRel.getLeftKeys() : joinRel.getRightKeys();
    // Create restricted group scan for scanRel and reconstruct the left side of the join.
    DbGroupScan restrictedGroupScan = ((DbGroupScan)scanRel.getGroupScan()).getRestrictedScan(
        scanRel.getColumns());
    RelNode leftRel =  new DrillScanRel(scanRel.getCluster(), scanRel.getTraitSet(), scanRel.getTable(),
        restrictedGroupScan, scanRel.getRowType(), scanRel.getColumns(), scanRel.partitionFilterPushdown());
    // Transform the project/filter rels if present
    if (lowerProjectRel != null) {
      leftRel = lowerProjectRel.copy(lowerProjectRel.getTraitSet(), ImmutableList.of(leftRel));
    }
    if (filterRel != null) {
      leftRel = filterRel.copy(filterRel.getTraitSet(), leftRel, filterRel.getCondition());
    }
    if (upperProjectRel != null) {
      leftRel = upperProjectRel.copy(upperProjectRel.getTraitSet(), ImmutableList.of(leftRel));
    }
    // Create the equi-join condition for the rowkey join
    RexNode joinCondition =
        RelOptUtil.createEquiJoinCondition(leftRel, leftJoinKeys,
            right, rightJoinKeys, joinRel.getCluster().getRexBuilder());
    logger.debug("Transforming: LeftKeys={}, LeftRowType={}, RightKeys={}, RightRowType={}",
        leftJoinKeys, leftRel.getRowType(), rightJoinKeys, right.getRowType());
    RowKeyJoinRel rowKeyJoin = new RowKeyJoinRel(joinRel.getCluster(), joinRel.getTraitSet(), leftRel, right,
        joinCondition, joinRel.getJoinType(), joinRel instanceof DrillSemiJoinRel);
    logger.info("Transforming: SUCCESS: Register runtime filter pushdown plan (rowkeyjoin)");
    call.transformTo(rowKeyJoin);
  }
}
