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
 * <p>
 * Resolves a scan schema throughout the scan lifecycle. Schema resolution
 * comes from a variety of sources. Resolution starts with preparing the
 * schema for the first reader:
 * <ul>
 * <li>Project list (wildcard, empty, or explicit)</li>
 * <li>Optional provided schema (strict or lenient)</li>
 * <li>Implicit columns</li>
 * <li>An "early" reader schema (one determined before reading any
 * data.</li>
 * </ul>
 * The result is a <i>defined schema</i> which may include;
 * <ul>
 * <li>Dynamic columns: those from the project list where we know only
 * the column name, but not its type.</li>
 * <li>Resolved columns: implicit or provided columns where we know
 * the name and type.</li>
 * </ul>
 * The schema itself can be one of two forms:
 * <ul>
 * <li>Open: meaning that the reader can add other columns. An open
 * schema results from a wildcard projection. Since the wildcard can appear
 * along with implicit columns, the schema can be open and have a set of
 * columns. If a provided schema appears, then the provided schema is
 * expanded here. If the schema is "lenient", then the reader can add
 * additional columns as it discovers them.</li>
 * <li>Closed: meaning that the reader cannot add additional columns.
 * A closed schema results from an empty or explicit projection list. A closed
 * schema also results from a wildcard projection and a strict schema.</li>
 * </ul>
 * <p>
 * Internally, the schema may start as open (has a wildcard), but may transition
 * to closed when processing a strict provided schema.
 * <p>
 * Once this class is complete, the scan can add columns only to an open schema.
 * All such columns are inserted at the wildcard location. If the wildcard appears
 * by itself, columns are appended. If the wildcard appears along with implicit columns,
 * then the reader columns appear at the wildcard location, before the implicit columns.
 * <p>
 * Once we have the initial reader input schema, we can then further refine
 * the schema with:
 * <ul>
 * <li>The reader "output" schema: the columns actually read by the
 * reader.</li>
 * <li>The set of "missing" columns: those projected, but which the reader did
 * not provide. We must make up a type for missing columns (and hope we guess
 * correctly.) In fact, the purpose of the provided (and possibly early reader)
 * schema is to avoid the need to guess.</li>
 * </ul>
 *
 * <h4>Implicit (Wildcard) Projection</h4>
 *
 * A query can contain a wildcard ({@code *}). In this case, the set of columns is
 * driven by the reader. Each scan might drive one, two or many readers. In an ideal
 * world, every reader would produce the same schema. In the real world, files tend
 * the evolve: early files have three columns, later files have five. In this case
 * some readers will produce one schema, other readers another. Much of the complexity
 * of Drill comes from this simple fact that Drill is a SQL engine that requires a
 * single schema for all rows, but Drill reads data sources which are free to return
 * any schema that they want.
 * <p>
 * A wildcard projection starts by accepting the schema produced by the first reader.
 * In "classic" mode, later readers can add columns (causing a schema change to be
 * sent downstream), but cannot change the types of existing columns. The code
 * here supports a "no schema change" mode in which the first reader discovers the
 * schema, which is then fixed for all subsequent readers. This mode cannot, however
 * prevent schema conflicts across scans running in different fragments.
 *
 * <h4>Explicit Projection</h4>
 *
 * Explicit projection provides the list of columns, but not their types.
 * Example: SELECT a, b, c.
 * <p>
 * The projection list holds the columns
 * as requested by the user in the {@code SELECT} clause of the query,
 * in the order which columns appear in that clause, along with additional
 * columns implied by other columns. The planner
 * determines which columns to project. In Drill, projection is speculative:
 * it is a list of names which the planner hopes will appear in the data
 * files. The reader must make up columns (the infamous nullable INT) when
 * it turns out that no such column exists. Else, the reader must figure out
 * the data type for any columns that does exist.
 * <p>
 * An explicit projection starts with the requested set of columns,
 * then looks in the table schema to find matches. Columns not in the project list
 * are not projected (not written to vectors). The reader columns provide the types
 * of the projected columns, "resolving" them to a concrete type.
 * <p>
 * An explicit projection may include columns that do not exist in
 * the source schema. In this case, we fill in null columns for
 * unmatched projections.
 * <p>
 * The challenge in this case is that Drill cannot know the type of missing columns;
 * Drill can only guess. If a reader in Scan 1 guesses a type, but a reader in
 * Scan 2 reads a column with a different type, then a schema conflict will
 * occur downstream.
 *
 * <h4>Maps</h4>
 *
 * Maps introduce a large amount of additional complexity. First, maps appear
 * in the project list as either:
 * <ul>
 * <li>A generic projection: just the name {@code m}, where {@code m} is a map.
 * In this case, we project all members of the map. That is, the map itself
 * is open in the above sense. Note that a map can be open even if the scan
 * schema itself is closed. That is, if the projection list contains only
 * {@code m}, the scan schema is closed, but the map is open (the reader will
 * discover the fields that make up the map.)</li>
 * <li>A specific projection: a list of map members: {@code m.x, m.y}. In this
 * case, we know that the downstream Project operator will pull just those two
 * members to the top level and discard the rest of the map. We can thus
 * project just those two members in the scan. As a result, the map is closed
 * in the above sense: any additional map members discovered by the reader will
 * be unprojected.</li>
 * <li>Hybrid: a projection list that includes both: {@code m, m.x}. Here, the
 * generic projection takes precedence. If the specific projection includes
 * qualifiers, {@code m, m.x[1]}, then that information is used to check the
 * type of column {@code x}.</li>
 * <li>Implied: in a wildcard projection, a column may turn out to be a map.
 * In this case, the map is open when the schema itself is open. (Remember that
 * a wildcard projection can result in a closed schema if paired with a strict
 * provided schema.</li>
 * </ul>
 *
 * <h4>Schema Definition</h4>
 *
 * This resolver is the first step in the scan schema process. The result is a
 * (typically dynamic) <i>defined schema</i>. To understand this concept, it helps
 * to compare Drill with other query engines. In most engines, the planner is
 * responsible for working out the scan schema from table metadata, from the
 * project list and so on. The scan is given a fully-defined schema which it
 * must use.
 * <p>
 * Drill is unique in that it uses a <i>dynamic schema</i> with columns and/or types
 * "to be named later." The scan must convert the dynamic schema into a concrete
 * schema sent downstream. This class implements some of the steps in doing so.
 * <p>
 * The result of this class is a schema identical to a defined schema that a
 * planner might produce. Since Drill is dynamic, the planner must be able to
 * produce a dynamic schema of the form described above. If the planner has table
 * metadata (here represented by a provided schema), then the planner could produce
 * a concrete defined schema (all types are defined.) Or, with a lenient provided
 * schema, the planner might produce a dynamic defined schema: one with some
 * concrete columns, some dynamic (name-only) columns.
 *
 * <h4>Implicit Columns</h4>
 *
 * This class handles one additional source of schema information: implicit
 * columns: those defined by Drill itself. Examples include {@code filename,
 * dir0}, etc. Implicit columns are available (at present) only for the file
 * storage plugin, but could be added for other storage plugins. The project list
 * can contain the names of implicit columns. If the query contains a wildcard,
 * then the project list may also contain implicit columns:
 * {@code filename, *, dir0}.
 * <p>
 * Implicit columns are known to Drill, so Drill itself can provide type information
 * for those columns, by an external implicit column parser. That parser locates
 * implicit columns by name, marks the columns as implicit, and takes care of
 * populating the columns at read time. We use a column property,
 * {@code IMPLICIT_COL_TYPE}, to mark a column as implicit. Later the scan mechanism
 * will omit such columns when preparing the <i>reader schema</i>.
 * <p>
 * If the planner were to provide a defined schema, then the planner would have
 * parsed out the implicit columns, provided their types, and marked them as
 * implicit. So, again, we see that this class produces, at scan time, the same
 * defined schema that the planner might produce at plan time.
 * <p>
 * Because of the way we handle implicit columns, we can allow the provided
 * schema to include them. The provided schema simply adds a column (with any
 * name), and sets the {@code IMPLICIT_COL_TYPE} property to indicate which
 * implicit column definition to use for that column. This is handy for allowing the
 * implicit column to include partition directories as regular columns.
 * <p>
 * We now have a parsing flow for this package:
 * <ul>
 * <li>Projection list (so we know what to include)</li>
 * <li>Provided schema (to add/mark columns as implicit)</li>
 * <li>Implicit columns, which looks for only for a) columns tagged as
 * implicit or b) dynamic columns (those not defined in the provided
 * schema.</li>
 * </ul>
 * <p>
 * Drill has long had a source of ambiguity: what happens if the reader has a column
 * with the same name as an implicit column. In this flow, the ambiguity is resolved
 * as follows:
 * <ul>
 * <li>If a provided schema has a column explicitly tagged as an implicit column,
 * then that column is unambiguously an implicit column independent of name.</li>
 * <li>If a provided schema has a column with the same name as an implicit column
 * (the names can be changed by a system/session option), then the fact that the
 * column is not marked as implicit unambiguously tells us that the column is not
 * implicit, despite the name.</li>
 * <li>If a column appears in the project list, but not in the provided schema,
 * and that column matches the (effective) name of some implicit column, then
 * the column is marked as implicit and is not passed to the reader. Further, the
 * projection filter will mark that column as unprojected in the reader, even if
 * the reader otherwise has a wildcard schema.</li>
 * </ul>
 *
 * <h4>Projection</h4>
 *
 * In prior versions of the scan operator, projection tended to be quite simple:
 * just check if a name appears in the project list. As we've seen from the above,
 * projection is actually quite complex with the need to reuse type information
 * where available, open and closed top-level and map schemas, the need to avoid
 * projecting columns with the same name as implicit columns, etc.
 * <p>
 * The {@code ProjectionFilter} classes handle projection. As it turns out, this
 * class must follow (variations of) the same rules when merging the provided
 * schema with the projection list and so on. To ensure a single implementation
 * of the complex projection rules, this class uses a projection filter when
 * resolving the provided schema. The devil is in the details, knowing when
 * a map is open or closed, enforcing consistency with known information, etc.
 *
 * <h4>Provided Schema</h4>
 *
 * With the advent of provided schema in Drill 1.16, the query plan can provide
 * not just column names (dynamic columns) but also the data type (concrete
 * columns.) In this case, the scan schema can resolve projected columns against
 * the provided schema, rather than waiting for the reader schema. Readers can use
 * the provided schema to choose a column type when the choice is ambiguous, or multiple
 * choices are possible.
 * <p>
 * If the projection list is a wildcard, then the wildcard expands to include all
 * columns from the provided schema, in the order of that schema. If the schema
 * is strict, then the scan schema becomes fixed, as if an explicit projection list
 * where used.
 * <p>
 * If the projection list is explicit, then each column is resolved against
 * the provided schema. If the projection list includes a column not in the
 * provided schema, then it falls to the reader (or missing columns mechanism)
 * to resolve that particular column.
 *
 * <h4>Early Reader Schema</h4>
 *
 * Some readers can declare their schema before reading data. For example, a JDBC
 * query gets back a row schema during the initial prepare step. In this case, the
 * reader is said to be <i>early schema</i>. The reader indicates an early schema
 * via its <i>schema negotiator</i>. The framework then uses this schema to resolve
 * the dynamic columns in the scan schema. If all columns are resolved this way,
 * then the scan can declare its own schema before reading any data.
 * <p>
 * An early reader schema can work with a provided schema. In this case, the early
 * reader schema must declare the same column type as the provided schema.
 * This is not a large obstacle: the provided schema should have originally come
 * from the reader (or a description of the reader) so conflicts should not
 * occur in normal operation.
 *
 * <h4>Reader Output Schema</h4>
 *
 * Once a reader loads a batch of data, it provides (via the
 * {@code ResultSetLoader}) the reader's <i>output schema</i>: the set of columns
 * actually read by the reader.
 * <p>
 * If the projection list contained a wildcard, then the reader output schema
 * will determine the set of columns that replaces the wildcard. (That is, all reader
 * columns are projected and the scan schema expands to reflect the actual columns.)
 * <p>
 * If the projection list is explicit (or made so by a strict provided schema),
 * then the reader output schema must be a subset of the scan schema: it is an error
 * for the reader to include extra columns as the scan mechanism won't know what to
 * do with those vectors. The projection mechanism (see below) integrates with the
 * {@code ResultSetLoader} to project only those columns needed; the others are
 * given to the reader as "dummy" column writers: writers that accept, but discard
 * their data.
 * <p>
 * Note the major difference between the early reader schema and the reader output
 * schema. The early reader schema includes all the columns that the reader can read.
 * The reader output schema includes only those columns that the reader actually read
 * (as controlled by the projection filter.) For most readers (CSV, JSON, etc.), there
 * is no early reader schema, there is only the reader output schema: the set of columns
 * (modulo projection) that turned out to be in the data source.
 *
 * <h4>Projection</h4
 *
 * The projection list tells the reader which columns to read. In this mechanism,
 * the projection list undergoes multiple transforms (expanding into a provided
 * schema, identifying implicit columns, etc.) Further, as columns are resolved
 * (via a provided schema, an earlier reader, etc.), the projection list can provide
 * type information as well.
 * <p>
 * To handle this, projection is driven by the (evolving) scan schema. In fact, the
 * schema mechanism uses the same projection implementation when applying the
 * provided schema and early reader schema.
 *
 * <h4>Assembling the Output Schema and Batch</h4>
 *
 * The <i>scan output schema</i> consists of up to three parts:
 * <ul>
 * <li>Reader columns (the reader output schema)</li>
 * <li>Missing columns (reader input columns which the reader does not
 * actually provide.)</li>
 * <li>Implicit columns.</li>
 * </ul>
 * Distinct mechanisms build each kind of schema. The reader builds the vectors
 * for the reader schema. A missing column handler builds the missing columns
 * (using provided or inferred types and values.) An implicit column manager
 * fills in the implicit columns based on file information.
 * <p>
 * The scan schema tracker tracks all three schemas together to form the
 * scan output schema. Tracking the combined schema ensures we preserve the
 * user's requested project ordering. The reader manager builds the vectors
 * using the above mechanisms, then merges the vectors (very easy to do in a
 * columnar system) to produce the output batch which matches the scan schema.
 *
 * <h4>Architecture Overview</h4>
 *
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
 *                +-------------+
 *                | Scan Schema |     +-------------------+
 *                |   Tracker   | --->| Projection Filter |
 *                +-------------+     +-------------------+
 *                       |                  |
 *                       v                  v
 *  +------+      +------------+     +------------+      +-----------+
 *  | File | ---> |   Reader   |---->| Result Set | ---> | Data File |
 *  | Data |      |            |     |   Loader   | <--- |  Reader   |
 *  +------+      +------------+     +------------+      +-----------+
 *                       |                  |
 *                       v                  |
 *                +------------+    Reader  |
 *                |   Reader   |    Schema  |
 *                | Lifecycle  | <----------+
 *                +------------+            |
 *                       |                  |
 *                       v                  |
 *                  +---------+    Loaded   |
 *                  | Output  |    Vectors  |
 *                  | Builder | <-----------+
 *                  +---------+
 *                       |
 *                       v
 *                 Output Batch
 * </pre>
 *
 * Omitted are the details of implicit and missing columns. The scan lifecycle
 * (not shown) orchestrates the whole process.
 * <p>
 * The result is a scan schema which can start entirely dynamic (just a wildcard
 * or list of column names), which is then resolved via a series of steps (some
 * of which involve the real work of the scanner: reading data.) The bottom is
 * the output: a full-resolved scan schema which exactly describes an output
 * data batch.
 */
package org.apache.drill.exec.physical.impl.scan.v3.schema;
