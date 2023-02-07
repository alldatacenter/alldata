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
package org.apache.drill.exec.physical.impl.protocol;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.RecordBatch.IterOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State machine that drives the operator executable. Converts
 * between the iterator protocol and the operator executable protocol.
 * Implemented as a separate class in anticipation of eventually
 * changing the record batch (iterator) protocol.
 *
 * <h4>Schema-Only Batch</h4>
 *
 * The scan operator is designed to provide an initial, empty, schema-only
 * batch. At the time that this code was written, it was (mis-?) understood
 * that Drill used a "fast schema" path that provided a schema-only batch
 * as the first batch. However, it turns out that most operators fail when
 * presented with an empty batch: many do not properly set the offset
 * vector for variable-width vectors to an initial 0 position, causing all
 * kinds of issues.
 * <p>
 * To work around this issue, the code defaults to *not* providing the
 * schema batch.
 */

public class OperatorDriver {

  private static final Logger logger = LoggerFactory.getLogger(OperatorDriver.class);

  public enum State {

    /**
     * Before the first call to next().
     */

    START,

    /**
     * Attempting to start the operator.
     */

    STARTING,

    /**
     * Read from readers.
     */

    RUN,

    /**
     * No more data to deliver.
     */

    END,

    /**
     * An error occurred.
     */

    FAILED,

    /**
     * Operation was cancelled. No more batches will be returned,
     * but close() has not yet been called.
     */

    CANCELED,

    /**
     * close() called and resources are released. No more batches
     * will be returned, but close() has not yet been called.
     * (This state is semantically identical to FAILED, it exists just
     * in case an implementation needs to know the difference between the
     * END, FAILED and CANCELED states.)
     */

    CLOSED
  }

  private OperatorDriver.State state = State.START;

  /**
   * Operator context. The driver "owns" the context and is responsible
   * for closing it.
   */

  private final OperatorContext opContext;
  private final OperatorExec operatorExec;
  private final BatchAccessor batchAccessor;
  private int schemaVersion;
  private final boolean enableSchemaBatch;

  public OperatorDriver(OperatorContext opContext, OperatorExec opExec, boolean enableSchemaBatch) {
    this.opContext = opContext;
    this.operatorExec = opExec;
    batchAccessor = operatorExec.batchAccessor();
    this.enableSchemaBatch = enableSchemaBatch;
  }

  /**
   * Get the next batch. Performs initialization on the first call.
   * @return the iteration outcome to send downstream
   */

  public IterOutcome next() {
    try {
      switch (state) {
      case START:
        return start();
      case RUN:
        return doNext();
      default:
        logger.debug("Extra call to next() in state {}: {}", state, operatorLabel());
        return IterOutcome.NONE;
      }
    } catch (UserException e) {
      cancelSilently();
      state = State.FAILED;
      throw e;
    } catch (Throwable t) {
      cancelSilently();
      state = State.FAILED;
      throw UserException.executionError(t)
        .addContext("Exception thrown from", operatorLabel())
        .build(logger);
    }
  }

  /**
   * Cancels the operator before reaching EOF.
   */

  public void cancel() {
    try {
      switch (state) {
      case START:
      case RUN:
        cancelSilently();
        break;
      default:
        break;
      }
    } finally {
      state = State.CANCELED;
    }
  }

 /**
   * Start the operator executor. Bind it to the various contexts.
   * Then start the executor and fetch the first schema.
   * @return result of the first batch, which should contain
   * only a schema, or EOF
   */

  private IterOutcome start() {
    state = State.STARTING;
    schemaVersion = -1;
    if (!enableSchemaBatch) {
      return doNext();
    }
    if (operatorExec.buildSchema()) {
      schemaVersion = batchAccessor.schemaVersion();

      // Report schema change.

      batchAccessor.container().schemaChanged();
      state = State.RUN;
      return IterOutcome.OK_NEW_SCHEMA;
    } else {
      state = State.END;
      return IterOutcome.NONE;
    }
  }

  /**
   * Fetch a record batch, detecting EOF and a new schema.
   * @return the <tt>IterOutcome</tt> for the above cases
   */

  private IterOutcome doNext() {
    if (!operatorExec.next()) {
      state = State.END;
      return IterOutcome.NONE;
    }
    int newVersion = batchAccessor.schemaVersion();
    boolean schemaChanged = newVersion != schemaVersion;

    // Set the container schema changed based on whether the
    // current schema differs from that the last time through
    // this method. That is, we take "schema changed" to be
    // "schema changed since last call to next." The result hide
    // trivial changes within this operator.

    if (schemaChanged) {
      batchAccessor.container().schemaChanged();
    }
    if (state == State.STARTING || schemaChanged) {
      schemaVersion = newVersion;
      state = State.RUN;
      return IterOutcome.OK_NEW_SCHEMA;
    }
    state = State.RUN;
    return IterOutcome.OK;
  }

  /**
   * Implement a cancellation, and ignore any exception that is
   * thrown. We're already in trouble here, no need to keep track
   * of additional things that go wrong.
   * <p>
   * Cancellation is done only if the operator is doing work.
   * The request is not propagated if either the operator never
   * started, or is already finished.
   */

  private void cancelSilently() {
    try {
      if (state == State.STARTING || state == State.RUN) {
        operatorExec.cancel();
      }
    } catch (Throwable t) {
      // Ignore; we're already in a bad state.
      logger.error("Exception thrown from cancel() for {}", operatorLabel(), t);
    }
  }

  private String operatorLabel() {
    return operatorExec.getClass().getCanonicalName();
  }

  public void close() {
    if (state == State.CLOSED) {
      return;
    }
    try {
      operatorExec.close();
    } catch (UserException e) {
      throw e;
    } catch (Throwable t) {
      throw UserException.executionError(t)
        .addContext("Exception thrown from", operatorLabel())
        .build(logger);
    } finally {
      opContext.close();
      state = State.CLOSED;
    }
  }

  public BatchAccessor batchAccessor() {
    return batchAccessor;
  }

  public OperatorContext operatorContext() {
    return opContext;
  }
}
