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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.MergeJoinPOP;
import org.apache.drill.exec.physical.impl.common.Comparator;
import org.apache.drill.exec.record.AbstractBinaryRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.JoinBatchMemoryManager;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.RecordIterator;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchIOType;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractContainerVector;

import java.util.HashSet;
import java.util.List;

import static org.apache.drill.exec.compile.sig.GeneratorMapping.GM;

/**
 * A join operator that merges two sorted streams using a record iterator.
 */
public class MergeJoinBatch extends AbstractBinaryRecordBatch<MergeJoinPOP> {

  private static final Logger logger = LoggerFactory.getLogger(MergeJoinBatch.class);

  private final MappingSet setupMapping =
    new MappingSet("null", "null",
      GM("doSetup", "doSetup", null, null),
      GM("doSetup", "doSetup", null, null));
  private final MappingSet copyLeftMapping =
    new MappingSet("leftIndex", "outIndex",
      GM("doSetup", "doSetup", null, null),
      GM("doSetup", "doCopyLeft", null, null));
  private final MappingSet copyRightMappping =
    new MappingSet("rightIndex", "outIndex",
      GM("doSetup", "doSetup", null, null),
      GM("doSetup", "doCopyRight", null, null));
  private final MappingSet compareMapping =
    new MappingSet("leftIndex", "rightIndex",
      GM("doSetup", "doSetup", null, null),
      GM("doSetup", "doCompare", null, null));
  private final MappingSet compareRightMapping =
    new MappingSet("rightIndex", "null",
      GM("doSetup", "doSetup", null, null),
      GM("doSetup", "doCompare", null, null));

  private final RecordIterator leftIterator;
  private final RecordIterator rightIterator;
  private final JoinStatus status;
  private final List<JoinCondition> conditions;
  private final List<Comparator> comparators;
  private final JoinRelType joinType;
  private JoinWorker worker;

  private class MergeJoinMemoryManager extends JoinBatchMemoryManager {

    MergeJoinMemoryManager(int outputBatchSize, RecordBatch leftBatch, RecordBatch rightBatch) {
      super(outputBatchSize, leftBatch, rightBatch, new HashSet<>());
    }

    /**
     * mergejoin operates on one record at a time from the left and right batches
     * using RecordIterator abstraction. We have a callback mechanism to get notified
     * when new batch is loaded in record iterator.
     * This can get called in the middle of current output batch we are building.
     * when this gets called, adjust number of output rows for the current batch and
     * update the value to be used for subsequent batches.
     */
    @Override
    public void update(int inputIndex) {
      super.update(inputIndex, status.getOutPosition());
      status.setTargetOutputRowCount(super.getCurrentOutgoingMaxRowCount()); // calculated by update()
      RecordBatchIOType type = inputIndex == 0 ? RecordBatchIOType.INPUT_LEFT : RecordBatchIOType.INPUT_RIGHT;
      RecordBatchStats.logRecordBatchStats(type, getRecordBatchSizer(inputIndex), getRecordBatchStatsContext());
    }
  }

  protected MergeJoinBatch(MergeJoinPOP popConfig, FragmentContext context, RecordBatch left, RecordBatch right) throws OutOfMemoryException {
    super(popConfig, context, true, left, right);

    // Instantiate the batch memory manager
    final int configuredBatchSize = (int) context.getOptions().getOption(ExecConstants.OUTPUT_BATCH_SIZE_VALIDATOR);
    batchMemoryManager = new MergeJoinMemoryManager(configuredBatchSize, left, right);

    RecordBatchStats.printConfiguredBatchSize(getRecordBatchStatsContext(),
      configuredBatchSize);

    if (popConfig.getConditions().size() == 0) {
      throw new UnsupportedOperationException("Merge Join currently does not support cartesian join.  This join operator was configured with 0 conditions");
    }
    this.leftIterator = new RecordIterator(left, this, oContext, 0, false, batchMemoryManager);
    this.rightIterator = new RecordIterator(right, this, oContext, 1, batchMemoryManager);
    this.joinType = popConfig.getJoinType();
    this.status = new JoinStatus(leftIterator, rightIterator, this);
    this.conditions = popConfig.getConditions();

    this.comparators = Lists.newArrayListWithExpectedSize(conditions.size());
    for (JoinCondition condition : conditions) {
      this.comparators.add(JoinUtils.checkAndReturnSupportedJoinComparator(condition));
    }
  }

  public JoinRelType getJoinType() {
    return joinType;
  }

