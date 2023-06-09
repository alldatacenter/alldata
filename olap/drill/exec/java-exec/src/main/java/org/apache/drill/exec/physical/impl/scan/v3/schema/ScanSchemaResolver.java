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
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.v3.schema.DynamicSchemaFilter.DynamicTupleFilter;
import org.apache.drill.exec.physical.impl.scan.v3.schema.MutableTupleSchema.ColumnHandle;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.ProjResult;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.DynamicColumn;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves a schema against the existing scan schema.
 * Expands columns by comparing the existing scan schema with
 * a "revised" (provided or reader) schema, adjusting the scan schema
 * accordingly. Maps are expanded recursively. Other columns
 * must match types (concrete columns) or the type must match projection
 * (for dynamic columns.)
 * <ul>
 * <li>Resolves a provided schema against the projection list.
 * The provided schema can be strict (converts a wildcard into
 * an explicit projection) or lenient (the reader can add
 * additional columns to a wildcard.)</li>
 * <li>Resolves an early reader schema against the projection list
 * and optional provided schema.</li>
 * <li>Resolves a reader output schema against a dynamic (projection
 * list), concreted (provided or prior reader) schema) or combination.
 * </ul>
 * <p>
 * In practice, the logic is simpler: given a schema (dynamic, concrete
 * or combination), further resolve the schema using the input schema
 * provided. Resolve dynamic columns, verify consistency of concrete
 * columns.
 * <p>
 * Projected columns start as <i>dynamic</i> (no type). Columns
 * are resolved to a known type as a schema identifies that type.
 * Subsequent schemas are obligated to use that same type to avoid
 * an inconsistent schema change downstream.
 * <p>
 * Expands columns by comparing the existing scan schema with
 * a "revised" (provided or reader) schema, adjusting the scan schema
 * accordingly. Maps are expanded recursively. Other columns
 * must match types (concrete columns) or the type must match projection
 * (for dynamic columns.)
 * <p>
 * A "resolved" projection list is a list of concrete columns: table
 * columns, nulls, file metadata or partition metadata. An unresolved list
 * has either table column names, but no match, or a wildcard column.
 * <p>
 * The idea is that the projection list moves through stages of resolution
 * depending on which information is available. An "early schema" table
 * provides schema information up front, and so allows fully resolving
 * the projection list on table open. A "late schema" table allows only a
 * partially resolved projection list, with the remainder of resolution
 * happening on the first (or perhaps every) batch.
 */
public class ScanSchemaResolver {
  private static final Logger logger = LoggerFactory.getLogger(ScanSchemaResolver.class);

  /**
   * Indicates the source of the schema to be analyzed.
   * Each schema type has subtly different rules. The
   * schema type allows us to inject those differences inline
   * within the resolution process. Also, each schema carries
   * a tag used for error reporting.
   */
  public enum SchemaType {
    STRICT_PROVIDED_SCHEMA("Provided"),
    LENIENT_PROVIDED_SCHEMA("Provided"),
    EARLY_READER_SCHEMA("Reader"),
    FIRST_READER_SCHEMA("Reader"),
    READER_SCHEMA("Reader"),
    MISSING_COLS("Missing columns");

    private final String source;

    SchemaType(String source) {
      this.source = source;
    }

    public String source() {
      return source;
    }
  }

  private final MutableTupleSchema schema;
  private final SchemaType mode;
  private final boolean allowMapAdditions;
  private final String source;
  private final CustomErrorContext errorContext;
  private final boolean allowColumnReorder;

  public ScanSchemaResolver(MutableTupleSchema schema, SchemaType mode,
      boolean allowMapAdditions,
      CustomErrorContext errorContext) {
    this.schema = schema;
    this.mode = mode;
    this.errorContext = errorContext;
    this.allowMapAdditions = allowMapAdditions;
    this.source = mode.source();
    switch (mode) {
      case STRICT_PROVIDED_SCHEMA:
      case LENIENT_PROVIDED_SCHEMA:
      case EARLY_READER_SCHEMA:
      case FIRST_READER_SCHEMA:

        // Allow reordering columns with projection is of the form
        // *, foo. Move bar to its place in the schema (foo, bar) rather
        // than at the end, as with implicit columns.
        this.allowColumnReorder = schema.projectionType() == ProjectionType.ALL;
        break;
      default:
        this.allowColumnReorder = false;
    }
  }

  public void applySchema(TupleMetadata sourceSchema) {
    switch (schema.projectionType()) {
      case ALL:
        projectSchema(sourceSchema);
        if (mode == SchemaType.STRICT_PROVIDED_SCHEMA) {
          schema.setProjectionType(ScanSchemaTracker.ProjectionType.SOME);
        }
        break;
      case SOME:
        projectSchema(sourceSchema);
        break;
      default:
        // Do nothing
    }
  }

