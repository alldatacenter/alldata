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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.BasicSqlType;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionCallFactory;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.IfExpression.IfCondition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.NullExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.StarColumnHelper;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexLocalRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexOver;
import org.apache.calcite.rex.RexRangeRef;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.util.NlsString;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.work.ExecErrorConstants;

import static org.apache.drill.exec.planner.physical.PlannerSettings.ENABLE_DECIMAL_DATA_TYPE;

/**
 * Utilities for Drill's planner.
 */
public class DrillOptiq {
  public static final String UNSUPPORTED_REX_NODE_ERROR = "Cannot convert RexNode to equivalent Drill expression. ";
  private static final Logger logger = LoggerFactory.getLogger(DrillOptiq.class);

  /**
   * Converts a tree of {@link RexNode} operators into a scalar expression in Drill syntax using one input.
   *
   * @param context parse context which contains planner settings
   * @param input data input
   * @param expr expression to be converted
   * @return converted expression
   */
  public static LogicalExpression toDrill(DrillParseContext context, RelNode input, RexNode expr) {
    return toDrill(context, Lists.newArrayList(input), expr);
  }

  /**
   * Converts a tree of {@link RexNode} operators into a scalar expression in Drill syntax using multiple inputs.
   *
   * @param context parse context which contains planner settings
   * @param inputs multiple data inputs
   * @param expr expression to be converted
   * @return converted expression
   */
  public static LogicalExpression toDrill(DrillParseContext context, List<RelNode> inputs, RexNode expr) {
    final RexToDrill visitor = new RexToDrill(context, inputs);
    return expr.accept(visitor);
  }
  public static LogicalExpression toDrill(DrillParseContext context, RelDataType type,
                                          RexBuilder builder, RexNode expr) {
    final RexToDrill visitor = new RexToDrill(context, type, builder);
    return expr.accept(visitor);
  }

  public static class RexToDrill extends RexVisitorImpl<LogicalExpression> {
    private final DrillParseContext context;
    private final List<RelDataTypeField> fieldList;
    private final RelDataType rowType;
    private final RexBuilder builder;

    RexToDrill(DrillParseContext context, List<RelNode> inputs) {
      super(true);
      this.context = context;
      this.fieldList = new ArrayList<>();
      if (inputs.size() > 0 && inputs.get(0)!=null) {
        this.rowType = inputs.get(0).getRowType();
        this.builder = inputs.get(0).getCluster().getRexBuilder();
      }
      else {
        this.rowType = null;
        this.builder = null;
      }
      /*
         Fields are enumerated by their presence order in input. Details {@link org.apache.calcite.rex.RexInputRef}.
         Thus we can merge field list from several inputs by adding them into the list in order of appearance.
         Each field index in the list will match field index in the RexInputRef instance which will allow us
         to retrieve field from filed list by index in {@link #visitInputRef(RexInputRef)} method. Example:

         Query: select t1.c1, t2.c1. t2.c2 from t1 inner join t2 on t1.c1 between t2.c1 and t2.c2

         Input 1: $0
         Input 2: $1, $2

         Result: $0, $1, $2
       */
      for (RelNode input : inputs) {
        if (input != null) {
          fieldList.addAll(input.getRowType().getFieldList());
        }
      }
    }
    public RexToDrill(DrillParseContext context, RelNode input) {
      this(context, Lists.newArrayList(input));
    }

    public RexToDrill(DrillParseContext context, RelDataType rowType, RexBuilder builder) {
      super(true);
      this.context = context;
      this.rowType = rowType;
      this.builder = builder;
      this.fieldList = rowType.getFieldList();
    }

    protected RelDataType getRowType() {
      return rowType;
    }

    protected RexBuilder getRexBuilder() {
      return builder;
    }

    @Override
    public LogicalExpression visitInputRef(RexInputRef inputRef) {
      final int index = inputRef.getIndex();
      final RelDataTypeField field = fieldList.get(index);
      return FieldReference.getWithQuotedRef(field.getName());
    }

