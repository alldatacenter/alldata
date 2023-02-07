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
package org.apache.drill.exec.physical.impl.scan.v3.lifecycle;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ReaderFactory;
import org.apache.drill.exec.physical.impl.scan.v3.SchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ProjectedColumn;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Implementation of the schema negotiation between scan operator and
 * batch reader. Anticipates that the select list (and/or the list of
 * predefined fields (implicit, partition) might be set by the scanner.
 * For now, all readers have their own implementation of the select
 * set.
 * <p>
 * Handles both early- and late-schema readers. Early-schema readers
 * provide a table schema, late-schema readers do not.
 * <p>
 * If the reader (or, later, the scanner) has a SELECT list, then that
 * select list is pushed down into the result set loader created for
 * the reader.
 * <p>
 * Also handles parsing out various column types, filling in null
 * columns and (via the vector cache), minimizing changes across
 * readers. In the worst case, a reader might have a column "c" in
 * one file, might skip "c" in the second file, and "c" may appear again
 * in a third file. This negotiator, along with the scan projection
 * and vector cache, "smoothes out" schema changes by preserving the vector
 * for "c" across all three files. In the first and third files "c" is
 * a vector written by the reader, in the second, it is a null column
 * filled in by the scan projector (assuming, of course, that "c"
 * is nullable or an array.)
 * <p>
 * Pushes limits into the result set loader. The caller must
 * either check the return value from {#code startBatch()}, or
 * call {@code atLimit()} after {@code harvest()} to detect when the scan
 * has reached the limit. Treat the limit condition the same as EOF.
 */
public class SchemaNegotiatorImpl implements SchemaNegotiator {

  protected final ReaderLifecycle readerLifecycle;
  protected TupleMetadata readerSchema;
  protected boolean isSchemaComplete;
  protected int batchSize = ValueVector.MAX_ROW_COUNT;
  protected CustomErrorContext baseErrorContext;
  protected CustomErrorContext readerErrorContext;

  public SchemaNegotiatorImpl(ReaderLifecycle readerLifecycle) {
    this.readerLifecycle = readerLifecycle;
    this.baseErrorContext = readerLifecycle.scanLifecycle().errorContext();
  }

  @Override
  public boolean isProjectionEmpty() {
    return readerLifecycle.schemaTracker().projectionType() == ProjectionType.NONE;
  }

  @Override
  public ProjectedColumn projectionFor(String colName) {
    return readerLifecycle.scanLifecycle().schemaTracker().columnProjection(colName);
  }

  @Override
  public TupleMetadata providedSchema() {
    return readerLifecycle.scanOptions().providedSchema();
  }

  @Override
  public TupleMetadata inputSchema() {
    return readerLifecycle.readerInputSchema();
  }

  @Override
  public OperatorContext context() {
    return readerLifecycle.scanLifecycle().context();
  }

  @Override
  public CustomErrorContext parentErrorContext() {
    return baseErrorContext;
  }

  @Override
  public CustomErrorContext errorContext() {
    return readerErrorContext == null ? baseErrorContext : readerErrorContext;
  }

  @Override
  public void setErrorContext(CustomErrorContext errorContext) {
    this.readerErrorContext = errorContext;
  }

  @Override
  public void tableSchema(TupleMetadata schema, boolean isComplete) {
    readerSchema = schema;
    isSchemaComplete = schema != null && isComplete;
  }

  @Override
  public void tableSchema(TupleMetadata schema) {
    readerSchema = schema;
  }

  @Override
  public void schemaIsComplete(boolean isComplete) {
    isSchemaComplete = isComplete;
  }

  public boolean isSchemaComplete() { return readerSchema != null && isSchemaComplete; }

  @Override
  public void batchSize(int maxRecordsPerBatch) {
    batchSize = maxRecordsPerBatch;
  }

  @Override
  public String userName() {
    return readerLifecycle.scanOptions().userName();
  }

  /**
   * Callback from the schema negotiator to build the schema from information from
   * both the table and scan operator. Returns the result set loader to be used
   * by the reader to write to the table's value vectors.
   *
   * @return the result set loader to be used by the reader
   */
  @Override
  public ResultSetLoader build() {
    return readerLifecycle.buildLoader();
  }

  public StaticBatchBuilder implicitColumnsLoader() {
    return null;
  }

  @SuppressWarnings("unchecked")
  public ManagedReader newReader(ReaderFactory<?> readerFactory) throws EarlyEofException {
    return ((ReaderFactory<SchemaNegotiator>) readerFactory).next(this);
  }

  protected void onEndBatch() { }
}
