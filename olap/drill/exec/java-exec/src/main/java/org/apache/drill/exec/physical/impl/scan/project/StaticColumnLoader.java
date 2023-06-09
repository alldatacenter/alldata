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
package org.apache.drill.exec.physical.impl.scan.project;

import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetOptionBuilder;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl;
import org.apache.drill.exec.record.VectorContainer;

/**
 * Base class for columns that take values based on the
 * reader, not individual rows. E.g. null columns (values
 * are all null) or file metadata (AKA "implicit") columns
 * that take values based on the file.
 */

public abstract class StaticColumnLoader {
  protected final ResultSetLoader loader;
  protected final ResultVectorCache vectorCache;

  public StaticColumnLoader(ResultVectorCache vectorCache) {

    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
          .vectorCache(vectorCache)
          .build();
    loader = new ResultSetLoaderImpl(vectorCache.allocator(), options);
    this.vectorCache = vectorCache;
  }

  /**
   * Populate static vectors with the defined static values.
   *
   * @param rowCount number of rows to generate. Must match the
   * row count in the batch returned by the reader
   */

  public abstract VectorContainer load(int rowCount);

  public void close() {
    loader.close();
  }
}
