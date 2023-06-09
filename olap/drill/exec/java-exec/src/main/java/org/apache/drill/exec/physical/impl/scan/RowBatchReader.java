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

import org.apache.drill.exec.record.VectorContainer;

/**
 * Extended version of a record reader used by the revised
 * scan batch operator. Use this for all new readers. Replaces the
 * original {@link RecordReader} interface.
 * <p>
 * Classes that extend from this interface must handle all
 * aspects of reading data, creating vectors, handling projection
 * and so on. That is, extensions of this class are intended to
 * be frameworks.
 * <p>
 * For most cases, a plugin probably wants to start from a
 * base implementation, such as the {@link ManagedReader} class,
 * which provides services for handling projection, setting up
 * the result set loader, handling schema smoothing, sharing
 * vectors across batches, etc.
 * <p>
 * Note that this interface reads a <b>batch</b> of rows, not
 * a single row. (The original {@code RecordReader} could be
 * confusing in this aspect.)
 * <p>
 * The expected lifecycle is:
 * <ul>
 * <li>Construction. Allocate no resources.</li>
 * <li>{@link #open()}: Allocate resources and set up the schema
 * (if early schema.)</li>
 * <li>{@link #output()} and {@link #schemaVersion()} to determine
 * the initial version. If no schema is available on open (the reader
 * is late-schema), return null for the output and -1 for the schema
 * version. Else, return non-negative for the version number and
 * an empty batch with a schema.</li>
 * <li>{@link #next()}} to retrieve the next record batch.
 * Return true if a batch is available, false if EOF. There is no
 * requirement to return a batch; the first call to {@code next()}
 * can return {@code false} if no data is available.}
 * <li>{@link #output()} and {@link #schemaVersion()} to obtain the
 * batch of records read, and to detect if the version of the schema
 * is different from the previous batch.</li>
 * <li>{@link #close()} when the reader is no longer needed. This
 * may occur before {@code next()} returns {@code false} if an
 * error occurs or a limit is reached.</li>
 * </ul>
 * Although the reader should not care, the scanner must return an
 * empty batch downstream to provide a "fast schema" for other operators.
 * The scan operator handles this transparently:
 * <ul>
 * <li>If the reader is early-schema, then the reader itself returns
 * an empty batch (via {@code output()</tt), with only schema after the call
 * to {@code open()}. The scanner sends this empty batch downstream
 * directly.</li>
 * <li>If the reader is late-schema, then the reader will read the first
 * batch from the reader. It will save that batch, extract the schema,
 * and send an "artificial" batch downstream to satisfy the "fast schema"
 * protocol.</li>
 * </ul>
 * As a result, there is little reason for the reader to worry about
 * "fast schema" other than providing an early schema, if available, else
 * don't worry about it.
 * <p>
 * If an error occurs, the reader can throw a {@link RuntimeException}
 * from any method. A {@link UserException} is preferred to provide
 * detailed information about the source of the problem.
 */
public interface RowBatchReader {

  /**
   * Name used when reporting errors. Can simply be the class name.
   *
   * @return display name for errors
   */
  String name();

  /**
   * Setup the record reader. Called just before the first call
   * to {@code next()}. Allocate resources here, not in the constructor.
   * Example: open files, allocate buffers, etc.
   *
   * @return true if the reader is open and ready to read (possibly no)
   * rows. false for a "soft" failure in which no schema or data is available,
   * but the scanner should not fail, it should move onto another reader
   *
   * @throws RuntimeException for "hard" errors that should terminate
   * the query. {@code UserException} preferred to explain the problem
   * better than the scan operator can by guessing at the cause
   */
  boolean open();

  /**
   * Called for the first reader within a scan. Allows the reader to
   * provide an empty batch with only the schema filled in. Readers that
   * are "early schema" (know the schema up front) should return true
   * and create an empty batch. Readers that are "late schema" should
   * return false. In that case, the scan operator will ask the reader
   * to load an actual data batch, and infer the schema from that batch.
   * <p>
   * This step is optional and is purely for performance.
   *
   * @return true if this reader can (and has) defined an empty batch
   * to describe the schema, false otherwise
   */
  boolean defineSchema();

  /**
   * Read the next batch. Reading continues until either EOF,
   * or until the mutator indicates that the batch is full.
   * The batch is considered valid if it is non-empty. Returning
   * {@code true} with an empty batch is valid, and is helpful on
   * the very first batch (returning schema only.) An empty batch
   * with a {@code false} return code indicates EOF and the batch
   * will be discarded. A non-empty batch along with a {@code false}
   * return result indicates a final, valid batch, but that EOF was
   * reached and no more data is available.
   * <p>
   * This somewhat complex protocol avoids the need to allocate a
   * final batch just to find out that no more data is available;
   * it allows EOF to be returned along with the final batch.
   *
   * @return {@code true} if more data may be available (and so
   * {@code next()} should be called again, {@code false} to indicate
   * that EOF was reached
   *
   * @throws RuntimeException ({@code UserException} preferred) if an
   * error occurs that should fail the query.
   */
  boolean next();

  /**
   * Return the container with the reader's output. This method is called
   * at two times:
   * <ul>
   * <li>Directly after the call to {@link #open()}. If the data source
   * can provide a schema at open time, then the reader should provide an
   * empty batch with the schema set. The scanner will return this schema
   * downstream to inform other operators of the schema.</li>
   * <li>Directly after a successful call to {@link next()} to retrieve
   * the batch produced by that call. (No call is made if {@code next()}
   * returns false.</li>
   * </ul>
   * Note that most operators require the same vectors be present in
   * each container. So, in practice, a reader must return the same
   * container, and same set of vectors, on each call.
   *
   * @return a vector container, with the record count and schema
   * set, that announces the schema after {@code open()} (optional)
   * or returns rows read after {@code next()} (required)
   */
  VectorContainer output();

  /**
   * Return the version of the schema returned by {@link output()}. The schema
   * is assumed to start at -1 (no schema). The reader is free to use any
   * numbering system it likes as long as:
   * <ul>
   * <li>The numbers are non-negative, and increase (by any increment),</li>
   * <li>Numbers between successive calls are idential if the batch schemas are
   * identical,</li>
   * <li>The number increases if a batch has a different schema than the
   * previous batch.</li>
   * </ul>
   * Numbers increment (or not) on calls to {@code next()}. Thus Two successive
   * calls to this method should return the same number if no {@code next()}
   * call lies between.
   * <p>
   * If the reader can return a schema on open (so-called "early-schema), then
   * this method must return a non-negative version number, even if the schema
   * happens to be empty (such as reading an empty file.)
   * <p>
   * However, if the reader cannot return a schema on open (so-called "late
   * schema"), then this method must return -1 (and {@code output()} must
   * return null) to indicate now schema is available when called before the
   * first call to {@code next()}.
   * <p>
   * No calls will be made to this method before {@code open()} after
   * {@code close(){@code  or after {@code next()} returns false. The implementation
   * is thus not required to handle these cases.
   *
   * @return the schema version, or -1 if no schema version is yet available
   */
  int schemaVersion();

  /**
   * Release resources. Called just after a failure, when the scanner
   * is cancelled, or after {@code next()} returns EOF. Release
   * all resources and close files. Guaranteed to be called if
   * {@code open()} returns normally; will not be called if {@code open()}
   * throws an exception.
   *
   * @throws RutimeException ({@code UserException} preferred) if an
   * error occurs that should fail the query.
   */
  void close();
}
