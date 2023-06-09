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
package org.apache.drill.exec.physical.impl.scan.v3.schema;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnMarker;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Computes <i>scan output schema</i> from a variety of sources.
 * <p>
 * The scan operator output schema can be <i>defined</i> or <i>dynamic.</i>
 *
 * <h4>Defined Schema</h4>
 *
 * The planner computes a defined schema from metadata, as in a typical
 * query engine. A defined schema defines the output schema directly:
 * the defined schema <b>is</b> the output schema. Drill's planner does not
 * yet support a defined schema, but work is in progress to get there for
 * some cases.
 * <p>
 * With a defined schema, the reader is given a fully-defined schema and
 * its job is to produce vectors that match the given schema. (The details
 * are handled by the {@link ResultSetLoader}.)
 * <p>
 * At present, since the planner does not actually provide a defined schema,
 * we support it in this class, and verify that the defined schema, if provided,
 * exactly matches the names in the project list in the same order.
 *
 * <h4>Dynamic Schema</h4>
 *
 * A dynamic schema is one defined at run time: the traditional Drill approach.
 * A dynamic schema starts with a <i>projection list</i> : a list of column names
 * without types.
 * This class converts the project list into a dynamic reader schema which is
 * a schema in which each column has the type {@code LATE}, which basically means
 * "a type to be named later" by the reader.
 *
 * <h4>Hybrid Schema</h4>
 *
 * Some readers support a <i>provided schema</i>, which is an concept similar to,
 * but distinct from, a defined schema. The provided schema provides <i>hints</i>
 * about a schema. At present, it
 * is an extra; not used or understood by the planner. Thus, the projection
 * list is independent of the provided schema: the lists may be disjoint.
 * <p>
 * With a provided schema, the project list defines the output schema. If the
 * provided schema provides projected columns, then the provided schema for those
 * columns flow to the output schema, just as for a defined schema. Similarly, the
 * reader is given a defined schema for those columns.
 * <p>
 * Where a provided schema differs is that the project list can include columns
 * not in the provided schema, such columns act like the dynamic case: the reader
 * defines the column type.
 *
 * <h4>Projection Types</h4>
 *
 * Drill will pass in a project list which is one of three kinds:
 * <p><ul>
 * <li>{@code >SELECT *}: Project all data source columns, whatever they happen
 * to be. Create columns using names from the data source. The data source
 * also determines the order of columns within the row.</li>
 * <li>{@code >SELECT a, b, c, ...}: Project a specific set of columns, identified by
 * case-insensitive name. The output row uses the names from the SELECT list,
 * but types from the data source. Columns appear in the row in the order
 * specified by the {@code SELECT}.</li>
 * <li{@code >SELECT ...}: Project nothing, occurs in {@code >SELECT COUNT(*)}
 * type queries. The provided projection list contains no (table) columns, though
 * it may contain metadata columns.</li>
 * </ul>
 * Names in the project list can reference any of five distinct types of output
 * columns:
 * <p><ul>
 * <li>Wildcard ("*") column: indicates the place in the projection list to insert
 * the table columns once found in the table projection plan.</li>
 * <li>Data source columns: columns from the underlying table. The table
 * projection planner will determine if the column exists, or must be filled
 * in with a null column.</li>
 * <li>The generic data source columns array: {@code >columns}, or optionally
 * specific members of the {@code >columns} array such as {@code >columns[1]}.
 * (Supported only by specific readers.)</li>
 * <li>Implicit columns: {@code >fqn}, {@code >filename}, {@code >filepath}
 * and {@code >suffix}. These reference
 * parts of the name of the file being scanned.</li>
 * <li>Partition columns: {@code >dir0}, {@code >dir1}, ...: These reference
 * parts of the path name of the file.</li>
 * </ul>
 *
 * <h4>Empty Schema</h4>
 *
 * A special case occurs if the projection list is empty which indicates that
 * the query is a {@code COUNT(*)}: we need only a count of columns, but none
 * of the values. Implementation of the count is left to the specific reader
 * as some can optimize this case. The output schema may include a single
 * dummy column. In this case, the first batch defines the schema expected
 * from all subsequent readers and batches.
 *
 * <h4>Implicit Columns</h4>
 *
 * The project list can contain implicit columns for data sources which support
 * them. Implicit columns are disjoint from data source columns and are provided
 * by Drill itself. This class effectively splits the projection list into
 * a set of implicit columns, and the remainder of the list which are the
 * reader columns.
 *
 * <h4>Reader Input Schema</h4>
 *
 * The various forms of schema above produce a <i>reader input schema</i>:
 * the schema given to the reader. The reader input schema is the set of
 * projected columns, minus implicit columns, along with available type
 * information.
 * <p>
 * If the reader can produce only one type
 * for each column, then the provided or defined schema should already specify
 * that type, and the reader can simply ignore the reader input schema. (This
 * feature allows this scheme to be compatible with older readers.)
 * <p>
 * However, if the reader can convert a column to multiple types, then the
 * reader should use the reader input schema to choose a type. If the input
 * schema is dynamic (type is {@code LATE}), then the reader chooses the
 * column type and should chose the "most natural" type.
 *
 * <h4>Reader Output Schema</h4>
 *
 * The reader proceeds to read a batch of data, choosing types for dynamic
 * columns. The reader may provide a subset of projected columns if, say
 * the reader reads an older file that is missing some columns or (for a
 * dynamic schema), the user specified columns which don't actually exist.
 * <p>
 * The result is the <i>reader output schema</i>: a subset of the reader
 * input schema in which each included column has a concrete type. (The
 * reader may have provided extra columns. In this case, the
 * {@code ResultSetLoader} will have ignored those columns, providing a
 * dummy column writer, and omitting non-projected columns from the reader
 * output schema.)
 * <p>
 * The reader output schema is provided to this class which resolves any
 * dynamic columns to the concrete type provided by the reader. If the
 * column was already resolved, this class ensures that the reader's
 * column type matches the resolved type to prevent column type changes.
 *
 * <h4>Dynamic Wildcard Schema</h4>
 *
 * Traditional query planners resolve the wildcard ({@code *}) in the
 * planner. When using a dynamic schema, Drill resolves the wildcard at
 * run time. In this case, the reader input schema is empty and the reader
 * defines the entire set of columns: names and types. This class then
 * replaces the wildcard with the columns from the reader.
 *
 * <h4>Missing Columns</h4>
 *
 * When the reader output schema is a subset of the reader input schema,
 * the we have a set of <i>missing columns</i> (also called "null columns").
 * A part of the scan framework must invent vectors for these columns. If
 * the type is available, then that is the type used, otherwise the missing
 * column handler must invent a type (such as the classic
 * {@code nullable INT} historically used.) If the mode is
 * nullable, the column is filled with nulls. If non-nullable, the column
 * is filled with a default value. All of this work happens outside of
 * this class.
 * <p>
 * The missing column handler defined its own output schema which is
 * resolved by this class identical to how the reader schema is resolved.
 * The result is that all columns are now resolved to a concrete type.
 * <p>
 * Missing columns may be needed even for a wildcard if a first reader
 * discovered 3 columns, say, but a later reader encounters only two of
 * them.
 *
 * <h4>Subsequent Readers and Schema Changes</h4>
 *
 * All of the above occurs during the first batch of data. After that,
 * the schema is fully defined: subsequent readers will encounter only
 * a fully defined schema, which it must handle the same as if the scan
 * was given a defined schema.
 * <p>
 * This rule works file for an explicit project list. However, if the
 * project list is dynamic, and contains a wildcard, then the reader
 * defines the output schema. What happens if a reader adds columns
 * (or a second or later reader discovers new columns)? Traditionally,
 * Drill simply adds those columns and sends a {@code OK_NEW_SCHEMA}
 * (schema change) downstream for other operators to deal with.
 * <p>
 * This class supports the traditional approach as an option. This class
 * also supports a more rational, strict rule: the schema is fixed after
 * the first batch. That is, the first batch defines a <i>schema commit
 * point</i> after which the scan agrees not to change the schema. In
 * this scenario, the first batch defines a schema (and project list)
 * given to all subsequent readers. Any new columns are ignored (with
 * a warning in the log.)
 *
 * <h4>Output Schema</h4>
 *
 * All of the above contribute to the <i>output schema</i>: the schema
 * sent downstream to the next operator. All of the above work is done to
 * either:
 * <ul>
 * <li>Pass the defined schema to the output, with the reader (and missing
 * columns handler) producing columns that match that schema.</li>
 * <li>Expand the dynamic schema with details provided by the reader
 * (and missing columns hander), including the actual set of columns if
 * the dynamic schema includes a wildcard.</li>
 * </ul>
 * <p>
 * Either way, the result is a schema which describes the actual vectors
 * sent downstream.
 *
 * <h4>Consumers</h4>
 *
 * Information from this class is used in multiple ways:
 * <ul>
 * <li>A project list is given to the {@code ResultSetLoader} to specify which
 * columns to project to vectors, and which to satisfy with a dummy column
 * reader.</li>
 * <li>The reader, via the {code SchemaNegotiator} uses the reader input
 * schema.</li>
 * <li>The reader, via the {@code ResultSetLoader} provides the reader output
 * schema.</li>
 * <li>An implicit column manager handles the various implicit and partition
 * directory columns: identifying them then later providing vector values.</li>
 * <li>A missing columns handler fills in missing columns.</li>
 * </ul>
 *
 * <h4>Design</h4>
 *
 * Schema resolution is a set of layers of choices. Each level and choice is
 * represented by a class: virtual method pick the right path based on class
 * type rather than using a large collection of if-statements.
 *
 * <h4>Maps</h4>
 *
 * Maps present a difficult challenge. Drill allows projection within maps
 * and we wish to exploit that in the scan. For example: {@code m.a}. The
 * column state classes provide a map class. However, the projection notation
 * is ambiguous: {@code m.a} could be a map {@code `m`} with a child column
 * {@code 'a'}. Or, it could be a {@code DICT} with a {code VARCHAR} key.
 * <p>
 * To handle this, if we only have the project list, we use an unresolved
 * column state, even if the projection itself has internal structure. We
 * use a projection-based filter in the {@code ResultSetLoader} to handle
 * the ambiguity. The projection filter, when presented with the reader's
 * choice for column type, will check if that type is consistent with projection.
 * If so, the reader will later present the reader output schema which we
 * use to resolve the projection-only unresolved column to a map column.
 * (Or, if the column turns out to be a {@code DICT}, to a simple unresolved
 * column.)
 * <p>
 * If the scan contains a second reader, then the second reader is given a
 * stricter form of projection filter: one based on the actual {@code MAP}
 * (or {@code DICT}) column.
 * <p>
 * If a defined or provided schema is available, then the schema tracker
 * does have sufficient information to resolve the column directly to a
 * map column, and the first reader will have the strict projection filter.
 * <p>
 * A user can project a map column which does not actually exist (or, at
 * least, is not known to the first reader.) In that case, the missing
 * column logic applies, but within the map. As a result, a second reader
 * may encounter a type conflict if it discovers the previously-missing
 * column, and finds that the default type conflicts with the real type.
 * <p>
 * @see {@link ImplicitColumnExplorer}, the class from which this class
 * evolved
 */