    @Override
    public LogicalExpression visitCall(RexCall call) {
//      logger.debug("RexCall {}, {}", call);
      final SqlSyntax syntax = call.getOperator().getSyntax();
      switch (syntax) {
      case BINARY:
        logger.debug("Binary");
        final String funcName = call.getOperator().getName().toLowerCase();
        return doFunction(call, funcName);
      case FUNCTION:
      case FUNCTION_ID:
        logger.debug("Function");
        return getDrillFunctionFromOptiqCall(call);
      case POSTFIX:
        logger.debug("Postfix");
        switch(call.getKind()){
          case IS_NOT_NULL:
          case IS_NOT_TRUE:
          case IS_NOT_FALSE:
          case IS_NULL:
          case IS_TRUE:
          case IS_FALSE:
          case OTHER:
            return FunctionCallFactory.createExpression(call.getOperator().getName().toLowerCase(),
                ExpressionPosition.UNKNOWN, call.getOperands().get(0).accept(this));
          default:
            throw notImplementedException(syntax, call);
        }
      case PREFIX:
        LogicalExpression arg = call.getOperands().get(0).accept(this);
        switch(call.getKind()){
          case NOT:
            return FunctionCallFactory.createExpression(call.getOperator().getName().toLowerCase(),
                ExpressionPosition.UNKNOWN, arg);
          case MINUS_PREFIX:
            List<LogicalExpression> operands = new ArrayList<>();
            operands.add(call.getOperands().get(0).accept(this));
            return FunctionCallFactory.createExpression("u-", operands);
          default:
            throw notImplementedException(syntax, call);
        }
      case SPECIAL:
        switch(call.getKind()){
        case CAST:
          return getDrillCastFunctionFromOptiq(call);
        case ROW:
          List<RelDataTypeField> fieldList = call.getType().getFieldList();
          List<RexNode> oldOperands = call.getOperands();
          List<LogicalExpression> newOperands = new ArrayList<>();
          for (int i = 0; i < oldOperands.size(); i++) {
            RexLiteral nameOperand = getRexBuilder().makeLiteral(fieldList.get(i).getName());
            RexNode valueOperand = call.operands.get(i);
            newOperands.add(nameOperand.accept(this));
            newOperands.add(valueOperand.accept(this));
          }
          return FunctionCallFactory.createExpression(call.op.getName().toLowerCase(), newOperands);
        case LIKE:
        case SIMILAR:
          return getDrillFunctionFromOptiqCall(call);
        case CASE:
          List<LogicalExpression> caseArgs = new ArrayList<>();
          for(RexNode r : call.getOperands()){
            caseArgs.add(r.accept(this));
          }

          caseArgs = Lists.reverse(caseArgs);
          // number of arguements are always going to be odd, because
          // Optiq adds "null" for the missing else expression at the end
          assert caseArgs.size()%2 == 1;
          LogicalExpression elseExpression = caseArgs.get(0);
          for (int i=1; i<caseArgs.size(); i=i+2) {
            elseExpression = IfExpression.newBuilder()
              .setElse(elseExpression)
              .setIfCondition(new IfCondition(caseArgs.get(i + 1), caseArgs.get(i))).build();
          }
          return elseExpression;

        default:
        }

        if (call.getOperator() == SqlStdOperatorTable.ITEM) {
          return handleItemOperator(call, syntax);
        }

        if (call.getOperator() == SqlStdOperatorTable.DATETIME_PLUS) {
          return doFunction(call, "+");
        }

        if (call.getOperator() == SqlStdOperatorTable.MINUS_DATE) {
          return doFunction(call, "-");
        }

        // fall through
      default:
        throw notImplementedException(syntax, call);
      }
    }

