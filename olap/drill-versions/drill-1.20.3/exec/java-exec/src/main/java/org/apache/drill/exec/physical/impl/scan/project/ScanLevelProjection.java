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
package org.apache.drill.exec.physical.impl.scan.project;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedColumn;
import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedWildcardColumn;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.physical.resultSet.project.ImpliedTupleRequest;
import org.apache.drill.exec.physical.resultSet.project.Projections;
import org.apache.drill.exec.physical.resultSet.project.RequestedTuple;
import org.apache.drill.exec.physical.resultSet.project.RequestedColumn;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Parses and analyzes the projection list passed to the scanner. The
 * scanner accepts a projection list and a plugin-specific set of items
 * to read. The scan operator produces a series of output batches, which
 * (in the best case) all have the same schema. Since Drill is "schema
 * on read", in practice batch schema may evolve. The framework tries
 * to "smooth" such changes where possible. An output schema adds another
 * level of stability by specifying the set of columns to project (for
 * wildcard queries) and the types of those columns (for all queries.)
 * <p>
 * The projection list is per scan, independent of any tables that the
 * scanner might scan. The projection list is then used as input to the
 * per-table projection planning.
 *
 * <h4>Overview</h4>
 *
 * In most query engines, this kind of projection analysis is done at
 * plan time. But, since Drill is schema-on-read, we don't know the
 * available columns, or their types, until we start scanning a table.
 * The table may provide the schema up-front, or may discover it as
 * the read proceeds. Hence, the job here is to make sense of the
 * project list based on static a-priori information, then to create
 * a list that can be further resolved against an table schema when it
 * appears. This give us two steps:
 * <ul>
 * <li>Scan-level projection: this class, that handles schema for the
 * entire scan operator.</li>
 * <li>Table-level projection: defined elsewhere, that merges the
 * table and scan-level projections.
 * </ul>
 * <p>
 * Accepts the inputs needed to plan a projection, builds the mappings,
 * and constructs the projection mapping object.
 * <p>
 * Builds the per-scan projection plan given a set of projected columns.
 * Determines the output schema, which columns to project from the data
 * source, which are metadata, and so on.
 * <p>
 * An annoying aspect of SQL is that the projection list (the list of
 * columns to appear in the output) is specified after the SELECT keyword.
 * In Relational theory, projection is about columns, selection is about
 * rows...
 *
 * <h4>Projection Mappings</h4>
 *
 * Mappings can be based on three primary use cases:
 * <p><ul>
 * <li><tt>SELECT *</tt>: Project all data source columns, whatever they happen
 * to be. Create columns using names from the data source. The data source
 * also determines the order of columns within the row.</li>
 * <li><tt>SELECT columns</tt>: Similar to SELECT * in that it projects all columns
 * from the data source, in data source order. But, rather than creating
 * individual output columns for each data source column, creates a single
 * column which is an array of Varchars which holds the (text form) of
 * each column as an array element.</li>
 * <li><tt>SELECT a, b, c, ...</tt>: Project a specific set of columns, identified by
 * case-insensitive name. The output row uses the names from the SELECT list,
 * but types from the data source. Columns appear in the row in the order
 * specified by the SELECT.</li>
 * <li<tt>SELECT ...</tt>: SELECT nothing, occurs in <tt>SELECT COUNT(*)</tt>
 * type queries. The provided projection list contains no (table) columns, though
 * it may contain metadata columns.</li>
 * </ul>
 * Names in the SELECT list can reference any of five distinct types of output
 * columns:
 * <p><ul>
 * <li>Wildcard ("*") column: indicates the place in the projection list to insert
 * the table columns once found in the table projection plan.</li>
 * <li>Data source columns: columns from the underlying table. The table
 * projection planner will determine if the column exists, or must be filled
 * in with a null column.</li>
 * <li>The generic data source columns array: <tt>columns</tt>, or optionally
 * specific members of the <tt>columns</tt> array such as <tt>columns[1]</tt>.</li>
 * <li>Implicit columns: <tt>fqn</tt>, <tt>filename</tt>, <tt>filepath</tt>
 * and <tt>suffix</tt>. These reference
 * parts of the name of the file being scanned.</li>
 * <li>Partition columns: <tt>dir0</tt>, <tt>dir1</tt>, ...: These reference
 * parts of the path name of the file.</li>
 * </ul>
 *
 * <h4>Projection with a Schema</h4>
 *
 * The client can provide an <i>output schema</i> that defines the types (and
 * defaults) for the tuple produced by the scan. When a schema is provided,
 * the above use cases are extended as follows:
 * <p><ul>
 * <li><tt>SELECT *</tt> with strict schema: All columns in the output schema
 * are projected, and only those columns. If a reader offers additional columns,
 * those columns are ignored. If the reader omits output columns, the default value
 * (if any) for the column is used.</li>
 * <li><tt>SELECT *</tt> with a non-strict schema: the output tuple contains all
 * columns from the output schema as explained above. In addition, if the reader
 * provides any columns not in the output schema, those columns are appended to
 * the end of the tuple. (That is, the output schema acts as it it were from
 * an imaginary "0th" reader.)</li>
 * <li>Explicit projection: only the requested columns appear, whether from the
 * output schema, the reader, or  as nulls.</li>
 * </ul>
 * <p>
 * @see {@link org.apache.drill.exec.store.ColumnExplorer}, the class from which this class
 * evolved
 */