public interface ScanSchemaTracker {

  enum ProjectionType {

    /**
     * This a wildcard projection. The project list may include
     * implicit columns in addition to the wildcard.
     */
    ALL,

    /**
     * This is an empty projection, such as for a COUNT(*) query.
     * No implicit columns will appear in such a scan.
     */
    NONE,

    /**
     * Explicit projection with a defined set of columns.
     */
    SOME
  }

  ProjectionType projectionType();

  /**
   * Return the projection for a column, if any.
   */
  ProjectedColumn columnProjection(String colName);

  /**
   * Is the scan schema resolved? The schema is resolved depending on the
   * complex lifecycle explained in the class comment. Resolution occurs
   * when the wildcard (if any) is expanded, and all explicit projection
   * columns obtain a definite type. If schema change is disabled, the
   * schema will not change once it is resolved. If schema change is allowed,
   * then batches or readers may extend the schema, triggering a schema
   * change, and so the scan schema may move from one resolved state to
   * another.
   * <p>
   * The schema will be fully resolved after the first batch of data arrives
   * from a reader (since the reader lifecycle will then fill in any missing
   * columns.) The schema may be resolved sooner (such as if a strict provided
   * schema, or an early reader schema is available and there are no missing
   * columns.)
   *
   * @return {@code} if the schema is resolved, and hence the
   * {@link #outputSchema()} is available, {@code false} if the schema
   * contains one or more dynamic columns which are not yet resolved.
   */
  boolean isResolved();

