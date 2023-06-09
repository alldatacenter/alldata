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

import java.util.Collection;

import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.PathSegment.ArraySegment;
import org.apache.drill.common.expression.PathSegment.NameSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.Propertied;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Parse the projection list into a dynamic tuple schema. Using
 * an enhanced form of dynamic column which records projection list
 * information (such as map members and array indexes.)
 * <p>
 * A wildcard project list can contain implicit columns in
 * addition to the wildcard. The wildcard defines the
 * <i>insert point</i>: the point at which reader-defined
 * columns are inserted as found.
 */
public class ScanProjectionParser {

  public static final String PROJECTION_TYPE_PROP = Propertied.DRILL_PROP_PREFIX + "proj-type";
  public static final String PROJECT_ALL = "all";
  public static final String PROJECT_NONE = "none";

  public static class ProjectionParseResult {
    public final int wildcardPosn;
    public final TupleMetadata dynamicSchema;

    public ProjectionParseResult(int wildcardPosn,
        TupleMetadata dynamicSchema) {
      this.wildcardPosn = wildcardPosn;
      this.dynamicSchema = dynamicSchema;
    }

    public boolean isProjectAll() { return wildcardPosn != -1; }
  }

  private int wildcardPosn = -1;

  public static ProjectionParseResult parse(Collection<SchemaPath> projList) {
    if (projList == null) {
      return SchemaUtils.projectAll();
    }
    if (projList.isEmpty()) {
      return SchemaUtils.projectNone();
    }
    return new ScanProjectionParser().parseProjection(projList);
  }

  private ProjectionParseResult parseProjection(Collection<SchemaPath> projList) {
    TupleMetadata tupleProj = new TupleSchema();
    for (SchemaPath col : projList) {
      parseMember(tupleProj, 0, col.getRootSegment());
    }
    return new ProjectionParseResult(wildcardPosn, tupleProj);
  }

  private void parseMember(TupleMetadata tuple, int depth, NameSegment nameSeg) {
    String colName = nameSeg.getPath();
    if (colName.equals(SchemaPath.DYNAMIC_STAR)) {
      tuple.setProperty(PROJECTION_TYPE_PROP, PROJECT_ALL);
      if (depth == 0) {
        Preconditions.checkState(wildcardPosn == -1);
        wildcardPosn = tuple.size();
      }
    } else {
      ProjectedColumn col = project(tuple, nameSeg.getPath());
      parseChildSeg(col, depth + 1, nameSeg);
    }
  }

  protected ProjectedColumn project(TupleMetadata tuple, String colName) {
    ColumnMetadata col = tuple.metadata(colName);
    ProjectedColumn projCol;
    if (col == null) {
      projCol = new ProjectedColumn(colName);
      tuple.addColumn(projCol);
    } else {
      projCol = (ProjectedColumn) col;
      projCol.bumpRefCount();
    }
    return projCol;
  }

  private void parseChildSeg(ProjectedColumn column, int depth, PathSegment parentPath) {
    if (parentPath.isLastPath()) {
      parseLeaf(column, depth);
    } else {
      PathSegment seg = parentPath.getChild();
      if (seg.isArray()) {
        parseArraySeg(column, depth, (ArraySegment) seg);
      } else {
        parseMemberSeg(column, depth, (NameSegment) seg);
      }
    }
  }

  /**
   * Parse a projection of the form {@code a}: that is, just a bare column.
   */
  private void parseLeaf(ProjectedColumn parent, int depth) {
    if (parent.isSimple()) {
      // Nothing to do
    } else if (parent.isArray() && depth == 1) {
      parent.projectAllElements();
    } else if (parent.isMap()) {
      parent.projectAllMembers();
    }
  }

  private void parseArraySeg(ProjectedColumn column, int depth, ArraySegment arraySeg) {
    boolean wasArray = column.isArray();
    column.becomeArray(Math.max(depth, column.arrayDims()));

    // Record only outermost dimension indexes
    if (depth == 1) {
      if (column.refCount() > 1 && !wasArray) {
        column.projectAllElements();
      } else {
        column.addIndex(arraySeg.getIndex());
      }
    }
    parseChildSeg(column, depth + 1, arraySeg);
  }

  private void parseMemberSeg(ProjectedColumn column, int depth, NameSegment memberSeg) {
    if (column.refCount() > 1 && !column.isMap()) {
      column.projectAllMembers();
    }
    TupleMetadata tuple = column.explicitMembers();
    if (tuple != null) {
      parseMember(tuple, depth, memberSeg);
    }
  }
}