public class ScanLevelProjection {

  /**
   * Identifies the kind of projection done for this scan.
   */
  public enum ScanProjectionType {

    /**
     * No projection. Occurs for SELECT COUNT(*) ... queries.
     */
    EMPTY,

    /**
     * Wildcard. Occurs for SELECT * ... queries when no output schema is
     * available. The scan projects all columns from all readers, using the
     * type from that reader. Schema "smoothing", if enabled, will attempt
     * to preserve column order, type and mode from one reader to the next.
     */
    WILDCARD,

    /**
     * Explicit projection. Occurs for SELECT a, b, c ... queries, whether or
     * not an output schema is present. In this case, the projection list
     * identifies the set of columns to project and their order. The output
     * schema, if present, specifies data types and modes.
     */
    EXPLICIT,

    /**
     * Wildcard query expanded using an output schema. Occurs for a
     * SELECT * ... query with an output schema. The set of projected columns
     * are those from the output schema, in the order specified by the schema,
     * with names (and name case) specified by the schema. In this mode, the
     * schema is partial: readers may include additional columns which are
     * appended to those provided by the schema.
     * <p>
     * TODO: Provide a strict mode that forces the use of the types and modes
     * from the output schema. In lenient mode, the framework will adjust
     * mode to allow the query to succeed (changing a required mode to
     * optional, say, if the column is not provided by the reader and has
     * no default. Strict mode would fail the query in this case.)
     * <p>
     * TODO: Enable schema smoothing in this case: use that mechanism to
     * smooth over the "extra" reader columns.
     */
    SCHEMA_WILDCARD,

    /**
     * Wildcard query expanded using an output schema in "strict" mode.
     * Only columns from the output schema will be projected. If a reader
     * offers columns not in the output schema, they will be ignored. That
     * is, a SELECT * query expands to exactly the columns in the schema.
     * <p>
     * TODO: Provide a strict column mode that will fail the query if a projected
     * column is required, has no default, and is not provided by the reader. In
     * the normal lenient mode, the scan framework will adjust the data mode to
     * optional so that the query will run.
     */
    STRICT_SCHEMA_WILDCARD;

    public boolean isWildcard() {
      return this == WILDCARD ||
             this == SCHEMA_WILDCARD ||
             this == STRICT_SCHEMA_WILDCARD;
    }
  }

  /**
   * Interface for add-on parsers, avoids the need to create
   * a single, tightly-coupled parser for all types of columns.
   * The main parser handles wildcards and assumes the rest of
   * the columns are table columns. The add-on parser can tag
   * columns as special, such as to hold metadata.
   */
  public interface ScanProjectionParser {
    void bind(ScanLevelProjection builder);
    boolean parse(RequestedColumn inCol);
    void validate();
    void validateColumn(ColumnProjection col);
    void build();
  }

  public static class Builder {
    private List<SchemaPath> projectionList;
    private final List<ScanProjectionParser> parsers = new ArrayList<>();
    private TupleMetadata providedSchema;

    /**
     * Context used with error messages.
     */
    protected CustomErrorContext errorContext;

    /**
     * Specify the set of columns in the SELECT list. Since the column list
     * comes from the query planner, assumes that the planner has checked
     * the list for syntax and uniqueness.
     *
     * @param projectionList list of columns in the SELECT list in SELECT list order
     * @return this builder
     */
    public Builder projection(List<SchemaPath> projectionList) {
      this.projectionList = projectionList;
      return this;
    }

    public Builder parsers(List<ScanProjectionParser> parsers) {
      this.parsers.addAll(parsers);
      return this;
    }

    public Builder providedSchema(TupleMetadata providedSchema) {
      this.providedSchema = providedSchema;
      return this;
    }

    public Builder errorContext(CustomErrorContext context) {
      this.errorContext = context;
      return this;
    }

    public ScanLevelProjection build() {
      return new ScanLevelProjection(this);
    }

    public TupleMetadata providedSchema( ) {
      return providedSchema == null || providedSchema.size() == 0
          ? null : providedSchema;
    }

    public List<SchemaPath> projectionList() {
      if (projectionList == null) {
        projectionList = new ArrayList<>();
        projectionList.add(SchemaPath.STAR_COLUMN);
      }
      return projectionList;
    }
  }

