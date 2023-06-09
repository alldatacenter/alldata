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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnMarker;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * A mutable form of a tuple schema. Allows insertions (at the wildcard position),
 * and replacing columns (as the schema becomes resolved). Tracks implicit columns
 * (those not filled in by the reader).
 * <p>
 * Does not implement the {@code TupleMetadata} interface because that interface
 * has far more functionality than is needed here, and assumes that column order
 * remains fixed (and hence columns can be addressed by position) which is not
 * true for this class.
 * <p>
 * This class represents the top-level tuple (the row.) Maps are also dynamic,
 * but provide a subset of resolution options:
 * map fields cannot be implicit. They can, however, be defined,
 * provided, discovered or missing. Map columns can start unresolved
 * if the map comes from projection. A map itself can be resolved,
 * but its members may be unresolved. New map members may only be added at the
 * end (there is no equivalent of a wildcard position.)
 */
public class MutableTupleSchema {

  /**
   * Holder for a column to allow inserting and replacing columns within
   * the top-level project list. Changes to the column within the holder
   * must go through the tuple itself so we can track schema versions.
   * <p>
   * Tracks the resolution status of each individual column as
   * described for {@link ScanSchemaTracker}. Models a column throughout the
   * projection lifecycle. Columns evolve from unresolved to resolved at
   * different times. Columns are either implicit (defined by the framework)
   * or normal (defined by the reader). Columns can be defined by the
   * planner (via a defined schema), partially defined (via a provided
   * schema), or discovered by the reader. Regardless of the path
   * to definition, by the time the first batch is delivered downstream,
   * each column has an output schema which describes the data.
   */
  public static class ColumnHandle {
    private ColumnMetadata col;
    private ImplicitColumnMarker marker;

    public ColumnHandle(ColumnMetadata col) {
      this.col = col;
    }

    public String name() {
      return col.name();
    }

    private void replace(ColumnMetadata col) {
      this.col = col;
    }

    private void resolve(ColumnMetadata col) {
      SchemaUtils.mergeColProperties(this.col, col);
      this.col = col;
    }

    private void resolveImplicit(ColumnMetadata col, ImplicitColumnMarker marker) {
      SchemaUtils.mergeColProperties(this.col, col);
      this.col = col;
      markImplicit(marker);
    }

    public void markImplicit(ImplicitColumnMarker marker) {
      this.marker = marker;
    }

    public ColumnMetadata column() { return col; }
    public boolean isImplicit() { return marker != null; }
    public void setIndex(int index) { marker.setIndex(index); }

    @Override
    public String toString() {
      return col.toString();
    }
  }

  protected final List<ColumnHandle> columns = new ArrayList<>();
  protected final Map<String, ColumnHandle> nameIndex =
      CaseInsensitiveMap.newHashMap();
  private ProjectionType projType;
  private int insertPoint = -1;
  private int version;

  public void setProjectionType(ScanSchemaTracker.ProjectionType type) {
    this.projType = type;

    // For project none, an empty schema is valid, so
    // force a bump in schema version.
    version++;
  }

  public void setInsertPoint(int insertPoint) {
    Preconditions.checkArgument(insertPoint == -1 ||
        insertPoint >= 0 && insertPoint <= size());
    this.insertPoint = insertPoint;
  }

  public ScanSchemaTracker.ProjectionType projectionType() { return projType; }
  public int size() { return columns.size(); }
  public int version() { return version; }

  /**
   * Provide the list of partially-resolved columns. Primarily for
   * the implicit column parser.
   */
  public List<ColumnHandle> columns() { return columns; }

  public ColumnHandle find(String colName) {
    return nameIndex.get(colName);
  }

  public void copyFrom(TupleMetadata from) {
    if (from.isEmpty()) {
      return;
    }
    for (ColumnMetadata projCol : from) {
      add(projCol.copy());
    }
    version++;
  }

  public void add(ColumnMetadata col) {
    ColumnHandle holder = new ColumnHandle(col);
    columns.add(holder);
    addIndex(holder);
    version++;
  }

  public void addIndex(ColumnHandle holder) {
    if (nameIndex.put(holder.column().name(), holder) != null) {
      throw new IllegalArgumentException("Duplicate scan projection column: " + holder.name());
    }
  }

  public ColumnHandle insert(int posn, ColumnMetadata col) {
    ColumnHandle holder = new ColumnHandle(col);
    columns.add(posn, holder);
    addIndex(holder);
    version++;
    return holder;
  }

  public ColumnHandle insert(ColumnMetadata col) {
    return insert(insertPoint++, col);
  }

  /**
   * Move a column from its current position (which must be past the insert point)
   * to the insert point. An index entry already exists. Special operation done
   * when matching a provided schema to a projection list that includes a wildcard
   * and explicitly projected columns. Works around unfortunate behavior in the
   * planner.
   */
  public void moveIfExplicit(String colName) {
    ColumnHandle holder = find(colName);
    Objects.requireNonNull(holder);
    int posn = columns.indexOf(holder);
    if (posn == insertPoint) {
      insertPoint++;
    } else if (posn > insertPoint) {
      columns.remove(posn);
      columns.add(insertPoint++, holder);
      version++;
    }
  }

  public boolean isResolved() {
    for (ColumnHandle handle : columns) {
      if (!isColumnResolved(handle.column())) {
        return false;
      }
    }
    return true;
  }

  private boolean isColumnResolved(ColumnMetadata col) {
    return !col.isDynamic() && (!col.isMap() || isMapResolved(col.tupleSchema()));
  }

  private boolean isMapResolved(TupleMetadata mapSchema) {
    for (ColumnMetadata col : mapSchema) {
      if (col.isDynamic()) {
        return false;
      }
      if (col.isMap() && !isMapResolved(col.tupleSchema())) {
        return false;
      }
    }
    return true;
  }

  public TupleMetadata toSchema() {
    TupleMetadata schema = new TupleSchema();
    for (ColumnHandle col : columns) {
      schema.addColumn(col.column());
    }
    return schema;
  }

  public void resolveImplicit(ColumnHandle col, ColumnMetadata resolved, ImplicitColumnMarker marker) {
    col.resolveImplicit(resolved, marker);
    version++;
  }

  public void replace(ColumnHandle col, ColumnMetadata resolved) {
    col.replace(resolved);
    version++;
  }

  public void resolve(ColumnHandle col, ColumnMetadata resolved) {
    col.resolve(resolved);
    version++;
  }

  public boolean isEmpty() { return columns.isEmpty(); }
}
