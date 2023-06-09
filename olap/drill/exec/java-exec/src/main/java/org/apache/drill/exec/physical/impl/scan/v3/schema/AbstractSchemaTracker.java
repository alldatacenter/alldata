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
package org.apache.drill.exec.physical.impl.scan.v3.schema;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.physical.impl.scan.v3.schema.MutableTupleSchema.ColumnHandle;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaResolver.SchemaType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;

/**
 * Base class for the projection-based and defined-schema-based
 * scan schema trackers.
 */
public abstract class AbstractSchemaTracker implements ScanSchemaTracker {

  protected final CustomErrorContext errorContext;
  protected final MutableTupleSchema schema = new MutableTupleSchema();
  protected boolean isResolved;
  private TupleMetadata outputSchema;
  private int outputSchemaVersion;

  public AbstractSchemaTracker(CustomErrorContext errorContext) {
    this.errorContext = errorContext;
  }

  /**
   * Validate a projection list against a defined-schema tuple. Recursively walks
   * the tree of maps to validate all nested tuples.
   *
   * @param projection the parsed projection list
   * @param schema the defined schema to validate against
   */
  protected static void validateProjection(TupleMetadata projection, TupleMetadata schema) {
    if (projection == null || SchemaUtils.isProjectAll(projection)) {
      return;
    }
    if (schema.size() != projection.size()) {
      throw new IllegalArgumentException("Defined schema and projection list do not match");
    }
    for (ColumnMetadata reqCol : projection) {
      ColumnMetadata schemaCol = schema.metadata(reqCol.name());
      if (schemaCol == null) {
        throw new IllegalArgumentException(String.format(
            "Defined schema and projection list do not match. " +
            "`%s` in project list, but not in defined schema",
            reqCol.name()));
      }
      if (schemaCol.isMap()) {
        validateProjection(reqCol.tupleSchema(), schemaCol.tupleSchema());
      }
    }
  }

  @Override
  public ProjectionType projectionType() { return schema.projectionType(); }

  @Override
  public CustomErrorContext errorContext() { return errorContext; }

  @Override
  public MutableTupleSchema internalSchema() { return schema; }

  @Override
  public boolean isResolved() { return isResolved; }

  @Override
  public int schemaVersion() { return schema.version(); }

  /**
   * Determine if the schema is resolved. It is resolved if the
   * schema itself is resolved. Since an empty schema is resolved, for the
   * {@code SELECT *} case, we require at least one column, which means
   * that something (provided schema, early reader schema) has provided
   * us with a schema. Once resolved, a schema can never become
   * unresolved: readers are not allowed to add dynamic columns.
   */
  protected void checkResolved() {
    if (isResolved) {
      return;
    }
    switch (projectionType()) {
    case ALL:
      isResolved = !schema.isEmpty() && schema.isResolved();
      break;
    default:
      isResolved = schema.isResolved();
    }
  }

  @Override
  public TupleMetadata applyImplicitCols() {
    checkResolved();
    if (projectionType() == ProjectionType.SOME && allColumnsAreImplicit()) {
      schema.setProjectionType(ProjectionType.NONE);
    }
    return implicitColumns();
  }

  private boolean allColumnsAreImplicit() {
    for (ColumnHandle handle : schema.columns()) {
      if (!handle.isImplicit()) {
        return false;
      }
    }
    return true;
  }

  private TupleMetadata implicitColumns() {
    TupleMetadata implicitCols = new TupleSchema();
    for (ColumnHandle handle : schema.columns()) {
      if (handle.isImplicit()) {
        handle.setIndex(implicitCols.size());
        implicitCols.addColumn(handle.column());
      }
    }
    return implicitCols;
  }

  @Override
  public TupleMetadata readerInputSchema() {
    TupleMetadata readerInputSchema = new TupleSchema();
    for (ColumnHandle handle : schema.columns()) {
      if (!handle.isImplicit()) {
        readerInputSchema.addColumn(handle.column());
      }
    }
    return readerInputSchema;
  }

  @Override
  public TupleMetadata missingColumns(TupleMetadata readerOutputSchema) {
    TupleMetadata missingCols = new TupleSchema();
    for (ColumnHandle handle : schema.columns()) {
      if (handle.isImplicit()) {
        continue;
      }
      ColumnMetadata readerCol = readerOutputSchema.metadata(handle.column().name());
      if (readerCol == null) {
        missingCols.addColumn(handle.column());
      } else if (readerCol.isMap()) {
        ColumnMetadata diff = MetadataUtils.diffMap(handle.column(), readerCol);
        if (diff != null) {
          missingCols.addColumn(diff);
        }
      }
    }
    return missingCols;
  }

  @Override
  public void resolveMissingCols(TupleMetadata missingCols) {
    new ScanSchemaResolver(schema, SchemaType.MISSING_COLS, false, errorContext)
        .applySchema(missingCols);
    checkResolved();
  }

  @Override
  public TupleMetadata outputSchema() {
    if (outputSchema == null || outputSchemaVersion < schema.version()) {
      outputSchema = buildOutputSchema();
    }
    return outputSchema;
  }

  private TupleMetadata buildOutputSchema() {
    TupleMetadata outputSchema = new TupleSchema();
    for (ColumnHandle handle : schema.columns()) {
      outputSchema.addColumn(handle.column());
    }
    return outputSchema;
  }
}
