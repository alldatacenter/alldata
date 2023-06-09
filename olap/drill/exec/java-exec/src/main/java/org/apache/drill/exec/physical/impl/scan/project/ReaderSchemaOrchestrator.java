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

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.impl.scan.file.FileMetadataColumn;
import org.apache.drill.exec.physical.impl.scan.file.FileMetadataColumnDefn;
import org.apache.drill.exec.physical.impl.scan.project.NullColumnBuilder.NullBuilderBuilder;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple.ResolvedRow;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetOptionBuilder;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitInternalFileColumns;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

import java.util.List;

/**
 * Orchestrates projection tasks for a single reader within the set that the
 * scan operator manages. Vectors are reused across readers, but via a vector
 * cache. All other state is distinct between readers.
 */
public class ReaderSchemaOrchestrator implements VectorSource {

  private final ScanSchemaOrchestrator scanOrchestrator;
  private long limit;
  private int readerBatchSize;
  private ResultSetLoaderImpl tableLoader;
  private int prevTableSchemaVersion = -1;

  /**
   * Assembles the table, metadata and null columns into the final output
   * batch to be sent downstream. The key goal of this class is to "smooth"
   * schema changes in this output batch by absorbing trivial schema changes
   * that occur across readers.
   */
  private ResolvedRow rootTuple;
  private VectorContainer tableContainer;

  public ReaderSchemaOrchestrator(
      ScanSchemaOrchestrator scanSchemaOrchestrator, long limit) {
    scanOrchestrator = scanSchemaOrchestrator;
    this.limit = limit;
    readerBatchSize = scanOrchestrator.options.scanBatchRecordLimit;
  }

  public void setBatchSize(int size) {
    if (size > 0) {
      readerBatchSize = size;
    }
  }

  @VisibleForTesting
  public ResultSetLoader makeTableLoader(TupleMetadata readerSchema) {
    return makeTableLoader(scanOrchestrator.scanProj.context(), readerSchema, -1);
  }

  public ResultSetLoader makeTableLoader(CustomErrorContext errorContext,
      TupleMetadata readerSchema, long localLimit) {
    ResultSetOptionBuilder options = new ResultSetOptionBuilder();
    options.rowCountLimit(Math.min(readerBatchSize, scanOrchestrator.options.scanBatchRecordLimit));
    options.vectorCache(scanOrchestrator.vectorCache);
    options.batchSizeLimit(scanOrchestrator.options.scanBatchByteLimit);
    options.errorContext(errorContext);

    // Set up a selection list if available and is a subset of
    // table columns. (Only needed for non-wildcard queries.)
    // The projection list includes all candidate table columns
    // whether or not they exist in the up-front schema. Handles
    // the odd case where the reader claims a fixed schema, but
    // adds a column later.
    options.projectionFilter(scanOrchestrator.scanProj.readerProjection);
    options.readerSchema(readerSchema);
    if (limit < 0) {
      limit = localLimit;
    } else if (localLimit >= 0) {
      limit = Math.min(localLimit, limit);
    }
    options.limit(limit);

    // Create the table loader
    tableLoader = new ResultSetLoaderImpl(scanOrchestrator.allocator, options.build());
    return tableLoader;
  }

  public boolean hasSchema() {
    return prevTableSchemaVersion >= 0;
  }

  public void defineSchema() {
    tableLoader.startEmptyBatch();
    endBatch();
  }

  public boolean startBatch() {
    return tableLoader.startBatch();
  }

  /**
   * Build the final output batch by projecting columns from the three input sources
   * to the output batch. First, build the metadata and/or null columns for the
   * table row count. Then, merge the sources.
   */
  public void endBatch() {
    endBatch(false);
  }

  /**
   * Build the final output batch by projecting columns from the three input sources
   * to the output batch. First, build the metadata and/or null columns for the
   * table row count. Then, merge the sources.
   *
   * @param eof is end of file
   */
  public boolean endBatch(boolean eof) {

    // Get the batch results in a container.
    tableContainer = tableLoader.harvest();

    boolean projected = resolveProjectingMetadata(eof);

    // If the schema changed, set up the final projection based on
    // the new (or first) schema.
    if (prevTableSchemaVersion < tableLoader.schemaVersion()) {
      reviseOutputProjection();
    } else {

      // Fill in the null and metadata columns.
      populateNonDataColumns();
    }
    if (projected) {
      projectMetadata(false);
    }
    int rowCount = tableContainer.getRecordCount();
    rootTuple.setRowCount(rowCount);
    scanOrchestrator.tallyBatch(rowCount);
    return eof || tableLoader.atLimit();
  }

  /**
   * Updates {@code PROJECT_METADATA} implicit column value to {@code "FALSE"} to handle current batch as
   * a batch with metadata information only for the case when this batch is first and empty.
   */
  private boolean resolveProjectingMetadata(boolean eof) {
    if (tableContainer.getRecordCount() == 0 && !hasSchema() && eof) {
      if (projectMetadata(true)) {
        tableContainer.setValueCount(tableContainer.getRecordCount() + 1);
        return true;
      }
    }
    return false;
  }