  /**
   * Gives the output schema version which will start at some arbitrary
   * positive number.
   * <p>
   * If schema change is allowed, the schema version allows detecting
   * schema changes as the scan schema moves from one resolved state to
   * the next. Each schema will have a unique, increasing version number.
   * A schema change has occurred if the version is newer than the previous
   * output schema version.
   *
   * @return the schema version. The absolute number is not important,
   * rather an increase indicates one or more columns were added at the
   * top level or within a map at some nesting level
   */
  int schemaVersion();

  /**
   * Drill defines a wildcard to expand not just reader columns, but also
   * partition columns. When the implicit column handlers sees that the
   * query has a wildcard (by calling {@link #isProjectAll()}), the handler
   * then determines which partition columns are needed and calls this
   * method to add each one.
   */
  void expandImplicitCol(ColumnMetadata resolved, ImplicitColumnMarker marker);

  /**
   * Indicate that implicit column parsing is complete. Returns the implicit
   * columns as identified by the implicit column handler, in the order of the
   * projection list. Implicit columns do not appear in a reader input schema,
   * and it is an error for the reader to produce such columns.
   *
   * @return a sub-schema of only implicit columns, in the order in which
   * they appear in the output schema
   */
  TupleMetadata applyImplicitCols();

  /**
   * If a reader can define a schema before reading data, apply that
   * schema to the scan schema. Allows the scan to report its output
   * schema before the first batch of data if the scan schema becomes
   * resolved after the early reader schema.
   */
  void applyEarlyReaderSchema(TupleMetadata readerSchema);

