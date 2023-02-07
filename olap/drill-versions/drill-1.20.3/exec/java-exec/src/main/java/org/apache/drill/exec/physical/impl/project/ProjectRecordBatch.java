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
package org.apache.drill.exec.physical.impl.project;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.SimpleRecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchIOType;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;

public class ProjectRecordBatch extends AbstractSingleRecordBatch<Project> {
  private static final Logger logger = LoggerFactory.getLogger(ProjectRecordBatch.class);

  protected List<ValueVector> allocationVectors;
  protected List<ComplexWriter> complexWriters;
  protected List<FieldReference> complexFieldReferencesList;
  protected ProjectMemoryManager memoryManager;
  private Projector projector;
  private boolean hasRemainder;
  private int remainderIndex;
  private int recordCount;
  private boolean first = true;
  private boolean wasNone; // whether a NONE iter outcome was already seen

  public ProjectRecordBatch(Project pop, RecordBatch incoming, FragmentContext context) {
    super(pop, context, incoming);
  }

  @Override
  public int getRecordCount() {
    return recordCount;
  }

  @Override
  protected void cancelIncoming() {
    super.cancelIncoming();
    hasRemainder = false;
  }

  @Override
  public IterOutcome innerNext() {
    if (wasNone) {
      return IterOutcome.NONE;
    }
    recordCount = 0;
    if (hasRemainder) {
      handleRemainder();
      // Check if we are supposed to return EMIT outcome and have consumed entire batch
      return getFinalOutcome(hasRemainder);
    }
    return super.innerNext();
  }

  @Override
  public VectorContainer getOutgoingContainer() {
    return container;
  }

  @Override
  protected IterOutcome doWork() {
    if (wasNone) {
      return IterOutcome.NONE;
    }

    int incomingRecordCount = incoming.getRecordCount();

    logger.trace("doWork(): incoming rc {}, incoming {}, Project {}", incomingRecordCount, incoming, this);
    //calculate the output row count
    memoryManager.update();

    if (first && incomingRecordCount == 0) {
      if (complexWriters != null) {
        IterOutcome next = null;
        while (incomingRecordCount == 0) {
          if (getLastKnownOutcome() == EMIT) {
            throw new UnsupportedOperationException("Currently functions producing complex types as output are not " +
                    "supported in project list for subquery between LATERAL and UNNEST. Please re-write the query using this " +
                    "function in the projection list of outermost query.");
          }

          next = next(incoming);
          setLastKnownOutcome(next);
          if (next == IterOutcome.NONE) {
            // since this is first batch and we already got a NONE, need to set up the schema
            doAlloc(0);
            setValueCount(0);
            wasNone = true;
            return IterOutcome.NONE;
          } else if (next != IterOutcome.OK && next != IterOutcome.OK_NEW_SCHEMA && next != EMIT) {
            return next;
          } else if (next == IterOutcome.OK_NEW_SCHEMA) {
            try {
              stats.startSetup();
              setupNewSchema();
            } finally {
              stats.stopSetup();
            }
          }
          incomingRecordCount = incoming.getRecordCount();
          memoryManager.update();
          logger.trace("doWork():[1] memMgr RC {}, incoming rc {}, incoming {}, Project {}",
                       memoryManager.getOutputRowCount(), incomingRecordCount, incoming, this);
        }
      }
    }

    if (complexWriters != null && getLastKnownOutcome() == EMIT) {
      throw UserException.unsupportedError()
          .message("Currently functions producing complex types as output are not " +
            "supported in project list for subquery between LATERAL and UNNEST. Please re-write the query using this " +
            "function in the projection list of outermost query.")
          .build(logger);
    }

    first = false;
    container.zeroVectors();

    int maxOuputRecordCount = memoryManager.getOutputRowCount();
    logger.trace("doWork():[2] memMgr RC {}, incoming rc {}, incoming {}, project {}",
                 memoryManager.getOutputRowCount(), incomingRecordCount, incoming, this);

    doAlloc(maxOuputRecordCount);
    long projectStartTime = System.currentTimeMillis();
    int outputRecords = projector.projectRecords(incoming, 0, maxOuputRecordCount, 0);
    long projectEndTime = System.currentTimeMillis();
    logger.trace("doWork(): projection: records {}, time {} ms", outputRecords, (projectEndTime - projectStartTime));

    setValueCount(outputRecords);
    recordCount = outputRecords;
    if (outputRecords < incomingRecordCount) {
      hasRemainder = true;
      remainderIndex = outputRecords;
    } else {
      assert outputRecords == incomingRecordCount;
      incoming.getContainer().zeroVectors();
    }
    // In case of complex writer expression, vectors would be added to batch run-time.
    // We have to re-build the schema.
    if (complexWriters != null) {
      container.buildSchema(SelectionVectorMode.NONE);
    }

    memoryManager.updateOutgoingStats(outputRecords);
    RecordBatchStats.logRecordBatchStats(RecordBatchIOType.OUTPUT, this, getRecordBatchStatsContext());

    // Get the final outcome based on hasRemainder since that will determine if all the incoming records were
    // consumed in current output batch or not
    return getFinalOutcome(hasRemainder);
  }