  /**
   * A project list can contain implicit columns in addition to the wildcard.
   * The wildcard defines the <i>insert point</i>: the point at which
   * reader-defined columns are inserted as found. This version applies a
   * provided schema to a projection. If we are given a query of the form
   * {@code SELECT * FROM foo ORDER BY bar}, Drill will give us a projection
   * list of the form {@code [`**`, `bar`]} and normal projection processing
   * will project all provided columns, except {@code bar}, in place of the
   * wildcard. Since this behavior differs from all other DBs, we apply special
   * processing, we move the projection column into the next wildcard position
   * as if Drill did not include the extra column projection. This is a hack,
   * but one that helps with ease-of-use. We apply the same rule to the first
   * reader schema for the same reason.
   */
  private void projectSchema(TupleMetadata sourceSchema) {
    for (ColumnMetadata colSchema : sourceSchema) {
      ColumnHandle existing = schema.find(colSchema.name());
       if (existing == null) {
        insertColumn(colSchema);
      } else {
        mergeColumn(existing, colSchema);
        if (allowColumnReorder) {
          schema.moveIfExplicit(colSchema.name());
        }
      }
    }
  }

  /**
   * Insert a new column into the schema at the wildcard insertion point.
   * Columns can generally only be inserted for a wildcard, and only when
   * schema change is allowed. A number of special cases occur for the
   * various kinds of schemas.
   */
  private void insertColumn(ColumnMetadata col) {
    switch (mode) {
      case FIRST_READER_SCHEMA:
      case READER_SCHEMA:
        if (schema.projectionType() != ProjectionType.ALL) {
          throw new IllegalStateException(
              "Reader should not have projected an unprojected column: " + col.name());
        }
        break;
      case EARLY_READER_SCHEMA:
      case LENIENT_PROVIDED_SCHEMA:
      case STRICT_PROVIDED_SCHEMA:
        if (schema.projectionType() != ProjectionType.ALL || SchemaUtils.isExcludedFromWildcard(col)) {
          return;
        }
        break;
      case MISSING_COLS:
        throw new IllegalStateException("Missing columns should not add new columns");
      default:
        throw new IllegalStateException(mode.name());
    }
    ColumnMetadata copy = col.copy();
    schema.insert(copy);

    // This is a top-level column inserted from a wildcard. If the column
    // is a map, then project all its members. But, don't do that for
    // a strict schema. ("Strict" means only allow members in the provided
    // schema.)
    if (copy.isMap() && mode != SchemaType.STRICT_PROVIDED_SCHEMA) {
      SchemaUtils.markProjectAll(copy);
    }
  }

  /**
   * Merge an incoming column with an existing column which can either be
   * dynamic or concrete. Special cases occur for implicit columns which are
   * independent of reader schema, but which reside in the same namespace,
   * causing potential conflicts.
   */
  private void mergeColumn(ColumnHandle col, ColumnMetadata colSchema) {
    switch (mode) {
      case LENIENT_PROVIDED_SCHEMA:
      case STRICT_PROVIDED_SCHEMA:
        // Even with a wildcard, the planner may add additional columns.
        // Example SELECT * FROM foo ORDER BY bar
        // The planner will provide us with [`*`, `bar`]
        break;
      case EARLY_READER_SCHEMA:
        // If the reader offers a column which duplicates an implicit column,
        // act as if the column is not projected, but give a warning since
        // the user might expect the column to be projected in a wildcard.
        if (col.isImplicit()) {
           logger.warn("Column {} shadows an implicit column of the same name: ignored",
              colSchema.name());
           return;
        }
        break;
      default:
        // The reader should not project a column with the same name as an
        // implicit column. The projection filter should have prevented it.
        // If projection does occur, we cannot accept the column and have
        // no way to dispose of the unwanted vector.
        if (col.isImplicit()) {
          throw UserException.validationError()
            .message("Reader column conflicts an implicit column, should not have been projected")
            .addContext("Column", colSchema.name())
            .addContext(errorContext)
            .build(logger);
        }
    }
    if (col.column().isDynamic()) {
      mergeColumnWithDynamic(col, colSchema);
    } else {
      mergeWithConcrete(col.column(), colSchema);
    }
  }

