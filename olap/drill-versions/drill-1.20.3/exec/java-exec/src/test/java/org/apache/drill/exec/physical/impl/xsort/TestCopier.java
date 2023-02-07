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
package org.apache.drill.exec.physical.impl.xsort;

import static org.apache.drill.test.rowSet.RowSetUtilities.objArray;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.physical.impl.xsort.SortTestUtilities.CopierTester;
import org.apache.drill.exec.physical.rowSet.RowSet.ExtendableRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetWriter;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.OperatorFixture;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Light-weight sanity test of the copier class. The implementation has
 * been used in production, so the tests here just check for the obvious
 * cases.
 * <p>
 * Note, however, that if significant changes are made to the copier,
 * then additional tests should be added to re-validate the code.
 */

@Category(OperatorTest.class)
public class TestCopier extends SubOperatorTest {

  @Test
  public void testEmptyInput() {
    TupleMetadata schema = SortTestUtilities.nonNullSchema();
    List<BatchGroup> batches = new ArrayList<>();
    Sort popConfig = SortTestUtilities.makeCopierConfig(Ordering.ORDER_ASC, Ordering.NULLS_UNSPECIFIED);
    OperatorContext opContext = fixture.newOperatorContext(popConfig);
    PriorityQueueCopierWrapper copier = new PriorityQueueCopierWrapper(opContext);
    VectorContainer dest = new VectorContainer();
    try {
      // TODO: Create a vector allocator to pass as last parameter so
      // that the test uses the same vector allocator as the production
      // code. Only nuisance is that we don't have the required metadata
      // readily at hand here...

      copier.startMerge(new BatchSchema(BatchSchema.SelectionVectorMode.NONE, schema.toFieldList()),
          batches, dest, 10, null);
      fail();
    } catch (AssertionError e) {
      // Expected
    } finally {
      opContext.close();
    }
  }

  @Test
  public void testEmptyBatch() throws Exception {
    TupleMetadata schema = SortTestUtilities.nonNullSchema();
    CopierTester tester = new CopierTester(fixture);
    tester.addInput(fixture.rowSetBuilder(schema)
          .withSv2()
          .build());

    tester.run();
  }

  @Test
  public void testSingleRow() throws Exception {
    TupleMetadata schema = SortTestUtilities.nonNullSchema();
    CopierTester tester = new CopierTester(fixture);
    tester.addInput(fixture.rowSetBuilder(schema)
          .addRow(10, "10")
          .withSv2()
          .build());

    tester.addOutput(fixture.rowSetBuilder(schema)
          .addRow(10, "10")
          .build());
    tester.run();
  }

  @Test
  public void testTwoBatchesSingleRow() throws Exception {
    TupleMetadata schema = SortTestUtilities.nonNullSchema();
    CopierTester tester = new CopierTester(fixture);
    tester.addInput(fixture.rowSetBuilder(schema)
          .addRow(10, "10")
          .withSv2()
          .build());
    tester.addInput(fixture.rowSetBuilder(schema)
          .addRow(20, "20")
          .withSv2()
          .build());

    tester.addOutput(fixture.rowSetBuilder(schema)
          .addRow(10, "10")
          .addRow(20, "20")
          .build());
    tester.run();
  }

  public static SingleRowSet makeDataSet(TupleMetadata schema, int first, int step, int count) {
    ExtendableRowSet rowSet = fixture.rowSet(schema);
    RowSetWriter writer = rowSet.writer(count);
    int value = first;
    for (int i = 0; i < count; i++, value += step) {
      RowSetUtilities.setFromInt(writer, 0, value);
      writer.scalar(1).setString(Integer.toString(value));
      writer.save();
    }
    writer.done();
    return rowSet;
  }

  @Test
  public void testMultipleOutput() throws Exception {
    TupleMetadata schema = SortTestUtilities.nonNullSchema();

    CopierTester tester = new CopierTester(fixture);
    tester.addInput(makeDataSet(schema, 0, 2, 10).toIndirect());
    tester.addInput(makeDataSet(schema, 1, 2, 10).toIndirect());

    tester.addOutput(makeDataSet(schema, 0, 1, 10));
    tester.addOutput(makeDataSet(schema, 10, 1, 10));
    tester.run();
  }

  // Also verifies that SV2s work

  @Test
  public void testMultipleOutputDesc() throws Exception {
    TupleMetadata schema = SortTestUtilities.nonNullSchema();

    CopierTester tester = new CopierTester(fixture);
    tester.sortOrder = Ordering.ORDER_DESC;
    tester.nullOrder = Ordering.NULLS_UNSPECIFIED;
    SingleRowSet input = makeDataSet(schema, 0, 2, 10).toIndirect();
    RowSetUtilities.reverse(input.getSv2());
    tester.addInput(input);

    input = makeDataSet(schema, 1, 2, 10).toIndirect();
    RowSetUtilities.reverse(input.getSv2());
    tester.addInput(input);

    tester.addOutput(makeDataSet(schema, 19, -1, 10));
    tester.addOutput(makeDataSet(schema, 9, -1, 10));

    tester.run();
  }