  /**
   * The schema which the reader should produce. Depending on the type of
   * the scan (specifically, if {@link #isProjectAll()} is {@code true}),
   * the reader may produce additional columns beyond those in the the
   * reader input schema. However, for any batch, the reader, plus the
   * missing columns handler, must produce all columns in the reader input
   * schema.
   * <p>
   * Formally:<pre><code>
   * reader input schema = output schema - implicit col schema
   * </code></pre>
   *
   * @return the sub-schema which includes those columns which the reader
   * should provide, excluding implicit columns
   */
  TupleMetadata readerInputSchema();

  /**
   * Identifies the missing columns given a reader output schema. The reader
   * output schema are those columns which the reader actually produced.
   * <p>
   * Formally:<pre><code>
   * missing cols = reader input schema - reader output schema
   * </code></pre>
   * <p>
   * The reader output schema can contain extra, newly discovered columns.
   * Those are ignored when computing missing columns. Thus, the subtraction
   * is set subtraction: remove columns common to the two sets.
   *
   * @code the sub-schema of the reader schema which excludes the columns
   * which the reader provided. The result are the "missing" columns which
   * have no values in a given batch of rows
   */
  TupleMetadata missingColumns(TupleMetadata readerOutputSchema);

  /**
   * Returns the scan output schema which is a somewhat complicated
   * computation that depends on the projection type.
   * <p>
   * For a wildcard schema:<pre><code>
   * output schema = implicit cols U reader output schema
   * </code></pre>
   * <p>
   * For an explicit projection:<pre><code>
   * output schema = projection list
   * </code></pre>
   * Where the projection list is augmented by types from the
   * provided schema, implicit columns or readers.
   * <p>
   * A defined schema <i>is</i> the output schema, so:<code><pre>
   * output schema = defined schema
   * </pre></code>
   *
   * @return the complete output schema provided by the scan to downstream
   * operators. Includes both reader and implicit columns, in the order
   * of the projection list or, for a wildcard, in the order of the first
   * reader
   */
  TupleMetadata outputSchema();