  @Override
  public int getRecordCount() {
    return status.getOutPosition();
  }

  @Override
  public void buildSchema() {
    // initialize iterators
    status.initialize();
    final IterOutcome leftOutcome = status.getLeftStatus();
    final IterOutcome rightOutcome = status.getRightStatus();

    if (!verifyOutcomeToSetBatchState(leftOutcome, rightOutcome)) {
      return;
    }

    allocateBatch(true);
    container.setEmpty();
  }

  @Override
  public IterOutcome innerNext() {
    // we do this in the here instead of the constructor because don't necessary want to start consuming on construction.
    status.prepare();
    // loop so we can start over again if we find a new batch was created.
    while (true) {
      boolean isNewSchema = false;
      // Check result of last iteration.
      switch (status.getOutcome()) {
        case SCHEMA_CHANGED:
          isNewSchema = true;
        case BATCH_RETURNED:
          allocateBatch(isNewSchema);
          status.resetOutputPos();
          status.setTargetOutputRowCount(batchMemoryManager.getOutputRowCount());
          break;
        case NO_MORE_DATA:
          status.resetOutputPos();
          logger.debug("NO MORE DATA; returning {}  NONE");
          return IterOutcome.NONE;
        case FAILURE:
          status.left.clearInflightBatches();
          status.right.clearInflightBatches();
          // Should handle at the source of the error to provide a better error message.
          throw UserException.executionError(null)
              .message("Merge failed")
              .build(logger);
        case WAITING:
          return IterOutcome.NOT_YET;
        default:
          throw new IllegalStateException();
      }

      boolean first = false;
      if (worker == null) {
        try {
          logger.debug("Creating New Worker");
          stats.startSetup();
          worker = generateNewWorker();
          first = true;
        } finally {
          stats.stopSetup();
        }
      }

      // join until we have a complete outgoing batch
      if (!worker.doJoin(status)) {
        worker = null;
      }

      // get the outcome of the last join iteration.
      switch (status.getOutcome()) {
        case BATCH_RETURNED:
          // only return new schema if new worker has been setup.
          logger.debug("BATCH RETURNED; returning {}", (first ? "OK_NEW_SCHEMA" : "OK"));
          setRecordCountInContainer();
          return first ? IterOutcome.OK_NEW_SCHEMA : IterOutcome.OK;
        case FAILURE:
          status.left.clearInflightBatches();
          status.right.clearInflightBatches();
          // Should handle at the source of the error to provide a better error message.
          throw UserException.executionError(null)
              .message("Merge failed")
              .build(logger);
        case NO_MORE_DATA:
          logger.debug("NO MORE DATA; returning {}",
            (status.getOutPosition() > 0 ? (first ? "OK_NEW_SCHEMA" : "OK") : (first ? "OK_NEW_SCHEMA" : "NONE")));
          setRecordCountInContainer();
          state = BatchState.DONE;
          return (first? IterOutcome.OK_NEW_SCHEMA : (status.getOutPosition() > 0 ? IterOutcome.OK: IterOutcome.NONE));
        case SCHEMA_CHANGED:
          worker = null;
          if (status.getOutPosition() > 0) {
            // if we have current data, let's return that.
            logger.debug("SCHEMA CHANGED; returning {} ", (first ? "OK_NEW_SCHEMA" : "OK"));
            setRecordCountInContainer();
            return first ? IterOutcome.OK_NEW_SCHEMA : IterOutcome.OK;
          } else{
            // loop again to rebuild worker.
            continue;
          }
        case WAITING:
          return IterOutcome.NOT_YET;
        default:
          throw new IllegalStateException();
      }
    }
  }

  private void setRecordCountInContainer() {
    container.setValueCount(getRecordCount());
    RecordBatchStats.logRecordBatchStats(RecordBatchIOType.OUTPUT, this, getRecordBatchStatsContext());
    batchMemoryManager.updateOutgoingStats(getRecordCount());
  }