  @Test
  public void testAscNullsLast() throws Exception {
    TupleMetadata schema = SortTestUtilities.nullableSchema();

    CopierTester tester = new CopierTester(fixture);
    tester.sortOrder = Ordering.ORDER_ASC;
    tester.nullOrder = Ordering.NULLS_LAST;
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(1, "1")
        .addRow(4, "4")
        .addRow(null, "null")
        .withSv2()
        .build());
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(2, "2")
        .addRow(3, "3")
        .addRow(null, "null")
        .withSv2()
        .build());

    tester.addOutput(fixture.rowSetBuilder(schema)
        .addRow(1, "1")
        .addRow(2, "2")
        .addRow(3, "3")
        .addRow(4, "4")
        .addRow(null, "null")
        .addRow(null, "null")
        .build());

    tester.run();
  }

  @Test
  public void testAscNullsFirst() throws Exception {
    TupleMetadata schema = SortTestUtilities.nullableSchema();

    CopierTester tester = new CopierTester(fixture);
    tester.sortOrder = Ordering.ORDER_ASC;
    tester.nullOrder = Ordering.NULLS_FIRST;
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(null, "null")
        .addRow(1, "1")
        .addRow(4, "4")
        .withSv2()
        .build());
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(null, "null")
        .addRow(2, "2")
        .addRow(3, "3")
        .withSv2()
        .build());

    tester.addOutput(fixture.rowSetBuilder(schema)
        .addRow(null, "null")
        .addRow(null, "null")
        .addRow(1, "1")
        .addRow(2, "2")
        .addRow(3, "3")
        .addRow(4, "4")
        .build());

    tester.run();
  }

  @Test
  public void testDescNullsLast() throws Exception {
    TupleMetadata schema = SortTestUtilities.nullableSchema();

    CopierTester tester = new CopierTester(fixture);
    tester.sortOrder = Ordering.ORDER_DESC;
    tester.nullOrder = Ordering.NULLS_LAST;
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(4, "4")
        .addRow(1, "1")
        .addRow(null, "null")
        .withSv2()
        .build());
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(3, "3")
        .addRow(2, "2")
        .addRow(null, "null")
        .withSv2()
        .build());

    tester.addOutput(fixture.rowSetBuilder(schema)
        .addRow(4, "4")
        .addRow(3, "3")
        .addRow(2, "2")
        .addRow(1, "1")
        .addRow(null, "null")
        .addRow(null, "null")
        .build());

    tester.run();
  }

  @Test
  public void testDescNullsFirst() throws Exception {
    TupleMetadata schema = SortTestUtilities.nullableSchema();

    CopierTester tester = new CopierTester(fixture);
    tester.sortOrder = Ordering.ORDER_DESC;
    tester.nullOrder = Ordering.NULLS_FIRST;
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(null, "null")
        .addRow(4, "4")
        .addRow(1, "1")
        .withSv2()
        .build());
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(null, "null")
        .addRow(3, "3")
        .addRow(2, "2")
        .withSv2()
        .build());

    tester.addOutput(fixture.rowSetBuilder(schema)
        .addRow(null, "null")
        .addRow(null, "null")
        .addRow(4, "4")
        .addRow(3, "3")
        .addRow(2, "2")
        .addRow(1, "1")
        .build());

    tester.run();
  }

  public static void runTypeTest(OperatorFixture fixture, MinorType type) throws Exception {
    TupleMetadata schema = SortTestUtilities.makeSchema(type, false);

    CopierTester tester = new CopierTester(fixture);
    tester.addInput(makeDataSet(schema, 0, 2, 5).toIndirect());
    tester.addInput(makeDataSet(schema, 1, 2, 5).toIndirect());

    tester.addOutput(makeDataSet(schema, 0, 1, 10));

    tester.run();
  }

  @Test
  public void testTypes() throws Exception {
    testAllTypes(fixture);
  }

  public static void testAllTypes(OperatorFixture fixture) throws Exception {
    runTypeTest(fixture, MinorType.INT);
    runTypeTest(fixture, MinorType.BIGINT);
    runTypeTest(fixture, MinorType.FLOAT4);
    runTypeTest(fixture, MinorType.FLOAT8);
    runTypeTest(fixture, MinorType.VARDECIMAL);
    runTypeTest(fixture, MinorType.VARCHAR);
    runTypeTest(fixture, MinorType.VARBINARY);
    runTypeTest(fixture, MinorType.DATE);
    runTypeTest(fixture, MinorType.TIME);
    runTypeTest(fixture, MinorType.TIMESTAMP);
    runTypeTest(fixture, MinorType.INTERVAL);
    runTypeTest(fixture, MinorType.INTERVALDAY);
    runTypeTest(fixture, MinorType.INTERVALYEAR);

    // Others not tested. See DRILL-5329
  }

  @Test
  public void testMapType() throws Exception {
    testMapType(fixture);
  }

  public void testMapType(OperatorFixture fixture) throws Exception {
    TupleMetadata schema = new SchemaBuilder()
        .add("key", MinorType.INT)
        .addMap("m1")
          .add("b", MinorType.INT)
          .addMap("m2")
            .add("c", MinorType.INT)
            .resumeMap()
          .resumeSchema()
        .buildSchema();

    CopierTester tester = new CopierTester(fixture);
    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(1, objArray(10, objArray(100)))
        .addRow(5, objArray(50, objArray(500)))
        .withSv2()
        .build());

    tester.addInput(fixture.rowSetBuilder(schema)
        .addRow(2, objArray(20, objArray(200)))
        .addRow(6, objArray(60, objArray(600)))
        .withSv2()
        .build());

    tester.addOutput(fixture.rowSetBuilder(schema)
        .addRow(1, objArray(10, objArray(100)))
        .addRow(2, objArray(20, objArray(200)))
        .addRow(5, objArray(50, objArray(500)))
        .addRow(6, objArray(60, objArray(600)))
        .build());

    tester.run();
  }
}
