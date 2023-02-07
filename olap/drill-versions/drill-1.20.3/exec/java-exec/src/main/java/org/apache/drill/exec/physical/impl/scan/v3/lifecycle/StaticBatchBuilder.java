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

import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetOptionBuilder;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ScalarWriter;

/**
 * Base class for columns that take values based on the
 * reader, not individual rows. E.g. null columns (values
 * are all null) or implicit file columns
 * that take values based on the file.
 * <p>
 * If a column needs a default value, that value can be set either via
 * the values vector or as a column
 * property on the column schema. The result set loader uses the default
 * value to fill empty values. Otherwise, the column is assumed to be nullable
 * and is filled with nulls.
 */
public abstract class StaticBatchBuilder {
  protected final TupleMetadata schema;
  protected final ResultSetLoader loader;

  public static class NullBatchBuilder extends StaticBatchBuilder {
    public NullBatchBuilder(ResultVectorCache vectorCache, TupleMetadata schema) {
      super(vectorCache, schema);
    }

    @Override
    protected void loadBatch(int rowCount) {
      loader.skipRows(rowCount);
    }
  }

  public static class RepeatedBatchBuilder extends StaticBatchBuilder {
    private final ScalarWriter writers[];
    private final Object values[];

    public RepeatedBatchBuilder(ResultVectorCache vectorCache, TupleMetadata schema, Object values[]) {
      super(vectorCache, schema);
      this.values = values;
      RowSetLoader writer = loader.writer();
      writers = new ScalarWriter[schema.size()];
      for (int i = 0; i < writers.length; i++) {
        writers[i] = writer.scalar(i);
      }
    }

    @Override
    protected void loadBatch(int rowCount) {
      RowSetLoader writer = loader.writer();
      int n = Math.min(values.length, schema.size());
      for (int i = 0; i < rowCount; i++) {
        writer.start();
        for (int j = 0; j < n; j++) {
          if (values[j] != null) {
            writers[j].setValue(values[j]);
          }
        }
        writer.save();
      }
    }
  }

  public StaticBatchBuilder(ResultVectorCache vectorCache, TupleMetadata schema) {
    this.schema = schema;
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
          .vectorCache(vectorCache)
          .readerSchema(schema)
          .build();
    this.loader = new ResultSetLoaderImpl(vectorCache.allocator(), options);
  }

  public TupleMetadata schema() { return schema; }

  /**
   * Populate static vectors with the defined static values.
   *
   * @param rowCount number of rows to generate. Must match the
   * row count in the batch returned by the reader
   */

  public VectorContainer load(int rowCount) {
    loader.startBatch();
    loadBatch(rowCount);
    return loader.harvest();
  }

  protected abstract void loadBatch(int rowCount);

  public void close() {
    loader.close();
  }

  public VectorContainer outputContainer() {
    return loader.outputContainer();
  }
}
