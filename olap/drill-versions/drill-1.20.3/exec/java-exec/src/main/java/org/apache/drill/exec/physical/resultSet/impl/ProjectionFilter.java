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
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.v3.schema.SchemaUtils;
import org.apache.drill.exec.physical.resultSet.project.RequestedTuple;
import org.apache.drill.exec.physical.resultSet.project.RequestedTuple.TupleProjectionType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a variety of ways to filter columns: no filtering, filter
 * by (parsed) projection list, or filter by projection list and
 * provided schema. Enforces consistency of actual reader schema and
 * projection list and/or provided schema.
 * <p>
 * Projection filters should not need to be extensible; filtering
 * depends only on projection and provided schema.
 */
public interface ProjectionFilter {
  Logger logger = LoggerFactory.getLogger(ProjectionFilter.class);

  ProjectionFilter PROJECT_ALL = new ImplicitProjectionFilter(true);
  ProjectionFilter PROJECT_NONE = new ImplicitProjectionFilter(false);
  ProjResult NOT_PROJECTED = new ProjResult(false, null, PROJECT_NONE);
  ProjResult PROJECTED = new ProjResult(true, null, PROJECT_ALL);

  class ProjResult {
    public final boolean isProjected;
    public final ColumnMetadata projection;
    public final ProjectionFilter mapFilter;

    public ProjResult(boolean isProjected) {
      this(isProjected, null, null);
    }

    public ProjResult(boolean isProjected, ColumnMetadata projection) {
      this(isProjected, projection, null);
    }

    public ProjResult(boolean isProjected, ColumnMetadata projection, ProjectionFilter mapFilter) {
      this.isProjected = isProjected;
      this.projection = projection;
      this.mapFilter = mapFilter;
    }
  }

  ProjResult projection(ColumnMetadata columnSchema);

  boolean isProjected(String colName);

  boolean isEmpty();

  static ProjectionFilter projectionFilter(RequestedTuple tupleProj,
      CustomErrorContext errorContext) {
    switch (tupleProj.type()) {
      case ALL:
        return PROJECT_ALL;
      case NONE:
        return PROJECT_NONE;
      default:
        return new DirectProjectionFilter(tupleProj, errorContext);
    }
  }

  static ProjectionFilter providedSchemaFilter(RequestedTuple tupleProj,
      TupleMetadata providedSchema, CustomErrorContext errorContext) {
    if (tupleProj.type() == TupleProjectionType.NONE) {
      return PROJECT_NONE;
    }
    if (providedSchema == null) {
      return projectionFilter(tupleProj, errorContext);
    }
    boolean strict = SchemaUtils.isStrict(providedSchema);
    if (providedSchema.isEmpty()) {
      if (strict) {
        return PROJECT_NONE;
      } else {
        return projectionFilter(tupleProj, errorContext);
      }
    }
    ProjectionFilter schemaFilter = strict ?
        new SchemaProjectionFilter(providedSchema, errorContext) :
        new TypeProjectionFilter(providedSchema, errorContext);
    return new CompoundProjectionFilter(
        new DirectProjectionFilter(tupleProj, errorContext),
        schemaFilter);
  }

  static ProjectionFilter definedSchemaFilter(
      TupleMetadata definedSchema, CustomErrorContext errorContext) {
    if (definedSchema.isEmpty()) {
      return PROJECT_NONE;
    } else {
      return new SchemaProjectionFilter(definedSchema, errorContext);
    }
  }

  /**
   * Implied projection: either project all or project none. Never
   * projects special columns (those marked as not being expanded in
   * SELECT *).
   */
  class ImplicitProjectionFilter implements ProjectionFilter {
    private final boolean projectAll;

    public ImplicitProjectionFilter(boolean projectAll) {
      this.projectAll = projectAll;
    }

    @Override
    public ProjResult projection(ColumnMetadata col) {
      if (SchemaUtils.isExcludedFromWildcard(col)) {
        return NOT_PROJECTED;
      } else {
        return projectAll ? PROJECTED : NOT_PROJECTED;
      }
    }

    @Override
    public boolean isProjected(String name) {
      return projectAll;
    }

    @Override
    public boolean isEmpty() {
      return !projectAll;
    }
  }

  /**
   * Projection filter based on the (parsed) projection list. Enforces that
   * the reader column is consistent with the form of projection (map,
   * array, or plain) in the projection list.
   */
  class DirectProjectionFilter implements ProjectionFilter {
    private final RequestedTuple projectionSet;
    private final CustomErrorContext errorContext;

    public DirectProjectionFilter(RequestedTuple projectionSet, CustomErrorContext errorContext) {
      this.projectionSet = projectionSet;
      this.errorContext = errorContext;
    }

    @Override
    public ProjResult projection(ColumnMetadata col) {
      if (projectionSet.enforceProjection(col, errorContext)) {
        return new ProjResult(true, null,
            projectionFilter(projectionSet.mapProjection(col.name()), errorContext));
      } else {
        return NOT_PROJECTED;
      }
    }

