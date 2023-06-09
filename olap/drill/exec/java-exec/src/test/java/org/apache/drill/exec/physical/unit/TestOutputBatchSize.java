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
package org.apache.drill.exec.physical.unit;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.FlattenPOP;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.config.MergeJoinPOP;
import org.apache.drill.exec.physical.config.NestedLoopJoinPOP;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.physical.config.UnionAll;
import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.planner.physical.AggPrelBase;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.exec.util.Text;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.apache.drill.test.LegacyOperatorTestBuilder;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestOutputBatchSize extends PhysicalOpUnitTestBase {
  private static final long initReservation = AbstractBase.INIT_ALLOCATION;
  private static final long maxAllocation = AbstractBase.MAX_ALLOCATION;
  // Keeping row count below 4096 so we do not produce more than one batch.
  // scanBatch with json reader produces batches of 4k.
  private int numRows = 4000;
  private static final String wideString =
    "b00dUrA0oa2i4ZEHg6zvPXPXlVQYB2BXe8T5gIEtvUDzcN6yUkIqyS07gaAy8k4ac6Bn1cxblsXFnkp8g8hiQkUMJPyl6" +
    "l0jTdsIzQ4PkVCURGGyF0aduGqCXUaKp91gqkRMvLhHhmrHdEb22QN20dXEHSygR7vrb2zZhhfWeJbXRsesuYDqdGig801IAS6VWRIdQtJ6gaRhCdNz";

  /**
   *  Figures out what will be total size of the batches for a given Json input batch.
   */
  private long getExpectedSize(List<String> expectedJsonBatches) throws ExecutionSetupException {
    // Create a dummy scanBatch to figure out the size.
    RecordBatch scanBatch = new ScanBatch(new MockPhysicalOperator(), fragContext, getReaderListForJsonBatches(expectedJsonBatches, fragContext));
    Iterable<VectorAccessible> batches = new BatchIterator(scanBatch);

    long totalSize = 0;
    for (VectorAccessible batch : batches) {
      RecordBatchSizer sizer = new RecordBatchSizer(batch);
      totalSize += sizer.getNetBatchSize();
    }
    return totalSize;
  }

  @Test
  public void testProjectMap() throws Exception {
    // create input rows like this.
    // "a" : 5, "b" : wideString, "c" : [{"trans_id":"t1", amount:100, trans_time:7777777, type:sports},
    //                                   {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}]
    StringBuilder batchString = new StringBuilder("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + "abc" + "\"," +
                         " \"c\" : { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}," +
                         " \"d\": { \"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}");
      batchString.append(i != numRows - 1 ? "}," : "}]");
    }
    List<String> inputJsonBatches = Lists.newArrayList();
    inputJsonBatches.add(batchString.toString());

    StringBuilder expectedString = new StringBuilder("[");
    for (int i = 0; i < numRows; i++) {
      expectedString.append("{\"aplusamount\": 105");
      expectedString.append(i != numRows - 1 ? "}," : "}]");
    }

    List<String> expectedJsonBatches = Lists.newArrayList();
    expectedJsonBatches.add(expectedString.toString());

    String[] baselineColumns = new String[1];
    baselineColumns[0] = "aplusamount";

    String[] expr = {"a + c.amount ", baselineColumns[0]};

    Project projectConf = new Project(parseExprs(expr), null);
    mockOpContext(projectConf, initReservation, maxAllocation);

    long totalSize = getExpectedSize(expectedJsonBatches);

    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(projectConf)
            .inputDataStreamJson(inputJsonBatches)
            .baselineColumns(baselineColumns)
            .expectedNumBatches(2)  // verify number of batches
            .expectedBatchSize(totalSize / 2); // verify batch size.

    Long[] baseLineValues = {(5l + 100l)}; // a + c.amount
    for (int i = 0; i < numRows; i++) {
      opTestBuilder.baselineValues(baseLineValues);
    }
    opTestBuilder.go();
  }

  @Test
  public void testProjectVariableWidthFunctions() throws  Exception {
    //size calculators
    StringBuilder batchString = new StringBuilder("[");
    String strValue = "abcde";
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\" : " + "\"" + strValue + "\"");
      batchString.append(i != numRows - 1 ? "}," : "}]");
    }
    List<String> inputJsonBatches = Lists.newArrayList();
    inputJsonBatches.add(batchString.toString());

    // inputSize, as calculated below will be numRows * (inputRowsize),
    // inputRowSize = metadata cols + sizeof("abcde"), numRows = 4000
    // So, inputSize = 4000 * ( 4 + 1 + 5 ) = 40000
    // inputSize is used as the batch memory limit for the tests.
    // Depending on the function being evaluated, different output batch counts will be expected
    long inputSize = getExpectedSize(inputJsonBatches);
    String inputSizeStr = inputSize + "";

    String [][] functions =
                         {  //{ OP name, OP result, OP SQL str, Memory Limit, Num Expected Batches }

                         // concat() o/p size will be 2 x input size, so at least 2 batches expected
                         {"concat", strValue + strValue, "concat(a,a)", inputSizeStr, 2 + ""},
                         // upper() o/p size will same as input size, so at least 1 batch is expected
                         {"upper", strValue.toUpperCase(),"upper(a)", inputSizeStr, 1 + ""},
                         // repeat() is assumed to produce a row-size of 50.
                         // input row size is 10 (null vector + offset vector + abcde)
                         // so at least 5 batches are expected
                         {"repeat", strValue + strValue, "repeatstr(a, 2)", inputSizeStr, 5 + ""},
                         // substr() is assumed to produce a row size which is same as input
                         // so at least 1 batch is expected
                         {"substr", strValue.substring(0, 4), "substr(a, 1, 4)", inputSizeStr, 1 + "" }
                      };

    for (String[] fn : functions) {
      String outputColumnName = fn[0] + "_result";
      String operationResult = fn[1];
      String exprStr = fn[2];
      long memoryLimit = Long.valueOf(fn[3]);
      int expectedNumBatches = Integer.valueOf(fn[4]);

      StringBuilder expectedString = new StringBuilder("[");
      for (int i = 0; i < numRows; i++) {
        expectedString.append("{\"" + outputColumnName + "\":" + operationResult);
        expectedString.append(i != numRows - 1 ? "}," : "}]");
      }

      List<String> expectedJsonBatches = Lists.newArrayList();
      expectedJsonBatches.add(expectedString.toString());

      String[] baselineColumns = new String[1];
      baselineColumns[0] = outputColumnName;

      String[] expr = {exprStr, baselineColumns[0]};

      Project projectConf = new Project(parseExprs(expr), null);
      mockOpContext(projectConf, initReservation, maxAllocation);

      fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", memoryLimit);

      LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
              .physicalOperator(projectConf)
              .inputDataStreamJson(inputJsonBatches)
              .baselineColumns(baselineColumns)
              .expectedNumBatches(expectedNumBatches)  // verify number of batches
              .expectedBatchSize(memoryLimit); // verify batch size.

      String[] baseLineValues = {operationResult}; //operation(a, a)
      for (int i = 0; i < numRows; i++) {
        opTestBuilder.baselineValues(baseLineValues);
      }
      opTestBuilder.go();
    }
  }


  @Test
  public void testProjectFixedWidthTransfer() throws Exception {
    testProjectFixedWidthImpl(true, 100);
  }

  @Test
  public void testProjectFixedWidthNewColumn() throws Exception {
    testProjectFixedWidthImpl(false, 100);
  }

   /**
    * Tests BatchSizing of fixed-width transfers and new column creations in Project.
    * Transfer: Evaluates 'select *'
    * New Columns: Evalutes 'select C0 + 5 as C0 ... C[columnCount] + 5 as C[columnCount]
    * @param transfer
    * @throws Exception
    */

  public void testProjectFixedWidthImpl(boolean transfer, int columnCount) throws  Exception {

    //generate a row with N columns C0..C[columnCount], value in a column is same as column id
    StringBuilder jsonRow = new StringBuilder("{");
    String[] baselineColumns = new String [columnCount];
    Object[] baselineValues = new Long[columnCount];

    int exprSize = (transfer ? 2 : 2 * columnCount);
    String[] expr = new String[exprSize];

    // Expr for a 'select *' as expected by parseExprs()
    if (transfer) {
      expr[0] = "`**`";
      expr[1] = "`**`";
    }

    for (int i = 0; i < columnCount; i++) {
      jsonRow.append("\"" + "C" + i + "\": " + i + ((i == columnCount - 1) ? "" : ","));
      baselineColumns[i] = "C" + i;
      if (!transfer) {
        expr[i * 2] = baselineColumns[i] + " + 5";
        expr[i * 2 + 1] = baselineColumns[i];
      }
      baselineValues[i] = (long)(transfer ? i : i + 5);
    }
    jsonRow.append("}");
    StringBuilder batchString = new StringBuilder("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append(jsonRow + ((i == numRows - 1) ? "" : ","));
    }
    batchString.append("]");
    List<String> inputJsonBatches = Lists.newArrayList();
    inputJsonBatches.add(batchString.toString());

    List<String> expectedJsonBatches = Lists.newArrayList();
    expectedJsonBatches.add(batchString.toString());

    Project projectConf = new Project(parseExprs(expr), null);
    mockOpContext(projectConf, initReservation, maxAllocation);

    long totalSize = getExpectedSize(expectedJsonBatches);

    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);


    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(projectConf)
            .inputDataStreamJson(inputJsonBatches)
            .baselineColumns(baselineColumns)
            .expectedNumBatches(2)  // verify number of batches
            .expectedBatchSize(totalSize / 2); // verify batch size.

    for (int i = 0; i < numRows; i++) {
      opTestBuilder.baselineValues(baselineValues);
    }
    opTestBuilder.go();
  }

  @Test
  public void testProjectVariableWidthTransfer() throws Exception {
    testProjectVariableWidthImpl(true, 50, "ABCDEFGHIJ");
  }

  @Test
  public void testProjectVariableWidthNewColumn() throws Exception {
    testProjectVariableWidthImpl(false, 50, "ABCDEFGHIJ");
  }

  @Test
  public void testProjectZeroWidth() throws Exception {
    testProjectVariableWidthImpl(true, 50, "");
  }


  public void testProjectVariableWidthImpl(boolean transfer, int columnCount, String testString) throws Exception {

    StringBuilder jsonRow = new StringBuilder("{");
    String[] baselineColumns = new String [columnCount];
    Object[] baselineValues = new String[columnCount];
    int exprSize = (transfer ? 2 : 2 * columnCount);
    String[] expr = new String[exprSize];

    // Expr for a 'select *' as expected by parseExprs()
    if (transfer) {
      expr[0] = "`**`";
      expr[1] = "`**`";
    }

    for (int i = 0; i < columnCount; i++) {
      jsonRow.append("\"" + "C" + i + "\": " + "\"" + testString + "\"" + ((i == columnCount - 1) ? "" : ","));
      baselineColumns[i] = "C" + i;
      if (!transfer) {
        expr[i * 2] = "lower(" + baselineColumns[i] + ")";
        expr[i * 2 + 1] = baselineColumns[i];
      }
      baselineValues[i] = (transfer ? testString : StringUtils.lowerCase(testString));
    }
    jsonRow.append("}");
    StringBuilder batchString = new StringBuilder("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append(jsonRow + ((i == numRows - 1) ? "" : ","));
    }
    batchString.append("]");
    List<String> inputJsonBatches = Lists.newArrayList();
    inputJsonBatches.add(batchString.toString());

    List<String> expectedJsonBatches = Lists.newArrayList();
    expectedJsonBatches.add(batchString.toString());

    Project projectConf = new Project(parseExprs(expr), null);
    mockOpContext(projectConf, initReservation, maxAllocation);

    long totalSize = getExpectedSize(expectedJsonBatches);

    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(projectConf)
            .inputDataStreamJson(inputJsonBatches)
            .baselineColumns(baselineColumns)
            .expectedNumBatches(2)  // verify number of batches
            .expectedBatchSize(totalSize / 2); // verify batch size.

    for (int i = 0; i < numRows; i++) {
      opTestBuilder.baselineValues(baselineValues);
    }
    opTestBuilder.go();
  }

  /**
   * Test expression with transfer and new columns
   * @throws Exception
   */
  @Test
  public void testProjectVariableWidthMixed() throws Exception {
    String testString = "ABCDEFGHIJ";
    StringBuilder jsonRow = new StringBuilder("{");
    // 50 new columns and 1 transfer
    final int colCount = 50 + 1;
    String[] baselineColumns = new String [colCount];
    Object[] baselineValues = new String[colCount];
    int exprSize = 2 * colCount;
    String[] expr = new String[exprSize];

    // columns C1 ... C50
    for (int i = 1; i < colCount; i++) {
      jsonRow.append("\"" + "C" + i + "\": " + "\"" + testString + "\"" + ((i == colCount - 1) ? "" : ","));
      baselineColumns[i] = "C" + i;
      // New columns lower(C1) as C1, ... lower(C50) as C50
      expr[i * 2] = "lower(" + baselineColumns[i] + ")";
      expr[i * 2 + 1] = baselineColumns[i];

      baselineValues[i] = StringUtils.lowerCase(testString);
    }


    //Transfer: C1 as COL1TR
    expr[0] = "C1";
    expr[1] = "COL1TR";
    baselineColumns[0] = "COL1TR";
    baselineValues[0] = testString;
    String expectedJsonRow = jsonRow.toString() + ", \"COL1TR\": \"" + testString + "\"}";
    jsonRow.append("}");

    StringBuilder batchString = new StringBuilder("[");
    StringBuilder expectedString = new StringBuilder("[");

    for (int i = 0; i < numRows; i++) {
      batchString.append(jsonRow + ((i == numRows - 1) ? "" : ","));
      expectedString.append(expectedJsonRow + ((i == numRows - 1) ? "" : ","));
    }
    batchString.append("]");
    expectedString.append("]");

    List<String> inputJsonBatches = Lists.newArrayList();
    inputJsonBatches.add(batchString.toString());

    List<String> expectedJsonBatches = Lists.newArrayList();
    expectedJsonBatches.add(expectedString.toString());

    Project projectConf = new Project(parseExprs(expr), null);
    mockOpContext(projectConf, initReservation, maxAllocation);

    long totalSize = getExpectedSize(expectedJsonBatches);

    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(projectConf)
            .inputDataStreamJson(inputJsonBatches)
            .baselineColumns(baselineColumns)
            .expectedNumBatches(2)  // verify number of batches
            .expectedBatchSize(totalSize / 2); // verify batch size.

    for (int i = 0; i < numRows; i++) {
      opTestBuilder.baselineValues(baselineValues);
    }
    opTestBuilder.go();
  }



  @Test
  public void testFlattenFixedWidth() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    // create input rows like this.
    // "a" : 5, "b" : wideString, "c" : [6,7,8,9]
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [6, 7, 8, 9]},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [6, 7, 8, 9]}");
    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 5, "b" : wideString, "c" : 6
    // "a" : 5, "b" : wideString, "c" : 7
    // "a" : 5, "b" : wideString, "c" : 8
    // "a" : 5, "b" : wideString, "c" : 9
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 6},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 7},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 8},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 9},");
    }
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 6},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 7},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 8},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : 9}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2)  // verify number of batches
      .expectedBatchSize(totalSize / 2); // verify batch size.

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, 6l);
      opTestBuilder.baselineValues(5l, wideString, 7l);
      opTestBuilder.baselineValues(5l, wideString, 8l);
      opTestBuilder.baselineValues(5l, wideString, 9l);
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenVariableWidth() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    // create input rows like this.
    // "a" : 5, "b" : wideString, "c" : ["parrot", "hummingbird", "owl", "woodpecker", "peacock"]
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\",\"c\" : [\"parrot\", \"hummingbird\", \"owl\", \"woodpecker\", \"peacock\"]},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\",\"c\" : [\"parrot\", \"hummingbird\", \"owl\", \"woodpecker\", \"peacock\"]}");
    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 5, "b" : wideString, "c" : parrot
    // "a" : 5, "b" : wideString, "c" : hummingbird
    // "a" : 5, "b" : wideString, "c" : owl
    // "a" : 5, "b" : wideString, "c" : woodpecker
    // "a" : 5, "b" : wideString, "c" : peacock
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"parrot\"},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"hummingbird\"},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"owl\"},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"woodpecker\"},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"peacock\"},");
    }
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"parrot\"},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"hummingbird\"},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"owl\"},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"woodpecker\"},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"peacock\"}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize / 2); // verify batch size.

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, "parrot");
      opTestBuilder.baselineValues(5l, wideString, "hummingbird");
      opTestBuilder.baselineValues(5l, wideString, "owl");
      opTestBuilder.baselineValues(5l, wideString, "woodpecker");
      opTestBuilder.baselineValues(5l, wideString, "peacock");
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenFixedWidthList() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    // create input rows like this.
    // "a" : 5, "b" : wideString, "c" : [[1,2,3,4], [5,6,7,8]]
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [" + "[1,2,3,4]," + "[5,6,7,8]" + "]");
      batchString.append("},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [" + "[1,2,3,4]," + "[5,6,7,8]" + "]");
    batchString.append("}]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 5, "b" : wideString, "c" : [1,2,3,4]
    // "a" : 5, "b" : wideString, "c" : [5,6,7,8]
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"[1,2,3,4]\"},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"[5,6,7,8]\"},");
    }

    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"[1,2,3,4]\"},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : \"[5,6,7,8]\"}");

    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);
    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize);  // verify batch size.

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, new ArrayList<Long>(Arrays.asList(1L, 2L, 3L, 4L)));
      opTestBuilder.baselineValues(5l, wideString, new ArrayList<Long>(Arrays.asList(5L, 6L, 7L, 8L)));
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenVariableWidthList() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    // create input rows like this.
    // "a" : 5, "b" : wideString, "c" : [["parrot", "hummingbird", "owl", "woodpecker"], ["hawk", "nightingale", "swallow", "peacock"]]
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," +
        "\"c\" : [" + "[\"parrot\", \"hummingbird\", \"owl\", \"woodpecker\"]," + "[\"hawk\",\"nightingale\",\"swallow\",\"peacock\"]" + "]");
      batchString.append("},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," +
      "\"c\" : [" + "[\"parrot\", \"hummingbird\", \"owl\", \"woodpecker\"]," + "[\"hawk\",\"nightingale\",\"swallow\",\"peacock\"]" + "]");
    batchString.append("}]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 5, "b" : wideString, "c" : ["parrot", "hummingbird", "owl", "woodpecker"]
    // "a" : 5, "b" : wideString, "c" : ["hawk", "nightingale", "swallow", "peacock"]
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();

    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [\"parrot\", \"hummingbird\", \"owl\", \"woodpecker\"]},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [\"hawk\", \"nightingale\", \"swallow\", \"peacock\"]},");
    }
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [\"parrot\", \"hummingbird\", \"owl\", \"woodpecker\"]},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [\"hawk\", \"nightingale\", \"swallow\", \"peacock\"]}");

    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);
    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize);  // verify batch size.

    final JsonStringArrayList<Text> birds1 = new JsonStringArrayList<Text>() {{
      add(new Text("parrot"));
      add(new Text("hummingbird"));
      add(new Text("owl"));
      add(new Text("woodpecker"));
    }};

    final JsonStringArrayList<Text> birds2 = new JsonStringArrayList<Text>() {{
      add(new Text("hawk"));
      add(new Text("nightingale"));
      add(new Text("swallow"));
      add(new Text("peacock"));
    }};

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, birds1);
      opTestBuilder.baselineValues(5l, wideString, birds2);
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenMap() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    // create input rows like this.
    // "a" : 5, "b" : wideString, "c" : [{"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}]

    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," +
        "\"c\" : [" + " { \"trans_id\":\"t1\", \"amount\":100, " +
        "\"trans_time\":7777777, \"type\":\"sports\"}," +
        " { \"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}");
      batchString.append("]},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," +
      "\"c\" : [" + " { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777," +
      " \"type\":\"sports\"}," +
      " { \"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}");
    batchString.append("]}]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 5, "b" : wideString, "c" : {"trans_id":"t1", amount:100, trans_time:7777777, type:sports}
    // "a" : 5, "b" : wideString, "c" : {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();

    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
        "{\"trans_id\":\"t1\", \"amount\":100, " +
        "\"trans_time\":7777777, \"type\":\"sports\"}},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
        "{\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}},");
    }

    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
      "{\"trans_id\":\"t1\", \"amount\":100, " +
      "\"trans_time\":7777777, \"type\":\"sports\"}},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
      "{\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}}");

    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);
    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize / 2);  // verify batch size.

    JsonStringHashMap<String, Object> resultExpected1 = new JsonStringHashMap<>();
    resultExpected1.put("trans_id", new Text("t1"));
    resultExpected1.put("amount", new Long(100));
    resultExpected1.put("trans_time", new Long(7777777));
    resultExpected1.put("type", new Text("sports"));

    JsonStringHashMap<String, Object> resultExpected2 = new JsonStringHashMap<>();
    resultExpected2.put("trans_id", new Text("t2"));
    resultExpected2.put("amount", new Long(1000));
    resultExpected2.put("trans_time", new Long(8888888));
    resultExpected2.put("type", new Text("groceries"));

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, resultExpected1);
      opTestBuilder.baselineValues(5l, wideString, resultExpected2);
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenListOfMaps() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    // create input rows like this.
    // "a" : 5, "b" : wideString,
    // "c" : [ [{"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}],
    //         [{"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}],
    //         [{"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}] ]

    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [" +
        "[ { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
        "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ], " +
        "[ { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
        "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ], " +
        "[ { \"trans_id\":\"t1\", \"amount\":100, " +
        "\"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
        "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ]");
      batchString.append("]},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [" +
      "[ { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
      "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ], " +
      "[ { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
      "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ], " +
      "[ { \"trans_id\":\"t1\", \"amount\":100, " +
      "\"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
      "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ]");
    batchString.append("]}]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 5, "b" : wideString, "c" : [{"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}]
    // "a" : 5, "b" : wideString, "c" : [{"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, {"trans_id":"t1", amount:100, trans_time:8888888, type:groceries}]
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();

    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
        "[ { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
        "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ]},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
        "[ { \"trans_id\":\"t1\", \"amount\":100, " +
        "\"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
        "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}]},");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
        "[ { \"trans_id\":\"t1\", \"amount\":100, " +
        "\"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
        "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}]},");
    }

    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
      "[ { \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
      "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"} ]},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
      "[ { \"trans_id\":\"t1\", \"amount\":100, " +
      "\"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
      "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}]},");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
      "[ { \"trans_id\":\"t1\", \"amount\":100, " +
      "\"trans_time\":7777777, \"type\":\"sports\"}," + " { " +
      "\"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}]}");

    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);
    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize / 2);  // verify batch size.

    final JsonStringHashMap<String, Object> resultExpected1 = new JsonStringHashMap<>();
    resultExpected1.put("trans_id", new Text("t1"));
    resultExpected1.put("amount", new Long(100));
    resultExpected1.put("trans_time", new Long(7777777));
    resultExpected1.put("type", new Text("sports"));

    final JsonStringHashMap<String, Object> resultExpected2 = new JsonStringHashMap<>();
    resultExpected2.put("trans_id", new Text("t2"));
    resultExpected2.put("amount", new Long(1000));
    resultExpected2.put("trans_time", new Long(8888888));
    resultExpected2.put("type", new Text("groceries"));

    final JsonStringArrayList<JsonStringHashMap<String, Object>> results = new JsonStringArrayList<JsonStringHashMap<String, Object>>() {{
      add(resultExpected1);
      add(resultExpected2);
    }};

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, results);
      opTestBuilder.baselineValues(5l, wideString, results);
      opTestBuilder.baselineValues(5l, wideString, results);
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenNestedMap() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    // create input rows like this.
    // "a" : 5, "b" : wideString,
    // "c" : [ {innerMap: {"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, "trans_id":"t1", amount:100, trans_time:8888888, type:groceries},
    //         {innerMap: {"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, "trans_id":"t1", amount:100, trans_time:8888888, type:groceries} ]

    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    StringBuilder innerMap = new StringBuilder();
    innerMap.append("{ \"trans_id\":\"inner_trans_t1\", \"amount\":100, \"trans_time\":7777777, \"type\":\"sports\"}");

    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [" +
        " { \"innerMap\": " + innerMap + ", \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, " +
        "\"type\":\"sports\"}," + " { \"innerMap\": " + innerMap +
        ", \"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}]");
      batchString.append("},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : [" +
      " { \"innerMap\": " + innerMap + ", \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, " +
      "\"type\":\"sports\"}," + " { \"innerMap\": " + innerMap + ",  \"trans_id\":\"t2\", \"amount\":1000, \"trans_time\":8888888, \"type\":\"groceries\"}");
    batchString.append("]}]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 5, "b" : wideString, "c" : {innerMap: {"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, "trans_id":"t1", amount:100, trans_time:8888888, type:groceries}
    // "a" : 5, "b" : wideString, "c" : {innerMap: {"trans_id":"t1", amount:100, trans_time:7777777, type:sports}, "trans_id":"t1", amount:100, trans_time:8888888, type:groceries}
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();

    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
        " { \"innerMap\": " + innerMap + ", \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, " +
        "\"type\":\"sports\"} }, ");
      expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
        " { \"innerMap\": " + innerMap + ", \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, " +
        "\"type\":\"sports\"} }, ");
    }

    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
      " { \"innerMap\": " + innerMap + ", \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, " +
      "\"type\":\"sports\"} }, ");
    expectedBatchString.append("{\"a\": 5, " + "\"b\" : " + "\"" + wideString + "\"," + "\"c\" : " +
      " { \"innerMap\": " + innerMap + ", \"trans_id\":\"t1\", \"amount\":100, \"trans_time\":7777777, " +
      "\"type\":\"sports\"} }");

    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);
    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize / 2);  // verify batch size.

    JsonStringHashMap<String, Object> innerMapResult = new JsonStringHashMap<>();
    innerMapResult.put("trans_id", new Text("inner_trans_t1"));
    innerMapResult.put("amount", new Long(100));
    innerMapResult.put("trans_time", new Long(7777777));
    innerMapResult.put("type", new Text("sports"));

    JsonStringHashMap<String, Object> resultExpected1 = new JsonStringHashMap<>();
    resultExpected1.put("trans_id", new Text("t1"));
    resultExpected1.put("amount", new Long(100));
    resultExpected1.put("trans_time", new Long(7777777));
    resultExpected1.put("type", new Text("sports"));
    resultExpected1.put("innerMap", innerMapResult);

    JsonStringHashMap<String, Object> resultExpected2 = new JsonStringHashMap<>();
    resultExpected2.put("trans_id", new Text("t2"));
    resultExpected2.put("amount", new Long(1000));
    resultExpected2.put("trans_time", new Long(8888888));
    resultExpected2.put("type", new Text("groceries"));
    resultExpected2.put("innerMap", innerMapResult);

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, resultExpected1);
      opTestBuilder.baselineValues(5l, wideString, resultExpected2);
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenUpperLimit() throws Exception {
    // test the upper limit of 65535 records per batch.
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    StringBuilder flattenElement = new StringBuilder();

    // Create list of 1000 elements
    flattenElement.append("[");
    for (int i = 0; i < 1000; i++) {
      flattenElement.append(i);
      flattenElement.append(",");
    }
    flattenElement.append(1000);
    flattenElement.append("]");

    batchString.append("[");

    numRows = 100;

    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, "  + "\"c\":" + flattenElement + "},");
    }
    batchString.append("{\"a\": 5, " + "\"c\":" + flattenElement + "}");
    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();

    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < 1000; j++) {
        expectedBatchString.append("{\"a\": 5, "  + "\"c\" :");
        expectedBatchString.append(j);
        expectedBatchString.append("},");
      }
    }
    for (int j = 0; j < 999; j++) {
      expectedBatchString.append("{\"a\": 5, "  + "\"c\" :");
      expectedBatchString.append(j);
      expectedBatchString.append("},");
    }

    expectedBatchString.append("{\"a\": 5, "  + "\"c\" :");
    expectedBatchString.append(1000);
    expectedBatchString.append("}");

    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);
    // set the output batch size to 1/2 of total size expected.
    // We will get 16 batches because of upper bound of 65535 rows.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    // Here we expect 16 batches because each batch will be limited by upper limit of 65535 records.
    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize / 2);  // verify batch size.

    for (long i = 0; i < numRows + 1; i++) {
      for (long j = 0; j < 1001; j++) {
        opTestBuilder.baselineValues(5l, j);
      }
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenLowerLimit() throws Exception {
    // test the lower limit of at least one batch
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    StringBuilder flattenElement = new StringBuilder();

    // Create list of 10 elements
    flattenElement.append("[");
    for (int i = 0; i < 10; i++) {
      flattenElement.append(i);
      flattenElement.append(",");
    }
    flattenElement.append(10);
    flattenElement.append("]");

    // create list of wideStrings
    final StringBuilder wideStrings = new StringBuilder();
    wideStrings.append("[");
    for (int i = 0; i < 10; i++) {
      wideStrings.append("\"" + wideString + "\",");
    }
    wideStrings.append("\"" + wideString + "\"");
    wideStrings.append("]");

    batchString.append("[");
    batchString.append("{\"a\": " + wideStrings + "," + "\"c\":" + flattenElement);
    batchString.append("}]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // set very low value of batch size for a large record size.
    // This is to test we atleast get one record per batch.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", 1024);

    // Here we expect 10 batches because each batch will be bounded by lower limit of at least 1 record.
    // do not check the output batch size as it will be more than configured value of 1024, so we get
    // at least one record out.
    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "c")
      .expectedNumBatches(10); // verify number of batches

    final JsonStringArrayList<Text> results = new JsonStringArrayList<Text>() {{
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
      add(new Text(wideString));
    }};

    for (long j = 0; j < 11; j++) {
      opTestBuilder.baselineValues(results, j);
    }

    opTestBuilder.go();
  }

  @Test
  public void testFlattenEmptyList() throws Exception {
    final PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("b"));

    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    StringBuilder flattenElement = new StringBuilder();

    flattenElement.append("[");
    flattenElement.append("]");

    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": 5, " + "\"b\" : " + flattenElement + "},");
    }
    batchString.append("{\"a\": 5, " + "\"b\" : " + flattenElement + "}");
    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .expectZeroRows();

    opTestBuilder.go();
  }

  @Test
  public void testFlattenLargeRecords() throws Exception {
    PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("c"));
    mockOpContext(flatten, initReservation, maxAllocation);

    // create input rows like this.
    // "a" : <id1>, "b" : wideString, "c" : [ 10 wideStrings ]
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    int arrayLength = 10;
    StringBuilder test = new StringBuilder();
    test.append("[ \"");
    for (int i = 0; i < arrayLength; i++) {
      test.append(wideString);
      test.append("\",\"");
    }
    test.append(wideString);
    test.append("\"]");

    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{" + "\"a\" :" + (new StringBuilder().append(i)) + ",\"b\": \"" + wideString + "\"," +
        "\"c\": " + test + "},");
    }
    batchString.append("{" + "\"a\" :" + (new StringBuilder().append(numRows)) + ",\"b\": \"" + wideString + "\"," +
      "\"c\": " + test + "}");
    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // output rows will be like this.
    // "a" : <id1>, "b" : wideString, "c" : wideString

    // Figure out what will be approximate total output size out of flatten for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int k = 0; k < (numRows) * 11; k++) {
      expectedBatchString.append("{" + "\"a\" :" + (new StringBuilder().append(k)) + ",\"b\": \"" + wideString + "\",");
      expectedBatchString.append("\"c\": \"" + wideString + "\"},");
    }
    expectedBatchString.append("{" + "\"a\" :" + (new StringBuilder().append(numRows)) + ",\"b\": \"" + wideString + "\",");
    expectedBatchString.append("\"c\": \"" + wideString + "\"}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);
    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(flatten)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b", "c")
      .expectedNumBatches(2) // verify number of batches
      .expectedBatchSize(totalSize / 2);  // verify batch size.

    for (long k = 0; k < ((numRows + 1)); k++) {
      for (int j = 0; j < arrayLength + 1; j++) {
        opTestBuilder.baselineValues(k, wideString, wideString);
      }
    }

    opTestBuilder.go();
  }

  @Test
  public void testMergeJoinMultipleOutputBatches() throws Exception {
    MergeJoinPOP mergeJoin = new MergeJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.INNER);
    mockOpContext(mergeJoin, initReservation, maxAllocation);

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately 4 batches because of fragmentation factor of 2 accounted for in merge join.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize/2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(mergeJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(4)  // verify number of batches
      .expectedBatchSize(totalSize / 2) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testMergeJoinSingleOutputBatch() throws Exception {
    MergeJoinPOP mergeJoin = new MergeJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.INNER);
    mockOpContext(mergeJoin, initReservation, maxAllocation);

    // create multiple batches from both sides.
    numRows = 4096 * 2;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to twice of total size expected.
    // We should get 1 batch.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize*2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(mergeJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(1)  // verify number of batches
      .expectedBatchSize(totalSize) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testMergeJoinUpperLimit() throws Exception {
    // test the upper limit of 65535 records per batch.
    MergeJoinPOP mergeJoin = new MergeJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.LEFT);
    mockOpContext(mergeJoin, initReservation, maxAllocation);

    numRows = 100000;

    // create left input rows like this.
    // "a1" : 5,  "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5,  "c1" : 1, "a2":6,  "c2": 1
    // "a1" : 5,  "c1" : 2, "a2":6,  "c2": 2
    // "a1" : 5,  "c1" : 3, "a2":6,  "c2": 3

    // expect two batches, batch limited by 65535 records
    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(mergeJoin)
      .baselineColumns("a1", "c1", "a2", "c2")
      .expectedNumBatches(2)  // verify number of batches
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, i, 6l, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testMergeJoinLowerLimit() throws Exception {
    // test the lower limit of at least one batch
    MergeJoinPOP mergeJoin = new MergeJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.RIGHT);
    mockOpContext(mergeJoin, initReservation, maxAllocation);

    numRows = 10;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3

    // set very low value of output batch size so we can do only one row per batch.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", 128);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(mergeJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(10)  // verify number of batches
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testUnionOutputBatch() throws Exception {
    UnionAll unionAll = new UnionAll(Collections.<PhysicalOperator> emptyList());
    mockOpContext(unionAll, initReservation, maxAllocation);

    // create  batches from both sides.
    numRows = 4000;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    rightBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1
    // "a1" : 5, "b1" : wideString, "c1" : 2
    // "a1" : 5, "b1" : wideString, "c1" : 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to twice of total size expected.
    // We should get 2 batches, one for the left and one for the right.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize*2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(unionAll)
      .baselineColumns("a1", "b1", "c1")
      .expectedNumBatches(2)  // verify number of batches
      .expectedBatchSize(totalSize) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i);
    }

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testUnionMultipleOutputBatches() throws Exception {
    UnionAll unionAll = new UnionAll(Collections.<PhysicalOperator> emptyList());
    mockOpContext(unionAll, initReservation, maxAllocation);

    // create multiple batches from both sides.
    numRows = 8000;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    rightBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1
    // "a1" : 5, "b1" : wideString, "c1" : 2
    // "a1" : 5, "b1" : wideString, "c1" : 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows*2; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to half of total size expected.
    // We should get 4 batches, 2 for the left and 2 for the right.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize/2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(unionAll)
      .baselineColumns("a1", "b1", "c1")
      .expectedNumBatches(4)  // verify number of batches
      .expectedBatchSize(totalSize) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i);
    }

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testUnionLowerLimit() throws Exception {
    UnionAll unionAll = new UnionAll(Collections.<PhysicalOperator> emptyList());
    mockOpContext(unionAll, initReservation, maxAllocation);

    // create multiple batches from both sides.
    numRows = 10;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();

    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    rightBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1
    // "a1" : 5, "b1" : wideString, "c1" : 2
    // "a1" : 5, "b1" : wideString, "c1" : 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows*2; i++) {
      expectedBatchString.append("{\"a1\": 5, " +  "\"c1\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"c1\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size very low so we get only one row per batch.
    // We should get 22 batches for 22 rows.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", 128);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(unionAll)
      .baselineColumns("a1","b1", "c1")
      .expectedNumBatches(22)  // verify number of batches
      .expectedBatchSize(totalSize) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i);
    }

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testHashJoinMultipleOutputBatches() throws Exception {
    HashJoinPOP hashJoin = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.INNER);
    mockOpContext(hashJoin, initReservation, maxAllocation);

    numRows = 4000 * 2;
    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately 4 batches because of fragmentation factor of 2 accounted for in merge join.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize/2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(4)  // verify number of batches
      .expectedBatchSize(totalSize / 2) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows+1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testHashJoinSingleOutputBatch() throws Exception {
    HashJoinPOP hashJoin = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.INNER);
    mockOpContext(hashJoin, initReservation, maxAllocation);

    // create multiple batches from both sides.
    numRows = 4096 * 2;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to twice of total size expected.
    // We should get 1 batch.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize*2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(1)  // verify number of batches
      .expectedBatchSize(totalSize) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testHashJoinUpperLimit() throws Exception {
    // test the upper limit of 65535 records per batch.
    HashJoinPOP hashJoin = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.INNER);
    mockOpContext(hashJoin, initReservation, maxAllocation);

    numRows = 100000;

    // create left input rows like this.
    // "a1" : 5,  "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5,  "c1" : 1, "a2":6,  "c2": 1
    // "a1" : 5,  "c1" : 2, "a2":6,  "c2": 2
    // "a1" : 5,  "c1" : 3, "a2":6,  "c2": 3

    // expect two batches, batch limited by 65535 records
    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashJoin)
      .baselineColumns("a1", "c1", "a2", "c2")
      .expectedNumBatches(2)  // verify number of batches
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, i, 6l, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testHashJoinLowerLimit() throws Exception {
    // test the lower limit of at least one batch
    HashJoinPOP hashJoin = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.INNER);
    mockOpContext(hashJoin, initReservation, maxAllocation);

    numRows = 10;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3

    // set very low value of output batch size so we can do only one row per batch.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", 128);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(10)  // verify number of batches
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testRightOuterHashJoin() throws Exception {

    HashJoinPOP hashJoin = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.RIGHT);
    mockOpContext(hashJoin, initReservation, maxAllocation);

    numRows = 4000 * 2;
    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately 4 batches because of fragmentation factor of 2 accounted for in merge join.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize/2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(4)  // verify number of batches
      .expectedBatchSize(totalSize / 2) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testLeftOuterHashJoin() throws Exception {

    HashJoinPOP hashJoin = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("c1", "EQUALS", "c2")), JoinRelType.LEFT);
    mockOpContext(hashJoin, initReservation, maxAllocation);

    numRows = 4000 * 2;
    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately 4 batches because of fragmentation factor of 2 accounted for in merge join.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize/2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashJoin)
      .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
      .expectedNumBatches(4)  // verify number of batches
      .expectedBatchSize(totalSize / 2) // verify batch size
      .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows+1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();

  }

  @Test
  public void testSimpleHashAgg() {
    HashAggregate aggConf = new HashAggregate(null, AggPrelBase.OperatorPhase.PHASE_1of1, parseExprs("a", "a"), parseExprs("sum(b)", "b_sum"), 1.0f);
    List<String> inputJsonBatches = Lists.newArrayList(
       "[{\"a\": 5, \"b\" : 1 }]",
         "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");

    legacyOpTestBuilder()
      .physicalOperator(aggConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("b_sum", "a")
      .baselineValues(6l, 5l)
      .baselineValues(8l, 3l)
      .go();
  }

  @Test
  public void testHashAggSum() throws ExecutionSetupException {
    HashAggregate hashAgg = new HashAggregate(null, AggPrelBase.OperatorPhase.PHASE_1of1, parseExprs("a", "a"), parseExprs("sum(b)", "b_sum"), 1.0f);

    // create input rows like this.
    // "a" : 1, "b" : 1
    // "a" : 1, "b" : 1
    // "a" : 1, "b" : 1
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
        batchString.append("{\"a\": " + i + ", \"b\": " + i + "},");
        batchString.append("{\"a\": " + i + ", \"b\": " + i + "},");
        batchString.append("{\"a\": " + i + ", \"b\": " + i + "},");
    }
    batchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}," );
    batchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}," );
    batchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}" );

    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of hash agg for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 1, "b" : 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");

    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": " + i + ", \"b\": " + (3*i) + "},");
    }
    expectedBatchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}" );
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashAgg)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b_sum")
      .expectedNumBatches(4)  // verify number of batches
      .expectedBatchSize(totalSize/2); // verify batch size.


    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues((long)i, (long)3*i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testHashAggAvg() throws ExecutionSetupException {
    HashAggregate hashAgg = new HashAggregate(null, AggPrelBase.OperatorPhase.PHASE_1of1, parseExprs("a", "a"), parseExprs("avg(b)", "b_avg"), 1.0f);

    // create input rows like this.
    // "a" : 1, "b" : 1
    // "a" : 1, "b" : 1
    // "a" : 1, "b" : 1
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": " + i + ", \"b\": " + i + "},");
      batchString.append("{\"a\": " + i + ", \"b\": " + i + "},");
      batchString.append("{\"a\": " + i + ", \"b\": " + i + "},");
    }
    batchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}," );
    batchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}," );
    batchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}" );

    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of hash agg for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 1, "b" : 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");

    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": " + i + ", \"b\": " + (3*i) + "},");
    }
    expectedBatchString.append("{\"a\": " + numRows + ", \"b\": " + numRows + "}" );
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashAgg)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b_avg")
      .expectedNumBatches(4)  // verify number of batches
      .expectedBatchSize(totalSize/2); // verify batch size.

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues((long)i, (double)i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testHashAggMax() throws ExecutionSetupException {
    HashAggregate hashAgg = new HashAggregate(null, AggPrelBase.OperatorPhase.PHASE_1of1, parseExprs("a", "a"), parseExprs("max(b)", "b_max"), 1.0f);

    // create input rows like this.
    // "a" : 1, "b" : "a"
    // "a" : 2, "b" : "aa"
    // "a" : 3, "b" : "aaa"
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"a\": " + i + ", \"b\": " + "\"a\"" + "},");
      batchString.append("{\"a\": " + i + ", \"b\": " + "\"aa\"" + "},");
      batchString.append("{\"a\": " + i + ", \"b\": " + "\"aaa\"" + "},");
    }
    batchString.append("{\"a\": " + numRows + ", \"b\": " + "\"a\"" + "}," );
    batchString.append("{\"a\": " + numRows + ", \"b\": " + "\"aa\"" + "}," );
    batchString.append("{\"a\": " + numRows + ", \"b\": " + "\"aaa\"" + "}" );

    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Figure out what will be approximate total output size out of hash agg for input above
    // We will use this sizing information to set output batch size so we can produce desired
    // number of batches that can be verified.

    // output rows will be like this.
    // "a" : 1, "b" : "aaa"
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");

    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a\": " + i + ", \"b\": " + "\"aaa\"" + "},");
    }
    expectedBatchString.append("{\"a\": " + numRows + ", \"b\": " + "\"aaa\"" + "}" );
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately get 2 batches and max of 4.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize / 2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
      .physicalOperator(hashAgg)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b_max")
      .expectedNumBatches(2)  // verify number of batches
      .expectedBatchSize(totalSize); // verify batch size.

    for (int i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues((long)i, "aaa");
    }

    opTestBuilder.go();
  }

  @Test
  public void testNestedLoopJoinMultipleOutputBatches() throws Exception {
    LogicalExpression functionCallExpr = new FunctionCall("equal",
            ImmutableList.of((LogicalExpression) new FieldReference("c1", ExpressionPosition.UNKNOWN),
                    (LogicalExpression) new FieldReference("c2", ExpressionPosition.UNKNOWN)),
            ExpressionPosition.UNKNOWN);

    NestedLoopJoinPOP nestedLoopJoin = new NestedLoopJoinPOP(null, null, JoinRelType.INNER, functionCallExpr);
    mockOpContext(nestedLoopJoin, initReservation, maxAllocation);

    numRows = 4000 * 2;
    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately 4 batches.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize/2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(nestedLoopJoin)
            .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
            .expectedNumBatches(4)  // verify number of batches
            .expectedBatchSize(totalSize / 2) // verify batch size
            .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows+1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();

  }

  @Test
  public void testNestedLoopJoinSingleOutputBatch() throws Exception {
    LogicalExpression functionCallExpr = new FunctionCall("equal",
            ImmutableList.of((LogicalExpression) new FieldReference("c1", ExpressionPosition.UNKNOWN),
                    (LogicalExpression) new FieldReference("c2", ExpressionPosition.UNKNOWN)),
            ExpressionPosition.UNKNOWN);

    NestedLoopJoinPOP nestedLoopJoin = new NestedLoopJoinPOP(null, null, JoinRelType.INNER, functionCallExpr);

    // create multiple batches from both sides.
    numRows = 4096 * 2;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to twice of total size expected.
    // We should get 1 batch.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize*2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(nestedLoopJoin)
            .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
            .expectedNumBatches(1)  // verify number of batches
            .expectedBatchSize(totalSize) // verify batch size
            .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testNestedLoopJoinUpperLimit() throws Exception {
    // test the upper limit of 65535 records per batch.
    LogicalExpression functionCallExpr = new FunctionCall("<",
            ImmutableList.of((LogicalExpression) new FieldReference("c1", ExpressionPosition.UNKNOWN),
                    (LogicalExpression) new FieldReference("c2", ExpressionPosition.UNKNOWN)),
            ExpressionPosition.UNKNOWN);

    NestedLoopJoinPOP nestedLoopJoin = new NestedLoopJoinPOP(null, null, JoinRelType.INNER, functionCallExpr);

    numRows = 500;

    // create left input rows like this.
    // "a1" : 5,  "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5,  "c1" : 1, "a2":6,  "c2": 1
    // "a1" : 5,  "c1" : 2, "a2":6,  "c2": 2
    // "a1" : 5,  "c1" : 3, "a2":6,  "c2": 3

    // we expect n(n+1)/2 number of records i.e. (500 * 501)/2 = 125250
    // expect two batches, batch limited by 65535 records
    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(nestedLoopJoin)
            .baselineColumns("a1", "c1", "a2", "c2")
            .expectedNumBatches(2)  // verify number of batches
            .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows+1; i++) {
      for (long j = i+1; j < numRows+1; j++) {
        opTestBuilder.baselineValues(5l, i, 6l, j);
      }
    }

    opTestBuilder.go();
  }

  @Test
  public void testNestedLoopJoinLowerLimit() throws Exception {
    // test the lower limit of at least one batch
    LogicalExpression functionCallExpr = new FunctionCall("equal",
            ImmutableList.of((LogicalExpression) new FieldReference("c1", ExpressionPosition.UNKNOWN),
                    (LogicalExpression) new FieldReference("c2", ExpressionPosition.UNKNOWN)),
            ExpressionPosition.UNKNOWN);

    NestedLoopJoinPOP nestedLoopJoin = new NestedLoopJoinPOP(null, null, JoinRelType.INNER, functionCallExpr);

    numRows = 10;

    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3

    // set very low value of output batch size so we can do only one row per batch.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", 128);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(nestedLoopJoin)
            .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
            .expectedNumBatches(10)  // verify number of batches
            .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows + 1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();
  }

  @Test
  public void testLeftNestedLoopJoin() throws Exception {
    LogicalExpression functionCallExpr = new FunctionCall("equal",
            ImmutableList.of((LogicalExpression) new FieldReference("c1", ExpressionPosition.UNKNOWN),
                    (LogicalExpression) new FieldReference("c2", ExpressionPosition.UNKNOWN)),
            ExpressionPosition.UNKNOWN);

    NestedLoopJoinPOP nestedLoopJoin = new NestedLoopJoinPOP(null, null, JoinRelType.LEFT, functionCallExpr);

    numRows = 4000 * 2;
    // create left input rows like this.
    // "a1" : 5, "b1" : wideString, "c1" : <id>
    List<String> leftJsonBatches = Lists.newArrayList();
    StringBuilder leftBatchString = new StringBuilder();
    leftBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i + "},");
    }
    leftBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows + "}");
    leftBatchString.append("]");

    leftJsonBatches.add(leftBatchString.toString());

    // create right input rows like this.
    // "a2" : 6, "b2" : wideString, "c2" : <id>
    List<String> rightJsonBatches = Lists.newArrayList();
    StringBuilder rightBatchString = new StringBuilder();
    rightBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    rightBatchString.append("{\"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    rightBatchString.append("]");
    rightJsonBatches.add(rightBatchString.toString());

    // output rows will be like this.
    // "a1" : 5, "b1" : wideString, "c1" : 1, "a2":6, "b2" : wideString, "c2": 1
    // "a1" : 5, "b1" : wideString, "c1" : 2, "a2":6, "b2" : wideString, "c2": 2
    // "a1" : 5, "b1" : wideString, "c1" : 3, "a2":6, "b2" : wideString, "c2": 3
    List<String> expectedJsonBatches = Lists.newArrayList();
    StringBuilder expectedBatchString = new StringBuilder();
    expectedBatchString.append("[");
    for (int i = 0; i < numRows; i++) {
      expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + i);
      expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + i + "},");
    }
    expectedBatchString.append("{\"a1\": 5, " + "\"b1\" : " + "\"" + wideString + "\"," + "\"c1\" : " + numRows);
    expectedBatchString.append(", \"a2\": 6, " + "\"b2\" : " + "\"" + wideString + "\"," + "\"c2\" : " + numRows + "}");
    expectedBatchString.append("]");
    expectedJsonBatches.add(expectedBatchString.toString());

    long totalSize = getExpectedSize(expectedJsonBatches);

    // set the output batch size to 1/2 of total size expected.
    // We will get approximately 4 batches.
    fragContext.getOptions().setLocalOption("drill.exec.memory.operator.output_batch_size", totalSize/2);

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(nestedLoopJoin)
            .baselineColumns("a1", "b1", "c1", "a2", "b2", "c2")
            .expectedNumBatches(4)  // verify number of batches
            .expectedBatchSize(totalSize / 2) // verify batch size
            .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches));

    for (long i = 0; i < numRows+1; i++) {
      opTestBuilder.baselineValues(5l, wideString, i, 6l, wideString, i);
    }

    opTestBuilder.go();

  }

  @Test
  public void testSizerRepeatedList() throws Exception {
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    StringBuilder newString = new StringBuilder();
    newString.append("[ [1,2,3,4], [5,6,7,8] ]");

    numRows = 9;
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"c\" : " + newString);
      batchString.append("},");
    }
    batchString.append("{\"c\" : " + newString);
    batchString.append("}");

    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Create a dummy scanBatch to figure out the size.
    RecordBatch scanBatch = new ScanBatch(new MockPhysicalOperator(),
      fragContext, getReaderListForJsonBatches(inputJsonBatches, fragContext));

    VectorAccessible va = new BatchIterator(scanBatch).iterator().next();
    RecordBatchSizer sizer = new RecordBatchSizer(va);

    assertEquals(1, sizer.columns().size());
    RecordBatchSizer.ColumnSize column = sizer.columns().get("c");
    assertNotNull(column);

    /**
     * stdDataSize:8*5*5, stdNetSize:8*5*5 + 4*5 + 4*5 + 4,
     * dataSizePerEntry:8*8, netSizePerEntry:8*8 + 4*2 + 4,
     * totalDataSize:8*8*10, totalNetSize:netSizePerEntry*10, valueCount:10,
     * elementCount:10, estElementCountPerArray:1, isVariableWidth:false
     */
    assertEquals(200, column.getStdDataSizePerEntry());
    assertEquals(244, column.getStdNetSizePerEntry());
    assertEquals(64, column.getDataSizePerEntry());
    assertEquals(76, column.getNetSizePerEntry());
    assertEquals(640, column.getTotalDataSize());
    assertEquals(760, column.getTotalNetSize());
    assertEquals(10, column.getValueCount());
    assertEquals(20, column.getElementCount());
    assertEquals(2, column.getCardinality(), 0.01);
    assertEquals(false, column.isVariableWidth());

    final int testRowCount = 1000;
    final int testRowCountPowerTwo = 2048;

    for (VectorWrapper<?> vw : va) {
      ValueVector v = vw.getValueVector();
      v.clear();

      RecordBatchSizer.ColumnSize colSize = sizer.getColumn(v.getField().getName());

      // Allocates to nearest power of two
      colSize.allocateVector(v, testRowCount);

      // offset vector of delegate vector i.e. outer array should have row count number of values.
      UInt4Vector offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals((Integer.highestOneBit(testRowCount) << 1), offsetVector.getValueCapacity());

      // Get inner vector of delegate vector.
      ValueVector vector = ((RepeatedValueVector) v).getDataVector();

      // Data vector of inner vector should
      // have 2 (outer array cardinality) * 4 (inner array cardinality) * row count number of values.
      ValueVector dataVector = ((RepeatedValueVector) vector).getDataVector();
      assertEquals(Integer.highestOneBit((testRowCount*8)  << 1), dataVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * row count number of values.
      offsetVector = ((RepeatedValueVector) vector).getOffsetVector();
      assertEquals((Integer.highestOneBit(testRowCount*2) << 1), offsetVector.getValueCapacity());
      v.clear();

      // Allocates the same as value passed since it is already power of two.
      // -1 is done for adjustment needed for offset vector.
      colSize.allocateVector(v, testRowCountPowerTwo - 1);

      // offset vector of delegate vector i.e. outer array should have row count number of values.
      offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals(testRowCountPowerTwo, offsetVector.getValueCapacity());

      // Get inner vector of delegate vector.
      vector = ((RepeatedValueVector) v).getDataVector();

      // Data vector of inner vector should
      // have 2 (outer array cardinality) * 4 (inner array cardinality) * row count number of values.
      dataVector = ((RepeatedValueVector) vector).getDataVector();
      assertEquals(testRowCountPowerTwo * 8, dataVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * row count number of values.
      offsetVector = ((RepeatedValueVector) vector).getOffsetVector();
      assertEquals(testRowCountPowerTwo * 2, offsetVector.getValueCapacity());
      v.clear();

      // MAX ROW COUNT
      colSize.allocateVector(v, ValueVector.MAX_ROW_COUNT - 1);

      // offset vector of delegate vector i.e. outer array should have row count number of values.
      offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals(ValueVector.MAX_ROW_COUNT, offsetVector.getValueCapacity());

      // Get inner vector of delegate vector.
      vector = ((RepeatedValueVector) v).getDataVector();

      // Data vector of inner vector should
      // have 2 (outer array cardinality) * 4 (inner array cardinality) * row count number of values.
      dataVector = ((RepeatedValueVector) vector).getDataVector();
      assertEquals(ValueVector.MAX_ROW_COUNT*8, dataVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * row count number of values.
      offsetVector = ((RepeatedValueVector) vector).getOffsetVector();
      assertEquals(ValueVector.MAX_ROW_COUNT*2, offsetVector.getValueCapacity());
      v.clear();

      // MIN ROW COUNT
      colSize.allocateVector(v, 0);

      // offset vector of delegate vector i.e. outer array should have 1 value.
      offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals(ValueVector.MIN_ROW_COUNT, offsetVector.getValueCapacity());

      // Get inner vector of delegate vector.
      vector = ((RepeatedValueVector) v).getDataVector();

      // Data vector of inner vector should have 1 value
      dataVector = ((RepeatedValueVector) vector).getDataVector();
      assertEquals(ValueVector.MIN_ROW_COUNT, dataVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * 1.
      offsetVector = ((RepeatedValueVector) vector).getOffsetVector();
      assertEquals(ValueVector.MIN_ROW_COUNT*2, offsetVector.getValueCapacity());
      v.clear();
    }
  }

  @Test
  public void testSizerRepeatedRepeatedList() throws Exception {
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    StringBuilder newString = new StringBuilder();
    newString.append("[ [[1,2,3,4], [5,6,7,8]], [[1,2,3,4], [5,6,7,8]] ]");

    numRows = 9;
    batchString.append("[");
    for (int i = 0; i < numRows; i++) {
      batchString.append("{\"c\" : " + newString);
      batchString.append("},");
    }
    batchString.append("{\"c\" : " + newString);
    batchString.append("}");

    batchString.append("]");
    inputJsonBatches.add(batchString.toString());

    // Create a dummy scanBatch to figure out the size.
    RecordBatch scanBatch = new ScanBatch(new MockPhysicalOperator(),
      fragContext, getReaderListForJsonBatches(inputJsonBatches, fragContext));

    VectorAccessible va = new BatchIterator(scanBatch).iterator().next();
    RecordBatchSizer sizer = new RecordBatchSizer(va);

    assertEquals(1, sizer.columns().size());
    RecordBatchSizer.ColumnSize column = sizer.columns().get("c");
    assertNotNull(column);

    /**
     * stdDataSize:8*5*5*5, stdNetSize:8*5*5*5 + 8*5*5 + 8*5 + 4,
     * dataSizePerEntry:16*8, netSizePerEntry:16*8 + 16*4 + 4*2 + 4*2,
     * totalDataSize:16*8*10, totalNetSize:netSizePerEntry*10, valueCount:10,
     * elementCount:10, estElementCountPerArray:1, isVariableWidth:false
     */
    assertEquals(1000, column.getStdDataSizePerEntry());
    assertEquals(1244, column.getStdNetSizePerEntry());
    assertEquals(128, column.getDataSizePerEntry());
    assertEquals(156, column.getNetSizePerEntry());
    assertEquals(1280, column.getTotalDataSize());
    assertEquals(1560, column.getTotalNetSize());
    assertEquals(10, column.getValueCount());
    assertEquals(20, column.getElementCount());
    assertEquals(2, column.getCardinality(), 0.01);
    assertEquals(false, column.isVariableWidth());

    final int testRowCount = 1000;
    final int testRowCountPowerTwo = 2048;

    for (VectorWrapper<?> vw : va) {
      ValueVector v = vw.getValueVector();
      v.clear();

      RecordBatchSizer.ColumnSize colSize = sizer.getColumn(v.getField().getName());

      // Allocates to nearest power of two
      colSize.allocateVector(v, testRowCount);

      // offset vector of delegate vector i.e. outer array should have row count number of values.
      UInt4Vector offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals((Integer.highestOneBit(testRowCount) << 1), offsetVector.getValueCapacity());

      // Get data vector of delegate vector. This is repeated list again
      ValueVector dataVector = ((RepeatedListVector) v).getDataVector();

      // offset vector of delegate vector of the inner repeated list
      // This should have row count * 2 number of values.
      offsetVector = ((RepeatedListVector) dataVector).getOffsetVector();
      assertEquals((Integer.highestOneBit(testRowCount*2) << 1), offsetVector.getValueCapacity());

      // Data vector of inner vector should have row count * 2 number of values - 1 (for offset vector adjustment).
      ValueVector innerDataVector = ((RepeatedValueVector) dataVector).getDataVector();
      assertEquals((Integer.highestOneBit((testRowCount*2)  << 1) - 1), dataVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * 2 (inner array cardinality) * row count number of values.
      offsetVector = ((RepeatedValueVector) innerDataVector).getOffsetVector();
      assertEquals((Integer.highestOneBit(testRowCount*4) << 1), offsetVector.getValueCapacity());

      // Data vector of inner vector should
      // have 2 (outer array cardinality) * 2 (inner array cardinality)  * row count number of values.
      dataVector = ((RepeatedValueVector) innerDataVector).getDataVector();
      assertEquals(Integer.highestOneBit(testRowCount << 1) * 16, dataVector.getValueCapacity());

      v.clear();

      // Allocates the same as value passed since it is already power of two.
      // -1 is done for adjustment needed for offset vector.
      colSize.allocateVector(v, testRowCountPowerTwo - 1);

      // offset vector of delegate vector i.e. outer array should have row count number of values.
      offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals(testRowCountPowerTwo, offsetVector.getValueCapacity());

      // Get data vector of delegate vector. This is repeated list again
      dataVector = ((RepeatedListVector) v).getDataVector();

      // offset vector of delegate vector of the inner repeated list
      // This should have row count * 2 number of values.
      offsetVector = ((RepeatedListVector) dataVector).getOffsetVector();
      assertEquals(testRowCountPowerTwo*2, offsetVector.getValueCapacity());

      // Data vector of inner vector should have row count * 2 number of values - 1 (for offset vector adjustment).
      innerDataVector = ((RepeatedValueVector) dataVector).getDataVector();
      assertEquals(testRowCountPowerTwo*2 - 1, dataVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * 2 (inner array cardinality) * row count number of values.
      offsetVector = ((RepeatedValueVector) innerDataVector).getOffsetVector();
      assertEquals(testRowCountPowerTwo*4, offsetVector.getValueCapacity());

      // Data vector of inner vector should
      // have 2 (outer array cardinality) * 2 (inner array cardinality)  * row count number of values.
      dataVector = ((RepeatedValueVector) innerDataVector).getDataVector();
      assertEquals(testRowCountPowerTwo * 16, dataVector.getValueCapacity());

      v.clear();

      // MAX ROW COUNT
      colSize.allocateVector(v, ValueVector.MAX_ROW_COUNT - 1);

      // offset vector of delegate vector i.e. outer array should have row count number of values.
      offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals(ValueVector.MAX_ROW_COUNT, offsetVector.getValueCapacity());

      // Get data vector of delegate vector. This is repeated list again
      dataVector = ((RepeatedListVector) v).getDataVector();

      // offset vector of delegate vector of the inner repeated list
      // This should have row count * 2 number of values.
      offsetVector = ((RepeatedListVector) dataVector).getOffsetVector();
      assertEquals(ValueVector.MAX_ROW_COUNT*2, offsetVector.getValueCapacity());

      // Data vector of inner vector should have row count * 2 number of values - 1 (for offset vector adjustment).
      innerDataVector = ((RepeatedValueVector) dataVector).getDataVector();
      assertEquals(ValueVector.MAX_ROW_COUNT*2 - 1, dataVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * 2 (inner array cardinality) * row count number of values.
      offsetVector = ((RepeatedValueVector) innerDataVector).getOffsetVector();
      assertEquals(ValueVector.MAX_ROW_COUNT*4, offsetVector.getValueCapacity());

      // Data vector of inner vector should
      // have 2 (outer array cardinality) * 2 (inner array cardinality)  * row count number of values.
      dataVector = ((RepeatedValueVector) innerDataVector).getDataVector();
      assertEquals(ValueVector.MAX_ROW_COUNT*16, dataVector.getValueCapacity());

      v.clear();

      // MIN ROW COUNT
      colSize.allocateVector(v, 0);

      // offset vector of delegate vector i.e. outer array should have 1 value.
      offsetVector = ((RepeatedListVector) v).getOffsetVector();
      assertEquals(ValueVector.MIN_ROW_COUNT, offsetVector.getValueCapacity());

      // Get data vector of delegate vector. This is repeated list again
      dataVector = ((RepeatedListVector) v).getDataVector();

      // offset vector of delegate vector of the inner repeated list
      offsetVector = ((RepeatedListVector) dataVector).getOffsetVector();
      assertEquals(ValueVector.MIN_ROW_COUNT, offsetVector.getValueCapacity());

      // offset vector of inner vector should have
      // 2 (outer array cardinality) * 1.
      offsetVector = ((RepeatedValueVector) innerDataVector).getOffsetVector();
      assertEquals(ValueVector.MIN_ROW_COUNT*2, offsetVector.getValueCapacity());

      // Data vector of inner vector should 1 value.
      dataVector = ((RepeatedValueVector) innerDataVector).getDataVector();
      assertEquals(ValueVector.MIN_ROW_COUNT, dataVector.getValueCapacity());

      v.clear();

    }
  }

}
