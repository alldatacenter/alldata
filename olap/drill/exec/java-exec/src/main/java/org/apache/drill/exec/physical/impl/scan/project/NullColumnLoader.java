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

import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;

/**
 * Create and populate null columns for the case in which a SELECT statement
 * refers to columns that do not exist in the actual table. Nullable and array
 * types are suitable for null columns. (Drill defines an empty array as the
 * same as a null array: not true, but the best we have at present.) Required
 * types cannot be used as we don't know what value to set into the column
 * values.
 * <p>
 * Seeks to preserve "vector continuity" by reusing vectors when possible.
 * Cases:
 * <ul>
 * <li>A column a was available in a prior reader (or batch), but is no longer
 * available, and is thus null. Reuses the type and vector of the prior reader
 * (or batch) to prevent trivial schema changes.</li>
 * <li>A column has an implied type (specified in the metadata about the
 * column provided by the reader.) That type information is used instead of
 * the defined null column type.</li>
 * <li>A column has no type information. The type becomes the null column type
 * defined by the reader (or nullable int by default.</li>
 * <li>Required columns are not suitable. If any of the above found a required
 * type, convert the type to nullable.</li>
 * <li>The resulting column and type, whatever it turned out to be, is placed
 * into the vector cache so that it can be reused by the next reader or batch,
 * to again preserve vector continuity.</li>
 * </ul>
 * The above rules eliminate "trivia" schema changes, but can still result in
 * "hard" schema changes if a required type is replaced by a nullable type.
 */

public class NullColumnLoader extends StaticColumnLoader {

  public interface NullColumnSpec {
    String name();
    MajorType type();
    void setType(MajorType type);
    String defaultValue();
    ColumnMetadata metadata();
  }

  public static final MajorType DEFAULT_NULL_TYPE = MajorType.newBuilder()
      .setMinorType(MinorType.INT)
      .setMode(DataMode.OPTIONAL)
      .build();

  private final MajorType nullType;
  private final boolean allowRequired;

  public NullColumnLoader(ResultVectorCache vectorCache, List<? extends NullColumnSpec> defns,
      MajorType nullType, boolean allowRequired) {
    super(vectorCache);

    // Normally, null columns must be optional or arrays. However
    // we allow required columns either if the client requests it,
    // or if the client's requested null type is itself required.
    // (The generated "null column" vectors will go into the vector
    // cache and be pulled back out; we must preserve the required
    // mode in this case.

    this.allowRequired = allowRequired ||
        (nullType != null && nullType.getMode() == DataMode.REQUIRED);

    // Use the provided null type, else the standard nullable int.

    if (nullType == null) {
      this.nullType = DEFAULT_NULL_TYPE;
    } else {
      this.nullType = nullType;
    }

    // Populate the loader schema from that provided

    RowSetLoader schema = loader.writer();
    for (int i = 0; i < defns.size(); i++) {
      NullColumnSpec defn = defns.get(i);
      ColumnMetadata colSchema = selectType(defn);
      if (defn.metadata() != null) {
        colSchema.setProperties(defn.metadata().properties());
      } else if (defn.defaultValue() != null) {
        colSchema.setDefaultValue(defn.defaultValue());
      }
      schema.addColumn(colSchema);
    }
  }

  /**
   * Implements the type mapping algorithm; preferring the best fit
   * to preserve the schema, else resorting to changes when needed.
   * @param defn output column definition
   * @return type of the empty column that implements the definition
   */

  private ColumnMetadata selectType(NullColumnSpec defn) {

    // Prefer the type of any previous occurrence of
    // this column.

    MajorType type = vectorCache.getType(defn.name());

    // Else, use the type defined in the projection, if any.

    if (type == null) {
      type = defn.type();
    }
    if (type != null && ! allowRequired && type.getMode() == DataMode.REQUIRED && defn.defaultValue() == null) {

      // Type was found in the vector cache and the type is required.
      // The client determines whether to map required types to optional.
      // The text readers use required Varchar columns for missing columns,
      // and so no required-to-optional mapping is desired. Other readers
      // want to use nulls for missing columns, and so need the
      // required-to-nullable mapping.

      type = MajorType.newBuilder()
            .setMinorType(type.getMinorType())
            .setMode(DataMode.OPTIONAL)
            .build();
    }

    // Else, use the specified null type.

    if (type == null) {
      type = nullType;
    }

    // If the schema had the special NULL type, replace it with the
    // null column type.

    if (type.getMinorType() == MinorType.NULL) {
      type = nullType;
    }
    defn.setType(type);
    return MetadataUtils.newScalar(defn.name(), type);
  }

  public VectorContainer output() {
    return loader.outputContainer();
  }

  @Override
  public VectorContainer load(int rowCount) {
    loader.startBatch();
    loader.skipRows(rowCount);
    return loader.harvest();
  }
}
