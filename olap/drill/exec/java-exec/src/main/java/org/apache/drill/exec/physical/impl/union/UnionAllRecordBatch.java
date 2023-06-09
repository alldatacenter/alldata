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
package org.apache.drill.exec.physical.impl.union;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.calcite.util.Pair;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.ValueVectorWriteExpression;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.UnionAll;
import org.apache.drill.exec.record.AbstractBinaryRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.JoinBatchMemoryManager;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchMemoryManager;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessibleUtilities;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.resolver.TypeCastRules;
import org.apache.drill.exec.util.VectorUtil;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchIOType;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnionAllRecordBatch extends AbstractBinaryRecordBatch<UnionAll> {
  private static final Logger logger = LoggerFactory.getLogger(UnionAllRecordBatch.class);

  private final SchemaChangeCallBack callBack = new SchemaChangeCallBack();
  private UnionAller unionall;
  private final List<TransferPair> transfers = new ArrayList<>();
  private final List<ValueVector> allocationVectors = new ArrayList<>();
  private int recordCount;
  private Iterator<Pair<IterOutcome, BatchStatusWrappper>> unionInputIterator;

  public UnionAllRecordBatch(UnionAll config, List<RecordBatch> children, FragmentContext context) throws OutOfMemoryException {
    super(config, context, true, children.get(0), children.get(1));

    // get the output batch size from config.
    int configuredBatchSize = (int) context.getOptions().getOption(ExecConstants.OUTPUT_BATCH_SIZE_VALIDATOR);
    batchMemoryManager = new RecordBatchMemoryManager(numInputs, configuredBatchSize);

    RecordBatchStats.printConfiguredBatchSize(getRecordBatchStatsContext(),
      configuredBatchSize);
  }

  @Override
  protected void buildSchema() {
    if (! prefetchFirstBatchFromBothSides()) {
      state = BatchState.DONE;
      return;
    }

    unionInputIterator = new UnionInputIterator(leftUpstream, left, rightUpstream, right);

    if (leftUpstream == IterOutcome.NONE && rightUpstream == IterOutcome.OK_NEW_SCHEMA) {
      inferOutputFieldsOneSide(right.getSchema());
    } else if (rightUpstream == IterOutcome.NONE && leftUpstream == IterOutcome.OK_NEW_SCHEMA) {
      inferOutputFieldsOneSide((left.getSchema()));
    } else if (leftUpstream == IterOutcome.OK_NEW_SCHEMA && rightUpstream == IterOutcome.OK_NEW_SCHEMA) {
      inferOutputFieldsBothSide(left.getSchema(), right.getSchema());
    }

    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);

    VectorAccessibleUtilities.allocateVectors(container, 0);
    VectorAccessibleUtilities.setValueCount(container, 0);
    container.setRecordCount(0);
  }

  @Override
  public IterOutcome innerNext() {
    while (true) {
      if (!unionInputIterator.hasNext()) {
        return IterOutcome.NONE;
      }

      Pair<IterOutcome, BatchStatusWrappper> nextBatch = unionInputIterator.next();
      IterOutcome upstream = nextBatch.left;
      BatchStatusWrappper batchStatus = nextBatch.right;

      switch (upstream) {
      case NONE:
        return upstream;
      case OK_NEW_SCHEMA:
        return doWork(batchStatus, true);
      case OK:
        // skip batches with same schema as the previous one yet having 0 row.
        if (batchStatus.batch.getRecordCount() == 0) {
          VectorAccessibleUtilities.clear(batchStatus.batch);
          continue;
        }
        return doWork(batchStatus, false);
      default:
        throw new IllegalStateException(String.format("Unknown state %s.", upstream));
      }
    }
  }

  @Override
  public int getRecordCount() {
    return recordCount;
  }

  private IterOutcome doWork(BatchStatusWrappper batchStatus, boolean newSchema) {
    Preconditions.checkArgument(batchStatus.batch.getSchema().getFieldCount() == container.getSchema().getFieldCount(),
        "Input batch and output batch have different field counthas!");

    if (newSchema) {
      createUnionAller(batchStatus.batch);
    }

    // Get number of records to include in the batch.
    final int recordsToProcess = Math.min(batchMemoryManager.getOutputRowCount(), batchStatus.getRemainingRecords());

    container.zeroVectors();
    batchMemoryManager.allocateVectors(allocationVectors, recordsToProcess);
    recordCount = unionall.unionRecords(batchStatus.recordsProcessed, recordsToProcess, 0);
    VectorUtil.setValueCount(allocationVectors, recordCount);
    container.setRecordCount(recordCount);

    // save number of records processed so far in batch status.
    batchStatus.recordsProcessed += recordCount;
    batchMemoryManager.updateOutgoingStats(recordCount);

    RecordBatchStats.logRecordBatchStats(RecordBatchIOType.OUTPUT, this, getRecordBatchStatsContext());

    if (callBack.getSchemaChangedAndReset()) {
      return IterOutcome.OK_NEW_SCHEMA;
    } else {
      return IterOutcome.OK;
    }
  }

  private void createUnionAller(RecordBatch inputBatch) {
    transfers.clear();
    allocationVectors.clear();

    ClassGenerator<UnionAller> cg = CodeGenerator.getRoot(UnionAller.TEMPLATE_DEFINITION, context.getOptions());
    cg.getCodeGenerator().plainJavaCapable(true);
    // cg.getCodeGenerator().saveCodeForDebugging(true);

    int index = 0;
    for(VectorWrapper<?> vw : inputBatch) {
      ValueVector vvIn = vw.getValueVector();
      ValueVector vvOut = container.getValueVector(index).getValueVector();

      MaterializedField inField = vvIn.getField();
      MaterializedField outputField = vvOut.getField();

      ErrorCollector collector = new ErrorCollectorImpl();
      // According to input data names, MinorTypes, DataModes, choose to
      // transfer directly,
      // rename columns or
      // cast data types (MinorType or DataMode)
      if (areAssignableTypes(inField.getType(), outputField.getType())) {
        // Transfer column
        TransferPair tp = vvIn.makeTransferPair(vvOut);
        transfers.add(tp);
      } else if (inField.getType().getMinorType() == TypeProtos.MinorType.NULL) {
        index++;
        continue;
      } else { // Copy data in order to rename the column
        SchemaPath inputPath = SchemaPath.getSimplePath(inField.getName());

        LogicalExpression expr = ExpressionTreeMaterializer.materialize(inputPath, inputBatch, collector, context.getFunctionRegistry());
        collector.reportErrors(logger);

        // If the inputs' DataMode is required and the outputs' DataMode is not required
        // cast to the one with the least restriction
        if (inField.getType().getMode() == TypeProtos.DataMode.REQUIRED
            && outputField.getType().getMode() != TypeProtos.DataMode.REQUIRED) {
          expr = ExpressionTreeMaterializer.convertToNullableType(expr, inField.getType().getMinorType(), context.getFunctionRegistry(), collector);
          collector.reportErrors(logger);
        }

        // If two inputs' MinorTypes are different or types are decimal with different scales,
        // inserts a cast before the Union operation
        if (isCastRequired(inField.getType(), outputField.getType())) {
          expr = ExpressionTreeMaterializer.addCastExpression(expr, outputField.getType(), context.getFunctionRegistry(), collector);
          collector.reportErrors(logger);
        }

        TypedFieldId fid = container.getValueVectorId(SchemaPath.getSimplePath(outputField.getName()));

        boolean useSetSafe = !(vvOut instanceof FixedWidthVector);
        ValueVectorWriteExpression write = new ValueVectorWriteExpression(fid, expr, useSetSafe);
        cg.addExpr(write);

        allocationVectors.add(vvOut);
      }
      ++index;
    }

    unionall = context.getImplementationClass(cg.getCodeGenerator());
    try {
      unionall.setup(context, inputBatch, this, transfers);
    } catch (SchemaChangeException e) {
      throw schemaChangeException(e, logger);
    }
  }

  /**
   * Checks whether cast should be added for transitioning values from the one type to another one.
   * {@code true} will be returned if minor types differ or scales differ for the decimal data type.
   *
   * @param first  first type
   * @param second second type
   * @return {@code true} if cast should be added, {@code false} otherwise
   */
  private boolean isCastRequired(MajorType first, MajorType second) {
    return first.getMinorType() != second.getMinorType()
        || (Types.areDecimalTypes(second.getMinorType(), first.getMinorType())
            && second.getScale() != first.getScale());
  }

  /**
   * Checks whether data may be transitioned between specified types without using casts.
   * {@code true} will be returned if minor types and data modes are the same for non-decimal data types,
   * or if minor types, data modes and scales are the same for decimal data types.
   *
   * @param first  first type
   * @param second second type
   * @return {@code true} if data may be transitioned between specified types without using casts,
   * {@code false} otherwise
   */
  private boolean areAssignableTypes(MajorType first, MajorType second) {
    boolean areDecimalTypes = Types.areDecimalTypes(first.getMinorType(), second.getMinorType());

    return Types.isSameTypeAndMode(first, second)
        && second.getMinorType() != TypeProtos.MinorType.MAP // Per DRILL-5521, existing bug for map transfer
        && (!areDecimalTypes || first.getScale() == second.getScale()); // scale should match for decimal data types
  }

  // The output table's column names always follow the left table,
  // where the output type is chosen based on DRILL's implicit casting rules
  private void inferOutputFieldsBothSide(final BatchSchema leftSchema, final BatchSchema rightSchema) {
    final Iterator<MaterializedField> leftIter = leftSchema.iterator();
    final Iterator<MaterializedField> rightIter = rightSchema.iterator();

    int index = 1;
    while (leftIter.hasNext() && rightIter.hasNext()) {
      MaterializedField leftField  = leftIter.next();
      MaterializedField rightField = rightIter.next();

      if (Types.isSameTypeAndMode(leftField.getType(), rightField.getType())) {
        MajorType.Builder builder = MajorType.newBuilder()
            .setMinorType(leftField.getType().getMinorType())
            .setMode(leftField.getDataMode());
        builder = Types.calculateTypePrecisionAndScale(leftField.getType(), rightField.getType(), builder);
        container.addOrGet(MaterializedField.create(leftField.getName(), builder.build()), callBack);
      } else if (Types.isUntypedNull(rightField.getType())) {
        container.addOrGet(leftField, callBack);
      } else if (Types.isUntypedNull(leftField.getType())) {
        container.addOrGet(MaterializedField.create(leftField.getName(), rightField.getType()), callBack);
      } else {
        // If the output type is not the same,
        // cast the column of one of the table to a data type which is the Least Restrictive
        MajorType.Builder builder = MajorType.newBuilder();
        if (leftField.getType().getMinorType() == rightField.getType().getMinorType()) {
          builder.setMinorType(leftField.getType().getMinorType());
          builder = Types.calculateTypePrecisionAndScale(leftField.getType(), rightField.getType(), builder);
        } else {
          List<TypeProtos.MinorType> types = Lists.newLinkedList();
          types.add(leftField.getType().getMinorType());
          types.add(rightField.getType().getMinorType());
          TypeProtos.MinorType outputMinorType = TypeCastRules.getLeastRestrictiveType(types);
          if (outputMinorType == null) {
            throw new DrillRuntimeException("Type mismatch between " + leftField.getType().getMinorType().toString() +
                " on the left side and " + rightField.getType().getMinorType().toString() +
                " on the right side in column " + index + " of UNION ALL");
          }
          builder.setMinorType(outputMinorType);
        }

        // The output data mode should be as flexible as the more flexible one from the two input tables
        List<TypeProtos.DataMode> dataModes = Lists.newLinkedList();
        dataModes.add(leftField.getType().getMode());
        dataModes.add(rightField.getType().getMode());
        builder.setMode(TypeCastRules.getLeastRestrictiveDataMode(dataModes));

        container.addOrGet(MaterializedField.create(leftField.getName(), builder.build()), callBack);
      }
      ++index;
    }

    assert !leftIter.hasNext() && ! rightIter.hasNext() :
      "Mismatch of column count should have been detected when validating sqlNode at planning";
  }

  private void inferOutputFieldsOneSide(final BatchSchema schema) {
    for (MaterializedField field : schema) {
      container.addOrGet(field, callBack);
    }
  }

  private class BatchStatusWrappper {
    boolean prefetched;
    final RecordBatch batch;
    final int inputIndex;
    final IterOutcome outcome;
    int recordsProcessed;
    int totalRecordsToProcess;

    BatchStatusWrappper(boolean prefetched, IterOutcome outcome, RecordBatch batch, int inputIndex) {
      this.prefetched = prefetched;
      this.outcome = outcome;
      this.batch = batch;
      this.inputIndex = inputIndex;
      this.totalRecordsToProcess = batch.getRecordCount();
      this.recordsProcessed = 0;
    }

    public int getRemainingRecords() {
      return (totalRecordsToProcess - recordsProcessed);
    }
  }

  private class UnionInputIterator implements Iterator<Pair<IterOutcome, BatchStatusWrappper>> {
    private final Stack<BatchStatusWrappper> batchStatusStack = new Stack<>();

    UnionInputIterator(IterOutcome leftOutCome, RecordBatch left, IterOutcome rightOutCome, RecordBatch right) {
      if (rightOutCome == IterOutcome.OK_NEW_SCHEMA) {
        batchStatusStack.push(new BatchStatusWrappper(true, IterOutcome.OK_NEW_SCHEMA, right, 1));
      }

      if (leftOutCome == IterOutcome.OK_NEW_SCHEMA) {
        batchStatusStack.push(new BatchStatusWrappper(true, IterOutcome.OK_NEW_SCHEMA, left, 0));
      }
    }

    @Override
    public boolean hasNext() {
      return ! batchStatusStack.isEmpty();
    }

    @Override
    public Pair<IterOutcome, BatchStatusWrappper> next() {
      while (!batchStatusStack.isEmpty()) {
        BatchStatusWrappper topStatus = batchStatusStack.peek();

        if (topStatus.prefetched) {
          topStatus.prefetched = false;
          batchMemoryManager.update(topStatus.batch, topStatus.inputIndex);
          RecordBatchStats.logRecordBatchStats(topStatus.inputIndex == 0 ? RecordBatchIOType.INPUT_LEFT : RecordBatchIOType.INPUT_RIGHT,
            batchMemoryManager.getRecordBatchSizer(topStatus.inputIndex),
            getRecordBatchStatsContext());
          return Pair.of(topStatus.outcome, topStatus);
        } else {

          // If we have more records to process, just return the top batch.
          if (topStatus.getRemainingRecords() > 0) {
            return Pair.of(IterOutcome.OK, topStatus);
          }

          IterOutcome outcome = UnionAllRecordBatch.this.next(topStatus.inputIndex, topStatus.batch);

          switch (outcome) {
          case OK:
          case OK_NEW_SCHEMA:
            // since we just read a new batch, update memory manager and initialize batch stats.
            topStatus.recordsProcessed = 0;
            topStatus.totalRecordsToProcess = topStatus.batch.getRecordCount();
            batchMemoryManager.update(topStatus.batch, topStatus.inputIndex);
            RecordBatchStats.logRecordBatchStats(topStatus.inputIndex == 0 ? RecordBatchIOType.INPUT_LEFT : RecordBatchIOType.INPUT_RIGHT,
              batchMemoryManager.getRecordBatchSizer(topStatus.inputIndex),
              getRecordBatchStatsContext());
            return Pair.of(outcome, topStatus);
          case NONE:
            batchStatusStack.pop();
            if (batchStatusStack.isEmpty()) {
              return Pair.of(IterOutcome.NONE, null);
            }
            break;
          default:
            throw new IllegalStateException(String.format("Unexpected state %s", outcome));
          }
        }
      }

      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  protected void cancelIncoming() {
    super.cancelIncoming();
    // prevent iterating union inputs after the cancellation request
    unionInputIterator = Collections.emptyIterator();
  }

  @Override
  public void close() {
    super.close();
    updateBatchMemoryManagerStats();

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "incoming aggregate left: batch count : %d, avg bytes : %d,  avg row bytes : %d, record count : %d",
      batchMemoryManager.getNumIncomingBatches(JoinBatchMemoryManager.LEFT_INDEX), batchMemoryManager.getAvgInputBatchSize(JoinBatchMemoryManager.LEFT_INDEX),
      batchMemoryManager.getAvgInputRowWidth(JoinBatchMemoryManager.LEFT_INDEX), batchMemoryManager.getTotalInputRecords(JoinBatchMemoryManager.LEFT_INDEX));

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "incoming aggregate right: batch count : %d, avg bytes : %d,  avg row bytes : %d, record count : %d",
      batchMemoryManager.getNumIncomingBatches(JoinBatchMemoryManager.RIGHT_INDEX), batchMemoryManager.getAvgInputBatchSize(JoinBatchMemoryManager.RIGHT_INDEX),
      batchMemoryManager.getAvgInputRowWidth(JoinBatchMemoryManager.RIGHT_INDEX), batchMemoryManager.getTotalInputRecords(JoinBatchMemoryManager.RIGHT_INDEX));

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "outgoing aggregate: batch count : %d, avg bytes : %d,  avg row bytes : %d, record count : %d",
      batchMemoryManager.getNumOutgoingBatches(), batchMemoryManager.getAvgOutputBatchSize(),
      batchMemoryManager.getAvgOutputRowWidth(), batchMemoryManager.getTotalOutputRecords());
  }

  @Override
  public void dump() {
    logger.error("UnionAllRecordBatch[container={}, left={}, right={}, leftOutcome={}, rightOutcome={}, "
            + "recordCount={}]", container, left, right, leftUpstream, rightUpstream, recordCount);
  }
}
