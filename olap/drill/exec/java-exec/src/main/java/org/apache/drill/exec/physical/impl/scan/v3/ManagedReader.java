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

/**
 * Extended version of a record reader which uses a size-aware batch mutator.
 * Use this for all new readers. Replaces the original
 * {@link org.apache.drill.exec.store.RecordReader} interface.
 * <p>
 * This interface is used to create readers that work with the projection
 * mechanism to provide services for handling projection, setting up the result
 * set loader, handling schema smoothing, sharing vectors across batches, etc.
 * <p>
 * Note that this interface reads a <b>batch</b> of rows, not a single row. (The
 * original {@code RecordReader} could be confusing in this aspect.)
 * <p>
 * The expected lifecycle is:
 * <ul>
 * <li>The reader factory creates the reader just before using it. (Unlike
 * the old {@code ScanBatch} which created all readers at the start of the
 * scan.)</li>
 * <li>Constructor: open the reader using the
 * {@link SchemaNegotiator} to configure the
 * scanner framework for this reader by specifying a schema (if known), desired
 * row counts and other configuration options. Call {@link SchemaNegotiator#build()}
 * to obtain a {@link org.apache.drill.exec.physical.resultSet.RowSetLoader}
 * to use to capture the rows that the reader reads.</li>
 * <li>{@link #next()}: called for each batch. The batch is written using the
 * result set loader obtained above. The scanner framework handles details of
 * tracking version changes, handling overflow, limiting record counts, and
 * so on. Return <tt>true</tt> to indicate a batch is available, <tt>false</tt>
 * to indicate EOF. The first call to <tt>next()</tt> can return <tt>false</tt>
 * if the data source has no rows.</li>
 * <li>{@link #close()}: called to release resources. May be called before
 * <tt>next()</tt> returns </tt>false</tt>.</li>
 * <p>
 * If an error occurs, the reader can throw a {@link RuntimeException}
 * from any method. A <tt>UserException</tt> is preferred to provide
 * detailed information about the source of the problem.
 */
public interface ManagedReader {

  /**
   * Exception thrown from the constructor if the data source is empty and
   * can produce no data or schema. The scan will skip over the reader.
   */
  @SuppressWarnings("serial")
  public class EarlyEofException extends Exception {
  }

  /**
   * Read the next batch. Reading continues until either EOF,
   * or until the mutator indicates that the batch is full.
   * The batch is considered valid if it is non-empty. Returning
   * <tt>true</tt> with an empty batch is valid, and is helpful on
   * the very first batch (returning schema only.) An empty batch
   * with a <tt>false</tt> return code indicates EOF and the batch
   * will be discarded. A non-empty batch along with a <tt>false</tt>
   * return result indicates a final, valid batch, but that EOF was
   * reached and no more data is available.
   * <p>
   * This somewhat complex protocol avoids the need to allocate a
   * final batch just to find out that no more data is available;
   * it allows EOF to be returned along with the final batch.
   *
   * @return <tt>true</tt> if more data may be available (and so
   * <tt>next()</tt> should be called again, <tt>false</tt> to indicate
   * that EOF was reached
   *
   * @throws RuntimeException (<tt>UserException</tt> preferred) if an
   * error occurs that should fail the query.
   */
  boolean next();

  /**
   * Release resources. Called just after a failure, when the scanner
   * is cancelled, or after <tt>next()</tt> returns EOF. Release
   * all resources and close files. Guaranteed to be called if
   * <tt>open()</tt> returns normally; will not be called if <tt>open()</tt>
   * throws an exception.
   *
   * @throws RuntimeException (<tt>UserException</tt> preferred) if an
   * error occurs that should fail the query.
   */
  void close();
}
