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
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorEvents;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator.ScanOrchestratorBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic scan framework for a "managed" reader which uses the scan schema
 * mechanisms encapsulated in the scan schema orchestrator. Handles binding
 * scan events to the scan orchestrator so that the scan schema is evolved
 * as the scan progresses. Readers are created and managed via a reader
 * factory class unique to each type of scan. The reader factory also provides
 * the scan-specific schema negotiator to be passed to the reader.
 * <p>
 * This framework is a bridge between operator logic and the scan projection
 * internals. It gathers scan-specific options in a builder abstraction, then
 * passes them on the scan orchestrator at the right time. By abstracting out this
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
 *
 * <h4>Orchestration</h4>
 *
 * The above is sufficient to drive the entire scan operator functionality.
 * Projection is done generically and is the same for all files. Only the
 * reader (created via the factory class) differs from one type of file to
 * another.
 * <p>
 * The framework achieves the work described below by composing a large
 * set of detailed classes, each of which performs some specific task. This
 * structure leaves the reader to simply infer schema and read data.
 * <p>
 * In particular, rather than do all the orchestration here (which would tie
 * that logic to the scan operation), the detailed work is delegated to the
 * {@link ScanSchemaOrchestrator} class, with this class as a "shim" between
 * the the Scan events API and the schema orchestrator implementation.
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
 * uses it during read.
 * <p>
 * It is important to note that the result set loader also defines a schema:
 * the schema requested by the reader. If the reader wants to read three
 * columns, a, b, and c, then that is the schema that the result set loader
 * supports. This is true even if the query plan only wants column a, or
 * wants columns c, a. The framework handles the projection task so the
 * reader does not have to worry about it. Reading an unwanted column
 * is low cost: the result set loader will have provided a "dummy" column
 * writer that simply discards the value. This is just as fast as having the
 * reader use if-statements or a table to determine which columns to save.
 * <p>
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
 */
public class ManagedScanFramework implements ScanOperatorEvents {
  static final Logger logger = LoggerFactory.getLogger(ManagedScanFramework.class);

  /**
   * Creates a batch reader on demand. Unlike earlier versions of Drill,
   * this framework creates readers one by one, when they are needed.
   * Doing so avoids excessive resource demands that come from creating
   * potentially thousands of readers up front.
   * <p>
   * The reader itself is unique to each file type. This interface
   * provides a common interface that this framework can use to create the
   * file-specific reader on demand.
   * <p>
   * Also manages opening the reader using a scan-specific schema
   * negotiator.
   */
  public interface ReaderFactory {
    void bind(ManagedScanFramework framework);
    ManagedReader<? extends SchemaNegotiator> next();
  }

  public static class ScanFrameworkBuilder extends ScanOrchestratorBuilder {
    protected ReaderFactory readerFactory;
    protected String userName;

    public void setReaderFactory(ReaderFactory readerFactory) {
      this.readerFactory = readerFactory;
    }

    public ReaderFactory readerFactory() { return readerFactory; }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    @Override
    public ScanOperatorEvents buildEvents() {
      return new ManagedScanFramework(this);
    }
  }

  // Inputs

  protected final ScanFrameworkBuilder builder;
  protected final ReaderFactory readerFactory;
  protected OperatorContext context;

  // Internal state

  protected ScanSchemaOrchestrator scanOrchestrator;

  public ManagedScanFramework(ScanFrameworkBuilder builder) {
    this.builder = builder;
    readerFactory = builder.readerFactory;
    assert readerFactory != null;
  }

  @Override
  public void bind(OperatorContext context) {
    this.context = context;
    configure();
    scanOrchestrator = new ScanSchemaOrchestrator(context.getAllocator(), builder);
    readerFactory.bind(this);
  }

  public OperatorContext context() { return context; }

  public ScanSchemaOrchestrator scanOrchestrator() {
    return scanOrchestrator;
  }

  public TupleMetadata outputSchema() {
    return scanOrchestrator.providedSchema();
  }

  public CustomErrorContext errorContext() { return builder.errorContext(); }

  protected void configure() { }

  @Override
  public RowBatchReader nextReader() {
    if (scanOrchestrator.atLimit()) {
      return null;
    }
    ManagedReader<? extends SchemaNegotiator> reader = readerFactory.next();
    return reader == null ? null : new ShimBatchReader(this, reader);
  }

  protected SchemaNegotiatorImpl newNegotiator() {
    return new SchemaNegotiatorImpl(this);
  }

  @SuppressWarnings("unchecked")
  public boolean open(ShimBatchReader shimBatchReader) {
    SchemaNegotiatorImpl schemaNegotiator = newNegotiator();
    schemaNegotiator.bind(shimBatchReader);
    return ((ManagedReader<SchemaNegotiator>) shimBatchReader.reader()).open(schemaNegotiator);
  }

  @Override
  public void close() {
    if (scanOrchestrator != null) {
      scanOrchestrator.close();
      scanOrchestrator = null;
    }
  }
}
