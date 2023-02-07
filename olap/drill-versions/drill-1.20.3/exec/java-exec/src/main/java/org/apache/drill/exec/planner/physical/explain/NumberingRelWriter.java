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
package org.apache.drill.exec.planner.physical.explain;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.calcite.avatica.util.Spacer;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.runtime.FlatLists;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.util.Pair;
import org.apache.drill.exec.planner.physical.LateralJoinPrel;
import org.apache.drill.exec.planner.physical.HashJoinPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.UnnestPrel;
import org.apache.drill.exec.planner.physical.explain.PrelSequencer.OpId;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

/**
 * Copied mostly from RelWriterImpl but customized to create user useful ids.
 */
class NumberingRelWriter implements RelWriter {
  //~ Instance fields --------------------------------------------------------

  protected final PrintWriter pw;
  private final SqlExplainLevel detailLevel;
  protected final Spacer spacer = new Spacer();
  private final List<Pair<String, Object>> values = new ArrayList<>();
  private final Map<String, Prel> sourceOperatorRegistry;

  private final Map<Prel, OpId> ids;
  //~ Constructors -----------------------------------------------------------

  public NumberingRelWriter(Map<Prel, OpId> ids, PrintWriter pw, SqlExplainLevel detailLevel) {
    this.pw = pw;
    this.ids = ids;
    this.detailLevel = detailLevel;
    this.sourceOperatorRegistry = new HashMap<>();
  }

  //~ Methods ----------------------------------------------------------------

  protected void explain_(
      RelNode rel,
      List<Pair<String, Object>> values) {
    RelMetadataQuery mq = RelMetadataQuery.instance();
    if (!mq.isVisibleInExplain(rel, detailLevel)) {
      // render children in place of this, at same level
      explainInputs(rel);
      return;
    }

    StringBuilder s = new StringBuilder();
    OpId id = ids.get(rel);
    if (id != null) {
      s.append(String.format("%02d-%02d", id.fragmentId, id.opId));
    }else{
      s.append("     ");
    }
    s.append("  ");

    if (id != null && id.opId == 0) {
      for (int i = 0; i < spacer.get(); i++) {
        s.append('-');
      }
    }else{
      spacer.spaces(s);
    }

    s.append("  ");

    s.append(rel.getRelTypeName().replace("Prel", ""));
    if (detailLevel != SqlExplainLevel.NO_ATTRIBUTES) {
      int j = 0;
      s.append(getDependentSrcOp(rel));
      for (Pair<String, Object> value : values) {
        if (value.right instanceof RelNode) {
          continue;
        }
        if (j++ == 0) {
          s.append("(");
        } else {
          s.append(", ");
        }
        s.append(value.left)
            .append("=[")
            .append(value.right)
            .append("]");
      }
      if (j > 0) {
        s.append(")");
      }
    }
    if (detailLevel == SqlExplainLevel.ALL_ATTRIBUTES) {
      s.append(" : rowType = ")
        .append(rel.getRowType())
        .append(": rowcount = ")
        .append(mq.getRowCount(rel))
        .append(", cumulative cost = ")
        .append(mq.getCumulativeCost(rel))
        .append(", id = ")
        .append(rel.getId());
    }
    pw.println(s);
    spacer.add(2);
    explainInputs(rel);
    spacer.subtract(2);
  }

  private String getDependentSrcOp(RelNode rel) {
    if (rel instanceof UnnestPrel) {
      return this.getDependentSrcOp((UnnestPrel) rel);
    }
    return "";
  }

  private String getDependentSrcOp(UnnestPrel unnest) {
    Prel parent = this.getRegisteredPrel(unnest.getParentClass());
    if (parent != null && parent instanceof LateralJoinPrel) {
      OpId id = ids.get(parent);
      return String.format(" [srcOp=%02d-%02d] ", id.fragmentId, id.opId);
    }
    return "";
  }

  public void register(Prel toRegister) {
    this.sourceOperatorRegistry.put(toRegister.getClass().getSimpleName(), toRegister);
  }

  public Prel getRegisteredPrel(Class<?> classname) {
    return this.sourceOperatorRegistry.get(classname.getSimpleName());
  }

  public void unRegister(Prel unregister) {
    this.sourceOperatorRegistry.remove(unregister.getClass().getSimpleName());
  }


  private void explainInputs(RelNode rel) {
    if (rel instanceof LateralJoinPrel) {
      this.explainInputs((LateralJoinPrel) rel);
    } else {
      List<RelNode> inputs = rel.getInputs();
      if (rel instanceof HashJoinPrel && ((HashJoinPrel) rel).isSwapped()) {
        HashJoinPrel joinPrel = (HashJoinPrel) rel;
        inputs = FlatLists.of(joinPrel.getRight(), joinPrel.getLeft());
      }
      for (RelNode input : inputs) {
        input.explain(this);
      }
    }
  }

  //Lateral is handled differently because explain plan
  //needs to show relation between Lateral and Unnest operators.
  private void explainInputs(LateralJoinPrel lateralJoinPrel) {
    lateralJoinPrel.getInput(0).explain(this);
    this.register(lateralJoinPrel);
    lateralJoinPrel.getInput(1).explain(this);
    this.unRegister(lateralJoinPrel);
  }

  public final void explain(RelNode rel, List<Pair<String, Object>> valueList) {
    explain_(rel, valueList);
  }

  public SqlExplainLevel getDetailLevel() {
    return detailLevel;
  }

  public RelWriter input(String term, RelNode input) {
    values.add(Pair.of(term, (Object) input));
    return this;
  }

  public RelWriter item(String term, Object value) {
    values.add(Pair.of(term, value));
    return this;
  }

  public RelWriter itemIf(String term, Object value, boolean condition) {
    if (condition) {
      item(term, value);
    }
    return this;
  }

  @SuppressWarnings("deprecation")
  public RelWriter done(RelNode node) {
    int i = 0;
    if (values.size() > 0 && values.get(0).left.equals("subset")) {
      ++i;
    }
    for (RelNode input : node.getInputs()) {
      assert values.get(i).right == input;
      ++i;
    }
    for (RexNode expr : node.getChildExps()) {
      assert values.get(i).right == expr;
      ++i;
    }
    final List<Pair<String, Object>> valuesCopy =
        ImmutableList.copyOf(values);
    values.clear();
    explain_(node, valuesCopy);
    pw.flush();
    return this;
  }

  public boolean nest() {
    return false;
  }

  /**
   * Converts the collected terms and values to a string. Does not write to
   * the parent writer.
   */
  public String simple() {
    final StringBuilder buf = new StringBuilder("(");
    for (Ord<Pair<String, Object>> ord : Ord.zip(values)) {
      if (ord.i > 0) {
        buf.append(", ");
      }
      buf.append(ord.e.left).append("=[").append(ord.e.right).append("]");
    }
    buf.append(")");
    return buf.toString();
  }
}

