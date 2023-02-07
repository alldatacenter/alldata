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
package org.apache.drill.exec.physical.config;

import java.util.List;

import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.exec.physical.base.AbstractSingle;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("project")
public class Project extends AbstractSingle{

  public static final String OPERATOR_TYPE = "PROJECT";

  private final List<NamedExpression> exprs;

  /**
   * {@link org.apache.drill.exec.planner.physical.ProjectPrel for the meaning of flag 'outputProj'}
   */
  private boolean outputProj = false;

  @JsonCreator
  public Project(@JsonProperty("exprs") List<NamedExpression> exprs, @JsonProperty("child") PhysicalOperator child, @JsonProperty("outputProj") boolean outputProj) {
    super(child);
    this.exprs = exprs;
    this.outputProj = outputProj;
  }

  public Project(List<NamedExpression> exprs, PhysicalOperator child) {
    super(child);
    this.exprs = exprs;
  }

  public List<NamedExpression> getExprs() {
    return exprs;
  }

  /**
   * @return true if Project is for the query's final output. Such Project is added by TopProjectVisitor,
   * to handle fast NONE when all the inputs to the query are empty and are skipped.
   */
  public boolean isOutputProj() {
    return outputProj;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E{
    return physicalVisitor.visitProject(this, value);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new Project(exprs, child, outputProj);
  }

  @Override
  public SelectionVectorMode getSVMode() {
    return child.getSVMode();
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