    @Override
    public boolean isProjected(String colName) {
      return projectionSet.isProjected(colName);
    }

    @Override
    public boolean isEmpty() {
      return projectionSet.isEmpty();
    }
  }

  /**
   * Schema-based projection.
   */
  abstract class BaseSchemaProjectionFilter implements ProjectionFilter {
    protected final TupleMetadata schema;
    protected final CustomErrorContext errorContext;

    private BaseSchemaProjectionFilter(TupleMetadata schema, CustomErrorContext errorContext) {
      this.schema = schema;
      this.errorContext = errorContext;
    }

    protected void validateColumn(ColumnMetadata schemaCol, ColumnMetadata readerCol) {
      if (schemaCol.isDynamic()) {
        return;
      }
      if (schemaCol.type() != readerCol.type() ||
          schemaCol.mode() != readerCol.mode()) {
        throw UserException.validationError()
          .message("Reader and scan column type conflict")
          .addContext("Scan column", schemaCol.columnString())
          .addContext("Reader column", readerCol.columnString())
          .addContext(errorContext)
          .build(logger);
      }
    }

    protected void validateMap(ColumnMetadata schemaCol) {
      if (!schemaCol.isMap()) {
        throw UserException.validationError()
          .message("Reader expected a map column, but the the schema column is not a map")
          .addContext("Provided column", schemaCol.columnString())
          .addContext("Reader column", schemaCol.name())
          .addContext(errorContext)
          .build(logger);
      }
    }

    @Override
    public boolean isEmpty() {
       return schema.isEmpty();
    }
  }

  /**
   * Projection based on a non-strict provided schema which enforces the type of known
   * columns, but has no opinion about additional columns.
   * <p>
   * If the column is found, enforces that the reader schema has the same type and
   * mode as the provided column.
   */
  class TypeProjectionFilter extends BaseSchemaProjectionFilter {

    public TypeProjectionFilter(TupleMetadata providedSchema, CustomErrorContext errorContext) {
      super(providedSchema, errorContext);
    }

    @Override
    public ProjResult projection(ColumnMetadata col) {
      ColumnMetadata providedCol = schema.metadata(col.name());
      if (providedCol == null) {
        return PROJECTED;
      } else {
        validateColumn(providedCol, col);
        if (providedCol.isMap()) {
          return new ProjResult(true, providedCol,
              new TypeProjectionFilter(providedCol.tupleSchema(), errorContext));
        } else {
          return new ProjResult(true, providedCol);
        }
      }
    }

    @Override
    public boolean isProjected(String name) {
      return true;
    }
  }

  /**
   * Projection filter in which a schema exactly defines the set of allowed
   * columns, and their types.
   */
  class SchemaProjectionFilter extends BaseSchemaProjectionFilter {

    public SchemaProjectionFilter(TupleMetadata definedSchema, CustomErrorContext errorContext) {
      super(definedSchema, errorContext);
    }

    @Override
    public ProjResult projection(ColumnMetadata col) {
      ColumnMetadata providedCol = schema.metadata(col.name());
      if (providedCol == null) {
        return NOT_PROJECTED;
      } else {
        validateColumn(providedCol, col);
        if (providedCol.isMap()) {
          return new ProjResult(true, providedCol,
              new SchemaProjectionFilter(providedCol.tupleSchema(), errorContext));
        } else {
          return new ProjResult(true, providedCol);
        }
      }
    }

    @Override
    public boolean isProjected(String name) {
      return schema.metadata(name) != null;
    }
  }

  /**
   * Compound filter for combining direct and provided schema projections.
   */
  class CompoundProjectionFilter implements ProjectionFilter {
    private final ProjectionFilter filter1;
    private final ProjectionFilter filter2;

    public CompoundProjectionFilter(ProjectionFilter filter1, ProjectionFilter filter2) {
      this.filter1 = filter1;
      this.filter2 = filter2;
    }

    @Override
    public ProjResult projection(ColumnMetadata col) {
      ProjResult result1 = filter1.projection(col);
      ProjResult result2 = filter2.projection(col);
      if (!result1.isProjected || !result2.isProjected) {
        return NOT_PROJECTED;
      }
      if (result1.mapFilter == null && result2.mapFilter == null) {
        return result1;
      }
      if (result1.mapFilter == PROJECT_ALL) {
        return result2;
      }
      if (result2.mapFilter == PROJECT_ALL) {
        return result1;
      }

      return new ProjResult(true,
          result1.projection == null ? result2.projection : result1.projection,
          new CompoundProjectionFilter(result1.mapFilter, result2.mapFilter));
    }

    @Override
    public boolean isProjected(String name) {
      return filter1.isProjected(name) && filter2.isProjected(name);
    }

    @Override
    public boolean isEmpty() {
      return filter1.isEmpty() || filter2.isEmpty();
    }
  }
}
