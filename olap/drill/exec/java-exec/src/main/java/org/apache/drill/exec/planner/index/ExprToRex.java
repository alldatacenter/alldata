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

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.exec.planner.sql.TypeInferenceUtils;

import java.math.BigDecimal;

/**
 * Convert a logicalExpression to RexNode, notice the inputRel could be in an old plan, but newRowType is the newly built rowType
 * that the new RexNode will be applied upon, so when reference fields, use newRowType, when need cluster, plannerSetting, etc, use old inputRel
 */
public class ExprToRex extends AbstractExprVisitor<RexNode, Void, RuntimeException> {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExprToRex.class);

  private final RexBuilder builder;
  private final RelDataType newRowType;
  private final RelNode inputRel;

  public ExprToRex(RelNode inputRel, RelDataType newRowType, RexBuilder builder) {
    this.inputRel = inputRel;
    this.newRowType = newRowType;
    this.builder = builder;
  }

  public static RelDataTypeField findField(String fieldName, RelDataType rowType) {
    final String rootPart = SchemaPath.parseFromString(fieldName).getRootSegmentPath();

    for (RelDataTypeField f : rowType.getFieldList()) {
      if (rootPart.equalsIgnoreCase(f.getName())) {
        return f;
      }
    }
    return null;
  }

  @Override
  public RexNode visitSchemaPath(SchemaPath path, Void value) throws RuntimeException {
    PathSegment pathSegment = path.getRootSegment();

    RelDataTypeField field = findField(pathSegment.getNameSegment().getPath(), newRowType);
    RexNode rexNode = field == null ? null : builder.makeInputRef(field.getType(), field.getIndex());
    while (!pathSegment.isLastPath()) {
      pathSegment = pathSegment.getChild();
      RexNode ref;
      if (pathSegment.isNamed()) {
        ref = builder.makeLiteral(pathSegment.getNameSegment().getPath());
      } else {
        ref = builder.makeBigintLiteral(BigDecimal.valueOf(pathSegment.getArraySegment().getIndex()));
      }
      rexNode = builder.makeCall(SqlStdOperatorTable.ITEM, rexNode, ref);
    }

    return rexNode;
  }

  @Override
  public RexNode visitCastExpression(CastExpression e, Void value) throws RuntimeException {
    RexNode convertedInput = e.getInput().accept(this, null);
    String typeStr = e.getMajorType().getMinorType().toString();

    if (SqlTypeName.get(typeStr) == null) {
      logger.debug("SqlTypeName could not find {}", typeStr);
    }

    SqlTypeName typeName = TypeInferenceUtils.getCalciteTypeFromDrillType(e.getMajorType().getMinorType());

    RelDataType targetType = TypeInferenceUtils.createCalciteTypeWithNullability(
        inputRel.getCluster().getTypeFactory(), typeName, true);
    return builder.makeCast(targetType, convertedInput);
  }

}