    private SchemaPath handleItemOperator(RexCall call, SqlSyntax syntax) {
      SchemaPath left = (SchemaPath) call.getOperands().get(0).accept(this);

      RelDataType dataType = call.getOperands().get(0).getType();
      boolean isMap = dataType.getSqlTypeName() == SqlTypeName.MAP;

      // Convert expr of item[*, 'abc'] into column expression 'abc'
      String rootSegName = left.getRootSegment().getPath();
      if (StarColumnHelper.isStarColumn(rootSegName)) {
        rootSegName = rootSegName.substring(0, rootSegName.indexOf(SchemaPath.DYNAMIC_STAR));
        final RexLiteral literal = (RexLiteral) call.getOperands().get(1);
        return SchemaPath.getSimplePath(rootSegName + literal.getValue2().toString());
      }

      final RexLiteral literal;
      RexNode operand = call.getOperands().get(1);
      if (operand instanceof RexLiteral) {
        literal = (RexLiteral) operand;
      } else if (isMap && operand.getKind() == SqlKind.CAST) {
        SqlTypeName castType = operand.getType().getSqlTypeName();
        SqlTypeName keyType = dataType.getKeyType().getSqlTypeName();
        Preconditions.checkArgument(castType == keyType,
            String.format("Wrong type CAST: expected '%s' but found '%s'", keyType.getName(), castType.getName()));
        literal = (RexLiteral) ((RexCall) operand).operands.get(0);
      } else {
        throw notImplementedException(syntax, call);
      }

      switch (literal.getTypeName()) {
        case DECIMAL:
        case INTEGER:
          if (isMap) {
            return handleMapNumericKey(literal, operand, dataType, left);
          }
          return left.getChild(((BigDecimal) literal.getValue()).intValue());
        case CHAR:
          if (isMap) {
            return handleMapCharKey(literal, operand, dataType, left);
          }
          return left.getChild(literal.getValue2().toString());
        case BOOLEAN:
          if (isMap) {
            BasicSqlType sqlType = (BasicSqlType) operand.getType();
            TypeProtos.DataMode mode = sqlType.isNullable() ? TypeProtos.DataMode.OPTIONAL : TypeProtos.DataMode.REQUIRED;
            return left.getChild(literal.getValue().toString(), literal.getValue(), Types.withMode(MinorType.BIT, mode));
          }
          // fall through
        default:
          throw notImplementedException(syntax, call);
      }
    }

    private DrillRuntimeException notImplementedException(SqlSyntax syntax, RexCall call) {
      String message = String.format("Syntax '%s(%s)' is not implemented.", syntax.toString(), call.toString());
      throw new DrillRuntimeException(message);
    }

    private SchemaPath handleMapNumericKey(RexLiteral literal, RexNode operand, RelDataType mapType, SchemaPath parentPath) {
      BigDecimal literalValue = (BigDecimal) literal.getValue();
      RelDataType sqlType = operand.getType();
      Object originalValue;

      TypeProtos.DataMode mode = sqlType.isNullable() ? TypeProtos.DataMode.OPTIONAL : TypeProtos.DataMode.REQUIRED;
      boolean arraySegment = false;
      MajorType type;
      switch (mapType.getKeyType().getSqlTypeName()) {
        case DOUBLE:
          type = Types.withMode(MinorType.FLOAT8, mode);
          originalValue = literalValue.doubleValue();
          break;
        case FLOAT:
          type = Types.withMode(MinorType.FLOAT4, mode);
          originalValue = literalValue.floatValue();
          break;
        case DECIMAL:
          type = Types.withPrecisionAndScale(MinorType.VARDECIMAL, mode, literalValue.precision(), literalValue.scale());
          originalValue = literalValue;
          break;
        case BIGINT:
          type = Types.withMode(MinorType.BIGINT, mode);
          originalValue = literalValue.longValue();
          break;
        case INTEGER:
          type = Types.withMode(MinorType.INT, mode);
          originalValue = literalValue.intValue();
          arraySegment = true;
          break;
        case SMALLINT:
          type = Types.withMode(MinorType.SMALLINT, mode);
          originalValue = literalValue.shortValue();
          arraySegment = true;
          break;
        case TINYINT:
          type = Types.withMode(MinorType.TINYINT, mode);
          originalValue = literalValue.byteValue();
          arraySegment = true;
          break;
        default:
          throw new AssertionError("Shouldn't reach there. Type: " + mapType.getKeyType().getSqlTypeName());
      }

      if (arraySegment) {
        return parentPath.getChild((int) originalValue, originalValue, type);
      } else {
        return parentPath.getChild(originalValue.toString(), originalValue, type);
      }
    }

