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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import io.netty.buffer.DrillBuf;
import org.apache.calcite.rex.RexExecutor;
import org.apache.calcite.util.DateString;
import org.apache.calcite.util.TimeString;
import org.apache.calcite.util.TimestampString;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.ExpressionStringBuilder;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers;
import org.apache.drill.exec.expr.fn.interpreter.InterpreterEvaluator;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.DateHolder;
import org.apache.drill.exec.expr.holders.Decimal18Holder;
import org.apache.drill.exec.expr.holders.Decimal28SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal38SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal9Holder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.IntervalDayHolder;
import org.apache.drill.exec.expr.holders.IntervalYearHolder;
import org.apache.drill.exec.expr.holders.NullableBigIntHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.expr.holders.NullableDateHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal18Holder;
import org.apache.drill.exec.expr.holders.NullableDecimal28SparseHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal38SparseHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal9Holder;
import org.apache.drill.exec.expr.holders.NullableFloat4Holder;
import org.apache.drill.exec.expr.holders.NullableFloat8Holder;
import org.apache.drill.exec.expr.holders.NullableIntHolder;
import org.apache.drill.exec.expr.holders.NullableIntervalDayHolder;
import org.apache.drill.exec.expr.holders.NullableIntervalYearHolder;
import org.apache.drill.exec.expr.holders.NullableTimeHolder;
import org.apache.drill.exec.expr.holders.NullableTimeStampHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.NullableVarDecimalHolder;
import org.apache.drill.exec.expr.holders.TimeHolder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.expr.holders.VarDecimalHolder;
import org.apache.drill.exec.ops.UdfUtilities;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.sql.TypeInferenceUtils;
import org.apache.drill.exec.vector.DateUtilities;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;

