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

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.StaticBatchBuilder.NullBatchBuilder;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;

/**
 * Builds the handler which provides values for columns in
 * an explicit project list but for which
 * the reader provides no values. Obtains types from a defined or provided
 * schema, or using a configurable default type. Fills in null values,
 * or a default value configured in a provided schema.
 * <p>
 * The set of missing columns may differ per reader or even per batch
 * within a reader. If reader 1 reads all columns, but reader 2 reads
 * a subset, then this class will use the column types from reader 1
 * when creating the columns missing from reader 2.
 * <p>
 * Unfortunately, Drill cannot predict the future, so the opposite
 * scenario will end badly: Reader 2 comes first, omits column "c",
 * this class chooses a default value, then Reader 1 wants the column
 * to be some other type. The query will fail with a type mismatch error.
 * <p>
 * Specifically, the mechanism uses the following rules to infer column
 * type:
 * <ul>
 * <li>For <i>resolved</i> columns (those with a type), use that type.
 * If the type is non-nullable, fill in a default value (generally 0 or
 * blank.) A column is resolved if given by a defined schema, a provided
 * schema or a prior reader.</li>
 * <li>For <i>unresolved</i> columns (those without a type), use the
 * default type configured in this builder. If no type is provied,
 * use a "default default" of Nullable INT, Drill's classic choice.</li>
 * <ul>
 * <p>
 * Note that Drill is not magic: relying on the default type is likely
 * to cause a type conflict across readers or across scans. A default
 * has no way of knowing if it matches the same column read in some other
 * fragment on some other node.
 * <p>
 * Work is separated in a schema-time part (to resolve column types)
 * and a read-time part (to create and fill the needed vectors.)
 *
 * <h4>Caveats</h4>
 *
 * The project mechanism handles nested "missing" columns as mentioned
 * above. This works to create null columns within maps that are defined by the
 * data source. However, the mechanism does not currently handle creating null
 * columns within repeated maps or lists. Doing so is possible, but requires
 * adding a level of cardinality computation to create the proper number of
 * "inner" values.
 */
public class MissingColumnHandlerBuilder {
  public static final MajorType DEFAULT_NULL_TYPE = MajorType.newBuilder()
      .setMinorType(MinorType.INT)
      .setMode(DataMode.OPTIONAL)
      .build();

  protected TupleMetadata inputSchema;
  protected MajorType nullType;
  protected boolean allowRequiredNullColumns;
  protected ResultVectorCache vectorCache;
  protected TupleMetadata outputSchema;

  public MissingColumnHandlerBuilder inputSchema(TupleMetadata inputSchema) {
    this.inputSchema = inputSchema;
    return this;
  }

  public MissingColumnHandlerBuilder nullType(MajorType nullType) {
    this.nullType = nullType;
    return this;
  }

  public MissingColumnHandlerBuilder allowRequiredNullColumns(boolean flag) {
    allowRequiredNullColumns = flag;
    return this;
  }

  public MissingColumnHandlerBuilder vectorCache(ResultVectorCache vectorCache) {
    this.vectorCache = vectorCache;
    return this;
  }

  public TupleMetadata buildSchema() {
    if (inputSchema == null || inputSchema.isEmpty()) {
      return null;
    }
    outputSchema = new TupleSchema();
    MajorType selectedNullType = nullType == null
        ? DEFAULT_NULL_TYPE : nullType;
    for (ColumnMetadata inputCol : inputSchema) {
      if (inputCol.isDynamic()) {
        outputSchema.addColumn(MetadataUtils.newScalar(inputCol.name(), selectedNullType));
      } else {
        outputSchema.addColumn(inputCol);
      }
    }
    return outputSchema;
  }

  public StaticBatchBuilder build() {
    buildSchema();
    if (outputSchema == null) {
      return null;
    } else {
      return new NullBatchBuilder(vectorCache, outputSchema);
    }
  }
}
