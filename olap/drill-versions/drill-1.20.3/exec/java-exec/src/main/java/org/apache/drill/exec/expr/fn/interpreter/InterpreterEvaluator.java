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
package org.apache.drill.exec.expr.fn.interpreter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.ConvertExpression;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.NullExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.DrillFuncHolderExpr;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.ValueVectorReadExpression;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.fn.DrillSimpleFuncHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.ops.UdfUtilities;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.vector.ValueHolderHelper;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import io.netty.buffer.DrillBuf;

public class InterpreterEvaluator {

  public static ValueHolder evaluateConstantExpr(UdfUtilities udfUtilities, LogicalExpression expr) {
    InitVisitor initVisitor = new InitVisitor(udfUtilities);
    EvalVisitor evalVisitor = new EvalVisitor(null, udfUtilities);
    expr.accept(initVisitor, null);
    return expr.accept(evalVisitor, -1);
  }

  public static void evaluate(RecordBatch incoming, ValueVector outVV, LogicalExpression expr) {
    evaluate(incoming.getRecordCount(), incoming.getContext(), incoming, outVV, expr);
  }

  public static void evaluate(int recordCount, UdfUtilities udfUtilities, VectorAccessible incoming, ValueVector outVV, LogicalExpression expr) {

    InitVisitor initVisitor = new InitVisitor(udfUtilities);
    EvalVisitor evalVisitor = new EvalVisitor(incoming, udfUtilities);

    expr.accept(initVisitor, incoming);

    for (int i = 0; i < recordCount; i++) {
      ValueHolder out = expr.accept(evalVisitor, i);
      TypeHelper.setValueSafe(outVV, i, out);
    }

    outVV.getMutator().setValueCount(recordCount);
  }