  /**
   * Merge a resolved column with a dynamic column (from the project list or a dynamic
   * defined schema). Verify consistency with the projection. Should have already
   * been done by the projection filter for reader output columns, done here for all
   * other schema types.
   */
  private void mergeColumnWithDynamic(ColumnHandle existing, ColumnMetadata revised) {
    DynamicColumn existingCol = (DynamicColumn) existing.column();
    if (existingCol instanceof ProjectedColumn) {
      SchemaUtils.verifyCompatibility((ProjectedColumn) existingCol, revised,
          source, errorContext);
    }
    if (existingCol.isMap() || revised.isMap()) {
      schema.replace(existing, createMap(existingCol, revised));
    } else {
      schema.resolve(existing, revised.copy());
    }
  }

  /**
   * Merge an incoming column with an existing resolved column. Non-map columns
   * must match. Maps are merged recursively.
   */
  private void mergeWithConcrete(ColumnMetadata existing,
      ColumnMetadata revised) {
    SchemaUtils.verifyConsistency(existing, revised, source, errorContext);
    if (existing.isMap()) {
      ProjectionFilter filter = new DynamicTupleFilter(existing.tupleSchema(),
          allowMapAdditions, errorContext, source);
      expandMapProjection(existing.tupleSchema(), filter, revised.tupleSchema());
    }
  }

  /**
   * Create a map column. The map might have an explicit projection
   * ({@code m.a, m.b}). To ensure consistency with reader behavior, use the same
   * projection filter as the reader to determine which provided or early reader schema
   * columns to project.
   */
  private ColumnMetadata createMap(DynamicColumn projection,
      ColumnMetadata revised) {
    return createMap(projection,
        DynamicTupleFilter.filterFor(projection, allowMapAdditions, errorContext, source),
        revised);
  }

  /**
   * Recursively create a map, including nested maps.
   */
  private ColumnMetadata createMap(DynamicColumn projection, ProjectionFilter filter,
      ColumnMetadata revised) {
    ColumnMetadata map = revised.cloneEmpty();
    SchemaUtils.mergeColProperties(map, projection);
    SchemaUtils.mergeColProperties(map, revised);
    copyDynamicMembers(map, projection);

    // When resolving a generic column to a map, the map is treated
    // as "map.*". That is, we can add additional columns later
    // (assuming the scan allows it.) However, if this is a strict schema,
    // then strict means no additional columns are allowed.
    if (!projection.isMap() && mode != SchemaType.STRICT_PROVIDED_SCHEMA) {
      SchemaUtils.markProjectAll(map);
    }
    expandMapProjection(map.tupleSchema(), filter, revised.tupleSchema());
    return map;
  }

  private void copyDynamicMembers(ColumnMetadata map, DynamicColumn projection) {
    if (projection.isMap()) {
      TupleMetadata mapSchema = map.tupleSchema();
      for (ColumnMetadata col : projection.tupleSchema()) {
        mapSchema.addColumn(col.copy());
      }
    }
  }

 /**
   * Given an existing map, a projection filter, and an actual
   * reader output, update the existing map with the reader schema,
   * gated on the projection schema. Note that the projection schema
   * may not be needed it the reader schema followed the projection
   * filter which was based on the projection map.
   */
  private void expandMapProjection(TupleMetadata scanSchema,
      ProjectionFilter filter,
      TupleMetadata revisedSchema) {
    for (ColumnMetadata readerCol : revisedSchema) {
      resolveMember(scanSchema, filter.projection(readerCol), readerCol);
    }
  }

  private void resolveMember(TupleMetadata scanSchema, ProjResult result,
      ColumnMetadata readerCol) {
    ColumnMetadata schemaCol = result.projection;
    if (!result.isProjected) {
      switch (mode) {
      case EARLY_READER_SCHEMA:
      case LENIENT_PROVIDED_SCHEMA:
      case STRICT_PROVIDED_SCHEMA:
        break;
      case FIRST_READER_SCHEMA:
      case READER_SCHEMA:
        if (!allowMapAdditions) {
          throw new IllegalStateException("Reader should not have projected column: " + readerCol.name());
        }
        break;
      default:
        throw new IllegalStateException(mode.name());
      }
    } else if (schemaCol == null) {
      ColumnMetadata copy = readerCol.copy();
      if (readerCol.isMap()) {
        SchemaUtils.markProjectAll(copy);
      }
      scanSchema.addColumn(copy);
    } else if (schemaCol.isDynamic()) {
      if (schemaCol.isMap()) {
        scanSchema.replace(createMap((ProjectedColumn) schemaCol,
            result.mapFilter, readerCol));
      } else {
        scanSchema.replace(readerCol.copy());
      }
    } else if (schemaCol.isMap()) {
      expandMapProjection(schemaCol.tupleSchema(),
          result.mapFilter, readerCol.tupleSchema());
    } // else cols are identical simple
  }
}
