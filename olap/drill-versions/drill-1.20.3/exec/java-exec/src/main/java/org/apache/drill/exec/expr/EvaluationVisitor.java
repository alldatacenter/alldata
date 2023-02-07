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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import io.netty.buffer.DrillBuf;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.AnyValueExpression;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.ConvertExpression;
import org.apache.drill.common.expression.ExpressionStringBuilder;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.IfExpression.IfCondition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.NullExpression;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
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
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.compile.sig.ConstantExpressionIdentifier;
import org.apache.drill.exec.compile.sig.GeneratorMapping;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.fn.AbstractFuncHolder;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.physical.impl.filter.ReturnValueExpression;
import org.apache.drill.exec.vector.ValueHolderHelper;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JLabel;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

/**
 * Visitor that generates code for eval
 */
public class EvaluationVisitor {
  private static final Logger logger = LoggerFactory.getLogger(EvaluationVisitor.class);

  /**
   * Callback function for the expression visitor
   */
  private interface VisitorCallback {
    HoldingContainer getHolder();
  }

  private static class ExpressionHolder {
    private final LogicalExpression expression;
    private final GeneratorMapping mapping;
    private final MappingSet mappingSet;

    ExpressionHolder(LogicalExpression expression, MappingSet mappingSet) {
      this.expression = expression;
      this.mapping = mappingSet.getCurrentMapping();
      this.mappingSet = mappingSet;
    }

