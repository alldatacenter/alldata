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
package org.apache.drill.exec.physical.impl.unnest;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.LateralContract;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.planner.common.DrillUnnestRelBase;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.store.mock.MockStorePOP;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class) public class TestUnnestCorrectness extends SubOperatorTest {


  // Operator Context for mock batch
  public static OperatorContext operatorContext;

  // use MockLateralJoinPop for MockRecordBatch ??
  public static PhysicalOperator mockPopConfig;


  @BeforeClass public static void setUpBeforeClass() throws Exception {
    mockPopConfig = new MockStorePOP(null);
    operatorContext = fixture.newOperatorContext(mockPopConfig);
  }

  @AfterClass public static void tearDownAfterClass() throws Exception {
    operatorContext.close();
  }

  @Test
  public void testUnnestFixedWidthColumn() {

    Object[][] data = {
        { new int[] {1, 2},
          new int[] {3, 4, 5}},
        { new int[] {6, 7, 8, 9},
          new int[] {10, 11, 12, 13, 14}}
    };

    // Create input schema
    TupleMetadata incomingSchema =
        new SchemaBuilder()
            .add("otherColumn", TypeProtos.MinorType.INT)
            .addArray("unnestColumn", TypeProtos.MinorType.INT)
            .buildSchema();
    TupleMetadata[] incomingSchemas = { incomingSchema, incomingSchema };

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    Integer[][] baseline = {{}, {}, {1, 1, 2, 2, 2}, {1, 2, 3, 4, 5}, {1, 1, 1, 1, 2, 2, 2, 2, 2}, {6, 7, 8, 9, 10, 11,
        12, 13, 14}};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestVarWidthColumn() {

    Object[][] data = {
        { new String[] {"", "zero"},
          new String[] {"one", "two", "three"}},
        { new String[] {"four", "five", "six", "seven"},
          new String[] {"eight", "nine", "ten", "eleven", "twelve"}}
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();
    TupleMetadata[] incomingSchemas = {incomingSchema, incomingSchema};

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    Object[][] baseline = {
        {}, {},
        {1, 1, 2, 2, 2}, {"", "zero", "one", "two", "three"},
        { 1, 1, 1, 1, 2, 2, 2, 2, 2}, {"four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve"}
    };

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestMapColumn() {

    Object[][] data = getMapData();

    // Create input schema
    TupleMetadata incomingSchema = getRepeatedMapSchema();
    TupleMetadata[] incomingSchemas = {incomingSchema, incomingSchema};

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    Object[][] baseline = getMapBaseline();

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestEmptyList() {

    Object[][] data = {
        { new String[] {},
          new String[] {}
        },
        { new String[] {},
          new String[] {}
        }
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();
    TupleMetadata[] incomingSchemas = {incomingSchema, incomingSchema};

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    // All subsequent batches are also empty
    String[][] baseline = {{}, {}, {}, {}, {}, {}};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestMultipleNewSchemaIncoming() {

    // Schema changes in incoming have no effect on unnest unless the type of the
    // unnest column itself has changed
    Object[][] data = {
        {
            new String[] {"0", "1"},
            new String[] {"2", "3", "4"}
        },
        {
            new String[] {"5", "6" },
        },
        {
            new String[] {"9"}
        }
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();
    TupleMetadata[] incomingSchemas = {incomingSchema, incomingSchema, incomingSchema};

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    Object[][] baseline = {
        {}, {},
        {1, 1, 2, 2, 2}, {"0", "1", "2", "3", "4"},
        {1, 1},
        {"5", "6" },
        {1}, {"9"}
    };

    RecordBatch.IterOutcome[] iterOutcomes = {
        RecordBatch.IterOutcome.OK_NEW_SCHEMA,
        RecordBatch.IterOutcome.OK,
        RecordBatch.IterOutcome.OK_NEW_SCHEMA};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestSchemaChange() {
    Object[][] data = {
        {
            new String[] {"0", "1"},
            new String[] {"2", "3", "4"}
        },
        {
            new String[] {"5", "6" },
        },
        {
            new int[] {9}
        }
    };

    // Create input schema
    TupleMetadata incomingSchema1 = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();
    TupleMetadata incomingSchema2 = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.INT).buildSchema();

    TupleMetadata[] incomingSchemas = {incomingSchema1, incomingSchema1, incomingSchema2};

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    // Another empty batch introduced by the schema change in the last batch
    Object[][] baseline = {
        {}, {},
        {1, 1, 2, 2, 2}, {"0", "1", "2", "3", "4"},
        {1, 1}, {"5", "6" },
        {}, {},
        {1}, {9}
    };

    RecordBatch.IterOutcome[] iterOutcomes = {
        RecordBatch.IterOutcome.OK_NEW_SCHEMA,
        RecordBatch.IterOutcome.OK,
        RecordBatch.IterOutcome.OK_NEW_SCHEMA};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestLimitBatchSize() {

    final int limitedOutputBatchSize = 1023; // one less than the power of two. See RecordBatchMemoryManager
                                             // .adjustOutputRowCount
    final int limitedOutputBatchSizeBytes = 1024*4*2; // (num rows+1) * size of int * num of columns (rowId, unnest_col)
    final int inputBatchSize = 1023+1;
    // single record batch with single row. The unnest column has one
    // more record than the batch size we want in the output
    Object[][] data = new Object[1][1];

    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[i].length; j++) {
        data[i][j] = new int[inputBatchSize];
        for (int k =0; k < inputBatchSize; k++) {
          ((int[])data[i][j])[k] = k;
        }
      }
    }
    Integer[][] baseline = new Integer[6][];
    baseline[0] = new Integer[] {};
    baseline[1] = new Integer[] {};
    baseline[2] = new Integer[limitedOutputBatchSize];
    baseline[3] = new Integer[limitedOutputBatchSize];
    baseline[4] = new Integer[1];
    baseline[5] = new Integer[1];
    for (int i = 0; i < limitedOutputBatchSize; i++) {
      baseline[2][i] = 1;
      baseline[3][i] = i;
    }
    baseline[4][0] = 1;
    baseline[5][0] = limitedOutputBatchSize;

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("otherColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.INT).buildSchema();

    TupleMetadata[] incomingSchemas = {incomingSchema};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK};

    final long outputBatchSize = fixture.getFragmentContext().getOptions().getOption(ExecConstants
        .OUTPUT_BATCH_SIZE_VALIDATOR);
    fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, limitedOutputBatchSizeBytes);

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    } finally {
      fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, outputBatchSize);
    }

  }

  @Test
  // Limit sends a kill. Unnest has more than one record batch for a record when
  // the kill is sent.
  public void testUnnestKillFromLimitSubquery1() {

    // similar to previous test; we split a record across more than one batch.
    // but we also set a limit less than the size of the batch so only one batch gets output.

    final int limitedOutputBatchSize = 1023; // one less than the power of two. See RecordBatchMemoryManager
                                             // .adjustOutputRowCount
    final int limitedOutputBatchSizeBytes = 1024*4*2; // (num rows+1) * size of int * num of columns (rowId, unnest_col)
    final int inputBatchSize = 1023+1;
    // single record batch with single row. The unnest column has one
    // more record than the batch size we want in the output
    Object[][] data = new Object[1][1];

    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[i].length; j++) {
        data[i][j] = new int[inputBatchSize];
        for (int k =0; k < inputBatchSize; k++) {
          ((int[])data[i][j])[k] = k;
        }
      }
    }
    Integer[][] baseline = new Integer[6][];
    baseline[0] = new Integer[] {};
    baseline[1] = new Integer[] {};
    baseline[2] = new Integer[limitedOutputBatchSize];
    baseline[3] = new Integer[limitedOutputBatchSize];
    baseline[4] = new Integer[1];
    baseline[5] = new Integer[1];
    for (int i = 0; i < limitedOutputBatchSize; i++) {
      baseline[2][i] = 1;
      baseline[3][i] = i;
    }
    baseline[4] = new Integer[] {}; // because of kill the next batch is an empty batch
    baseline[5] = new Integer[] {}; // because of kill the next batch is an empty batch

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("otherColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.INT).buildSchema();

    TupleMetadata[] incomingSchemas = {incomingSchema};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK};

    final long outputBatchSize = fixture.getFragmentContext().getOptions().getOption(ExecConstants
        .OUTPUT_BATCH_SIZE_VALIDATOR);
    fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, limitedOutputBatchSizeBytes);

    try {
      testUnnest(incomingSchemas, iterOutcomes, 100, -1, data, baseline); // Limit of 100 values for unnest.
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    } finally {
      fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, outputBatchSize);
    }

  }

  @Test
  // Limit sends a kill. Unnest has exactly one record batch for a record when
  // the kill is sent. This test is actually useless since it tests the behaviour of
  // the mock lateral which doesn't send kill at all if it gets an EMIT. We expect limit
  // to do so, so let's keep the test to demonstrate the expected behaviour.
  public void testUnnestKillFromLimitSubquery2() {

    // similar to previous test but the size of the array fits exactly into the record batch;
    final int limitedOutputBatchSize = 1023; // one less than the power of two. See RecordBatchMemoryManager
                                             // .adjustOutputRowCount
    final int limitedOutputBatchSizeBytes = 1024*4*2; // (num rows+1) * size of int * num of columns (rowId, unnest_col)
    final int inputBatchSize = 1023;
    // single record batch with single row. The unnest column has one
    // more record than the batch size we want in the output
    Object[][] data = new Object[1][1];

    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[i].length; j++) {
        data[i][j] = new int[inputBatchSize];
        for (int k =0; k < inputBatchSize; k++) {
          ((int[])data[i][j])[k] = k;
        }
      }
    }
    Integer[][] baseline = new Integer[4][];
    baseline[0] = new Integer[] {};
    baseline[1] = new Integer[] {};
    baseline[2] = new Integer[limitedOutputBatchSize];
    baseline[3] = new Integer[limitedOutputBatchSize];
    for (int i = 0; i < limitedOutputBatchSize; i++) {
      baseline[2][i] = 1;
      baseline[3][i] = i;
    }

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("rowNumber", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.INT).buildSchema();

    TupleMetadata[] incomingSchemas = {incomingSchema};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK};

    final long outputBatchSize = fixture.getFragmentContext().getOptions().getOption(ExecConstants
        .OUTPUT_BATCH_SIZE_VALIDATOR);
    fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, limitedOutputBatchSizeBytes);

    try {
      testUnnest(incomingSchemas, iterOutcomes, 100, -1, data, baseline); // Limit of 100 values for unnest.
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    } finally {
      fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, outputBatchSize);
    }

  }


  @Test
  public void testUnnestNonArrayColumn() {

    Object[][] data = {
        { new Integer (1),
            new Integer (3)},
        { new Integer (6),
            new Integer (10)}
    };

    // Create input schema
    TupleMetadata incomingSchema =
        new SchemaBuilder()
            .add("rowNumber", TypeProtos.MinorType.INT)
            .add("unnestColumn", TypeProtos.MinorType.INT)
            .buildSchema();
    TupleMetadata[] incomingSchemas = { incomingSchema, incomingSchema };

    // We expect an Exception
    Integer[][] baseline = {};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline);
    } catch (UserException e) {
      return; // succeeded
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }


  // test unnest for various input conditions without invoking kill
  private <T> void testUnnest(
      TupleMetadata[] incomingSchemas,
      RecordBatch.IterOutcome[] iterOutcomes,
      T[][] data,
      T[][] baseline ) throws Exception{
    testUnnest(incomingSchemas, iterOutcomes, -1, -1, data, baseline);
  }

  // test unnest for various input conditions optionally invoking kill. if the kill or killBatch
  // parameter is greater than 0 then the record batch is sent a kill after that many batches have been processed
  private <T> void testUnnest( TupleMetadata[] incomingSchemas,
      RecordBatch.IterOutcome[] iterOutcomes,
      int unnestLimit, // kill unnest after every 'unnestLimit' number of values in every record
      int execKill, // number of batches after which to kill the execution (!)
      T[][] data,
      T[][] baseline) throws Exception {

    // Get the incoming container with dummy data for LJ
    final List<VectorContainer> incomingContainer = new ArrayList<>(data.length);

    // Create data
    ArrayList<RowSet.SingleRowSet> rowSets = new ArrayList<>();

    int rowNumber = 0;
    int batchNum = 0;
    for ( Object[] recordBatch : data) {
      RowSetBuilder rowSetBuilder = fixture.rowSetBuilder(incomingSchemas[batchNum]);
      for ( Object rowData : recordBatch) {
        rowSetBuilder.addRow(++rowNumber, rowData);
      }
      RowSet.SingleRowSet rowSet = rowSetBuilder.build();
      rowSets.add(rowSet);
      incomingContainer.add(rowSet.container());
      batchNum++;
    }

    // Get the unnest POPConfig
    final UnnestPOP unnestPopConfig = new UnnestPOP(null, new SchemaPath(new PathSegment.NameSegment("unnestColumn")), DrillUnnestRelBase.IMPLICIT_COLUMN);

    // Get the IterOutcomes for LJ
    final List<RecordBatch.IterOutcome> outcomes = new ArrayList<>(iterOutcomes.length);
    for(RecordBatch.IterOutcome o : iterOutcomes) {
      outcomes.add(o);
    }

    // Create incoming MockRecordBatch
    final MockRecordBatch incomingMockBatch =
        new MockRecordBatch(fixture.getFragmentContext(), operatorContext, incomingContainer, outcomes,
            incomingContainer.get(0).getSchema());

    final MockLateralJoinBatch lateralJoinBatch =
        new MockLateralJoinBatch(fixture.getFragmentContext(), operatorContext, incomingMockBatch);


    // setup Unnest record batch
    final UnnestRecordBatch unnestBatch =
        new UnnestRecordBatch(unnestPopConfig, fixture.getFragmentContext());

    // set pointer to Lateral in unnest pop config
    unnestBatch.setIncoming((LateralContract) lateralJoinBatch);

    // set backpointer to lateral join in unnest
    lateralJoinBatch.setUnnest(unnestBatch);
    lateralJoinBatch.setUnnestLimit(unnestLimit);

    // Simulate the pipeline by calling next on the incoming
    List<ValueVector> results = null;
    int batchesProcessed = 0;
    try {
      while (!isTerminal(lateralJoinBatch.next())) {
        batchesProcessed++;
        if (batchesProcessed == execKill) {
          lateralJoinBatch.getContext().getExecutorState().fail(new DrillException("Testing failure of execution."));
          lateralJoinBatch.cancel();
        }
        // else nothing to do
      }

      // Check results against baseline
      results = lateralJoinBatch.getResultList();

      int i = 0;
      for (ValueVector vv : results) {
        int valueCount = vv.getAccessor().getValueCount();
        if (valueCount != baseline[i].length) {
          fail("Test failed in validating unnest output. Value count mismatch.");
        }
        for (int j = 0; j < valueCount; j++) {

          if (vv instanceof MapVector) {
            if (!compareMapBaseline(baseline[i][j], vv.getAccessor().getObject(j))) {
              fail("Test failed in validating unnest(Map) output. Value mismatch");
            }
          } else if (vv instanceof VarCharVector) {
            Object val = vv.getAccessor().getObject(j);
            if (((String) baseline[i][j]).compareTo(val.toString()) != 0) {
              fail("Test failed in validating unnest output. Value mismatch. Baseline value[]" + i + "][" + j + "]"
                  + ": " + baseline[i][j] + "   VV.getObject(j): " + val);
            }
          } else {
            Object val = vv.getAccessor().getObject(j);
            if (!baseline[i][j].equals(val)) {
              fail(
                  "Test failed in validating unnest output. Value mismatch. Baseline value[" + i + "][" + j + "]" + ": "
                      + baseline[i][j] + "   VV.getObject(j): " + val);
            }
          }
        }
        i++;

      }

      assertTrue(lateralJoinBatch.isCompleted());

    } catch (UserException e) {
      throw e; // Valid exception
    } catch (Exception e) {
      fail("Test failed in validating unnest output. Exception : " + e.getMessage());
    } finally {
      // Close all the resources for this test case
      unnestBatch.close();
      lateralJoinBatch.close();
      incomingMockBatch.close();

      if (results != null) {
        for (ValueVector vv : results) {
          vv.clear();
        }
      }
      for(RowSet.SingleRowSet rowSet: rowSets) {
        rowSet.clear();
      }
    }

  }

  /**
   * Build a schema with a repeated map -
   *
   *  {
   *    rowNum,
   *    mapColumn : [
   *       {
   *         colA,
   *         colB : [
   *            varcharCol
   *         ]
   *       }
   *    ]
   *  }
   *
   * @see org.apache.drill.exec.physical.resultSet.impl.TestResultSetLoaderMapArray TestResultSetLoaderMapArray for
   * similar schema and data
   * @return TupleMetadata corresponding to the schema
   */
  private TupleMetadata getRepeatedMapSchema() {
    TupleMetadata schema = new SchemaBuilder()
        .add("rowNum", TypeProtos.MinorType.INT)
        .addMapArray("unnestColumn")
          .add("colA", TypeProtos.MinorType.INT)
          .addArray("colB", TypeProtos.MinorType.VARCHAR)
        .resumeSchema()
        .buildSchema();
    return schema;
  }

  private Object[][] getMapData( ) {

    Object[][] d = {
      {
          new Object[] {},
          new Object[] {
              new Object[] {11, new String[] {"1.1.1", "1.1.2" }},
              new Object[] {12, new String[] {"1.2.1", "1.2.2" }}
          },

          new Object[] {
              new Object[] {21, new String[] {"2.1.1", "2.1.2" }},
              new Object[] {22, new String[] {}},
              new Object[] {23, new String[] {"2.3.1", "2.3.2" }}
          }
      },
      {
        new Object[] {
            new Object[] {31, new String[] {"3.1.1", "3.1.2" }},
            new Object[] {32, new String[] {"3.2.1", "3.2.2" }}
        }
      }
    };

    return d;
  }

  private Object[][] getMapBaseline() {

    Object[][] d = {
        new Object[] {},    // Empty record batch returned by OK_NEW_SCHEMA
        new Object[] {},    // Empty record batch returned by OK_NEW_SCHEMA
        new Object[] {2, 2, 3, 3, 3}, // rowId 1 has no corresponding rows in the unnest array
        new Object[] {
            "{\"colA\":11,\"colB\":[\"1.1.1\",\"1.1.2\"]}",
            "{\"colA\":12,\"colB\":[\"1.2.1\",\"1.2.2\"]}",
            "{\"colA\":21,\"colB\":[\"2.1.1\",\"2.1.2\"]}",
            "{\"colA\":22,\"colB\":[]}",
            "{\"colA\":23,\"colB\":[\"2.3.1\",\"2.3.2\"]}"
        },
        new Object[] {1, 1},
        new Object[] {
            "{\"colA\":31,\"colB\":[\"3.1.1\",\"3.1.2\"]}",
            "{\"colA\":32,\"colB\":[\"3.2.1\",\"3.2.2\"]}"
        }
    };
    return d;
  }

  private boolean compareMapBaseline(Object baselineValue, Object vector) {
    String vv = vector.toString();
    String b = (String)baselineValue;
    return vv.equalsIgnoreCase(b);
  }

  private boolean isTerminal(RecordBatch.IterOutcome outcome) {
    return (outcome == RecordBatch.IterOutcome.NONE);
  }
}

