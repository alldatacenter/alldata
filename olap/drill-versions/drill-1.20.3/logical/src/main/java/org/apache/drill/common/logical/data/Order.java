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
package org.apache.drill.common.logical.data;

import java.util.Iterator;
import java.util.List;

import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@JsonTypeName("order")
public class Order extends SingleInputOperator {

  private final List<Ordering> orderings;
  private final FieldReference within;

  @JsonCreator
  public Order(@JsonProperty("within") FieldReference within, @JsonProperty("orderings") List<Ordering> orderings) {
    this.orderings = orderings;
    this.within = within;
  }

  public List<Ordering> getOrderings() {
    return orderings;
  }

  public FieldReference getWithin() {
    return within;
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
      return logicalVisitor.visitOrder(this, value);
  }

  @Override
  public Iterator<LogicalOperator> iterator() {
      return Iterators.singletonIterator(getInput());
  }


  /**
   * Representation of a SQL &lt;sort specification>.
   */
  public static class Ordering {

    public static final String ORDER_ASC = "ASC";
    public static final String ORDER_DESC = "DESC";
    public static final String ORDER_ASCENDING = "ASCENDING";
    public static final String ORDER_DESCENDING = "DESCENDING";

    public static final String NULLS_FIRST = "FIRST";
    public static final String NULLS_LAST = "LAST";
    public static final String NULLS_UNSPECIFIED = "UNSPECIFIED";

    private final LogicalExpression expr;
    /** Net &lt;ordering specification>. */
    private final Direction direction;
    /** Net &lt;null ordering> */
    private final NullDirection nullOrdering;
    /** The values in the plans for ordering specification are ASC, DESC, not the
     * full words featured in the Calcite {@link Direction} Enum, need to map between them. */
    private static ImmutableMap<String, Direction> DRILL_TO_CALCITE_DIR_MAPPING =
        ImmutableMap.<String, Direction>builder()
        .put(ORDER_ASC, Direction.ASCENDING)
        .put(ORDER_DESC, Direction.DESCENDING)
        .put(ORDER_ASCENDING, Direction.ASCENDING)
        .put(ORDER_DESCENDING, Direction.DESCENDING).build();
    private static ImmutableMap<String, NullDirection> DRILL_TO_CALCITE_NULL_DIR_MAPPING =
        ImmutableMap.<String, NullDirection>builder()
            .put(NULLS_FIRST, NullDirection.FIRST)
            .put(NULLS_LAST, NullDirection.LAST)
            .put(NULLS_UNSPECIFIED, NullDirection.UNSPECIFIED).build();

    /**
     * Constructs a sort specification.
     * @param  expr  ...
     * @param  strOrderingSpec  the &lt;ordering specification> as string;
     *             allowed values: {@code "ASC"}, {@code "DESC"}, {@code null};
     *             null specifies default &lt;ordering specification>
     *                   ({@code "ASC"} / {@link Direction#ASCENDING})
     * @param  strNullOrdering   the &lt;null ordering> as string;
     *             allowed values: {@code "FIRST"}, {@code "LAST"},
     *             {@code "UNSPECIFIED"}, {@code null};
     *             null specifies default &lt;null ordering>
     *             (omitted / {@link NullDirection#UNSPECIFIED}, interpreted later)
     */
    @JsonCreator
    public Ordering( @JsonProperty("order") String strOrderingSpec,
                     @JsonProperty("expr") LogicalExpression expr,
                     @JsonProperty("nullDirection") String strNullOrdering ) {
      this.expr = expr;
      this.direction = getOrderingSpecFromString(strOrderingSpec);
      this.nullOrdering = getNullOrderingFromString(strNullOrdering);
    }

    public Ordering(Direction direction, LogicalExpression e, NullDirection nullOrdering) {
      this.expr = e;
      this.direction = filterDrillSupportedDirections(direction);
      this.nullOrdering = nullOrdering;
    }

    public Ordering(Direction direction, LogicalExpression e) {
      this(direction, e, NullDirection.FIRST);
    }

    @VisibleForTesting
    public static Direction getOrderingSpecFromString(String strDirection) {
      Direction dir = null;
      if (strDirection != null) {
        dir = DRILL_TO_CALCITE_DIR_MAPPING.get(strDirection.toUpperCase());
      }
      if (dir != null || strDirection == null) {
        return filterDrillSupportedDirections(dir);
      } else {
        throw new DrillRuntimeException(
            "Unknown <ordering specification> string (not \"ASC\", \"DESC\", "
                + "or null): \"" + strDirection + "\"" );
      }
    }

