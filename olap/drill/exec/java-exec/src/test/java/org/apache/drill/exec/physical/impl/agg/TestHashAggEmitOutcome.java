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
package org.apache.drill.exec.physical.impl.agg;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.impl.BaseTestOpBatchEmitOutcome;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.aggregate.HashAggBatch;
import org.apache.drill.exec.planner.physical.AggPrelBase;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.NONE;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK_NEW_SCHEMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

@Category(OperatorTest.class)
public class TestHashAggEmitOutcome extends BaseTestOpBatchEmitOutcome {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestHashAggEmitOutcome.class);


  /**
   *  A generic method to execute a Hash-Aggr emit test, based on the given parameters.
   *  Can take at most two generic input batches, and verify against at most two non-empty output
   *  batches (and unlimited number of input/output empty batches may be used)
   *
   *      This interface is a little ugly, because Java does not support simple initializations
   *      other than for arrays (e.g., no "structs" like in c++)
   *
   *   Input batch 1 is build in (see BaseTestOpBatchEmitOutcome.java)
   * @param inp2_1 - Input batch 2, first col (use null if not needed)
   * @param inp2_2 - input batch 2, second col
   * @param inp2_3 - Input batch 2, third col
   * @param inp3_1 - Input batch 3, first col (use null if not needed)
   * @param inp3_2 - input batch 3, second col
   * @param inp3_3 - input batch 3, third col
   * @param exp1_1 - First expected batch, col 1
   * @param exp1_2 - First expected batch, col 2
   * @param exp2_1 - Second expected batch, col 1
   * @param exp2_2 - Second expected batch, col 2
   * @param inpRowSet - Which input batches to use (the empty, i.e. 0, can be used multiple times)
   * @param inpOutcomes - Which input IterOutcomes to mark each input batch
   * @param outputRowCounts - expected number of rows, in each output batch
   * @param outputOutcomes - the expected output outcomes
   */
  private void testHashAggrEmit(int[] inp2_1, int[] inp2_2, String[] inp2_3,  // first input batch
                                int[] inp3_1, int[] inp3_2, String[] inp3_3,  // second input batch
                                String[] exp1_1, int[] exp1_2,            // first expected
                                String[] exp2_1, int[] exp2_2,            // second expected
                                int[] inpRowSet, RecordBatch.IterOutcome[] inpOutcomes,  // input batches + outcomes
                                List<Integer> outputRowCounts,  // output row counts per each out batch
                                List<RecordBatch.IterOutcome> outputOutcomes) // output outcomes
  {
    // First input batch
    RowSetBuilder builder2 = operatorFixture.rowSetBuilder(inputSchema);
    if ( inp2_1 != null ) {
      for (int i = 0; i < inp2_1.length; i++) {
        builder2 = builder2.addRow(inp2_1[i], inp2_2[i], inp2_3[i]);
      }
    }
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = builder2.build();

    // Second input batch
    RowSetBuilder builder3 = operatorFixture.rowSetBuilder(inputSchema);
    if ( inp3_1 != null ) {
      for (int i = 0; i < inp3_1.length; i++) {
        builder3 = builder3.addRow(inp3_1[i], inp3_2[i], inp3_3[i]);
      }
    }
    final RowSet.SingleRowSet nonEmptyInputRowSet3 = builder3.build();

    final TupleMetadata resultSchema = new SchemaBuilder()
      .add("name", TypeProtos.MinorType.VARCHAR)
      .addNullable("total_sum", TypeProtos.MinorType.BIGINT)
      .buildSchema();

    // First expected batch
    RowSetBuilder expectedBuilder1 = operatorFixture.rowSetBuilder(resultSchema);
    if ( exp1_1 != null ) {
      for ( int i = 0; i < exp1_1.length; i++ ) {
        expectedBuilder1 = expectedBuilder1.addRow(exp1_1[i], (long)exp1_2[i]);
      }
    }
    final RowSet.SingleRowSet expectedRowSet1 = expectedBuilder1.build();

    // Second expected batch
    RowSetBuilder expectedBuilder2 = operatorFixture.rowSetBuilder(resultSchema);
    if ( exp2_1 != null ) {
      for ( int i = 0; i < exp2_1.length; i++ ) {
        expectedBuilder2 = expectedBuilder2.addRow(exp2_1[i], (long)exp2_2[i]);
      }
    }
    final RowSet.SingleRowSet expectedRowSet2 = expectedBuilder2.build();

    // Add the input batches, in the order/type given
    for ( int inp : inpRowSet) {
      switch ( inp ) {
        case 0: inputContainer.add(emptyInputRowSet.container());
          break;
        case 1: inputContainer.add(nonEmptyInputRowSet.container());
          break;
        case 2: inputContainer.add(nonEmptyInputRowSet2.container());
          break;
        case 3: inputContainer.add(nonEmptyInputRowSet3.container());
          break;
        default:
          fail();
      }
    }
    // build the outcomes
    inputOutcomes.addAll(Arrays.asList(inpOutcomes));

    //
    //  Build the Hash Agg Batch operator
    //
    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final HashAggregate hashAggrConfig = new HashAggregate(null, AggPrelBase.OperatorPhase.PHASE_1of1,
      parseExprs("name_left", "name"),
      parseExprs("sum(id_left+cost_left)", "total_sum"),
      1.0f);

    final HashAggBatch haBatch = new HashAggBatch(hashAggrConfig, mockInputBatch,
      operatorFixture.getFragmentContext());

    //
    //  Iterate thru the next batches, and verify expected outcomes
    //
    assertEquals(outputRowCounts.size(), outputOutcomes.size());
    boolean firstOne = true;

    for (int ind = 0; ind < outputOutcomes.size(); ind++) {
      RecordBatch.IterOutcome expOut = outputOutcomes.get(ind);
      assertSame(expOut, haBatch.next());
      if (expOut == NONE) {
        break;
      } // done
      RowSet actualRowSet = DirectRowSet.fromContainer(haBatch.getContainer());
      int expectedSize = outputRowCounts.get(ind);
      // System.out.println(expectedSize);
      if (0 == expectedSize) {
        assertEquals(expectedSize, haBatch.getRecordCount());
      } else if (firstOne) {
        firstOne = false;
        new RowSetComparison(expectedRowSet1).verify(actualRowSet);
      } else {
        new RowSetComparison(expectedRowSet2).verify(actualRowSet);
      }
    }

    // Release memory for row sets
    nonEmptyInputRowSet2.clear();
    nonEmptyInputRowSet3.clear();
    expectedRowSet2.clear();
    expectedRowSet1.clear();
  }

  ////////////////////////////////////////////////
  //
  //     T H E    U N I T   T E S T S
  //
  ////////////////////////////////////////////////

  /**
   * Test receiving just a single input batch, empty
   */
  @Test
  public void testHashAggrWithEmptyDataSet() {
    int[] inpRowSet = {0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA};

    List<Integer> outputRowCounts = Arrays.asList(0, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, NONE);

    testHashAggrEmit(null, null, null, null, null, null, null, null,
      null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   * Verifies that if HashAggBatch receives empty batches with OK_NEW_SCHEMA and EMIT outcome then it correctly produces
   * empty batches as output. First empty batch will be with OK_NEW_SCHEMA and second will be with EMIT outcome.
   */
  @Test
  public void testHashAggrEmptyBatchEmitOutcome() {
    int[] inpRowSet = {0, 0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 0, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, NONE);

    testHashAggrEmit(null, null, null, null, null, null, null, null,
      null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   * Verifies that if HashAgg receives a RecordBatch with EMIT outcome post build schema phase then it produces
   * output for those input batch correctly. The first output batch will always be returned with OK_NEW_SCHEMA
   * outcome followed by EMIT with empty batch.
   */
  @Test
  public void testHashAggrNonEmptyBatchEmitOutcome() {
    int[] inp2_1 = {2, 2, 13, 13, 4};
    int[] inp2_2 = {20, 20, 130, 130, 40};
    String[] inp2_3 = {"item2", "item2", "item13", "item13", "item4"};

    String[] exp1_1 = {"item2", "item13", "item4"};
    int[] exp1_2 = {44, 286, 44};

    int[] inpRowSet = {0, 2};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 3, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, null, null, null,
      exp1_1, exp1_2, null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   *
   */
  @Test
  public void testHashAggrEmptyBatchFollowedByNonEmptyBatchEmitOutcome() {
    int[] inp2_1 = {2, 13, 4, 0, 0, 0};
    int[] inp2_2 = {20, 130, 40, 2000, 1300, 4000};
    String[] inp2_3 = {"item2", "item13", "item4", "item2", "item13", "item4"};

    String[] exp1_1 = {"item2", "item13", "item4"};
    int[] exp1_2 = {2022, 1443, 4044};

    int[] inpRowSet = {0, 0, 2};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, EMIT, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 0, 3, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, EMIT, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, null, null, null,
      exp1_1, exp1_2, null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   *
   */
  @Test
  public void testHashAggrMultipleEmptyBatchFollowedByNonEmptyBatchEmitOutcome() {
    int[] inp2_1 = {2, 13, 4, 0, 1, 0, 1};
    int[] inp2_2 = {20, 130, 40, 0, 11000, 0, 33000};
    String[] inp2_3 = {"item2", "item13", "item4", "item2", "item2", "item13", "item13"};

    String[] exp1_1 = {"item2", "item13", "item4"};
    int[] exp1_2 = {11023, 33144, 44};

    int[] inpRowSet = {0, 0, 0, 0, 2};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, EMIT, EMIT, EMIT, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 0, 0, 0, 3, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, EMIT, EMIT, EMIT, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, null, null, null,
      exp1_1, exp1_2, null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   * Verifies that if HashAggr receives multiple non-empty record batch with EMIT outcome in between then it produces
   * output for those input batch correctly. In this case it receives first non-empty batch with OK_NEW_SCHEMA in
   * buildSchema phase followed by an empty batch with EMIT outcome. For this combination it produces output for the
   * record received so far along with EMIT outcome. Then it receives second non-empty batch with OK outcome and
   * produces output for it differently.
   */
  @Test
  public void testHashAgrResetsAfterFirstEmitOutcome() {
    int[] inp2_1 = {2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2};
    int[] inp2_2 = {20, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 20};
    String[] inp2_3 = {"item2", "item3", "item3", "item3", "item3", "item3", "item3", "item3", "item3", "item3", "item3", "item2"};

    String[] exp1_1 = {"item1"};
    int[] exp1_2 = {11};

    String[] exp2_1 = {"item2", "item3"};
    int[] exp2_2 = {44, 330};

    int[] inpRowSet = {1, 0, 2, 0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, EMIT, OK, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 1, 2, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, EMIT, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, null, null, null,
      exp1_1, exp1_2, exp2_1, exp2_2, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   * Verifies HashAggr correctness for the case where it receives non-empty batch in build schema phase followed by
   * empty batchs with OK and EMIT outcomes.
   */
  @Test
  public void testHashAggr_NonEmptyFirst_EmptyOKEmitOutcome() {

    String[] exp1_1 = {"item1"};
    int[] exp1_2 = {11};

    int[] inpRowSet = {1, 0, 0, 0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, OK, EMIT, NONE};

    List<Integer> outputRowCounts = Arrays.asList(0, 1, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, NONE);

    testHashAggrEmit(null, null, null, null, null, null,
      exp1_1, exp1_2, null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   * Verifies that if HashAggr receives multiple non-empty record batches with EMIT outcome in between then it produces
   * output for those input batch correctly. In this case it receives first non-empty batch with OK_NEW_SCHEMA in
   * buildSchema phase followed by an empty batch with EMIT outcome. For this combination it produces output for the
   * record received so far along with EMIT outcome. Then it receives second non-empty batch with OK outcome and
   * produces output for it differently.
   */
  @Test
  public void testHashAggrMultipleOutputBatch() {
    int[] inp2_1 = {4, 2, 5, 3, 5, 4};
    int[] inp2_2 = {40, 20, 50, 30, 50, 40};
    String[] inp2_3 = {"item4", "item2", "item5", "item3", "item5", "item4"};

    String[] exp1_1 = {"item1"};
    int[] exp1_2 = {11};

    String[] exp2_1 = {"item4", "item2", "item5", "item3"};
    int[] exp2_2 = {88, 22, 110, 33};

    int[] inpRowSet = {1, 0, 2};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, EMIT, OK};

    List<Integer> outputRowCounts = Arrays.asList(0, 1, 4, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, OK, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, null, null, null,
      exp1_1, exp1_2, exp2_1, exp2_2, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   *
   */
  @Test
  public void testHashAggrMultipleEMITOutcome() {
    int[] inp2_1 = {2, 3};
    int[] inp2_2 = {20, 30};
    String[] inp2_3 = {"item2", "item3"};

    String[] exp1_1 = {"item1"};
    int[] exp1_2 = {11};

    String[] exp2_1 = {"item2", "item3"};
    int[] exp2_2 = {22, 33};

    int[] inpRowSet = {1, 0, 2, 0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, EMIT, EMIT, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 1, 2, 0, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, EMIT, EMIT, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, null, null, null,
      exp1_1, exp1_2, exp2_1, exp2_2, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   *
   */
  @Test
  public void testHashAggrMultipleInputToSingleOutputBatch() {
    int[] inp2_1 = {2};
    int[] inp2_2 = {20};
    String[] inp2_3 = {"item2"};

    String[] exp1_1 = {"item1", "item2"};
    int[] exp1_2 = {   11,    22};

    int[] inpRowSet = {1, 0, 2, 0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, OK, OK, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 2, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, null, null, null,
      exp1_1, exp1_2, null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   *
   */
  @Test
  public void testHashAggrMultipleInputToMultipleOutputBatch() {
    int[] inp2_1 = {7, 2, 7, 3};
    int[] inp2_2 = {70, 20, 70, 33};
    String[] inp2_3 = {"item7", "item1", "item7", "item3"};

    int[] inp3_1 = {17, 7, 3, 13, 9, 13};
    int[] inp3_2 = {170, 71, 30, 130, 123, 130};
    String[] inp3_3 = {"item17", "item7", "item3", "item13", "item3", "item13"};

    String[] exp1_1 = {"item1", "item7", "item3"};
    int[] exp1_2 = {33, 154, 36};

    String[] exp2_1 = {"item17", "item7", "item3", "item13"};
    int[] exp2_2 = {187, 78, 165, 286};

    int[] inpRowSet = {1, 0, 2, 0, 3, 0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, OK, EMIT, OK, OK, EMIT};

    List<Integer> outputRowCounts = Arrays.asList(0, 3, 4, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, EMIT, EMIT, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, inp3_1, inp3_2, inp3_3,
      exp1_1, exp1_2, exp2_1, exp2_2, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

  /**
   * **************************************************************************************
   *      Test validating a regular HashAggr behavior with no EMIT outcome input
   * **************************************************************************************
   */
  @Test
  public void testHashAggr_WithEmptyNonEmptyBatchesAndOKOutcome() {
    int[] inp2_1 = {2, 7, 3, 13, 13, 13};
    int[] inp2_2 = {20, 70, 33, 130, 130, 130};
    String[] inp2_3 = {"item1", "item7", "item3", "item13", "item13", "item13"};

    int[] inp3_1 = {17, 23, 130, 0};
    int[] inp3_2 = {170, 230, 1300, 0};
    String[] inp3_3 = {"item7", "item23", "item130", "item130"};

    String[] exp1_1 = {"item1", "item7", "item3", "item13", "item23", "item130"};
    int[] exp1_2 = {33, 264, 36, 429, 253, 1430};

    int[] inpRowSet = {1, 0, 2, 0, 3, 0};
    RecordBatch.IterOutcome[] inpOutcomes = {OK_NEW_SCHEMA, OK, OK, OK, OK, OK};

    List<Integer> outputRowCounts = Arrays.asList(0, 6, 0);
    List<RecordBatch.IterOutcome> outputOutcomes = Arrays.asList(OK_NEW_SCHEMA, OK, NONE);

    testHashAggrEmit(inp2_1, inp2_2, inp2_3, inp3_1, inp3_2, inp3_3, exp1_1, exp1_2,
      null, null, inpRowSet, inpOutcomes, outputRowCounts, outputOutcomes);
  }

}
