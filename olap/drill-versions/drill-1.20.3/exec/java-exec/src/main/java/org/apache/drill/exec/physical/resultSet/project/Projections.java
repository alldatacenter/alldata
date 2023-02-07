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

import java.util.Collection;
import java.util.List;

import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.PathSegment.ArraySegment;
import org.apache.drill.common.expression.PathSegment.NameSegment;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.common.expression.SchemaPath;

/**
 * Converts a projection list passed to an operator into a scan projection list,
 * coalescing multiple references to the same column into a single reference.
 */
public class Projections {

  private Projections() { }

  public static RequestedTuple projectAll() {
    return ImpliedTupleRequest.ALL_MEMBERS;
  }

  public static RequestedTuple projectNone() {
    return ImpliedTupleRequest.NO_MEMBERS;
  }

  /**
   * Parse a projection list. The list should consist of a list of column names;
   * or wildcards. An empty list means
   * nothing is projected. A null list means everything is projected (that is, a
   * null list here is equivalent to a wildcard in the SELECT statement.)
   * <p>
   * The projection list may include both a wildcard and column names (as in
   * the case of implicit columns.) This results in a final list that both
   * says that everything is projected, and provides the list of columns.
   * <p>
   * Parsing is used at two different times. First, to parse the list from
   * the physical operator. This has the case above: an explicit wildcard
   * and/or additional columns. Then, this class is used again to prepare the
   * physical projection used when reading. In this case, wildcards should
   * be removed, implicit columns pulled out, and just the list of read-level
   * columns should remain.
   *
   * @param projList
   *          the list of projected columns, or null if no projection is to be
   *          done
   * @return a projection set that implements the specified projection
   */
  public static RequestedTuple parse(Collection<SchemaPath> projList) {
    if (projList == null) {
      return projectAll();
    }
    if (projList.isEmpty()) {
      return projectNone();
    }
    RequestedTupleImpl tupleProj = new RequestedTupleImpl();
    for (SchemaPath col : projList) {
      parseMember(tupleProj, col.getRootSegment());
    }
    return tupleProj;
  }

  private static void parseMember(RequestedTupleImpl tuple, NameSegment nameSeg) {
    RequestedColumn col = tuple.project(nameSeg.getPath());
    if (!col.isWildcard()) {
      RequestedColumnImpl colImpl = (RequestedColumnImpl) col;
      parseChildSeg(colImpl, colImpl, nameSeg);
    }
  }

  private static void parseChildSeg(RequestedColumnImpl column, QualifierContainer parent, PathSegment parentPath) {
    if (parentPath.isLastPath()) {
      parseLeaf(parent);
    } else {
      PathSegment seg = parentPath.getChild();
      if (seg.isArray()) {
        parseArraySeg(column, parent, (ArraySegment) seg);
      } else {
        parseMemberSeg(column, parent, (NameSegment) seg);
      }
    }
  }

  /**
   * Parse a projection of the form {@code a}: that is, just a bare column.
   */
  private static void parseLeaf(QualifierContainer parent) {
    Qualifier qual = parent.qualifier();
    if (qual == null) {
      // Nothing to do
    } else if (qual.isArray()) {
      qual.projectAllElements();
    } else if (qual.isTuple()) {
      qual.projectAllMembers();
    }
  }

  private static void parseArraySeg(RequestedColumnImpl column, QualifierContainer parent, ArraySegment arraySeg) {
    Qualifier prevQualifier = parent.qualifier();
    Qualifier qualifier = parent.requireQualifier();
    if (column.refCount() > 1 && (prevQualifier == null || !prevQualifier.isArray())) {
      qualifier.projectAllElements();
    } else {
      qualifier.addIndex(arraySeg.getIndex());
    }
    parseChildSeg(column, qualifier, arraySeg);
  }

  private static void parseMemberSeg(RequestedColumnImpl column, QualifierContainer parent, NameSegment memberSeg) {
    Qualifier prevQualifier = parent.qualifier();
    Qualifier qualifier = parent.requireQualifier();
    if (column.refCount() > 1 && (prevQualifier == null || !prevQualifier.isTuple())) {
      qualifier.projectAllMembers();
    } else {
      RequestedTupleImpl tuple = qualifier.explicitMembers();
      if (tuple != null) {
        parseMember(tuple, memberSeg);
      }
    }
  }

  /**
   * Create a requested tuple projection from a rewritten top-level
   * projection list. The columns within the list have already been parsed to
   * pick out arrays, maps and scalars. The list must not include the
   * wildcard: a wildcard list must be passed in as a null list. An
   * empty list means project nothing. Null list means project all, else
   * project only the columns in the list.
   *
   * @param projList top-level, parsed columns
   * @return the tuple projection for the top-level row
   */
  public static RequestedTuple build(List<RequestedColumn> projList) {
    if (projList == null) {
      return new ImpliedTupleRequest(true);
    }
    if (projList.isEmpty()) {
      return projectAll();
    }
    return new RequestedTupleImpl(projList);
  }

  /**
   * Reports whether the column is a special column which should not be
   * expanded in a wildcard. Used for specialized columns in readers
   * such as the Log format.
   */
  public static boolean excludeFromWildcard(ColumnMetadata col) {
    return col.booleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD);
  }
}