  @Override
  public void close() {
    updateBatchMemoryManagerStats();

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "incoming aggregate left: batch count : %d, avg bytes : %d,  avg row bytes : %d, record count : %d",
      batchMemoryManager.getNumIncomingBatches(JoinBatchMemoryManager.LEFT_INDEX),
      batchMemoryManager.getAvgInputBatchSize(JoinBatchMemoryManager.LEFT_INDEX),
      batchMemoryManager.getAvgInputRowWidth(JoinBatchMemoryManager.LEFT_INDEX),
      batchMemoryManager.getTotalInputRecords(JoinBatchMemoryManager.LEFT_INDEX));

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "incoming aggregate right: batch count : %d, avg bytes : %d,  avg row bytes : %d, record count : %d",
      batchMemoryManager.getNumIncomingBatches(JoinBatchMemoryManager.RIGHT_INDEX),
      batchMemoryManager.getAvgInputBatchSize(JoinBatchMemoryManager.RIGHT_INDEX),
      batchMemoryManager.getAvgInputRowWidth(JoinBatchMemoryManager.RIGHT_INDEX),
      batchMemoryManager.getTotalInputRecords(JoinBatchMemoryManager.RIGHT_INDEX));

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "outgoing aggregate: batch count : %d, avg bytes : %d,  avg row bytes : %d, record count : %d",
      batchMemoryManager.getNumOutgoingBatches(), batchMemoryManager.getAvgOutputBatchSize(),
      batchMemoryManager.getAvgOutputRowWidth(), batchMemoryManager.getTotalOutputRecords());

    super.close();
    leftIterator.close();
    rightIterator.close();
  }

  private JoinWorker generateNewWorker() {
    final ClassGenerator<JoinWorker> cg = CodeGenerator.getRoot(JoinWorker.TEMPLATE_DEFINITION, context.getOptions());
    cg.getCodeGenerator().plainJavaCapable(true);
    // cg.getCodeGenerator().saveCodeForDebugging(true);
    final ErrorCollector collector = new ErrorCollectorImpl();

    // Generate members and initialization code
    /////////////////////////////////////////

    // declare and assign JoinStatus member
    cg.setMappingSet(setupMapping);
    JClass joinStatusClass = cg.getModel().ref(JoinStatus.class);
    JVar joinStatus = cg.clazz.field(JMod.NONE, joinStatusClass, "status");
    cg.getSetupBlock().assign(JExpr._this().ref(joinStatus), JExpr.direct("status"));

    // declare and assign outgoing VectorContainer member
    JClass vectorContainerClass = cg.getModel().ref(VectorContainer.class);
    JVar outgoingVectorContainer = cg.clazz.field(JMod.NONE, vectorContainerClass, "outgoing");
    cg.getSetupBlock().assign(JExpr._this().ref(outgoingVectorContainer), JExpr.direct("outgoing"));

    // declare and assign incoming left RecordBatch member
    JClass recordBatchClass = cg.getModel().ref(RecordIterator.class);
    JVar incomingLeftRecordBatch = cg.clazz.field(JMod.NONE, recordBatchClass, "incomingLeft");
    cg.getSetupBlock().assign(JExpr._this().ref(incomingLeftRecordBatch), joinStatus.ref("left"));

    // declare and assign incoming right RecordBatch member
    JVar incomingRightRecordBatch = cg.clazz.field(JMod.NONE, recordBatchClass, "incomingRight");
    cg.getSetupBlock().assign(JExpr._this().ref(incomingRightRecordBatch), joinStatus.ref("right"));

    // declare 'incoming' member so VVReadExpr generated code can point to the left or right batch
    JVar incomingRecordBatch = cg.clazz.field(JMod.NONE, recordBatchClass, "incoming");

    /*
     * Materialize expressions on both sides of the join condition. Check if both the sides
     * have the same return type, if not then inject casts so that comparison function will work as
     * expected
     */
    LogicalExpression leftExpr[] = new LogicalExpression[conditions.size()];
    LogicalExpression rightExpr[] = new LogicalExpression[conditions.size()];
    IterOutcome lastLeftStatus = status.getLeftStatus();
    IterOutcome lastRightStatus = status.getRightStatus();
    for (int i = 0; i < conditions.size(); i++) {
      JoinCondition condition = conditions.get(i);
      leftExpr[i] =  materializeExpression(condition.getLeft(), lastLeftStatus, leftIterator, collector);
      rightExpr[i] = materializeExpression(condition.getRight(), lastRightStatus, rightIterator, collector);
    }

    // if right side is empty, rightExpr will most likely default to NULLABLE INT which may cause the following
    // call to throw an exception. In this case we can safely skip adding the casts
    if (lastRightStatus != IterOutcome.NONE) {
      JoinUtils.addLeastRestrictiveCasts(leftExpr, leftIterator, rightExpr, rightIterator, context);
    }
    //generate doCompare() method
    /////////////////////////////////////////
    generateDoCompare(cg, incomingRecordBatch, leftExpr, incomingLeftRecordBatch, rightExpr,
      incomingRightRecordBatch, collector);

    // generate copyLeft()
    //////////////////////
    cg.setMappingSet(copyLeftMapping);
    int vectorId = 0;
    if (worker == null || !status.left.finished()) {
      for (VectorWrapper<?> vw : leftIterator) {
        MajorType inputType = vw.getField().getType();
        MajorType outputType;
        if (joinType == JoinRelType.RIGHT && inputType.getMode() == DataMode.REQUIRED) {
          outputType = Types.overrideMode(inputType, DataMode.OPTIONAL);
        } else {
          outputType = inputType;
        }
        // TODO (DRILL-4011): Factor out CopyUtil and use it here.
        TypedFieldId inTypedFieldId = new TypedFieldId.Builder().finalType(inputType)
            .addId(vectorId)
            .build();
        JVar vvIn = cg.declareVectorValueSetupAndMember("incomingLeft", inTypedFieldId);
        TypedFieldId outTypedFieldId = new TypedFieldId.Builder().finalType(outputType)
            .addId(vectorId)
            .build();
        JVar vvOut = cg.declareVectorValueSetupAndMember("outgoing", outTypedFieldId);
        // todo: check result of copyFromSafe and grow allocation
        cg.getEvalBlock().add(vvOut.invoke("copyFromSafe")
          .arg(copyLeftMapping.getValueReadIndex())
          .arg(copyLeftMapping.getValueWriteIndex())
          .arg(vvIn));
        cg.rotateBlock();
        ++vectorId;
      }
    }

    // generate copyRight()
    ///////////////////////
    cg.setMappingSet(copyRightMappping);

    int rightVectorBase = vectorId;
    if (status.getRightStatus() != IterOutcome.NONE && (worker == null || !status.right.finished())) {
      for (VectorWrapper<?> vw : rightIterator) {
        MajorType inputType = vw.getField().getType();
        MajorType outputType;
        if (joinType == JoinRelType.LEFT && inputType.getMode() == DataMode.REQUIRED) {
          outputType = Types.overrideMode(inputType, DataMode.OPTIONAL);
        } else {
          outputType = inputType;
        }
        // TODO (DRILL-4011): Factor out CopyUtil and use it here.
        TypedFieldId inTypedFieldId = new TypedFieldId.Builder().finalType(inputType)
            .addId(vectorId - rightVectorBase)
            .build();
        JVar vvIn = cg.declareVectorValueSetupAndMember("incomingRight", inTypedFieldId);
        TypedFieldId outTypedFieldId = new TypedFieldId.Builder().finalType(outputType)
            .addId(vectorId)
            .build();
        JVar vvOut = cg.declareVectorValueSetupAndMember("outgoing", outTypedFieldId);
        // todo: check result of copyFromSafe and grow allocation
        cg.getEvalBlock().add(vvOut.invoke("copyFromSafe")
          .arg(copyRightMappping.getValueReadIndex())
          .arg(copyRightMappping.getValueWriteIndex())
          .arg(vvIn));
        cg.rotateBlock();
        ++vectorId;
      }
    }

    JoinWorker w = context.getImplementationClass(cg);
    try {
      w.setupJoin(context, status, this.container);
    } catch (SchemaChangeException e) {
      throw schemaChangeException(e, logger);
    }
    return w;
  }

  private void allocateBatch(boolean newSchema) {
    boolean leftAllowed = status.getLeftStatus() != IterOutcome.NONE;
    boolean rightAllowed = status.getRightStatus() != IterOutcome.NONE;

    if (newSchema) {
      container.clear();
      // add fields from both batches
      if (leftAllowed) {
        for (VectorWrapper<?> w : leftIterator) {
          MajorType inputType = w.getField().getType();
          MajorType outputType;
          if (joinType == JoinRelType.RIGHT && inputType.getMode() == DataMode.REQUIRED) {
            outputType = Types.overrideMode(inputType, DataMode.OPTIONAL);
          } else {
            outputType = inputType;
          }
          MaterializedField newField = MaterializedField.create(w.getField().getName(), outputType);
          ValueVector v = container.addOrGet(newField);
          if (v instanceof AbstractContainerVector) {
            w.getValueVector().makeTransferPair(v);
            v.clear();
          }
        }
      }
      if (rightAllowed) {
        for (VectorWrapper<?> w : rightIterator) {
          MajorType inputType = w.getField().getType();
          MajorType outputType;
          if (joinType == JoinRelType.LEFT && inputType.getMode() == DataMode.REQUIRED) {
            outputType = Types.overrideMode(inputType, DataMode.OPTIONAL);
          } else {
            outputType = inputType;
          }
          MaterializedField newField = MaterializedField.create(w.getField().getName(), outputType);
          ValueVector v = container.addOrGet(newField);
          if (v instanceof AbstractContainerVector) {
            w.getValueVector().makeTransferPair(v);
            v.clear();
          }
        }
      }
    } else {
      container.zeroVectors();
    }

    // Allocate memory for the vectors.
    // This will iteratively allocate memory for all nested columns underneath.
    int outputRowCount = batchMemoryManager.getOutputRowCount();
    for (VectorWrapper<?> w : container) {
      RecordBatchSizer.ColumnSize colSize = batchMemoryManager.getColumnSize(w.getField().getName());
      colSize.allocateVector(w.getValueVector(), outputRowCount);
    }

    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);
    logger.debug("Built joined schema: {}", container.getSchema());
  }

  private void generateDoCompare(ClassGenerator<JoinWorker> cg, JVar incomingRecordBatch,
                                 LogicalExpression[] leftExpression, JVar incomingLeftRecordBatch,
                                 LogicalExpression[] rightExpression,
                                 JVar incomingRightRecordBatch, ErrorCollector collector) {

    cg.setMappingSet(compareMapping);
    if (status.getRightStatus() != IterOutcome.NONE) {
      assert leftExpression.length == rightExpression.length;

      for (int i = 0; i < leftExpression.length; i++) {
        // generate compare()
        ////////////////////////
        cg.setMappingSet(compareMapping);
        cg.getSetupBlock().assign(JExpr._this().ref(incomingRecordBatch), JExpr._this().ref(incomingLeftRecordBatch));
        ClassGenerator.HoldingContainer compareLeftExprHolder = cg.addExpr(leftExpression[i], ClassGenerator.BlkCreateMode.FALSE);

        cg.setMappingSet(compareRightMapping);
        cg.getSetupBlock().assign(JExpr._this().ref(incomingRecordBatch), JExpr._this().ref(incomingRightRecordBatch));
        ClassGenerator.HoldingContainer compareRightExprHolder = cg.addExpr(rightExpression[i], ClassGenerator.BlkCreateMode.FALSE);

        LogicalExpression fh =
          FunctionGenerationHelper.getOrderingComparatorNullsHigh(compareLeftExprHolder,
            compareRightExprHolder,
            context.getFunctionRegistry());
        HoldingContainer out = cg.addExpr(fh, ClassGenerator.BlkCreateMode.FALSE);

        // If not 0, it means not equal.
        // Null compares to Null should returns null (unknown). In such case, we return 1 to indicate they are not equal.
        if (compareLeftExprHolder.isOptional() && compareRightExprHolder.isOptional()
          && comparators.get(i) == Comparator.EQUALS) {
          JConditional jc = cg.getEvalBlock()._if(compareLeftExprHolder.getIsSet().eq(JExpr.lit(0)).
            cand(compareRightExprHolder.getIsSet().eq(JExpr.lit(0))));
          jc._then()._return(JExpr.lit(1));
          jc._elseif(out.getValue().ne(JExpr.lit(0)))._then()._return(out.getValue());
        } else {
          cg.getEvalBlock()._if(out.getValue().ne(JExpr.lit(0)))._then()._return(out.getValue());
        }
      }
    }

    //Pass the equality check for all the join conditions. Finally, return 0.
    cg.getEvalBlock()._return(JExpr.lit(0));
  }

  private LogicalExpression materializeExpression(LogicalExpression expression, IterOutcome lastStatus,
                                                  VectorAccessible input, ErrorCollector collector) {
    LogicalExpression materializedExpr;
    if (lastStatus != IterOutcome.NONE) {
      materializedExpr = ExpressionTreeMaterializer.materialize(expression, input,
          collector, context.getFunctionRegistry(), unionTypeEnabled);
    } else {
      materializedExpr = new TypedNullConstant(Types.optional(MinorType.INT));
    }
    collector.reportErrors(logger);
    return materializedExpr;
  }

  @Override
  public void dump() {
    logger.error("MergeJoinBatch[container={}, left={}, right={}, leftOutcome={}, rightOutcome={}, joinType={}, leftIterator={}," +
            " rightIterator={}, joinStatus={}, joinType={}]",
        container, left, right, leftUpstream, rightUpstream, joinType, leftIterator, rightIterator, status, joinType);
  }
}
