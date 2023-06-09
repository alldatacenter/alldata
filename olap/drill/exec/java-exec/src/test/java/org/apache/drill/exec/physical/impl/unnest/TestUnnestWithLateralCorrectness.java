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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.LateralContract;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.join.LateralJoinBatch;
import org.apache.drill.exec.physical.impl.project.ProjectRecordBatch;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.planner.common.DrillLateralJoinRelBase;
import org.apache.drill.exec.planner.common.DrillUnnestRelBase;
import org.apache.drill.exec.planner.logical.DrillLogicalTestUtils;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
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

import static org.junit.Assert.fail;

@Category(OperatorTest.class)
public class TestUnnestWithLateralCorrectness extends SubOperatorTest {

  // Operator Context for mock batch
  public static OperatorContext operatorContext;

  public static PhysicalOperator mockPopConfig;
  public static LateralJoinPOP ljPopConfig;

  @BeforeClass public static void setUpBeforeClass() throws Exception {
    mockPopConfig = new MockStorePOP(null);
    ljPopConfig = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());
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
            .add("rowNumber", TypeProtos.MinorType.INT)
            .addArray("unnestColumn", TypeProtos.MinorType.INT)
            .buildSchema();
    TupleMetadata[] incomingSchemas = { incomingSchema, incomingSchema };

