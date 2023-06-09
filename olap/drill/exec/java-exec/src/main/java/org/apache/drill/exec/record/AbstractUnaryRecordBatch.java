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
package org.apache.drill.exec.record;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.vector.SchemaChangeCallBack;

/**
 * Base class for operators that have a single input. The concrete implementations provide the
 * input by implementing the getIncoming() method
 * Known implementations:  AbstractSingleRecordBatch and AbstractTableFunctionRecordBatch.
 * @see org.apache.drill.exec.record.AbstractRecordBatch
 * @see org.apache.drill.exec.record.AbstractSingleRecordBatch
 * @see org.apache.drill.exec.record.AbstractTableFunctionRecordBatch
 * @param <T>
 */
public abstract class AbstractUnaryRecordBatch<T extends PhysicalOperator> extends AbstractRecordBatch<T> {

  protected SchemaChangeCallBack callBack = new SchemaChangeCallBack();
  private IterOutcome lastKnownOutcome;

  public AbstractUnaryRecordBatch(T popConfig, FragmentContext context) throws OutOfMemoryException {
    super(popConfig, context, false);
  }

  protected abstract RecordBatch getIncoming();

  @Override
  protected void cancelIncoming() {
    getIncoming().cancel();
  }

  @Override
  public IterOutcome innerNext() {
    final RecordBatch incoming = getIncoming();
    // Short circuit if record batch has already sent all data and is done
    if (state == BatchState.DONE) {
      return IterOutcome.NONE;
    }

    IterOutcome upstream = next(incoming);
    if (state != BatchState.FIRST && upstream == IterOutcome.OK && incoming.getRecordCount() == 0) {
      do {
        incoming.getContainer().zeroVectors();
      } while ((upstream = next(incoming)) == IterOutcome.OK && incoming.getRecordCount() == 0);
    }
    if (state == BatchState.FIRST) {
      if (upstream == IterOutcome.OK) {
        upstream = IterOutcome.OK_NEW_SCHEMA;
      } else if (upstream == IterOutcome.EMIT) {
        throw new IllegalStateException("Received first batch with unexpected EMIT IterOutcome");
      }
    }

    // update the last outcome seen
    lastKnownOutcome = upstream;
    switch (upstream) {
      case NONE:
        if (state == BatchState.FIRST) {
          return handleNullInput();
        }
        return upstream;
      case NOT_YET:
        if (state == BatchState.FIRST) {
          container.buildSchema(SelectionVectorMode.NONE);
        }
        return upstream;
      case OK_NEW_SCHEMA:
        if (state == BatchState.FIRST) {
          state = BatchState.NOT_FIRST;
        }
        try {
          stats.startSetup();
          if (!setupNewSchema()) {
            upstream = IterOutcome.OK;
          }
        } finally {
          stats.stopSetup();
        }
        // fall through.
      case OK:
      case EMIT:
        assert state != BatchState.FIRST : "First batch should be OK_NEW_SCHEMA";
        container.zeroVectors();
        IterOutcome out = doWork();

        // since doWork method does not know if there is a new schema, it will always return IterOutcome.OK if it was successful.
        // But if upstream is IterOutcome.OK_NEW_SCHEMA, we should return that
        if (out != IterOutcome.OK) {
          upstream = out;
        }

        // Check if schema has changed
        if (callBack.getSchemaChangedAndReset()) {
          return IterOutcome.OK_NEW_SCHEMA;
        }

        return upstream; // change if upstream changed, otherwise normal.
      default:
        throw new UnsupportedOperationException();
    }
  }

  protected abstract boolean setupNewSchema();
  protected abstract IterOutcome doWork();

  /**
   * Default behavior to handle NULL input (aka FAST NONE): incoming return NONE
   * before return a OK_NEW_SCHEMA: This could happen when the underneath Scan
   * operators do not produce any batch with schema.
   * <p>
   * Notice that NULL input is different from input with an empty batch. In the
   * later case, input provides at least a batch, thought it's empty.
   * </p>
   * <p>
   * This behavior could be override in each individual operator, if the
   * operator's semantics is to inject a batch with schema.
   * </p>
   *
   * @return IterOutcome.NONE.
   */
  protected IterOutcome handleNullInput() {
    container.buildSchema(SelectionVectorMode.NONE);
    container.setEmpty();
    return IterOutcome.NONE;
  }

  protected IterOutcome getLastKnownOutcome() {
    return lastKnownOutcome;
  }

  /**
   * Set's the outcome received with current input batch in processing
   * @param outcome
   */
  protected void setLastKnownOutcome(IterOutcome outcome) {
    lastKnownOutcome = outcome;
  }
}