  // Input

  /**
   * Context used with error messages.
   */
  protected final CustomErrorContext errorContext;
  protected final List<SchemaPath> projectionList;
  protected final TupleMetadata readerSchema;

  // Configuration

  protected List<ScanProjectionParser> parsers;

  // Internal state

  protected boolean includesWildcard;
  protected boolean sawWildcard;

  // Output

  protected List<ColumnProjection> outputCols = new ArrayList<>();

  /**
   * Projection definition for the scan a whole. Parsed form of the input
   * projection list.
   */
  protected RequestedTuple outputProjection;

  /**
   * Projection definition passed to each reader. This is the set of
   * columns that the reader is asked to provide.
   */
  protected ProjectionFilter readerProjection;
  protected ScanProjectionType projectionType;

  private ScanLevelProjection(Builder builder) {
    this.projectionList = builder.projectionList();
    this.parsers = builder.parsers;
    this.readerSchema = builder.providedSchema();
    this.errorContext = builder.errorContext;
    doParse();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder shortcut, primarily for tests.
   */
  @VisibleForTesting
  public static ScanLevelProjection build(List<SchemaPath> projectionList,
      List<ScanProjectionParser> parsers) {
    return new Builder()
        .projection(projectionList)
        .parsers(parsers)
        .build();
  }

  /**
   * Builder shortcut, primarily for tests.
   */
  @VisibleForTesting
  public static ScanLevelProjection build(List<SchemaPath> projectionList,
      List<ScanProjectionParser> parsers,
      TupleMetadata outputSchema) {
    return new Builder()
        .projection(projectionList)
        .parsers(parsers)
        .providedSchema(outputSchema)
        .build();
  }

  private void doParse() {
    outputProjection = Projections.parse(projectionList);
    switch (outputProjection.type()) {
      case ALL:
        includesWildcard = true;
        projectionType = ScanProjectionType.WILDCARD;
        break;
      case NONE:
        projectionType = ScanProjectionType.EMPTY;
        break;
      default:
        projectionType = ScanProjectionType.EXPLICIT;
        break;
    }

    for (ScanProjectionParser parser : parsers) {
      parser.bind(this);
    }

    // Process projected columns.
    for (RequestedColumn inCol : outputProjection.projections()) {
      if (inCol.isWildcard()) {
        mapWildcard(inCol);
      } else {
        mapColumn(inCol);
      }
    }
    verify();
    for (ScanProjectionParser parser : parsers) {
      parser.build();
    }

    buildReaderProjection();
  }

  private void buildReaderProjection() {

    // Create the reader projection which includes either all columns
    // (saw a wildcard) or just the unresolved columns (which excludes
    // implicit columns.)
    //
    // Note that only the wildcard without schema can omit the output
    // projection. With a schema, we want the schema columns (which may
    // or may not correspond to reader columns.)

    RequestedTuple rootProjection;
    if (projectionType == ScanProjectionType.EMPTY) {
      rootProjection = ImpliedTupleRequest.NO_MEMBERS;
    } else if (projectionType != ScanProjectionType.EXPLICIT) {
      rootProjection = ImpliedTupleRequest.ALL_MEMBERS;
    } else {
      List<RequestedColumn> outputProj = new ArrayList<>();
      for (ColumnProjection col : outputCols) {
        if (col instanceof AbstractUnresolvedColumn) {
          outputProj.add(((AbstractUnresolvedColumn) col).element());
        }
      }
      rootProjection = Projections.build(outputProj);
    }
    readerProjection = ProjectionFilter.providedSchemaFilter(
        rootProjection, readerSchema, errorContext);
  }

  /**
   * Wildcard is special: add it, then let parsers add any custom
   * columns that are needed. The order is important: we want custom
   * columns to follow table columns.
   */
  private void mapWildcard(RequestedColumn inCol) {

    // Wildcard column: this is a SELECT * query.
    assert includesWildcard;
    if (sawWildcard) {
      throw new IllegalArgumentException("Duplicate * entry in project list");
    }

    // Expand strict schema columns, if provided
    assert projectionType == ScanProjectionType.WILDCARD;
    boolean expanded = expandOutputSchema();

    // Remember the wildcard position, if we need to insert it.
    // Ensures that the main wildcard expansion occurs before add-on
    // columns.
    int wildcardPosn = outputCols.size();

    // Parsers can consume the wildcard. But, all parsers must
    // have visibility to the wildcard column.
    for (ScanProjectionParser parser : parsers) {
      if (parser.parse(inCol)) {
        wildcardPosn = -1;
      }
    }

    // Set this flag only after the parser checks.
    sawWildcard = true;

    // If not consumed, put the wildcard column into the projection list as a
    // placeholder to be filled in later with actual table columns.
    if (expanded) {
      projectionType =
          readerSchema.booleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP)
          ? ScanProjectionType.STRICT_SCHEMA_WILDCARD
          : ScanProjectionType.SCHEMA_WILDCARD;
    } else if (wildcardPosn != -1) {
      outputCols.add(wildcardPosn, new UnresolvedWildcardColumn(inCol));
    }
  }