    Integer[][][] baseline = {
        {
          {1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4}, //rowNum
          {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14} // unnestColumn_flat
        }
    };

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, false);
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

    Object[][][] baseline = {
      {
        {1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4}, // rowNum
        {"", "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven",
            "twelve"} // unnestColumn_flat
      }
    };

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, false);
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

    Object[][][] baseline = getMapBaseline();

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, false);
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

    // All batches are empty
    String[][][] baseline = {{{}}};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, false);
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

    Object[][][] baseline = {
        {
          {1, 1, 2, 2, 2, 3, 3},
          {"0", "1", "2", "3", "4", "5", "6"}
        },
        {
          {4},
          {"9"}
        }
    };

    RecordBatch.IterOutcome[] iterOutcomes = {
        RecordBatch.IterOutcome.OK_NEW_SCHEMA,
        RecordBatch.IterOutcome.OK,
        RecordBatch.IterOutcome.OK_NEW_SCHEMA};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, false);
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

    Object[][][] baseline = {
        {
          {1, 1, 2, 2, 2, 3, 3},
          {"0", "1", "2", "3", "4", "5", "6"}
        },
        {
          {4},
          {9}
        }
    };

    RecordBatch.IterOutcome[] iterOutcomes = {
        RecordBatch.IterOutcome.OK_NEW_SCHEMA,
        RecordBatch.IterOutcome.OK,
        RecordBatch.IterOutcome.OK_NEW_SCHEMA};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, false);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }
  }

  private void testUnnestBatchSizing(int inputBatchSize, int limitOutputBatchSize,
                                     int limitOutputBatchSizeBytes, boolean excludeUnnestColumn) {
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

    Integer[][][] baseline = new Integer[2][2][];
    baseline[0][0] = new Integer[limitOutputBatchSize];
    baseline[0][1] = new Integer[limitOutputBatchSize];
    baseline[1][0] = new Integer[1];
    baseline[1][1] = new Integer[1];
    for (int i = 0; i < limitOutputBatchSize; i++) {
      baseline[0][0][i] = 1;
      baseline[0][1][i] = i;
    }
    baseline[1][0][0] = 1; // row Num
    baseline[1][1][0] = limitOutputBatchSize; // value

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
      .add("rowNumber", TypeProtos.MinorType.INT)
      .addArray("unnestColumn", TypeProtos.MinorType.INT).buildSchema();

    TupleMetadata[] incomingSchemas = {incomingSchema};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK};

    final long outputBatchSize = fixture.getFragmentContext().getOptions().getOption(ExecConstants
      .OUTPUT_BATCH_SIZE_VALIDATOR);
    fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, limitOutputBatchSizeBytes);

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, excludeUnnestColumn);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    } finally {
      fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, outputBatchSize);
    }
  }

  @Test
  public void testUnnestLimitBatchSize_WithExcludedCols() {
    LateralJoinPOP previoudPop = ljPopConfig;
    List<SchemaPath> excludedCols = new ArrayList<>();
    excludedCols.add(SchemaPath.getSimplePath("unnestColumn"));
    ljPopConfig = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, excludedCols);
    final int limitedOutputBatchSize = 127;
    final int inputBatchSize = limitedOutputBatchSize + 1;
    // Since we want 127 row count and because of nearest power of 2 adjustment output row count will be reduced to
    // 64. So we should configure batch size for (N+1) rows if we want to output N rows where N is not power of 2
    // size of lateral output batch = (N+1)*8 bytes, where N = output batch row count
    //  Lateral output batch size = (N+1) * (input row size without unnest field) + (N+1) * size of single unnest column
    //                            = (N+1) * (size of row id) + (N+1) * (size of single array entry)
    //                            = (N+1)*4 + (N+1) * 4
    //                            = (N+1) * 8
    // configure the output batch size to be one more record than that so that the batch sizer can round down
    final int limitedOutputBatchSizeBytes = 8 * (limitedOutputBatchSize + 1);
    testUnnestBatchSizing(inputBatchSize, limitedOutputBatchSize, limitedOutputBatchSizeBytes, true);
    ljPopConfig = previoudPop;
  }

  @Test
  public void testUnnestLimitBatchSize() {
    final int limitedOutputBatchSize = 127;
    final int inputBatchSize = limitedOutputBatchSize + 1;
    // size of lateral output batch = 4N * (N + 5) bytes, where N = output batch row count
    //  Lateral output batch size =  N * input row size + N * size of single unnest column
    //                            =  N * (size of row id + size of array offset vector + (N + 1 )*size of single array entry))
    //                              + N * 4
    //                            = N * (4 + 2*4 + (N+1)*4 )  + N * 4
    //                            = N * (16 + 4N) + N * 4
    //                            = 4N * (N + 5)
    // configure the output batch size to be one more record than that so that the batch sizer can round down
    final int limitedOutputBatchSizeBytes = 4 * limitedOutputBatchSize * (limitedOutputBatchSize + 6);
    testUnnestBatchSizing(inputBatchSize, limitedOutputBatchSize, limitedOutputBatchSizeBytes, false);
  }

  @Test
  // Limit sends a kill. Unnest has more than one record batch for a record when
  // the kill is sent.
  public void testUnnestKillFromLimitSubquery1() {

    // similar to previous test; we split a record across more than one batch.
    // but we also set a limit less than the size of the batch so only one batch gets output.
    final int limitedOutputBatchSize = 127;
    final int inputBatchSize = limitedOutputBatchSize + 1;
    final int limitedOutputBatchSizeBytes = 4 * limitedOutputBatchSize * (limitedOutputBatchSize + 6);

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

    // because of kill we only get one batch back
    Integer[][][] baseline = new Integer[1][2][];
    baseline[0][0] = new Integer[limitedOutputBatchSize];
    baseline[0][1] = new Integer[limitedOutputBatchSize];
    for (int i = 0; i < limitedOutputBatchSize; i++) {
      baseline[0][0][i] = 1;
      baseline[0][1][i] = i;
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
      testUnnest(incomingSchemas, iterOutcomes, -1, 1, data, baseline, false); // Limit of 100 values for unnest.
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    } finally {
      fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, outputBatchSize);
    }
  }

  @Test
  // Limit sends a kill. Unnest has exactly one record batch for a record when
  // the kill is sent. This test is actually useless since it tests the behaviour of
  // lateral which doesn't send kill at all if it gets an EMIT. We expect limit
  // to do so, so let's keep the test to demonstrate the expected behaviour.
  public void testUnnestKillFromLimitSubquery2() {

    // similar to previous test but the size of the array fits exactly into the record batch;

    final int limitedOutputBatchSize = 127;
    final int inputBatchSize = limitedOutputBatchSize + 1;
    final int limitedOutputBatchSizeBytes = 4 * limitedOutputBatchSize * (limitedOutputBatchSize + 6);

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

    // because of kill we only get one batch back
    Integer[][][] baseline = new Integer[1][2][];
    baseline[0][0] = new Integer[limitedOutputBatchSize];
    baseline[0][1] = new Integer[limitedOutputBatchSize];
    for (int i = 0; i < limitedOutputBatchSize; i++) {
      baseline[0][0][i] = 1;
      baseline[0][1][i] = i;
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
      testUnnest(incomingSchemas, iterOutcomes, -1, 1, data, baseline, false); // Limit of 100 values for unnest.
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
    Integer[][][] baseline = {};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnest(incomingSchemas, iterOutcomes, data, baseline, false);
    } catch (UserException|UnsupportedOperationException e) {
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
      T[][][] baseline,
      boolean excludeUnnestColumn) throws Exception{
    testUnnest(incomingSchemas, iterOutcomes, -1, -1, data, baseline, excludeUnnestColumn);
  }

  // test unnest for various input conditions optionally invoking kill. if the kill or killBatch
  // parameter is greater than 0 then the record batch is sent a kill after that many batches have been processed
  private <T> void testUnnest( TupleMetadata[] incomingSchemas,
      RecordBatch.IterOutcome[] iterOutcomes,
      int unnestLimit, // kill unnest after every 'unnestLimit' number of values in every record
      int execKill, // number of batches after which to kill the execution (!)
      T[][] data,
      T[][][] baseline,
      boolean excludeUnnestColumn) throws Exception {

    // Get the incoming container with dummy data for LJ
    final List<VectorContainer> incomingContainer = new ArrayList<>(data.length);

    // Create data
    ArrayList<RowSet.SingleRowSet> rowSets = new ArrayList<>();
    int rowNumber = 0;
    int batchNum = 0;
    for (Object[] recordBatch : data) {
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
    final UnnestPOP unnestPopConfig = new UnnestPOP(null, SchemaPath.getCompoundPath("unnestColumn"), DrillUnnestRelBase.IMPLICIT_COLUMN);

    // Get the IterOutcomes for LJ
    final List<RecordBatch.IterOutcome> outcomes = new ArrayList<>(iterOutcomes.length);
    for(RecordBatch.IterOutcome o : iterOutcomes) {
      outcomes.add(o);
    }

    // Create incoming MockRecordBatch
    final MockRecordBatch incomingMockBatch =
        new MockRecordBatch(fixture.getFragmentContext(), operatorContext, incomingContainer, outcomes,
            incomingContainer.get(0).getSchema());

    // setup Unnest record batch
    final UnnestRecordBatch unnestBatch =
        new UnnestRecordBatch(unnestPopConfig, fixture.getFragmentContext());

    // project is required to rename the columns so as to disambiguate the same column name from
    // unnest operator and the regular scan.
    final Project projectPopConfig = new Project(DrillLogicalTestUtils.parseExprs("unnestColumn", "unnestColumn1",
      unnestPopConfig.getImplicitColumn(), unnestPopConfig.getImplicitColumn()), null);

    final ProjectRecordBatch projectBatch =
        new ProjectRecordBatch( projectPopConfig, unnestBatch, fixture.getFragmentContext());

    final LateralJoinBatch lateralJoinBatch =
        new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(), incomingMockBatch, projectBatch);

    // set pointer to Lateral in unnest
    unnestBatch.setIncoming((LateralContract) lateralJoinBatch);

    // Simulate the pipeline by calling next on the incoming

    // results is an array of batches, each batch being an array of output vectors.
    List<List<ValueVector> > resultList = new ArrayList<>();
    List<List<ValueVector> > results = null;
    int batchesProcessed = 0;
    try{
      try {
        while (!isTerminal(lateralJoinBatch.next())) {
          if (lateralJoinBatch.getRecordCount() > 0) {
            addBatchToResults(resultList, lateralJoinBatch);
          }
          batchesProcessed++;
          if (batchesProcessed == execKill) {
            // Errors are reported by throwing an exception.
            // Simulate by skipping out of the loop
            break;
          }
          // else nothing to do
        }
      } catch (UserException e) {
        throw e;
      } catch (Exception e) {
        fail(e.getMessage());
      }

      // Check results against baseline
      results = resultList;

      int batchIndex = 0;
      int vectorIndex = 0;
      //int valueIndex = 0;
      for (List<ValueVector> batch: results) {
        int vectorCount= batch.size();
        int expectedVectorCount = (excludeUnnestColumn) ? 0 : 1;
        expectedVectorCount += baseline[batchIndex].length;
        if (vectorCount!= expectedVectorCount) { // baseline does not include the original unnest column
          fail("Test failed in validating unnest output. Batch column count mismatch.");
        }
        for (ValueVector vv : batch) {
          if(vv.getField().getName().equals("unnestColumn")) {
            continue; // skip the original input column
          }
          int valueCount = vv.getAccessor().getValueCount();
          if (valueCount!= baseline[batchIndex][vectorIndex].length) {
            fail("Test failed in validating unnest output. Value count mismatch in batch number " + (batchIndex+1) +""
                + ".");
          }

          for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
            if (vv instanceof MapVector) {
              if (!compareMapBaseline(baseline[batchIndex][vectorIndex][valueIndex], vv
                  .getAccessor()
                  .getObject(valueIndex))) {
                fail("Test failed in validating unnest(Map) output. Value mismatch");
              }
            } else if (vv instanceof VarCharVector) {
              Object val = vv.getAccessor().getObject(valueIndex);
              if (((String) baseline[batchIndex][vectorIndex][valueIndex]).compareTo(val.toString()) != 0) {
                fail("Test failed in validating unnest output. Value mismatch. Baseline value[]" + vectorIndex + "][" + valueIndex
                    + "]" + ": " + baseline[vectorIndex][valueIndex] + "   VV.getObject(valueIndex): " + val);
              }
            } else {
              Object val = vv.getAccessor().getObject(valueIndex);
              if (!baseline[batchIndex][vectorIndex][valueIndex].equals(val)) {
                fail("Test failed in validating unnest output. Value mismatch. Baseline value[" + vectorIndex + "][" + valueIndex
                    + "]" + ": "
                    + baseline[batchIndex][vectorIndex][valueIndex] + "   VV.getObject(valueIndex): " + val);
              }
            }
          }
          vectorIndex++;
        }
        vectorIndex=0;
        batchIndex++;
      }
    } catch (UserException e) {
      throw e; // Valid exception
    } catch (Exception e) {
      fail("Test failed. Exception : " + e.getMessage());
    } finally {
      // Close all the resources for this test case
      unnestBatch.close();
      lateralJoinBatch.close();
      incomingMockBatch.close();

      if (results != null) {
        for (List<ValueVector> batch : results) {
          for (ValueVector vv : batch) {
            vv.clear();
          }
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

  private Object[][][] getMapBaseline() {

    Object[][][] d = {
      {
          {2,2,3,3,3,4,4},
          {
            "{\"colA\":11,\"colB\":[\"1.1.1\",\"1.1.2\"]}",
              "{\"colA\":12,\"colB\":[\"1.2.1\",\"1.2.2\"]}",
              "{\"colA\":21,\"colB\":[\"2.1.1\",\"2.1.2\"]}",
              "{\"colA\":22,\"colB\":[]}",
              "{\"colA\":23,\"colB\":[\"2.3.1\",\"2.3.2\"]}",
              "{\"colA\":31,\"colB\":[\"3.1.1\",\"3.1.2\"]}",
              "{\"colA\":32,\"colB\":[\"3.2.1\",\"3.2.2\"]}"
          }
      }
    };
    return d;
  }

  private Object[][][] getNestedMapBaseline() {

    Object[][][] d = {
        {
            {2,2,2,2,3,3,3,3,4,4,4,4},
            {
                "1.1.1",
                "1.1.2",
                "1.2.1",
                "1.2.2",
                "2.1.1",
                "2.1.2",
                "2.3.1",
                "2.3.2",
                "3.1.1",
                "3.1.2",
                "3.2.1",
                "3.2.2"
            }
        }
    };
    return d;
  }

  private boolean compareMapBaseline(Object baselineValue, Object vector) {
    String vv = vector.toString();
    String b = (String)baselineValue;
    return vv.equalsIgnoreCase(b);
  }

  private int addBatchToResults(List<List<ValueVector> > resultList, RecordBatch inputBatch) {
    int count = 0;
    final RecordBatchData batchCopy = new RecordBatchData(inputBatch, operatorContext.getAllocator());
    boolean success = false;
    try {
      count = batchCopy.getRecordCount();
      resultList.add(batchCopy.getVectors());
      success = true;
    } finally {
      if (!success) {
        batchCopy.clear();
      }
    }
    return count;
  }

  private boolean isTerminal(RecordBatch.IterOutcome outcome) {
    return (outcome == RecordBatch.IterOutcome.NONE);
  }

  /**
   *     Run a plan like the following for various input batches :
   *             Lateral1
   *               /    \
   *              /    Lateral2
   *            Scan      / \
   *                     /   \
   *                Project1 Project2
   *                   /       \
   *                  /         \
   *              Unnest1      Unnest2
   *
   *
   * @param incomingSchemas
   * @param iterOutcomes
   * @param execKill
   * @param data
   * @param baseline
   * @param <T>
   * @throws Exception
   */
  private <T> void testNestedUnnest( TupleMetadata[] incomingSchemas,
      RecordBatch.IterOutcome[] iterOutcomes,
      int execKill, // number of batches after which to kill the execution (!)
      T[][] data,
      T[][][] baseline) throws Exception {

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
    final UnnestPOP unnestPopConfig1 = new UnnestPOP(null, SchemaPath.getSimplePath("unnestColumn"), DrillUnnestRelBase.IMPLICIT_COLUMN);
    final UnnestPOP unnestPopConfig2 = new UnnestPOP(null, SchemaPath.getSimplePath("colB"), DrillUnnestRelBase.IMPLICIT_COLUMN);

    // Get the IterOutcomes for LJ
    final List<RecordBatch.IterOutcome> outcomes = new ArrayList<>(iterOutcomes.length);
    for(RecordBatch.IterOutcome o : iterOutcomes) {
      outcomes.add(o);
    }

    // Create incoming MockRecordBatch
    final MockRecordBatch incomingMockBatch =
        new MockRecordBatch(fixture.getFragmentContext(), operatorContext, incomingContainer, outcomes,
            incomingContainer.get(0).getSchema());

    // setup Unnest record batch
    final UnnestRecordBatch unnestBatch1 =
        new UnnestRecordBatch(unnestPopConfig1, fixture.getFragmentContext());
    final UnnestRecordBatch unnestBatch2 =
        new UnnestRecordBatch(unnestPopConfig2, fixture.getFragmentContext());

    // Create intermediate Project
    final Project projectPopConfig1 =
        new Project(DrillLogicalTestUtils.parseExprs("unnestColumn.colB", "colB",
          unnestPopConfig1.getImplicitColumn(), unnestPopConfig1.getImplicitColumn()), unnestPopConfig1);
    final ProjectRecordBatch projectBatch1 =
        new ProjectRecordBatch(projectPopConfig1, unnestBatch1, fixture.getFragmentContext());
    final Project projectPopConfig2 =
        new Project(DrillLogicalTestUtils.parseExprs("colB", "unnestColumn2",
          unnestPopConfig2.getImplicitColumn(), unnestPopConfig2.getImplicitColumn()), unnestPopConfig2);
    final ProjectRecordBatch projectBatch2 =
        new ProjectRecordBatch(projectPopConfig2, unnestBatch2, fixture.getFragmentContext());

    final LateralJoinPOP ljPopConfig2 = new LateralJoinPOP(projectPopConfig1, projectPopConfig2, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());
    final LateralJoinPOP ljPopConfig1 = new LateralJoinPOP(mockPopConfig, ljPopConfig2, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    final LateralJoinBatch lateralJoinBatch2 =
        new LateralJoinBatch(ljPopConfig2, fixture.getFragmentContext(), projectBatch1, projectBatch2);
    final LateralJoinBatch lateralJoinBatch1 =
        new LateralJoinBatch(ljPopConfig1, fixture.getFragmentContext(), incomingMockBatch, lateralJoinBatch2);

    // set pointer to Lateral in unnest
    unnestBatch1.setIncoming((LateralContract) lateralJoinBatch1);
    unnestBatch2.setIncoming((LateralContract) lateralJoinBatch2);

    // Simulate the pipeline by calling next on the incoming

    // results is an array ot batches, each batch being an array of output vectors.
    List<List<ValueVector> > resultList = new ArrayList<>();
    List<List<ValueVector> > results = null;
    int batchesProcessed = 0;
    try{
      try {
        while (!isTerminal(lateralJoinBatch1.next())) {
          if (lateralJoinBatch1.getRecordCount() > 0) {
            addBatchToResults(resultList, lateralJoinBatch1);
          }
          batchesProcessed++;
          if (batchesProcessed == execKill) {
            lateralJoinBatch1.getContext().getExecutorState().fail(new DrillException("Testing failure of execution."));
            lateralJoinBatch1.cancel();
          }
          // else nothing to do
        }
      } catch (UserException e) {
        throw e;
      } catch (Exception e) {
        throw new Exception ("Test failed to execute lateralJoinBatch.next() because: " + e.getMessage());
      }

      // Check results against baseline
      results = resultList;

      int batchIndex = 0;
      int vectorIndex = 0;
      //int valueIndex = 0;
      for ( List<ValueVector> batch: results) {
        int vectorCount= batch.size();
        if (vectorCount!= baseline[batchIndex].length+2) { // baseline does not include the original unnest column(s)
          fail("Test failed in validating unnest output. Batch column count mismatch.");
        }
        for (ValueVector vv : batch) {
          if(vv.getField().getName().equals("unnestColumn") || vv.getField().getName().equals("colB")) {
            continue; // skip the original input column
          }
          int valueCount = vv.getAccessor().getValueCount();
          if (valueCount!= baseline[batchIndex][vectorIndex].length) {
            fail("Test failed in validating unnest output. Value count mismatch in batch number " + (batchIndex+1) +""
                + ".");
          }

          for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
            if (vv instanceof MapVector) {
              if (!compareMapBaseline(baseline[batchIndex][vectorIndex][valueIndex], vv
                  .getAccessor()
                  .getObject(valueIndex))) {
                fail("Test failed in validating unnest(Map) output. Value mismatch");
              }
            } else if (vv instanceof VarCharVector) {
              Object val = vv.getAccessor().getObject(valueIndex);
              if (((String) baseline[batchIndex][vectorIndex][valueIndex]).compareTo(val.toString()) != 0) {
                fail("Test failed in validating unnest output. Value mismatch. Baseline value[]" + vectorIndex + "][" + valueIndex
                    + "]" + ": " + baseline[vectorIndex][valueIndex] + "   VV.getObject(valueIndex): " + val);
              }
            } else {
              Object val = vv.getAccessor().getObject(valueIndex);
              if (!baseline[batchIndex][vectorIndex][valueIndex].equals(val)) {
                fail("Test failed in validating unnest output. Value mismatch. Baseline value[" + vectorIndex + "][" + valueIndex
                    + "]" + ": "
                    + baseline[batchIndex][vectorIndex][valueIndex] + "   VV.getObject(valueIndex): " + val);
              }
            }
          }
          vectorIndex++;
        }
        vectorIndex=0;
        batchIndex++;
      }
    } catch (UserException e) {
      throw e; // Valid exception
    } catch (Exception e) {
      fail("Test failed. Exception : " + e.getMessage());
    } finally {
      // Close all the resources for this test case
      unnestBatch1.close();
      lateralJoinBatch1.close();
      unnestBatch2.close();
      lateralJoinBatch2.close();
      incomingMockBatch.close();

      if (results != null) {
        for (List<ValueVector> batch : results) {
          for (ValueVector vv : batch) {
            vv.clear();
          }
        }
      }
      for(RowSet.SingleRowSet rowSet: rowSets) {
        rowSet.clear();
      }
    }
  }

  @Test
  public void testNestedUnnestMapColumn() {

    Object[][] data = getMapData();

    // Create input schema
    TupleMetadata incomingSchema = getRepeatedMapSchema();
    TupleMetadata[] incomingSchemas = {incomingSchema, incomingSchema};

    Object[][][] baseline = getNestedMapBaseline();

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testNestedUnnest(incomingSchemas, iterOutcomes, 0, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }
  }
}

