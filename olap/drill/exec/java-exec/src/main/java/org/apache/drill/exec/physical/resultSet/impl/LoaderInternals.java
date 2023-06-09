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
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.memory.BufferAllocator;

/**
 * The internal view of the result set loader. Provides operations to manage the batch
 * and batch schema.
 */
interface LoaderInternals {

  /**
   * Allocator to use when allocating buffers for vectors
   * @return buffer allocator
   */
  BufferAllocator allocator();

  /**
   * Increments the schema version when adding a new column anywhere in
   * the writer hierarchy.
   *
   * @return the new schema version
   */
  int bumpVersion();

  /**
   * Reports the current schema version. Used when adding an unprojected
   * column which should not affect the output schema.
   *
   * @return the current schema version
   */
  int activeSchemaVersion();

  /**
   * Accumulate the initial vector allocation sizes.
   *
   * @param allocationBytes number of bytes allocated to a vector
   * in the batch setup step
   */
  void tallyAllocations(int allocationBytes);

  /**
   * Reports whether the loader is in the overflow state. The overflow
   * state occurs when a vector has become full, but before the batch
   * is harvested.
   *
   * @return {@code true</tt> if an overflow has occurred in the present
   * row
   */
  boolean hasOverflow();

  /**
   * Return whether a vector within the current batch can expand. Limits
   * are enforce only if a limit was provided in the options.
   *
   * @param delta increase in vector size
   * @return true if the vector can expand, false if an overflow
   * event should occur
   */
  boolean canExpand(int delta);

  /**
   * Indicates that an overflow has just occurred. Triggers the overflow
   * mechanism. Upon return, the writer that triggered the overflow will
   * find that it is now working with a new vector, and a new write
   * position, that should allow saving of the in-flight value (unless
   * that one value is larger than the maximum vector size.)
   */
  void overflowed();

  /**
   * Current writer row index.
   *
   * @return the current write row index
   */
  int rowIndex();

  /**
   * The desired number of rows in the output batch.
   *
   * @return the target row count
   */
  int targetRowCount();

  /**
   * Indicates if the loader is in a writable state. A writable state
   * occurs when a batch has been started, before the batch overflows
   * or is harvested.
   *
   * @return {@code true} if values can be written to vectors
   */
  boolean writeable();

  ColumnBuilder columnBuilder();

  CustomErrorContext errorContext();
}
