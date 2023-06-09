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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.Leaf;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.impl.unnest.UnnestRecordBatch;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@JsonTypeName("unnest")
public class UnnestPOP extends AbstractBase implements Leaf {

  public static final String OPERATOR_TYPE = "UNNEST";

  @JsonProperty("implicitColumn")
  private final String implicitColumn;

  private final SchemaPath column;

  @JsonIgnore
  private UnnestRecordBatch unnestBatch;

  @JsonCreator
  public UnnestPOP(
      @JsonProperty("child") PhysicalOperator child, // Operator with incoming record batch
      @JsonProperty("column") SchemaPath column,
      @JsonProperty("implicitColumn") String implicitColumn) {
    this.column = column;
    this.implicitColumn = implicitColumn;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) throws ExecutionSetupException {
    assert children.isEmpty();
    UnnestPOP newUnnest = new UnnestPOP(null, column, this.implicitColumn);
    newUnnest.addUnnestBatch(this.unnestBatch);
    return newUnnest;
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  public SchemaPath getColumn() {
    return column;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitUnnest(this, value);
  }

  public void addUnnestBatch(UnnestRecordBatch unnestBatch) {
    this.unnestBatch = unnestBatch;
  }

  @JsonIgnore
  public UnnestRecordBatch getUnnestBatch() {
    return this.unnestBatch;
  }

  @JsonProperty("implicitColumn")
  public String getImplicitColumn() { return this.implicitColumn; }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