    private SchemaPath handleMapCharKey(RexLiteral literal, RexNode operand, RelDataType mapType, SchemaPath parentPath) {
      TypeProtos.DataMode mode = operand.getType().isNullable()
          ? TypeProtos.DataMode.OPTIONAL : TypeProtos.DataMode.REQUIRED;
      TypeProtos.MajorType type;
      switch (mapType.getKeyType().getSqlTypeName()) {
        case TIMESTAMP:
          type = Types.withMode(MinorType.TIMESTAMP, mode);
          break;
        case DATE:
          type = Types.withMode(MinorType.DATE, mode);
          break;
        case TIME:
          type = Types.withMode(MinorType.TIME, mode);
          break;
        case INTERVAL_DAY:
          type = Types.withMode(MinorType.INTERVALDAY, mode);
          break;
        case INTERVAL_YEAR:
          type = Types.withMode(MinorType.INTERVALYEAR, mode);
          break;
        case INTERVAL_MONTH:
          type = Types.withMode(MinorType.INTERVAL, mode);
          break;
        default:
          type = Types.withMode(MinorType.VARCHAR, mode);
          break;
      }
      return parentPath.getChild(literal.getValue2().toString(), literal.getValue2(), type);
    }

    private LogicalExpression doFunction(RexCall call, String funcName) {
      List<LogicalExpression> args = new ArrayList<>();
      for(RexNode r : call.getOperands()){
        args.add(r.accept(this));
      }

      if (FunctionCallFactory.isBooleanOperator(funcName)) {
        LogicalExpression func = FunctionCallFactory.createBooleanOperator(funcName, args);
        return func;
      } else {
        args = Lists.reverse(args);
        LogicalExpression lastArg = args.get(0);
        for(int i = 1; i < args.size(); i++){
          lastArg = FunctionCallFactory.createExpression(funcName, Lists.newArrayList(args.get(i), lastArg));
        }

        return lastArg;
      }

    }
    private LogicalExpression doUnknown(RexNode o){
      // raise an error
      throw UserException.planError().message(UNSUPPORTED_REX_NODE_ERROR +
              "RexNode Class: %s, RexNode Digest: %s", o.getClass().getName(), o.toString()).build(logger);
    }
    @Override
    public LogicalExpression visitLocalRef(RexLocalRef localRef) {
      return doUnknown(localRef);
    }

    @Override
    public LogicalExpression visitOver(RexOver over) {
      return doUnknown(over);
    }

    @Override
    public LogicalExpression visitCorrelVariable(RexCorrelVariable correlVariable) {
      return doUnknown(correlVariable);
    }

    @Override
    public LogicalExpression visitDynamicParam(RexDynamicParam dynamicParam) {
      return doUnknown(dynamicParam);
    }

    @Override
    public LogicalExpression visitRangeRef(RexRangeRef rangeRef) {
      return doUnknown(rangeRef);
    }

    @Override
    public LogicalExpression visitFieldAccess(RexFieldAccess fieldAccess) {
      SchemaPath logicalRef = (SchemaPath) fieldAccess.getReferenceExpr().accept(this);
      return logicalRef.getChild(fieldAccess.getField().getName());
    }

    private LogicalExpression getDrillCastFunctionFromOptiq(RexCall call){
      LogicalExpression arg = call.getOperands().get(0).accept(this);
      MajorType castType;

      switch (call.getType().getSqlTypeName()) {
        case VARCHAR:
        case CHAR:
          castType = Types.required(MinorType.VARCHAR).toBuilder().setPrecision(call.getType().getPrecision()).build();
          break;
        case INTEGER:
          castType = Types.required(MinorType.INT);
          break;
        case FLOAT:
          castType = Types.required(MinorType.FLOAT4);
          break;
        case DOUBLE:
          castType = Types.required(MinorType.FLOAT8);
          break;
        case DECIMAL:
          if (!context.getPlannerSettings().getOptions().getOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE)) {
            throw UserException.unsupportedError()
                .message(ExecErrorConstants.DECIMAL_DISABLE_ERR_MSG)
                .build(logger);
          }

          int precision = call.getType().getPrecision();
          int scale = call.getType().getScale();

          castType = TypeProtos.MajorType.newBuilder()
                .setMinorType(MinorType.VARDECIMAL)
                .setPrecision(precision)
                .setScale(scale)
                .build();
          break;
        case INTERVAL_YEAR:
        case INTERVAL_YEAR_MONTH:
        case INTERVAL_MONTH:
          castType = Types.required(MinorType.INTERVALYEAR);
          break;
        case INTERVAL_DAY:
        case INTERVAL_DAY_HOUR:
        case INTERVAL_DAY_MINUTE:
        case INTERVAL_DAY_SECOND:
        case INTERVAL_HOUR:
        case INTERVAL_HOUR_MINUTE:
        case INTERVAL_HOUR_SECOND:
        case INTERVAL_MINUTE:
        case INTERVAL_MINUTE_SECOND:
        case INTERVAL_SECOND:
          castType = Types.required(MinorType.INTERVALDAY);
          break;
        case BOOLEAN:
          castType = Types.required(MinorType.BIT);
          break;
        case BINARY:
          castType = Types.required(MinorType.VARBINARY);
          break;
        case ANY:
          return arg; // Type will be same as argument.
        default:
          castType = Types.required(MinorType.valueOf(call.getType().getSqlTypeName().getName()));
      }
      return FunctionCallFactory.createCast(castType, ExpressionPosition.UNKNOWN, arg);
    }

