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
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.DynamicColumn;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Projection filter based on the scan schema which typically starts as fully
 * dynamic, then becomes more concrete as the scan progresses. Enforces that
 * projected columns must be consistent with either projection, or the existing
 * concrete schema for that columns.
 */
public abstract class DynamicSchemaFilter implements ProjectionFilter {

  /**
   * Describes how to handle candidate columns not currently in the
   * scan schema, which turns out to be a surprisingly complex
   * question. At the top level, we add columns only if the query
   * contains a wildcard. But, within maps, there are additional
   * constraints: we can add new members to a map even if the query
   * itself does not contain a wildcard.
   */
  public enum NewColumnsMode {

    /**
     * No new columns are allowed at this level or in maps
     * below this level. Occurs when the schema is defined
     * or with a strict provided schema.
     */
    NONE,

    /**
     * New columns are allowed at this level and below.
     * Occurs in a wildcard projection in which there are no
     * constraints on the columns which can be added.
     */
    ALL,

    /**
     * New columns cannot be added at this level, but can be
     * added in maps below this level. Occurs in a query where
     * the projection list is explicit: {@code a, b, m}, and it
     * turns out that {@code m} is a map. A simple {@code m}
     * projection is logically equivalent to {@code m.*}.
     * <p>
     * This same logic can apply to maps if the project list contains
     * something like {@code m.a, m.m2}, and {@code m2} turns out
     * to be a map.
     */
    CHILD_ONLY
  }

  protected final CustomErrorContext errorContext;
  protected final String source;
  protected final NewColumnsMode newColumnsMode;

  public DynamicSchemaFilter(CustomErrorContext errorContext,
      String source, NewColumnsMode newColumnsMode) {
    this.errorContext = errorContext;
    this.source = source;
    this.newColumnsMode = newColumnsMode;
  }

  public ProjResult buildProjection(ColumnMetadata schemaCol, ColumnMetadata probeCol) {
    if (schemaCol == null) {
      return newColumnProjection();
    }
    if (schemaCol instanceof ProjectedColumn) {

      // Column comes from the project list
      return fromProjection((ProjectedColumn) schemaCol, probeCol);
    } else if (schemaCol instanceof DynamicColumn) {
      return PROJECTED;
    } else {

      // Column has a schema defined earlier.
      return fromSchema(schemaCol, probeCol);
    }
  }

  protected ProjResult newColumnProjection() {
    // No match. If this is an open schema, project the column
    // and its children, if any. If closed, don't project the column.
    return newColumnsMode == NewColumnsMode.ALL ? PROJECTED : NOT_PROJECTED;
  }

  /**
   * A column exists in the scan schema, and is dynamic. The proposed
   * column can be projected. First, however, we verify consistency.
   */
  private ProjResult fromProjection(ProjectedColumn projCol, ColumnMetadata probeCol) {

    // Verify that the reader/provided column is consistent with projection
    SchemaUtils.verifyCompatibility(projCol, probeCol, source, errorContext);

    if (projCol.isMap()) {

      // The projected column is a map (has named members). Track these to
      // project children.
      return new ProjResult(true, projCol, mapProjection(projCol));
    } else {

      // The projected column is generic. Harmlessly project all children
      // for both map and non-map columns.
      return new ProjResult(true, projCol, PROJECT_ALL);
    }
  }

  /**
   * A column exists in the scan schema, and is concrete. The proposed
   * column can be projected. Verify consistency. The reader should not be
   * proposing a column with the wrong type or mode since it was told the
   * reader input schema, and that schema was derived from a provided schema
   * (which should be acceptable to the reader) or by a prior reader in the
   * same scan.
   */
  protected ProjResult fromSchema(ColumnMetadata schemaCol,
      ColumnMetadata probeCol) {
    SchemaUtils.verifyConsistency(schemaCol, probeCol, source, errorContext);
    if (schemaCol.isMap()) {
      return new ProjResult(true, schemaCol, mapProjection(schemaCol));
    } else {
      return new ProjResult(true, schemaCol);
    }
  }

  private ProjectionFilter mapProjection(ColumnMetadata map) {
    return new DynamicTupleFilter(map.tupleSchema(),
        newColumnsMode != NewColumnsMode.NONE,
        errorContext, source);
  }

  @Override
  public boolean isProjected(String colName) {

    // To avoid duplicating logic, create a dynamic column
    // to run though the above checks.
    return projection(MetadataUtils.newDynamic(colName)).isProjected;
  }

  /**
   * Filter for a map, represented by a {@code TupleMetadata}.
   */
  public static class DynamicTupleFilter extends DynamicSchemaFilter {
    private final TupleMetadata mapSchema;

    public DynamicTupleFilter(TupleMetadata mapSchema, boolean isOpen,
        CustomErrorContext errorContext,
        String source) {
      super(errorContext, source, newColumnsMode(mapSchema, isOpen));
      this.mapSchema = mapSchema;
    }

    private static NewColumnsMode newColumnsMode(TupleMetadata projectionSet, boolean isOpen) {
      if (!isOpen) {
        return NewColumnsMode.NONE;
      } else if (SchemaUtils.isProjectAll(projectionSet)) {
        return NewColumnsMode.ALL;
      } else {
        return NewColumnsMode.CHILD_ONLY;
      }
    }

    public static ProjectionFilter filterFor(DynamicColumn col, boolean allowMapAdditions,
        CustomErrorContext errorContext, String source) {
      if (col.isMap()) {
        return new DynamicTupleFilter(col.tupleSchema(), allowMapAdditions, errorContext, source);
      } else {
        return PROJECT_ALL;
      }
    }

    public DynamicTupleFilter(TupleMetadata projectionSet, CustomErrorContext errorContext) {
      this(projectionSet, true, errorContext, "Reader");
    }

    @Override
    public ProjResult projection(ColumnMetadata col) {
      return buildProjection(mapSchema.metadata(col.name()), col);
    }

    @Override
    public boolean isEmpty() {
      return mapSchema.isEmpty();
    }
  }

  /**
   * Filter for the top-level dynamic schema.
   */
  public static class RowSchemaFilter extends DynamicSchemaFilter {
    private final MutableTupleSchema schema;

    public RowSchemaFilter(MutableTupleSchema schema, boolean allowMapChanges,
        CustomErrorContext errorContext) {
      super(errorContext, "Reader", newColumnsMode(schema, allowMapChanges));
      this.schema = schema;
    }

    private static NewColumnsMode newColumnsMode(MutableTupleSchema schema, boolean allowMapChanges) {
      if (schema.projectionType() == ProjectionType.ALL) {
        return NewColumnsMode.ALL;
      } else if (allowMapChanges) {
        return NewColumnsMode.CHILD_ONLY;
      } else {
        return NewColumnsMode.NONE;
      }
    }

    @Override
    public ProjResult projection(ColumnMetadata col) {
      ColumnHandle handle = schema.find(col.name());
      if (handle == null) {
        return newColumnProjection();
      }

      // Top-level columns can be implicit. Do not project a reader
      // column of the same name as an implicit column, even if this
      // is a wildcard projection.
      if (handle.isImplicit()) {
        logger.warn("Ignoring reader column with the same name as an implicit column: {}",
            col.name());
        return NOT_PROJECTED;
      }
      return buildProjection(handle.column(), col);
    }

    @Override
    public boolean isEmpty() {
      return schema.projectionType() == ScanSchemaTracker.ProjectionType.NONE;
    }
  }
}
