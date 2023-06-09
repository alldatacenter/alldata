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

import java.util.List;

import org.apache.drill.common.exceptions.ExpressionParsingException;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@JsonTypeName("project")
public class Project extends SingleInputOperator {

  private final List<NamedExpression> selections;

  @JsonCreator
  public Project(@JsonProperty("projections") List<NamedExpression> selections) {
    this.selections = selections;
    if (selections == null || selections.size() == 0) {
      throw new ExpressionParsingException(
          "Project did not provide any projection selections.  At least one projection must be provided.");
    }
  }

  @JsonProperty("projections")
  public List<NamedExpression> getSelections() {
    return selections;
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitProject(this, value);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends AbstractSingleBuilder<Project, Builder> {

    private List<NamedExpression> exprs = Lists.newArrayList();

    public Builder addExpr(NamedExpression expr) {
      exprs.add(expr);
      return this;
    }

    public Builder addExpr(FieldReference ref, LogicalExpression expr) {
      exprs.add(new NamedExpression(expr, ref));
      return this;
    }

    @Override
    public Project internalBuild() {
      return new Project(exprs);
    }

  }

}
