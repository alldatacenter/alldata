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

import org.apache.drill.exec.vector.accessor.TupleWriter;

/**
 * Interface for writing values to a row set. Only available for newly-created
 * single row sets.
 * <p>
 * Typical usage:
 *
 * <pre></code>
 * void writeABatch() {
 *   RowSetLoader writer = ...
 *   while (! writer.isFull()) {
 *     writer.start();
 *     writer.scalar(0).setInt(10);
 *     writer.scalar(1).setString("foo");
 *     ...
 *     writer.save();
 *   }
 * }</code></pre>
 * Alternative usage:
 *
 * <pre></code>
 * void writeABatch() {
 *   RowSetLoader writer = ...
 *   while (writer.start()) {
 *     writer.scalar(0).setInt(10);
 *     writer.scalar(1).setString("foo");
 *     ...
 *     writer.save();
 *   }
 * }</code></pre>
 *
 * The above writes until the batch is full, based on size or vector overflow.
 * That is, the details of vector overflow are hidden from the code that calls
 * the writer.
 */

public interface RowSetLoader extends TupleWriter {

  ResultSetLoader loader();

  /**
   * Write a row of values, given by Java objects. Object type must match
   * expected column type. Stops writing, and returns false, if any value causes
   * vector overflow. Value format:
   * <ul>
   * <li>For scalars, the value as a suitable Java type (int or Integer, say,
   * for <tt>INTEGER</tt> values.)</li>
   * <li>For scalar arrays, an array of a suitable Java primitive type for
   * scalars. For example, <tt>int[]</tt> for an <tt>INTEGER</tt> column.</li>
   * <li>For a Map, an <tt>Object<tt> array with values encoded as above.
   * (In fact, the list here is the same as the map format.</li>
   * <li>For a list (repeated map, list of list), an <tt>Object</tt> array with
   * values encoded as above. (So, for a repeated map, an outer <tt>Object</tt>
   * map encodes the array, an inner one encodes the map members.</li>
   * </ul>
   *
   * @param values
   *          variable-length argument list of column values
   * @return this writer
   */
  RowSetLoader addRow(Object... values);

  /**
   * Similar to {@link #addRow(Object...)}, but for the odd case in which a
   * row consists of a single column that is an object array (such as for
   * a list or map) and so is ambiguous.
   *
   * @param value value of the one and only column
   * @return this writer
   */
  RowSetLoader addSingleCol(Object value);

  /**
   * Indicates that no more rows fit into the current row batch and that the row
   * batch should be harvested and sent downstream. Any overflow row is
   * automatically saved for the next cycle. The value is undefined when a batch
   * is not active.
   * <p>
   * Will be false on the first row, and all subsequent rows until either the
   * maximum number of rows are written, or a vector overflows. After that, will
   * return true. The method returns false as soon as any column writer
   * overflows even in the middle of a row write. That is, this writer does not
   * automatically handle overflow rows because that added complexity is seldom
   * needed for tests.
   *
   * @return true if another row can be written, false if not
   */
  boolean isFull();

  /**
   * Used to push a limit down to the file reader. This method checks to see whether
   * the maxRecords parameter is not zero (for no limit) and is not greater than the
   * current record count.
   * @param maxRecords Maximum rows to be returned. (From the limit clause of the query)
   * @return True if the row count exceeds the maxRecords, false if not.
   */
  @Deprecated() // use the limit in options instead
  boolean limitReached(int maxRecords);

  /**
   * The number of rows in the current row set. Does not count any overflow row
   * saved for the next batch.
   *
   * @return number of rows to be sent downstream
   */
  int rowCount();

  /**
   * The index of the current row. Same as the row count except in an overflow
   * row in which case the row index will revert to zero as soon as any vector
   * overflows. Note: this means that the index can change between columns in a
   * single row. Applications usually don't use this index directly; rely on the
   * writers to write to the proper location.
   *
   * @return the current write index
   */
  int rowIndex();

  /**
   * Prepare a new row for writing. Call this before each row.
   * <p>
   * Handles a very special case: that of discarding the last row written.
   * A reader can read a row into vectors, then "sniff" the row to check,
   * for example, against a filter. If the row is not wanted, simply omit
   * the call to <tt>save()</tt> and the next all to <tt>start()</tt> will
   * discard the unsaved row.
   * <p>
   * Note that the vectors still contain values in the
   * discarded position; just the various pointers are unset. If
   * the batch ends before the discarded values are overwritten, the
   * discarded values just exist at the end of the vector. Since vectors
   * start with garbage contents, the discarded values are simply a different
   * kind of garbage. But, if the client writes a new row, then the new
   * row overwrites the discarded row. This works because we only change
   * the tail part of a vector; never the internals.
   *
   * @return true if another row can be added, false if the batch is full
   */
  boolean start();

  /**
   * Saves the current row and moves to the next row. Failing to call this
   * method effectively abandons the in-flight row; something that may be useful
   * to recover from partially-written rows that turn out to contain errors.
   * Done automatically if using <tt>setRow()</tt>.
   */
  void save();
}