  private void handleRemainder() {
    int remainingRecordCount = incoming.getRecordCount() - remainderIndex;
    assert memoryManager.incomingBatch() == incoming;
    int recordsToProcess = Math.min(remainingRecordCount, memoryManager.getOutputRowCount());
    doAlloc(recordsToProcess);

    logger.trace("handleRemainder: remaining RC {}, toProcess {}, remainder index {}, incoming {}, Project {}",
                 remainingRecordCount, recordsToProcess, remainderIndex, incoming, this);

    long projectStartTime = System.currentTimeMillis();
    int projRecords = projector.projectRecords(this.incoming, remainderIndex, recordsToProcess, 0);
    long projectEndTime = System.currentTimeMillis();

    logger.trace("handleRemainder: projection: records {}, time {} ms", projRecords,(projectEndTime - projectStartTime));

    if (projRecords < remainingRecordCount) {
      setValueCount(projRecords);
      recordCount = projRecords;
      remainderIndex += projRecords;
    } else {
      setValueCount(remainingRecordCount);
      hasRemainder = false;
      remainderIndex = 0;
      incoming.getContainer().zeroVectors();
      recordCount = remainingRecordCount;
    }
    // In case of complex writer expression, vectors would be added to batch run-time.
    // We have to re-build the schema.
    if (complexWriters != null) {
      container.buildSchema(SelectionVectorMode.NONE);
    }

    memoryManager.updateOutgoingStats(projRecords);
    RecordBatchStats.logRecordBatchStats(RecordBatchIOType.OUTPUT, this, getRecordBatchStatsContext());
  }

  // Called from generated code.

  public void addComplexWriter(ComplexWriter writer) {
    complexWriters.add(writer);
  }

  private void doAlloc(int recordCount) {
    // Allocate vv in the allocationVectors.
    for (ValueVector v : allocationVectors) {
      AllocationHelper.allocateNew(v, recordCount);
    }

    // Allocate vv for complexWriters.
    if (complexWriters != null) {
      for (ComplexWriter writer : complexWriters) {
        writer.allocate();
      }
    }
  }

  private void setValueCount(int count) {
    if (count == 0) {
      container.setEmpty();
      return;
    }
    for (ValueVector v : allocationVectors) {
      v.getMutator().setValueCount(count);
    }

    // Value counts for vectors should have been set via
    // the transfer pairs or vector copies.
    container.setRecordCount(count);

    if (complexWriters == null) {
      return;
    }

    for (ComplexWriter writer : complexWriters) {
      writer.setValueCount(count);
    }
  }

  @Override
  protected boolean setupNewSchema() {
    setupNewSchemaFromInput(incoming);
    if (container.isSchemaChanged() || callBack.getSchemaChangedAndReset()) {
      container.buildSchema(SelectionVectorMode.NONE);
      return true;
    } else {
      return false;
    }
  }

