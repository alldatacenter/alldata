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

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.BaseValueVector;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;

/**
 * Builds a result set (series of zero or more row sets) based on a defined
 * schema which may
 * evolve (expand) over time. Automatically rolls "overflow" rows over
 * when a batch fills.
 * <p>
 * Many of the methods in this interface verify that the loader is
 * in the proper state. For example, an exception is thrown if the caller
 * attempts to save a row before starting a batch. However, the per-column
 * write methods are checked only through assertions that should enabled
 * during testing, but will be disabled during production.
 *
 * @see {@link VectorContainerWriter}, the class which this class
 * replaces
 */
public interface ResultSetLoader {

  public static final int DEFAULT_ROW_COUNT = BaseValueVector.INITIAL_VALUE_ALLOCATION;

  /**
   * Context for error messages.
   */
  CustomErrorContext errorContext();

  /**
   * Current schema version. The version increments by one each time
   * a column is added.
   * @return the current schema version
   */
  int schemaVersion();

  /**
   * Adjust the number of rows to produce in the next batch. Takes
   * affect after the next call to {@link #startBatch()}.
   *
   * @param count target batch row count
   */
  void setTargetRowCount(int count);

  /**
   * The number of rows produced by this loader (as configured in the loader
   * options.)
   *
   * @return the target row count for batches that this loader produces
   */
  int targetRowCount();

  /**
   * The maximum number of rows for the present batch. Will be the lesser
   * of the {@link #targetRowCount()) and the overall scan limit remaining.
   */
  int maxBatchSize();

  /**
   * The largest vector size produced by this loader (as specified by
   * the value vector limit.)
   *
   * @return the largest vector size. Attempting to extend a vector beyond
   * this limit causes automatic vector overflow and terminates the
   * in-flight batch, even if the batch has not yet reached the target
   * row count
   */
  int targetVectorSize();

  /**
   * Total number of batches created. Includes the current batch if
   * the row count in this batch is non-zero.
   * @return the number of batches produced including the current
   * one
   */
  int batchCount();

  /**
   * Total number of rows loaded for all previous batches and the
   * current batch.
   * @return total row count
   */
  long totalRowCount();

  /**
   * Report whether the loader currently holds rows. If within a batch,
   * reports if at least one row has been read (which might be a look-ahead
   * row.) If between batches, reports if a look-ahead row is available.
   *
   * @return true if at least one row is available to harvest, false
   * otherwise
   */
  boolean hasRows();

  /**
   * Start a new row batch. Valid only when first started, or after the
   * previous batch has been harvested.
   *
   * @return {@code true} if another batch can be read, {@code false} if
   * the reader has reached the given scan limit.
   */
  boolean startBatch();

  /**
   * Writer for the top-level tuple (the entire row). Valid only when
   * the mutator is actively writing a batch (after <tt>startBatch()</tt>
   * but before </tt>harvest()</tt>.)
   *
   * @return writer for the top-level columns
   */
  RowSetLoader writer();

  /**
   * Reports whether the loader is in a writable state. The writable state
   * occurs only when a batch has been started, and before that batch
   * becomes full.
   *
   * @return true if the client can add a row to the loader, false if
   * not
   */
  boolean writeable();

  /**
   * Load a row using column values passed as variable-length arguments. Expects
   * map values to represented as an array.
   * A schema of (a:int, b:map(c:varchar)) would be>
   * set as <br><tt>loadRow(10, new Object[] {"foo"});</tt><br>
   * Values of arrays can be expressed as a Java
   * array. A schema of (a:int, b:int[]) can be set as<br>
   * <tt>loadRow(10, new int[] {100, 200});</tt><br>.
   * Primarily for testing, too slow for production code.
   * <p>
   * If the row consists of a single map or list, then the one value will be an
   * <tt>Object</tt> array, creating an ambiguity. Use <tt>writer().set(0, value);</tt>
   * in this case.
   *
   * @param values column values in column index order
   * @return this loader
   */
  ResultSetLoader setRow(Object...values);

  /**
   * Requests to skip the given number of rows. Returns the number of rows
   * actually skipped (which is limited by batch count.)
   * <p>
   * Used in <tt>SELECT COUNT(*)</tt> style queries when the downstream
   * operators want just record count, but no actual rows.
   * <p>
   * Also used to fill in a batch of only null values (such a filling
   * in a set of null vectors for unprojected columns.)
   *
   * @param requestedCount
   *          the number of rows to skip
   * @return the actual number of rows skipped, which may be less than the
   *         requested amount. If less, the client should call this method for
   *         multiple batches until the requested count is reached
   */
  int skipRows(int requestedCount);

  /**
   * Reports if this is an empty projection such as occurs in a
   * <tt>SELECT COUNT(*)</tt> query. If the projection is empty, then
   * the downstream needs only the row count set in each batch, but no
   * actual vectors will be created. In this case, the client can do
   * the work to populate rows (the data will be discarded), or can call
   * {@link #skipRows(int)} to skip over the number of rows that would
   * have been read if any data had been projected.
   * <p>
   * Note that the empty schema case can also occur if the project list
   * from the <tt>SELECT</tt> clause is disjoint from the table schema.
   * For example, <tt>SELECT a, b</tt> from a table with schema
   * <tt>(c, d)</tt>.
   *
   * @return true if no columns are actually projected, false if at
   * least one column is projected
   */
  boolean isProjectionEmpty();

  /**
   * Returns the active output schema; the schema used by the writers,
   * minus any unprojected columns. This is usually the same as the
   * output schema, but may differ if the writer adds columns during
   * an overflow row. Unlike the output schema, this schema is defined
   * as long as the loader is open.
   */
  TupleMetadata activeSchema();

  /**
   * Returns the output container which holds (or will hold) batches
   * from this loader. For use when the container is needed prior
   * to "harvesting" a batch. The data is not valid until
   * {@link #harvest()} is called, and is no longer valid once
   * {@link #startBatch()} is called.
   *
   * @return container used to publish results from this loader
   */
  VectorContainer outputContainer();

  /**
   * Harvest the current row batch, and reset the mutator
   * to the start of the next row batch (which may already contain
   * an overflow row.
   * <p>
   * The schema of the returned container is defined as:
   * <ul>
   * <li>The schema as passed in via the loader options, plus</li>
   * <li>Columns added dynamically during write, minus</li>
   * <li>Any columns not included in the project list, minus</li>
   * <li>Any columns added in the overflow row.</li>
   * </ul>
   * That is, column order is as defined by the initial schema and column
   * additions. In particular, the schema order is <b>not</b> defined by
   * the projection list. (Another mechanism is required to reorder columns
   * for the actual projection.)
   *
   * @return the row batch to send downstream
   */
  VectorContainer harvest();

  /**
   * After a {@link #harvest()}, call, call this method to determine if
   * the scan limit has been hit. If so, treat this as the final batch
   * for the reader, even if more data is available to read.
   *
   * @return {@code true} if the scan has reached a set scan row limit,
   * {@code false} if there is no limit, or more rows can be read.
   */
  boolean atLimit();

  /**
   * The schema of the harvested batch. Valid until the start of the
   * next batch.
   *
   * @return the extended schema of the harvested batch which includes
   * any allocation hints used when creating the batch
   */
  TupleMetadata outputSchema();

  /**
   * Peek at the internal vector cache for readers that need a bit of help
   * resolving types based on what was previously seen.
   *
   * @return real or dummy vector cache
   */
  ResultVectorCache vectorCache();

  /**
   * Called after all rows are returned, whether because no more data is
   * available, or the caller wishes to cancel the current row batch
   * and complete.
   */
  void close();
}
