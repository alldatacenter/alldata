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

/**
 * Provides a second-generation row set (AKA "record batch") writer used
 * by client code to<ul>
 * <li>Define the schema of a result set.</li>
 * <li>Write data into the vectors backing a row set.</li></ul>
 * <p>
 * <h4>Terminology</h4>
 * The code here follows the "row/column" naming convention rather than
 * the "record/field" convention.
 * <dl>
 * <dt>Result set</dt>
 * <dd>A set of zero or more row sets that hold rows of data.<dd>
 * <dt>Row set</dt>
 * <dd>A collection of rows with a common schema. Also called a "row
 * batch" or "record batch." (But, in Drill, the term "record batch" also
 * usually means an operator on that set of records. Here, a row set is
 * just the rows &nash; separate from operations on that data.</dd>
 * <dt>Row</dt>
 * <dd>A single row of data, in the usual database sense. Here, a row is
 * a kind of tuple (see below) allowing both name and index access to
 * columns.</dd>
 * <dt>Tuple</dt>
 * <dd>In relational theory, a row is a tuple: a collection of values
 * defined by a schema. Tuple values are indexed by position or name.</dd>
 * <dt>Column</dt>
 * <dd>A single value within a row or row set. (Generally, the context
 * makes clear if the term refers to single value or all values for a
 * column for a row set. Columns are backed by value vectors.</dd>
 * <dt>Map</dt>
 * <dd>In Drill, a map is what other systems call a "structure". It is,
 * in fact, a nested tuple. In a Java or Python map, each map instance has
 * a distinct set of name/value pairs. But, in Drill, all map instances have
 * the same schema; hence the so-called "map" is really a tuple. This
 * implementation exploits that fact and treats the row, and nested maps,
 * almost identically: both provide columns indexed by name or position.</dd>
 * <dt>Row Set Mutator</dt>
 * <dd>An awkward name, but retains the "mutator" name from the previous
 * generation. The mechanism to build a result set as series of row sets.</dd>
 * <dt>Tuple Loader</dt>
 * <dd>Mechanism to build a single tuple (row or map) by providing name
 * or index access to columns. A better name would b "tuple writer", but
 * that name is already used elsewhere.</dd>
 * <dt>Column Loader</dt>
 * <dd>Mechanism to write values to a single column.<dd>
 * </dl>
 * <h4>Building the Schema</h4>
 * The row set mutator works for two cases: a known schema or a discovered
 * schema. A known schema occurs in the case, such as JDBC, where the
 * underlying data source can describe the schema before reading any rows.
 * In this case, client code can build the schema and pass that schema to
 * the mutator directly. Alternatively, the client code can build the
 * schema column-by-column before the first row is read.
 * <p>
 * Readers that discover schema can build the schema incrementally: add
 * a column, load data for that column for one row, discover the next
 * column, and so on. Almost any kind of column can be added at any time
 * within the first batch:<ul>
 * <li>Required columns are "back-filled" with zeros in the active batch,
 * if that value
 * makes sense for the column. (Date and Interval columns will throw an
 * exception if added after the first row as there is no good "zero"
 * value for that column. Varchar columns are back-filled with blanks.<li>
 * <li>Optional (nullable) columns can be added at any time; they are
 * back-filled with nulls in the active batch. In general, if a column is
 * added after the first row, it should be nullable, not required, unless
 * the data source has a "missing = blank or zero" policy.</li>
 * <li>Repeated (array) columns can be added at any time; they are
 * back-filled with empty entries in the first batch. Arrays can also be
 * safely added at any time.</li></ul>
 * Client code must be aware of the semantics of adding columns at various
 * times.<ul>
 * <li>Columns added before or during the first row are the trivial case;
 * this works for all data types and modes.</li>
 * <li>Required (non-nullable0 structured columns (Date, Period) cannot be
 * added after the first row (as there is no good zero-fill value.)</li>
 * <li>Columns added within the first batch appear to the rest of Drill as
 * if they were added before the first row: the downstream operators see the
 * same schema from batch to batch.</li>
 * <li>Columns added <i>after</i> the first batch will trigger a
 * schema-change event downstream.</li>
 * <li>The above is true during an "overflow row" (see below.) Once
 * overflow occurs, columns added later in that overflow row will actually
 * appear in the next batch, and will trigger a schema change when that
 * batch is returned. That is, overflow "time shifts" a row addition from
 * one batch to the next, and so it also time-shifts the column addition.
 * </li></ul>
 * Use the {@link org.apache.drill.exec.record.metadata.TupleBuilder} class
 * to build the schema. The schema class is part of the
 * {@link org.apache.drill.exec.physical.resultSet.RowSetLoader} object available from the
 * {@link org.apache.drill.exec.physical.resultSet.ResultSetLoader#writer()} method.
 * <h4>Using the Schema</h4>
 * Presents columns using a physical schema. That is, map columns appear
 * as columns that provide a nested map schema. Presumes that column
 * access is primarily structural: first get a map, then process all
 * columns for the map.
 * <p>
 * If the input is a flat structure, then the physical schema has a
 * flattened schema as the degenerate case.
 * <p>
 * In both cases, access to columns is by index or by name. If new columns
 * are added while loading, their index is always at the end of the existing
 * columns.
 * <h4>Writing Data to the Batch</h4>
 * Each batch is delimited by a call to {@link org.apache.drill.exec.physical.resultSet.ResultSetLoader#startBatch()}
 * and a call to {@link org.apache.drill.exec.physical.resultSet.impl.VectorState#harvestWithLookAhead()}
 * to obtain the completed batch. Note that readers do not
 * call these methods; the scan operator does this work.
 * <p>
 * Each row is delimited by a call to {@code startValue()} and a call to
 * {@code saveRow()}. <tt>startRow()</tt> performs initialization necessary
 * for some vectors such as repeated vectors. <tt>saveRow()</tt> moves the
 * row pointer ahead.
 * <p>
 * A reader can easily reject a row by calling <tt>startRow()</tt>, begin
 * to load a row, but omitting the call to <tt>saveRow()</tt> In this case,
 * the next call to <tt>startRow()</tt> repositions the row pointer to the
 * same row, and new data will overwrite the previous data, effectively erasing
 * the unwanted row. This also works for the last row; omitting the call to
 * <tt>saveRow()</tt> causes the batch to hold only the rows actually
 * saved.
 * <p>
 * Readers then write to each column. Columns are accessible via index
 * ({@link org.apache.drill.exec.physical.resultSet.RowSetLoader#column(int)} or by name
 * ({@link org.apache.drill.exec.physical.resultSet.RowSetLoader#column(String)}.
 * Indexed access is much faster.
 * Column indexes are defined by the order that columns are added. The first
 * column is column 0, the second is column 1 and so on.
 * <p>
 * Each call to the above methods returns the same column writer, allowing the
 * reader to cache column writers for additional performance.
 * <p>
 * All column writers are of the same class; there is no need to cast to a
 * type corresponding to the vector. Instead, they provide a variety of
 * <tt>set<i>Type</i></tt> methods, where the type is one of various Java
 * primitive or structured types. Most vectors provide just one method, but
 * others (such as VarChar) provide two. The implementation will throw an
 * exception if the vector does not support a particular type.
 * <p>
 * Note that this class uses the term "loader" for row and column writers
 * since the term "writer" is already used by the legacy record set mutator
 * and column writers.
 * <h4>Handling Batch Limits</h4>
 * The mutator enforces two sets of batch limits:<ol>
 * <li>The number of rows per batch. The limit defaults to 64K (the Drill
 * maximum), but can be set lower by the client.</li>
 * <li>The size of the largest vector, which is capped at 16 MB. (A future
 * version may allow adjustable caps, or cap the memory of the entire
 * batch.</li></ol>
 * Both limits are presented to the client via the
 * {@link org.apache.drill.exec.physical.resultSet.RowSetLoader#isFull()} method.
 * After each call to {@code saveRow()},
 * the client should call <tt>isFull()</tt> to determine if the client can add another row. Note
 * that failing to do this check will cause the next call to
 * {@link org.apache.drill.exec.physical.resultSet.ResultSetLoader#startBatch()} to throw an exception.
 * <p>
 * The limits have subtle differences, however. Row limits are simple: at
 * the end of the last row, the mutator notices that no more rows are possible,
 * and so does not allow starting a new row.
 * <p>
 * Vector overflow is more complex. A row may consist of columns (a, b, c).
 * The client may write column a, but then column b might trigger a vector
 * overflow. (For example, b is a Varchar, and the value for b is larger than
 * the space left in the vector.) The client cannot stop and rewrite a. Instead,
 * the client simply continues writing the row. The mutator, internally, moves
 * this "overflow" row to a new batch. The overflow row becomes the first row
 * of the next batch rather than the first row of the current batch.
 * <p>
 * For this reason, the client can treat the two overflow cases identically,
 * as described above.
 * <p>
 * There are some subtle differences between the two cases that clients may
 * occasionally may need to expect:<ul>
 * <li>When a vector overflow occurs, the returned batch will have one
 * fewer rows than the client might expect if it is simply counting the rows
 * written.</li>
 * <li>A new column added to the batch after overflow occurs will appear in
 * the <i>next</i> batch, triggering a schema change between the current and
 * next batches.</li></ul>
 */
package org.apache.drill.exec.physical.resultSet;
