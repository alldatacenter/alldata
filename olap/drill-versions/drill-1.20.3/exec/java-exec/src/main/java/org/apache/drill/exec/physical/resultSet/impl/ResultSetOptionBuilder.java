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
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl.ResultSetOptions;
import org.apache.drill.exec.physical.resultSet.project.RequestedTuple;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.BaseValueVector;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Builder for the options for the row set loader. Reasonable defaults
 * are provided for all options; use the default options for test code or
 * for clients that don't need special settings.
 */
public class ResultSetOptionBuilder {
  protected int vectorSizeLimit;
  protected int rowCountLimit;
  protected ResultVectorCache vectorCache;
  protected RequestedTuple projectionSet;
  protected ProjectionFilter projectionFilter;
  protected TupleMetadata readerSchema;
  protected long maxBatchSize;
  protected long scanLimit = Long.MAX_VALUE;

  /**
   * Error message context
   */
  protected CustomErrorContext errorContext;

  public ResultSetOptionBuilder() {
    // Start with the default option values.
    ResultSetOptions options = new ResultSetOptions();
    vectorSizeLimit = options.vectorSizeLimit;
    rowCountLimit = options.rowCountLimit;
    maxBatchSize = options.maxBatchSize;
  }

  /**
   * Specify the maximum number of rows per batch. Defaults to
   * {@link BaseValueVector#INITIAL_VALUE_ALLOCATION}. Batches end either
   * when this limit is reached, or when a vector overflows, whichever
   * occurs first. The limit is capped at {@link ValueVector#MAX_ROW_COUNT}.
   *
   * @param limit the row count limit
   * @return this builder
   */
  public ResultSetOptionBuilder rowCountLimit(int limit) {
    rowCountLimit = Math.max(1,
        Math.min(limit, ValueVector.MAX_ROW_COUNT));
    return this;
  }

  public ResultSetOptionBuilder batchSizeLimit(int bytes) {
    maxBatchSize = bytes;
    return this;
  }

  /**
   * Downstream operators require "vector persistence": the same vector
   * must represent the same column in every batch. For the scan operator,
   * which creates multiple readers, this can be a challenge. The vector
   * cache provides a transparent mechanism to enable vector persistence
   * by returning the same vector for a set of independent readers. By
   * default, the code uses a "null" cache which creates a new vector on
   * each request. If a true cache is needed, the caller must provide one
   * here.
   */
  public ResultSetOptionBuilder vectorCache(ResultVectorCache vectorCache) {
    this.vectorCache = vectorCache;
    return this;
  }

  /**
   * Clients can use the row set builder in several ways:
   * <ul>
   * <li>Provide the schema up front, when known, by using this method to
   * provide the schema.</li>
   * <li>Discover the schema on the fly, adding columns during the write
   * operation. Leave this method unset to start with an empty schema.</li>
   * <li>A combination of the above.</li>
   * </ul>
   * @param readerSchema the initial schema for the loader
   * @return this builder
   */
  public ResultSetOptionBuilder readerSchema(TupleMetadata readerSchema) {
    this.readerSchema = readerSchema;
    return this;
  }

  public ResultSetOptionBuilder projection(RequestedTuple projSet) {
    this.projectionSet = projSet;
    return this;
  }

  public ResultSetOptionBuilder projectionFilter(ProjectionFilter projectionFilter) {
    this.projectionFilter = projectionFilter;
    return this;
  }

  public ResultSetOptionBuilder limit(long limit) {
    if (limit < 0) {
      this.scanLimit = Long.MAX_VALUE;
    } else {
      this.scanLimit = limit;
    }
    return this;
  }

  /**
   * Provides context for error messages.
   */
  public ResultSetOptionBuilder errorContext(CustomErrorContext context) {
    this.errorContext = context;
    return this;
  }

  public ResultSetOptions build() {
    return new ResultSetOptions(this);
  }
}