    private LogicalExpression getDrillFunctionFromOptiqCall(RexCall call) {
      List<LogicalExpression> args = new ArrayList<>();

      for(RexNode n : call.getOperands()){
        args.add(n.accept(this));
      }

      int argsSize = args.size();
      String functionName = call.getOperator().getName().toLowerCase();

      // TODO: once we have more function rewrites and a patter emerges from different rewrites, factor this out in a better fashion
      /* Rewrite extract functions in the following manner
       * extract(year, date '2008-2-23') ---> extractYear(date '2008-2-23')
       */
      switch (functionName) {
        case "extract": {

          // Assert that the first argument to extract is a QuotedString
          assert args.get(0) instanceof ValueExpressions.QuotedString;

          // Get the unit of time to be extracted
          String timeUnitStr = ((ValueExpressions.QuotedString) args.get(0)).value;

          TimeUnit timeUnit = TimeUnit.valueOf(timeUnitStr);

          switch (timeUnit) {
            case YEAR:
            case MONTH:
            case WEEK:
            case DAY:
            case HOUR:
            case MINUTE:
            case SECOND:
              String functionPostfix = StringUtils.capitalize(timeUnitStr.toLowerCase());
              functionName += functionPostfix;
              return FunctionCallFactory.createExpression(functionName, args.subList(1, 2));
            default:
              throw new UnsupportedOperationException("extract function supports the following time units: YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND");
          }
        }
        case "timestampdiff": {

          // Assert that the first argument to extract is a QuotedString
          Preconditions.checkArgument(args.get(0) instanceof ValueExpressions.QuotedString,
            "The first argument of TIMESTAMPDIFF function should be QuotedString");

          String timeUnitStr = ((ValueExpressions.QuotedString) args.get(0)).value;

          TimeUnit timeUnit = TimeUnit.valueOf(timeUnitStr);

          switch (timeUnit) {
            case YEAR:
            case MONTH:
            case DAY:
            case HOUR:
            case MINUTE:
            case SECOND:
            case QUARTER:
            case WEEK:
            case MICROSECOND:
            case NANOSECOND:
              String functionPostfix = StringUtils.capitalize(timeUnitStr.toLowerCase());
              functionName += functionPostfix;
              return FunctionCallFactory.createExpression(functionName, args.subList(1, 3));
            default:
              throw new UnsupportedOperationException("TIMESTAMPDIFF function supports the following time units: " +
                  "YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, QUARTER, WEEK, MICROSECOND, NANOSECOND");
          }
        }
        case "trim": {
          String trimFunc;
          List<LogicalExpression> trimArgs = new ArrayList<>();

          assert args.get(0) instanceof ValueExpressions.QuotedString;
          switch (((ValueExpressions.QuotedString) args.get(0)).value.toUpperCase()) {
            case "LEADING":
              trimFunc = "ltrim";
              break;
            case "TRAILING":
              trimFunc = "rtrim";
              break;
            case "BOTH":
              trimFunc = "btrim";
              break;
            default:
              throw new UnsupportedOperationException("Invalid argument for TRIM function. " +
                  "Expected one of the following: LEADING, TRAILING, BOTH");
          }

          trimArgs.add(args.get(2));
          trimArgs.add(args.get(1));

          return FunctionCallFactory.createExpression(trimFunc, trimArgs);
        }
        case "date_part": {
          // Rewrite DATE_PART functions as extract functions
          // assert that the function has exactly two arguments
          assert argsSize == 2;

          /* Based on the first input to the date_part function we rewrite the function as the
           * appropriate extract function. For example
           * date_part('year', date '2008-2-23') ------> extractYear(date '2008-2-23')
           */
          assert args.get(0) instanceof QuotedString;

          QuotedString extractString = (QuotedString) args.get(0);
          String functionPostfix = StringUtils.capitalize(extractString.value.toLowerCase());
          return FunctionCallFactory.createExpression("extract" + functionPostfix, args.subList(1, 2));
        }
        case "concat": {

          if (argsSize == 1) {
            /*
             * We treat concat with one argument as a special case. Since we don't have a function
             * implementation of concat that accepts one argument. We simply add another dummy argument
             * (empty string literal) to the list of arguments.
             */
            List<LogicalExpression> concatArgs = new LinkedList<>(args);
            concatArgs.add(QuotedString.EMPTY_STRING);

            return FunctionCallFactory.createExpression(functionName, concatArgs);

          } else if (argsSize > 2) {
            List<LogicalExpression> concatArgs = new ArrayList<>();

            /* stack concat functions on top of each other if we have more than two arguments
             * Eg: concat(col1, col2, col3) => concat(concat(col1, col2), col3)
             */
            concatArgs.add(args.get(0));
            concatArgs.add(args.get(1));

            LogicalExpression first = FunctionCallFactory.createExpression(functionName, concatArgs);

            for (int i = 2; i < argsSize; i++) {
              concatArgs = new ArrayList<>();
              concatArgs.add(first);
              concatArgs.add(args.get(i));
              first = FunctionCallFactory.createExpression(functionName, concatArgs);
            }

            return first;
          }
          break;
        }
        case "length": {
          if (argsSize == 2) {

            // Second argument should always be a literal specifying the encoding format
            assert args.get(1) instanceof ValueExpressions.QuotedString;

            String encodingType = ((ValueExpressions.QuotedString) args.get(1)).value;
            functionName += StringUtils.capitalize(encodingType.toLowerCase());

            return FunctionCallFactory.createExpression(functionName, args.subList(0, 1));
          }
          break;
        }
        case "convert_from":
        case "convert_to": {
          if (args.get(1) instanceof QuotedString) {
            return FunctionCallFactory.createConvert(functionName, ((QuotedString) args.get(1)).value, args.get(0), ExpressionPosition.UNKNOWN);
          }
          break;
        }
        case "date_trunc": {
          return handleDateTruncFunction(args);
        }
      }

      return FunctionCallFactory.createExpression(functionName, args);
    }

