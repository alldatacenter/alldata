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
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnMarker;
import org.apache.drill.exec.physical.impl.scan.v3.schema.DynamicSchemaFilter.RowSchemaFilter;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Simple "tracker" based on a defined, fixed schema. The only resolution
 * needed is to identify which columns, if any, are implicit. Readers are
 * ignorant of this state: readers may still produce a subset of the defined
 * columns and so missing columns may be needed. However, readers cannot
 * add to the set of output columns, nor change their types.
 */
public class SchemaBasedTracker extends AbstractSchemaTracker {

  private final TupleMetadata definedSchema;

  public SchemaBasedTracker(TupleMetadata definedSchema, CustomErrorContext errorContext) {
    super(errorContext);
    this.definedSchema = definedSchema;
    schema.copyFrom(definedSchema);

    ScanSchemaTracker.ProjectionType projType;
    if (schema.size() == 0) {
      projType = ScanSchemaTracker.ProjectionType.NONE;
    } else {
      projType = ScanSchemaTracker.ProjectionType.SOME;
    }
    schema.setProjectionType(projType);
    checkResolved();

    // If not resolved, should not have used this tracker.
    Preconditions.checkState(isResolved);
  }

  /**
   * Validate a projection list (provided as an argument) against a
   * defined schema already held by this tracker. Ensures that, when we
   * have both a defined schema and projection list, that they are
   * consistent.
   *
   * @param projection the parsed projection list
   */
  public void validateProjection(TupleMetadata projection) {
    if (projection != null) {
      validateProjection(projection, definedSchema);
    }
  }

  @Override
  public void applyEarlyReaderSchema(TupleMetadata readerSchema) { }

  /**
   * Set up a projection filter using the defined schema
   */
  @Override
  public ProjectionFilter projectionFilter(CustomErrorContext errorContext) {
    switch (projectionType()) {
      case NONE:
        return ProjectionFilter.PROJECT_NONE;
      case SOME:
        return new RowSchemaFilter(schema, false, errorContext);
      default:
        throw new IllegalStateException(projectionType().name());
    }
  }

  @Override
  public void applyReaderSchema(TupleMetadata readerOutputSchema,
      CustomErrorContext errorContext) {
    // TODO: Validate reader output is a subset of the schema
  }

  @Override
  public void expandImplicitCol(ColumnMetadata resolved, ImplicitColumnMarker marker) {
    throw new IllegalStateException("Can't expand a defined schema.");
  }

  @Override
  public int schemaVersion() { return 1; }

  @Override
  public ProjectedColumn columnProjection(String colName) { return null; }
}
