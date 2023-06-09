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
package org.apache.drill.exec.store.base.filter;

import java.util.List;

import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Condensed form of a Drill WHERE clause expression
 * node. Models only those nodes that are typically used
 * in rewrite-style filter push-down. Not intended for
 * the more complex interpreted form of filter push-down
 * such as that needed for directory-based partitions.
 * <p>
 * Each node references the source Calcite node as well as
 * the selectivity that Calcite attaches to the node. Any
 * expressions pushed to a scan must reduce the scan
 * cost by the amount of the selectivity, else Calcite will
 * conclude that the original plan (without push-down) is
 * cheaper.
 */
public abstract class ExprNode {

  @JsonIgnore
  private RexNode rexNode;

  public ExprNode() {
    rexNode = null;
  }

  public void tag(RexNode rexNode) {
    this.rexNode = rexNode;
  }

  public RexNode rexNode() { return rexNode; }

  public abstract double selectivity();

  public interface ColumnTypeNode {
    String colName();
    MinorType type();
  }

  /**
   * An expression node with an unlimited set of children.
   */
  public abstract static class ListNode extends ExprNode {
    @JsonProperty("children")
    public final List<ExprNode> children;

    public ListNode(List<ExprNode> children) {
      this.children = children;
    }
  }

  /**
   * Represents a set of AND'ed expressions in Conjunctive Normal
   * Form (CNF). Typically the WHERE clause is rewritten to gather
   * all expressions into a single CNF node. The children are often
   * called "conjuncts."
   */
  public static class AndNode extends ListNode {

    @JsonCreator
    public AndNode(@JsonProperty("children") List<ExprNode> children) {
      super(children);
    }

    @Override
    public double selectivity() {
      double selectivity = 1;
      for (ExprNode child : children) {
        selectivity *= child.selectivity();
      }
      return selectivity;
    }
  }

  /**
   * Represents a set of OR'ed expressions in Disjunctive Normal
   * Form (CNF). Or'ed expresssions often cannot be pushed down except in the
   * special case of a series of expressions on the same column, which can
   * sometimes be handled as a series of requests, each for a different disjunct.
   *
   */
  public static class OrNode extends ListNode {

    @JsonCreator
    public OrNode(@JsonProperty("children") List<ExprNode> children) {
      super(children);
    }

    /**
     * Compute the selectivity of this DNF (OR) clause. Drill assumes
     * the selectivity of = is 0.15. An OR is a series of equal statements,
     * so selectivity is n * 0.15. However, limit total selectivity to
     * 0.9 (that is, if there are more than 6 clauses in the DNF, assume
     * at least some reduction.)
     *
     * @return the estimated selectivity of this DNF clause
     */
    @Override
    public double selectivity() {
      if (children.size() == 0) {
        return 1.0;
      }
      double selectivity = 0;
      for (ExprNode child : children) {
        selectivity += child.selectivity();
      }
      return Math.min(0.9, selectivity);
    }
  }

  public abstract static class RelOpNode extends ExprNode {
    @JsonProperty("op")
    public final RelOp op;

    public RelOpNode(RelOp op) {
      this.op = op;
    }

    @Override
    public double selectivity() {
      return op.selectivity();
    }
  }

  /**
   * Semanticized form of a Calcite relational operator. Abstracts
   * out the Drill implementation details to capture just the
   * column name, operator and value. Supports only expressions
   * of the form:<br>
   * {@code <column> <relop> <const>}<br>
   * Where the column is a simple name (not an array or map reference),
   * the relop is one of a defined set, and the constant is one
   * of the defined Drill types.
   * <p>
   * (The driver will convert expressions of the form:<br>
   * {@code <const> <relop> <column>}<br>
   * into the normalized form represented here.
   */
  @JsonInclude(Include.NON_NULL)
  @JsonPropertyOrder({"colName", "op", "value"})
  public static class ColRelOpConstNode extends RelOpNode {
    @JsonProperty("colName")
    public final String colName;
    @JsonProperty("value")
    public final ConstantHolder value;

    @JsonCreator
    public ColRelOpConstNode(
        @JsonProperty("colName") String colName,
        @JsonProperty("op") RelOp op,
        @JsonProperty("value") ConstantHolder value) {
      super(op);
      Preconditions.checkArgument(op.argCount() == 1 || value != null);
      this.colName = colName;
      this.value = value;
    }

    /**
     * Rewrite the RelOp with a normalized value.
     *
     * @param from the original RelOp
     * @param value the new value with a different type and matching
     * value
     */
    public ColRelOpConstNode(ColRelOpConstNode from, ConstantHolder value) {
      super(from.op);
      Preconditions.checkArgument(from.op.argCount() == 2);
      this.colName = from.colName;
      this.value = value;
    }

    /**
     * Rewrite a relop using the given normalized value.
     *
     * @param normalizedValue given normalized value
     * @return a new RelOp with the normalized value. Will be the same relop
     * if the normalized value is the same as the unnormalized value.
     */
    public ColRelOpConstNode normalize(ConstantHolder normalizedValue) {
      if (value == normalizedValue) {
        return this;
      }
      return new ColRelOpConstNode(this, normalizedValue);
    }

    public ColRelOpConstNode rewrite(String newName, ConstantHolder newValue) {
      if (value == newValue && colName.equals(newName)) {
        return this;
      }
      return new ColRelOpConstNode(newName, op, newValue);
    }

    @Override
    public String toString() {
      PlanStringBuilder builder = new PlanStringBuilder(this)
        .field("op", op.name())
        .field("colName", colName);
      if (value != null) {
        builder.field("type", value.type.name())
               .field("value", value.value);
      }
      return builder.toString();
    }
  }
}
