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
/**
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
package org.apache.drill.exec.planner.index;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexLocalRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexOver;
import org.apache.calcite.rex.RexRangeRef;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;


/**
 * A visitor class that analyzes a filter condition (typically an index condition)
 * and a supplied input collation and determines what the output collation would be
 * after applying the filter.
 */
public class FindFiltersForCollation extends RexVisitorImpl<Boolean> {

  // map of field collation (e.g [0]) to the list of filter conditions
  // involving the same field
  private Map<Integer, List<RexNode> > collationFilterMap = Maps.newHashMap();

  // set of comparison operators that allow collation
  private Set<SqlKind> allowedComparisons = Sets.newHashSet();

  // input rel's rowtype
  private RelDataType inputRowType;

  private int currentFieldIndex;

  public FindFiltersForCollation(RelNode input) {
    super(true);
    inputRowType = input.getRowType();
    init();
  }

  /**
   * Initialize the set of comparison operators that allow creating collation property.
   */
  private void init() {
    allowedComparisons.addAll(SqlKind.COMPARISON);
    allowedComparisons.add(SqlKind.LIKE);
  }

  /**
   * For each RelFieldCollation, gather the set of filter conditions corresponding to it
   * e.g suppose input collation is [0][1] and there are filter conditions: $0 = 5 AND $1 > 10 AND $1 <20
   * then the map will have 2 entries:
   * [0] -> ($0 = 5)
   * [1] -> {($1 > 10), ($1 < 20)}
   *
   * @param indexCondition index condition to analyze
   * @return list of output RelFieldCollation
   */
  public Map<Integer, List<RexNode> > analyze(RexNode indexCondition) {
    for (int idx = 0; idx < inputRowType.getFieldCount(); idx++) {
      currentFieldIndex = idx;
      indexCondition.accept(this);
    }
    return collationFilterMap;
  }

  @Override
  public Boolean visitInputRef(RexInputRef inputRef) {
    if (inputRef.getIndex() == currentFieldIndex) {
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitLiteral(RexLiteral literal) {
    return true;
  }

  @Override
  public Boolean visitOver(RexOver over) {
    return false;
  }

  @Override
  public Boolean visitCorrelVariable(RexCorrelVariable correlVariable) {
    return false;
  }

  @Override
  public Boolean visitCall(RexCall call) {
    final SqlOperator op = call.getOperator();
    final SqlKind kind = op.getKind();

    if (kind == SqlKind.AND) {
      for (RexNode n : call.getOperands()) {
        n.accept(this);
      }
    } else if (kind == SqlKind.CAST) {
      // For the filter analyzer itself, if the Project has not been pushed
      // down below the Filter, then CAST is present in the filter condition.
      // Return True for such case since CAST exprs are valid for collation.
      // Otherwise, filter is only referencing output of the Project and we won't
      // hit this else condition (i.e filter will have $0, $1 etc which would be
      // visited by visitInputRef()).
      return true;
    } else if (op == SqlStdOperatorTable.ITEM) {
      List<RexNode> ops = call.getOperands();
      boolean left = ops.get(0).accept(this);
      boolean right = ops.get(1).accept(this);
      return left && right;
    } else if (allowedComparisons.contains(kind)) {
      List<RexNode> ops = call.getOperands();
      boolean left = ops.get(0).accept(this);
      boolean right = ops.get(1).accept(this);

      if (left && right) {
        if (collationFilterMap.containsKey(currentFieldIndex)) {
          List<RexNode> n = collationFilterMap.get(currentFieldIndex);
          n.add(call);
        } else {
          List<RexNode> clist = Lists.newArrayList();
          clist.add(call);
          collationFilterMap.put(currentFieldIndex, clist);
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitDynamicParam(RexDynamicParam dynamicParam) {
    return false;
  }

  @Override
  public Boolean visitRangeRef(RexRangeRef rangeRef) {
    return false;
  }

  @Override
  public Boolean visitFieldAccess(RexFieldAccess fieldAccess) {
    return false;
  }

  @Override
  public Boolean visitLocalRef(RexLocalRef localRef) {
    return false;
  }

}
