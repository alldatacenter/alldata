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

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.vector.ValueVector;

/**
 * A record batch contains a set of field values for a particular range of
 * records.
 * <p>
 *   In the case of a record batch composed of ValueVectors, ideally a batch
 *   fits within L2 cache (~256kB per core).  The set of value vectors does
 *   not change except during a call to {@link #next()} that returns
 *   {@link IterOutcome#OK_NEW_SCHEMA} value.
 * </p>
 * <p>
 *   A key thing to know is that the Iterator provided by a record batch must
 *   align with the rank positions of the field IDs provided using
 *   {@link #getValueVectorId}.
 * </p>
 */
public interface RecordBatch extends VectorAccessible {

  /** max num of rows in a batch, limited by 2-byte length in SV2: 65536 = 2^16 */
  int MAX_BATCH_ROW_COUNT = ValueVector.MAX_ROW_COUNT;

  /**
   * Describes the outcome of incrementing RecordBatch forward by a call to
   * {@link #next()}.
   * <p>
   *   Key characteristics of the return value sequence:
   * </p>
   * <ul>
   *   <li>
   *     {@code OK_NEW_SCHEMA} always appears unless {@code STOP} appears.  (A
   *     batch returns {@code OK_NEW_SCHEMA} before returning {@code NONE} even
   *     if the batch has zero rows.)
   *   </li>
   *   <li>{@code OK_NEW_SCHEMA} always appears before {@code OK} appears.</li>
   *   <li>
   *     The last value is always {@code NONE} or {@code STOP}, and {@code NONE}
   *     and {@code STOP} appear only as the last value.
   *   </li>
   * </ul>
   * <p>
   *  <h4>Details</h4>
   * </p>
   * <p>
   *   For normal completion, the basic sequence of return values from calls to
   *   {@code next()} on a {@code RecordBatch} is:
   * </p>
   * <ol>
   *   <li>
   *     an {@link #OK_NEW_SCHEMA} value followed by zero or more {@link #OK}
   *     values,
   *   </li>
   *   <li>
   *     zero or more subsequences each having an {@code OK_NEW_SCHEMA} value
   *     followed by zero or more {@code OK} values, and then
   *   </li>
   *   <li>
   *     a {@link #NONE} value.
   *   </li>
   * </ol>
   * <p>
   *   In addition to that basic sequence, {@link #NOT_YET}
   *   values can appear anywhere in the subsequence
   *   before the terminal value ({@code NONE} or {@code STOP}).
   * </p>
   * <p>
   *   For abnormal termination, the sequence is truncated (before the
   *   {@code NONE}) and ends with an exception.  That is, the sequence begins
   *   with a subsequence that is some prefix of a normal-completion sequence
   *   and that does not contain {@code NONE}, and ends with the
   *   caller throwing a (query-fatal) exception.
   * </p>
   * <p>
   *   The normal-completion return sequence is matched by the following
   *   regular-expression-style grammar:
   *   <pre>
   *     ( NOT_YET*  OK_NEW_SCHEMA
   *       NOT_YET*  OK )*
   *     )+
   *     NOT_YET*    &lt;exception></pre>
   * </p>
   * <h4>Obsolete Outcomes</h4>
   *
   * The former {@code OUT_OF_MEMORY} state was never really used.
   * It is now handled by calling
   * {@link FragmentContext#requestMemory()}
   * at the point that the operator realizes it is short on memory.
   * <p>
   * The former {@code STOP} state was replaced with a "fail fast"
   * approach that throws an exception when an error is detected.
   */
  enum IterOutcome {
    /**
     * Normal completion of batch.
     * <p>
     *   The call to {@link #next()}
     *   read no records,
     *   the batch has and will have no more results to return,
     *   and {@code next()} must not be called again.
     * </p>
     * <p>
     *   This value will be returned only after {@link #OK_NEW_SCHEMA} has been
     *   returned at least once (not necessarily <em>immediately</em> after).
     * </p>
     * <p>
     *   Also after a RecordBatch returns NONE a RecordBatch should:
     *   <ul>
     *     <li>Contain the last valid schema seen by the operator.</li>
     *     <li>Contain a VectorContainer with empty columns corresponding to the last valid schema.</li>
     *     <li>Return a record count of 0.</li>
     *   </ul>
     * </p>
     */
    NONE(false),

    /**
     * Zero or more records with same schema.
     * <p>
     *   The call to {@link #next()}
     *   read zero or more records,
     *   the schema has not changed since the last time {@code OK_NEW_SCHEMA}
     *     was returned,
     *   and the batch will have more results to return (at least completion or
     *     abnormal termination ({@code NONE} or {@code STOP})).
     *     ({@code next()} should be called again.)
     * </p>
     * <p>
     *   This will be returned only after {@link #OK_NEW_SCHEMA} has been
     *   returned at least once (not necessarily <em>immediately</em> after).
     * </p>
     */
    OK(false),

