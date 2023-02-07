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

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@JsonTypeName("groupingaggregate")
public class GroupingAggregate extends SingleInputOperator{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GroupingAggregate.class);

  private final List<NamedExpression> keys;
  private final List<NamedExpression> exprs;

  public GroupingAggregate(@JsonProperty("keys") List<NamedExpression> keys, @JsonProperty("exprs") List<NamedExpression> exprs) {
    super();
    this.keys = keys;
    this.exprs = exprs;
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
      return logicalVisitor.visitGroupingAggregate(this, value);
  }

  @Override
  public Iterator<LogicalOperator> iterator() {
    return Iterators.singletonIterator(getInput());
  }

  public static Builder builder(){
    return new Builder();
  }

  public List<NamedExpression> getKeys(){
    return keys;
  }

  public List<NamedExpression> getExprs(){
    return exprs;
  }

  public static class Builder extends AbstractSingleBuilder<GroupingAggregate, Builder>{
    private List<NamedExpression> keys = Lists.newArrayList();
    private List<NamedExpression> exprs = Lists.newArrayList();

    public Builder addKey(FieldReference ref, LogicalExpression expr){
      keys.add(new NamedExpression(expr, ref));
      return this;
    }

    public Builder addKey(NamedExpression expr){
      keys.add(expr);
      return this;
    }

    public Builder addExpr(NamedExpression expr){
      exprs.add(expr);
      return this;
    }

    public Builder addExpr(FieldReference ref, LogicalExpression expr){
      exprs.add(new NamedExpression(expr, ref));
      return this;
    }

    @Override
    public GroupingAggregate internalBuild(){
      GroupingAggregate ga =  new GroupingAggregate(keys, exprs);
      return ga;
    }

  }



}
