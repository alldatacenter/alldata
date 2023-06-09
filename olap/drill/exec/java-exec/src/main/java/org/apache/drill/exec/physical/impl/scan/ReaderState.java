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
package org.apache.drill.exec.physical.impl.scan;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.accessor.InvalidConversionError;
import org.apache.drill.exec.vector.accessor.UnsupportedConversionError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a row batch reader through its lifecycle. Created when the reader
 * is opened, discarded when the reader is closed. Encapsulates state that
 * follows the life of the reader. This moves common scanner state out of
 * each reader in order to make the reader as simple as possible.
 * <p>
 * This class is private to the scan operator and is not meant to be used,
 * or even visible, outside of that operator itself. Instead, all reader-specific
 * functionality should be in the {@link RowBatchReader} subclass.
 * <p>
 * Each reader is managed in the context of a scan operator. A prior reader
 * may have established a schema that this reader will (hopefully) continue
 * to use. This reader may have no data (and thus no schema). The scan
 * operator will try to quietly pass over this reader, using a schema from
 * either a prior reader or a later reader.
 *
 * <h4>Error Handling</h4>
 *
 * Handles all possible reader errors to control the exceptions thrown
 * to callers. Drill uses unchecked exceptions which makes error handling a
 * bit hard to follow. The reader can report any kind of error. If might
 * nicely report an (unchecked) UserException with details of the failure.
 * Or, it might throw some other unchecked exception (NPE, illegal state
 * or whatever.) This method ensues that all errors are mapped to a
 * UserException for two reasons. First, this ensures that errors are
 * attributed to the reader itself. Second, callers need not also go
 * through the "which kind of exception" dance: they are assured that
 * only UserException is thrown with proper explanations filled in.
 *
 * <h4>Schema Handling</h4>
 *
 * Readers can report schema in one of two ways. An "early schema" reader
 * reports the schema during the reader open. For example, Parquet can
 * obtain the schema from the file header. CSV with headers finds the
 * schema by reading the header row. This is the "happy path": the
 * reader provides a schema on open, calls to next() just provide data.
 * <p>
 * "Late schema" readers are more complex. Opening the reader is not
 * sufficient to discover the schema. Instead, the reader has to read a
 * batch of data in order to infer the schema. The scanner, however, must
 * report schema before the first row of data. This class acts as a shim:
 * it will read ahead one batch of data on open in order to obtain a
 * schema. It will then deliver that "look-ahead" batch on the first
 * call to next().
 * <p>
 * Readers are free to alter the schema at any time by discovering new
 * columns. (Think JSON.) Considerable complexity (mostly implemented
 * elsewhere) is needed to properly handle this case, and to do so in
 * a way that is transparent to the reader.
 * <p>
 * Because schema handling is complex, the logic is split into two
 * parts. This class orchestrates schema actions. A reader-level shim
 * handles the details of schema coordination in a way that is specific
 * to each kind of reader (file reader, generic reader, etc.)
 *
 * <h4>Batch Overflow Handling<h4>
 *
 * A key driving principle of this design is the need to limit batch sizes.
 * Readers, in general, cannot easily determine when to stop reading rows
 * to just fill a batch, minimizing internal fragmentation. Instead, this
 * is the job of the result set loader. This class simply needs to be
 * aware that, even if the reader itself says it may have no more data,
 * there could still be an overflow row in the look-ahead batch maintained
 * by the result set loader. This class hides that complexity from the
 * reader, but provides a clean protocol for the scan operator.
 *
 * <h4>Advice to Developers</hr>
 *
 * The protocol supported here creates a very
 * simple API for readers to implement. We expect contributors to create
 * a large number of readers: our job is to maintain a very clean API
 * that hides as many routine details as possible from the reader
 * author.
 * <p>
 * While Drill supports many readers, there is just one scan operator,
 * with this class as the shim between the two. When considering changes,
 * understand how those changes will affect a wide range of readers. A fix
 * that make sense for one might not make sense for another.
 * <p>
 * Some of the considerations include:
 * <ul>
 * <li>Keep the number of methods small: focus on the essentials.</li>
 * <li>Most of the complex interplay between scan operator and reader
 * is due to schema negotiation. Handle all that via the schema
 * negotiator class rather than by adding methods to the reader
 * interface.</li>
 * <li>As explained above, error handling is complex due to the
 * use of unchecked exceptions. Encourage reader authors to handle all
 * errors themselves by throwing a UserException that clearly states
 * the problem, if it is caused by the user's query, by an environment
 * issue or other item that the user can correct. Use any Java
 * exception to report unexpected errors (assertions, NPEs, etc.) This
 * class wraps all those errors handle whatever the author chooses to
 * do while providing a clean error interface to callers.</li>
 * <li>Readers are simplest if the don't have to worry about repeated
 * calls to next() after the reader reports EOF. This class buffers
 * those calls to enforce the protocol.</li>
 * <li>Handles both early and late schema readers to provide a uniform
 * API, and to allow odd-ball hybrid cases.</li>
 * </ul>
 * When considering changes and fixes to this class (or to the readers),
 * think about how to preserve the clean separation that this class
 * attempts to provide.
 */

