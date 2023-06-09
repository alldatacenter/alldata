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
package org.apache.drill.test;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.junit.ComparisonFailure;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class OperatorTestBuilderTest extends PhysicalOpUnitTestBase {
  public static final String FIRST_NAME_COL = "firstname";
  public static final String LAST_NAME_COL = "lastname";

  @Test
  public void noCombineUnorderedTestPass() throws Exception {
    executeTest(buildInputData(), true, false);
  }

  @Test(expected = AssertionError.class)
  public void noCombineUnorderedTestFail() throws Exception {
    executeTest(buildIncorrectData(), true, false);
  }

  @Test
  public void noCombineOrderedTestPass() throws Exception {
    executeTest(buildInputData(), false, false);
  }

  @Test(expected = ComparisonFailure.class)
  public void noCombineOrderedTestFail() throws Exception {
    executeTest(buildIncorrectData(), false, false);
  }

  @Test
  public void combineUnorderedTestPass() throws Exception {
    executeTest(buildInputData(), true, true);
  }

  @Test(expected = AssertionError.class)
  public void combineUnorderedTestFail() throws Exception {
    executeTest(buildIncorrectData(), true, true);
  }

  @Test
  public void combineOrderedTestPass() throws Exception {
    executeTest(buildInputData(), false, true);
  }

  @Test(expected = ComparisonFailure.class)
  public void combineOrderedTestFail() throws Exception {
    executeTest(buildIncorrectData(), false, true);
  }

  private Project createProjectPhysicalOperator() {
    final List<NamedExpression> exprs = Arrays.asList(
      new NamedExpression(SchemaPath.getSimplePath(FIRST_NAME_COL), new FieldReference(FIRST_NAME_COL)),
      new NamedExpression(SchemaPath.getSimplePath(LAST_NAME_COL), new FieldReference(LAST_NAME_COL)));

    return new Project(exprs, new MockPhysicalOperator(), true);
  }

  private static TupleMetadata buildSchema() {
    return new SchemaBuilder()
      .add(FIRST_NAME_COL, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .add(LAST_NAME_COL, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .buildSchema();
  }

  private RowSet buildInputData() {
    return new RowSetBuilder(operatorFixture.allocator(), buildSchema()).
      addRow("billy", "bob").
      addRow("bobby", "fillet").
      build();
  }

  private RowSet buildIncorrectData() {
    return new RowSetBuilder(operatorFixture.allocator(), buildSchema()).
      addRow("billy", "bob").
      addRow("bambam", "fofof").
      build();
  }

  private void executeTest(final RowSet expectedRowSet, boolean unordered, boolean combine) throws Exception {
    final MockRecordBatch inputRowSetBatch = new MockRecordBatch.Builder().
      sendData(buildInputData()).
      build(fragContext);

    final OperatorTestBuilder testBuilder = opTestBuilder()
      .physicalOperator(createProjectPhysicalOperator());

    if (combine) {
      testBuilder.combineOutputBatches();
    }

    if (unordered) {
      testBuilder.unordered();
    }

    testBuilder
      .addUpstreamBatch(inputRowSetBatch)
      .addExpectedResult(expectedRowSet)
      .go();
  }

  public static class MockPhysicalOperator extends AbstractBase {
    @Override
    public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
      return null;
    }

    @Override
    public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) throws ExecutionSetupException {
      return null;
    }

    @Override
    public String getOperatorType() {
      return "";
    }

    @Override
    public Iterator<PhysicalOperator> iterator() {
      return null;
    }
  }
}
