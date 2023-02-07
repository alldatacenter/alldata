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

import org.apache.drill.exec.ops.OperatorContext;

/**
 * Core protocol for a Drill operator execution.
 *
 * <h4>Lifecycle</h4>
 *
 * <ul>
 * <li>Creation via an operator-specific constructor in the
 * corresponding <tt>RecordBatchCreator</tt>.</li>
 * <li><tt>bind()</tt> called to provide the operator services.</li>
 * <li><tt>buildSchema()</tt> called to define the schema before
 * fetching the first record batch.</li>
 * <li><tt>next()</tt> called repeatedly to prepare each new record
 * batch until EOF or until cancellation.</li>
 * <li><tt>cancel()</tt> called if the operator should quit early.</li>
 * <li><tt>close()</tt> called to release resources. Note that
 * <tt>close()</tt> is called in response to:<ul>
 *   <li>EOF</li>
 *   <li>After <tt>cancel()</tt></li>
 *   <li>After an exception is thrown.</li></ul></li>
 * </ul>
 *
 * <h4>Error Handling</h4>
 *
 * Any method can throw an (unchecked) exception. (Drill does not use
 * checked exceptions.) Preferably, the code will throw a
 * <tt>UserException</tt> that explains the error to the user. If any
 * other kind of exception is thrown, then the enclosing class wraps it
 * in a generic <tt>UserException</tt> that indicates that "something went
 * wrong", which is less than ideal.
 *
 * <h4>Result Set</h4>
 * The operator "publishes" a result set in response to returning
 * <tt>true</tt> from <tt>next()</tt> by populating a
 * {@link BatchAccesor} provided via {@link #batchAccessor()}. For
 * compatibility with other Drill operators, the set of vectors within
 * the batch must be the same from one batch to the next.
 */

public interface OperatorExec {

  /**
   * Bind this operator to the context. The context provides access
   * to per-operator, per-fragment and per-Drillbit services.
   * Also provides access to the operator definition (AKA "pop
   * config") for this operator.
   *
   * @param context operator context
   */

  public void bind(OperatorContext context);

  /**
   * Provides a generic access mechanism to the batch's output data.
   * This method is called after a successful return from
   * {@link #buildSchema()} and {@link #next()}. The batch itself
   * can be held in a standard {@link VectorContainer}, or in some
   * other structure more convenient for this operator.
   *
   * @return the access for the batch's output container
   */

  BatchAccessor batchAccessor();

  /**
   * Retrieves the schema of the batch before the first actual batch
   * of data. The schema is returned via an empty batch (no rows,
   * only schema) from {@link #batchAccessor()}.
   *
   * @return true if a schema is available, false if the operator
   * reached EOF before a schema was found
   */

  boolean buildSchema();

  /**
   * Retrieves the next batch of data. The data is returned via
   * the {@link #batchAccessor()} method.
   *
   * @return true if another batch of data is available, false if
   * EOF was reached and no more data is available
   */

  boolean next();

  /**
   * Alerts the operator that the query was cancelled. Generally
   * optional, but allows the operator to realize that a cancellation
   * was requested.
   */

  void cancel();

  /**
   * Close the operator by releasing all resources that the operator
   * held. Called after {@link #cancel()} and after {@link #batchAccessor()}
   * or {@link #next()} returns false.
   * <p>
   * Note that there may be a significant delay between the last call to
   * <tt>next()</tt> and the call to <tt>close()</tt> during which
   * downstream operators do their work. A tidy operator will release
   * resources immediately after EOF to avoid holding onto memory or other
   * resources that could be used by downstream operators.
   */

  void close();
}
