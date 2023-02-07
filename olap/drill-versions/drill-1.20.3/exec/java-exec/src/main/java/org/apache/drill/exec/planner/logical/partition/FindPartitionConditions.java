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
package org.apache.drill.exec.planner.logical.partition;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;

import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexOver;
import org.apache.calcite.rex.RexRangeRef;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlBinaryOperator;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.sql.fun.SqlRowOperator;
import org.apache.calcite.util.Util;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;


public class FindPartitionConditions extends RexVisitorImpl<Void> {
  /** Whether an expression is a directory filter, and if so, whether
   * it can be pushed into the scan.
   */
  enum PushDirFilter {
    NO_PUSH, PUSH, PARTIAL_PUSH
  }

  /**
   * During top-down traversal of the expression tree, keep track of the
   * current operators that are directory filters. Children that are
   * directory filters add themselves to their parent operators.
   *
   * NOTE: this auxiliary class is necessary because RexNodes are immutable.
   * If they were mutable, we could have easily added/dropped inputs as we
   * encountered directory filters.
   */
  public class OpState {
    private SqlOperator sqlOperator;
    private List<RexNode> children = Lists.newArrayList();
    public OpState(SqlOperator op) {
      sqlOperator = op;
    }
    public SqlOperator getOp() {
      return sqlOperator;
    }
    public void addChild(RexNode n) {
      if (!children.contains(n)) {
        children.add(n);
      }
    }
    public List<RexNode> getChildren() {
      return children;
    }
    public void clear() {
      children.clear();
    }
  }

  private final BitSet dirs;

  // The Scan could be projecting several dirN columns but we are only interested in the
  // ones that are referenced by the Filter, so keep track of such referenced dirN columns.
  private final BitSet referencedDirs;

  private final List<PushDirFilter> pushStatusStack =  Lists.newArrayList();
  private final Deque<OpState> opStack = new ArrayDeque<OpState>();

  /* While traversing the filter tree, some RexCalls need special handling where
   * a partial expression cannot be built bottom up and the whole expression needs
   * to be built. Eg: cast(VALUE/COLUMN as Type). We need to treat this as holistic
   * expression and cannot treat the VALUE/COLUMN to be casted as a separate expression
   * Below count keeps track of expressions that need such handling. Its type is an integer
   * and not a boolean because we can have such nested expressions.
   */
  private int holisticExpression = 0;

  private RexBuilder builder = null;
  private RexNode resultCondition = null;

  public FindPartitionConditions(BitSet dirs) {
    // go deep
    super(true);
    this.dirs = dirs;
    this.referencedDirs = new BitSet(dirs.size());
  }

  public FindPartitionConditions(BitSet dirs, RexBuilder builder) {
    // go deep
    super(true);
    this.dirs = dirs;
    this.builder = builder;
    this.referencedDirs = new BitSet(dirs.size());
  }

  public void analyze(RexNode exp) {
    assert pushStatusStack.isEmpty();

    exp.accept(this);

    // Deal with top of stack
    assert pushStatusStack.size() == 1;
    PushDirFilter rootPushDirFilter = pushStatusStack.get(0);
    if (rootPushDirFilter == PushDirFilter.PUSH) {
      // The entire subtree was directory filter, so add it to the result.
      addResult(exp);
    }
    pushStatusStack.clear();
  }

  public RexNode getFinalCondition() {
    return resultCondition;
  }

  public BitSet getReferencedDirs() {
    return referencedDirs;
  }

  private Void pushVariable() {
    pushStatusStack.add(PushDirFilter.NO_PUSH);
    return null;
  }

  private void addResult(RexNode exp) {
    // processing a special expression, will be handled holistically by the parent
    if (holisticExpression > 0) {
      return;
    }
    // when we find a directory filter, add it to the current operator's
    // children (if one exists)
    if (!opStack.isEmpty()) {
      OpState op = opStack.peek();
      op.addChild(exp);
    } else {
      resultCondition = exp;
    }
  }

  /*
   * One of the children for the current OP is a NO PUSH. Clear all the children
   * added for the current OP that were a PUSH.
   */
  private void clearChildren() {
    if (!opStack.isEmpty()) {
      OpState op = opStack.peek();
      if (op.getChildren().size() >= 1) {
        op.clear();
      }
    }
  }

  /*
   * Pop the current operation from the stack (opStack) as we are done with it.
   * If it has children, then it means the current OP itself is a PUSH
   * and hence add itself as a child to the parent OP. If no more parents
   * remains, it means that the current OP is the root condition and
   * set the root condition in that case.
   */
  private void popOpStackAndBuildFilter() {
    // Parsing a special expression; handled holistically by the parent
    if (holisticExpression > 0) {
      return;
    }
    OpState currentOp = opStack.pop();
    int size = currentOp.getChildren().size();
    RexNode newFilter = null;
    if (size >= 1) {
      if (size == 1 && currentOp.getOp() instanceof SqlBinaryOperator) {
        /* The only operator for which we allow partial pushes is AND.
         * For all other operators we clear the children if one of the
         * children is a no push.
         */
        if (currentOp.getOp().getKind() == SqlKind.AND) {
          newFilter = currentOp.getChildren().get(0);
          for (OpState opState : opStack) {
            if (opState.getOp().getKind() == SqlKind.NOT) {
              //AND under NOT should not get pushed
              newFilter = null;
            }
          }

        }
      } else {
        newFilter = builder.makeCall(currentOp.getOp(), currentOp.getChildren());
      }
    }

    if (newFilter != null) {
      // add this new filter to my parent boolean operator's children
      if (!opStack.isEmpty()) {
        OpState parentOp = opStack.peek();
        parentOp.addChild(newFilter);
      } else {
        resultCondition = newFilter;
      }
    }
  }

