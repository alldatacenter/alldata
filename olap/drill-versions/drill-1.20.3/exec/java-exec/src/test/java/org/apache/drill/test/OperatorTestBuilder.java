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

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.svremover.Copier;
import org.apache.drill.exec.physical.impl.svremover.GenericCopier;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VariableWidthVector;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OperatorTestBuilder {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperatorTestBuilder.class);

  private final List<RowSet> expectedResults = new ArrayList<>();
  private final List<MockRecordBatch> upstreamBatches = new ArrayList<>();
  private PhysicalOpUnitTestBase physicalOpUnitTestBase;

  private PhysicalOperator physicalOperator;
  private long initReservation = AbstractBase.INIT_ALLOCATION;
  private long maxAllocation = AbstractBase.MAX_ALLOCATION;
  private Optional<Integer> expectedNumBatchesOpt = Optional.empty();
  private Optional<Integer> expectedTotalRowsOpt = Optional.empty();
  private boolean combineOutputBatches;
  private boolean unordered;

  public OperatorTestBuilder(PhysicalOpUnitTestBase physicalOpUnitTestBase) {
    this.physicalOpUnitTestBase = physicalOpUnitTestBase;
  }

  @SuppressWarnings("unchecked")
  public void go() throws Exception {
    final List<RowSet> actualResults = new ArrayList<>();
    CloseableRecordBatch testOperator = null;

    try {
      validate();
      int expectedNumBatches = expectedNumBatchesOpt.orElse(expectedResults.size());
      physicalOpUnitTestBase.mockOpContext(physicalOperator, initReservation, maxAllocation);

      final BatchCreator<PhysicalOperator> opCreator = (BatchCreator<PhysicalOperator>) physicalOpUnitTestBase.opCreatorReg.getOperatorCreator(physicalOperator.getClass());
      testOperator = opCreator.getBatch(physicalOpUnitTestBase.fragContext, physicalOperator, (List)upstreamBatches);

      batchIterator: for (int batchIndex = 0;; batchIndex++) {
        final RecordBatch.IterOutcome outcome = testOperator.next();

        switch (outcome) {
          case NONE:
            if (!combineOutputBatches) {
              Assert.assertEquals(expectedNumBatches, batchIndex);
            }
            // We are done iterating over batches. Now we need to compare them.
            break batchIterator;
          case OK_NEW_SCHEMA:
            boolean skip = true;

            try {
              skip = testOperator.getContainer().getRecordCount() == 0;
            } catch (IllegalStateException e) {
              // We should skip this batch in this case. It means no data was included with the okay schema
            } finally {
              if (skip) {
                batchIndex--;
                break;
              }
            }
          case OK:
            if (!combineOutputBatches && batchIndex >= expectedNumBatches) {
              testOperator.getContainer().clear();
              Assert.fail("More batches received than expected.");
            } else {
              final boolean hasSelectionVector = testOperator.getSchema().getSelectionVectorMode().hasSelectionVector;
              final VectorContainer container = testOperator.getContainer();

              if (hasSelectionVector) {
                throw new UnsupportedOperationException("Implement DRILL-6698");
              } else {
                actualResults.add(DirectRowSet.fromContainer(container));
              }
              break;
            }
          default:
            throw new UnsupportedOperationException("Can't handle this yet");
        }
      }

      int actualTotalRows = actualResults.stream()
        .mapToInt(RowSet::rowCount)
        .reduce(Integer::sum)
        .orElse(0);

      if (expectedResults.isEmpty()) {
        Assert.assertEquals((int) expectedTotalRowsOpt.orElse(0), actualTotalRows);
        // We are done, we don't have any expected result to compare
        return;
      }

      if (combineOutputBatches) {
        final RowSet expectedBatch = expectedResults.get(0);
        final RowSet actualBatch = DirectRowSet.fromSchema(
          physicalOpUnitTestBase.operatorFixture.allocator, actualResults.get(0).container().getSchema());
        final VectorContainer actualBatchContainer = actualBatch.container();
        actualBatchContainer.setRecordCount(0);

        final int numColumns = expectedBatch.schema().size();
        List<MutableInt> totalBytesPerColumn = new ArrayList<>();

        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
          totalBytesPerColumn.add(new MutableInt());
        }

        // Get column sizes for each result batch

        final List<List<RecordBatchSizer.ColumnSize>> columnSizesPerBatch = actualResults.stream().map(rowSet -> {
          switch (rowSet.indirectionType()) {
            case NONE:
              return new RecordBatchSizer(rowSet.container()).columnsList();
            default:
              throw new UnsupportedOperationException("Implement DRILL-6698");
          }
        }).collect(Collectors.toList());

        // Get total bytes per column

        for (List<RecordBatchSizer.ColumnSize> columnSizes: columnSizesPerBatch) {
          for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
            final MutableInt totalBytes = totalBytesPerColumn.get(columnIndex);
            final RecordBatchSizer.ColumnSize columnSize = columnSizes.get(columnIndex);
            totalBytes.add(columnSize.getTotalDataSize());
          }
        }

        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
          final ValueVector valueVector = actualBatchContainer
            .getValueVector(columnIndex)
            .getValueVector();

          if (valueVector instanceof FixedWidthVector) {
            ((FixedWidthVector) valueVector).allocateNew(actualTotalRows);
          } else if (valueVector instanceof VariableWidthVector) {
            final MutableInt totalBytes = totalBytesPerColumn.get(columnIndex);
            ((VariableWidthVector) valueVector).allocateNew(totalBytes.getValue(), actualTotalRows);
          } else {
            throw new UnsupportedOperationException();
          }
        }

        try {
          int currentIndex = 0;

          for (RowSet actualRowSet: actualResults) {
            final Copier copier;
            final VectorContainer rowSetContainer = actualRowSet.container();
            rowSetContainer.setRecordCount(actualRowSet.rowCount());

            switch (actualRowSet.indirectionType()) {
              case NONE:
                copier = new GenericCopier();
                break;
              default:
                throw new UnsupportedOperationException("Implement DRILL-6698");
            }

            copier.setup(rowSetContainer, actualBatchContainer);
            copier.appendRecords(currentIndex, actualRowSet.rowCount());
            currentIndex += actualRowSet.rowCount();

            verify(expectedBatch, actualBatch);
          }
        } finally {
          actualBatch.clear();
        }
      } else {
        // Compare expected and actual results
        for (int batchIndex = 0; batchIndex < expectedNumBatches; batchIndex++) {
          final RowSet expectedBatch = expectedResults.get(batchIndex);
          final RowSet actualBatch = actualResults.get(batchIndex);

          verify(expectedBatch, actualBatch);
        }
      }
    } finally {
      // free resources

      if (testOperator != null) {
        testOperator.close();
      }

      actualResults.forEach(rowSet -> rowSet.clear());

      if (expectedResults != null) {
        expectedResults.forEach(rowSet -> rowSet.clear());
      }

      upstreamBatches.forEach(rowSetBatch -> {
        try {
          rowSetBatch.close();
        } catch (Exception e) {
          logger.error("Error while closing RowSetBatch", e);
        }
      });
    }
  }

  private void verify(final RowSet expectedBatch, final RowSet actualBatch) {
    if (unordered) {
      new RowSetComparison(expectedBatch).unorderedVerify(actualBatch);
    } else {
      new RowSetComparison(expectedBatch).verify(actualBatch);
    }
  }

  /**
   * Make sure the inputs are valid.
   */
  private void validate() {
    if (combineOutputBatches) {
      Preconditions.checkArgument(expectedResults.isEmpty() || expectedResults.size() == 1,
        "The number of expected result batches needs to be zero or one when combining output batches");
      Preconditions.checkArgument((expectedResults.isEmpty() && (!expectedNumBatchesOpt.isPresent() && expectedTotalRowsOpt.isPresent())) ||
          (!expectedResults.isEmpty() && (!expectedNumBatchesOpt.isPresent() && !expectedTotalRowsOpt.isPresent())),
        "When definig expectedResults, you cannot define expectedNumBatch or expectedTotalRows and vice versa");
    } else {
      Preconditions.checkArgument((expectedResults.isEmpty() && (expectedNumBatchesOpt.isPresent() || expectedTotalRowsOpt.isPresent())) ||
          (!expectedResults.isEmpty() && (!expectedNumBatchesOpt.isPresent() && !expectedTotalRowsOpt.isPresent())),
        "When definig expectedResults, you cannot define expectedNumBatch or expectedTotalRows and vice versa");
    }
  }

  public OperatorTestBuilder physicalOperator(PhysicalOperator batch) {
    this.physicalOperator = batch;
    return this;
  }

  public OperatorTestBuilder initReservation(long initReservation) {
    this.initReservation = initReservation;
    return this;
  }

  public OperatorTestBuilder maxAllocation(long maxAllocation) {
    this.maxAllocation = maxAllocation;
    return this;
  }

  public OperatorTestBuilder expectedNumBatches(int expectedNumBatches) {
    this.expectedNumBatchesOpt = Optional.of(expectedNumBatches);
    return this;
  }

  public OperatorTestBuilder expectedTotalRows(int expectedTotalRows) {
    this.expectedTotalRowsOpt = Optional.of(expectedTotalRows);
    return this;
  }

  /**
   * Combines all the batches output by the operator into a single batch for comparison.
   * @return This {@link OperatorTestBuilder}.
   */
  public OperatorTestBuilder combineOutputBatches() {
    combineOutputBatches = true;
    return this;
  }

  public OperatorTestBuilder unordered() {
    unordered = true;
    return this;
  }

  public OperatorTestBuilder addUpstreamBatch(final MockRecordBatch mockRecordBatch) {
    Preconditions.checkNotNull(mockRecordBatch);
    upstreamBatches.add(mockRecordBatch);
    return this;
  }

  public OperatorTestBuilder addExpectedResult(final RowSet rowSet) {
    Preconditions.checkNotNull(rowSet);
    expectedResults.add(rowSet);
    return this;
  }
}
