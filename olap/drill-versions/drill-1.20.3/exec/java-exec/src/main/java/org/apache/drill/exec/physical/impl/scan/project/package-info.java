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
 * Provides run-time semantic analysis of the projection list for the
 * scan operator. The project list can include table columns and a
 * variety of special columns. Requested columns can exist in the table,
 * or may be "missing" with null values applied. The code here prepares
 * a run-time projection plan based on the actual table schema.
 *
 * <h4>Overview</h4>
 *
 * The projection framework look at schema as a set of transforms:
 * <p>
 * <ul>
 * <li>Scan level: physical plan projection list and optional provided
 * schema information.</li>
 * <li>File level: materializes implicit file and parition columns.</li>
 * <li>Reader level: integrates the actual schema discovered by the
 * reader with the scan-level projection list.</li>
 * </ul>
 * <p>
 * Projection turns out to be a very complex operation in a schema-on-read
 * system such as Drill. Provided schema helps resolve ambiguities inherent
 * in schema-on-read, but at the cost of some additional complexity.
 *
 * <h4>Background</h4>
 *
 * The Scan-level projection holds the list of columns
 * (or the wildcard) as requested by the user in the query. The planner
 * determines which columns to project. In Drill, projection is speculative:
 * it is a list of names which the planner hopes will appear in the data
 * files. The reader must make up columns (the infamous nullable INT) when
 * it turns out that no such column exists. Else, the reader must figure out
 * the data type for any columns that does exist.
 * <p>
 * With the advent of provided schema in Drill 1.16, the scan level projection
 * integrates that schema information with the projection list provided in
 * the physical operator. If a schema is provided, then each scan-level
 * column tracks the schema information for that column.
 * <p>
 * The scan-level projection also
 * implements the special rules for a "strict" provided schema: if the operator
 * projection list contains a wildcard, a schema is provided, and the schema
 * is strict, then the scan level projection expands the wildcard into the
 * set of columns in the provided schema. Doing so ensures that the scan
 * output contains exactly those columns from the schema, even if the columns
 * must be null or at a default value. (The result set loader does additional
 * filtering as well.)
 * <p>
 * The scan project list defines the set of columns which the scan operator
 * is obliged to send downstream. Ideally, the scan operator sends exactly the
 * same schema (the project list with types filled in) for all batches. Since
 * batches may come from different files, the scan operator is obligated to
 * unify the schemas from those files (or blocks.)
 * <p>
 * Reader (file)-level projection occurs for each reader. A single scan
 * may use multiple readers to read data. From the reader's perspective, it
 * offers the schema it discovers in the file. The reader itself is rather
 * inflexible: it must deal with the data it finds, of the type found in
 * the data source.
 * <p>
 * The reader thus tells the result set loader that it has such-and-so schema.
 * It does that either at open time (so-called "early" schema, such as for
 * CSV, JDBC or Parquet) or as it discovers the columns (so-called "late"
 * schema as in JSON.) Again, in each case, the data source schema is what
 * it is; it can't be changed due to the wishes of the scan-level projection.
 * <p>
 * Readers obtain column schema from the file or data source. For example,
 * a Parquet reader can obtain schema information
 * from the Parquet headers. A JDBC reader obtains schema information from the
 * returned schema. As noted above, we use the term "early schema" when type
 * information is available at open time, before reading the first row of data.
 * <p>
 * By contrast eaders such as JSON and CSV are "late schema": they don't know the data
 * schema until they read the file. This is true "schema on read." Further, for
 * JSON, the data may change from one batch to the next as the reader "discovers"
 * fields that did not appear in earlier batches. This requires some amount of
 * "schema smoothing": the ability to preserve a consistent output schema even
 * as the input schema jiggles around some.
 * <p>
 * Drill supports many kinds of data sources via plugins. The DFS plugin works
 * with files in a distributed store such as HDFS. Such file-based readers
 * add implicit file or partition columns. Since these columns are generic to
 * all format plugins, they are factored out into a file scan framework which
 * inserts the "implicit" columns separate from the reader-provided columns.
 *
 * <h4>Design</h4>
 *
 * This leads to a multi-stage merge operation. The result set loader is
 * presented with each column one-by-one (either at open time or during read.)
 * When a column is presented, the projection framework makes a number of
 * decisions:
 * <p>
 * <ul>
 * <li>Is the column projected? For example, if a query is <tt>SELECT a, b, c</tt>
 * and the reader offers column <tt>d</tt>, then column d will not be projected.
 * In the wildcard case, "special" columns will be omitted from the column
 * expansion and will be unprojected.</li>
 * <li>Is type conversion needed? If a schema is provided, and the type of the
 * column requested in the provided schema differs from that offered by the
 * reader, the framework can insert a type-conversion "shim", assuming that
 * the framework knows how to do the conversion. Else, and error is raised.</li>
 * <li>Is the column type and mode consistent with the projection list?
 * Suppose the query is <tt>SELECT a, b[10], c.d</tt>. Column `a` matches
 * any reader column. But, column `b` is valid only for an array (not a map
 * and not a scalar.) Column `c` must be a map (or array of maps.) And so on.</li>
 * </ul>
 * <p>
 * The result is a refined schema: the scan level schema with more information
 * filled in. For Parquet, all projection information can be filled in. For
 * CSV or JSON, we can only add file metadata information, but not yet the
 * actual data schema.
 * <p>
 * Batch-level schema: once a reader reads actual data, it now knows
 * exactly what it read. This is the "schema on read model." Thus, after reading
 * a batch, any remaining uncertainty about the projected schema is removed.
 * The actual data defined data types and so on.
 * <p>
 * The goal of this mechanism is to handle the above use cases cleanly, in a
 * common set of classes, and to avoid the need for each reader to figure out
 * all these issues for themselves (as was the case with earlier versions of
 * Drill.)
 * <p>
 * Because these issues are complex, the code itself is complex. To make the
 * code easier to manage, each bit of functionality is encapsulated in a
 * distinct class. Classes combine via composition to create a "framework"
 * suitable for each kind of reader: whether it be early or late schema,
 * file-based or something else, etc.
 *
 * <h4>Nuances of Reader-Level Projection</h4>
 *
 * We've said that the scan-level projection identifies what the query
 * <i>wants</i>. We've said that the reader identifies what the external
 * data actually <i>is</i>. We've mentioned how we bridge between the
 * two. Here we explore this in more detail.
 * <p>
 * Run-time schema resolution occurs at various stages:
 * <p>
 * <ul>
 * <li>The per-column resolution identified earlier: matching types,
 * type conversion, and so on.</li>
 * <li>The reader provides some set of columns. We don't know which
 * columns until the end of the first (or more generally, every) batch.
 * Suppose the query wants <tt>SELECT a, b, c</tt> but the reader turns
 * out to provide only `a` and `b`. On after the first batch do we
 * realize that we need column `c` as a "null" column (of a type defined
 * in the provided schema, specified by the plugin, or good-old nullable
 * INT.)</li>
 * <li>The result set loader will have created "dummy" columns for
 * unprojected columns. The reader can still write to such columns
 * (because they represent data in the file), but the associated column
 * writer simply ignores the data. As a result, the result set loader
 * should produce only a (possibly full) subset of projected columns.</li>
 * <li>After each reader batch, the projection framework goes to work
 * filling in implicit columns, and filling in missing columns. It is
 * important to remember that this pass *must* be done *after* a batch
 * is read since we don't now the columns that the reader can provided
 * until after a batch is read.</li>
 * <li>Some readers, such as JSON, can "change its mind" about the
 * schema across batches. For example, the first batch may include
 * only columns a and b. Later in the JSON file, the reader may
 * discover column c. This means that the above post-batch analysis
 * must be repeated each time the reader changes the schema. (The result
 * set loader tracks schema changes for this purpose.)</li>
 * <li>File schemas evolve. The same changes noted above can occur
 * cross files. Maybe file 1 has column `x` as a BIGINT, while file 2
 * has column 'x' as INT. A "smoothing" step attempts to avoid hard
 * schema changes if they can be avoided. While smoothing is a clever
 * idea, it only handles some cases. Provided schema is a more reliable
 * solution (but is not yet widely deployed.)</li>
 * </ul>
 *
 * <h4>Reader-Level Projection Set</h4>
 *
 * The Projection Set mechanism is designed to handle the increasing nuances
 * of Drill run-time projection by providing a source of information about
 * each column that the reader may discover:
 * <ul>
 * <li>Is the column projected?</li><ul>
 *   <li>If the query is explicit (<tt>SELECT a, b, c</tt>), is the column
 *   in the projection list?</li>
 *   <li>If the query is a wildcard (<tt>SELECT *</tt>), is the column
 *   marked as special (not included in the wildcard)?</li>
 *   <li>If the query is wildcard, and a strict schema is provided, is
 *   the column part of the provided schema?</li></ul></li>
 * <li>Verify column is consistent with projection.</li>
 * <li>Type conversion, if needed.</li>
 * </ul>
 *
 * <h4>Projection Via Rewrites</h4>
 *
 * The core concept is one of successive refinement of the project
 * list through a set of rewrites:
 * <p>
 * <ul>
 * <li>Scan-level rewrite: convert {@link SchemaPath} entries into
 * internal column nodes, tagging the nodes with the column type:
 * wildcard, unresolved table column, or special columns (such as
 * file metadata.) The scan-level rewrite is done once per scan
 * operator.</li>
 * <li>Reader-level rewrite: convert the internal column nodes into
 * other internal nodes, leaving table column nodes unresolved. The
 * typical use is to fill in metadata columns with information about a
 * specific file.</li>
 * <li>Schema-level rewrite: given the actual schema of a record batch,
 * rewrite the reader-level projection to describe the final projection
 * from incoming data to output container. This step fills in missing
 * columns, expands wildcards, etc.</li>
 * </ul>
 * The following outlines the steps from scan plan to per-file data
 * loading to producing the output batch. The center path is the
 * projection metadata which turns into an actual output batch.
 * <pre>
 *                   Scan Plan
 *                       |
 *                       v
 *               +--------------+
 *               | Project List |
 *               |    Parser    |
 *               +--------------+
 *                       |
 *                       v
 *                +------------+
 *                | Scan Level |     +----------------+
 *                | Projection | --->| Projection Set |
 *                +------------+     +----------------+
 *                       |                  |
 *                       v                  v
 *  +------+      +------------+     +------------+      +-----------+
 *  | File | ---> | File Level |     | Result Set | ---> | Data File |
 *  | Data |      | Projection |     |   Loader   | <--- |  Reader   |
 *  +------+      +------------+     +------------+      +-----------+
 *                       |                  |
 *                       v                  |
 *               +--------------+   Reader  |
 *               | Reader Level |   Schema  |
 *               |  Projection  | <---------+
 *               +--------------+           |
 *                       |                  |
 *                       v                  |
 *                  +--------+   Loaded     |
 *                  | Output |   Vectors    |
 *                  | Mapper | <------------+
 *                  +--------+
 *                       |
 *                       v
 *                 Output Batch
 * </pre>
 * <p>
 * The left side can be thought of as the "what we want" description of the
 * schema, with the right side being "what the reader actually discovered."
 * <p>
 * The output mapper includes mechanisms to populate implicit columns, create
 * null columns, and to merge implicit, null and data columns, omitting
 * unprojected data columns.
 * <p>
 * In all cases, projection must handle maps, which are a recursive structure
 * much like a row. That is, Drill consists of nested tuples (the row and maps),
 * each of which contains columns which can be maps. Thus, there is a set of
 * alternating layers of tuples, columns, tuples, and so on until we get to leaf
 * (non-map) columns. As a result, most of the above structures are in the form
 * of tuple trees, requiring recursive algorithms to apply rules down through the
 * nested layers of tuples.
 * <p>
 * The above mechanism is done at runtime, in each scan fragment. Since Drill is
 * schema-on-read, and has no plan-time schema concept, run-time projection is
 * required. On the other hand, if Drill were ever to support the "classic"
 * plan-time schema resolution, then much of this work could be done at plan
 * time rather than (redundantly) at runtime. The main change would be to do
 * the work abstractly, working with column and row descriptions, rather than
 * concretely with vectors as is done here. Then, that abstract description
 * would feed directly into these mechanisms with the "final answer" about
 * projection, batch layout, and so on. The parts of this mechanism that
 * create and populate vectors would remain.
 */

package org.apache.drill.exec.physical.impl.scan.project;
