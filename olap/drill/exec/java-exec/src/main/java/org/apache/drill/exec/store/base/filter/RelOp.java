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

/**
 * Fixed set of Drill relational operators, using well-defined
 * names. Distilled from the more general string function names
 * used in the query plan tree.
 */
public enum RelOp {
  // Order of LT, LE, GE, GT is important for
  // value comparisons
  EQ, NE, LT, LE, GE, GT, IS_NULL, IS_NOT_NULL;

  /**
   * Return the result of flipping the sides of an
   * expression:</br>
   * {@code a op b} &rarr; {@code b op.invert() a}
   *
   * @return a new relop resulting from flipping the sides
   * of the expression, or this relop if the operation
   * is symmetric.
   */
  public RelOp invert() {
    switch(this) {
      case LT:
        return GT;
      case LE:
        return GE;
      case GT:
        return LT;
      case GE:
        return LE;
      default:
        return this;
    }
  }

  /**
   * Returns the number of arguments for the relop.
   * @return 1 for IS (NOT) NULL, 2 otherwise
   */
  public int argCount() {
    switch (this) {
      case IS_NULL:
      case IS_NOT_NULL:
        return 1;
      default:
        return 2;
    }
  }

  /**
   * Poor-man's guess at selectivity of each operator.
   * Should match Calcite's built-in defaults. The Calcite estimates
   * are not great, but we need to match them.
   * <p>
   * If a query has access to metadata, then each predicates should
   * have a computed selectivity based on that metadata. This
   * mechanism should be extended to include that selectivity as a field,
   * and pass it back from this method.
   *
   * @return crude estimate of operator selectivity
   * @see {@code package org.apache.calcite.rel.metadata.RelMdUtil}
   */
  public double selectivity() {
    switch (this) {
      case EQ:
        return 0.15;
      case GE:
      case GT:
      case LE:
      case LT:
      case NE: // Very bad estimate!
        return 0.5;
      case IS_NOT_NULL:
      case IS_NULL:
        return 0.9;
      default:
        return 0.25;
    }
  }
}