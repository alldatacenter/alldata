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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.physical.impl.scan.project.NullColumnLoader.NullColumnSpec;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Manages null columns by creating a null column loader for each
 * set of non-empty null columns. This class acts as a scan-wide
 * facade around the per-schema null column loader.
 */

public class NullColumnBuilder implements VectorSource {

  public static class NullBuilderBuilder {
    protected MajorType nullType;
    protected boolean allowRequiredNullColumns;
    protected TupleMetadata outputSchema;

    public NullBuilderBuilder setNullType(MajorType nullType) {
      this.nullType = nullType;
      return this;
    }

    public NullBuilderBuilder allowRequiredNullColumns(boolean flag) {
      allowRequiredNullColumns = flag;
      return this;
    }

    public NullBuilderBuilder setOutputSchema(TupleMetadata outputSchema) {
      this.outputSchema = outputSchema;
      return this;
    }

    public NullColumnBuilder build() {
      return new NullColumnBuilder(this);
    }
  }

  /**
   * Creates null columns if needed.
   */

  protected final List<NullColumnSpec> nullCols = new ArrayList<>();
  private NullColumnLoader nullColumnLoader;
  private VectorContainer outputContainer;
  protected TupleMetadata outputSchema;

  /**
   * The reader-specified null type if other than the default.
   */

  private final MajorType nullType;
  private final boolean allowRequiredNullColumns;

  public NullColumnBuilder(NullBuilderBuilder builder) {
    this.nullType = builder.nullType;
    this.allowRequiredNullColumns = builder.allowRequiredNullColumns;
    this.outputSchema = builder.outputSchema;
  }

  public NullColumnBuilder newChild(String mapName) {
    NullBuilderBuilder builder = new NullBuilderBuilder()
        .setNullType(nullType)
        .allowRequiredNullColumns(allowRequiredNullColumns);

    // Pass along the schema of the child map if 1) we have an output schema,
    // 2) the column is defined in that schema, and 3) the column is a map.
    if (outputSchema != null) {
      ColumnMetadata colSchema = outputSchema.metadata(mapName);
      if (colSchema != null) {
        builder.setOutputSchema(colSchema.tupleSchema());
      }
    }
    return builder.build();
  }

  public ResolvedNullColumn add(String name) {
    return add(name, null);
  }

  public ResolvedNullColumn add(ColumnMetadata colDefn) {
    final ResolvedNullColumn col = new ResolvedNullColumn(
        colDefn, this, nullCols.size());
    nullCols.add(col);
    return col;
  }

  public ResolvedNullColumn add(String name, MajorType type) {

    // If type is provided, use it. (Used during schema smoothing.)
    // Else if there is an output schema, and the column appears in
    // that schema, use that type.

    ResolvedNullColumn col = null;
    if (outputSchema != null) {
      ColumnMetadata outputCol = outputSchema.metadata(name);
      if (outputCol != null) {
        if (type == null) {

          // No preferred type, so no conflict

          col = new ResolvedNullColumn(outputCol, this, nullCols.size());
        } else if (type.getMinorType() != outputCol.type()) {

          // Conflict in type, can't use default value.

        } else if (type.getMode() != outputCol.mode()) {

          // Conflict in mode. Use the specified name and type, but
          // use the default value from the output schema.

          col = new ResolvedNullColumn(name, type,
              outputCol.defaultValue(), this, nullCols.size());
        } else {

          // Type and modes matches, just the output column

          col = new ResolvedNullColumn(outputCol, this, nullCols.size());
        }
      }
    }
    if (col == null) {
      col = new ResolvedNullColumn(name, type, null, this, nullCols.size());
    }
    nullCols.add(col);
    return col;
  }

  public void build(ResultVectorCache vectorCache) {
    close();

    // If no null columns for this schema, no need to create
    // the loader.

    if (hasColumns()) {
      nullColumnLoader = new NullColumnLoader(vectorCache, nullCols, nullType, allowRequiredNullColumns);
      outputContainer = nullColumnLoader.output();
    }
  }

  public boolean hasColumns() {
    return nullCols != null && ! nullCols.isEmpty();
  }

  public void load(int rowCount) {
    if (nullColumnLoader != null) {
      final VectorContainer output = nullColumnLoader.load(rowCount);
      assert output == outputContainer;
    }
  }

  @Override
  public ValueVector vector(int index) {
    return outputContainer.getValueVector(index).getValueVector();
  }

  @VisibleForTesting
  public VectorContainer output() { return outputContainer; }

  public void close() {
    if (nullColumnLoader != null) {
      nullColumnLoader.close();
      nullColumnLoader = null;
    }
    if (outputContainer != null) {
      outputContainer.clear();
      outputContainer = null;
    }
  }
}
