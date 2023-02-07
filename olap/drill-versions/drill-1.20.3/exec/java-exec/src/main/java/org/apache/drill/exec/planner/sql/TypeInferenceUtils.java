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
package org.apache.drill.exec.planner.sql;

import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlDynamicParam;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.type.SqlTypeName;

import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionCallFactory;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.MajorTypeInLogicalExpression;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.exec.planner.types.DrillRelDataTypeSystem;
import org.apache.drill.exec.resolver.FunctionResolver;
import org.apache.drill.exec.resolver.FunctionResolverFactory;
import org.apache.drill.exec.resolver.TypeCastRules;

import java.util.List;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class TypeInferenceUtils {
  private static final Logger logger = LoggerFactory.getLogger(TypeInferenceUtils.class);

  public static final TypeProtos.MajorType UNKNOWN_TYPE = TypeProtos.MajorType.getDefaultInstance();
  private static final ImmutableMap<TypeProtos.MinorType, SqlTypeName> DRILL_TO_CALCITE_TYPE_MAPPING
      = ImmutableMap.<TypeProtos.MinorType, SqlTypeName> builder()
      .put(TypeProtos.MinorType.INT, SqlTypeName.INTEGER)
      .put(TypeProtos.MinorType.BIGINT, SqlTypeName.BIGINT)
      .put(TypeProtos.MinorType.FLOAT4, SqlTypeName.FLOAT)
      .put(TypeProtos.MinorType.FLOAT8, SqlTypeName.DOUBLE)
      .put(TypeProtos.MinorType.VARCHAR, SqlTypeName.VARCHAR)
      .put(TypeProtos.MinorType.BIT, SqlTypeName.BOOLEAN)
      .put(TypeProtos.MinorType.DATE, SqlTypeName.DATE)
      .put(TypeProtos.MinorType.DECIMAL9, SqlTypeName.DECIMAL)
      .put(TypeProtos.MinorType.DECIMAL18, SqlTypeName.DECIMAL)
      .put(TypeProtos.MinorType.DECIMAL28SPARSE, SqlTypeName.DECIMAL)
      .put(TypeProtos.MinorType.DECIMAL38SPARSE, SqlTypeName.DECIMAL)
      .put(TypeProtos.MinorType.VARDECIMAL, SqlTypeName.DECIMAL)
      .put(TypeProtos.MinorType.TIME, SqlTypeName.TIME)
      .put(TypeProtos.MinorType.TIMESTAMP, SqlTypeName.TIMESTAMP)
      .put(TypeProtos.MinorType.VARBINARY, SqlTypeName.VARBINARY)
      .put(TypeProtos.MinorType.INTERVALYEAR, SqlTypeName.INTERVAL_YEAR_MONTH)
      .put(TypeProtos.MinorType.INTERVALDAY, SqlTypeName.INTERVAL_DAY)
      .put(TypeProtos.MinorType.MAP, SqlTypeName.MAP)
      .put(TypeProtos.MinorType.LIST, SqlTypeName.ARRAY)
      .put(TypeProtos.MinorType.LATE, SqlTypeName.ANY)

      // These are defined in the Drill type system but have been turned off for now
      // .put(TypeProtos.MinorType.TINYINT, SqlTypeName.TINYINT)
      // .put(TypeProtos.MinorType.SMALLINT, SqlTypeName.SMALLINT)
      // Calcite types currently not supported by Drill, nor defined in the Drill type list:
      //      - CHAR, SYMBOL, MULTISET, DISTINCT, STRUCTURED, ROW, OTHER, CURSOR, COLUMN_LIST
      .build();

  private static final ImmutableMap<SqlTypeName, TypeProtos.MinorType> CALCITE_TO_DRILL_MAPPING
      = ImmutableMap.<SqlTypeName, TypeProtos.MinorType> builder()
      .put(SqlTypeName.INTEGER, TypeProtos.MinorType.INT)
      .put(SqlTypeName.BIGINT, TypeProtos.MinorType.BIGINT)
      .put(SqlTypeName.FLOAT, TypeProtos.MinorType.FLOAT4)
      .put(SqlTypeName.DOUBLE, TypeProtos.MinorType.FLOAT8)
      .put(SqlTypeName.VARCHAR, TypeProtos.MinorType.VARCHAR)
      .put(SqlTypeName.BOOLEAN, TypeProtos.MinorType.BIT)
      .put(SqlTypeName.DATE, TypeProtos.MinorType.DATE)
      .put(SqlTypeName.TIME, TypeProtos.MinorType.TIME)
      .put(SqlTypeName.TIMESTAMP, TypeProtos.MinorType.TIMESTAMP)
      .put(SqlTypeName.VARBINARY, TypeProtos.MinorType.VARBINARY)
      .put(SqlTypeName.INTERVAL_YEAR, TypeProtos.MinorType.INTERVALYEAR)
      .put(SqlTypeName.INTERVAL_YEAR_MONTH, TypeProtos.MinorType.INTERVALYEAR)
      .put(SqlTypeName.INTERVAL_MONTH, TypeProtos.MinorType.INTERVALYEAR)
      .put(SqlTypeName.INTERVAL_DAY, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_DAY_HOUR, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_DAY_MINUTE, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_DAY_SECOND, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_HOUR, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_HOUR_MINUTE, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_HOUR_SECOND, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_MINUTE, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_MINUTE_SECOND, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.INTERVAL_SECOND, TypeProtos.MinorType.INTERVALDAY)
      .put(SqlTypeName.DECIMAL, TypeProtos.MinorType.VARDECIMAL)

      // SqlTypeName.CHAR is the type for Literals in Calcite, Drill treats Literals as VARCHAR also
      .put(SqlTypeName.CHAR, TypeProtos.MinorType.VARCHAR)

      // The following types are not added due to a variety of reasons:
      // (1) Disabling decimal type
      //.put(SqlTypeName.DECIMAL, TypeProtos.MinorType.DECIMAL9)
      //.put(SqlTypeName.DECIMAL, TypeProtos.MinorType.DECIMAL18)
      //.put(SqlTypeName.DECIMAL, TypeProtos.MinorType.DECIMAL28SPARSE)
      //.put(SqlTypeName.DECIMAL, TypeProtos.MinorType.DECIMAL38SPARSE)

      // (2) These 2 types are defined in the Drill type system but have been turned off for now
      // .put(SqlTypeName.TINYINT, TypeProtos.MinorType.TINYINT)
      // .put(SqlTypeName.SMALLINT, TypeProtos.MinorType.SMALLINT)

      // (3) Calcite types currently not supported by Drill, nor defined in the Drill type list:
      //      - SYMBOL, MULTISET, DISTINCT, STRUCTURED, ROW, OTHER, CURSOR, COLUMN_LIST
      // .put(SqlTypeName.MAP, TypeProtos.MinorType.MAP)
      // .put(SqlTypeName.ARRAY, TypeProtos.MinorType.LIST)
      .build();

  private static final ImmutableMap<String, SqlReturnTypeInference> funcNameToInference = ImmutableMap.<String, SqlReturnTypeInference> builder()
      .put("DATE_PART", DrillDatePartSqlReturnTypeInference.INSTANCE)
      .put(SqlStdOperatorTable.TIMESTAMP_ADD.getName(), DrillTimestampAddTypeInference.INSTANCE)
      .put(SqlKind.SUM.name(), DrillSumSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.COUNT.name(), DrillCountSqlReturnTypeInference.INSTANCE)
      .put("CONCAT", DrillConcatSqlReturnTypeInference.INSTANCE_CONCAT)
      .put("CONCATOPERATOR", DrillConcatSqlReturnTypeInference.INSTANCE_CONCAT_OP)
      .put("LENGTH", DrillLengthSqlReturnTypeInference.INSTANCE)
      .put("LPAD", DrillPadSqlReturnTypeInference.INSTANCE)
      .put("RPAD", DrillPadSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.LTRIM.name(), DrillTrimSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.RTRIM.name(), DrillTrimSqlReturnTypeInference.INSTANCE)
      .put("BTRIM", DrillTrimSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.TRIM.name(), DrillTrimSqlReturnTypeInference.INSTANCE)
      .put("CONVERT_TO", DrillConvertToSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.EXTRACT.name(), DrillExtractSqlReturnTypeInference.INSTANCE)
      .put("SQRT", DrillSqrtSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.CAST.name(), DrillCastSqlReturnTypeInference.INSTANCE)
      .put("FLATTEN", DrillDeferToExecSqlReturnTypeInference.INSTANCE)
      .put("KVGEN", DrillDeferToExecSqlReturnTypeInference.INSTANCE)
      .put("CONVERT_FROM", DrillDeferToExecSqlReturnTypeInference.INSTANCE)

      // Functions that return the same type
      .put("LOWER", DrillSameSqlReturnTypeInference.THE_SAME_RETURN_TYPE)
      .put("UPPER", DrillSameSqlReturnTypeInference.THE_SAME_RETURN_TYPE)
      .put("INITCAP", DrillSameSqlReturnTypeInference.THE_SAME_RETURN_TYPE)
      .put("REVERSE", DrillSameSqlReturnTypeInference.THE_SAME_RETURN_TYPE)

      // Window Functions
      // RANKING
      .put(SqlKind.CUME_DIST.name(), DrillRankingSqlReturnTypeInference.INSTANCE_DOUBLE)
      .put(SqlKind.DENSE_RANK.name(), DrillRankingSqlReturnTypeInference.INSTANCE_BIGINT)
      .put(SqlKind.PERCENT_RANK.name(), DrillRankingSqlReturnTypeInference.INSTANCE_DOUBLE)
      .put(SqlKind.RANK.name(), DrillRankingSqlReturnTypeInference.INSTANCE_BIGINT)
      .put(SqlKind.ROW_NUMBER.name(), DrillRankingSqlReturnTypeInference.INSTANCE_BIGINT)

      // NTILE
      .put(SqlKind.NTILE.name(), DrillNTILESqlReturnTypeInference.INSTANCE)

      // LEAD, LAG
      .put(SqlKind.LEAD.name(), DrillLeadLagSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.LAG.name(), DrillLeadLagSqlReturnTypeInference.INSTANCE)

      // FIRST_VALUE, LAST_VALUE
      .put(SqlKind.FIRST_VALUE.name(), DrillSameSqlReturnTypeInference.THE_SAME_RETURN_TYPE)
      .put(SqlKind.LAST_VALUE.name(), DrillSameSqlReturnTypeInference.THE_SAME_RETURN_TYPE)

      // Functions rely on DrillReduceAggregatesRule for expression simplification as opposed to getting evaluated directly
      .put(SqlKind.AVG.name(), DrillAvgAggSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.STDDEV_POP.name(), DrillAvgAggSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.STDDEV_SAMP.name(), DrillAvgAggSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.VAR_POP.name(), DrillAvgAggSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.VAR_SAMP.name(), DrillAvgAggSqlReturnTypeInference.INSTANCE)
      .put(SqlKind.MIN.name(), DrillSameSqlReturnTypeInference.ALL_NULLABLE)
      .put(SqlKind.MAX.name(), DrillSameSqlReturnTypeInference.ALL_NULLABLE)
      .build();

  /**
   * Set of the decimal functions which return type cannot be determined exactly for some reasons at the current stage.
   * For example functions which takes as an parameter scale or precision of return type.
   */
  private static final Set<String> SET_SCALE_DECIMAL_FUNCTIONS = ImmutableSet.<String> builder()
      .add("ROUND")
      .add("TRUNC")
      .add("TRUNCATE")
      .build();

  /**
   * Given a Drill's TypeProtos.MinorType, return a Calcite's corresponding SqlTypeName
   */
  public static SqlTypeName getCalciteTypeFromDrillType(final TypeProtos.MinorType type) {
    if(!DRILL_TO_CALCITE_TYPE_MAPPING.containsKey(type)) {
      return SqlTypeName.ANY;
    }

    return DRILL_TO_CALCITE_TYPE_MAPPING.get(type);
  }

  /**
   * Given a Calcite's RelDataType, return a Drill's corresponding TypeProtos.MinorType
   */
  public static TypeProtos.MinorType getDrillTypeFromCalciteType(final RelDataType relDataType) {
    final SqlTypeName sqlTypeName = relDataType.getSqlTypeName();
    return getDrillTypeFromCalciteType(sqlTypeName);
  }

  /**
   * Returns {@link TypeProtos.MajorType} instance which corresponds to specified {@code RelDataType relDataType}
   * with its nullability, scale and precision if it is available.
   *
   * @param relDataType RelDataType to convert
   * @return {@link TypeProtos.MajorType} instance
   */
  public static TypeProtos.MajorType getDrillMajorTypeFromCalciteType(RelDataType relDataType) {
    final SqlTypeName sqlTypeName = relDataType.getSqlTypeName();

    TypeProtos.MinorType minorType = getDrillTypeFromCalciteType(sqlTypeName);
    TypeProtos.MajorType.Builder typeBuilder = TypeProtos.MajorType.newBuilder().setMinorType(minorType);
    switch (minorType) {
      case VAR16CHAR:
      case VARCHAR:
      case VARBINARY:
      case TIMESTAMP:
        if (relDataType.getPrecision() > 0) {
          typeBuilder.setPrecision(relDataType.getPrecision());
        }
        break;
      case VARDECIMAL:
        typeBuilder.setPrecision(relDataType.getPrecision());
        typeBuilder.setScale(relDataType.getScale());
    }
    if (relDataType.isNullable()) {
      typeBuilder.setMode(TypeProtos.DataMode.OPTIONAL);
    } else {
      typeBuilder.setMode(TypeProtos.DataMode.REQUIRED);
    }
    return typeBuilder.build();
  }

  /**
   * Given a Calcite's SqlTypeName, return a Drill's corresponding TypeProtos.MinorType
   */
  public static TypeProtos.MinorType getDrillTypeFromCalciteType(final SqlTypeName sqlTypeName) {
    if(!CALCITE_TO_DRILL_MAPPING.containsKey(sqlTypeName)) {
      return TypeProtos.MinorType.LATE;
    }

    return CALCITE_TO_DRILL_MAPPING.get(sqlTypeName);
  }

  /**
   * Give the name and DrillFuncHolder list, return the inference mechanism.
   */
  public static SqlReturnTypeInference getDrillSqlReturnTypeInference(
      final String name,
      final List<DrillFuncHolder> functions) {

    final String nameCap = name.toUpperCase();
    if(funcNameToInference.containsKey(nameCap)) {
      return funcNameToInference.get(nameCap);
    } else {
      return new DrillDefaultSqlReturnTypeInference(functions);
    }
  }

  /**
   * Checks if given type is string scalar type.
   *
   * @param sqlTypeName Calcite's sql type name
   * @return true if given type is string scalar type
   */
  public static boolean isScalarStringType(final SqlTypeName sqlTypeName) {
    return sqlTypeName == SqlTypeName.VARCHAR || sqlTypeName == SqlTypeName.CHAR;
  }

  private static class DrillDefaultSqlReturnTypeInference implements SqlReturnTypeInference {
    private final List<DrillFuncHolder> functions;

    public DrillDefaultSqlReturnTypeInference(List<DrillFuncHolder> functions) {
      this.functions = functions;
    }

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      if (functions.isEmpty()) {
        return factory.createTypeWithNullability(
            factory.createSqlType(SqlTypeName.ANY),
            true);
      }

      // The following logic is just a safe play:
      // Even if any of the input arguments has ANY type,
      // it "might" still be possible to determine the return type based on other non-ANY types
      for (RelDataType type : opBinding.collectOperandTypes()) {
        if (getDrillTypeFromCalciteType(type) == TypeProtos.MinorType.LATE) {
          // This code for boolean output type is added for addressing DRILL-1729
          // In summary, if we have a boolean output function in the WHERE-CLAUSE,
          // this logic can validate and execute user queries seamlessly
          boolean allBooleanOutput = true;
          boolean isNullable = false;
          for (DrillFuncHolder function : functions) {
            if (function.getReturnType().getMinorType() != TypeProtos.MinorType.BIT) {
              allBooleanOutput = false;
              break;
            }
            if (function.getReturnType().getMode() == TypeProtos.DataMode.OPTIONAL
                || function.getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL) {
              isNullable = true;
            }
          }

          if (allBooleanOutput) {
            return factory.createTypeWithNullability(
                factory.createSqlType(SqlTypeName.BOOLEAN), isNullable);
          } else {
            return factory.createTypeWithNullability(
                factory.createSqlType(SqlTypeName.ANY),
                true);
          }
        } else if (SET_SCALE_DECIMAL_FUNCTIONS.contains(opBinding.getOperator().getName())
            && getDrillTypeFromCalciteType(type) == TypeProtos.MinorType.VARDECIMAL) {
          return factory.createTypeWithNullability(
              factory.createSqlType(SqlTypeName.ANY),
              true);
        }
      }

      final FunctionCall functionCall = convertSqlOperatorBindingToFunctionCall(opBinding);
      final DrillFuncHolder func = resolveDrillFuncHolder(opBinding, functions, functionCall);

      final RelDataType returnType = getReturnType(opBinding,
          func.getReturnType(functionCall.args()), func.getNullHandling());

      return returnType.getSqlTypeName() == SqlTypeName.VARBINARY
          ? createCalciteTypeWithNullability(factory, SqlTypeName.ANY, returnType.isNullable())
              : returnType;
    }

    private static RelDataType getReturnType(final SqlOperatorBinding opBinding,
        final TypeProtos.MajorType returnType, FunctionTemplate.NullHandling nullHandling) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();

      // least restrictive type (nullable ANY type)
      final RelDataType nullableAnyType = factory.createTypeWithNullability(
          factory.createSqlType(SqlTypeName.ANY),
          true);

      if (UNKNOWN_TYPE.equals(returnType)) {
        return nullableAnyType;
      }

      final TypeProtos.MinorType minorType = returnType.getMinorType();
      final SqlTypeName sqlTypeName = getCalciteTypeFromDrillType(minorType);
      if (sqlTypeName == null) {
        return nullableAnyType;
      }

      final boolean isNullable;
      switch (returnType.getMode()) {
        case REPEATED:
        case OPTIONAL:
          isNullable = true;
          break;

        case REQUIRED:
          switch (nullHandling) {
            case INTERNAL:
              isNullable = false;
              break;

            case NULL_IF_NULL:
              boolean isNull = false;
              for (int i = 0; i < opBinding.getOperandCount(); ++i) {
                if (opBinding.getOperandType(i).isNullable()) {
                  isNull = true;
                  break;
                }
              }

              isNullable = isNull;
              break;
            default:
              throw new UnsupportedOperationException();
          }
          break;

        default:
          throw new UnsupportedOperationException();
      }

      return convertToCalciteType(factory, returnType, isNullable);
    }
  }

  private static class DrillDeferToExecSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillDeferToExecSqlReturnTypeInference INSTANCE = new DrillDeferToExecSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      return factory.createTypeWithNullability(
          factory.createSqlType(SqlTypeName.ANY),
          true);
    }
  }

  private static class DrillSumSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillSumSqlReturnTypeInference INSTANCE = new DrillSumSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      // If there is group-by and the imput type is Non-nullable,
      // the output is Non-nullable;
      // Otherwise, the output is nullable.
      final boolean isNullable = opBinding.getGroupCount() == 0
          || opBinding.getOperandType(0).isNullable();

      if(getDrillTypeFromCalciteType(opBinding.getOperandType(0)) == TypeProtos.MinorType.LATE) {
        return createCalciteTypeWithNullability(
            factory,
            SqlTypeName.ANY,
            isNullable);
      }

      // Determines SqlTypeName of the result.
      // For the case when input may be implicitly casted to BIGINT, the type of result is BIGINT.
      // Else for the case when input may be implicitly casted to FLOAT4, the type of result is DOUBLE.
      // Else for the case when input may be implicitly casted to VARDECIMAL, the type of result is DECIMAL
      // with the same scale as input and max allowed numeric precision.
      // Else for the case when input may be implicitly casted to FLOAT8, the type of result is DOUBLE.
      // When none of these conditions is satisfied, error is thrown.
      // This order of checks is caused by the order of types in ResolverTypePrecedence.precedenceMap
      final RelDataType operandType = opBinding.getOperandType(0);
      final TypeProtos.MinorType inputMinorType = getDrillTypeFromCalciteType(operandType);
      if (TypeCastRules.getLeastRestrictiveType(Lists.newArrayList(inputMinorType, TypeProtos.MinorType.BIGINT))
          == TypeProtos.MinorType.BIGINT) {
        return createCalciteTypeWithNullability(
            factory,
            SqlTypeName.BIGINT,
            isNullable);
      } else if (TypeCastRules.getLeastRestrictiveType(Lists.newArrayList(inputMinorType, TypeProtos.MinorType.FLOAT4))
          == TypeProtos.MinorType.FLOAT4) {
        return createCalciteTypeWithNullability(
            factory,
            SqlTypeName.DOUBLE,
            isNullable);
      } else if (TypeCastRules.getLeastRestrictiveType(Lists.newArrayList(inputMinorType, TypeProtos.MinorType.VARDECIMAL))
          == TypeProtos.MinorType.VARDECIMAL) {
        RelDataType sqlType = factory.createSqlType(SqlTypeName.DECIMAL,
          DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision(),
          Math.min(operandType.getScale(),
            DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericScale()));
        return factory.createTypeWithNullability(sqlType, isNullable);
      } else if (TypeCastRules.getLeastRestrictiveType(Lists.newArrayList(inputMinorType, TypeProtos.MinorType.FLOAT8))
          == TypeProtos.MinorType.FLOAT8) {
        return createCalciteTypeWithNullability(
            factory,
            SqlTypeName.DOUBLE,
            isNullable);
      } else {
        throw UserException
            .functionError()
            .message(String.format("%s does not support operand types (%s)",
                opBinding.getOperator().getName(),
                opBinding.getOperandType(0).getSqlTypeName()))
            .build(logger);
      }
    }
  }

  private static class DrillCountSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillCountSqlReturnTypeInference INSTANCE = new DrillCountSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      final SqlTypeName type = SqlTypeName.BIGINT;
      return createCalciteTypeWithNullability(
          factory,
          type,
          false);
    }
  }

  private static class DrillConcatSqlReturnTypeInference implements SqlReturnTypeInference {
    // Difference between concat function and concat operator ('||') is that concat function resolves nulls internally,
    // i.e. does not return nulls at all.
    private static final DrillConcatSqlReturnTypeInference INSTANCE_CONCAT = new DrillConcatSqlReturnTypeInference(false);
    private static final DrillConcatSqlReturnTypeInference INSTANCE_CONCAT_OP = new DrillConcatSqlReturnTypeInference(true);

    private final boolean isNullIfNull;

    public DrillConcatSqlReturnTypeInference(boolean isNullIfNull) {
      this.isNullIfNull = isNullIfNull;
    }

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {

      // If the underlying columns cannot offer information regarding the precision of the VarChar,
      // Drill uses the largest to represent it.
      int totalPrecision = 0;
      for (RelDataType relDataType : opBinding.collectOperandTypes()) {
        if (isScalarStringType(relDataType.getSqlTypeName()) && relDataType.getPrecision() != RelDataType.PRECISION_NOT_SPECIFIED) {
          totalPrecision += relDataType.getPrecision();
        } else {
          totalPrecision = Types.MAX_VARCHAR_LENGTH;
          break;
        }
      }

      totalPrecision = totalPrecision > Types.MAX_VARCHAR_LENGTH ? Types.MAX_VARCHAR_LENGTH : totalPrecision;
      boolean isNullable = isNullIfNull && isNullable(opBinding.collectOperandTypes());

      return opBinding.getTypeFactory().createTypeWithNullability(
          opBinding.getTypeFactory().createSqlType(SqlTypeName.VARCHAR, totalPrecision),
          isNullable);
    }
  }

  private static class DrillLengthSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillLengthSqlReturnTypeInference INSTANCE = new DrillLengthSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      final SqlTypeName sqlTypeName = SqlTypeName.BIGINT;

      // We need to check only the first argument because
      // the second one is used to represent encoding type
      final boolean isNullable = opBinding.getOperandType(0).isNullable();
      return createCalciteTypeWithNullability(
          factory,
          sqlTypeName,
          isNullable);
    }
  }

  private static class DrillPadSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillPadSqlReturnTypeInference INSTANCE = new DrillPadSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      if (opBinding instanceof SqlCallBinding && (((SqlCallBinding) opBinding).operand(1) instanceof  SqlNumericLiteral)) {
        int precision = ((SqlNumericLiteral) ((SqlCallBinding) opBinding).operand(1)).intValue(true);
        RelDataType sqlType = opBinding.getTypeFactory().createSqlType(SqlTypeName.VARCHAR, Math.max(precision, 0));
        return opBinding.getTypeFactory().createTypeWithNullability(sqlType, isNullable(opBinding.collectOperandTypes()));
      }

      return createCalciteTypeWithNullability(
          opBinding.getTypeFactory(),
          SqlTypeName.VARCHAR,
          isNullable(opBinding.collectOperandTypes()));

    }
  }

  private static class DrillTrimSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillTrimSqlReturnTypeInference INSTANCE = new DrillTrimSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      return createCalciteTypeWithNullability(
          opBinding.getTypeFactory(),
          SqlTypeName.VARCHAR,
          isNullable(opBinding.collectOperandTypes()));
    }
  }

  private static class DrillTimestampAddTypeInference implements SqlReturnTypeInference {
    private static final SqlReturnTypeInference INSTANCE = new DrillTimestampAddTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      RelDataTypeFactory factory = opBinding.getTypeFactory();
      // operands count ond order is checked at parsing stage
      RelDataType inputType = opBinding.getOperandType(2);
      boolean isNullable = inputType.isNullable() || opBinding.getOperandType(1).isNullable();

      SqlTypeName inputTypeName = inputType.getSqlTypeName();

      TimeUnit qualifier = ((SqlLiteral) ((SqlCallBinding) opBinding).operand(0)).getValueAs(TimeUnit.class);

      SqlTypeName sqlTypeName;

      // follow up with type inference of reduced expression
      switch (qualifier) {
        case DAY:
        case WEEK:
        case MONTH:
        case QUARTER:
        case YEAR:
        case NANOSECOND:  // NANOSECOND is not supported by Calcite SqlTimestampAddFunction.
                          // Once it is fixed, NANOSECOND should be moved to the group below.
          sqlTypeName = inputTypeName;
          break;
        case MICROSECOND:
        case MILLISECOND:
          // precision should be specified for MICROSECOND and MILLISECOND
          return factory.createTypeWithNullability(
              factory.createSqlType(SqlTypeName.TIMESTAMP, 3),
              isNullable);
        case SECOND:
        case MINUTE:
        case HOUR:
          if (inputTypeName == SqlTypeName.TIME) {
            sqlTypeName = SqlTypeName.TIME;
          } else {
            sqlTypeName = SqlTypeName.TIMESTAMP;
          }
          break;
        default:
          sqlTypeName = SqlTypeName.ANY;
      }

      // preserves precision of input type if it was specified
      if (inputType.getSqlTypeName().allowsPrecNoScale()) {
        RelDataType type = factory.createSqlType(sqlTypeName, inputType.getPrecision());
        return factory.createTypeWithNullability(type, isNullable);
      }
      return createCalciteTypeWithNullability(
          opBinding.getTypeFactory(),
          sqlTypeName,
          isNullable);
    }
  }

  private static class DrillSubstringSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillSubstringSqlReturnTypeInference INSTANCE = new DrillSubstringSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      boolean isNullable = isNullable(opBinding.collectOperandTypes());

      boolean isScalarString = isScalarStringType(opBinding.getOperandType(0).getSqlTypeName());
      int precision = opBinding.getOperandType(0).getPrecision();

      if (isScalarString && precision != RelDataType.PRECISION_NOT_SPECIFIED) {
        RelDataType sqlType = opBinding.getTypeFactory().createSqlType(SqlTypeName.VARCHAR, precision);
        return opBinding.getTypeFactory().createTypeWithNullability(sqlType, isNullable);
      }

      return createCalciteTypeWithNullability(
          opBinding.getTypeFactory(),
          SqlTypeName.VARCHAR,
          isNullable);
    }
  }

  private static class DrillConvertToSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillConvertToSqlReturnTypeInference INSTANCE = new DrillConvertToSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      final SqlTypeName type = SqlTypeName.VARBINARY;

      return createCalciteTypeWithNullability(
          factory, type, opBinding.getOperandType(0).isNullable());
    }
  }

  private static class DrillExtractSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillExtractSqlReturnTypeInference INSTANCE = new DrillExtractSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      final TimeUnit timeUnit = opBinding.getOperandType(0).getIntervalQualifier().getStartUnit();
      final boolean isNullable = opBinding.getOperandType(1).isNullable();

      final SqlTypeName sqlTypeName = getSqlTypeNameForTimeUnit(timeUnit.name());
      return createCalciteTypeWithNullability(
          factory,
          sqlTypeName,
          isNullable);
    }
  }

  private static class DrillSqrtSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillSqrtSqlReturnTypeInference INSTANCE = new DrillSqrtSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      final boolean isNullable = opBinding.getOperandType(0).isNullable();
      return createCalciteTypeWithNullability(
          factory,
          SqlTypeName.DOUBLE,
          isNullable);
    }
  }

  private static class DrillDatePartSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillDatePartSqlReturnTypeInference INSTANCE = new DrillDatePartSqlReturnTypeInference();

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      final boolean isNullable = opBinding.getOperandType(1).isNullable();

      if (!(opBinding instanceof SqlCallBinding) || !(((SqlCallBinding) opBinding).operand(0) instanceof SqlCharStringLiteral)) {
        return createCalciteTypeWithNullability(factory,
            SqlTypeName.ANY,
            isNullable);
      }

      final String part = ((SqlCharStringLiteral) ((SqlCallBinding) opBinding).operand(0))
          .getNlsString()
          .getValue()
          .toUpperCase();

      final SqlTypeName sqlTypeName = getSqlTypeNameForTimeUnit(part);
      return createCalciteTypeWithNullability(
          factory,
          sqlTypeName,
          isNullable);
    }
  }

  private static class DrillCastSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillCastSqlReturnTypeInference INSTANCE = new DrillCastSqlReturnTypeInference();

    @Override
    @SuppressWarnings("deprecation")
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      final boolean isNullable = opBinding
          .getOperandType(0)
          .isNullable();

      RelDataType ret = factory.createTypeWithNullability(
          opBinding.getOperandType(1),
          isNullable);
      if (opBinding instanceof SqlCallBinding) {
        SqlCallBinding callBinding = (SqlCallBinding) opBinding;
        SqlNode operand0 = callBinding.operand(0);

        // dynamic parameters and null constants need their types assigned
        // to them using the type they are casted to.
        if(((operand0 instanceof SqlLiteral)
            && (((SqlLiteral) operand0).getValue() == null))
                || (operand0 instanceof SqlDynamicParam)) {
          callBinding.getValidator().setValidatedNodeType(
              operand0,
              ret);
        }
      }

      return ret;
    }
  }

  private static class DrillRankingSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillRankingSqlReturnTypeInference INSTANCE_BIGINT = new DrillRankingSqlReturnTypeInference(SqlTypeName.BIGINT);
    private static final DrillRankingSqlReturnTypeInference INSTANCE_DOUBLE = new DrillRankingSqlReturnTypeInference(SqlTypeName.DOUBLE);

    private final SqlTypeName returnType;
    private DrillRankingSqlReturnTypeInference(final SqlTypeName returnType) {
      this.returnType = returnType;
    }

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      return createCalciteTypeWithNullability(
          opBinding.getTypeFactory(),
          returnType,
          false);
    }
  }

  private static class DrillNTILESqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillNTILESqlReturnTypeInference INSTANCE = new DrillNTILESqlReturnTypeInference();
    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      return createCalciteTypeWithNullability(
          opBinding.getTypeFactory(),
          SqlTypeName.INTEGER,
          opBinding.getOperandType(0).isNullable());
    }
  }

  private static class DrillLeadLagSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillLeadLagSqlReturnTypeInference INSTANCE = new DrillLeadLagSqlReturnTypeInference();
    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      return opBinding.getTypeFactory().createTypeWithNullability(opBinding.getOperandType(0), true);
    }
  }

  private static class DrillSameSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillSameSqlReturnTypeInference THE_SAME_RETURN_TYPE = new DrillSameSqlReturnTypeInference(true);
    private static final DrillSameSqlReturnTypeInference ALL_NULLABLE = new DrillSameSqlReturnTypeInference(false);

    private final boolean preserveNullability;

    public DrillSameSqlReturnTypeInference(boolean preserveNullability) {
      this.preserveNullability = preserveNullability;
    }

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      if (preserveNullability) {
        return opBinding.getOperandType(0);
      }
      return opBinding.getTypeFactory().createTypeWithNullability(opBinding.getOperandType(0), true);
    }
  }

  private static class DrillAvgAggSqlReturnTypeInference implements SqlReturnTypeInference {
    private static final DrillAvgAggSqlReturnTypeInference INSTANCE = new DrillAvgAggSqlReturnTypeInference();
    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      final RelDataTypeFactory factory = opBinding.getTypeFactory();
      // If there is group-by and the imput type is Non-nullable,
      // the output is Non-nullable;
      // Otherwise, the output is nullable.
      final boolean isNullable = opBinding.getGroupCount() == 0
          || opBinding.getOperandType(0).isNullable();

      if (getDrillTypeFromCalciteType(opBinding.getOperandType(0)) == TypeProtos.MinorType.LATE) {
        return createCalciteTypeWithNullability(
            factory,
            SqlTypeName.ANY,
            isNullable);
      }

      // Determines SqlTypeName of the result.
      // For the case when input may be implicitly casted to FLOAT4, the type of result is DOUBLE.
      // Else for the case when input may be implicitly casted to VARDECIMAL, the type of result is DECIMAL
      // with scale max(6, input) and max allowed numeric precision.
      // Else for the case when input may be implicitly casted to FLOAT8, the type of result is DOUBLE.
      // When none of these conditions is satisfied, error is thrown.
      // This order of checks is caused by the order of types in ResolverTypePrecedence.precedenceMap
      final RelDataType operandType = opBinding.getOperandType(0);
      final TypeProtos.MinorType inputMinorType = getDrillTypeFromCalciteType(operandType);
      if (TypeCastRules.getLeastRestrictiveType(Lists.newArrayList(inputMinorType, TypeProtos.MinorType.FLOAT4))
          == TypeProtos.MinorType.FLOAT4) {
        return createCalciteTypeWithNullability(
            factory,
            SqlTypeName.DOUBLE,
            isNullable);
      } else if (TypeCastRules.getLeastRestrictiveType(Lists.newArrayList(inputMinorType, TypeProtos.MinorType.VARDECIMAL))
          == TypeProtos.MinorType.VARDECIMAL) {
        RelDataType sqlType = factory.createSqlType(SqlTypeName.DECIMAL,
            DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision(),
            Math.min(Math.max(6, operandType.getScale()),
                DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericScale()));
        return factory.createTypeWithNullability(sqlType, isNullable);
      } else if (TypeCastRules.getLeastRestrictiveType(Lists.newArrayList(inputMinorType, TypeProtos.MinorType.FLOAT8))
          == TypeProtos.MinorType.FLOAT8) {
        return createCalciteTypeWithNullability(
            factory,
            SqlTypeName.DOUBLE,
            isNullable);
      } else {
        throw UserException
            .functionError()
            .message(String.format("%s does not support operand types (%s)",
                opBinding.getOperator().getName(),
                opBinding.getOperandType(0).getSqlTypeName()))
            .build(logger);
      }
    }
  }

  private static DrillFuncHolder resolveDrillFuncHolder(final SqlOperatorBinding opBinding,
      final List<DrillFuncHolder> functions, FunctionCall functionCall) {
    final FunctionResolver functionResolver = FunctionResolverFactory.getResolver(functionCall);
    final DrillFuncHolder func = functionResolver.getBestMatch(functions, functionCall);

    // Throw an exception
    // if no DrillFuncHolder matched for the given list of operand types
    if (func == null) {
      StringBuilder operandTypes = new StringBuilder();
      for (int i = 0; i < opBinding.getOperandCount(); ++i) {
        RelDataType operandType = opBinding.getOperandType(i);
        operandTypes.append(operandType.getSqlTypeName());
        if (operandType.isNullable()) {
          operandTypes.append(":OPTIONAL");
        }
        if (i < opBinding.getOperandCount() - 1) {
          operandTypes.append(",");
        }
      }

      throw UserException
          .functionError()
          .message(String.format("%s does not support operand types (%s)",
              opBinding.getOperator().getName(),
              operandTypes.toString()))
          .build(logger);
    }
    return func;
  }

  /**
   * For Extract and date_part functions, infer the return types based on timeUnit
   */
  public static SqlTypeName getSqlTypeNameForTimeUnit(String timeUnitStr) {
    TimeUnit timeUnit = TimeUnit.valueOf(timeUnitStr);
    switch (timeUnit) {
      case YEAR:
      case MONTH:
      case WEEK:
      case DAY:
      case HOUR:
      case MINUTE:
        return SqlTypeName.BIGINT;
      case SECOND:
        return SqlTypeName.DOUBLE;
      default:
        throw UserException
            .functionError()
            .message("extract function supports the following time units: YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND")
            .build(logger);
    }
  }

  /**
   * Given a {@link SqlTypeName} and nullability, create a RelDataType from the RelDataTypeFactory
   *
   * @param typeFactory RelDataTypeFactory used to create the RelDataType
   * @param sqlTypeName the given SqlTypeName
   * @param isNullable  the nullability of the created RelDataType
   * @return RelDataType Type of call
   */
  public static RelDataType createCalciteTypeWithNullability(RelDataTypeFactory typeFactory,
                                                             SqlTypeName sqlTypeName,
                                                             boolean isNullable) {
    RelDataType type;
    if (sqlTypeName.getFamily() == SqlTypeFamily.INTERVAL_DAY_TIME) {
      type = typeFactory.createSqlIntervalType(
          new SqlIntervalQualifier(
              TimeUnit.DAY,
              TimeUnit.MINUTE,
              SqlParserPos.ZERO));
    } else if (sqlTypeName.getFamily() == SqlTypeFamily.INTERVAL_YEAR_MONTH) {
      type = typeFactory.createSqlIntervalType(
          new SqlIntervalQualifier(
              TimeUnit.YEAR,
              TimeUnit.MONTH,
              SqlParserPos.ZERO));
    } else if (sqlTypeName == SqlTypeName.VARCHAR) {
      type = typeFactory.createSqlType(sqlTypeName, Types.MAX_VARCHAR_LENGTH);
    } else {
      type = typeFactory.createSqlType(sqlTypeName);
    }
    return typeFactory.createTypeWithNullability(type, isNullable);
  }

  /**
   * Creates a RelDataType using specified RelDataTypeFactory which corresponds to specified TypeProtos.MajorType.
   *
   * @param typeFactory RelDataTypeFactory used to create the RelDataType
   * @param drillType   the given TypeProtos.MajorType
   * @param isNullable  nullability of the resulting type
   * @return RelDataType which corresponds to specified TypeProtos.MajorType
   */
  public static RelDataType convertToCalciteType(RelDataTypeFactory typeFactory,
                                                 TypeProtos.MajorType drillType, boolean isNullable) {
    SqlTypeName sqlTypeName = getCalciteTypeFromDrillType(drillType.getMinorType());
    if (sqlTypeName == SqlTypeName.DECIMAL) {
      return typeFactory.createTypeWithNullability(
          typeFactory.createSqlType(sqlTypeName, drillType.getPrecision(),
              drillType.getScale()), isNullable);
    }
    return createCalciteTypeWithNullability(typeFactory, sqlTypeName, isNullable);
  }

  /**
   * Given a SqlOperatorBinding, convert it to FunctionCall
   * @param  opBinding    the given SqlOperatorBinding
   * @return FunctionCall the converted FunctionCall
   */
  public static FunctionCall convertSqlOperatorBindingToFunctionCall(final SqlOperatorBinding opBinding) {
    final List<LogicalExpression> args = Lists.newArrayList();

    for (int i = 0; i < opBinding.getOperandCount(); ++i) {
      final RelDataType type = opBinding.getOperandType(i);
      final TypeProtos.MinorType minorType = getDrillTypeFromCalciteType(type);
      TypeProtos.DataMode dataMode =
          type.isNullable() ? TypeProtos.DataMode.OPTIONAL : TypeProtos.DataMode.REQUIRED;

      TypeProtos.MajorType.Builder builder =
          TypeProtos.MajorType.newBuilder()
              .setMode(dataMode)
              .setMinorType(minorType);

      if (Types.isDecimalType(minorType)) {
        builder
            .setScale(type.getScale())
            .setPrecision(type.getPrecision());
      }

      args.add(new MajorTypeInLogicalExpression(builder.build()));
    }

    final String drillFuncName = FunctionCallFactory.convertToDrillFunctionName(opBinding.getOperator().getName());
    return new FunctionCall(
        drillFuncName,
        args,
        ExpressionPosition.UNKNOWN);
  }

  /**
   * Checks if at least one of the operand types is nullable.
   *
   * @param operandTypes operand types
   * @return true if one of the operands is nullable, false otherwise
   */
  private static boolean isNullable(List<RelDataType> operandTypes) {
    for (RelDataType relDataType : operandTypes) {
      if (relDataType.isNullable()) {
        return true;
      }
    }
    return false;
  }

  /**
   * This class is not intended to be instantiated
   */
  private TypeInferenceUtils() {

  }
}