  /**
   * A reader is responsible for reading columns in the reader input schema.
   * A reader may read additional columns. The projection filter is passed to
   * the {@link ResultSetLoader} to determine which columns should be projected,
   * allowing the reader to be blissfully ignorant of which columns are needed.
   * The result set loader provides a dummy reader for unprojected columns.
   * (A reader can, via the result set loader, find if a column is projected if
   * doing so helps reader efficiency.)
   * <p>
   * The projection filter is the first line of defense for schema conflicts.
   * The {code ResultSetLoader} will query the filter with a full column
   * schema. If that schema conflicts with the scan schema for that column,
   * this method will raise a {@code UserException}, which typically indicates
   * a programming error, or a very odd data source in which a column changes
   * types between batches.
   *
   * @param errorContext the reader-specific error context to use if
   * errors are found
   * @return a filter used to decide which reader columns to project during
   * reading
   */
  ProjectionFilter projectionFilter(CustomErrorContext errorContext);

  /**
   * Once a reader has read a batch, the reader will have provided a type
   * for each projected column which the reader knows about. For a wildcard
   * projection, the reader will have added all the columns that it found.
   * This call takes the reader output schema and merges it with the current
   * scan schema to resolve dynamic types to concrete types and to add
   * newly discovered columns.
   * <p>
   * The process can raise an exception if the reader projects a column that
   * it shouldn't (which is not actually possible because of the way the
   * {@code ResultSetLoader} works.) An error can also occur if the reader
   * provides a type different than that already defined in the scan schema
   * by a defined schema, a provided schema, or a previous reader in the same
   * scan. In such cases, the reader is expected to have converted its input
   * type to the specified type, which was presumably selected because the
   * reader is capable of the required conversion.
   *
   * @param readerOutputSchema the actual schema produced by a reader when
   * reading a record batch
   * @param errorContext the reader-specific error context to use if
   * errors are found
   */
  void applyReaderSchema(TupleMetadata readerOutputSchema,
      CustomErrorContext errorContext);

  /**
   * The missing column handler obtains the list of missing columns from
   * {@link #missingColumns()}. Depending on the scan lifecycle, some of the
   * columns may have a type, others may be dynamic. The missing column handler
   * chooses a type for any dynamic columns, then calls this method to tell
   * the scan schema tracker the now-resolved column type.
   * <p>
   * Note: a goal of the provided/defined schema system is to avoid the need
   * to guess types for missing columns since doing so quite often leads
   * to problems further downstream in the query. Ideally, the type of missing
   * columns will be known (via the provided or defined schema) to avoid
   * such conflicts.
   */
  void resolveMissingCols(TupleMetadata missingCols);

  /**
   * The scan-level error context used for errors which may occur before the
   * first reader starts. The reader will provide a more detailed error context
   * that describes what is being read.
   *
   * @return the scan-level error context
   */
  CustomErrorContext errorContext();

  /**
   * Returns the internal scan schema. Primarily for testing.
   * @return the internal mutable scan schema
   */
  @VisibleForTesting
  MutableTupleSchema internalSchema();
}