    private LogicalExpression handleDateTruncFunction(final List<LogicalExpression> args) {
      // Assert that the first argument to extract is a QuotedString
      assert args.get(0) instanceof ValueExpressions.QuotedString;

      // Get the unit of time to be extracted
      String timeUnitStr = ((ValueExpressions.QuotedString) args.get(0)).value.toUpperCase();

      TimeUnit timeUnit = TimeUnit.valueOf(timeUnitStr);

      switch (timeUnit) {
        case YEAR:
        case MONTH:
        case DAY:
        case HOUR:
        case MINUTE:
        case SECOND:
        case WEEK:
        case QUARTER:
        case DECADE:
        case CENTURY:
        case MILLENNIUM:
          final String functionPostfix = StringUtils.capitalize(timeUnitStr.toLowerCase());
          return FunctionCallFactory.createExpression("date_trunc_" + functionPostfix, args.subList(1, 2));
        default:
          throw new UnsupportedOperationException("date_trunc function supports the following time units: " +
              "YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, WEEK, QUARTER, DECADE, CENTURY, MILLENNIUM");
      }
    }

    @Override
    public LogicalExpression visitLiteral(RexLiteral literal) {
      switch(literal.getType().getSqlTypeName()){
      case BIGINT:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.BIGINT);
        }
        long l = (((BigDecimal) literal.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP)).longValue();
        return ValueExpressions.getBigInt(l);
      case BOOLEAN:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.BIT);
        }
        return ValueExpressions.getBit(((Boolean) literal.getValue()));
      case CHAR:
        if (isLiteralNull(literal)) {
          return createStringNullExpr(literal.getType().getPrecision());
        }
        return ValueExpressions.getChar(((NlsString)literal.getValue()).getValue(), literal.getType().getPrecision());
      case DOUBLE:
        if (isLiteralNull(literal)){
          return createNullExpr(MinorType.FLOAT8);
        }
        double d = ((BigDecimal) literal.getValue()).doubleValue();
        return ValueExpressions.getFloat8(d);
      case FLOAT:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.FLOAT4);
        }
        float f = ((BigDecimal) literal.getValue()).floatValue();
        return ValueExpressions.getFloat4(f);
      case INTEGER:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.INT);
        }
        int a = (((BigDecimal) literal.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP)).intValue();
        return ValueExpressions.getInt(a);

      case DECIMAL:
        if (context.getPlannerSettings().getOptions()
            .getBoolean(ENABLE_DECIMAL_DATA_TYPE.getOptionName())) {
          if (isLiteralNull(literal)) {
            return new TypedNullConstant(
                Types.withPrecisionAndScale(
                    MinorType.VARDECIMAL,
                    TypeProtos.DataMode.OPTIONAL,
                    literal.getType().getPrecision(),
                    literal.getType().getScale()
                ));
          }
          return ValueExpressions.getVarDecimal((BigDecimal) literal.getValue(),
              literal.getType().getPrecision(),
              literal.getType().getScale());
        }
        double dbl = ((BigDecimal) literal.getValue()).doubleValue();
        logger.warn("Converting exact decimal into approximate decimal.\n" +
            "Please enable decimal data types using `planner.enable_decimal_data_type`.");
        return ValueExpressions.getFloat8(dbl);
      case VARCHAR:
        if (isLiteralNull(literal)) {
          return createStringNullExpr(literal.getType().getPrecision());
        }
        return ValueExpressions.getChar(((NlsString)literal.getValue()).getValue(), literal.getType().getPrecision());
      case SYMBOL:
        if (isLiteralNull(literal)) {
          return createStringNullExpr(literal.getType().getPrecision());
        }
        return ValueExpressions.getChar(literal.getValue().toString(), literal.getType().getPrecision());
      case DATE:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.DATE);
        }
        return (ValueExpressions.getDate((GregorianCalendar)literal.getValue()));
      case TIME:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.TIME);
        }
        return (ValueExpressions.getTime((GregorianCalendar)literal.getValue()));
      case TIMESTAMP:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.TIMESTAMP);
        }
        return (ValueExpressions.getTimeStamp((GregorianCalendar) literal.getValue()));
      case INTERVAL_YEAR_MONTH:
      case INTERVAL_YEAR:
      case INTERVAL_MONTH:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.INTERVALYEAR);
        }
        return (ValueExpressions.getIntervalYear(((BigDecimal) (literal.getValue())).intValue()));
      case INTERVAL_DAY:
      case INTERVAL_DAY_HOUR:
      case INTERVAL_DAY_MINUTE:
      case INTERVAL_DAY_SECOND:
      case INTERVAL_HOUR:
      case INTERVAL_HOUR_MINUTE:
      case INTERVAL_HOUR_SECOND:
      case INTERVAL_MINUTE:
      case INTERVAL_MINUTE_SECOND:
      case INTERVAL_SECOND:
        if (isLiteralNull(literal)) {
          return createNullExpr(MinorType.INTERVALDAY);
        }
        return (ValueExpressions.getIntervalDay(((BigDecimal) (literal.getValue())).longValue()));
      case NULL:
        return NullExpression.INSTANCE;
      case ANY:
        if (isLiteralNull(literal)) {
          return NullExpression.INSTANCE;
        }
      default:
        throw new UnsupportedOperationException(String.format("Unable to convert the value of %s and type %s to a Drill constant expression.", literal, literal.getType().getSqlTypeName()));
      }
    }

    /**
     * Create nullable major type using given minor type
     * and wraps it in typed null constant.
     *
     * @param type minor type
     * @return typed null constant instance
     */
    private TypedNullConstant createNullExpr(MinorType type) {
      return new TypedNullConstant(Types.optional(type));
    }

    /**
     * Create nullable varchar major type with given precision
     * and wraps it in typed null constant.
     *
     * @param precision precision value
     * @return typed null constant instance
     */
    private TypedNullConstant createStringNullExpr(int precision) {
      return new TypedNullConstant(Types.withPrecision(MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL, precision));
    }
  }

  public static boolean isLiteralNull(RexLiteral literal) {
    return literal.getTypeName().getName().equals("NULL");
  }
}
