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

package org.apache.drill.exec.planner.index;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.calcite.rel.RelNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FunctionalIndexHelper {

  public static RelDataType rewriteFunctionalRowType(RelNode origScan, IndexCallContext indexContext,
                                                     FunctionalIndexInfo functionInfo) {
    return rewriteFunctionalRowType(origScan, indexContext, functionInfo, null);
  }
  /**
   * if a field in rowType serves only the to-be-replaced column(s), we should replace it with new name "$1",
   * otherwise we should keep this dataTypeField and add a new one for "$1"
   * @param origScan  the original scan whose rowtype is to be rewritten
   * @param indexContext the index plan context
   * @param functionInfo functional index information that may impact rewrite
   * @return
   */
  public static RelDataType rewriteFunctionalRowType(RelNode origScan, IndexCallContext indexContext,
      FunctionalIndexInfo functionInfo, Collection<SchemaPath> addedPaths) {
    RelDataType origRowType = origScan.getRowType();
    if (!functionInfo.hasFunctional()) {
      return origRowType;
    }

    List<RelDataTypeField> fields = Lists.newArrayList();

    Set<String> leftOutFieldNames  = Sets.newHashSet();
    if (indexContext.getLeftOutPathsInFunctions() != null) {
      for (LogicalExpression expr : indexContext.getLeftOutPathsInFunctions()) {
        leftOutFieldNames.add(((SchemaPath) expr).getRootSegmentPath());
      }
    }

    Set<String> fieldInFunctions  = Sets.newHashSet();
    for (SchemaPath path: functionInfo.allPathsInFunction()) {
      fieldInFunctions.add(path.getRootSegmentPath());
    }

    RelDataTypeFactory typeFactory = origScan.getCluster().getTypeFactory();

    for ( RelDataTypeField field: origRowType.getFieldList()) {
      final String fieldName = field.getName();
      if (fieldInFunctions.contains(fieldName)) {
        if (!leftOutFieldNames.contains(fieldName)) {
          continue;
        }
      }

      fields.add(new RelDataTypeFieldImpl(
          SchemaPath.parseFromString(fieldName).getRootSegmentPath(), fields.size(),
          typeFactory.createSqlType(SqlTypeName.ANY)));
    }

    final Collection<SchemaPath> toAddToRowType = (addedPaths == null)? functionInfo.allNewSchemaPaths() : addedPaths;

    for (SchemaPath dollarPath : toAddToRowType) {
      fields.add(
          new RelDataTypeFieldImpl(dollarPath.getRootSegmentPath(), fields.size(),
              origScan.getCluster().getTypeFactory().createSqlType(SqlTypeName.ANY)));
    }

    return new RelRecordType(fields);
  }

  /**
   * For IndexScan in non-covering case, rowType to return contains only row_key('_id') of primary table.
   * so the rowType for IndexScan should be converted from [Primary_table.row_key, primary_table.indexed_col]
   * to [indexTable.row_key(primary_table.indexed_col), indexTable.<primary_key.row_key> (Primary_table.row_key)]
   * This will impact the columns of scan, the rowType of ScanRel
   *
   * @param origScan
   * @param idxMarker  the IndexableExprMarker that has analyzed original index condition on top of index scan
   * @param idxScan
   * @return
   */
  public static RelDataType convertRowTypeForIndexScan(DrillScanRelBase origScan,
                                                       IndexableExprMarker idxMarker,
                                                       IndexGroupScan idxScan,
                                                       FunctionalIndexInfo functionInfo) {
    RelDataTypeFactory typeFactory = origScan.getCluster().getTypeFactory();
    List<RelDataTypeField> fields = new ArrayList<>();

    Set<SchemaPath> rowPaths = new LinkedHashSet<>();
    //row_key in the rowType of scan on primary table
    RelDataTypeField rowkey_primary;

    RelRecordType newRowType = null;

    DbGroupScan scan = (DbGroupScan) IndexPlanUtils.getGroupScan(origScan);

    //first add row_key of primary table,
    rowkey_primary = new RelDataTypeFieldImpl(
        scan.getRowKeyName(), fields.size(),
        typeFactory.createSqlType(SqlTypeName.ANY));
    fields.add(rowkey_primary);

    Map<RexNode, LogicalExpression> idxExprMap = idxMarker.getIndexableExpression();

    for (LogicalExpression indexedExpr : idxExprMap.values()) {
      if (indexedExpr instanceof SchemaPath) {
        rowPaths.add((SchemaPath) indexedExpr);
      }
      else if(indexedExpr instanceof CastExpression) {
        SchemaPath newPath = functionInfo.getNewPathFromExpr(indexedExpr);
        if(newPath != null) {
          rowPaths.add(newPath);
        }
      }
    }
    for (SchemaPath newPath : rowPaths) {
      fields.add(new RelDataTypeFieldImpl(
          newPath.getRootSegmentPath(), fields.size(),
          typeFactory.createSqlType(SqlTypeName.ANY)));
    }

    //update columns of groupscan accordingly
    Set<RelDataTypeField> rowfields = Sets.newLinkedHashSet();
    final List<SchemaPath> columns = Lists.newArrayList();
    for (RelDataTypeField f : fields) {
      SchemaPath path = SchemaPath.parseFromString(f.getName());
      rowfields.add(new RelDataTypeFieldImpl(
          path.getRootSegmentPath(), rowfields.size(),
          typeFactory.createSqlType(SqlTypeName.ANY)
      ));
      columns.add(path);
    }
    idxScan.setColumns(columns);

    //rowtype does not take the whole path, but only the rootSegment of the SchemaPath
    newRowType = new RelRecordType(Lists.newArrayList(rowfields));
    return newRowType;
  }

  public static RexNode convertConditionForIndexScan(RexNode idxCondition,
      RelNode origScan, RelDataType idxRowType, RexBuilder builder, FunctionalIndexInfo functionInfo) {
    IndexableExprMarker marker = new IndexableExprMarker(origScan);
    idxCondition.accept(marker);
    SimpleRexRemap remap = new SimpleRexRemap(origScan, idxRowType, builder);
    remap.setExpressionMap(functionInfo.getExprMap());

    if (functionInfo.supportEqualCharConvertToLike()) {
      final Map<LogicalExpression, LogicalExpression> indexedExprs = functionInfo.getExprMap();

      final Map<RexNode, LogicalExpression> equalCastMap = marker.getEqualOnCastChar();

      Map<RexNode, LogicalExpression> toRewriteEqualCastMap = Maps.newHashMap();

      // the marker collected all equal-cast-varchar, now check which one we should replace
      for (Map.Entry<RexNode, LogicalExpression> entry : equalCastMap.entrySet()) {
        CastExpression expr = (CastExpression) entry.getValue();
        //whether this cast varchar/char expression is indexed even the length is not the same
        for (LogicalExpression indexed : indexedExprs.keySet()) {
          if (indexed instanceof CastExpression) {
            final CastExpression indexedCast = (CastExpression) indexed;
            if (expr.getInput().equals(indexedCast.getInput())
                && expr.getMajorType().getMinorType().equals(indexedCast.getMajorType().getMinorType())
                //if expr's length < indexedCast's length, we should convert equal to LIKE for this condition
                && expr.getMajorType().getPrecision() < indexedCast.getMajorType().getPrecision()) {
              toRewriteEqualCastMap.put(entry.getKey(), entry.getValue());
            }
          }
        }
      }
      if (toRewriteEqualCastMap.size() > 0) {
        idxCondition = remap.rewriteEqualOnCharToLike(idxCondition, toRewriteEqualCastMap);
      }
    }

    return remap.rewriteWithMap(idxCondition, marker.getIndexableExpression());
  }
}