  private void setupNewSchemaFromInput(RecordBatch incomingBatch) {
    // get the output batch size from config.
    int configuredBatchSize = (int) context.getOptions().getOption(ExecConstants.OUTPUT_BATCH_SIZE_VALIDATOR);
    setupNewSchema(incomingBatch, configuredBatchSize);

    ProjectBatchBuilder batchBuilder = new ProjectBatchBuilder(this,
        container, callBack, incomingBatch);
    ProjectionMaterializer em = new ProjectionMaterializer(context.getOptions(),
        incomingBatch, popConfig.getExprs(), context.getFunctionRegistry(),
        batchBuilder, unionTypeEnabled);
    boolean saveCode = false;
    // Uncomment this line to debug the generated code.
    // saveCode = true;
    projector = em.generateProjector(context, saveCode);
    try {
      projector.setup(context, incomingBatch, this, batchBuilder.transfers());
    } catch (SchemaChangeException e) {
      throw UserException.schemaChangeError(e)
          .addContext("Unexpected schema change in the Project operator")
          .build(logger);
    }
  }

  /**
   * Handle Null input specially when Project operator is for query output.
   * This happens when the input returns no batches (returns a FAST {@code NONE} directly).
   *
   * <p>
   * Project operator has to return a batch with schema derived using the following 3 rules:
   * </p>
   * <ul>
   *  <li>Case 1:  * ==> expand into an empty list of columns. </li>
   *  <li>Case 2:  regular column reference ==> treat as nullable-int column </li>
   *  <li>Case 3:  expressions => Call ExpressionTreeMaterialization over an empty vector contain.
   *           Once the expression is materialized without error, use the output type of materialized
   *           expression. </li>
   * </ul>
   *
   * <p>
   * The batch is constructed with the above rules, and recordCount = 0.
   * Returned with {@code OK_NEW_SCHEMA} to down-stream operator.
   * </p>
   */
  @Override
  protected IterOutcome handleNullInput() {
    if (!popConfig.isOutputProj()) {
      BatchSchema incomingSchema = incoming.getSchema();
      if (incomingSchema != null && incomingSchema.getFieldCount() > 0) {
        setupNewSchemaFromInput(incoming);
      }
      return super.handleNullInput();
    }

    VectorContainer emptyVC = new VectorContainer();
    emptyVC.buildSchema(SelectionVectorMode.NONE);
    RecordBatch emptyIncomingBatch = new SimpleRecordBatch(emptyVC, context);

    setupNewSchemaFromInput(emptyIncomingBatch);

    doAlloc(0);
    container.buildSchema(SelectionVectorMode.NONE);
    container.setEmpty();
    wasNone = true;
    return IterOutcome.OK_NEW_SCHEMA;
  }

  @Override
  protected IterOutcome getFinalOutcome(boolean hasMoreRecordInBoundary) {
    // In a case of complex writers vectors are added at runtime, so the schema
    // may change (e.g. when a batch contains new column(s) not present in previous batches)
    if (complexWriters != null) {
      return IterOutcome.OK_NEW_SCHEMA;
    }
    return super.getFinalOutcome(hasMoreRecordInBoundary);
  }

  private void setupNewSchema(RecordBatch incomingBatch, int configuredBatchSize) {
    memoryManager = new ProjectMemoryManager(configuredBatchSize);
    memoryManager.init(incomingBatch, ProjectRecordBatch.this);
    if (allocationVectors != null) {
      for (ValueVector v : allocationVectors) {
        v.clear();
      }
    }
    allocationVectors = new ArrayList<>();

    if (complexWriters != null) {
      container.clear();
    } else {
      // Release the underlying DrillBufs and reset the ValueVectors to empty
      // Not clearing the container here is fine since Project output schema is
      // not determined solely based on incoming batch. It is defined by the
      // expressions it has to evaluate.
      //
      // If there is a case where only the type of ValueVector already present
      // in container is changed then addOrGet method takes care of it by
      // replacing the vectors.
      container.zeroVectors();
    }
  }

  @Override
  public void dump() {
    logger.error("ProjectRecordBatch[projector={}, hasRemainder={}, remainderIndex={}, recordCount={}, container={}]",
        projector, hasRemainder, remainderIndex, recordCount, container);
  }
}