public class DrillConstExecutor implements RexExecutor {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillConstExecutor.class);

  private final PlannerSettings plannerSettings;

  // This is a list of all types that cannot be folded at planning time for various reasons, most of the types are
  // currently not supported at all. The reasons for the others can be found in the evaluation code in the reduce method
  public static final List<Object> NON_REDUCIBLE_TYPES = ImmutableList.builder().add(
      // cannot represent this as a literal according to calcite
      TypeProtos.MinorType.INTERVAL,

      // TODO - map and list are used in Drill but currently not expressible as literals, these can however be
      // outputs of functions that take literals as inputs (such as a convert_fromJSON with a literal string
      // as input), so we need to identify functions with these return types as non-foldable until we have a
      // literal representation for them
      TypeProtos.MinorType.MAP, TypeProtos.MinorType.LIST,

      // TODO - DRILL-2551 - Varbinary is used in execution, but it is missing a literal definition
      // in the logical expression representation and subsequently is not supported in
      // RexToDrill and the logical expression visitors
      TypeProtos.MinorType.VARBINARY,

      TypeProtos.MinorType.TIMESTAMPTZ, TypeProtos.MinorType.TIMETZ, TypeProtos.MinorType.LATE,
      TypeProtos.MinorType.TINYINT, TypeProtos.MinorType.SMALLINT, TypeProtos.MinorType.GENERIC_OBJECT, TypeProtos.MinorType.NULL,
      TypeProtos.MinorType.DECIMAL28DENSE, TypeProtos.MinorType.DECIMAL38DENSE, TypeProtos.MinorType.MONEY,
      TypeProtos.MinorType.FIXEDBINARY, TypeProtos.MinorType.FIXEDCHAR, TypeProtos.MinorType.FIXED16CHAR,
      TypeProtos.MinorType.VAR16CHAR, TypeProtos.MinorType.UINT1, TypeProtos.MinorType.UINT2, TypeProtos.MinorType.UINT4,
      TypeProtos.MinorType.UINT8)
      .build();

  private final FunctionImplementationRegistry funcImplReg;
  private final UdfUtilities udfUtilities;

  public DrillConstExecutor(FunctionImplementationRegistry funcImplReg, UdfUtilities udfUtilities, PlannerSettings plannerSettings) {
    this.funcImplReg = funcImplReg;
    this.udfUtilities = udfUtilities;
    this.plannerSettings = plannerSettings;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void reduce(RexBuilder rexBuilder, List<RexNode> constExps, List<RexNode> reducedValues) {
    for (RexNode newCall : constExps) {
      LogicalExpression logEx = DrillOptiq.toDrill(new DrillParseContext(plannerSettings), (RelNode) null /* input rel */, newCall);

      ErrorCollectorImpl errors = new ErrorCollectorImpl();
      LogicalExpression materializedExpr = ExpressionTreeMaterializer.materialize(logEx, null, errors, funcImplReg);
      if (errors.getErrorCount() != 0) {
        String message = String.format(
            "Failure while materializing expression in constant expression evaluator [%s].  Errors: %s",
            newCall.toString(), errors.toString());
        throw UserException.planError()
          .message(message)
          .build(logger);
      }

      if (NON_REDUCIBLE_TYPES.contains(materializedExpr.getMajorType().getMinorType())) {
        logger.debug("Constant expression not folded due to return type {}, complete expression: {}",
            materializedExpr.getMajorType(),
            ExpressionStringBuilder.toString(materializedExpr));
        reducedValues.add(newCall);
        continue;
      }

      ValueHolder output = InterpreterEvaluator.evaluateConstantExpr(udfUtilities, materializedExpr);
      RelDataTypeFactory typeFactory = rexBuilder.getTypeFactory();

      if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL && TypeHelper.isNull(output)) {
        SqlTypeName sqlTypeName = TypeInferenceUtils.getCalciteTypeFromDrillType(materializedExpr.getMajorType().getMinorType());
        if (sqlTypeName == null) {
          String message = String.format("Error reducing constant expression, unsupported type: %s.",
              materializedExpr.getMajorType().getMinorType());
          throw UserException.unsupportedError()
            .message(message)
            .build(logger);
        }

        RelDataType type = TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, sqlTypeName, true);
        reducedValues.add(rexBuilder.makeNullLiteral(type));
        continue;
      }

      Function<ValueHolder, RexNode> literator = valueHolder -> {
        switch (materializedExpr.getMajorType().getMinorType()) {
          case INT: {
            int value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                ((NullableIntHolder) valueHolder).value : ((IntHolder) valueHolder).value;
            return rexBuilder.makeLiteral(new BigDecimal(value),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.INTEGER, newCall.getType().isNullable()), false);
          }
          case BIGINT: {
            long value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                ((NullableBigIntHolder) valueHolder).value : ((BigIntHolder) valueHolder).value;
            return rexBuilder.makeLiteral(new BigDecimal(value),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.BIGINT, newCall.getType().isNullable()), false);
          }
          case FLOAT4: {
            float value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                ((NullableFloat4Holder) valueHolder).value : ((Float4Holder) valueHolder).value;

            // +Infinity, -Infinity and NaN must be represented as strings since
            // BigDecimal cannot represent them.
            if (!Float.isFinite(value)) {
              return rexBuilder.makeLiteral(Float.toString(value));
            }

            return rexBuilder.makeLiteral(new BigDecimal(value),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.FLOAT, newCall.getType().isNullable()), false);
          }
          case FLOAT8: {
            double value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                ((NullableFloat8Holder) valueHolder).value : ((Float8Holder) valueHolder).value;

            // +Infinity, -Infinity and NaN must be represented as strings since
            // BigDecimal cannot represent them.
            if (!Double.isFinite(value)) {
              return rexBuilder.makeLiteral(Double.toString(value));
            }

            return rexBuilder.makeLiteral(new BigDecimal(value),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.DOUBLE, newCall.getType().isNullable()), false);
          }
          case VARCHAR: {
            String value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                StringFunctionHelpers.getStringFromVarCharHolder((NullableVarCharHolder) valueHolder) :
                StringFunctionHelpers.getStringFromVarCharHolder((VarCharHolder) valueHolder);
            RelDataType type = typeFactory.createSqlType(SqlTypeName.VARCHAR, newCall.getType().getPrecision());
            RelDataType typeWithNullability = typeFactory.createTypeWithNullability(type, newCall.getType().isNullable());
            return rexBuilder.makeLiteral(value, typeWithNullability, false);
          }
          case BIT: {
            boolean value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                ((NullableBitHolder) valueHolder).value == 1 : ((BitHolder) valueHolder).value == 1;
            return rexBuilder.makeLiteral(value,
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.BOOLEAN, newCall.getType().isNullable()), false);
          }
          case DATE: {
            Calendar value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                new DateTime(((NullableDateHolder) valueHolder).value, DateTimeZone.UTC).toCalendar(null) :
                new DateTime(((DateHolder) valueHolder).value, DateTimeZone.UTC).toCalendar(null);
            return rexBuilder.makeLiteral(DateString.fromCalendarFields(value),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.DATE, newCall.getType().isNullable()), false);
          }
          case DECIMAL9: {
            long value;
            int scale;
            if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
              NullableDecimal9Holder decimal9Out = (NullableDecimal9Holder) valueHolder;
              value = decimal9Out.value;
              scale = decimal9Out.scale;
            } else {
              Decimal9Holder decimal9Out = (Decimal9Holder) valueHolder;
              value = decimal9Out.value;
              scale = decimal9Out.scale;
            }
            return rexBuilder.makeLiteral(
                new BigDecimal(BigInteger.valueOf(value), scale),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.DECIMAL, newCall.getType().isNullable()),
                false);
          }
          case DECIMAL18: {
            long value;
            int scale;
            if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
              NullableDecimal18Holder decimal18Out = (NullableDecimal18Holder) valueHolder;
              value = decimal18Out.value;
              scale = decimal18Out.scale;
            } else {
              Decimal18Holder decimal18Out = (Decimal18Holder) valueHolder;
              value = decimal18Out.value;
              scale = decimal18Out.scale;
            }
            return rexBuilder.makeLiteral(
                new BigDecimal(BigInteger.valueOf(value), scale),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.DECIMAL, newCall.getType().isNullable()),
                false);
          }
          case VARDECIMAL: {
            DrillBuf buffer;
            int start;
            int end;
            int scale;
            int precision;
            if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
              NullableVarDecimalHolder varDecimalHolder = (NullableVarDecimalHolder) valueHolder;
              buffer = varDecimalHolder.buffer;
              start = varDecimalHolder.start;
              end = varDecimalHolder.end;
              scale = varDecimalHolder.scale;
              precision = varDecimalHolder.precision;
            } else {
              VarDecimalHolder varDecimalHolder = (VarDecimalHolder) valueHolder;
              buffer = varDecimalHolder.buffer;
              start = varDecimalHolder.start;
              end = varDecimalHolder.end;
              scale = varDecimalHolder.scale;
              precision = varDecimalHolder.precision;
            }
            return rexBuilder.makeLiteral(
                org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromDrillBuf(buffer, start, end - start, scale),
                typeFactory.createSqlType(SqlTypeName.DECIMAL, precision, scale),
                false);
          }
          case DECIMAL28SPARSE: {
            DrillBuf buffer;
            int start;
            int scale;
            if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
              NullableDecimal28SparseHolder decimal28Out = (NullableDecimal28SparseHolder) valueHolder;
              buffer = decimal28Out.buffer;
              start = decimal28Out.start;
              scale = decimal28Out.scale;
            } else {
              Decimal28SparseHolder decimal28Out = (Decimal28SparseHolder) valueHolder;
              buffer = decimal28Out.buffer;
              start = decimal28Out.start;
              scale = decimal28Out.scale;
            }
            return rexBuilder.makeLiteral(
                org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromSparse(buffer, start * 20, 5, scale),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.DECIMAL, newCall.getType().isNullable()), false);
          }
          case DECIMAL38SPARSE: {
            DrillBuf buffer;
            int start;
            int scale;
            if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
              NullableDecimal38SparseHolder decimal38Out = (NullableDecimal38SparseHolder) valueHolder;
              buffer = decimal38Out.buffer;
              start = decimal38Out.start;
              scale = decimal38Out.scale;
            } else {
              Decimal38SparseHolder decimal38Out = (Decimal38SparseHolder) valueHolder;
              buffer = decimal38Out.buffer;
              start = decimal38Out.start;
              scale = decimal38Out.scale;
            }
            return rexBuilder.makeLiteral(org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromSparse(buffer, start * 24, 6, scale),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.DECIMAL, newCall.getType().isNullable()),
                false);
          }
          case TIME: {
            Calendar value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                new DateTime(((NullableTimeHolder) valueHolder).value, DateTimeZone.UTC).toCalendar(null) :
                new DateTime(((TimeHolder) valueHolder).value, DateTimeZone.UTC).toCalendar(null);
            RelDataType type = typeFactory.createSqlType(SqlTypeName.TIME, newCall.getType().getPrecision());
            RelDataType typeWithNullability = typeFactory.createTypeWithNullability(type, newCall.getType().isNullable());
            return rexBuilder.makeLiteral(TimeString.fromCalendarFields(value), typeWithNullability, false);
          }
          case TIMESTAMP: {
            Calendar value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                new DateTime(((NullableTimeStampHolder) valueHolder).value, DateTimeZone.UTC).toCalendar(null) :
                new DateTime(((TimeStampHolder) valueHolder).value, DateTimeZone.UTC).toCalendar(null);
            RelDataType type = typeFactory.createSqlType(SqlTypeName.TIMESTAMP, newCall.getType().getPrecision());
            RelDataType typeWithNullability = typeFactory.createTypeWithNullability(type, newCall.getType().isNullable());
            return rexBuilder.makeLiteral(TimestampString.fromCalendarFields(value), typeWithNullability, false);
          }
          case INTERVALYEAR: {
            BigDecimal value = (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) ?
                new BigDecimal(((NullableIntervalYearHolder) valueHolder).value) :
                new BigDecimal(((IntervalYearHolder) valueHolder).value);
            return rexBuilder.makeLiteral(value,
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.INTERVAL_YEAR_MONTH, newCall.getType().isNullable()), false);
          }
          case INTERVALDAY: {
            int days;
            int milliseconds;
            if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
              NullableIntervalDayHolder intervalDayOut = (NullableIntervalDayHolder) valueHolder;
              days = intervalDayOut.days;
              milliseconds = intervalDayOut.milliseconds;
            } else {
              IntervalDayHolder intervalDayOut = (IntervalDayHolder) valueHolder;
              days = intervalDayOut.days;
              milliseconds = intervalDayOut.milliseconds;
            }
            return rexBuilder.makeLiteral(
                new BigDecimal(days * (long) DateUtilities.daysToStandardMillis + milliseconds),
                TypeInferenceUtils.createCalciteTypeWithNullability(typeFactory, SqlTypeName.INTERVAL_DAY,
                    newCall.getType().isNullable()), false);
          }
          // The list of known unsupported types is used to trigger this behavior of re-using the input expression
          // before the expression is even attempted to be evaluated, this is just here as a last precaution a
          // as new types may be added in the future.
          default:
            logger.debug("Constant expression not folded due to return type {}, complete expression: {}",
                materializedExpr.getMajorType(),
                ExpressionStringBuilder.toString(materializedExpr));
            return newCall;
        }
      };

      reducedValues.add(literator.apply(output));
    }
  }
}
