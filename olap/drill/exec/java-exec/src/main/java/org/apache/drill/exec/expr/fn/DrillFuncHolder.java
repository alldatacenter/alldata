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
package org.apache.drill.exec.expr.fn;

import java.util.Arrays;
import java.util.List;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.compile.bytecode.ScalarReplacementTypes;
import org.apache.drill.exec.compile.sig.SignatureHolder;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.BlockType;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.DrillFuncHolderExpr;
import org.apache.drill.exec.expr.EvaluationVisitor.VectorVariableHolder;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.fn.output.OutputWidthCalculator;
import org.apache.drill.exec.expr.holders.UnionHolder;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.ops.UdfUtilities;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JAssignmentTarget;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public abstract class DrillFuncHolder extends AbstractFuncHolder {
  private static final Logger logger = LoggerFactory.getLogger(DrillFuncHolder.class);

  private final FunctionAttributes attributes;
  private final FunctionInitializer initializer;

  public DrillFuncHolder(
      FunctionAttributes attributes,
      FunctionInitializer initializer) {
    this.attributes = attributes;
    this.initializer = initializer;

    checkNullHandling(attributes.getNullHandling());
  }

  /**
   * Check if function type supports provided null handling strategy.
   * <p>
   * Keep in mind that this method is invoked in
   * {@link #DrillFuncHolder(FunctionAttributes, FunctionInitializer)}
   * constructor so make sure not to use any state fields when overriding the
   * method to avoid uninitialized state.
   * </p>
   *
   * @param nullHandling
   *          null handling strategy defined for a function
   * @throws IllegalArgumentException
   *           if provided {@code nullHandling} is not supported
   */
  protected void checkNullHandling(NullHandling nullHandling) {
  }

  protected String meth(String methodName) {
    return meth(methodName, true);
  }

  protected String meth(String methodName, boolean required) {
    String method = initializer.getMethod(methodName);
    if (method == null) {
      if (!required) {
        return "";
      }
      throw UserException
          .functionError()
          .message("Failure while trying use function. No body found for required method %s.", methodName)
          .addContext("FunctionClass", initializer.getClassName())
          .build(logger);
    }
    return method;
  }

  @Override
  public JVar[] renderStart(ClassGenerator<?> g, HoldingContainer[] inputVariables, FieldReference fieldReference) {
    return declareWorkspaceVariables(g);
  }

  @Override
  public FunctionHolderExpression getExpr(String name, List<LogicalExpression> args, ExpressionPosition pos) {
    return new DrillFuncHolderExpr(name, this, args, pos);
  }

  public boolean isAggregating() {
    return false;
  }

  public boolean isDeterministic() {
    return attributes.isDeterministic();
  }

  public boolean isNiladic() {
    return attributes.isNiladic();
  }

  public boolean isInternal() {
    return attributes.isInternal();
  }

  public boolean isVarArg() {
    return attributes.isVarArg();
  }

  /**
   * Generates string representation of function input parameters:
   * PARAMETER_TYPE_1-PARAMETER_MODE_1,PARAMETER_TYPE_2-PARAMETER_MODE_2
   * Example: VARCHAR-REQUIRED,VARCHAR-OPTIONAL
   * Returns empty string if function has no input parameters.
   *
   * @return string representation of function input parameters
   */
  public String getInputParameters() {
    StringBuilder builder = new StringBuilder();
    for (ValueReference ref : attributes.getParameters()) {
      MajorType type = ref.getType();
      builder.append(",");
      builder.append(type.getMinorType().toString());
      builder.append("-");
      builder.append(type.getMode().toString());
    }
    if (isVarArg() && getParamCount() > 0) {
      builder.append("...");
    }
    return builder.length() == 0 ? builder.toString() : builder.substring(1);
  }

  /**
   * @return instance of class loader used to load function
   */
  public ClassLoader getClassLoader() {
    return initializer.getClassLoader();
  }

  protected JVar[] declareWorkspaceVariables(ClassGenerator<?> g) {
    JVar[] workspaceJVars = new JVar[attributes.getWorkspaceVars().length];
    for (int i = 0; i < attributes.getWorkspaceVars().length; i++) {
      WorkspaceReference ref = attributes.getWorkspaceVars()[i];
      JType jtype = g.getModel()._ref(ref.getType());
      workspaceJVars[i] = g.declareClassField("work", jtype);

      if (ScalarReplacementTypes.CLASSES.contains(ref.getType())) {
        JBlock b = g.getBlock(SignatureHolder.DRILL_INIT_METHOD);
        b.assign(workspaceJVars[i], JExpr._new(jtype));
      }

      if (ref.isInject()) {
        assignInjectableValue(g, workspaceJVars[i], ref);
      }
    }
    return workspaceJVars;
  }

  protected void assignInjectableValue(ClassGenerator<?> g, JVar variable, WorkspaceReference ref) {
    if (UdfUtilities.INJECTABLE_GETTER_METHODS.get(ref.getType()) != null) {
      g.getBlock(BlockType.SETUP).assign(
          variable,
          g.getMappingSet().getIncoming().invoke("getContext").invoke(
              UdfUtilities.INJECTABLE_GETTER_METHODS.get(ref.getType())
          ));
    } else {
      // Invalid injectable type provided, this should have been caught in FunctionConverter
      throw new DrillRuntimeException("Invalid injectable type requested in UDF: " +
              ref.getType().getSimpleName());
    }
  }

  /**
   * Generate the body of a Drill function by copying the source code of the
   * corresponding function method into the generated output. For this to work,
   * all symbol references must be absolute (they cannot refer to imports),
   * or they must refer to local variables or class fields marked with an
   * annotation.
   * <p>
   * To make this work, the function body is wrapped in a code block that
   * simulates the class fields by declaring local variables of the same
   * name, and assigning those variables based on the input and workspace
   * variables provided.
   * <p>
   * This version is used for blocks other than the main eval block.
   *
   * @param g code generator
   * @param bt type of the block to be added
   * @param body source code of the block. Optional. Block will be omitted
   * if the method body is null or empty
   * @param inputVariables list of input variable bindings which match up to declared
   * <code>@Param</code> member variables in order. The input variables have the
   * same name as the parameters that they represent
   * @param workspaceJVars list of workspace variables, structures the same as
   * input variables
   * @param workspaceOnly <code>true</code> if this is a setup block and
   * we should declare only constant workspace variables, <code>false</code> to
   * declare all variables
   */
  protected void generateBody(ClassGenerator<?> g, BlockType bt, String body, HoldingContainer[] inputVariables,
      JVar[] workspaceJVars, boolean workspaceOnly) {
    if (Strings.isNullOrEmpty(body) || body.trim().isEmpty()) {
      return;
    }
    g.getBlock(bt).directStatement(String.format(
        "/** start %s for function %s **/ ", bt.name(), attributes.getRegisteredNames()[0]));

    JBlock sub = new JBlock(true, true);
    addProtectedBlock(g, sub, body, inputVariables, workspaceJVars, workspaceOnly);
    g.getBlock(bt).add(sub);

    g.getBlock(bt).directStatement(String.format(
        "/** end %s for function %s **/ ", bt.name(), attributes.getRegisteredNames()[0]));
  }

  /**
   * Generate the function block itself, without surrounding comments, and
   * whether or not the method is empty.
   */
  protected void addProtectedBlock(ClassGenerator<?> g, JBlock sub, String body,
      HoldingContainer[] inputVariables,
      JVar[] workspaceJVars, boolean workspaceOnly) {

    // Create the binding between simulated function fields and their values,
    if (inputVariables != null) {
      // If VarArgs, all input variables go into a single array.
      if (isVarArg()) {
        declareVarArgArray(g.getModel(), sub, inputVariables);
      }
      for (int i = 0; i < inputVariables.length; i++) {
        if (workspaceOnly && !inputVariables[i].isConstant()) {
          continue;
        }
        declareInputVariable(g.getModel(), sub, inputVariables[i], i);
      }
    }

    // Declare workspace variables
    JVar[] internalVars = new JVar[workspaceJVars.length];
    for (int i = 0; i < workspaceJVars.length; i++) {
      internalVars[i] = sub.decl(
          g.getModel()._ref(attributes.getWorkspaceVars()[i].getType()),
          attributes.getWorkspaceVars()[i].getName(), workspaceJVars[i]);
    }

    // Add the source code for the block.
    Preconditions.checkNotNull(body);
    sub.directStatement(body);

    // reassign workspace variables back to global space.
    for (int i = 0; i < workspaceJVars.length; i++) {
      sub.assign(workspaceJVars[i], internalVars[i]);
    }
  }

  /**
   * Declares array for storing vararg function arguments.
   *
   * @param model          code model to generate the code
   * @param jBlock         block of code to be populated
   * @param inputVariables array of input variables for current function
   */
  protected void declareVarArgArray(JCodeModel model, JBlock jBlock, HoldingContainer[] inputVariables) {
    ValueReference parameter = getAttributeParameter(getParamCount() - 1);
    JType defaultType;
    if (inputVariables.length >= getParamCount()) {
      defaultType = inputVariables[inputVariables.length - 1].getHolder().type();
    } else if (parameter.getType().getMinorType() == MinorType.LATE) {
      defaultType = model._ref(ValueHolder.class);
    } else {
      defaultType = TypeHelper.getHolderType(model, parameter.getType().getMinorType(), parameter.getType().getMode());
    }
    JType paramClass = getParamClass(model, parameter, defaultType);
    jBlock.decl(paramClass.array(), parameter.getName(), JExpr.newArray(paramClass, inputVariables.length - getParamCount() + 1));
  }

  /**
   * Generate the top part of a function call which simulates passing parameters
   * into the function. Given the following declaration: <code><pre>
   * public static class UnionIsBigInt implements DrillSimpleFunc {
   *   @Param UnionHolder in;
   * </pre></code>, we generate code like the following: <code><pre>
   *  final BitHolder out = new BitHolder();
   *  FieldReader in = reader4;
   * </pre></code>
   * <p>
   * Declares attribute parameter which corresponds to specified {@code currentIndex}
   * in specified {@code jBlock}. Parameters are those fields
   * in the function declaration annotated with a <tt>@Param</tt> tag. Parameters
   * or expressions can both be represented by either a <code>FieldReader</code>
   * or a value holder. Perform conversion between the two as needed.
   *
   * @param model         code model to generate the code
   * @param jBlock        block of code to be populated
   * @param inputVariable input variable for current function
   * @param currentIndex  index of current parameter
   */
  protected void declareInputVariable(JCodeModel model, JBlock jBlock,
      HoldingContainer inputVariable, int currentIndex) {
    ValueReference parameter = getAttributeParameter(currentIndex);
    if (!inputVariable.isReader() && parameter.isFieldReader()) {
      convertHolderToReader(model, jBlock, inputVariable, currentIndex, parameter);
    } else if (inputVariable.isReader() && !parameter.isFieldReader()) {
      convertReaderToHolder(model, jBlock, inputVariable, currentIndex, parameter);
    } else {
      assignParamDirectly(jBlock, inputVariable, currentIndex, parameter);
    }
  }

  /**
   * Convert an input variable (in the generated code) to a reader as declared
   * on the input parameter.
   */
  private void convertHolderToReader(JCodeModel model, JBlock jBlock,
      HoldingContainer inputVariable, int currentIndex,
      ValueReference parameter) {
    JVar inputHolder = inputVariable.getHolder();
    MajorType inputSqlType = inputVariable.getMajorType();

    // The parameter is a reader in this case.
    JType paramClass = model._ref(FieldReader.class);
    if (Types.isComplex(inputSqlType)) {
        throw new UnsupportedOperationException(String.format(
            "Cannot convert values of type %s from a holder to a reader",
            inputSqlType.getMinorType().name()));
    } else if (Types.isUnion(inputSqlType)) {
      // For the UNION type, there is no simple conversion from
      // a value holder to a reader.
      //
      // Prior to the fix for DRILL-7502, the parameter is redefined
      // from type FieldReader to type UnionHolder. This is clearly
      // wrong, but it worked in most cases. It DOES NOT work for the
      // typeof() function, which needs a reader. Old code:

      // assignParamDirectly(jBlock, inputVariable, currentIndex, parameter);

      // A large amount of code depends on the UNION being represented
      // as a UnionHolder, especially that for handling varying types.
      // ExpressionTreeMaterializer.rewriteUnionFunction(), for example
      // relies heavily on UnionHolder. As a result, we cannot simply
      // change the "holder" for the UNION to be a reader, as is done
      // for complex types.
      //
      // Run TestTopNSchemaChanges.testNumericTypes with saving of code
      // enabled, and look at the second "PriorityQueueGen" for an example.

      // One would think that the following should work: the UnionHolder
      // has a reader field. However, that field is not set in the code
      // gen path; only when creating a holder from a reader. Code which
      // does NOT work:

      // jBlock.decl(paramClass, parameter.getName(), inputHolder.ref("reader"));

      // One solution that works (but is probably slow and redundant) is to
      // obtain the reader directly from the underling value vector. We saved
      // this information when defining the holder. We retrieve it here
      // and insert the vector --> reader conversion.
      VectorVariableHolder vvHolder = (VectorVariableHolder) inputVariable;
      JVar readerVar = vvHolder.generateUnionReader();
      declare(jBlock, parameter, paramClass, readerVar, currentIndex);

      // TODO: This probably needs a more elegant solution, but this does
      // work for now. Run TestTypeFns.testUnionType to verify.
    } else {
      // FooHolderReader param = new FooHolderReader(inputVar);
      JType singularReaderClass = model._ref(
          TypeHelper.getHolderReaderImpl(inputSqlType.getMinorType(),
              inputSqlType.getMode()));
      JInvocation reader = JExpr._new(singularReaderClass).arg(inputHolder);
      declare(jBlock, parameter, paramClass, reader, currentIndex);
    }
  }

  /**
   * Convert an input value holder (in the generated code) into a value
   * holder as declared on the input parameter.
   */
  private void convertReaderToHolder(JCodeModel model, JBlock jBlock,
      HoldingContainer inputVariable, int currentIndex,
      ValueReference parameter) {
    JVar inputHolder = inputVariable.getHolder();
    MajorType inputSqlType = inputVariable.getMajorType();
    if (Types.isComplex(parameter.getType())) {
      // For complex data-types (repeated maps/lists/dicts) the input to the
      // aggregate will be a FieldReader. However, aggregate
      // functions like ANY_VALUE, will assume the input to be a
      // RepeatedMapHolder etc. Generate boilerplate code, to map
      // from FieldReader to respective Holder.

      // FooHolder param = new FooHolder();
      // param.reader = inputVar;
      JType holderClass = getParamClass(model, parameter, inputHolder.type());
      JAssignmentTarget holderVar = declare(jBlock, parameter, holderClass, JExpr._new(holderClass), currentIndex);
      jBlock.assign(holderVar.ref("reader"), inputHolder);
    } else if (Types.isUnion(inputSqlType)) {
      // Normally unions are generated as a UnionHolder. However, if a parameter
      // is a FieldReader, then we must generate the union as a UnionReader.
      // Then, if there is another function that use a holder, we can convert
      // from the UnionReader to a UnionHolder.
      //
      // UnionHolder param = new UnionHolder();
      // inputVar.read(param);
      JType holderClass = model._ref(UnionHolder.class);
      JAssignmentTarget paramVar = jBlock.decl(holderClass, parameter.getName(), JExpr._new(holderClass));
      JInvocation readCall = inputHolder.invoke("read");
      readCall.arg(paramVar);
      jBlock.add(readCall);
    } else {
      throw new UnsupportedOperationException(String.format(
          "Cannot convert values of type %s from a reader to a holder",
          inputSqlType.getMinorType().name()));
    }
  }

  /**
   * The input variable and parameter are both either a holder or a
   * field reader.
   */
  private void assignParamDirectly(JBlock jBlock,
      HoldingContainer inputVariable, int currentIndex,
      ValueReference parameter) {

    // Declare parameter as the type of the input variable because
    // if we get here, they must be the same type.
    //
    // InputType param = inputVar;
    JVar inputHolder = inputVariable.getHolder();
    declare(jBlock, parameter, inputHolder.type(),
        inputHolder, currentIndex);
  }

  /**
   * Returns {@link JType} instance which corresponds to the parameter of the function.
   *
   * @param model       code model to generate the code
   * @param parameter   function parameter which determines resulting type
   * @param defaultType type to be returned for the case when parameter does not hold specific type
   * @return {@link JType} instance which corresponds to the parameter of the function
   */
  private JType getParamClass(JCodeModel model, ValueReference parameter, JType defaultType) {
    if (parameter.isFieldReader()) {
      return model._ref(FieldReader.class);
    }

    if (Types.isComplex(parameter.getType())) {
      MajorType type = parameter.getType();
      return TypeHelper.getComplexHolderType(model, type.getMinorType(), type.getMode());
    }

    return defaultType;
  }

  /**
   * Declares specified {@code paramExpression} in specified {@code jBlock}
   * and assigns it to the array component if required and / or returns declared expression.
   *
   * @param jBlock          target block where declaration is added
   * @param parameter       function parameter which should be declared
   * @param paramClass      type of the declared variable
   * @param paramExpression expression to be declared
   * @param currentIndex    index of current parameter
   * @return declared expression
   */
  protected JAssignmentTarget declare(JBlock jBlock, ValueReference parameter,
      JType paramClass, JExpression paramExpression, int currentIndex) {
    if (parameter.isVarArg()) {
      // For VarArg, an array has already been generated:

      // FieldReader[] in = new FieldReader[ 4 ] ;

      // Here we assign the input value to the array:

      // in[ 0 ] = new VarCharHolderReaderImpl(constant1);
      JAssignmentTarget arrayComponent = JExpr.ref(parameter.getName()).component(JExpr.lit(currentIndex - getParamCount() + 1));
      jBlock.assign(arrayComponent, paramExpression);
      return arrayComponent;
    } else {
      // Actual define the input variable with an expression.
      // Foo in = bar12;

      return jBlock.decl(paramClass, parameter.getName(), paramExpression);
    }
  }

  public boolean matches(MajorType returnType, List<MajorType> argTypes) {

    if (!softCompare(returnType, attributes.getReturnValue().getType())) {
      return false;
    }

    if (argTypes.size() != attributes.getParameters().length) {
      return false;
    }

    for (int i = 0; i < attributes.getParameters().length; i++) {
      if (!softCompare(getAttributeParameter(i).getType(), argTypes.get(i))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public MajorType getParamMajorType(int i) {
    return getAttributeParameter(i).getType();
  }

  @Override
  public int getParamCount() {
    return attributes.getParameters().length;
  }

  public boolean isConstant(int i) {
    return getAttributeParameter(i).isConstant();
  }

  /**
   * Returns i-th function attribute parameter.
   * For the case when current function is vararg and specified index
   * is greater than or equals to the attributes count, the last function attribute parameter  is returned.
   *
   * @param i index of function attribute parameter to  be returned
   * @return i-th function attribute parameter
   */
  public ValueReference getAttributeParameter(int i) {
    if (i >= getParamCount() && attributes.isVarArg()) {
      return attributes.getParameters()[getParamCount() - 1];
    }
    return attributes.getParameters()[i];
  }

  public boolean isFieldReader(int i) {
    return getAttributeParameter(i).isFieldReader();
  }

  public MajorType getReturnType(final List<LogicalExpression> logicalExpressions) {
    return attributes.getReturnType().getType(logicalExpressions, attributes);
  }

  public OutputWidthCalculator getOutputWidthCalculator() {
    return attributes.getOutputWidthCalculatorType().getOutputWidthCalculator();
  }

  public int variableOutputSizeEstimate(){
    return attributes.variableOutputSizeEstimate();
  }

  public NullHandling getNullHandling() {
    return attributes.getNullHandling();
  }

  private boolean softCompare(MajorType a, MajorType b) {
    return Types.softEquals(a, b, getNullHandling() == NullHandling.NULL_IF_NULL);
  }

  public String[] getRegisteredNames() {
    return attributes.getRegisteredNames();
  }

  public int getCostCategory() {
    return attributes.getCostCategory().getValue();
  }

  public ValueReference[] getParameters() {
    return attributes.getParameters();
  }

  public boolean checkPrecisionRange() {
    return attributes.checkPrecisionRange();
  }

  public MajorType getReturnType() {
    return attributes.getReturnValue().getType();
  }

  public ValueReference getReturnValue() {
    return attributes.getReturnValue();
  }

  public WorkspaceReference[] getWorkspaceVars() {
    return attributes.getWorkspaceVars();
  }

  @Override
  public String toString() {
    final int maxLen = 10;
    return this.getClass().getSimpleName()
        + " [functionNames=" + Arrays.toString(attributes.getRegisteredNames())
        + ", returnType=" + Types.toString(attributes.getReturnValue().getType())
        + ", nullHandling=" + attributes.getNullHandling()
        + ", parameters=" + (attributes.getParameters() != null ?
        Arrays.asList(attributes.getParameters()).subList(0, Math.min(attributes.getParameters().length, maxLen)) : null) + "]";
  }
}