    /**
     * New schema, maybe with records.
     * <p>
     *   The call to {@link #next()}
     *   changed the schema and vector structures
     *   and read zero or more records,
     *   and the batch will have more results to return (at least completion or
     *     abnormal termination ({@code NONE} or {@code STOP})).
     *     ({@code next()} should be called again.)
     * </p>
     */
    OK_NEW_SCHEMA(false),

    /**
     * No data yet.
     * <p>
     *   The call to {@link #next()}
     *   read no data,
     *   and the batch will have more results to return in the future (at least
     *     completion or abnormal termination ({@code NONE} or {@code STOP})).
     *   The caller should call {@code next()} again, but should do so later
     *     (including by returning {@code NOT_YET} to its caller).
     * </p>
     * <p>
     *   Normally, the caller should perform any locally available work while
     *   waiting for incoming data from the callee, for example, doing partial
     *   sorts on already received data while waiting for additional data to
     *   sort.
     * </p>
     * <p>
     *   Used by batches that haven't received incoming data yet.
     * </p>
     */
    NOT_YET(false),

    /**
     * Emit record to produce output batches.
     * <p>
     *   The call to {@link #next()},
     *   read zero or more records with no change in schema as compared to last
     *   time. It is an indication from upstream operator to unblock and
     *   produce an output batch based on all the records current operator
     *   possess. The caller should return this outcome to it's downstream
     *   operators except LateralJoinRecordBatch, which will consume any EMIT
     *   from right branch but will pass through EMIT from left branch.
     * </p>
     * <p>
     *   Caller should produce one or more output record batch based on all the
     *   current data and restart fresh for any new input. If there are multiple
     *   output batches then caller should send EMIT only with last batch and OK
     *   with all previous batches.
     *   For example: Hash Join when received EMIT on build side will stop build
     *   side and call next() on probe side until it sees EMIT. On seeing EMIT
     *   from probe side, it should perform JOIN and produce output batches.
     *   Later it should clear all the data on both build and probe side of
     *   input and again start from build side.
     * </p>
     */
    EMIT(false);

    private final boolean error;

    IterOutcome(boolean error) {
      this.error = error;
    }

    public boolean isError() {
      return error;
    }
  }

  /**
   * Gets the FragmentContext of the current query fragment.  Useful for
   * reporting failure information or other query-level information.
   */
  FragmentContext getContext();

  /**
   * Gets the current schema of this record batch.
   * <p>
   *   May be called only when the most recent call to {@link #next}, if any,
   *   returned {@link IterOutcome#OK_NEW_SCHEMA} or {@link IterOutcome#OK}.
   * </p>
   * <p>
   *   The schema changes when and only when {@link #next} returns
   *   {@link IterOutcome#OK_NEW_SCHEMA}.
   * </p>
   */
  @Override
  BatchSchema getSchema();

  /**
   * Informs child operators that no more data is needed. Only called
   * for "normal" cancellation to avoid unnecessary compute in any worker
   * threads. For the error case, the fragment
   * executor will call close() on each child automatically.
   * <p>
   * The operator which triggers the cancel MUST send a <code>NONE</code>
   * status downstream, or throw an exception. It is not legal to
   * call <code>next()</code> on an operator after calling its
   * <code>cancel()</code> method.
   */
  void cancel();

  VectorContainer getOutgoingContainer();

  /**
   *  Return the internal vector container
   *
   * @return The internal vector container
   */
  VectorContainer getContainer();

  /**
   * Gets the value vector type and ID for the given schema path.  The
   * TypedFieldId should store a fieldId which is the same as the ordinal
   * position of the field within the Iterator provided this class's
   * implementation of {@code Iterable<ValueVector>}.
   *
   * @param path
   *          The path where the vector should be located.
   * @return The local field id associated with this vector. If no field matches this path, this will return a null
   *         TypedFieldId
   */
  @Override
  TypedFieldId getValueVectorId(SchemaPath path);

  @Override
  VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids);

  /**
   * Updates the data in each Field reading interface for the next range of
   * records.
   * <p>
   *   Once a RecordBatch's {@code next()} has returned {@link IterOutcome#NONE}
   *   or {@link IterOutcome#STOP}, the consumer should no longer call
   *   {@code next()}.  Behavior at this point is undefined and likely to
   *   throw an exception.
   * </p>
   * <p>
   *   See {@link IterOutcome} for the protocol (possible sequences of return
   *   values).
   * </p>
   *
   *
   * @return An IterOutcome describing the result of the iteration.
   */
  IterOutcome next();

  /**
   * Gets a writable version of this batch.  Takes over ownership of existing
   * buffers.
   */
  WritableBatch getWritableBatch();

  /**
   * Perform dump of this batch's state to logs.
   */
  void dump();
}