    @VisibleForTesting
    public static NullDirection getNullOrderingFromString( String strNullOrdering ) {
      NullDirection nullDir = null;
      if (strNullOrdering != null) {
        nullDir = DRILL_TO_CALCITE_NULL_DIR_MAPPING.get(strNullOrdering.toUpperCase());
      }
      if (nullDir != null || strNullOrdering == null) {
        return filterDrillSupportedNullDirections(nullDir);
      } else {
        throw new DrillRuntimeException(
            "Internal error:  Unknown <null ordering> string (not "
                + "\"" + NULLS_FIRST + "\", "
                + "\"" + NULLS_LAST + "\", or "
                + "\"" + NULLS_UNSPECIFIED + "\" or null): "
                + "\"" + strNullOrdering + "\"" );
      }
    }

    /**
     * Disallows unsupported values for ordering direction (provided by Calcite but not implemented by Drill)
     *
     * Provides a default value of ASCENDING in the case of a null.
     *
     * @param direction
     * @return - a sanitized direction value
     */
    private static Direction filterDrillSupportedDirections(Direction direction) {
      if (direction == null || direction == Direction.ASCENDING) {
        return Direction.ASCENDING;
      }
      else if (Direction.DESCENDING.equals( direction) ) {
        return direction;
      }
      else {
        throw new DrillRuntimeException(
            "Unknown <ordering specification> string (not \"ASC\", \"DESC\", "
            + "or null): \"" + direction + "\"" );
      }
    }

    /**
     * Disallows unsupported values for null ordering (provided by Calcite but not implemented by Drill),
     * currently all values are supported.
     *
     * Provides a default value of UNSPECIFIED in the case of a null.
     *
     * @param nullDirection
     * @return - a sanitized direction value
     */
    private static NullDirection filterDrillSupportedNullDirections(NullDirection nullDirection) {
      if ( null == nullDirection) {
        return NullDirection.UNSPECIFIED;
      }
      switch (nullDirection) {
        case LAST:
        case FIRST:
        case UNSPECIFIED:
          return nullDirection;
        default:
          throw new DrillRuntimeException(
              "Internal error:  Unknown <null ordering> string (not "
                  + "\"" + NullDirection.FIRST.name() + "\", "
                  + "\"" + NullDirection.LAST.name() + "\", or "
                  + "\"" + NullDirection.UNSPECIFIED.name() + "\" or null): "
                  + "\"" + nullDirection + "\"" );
      }
   }

    @Override
    public String toString() {
      return
          super.toString()
          + "[ "
          + " expr = " + expr
          + ", direction = " + direction
          + ", nullOrdering = " + nullOrdering
          + "] ";
    }

    @JsonIgnore
    public Direction getDirection() {
      return direction;
    }

    public LogicalExpression getExpr() {
      return expr;
    }

    public String getOrder() {
      switch (direction) {
      case ASCENDING:
        return Direction.ASCENDING.shortString;
      case DESCENDING:
        return Direction.DESCENDING.shortString;
      default:
        throw new DrillRuntimeException(
            "Unexpected " + Direction.class.getName() + " value other than "
            + Direction.ASCENDING + " or " + Direction.DESCENDING + ": "
            + direction );
      }
    }

    public NullDirection getNullDirection() {
      return nullOrdering;
    }

    /**
     * Reports whether NULL sorts high or low in this ordering.
     *
     * @return
     * {@code true}  if NULL sorts higher than any other value;
     * {@code false} if NULL sorts lower  than any other value
     */
    public boolean nullsSortHigh() {
      final boolean nullsHigh;

      switch (nullOrdering) {

      case UNSPECIFIED:
        // Default:  NULL sorts high: like NULLS LAST if ASC, FIRST if DESC.
        nullsHigh = true;
        break;

      case FIRST:
        // FIRST: NULL sorts low with ASC, high with DESC.
        nullsHigh = Direction.DESCENDING == getDirection();
        break;

      case LAST:
        // LAST: NULL sorts high with ASC, low with DESC.
        nullsHigh = Direction.ASCENDING == getDirection();
        break;

      default:
        throw new DrillRuntimeException(
            "Unexpected " + NullDirection.class.getName() + " value other than "
            + NullDirection.FIRST + ", " + NullDirection.LAST + " or " + NullDirection.UNSPECIFIED + ": "
            + nullOrdering );
      }

      return nullsHigh;
    }

  }

  public static Builder builder(){
    return new Builder();
  }

  public static class Builder extends AbstractSingleBuilder<Order, Builder>{
    private List<Ordering> orderings = Lists.newArrayList();
    private FieldReference within;

    public Builder setWithin(FieldReference within){
      this.within = within;
      return this;
    }

    public Builder addOrdering(Direction direction, LogicalExpression e, NullDirection collation){
      orderings.add(new Ordering(direction, e, collation));
      return this;
    }

    @Override
    public Order internalBuild() {
      return new Order(within, orderings);
    }


  }
}
