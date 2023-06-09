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
package org.apache.drill.exec.physical.resultSet.project;

import java.util.List;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleNameSpace;

/**
 * Represents an explicit projection at some tuple level. A tuple is the
 * top-level row or a map.
 * <p>
 * A column is projected if it is explicitly listed in the selection list.
 * <p>
 * If a column is a map, then the projection for the map's columns is based on
 * two rules:
 * <ol>
 * <li>If the projection list includes at least one explicit mention of a map
 * member, then include only those columns explicitly listed.</li>
 * <li>If the projection at the parent level lists only the map column itself
 * (which the projection can't know is a map), then assume this implies all
 * columns, as if the entry where "map.*".</li>
 * </ol>
 * <p>
 * Examples:<br>
 * <code>m</code><br>
 * If <code>m</code> turns out to be a map, project all members of
 * <code>m</code>.<br>
 * <code>m.a</code><br>
 * Column <code>m</code> must be a map. Project only column <code>a</code>.<br>
 * <code>m, m.a</code><br>
 * Tricky case. We interpret this as projecting only the "a" element of map m.
 * <p>
 * The projection set is built from a list of columns, represented as
 * {@link SchemaPath} objects, provided by the physical plan. The structure of
 * <tt>SchemaPath</tt> is a bit awkward:
 * <p>
 * <ul>
 * <li><tt>SchemaPath> is a wrapper for a column which directly holds the
 * <tt>NameSegment</tt> for the top-level column.</li>
 * <li><tt>NameSegment</tt> holds a name. This can be a top name such as
 * `a`, or parts of a compound name such as `a`.`b`. Each <tt>NameSegment</tt>
 * has a "child" that points to the option following parts of the name.</li>
 * <li><PathSegment</tt> is the base class for the parts of a name.</tt>
 * <li><tt>ArraySegment</tt> is the other kind of name part and represents
 * an array index such as the "[1]" in `columns`[1].</li>
 * <ul>
 * The parser considers names and array indexes. Example:<pre><code>
 * a
 * a.b
 * a[2]
 * a[2].b
 * a[1][2][3]
 * a[1][2][3].b.c
 * a['foo'][0].b['bar']
 * </code></pre>
 *
 * <h4>Usage</h4>
 * The projection information is a <i>pattern</i> which supports queries of the
 * form "is this column projected", and "if projected, is the projection consistent
 * with such-and-so concrete type?" Clients should not try to work out the
 * meaning of the pattern: doing so is very complex. Instead, do the following:
 *
 * <pre><code>
 * String colName = ...;
 * ColumnMetadata colDef = ...;
 * InputTupleProjection tupleProj = ...
 * if (tupleProj.isProjected(colName)) {
 *   if (!tupleProj.isComsistentWith(colDef)) {
 *     // Raise an error
 *   }
 *   // Handle a projected column.
 * }</code></pre>
 */
public class RequestedTupleImpl implements RequestedTuple {

  private final RequestedColumnImpl parent;
  protected TupleProjectionType projectionType = TupleProjectionType.SOME;
  private final TupleNameSpace<RequestedColumn> projection = new TupleNameSpace<>();

  public RequestedTupleImpl() {
    this.parent = null;
  }

  public RequestedTupleImpl(RequestedColumnImpl parent) {
    this.parent = parent;
  }

  public RequestedTupleImpl(List<RequestedColumn> cols) {
    parent = null;
    for (RequestedColumn col : cols) {
      projection.add(col.name(), col);
    }
  }

  @Override
  public int size() { return projection.count(); }

  @Override
  public RequestedColumn get(int i) {
    return projection.get(i);
  }

  @Override
  public RequestedColumn get(String colName) {
    return projection.get(colName.toLowerCase());
  }

  protected RequestedColumnImpl getImpl(String colName) {
    return (RequestedColumnImpl) get(colName);
  }

  protected RequestedColumn project(String colName) {
    RequestedColumn col = get(colName);
    if (col != null) {
      if (col instanceof RequestedColumnImpl) {
        ((RequestedColumnImpl) col).bumpRefCount();
      }
    } else {
      if (colName.equals(SchemaPath.DYNAMIC_STAR)) {
        projectionType = TupleProjectionType.ALL;
        col = new RequestedWildcardColumn(this, colName);
      } else {
        col = new RequestedColumnImpl(this, colName);
      }
      projection.add(colName, col);
    }
    return col;
  }

  @Override
  public List<RequestedColumn> projections() {
    return projection.entries();
  }

  @Override
  public void buildName(StringBuilder buf) {
    if (parent != null) {
      parent.buildName(buf);
    }
  }

  /**
   * Tuple projection type. This is a rough approximation. A scan-level projection
   * may include both a wildcard and implicit columns. This form is best used
   * in testing where such ambiguities do not apply.
   */
  @Override
  public TupleProjectionType type() {
    return projectionType;
  }

  @Override
  public boolean isProjected(String colName) {
    return projectionType == TupleProjectionType.ALL ? true : get(colName) != null;
  }

  @Override
  public boolean isProjected(ColumnMetadata columnSchema) {
    if (!isProjected(columnSchema.name())) {
      return false;
    }
    return projectionType == TupleProjectionType.ALL ?
        !Projections.excludeFromWildcard(columnSchema) : true;
  }

  @Override
  public boolean enforceProjection(ColumnMetadata columnSchema, CustomErrorContext errorContext) {
    if (projectionType == TupleProjectionType.ALL) {
      return true;
    }
    RequestedColumn reqCol = get(columnSchema.name());
    if (reqCol == null) {
      return false;
    }
    ProjectionChecker.validateProjection(reqCol, columnSchema, errorContext);
    return true;
  }

  @Override
  public RequestedTuple mapProjection(String colName) {
    switch (projectionType) {
      case ALL:
        return ImpliedTupleRequest.ALL_MEMBERS;
      case NONE:
        return ImpliedTupleRequest.NO_MEMBERS;
      default:
        RequestedColumnImpl colProj = getImpl(colName);
        return colProj == null ? ImpliedTupleRequest.NO_MEMBERS : colProj.tuple();
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder()
        .append("{");
    boolean first = true;
    for (RequestedColumn col : projections()) {
      if (!first) {
        buf.append(", ");
      }
      first = false;
      buf.append(col.toString());
    }
    return buf.append("}").toString();
  }

  @Override
  public boolean isEmpty() { return projections().isEmpty(); }
}
