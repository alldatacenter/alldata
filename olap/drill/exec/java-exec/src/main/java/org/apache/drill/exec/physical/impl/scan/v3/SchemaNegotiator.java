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
package org.apache.drill.exec.physical.impl.scan.v3;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ProjectedColumn;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Negotiates the table schema with the scanner framework and provides
 * context information for the reader. Scans use either a "dynamic" or
 * a defined schema.
 * <p>
 * Regardless of the schema type, the result of building the schema is a
 * result set loader used to prepare batches for use in the query. The reader
 * can simply read all columns, allowing the framework to discard unwanted
 * values. Or for efficiency, the reader can check the column metadata to
 * determine if a column is projected, and if not, then don't even read
 * the column from the input source.
 *
 * <h4>Defined Schema</h4>
 *
 * If defined, the execution plan provides the output schema (presumably
 * computed from an accurate metadata source.) The reader must populate
 * the proscribed rows, performing column type conversions as needed.
 * The reader can determine if the schema is defined by calling
 * {@link hasOutputSchema()}.
 * <p>
 * At present, the scan framework filters the "provided schema" against
 * the project list so that this class presents only the actual output
 * schema. Future versions may do the filtering in the planner, but
 * the result for readers will be the same either way.
 *
 * <h4>Dynamic Schema</h4>
 *
 * A dynamic schema occurs when the plan does not specify a schema.
 * Drill is unique in its support for "schema on read" in the sense
 * that Drill does not know the schema until the reader defines it at
 * scan time.
 * <p>
 * The reader and scan framework coordinate to form the output schema.
 * The reader offers the columns it has available. The scan framework
 * uses the projection list to decide which to accept. Either way the
 * scan framework provides a column reader for the column (returning a
 * do-nothing "dummy" reader if the column is unprojected.)
 * <p>
 * With a dynamic schema, readers offer a schema in one of two ways:
 * <p>
 * The reader provides the table schema in one of two ways: early schema
 * or late schema. Either way, the project list from the physical plan
 * determines which
 * table columns are materialized and which are not. Readers are provided
 * for all table columns for readers that must read sequentially, but
 * only the materialized columns are written to value vectors.
 *
 * <h4>Early Dynamic Schema</h4>
 *
 * Some readers can determine the source schema at the start of a scan.
 * For example, a CSV file has headers, a Parquet file has footers, both
 * of which define a schema. This case is called "early schema." The
 * reader fefines the schema by calling
 * {@link #tableSchema(TupleMetadata)} to provide the known schema.
 *
 * <h4>Late Dynamic Schema</h4>
 *
 * Other readers don't know the input schema until the reader actually
 * reads the data. For example, JSON typically has no schema, but does
 * have sufficient structure (name/value pairs) to infer one.
 * <p>
 * The late schema reader calls {@link RowSetLoader#addColumn()} to
 * add each column as it is discovered during the scan.
 * <p>
 * Note that, to avoid schema conflicts, a late schema reader
 * <i><b>must</b></i> define the full set of columns in the first batch,
 * and must stick to that schema for all subsequent batches. This allows
 * the reader to look one batch ahead to learn the columns.
 * <p>
 * Drill, however, cannot predict the future. Without a defined schema,
 * downstream operators cannot know which columns might appear later
 * in the scan, with which types. Today this is a strong guideline.
 * Future versions may enforce this rule.
 */
public interface SchemaNegotiator {

  OperatorContext context();

  /**
   * The context to use as a parent when creating a custom context.
   * <p>
   * (Obtain the error context for this reader from the
   * {@link ResultSetLoader}.
   */
  CustomErrorContext parentErrorContext();

  /**
   * Specify an advanced error context which allows the reader to
   * fill in custom context values.
   */
  void setErrorContext(CustomErrorContext context);

  /**
   * Returns the error context to use for this reader: either the
   * parent or the reader-specific context set in
   * {@link #setErrorContext(CustomErrorContext)}.
   */
  CustomErrorContext errorContext();

  /**
   * Name of the user running the query.
   */
  String userName();

  /**
   * Report whether the projection list is empty, as occurs in two
   * cases:
   * <ul>
   * <li><tt>SELECT COUNT(*) ...</tt> -- empty project.</ul>
   * <li><tt>SELECT a, b FROM table(c d)</tt> -- disjoint project.</li>
   * </ul>
   * @return true if no columns are projected, and the client can
   * make use of {@link ResultSetLoader#skipRows(int)} to indicate the
   * row count, false if at least one column is projected and so
   * data must be written using the loader
   */
  boolean isProjectionEmpty();

  ProjectedColumn projectionFor(String colName);

  /**
   * Returns the provided schema, if defined. The provided schema is a
   * description of the source schema viewed as a Drill schema.
   * <p>
   * If a schema is provided,
   * the reader should use that schema, converting or ignoring columns
   * as needed. A scan without a provided schema has a "dynamic" schema
   * to be defined by the scan operator itself along with the column
   * projection list.
   * <p>
   * The provided schema describes the columns that the reader is
   * capable of providing. Any given query may choose to project a subset
   * of these columns.
   *
   * @return the provided schema if the execution plan defines the output
   * schema, {@code null} if the schema should be computed dynamically
   * from the source schema and column projections
   */
  TupleMetadata providedSchema();

  /**
   * Returns the reader input schema: the schema which describes the
   * set of columns this reader should produce. The schema can be
   * fully dynamic (a wildcard), fully concrete (a list of columns
   * and their types) or partially dynamic (a list of columns, but some
   * types are not known.) If the reader has a choice of ways to map
   * input columns to types, the reader must choose the type specified
   * in the schema. Presumably, the specified types are those that the
   * reader is capable of providing; there is no expectation that the
   * reader will convert to arbitrary types.
   * <p>
   * If the type is {@code LATE} (dynamic), then the reader can choose
   * the type. Subsequent readers in the same scan will then see the
   * same column as concrete, with the type selected by the present
   * reader.
   * <p>
   * The reader is free to add additional columns. The internal mechanism
   * will just ignore those columns and return a dummy reader for them.
   * <p>
   * The reader is also free to ignore columns. In this case, the internal
   * mechanism will fill in {@code NULL} or default values.
   */
  TupleMetadata inputSchema();

  /**
   * Specify the table schema if this is an early-schema reader. Need
   * not be called for a late-schema readers. The schema provided here,
   * if any, is a base schema: the reader is free to discover additional
   * columns during the read.
   *
   * @param schema the table schema if known at open time
   * @param isComplete true if the schema is complete: if it can be used
   * to define an empty schema-only batch for the first reader. Set to
   * false if the schema is partial: if the reader must read rows to
   * determine the full schema
   */
  void tableSchema(TupleMetadata schema, boolean isComplete);
  void tableSchema(TupleMetadata schema);
  void schemaIsComplete(boolean isComplete);

  /**
   * Set the preferred batch size (which may be overridden by the
   * result set loader in order to limit vector or batch size.)
   *
   * @param maxRecordsPerBatch preferred number of record per batch
   */
  void batchSize(int maxRecordsPerBatch);

  /**
   * Build the schema, plan the required projections and static
   * columns and return a loader used to populate value vectors.
   * If the select list includes a subset of table columns, then
   * the loader will be set up in table schema order, but the unneeded
   * column loaders will be null, meaning that the batch reader should
   * skip setting those columns.
   *
   * @return the loader for the table with columns arranged in table
   * schema order
   */
  ResultSetLoader build();
}