  private boolean expandOutputSchema() {
    if (readerSchema == null) {
      return false;
    }

    // Expand the wildcard. From the perspective of the reader, this is an explicit
    // projection, so enumerate the columns as though they were in the project list.
    // Take the projection type from the output column's data type. That is,
    // INT[] is projected as ARRAY, etc.

    for (int i = 0; i < readerSchema.size(); i++) {
      ColumnMetadata col = readerSchema.metadata(i);

      // Skip columns tagged as "special"; those that should not expand
      // automatically.
      if (col.booleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD)) {
        continue;
      }
      outputCols.add(new UnresolvedColumn(null, col));
    }
    return true;
  }

  /**
   * Map the column into one of five categories.
   * <ol>
   * <li>Star column (to designate SELECT *)</li>
   * <li>Partition file column (dir0, dir1, etc.)</li>
   * <li>Implicit column (fqn, filepath, filename, suffix)</li>
   * <li>Special <tt>columns</tt> column which holds all columns as
   * an array.</li>
   * <li>Table column. The actual match against the table schema
   * is done later.</li>
   * </ol>
   *
   * Actual mapping is done by parser extensions for all but the
   * basic cases.
   *
   * @param inCol the SELECT column
   */
  private void mapColumn(RequestedColumn inCol) {

    // Give the extensions first crack at each column.
    // Some may want to "sniff" a column, even if they
    // don't fully handle it.

    for (ScanProjectionParser parser : parsers) {
      if (parser.parse(inCol)) {
        return;
      }
    }

    // If the project list has a wildcard, and the column is not one recognized
    // by the specialized parsers above, then just ignore it. It is likely a duplicate
    // column name. In any event, it will be processed by the Project operator on
    // top of this scan.

    if (includesWildcard) {
      return;
    }

    // This is a desired table column.
    addTableColumn(inCol);
  }

  private void addTableColumn(RequestedColumn inCol) {
    ColumnMetadata outputCol = null;
    if (readerSchema != null) {
      outputCol = readerSchema.metadata(inCol.name());
    }
    addTableColumn(new UnresolvedColumn(inCol, outputCol));
  }

  public void addTableColumn(ColumnProjection outCol) {
    outputCols.add(outCol);
  }

  public void addMetadataColumn(ColumnProjection outCol) {
    outputCols.add(outCol);
  }

  /**
   * Once all columns are identified, perform a final pass
   * over the set of columns to do overall validation. Each
   * add-on parser is given an opportunity to do its own
   * validation.
   */
  private void verify() {

    // Let parsers do overall validation.
    for (ScanProjectionParser parser : parsers) {
      parser.validate();
    }

    // Validate column-by-column.
    for (ColumnProjection outCol : outputCols) {
      for (ScanProjectionParser parser : parsers) {
        parser.validateColumn(outCol);
      }
    }
  }

  public CustomErrorContext context() { return errorContext; }

  /**
   * Return the set of columns from the SELECT list
   * @return the SELECT list columns, in SELECT list order
   */
  public List<SchemaPath> requestedCols() { return projectionList; }

  /**
   * The entire set of output columns, in output order. Output order is
   * that specified in the SELECT (for an explicit list of columns) or
   * table order (for SELECT * queries).
   * @return the set of output columns in output order
   */
  public List<ColumnProjection> columns() { return outputCols; }

  public ScanProjectionType projectionType() { return projectionType; }

  /**
   * Return whether this is a SELECT * query
   * @return true if this is a SELECT * query
   */
  public boolean projectAll() { return projectionType.isWildcard(); }

  /**
   * Returns true if the projection list is empty. This usually
   * indicates a <tt>SELECT COUNT(*)</tt> query (though the scan
   * operator does not have the context to know that an empty
   * list does, in fact, imply a count-only query...)
   *
   * @return true if no table columns are projected, false
   * if at least one column is projected (or the query contained
   * the wildcard)
   */
  public boolean isEmptyProjection() { return projectionType == ScanProjectionType.EMPTY; }

  public RequestedTuple rootProjection() { return outputProjection; }

  public ProjectionFilter readerProjection() { return readerProjection; }

  public boolean hasReaderSchema() { return readerSchema != null; }

  public TupleMetadata readerSchema() { return readerSchema; }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("[")
        .append(getClass().getSimpleName())
        .append(" projection=")
        .append(outputCols.toString())
        .append("]")
        .toString();
  }
}