  /**
   * Assigns specified {@code Object[] args} to the function arguments,
   * evaluates function and returns its result.
   *
   * @param interpreter function to be evaluated
   * @param args        function arguments
   * @param funcName    name of the function
   * @return result of function call stored in {@link ValueHolder}
   * @throws Exception if {@code args} types does not match function input arguments types
   */
  public static ValueHolder evaluateFunction(DrillSimpleFunc interpreter, Object[] args, String funcName) throws Exception {
    Preconditions.checkArgument(interpreter != null,
        "interpreter could not be null when use interpreted model to evaluate function " + funcName);

    // the current input index to assign into the next available parameter, found using the @Param notation
    // the order parameters are declared in the java class for the DrillFunc is meaningful
    int currParameterIndex = 0;
    Field outField = null;
    try {
      Field[] fields = interpreter.getClass().getDeclaredFields();
      for (Field f : fields) {
        // if this is annotated as a parameter to the function
        if (f.getAnnotation(Param.class) != null) {
          f.setAccessible(true);
          if (f.getType().getComponentType() != null) {
            Object array = Array.newInstance(f.getType().getComponentType(), args.length - currParameterIndex);
            for (int i = 0; i < args.length - currParameterIndex; i++) {
              Array.set(array, i, args[currParameterIndex + i]);
            }
            currParameterIndex = args.length;
            f.set(interpreter, array);
          } else if (currParameterIndex < args.length) {
            f.set(interpreter, args[currParameterIndex]);
            currParameterIndex++;
          }
        } else if (f.getAnnotation(Output.class) != null) {
          f.setAccessible(true);
          outField = f;
          // create an instance of the holder for the output to be stored in
          f.set(interpreter, f.getType().newInstance());
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    if (args.length != currParameterIndex ) {
      throw new DrillRuntimeException(
          String.format("Wrong number of parameters provided to interpreted expression evaluation " +
                  "for function %s, expected %d parameters, but received %d.",
              funcName, currParameterIndex, args.length));
    }
    if (outField == null) {
      throw new DrillRuntimeException("Malformed DrillFunction without a return type: " + funcName);
    }
    interpreter.setup();
    interpreter.eval();

    return (ValueHolder) outField.get(interpreter);
  }

  private static class InitVisitor extends AbstractExprVisitor<LogicalExpression, VectorAccessible, RuntimeException> {

    private final UdfUtilities udfUtilities;

    protected InitVisitor(UdfUtilities udfUtilities) {
      super();
      this.udfUtilities = udfUtilities;
    }

    @Override
    public LogicalExpression visitFunctionHolderExpression(FunctionHolderExpression holderExpr, VectorAccessible incoming) {
      if (!(holderExpr.getHolder() instanceof DrillSimpleFuncHolder)) {
        throw new UnsupportedOperationException("Only Drill simple UDF can be used in interpreter mode!");
      }

      DrillSimpleFuncHolder holder = (DrillSimpleFuncHolder) holderExpr.getHolder();

      for (int i = 0; i < holderExpr.args.size(); i++) {
        holderExpr.args.get(i).accept(this, incoming);
      }

      try {
        DrillSimpleFunc interpreter = holder.createInterpreter();
        Field[] fields = interpreter.getClass().getDeclaredFields();
        for (Field f : fields) {
          if (f.getAnnotation(Inject.class) != null) {
            f.setAccessible(true);
            Class<?> fieldType = f.getType();
            if (UdfUtilities.INJECTABLE_GETTER_METHODS.get(fieldType) != null) {
              Method method = udfUtilities.getClass().getMethod(UdfUtilities.INJECTABLE_GETTER_METHODS.get(fieldType));
              f.set(interpreter, method.invoke(udfUtilities));
            } else {
              // Invalid injectable type provided, this should have been caught in FunctionConverter
              throw new DrillRuntimeException("Invalid injectable type requested in UDF: " + fieldType.getSimpleName());
            }
          } else { // do nothing with non-inject fields here
            continue;
          }
        }

        ((DrillFuncHolderExpr) holderExpr).setInterpreter(interpreter);
        return holderExpr;

      } catch (Exception ex) {
        throw new RuntimeException("Error in evaluating function of " + holderExpr.getName() + ": ", ex);
      }
    }

    @Override
    public LogicalExpression visitUnknown(LogicalExpression e, VectorAccessible incoming) throws RuntimeException {
      for (LogicalExpression child : e) {
        child.accept(this, incoming);
      }

      return e;
    }
  }

  public static class EvalVisitor extends AbstractExprVisitor<ValueHolder, Integer, RuntimeException> {
    private final VectorAccessible incoming;
    private final UdfUtilities udfUtilities;

    protected EvalVisitor(VectorAccessible incoming, UdfUtilities udfUtilities) {
      super();
      this.incoming = incoming;
      this.udfUtilities = udfUtilities;
    }

    @Override
    public ValueHolder visitFunctionCall(FunctionCall call, Integer value) throws RuntimeException {
      return visitUnknown(call, value);
    }

    @Override
    public ValueHolder visitSchemaPath(SchemaPath path,Integer value) throws RuntimeException {
      return visitUnknown(path, value);
    }

    @Override
    public ValueHolder visitDecimal9Constant(ValueExpressions.Decimal9Expression decExpr,Integer value) throws RuntimeException {
      return ValueHolderHelper.getDecimal9Holder(decExpr.getIntFromDecimal(), decExpr.getPrecision(), decExpr.getScale());
    }

    @Override
    public ValueHolder visitDecimal18Constant(ValueExpressions.Decimal18Expression decExpr,Integer value) throws RuntimeException {
      return ValueHolderHelper.getDecimal18Holder(decExpr.getLongFromDecimal(), decExpr.getPrecision(), decExpr.getScale());
    }

    @Override
    public ValueHolder visitDecimal28Constant(final ValueExpressions.Decimal28Expression decExpr,Integer value) throws RuntimeException {
      return getConstantValueHolder(decExpr.getBigDecimal().toString(), decExpr.getMajorType().getMinorType(), new Function<DrillBuf, ValueHolder>() {
        @Nullable
        @Override
        public ValueHolder apply(DrillBuf buffer) {
          return ValueHolderHelper.getDecimal28Holder(buffer, decExpr.getBigDecimal());
        }
      });
    }

    @Override
    public ValueHolder visitDecimal38Constant(final ValueExpressions.Decimal38Expression decExpr,Integer value) throws RuntimeException {
      return getConstantValueHolder(decExpr.getBigDecimal().toString(), decExpr.getMajorType().getMinorType(), new Function<DrillBuf, ValueHolder>() {
        @Nullable
        @Override
        public ValueHolder apply(DrillBuf buffer) {
          return ValueHolderHelper.getDecimal38Holder(buffer, decExpr.getBigDecimal());
        }
      });
    }

    @Override
    public ValueHolder visitVarDecimalConstant(final ValueExpressions.VarDecimalExpression decExpr, Integer value) throws RuntimeException {
      return getConstantValueHolder(decExpr.getBigDecimal().toString(), decExpr.getMajorType().getMinorType(),
          buffer -> ValueHolderHelper.getVarDecimalHolder(Objects.requireNonNull(buffer), decExpr.getBigDecimal()));
    }

    @Override
    public ValueHolder visitDateConstant(ValueExpressions.DateExpression dateExpr,Integer value) throws RuntimeException {
      return ValueHolderHelper.getDateHolder(dateExpr.getDate());
    }

    @Override
    public ValueHolder visitTimeConstant(ValueExpressions.TimeExpression timeExpr,Integer value) throws RuntimeException {
      return ValueHolderHelper.getTimeHolder(timeExpr.getTime());
    }

    @Override
    public ValueHolder visitTimeStampConstant(ValueExpressions.TimeStampExpression timestampExpr,Integer value) throws RuntimeException {
      return ValueHolderHelper.getTimeStampHolder(timestampExpr.getTimeStamp());
    }

    @Override
    public ValueHolder visitIntervalYearConstant(ValueExpressions.IntervalYearExpression intExpr,Integer value) throws RuntimeException {
      return ValueHolderHelper.getIntervalYearHolder(intExpr.getIntervalYear());
    }

    @Override
    public ValueHolder visitIntervalDayConstant(ValueExpressions.IntervalDayExpression intExpr,Integer value) throws RuntimeException {
      return ValueHolderHelper.getIntervalDayHolder(intExpr.getIntervalDay(), intExpr.getIntervalMillis());
    }

    @Override
    public ValueHolder visitBooleanConstant(ValueExpressions.BooleanExpression e,Integer value) throws RuntimeException {
      return ValueHolderHelper.getBitHolder(e.getBoolean() == false ? 0 : 1);
    }

    @Override
    public ValueHolder visitNullConstant(TypedNullConstant e,Integer value) throws RuntimeException {
      // create a value holder for the given type, defaults to NULL value if not set
      return TypeHelper.createValueHolder(e.getMajorType());
    }

    // TODO - review what to do with these
    // **********************************
    @Override
    public ValueHolder visitConvertExpression(ConvertExpression e,Integer value) throws RuntimeException {
      return visitUnknown(e, value);
    }

    @Override
    public ValueHolder visitNullExpression(NullExpression e,Integer value) throws RuntimeException {
      return visitUnknown(e, value);
    }

    // TODO - review what to do with these (2 functions above)
    //********************************************
    @Override
    public ValueHolder visitFunctionHolderExpression(FunctionHolderExpression holderExpr, Integer inIndex) {
      if (! (holderExpr.getHolder() instanceof DrillSimpleFuncHolder)) {
        throw new UnsupportedOperationException("Only Drill simple UDF can be used in interpreter mode!");
      }

      DrillSimpleFuncHolder holder = (DrillSimpleFuncHolder) holderExpr.getHolder();

      // function arguments may have different types:
      // usually ValueHolder inheritors but sometimes FieldReader ones
      Object[] args = new Object[holderExpr.args.size()];
      for (int i = 0; i < holderExpr.args.size(); i++) {
        ValueHolder valueHolder = holderExpr.args.get(i).accept(this, inIndex);
        Object resultArg = valueHolder;
        TypeProtos.MajorType argType = TypeHelper.getValueHolderType(valueHolder);
        TypeProtos.MajorType holderParamType = holder.getParamMajorType(i);
        // In case function use "NULL_IF_NULL" policy.
        if (holder.getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL) {
          // Case 1: parameter is non-nullable, argument is nullable.
          if (holderParamType.getMode() == TypeProtos.DataMode.REQUIRED
              && argType.getMode() == TypeProtos.DataMode.OPTIONAL) {
            // Case 1.1 : argument is null, return null value holder directly.
            if (TypeHelper.isNull(valueHolder)) {
              return TypeHelper.createValueHolder(holderExpr.getMajorType());
            } else {
              // Case 1.2: argument is nullable but not null value, deNullify it.
              resultArg = TypeHelper.deNullify(valueHolder);
            }
          } else if (holderParamType.getMode() == TypeProtos.DataMode.OPTIONAL
              && argType.getMode() == TypeProtos.DataMode.REQUIRED) {
            // Case 2: parameter is nullable, argument is non-nullable. Nullify it.
            resultArg = TypeHelper.nullify(valueHolder);
          }
        }
        if (holder.getAttributeParameter(i).isFieldReader()) {
          resultArg = BasicTypeHelper.getHolderReaderImpl(argType, valueHolder);
        }
        args[i] = resultArg;
      }

      try {
        DrillSimpleFunc interpreter =  ((DrillFuncHolderExpr) holderExpr).getInterpreter();

        ValueHolder out = evaluateFunction(interpreter, args, holderExpr.getName());

        TypeProtos.MajorType outputType = TypeHelper.getValueHolderType(out);
        if (outputType.getMode() == TypeProtos.DataMode.OPTIONAL &&
            holderExpr.getMajorType().getMode() == TypeProtos.DataMode.REQUIRED) {
          return TypeHelper.deNullify(out);
        } else if (outputType.getMode() == TypeProtos.DataMode.REQUIRED &&
              holderExpr.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
          return TypeHelper.nullify(out);
        } else {
          return out;
        }

      } catch (Exception ex) {
        throw new RuntimeException("Error in evaluating function of " + holderExpr.getName(), ex);
      }
    }

    @Override
    public ValueHolder visitBooleanOperator(BooleanOperator op, Integer inIndex) {
      // Apply short circuit evaluation to boolean operator.
      if (op.getName().equals(FunctionNames.AND)) {
        return visitBooleanAnd(op, inIndex);
      } else if(op.getName().equals(FunctionNames.OR)) {
        return visitBooleanOr(op, inIndex);
      } else {
        throw new UnsupportedOperationException(
            String.format("BooleanOperator can only be %s, %s. You are using %s",
                FunctionNames.AND, FunctionNames.OR, op.getName()));
      }
    }

    @Override
    public ValueHolder visitIfExpression(IfExpression ifExpr, Integer inIndex) throws RuntimeException {
      ValueHolder condHolder = ifExpr.ifCondition.condition.accept(this, inIndex);

      Preconditions.checkArgument (condHolder instanceof BitHolder || condHolder instanceof NullableBitHolder,
          "IfExpression's condition does not have type of BitHolder or NullableBitHolder.");

      Trivalent flag = isBitOn(condHolder);

      switch (flag) {
        case TRUE:
          return ifExpr.ifCondition.expression.accept(this, inIndex);
        case FALSE:
        case NULL:
          return ifExpr.elseExpression.accept(this, inIndex);
        default:
          throw new UnsupportedOperationException("No other possible choice. Something is not right");
      }
    }

    @Override
    public ValueHolder visitIntConstant(ValueExpressions.IntExpression e, Integer inIndex) throws RuntimeException {
      return ValueHolderHelper.getIntHolder(e.getInt());
    }

    @Override
    public ValueHolder visitFloatConstant(ValueExpressions.FloatExpression fExpr, Integer value) throws RuntimeException {
      return ValueHolderHelper.getFloat4Holder(fExpr.getFloat());
    }

    @Override
    public ValueHolder visitLongConstant(ValueExpressions.LongExpression intExpr, Integer value) throws RuntimeException {
      return ValueHolderHelper.getBigIntHolder(intExpr.getLong());
    }

    @Override
    public ValueHolder visitDoubleConstant(ValueExpressions.DoubleExpression dExpr, Integer value) throws RuntimeException {
      return ValueHolderHelper.getFloat8Holder(dExpr.getDouble());
    }

    @Override
    public ValueHolder visitQuotedStringConstant(final ValueExpressions.QuotedString e, Integer value) throws RuntimeException {
      return getConstantValueHolder(e.value, e.getMajorType().getMinorType(), new Function<DrillBuf, ValueHolder>() {
        @Nullable
        @Override
        public ValueHolder apply(DrillBuf buffer) {
          return ValueHolderHelper.getVarCharHolder(buffer, e.value);
        }
      });
    }

    @Override
    public ValueHolder visitUnknown(LogicalExpression e, Integer inIndex) throws RuntimeException {
      if (e instanceof ValueVectorReadExpression) {
        return visitValueVectorReadExpression((ValueVectorReadExpression) e, inIndex);
      } else {
        return super.visitUnknown(e, inIndex);
      }
    }

    protected ValueHolder visitValueVectorReadExpression(ValueVectorReadExpression e, Integer inIndex)
        throws RuntimeException {
      TypeProtos.MajorType type = e.getMajorType();

      ValueVector vv;
      ValueHolder holder;
      try {
        switch (type.getMode()) {
          case OPTIONAL:
          case REQUIRED:
            vv = incoming.getValueAccessorById(TypeHelper.getValueVectorClass(type.getMinorType(),type.getMode()), e.getFieldId().getFieldIds()).getValueVector();
            holder = TypeHelper.getValue(vv, inIndex.intValue());
            return holder;
          default:
            throw new UnsupportedOperationException("Type of " + type + " is not supported yet in interpreted expression evaluation!");
        }
      } catch (Exception ex){
        throw new DrillRuntimeException("Error when evaluate a ValueVectorReadExpression: " + ex);
      }
    }

    // Use Kleene algebra for three-valued logic :
    //  value of boolean "and" when one side is null
    //    p       q     p and q
    //    true    null     null
    //    false   null     false
    //    null    true     null
    //    null    false    false
    //    null    null     null
    //  "and" : 1) if any argument is false, return false. false is earlyExitValue.
    //          2) if none argument is false, but at least one is null, return null.
    //          3) finally, return true (finalValue).
    private ValueHolder visitBooleanAnd(BooleanOperator op, Integer inIndex) {
      ValueHolder [] args = new ValueHolder [op.argCount()];
      boolean hasNull = false;
      for (int i = 0; i < op.argCount(); i++) {
        args[i] = op.arg(i).accept(this, inIndex);

        Trivalent flag = isBitOn(args[i]);

        switch (flag) {
          case FALSE:
            return op.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL? TypeHelper.nullify(ValueHolderHelper.getBitHolder(0)) : ValueHolderHelper.getBitHolder(0);
          case NULL:
            hasNull = true;
          case TRUE:
        }
      }

      if (hasNull) {
        return ValueHolderHelper.getNullableBitHolder(true, 0);
      } else {
        return op.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL? TypeHelper.nullify(ValueHolderHelper.getBitHolder(1)) : ValueHolderHelper.getBitHolder(1);
      }
    }

    //  value of boolean "or" when one side is null
    //    p       q       p and q
    //    true    null     true
    //    false   null     null
    //    null    true     true
    //    null    false    null
    //    null    null     null
    private ValueHolder visitBooleanOr(BooleanOperator op, Integer inIndex) {
      ValueHolder [] args = new ValueHolder [op.argCount()];
      boolean hasNull = false;
      for (int i = 0; i < op.argCount(); i++) {
        args[i] = op.arg(i).accept(this, inIndex);

        Trivalent flag = isBitOn(args[i]);

        switch (flag) {
          case TRUE:
            return op.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL? TypeHelper.nullify(ValueHolderHelper.getBitHolder(1)) : ValueHolderHelper.getBitHolder(1);
          case NULL:
            hasNull = true;
          case FALSE:
        }
      }

      if (hasNull) {
        return ValueHolderHelper.getNullableBitHolder(true, 0);
      } else {
        return op.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL? TypeHelper.nullify(ValueHolderHelper.getBitHolder(0)) : ValueHolderHelper.getBitHolder(0);
      }
    }

    public enum Trivalent {
      FALSE,
      TRUE,
      NULL
    }

    private Trivalent isBitOn(ValueHolder holder) {
      Preconditions.checkArgument(holder instanceof BitHolder || holder instanceof NullableBitHolder,
          "Input does not have type of BitHolder or NullableBitHolder.");

      if ( (holder instanceof BitHolder && ((BitHolder) holder).value == 1)) {
        return Trivalent.TRUE;
      } else if (holder instanceof NullableBitHolder && ((NullableBitHolder) holder).isSet == 1 && ((NullableBitHolder) holder).value == 1) {
        return Trivalent.TRUE;
      } else if (holder instanceof NullableBitHolder && ((NullableBitHolder) holder).isSet == 0) {
        return Trivalent.NULL;
      } else {
        return Trivalent.FALSE;
      }
    }

    private ValueHolder getConstantValueHolder(String value, MinorType type, Function<DrillBuf, ValueHolder> holderInitializer) {
      return udfUtilities.getConstantValueHolder(value, type, holderInitializer);
    }
  }
}

