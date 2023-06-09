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
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanProjectionParser.ProjectionParseResult;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaResolver.SchemaType;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Schema tracker for the "normal" case in which schema starts from a simple
 * projection list of column names, optionally with a provided schema. The
 * schema evolves by locating implicit columns, then having he reader define
 * column types, and so on.
 */
public class ProjectionSchemaTracker extends AbstractSchemaTracker {

  private final TupleMetadata projection;
  private final boolean allowSchemaChange;
  private int implicitInsertPoint;
  private int readerSchemaCount;
  private boolean allowMapAdditions = true;

  public ProjectionSchemaTracker(TupleMetadata definedSchema,
      ProjectionParseResult parseResult,
      CustomErrorContext errorContext) {
    super(errorContext);
    this.projection = parseResult.dynamicSchema;
    this.allowSchemaChange = false;
    schema.copyFrom(definedSchema);
    validateProjection(parseResult.dynamicSchema, definedSchema);

    ProjectionType projType;
    if (schema.size() == 0) {
      projType = ProjectionType.NONE;
    } else {
      projType = ProjectionType.SOME;
    }
    schema.setProjectionType(projType);
    this.implicitInsertPoint = -1;
    checkResolved();
  }

  public ProjectionSchemaTracker(ProjectionParseResult parseResult, boolean allowSchemaChange,
      CustomErrorContext errorContext) {
    super(errorContext);
    this.projection = parseResult.dynamicSchema;
    this.allowSchemaChange = allowSchemaChange;
    this.schema.copyFrom(projection);

    // Work out the projection type: wildcard, empty, or explicit.
    ProjectionType projType;
    if (parseResult.isProjectAll()) {
      projType = ProjectionType.ALL;
    } else if (projection.isEmpty()) {
      projType = ProjectionType.NONE;
      this.isResolved = true;
      this.allowMapAdditions = false;
    } else {
      projType = ProjectionType.SOME;
    }
    this.schema.setProjectionType(projType);

    // If wildcard, record the wildcard position.
    this.schema.setInsertPoint(parseResult.wildcardPosn);
    this.implicitInsertPoint = parseResult.wildcardPosn;
  }

  @Override
  public ProjectedColumn columnProjection(String colName) {
    return (ProjectedColumn) projection.metadata(colName);
  }

  public void applyProvidedSchema(TupleMetadata providedSchema) {
    boolean isStrict = SchemaUtils.isStrict(providedSchema);
    new ScanSchemaResolver(schema,
        isStrict ? SchemaType.STRICT_PROVIDED_SCHEMA : SchemaType.LENIENT_PROVIDED_SCHEMA,
        true, errorContext)
      .applySchema(providedSchema);
    checkResolved();
    if (isStrict) {
      allowMapAdditions = false;
    }
  }

  @Override
  public void applyEarlyReaderSchema(TupleMetadata readerSchema) {
    new ScanSchemaResolver(schema, SchemaType.EARLY_READER_SCHEMA, true, errorContext)
      .applySchema(readerSchema);
    checkResolved();
  }

  /**
   * Set up a projection filter using the reader input schema returned
   * from {@link #readerInputSchema()}.
   * <ul>
   * <li>If this is an empty projection (@{code SELECT COUNT(*)}), then
   * noting can be projected at all.</li>
   * <li>If this is an explicit projection (@code SELECT a, b)}, then
   * the set of top-level columns is fixed, though the types are unknown.
   * Maps allow new members depending on the map projection: a generic
   * projection ({@code m}) allows new members, a specific projection
   * ({@code m.a, m.b}) does not allow new members.</li>
   * <li>If the schema has been resolved and is now fixed (closed), then
   * no new columns are allowed either at the top level or in maps.</li>
   * <li>If the schema is open ({@code SELECT *} for the first reader,
   * or schema change is allowed in the second reader), and we have
   * no columns, then just project everything.</li>
   * <li>If the schema is open, but we have seen some columns, then
   * columns can still be added, but existing columns must match the
   * existing schema.</li>
   * </ul>
   * <p>
   * Static filters handle the simple "none" and "starting from nothing
   * all" cases. The dynamic schema filter handles the case of existing
   * columns whether dynamic or static.
   */
  @Override
  public ProjectionFilter projectionFilter(CustomErrorContext errorContext) {
    switch (projectionType()) {
      case ALL:

        // Empty schema implies we've only seen the wildcard this far.
        if (schema.size() == 0) {
          return ProjectionFilter.PROJECT_ALL;
        }
        break;
      case NONE:
        return ProjectionFilter.PROJECT_NONE;
      default:
    }
    return new RowSchemaFilter(schema, allowMapAdditions, errorContext);
  }

  @Override
  public void applyReaderSchema(TupleMetadata readerOutputSchema,
      CustomErrorContext errorContext) {
    SchemaType schemaType;

    // The first reader can reposition columns projected with a wildcard,
    // other readers cannot as we want to preserve column order after the
    // first batch.
    if (readerSchemaCount == 0 && allowSchemaChange) {
      schemaType = SchemaType.FIRST_READER_SCHEMA;
    } else {
      schemaType = SchemaType.READER_SCHEMA;
    }
    new ScanSchemaResolver(schema, schemaType, allowMapAdditions, errorContext)
        .applySchema(readerOutputSchema);
    if (!allowSchemaChange) {
      allowMapAdditions = false;
      if (projectionType() == ProjectionType.ALL) {
        schema.setProjectionType(ProjectionType.SOME);
      }
    }
    checkResolved();
    readerSchemaCount++;
  }

  @Override
  public void expandImplicitCol(ColumnMetadata resolved, ImplicitColumnMarker marker) {
    schema.insert(implicitInsertPoint++, resolved).markImplicit(marker);
  }
}
