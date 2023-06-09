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

import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.expression.FieldReference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import java.util.Iterator;
import java.util.List;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkState;

@JsonTypeName("window")
public class Window extends SingleInputOperator {
  private final List<NamedExpression> withins;
  private final List<NamedExpression> aggregations;
  private final List<Order.Ordering> orderings;
  private final long start;
  private final long end;


  @JsonCreator
  public Window(@JsonProperty("withins") List<NamedExpression> withins,
                @JsonProperty("aggregations") List<NamedExpression> aggregations,
                @JsonProperty("orderings") List<Order.Ordering> orderings,
                @JsonProperty("start") Long start,
                @JsonProperty("end") Long end) {
    super();
    this.withins = withins;
    this.start = start == null ? Long.MIN_VALUE : start;
    this.end = end == null ? Long.MIN_VALUE : end;
    this.aggregations = aggregations;
    this.orderings = orderings;
  }

  public List<NamedExpression> getWithins() {
    return withins;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public List<NamedExpression> getAggregations() {
    return aggregations;
  }

  public List<Order.Ordering> getOrderings() {
    return orderings;
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitWindow(this, value);
  }

  @Override
  public Iterator<LogicalOperator> iterator() {
    return Iterators.singletonIterator(getInput());
  }

  public static class Builder extends AbstractSingleBuilder<Window, Builder>{
    private List<NamedExpression> aggregations = Lists.newArrayList();
    private List<NamedExpression> withins = Lists.newArrayList();
    private List<Order.Ordering> orderings = Lists.newArrayList();
    private long start = Long.MIN_VALUE;
    private long end = Long.MIN_VALUE;


    public Builder addAggregation(FieldReference ref, LogicalExpression expr){
      aggregations.add(new NamedExpression(expr, ref));
      return this;
    }

    public Builder addWithin(FieldReference within, LogicalExpression expr) {
      withins.add(new NamedExpression(expr, within));
      return this;
    }

    public Window internalBuild() {
      //TODO withins can actually be empty: over(), over(order by <expression>), ...
      checkState(!withins.isEmpty(), "Withins in window must not be empty.");
      checkState(!aggregations.isEmpty(), "Aggregations in window must not be empty.");
      return new Window(withins, aggregations, orderings, start, end);
    }

    public Builder addOrdering(Order.Ordering ordering) {
      orderings.add(ordering);
      return this;
    }
  }
}
