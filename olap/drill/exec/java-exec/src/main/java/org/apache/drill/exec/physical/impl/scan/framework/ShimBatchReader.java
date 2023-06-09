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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiatorImpl.NegotiatorListener;
import org.apache.drill.exec.physical.impl.scan.project.ReaderSchemaOrchestrator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.record.VectorContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a layer of row batch reader that works with a
 * result set loader and schema manager to structure the data
 * read by the actual row batch reader.
 * <p>
 * Provides the row set loader used to construct record batches.
 * <p>
 * The idea of this class is that schema construction is complex,
 * and varies depending on the kind of reader. Rather than pack
 * that logic into the scan operator and scan-level reader state,
 * this class abstracts out the schema logic. This allows a variety
 * of solutions as needed for different readers.
 */
public class ShimBatchReader implements RowBatchReader, NegotiatorListener {
  static final Logger logger = LoggerFactory.getLogger(ShimBatchReader.class);

  protected final ManagedScanFramework framework;
  protected final ManagedReader<? extends SchemaNegotiator> reader;
  protected final ReaderSchemaOrchestrator readerOrchestrator;
  protected SchemaNegotiatorImpl schemaNegotiator;
  protected ResultSetLoader tableLoader;

  /**
   * True once the reader reports EOF. This shim may keep going for another
   * batch to handle any look-ahead row on the last batch.
   */
  private boolean eof;

  public ShimBatchReader(ManagedScanFramework manager,
      ManagedReader<? extends SchemaNegotiator> reader) {
    this.framework = manager;
    this.reader = reader;
    readerOrchestrator = manager.scanOrchestrator().startReader();
  }

  @Override
  public String name() {
    return reader.getClass().getSimpleName();
  }

  public ManagedReader<? extends SchemaNegotiator> reader() { return reader; }

  @Override
  public boolean open() {

    // Build and return the result set loader to be used by the reader.

    if (!framework.open(this)) {

      // If we had a soft failure, then there should be no schema.
      // The reader should not have negotiated one. Not a huge
      // problem, but something is out of whack.

      assert tableLoader == null;
      if (tableLoader != null) {
        logger.warn("Reader " + reader.getClass().getSimpleName() +
            " returned false from open, but negotiated a schema.");
      }
      return false;
    }

    // Storage plugins are extensible: a novice developer may not
    // have known to create the table loader. Fail in this case.

    if (tableLoader == null) {
      throw UserException.internalError(null)
        .addContext("Reader " + reader.getClass().getSimpleName() +
                    " returned true from open, but did not call SchemaNegotiator.build().")
        .build(logger);
    }
    return true;
  }

  @Override
  public boolean defineSchema() {
    if (schemaNegotiator.isSchemaComplete()) {
      readerOrchestrator.defineSchema();
      return true;
    }
    return false;
  }

  @Override
  public boolean next() {

    // The reader may report EOF, but the result set loader might
    // have a lookahead row.

    if (eof && !tableLoader.hasRows()) {
      return false;
    }

    // Hit the per-reader limit?
    if (tableLoader.atLimit()) {
      return false;
    }

    // Prepare for the batch.

    if (!readerOrchestrator.startBatch()) {
      eof = true;
      return false;
    }

    // Read the batch. The reader should report EOF if it hits the
    // end of data, even if the reader returns rows. This will prevent allocating
    // a new batch just to learn about EOF. Don't read if the reader
    // already reported EOF. In that case, we're just processing any last
    // lookahead row in the result set loader.

    if (!eof) {
      eof = !reader.next();
    }

    // Add implicit columns, if any.
    // Identify the output container and its schema version.
    // Having a correct row count, even if 0, is important to
    // the scan operator.

    eof = readerOrchestrator.endBatch(eof);

    // Return EOF (false) only when the reader reports EOF
    // and the result set loader has drained its rows from either
    // this batch or lookahead rows.

    return !eof || tableLoader.hasRows();
  }

  @Override
  public VectorContainer output() {

    // Output should be defined only if vector schema has
    // been defined.

    if (framework.scanOrchestrator().hasSchema()) {
      return framework.scanOrchestrator().output();
    } else {
      return null;
    }
  }

  @Override
  public void close() {

    // Track exceptions and keep closing

    RuntimeException ex = null;
    try {

      // Close the actual reader

      reader.close();
    } catch (RuntimeException e) {
      ex = e;
    }

    // Inform the scan orchestrator that the reader is closed.
    // The scan orcestrator closes the reader orchestrator which
    // closes the table loader, so we don't close the table loader
    // here.

    framework.scanOrchestrator().closeReader();

    // Throw any exceptions.

    if (ex != null) {
      throw ex;
    }
  }

  @Override
  public int schemaVersion() {
    return tableLoader.schemaVersion();
  }

  @Override
  public ResultSetLoader build(SchemaNegotiatorImpl schemaNegotiator) {
    this.schemaNegotiator = schemaNegotiator;
    readerOrchestrator.setBatchSize(schemaNegotiator.batchSize);
    tableLoader = readerOrchestrator.makeTableLoader(schemaNegotiator.errorContext(),
        schemaNegotiator.tableSchema, schemaNegotiator.limit);
    return tableLoader;
  }
}
