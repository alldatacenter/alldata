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

import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Iterates over the set of batches in a result set, providing
 * a row set reader to iterate over the rows within each batch.
 * Handles schema changes between batches. A typical use is to
 * iterate over batches from an upstream operator. Protocol:
 *
 * <h4>Protocol</h4>
 * <ol>
 * <li>Create an instance.</li>
 * <li>For each incoming batch:
 *   <ol>
 *   <li>Call {@link #start()} to attach the batch. The associated
 *       {@link BatchAccessor} reports if the schema has changed.</li>
 *   <li>Call {@link #reader()} to obtain a reader.</li>
 *   <li>Iterate over the batch using the reader.</li>
 *   <li>Call {@link #release()} to free the memory for the
 *       incoming batch. Or, to call {@link #detach()} to keep
 *       the batch memory.</li>
 *   </ol>
 * <li>Call {@link #close()} after all batches are read.</li>
 * </ol>
 * <ul>
 * <li>Create the result set reader via a specific subclass.
 * If a query has a null result (no rows,
 * no schema), the code which creates this class should instead
 * indicate that no results are available. This class is only for
 * the cases </li>
 * <li>Call {@link #schema()}, if desired, to obtain the schema
 * for this result set.</li>
 * <li>Call {@link #next()} to advance to the first batch.</li>
 * <li>If {@code next()} returns {@code true}, then call
 * {@link #reader()} to obtain a reader over rows. This reader also
 * provides the batch schema.</li>
 * <li>Use the reader to iterate over rows in the batch.</li>
 * <li>Call {@code next()} to advance to the next batch and
 * repeat.</li>
 * </ul>
 * <p>
 * The implementation may perform complex tasks behind the scenes:
 * coordinate with the query runner (if remote), drive an operator
 * (if within a DAG), etc. The implementation takes an interface
 * that interfaces with the source of batches.
 * <p>
 * Designed to handle batches arriving from a single upstream
 * operator. Uses Drill's strict form of schema identity: that
 * not only must the column definitions match; the vectors must
 * be identical from one batch to the next. If the vectors differ,
 * then this class assumes a new schema has occurred, and will
 * rebuild all the underlying readers, which can be costly.
 */
public interface PullResultSetReader {

  /**
   * Advance to the next batch of data. The iterator starts
   * positioned before the first batch (but after obtaining
   * a schema.)
   * @return {@code true} if another batch is available,
   * {@code false} if EOF
   */
  boolean next();

  /**
   * Return the schema for this result set.
   */
  TupleMetadata schema();

  int schemaVersion();

  /**
   * Obtain a reader to iterate over the rows of the batch. The return
   * value will likely be the same reader each time, so that this call
   * is optional after the first batch.
   */
  RowSetReader reader();

  /**
   * Close this reader. Releases any memory still assigned
   * to any attached batch. Call {@link #detach()} first if
   * you want to preserve the batch memory.
   */
  void close();
}