    @Override
    public int hashCode() {
      return expression.accept(new HashVisitor(), null);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ExpressionHolder)) {
        return false;
      }
      ExpressionHolder that = (ExpressionHolder) obj;
      return this.mappingSet == that.mappingSet && this.mapping == that.mapping && expression.accept(new EqualityVisitor(), that.expression);
    }
  }

  /**
   * Extended variable descriptor ("holding container") for the variable
   * which references the value holder ("FooHolder") that stores the value
   * from a value vector. Used to pass along the original value vector
   * variable along with the value holder. In particular, this class allows
   * "time travel": to retroactively generate a reader for a union once
   * we realize we need the reader.
   * <p>
   * TODO: This is not especially elegant. But the code that declares the
   * holder/reader does not know about the parameter(s) that will use it,
   * and the normal <code>HoldingContainer</code> can hold only one variable,
   * not a broader context. This version holds more context. Perhaps this
   * idea should be generalized.
   */
  public static class VectorVariableHolder extends HoldingContainer {

    private final ClassGenerator<?> classGen;
    private final JBlock vvSetupBlock;
    private final int insertPosn;
    private final JExpression vectorExpr;
    private final JExpression recordIndex;
    private JVar readerVar;

    public VectorVariableHolder(HoldingContainer base, ClassGenerator<?> classGen,
        JBlock vvSetupBlock, JExpression vectorExpr, JExpression recordIndex) {
      super(base);
      this.classGen = classGen;
      this.vvSetupBlock = vvSetupBlock;
      this.vectorExpr = vectorExpr;
      this.recordIndex = recordIndex;
      insertPosn = vvSetupBlock.pos();
    }

    /**
     * Specialized hack for the UNION type to obtain a <code>FieldReader</code>
     * directly from the value vector, bypassing the <code>UnionHolder</code>
     * created from that vector. Handles setting the reader position only once
     * in an eval block rather than on each reference. There may be multiple
     * functions that need the reader. To ensure we create only one common
     * reader, we "go back in time" to add the reader at the point after
     * we declared the <code>UnionHolder</code>.
     */
    public JVar generateUnionReader() {
      if (readerVar == null) {
        createReaderVar();
      }
      return readerVar;
    }

    private void createReaderVar() {
      // Rewind to where we added the UnionHolder
      int curPosn = vvSetupBlock.pos();
      vvSetupBlock.pos(insertPosn);

      // UnionReader readerx = vvy.getReader();
      JType readerClass = classGen.getModel()._ref(FieldReader.class);
      JExpression expr = vectorExpr.invoke("getReader");
      readerVar = vvSetupBlock.decl(readerClass, classGen.getNextVar("reader"), expr);

      // readerx.setPosition(indexExpr);
      JInvocation setPosnStmt = readerVar.invoke("setPosition").arg(recordIndex);
      vvSetupBlock.add(setPosnStmt);

      // Put the insert position back to where it was; adjusting
      // for the statements just added.
      int offset = vvSetupBlock.pos() - insertPosn;
      vvSetupBlock.pos(curPosn + offset);
    }
  }

  public Map<ExpressionHolder, HoldingContainer> previousExpressions = new HashMap<>();
  private final Stack<Map<ExpressionHolder, HoldingContainer>> mapStack = new Stack<>();

  public HoldingContainer addExpr(LogicalExpression e, ClassGenerator<?> generator) {
    Set<LogicalExpression> constantBoundaries;
    if (generator.getMappingSet().hasEmbeddedConstant()) {
      constantBoundaries = Collections.emptySet();
    } else {
      constantBoundaries = ConstantExpressionIdentifier.getConstantExpressionSet(e);
    }
    return e.accept(new CSEFilter(constantBoundaries), generator);
  }

  void newScope() {
    mapStack.push(previousExpressions);
    previousExpressions = Maps.newHashMap();
  }

  void leaveScope() {
    previousExpressions.clear();
    previousExpressions = mapStack.pop();
  }

  /**
   * Get a HoldingContainer for the expression if it had been already evaluated
   */
  private HoldingContainer getPrevious(LogicalExpression expression, MappingSet mappingSet) {
    ExpressionHolder holder = new ExpressionHolder(expression, mappingSet);
    HoldingContainer previous = null;
    for (Map<ExpressionHolder,HoldingContainer> m : mapStack) {
      previous = m.get(holder);
      if (previous != null) {
        break;
      }
    }
    if (previous == null) {
      previous = previousExpressions.get(holder);
    }
    if (previous != null) {
      logger.debug("Found previously evaluated expression: {}", ExpressionStringBuilder.toString(expression));
    }
    return previous;
  }

  private void put(LogicalExpression expression, HoldingContainer hc, MappingSet mappingSet) {
    previousExpressions.put(new ExpressionHolder(expression, mappingSet), hc);
  }

  private static class EvalVisitor extends AbstractExprVisitor<HoldingContainer, ClassGenerator<?>, RuntimeException> {

    @Override
    public HoldingContainer visitFunctionCall(FunctionCall call, ClassGenerator<?> generator) throws RuntimeException {
      throw new UnsupportedOperationException("FunctionCall is not expected here. "
          + "It should have been converted to FunctionHolderExpression in materialization");
    }

    @Override
    public HoldingContainer visitBooleanOperator(BooleanOperator op,
        ClassGenerator<?> generator) throws RuntimeException {
      if (op.getName().equals(FunctionNames.AND)) {
        return visitBooleanAnd(op, generator);
      } else if(op.getName().equals(FunctionNames.OR)) {
        return visitBooleanOr(op, generator);
      } else {
        throw new UnsupportedOperationException(
            "BooleanOperator can only be booleanAnd, booleanOr. You are using " + op.getName());
      }
    }

    /**
     * Generate a function call (which actually in-lines the function within
     * the generated code.)
     */
    @Override
    public HoldingContainer visitFunctionHolderExpression(FunctionHolderExpression holderExpr,
        ClassGenerator<?> generator) throws RuntimeException {

      // Reference to the function definition.
      AbstractFuncHolder fnHolder = (AbstractFuncHolder) holderExpr.getHolder();

      // Define (internal) workspace variables
      JVar[] workspaceVars = fnHolder.renderStart(generator, null, holderExpr.getFieldReference());

      if (fnHolder.isNested()) {
        generator.getMappingSet().enterChild();
      }

      // Create (or obtain) value holders for each of the actual function
      // arguments. In many cases (all? some?), the function reference
      // (holder expr) represents arguments as value holders.
      HoldingContainer[] args = new HoldingContainer[holderExpr.args.size()];
      for (int i = 0; i < holderExpr.args.size(); i++) {
        args[i] = holderExpr.args.get(i).accept(this, generator);
      }

      // Aggregate functions generate the per-value code here.
      fnHolder.renderMiddle(generator, args, workspaceVars);

      if (fnHolder.isNested()) {
        generator.getMappingSet().exitChild();
      }

      // Simple functions generate per-value code here.
      // Aggregate functions generate the per-group aggregate here.
      return fnHolder.renderEnd(generator, args, workspaceVars, holderExpr);
    }

    @Override
    public HoldingContainer visitIfExpression(IfExpression ifExpr, ClassGenerator<?> generator) throws RuntimeException {
      JBlock local = generator.getEvalBlock();

      HoldingContainer output = generator.declare(ifExpr.getMajorType());

      JBlock conditionalBlock = new JBlock(false, false);
      IfCondition c = ifExpr.ifCondition;

      HoldingContainer holdingContainer = c.condition.accept(this, generator);
      JConditional jc;
      if (holdingContainer.isOptional()) {
        jc = conditionalBlock._if(holdingContainer.getIsSet().eq(JExpr.lit(1)).cand(holdingContainer.getValue().eq(JExpr.lit(1))));
      } else {
        jc = conditionalBlock._if(holdingContainer.getValue().eq(JExpr.lit(1)));
      }

      generator.nestEvalBlock(jc._then());

      HoldingContainer thenExpr = c.expression.accept(this, generator);

      generator.unNestEvalBlock();

      List<String> holderFields = ValueHolderHelper.getHolderParams(output.getMajorType());
      if (thenExpr.isOptional()) {
        JConditional newCond = jc._then()._if(thenExpr.getIsSet().ne(JExpr.lit(0)));
        JBlock b = newCond._then();
        for (String holderField : holderFields) {
          b.assign(output.f(holderField), thenExpr.f(holderField));
        }
      } else {
        for (String holderField : holderFields) {
          jc._then().assign(output.f(holderField), thenExpr.f(holderField));
        }
      }

      generator.nestEvalBlock(jc._else());

      HoldingContainer elseExpr = ifExpr.elseExpression.accept(this, generator);

      generator.unNestEvalBlock();

      if (elseExpr.isOptional()) {
        JConditional newCond = jc._else()._if(elseExpr.getIsSet().ne(JExpr.lit(0)));
        JBlock b = newCond._then();
        for (String holderField : holderFields) {
          b.assign(output.f(holderField), elseExpr.f(holderField));
        }
      } else {
        for (String holderField : holderFields) {
          jc._else().assign(output.f(holderField), elseExpr.f(holderField));
        }
      }
      local.add(conditionalBlock);
      return output;
    }

    @Override
    public HoldingContainer visitSchemaPath(SchemaPath path, ClassGenerator<?> generator) throws RuntimeException {
      throw new UnsupportedOperationException("All schema paths should have been replaced with ValueVectorExpressions.");
    }

    private HoldingContainer getHoldingContainer(ClassGenerator<?> generator,
                                                 MajorType majorType,
                                                 Function<DrillBuf, ? extends ValueHolder> function) {
      JType holderType = generator.getHolderType(majorType);
      Pair<Integer, JVar> depthVar = generator.declareClassConstField("const", holderType, function);
      JFieldRef outputSet = null;
      JVar var = depthVar.getValue();
      if (majorType.getMode() == TypeProtos.DataMode.OPTIONAL) {
        outputSet = var.ref("isSet");
      }
      return new HoldingContainer(majorType, var, var.ref("value"), outputSet);
    }

    @Override
    public HoldingContainer visitLongConstant(LongExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getBigIntHolder(e.getLong()));
    }

    @Override
    public HoldingContainer visitIntConstant(IntExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getIntHolder(e.getInt()));
    }

    @Override
    public HoldingContainer visitDateConstant(DateExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getDateHolder(e.getDate()));
    }

    @Override
    public HoldingContainer visitTimeConstant(TimeExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getTimeHolder(e.getTime()));
    }

    @Override
    public HoldingContainer visitIntervalYearConstant(IntervalYearExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getIntervalYearHolder(e.getIntervalYear()));
    }

    @Override
    public HoldingContainer visitTimeStampConstant(TimeStampExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getTimeStampHolder(e.getTimeStamp()));
    }

    @Override
    public HoldingContainer visitFloatConstant(FloatExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getFloat4Holder(e.getFloat()));
    }

    @Override
    public HoldingContainer visitDoubleConstant(DoubleExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getFloat8Holder(e.getDouble()));
    }

    @Override
    public HoldingContainer visitBooleanConstant(BooleanExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getBitHolder(e.getBoolean() ? 1 : 0));
    }

    /**
     * Handles implementation-specific expressions not known to the visitor
     * mechanism.
     */
    // TODO: Consider adding these "unknown" expressions to the visitor class.
    @Override
    public HoldingContainer visitUnknown(LogicalExpression e, ClassGenerator<?> generator) throws RuntimeException {
      if (e instanceof ValueVectorReadExpression) {
        return visitValueVectorReadExpression((ValueVectorReadExpression) e, generator);
      } else if (e instanceof ValueVectorWriteExpression) {
        return visitValueVectorWriteExpression((ValueVectorWriteExpression) e, generator);
      } else if (e instanceof ReturnValueExpression) {
        return visitReturnValueExpression((ReturnValueExpression) e, generator);
      } else if (e instanceof HoldingContainerExpression) {
        // Expression contains a "holder", just return it.
        return ((HoldingContainerExpression) e).getContainer();
      } else if (e instanceof NullExpression) {
        return generator.declare(e.getMajorType());
      } else if (e instanceof TypedNullConstant) {
        return generator.declare(e.getMajorType());
      } else {
        return super.visitUnknown(e, generator);
      }
    }

    /**
     * <p>
     * Creates local variable based on given parameter type and name and assigns
     * parameter to this local instance.
     * </p>
     *
     * <p>
     * Example: <br/>
     * IntHolder seedValue0 = new IntHolder();<br/>
     * seedValue0 .value = seedValue;
     * </p>
     *
     * @param e parameter expression
     * @param generator class generator
     * @return holder instance
     */
    @Override
    public HoldingContainer visitParameter(ValueExpressions.ParameterExpression e, ClassGenerator<?> generator) {
      HoldingContainer out = generator.declare(e.getMajorType(), e.getName(), true);
      generator.getEvalBlock().assign(out.getValue(), JExpr.ref(e.getName()));
      return out;
    }

    private HoldingContainer visitValueVectorWriteExpression(ValueVectorWriteExpression e, ClassGenerator<?> generator) {

      final LogicalExpression child = e.getChild();
      final HoldingContainer inputContainer = child.accept(this, generator);

      JBlock block = generator.getEvalBlock();
      JExpression outIndex = generator.getMappingSet().getValueWriteIndex();
      JVar vv = generator.declareVectorValueSetupAndMember(generator.getMappingSet().getOutgoing(), e.getFieldId());

      // Only when the input is a reader, use writer interface to copy value.
      // Otherwise, input is a holder and we use vv mutator to set value.
      if (inputContainer.isReader()) {
        JType writerImpl = generator.getModel()._ref(
            TypeHelper.getWriterImpl(inputContainer.getMinorType(), inputContainer.getMajorType().getMode()));
        JType writerIFace = generator.getModel()._ref(
            TypeHelper.getWriterInterface(inputContainer.getMinorType(), inputContainer.getMajorType().getMode()));
        JVar writer = generator.declareClassField("writer", writerIFace);
        generator.getSetupBlock().assign(writer, JExpr._new(writerImpl).arg(vv).arg(JExpr._null()));
        generator.getEvalBlock().add(writer.invoke("setPosition").arg(outIndex));
        String copyMethod = inputContainer.isSingularRepeated() ? "copyAsValueSingle" : "copyAsValue";
        generator.getEvalBlock().add(inputContainer.getHolder().invoke(copyMethod).arg(writer));
        if (e.isSafe()) {
          HoldingContainer outputContainer = generator.declare(Types.REQUIRED_BIT);
          generator.getEvalBlock().assign(outputContainer.getValue(), JExpr.lit(1));
          return outputContainer;
        }
      } else {
        final JInvocation setMeth = GetSetVectorHelper.write(e.getChild().getMajorType(), vv, inputContainer, outIndex, e.isSafe() ? "setSafe" : "set");
          if (inputContainer.isOptional()) {
            JConditional jc = block._if(inputContainer.getIsSet().eq(JExpr.lit(0)).not());
            block = jc._then();
          }
          block.add(setMeth);
      }
      return null;
    }

    /**
     * Generate code to extract a value from a value vector into a local
     * variable which can be passed to a function. The local variable is either
     * a value holder (<code>FooHolder</code>) or a reader. In most cases, the
     * decision is made based on the type of the vector. Primitives use value
     * holders, complex variables use readers.
     * <p>
     * Code in {@link DrillFuncHolder#declareInputVariable} converts the local
     * variable into the form requested by the </code>{@literal @}Param</code>
     * annotation which itself can be a value holder or a reader.
     * <p>
     * A special case occurs for the UNION type. Drill supports conversion from
     * a reader to a holder, but not the other way around. We prefer to use a
     * holder. But, if the function wants a reader, we must generate a reader.
     * If some other function wants a holder, we can convert from the reader to
     * a holder.
     */
    private HoldingContainer visitValueVectorReadExpression(ValueVectorReadExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      // declare value vector
      DirectExpression batchName;
      JExpression batchIndex;
      JExpression recordIndex;

      // If value vector read expression has batch reference, use its values in
      // generated code, otherwise use values provided by mapping set (which
      // point to only one batch) primarily used for non-equi joins where
      // expression conditions may refer to more than one batch
      BatchReference batchRef = e.getBatchRef();
      if (batchRef != null) {
        batchName = DirectExpression.direct(batchRef.getBatchName());
        batchIndex = DirectExpression.direct(batchRef.getBatchIndex());
        recordIndex = DirectExpression.direct(batchRef.getRecordIndex());
      } else {
        batchName = generator.getMappingSet().getIncoming();
        batchIndex = generator.getMappingSet().getValueReadIndex();
        recordIndex = batchIndex;
      }

      JExpression vv1 = generator.declareVectorValueSetupAndMember(batchName, e.getFieldId());
      JExpression componentVariable = batchIndex.shrz(JExpr.lit(16));
      if (e.isSuperReader()) {
        vv1 = (vv1.component(componentVariable));
        recordIndex = recordIndex.band(JExpr.lit((int) Character.MAX_VALUE));
      }

      // evaluation work.
      MajorType type = e.getMajorType();
      final boolean hasReadPath = e.hasReadPath();
      final boolean complex = Types.isComplex(type);

      if (!hasReadPath && !complex) {
        return createHolderForVector(generator, recordIndex, vv1, type);
      } else {
        return createReaderForVector(e, generator, recordIndex, vv1, type);
      }
    }

    private HoldingContainer createHolderForVector(ClassGenerator<?> generator,
        JExpression recordIndex, JExpression vv1, MajorType type) {
      JBlock eval = new JBlock();
      HoldingContainer out = generator.declare(type);
      if (Types.isRepeated(type)) {
        JExpression expr = vv1.invoke("getReader");
        // Set correct position to the reader
        eval.add(expr.invoke("reset"));
        eval.add(expr.invoke("setPosition").arg(recordIndex));
      }

      // Generate code to read the vector into a holder
      GetSetVectorHelper.read(type, vv1, eval, out, generator.getModel(), recordIndex);
      JBlock evalBlock = generator.getEvalBlock();
      evalBlock.add(eval);
      return new VectorVariableHolder(out, generator, evalBlock, vv1, recordIndex);
    }

    private HoldingContainer createReaderForVector(ValueVectorReadExpression e,
        ClassGenerator<?> generator, JExpression recordIndex, JExpression vv1,
        MajorType type) {

      HoldingContainer out = generator.declare(type);

      final boolean repeated = Types.isRepeated(type);
      final boolean complex = Types.isComplex(type);
      final boolean listVector = e.getTypedFieldId().isListVector();
      final boolean isUnion = Types.isUnion(type);

      // Create a reader for the vector.

      JExpression expr = vv1.invoke("getReader");
      PathSegment seg = e.getReadPath();

      JVar isNull = null;
      boolean isNullReaderLikely = isNullReaderLikely(seg, complex || repeated || listVector || isUnion);
      if (isNullReaderLikely) {
        isNull = generator.getEvalBlock().decl(generator.getModel().INT, generator.getNextVar("isNull"), JExpr.lit(0));
      }

      JLabel label = generator.getEvalBlock().label("complex");
      JBlock eval = generator.getEvalBlock().block();

      // Position to the correct value.
      eval.add(expr.invoke("reset"));
      eval.add(expr.invoke("setPosition").arg(recordIndex));
      int listNum = 0;

      // Variable to store index of key in dict (if there is one)
      // if entry for the key is not found, will be assigned a value of SingleDictReaderImpl#NOT_FOUND.
      JVar valueIndex = eval.decl(generator.getModel().INT, "valueIndex", JExpr.lit(-2));

      int depth = 0;

      while (seg != null) {
        if (seg.isArray()) {

          // stop once we get to the last segment and the final type is
          // neither complex nor repeated (map, dict, list, repeated list).
          // In case of non-complex and non-repeated type, we return Holder,
          // in stead of FieldReader.
          if (seg.isLastPath() && !complex && !repeated && !listVector) {
            break;
          }

          if (e.getFieldId().isDict(depth)) {
            expr = getDictReaderReadByKeyExpression(generator, eval, expr, seg, valueIndex, isNull);
            seg = seg.getChild();
            depth++;
            continue;
          }

          JVar list = generator.declareClassField("list", generator.getModel()._ref(FieldReader.class));
          eval.assign(list, expr);

          // If this is an array, set a single position for the expression to
          // allow us to read the right data lower down.
          JVar desiredIndex = eval.decl(generator.getModel().INT, "desiredIndex" + listNum,
              JExpr.lit(seg.getArraySegment().getIndex()));
          // start with negative one so that we are at zero after first call
          // to next.
          JVar currentIndex = eval.decl(generator.getModel().INT, "currentIndex" + listNum, JExpr.lit(-1));

          eval._while(
              currentIndex.lt(desiredIndex)
                  .cand(list.invoke("next"))).body().assign(currentIndex, currentIndex.plus(JExpr.lit(1)));

          JBlock ifNoVal = eval._if(desiredIndex.ne(currentIndex))._then().block();
          if (out.isOptional()) {
            ifNoVal.assign(out.getIsSet(), JExpr.lit(0));
          }
          ifNoVal.assign(isNull, JExpr.lit(1));
          ifNoVal._break(label);

          expr = list.invoke("reader");
          listNum++;
        } else {

          if (e.getFieldId().isDict(depth)) {
            MajorType finalType = e.getFieldId().getFinalType();
            if (seg.getChild() == null && !(Types.isComplex(finalType) || Types.isRepeated(finalType))) {
              // This is the last segment:
              eval.add(expr.invoke("read").arg(getKeyExpression(seg, generator)).arg(out.getHolder()));
              return out;
            }

            expr = getDictReaderReadByKeyExpression(generator, eval, expr, seg, valueIndex, isNull);
            seg = seg.getChild();
            depth++;
            continue;
          }

          JExpression fieldName = JExpr.lit(seg.getNameSegment().getPath());
          expr = expr.invoke("reader").arg(fieldName);
        }
        seg = seg.getChild();
        depth++;
      }

      // expected that after loop depth at least equal to last id index
      depth = Math.max(depth, e.getFieldId().getFieldIds().length - 1);

      if (complex || repeated) {

        // Declare the reader for this vector
        JVar complexReader = generator.declareClassField("reader", generator.getModel()._ref(FieldReader.class));

        if (isNullReaderLikely) {
          JConditional jc = generator.getEvalBlock()._if(isNull.eq(JExpr.lit(0)));

          JClass nrClass = generator.getModel().ref(org.apache.drill.exec.vector.complex.impl.NullReader.class);
          JExpression nullReader;
          if (complex) {
            nullReader = nrClass.staticRef("EMPTY_MAP_INSTANCE");
          } else {
            nullReader = nrClass.staticRef("EMPTY_LIST_INSTANCE");
          }

          jc._then().assign(complexReader, expr);
          jc._else().assign(complexReader, nullReader);
        } else {
          eval.assign(complexReader, expr);
        }

        HoldingContainer hc = new HoldingContainer(type, complexReader, null, null, false, true);
        return hc;
      } else {

        // For a DICT, create a holder, then obtain the reader.

        if (seg != null) {
          JExpression holderExpr = out.getHolder();
          JExpression argExpr;
          if (e.getFieldId().isDict(depth)) {
            holderExpr = JExpr.cast(generator.getModel()._ref(ValueHolder.class), holderExpr);
            argExpr = getKeyExpression(seg, generator);
          } else {
            argExpr = JExpr.lit(seg.getArraySegment().getIndex());
          }
          JClass dictReaderClass = generator.getModel().ref(org.apache.drill.exec.vector.complex.impl.SingleDictReaderImpl.class);
          JConditional jc = eval._if(valueIndex.ne(dictReaderClass.staticRef("NOT_FOUND")));
          jc._then().add(expr.invoke("read").arg(argExpr).arg(holderExpr));
        } else {
          eval.add(expr.invoke("read").arg(out.getHolder()));
        }
      }
      return out;
    }

    /*  Check if a Path expression could produce a NullReader. A path expression will produce a null reader, when:
     *   1) It contains an array segment as non-leaf segment :  a.b[2].c.  segment [2] might produce null reader.
     *   2) It contains an array segment as leaf segment, AND the final output is complex or repeated : a.b[2], when
     *     the final type of this expression is a map, or repeated list, or repeated map.
     */
    private boolean isNullReaderLikely(PathSegment seg, boolean complexOrRepeated) {
      while (seg != null) {
        if (seg.isArray() && !seg.isLastPath()) {
          return true;
        }

        if (seg.isLastPath() && complexOrRepeated) {
          return true;
        }

        seg = seg.getChild();
      }
      return false;
    }

    /**
     * Adds code to {@code eval} block which reads values by key from {@code expr} which is an instance of
     * {@link org.apache.drill.exec.vector.complex.reader.BaseReader.DictReader}.
     *
     *
     * @param generator current class generator
     * @param eval evaluation block the code will be added to
     * @param expr DICT reader to read values from
     * @param segment segment containing original key value
     * @param valueIndex current value index (will be reassigned in the method)
     * @param isNull variable to indicate whether entry with the key exists in the DICT.
     *               Will be set to {@literal 1} if the key is not present
     * @return expression corresponding to {@link org.apache.drill.exec.vector.complex.DictVector#FIELD_VALUE_NAME}'s
     *         reader with its position set to index corresponding to the key
     */
    private JExpression getDictReaderReadByKeyExpression(ClassGenerator<?> generator, JBlock eval, JExpression expr,
                                                         PathSegment segment, JVar valueIndex, JVar isNull) {
      JVar dictReader = generator.declareClassField("dictReader", generator.getModel()._ref(FieldReader.class));
      eval.assign(dictReader, expr);

      JExpression keyExpr = getKeyExpression(segment, generator);

      eval.assign(valueIndex, expr.invoke("find").arg(keyExpr));

      JConditional conditional = eval._if(valueIndex.gt(JExpr.lit(-1)));
      JBlock ifFound = conditional._then().block();
      expr = dictReader.invoke("reader").arg(JExpr.lit("value"));
      ifFound.add(expr.invoke("setPosition").arg(valueIndex));

      JBlock elseBlock = conditional._else().block();

      JClass nrClass = generator.getModel().ref(org.apache.drill.exec.vector.complex.impl.NullReader.class);
      JExpression nullReader = nrClass.staticRef("EMPTY_MAP_INSTANCE");

      elseBlock.assign(dictReader, nullReader);
      if (isNull != null) {
        elseBlock.assign(isNull, JExpr.lit(1));
      }

      return expr;
    }

    /**
     * Transforms a segment to appropriate Java Object representation of key ({@link org.apache.drill.exec.vector.complex.DictVector#FIELD_KEY_NAME})
     * which is used when retrieving values from dict with key. In case if key vector's Java equivalent is primitive,
     * i.e. {@code boolean}, {@code int}, {@code double} etc., then primitive type is used.
     * Otherwise, an {@code Object} instance is created (i.e, {@code BigDecimal} for {@link org.apache.drill.common.types.TypeProtos.MinorType#VARDECIMAL},
     * {@code LocalDateTime} for {@link org.apache.drill.common.types.TypeProtos.MinorType#TIMESTAMP} etc.).
     *
     * @param segment a path segment providing the value
     * @param generator current class generator
     * @return Java Object representation of key wrapped into {@link JVar}
     */
    private JExpression getKeyExpression(PathSegment segment, ClassGenerator<?> generator) {
      MajorType valueType = segment.getOriginalValueType();
      JType keyType;
      JExpression newKeyObject;
      JVar dictKey;
      if (segment.isArray()) {
        if (valueType == null) {
          return JExpr.lit(segment.getArraySegment().getIndex());
        }
        switch(valueType.getMinorType()) {
          case INT:
            return JExpr.cast(generator.getModel().ref(Object.class), JExpr.lit(segment.getArraySegment().getIndex()));
          case SMALLINT:
            return JExpr.lit((short) segment.getOriginalValue());
          case TINYINT:
            return JExpr.lit((byte) segment.getOriginalValue());
          default:
            throw new IllegalArgumentException("ArraySegment!");
        }
      } else { // named
        if (valueType == null) {
          return JExpr.lit(segment.getNameSegment().getPath());
        }
        switch (valueType.getMinorType()) {
          case VARCHAR:
            String vcValue = (String) segment.getOriginalValue();
            keyType = generator.getModel()._ref(org.apache.drill.exec.util.Text.class);
            newKeyObject = JExpr._new(keyType).arg(vcValue);
            dictKey = generator.declareClassField("dictKey", keyType);
            generator.getSetupBlock().assign(dictKey, newKeyObject);
            return dictKey;
          case VARDECIMAL:
            BigDecimal bdValue = (BigDecimal) segment.getOriginalValue();
            keyType = generator.getModel()._ref(java.math.BigDecimal.class);

            JClass rmClass = generator.getModel().ref(java.math.RoundingMode.class);
            newKeyObject = JExpr._new(keyType).arg(JExpr.lit(bdValue.doubleValue())).invoke("setScale")
                .arg(JExpr.lit(bdValue.scale()))
                .arg(rmClass.staticRef("HALF_UP"));
            dictKey = generator.declareClassField("dictKey", keyType);
            generator.getSetupBlock().assign(dictKey, newKeyObject);
            return dictKey;
          case BIGINT:
            return JExpr.lit((long) segment.getOriginalValue());
          case FLOAT4:
            return JExpr.lit((float) segment.getOriginalValue());
          case FLOAT8:
            return JExpr.lit((double) segment.getOriginalValue());
          case BIT:
            return JExpr.lit((boolean) segment.getOriginalValue());
          case TIMESTAMP:
            return getDateTimeKey(segment, generator, LocalDateTime.class, "parseBest");
          case DATE:
            return getDateTimeKey(segment, generator, LocalDate.class, "parseLocalDate");
          case TIME:
            return getDateTimeKey(segment, generator, LocalTime.class, "parseLocalTime");
          default:
            throw new IllegalArgumentException("NamedSegment!");
        }
      }
    }

    private JVar getDateTimeKey(PathSegment segment, ClassGenerator<?> generator, Class<?> javaClass, String methodName) {
      String strValue = (String) segment.getOriginalValue();

      JClass dateUtilityClass = generator.getModel().ref(org.apache.drill.exec.expr.fn.impl.DateUtility.class);
      JExpression newKeyObject = dateUtilityClass.staticInvoke(methodName).arg(JExpr.lit(strValue));
      JType keyType = generator.getModel()._ref(javaClass);
      JVar dictKey = generator.declareClassField("dictKey", keyType);
      generator.getSetupBlock().assign(dictKey, newKeyObject);
      return dictKey;
    }

    private HoldingContainer visitReturnValueExpression(ReturnValueExpression e, ClassGenerator<?> generator) {
      LogicalExpression child = e.getChild();
      // Preconditions.checkArgument(child.getMajorType().equals(Types.REQUIRED_BOOLEAN));
      HoldingContainer hc = child.accept(this, generator);
      if (e.isReturnTrueOnOne()) {
        generator.getEvalBlock()._return(hc.getValue().eq(JExpr.lit(1)));
      } else {
        generator.getEvalBlock()._return(hc.getValue());
      }

      return null;
    }

    @Override
    public HoldingContainer visitQuotedStringConstant(QuotedString e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getVarCharHolder(buffer, e.getString()));
    }

    @Override
    public HoldingContainer visitIntervalDayConstant(IntervalDayExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getIntervalDayHolder(e.getIntervalDay(), e.getIntervalMillis()));
    }

    @Override
    public HoldingContainer visitDecimal9Constant(Decimal9Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getDecimal9Holder(e.getIntFromDecimal(), e.getPrecision(), e.getScale()));
    }

    @Override
    public HoldingContainer visitDecimal18Constant(Decimal18Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getDecimal18Holder(e.getLongFromDecimal(), e.getPrecision(), e.getScale()));
    }

    @Override
    public HoldingContainer visitDecimal28Constant(Decimal28Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getDecimal28Holder(buffer, e.getBigDecimal()));
    }

    @Override
    public HoldingContainer visitDecimal38Constant(Decimal38Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getDecimal38Holder(buffer, e.getBigDecimal()));
    }

    @Override
    public HoldingContainer visitVarDecimalConstant(VarDecimalExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return getHoldingContainer(
        generator,
        e.getMajorType(),
        buffer -> ValueHolderHelper.getVarDecimalHolder(buffer, e.getBigDecimal()));
    }

    @Override
    public HoldingContainer visitCastExpression(CastExpression e, ClassGenerator<?> value) throws RuntimeException {
      throw new UnsupportedOperationException("CastExpression is not expected here. "
          + "It should have been converted to FunctionHolderExpression in materialization");
    }

    @Override
    public HoldingContainer visitConvertExpression(ConvertExpression e, ClassGenerator<?> value)
        throws RuntimeException {
      String convertFunctionName = e.getConvertFunction() + e.getEncodingType();

      List<LogicalExpression> newArgs = new ArrayList<>();
      newArgs.add(e.getInput()); // input_expr

      FunctionCall fc = new FunctionCall(convertFunctionName, newArgs, e.getPosition());
      return fc.accept(this, value);
    }

    @Override
    public HoldingContainer visitAnyValueExpression(AnyValueExpression e, ClassGenerator<?> value)
        throws RuntimeException {

      List<LogicalExpression> newArgs = new ArrayList<>();
      newArgs.add(e.getInput()); // input_expr

      FunctionCall fc = new FunctionCall(AnyValueExpression.ANY_VALUE, newArgs, e.getPosition());
      return fc.accept(this, value);
    }

    private HoldingContainer visitBooleanAnd(BooleanOperator op,
        ClassGenerator<?> generator) {

      HoldingContainer out = generator.declare(op.getMajorType());

      JLabel label = generator.getEvalBlockLabel("AndOP");
      JBlock eval = generator.createInnerEvalBlock();
      generator.nestEvalBlock(eval);  // enter into nested block

      HoldingContainer arg = null;

      JExpression e = null;

      //  value of boolean "and" when one side is null
      //    p       q     p and q
      //    true    null     null
      //    false   null     false
      //    null    true     null
      //    null    false    false
      //    null    null     null
      for (LogicalExpression expr : op.args()) {
        arg = expr.accept(this, generator);

        JBlock earlyExit = null;
        if (arg.isOptional()) {
          JExpression expr1 = arg.getIsSet().eq(JExpr.lit(1));
          // UntypedNullHolder does not have `value` field
          if (arg.getMinorType() != TypeProtos.MinorType.NULL) {
            expr1 = expr1.cand(arg.getValue().ne(JExpr.lit(1)));
          }
          earlyExit = eval._if(expr1)._then();
          if (e == null) {
            e = arg.getIsSet();
          } else {
            e = e.mul(arg.getIsSet());
          }
        } else {
          earlyExit = eval._if(arg.getValue().ne(JExpr.lit(1)))._then();
        }

        if (out.isOptional()) {
          earlyExit.assign(out.getIsSet(), JExpr.lit(1));
        }

        earlyExit.assign(out.getValue(),  JExpr.lit(0));
        earlyExit._break(label);
      }

      if (out.isOptional()) {
        assert (e != null);

        JConditional notSetJC = eval._if(e.eq(JExpr.lit(0)));
        notSetJC._then().assign(out.getIsSet(), JExpr.lit(0));

        JBlock setBlock = notSetJC._else().block();
        setBlock.assign(out.getIsSet(), JExpr.lit(1));
        setBlock.assign(out.getValue(), JExpr.lit(1));
      } else {
        assert (e == null);
        eval.assign(out.getValue(), JExpr.lit(1));
      }

      generator.unNestEvalBlock();     // exit from nested block

      return out;
    }

    private HoldingContainer visitBooleanOr(BooleanOperator op,
        ClassGenerator<?> generator) {

      HoldingContainer out = generator.declare(op.getMajorType());

      JLabel label = generator.getEvalBlockLabel("OrOP");
      JBlock eval = generator.createInnerEvalBlock();
      generator.nestEvalBlock(eval);   // enter into nested block.

      HoldingContainer arg = null;

      JExpression e = null;

      //  value of boolean "or" when one side is null
      //    p       q       p and q
      //    true    null     true
      //    false   null     null
      //    null    true     true
      //    null    false    null
      //    null    null     null
      for (LogicalExpression expr : op.args()) {
        arg = expr.accept(this, generator);

        JBlock earlyExit = null;
        if (arg.isOptional()) {
          earlyExit = eval._if(arg.getIsSet().eq(JExpr.lit(1)).cand(arg.getValue().eq(JExpr.lit(1))))._then();
          if (e == null) {
            e = arg.getIsSet();
          } else {
            e = e.mul(arg.getIsSet());
          }
        } else {
          earlyExit = eval._if(arg.getValue().eq(JExpr.lit(1)))._then();
        }

        if (out.isOptional()) {
          earlyExit.assign(out.getIsSet(), JExpr.lit(1));
        }

        earlyExit.assign(out.getValue(),  JExpr.lit(1));
        earlyExit._break(label);
      }

      if (out.isOptional()) {
        assert (e != null);

        JConditional notSetJC = eval._if(e.eq(JExpr.lit(0)));
        notSetJC._then().assign(out.getIsSet(), JExpr.lit(0));

        JBlock setBlock = notSetJC._else().block();
        setBlock.assign(out.getIsSet(), JExpr.lit(1));
        setBlock.assign(out.getValue(), JExpr.lit(0));
      } else {
        assert (e == null);
        eval.assign(out.getValue(), JExpr.lit(0));
      }

      generator.unNestEvalBlock();   // exit from nested block.

      return out;
    }
  }

  /**
   * Creates a representation of the "Holder" for a constant
   * value. For optimization, constant holders are cached. This
   * visitor retrieves an existing cached holder, if available, else
   * creates a new one.
   */
  private class CSEFilter extends ConstantFilter {

    public CSEFilter(Set<LogicalExpression> constantBoundaries) {
      super(constantBoundaries);
    }

    @Override
    public HoldingContainer visitFunctionCall(FunctionCall call, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(call, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitFunctionCall(call, generator);
        put(call, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitFunctionHolderExpression(FunctionHolderExpression holder, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(holder, generator.getMappingSet());
      if (hc == null || holder.isRandom()) {
        hc = super.visitFunctionHolderExpression(holder, generator);
        put(holder, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitIfExpression(IfExpression ifExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(ifExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitIfExpression(ifExpr, generator);
        put(ifExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitBooleanOperator(BooleanOperator call, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(call, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitBooleanOperator(call, generator);
        put(call, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitSchemaPath(SchemaPath path, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(path, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitSchemaPath(path, generator);
        put(path, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitIntConstant(IntExpression intExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(intExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitIntConstant(intExpr, generator);
        put(intExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitFloatConstant(FloatExpression fExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(fExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitFloatConstant(fExpr, generator);
        put(fExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitLongConstant(LongExpression longExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(longExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitLongConstant(longExpr, generator);
        put(longExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitDateConstant(DateExpression dateExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(dateExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitDateConstant(dateExpr, generator);
        put(dateExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitTimeConstant(TimeExpression timeExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(timeExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitTimeConstant(timeExpr, generator);
        put(timeExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitTimeStampConstant(TimeStampExpression timeStampExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(timeStampExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitTimeStampConstant(timeStampExpr, generator);
        put(timeStampExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitIntervalYearConstant(IntervalYearExpression intervalYearExpression, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(intervalYearExpression, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitIntervalYearConstant(intervalYearExpression, generator);
        put(intervalYearExpression, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitIntervalDayConstant(IntervalDayExpression intervalDayExpression, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(intervalDayExpression, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitIntervalDayConstant(intervalDayExpression, generator);
        put(intervalDayExpression, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitDecimal9Constant(Decimal9Expression decExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(decExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitDecimal9Constant(decExpr, generator);
        put(decExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitDecimal18Constant(Decimal18Expression decExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(decExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitDecimal18Constant(decExpr, generator);
        put(decExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitDecimal28Constant(Decimal28Expression decExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(decExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitDecimal28Constant(decExpr, generator);
        put(decExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitDecimal38Constant(Decimal38Expression decExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(decExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitDecimal38Constant(decExpr, generator);
        put(decExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitVarDecimalConstant(VarDecimalExpression decExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(decExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitVarDecimalConstant(decExpr, generator);
        put(decExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitDoubleConstant(DoubleExpression dExpr, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(dExpr, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitDoubleConstant(dExpr, generator);
        put(dExpr, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitBooleanConstant(BooleanExpression e, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(e, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitBooleanConstant(e, generator);
        put(e, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitQuotedStringConstant(QuotedString e, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(e, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitQuotedStringConstant(e, generator);
        put(e, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitNullConstant(TypedNullConstant e, ClassGenerator<?> generator) throws RuntimeException {
      return super.visitNullConstant(e, generator);
    }

    @Override
    public HoldingContainer visitNullExpression(NullExpression e, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(e, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitNullExpression(e, generator);
        put(e, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitUnknown(LogicalExpression e, ClassGenerator<?> generator) throws RuntimeException {
      if (e instanceof ValueVectorReadExpression) {
        HoldingContainer hc = getPrevious(e, generator.getMappingSet());
        if (hc == null) {
          hc = super.visitUnknown(e, generator);
          put(e, hc, generator.getMappingSet());
        }
        return hc;
      }
      return super.visitUnknown(e, generator);
    }

    @Override
    public HoldingContainer visitCastExpression(CastExpression e, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(e, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitCastExpression(e, generator);
        put(e, hc, generator.getMappingSet());
      }
      return hc;
    }

    @Override
    public HoldingContainer visitConvertExpression(ConvertExpression e, ClassGenerator<?> generator) throws RuntimeException {
      HoldingContainer hc = getPrevious(e, generator.getMappingSet());
      if (hc == null) {
        hc = super.visitConvertExpression(e, generator);
        put(e, hc, generator.getMappingSet());
      }
      return hc;
    }
  }

  /**
   * An evaluation visitor which special cases constant (sub-) expressions
   * of any form, passing non-constant expressions to the parent
   * visitor.
   */
  private static class ConstantFilter extends EvalVisitor {

    private final Set<LogicalExpression> constantBoundaries;

    public ConstantFilter(Set<LogicalExpression> constantBoundaries) {
      this.constantBoundaries = constantBoundaries;
    }

    @Override
    public HoldingContainer visitFunctionCall(FunctionCall e, ClassGenerator<?> generator) throws RuntimeException {
      throw new UnsupportedOperationException("FunctionCall is not expected here. "
          + "It should have been converted to FunctionHolderExpression in materialization");
    }

    private HoldingContainer visitExpression(LogicalExpression e, ClassGenerator<?> generator,
                                             VisitorCallback visitorCallback) throws RuntimeException {
      if (constantBoundaries.contains(e)) {
        generator.getMappingSet().enterConstant();
        HoldingContainer c = visitorCallback.getHolder();

        return renderConstantExpression(generator, c, e);
      } else if (generator.getMappingSet().isWithinConstant()) {
        return visitorCallback.getHolder().setConstant(true);
      } else {
        return visitorCallback.getHolder();
      }
    }

    @Override
    public HoldingContainer visitFunctionHolderExpression(FunctionHolderExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
     return visitExpression(e, generator, () -> super.visitFunctionHolderExpression(e, generator));
    }

    @Override
    public HoldingContainer visitBooleanOperator(BooleanOperator e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitBooleanOperator(e, generator));
    }
    @Override
    public HoldingContainer visitIfExpression(IfExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitIfExpression(e, generator));
    }

    @Override
    public HoldingContainer visitSchemaPath(SchemaPath e, ClassGenerator<?> generator) throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitSchemaPath(e, generator));
    }

    @Override
    public HoldingContainer visitLongConstant(LongExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitLongConstant(e, generator));
    }

    @Override
    public HoldingContainer visitDecimal9Constant(Decimal9Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitDecimal9Constant(e, generator));
    }

    @Override
    public HoldingContainer visitDecimal18Constant(Decimal18Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitDecimal18Constant(e, generator));
    }

    @Override
    public HoldingContainer visitDecimal28Constant(Decimal28Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitDecimal28Constant(e, generator));
    }

    @Override
    public HoldingContainer visitDecimal38Constant(Decimal38Expression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitDecimal38Constant(e, generator));
    }

    @Override
    public HoldingContainer visitVarDecimalConstant(VarDecimalExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitVarDecimalConstant(e, generator));
    }

    @Override
    public HoldingContainer visitIntConstant(IntExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitIntConstant(e, generator));
    }

    @Override
    public HoldingContainer visitDateConstant(DateExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitDateConstant(e, generator));
    }

    @Override
    public HoldingContainer visitTimeConstant(TimeExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitTimeConstant(e, generator));
    }

    @Override
    public HoldingContainer visitIntervalYearConstant(IntervalYearExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitIntervalYearConstant(e, generator));
    }

    @Override
    public HoldingContainer visitTimeStampConstant(TimeStampExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitTimeStampConstant(e, generator));
    }

    @Override
    public HoldingContainer visitFloatConstant(FloatExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitFloatConstant(e, generator));
    }

    @Override
    public HoldingContainer visitDoubleConstant(DoubleExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitDoubleConstant(e, generator));
    }

    @Override
    public HoldingContainer visitBooleanConstant(BooleanExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitBooleanConstant(e, generator));
    }

    @Override
    public HoldingContainer visitUnknown(LogicalExpression e, ClassGenerator<?> generator) throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitUnknown(e, generator));
    }

    @Override
    public HoldingContainer visitQuotedStringConstant(QuotedString e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitQuotedStringConstant(e, generator));
    }

    @Override
    public HoldingContainer visitIntervalDayConstant(IntervalDayExpression e, ClassGenerator<?> generator)
        throws RuntimeException {
      return visitExpression(e, generator, () -> super.visitIntervalDayConstant(e, generator));
    }

    /**
     * Get a HoldingContainer for a constant expression. The returned
     * HoldingContainer will indicate it's for a constant expression.
     */
    private HoldingContainer renderConstantExpression(ClassGenerator<?> generator, HoldingContainer input,
                                                      LogicalExpression expr) {
      MajorType.Builder newTypeBuilder = MajorType.newBuilder(input.getMajorType());

      if (expr instanceof DrillFuncHolderExpr &&
          ((DrillFuncHolderExpr) expr).getHolder().getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL) {
        newTypeBuilder.setMode(expr.getMajorType().getMode());
      }
      MajorType newType = newTypeBuilder.build();
      JVar fieldValue = generator.declareClassField("constant", generator.getHolderType(newType));

      // Creates a new vector for class field and assigns to its fields values from output field
      // to allow scalar replacement for source objects
      generator.getEvalBlock().assign(fieldValue, JExpr._new(generator.getHolderType(newType)));
      List<String> holderFields = ValueHolderHelper.getHolderParams(newType);
      for (String holderField : holderFields) {
        generator.getEvalBlock().assign(fieldValue.ref(holderField), input.getHolder().ref(holderField));
      }
      generator.getMappingSet().exitConstant();
      return new HoldingContainer(input.getMajorType(), fieldValue, fieldValue.ref("value"), fieldValue.ref("isSet"))
          .setConstant(true);
    }
  }
}
