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
package org.apache.drill.exec.physical.resultSet;

import org.apache.drill.exec.physical.impl.aggregate.BatchIterator;
import org.apache.drill.exec.record.VectorContainer;

/**
 * Copies rows from an input batch to an output batch. The input
 * batch is assumed to have a selection vector, or the caller
 * will pick the rows to copy.
 * <p>
 * Works to create full output batches to minimize per-batch
 * overhead and to eliminate unnecessary empty batches if no
 * rows are copied.
 * <p>
 * The output batches are assumed to have the same schema as
 * input batches. (No projection occurs.) The output schema will
 * change each time the input schema changes. (For an SV4, then
 * the upstream operator must have ensured all batches covered
 * by the SV4 have the same schema.)
 * <p>
 * This implementation works with a single stream of batches which,
 * following Drill's rules, must consist of the same set of vectors on
 * each non-schema-change batch.
 *
 * <h4>Protocol</h4>
 *
 * Overall lifecycle:
 * <ol>
 * <li>Create an instance of the
 *     {@link org.apache.drill.exec.physical.resultSet.impl.ResultSetCopierImpl
 *      ResultSetCopierImpl} class, passing the input row set reader
 *      to the constructor.</li>
 * <li>Loop to process each output batch as shown below. That is, continually
 *     process calls to the {@link BatchIterator#next()} method.</li>
 * <li>Call {@link #close()}.</li>
 * </ol>
 * <p>
 *
 * To build each output batch:
 *
 * <pre><code>
 * public IterOutcome next() {
 *   copier.startOutputBatch();
 *   while (!copier.isFull() {
 *     IterOutcome innerResult = inner.next();
 *     if (innerResult == DONE) { break; }
 *     copier.startInputBatch();
 *     copier.copyAllRows();
 *   }
 *   if (copier.hasRows()) {
 *     outputContainer = copier.harvest();
 *     return outputContainer.isSchemaChanged() ? OK_NEW_SCHEMA ? OK;
 *   } else { return DONE; }
 * }
 * </code></pre>
 * <p>
 * The above assumes that the upstream operator can be polled
 * multiple times in the DONE state. The extra polling is
 * needed to handle any in-flight copies when the input
 * exhausts its batches.
 * <p>
 * The above also shows that the copier handles and reports
 * schema changes by setting the schema change flag in the
 * output container. Real code must handle multiple calls to
 * next() in the DONE state, and work around lack of such support
 * in its input (perhaps by tracking a state.)
 * <p>
 * An input batch is processed by copying the rows. Copying can be done
 * row-by row, via a row range, or by copying the entire input batch as
 * shown in the example.
 * Copying the entire batch make sense when the input batch carries as
 * selection vector that identifies which rows to copy, in which
 * order.
 * <p>
 * Because we wish to fill the output batch, we may be able to copy
 * part of a batch, the whole batch, or multiple batches to the output.
 */
public interface ResultSetCopier {

  /**
   * Start the next output batch.
   */
  void startOutputBatch();

  /**
   * Start the next input batch. The input batch must be held
   * by the {@code ResultSetReader} passed into the constructor.
   */
  boolean nextInputBatch();

  /**
   * If copying rows one by one, copy the next row from the
   * input.
   *
   * @return true if more rows remain on the input, false
   * if all rows are exhausted
   */
  boolean copyNextRow();

  /**
   * Copy a row at the given position. For those cases in
   * which random copying is needed, but a selection vector
   * is not available. Note that this version is slow because
   * of the need to reset indexes for every row. Better to
   * use a selection vector, then copy sequentially.
   *
   * @param inputRowIndex the input row position. If a selection vector
   * is attached, then this is the selection vector position
   */
  void copyRow(int inputRowIndex);

  /**
   * Copy all (remaining) input rows to the output.
   * If insufficient space exists in the output, does a partial
   * copy, and {@link #isCopyPending()} will return true.
   */
  void copyAllRows();

  /**
   * Reports if the output batch has rows. Useful after the end
   * of input to determine if a partial output batch exists to
   * send downstream.
   * @return true if the output batch has one or more rows
   */
  boolean hasOutputRows();

  /**
   * Reports if the output batch is full and must be sent
   * downstream. The output batch can be full in the middle
   * of a copy, in which case {@link #isCopyPending()} will
   * also return true.
   * <p>
   * This function also returns true if a schema change
   * occurred on the latest input row, in which case the
   * partially-completed batch of the old schema must be
   * flushed downstream.
   *
   * @return true if the output is full and must be harvested
   * and sent downstream
   */
  boolean isOutputFull();

  /**
   * Helper method to determine if a copy is pending: more rows
   * remain to be copied. If so, start a new output batch, which
   * will finish the copy. Do that before start a new input
   * batch.
   * @return
   */
  boolean isCopyPending();

  /**
   * Obtain the output batch. Returned as a vector container
   * since the output will not have a selection vector.
   *
   * @return a vector container holding the output batch
   */
  VectorContainer harvest();

  /**
   * Release resources, including any pending input batch
   * and any non-harvested output batch.
   */
  void close();
}
