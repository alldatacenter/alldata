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
package org.apache.drill.exec.physical.impl.scan.framework;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.vector.ValueVector;

import com.typesafe.config.Config;

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
 */
public class SchemaNegotiatorImpl implements SchemaNegotiator {

  public interface NegotiatorListener {
    ResultSetLoader build(SchemaNegotiatorImpl schemaNegotiator);
  }

  protected final ManagedScanFramework framework;
  private NegotiatorListener listener;
  protected CustomErrorContext context;
  protected TupleMetadata providedSchema;
  protected TupleMetadata tableSchema;
  protected boolean isSchemaComplete;
  protected int batchSize = ValueVector.MAX_ROW_COUNT;
  protected long limit = -1;

  public SchemaNegotiatorImpl(ManagedScanFramework framework) {
    this.framework = framework;
    this.providedSchema = framework.outputSchema();
  }

  public void bind(NegotiatorListener listener) {
    this.listener = listener;
  }

  @Override
  public boolean isProjectionEmpty() {
    return framework.scanOrchestrator().isProjectNone();
  }

  @Override
  public boolean hasProvidedSchema() {
    // Does not count as an output schema if no columns
    // (only properties) are provided.
    return providedSchema != null && providedSchema.size() > 0;
  }

  @Override
  public TupleMetadata providedSchema() {
    return providedSchema;
  }

  @Override
  public OperatorContext context() {
    return framework.context();
  }

  @Override
  public Config drillConfig() {
    return context().getFragmentContext().getConfig();
  }

  @Override
  public OptionSet queryOptions() {
    return context().getFragmentContext().getOptions();
  }

  @Override
  public CustomErrorContext parentErrorContext() {
    return framework.errorContext();
  }

  public CustomErrorContext errorContext() {
    return context;
  }

  @Override
  public void setErrorContext(CustomErrorContext context) {
    this.context = context;
  }

  @Override
  public void tableSchema(TupleMetadata schema, boolean isComplete) {
    tableSchema = schema;
    isSchemaComplete = schema != null && isComplete;
  }

  public boolean isSchemaComplete() { return tableSchema != null && isSchemaComplete; }

  @Override
  public void batchSize(int maxRecordsPerBatch) {
    batchSize = maxRecordsPerBatch;
  }

  @Override
  public String userName() {
    return framework.builder.userName;
  }

  @Override
  public void limit(long limit) {
    this.limit = limit;
  }

  /**
   * Callback from the schema negotiator to build the schema from information from
   * both the table and scan operator. Returns the result set loader to be used
   * by the reader to write to the table's value vectors.
   *
   * schema information
   * @return the result set loader to be used by the reader
   */
  @Override
  public ResultSetLoader build() {

    // Build and return the result set loader to be used by the reader.
    return listener.build(this);
  }
}
