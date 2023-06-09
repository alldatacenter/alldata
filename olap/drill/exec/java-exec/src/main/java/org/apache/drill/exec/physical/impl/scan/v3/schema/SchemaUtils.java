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
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.project.RequestedColumn;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.Propertied;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of schema utilities that don't fit well as methods on the column
 * or tuple classes.
 * <p>
 * Provides methods to check if a column is consistent with the projection
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
public class SchemaUtils {
  protected static final Logger logger = LoggerFactory.getLogger(SchemaUtils.class);

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
  public static boolean isConsistent(ProjectedColumn colReq, ColumnMetadata readCol) {
    if (readCol.isDynamic()) {

      // Don't know the type. This is a name-only probe.
      return true;
    }

    // If the projection is map-like, but the proposed concrete column
    // is not map-like, then the columns are not compatible.
    if (colReq.isMap() && !(readCol.isMap() || readCol.isDict() || readCol.isVariant())) {
      return false;
    }

    // If the projection is array-like, but the proposed concrete column
    // is not map-like, or does not have at least as many dimensions as the
    // projection, then the column is not compatible.
    if (colReq.isArray()) {
      if (colReq.arrayDims() == 1) {
        return readCol.isArray() || readCol.isDict() || readCol.isVariant();
      } else {
        return readCol.type() == MinorType.LIST || readCol.isDict() || readCol.isVariant();
      }
    }
    return true;
  }

  /**
   * Perform the column-level projection as described in
   * {@link #isConsistent(RequestedColumn, ColumnMetadata)}, and raise a
   * {@code UserException} if the column is not consistent with projection.
   *
   * @param colReq the column-level projection description
   * @param actual metadata for the column which the reader has actually
   * produced
   * @param errorContext additional error context to pass along in the
   * exception
   * @throws UserException if the read column is not consistent with the
   * projection description for the column
   */
  public static void verifyCompatibility(ProjectedColumn colReq, ColumnMetadata actual,
      String source, CustomErrorContext errorContext) {
    if (!isConsistent(colReq, actual)) {
      throw UserException.validationError()
        .message(source + " column type not compatible with projection specification")
        .addContext("Projected column", colReq.projectString())
        .addContext(source + " column", actual.columnString())
        .addContext(errorContext)
        .build(logger);
    }
  }

  public static void verifyConsistency(ColumnMetadata existing,
      ColumnMetadata revised, String source,
      CustomErrorContext errorContext) {
    if (existing.isDynamic() || revised.isDynamic()) {
      return;
    }
    if (existing.type() != revised.type() ||
        existing.mode() != revised.mode()) {
     throw UserException.validationError()
       .message("Scan and " + source + " column type conflict")
       .addContext("Scan column", existing.columnString())
       .addContext(source + " column", revised.columnString())
       .addContext(errorContext)
       .build(logger);
    }
  }

  public static void verifyProjection(ColumnMetadata existing,
      ColumnMetadata revised, String source,
      CustomErrorContext errorContext) {
    if (existing instanceof ProjectedColumn) {
      verifyCompatibility((ProjectedColumn) existing, revised, source, errorContext);
    } else {
      verifyConsistency(existing, revised, source, errorContext);
    }
  }

  public static void mergeColProperties(ColumnMetadata existing, ColumnMetadata revised) {
    mergeProperties(existing, revised);
    if (existing.isMap() && revised.isMap()) {
      mergeProperties(existing.tupleSchema(), revised.tupleSchema());
    }
  }

  public static void mergeProperties(Propertied existing, Propertied revised) {
    if (!revised.hasProperties()) {
      return;
    }
    existing.properties().putAll(revised.properties());
  }

  public static boolean isStrict(TupleMetadata schema) {
    return schema.booleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP);
  }

  public static void markStrict(TupleMetadata schema) {
    schema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
  }

  public static String implicitColType(ColumnMetadata col) {
    return col.property(ColumnMetadata.IMPLICIT_COL_TYPE);
  }

  public static boolean isImplicit(ColumnMetadata col) {
    return implicitColType(col) != null;
  }

  public static void markImplicit(ColumnMetadata col, String value) {
    col.setProperty(ColumnMetadata.IMPLICIT_COL_TYPE, value);
  }

  public static void markAsPartition(ColumnMetadata col, int level) {
    markImplicit(col, ColumnMetadata.IMPLICIT_PARTITION_PREFIX + level);
  }

  public static void markExcludeFromWildcard(ColumnMetadata col) {
    col.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
  }

  public static boolean isExcludedFromWildcard(ColumnMetadata col) {
    return col.booleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD);
  }

  public static ScanProjectionParser.ProjectionParseResult projectAll() {
    TupleMetadata projSet = new TupleSchema();
    projSet.setProperty(ScanProjectionParser.PROJECTION_TYPE_PROP, ScanProjectionParser.PROJECT_ALL);
    return new ScanProjectionParser.ProjectionParseResult(0, projSet);
  }

  public static void markProjectAll(ColumnMetadata col) {
    Preconditions.checkArgument(col.isMap());
    col.tupleSchema().setProperty(ScanProjectionParser.PROJECTION_TYPE_PROP, ScanProjectionParser.PROJECT_ALL);
  }

  public static ScanProjectionParser.ProjectionParseResult projectNone() {
    TupleMetadata projSet = new TupleSchema();
    projSet.setProperty(ScanProjectionParser.PROJECTION_TYPE_PROP, ScanProjectionParser.PROJECT_NONE);
    return new ScanProjectionParser.ProjectionParseResult(-1, projSet);
  }

  public static boolean isProjectAll(TupleMetadata tuple) {
    return ScanProjectionParser.PROJECT_ALL.equals(tuple.property(ScanProjectionParser.PROJECTION_TYPE_PROP));
  }

  public static boolean isProjectNone(TupleMetadata tuple) {
    return ScanProjectionParser.PROJECT_NONE.equals(tuple.property(ScanProjectionParser.PROJECTION_TYPE_PROP));
  }

  public static void copyMapProperties(ProjectedColumn source,
      ColumnMetadata dest) {
    if (source != null && source.isMap()) {
      Preconditions.checkArgument(dest.isMap());
      SchemaUtils.copyProperties(source.tupleSchema(), dest.tupleSchema());
    } else {
      markProjectAll(dest);
    }
  }

  static void copyProperties(TupleMetadata source,
      TupleMetadata dest) {
    String value = source.property(ScanProjectionParser.PROJECTION_TYPE_PROP);
    if (value != null) {
      dest.setProperty(ScanProjectionParser.PROJECTION_TYPE_PROP, value);
    }
  }
}
