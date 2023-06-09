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
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.impl.protocol.OperatorDriver;
import org.apache.drill.exec.physical.impl.protocol.OperatorRecordBatch;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorEvents;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.impl.scan.project.ReaderLevelProjection.ReaderProjectionResolver;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection.ScanProjectionParser;
import org.apache.drill.exec.physical.resultSet.impl.ResultVectorCacheImpl;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Performs projection of a record reader, along with a set of static
 * columns, to produce the final "public" result set (record batch)
 * for the scan operator. Primarily solves the "vector permanence"
 * problem: that the scan operator must present the same set of vectors
 * to downstream operators despite the fact that the scan operator hosts
 * a series of readers, each of which builds its own result set.
 * <p>
 * Provides the option to continue a schema from one batch to the next.
 * This can reduce spurious schema changes in formats, such as JSON, with
 * varying fields. It is not, however, a complete solution as the outcome
 * still depends on the order of file scans and the division of files across
 * readers.
 * <p>
 * Provides the option to infer the schema from the first batch. The "quick path"
 * to obtain the schema will read one batch, then use that schema as the returned
 * schema, returning the full batch in the next call to <tt>next()</tt>.
 *
 * <h4>Publishing the Final Result Set<h4>
 *
 * This class "publishes" a vector container that has the final, projected
 * form of a scan. The projected schema include:
 * <ul>
 * <li>Columns from the reader.</li>
 * <li>Static columns, such as implicit or partition columns.</li>
 * <li>Null columns for items in the select list, but not found in either
 * of the above two categories.</li>
 * </ul>
 * The order of columns is that set by the select list (or, by the reader for
 * a <tt>SELECT *</tt> query.
 *
 * <h4>Schema Handling</h4>
 *
 * The mapping handles a variety of cases:
 * <ul>
 * <li>An early-schema table (one in which we know the schema and
 * the schema remains constant for the whole table.</li>
 * <li>A late schema table (one in which we discover the schema as
 * we read the table, and where the schema can change as the read
 * progresses.)<ul>
 * <li>Late schema table with SELECT * (we want all columns, whatever
 * they happen to be.)</li>
 * <li>Late schema with explicit select list (we want only certain
 * columns when they happen to appear in the input.)</li></ul></li>
 * </ul>
 *
 * <h4>Implementation Overview</h4>
 *
 * Major tasks of this class include:
 * <ul>
 * <li>Project table columns (change position and or name).</li>
 * <li>Insert static and null columns.</li>
 * <li>Schema smoothing. That is, if table A produces columns (a, b), but
 * table B produces only (a), use the type of the first table's b column for the
 * null created for the missing b in table B.</li>
 * <li>Vector persistence: use the same set of vectors across readers as long
 * as the reader schema does not cause a "hard" schema change (change in type,
 * introduction of a new column.</li>
 * <li>Detection of schema changes (change of type, introduction of a new column
 * for a <tt>SELECT *</tt> query, changing the projected schema, and reporting
 * the change downstream.</li>
 * </ul>
 * A projection is needed to:
 * <ul>
 * <li>Reorder table columns</li>
 * <li>Select a subset of table columns</li>
 * <li>Fill in missing select columns</li>
 * <li>Fill in implicit or partition columns</li>
 * </ul>
 * Creates and returns the batch merger that does the projection.
 *
 * <h4>Projection</h4>
 *
 * To visualize this, assume we have numbered table columns, lettered
 * implicit, null or partition columns:<pre><code>
 * [ 1 | 2 | 3 | 4 ]    Table columns in table order
 * [ A | B | C ]        Static columns
 * </code></pre>
 * Now, we wish to project them into select order.
 * Let's say that the SELECT clause looked like this, with "t"
 * indicating table columns:<pre><code>
 * SELECT t2, t3, C, B, t1, A, t2 ...
 * </code></pre>
 * Then the projection looks like this:<pre><code>
 * [ 2 | 3 | C | B | 1 | A | 2 ]
 * </code></pre>
 * Often, not all table columns are projected. In this case, the
 * result set loader presents the full table schema to the reader,
 * but actually writes only the projected columns. Suppose we
 * have:<pre><code>
 * SELECT t3, C, B, t1,, A ...
 * </code></pre>
 * Then the abbreviated table schema looks like this:<pre><code>
 * [ 1 | 3 ]</code></pre>
 * Note that table columns retain their table ordering.
 * The projection looks like this:<pre><code>
 * [ 2 | C | B | 1 | A ]
 * </code></pre>
 * <p>
 * The projector is created once per schema, then can be reused for any
 * number of batches.
 * <p>
 * Merging is done in one of two ways, depending on the input source:
 * <ul>
 * <li>For the table loader, the merger discards any data in the output,
 * then exchanges the buffers from the input columns to the output,
 * leaving projected columns empty. Note that unprojected columns must
 * be cleared by the caller.</li>
 * <li>For implicit and null columns, the output vector is identical
 * to the input vector.</li>
 */
public class ScanSchemaOrchestrator {

  public static final int MIN_BATCH_BYTE_SIZE = 256 * 1024;
  public static final int MAX_BATCH_BYTE_SIZE = Integer.MAX_VALUE;
  public static final int DEFAULT_BATCH_ROW_COUNT = 4096;
  public static final int DEFAULT_BATCH_BYTE_COUNT = ValueVector.MAX_BUFFER_SIZE;
  public static final int MAX_BATCH_ROW_COUNT = ValueVector.MAX_ROW_COUNT;

  public abstract static class ScanOrchestratorBuilder {

    private MajorType nullType;
    private MetadataManager metadataManager;
    private int scanBatchRecordLimit = DEFAULT_BATCH_ROW_COUNT;
    private int scanBatchByteLimit = DEFAULT_BATCH_BYTE_COUNT;
    private final List<ScanProjectionParser> parsers = new ArrayList<>();
    private final List<ReaderProjectionResolver> schemaResolvers = new ArrayList<>();
    private boolean useSchemaSmoothing;
    private boolean allowRequiredNullColumns;
    private List<SchemaPath> projection;
    private TupleMetadata providedSchema;

    /**
     * Option that enables whether the scan operator starts with an empty
     * schema-only batch (the so-called "fast schema" that Drill once tried
     * to provide) or starts with a non-empty data batch (which appears to
     * be the standard since the "Empty Batches" project some time back.)
     * See more details in {@link OperatorDriver} Javadoc.
     * <p>
     * Defaults to <tt>false</tt>, meaning to <i>not</i> provide the empty
     * schema batch. DRILL-7305 explains that many operators fail when
     * presented with an empty batch, so do not enable this feature until
     * those issues are fixed. Of course, do enable the feature if you want
     * to track down the DRILL-7305 bugs.
     */
    private boolean enableSchemaBatch;

    /**
     * Option to disable empty results. An empty result occurs if no
     * reader has any data, but at least one reader can provide a schema.
     * In this case, the scan can return a single, empty batch, with
     * an associated schema. This is the correct SQL result for an
     * empty query. However, if this result triggers empty-batch bugs
     * in other operators, we can, instead, disable this feature and
     * return a null result set: no schema, no batch, just a "fast NONE",
     * an immediate return of NONE from the Volcano iterator.
     * <p>
     * Disabling this option is not desirable: it means that the user
     * gets no schema for queries that should be able to return one. So,
     * disable this option only if we cannot find or fix empty-batch
     * bugs.
     */
    public boolean disableEmptyResults;

    /**
     * Context for error messages.
     */
    private CustomErrorContext errorContext;

    /**
     * Pushed-down scan LIMIT.
     */
    private long limit = -1;

    /**
     * Specify an optional metadata manager. Metadata is a set of constant
     * columns with per-reader values. For file-based sources, this is usually
     * the implicit and partition columns; but it could be other items for other
     * data sources.
     *
     * @param metadataMgr the application-specific metadata manager to use
     * for this scan
     */
    public void withImplicitColumns(MetadataManager metadataMgr) {
      metadataManager = metadataMgr;
      schemaResolvers.add(metadataManager.resolver());
    }

    /**
     * Specify a custom batch record count. This is the maximum number of records
     * per batch for this scan. Readers can adjust this, but the adjustment is capped
     * at the value specified here
     *
     * @param batchRecordLimit maximum records per batch
     */
    public void batchRecordLimit(int batchRecordLimit) {
      scanBatchRecordLimit = Math.max(1,
          Math.min(batchRecordLimit, ValueVector.MAX_ROW_COUNT));
    }

    public void batchByteLimit(int byteLimit) {
      scanBatchByteLimit = Math.max(MIN_BATCH_BYTE_SIZE,
          Math.min(byteLimit, MAX_BATCH_BYTE_SIZE));
    }

    /**
     * Specify the type to use for null columns in place of the standard
     * nullable int. This type is used for all missing columns. (Readers
     * that need per-column control need a different mechanism.)
     *
     * @param nullType the type to use for null columns
     */
    public void nullType(MajorType nullType) {
      this.nullType = nullType;
    }

    /**
     * Enable schema smoothing: introduces an addition level of schema
     * resolution each time a schema changes from a reader.
     *
     * @param flag true to enable schema smoothing, false to disable
     */
    public void enableSchemaSmoothing(boolean flag) {
      useSchemaSmoothing = flag;
   }

    public void allowRequiredNullColumns(boolean flag) {
      allowRequiredNullColumns = flag;
    }

    public void addParser(ScanProjectionParser parser) {
      parsers.add(parser);
    }

    public void addResolver(ReaderProjectionResolver resolver) {
      schemaResolvers.add(resolver);
    }

    public void projection(List<SchemaPath> projection) {
      this.projection = projection;
    }

    public void enableSchemaBatch(boolean option) {
      enableSchemaBatch = option;
    }

    public void disableEmptyResults(boolean option) {
      disableEmptyResults = option;
    }

    public void providedSchema(TupleMetadata providedSchema) {
      this.providedSchema = providedSchema;
    }

    public TupleMetadata providedSchema() {
      return providedSchema;
    }

    public void limit(long limit) {
      this.limit = Math.max(-1, limit);
    }

    public long limit() { return limit; }

    public void errorContext(CustomErrorContext context) {
      this.errorContext = context;
    }

    public CustomErrorContext errorContext() {
      return errorContext;
    }

    @VisibleForTesting
    public ScanOperatorExec buildScan() {
      return new ScanOperatorExec(buildEvents(), !disableEmptyResults);
    }

    public OperatorRecordBatch buildScanOperator(FragmentContext fragContext, PhysicalOperator pop) {
      return new OperatorRecordBatch(fragContext, pop, buildScan(), enableSchemaBatch);
    }

    public abstract ScanOperatorEvents buildEvents();
  }

  public static class ScanSchemaOptions {

    /**
     * Custom null type, if provided by the operator. If
     * not set, the null type is the Drill default.
     */
    public final MajorType nullType;
    public final int scanBatchRecordLimit;
    public final int scanBatchByteLimit;
    public final List<ScanProjectionParser> parsers;

    /**
     * List of resolvers used to resolve projection columns for each
     * new schema. Allows operators to introduce custom functionality
     * as a plug-in rather than by copying code or subclassing this
     * mechanism.
     */
    public final List<ReaderProjectionResolver> schemaResolvers;

    public final List<SchemaPath> projection;
    public final boolean useSchemaSmoothing;
    public final boolean allowRequiredNullColumns;
    public final TupleMetadata providedSchema;
    public final long limit;

    /**
     * Context for error messages.
     */
    public final CustomErrorContext context;

    protected ScanSchemaOptions(ScanOrchestratorBuilder builder) {
      nullType = builder.nullType;
      scanBatchRecordLimit = builder.scanBatchRecordLimit;
      scanBatchByteLimit = builder.scanBatchByteLimit;
      parsers = builder.parsers;
      schemaResolvers = builder.schemaResolvers;
      projection = builder.projection;
      useSchemaSmoothing = builder.useSchemaSmoothing;
      context = builder.errorContext;
      providedSchema = builder.providedSchema;
      allowRequiredNullColumns = builder.allowRequiredNullColumns;
      limit = builder.limit < 0 ? Long.MAX_VALUE : builder.limit;
    }

    protected TupleMetadata providedSchema() {
      return providedSchema;
    }
  }

  // Configuration

  protected final BufferAllocator allocator;
  protected final ScanSchemaOptions options;

  /**
   * Creates the metadata (file and directory) columns, if needed.
   */
  public final MetadataManager metadataManager;

  // Internal state

  /**
   * Cache used to preserve the same vectors from one output batch to the
   * next to keep the Project operator happy (which depends on exactly the
   * same vectors.
   * <p>
   * If the Project operator ever changes so that it depends on looking up
   * vectors rather than vector instances, this cache can be deprecated.
   */
  protected final ResultVectorCacheImpl vectorCache;
  protected final ScanLevelProjection scanProj;
  private ReaderSchemaOrchestrator currentReader;
  protected final SchemaSmoother schemaSmoother;
  protected int batchCount;
  protected long rowCount;

  // Output

  protected VectorContainer outputContainer;

  public ScanSchemaOrchestrator(BufferAllocator allocator, ScanOrchestratorBuilder builder) {
    this.allocator = allocator;
    this.options = new ScanSchemaOptions(builder);

    vectorCache = new ResultVectorCacheImpl(allocator, options.useSchemaSmoothing);

    // If no metadata manager was provided, create a mock
    // version just to keep code simple.
    if (builder.metadataManager == null) {
      metadataManager = new NoOpMetadataManager();
    } else {
      metadataManager = builder.metadataManager;
    }
    metadataManager.bind(vectorCache);

    // Bind metadata manager parser to scan projector.
    // A "real" (non-mock) metadata manager will provide
    // a projection parser. Use this to tell us that this
    // setup supports metadata.

    ScanProjectionParser parser = metadataManager.projectionParser();
    if (parser != null) {

      // Insert in last position to expand wildcards at
      // the end of the tuple.
      options.parsers.add(parser);
    }

    // Parse the projection list.
    scanProj = ScanLevelProjection.builder()
        .projection(options.projection)
        .parsers(options.parsers)
        .providedSchema(options.providedSchema())
        .errorContext(builder.errorContext())
        .build();
    if (scanProj.projectAll() && options.useSchemaSmoothing) {
      schemaSmoother = new SchemaSmoother(scanProj, options.schemaResolvers);
    } else {
      schemaSmoother = null;
    }

    // Build the output container.
    outputContainer = new VectorContainer(allocator);
  }

  public ReaderSchemaOrchestrator startReader() {
    closeReader();
    currentReader = new ReaderSchemaOrchestrator(this, options.limit - rowCount);
    return currentReader;
  }

  public boolean isProjectNone() {
    return scanProj.isEmptyProjection();
  }

  public boolean hasSchema() {
    return currentReader != null && currentReader.hasSchema();
  }

  /**
   * Returns the provided reader schema.
   */
  public TupleMetadata providedSchema() {
    return options.providedSchema();
  }

  public VectorContainer output() {
    return outputContainer;
  }

  public void tallyBatch(int rowCount) {
    this.batchCount++;
    this.rowCount += rowCount;
  }

  public boolean atLimit() {
    return batchCount > 0 && rowCount >= options.limit;
  }

  public void closeReader() {
    if (currentReader != null) {
      currentReader.close();
      currentReader = null;
    }
  }

  public void close() {
    closeReader();
    if (outputContainer != null) {
      outputContainer.clear();
      outputContainer = null;
    }
    vectorCache.close();
    metadataManager.close();
  }
}