  private boolean isHolisticExpression(RexCall call) {
    /* If we are already processing a holistic expression then all
     * nested expressions should be treated holistically as well
     */
    if (holisticExpression > 0) {
      return true;
    }

    if (call.getOperator().getSyntax() == SqlSyntax.SPECIAL ||
        call.getOperator().getSyntax() == SqlSyntax.FUNCTION) {
      return true;
    }
    return false;
  }

  protected boolean inputRefToPush(RexInputRef inputRef) {
    return dirs.get(inputRef.getIndex());
  }

  public Void visitInputRef(RexInputRef inputRef) {
    if (inputRefToPush(inputRef)) {
      pushStatusStack.add(PushDirFilter.PUSH);
      addResult(inputRef);
      referencedDirs.set(inputRef.getIndex());
    } else {
      pushStatusStack.add(PushDirFilter.NO_PUSH);
    }
    return null;
  }

  public Void visitLiteral(RexLiteral literal) {
    pushStatusStack.add(PushDirFilter.PUSH);
    addResult(literal);
    return null;
  }

  public Void visitOver(RexOver over) {
    // assume NO_PUSH until proven otherwise
    analyzeCall(over, PushDirFilter.NO_PUSH);
    return null;
  }

  public Void visitCorrelVariable(RexCorrelVariable correlVariable) {
    return pushVariable();
  }

  public Void visitCall(RexCall call) {
    analyzeCall(call, PushDirFilter.PUSH);
    return null;
  }

  /*
   * Traverse over the RexCall recursively. All children that are potential
   * PUSH are added as children to the current operator. Once all children
   * of the current operator are processed and if it satisfies to be a
   * PUSH then the current operator is added as a child to its parent and so on.
   *
   * This bottom up building does not work for some expressions like 'cast'.
   * In that case, we don't add the current operator or its children in the
   * opStack and based on whether the special expression is a PUSH we add the
   * whole expression to its parent op. Fpr such special expressions we maintain
   * specialExpression count.
   */
  private void analyzeCall(RexCall call, PushDirFilter callPushDirFilter) {
    if (isHolisticExpression(call)) {
      holisticExpression++;
    } else {
      opStack.push(new OpState(call.getOperator()));
    }

    // visit operands, pushing their states onto stack
    super.visitCall(call);

    // look for NO_PUSH operands
    int operandCount = call.getOperands().size();
    List<PushDirFilter> operandStack = Util.last(pushStatusStack, operandCount);
    for (PushDirFilter operandPushDirFilter : operandStack) {
      if (operandPushDirFilter == PushDirFilter.NO_PUSH) {
        callPushDirFilter = PushDirFilter.NO_PUSH;
      } else if (operandPushDirFilter == PushDirFilter.PARTIAL_PUSH) {
        callPushDirFilter = PushDirFilter.PARTIAL_PUSH;
      }
    }

    // Even if all operands are PUSH, the call itself may
    // be non-deterministic.
    if (!call.getOperator().isDeterministic()) {
      callPushDirFilter = PushDirFilter.NO_PUSH;
    } else if (call.getOperator().isDynamicFunction()) {
      // For now, treat it same as non-deterministic.
      callPushDirFilter = PushDirFilter.NO_PUSH;
    }

    // Row operator itself can't be reduced to a PUSH
    if ((callPushDirFilter == PushDirFilter.PUSH)
        && (call.getOperator() instanceof SqlRowOperator)) {
      callPushDirFilter = PushDirFilter.NO_PUSH;
    }


    if (callPushDirFilter == PushDirFilter.NO_PUSH) {
      OpState currentOp = opStack.peek();
      if (currentOp != null) {
        if (currentOp.sqlOperator.getKind() != SqlKind.AND) {
          clearChildren();
        } else {
          // AND op, check if we pushed some children
          if (currentOp.children.size() > 0) {
            callPushDirFilter = PushDirFilter.PARTIAL_PUSH;
          }
        }
      }
    }

    // pop operands off of the stack
    operandStack.clear();

    if (isHolisticExpression(call)) {
      assert holisticExpression > 0;
      holisticExpression--;
      if (callPushDirFilter == PushDirFilter.PUSH) {
        addResult(call);
      }
    } else {
      popOpStackAndBuildFilter();
    }

    // push PushDirFilter result for this call onto stack
    pushStatusStack.add(callPushDirFilter);
  }

  public Void visitDynamicParam(RexDynamicParam dynamicParam) {
    return pushVariable();
  }

  public Void visitRangeRef(RexRangeRef rangeRef) {
    return pushVariable();
  }

  public Void visitFieldAccess(RexFieldAccess fieldAccess) {
    return pushVariable();
  }


}
