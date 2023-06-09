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
 * Defines the projection, vector continuity and other operations for
 * a set of one or more readers. Separates the core reader protocol from
 * the logic of working with batches.
 *
 * <h4>Schema Evolution</h4>
 *
 * Drill discovers schema on the fly. The scan operator hosts multiple readers.
 * In general, each reader may have a distinct schema, though the user typically
 * arranges data in a way that scanned files have a common schema (else SQL
 * is the wrong tool for analysis.) Still, subtle changes can occur: file A
 * is an old version without a new column c, while file B includes the column.
 * And so on.
 * <p>
 * The scan operator works to ensure schema continuity as much as
 * possible, smoothing out "soft" schema changes that are simply artifacts of
 * reading a collection of files. Only "hard" changes (true changes) are
 * passed downstream.
 * <p>
 * First, let us define three types of schema change:
 * <ul>
 * <li>Trivial: a change in vectors underlying a column. Column a may
 * be a required integer, but if the actual value vector changes, some
 * operators treat this as a schema change.</li>
 * <li>Soft: a change in column order, but not the type or existence of
 * columns. Soft changes cause problems for operators that index columns by
 * position, since column positions change.</li>
 * <li>Hard: addition or deletion of a column, or change of column
 * data type.</li>
 * </ul>
 * The goal of this package is to smooth out trivial and soft schema changes,
 * and avoid hard schema changes where possible. To do so, we must understand
 * the sources of schema change.
 * <ul>
 * <li>Trivial schema changes due to multiple readers. In general, each reader
 * should be independent of other readers, since each file is independent of
 * other files. Since readers are independent, they should have control over
 * the schema (and vectors) used to read the file. However, distinct vectors
 * trigger a trivial schema change.</li>
 * <li>Schema changes due to readers that discover data schema as the read
 * progresses. The start of a file might have two columns, a and b. A second
 * batch might discover column c. Then, when reading a second file, the process
 * might repeat. In general, if we have already seen column c in the first file,
 * there is no need to trigger a schema change upon discovering it in the
 * second. (Though there are subtle issues, such as handling required types.)
 * </li>
 * <li>Schema changes due to columns that appear in one file, but not another.
 * This is a variation of the above issue, but occurs across files, as
 * explained above.</li>
 * <li>Schema changes due to guessing wrong about the type of a missing
 * column. A query might request columns a, b and c. The first file has columns
 * a and b, forcing the scan operator to "make up" a column c. Typically Drill
 * uses a nullable int. But, a later reader might find a column c and realize
 * that it is actually a Varchar.</li>
 * <li>Actual schema changes in the data: a file might contain a run of numbers,
 * only to insert a string later. A file A might have columns a and b, while a
 * second file adds column c. (In general, columns are not removed, only added
 * or have the type changed.)</li>
 * </ul>
 * To avoid soft schema changes, this scan
 * operator uses a variety of schema smoothing "levels":
 * <ul>
 * <li>Level 0: anything that the reader might do, such as a JDBC data source
 * requesting only the required columns.</li>
 * <li>Level 1: for queries with an explicit select (SELECT a, b, c...), the
 * result set loader will filter out unwanted columns. So, if file A also
 * includes column d, and file B adds d and f, the result set loader will
 * project out the unneeded columns, avoiding schema change (and unnecessary
 * vector writes.</li>
 * <li>Level 2: for multiple readers, or readers with evolving schema, a
 * buffering layer fills in missing columns using the type already seen,
 * if possible.</li>
 * <li>Level 3: soft changes are avoided by projecting table columns (and
 * metadata columns) into the order requested by an explicit select.</li>
 * <li>Level 4: the scan operator itself monitors the resulting schema,
 * watching for changes that cancel out. For example, each reader builds
 * its own schema. If the two files have an identical schema, then the resulting
 * schemas are identical and no schema change need be advertised downstream.
 * </li>
 * </ul>
 * Keep the above in mind when looking at individual classes. Mapping of
 * levels to classes:
 * <ul>
 * <li>Level 1: {@link LogicalTupleSet} in the {@link ResultSetLoader}
 * class.</li>
 * <li>Level 2: {@link ProjectionPlanner}, {@link ScanProjection} and
 * {@link ScanProjector}.</li>
 * <li>Level 3: {@link ScanProjector}.</li>
 * <li>Level 4: {@link OperatorRecordBatch.SchemaTracker}.</li>
 * </ul>
 * <h4>Selection List Processing</h4>
 * A key challenge in the scan operator is mapping of table schemas to the
 * schema returned by the scan operator. We recognize three distinct kinds of
 * selection:
 * <ul>
 * <li>Wildcard: <tt>SELECT *</tt></li>
 * <li>Generic: <tt>SELECT columns</tt>, where "columns" is an array of column
 * values. Supported only by readers that return text columns.</li>
 * <li>Explicit: <tt>SELECT a, b, c, ...</tt> where "a", "b", "c" and so on are
 * the <i>expected</i> names of table columns</li>
 * </ul>
 * Since Drill uses schema-on-read, we are not sure of the table schema until
 * we actually ask the reader to open the table (file or other data source.)
 * Then, a table can be "early schema" (known at open time) or "late schema"
 * (known only after reading data.)
 * <p>
 * A selection list goes through three distinct phases to result in a final
 * schema of the batch returned downstream.
 * <ol>
 * <li>Query selection planning: resolves column names to metadata (AKA implicit
 * or partition) columns, to "*", to the special "columns" column, or to a list
 * of expected table columns.</li>
 * <li>File selection planning: determines the values of metadata columns based
 * on file and/or directory names.</li>
 * <li>Table selection planning: determines the fully resolved output schema by
 * resolving the wildcard or selection list against the actual table columns.
 * A late-schema table may do table selection planning multiple times: once
 * each time the schema changes.</li>
 * </ul>
 * <h4>Output Batch Construction</h4>
 * The batches produced by the scan have up to four distinct kinds of columns:
 * <ul>
 * <li>File metadata (filename, fqn, etc.)</li>
 * <li>Directory (partition metadata: (dir0, dir1, etc.)</li>
 * <li>Table columns (or the special "columns" column)</li>
 * <li>Null columns (expected table columns that turned out to not match any
 * actual table column. To avoid errors, Drill returns these as columns filled
 * with nulls.</li>
 * </ul>
 * In practice, the scan operator uses three distinct result set loaders to create
 * these columns:
 * <ul>
 * <li>Table loader: for the table itself. This loader uses a selection layer to
 * write only the data needed for the output batch, skipping unused columns.</li>
 * <li>Metadata loader: to create the file and directory metadata columns.</li>
 * <li>Null loader: to populate the null columns.</li>
 * </ul>
 * The scan operator merges the three sets of columns to produce the output
 * batch. The merge step also performs projection: it places columns into the
 * output batch in the order specified by the original SELECT list (or table order,
 * if the original SELECT had a wildcard.) Fortunately, this is just involves
 * moving around pointers to vectors; no actual data is moved during projection.
 *
 * <h4>Class Structure</h4>
 *
 * Some of the key classes here include:
 * <ul>
 * <li>{@link RowBatchReader} an extremely simple interface for reading data.
 * We would like many developers to create new plugins and readers. The simplified
 * interface pushes all complexity into the scan framework, leaving the reader to
 * just read.</li>
 * <li>{@link ShimBatchReader} an implementation of the above that converts from
 * the simplified API to add additional structure to work with the result set loader.
 * (The base interface is agnostic about how rows are read.)</li>
 * <li>{@link ScheamNegotiator} and interface that allows a batch reader to
 * "negotiate" a schema with the scan framework. The scan framework knows the
 * columns that are to be projected. The reader knows what columns it can offer.
 * The schema negotiator works out how to combine the two. It expresses the result
 * as a result set loader. Column writers are defined for all columns that the
 * reader wants to read, but only the materialized (projected) columns have actual
 * vectors behind them. The non-projected columns are "free-wheeling" "dummy"
 * writers.
 * </li>
 *
 * And, yes, sorry for the terminology. File "readers" read from files, but
 * use column "writers" to write to value vectors.
 */
package org.apache.drill.exec.physical.impl.scan.framework;
