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

import java.util.Collections;
import java.util.List;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.impl.protocol.OperatorDriver;
import org.apache.drill.exec.physical.impl.protocol.OperatorRecordBatch;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.ScanEventListener;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.ScanLifecycle;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Gathers options for the {@link ScanLifecycle} then builds a scan lifecycle
 * instance.
 * <p>
 * This framework is a bridge between operator logic and the scan
 * internals. It gathers scan-specific options in a builder abstraction, then
 * passes them on the scan lifecycle at the right time. By abstracting out this
 * plumbing, a scan batch creator simply chooses the proper framework builder, passes
 * config options, and implements the matching "managed reader" and factory. All details
 * of setup, projection, and so on are handled by the framework and the components
 * that the framework builds upon.
 *
 * <h4>Inputs</h4>
 *
 * At this basic level, a scan framework requires just a few simple inputs:
 * <ul>
 * <li>The options defined by the scan projection framework such as the
 * projection list.</li>
 * <li>A reader factory to create a reader for each of the files or blocks
 * to be scanned. (Readers are expected to be created one-by-one as files
 * are read.)</li>
 * <li>The operator context which provides access to a memory allocator and
 * other plumbing items.</li>
 * </ul>
 * <p>
 * In practice, there are other options to fine tune behavior (provided schema,
 * custom error context, various limits, etc.)
 */
public class ScanLifecycleBuilder {

  public static final int MIN_BATCH_BYTE_SIZE = 256 * 1024;
  public static final int MAX_BATCH_BYTE_SIZE = Integer.MAX_VALUE;
  public static final int DEFAULT_BATCH_ROW_COUNT = 4096;
  public static final int DEFAULT_BATCH_BYTE_COUNT = ValueVector.MAX_BUFFER_SIZE;
  public static final int MAX_BATCH_ROW_COUNT = ValueVector.MAX_ROW_COUNT;

  public static class DummyReaderFactory implements ReaderFactory<SchemaNegotiator> {

    @Override
    public boolean hasNext() { return false; }

    @Override
    public ManagedReader next(SchemaNegotiator negotiator) {
      return null;
    }
  }

  public interface SchemaValidator {
    void validate(ScanSchemaTracker schema);
  }

  private OptionSet options;
  private ReaderFactory<?> readerFactory;
  protected String userName;
  protected MajorType nullType;
  private int scanBatchRecordLimit = DEFAULT_BATCH_ROW_COUNT;
  private int scanBatchByteLimit = DEFAULT_BATCH_BYTE_COUNT;
  protected boolean allowRequiredNullColumns;
  private List<SchemaPath> projection;
  protected TupleMetadata definedSchema;
  protected TupleMetadata providedSchema;

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
  protected boolean enableSchemaBatch;

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
  protected boolean disableEmptyResults;

  /**
   * Option to disable schema changes. If {@code false}, then the first
   * batch commits the scan to a single, unchanged schema. If {@code true}
   * (the legacy default), then each batch or reader can change the schema,
   * even though downstream operators generally cannot handle a schema
   * change. The goal is to evolve all readers so that they do not
   * generate schema changes.
   */
  protected boolean allowSchemaChange = true;

  /**
   * Optional schema validator to perform per-scan checks of the
   * projection or resolved schema.
   */
  protected SchemaValidator schemaValidator;

  /**
   * Context for error messages.
   */
  protected CustomErrorContext errorContext;

  /**
   * Pushed-down scan LIMIT.
   */
  private long limit = Long.MAX_VALUE;

  public void options(OptionSet options) {
    this.options = options;
  }

  public OptionSet options() {
    return options;
  }

  public void readerFactory(ReaderFactory<?> readerFactory) {
    this.readerFactory = readerFactory;
  }

  public void userName(String userName) {
    this.userName = userName;
  }

  public String userName() {
    return userName;
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

  public void allowRequiredNullColumns(boolean flag) {
    allowRequiredNullColumns = flag;
  }

  public boolean allowRequiredNullColumns() {
    return allowRequiredNullColumns;
  }

  public void allowSchemaChange(boolean flag) {
    allowSchemaChange = flag;
  }

  public boolean allowSchemaChange() {
    return allowSchemaChange;
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

  public void definedSchema(TupleMetadata definedSchema) {
    this.definedSchema = definedSchema;
  }

  public TupleMetadata definedSchema() {
    return definedSchema;
  }

  public void providedSchema(TupleMetadata providedSchema) {
    this.providedSchema = providedSchema;
  }

  public TupleMetadata providedSchema() {
    return providedSchema;
  }

  public void errorContext(CustomErrorContext context) {
    this.errorContext = context;
  }

  public CustomErrorContext errorContext() {
    if (errorContext == null) {
      errorContext = new EmptyErrorContext();
    }
    return errorContext;
  }

  public List<SchemaPath> projection() {
    if (projection == null) {
      projection = Collections.singletonList(SchemaPath.STAR_COLUMN);
    }
    return projection;
  }

  public int scanBatchRecordLimit() {
    return Math.max(1, Math.min(scanBatchRecordLimit, MAX_BATCH_ROW_COUNT));
  }

  public int scanBatchByteLimit() {
    return Math.max(MIN_BATCH_BYTE_SIZE, Math.min(scanBatchByteLimit, MAX_BATCH_BYTE_SIZE));
  }

  public MajorType nullType() {
    return nullType;
  }

  public ReaderFactory<?> readerFactory() {
    if (readerFactory == null) {
      readerFactory = new DummyReaderFactory();
    }
    return readerFactory;
  }

  public void schemaValidator(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  public SchemaValidator schemaValidator() { return schemaValidator; }

  public void limit(long limit) {
    // Operator definitions use -1 for "no limit", this mechanism
    // uses a very big number for "no limit."
    if (limit < 0) {
      this.limit = Long.MAX_VALUE;
    } else {
      this.limit = limit;
    }
  }

  public long limit() { return limit; }

  public ScanLifecycle build(OperatorContext context) {
    return new ScanLifecycle(context, this);
  }

  @VisibleForTesting
  public ScanOperatorExec buildScan() {
    return new ScanOperatorExec(
        new ScanEventListener(this),
             !disableEmptyResults);
  }

  public OperatorRecordBatch buildScanOperator(FragmentContext fragContext, PhysicalOperator pop) {
    return new OperatorRecordBatch(fragContext, pop, buildScan(), enableSchemaBatch);
  }
}