class ReaderState {
  static final Logger logger = LoggerFactory.getLogger(ReaderState.class);

  private enum State {

    /**
     * Initial state before opening the reader.
     */

    START,

    /**
     * The scan operator is obligated to provide a "fast schema", without data,
     * before the first row of data. "Early schema" readers (those that provide
     * a schema) can simply provide the schema. However "late schema" readers
     * (those that discover the schema during read) must fetch a batch of rows
     * to infer schema.
     * <p>
     * Note that the reader must fetch an entire batch of rows, not just sample
     * the first dozen or so. This allows the "schema smoothing" features to work,
     * such as finding the first non-null row in JSON, etc. The larger the sample
     * size, the more likely that ambiguities can be resolved (though there is
     * no guarantee, unfortunately.)
     * <p>
     * This state indicates that the reader has read the look-ahead batch so that
     * the next call to {@link ReaderState#next()} will return this look-ahead
     * batch rather than reading a new one.
     */

    LOOK_AHEAD,

    /**
     * As above, but the reader hit EOF during the read of the look-ahead batch.
     * The {@link ReaderState#next()} checks if the lookahead batch has any
     * rows. If so it will return it and move to the EOF state.
     * <p>
     * The goal of the EOF states is to avoid calling the reader's next
     * method after the reader reports EOF. This is done to avoid the need
     * for readers to handle repeated reads after EOF.
     * <p>
     * Note that the look-ahead batch here is distinct from the look-ahead
     * row in the result set loader. That look-ahead is handled by the
     * (shim) reader which this class manages.
     */

    LOOK_AHEAD_WITH_EOF,

    /**
     * Normal state: the reader has supplied data but not yet reported EOF.
     */

    ACTIVE,

    /**
     * The reader has reported EOF. No look-ahead batch is active. The
     * reader's next() method will no longer be called.
     */

    EOF,

    /**
     * The reader is closed: no further operations are allowed.
     */

    CLOSED
  };

  final ScanOperatorExec scanOp;
  private final RowBatchReader reader;
  private State state = State.START;
  private VectorContainer lookahead;

  public ReaderState(ScanOperatorExec scanOp, RowBatchReader reader) {
    this.scanOp = scanOp;
    this.reader = reader;
  }

  /**
   * Open the next available reader, if any, preparing both the
   * reader and table loader.
   *
   * @return true if the reader has data, false if EOF was found on
   * open
   * @throws UserException for all errors
   */

