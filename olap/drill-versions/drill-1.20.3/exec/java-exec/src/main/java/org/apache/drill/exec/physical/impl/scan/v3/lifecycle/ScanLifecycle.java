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
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ReaderFactory;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaConfigBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ResultVectorCacheImpl;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
/**
 * Basic scan framework for a set of "managed" readers and which uses the
 * scan schema tracker to evolve the scan output schema.
 * Readers are created and managed via a reader
 * factory class unique to each type of scan. The reader factory also provides
 * the scan-specific schema negotiator to be passed to the reader.
 *
 * <h4>Lifecycle</h4>
 *
 * The options provided in the {@link ScanLifecycleBuilder} are
 * sufficient to drive the entire scan operator functionality.
 * Schema resolution and projection is done generically and is the same for all
 * data sources. Only the
 * reader (created via the factory class) differs from one type of file to
 * another.
 * <p>
 * The framework achieves the work described below by composing a
 * set of detailed classes, each of which performs some specific task. This
 * structure leaves the reader to simply infer schema and read data.
 *
 * <h4>Reader Integration</h4>
 *
 * The details of how a file is structured, how a schema is inferred, how
 * data is decoded: all that is encapsulated in the reader. The only real
 * Interaction between the reader and the framework is:
 * <ul>
 * <li>The reader factory creates a reader and the corresponding schema
 * negotiator.</li>
 * <li>The reader "negotiates" a schema with the framework. The framework
 * knows the projection list from the query plan, knows something about
 * data types (whether a column should be scalar, a map or an array), and
 * knows about the schema already defined by prior readers. The reader knows
 * what schema it can produce (if "early schema.") The schema negotiator
 * class handles this task.</li>
 * <li>The reader reads data from the file and populates value vectors a
 * batch at a time. The framework creates the result set loader to use for
 * this work. The schema negotiator returns that loader to the reader, which
 * uses it during read.<p>
 * A reader may be "late schema", true "schema on read." In this case, the
 * reader simply tells the result set loader to create a new column reader
 * on the fly. The framework will work out if that new column is to be
 * projected and will return either a real column writer (projected column)
 * or a dummy column writer (unprojected column.)</li>
 * <li>The reader then reads batches of data until all data is read. The
 * result set loader signals when a batch is full; the reader should not
 * worry about this detail itself.</li>
 * <li>The reader then releases its resources.</li>
 * </ul>
 * <p>
 * See {@link ScanSchemaTracker} for details about how the scan schema
 * evolves over the scan lifecycle.
 *
 * <h4>Lifecycle</h4>
 *
 * Coordinates the components that make up a scan implementation:
 * <ul>
 * <li>{@link ScanSchemaTracker} which resolves the scan schema over the
 * lifetime of the scan.</li>
 * <li>Implicit columns manager which identifies and populates implicit
 * file columns, partition columns, and Drill's internal metadata
 * columns.</li>
 * <li>The actual readers which load (possibly a subset of) the
 * columns requested from the input source.</li>
 * </ul>
 * <p>
 * Implicit columns are unique to each storage plugin. At present, they
 * are defined only for the file system plugin. To handle such variation,
 * each extension defines a subclass of the {@link ScanLifecycleBuilder} class to
 * create the implicit columns manager (and schema negotiator) unique to
 * a certain kind of scan.
 * <p>
 * Each reader is tracked by a {@link ReaderLifecycle} which handles:
 * <ul>
 * <li>Setting up the {@link ResultSetLoader} for the reader.</li>
 * <li>The concrete values for implicit columns for that reader
 * (and its file, if file-based.)</li>
 * <li>The missing columns handler which "makes up" values for projected
 * columns not read by the reader.</li>
 * <li>Batch asssembler, which combines the three sources of vectors
 * to create the output batch with the schema specified by the
 * schema tracker.</li>
 * </ul>
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
 * @see ScanSchemaTracker for a description of the schema lifecycle
 * which drives a scan
 */
public class ScanLifecycle {

  private final OperatorContext context;
  private final ScanLifecycleBuilder options;
  private final ScanSchemaTracker schemaTracker;
  private final ReaderFactory<?> readerFactory;
  private int batchCount;
  private long rowCount;

  /**
   * Cache used to preserve the same vectors from one output batch to the
   * next to keep the Project operator happy (which depends on exactly the
   * same vectors.
   * <p>
   * If the Project operator ever changes so that it depends on looking up
   * vectors rather than vector instances, this cache can be deprecated.
   */
  private final ResultVectorCacheImpl vectorCache;

  public ScanLifecycle(OperatorContext context, ScanLifecycleBuilder builder) {
    this.context = context;
    this.options = builder;
    this.schemaTracker = new ScanSchemaConfigBuilder()
        .projection(builder.projection())
        .definedSchema(builder.definedSchema())
        .providedSchema(builder.providedSchema())
        .allowSchemaChange(builder.allowSchemaChange())
        .build();
    if (builder.schemaValidator() != null) {
      builder.schemaValidator().validate(schemaTracker);
    }
    this.vectorCache = new ResultVectorCacheImpl(allocator(), false);
    this.readerFactory = builder.readerFactory();
  }

  public OperatorContext context() { return context; }
  public ScanLifecycleBuilder options() { return options; }
  public ScanSchemaTracker schemaTracker() { return schemaTracker; }
  public ResultVectorCacheImpl vectorCache() { return vectorCache; }
  public ReaderFactory<?> readerFactory() { return readerFactory; }
  public boolean hasOutputSchema() { return schemaTracker.isResolved(); }
  public CustomErrorContext errorContext() { return options.errorContext(); }
  public BufferAllocator allocator() { return context.getAllocator(); }
  public int batchCount() { return batchCount; }
  public long rowCount() { return rowCount; }

  public void tallyBatch(int rowCount) {
    batchCount++;
    this.rowCount += rowCount;
  }

  public RowBatchReader nextReader() {
    // Check limit. But, do at least one (zero row) batch
    // to capture schema.
    if (batchCount > 0 && rowCount >= options.limit()) {
      return null;
    }
    if (!readerFactory.hasNext()) {
      return null;
    }
    return new ReaderLifecycle(this, options.limit() - rowCount);
  }

  protected SchemaNegotiatorImpl newNegotiator(ReaderLifecycle readerLifecycle) {
    return new SchemaNegotiatorImpl(readerLifecycle);
  }

  public TupleMetadata outputSchema() {
    return schemaTracker.outputSchema();
  }

  public void close() { }
}
