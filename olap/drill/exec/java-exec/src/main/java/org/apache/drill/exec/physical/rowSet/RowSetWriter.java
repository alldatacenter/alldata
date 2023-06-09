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
package org.apache.drill.exec.physical.rowSet;

import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;

/**
 * Interface for writing values to a row set. Only available
 * for newly-created, single, direct row sets. Eventually, if
 * we want to allow updating a row set, we have to create a
 * new row set with the updated columns, then merge the new
 * and old row sets to create a new immutable row set.
 * <p>
 * Typical usage:
 * <pre></code>
 * void writeABatch() {
 *   RowSetWriter writer = ...
 *   while (! writer.isFull()) {
 *     writer.scalar(0).setInt(10);
 *     writer.scalar(1).setString("foo");
 *     ...
 *     writer.save();
 *   }
 * }</code></pre>
 * The above writes until the batch is full, based on size. If values
 * are large enough to potentially cause vector overflow, do the
 * following instead:
 * <pre></code>
 * void writeABatch() {
 *   RowSetWriter writer = ...
 *   while (! writer.isFull()) {
 *     writer.column(0).setInt(10);
 *     try {
 *        writer.column(1).setString("foo");
 *     } catch (VectorOverflowException e) { break; }
 *     ...
 *     writer.save();
 *   }
 *   // Do something with the partially-written last row.
 * }</code></pre>
 * <p>
 * This writer is for testing, so no provision is available to handle a
 * partial last row. (Elsewhere n Drill there are classes that handle that case.)
 */

public interface RowSetWriter extends TupleWriter {

  /**
   * Write a row of values, given by Java objects. Object type must
   * match expected column type. Stops writing, and returns false,
   * if any value causes vector overflow. Value format:
   * <ul>
   * <li>For scalars, the value as a suitable Java type (int or
   * Integer, say, for <tt>INTEGER</tt> values.)</li>
   * <li>For scalar arrays, an array of a suitable Java primitive type
   * for scalars. For example, <tt>int[]</tt> for an <tt>INTEGER</tt>
   * column.</li>
   * <li>For a Map, an <tt>Object<tt> array with values encoded as above.
   * (In fact, the list here is the same as the map format.</li>
   * <li>For a list (repeated map, list of list), an <tt>Object</tt>
   * array with values encoded as above. (So, for a repeated map, an outer
   * <tt>Object</tt> map encodes the array, an inner one encodes the
   * map members.</li>
   * </ul>
   *
   * @param values variable-length argument list of column values
   */

  RowSetWriter addRow(Object...values);
  RowSetWriter addSingleCol(Object value);

  /**
   * Indicates if the current row position is valid for
   * writing. Will be false on the first row, and all subsequent
   * rows until either the maximum number of rows are written,
   * or a vector overflows. After that, will return true. The
   * method returns false as soon as any column writer overflows
   * even in the middle of a row write. That is, this writer
   * does not automatically handle overflow rows because that
   * added complexity is seldom needed for tests.
   *
   * @return true if the current row can be written, false
   * if not
   */

  boolean isFull();
  int rowIndex();

  /**
   * Saves the current row and moves to the next row.
   * Done automatically if using <tt>setRow()</tt>.
   */

  void save();

  /**
   * Finish writing and finalize the row set being
   * written.
   * @return the completed, read-only row set without a
   * selection vector
   */

  SingleRowSet done();
}