  boolean open() {

    // Open the reader. This can fail. if it does, clean up.

    try {

      // The reader can return a "soft" failure: the open worked, but
      // the file is empty, non-existent or some other form of "no data."
      // Handle this by immediately moving to EOF. The scanner will quietly
      // pass over this reader and move onto the next, if any.

      if (!reader.open()) {
        state = State.EOF;
        return false;
      }

    // When catching errors, leave the reader member set;
    // we must close it on close() later.

    } catch (UserException e) {

      // Throw user exceptions as-is

      throw e;
    } catch (UnsupportedConversionError e) {

      // Occurs if the provided schema asks to convert a reader-provided
      // schema in a way that Drill (or the reader) cannot support.
      // Example: implicit conversion of a float to an INTERVAL
      // In such a case, there are no "natural" rules, a reader would have
      // to provide ad-hoc rules or no conversion is possible.

      throw UserException.validationError(e)
        .message("Invalid runtime type conversion")
        .build(logger);
    } catch (Throwable t) {

      // Wrap all others in a user exception.

      throw UserException.executionError(t)
        .addContext("Open failed for reader", reader.name())
        .build(logger);
    }

    state = State.ACTIVE;
    return true;
  }

  /**
   * Prepare the schema for this reader. Called for the first reader within a
   * scan batch, if the reader returns <tt>true</tt> from <tt>open()</tt>.
   * Asks the reader if it can provide a schema-only empty batch by calling
   * the reader's <tt>defineSchema()</tt> method. If this is an early-schema
   * reader, and it can provide a schema, then it should create an empty
   * batch so that the the result set loader already has
   * the proper value vectors set up. If this is a late-schema reader, we must
   * read one batch to get the schema, then set aside the data for the next
   * call to <tt>next()</tt>.
   * <p>
   * Semantics for all readers:
   * <ul>
   * <li>If the file was not found, <tt>open()</tt> returned false and this
   * method should never be called.</li>
   * </ul>
   * <p>
   * Semantics for early-schema readers:
   * <ul>
   * <li>If if turned out that the file was
   * empty when trying to read the schema, <tt>open()</tt> returned false
   * and this method should never be called.</tt>
   * <li>Otherwise, the reader does not know if it is the first reader or
   * not. The call to <tt>defineSchema()</tt> notifies the reader that it
   * is the first one. The reader should set up in the result set loader
   * with an empty batch.
   * </ul>
   * <p>
   * Semantics for late-schema readers:
   * <ul>
   * <li>This method will ask the reader to
   * read a batch. If the reader hits EOF before finding any data, this method
   * will return false, indicating that no schema is available.</li>
   * <li>If the reader can read enough of the file to
   * figure out the schema, but the file has no data, then this method will
   * return <tt>true</tt> and a schema will be available. The first call to
   * <tt>next()</tt> will report EOF.</li>
   * <li>Otherwise, this method returns true, sets up an empty batch with the
   * schema, saves the data batch, and will return that look-ahead batch on the
   * first call to <tt>next()</tt>.</li>
   * </ul>
   * @return true if the schema was read, false if EOF was reached while trying
   * to read the schema.
   * @throws UserException for all errors
   */
  protected boolean buildSchema() {

    if (reader.defineSchema()) {

      // Bind the output container to the output of the scan operator.
      // This returns an empty batch with the schema filled in.
      scanOp.containerAccessor.setSchema(reader.output());
      return true;
    }

    // Late schema. Read a batch.
    if (! next()) {
      return false;
    }
    VectorContainer container = reader.output();
    if (container.getRecordCount() == 0) {
      return true;
    }

    // The reader returned actual data. Just forward the schema
    // in the operator's container, saving the data for next time
    // in a dummy container.
    assert lookahead == null;
    lookahead = new VectorContainer(scanOp.context.getAllocator(), scanOp.containerAccessor.schema());
    lookahead.setRecordCount(0);
    lookahead.exchange(scanOp.containerAccessor.container());
    state = state == State.EOF ? State.LOOK_AHEAD_WITH_EOF : State.LOOK_AHEAD;
    return true;
  }

