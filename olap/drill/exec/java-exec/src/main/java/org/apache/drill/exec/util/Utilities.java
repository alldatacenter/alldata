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
package org.apache.drill.exec.util;

import java.util.Collection;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.planner.cost.DrillDefaultRelMetadataProvider;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillTranslatableTable;
import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class Utilities {

  public static final String COL_NULL_ERROR = "Columns cannot be null. Use star column to select all fields.";

  public static String getFileNameForQueryFragment(FragmentContext context, String location, String tag) {
     /*
     * From the context, get the query id, major fragment id, minor fragment id. This will be used as the file name to
     * which we will dump the incoming buffer data
     */
    ExecProtos.FragmentHandle handle = context.getHandle();

    String qid = QueryIdHelper.getQueryId(handle.getQueryId());

    int majorFragmentId = handle.getMajorFragmentId();
    int minorFragmentId = handle.getMinorFragmentId();

    return String.format("%s//%s_%s_%s_%s", location, qid, majorFragmentId, minorFragmentId, tag);
  }

  /**
   * Create {@link org.apache.drill.exec.proto.BitControl.QueryContextInformation} with given <i>defaultSchemaName</i>. Rest of the members of the
   * QueryContextInformation is derived from the current state of the process.
   *
   * @return A {@link org.apache.drill.exec.proto.BitControl.QueryContextInformation} with given <i>defaultSchemaName</i>.
   */
  public static QueryContextInformation createQueryContextInfo(final String defaultSchemaName,
      final String sessionId) {
    final long queryStartTime = System.currentTimeMillis();
    final int timeZone = DateUtility.getIndex(System.getProperty("user.timezone"));
    return QueryContextInformation.newBuilder()
        .setDefaultSchemaName(defaultSchemaName)
        .setQueryStartTime(queryStartTime)
        .setTimeZone(timeZone)
        .setSessionId(sessionId)
        .build();
  }

  /**
   * Read the manifest file and get the Drill version number
   * @return The Drill version.
   */
  public static String getDrillVersion() {
    return Utilities.class.getPackage().getImplementationVersion();
  }

  /**
   * Return true if list of schema path has star column.
   *
   * @return True if the list of {@link org.apache.drill.common.expression.SchemaPath}s has star column.
   */
  public static boolean isStarQuery(Collection<SchemaPath> projected) {
    return Preconditions.checkNotNull(projected, COL_NULL_ERROR).stream()
      .anyMatch(SchemaPath::isDynamicStar);
  }

  /**
   * Return true if the row type has star column.
   */
  public static boolean isStarQuery(RelDataType projected) {
    return projected.getFieldNames().stream()
      .anyMatch(SchemaPath.DYNAMIC_STAR::equals);
  }

  /**
   * Gets {@link DrillTable}, either wrapped in RelOptTable, or DrillTranslatableTable.
   *
   * @param table table instance
   * @return Drill table
   */
  public static DrillTable getDrillTable(RelOptTable table) {
    DrillTable drillTable = table.unwrap(DrillTable.class);
    if (drillTable == null && table.unwrap(DrillTranslatableTable.class) != null) {
      drillTable = table.unwrap(DrillTranslatableTable.class).getDrillTable();
    }
    return drillTable;
  }

  /**
   * Converts literal into path segment based on its type.
   * For unsupported types, returns null.
   *
   * @param literal literal
   * @return new path segment, null otherwise
   */
  public static PathSegment convertLiteral(RexLiteral literal) {
    switch (literal.getType().getSqlTypeName()) {
      case CHAR:
        return new PathSegment.NameSegment(RexLiteral.stringValue(literal));
      case INTEGER:
        return new PathSegment.ArraySegment(RexLiteral.intValue(literal));
      default:
        return null;
    }
  }

  public static JaninoRelMetadataProvider registerJaninoRelMetadataProvider() {
    JaninoRelMetadataProvider relMetadataProvider = JaninoRelMetadataProvider.of(DrillDefaultRelMetadataProvider.INSTANCE);
    RelMetadataQuery.THREAD_PROVIDERS.set(relMetadataProvider);
    return relMetadataProvider;
  }
}
