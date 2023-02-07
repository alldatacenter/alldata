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
package org.apache.drill.exec.expr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.ConvertExpression;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.IfExpression.IfCondition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.NullExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedFieldExpr;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.expression.ValueExpressions.BooleanExpression;
import org.apache.drill.common.expression.ValueExpressions.DateExpression;
import org.apache.drill.common.expression.ValueExpressions.Decimal18Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal28Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal38Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal9Expression;
import org.apache.drill.common.expression.ValueExpressions.DoubleExpression;
import org.apache.drill.common.expression.ValueExpressions.FloatExpression;
import org.apache.drill.common.expression.ValueExpressions.IntExpression;
import org.apache.drill.common.expression.ValueExpressions.IntervalDayExpression;
import org.apache.drill.common.expression.ValueExpressions.IntervalYearExpression;
import org.apache.drill.common.expression.ValueExpressions.LongExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.ValueExpressions.TimeExpression;
import org.apache.drill.common.expression.ValueExpressions.TimeStampExpression;
import org.apache.drill.common.expression.ValueExpressions.VarDecimalExpression;
import org.apache.drill.common.expression.fn.FunctionReplacementUtils;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.common.expression.visitors.ConditionalExprOptimizer;
import org.apache.drill.common.expression.visitors.ExpressionValidator;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.fn.AbstractFuncHolder;
import org.apache.drill.exec.expr.fn.DrillComplexWriterFuncHolder;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.exec.expr.fn.ExceptionFunction;
import org.apache.drill.exec.expr.fn.FunctionLookupContext;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.resolver.FunctionResolver;
import org.apache.drill.exec.resolver.FunctionResolverFactory;
import org.apache.drill.exec.resolver.TypeCastRules;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.metastore.util.SchemaPathUtils;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionTreeMaterializer {
  static final Logger logger = LoggerFactory.getLogger(ExpressionTreeMaterializer.class);

  private ExpressionTreeMaterializer() { }

  public static LogicalExpression materialize(LogicalExpression expr,
      VectorAccessible batch, ErrorCollector errorCollector,
      FunctionLookupContext functionLookupContext) {
    return ExpressionTreeMaterializer.materialize(expr, batch, errorCollector, functionLookupContext, false, false);
  }

  public static LogicalExpression materializeAndCheckErrors(LogicalExpression expr,
      VectorAccessible batch, FunctionLookupContext functionLookupContext) {
    ErrorCollector collector = new ErrorCollectorImpl();
    LogicalExpression e = ExpressionTreeMaterializer.materialize(expr, batch, collector, functionLookupContext, false, false);
    collector.reportErrors(logger);
    return e;
  }

  public static LogicalExpression materialize(LogicalExpression expr,
      VectorAccessible batch, ErrorCollector errorCollector, FunctionLookupContext functionLookupContext,
      boolean allowComplexWriterExpr) {
    return materialize(expr, batch, errorCollector, functionLookupContext, allowComplexWriterExpr, false);
  }

  public static LogicalExpression materializeFilterExpr(LogicalExpression expr,
      TupleMetadata fieldTypes, ErrorCollector errorCollector, FunctionLookupContext functionLookupContext) {
    final FilterMaterializeVisitor filterMaterializeVisitor = new FilterMaterializeVisitor(fieldTypes, errorCollector);
    return expr.accept(filterMaterializeVisitor, functionLookupContext);
  }

  /**
   * Materializes logical expression taking into account passed parameters.
   * Is used to materialize logical expression that contains reference to one batch.
   *
   * @param expr logical expression to be materialized
   * @param batch batch instance
   * @param errorCollector error collector
   * @param functionLookupContext context to find drill function holder
   * @param allowComplexWriterExpr true if complex expressions are allowed
   * @param unionTypeEnabled true if union type is enabled
   * @return materialized logical expression
   */
  public static LogicalExpression materialize(LogicalExpression expr,
                                              VectorAccessible batch,
                                              ErrorCollector errorCollector,
                                              FunctionLookupContext functionLookupContext,
                                              boolean allowComplexWriterExpr,
                                              boolean unionTypeEnabled) {
    Map<VectorAccessible, BatchReference> batches = Maps.newHashMap();
    batches.put(batch, null);
    return materialize(expr, batches, errorCollector, functionLookupContext,
        allowComplexWriterExpr, unionTypeEnabled);
  }

  /**
   * Materializes logical expression taking into account passed parameters. Is
   * used to materialize logical expression that can contain several batches
   * with or without custom batch reference.
   *
   * @param expr
   *          logical expression to be materialized
   * @param batches
   *          one or more batch instances used in expression
   * @param errorCollector
   *          error collector
   * @param functionLookupContext
   *          context to find drill function holder
   * @param allowComplexWriterExpr
   *          true if complex expressions are allowed
   * @param unionTypeEnabled
   *          true if union type is enabled
   * @return materialized logical expression
   */
  public static LogicalExpression materialize(LogicalExpression expr,
                                              Map<VectorAccessible, BatchReference> batches,
                                              ErrorCollector errorCollector,
                                              FunctionLookupContext functionLookupContext,
                                              boolean allowComplexWriterExpr,
                                              boolean unionTypeEnabled) {

    LogicalExpression out = expr.accept(
        new MaterializeVisitor(batches, errorCollector, allowComplexWriterExpr, unionTypeEnabled),
        functionLookupContext);

    if (!errorCollector.hasErrors()) {
      out = out.accept(ConditionalExprOptimizer.INSTANCE, null);
    }

    if (out instanceof NullExpression) {
      return new TypedNullConstant(Types.optional(MinorType.INT));
    } else {
      return out;
    }
  }

  public static LogicalExpression convertToNullableType(LogicalExpression fromExpr,
      MinorType toType, FunctionLookupContext functionLookupContext, ErrorCollector errorCollector) {
    String funcName = "convertToNullable" + toType.toString();
    List<LogicalExpression> args = new ArrayList<>();
    args.add(fromExpr);
    FunctionCall funcCall = new FunctionCall(funcName, args, ExpressionPosition.UNKNOWN);
    FunctionResolver resolver = FunctionResolverFactory.getResolver(funcCall);

    DrillFuncHolder matchedConvertToNullableFuncHolder = functionLookupContext.findDrillFunction(resolver, funcCall);
    if (matchedConvertToNullableFuncHolder == null) {
      logFunctionResolutionError(errorCollector, funcCall);
      return NullExpression.INSTANCE;
    }

    return matchedConvertToNullableFuncHolder.getExpr(funcName, args, ExpressionPosition.UNKNOWN);
  }

  public static LogicalExpression addCastExpression(LogicalExpression fromExpr,
      MajorType toType, FunctionLookupContext functionLookupContext, ErrorCollector errorCollector) {
    return addCastExpression(fromExpr, toType, functionLookupContext, errorCollector, true);
  }

  public static LogicalExpression addCastExpression(LogicalExpression fromExpr, MajorType toType,
      FunctionLookupContext functionLookupContext, ErrorCollector errorCollector, boolean exactResolver) {
    String castFuncName = FunctionReplacementUtils.getCastFunc(toType.getMinorType());
    List<LogicalExpression> castArgs = new ArrayList<>();
    castArgs.add(fromExpr);  //input_expr

    if (fromExpr.getMajorType().getMinorType() == MinorType.UNION && toType.getMinorType() == MinorType.UNION) {
      return fromExpr;
    }

    if (Types.isDecimalType(toType)) {
      // Add the scale and precision to the arguments of the implicit cast
      castArgs.add(new ValueExpressions.IntExpression(toType.getPrecision(), null));
      castArgs.add(new ValueExpressions.IntExpression(toType.getScale(), null));
    } else if (!Types.isFixedWidthType(toType) && !Types.isUnion(toType)) {

      /* We are implicitly casting to VARCHAR so we don't have a max length,
       * using an arbitrary value. We trim down the size of the stored bytes
       * to the actual size so this size doesn't really matter.
       */
      castArgs.add(new ValueExpressions.LongExpression(Types.MAX_VARCHAR_LENGTH, null));
    }

    FunctionCall castCall = new FunctionCall(castFuncName, castArgs, ExpressionPosition.UNKNOWN);
    FunctionResolver resolver;
    if (exactResolver) {
      resolver = FunctionResolverFactory.getExactResolver(castCall);
    } else {
      resolver = FunctionResolverFactory.getResolver(castCall);
    }
    DrillFuncHolder matchedCastFuncHolder = functionLookupContext.findDrillFunction(resolver, castCall);

    if (matchedCastFuncHolder == null) {
      logFunctionResolutionError(errorCollector, castCall);
      return NullExpression.INSTANCE;
    }
    return matchedCastFuncHolder.getExpr(castFuncName, castArgs, ExpressionPosition.UNKNOWN);
  }

  private static void logFunctionResolutionError(ErrorCollector errorCollector, FunctionCall call) {
    // add error to collector
    StringBuilder sb = new StringBuilder();
    sb.append("Missing function implementation: ");
    sb.append("[");
    sb.append(call.getName());
    sb.append("(");
    boolean first = true;
    for (LogicalExpression e : call.args()) {
      TypeProtos.MajorType mt = e.getMajorType();
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(mt.getMinorType().name());
      sb.append("-");
      sb.append(mt.getMode().name());
    }
    sb.append(")");
    sb.append("]");

    errorCollector.addGeneralError(call.getPosition(), sb.toString());
  }

  /**
   * Visitor that wraps schema path into value vector read expression
   * if schema path is present in one of the batches,
   * otherwise instance of null expression.
   */
  private static class MaterializeVisitor extends AbstractMaterializeVisitor {

    private final Map<VectorAccessible, BatchReference> batches;

    public MaterializeVisitor(Map<VectorAccessible, BatchReference> batches,
                              ErrorCollector errorCollector,
                              boolean allowComplexWriter,
                              boolean unionTypeEnabled) {
      super(errorCollector, allowComplexWriter, unionTypeEnabled);
      this.batches = batches;
    }

    @Override
    public LogicalExpression visitSchemaPath(final SchemaPath path, FunctionLookupContext functionLookupContext) {
      TypedFieldId tfId = null;
      BatchReference batchRef = null;
      for (Map.Entry<VectorAccessible, BatchReference> entry : batches.entrySet()) {
        tfId = entry.getKey().getValueVectorId(path);
        if (tfId != null) {
          batchRef = entry.getValue();
          break;
        }
      }

      if (tfId == null) {
        logger.warn("Unable to find value vector of path {}, returning null instance.", path);
        return NullExpression.INSTANCE;
      } else {
        return new ValueVectorReadExpression(tfId, batchRef);
      }
    }
  }

  private static class FilterMaterializeVisitor extends AbstractMaterializeVisitor {
    private final TupleMetadata types;

    public FilterMaterializeVisitor(TupleMetadata types, ErrorCollector errorCollector) {
      super(errorCollector, false, false);
      this.types = types;
    }

    @Override
    public LogicalExpression visitSchemaPath(SchemaPath path, FunctionLookupContext functionLookupContext) {
      MajorType type = null;

      ColumnMetadata columnMetadata = SchemaPathUtils.getColumnMetadata(path.getUnIndexed(), types);

      if (columnMetadata != null) {
        type = columnMetadata.majorType();
        // for the case when specified path refers to array element, makes its type optional
        if (path.isArray()) {
          type = type.toBuilder().setMode(DataMode.OPTIONAL).build();
        }
      }

      if (type != null) {
        return new TypedFieldExpr(path, type);
      } else {
        logger.warn("Unable to find value vector of path {}, returning null-int instance.", path);
        return new TypedFieldExpr(path, Types.OPTIONAL_INT);
      }
    }
  }

  private abstract static class AbstractMaterializeVisitor extends AbstractExprVisitor<LogicalExpression, FunctionLookupContext, RuntimeException> {
    private final ExpressionValidator validator = new ExpressionValidator();
    private ErrorCollector errorCollector;
    private final Deque<ErrorCollector> errorCollectors = new ArrayDeque<>();
    private final boolean allowComplexWriter;
    /**
     * If this is false, the materializer will not handle or create UnionTypes
     * Once this code is more well tested, we will probably remove this flag
     */
    private final boolean unionTypeEnabled;

    public AbstractMaterializeVisitor(ErrorCollector errorCollector, boolean allowComplexWriter, boolean unionTypeEnabled) {
      this.errorCollector = errorCollector;
      this.allowComplexWriter = allowComplexWriter;
      this.unionTypeEnabled = unionTypeEnabled;
    }

    private LogicalExpression validateNewExpr(LogicalExpression newExpr) {
      newExpr.accept(validator, errorCollector);
      return newExpr;
    }

    @Override
    public abstract LogicalExpression visitSchemaPath(SchemaPath path, FunctionLookupContext functionLookupContext);

    @Override
    public LogicalExpression visitUnknown(LogicalExpression e, FunctionLookupContext functionLookupContext) {
      return e;
    }

    @Override
    public LogicalExpression visitFunctionHolderExpression(FunctionHolderExpression holder,
        FunctionLookupContext functionLookupContext) {
      // A function holder is already materialized, no need to rematerialize.
      // generally this won't be used unless we materialize a partial tree and
      // rematerialize the whole tree.
      return holder;
    }

    @Override
    public LogicalExpression visitBooleanOperator(BooleanOperator op, FunctionLookupContext functionLookupContext) {
      List<LogicalExpression> args = new ArrayList<>();
      for (LogicalExpression expr : op.args()) {
        LogicalExpression newExpr = expr.accept(this, functionLookupContext);
        assert newExpr != null : String.format("Materialization of %s return a null expression.", expr);
        args.add(newExpr);
      }

      // Replace with a new function call, since its argument could be changed.
      return new BooleanOperator(op.getName(), args, op.getPosition());
    }

    private int computePrecision(LogicalExpression currentArg) {
      int precision = currentArg.getMajorType().getPrecision();
      return DecimalUtility.getDefaultPrecision(currentArg.getMajorType().getMinorType(), precision);
    }

    @Override
    public LogicalExpression visitFunctionCall(FunctionCall call, FunctionLookupContext functionLookupContext) {

      // Possibly convert input expressions with a rewritten expression.
      List<LogicalExpression> args = new ArrayList<>();
      for (LogicalExpression expr : call.args()) {
        LogicalExpression newExpr = expr.accept(this, functionLookupContext);
        assert newExpr != null : String.format("Materialization of %s returned a null expression.", expr);
        args.add(newExpr);
      }

      // Replace with a new function call, since its argument could be changed.
      call = new FunctionCall(call.getName(), args, call.getPosition());

      // Resolve the function
      FunctionResolver resolver = FunctionResolverFactory.getResolver(call);
      DrillFuncHolder matchedFuncHolder = functionLookupContext.findDrillFunction(resolver, call);

      if (matchedFuncHolder instanceof DrillComplexWriterFuncHolder && ! allowComplexWriter) {
        errorCollector.addGeneralError(call.getPosition(),
            "Only ProjectRecordBatch could have complex writer function. You are using complex writer function "
                + call.getName() + " in a non-project operation!");
      }

      if (matchedFuncHolder != null) {
        return bindDrillFunc(call, functionLookupContext, matchedFuncHolder);
      }

      // as no drill func is found, search for a non-Drill function.
      AbstractFuncHolder matchedNonDrillFuncHolder = functionLookupContext.findNonDrillFunction(call);
      if (matchedNonDrillFuncHolder != null) {
        return bindNonDrillFunc(call, functionLookupContext,
            matchedNonDrillFuncHolder);
      }

      if (hasUnionInput(call)) {
        return rewriteUnionFunction(call, functionLookupContext);
      }

      // No match found. Add an error (which will fail the query), but also
      // return a NULL instance so that analysis can continue.
      logFunctionResolutionError(errorCollector, call);
      return NullExpression.INSTANCE;
    }

    /**
     * Bind a call to a Drill function, casting input and output arguments
     * as needed. Casts done here are of SQL types, not internal Drill types
     * (FieldReader vs. Holder).
     *
     * @param call the logical call expression
     * @param functionLookupContext function registry
     * @param matchedFuncHolder the matched Drill function declaration
     * @return a new expression that represents the actual function call
     */
    private LogicalExpression bindDrillFunc(FunctionCall call,
        FunctionLookupContext functionLookupContext,
        DrillFuncHolder matchedFuncHolder) {

      // New arg lists, possible with implicit cast inserted.
      List<LogicalExpression> argsWithCast = new ArrayList<>();

      // Compare param type against arg type. Insert cast on top of arg, whenever necessary.
      for (int i = 0; i < call.argCount(); ++i) {
        LogicalExpression currentArg = call.arg(i);

        TypeProtos.MajorType paramType = matchedFuncHolder.getParamMajorType(i);

        // Case 1: If  1) the argument is NullExpression
        //             2) the minor type of parameter of matchedFuncHolder is not LATE
        //                (the type of null expression is still unknown)
        //             3) the parameter of matchedFuncHolder allows null input, or func's null_handling
        //                is NULL_IF_NULL (means null and non-null are exchangeable).
        //         then replace NullExpression with a TypedNullConstant
        if (currentArg.equals(NullExpression.INSTANCE) && !MinorType.LATE.equals(paramType.getMinorType()) &&
            (TypeProtos.DataMode.OPTIONAL.equals(paramType.getMode()) ||
            matchedFuncHolder.getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL)) {
          // Case 1: argument is a null expression, convert it to a typed null
          argsWithCast.add(new TypedNullConstant(paramType));
        } else if (Types.softEquals(paramType, currentArg.getMajorType(),
            matchedFuncHolder.getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL) ||
                   matchedFuncHolder.isFieldReader(i)) {
          currentArg = addNullableCast(currentArg, paramType, functionLookupContext);
          // Case 2: argument and parameter matches, or parameter is FieldReader.  Do nothing.
          argsWithCast.add(currentArg);
        } else {
          // Case 3: insert cast if param type is different from arg type.
          if (Types.isDecimalType(paramType)) {
            // We are implicitly promoting a decimal type, set the required scale and precision
            paramType = MajorType.newBuilder().setMinorType(paramType.getMinorType()).setMode(paramType.getMode()).
                setScale(currentArg.getMajorType().getScale()).setPrecision(computePrecision(currentArg)).build();
          }
          LogicalExpression castExpression = addCastExpression(currentArg, paramType, functionLookupContext, errorCollector);
          castExpression = addNullableCast(castExpression, paramType, functionLookupContext);
          argsWithCast.add(castExpression);
        }
      }

      FunctionHolderExpression funcExpr = matchedFuncHolder.getExpr(call.getName(), argsWithCast, call.getPosition());

      // Convert old-style Decimal return type to VarDecimal
      MajorType funcExprMajorType = funcExpr.getMajorType();
      if (DecimalUtility.isObsoleteDecimalType(funcExprMajorType.getMinorType())) {
        MajorType majorType =
            MajorType.newBuilder()
                .setMinorType(MinorType.VARDECIMAL)
                .setMode(funcExprMajorType.getMode())
                .setScale(funcExprMajorType.getScale())
                .setPrecision(funcExprMajorType.getPrecision())
                .build();
        return addCastExpression(funcExpr, majorType, functionLookupContext, errorCollector);
      }
      return funcExpr;
    }

    /**
     * Adds cast to {@code DataMode.OPTIONAL} data mode if specified {@code LogicalExpression argument}
     * has {@code DataMode.REQUIRED} data mode and specified {@code MajorType targetParamType}
     * has {@code DataMode.OPTIONAL} one.
     *
     * @param argument              argument expression
     * @param functionParamType     type of function argument
     * @param functionLookupContext function lookup context
     * @return expression with the added cast to {@code DataMode.OPTIONAL} data mode if required,
     * otherwise unchanged {@code LogicalExpression argument}
     */
    private LogicalExpression addNullableCast(LogicalExpression argument, MajorType functionParamType,
        FunctionLookupContext functionLookupContext) {
      if (functionParamType.getMode() == DataMode.OPTIONAL
          && argument.getMajorType().getMode() == DataMode.REQUIRED) {
        // convert argument to nullable type if function require nullable argument
        argument = convertToNullableType(argument, functionParamType.getMinorType(), functionLookupContext, errorCollector);
      }
      return argument;
    }

    private LogicalExpression bindNonDrillFunc(FunctionCall call,
        FunctionLookupContext functionLookupContext,
        AbstractFuncHolder matchedNonDrillFuncHolder) {
      // Insert implicit cast function holder expressions if required
      List<LogicalExpression> extArgsWithCast = new ArrayList<>();

      for (int i = 0; i < call.argCount(); ++i) {
        final LogicalExpression currentArg = call.arg(i);
        TypeProtos.MajorType paramType = matchedNonDrillFuncHolder.getParamMajorType(i);

        if (Types.softEquals(paramType, currentArg.getMajorType(), true)) {
          extArgsWithCast.add(currentArg);
        } else {
          // Insert cast if param type is different from arg type.
          if (Types.isDecimalType(paramType)) {
            // We are implicitly promoting a decimal type, set the required scale and precision
            paramType = MajorType.newBuilder().setMinorType(paramType.getMinorType()).setMode(paramType.getMode()).
                setScale(currentArg.getMajorType().getScale()).setPrecision(computePrecision(currentArg)).build();
          }
          extArgsWithCast.add(addCastExpression(currentArg, paramType, functionLookupContext, errorCollector));
        }
      }

      return matchedNonDrillFuncHolder.getExpr(call.getName(), extArgsWithCast, call.getPosition());
    }

    private boolean hasUnionInput(FunctionCall call) {
      for (LogicalExpression arg : call.args()) {
        if (arg.getMajorType().getMinorType() == MinorType.UNION) {
          return true;
        }
      }
      return false;
    }

    /**
     * Converts a function call with a Union type input into a case statement,
     * where each branch of the case corresponds to one of the subtypes of the
     * Union type. The function call is materialized in each of the branches,
     * with the union input cast to the specific type corresponding to the
     * branch of the case statement
     */
    private LogicalExpression rewriteUnionFunction(FunctionCall call, FunctionLookupContext functionLookupContext) {
      LogicalExpression[] args = new LogicalExpression[call.argCount()];
      call.args().toArray(args);

      for (int i = 0; i < args.length; i++) {
        LogicalExpression arg = call.arg(i);
        MajorType majorType = arg.getMajorType();

        if (majorType.getMinorType() != MinorType.UNION) {
          continue;
        }

        List<MinorType> subTypes = majorType.getSubTypeList();
        Preconditions.checkState(subTypes.size() > 0, "Union type has no subtypes");

        Queue<IfCondition> ifConditions = Lists.newLinkedList();

        for (MinorType minorType : subTypes) {
          LogicalExpression ifCondition = getIsTypeExpressionForType(minorType, arg.accept(new CloneVisitor(), null));
          args[i] = getUnionAssertFunctionForType(minorType, arg.accept(new CloneVisitor(), null));

          List<LogicalExpression> newArgs = new ArrayList<>();
          for (LogicalExpression e : args) {
            newArgs.add(e.accept(new CloneVisitor(), null));
          }

          // When expanding the expression tree to handle the different
          // subtypes, we will not throw an exception if one
          // of the branches fails to find a function match, since it is
          // possible that code path will never occur in execution.
          // So instead of failing to materialize, we generate code to throw the
          // exception during execution if that code path is hit.

          errorCollectors.push(errorCollector);
          errorCollector = new ErrorCollectorImpl();

          LogicalExpression thenExpression = new FunctionCall(call.getName(), newArgs, call.getPosition()).accept(this, functionLookupContext);

          if (errorCollector.hasErrors()) {
            thenExpression = getExceptionFunction(errorCollector.toErrorString());
          }

          errorCollector = errorCollectors.pop();

          IfExpression.IfCondition condition = new IfCondition(ifCondition, thenExpression);
          ifConditions.add(condition);
        }

        LogicalExpression ifExpression = ifConditions.poll().expression;

        while (!ifConditions.isEmpty()) {
          ifExpression = IfExpression.newBuilder().setIfCondition(ifConditions.poll()).setElse(ifExpression).build();
        }

        args[i] = ifExpression;
        return ifExpression.accept(this, functionLookupContext);
      }
      throw new UnsupportedOperationException("Did not find any Union input types");
    }

    /**
     * Returns the function call whose purpose is to throw an Exception if that code is hit during execution
     * @param message the exception message
     * @return
     */
    private LogicalExpression getExceptionFunction(String message) {
      QuotedString msg = new QuotedString(message, message.length(), ExpressionPosition.UNKNOWN);
      List<LogicalExpression> args = new ArrayList<>();
      args.add(msg);
      return new FunctionCall(ExceptionFunction.EXCEPTION_FUNCTION_NAME, args, ExpressionPosition.UNKNOWN);
    }

    /**
     * Returns the function which asserts that the current subtype of a union type is a specific type, and allows the materializer
     * to bind to that specific type when doing function resolution
     * @param type
     * @param arg
     * @return
     */
    private LogicalExpression getUnionAssertFunctionForType(MinorType type, LogicalExpression arg) {
      if (type == MinorType.UNION) {
        return arg;
      }
      if (type == MinorType.LIST || type == MinorType.MAP) {
        return getExceptionFunction("Unable to cast union to " + type);
      }
      String castFuncName = String.format("assert_%s", type.toString());
      return new FunctionCall(castFuncName, Collections.singletonList(arg), ExpressionPosition.UNKNOWN);
    }

    /**
     * Get the function that tests whether a union type is a specific type
     * @param type
     * @param arg
     * @return
     */
    private LogicalExpression getIsTypeExpressionForType(MinorType type, LogicalExpression arg) {
      String isFuncName = String.format("is_%s", type.toString());
      List<LogicalExpression> args = new ArrayList<>();
      args.add(arg);
      return new FunctionCall(isFuncName, args, ExpressionPosition.UNKNOWN);
    }

    @Override
    public LogicalExpression visitIfExpression(IfExpression ifExpr, FunctionLookupContext functionLookupContext) {
      IfExpression.IfCondition conditions = ifExpr.ifCondition;
      LogicalExpression newElseExpr = ifExpr.elseExpression.accept(this, functionLookupContext);

      LogicalExpression newCondition = conditions.condition.accept(this, functionLookupContext);
      LogicalExpression newExpr = conditions.expression.accept(this, functionLookupContext);
      conditions = new IfExpression.IfCondition(newCondition, newExpr);

      MinorType thenType = conditions.expression.getMajorType().getMinorType();
      MinorType elseType = newElseExpr.getMajorType().getMinorType();
      boolean hasUnion = thenType == MinorType.UNION || elseType == MinorType.UNION;
      MajorType outputType = ifExpr.outputType;
      if (unionTypeEnabled) {
        if (thenType != elseType && !(thenType == MinorType.NULL || elseType == MinorType.NULL)) {

          MajorType.Builder builder = MajorType.newBuilder().setMinorType(MinorType.UNION).setMode(DataMode.OPTIONAL);
          if (thenType == MinorType.UNION) {
            for (MinorType subType : conditions.expression.getMajorType().getSubTypeList()) {
              builder.addSubType(subType);
            }
          } else {
            builder.addSubType(thenType);
          }
          if (elseType == MinorType.UNION) {
            for (MinorType subType : newElseExpr.getMajorType().getSubTypeList()) {
              builder.addSubType(subType);
            }
          } else {
            builder.addSubType(elseType);
          }
          outputType = builder.build();
          conditions = new IfExpression.IfCondition(newCondition,
                  addCastExpression(conditions.expression, outputType, functionLookupContext, errorCollector, false));
          newElseExpr = addCastExpression(newElseExpr, outputType, functionLookupContext, errorCollector, false);
        }

      } else {
        // Check if we need a cast
        if (thenType != elseType && !(thenType == MinorType.NULL || elseType == MinorType.NULL)) {

          MinorType leastRestrictive = TypeCastRules.getLeastRestrictiveType((Arrays.asList(thenType, elseType)));
          if (leastRestrictive != thenType) {
            // Implicitly cast the then expression
            conditions = new IfExpression.IfCondition(newCondition,
            addCastExpression(conditions.expression, newElseExpr.getMajorType(), functionLookupContext, errorCollector));
          } else if (leastRestrictive != elseType) {
            // Implicitly cast the else expression
            newElseExpr = addCastExpression(newElseExpr, conditions.expression.getMajorType(), functionLookupContext, errorCollector);
          } else {
            /* Cannot cast one of the two expressions to make the output type of if and else expression
             * to be the same. Raise error.
             */
            throw new DrillRuntimeException("Case expression should have similar output type on all its branches");
          }
        }
      }

      // Resolve NullExpression into TypedNullConstant by visiting all conditions
      // We need to do this because we want to give the correct MajorType to the Null constant
      List<LogicalExpression> allExpressions = new ArrayList<>();
      allExpressions.add(conditions.expression);
      allExpressions.add(newElseExpr);

      boolean containsNullType = allExpressions.stream()
          .anyMatch(expression -> expression.getMajorType().getMinorType() == MinorType.NULL);
      if (containsNullType) {
        Optional<LogicalExpression> nonNullExpr = allExpressions.stream()
            .filter(expression -> expression.getMajorType().getMinorType() != MinorType.NULL)
            .findAny();

        if (nonNullExpr.isPresent()) {
          MajorType type = nonNullExpr.get().getMajorType();
          conditions = new IfExpression.IfCondition(conditions.condition, rewriteNullExpression(conditions.expression, type));
          newElseExpr = rewriteNullExpression(newElseExpr, type);
        }
      }

      if (!hasUnion) {
        // If the type of the IF expression is nullable, apply a convertToNullable*Holder function for "THEN"/"ELSE"
        // expressions whose type is not nullable.
        if (IfExpression.newBuilder().setElse(newElseExpr).setIfCondition(conditions).build().getMajorType().getMode()
                == DataMode.OPTIONAL) {
          IfExpression.IfCondition condition = conditions;
          if (condition.expression.getMajorType().getMode() != DataMode.OPTIONAL) {
            conditions = new IfExpression.IfCondition(condition.condition, getConvertToNullableExpr(ImmutableList.of(condition.expression),
                    condition.expression.getMajorType().getMinorType(), functionLookupContext));
          }

          if (newElseExpr.getMajorType().getMode() != DataMode.OPTIONAL) {
            newElseExpr = getConvertToNullableExpr(ImmutableList.of(newElseExpr),
                    newElseExpr.getMajorType().getMinorType(), functionLookupContext);
          }
        }
      }

      return validateNewExpr(
          IfExpression.newBuilder()
              .setElse(newElseExpr)
              .setIfCondition(conditions)
              .setOutputType(outputType)
              .build());
    }

    private LogicalExpression getConvertToNullableExpr(List<LogicalExpression> args, MinorType minorType,
        FunctionLookupContext functionLookupContext) {
      String funcName = "convertToNullable" + minorType.toString();
      FunctionCall funcCall = new FunctionCall(funcName, args, ExpressionPosition.UNKNOWN);
      FunctionResolver resolver = FunctionResolverFactory.getResolver(funcCall);

      DrillFuncHolder matchedConvertToNullableFuncHolder = functionLookupContext.findDrillFunction(resolver, funcCall);

      if (matchedConvertToNullableFuncHolder == null) {
        logFunctionResolutionError(errorCollector, funcCall);
        return NullExpression.INSTANCE;
      }

      return matchedConvertToNullableFuncHolder.getExpr(funcName, args, ExpressionPosition.UNKNOWN);
    }

    private LogicalExpression rewriteNullExpression(LogicalExpression expr, MajorType type) {
      if(expr instanceof NullExpression) {
        return new TypedNullConstant(type);
      } else if (expr instanceof IfExpression) {
        return rewriteIfWithNullExpression((IfExpression) expr, type);
      } else {
        return expr;
      }
    }

    private LogicalExpression rewriteIfWithNullExpression(IfExpression expr, MajorType type) {
      IfCondition condition = new IfExpression.IfCondition(expr.ifCondition.condition, rewriteNullExpression(expr.ifCondition.expression, type));
      LogicalExpression elseExpression = rewriteNullExpression(expr.elseExpression, type);
      return new IfExpression.Builder()
          .setPosition(expr.getPosition())
          .setOutputType(expr.outputType)
          .setIfCondition(condition)
          .setElse(elseExpression)
          .build();
    }

    @Override
    public LogicalExpression visitIntConstant(IntExpression intExpr, FunctionLookupContext functionLookupContext) {
      return intExpr;
    }

    @Override
    public LogicalExpression visitFloatConstant(FloatExpression fExpr, FunctionLookupContext functionLookupContext) {
      return fExpr;
    }

    @Override
    public LogicalExpression visitLongConstant(LongExpression intExpr, FunctionLookupContext functionLookupContext) {
      return intExpr;
    }

    @Override
    public LogicalExpression visitDateConstant(DateExpression intExpr, FunctionLookupContext functionLookupContext) {
      return intExpr;
    }

    @Override
    public LogicalExpression visitTimeConstant(TimeExpression intExpr, FunctionLookupContext functionLookupContext) {
      return intExpr;
    }

    @Override
    public LogicalExpression visitTimeStampConstant(TimeStampExpression intExpr, FunctionLookupContext functionLookupContext) {
      return intExpr;
    }

    @Override
    public LogicalExpression visitNullConstant(TypedNullConstant nullConstant, FunctionLookupContext functionLookupContext) throws RuntimeException {
      return nullConstant;
    }

    @Override
    public LogicalExpression visitIntervalYearConstant(IntervalYearExpression intExpr, FunctionLookupContext functionLookupContext) {
      return intExpr;
    }

    @Override
    public LogicalExpression visitIntervalDayConstant(IntervalDayExpression intExpr, FunctionLookupContext functionLookupContext) {
      return intExpr;
    }

    @Override
    public LogicalExpression visitDecimal9Constant(Decimal9Expression decExpr, FunctionLookupContext functionLookupContext) {
      return decExpr;
    }

    @Override
    public LogicalExpression visitDecimal18Constant(Decimal18Expression decExpr, FunctionLookupContext functionLookupContext) {
      return decExpr;
    }

    @Override
    public LogicalExpression visitDecimal28Constant(Decimal28Expression decExpr, FunctionLookupContext functionLookupContext) {
      return decExpr;
    }

    @Override
    public LogicalExpression visitDecimal38Constant(Decimal38Expression decExpr, FunctionLookupContext functionLookupContext) {
      return decExpr;
    }

    @Override
    public LogicalExpression visitVarDecimalConstant(VarDecimalExpression decExpr, FunctionLookupContext functionLookupContext) {
      return decExpr;
    }

    @Override
    public LogicalExpression visitDoubleConstant(DoubleExpression dExpr, FunctionLookupContext functionLookupContext) {
      return dExpr;
    }

    @Override
    public LogicalExpression visitBooleanConstant(BooleanExpression e, FunctionLookupContext functionLookupContext) {
      return e;
    }

    @Override
    public LogicalExpression visitQuotedStringConstant(QuotedString e, FunctionLookupContext functionLookupContext) {
      return e;
    }

    @Override
    public LogicalExpression visitConvertExpression(ConvertExpression e, FunctionLookupContext functionLookupContext) {
      String convertFunctionName = e.getConvertFunction() + e.getEncodingType();

      List<LogicalExpression> newArgs = new ArrayList<>();
      newArgs.add(e.getInput());  //input_expr

      FunctionCall fc = new FunctionCall(convertFunctionName, newArgs, e.getPosition());
      return fc.accept(this, functionLookupContext);
    }

    @Override
    public LogicalExpression visitCastExpression(CastExpression e, FunctionLookupContext functionLookupContext) {

      // if the cast is pointless, remove it.
      LogicalExpression input = e.getInput().accept(this,  functionLookupContext);

      MajorType newMajor = e.getMajorType(); // Output type
      MinorType newMinor = input.getMajorType().getMinorType(); // Input type

      if (castEqual(e.getPosition(), input.getMajorType(), newMajor)) {
        return input; // don't do pointless cast.
      }

      if (newMinor == MinorType.LATE) {
        // if the type still isn't fully bound, leave as cast expression.
        return new CastExpression(input, e.getMajorType(), e.getPosition());
      } else if (newMinor == MinorType.NULL) {
        // if input is a NULL expression, remove cast expression and return a TypedNullConstant directly
        // preserve original precision and scale if present
        return new TypedNullConstant(e.getMajorType().toBuilder().setMode(DataMode.OPTIONAL).build());
      } else {
        // if the type is fully bound, convert to functioncall and materialze the function.
        MajorType type = e.getMajorType();

        // Get the cast function name from the map
        String castFuncWithType = FunctionReplacementUtils.getCastFunc(type.getMinorType());

        List<LogicalExpression> newArgs = new ArrayList<>();
        newArgs.add(input);  //input_expr

        if (Types.isDecimalType(type)) {
          newArgs.add(new ValueExpressions.IntExpression(type.getPrecision(), null));
          newArgs.add(new ValueExpressions.IntExpression(type.getScale(), null));
        } else if (!Types.isFixedWidthType(type)) { //VarLen type
          newArgs.add(new ValueExpressions.LongExpression(type.getPrecision(), null));
        }

        FunctionCall fc = new FunctionCall(castFuncWithType, newArgs, e.getPosition());
        return fc.accept(this, functionLookupContext);
      }
    }

    private boolean castEqual(ExpressionPosition pos, MajorType from, MajorType to) {
      if (!from.getMinorType().equals(to.getMinorType())) {
        return false;
      }
      switch(from.getMinorType()) {
      case FLOAT4:
      case FLOAT8:
      case INT:
      case BIGINT:
      case BIT:
      case TINYINT:
      case SMALLINT:
      case UINT1:
      case UINT2:
      case UINT4:
      case UINT8:
      case TIME:
      case TIMESTAMP:
      case TIMESTAMPTZ:
      case DATE:
      case INTERVAL:
      case INTERVALDAY:
      case INTERVALYEAR:
        // nothing else matters.
        return true;
      case DECIMAL9:
      case DECIMAL18:
      case DECIMAL28DENSE:
      case DECIMAL28SPARSE:
      case DECIMAL38DENSE:
      case DECIMAL38SPARSE:
      case VARDECIMAL:
        return to.getScale() == from.getScale() && to.getPrecision() == from.getPrecision();

      case FIXED16CHAR:
      case FIXEDBINARY:
      case FIXEDCHAR:
        // width always matters
        this.errorCollector.addGeneralError(pos, "Casting fixed width types are not yet supported..");
        return false;

      case VAR16CHAR:
      case VARBINARY:
      case VARCHAR:
        // We could avoid redundant cast:
        // 1) when "to" length is no smaller than "from" length and "from" length is known (>0),
        // 2) or "to" length is unknown (0 means unknown length?).
        // Case 1 and case 2 mean that cast will do nothing.
        // In other cases, cast is required to trim the "from" according to "to" length.
        return (to.getPrecision() >= from.getPrecision() && from.getPrecision() > 0) || to.getPrecision() == 0;

      default:
        errorCollector.addGeneralError(pos, String.format("Casting rules are unknown for type %s.", from));
        return false;
      }
    }
  }
}