  /**
   * Read another batch of data from the reader. Can be called repeatedly
   * even after EOF. Handles look-ahead batches.
   *
   * @return true if a batch of rows is available in the scan operator,
   * false if EOF was hit
   * @throws UserException for all reader errors
   */
  protected boolean next() {
    switch (state) {
    case LOOK_AHEAD:
    case LOOK_AHEAD_WITH_EOF:
      // Use batch previously read.
      assert lookahead != null;
      lookahead.exchange(scanOp.containerAccessor.container());
      assert lookahead.getRecordCount() == 0;
      lookahead = null;
      if (state == State.LOOK_AHEAD_WITH_EOF) {
        state = State.EOF;
      } else {
        state = State.ACTIVE;
      }
      return true;

    case ACTIVE:
      return readBatch();

    case EOF:
      return false;

    default:
      throw new IllegalStateException("Unexpected state: " + state);
    }
  }

  /**
   * Read a batch from the reader.
   * <p>
   * Expected semantics for the reader's <tt>next()</tt> method:
   * <ul>
   * <li>Non-empty batch and return true: data returned and more
   * data is (probably) available.</li>
   * <li>Empty batch and return true: data returned but it is the last
   * batch; EOF was reached while reading the batch.</li>
   * <li>Empty batch and return false: EOF reached, discard the
   * empty batch. (An inefficient way to indicate EOF since a set
   * of vectors is allocated, then discarded. The previous result
   * is preferred when possible.</li>
   * <li>Empty batch and return true: An odd case that is allowed;
   * the batch is discarded and <tt>next()</tt> is called again.</li>
   * </ul>
   * In short:
   * <ul>
   * <li>A non-empty batch says that there is data to return.</li>
   * <li>The return code says whether <tt>next()</tt> should be called
   * again.</li>
   * </ul>
   *
   * @return true if a batch was read, false if the reader hit EOF
   * @throws UserException for all reader errors
   */
  private boolean readBatch() {

    // Try to read a batch. This may fail. If so, clean up the mess.
    boolean more;
    try {
      more = reader.next();
    } catch (UserException e) {
      throw e;
    } catch (InvalidConversionError e) {

      // Occurs when a specific data value to be converted to another type
      // is not valid for that conversion. For example, providing the value
      // "foo" to a string-to-int conversion.
      throw UserException.unsupportedError(e)
        .message("Invalid data value for automatic type conversion")
        .addContext("Read failed for reader", reader.name())
        .build(logger);
    } catch (Throwable t) {
      throw UserException.executionError(t)
        .addContext("Read failed for reader", reader.name())
        .build(logger);
    }

    VectorContainer output = reader.output();
    if (!more) {
      state = State.EOF;
      if (output == null) {
        return false;
      }

      // The reader can indicate EOF (they can't return any more rows)
      // while returning a non-empty final batch. This is the typical
      // case with files: the reader read some records and then hit
      // EOF. Avoids the need for the reader to keep an EOF state.
      if (output.getRecordCount() == 0) {

        // No results, possibly from the first batch.
        // If the scan has no schema, but this (possibly empty) reader
        // does have a schema, then pass along this empty batch
        // as a candidate empty result set of the entire scan.
        if (scanOp.containerAccessor.schemaVersion() == 0 &&
            reader.schemaVersion() > 0) {
          scanOp.containerAccessor.setSchema(output);
        }
        output.zeroVectors();
        return false;
      }

      // EOF (the reader can provide no more batches), but
      // the reader did provide rows in this batch. Fall through.
    }

    // Late schema readers may change their schema between batches.
    // Early schema changes only on the first batch of the next
    // reader. (This is not a hard and fast rule, only a definition:
    // a reader that starts with a schema, but later changes it, has
    // morphed from an early- to late-schema reader.)
    scanOp.containerAccessor.addBatch(output);
    return true;
  }

  /**
   * Close the current reader. The hard part is handling the possible
   * error conditions, and cleaning up despite those errors.
   */
  void close() {
    if (state == State.CLOSED) {
      return;
    }

    // Close the reader. This can fail.
    try {
      reader.close();
    } catch (UserException e) {
      throw e;
    } catch (Throwable t) {
      throw UserException.executionError(t)
        .addContext("Close failed for reader", reader.name())
        .build(logger);
    } finally {

      // Will not throw exceptions
      if (lookahead != null) {
        lookahead.clear();
        lookahead = null;
      }
      state = State.CLOSED;
    }
  }
}
