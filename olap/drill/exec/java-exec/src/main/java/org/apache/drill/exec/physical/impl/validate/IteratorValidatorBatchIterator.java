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
package org.apache.drill.exec.physical.impl.validate;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.NONE;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK_NEW_SCHEMA;

import java.util.Iterator;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.VectorValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IteratorValidatorBatchIterator implements CloseableRecordBatch {
  private static final Logger logger =
      LoggerFactory.getLogger(IteratorValidatorBatchIterator.class);

  static final boolean VALIDATE_VECTORS = true;

  /** For logging/debugability only. */
  private static volatile int instanceCount;

  /** @see org.apache.drill.exec.physical.config.IteratorValidator */
  private final boolean isRepeatable;

  /** For logging/debugability only. */
  private final int instNum;
  {
    instNum = ++instanceCount;
  }

  /**
   * The upstream batch, calls to which and return values from which are
   * checked by this validator.
   */
  private final RecordBatch incoming;

  /** Incoming batch's type (simple class name); for logging/debugability
   *  only. */
  private final String batchTypeName;

  /** Exception state of incoming batch; last value thrown by its next()
   *  method. */
  private Throwable exceptionState;

  /** Main state of incoming batch; last value returned by its next() method. */
  private IterOutcome batchState;

  /** Last schema retrieved after OK_NEW_SCHEMA or OK from next().  Null if none
   *  yet. Currently for logging/debugability only. */
  private BatchSchema lastSchema;

  /** Last schema retrieved after OK_NEW_SCHEMA from next().  Null if none yet.
   *  Currently for logging/debugability only. */
  private BatchSchema lastNewSchema;

  /**
   * {@link IterOutcome} return value sequence validation state.
   * (Only needs enough to validate returns of OK.)
   */
  private enum ValidationState {
    /** Initial state:  Have not gotten any OK_NEW_SCHEMA yet and not
     *  terminated.  OK is not allowed yet. */
    INITIAL_NO_SCHEMA,
    /** Have gotten OK_NEW_SCHEMA already and not terminated.  OK is allowed
     *  now. */
    HAVE_SCHEMA,
    /** Terminal state:  Have seen NONE or STOP.  Nothing more is allowed. */
    TERMINAL
  }

  /** High-level IterOutcome sequence state. */
  private ValidationState validationState = ValidationState.INITIAL_NO_SCHEMA;

  /**
   * Enable/disable per-batch vector validation. Enable only to debug vector
   * corruption issues.
   */
  private boolean validateBatches;

  public IteratorValidatorBatchIterator(RecordBatch incoming, boolean isRepeatable) {
    this.incoming = incoming;
    batchTypeName = incoming.getClass().getSimpleName();
    this.isRepeatable = isRepeatable;

    // (Log construction and close() at same level to bracket instance's activity.)
    logger.trace( "[#{}; on {}; repeatable: {}]: Being constructed.", instNum, batchTypeName, isRepeatable);
  }

  public IteratorValidatorBatchIterator(RecordBatch incoming) {
    this(incoming, false);
  }

  public void enableBatchValidation(boolean option) {
    validateBatches = option;
  }

  @Override
  public String toString() {
    return
        super.toString()
        + "["
        + "instNum = " + instNum
        + ", validationState = " + validationState
        + ", batchState = " + batchState
        + ", ... "
        + "; incoming = " + incoming
        + "]";
  }

  private void validateReadState(String operation) {
    if (batchState == null) {
      throw new IllegalStateException(
          String.format(
              "Batch data read operation (%s) attempted before first next() call"
              + " on batch [#%d, %s].",
              operation, instNum, batchTypeName));
    }
    switch (batchState) {
    case OK:
    case OK_NEW_SCHEMA:
    case NONE:
    case EMIT:
      return;
    default:
      throw new IllegalStateException(
          String.format(
              "Batch data read operation (%s) attempted when last next() call"
              + " on batch [#%d, %s] returned %s (not %s or %s).",
              operation, instNum, batchTypeName, batchState, OK, OK_NEW_SCHEMA));
    }
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    validateReadState("iterator()");
    return incoming.iterator();
  }

  @Override
  public FragmentContext getContext() {
    return incoming.getContext();
  }

  @Override
  public BatchSchema getSchema() {
    return incoming.getSchema();
  }

  @Override
  public int getRecordCount() {
    validateReadState("getRecordCount()");
    return incoming.getRecordCount();
  }

  @Override
  public void cancel() {
    incoming.cancel();
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    validateReadState("getSelectionVector2()");
    return incoming.getSelectionVector2();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    validateReadState("getSelectionVector4()");
    return incoming.getSelectionVector4();
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    validateReadState("getValueVectorId(SchemaPath)");
    return incoming.getValueVectorId(path);
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
//    validateReadState(); TODO fix this
    return incoming.getValueAccessorById(clazz, ids);
  }

  @Override
  public IterOutcome next() {
    logger.trace( "[#{}; on {}]: next() called.", instNum, batchTypeName);
    final IterOutcome prevBatchState = batchState;
    try {

      // Check whether next() should even have been called in current state.
      if (null != exceptionState) {
        throw new IllegalStateException(
            String.format(
                "next() [on #%d; %s] called again after it threw %s (after"
                + " returning %s).  Caller should not have called next() again.",
                instNum, batchTypeName, exceptionState, batchState));
      }
      // (Note:  This could use validationState.)
      if (!isRepeatable && batchState == NONE) {
        throw new IllegalStateException(
            String.format(
                "next() [on #%d, %s] called again after it returned %s."
                + "  Caller should not have called next() again.",
                instNum, batchTypeName, batchState));
      }

      // Now get result from upstream next().
      batchState = incoming.next();

      logger.trace("[#{}; on {}]: incoming next() return: ({} ->) {}",
                   instNum, batchTypeName, prevBatchState, batchState);

      // Check state transition and update high-level state.
      switch (batchState) {
        case OK_NEW_SCHEMA:
          // OK_NEW_SCHEMA is allowed at any time, except if terminated (checked
          // above).
          // OK_NEW_SCHEMA moves to have-seen-schema state.
          validationState = ValidationState.HAVE_SCHEMA;
          validateBatch();
          break;
        case OK:
        case EMIT:
          // OK is allowed as long as OK_NEW_SCHEMA was seen, except if terminated
          // (checked above).
          if (validationState != ValidationState.HAVE_SCHEMA) {
            throw new IllegalStateException(
                String.format(
                    "next() returned %s without first returning %s [#%d, %s]",
                    batchState, OK_NEW_SCHEMA, instNum, batchTypeName));
          }
          validateBatch();
          // OK doesn't change high-level state.
          break;
        case NONE:
          // NONE is allowed even without seeing a OK_NEW_SCHEMA. Such NONE is called
          // FAST NONE.
          // NONE moves to TERMINAL high-level state if NOT repeatable.
          if (!isRepeatable) {
            validationState = ValidationState.TERMINAL;
          }
          break;
        case NOT_YET:
          // NOT_YET is allowed at any time, except if
          // terminated (checked above).
          // NOT_YET doesn't change high-level state.
          break;
        default:
          throw new AssertionError(
              "Unhandled new " + IterOutcome.class.getSimpleName() + " value "
              + batchState);
      }

      // Validate schema when available.
      if (batchState == OK || batchState == OK_NEW_SCHEMA) {
        final BatchSchema prevLastNewSchema = lastNewSchema;

        lastSchema = incoming.getSchema();
        if (batchState == OK_NEW_SCHEMA) {
          lastNewSchema = lastSchema;
        }

        if (logger.isTraceEnabled()) {
          logger.trace("[#{}; on {}]: incoming next() return: #records = {}, "
                       + "\n  schema:"
                       + "\n    {}, "
                       + "\n  prev. new ({}):"
                       + "\n    {}",
                       instNum, batchTypeName, incoming.getRecordCount(),
                       lastSchema,
                       lastSchema.equals(prevLastNewSchema) ? "equal" : "not equal",
                       prevLastNewSchema);
          }

        if (lastSchema == null) {
          throw new IllegalStateException(
              String.format(
                  "Incoming batch [#%d, %s] has a null schema. This is not allowed.",
                  instNum, batchTypeName));
        }
        // It's legal for a batch to have zero field. For instance, a relational table could have
        // zero columns. Querying such table requires execution operator to process batch with 0 field.
        if (incoming.getRecordCount() > MAX_BATCH_ROW_COUNT) {
          throw new IllegalStateException(
              String.format(
                  "Incoming batch [#%d, %s] has size %d, which is beyond the"
                  + " limit of %d",
                  instNum, batchTypeName, incoming.getRecordCount(), MAX_BATCH_ROW_COUNT
                  ));
        }

        if (VALIDATE_VECTORS) {
          VectorValidator.validate(incoming);
        }
      }

      return batchState;
    } catch (RuntimeException | Error e) {
      exceptionState = e;
      logger.trace("[#{}, on {}]: incoming next() exception: ({} ->) {}",
                   instNum, batchTypeName, prevBatchState, exceptionState,
                   exceptionState);
      throw e;
    }
  }

  private void validateBatch() {
    if (validateBatches || VALIDATE_VECTORS) {
      if (!BatchValidator.validate(incoming)) {
        throw new IllegalStateException(
            "Batch validation failed. Source operator: " +
            incoming.getClass().getSimpleName());
      }
      // The following validation currently calculates, and discards
      // a hash code. Since it requires manual checking, it is
      // disabled by default.
      // VectorValidator.validate(incoming);
    }
  }

  @Override
  public WritableBatch getWritableBatch() {
    validateReadState("getWritableBatch()");
    return incoming.getWritableBatch();
  }

  @Override
  public void close() {
    // (Log construction and close() calls at same logging level to bracket
    // instance's activity.)
    logger.trace( "[#{}; on {}]: close() called, state = {} / {}.",
                  instNum, batchTypeName, batchState, exceptionState);
  }

  @Override
  public VectorContainer getOutgoingContainer() {
    throw new UnsupportedOperationException(
        String.format("You should not call getOutgoingContainer() for class %s",
                      this.getClass().getCanonicalName()));
  }

  @Override
  public VectorContainer getContainer() {
    return incoming.getContainer();
  }

  public RecordBatch getIncoming() { return incoming; }

  @Override
  public void dump() {
    logger.error("IteratorValidatorBatchIterator[container={}, instNum={}, batchTypeName={}, lastSchema={}, "
           + "lastNewSchema={}]", getContainer(), instNum, batchTypeName, lastSchema, lastNewSchema);
  }
}
