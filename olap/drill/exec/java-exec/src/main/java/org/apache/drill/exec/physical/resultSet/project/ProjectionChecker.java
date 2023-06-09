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

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to check if a column is consistent with the projection
 * requested for a query. Used for scans: the reader offers certain columns
 * and the scan operator must decide whether to accept them, and if so,
 * if the column that has actually appeared is consistent with the projection
 * schema path provided by the planner. An obvious example is if projection
 * asks for {@code a[0]} (and array), but the reader offer up {@code a}
 * as a non-array column.
 * <p>
 * Checks are reasonable, but not complete. Particularly in the {@code DICT}
 * case, projection depends on multiple factors, such as the type of the
 * key and values. This class does not (yet) handle that complexity.
 * Instead, the goal is no false negatives for the complex cases, while
 * catching the simple cases.
 * <p>
 * The Project operator or other consuming operator is the final arbitrator
 * of whether a particular column satisfies a particular projection. This
 * class tries to catch those errors early to provide better error
 * messages.
 */
public class ProjectionChecker {
  private static final Logger logger = LoggerFactory.getLogger(ProjectionChecker.class);

  private ProjectionChecker() { }

  /**
   * Check if the given read column is consistent with the projection requested for
   * a tuple. This form handles wildcard projection and unprojected columns; cases
   * where there is no column-level projection information.
   *
   * @param tuple the tuple-level projection description
   * @param readCol metadata for the column which the reader has actually
   * produced
   * @return {@code true} if the column is consistent with projection (or if the
   * column is too complex to check), {@code false} if the column is not
   * consistent and represents an error case. Also returns {@code true} if
   * the column is not projected, as any type of column can be ignored
   */
  public static boolean isConsistent(RequestedTuple tuple, ColumnMetadata readCol) {
    if (tuple == null || !tuple.isProjected(readCol.name())) {
      return true;
    }
    // If the column is projected, it may be projected implicitly.
    // Only check explicit projection.
    RequestedColumn col = tuple.get(readCol.name());
    if (col == null) {
      return true;
    } else {
      return isConsistent(col, readCol);
    }
  }

  /**
   * Check if the given read column is consistent with the projection requested for
   * that column. Does not handle subtleties such as DICT key types, actual types
   * in a UNION, etc.
   *
   * @param colReq the column-level projection description
   * @param readCol metadata for the column which the reader has actually
   * produced
   * @return {@code true} if the column is consistent with projection (or if the
   * column is too complex to check), {@code false} if the column is not
   * consistent and represents an error case. Also returns {@code true} if
   * the column is not projected, as any type of column can be ignored
   */
  public static boolean isConsistent(RequestedColumn colReq, ColumnMetadata readCol) {
    if (colReq == null || readCol == null) {
      return true;
    }
    if (colReq.isTuple() && !(readCol.isMap() || readCol.isDict() || readCol.isVariant())) {
      return false;
    }
    if (colReq.isArray()) {
      if (colReq.arrayDims() == 1) {
        return readCol.isArray() || readCol.isDict() || readCol.isVariant();
      } else {
        return readCol.type() == MinorType.LIST || readCol.isDict() || readCol.isVariant();
      }
    }
    return true;
  }

  public static void validateProjection(RequestedColumn colReq, ColumnMetadata readCol) {
    validateProjection(colReq, readCol, null);
  }

  /**
   * Perform the column-level projection as described in
   * {@link #isConsistent(RequestedColumn, ColumnMetadata)}, and raise a
   * {@code UserException} if the column is not consistent with projection.
   *
   * @param colReq the column-level projection description
   * @param readCol metadata for the column which the reader has actually
   * produced
   * @param errorContext additional error context to pass along in the
   * exception
   * @throws UserException if the read column is not consistent with the
   * projection description for the column
   */
  public static void validateProjection(RequestedColumn colReq, ColumnMetadata readCol,
      CustomErrorContext errorContext) {
    if (!isConsistent(colReq, readCol)) {
      throw UserException.validationError()
        .message("Column type not compatible with projection specification")
        .addContext("Column:", readCol.name())
        .addContext("Projection type:", colReq.toString())
        .addContext("Column type:", Types.getSqlTypeName(readCol.majorType()))
        .addContext(errorContext)
        .build(logger);
    }
  }
}
