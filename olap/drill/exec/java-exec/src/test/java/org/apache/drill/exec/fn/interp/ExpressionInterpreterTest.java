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
package org.apache.drill.exec.fn.interp;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.categories.SqlTest;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.fn.interpreter.InterpreterEvaluator;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.store.mock.MockScanBatchCreator;
import org.apache.drill.exec.store.mock.MockSubScanPOP;
import org.apache.drill.exec.store.mock.MockTableDef;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, SqlTest.class})
public class ExpressionInterpreterTest  extends PopUnitTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExpressionInterpreterTest.class);

  @Test
  public void interpreterNullableStrExpr() throws Exception {
    final String[] colNames = {"col1"};
    final TypeProtos.MajorType[] colTypes = {Types.optional(TypeProtos.MinorType.VARCHAR)};
    final String expressionStr =  "substr(col1, 1, 3)";
    final String[] expectedFirstTwoValues = {"aaa", "null"};

    doTest(expressionStr, colNames, colTypes, expectedFirstTwoValues);
  }


  @Test
  public void interpreterNullableBooleanExpr() throws Exception {
    final String[] colNames = {"col1"};
    final TypeProtos.MajorType[] colTypes = {Types.optional(TypeProtos.MinorType.VARCHAR)};
    final String expressionStr =  "col1 < 'abc' and col1 > 'abc'";
    final String[] expectedFirstTwoValues = {"false", "null"};

    doTest(expressionStr, colNames, colTypes, expectedFirstTwoValues);
  }


  @Test
  public void interpreterNullableIntegerExpr() throws Exception {
    final String[] colNames = {"col1"};
    final TypeProtos.MajorType[] colTypes = {Types.optional(TypeProtos.MinorType.INT)};
    final String expressionStr = "col1 + 100 - 1 * 2 + 2";
    final String[] expectedFirstTwoValues = {"-2147483548", "null"};

    doTest(expressionStr, colNames, colTypes, expectedFirstTwoValues);
  }

  @Test
  public void interpreterLikeExpr() throws Exception {
    final String[] colNames = {"col1"};
    final TypeProtos.MajorType[] colTypes = {Types.optional(TypeProtos.MinorType.VARCHAR)};
    final String expressionStr =  "like(col1, 'aaa%')";
    final String[] expectedFirstTwoValues = {"true", "null"};

    doTest(expressionStr, colNames, colTypes, expectedFirstTwoValues);
  }

  @Test
  public void interpreterCastExpr() throws Exception {
    final String[] colNames = {"col1"};
    final TypeProtos.MajorType[] colTypes = {Types.optional(TypeProtos.MinorType.VARCHAR)};
    final String expressionStr =  "cast(3+4 as float8)";
    final String[] expectedFirstTwoValues = {"7.0", "7.0"};

    doTest(expressionStr, colNames, colTypes, expectedFirstTwoValues);
  }

  @Test
  public void interpreterCaseExpr() throws Exception {
    final String[] colNames = {"col1"};
    final TypeProtos.MajorType[] colTypes = {Types.optional(TypeProtos.MinorType.VARCHAR)};
    final String expressionStr =  "case when substr(col1, 1, 3)='aaa' then 'ABC' else 'XYZ' end";
    final String[] expectedFirstTwoValues = {"ABC", "XYZ"};

    doTest(expressionStr, colNames, colTypes, expectedFirstTwoValues);
  }

  @Test
  public void interpreterDateTest() throws Exception {
    final String[] colNames = {"col1"};
    final TypeProtos.MajorType[] colTypes = {Types.optional(TypeProtos.MinorType.INT)};
    final String expressionStr = "now()";
    final BitControl.PlanFragment planFragment = BitControl.PlanFragment.getDefaultInstance();
    final QueryContextInformation queryContextInfo = planFragment.getContext();
    final int timeZoneIndex = queryContextInfo.getTimeZone();
    final DateTimeZone timeZone = DateTimeZone.forID(org.apache.drill.exec.expr.fn.impl.DateUtility.getTimeZone(timeZoneIndex));
    final org.joda.time.DateTime now = new org.joda.time.DateTime(queryContextInfo.getQueryStartTime(), timeZone);

    final long queryStartDate = now.getMillis();

    final TimeStampHolder out = new TimeStampHolder();

    out.value = queryStartDate;

    final ByteBuffer buffer = ByteBuffer.allocate(12);
    buffer.putLong(out.value);
    final long l = buffer.getLong(0);
    final LocalDateTime t = Instant.ofEpochMilli(l).atZone(ZoneOffset.systemDefault()).toLocalDateTime();

    final String[] expectedFirstTwoValues = {t.toString(), t.toString()};

    doTest(expressionStr, colNames, colTypes, expectedFirstTwoValues, planFragment);
  }


  protected void doTest(String expressionStr, String[] colNames, TypeProtos.MajorType[] colTypes, String[] expectFirstTwoValues) throws Exception {
    doTest(expressionStr, colNames, colTypes, expectFirstTwoValues, BitControl.PlanFragment.getDefaultInstance());
  }

  protected void doTest(String expressionStr, String[] colNames, TypeProtos.MajorType[] colTypes, String[] expectFirstTwoValues, BitControl.PlanFragment planFragment) throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
    final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);

    bit1.run();

    // Create a mock scan batch as input for evaluation.
    assertEquals(colNames.length, colTypes.length);

    final MockTableDef.MockColumn[] columns = new MockTableDef.MockColumn[colNames.length];

    for (int i = 0; i < colNames.length; i++ ) {
      columns[i] = new MockTableDef.MockColumn(colNames[i], colTypes[i].getMinorType(), colTypes[i].getMode(), 0, 0, 0, null, null, null);
    }

    final MockTableDef.MockScanEntry entry = new MockTableDef.MockScanEntry(10, false, 0, 1, columns);
    final MockSubScanPOP scanPOP = new MockSubScanPOP("testTable", false, java.util.Collections.singletonList(entry));

    final CloseableRecordBatch batch = createMockScanBatch(bit1, scanPOP, planFragment);

    batch.next();

    final ValueVector vv = evalExprWithInterpreter(expressionStr, batch, bit1);

    // Verify the first 2 values in the output of evaluation.
    assertEquals(2, expectFirstTwoValues.length);
    assertEquals(expectFirstTwoValues[0], getValueFromVector(vv, 0));
    assertEquals(expectFirstTwoValues[1], getValueFromVector(vv, 1));

    showValueVectorContent(vv);

    vv.clear();
    batch.close();
    batch.getContext().close();
    bit1.close();
  }

  private CloseableRecordBatch createMockScanBatch(Drillbit bit, MockSubScanPOP scanPOP, BitControl.PlanFragment planFragment) {
    final List<RecordBatch> children = Lists.newArrayList();
    final MockScanBatchCreator creator = new MockScanBatchCreator();

    try {
      final FragmentContextImpl context =
          new FragmentContextImpl(bit.getContext(), planFragment, null, bit.getContext().getFunctionImplementationRegistry());
      return creator.getBatch(context,scanPOP, children);
    } catch (Exception ex) {
      throw new DrillRuntimeException("Error when setup fragment context" + ex);
    }
  }

  private ValueVector evalExprWithInterpreter(String expression, RecordBatch batch, Drillbit bit) throws Exception {
    final LogicalExpression expr = parseExpr(expression);
    final ErrorCollector error = new ErrorCollectorImpl();
    final LogicalExpression materializedExpr = ExpressionTreeMaterializer.materialize(expr, batch, error, bit.getContext().getFunctionImplementationRegistry());
    if (error.getErrorCount() != 0) {
      logger.error("Failure while materializing expression [{}].  Errors: {}", expression, error);
      assertEquals(0, error.getErrorCount());
    }

    final MaterializedField outputField = MaterializedField.create("outCol", materializedExpr.getMajorType());
    final ValueVector vector = TypeHelper.getNewVector(outputField, bit.getContext().getAllocator());

    vector.allocateNewSafe();
    InterpreterEvaluator.evaluate(batch, vector, materializedExpr);

    return vector;
  }

  private void showValueVectorContent(ValueVector vw) {
    for (int row = 0; row < vw.getAccessor().getValueCount(); row++) {
      final Object o = vw.getAccessor().getObject(row);
      final String cellString;
      if (o instanceof byte[]) {
        cellString = DrillStringUtils.toBinaryString((byte[]) o);
      } else {
        cellString = DrillStringUtils.escapeNewLines(String.valueOf(o));
      }
      logger.info("{}th value: {}", row, cellString);
    }
  }

  private String getValueFromVector(ValueVector vw, int index) {
    final Object o = vw.getAccessor().getObject(index);
    final String cellString;
    if (o instanceof byte[]) {
      cellString = DrillStringUtils.toBinaryString((byte[]) o);
    } else {
      cellString = DrillStringUtils.escapeNewLines(String.valueOf(o));
    }
    return cellString;
  }
}