  /**
   * Sets {@code PROJECT_METADATA} implicit column value.
   *
   * @param projectMetadata whether {@code PROJECT_METADATA} should be replaced
   * @return {@code true} if {@code PROJECT_METADATA} implicit column
   * is present in scan projection and its value is updated
   */
  private boolean projectMetadata(boolean projectMetadata) {
    ImplicitInternalFileColumns original;
    ImplicitInternalFileColumns newColumn;
    if (projectMetadata) {
      original = ImplicitInternalFileColumns.USE_METADATA;
      newColumn = ImplicitInternalFileColumns.PROJECT_METADATA;
    } else {
      original = ImplicitInternalFileColumns.PROJECT_METADATA;
      newColumn = ImplicitInternalFileColumns.USE_METADATA;
    }
    List<ColumnProjection> outputColumns = scanOrchestrator.scanProj.columns();
    for (int i = 0; i < outputColumns.size(); i++) {
      ColumnProjection outputColumn = outputColumns.get(i);
      if (outputColumn instanceof FileMetadataColumn) {
        FileMetadataColumn metadataColumn = (FileMetadataColumn) outputColumn;
        if (metadataColumn.defn().defn == original) {

          projectMetadata = scanOrchestrator.scanProj.requestedCols().stream()
              .anyMatch(SchemaPath.getSimplePath(metadataColumn.name())::equals);

          if (projectMetadata) {
            outputColumns.set(i,
                new FileMetadataColumn(
                    metadataColumn.name(),
                    new FileMetadataColumnDefn(metadataColumn.defn().colName(), newColumn)));
          }
          return projectMetadata;
        }
      }
    }
    return false;
  }

  private void populateNonDataColumns() {
    int rowCount = tableContainer.getRecordCount();
    scanOrchestrator.metadataManager.load(rowCount);
    rootTuple.loadNulls(rowCount);
  }

  /**
   * Create the list of null columns by comparing the SELECT list against the
   * columns available in the batch schema. Create null columns for those that
   * are missing. This is done for the first batch, and any time the schema
   * changes. (For early-schema, the projection occurs once as the schema is set
   * up-front and does not change.) For a SELECT *, the null column check
   * only need be done if null columns were created when mapping from a prior
   * schema.
   */
  private void reviseOutputProjection() {

    // Do the table-schema level projection; the final matching
    // of projected columns to available columns.

    TupleMetadata readerSchema = tableLoader.outputSchema();
    if (scanOrchestrator.schemaSmoother != null) {
      doSmoothedProjection(readerSchema);
    } else {
      switch(scanOrchestrator.scanProj.projectionType()) {
        case EMPTY:
        case EXPLICIT:
          doExplicitProjection(readerSchema);
          break;
        case SCHEMA_WILDCARD:
        case STRICT_SCHEMA_WILDCARD:
          doStrictWildcardProjection(readerSchema);
          break;
        case WILDCARD:
          doWildcardProjection(readerSchema);
          break;
        default:
          throw new IllegalStateException(scanOrchestrator.scanProj.projectionType().toString());
      }
    }

    // Combine metadata, nulls and batch data to form the final
    // output container. Columns are created by the metadata and null
    // loaders only in response to a batch, so create the first batch.
    rootTuple.buildNulls(scanOrchestrator.vectorCache);
    scanOrchestrator.metadataManager.define();
    populateNonDataColumns();
    rootTuple.project(tableContainer, scanOrchestrator.outputContainer);
    prevTableSchemaVersion = tableLoader.schemaVersion();
  }

  private void doSmoothedProjection(TupleMetadata tableSchema) {
    rootTuple = newRootTuple();
    scanOrchestrator.schemaSmoother.resolve(tableSchema, rootTuple);
  }

  /**
   * Query contains a wildcard. The schema-level projection includes
   * all columns provided by the reader.
   */
  private void doWildcardProjection(TupleMetadata tableSchema) {
    rootTuple = newRootTuple();
    new WildcardProjection(scanOrchestrator.scanProj,
        tableSchema, rootTuple, scanOrchestrator.options.schemaResolvers);
  }

  private void doStrictWildcardProjection(TupleMetadata tableSchema) {
    rootTuple = newRootTuple();
    new WildcardSchemaProjection(scanOrchestrator.scanProj,
        tableSchema, rootTuple, scanOrchestrator.options.schemaResolvers);
  }

  private ResolvedRow newRootTuple() {
    return new ResolvedRow(new NullBuilderBuilder()
        .setNullType(scanOrchestrator.options.nullType)
        .allowRequiredNullColumns(scanOrchestrator.options.allowRequiredNullColumns)
        .setOutputSchema(scanOrchestrator.options.providedSchema())
        .build());
  }

  /**
   * Explicit projection: include only those columns actually
   * requested by the query, which may mean filling in null
   * columns for projected columns that don't actually exist
   * in the table.
   *
   * @param tableSchema newly arrived schema
   */
  private void doExplicitProjection(TupleMetadata tableSchema) {
    rootTuple = newRootTuple();
    new ExplicitSchemaProjection(scanOrchestrator.scanProj,
            tableSchema, rootTuple,
            scanOrchestrator.options.schemaResolvers);
  }

  @Override
  public ValueVector vector(int index) {
    return tableContainer.getValueVector(index).getValueVector();
  }

  public void close() {
    RuntimeException ex = null;
    try {
      if (tableLoader != null) {
        tableLoader.close();
        tableLoader = null;
      }
    }
    catch (RuntimeException e) {
      ex = e;
    }
    try {
      if (rootTuple != null) {
        rootTuple.close();
        rootTuple = null;
      }
    }
    catch (RuntimeException e) {
      ex = ex == null ? e : ex;
    }
    scanOrchestrator.metadataManager.endFile();
    if (ex != null) {
      throw ex;
    }
  }
}
