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

import static org.apache.drill.exec.compile.sig.GeneratorMapping.GM;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.netty.buffer.DrillBuf;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.sig.CodeGeneratorArgument;
import org.apache.drill.exec.compile.sig.CodeGeneratorMethod;
import org.apache.drill.exec.compile.sig.GeneratorMapping;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.compile.sig.SignatureHolder;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.fn.WorkspaceReference;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JLabel;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.apache.drill.exec.server.options.OptionSet;

public class ClassGenerator<T> {

  public static final GeneratorMapping DEFAULT_SCALAR_MAP = GM("doSetup", "doEval", null, null);
  public static final GeneratorMapping DEFAULT_CONSTANT_MAP = GM("doSetup", "doSetup", null, null);
  public static final String INNER_CLASS_FIELD_NAME = "innerClassField";

  public enum BlockType {SETUP, EVAL, RESET, CLEANUP}

  private final SignatureHolder sig;
  private final EvaluationVisitor evaluationVisitor;
  private final Map<ValueVectorSetup, JVar> vvDeclaration = Maps.newHashMap();
  private final Map<String, ClassGenerator<T>> innerClasses = Maps.newHashMap();
  private final List<TypedFieldId> workspaceTypes = Lists.newArrayList();
  private final Map<WorkspaceReference, JVar> workspaceVectors = Maps.newHashMap();
  private final Map<Pair<Integer, JVar>, Function<DrillBuf, ? extends ValueHolder>> constantVars;
  private final CodeGenerator<T> codeGenerator;

  public final JDefinedClass clazz;

  private final JCodeModel model;
  private final OptionSet optionManager;

  private ClassGenerator<T> innerClassGenerator;
  private LinkedList<SizedJBlock>[] blocks;
  private LinkedList<SizedJBlock>[] oldBlocks;

  private JVar innerClassField;

  /**
   * Assume that field has 3 indexes within the constant pull: index of the<br>
   * CONSTANT_Fieldref_info + CONSTANT_Fieldref_info.name_and_type_index + CONSTANT_NameAndType_info.name_index.
   * CONSTANT_NameAndType_info.descriptor_index has limited range of values,
   * CONSTANT_Fieldref_info.class_index is the same for a single class, they will
   * be taken into account later.
   * <p>
   * Local variable has 1 index within the constant pool.
   * {@link org.objectweb.asm.MethodWriter#visitLocalVariable(String, String, String, Label, Label, int)}
   * <p>
   * For upper estimation of max index value, suppose that each field and local
   * variable uses different literal values that have two indexes, then the
   * number of occupied indexes within the constant pull is<br>
   * fieldCount * 3 + fieldCount * 2 + (index - fieldCount) * 3 => fieldCount * 2 + index * 3
   * <p>
   * Assumed that method has 3 indexes within the constant pull: index of the
   * CONSTANT_Methodref_info + CONSTANT_Methodref_info.name_and_type_index +
   * CONSTANT_NameAndType_info.name_index.
   * <p>
   * For the upper estimation of number of split methods suppose that each
   * expression in the method uses single variable. Suppose that the max number
   * of indexes within the constant pull occupied by fields and local variables
   * is M, the number of split methods is N, number of abstract methods in the
   * template is A, then splitted methods count is<br>
   * N = (M - A * N * 3) / 50 => N = M / (50 + A * 3)
   * <p>
   * Additionally should be taken into account class references; fields and
   * methods from the template, so reserves 1000 for them.
   * <p>
   * Then the size of the occupied part in the constant pull is<br>
   * (fieldCount * 2 + index * 3 + 1000) * (1 + 3 / (50 + A * 3))
   */
  private long maxIndex;

  private int index;
  private int labelIndex;
  private MappingSet mappings;

  public static MappingSet getDefaultMapping() {
    return new MappingSet("inIndex", "outIndex", DEFAULT_CONSTANT_MAP, DEFAULT_SCALAR_MAP);
  }

  @SuppressWarnings("unchecked")
  ClassGenerator(CodeGenerator<T> codeGenerator, MappingSet mappingSet, SignatureHolder signature,
                 EvaluationVisitor eval, JDefinedClass clazz, JCodeModel model,
                 OptionSet optionManager) throws JClassAlreadyExistsException {
    this.codeGenerator = codeGenerator;
    this.clazz = clazz;
    this.mappings = mappingSet;
    this.sig = signature;
    this.evaluationVisitor = eval;
    this.model = model;
    this.optionManager = optionManager;
    constantVars = new HashMap<>();

    blocks = new LinkedList[sig.size()];
    for (int i =0; i < sig.size(); i++) {
      blocks[i] = Lists.newLinkedList();
    }
    rotateBlock();

    for (SignatureHolder child : signature.getChildHolders()) {
      Class<?> innerClass = child.getSignatureClass();
      String innerClassName = innerClass.getSimpleName();

      // Create the inner class as private final. If the template (super) class
      // is static, then make the subclass static as well. Note the conversion
      // from the JDK Modifier values to the JCodeModel JMod values: the
      // values are different.

      int mods = JMod.FINAL;
      if ((innerClass.getModifiers() & Modifier.STATIC) != 0) {
        mods += JMod.STATIC;
      }
      JDefinedClass innerClazz = clazz._class(mods, innerClassName);
      innerClasses.put(innerClassName, new ClassGenerator<>(codeGenerator, mappingSet, child, eval, innerClazz, model, optionManager));
    }
    long maxExprsNumber = optionManager != null ? optionManager.getOption(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE_VALIDATOR) : 50;
    maxIndex = Math.round((0xFFFF / (1 + 3. / (3 * sig.size() + maxExprsNumber)) - 1000) / 3);
  }

  public ClassGenerator<T> getInnerGenerator(String name) {
    ClassGenerator<T> inner = innerClasses.get(name);
    Preconditions.checkNotNull(inner);
    return inner;
  }

  public MappingSet getMappingSet() {
    return mappings;
  }

  public void setMappingSet(MappingSet mappings) {
    if (innerClassGenerator != null) {
      innerClassGenerator.setMappingSet(mappings);
    }
    this.mappings = mappings;
  }

  public CodeGenerator<T> getCodeGenerator() {
    return codeGenerator;
  }

  private GeneratorMapping getCurrentMapping() {
    return mappings.getCurrentMapping();
  }

  public JBlock getBlock(String methodName) {
    JBlock blk = this.blocks[sig.get(methodName)].getLast().getBlock();
    Preconditions.checkNotNull(blk, "Requested method name of %s was not available for signature %s.",  methodName, this.sig);
    return blk;
  }

  public JBlock getBlock(BlockType type) {
    return getBlock(getCurrentMapping().getMethodName(type));
  }

  public JBlock getSetupBlock() {
    return getBlock(getCurrentMapping().getMethodName(BlockType.SETUP));
  }

  public JBlock getEvalBlock() {
    return getBlock(getCurrentMapping().getMethodName(BlockType.EVAL));
  }

  public JBlock getResetBlock() {
    return getBlock(getCurrentMapping().getMethodName(BlockType.RESET));
  }

  public JBlock getCleanupBlock() {
    return getBlock(getCurrentMapping().getMethodName(BlockType.CLEANUP));
  }

  public void nestEvalBlock(JBlock block) {
    String methodName = getCurrentMapping().getMethodName(BlockType.EVAL);
    evaluationVisitor.newScope();
    this.blocks[sig.get(methodName)].addLast(new SizedJBlock(block));
  }

  public void unNestEvalBlock() {
    String methodName = getCurrentMapping().getMethodName(BlockType.EVAL);
    evaluationVisitor.leaveScope();
    this.blocks[sig.get(methodName)].removeLast();
  }

  public JLabel getEvalBlockLabel (String prefix) {
    return getEvalBlock().label(prefix + labelIndex++);
  }

  /**
   * Creates an inner braced and indented block
   * @param type type of the created block
   * @return a newly created inner block
   */
  private JBlock createInnerBlock(BlockType type) {
    final JBlock currBlock = getBlock(type);
    final JBlock innerBlock = new JBlock();
    currBlock.add(innerBlock);
    return innerBlock;
  }

  /**
   * Creates an inner braced and indented block for evaluation of the expression.
   * @return a newly created inner eval block
   */
  protected JBlock createInnerEvalBlock() {
    return createInnerBlock(BlockType.EVAL);
  }

  public JVar declareVectorValueSetupAndMember(String batchName, TypedFieldId fieldId) {
    return declareVectorValueSetupAndMember(DirectExpression.direct(batchName), fieldId);
  }

  /**
   * Creates class variable for the value vector using metadata from {@code fieldId}
   * and initializes it using setup blocks.
   *
   * @param batchName expression for invoking {@code getValueAccessorById} method
   * @param fieldId   metadata of the field that should be declared
   * @return a newly generated class field
   */
  public JVar declareVectorValueSetupAndMember(DirectExpression batchName, TypedFieldId fieldId) {
    // declares field in the inner class if innerClassGenerator has been created
    if (innerClassGenerator != null) {
      return innerClassGenerator.declareVectorValueSetupAndMember(batchName, fieldId);
    }
    final ValueVectorSetup setup = new ValueVectorSetup(batchName, fieldId);
//    JVar var = this.vvDeclaration.get(setup);
//    if(var != null) return var;

    Class<?> valueVectorClass = fieldId.getIntermediateClass();
    JClass vvClass = model.ref(valueVectorClass);
    JClass retClass = vvClass;
    String vectorAccess = "getValueVector";
    if (fieldId.isHyperReader()) {
      retClass = retClass.array();
      vectorAccess = "getValueVectors";
    }

    JVar vv = declareClassField("vv", retClass);
    JClass t = model.ref(SchemaChangeException.class);
    JType objClass = model.ref(Object.class);
    JBlock b = getSetupBlock();

    JVar fieldArr = b.decl(model.INT.array(), "fieldIds" + index++, JExpr.newArray(model.INT, fieldId.getFieldIds().length));
    int[] fieldIndices = fieldId.getFieldIds();
    for (int i = 0; i < fieldIndices.length; i++) {
       b.assign(fieldArr.component(JExpr.lit(i)), JExpr.lit(fieldIndices[i]));
    }

    JInvocation invoke = batchName
        .invoke("getValueAccessorById")
        .arg(vvClass.dotclass())
        .arg(fieldArr);

    JVar obj = b.decl(
        objClass,
        getNextVar("tmp"),
        invoke.invoke(vectorAccess));

    b._if(obj.eq(JExpr._null()))._then()._throw(JExpr._new(t).arg(JExpr.lit(
        String.format("Failure while loading vector %s with id: %s.", vv.name(), fieldId.toString()))));
    //b.assign(vv, JExpr.cast(retClass, ((JExpression) JExpr.cast(wrapperClass, obj)).invoke(vectorAccess)));
    b.assign(vv, JExpr.cast(retClass, obj));
    vvDeclaration.put(setup, vv);

    return vv;
  }

  public enum BlkCreateMode {
    /** Create new block */
    TRUE,
    /** Do not create block; put into existing block. */
    FALSE,
    /** Create new block only if # of expressions added hit upper-bound
     * ({@link ExecConstants#CODE_GEN_EXP_IN_METHOD_SIZE}). */
    TRUE_IF_BOUND
  }

  public HoldingContainer addExpr(LogicalExpression ex) {
    // default behavior is always to put expression into new block.
    return addExpr(ex, BlkCreateMode.TRUE);
  }

  public HoldingContainer addExpr(LogicalExpression ex, BlkCreateMode mode) {
    if (mode == BlkCreateMode.TRUE || mode == BlkCreateMode.TRUE_IF_BOUND) {
      rotateBlock(mode);
    }

    for (LinkedList<SizedJBlock> b : blocks) {
      b.getLast().incCounter();
    }

    return evaluationVisitor.addExpr(ex, this);
  }

  public void rotateBlock() {
    // default behavior is always to create new block.
    rotateBlock(BlkCreateMode.TRUE);
  }

  /**
   * Assigns {@link #blocks} from the last nested {@link #innerClassGenerator} to {@link this#blocks}
   * recursively if {@link #innerClassGenerator} has been created.
   */
  private void setupValidBlocks() {
    if (createNestedClass()) {
      // blocks from the last inner class should be used
      setupInnerClassBlocks();
    }
  }

  /**
   * Creates {@link #innerClassGenerator} with inner class
   * if {@link #hasMaxIndexValue()} returns {@code true}.
   *
   * @return true if splitting happened.
   */
  private boolean createNestedClass() {
    if (hasMaxIndexValue()) {
      // all new fields will be declared in the class from innerClassGenerator
      if (innerClassGenerator == null) {
        try {
          JDefinedClass innerClazz = clazz._class(JMod.NONE, clazz.name() + "0");
          innerClassGenerator = new ClassGenerator<>(codeGenerator, mappings, sig, evaluationVisitor, innerClazz, model, optionManager);
        } catch (JClassAlreadyExistsException e) {
          throw new DrillRuntimeException(e);
        }
        oldBlocks = blocks;
        innerClassGenerator.index = index;
        innerClassGenerator.maxIndex += index;
        // blocks from the inner class should be used
        setupInnerClassBlocks();
        innerClassField = clazz.field(JMod.NONE, model.ref(innerClassGenerator.clazz.name()), INNER_CLASS_FIELD_NAME);
        return true;
      }
      return innerClassGenerator.createNestedClass();
    }
    return false;
  }

  /**
   * Checks that {@link #index} has reached its max value.
   *
   * @return true if {@code index + clazz.fields().size() * 2 / 3} is greater than {@code maxIndex}
   */
  private boolean hasMaxIndexValue() {
    return index + clazz.fields().size() * 2 / 3 > maxIndex;
  }

  /**
   * Gets blocks from the last inner {@link ClassGenerator innerClassGenerator}
   * and assigns it to the {@link this#blocks} recursively.
   */
  private void setupInnerClassBlocks() {
    if (innerClassGenerator != null) {
      innerClassGenerator.setupInnerClassBlocks();
      blocks = innerClassGenerator.blocks;
    }
  }

  /**
   * Create a new code block, closing the current block.
   *
   * @param mode the {@link BlkCreateMode block create mode}
   * for the new block.
   */
  private void rotateBlock(BlkCreateMode mode) {
    boolean blockRotated = false;
    for (LinkedList<SizedJBlock> b : blocks) {
      if (mode == BlkCreateMode.TRUE ||
          (mode == BlkCreateMode.TRUE_IF_BOUND &&
            optionManager != null &&
            b.getLast().getCount() > optionManager.getOption(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE_VALIDATOR))) {
        b.add(new SizedJBlock(new JBlock(true, true)));
        blockRotated = true;
      }
    }
    if (blockRotated) {
      evaluationVisitor.previousExpressions.clear();
      setupValidBlocks();
    }
  }

  /**
   * Creates methods from the signature {@code sig} with body from the
   * appropriate {@code blocks}.
   */
  void flushCode() {
    if (innerClassGenerator != null) {
      blocks = oldBlocks;
      innerClassGenerator.flushCode();
    }
    int i = 0;
    for (CodeGeneratorMethod method : sig) {
      JMethod outer = clazz.method(JMod.PUBLIC, model._ref(method.getReturnType()), method.getMethodName());
      for (CodeGeneratorArgument arg : method) {
        outer.param(arg.getType(), arg.getName());
      }
      for (Class<?> c : method.getThrowsIterable()) {
        outer._throws(model.ref(c));
      }
      outer._throws(SchemaChangeException.class);

      int methodIndex = 0;
      int exprsInMethod = 0;
      boolean isVoidMethod = method.getReturnType() == void.class;
      for (SizedJBlock sb : blocks[i++]) {
        JBlock b = sb.getBlock();
        if (!b.isEmpty()) {
          if (optionManager != null &&
              exprsInMethod > optionManager.getOption(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE_VALIDATOR)) {
            JMethod inner = clazz.method(JMod.PRIVATE, model._ref(method.getReturnType()), method.getMethodName() + methodIndex);
            JInvocation methodCall = JExpr.invoke(inner);
            for (CodeGeneratorArgument arg : method) {
              inner.param(arg.getType(), arg.getName());
              methodCall.arg(JExpr.direct(arg.getName()));
            }
            for (Class<?> c : method.getThrowsIterable()) {
              inner._throws(model.ref(c));
            }
            inner._throws(SchemaChangeException.class);

            if (isVoidMethod) {
              outer.body().add(methodCall);
            } else {
              outer.body()._return(methodCall);
            }
            outer = inner;
            exprsInMethod = 0;
            ++methodIndex;
          }
          outer.body().add(b);
          exprsInMethod += sb.getCount();
        }
      }
      if (innerClassField != null) {
        // creates inner class instance and initializes innerClassField
        if (method.getMethodName().equals("__DRILL_INIT__")) {
          JInvocation rhs = JExpr._new(innerClassGenerator.clazz);
          JBlock block = new JBlock().assign(innerClassField, rhs);
          outer.body().add(block);
        }

        List<JType> argTypes = new ArrayList<>();
        for (CodeGeneratorArgument arg : method) {
          argTypes.add(model._ref(arg.getType()));
        }
        JMethod inner = innerClassGenerator.clazz.getMethod(method.getMethodName(), argTypes.toArray(new JType[0]));

        if (inner != null) {
          // removes empty method from the inner class
          if (inner.body().isEmpty()) {
            innerClassGenerator.clazz.methods().remove(inner);
            continue;
          }

          JInvocation methodCall = innerClassField.invoke(inner);
          for (CodeGeneratorArgument arg : method) {
            methodCall.arg(JExpr.direct(arg.getName()));
          }
          if (isVoidMethod) {
            outer.body().add(methodCall);
          } else {
            outer.body()._return(methodCall);
          }
        }
      }
    }

    for(ClassGenerator<T> child : innerClasses.values()) {
      child.flushCode();
    }
  }

  public JCodeModel getModel() {
    return model;
  }

  public String getNextVar() {
    return "v" + index++;
  }

  public String getNextVar(String prefix) {
    return prefix + index++;
  }

  public JVar declareClassField(String prefix, JType t) {
    return declareClassField(prefix, t, null);
  }

  public JVar declareClassField(String prefix, JType t, JExpression init) {
    if (innerClassGenerator != null && hasMaxIndexValue()) {
      return innerClassGenerator.declareClassField(prefix, t, init);
    }
    return clazz.field(JMod.NONE, t, getNextVar(prefix), init);
  }

  public Pair<Integer, JVar> declareClassConstField(String prefix, JType t,
                                                    Function<DrillBuf, ? extends ValueHolder> function) {
    return declareClassConstField(prefix, t, null, function);
  }

  /**
   * Declare a constant field for the class.
   * The class field innerClassField will be created if innerClassGenerator exists.
   *
   * @param prefix the prefix name of class field
   * @param t the type of class field
   * @param init init expression
   * @param function holds the constant value which
   * returns a value holder must be set to the class field when the class instance created.
   * @return the depth of nested class, class field
   */
  public Pair<Integer, JVar> declareClassConstField(String prefix, JType t, JExpression init,
                                                    Function<DrillBuf, ? extends ValueHolder> function) {
    JVar var;
    int depth = 1;
    if (innerClassGenerator != null) {
      Pair<Integer, JVar> nested = innerClassGenerator.declareClassConstField(prefix, t, init, function);
      depth = nested.getKey() + 1;
      var = nested.getValue();
    } else {
      var = clazz.field(JMod.NONE, t, getNextVar(prefix), init);
    }
    Pair<Integer, JVar> depthVar = Pair.of(depth, var);
    constantVars.put(depthVar, function);
    return depthVar;
  }

  public Map<Pair<Integer, JVar>, Function<DrillBuf, ? extends ValueHolder>> getConstantVars() {
    return constantVars;
  }

  public HoldingContainer declare(MajorType t) {
    return declare(t, true);
  }

  public HoldingContainer declare(MajorType t, boolean includeNewInstance) {
    return declare(t, "out", includeNewInstance);
  }

  /**
   * Adds a local variable declaration based on given name and type.
   *
   * @param t major type
   * @param name variable name
   * @param includeNewInstance whether to create new instance
   * @return holder instance
   */
  public HoldingContainer declare(MajorType t, String name, boolean includeNewInstance) {
    JType holderType = getHolderType(t);
    JVar var;
    if (includeNewInstance) {
      var = getEvalBlock().decl(holderType, getNextVar(name), JExpr._new(holderType));
    } else {
      var = getEvalBlock().decl(holderType, getNextVar(name));
    }
    JFieldRef outputSet = null;
    if (t.getMode() == DataMode.OPTIONAL) {
      outputSet = var.ref("isSet");
    }
    return new HoldingContainer(t, var, var.ref("value"), outputSet);
  }

  public List<TypedFieldId> getWorkspaceTypes() {
    return workspaceTypes;
  }

  public Map<WorkspaceReference, JVar> getWorkspaceVectors() {
    return workspaceVectors;
  }

  /**
   * Prepare the generated class for use as a plain-old Java class
   * (to be compiled by a compiler and directly loaded without a
   * byte-code merge. Three additions are necessary:
   * <ul>
   * <li>The class must extend its template as we won't merge byte
   * codes.</li>
   * <li>A constructor is required to call the <tt>__DRILL_INIT__</tt>
   * method. If this is a nested class, then the constructor must
   * include parameters defined by the base class.</li>
   * <li>For each nested class, create a method that creates an
   * instance of that nested class using a well-defined name. This
   * method overrides the base class method defined for this purpose.</li>
   */
  public void preparePlainJava() {

    // If this generated class uses the "straight Java" technique
    // (no byte code manipulation), then the class must extend the
    // template so it plays by normal Java rules for finding the
    // template methods via inheritance rather than via code injection.

    Class<?> baseClass = sig.getSignatureClass();
    clazz._extends(baseClass);

    // Create a constuctor for the class: either a default one,
    // or (for nested classes) one that passes along arguments to
    // the super class constructor.

    Constructor<?>[] ctors = baseClass.getConstructors();
    for (Constructor<?> ctor : ctors) {
      addCtor(ctor.getParameterTypes());
    }

    // Some classes have no declared constructor, but we need to generate one
    // anyway.

    if ( ctors.length == 0 ) {
      addCtor( new Class<?>[] {} );
    }

    // Repeat for inner classes.

    for(ClassGenerator<T> child : innerClasses.values()) {
      child.preparePlainJava();

      // If there are inner classes, then we need to generate a "shim" method
      // to instantiate that class.
      //
      // protected TemplateClass.TemplateInnerClass newTemplateInnerClass( args... ) {
      //    return new GeneratedClass.GeneratedInnerClass( args... );
      // }
      //
      // The name is special, it is "new" + inner class name. The template must
      // provide a method of this name that creates the inner class instance.

      String innerClassName = child.clazz.name();
      JMethod shim = clazz.method(JMod.PROTECTED, child.sig.getSignatureClass(), "new" + innerClassName);
      JInvocation childNew = JExpr._new(child.clazz);
      Constructor<?>[] childCtors = child.sig.getSignatureClass().getConstructors();
      Class<?>[] params;
      if (childCtors.length==0) {
        params = new Class<?>[0];
      } else {
        params = childCtors[0].getParameterTypes();
      }
      for (int i = 1; i < params.length; i++) {
        Class<?> p = params[i];
        childNew.arg(shim.param(model._ref(p), "arg" + i));
      }
      shim.body()._return(JExpr._this().invoke("injectMembers").arg(childNew));
    }
  }

  /**
   * The code generator creates a method called __DRILL_INIT__ which takes the
   * place of the constructor when the code goes though the byte code merge.
   * For Plain-old Java, we call the method from a constructor created for
   * that purpose. (Generated code, fortunately, never includes a constructor,
   * so we can create one.) Since the init block throws an exception (which
   * should never occur), the generated constructor converts the checked
   * exception into an unchecked one so as to not require changes to the
   * various places that create instances of the generated classes.
   *
   * Example:<code><pre>
   * public StreamingAggregatorGen1() {
   *       try {
   *         __DRILL_INIT__();
   *     } catch (SchemaChangeException e) {
   *         throw new UnsupportedOperationException(e);
   *     }
   * }</pre></code>
   *
   * Note: in Java 8 we'd use the <tt>Parameter</tt> class defined in Java's
   * introspection package. But, Drill prefers Java 7 which only provides
   * parameter types.
   */
  private void addCtor(Class<?>[] parameters) {
    JMethod ctor = clazz.constructor(JMod.PUBLIC);
    JBlock body = ctor.body();

    // If there are parameters, need to pass them to the super class.
    if (parameters.length > 0) {
      JInvocation superCall = JExpr.invoke("super");

      // This case only occurs for nested classes, and all nested classes
      // in Drill are inner classes. Don't pass along the (hidden)
      // this$0 field.

      for (int i = 1; i < parameters.length; i++) {
        Class<?> p = parameters[i];
        superCall.arg(ctor.param(model._ref(p), "arg" + i));
      }
      body.add(superCall);
    }
    JTryBlock tryBlock = body._try();
    tryBlock.body().invoke(SignatureHolder.DRILL_INIT_METHOD);
    JCatchBlock catchBlock = tryBlock._catch(model.ref(SchemaChangeException.class));
    catchBlock.body()._throw(JExpr._new(model.ref(UnsupportedOperationException.class)).arg(catchBlock.param("e")));
  }

  private static class ValueVectorSetup {
    final DirectExpression batch;
    final TypedFieldId fieldId;

    public ValueVectorSetup(DirectExpression batch, TypedFieldId fieldId) {
      super();
      this.batch = batch;
      this.fieldId = fieldId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((batch == null) ? 0 : batch.hashCode());
      result = prime * result + ((fieldId == null) ? 0 : fieldId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ValueVectorSetup other = (ValueVectorSetup) obj;
      if (batch == null) {
        if (other.batch != null) {
          return false;
        }
      } else if (!batch.equals(other.batch)) {
        return false;
      }
      if (fieldId == null) {
        if (other.fieldId != null) {
          return false;
        }
      } else if (!fieldId.equals(other.fieldId)) {
        return false;
      }
      return true;
    }
  }

  /**
   * Represents a (Nullable)?(Type)Holder instance. Allows code
   * gen to declare, set, work with and retrieve values from a holder.
   * Holders exploit scalar replacement in Drill (or in Java): that
   * the holder can, via code rewrites, be replaced by scalars that
   * do the same job.
   */
  // TODO: Might be better modeled as a set of classes for the special
  // kinds of holders rather than a generic class and many if-statements.
  public static class HoldingContainer {
    private final JVar holder;
    private final JFieldRef value;
    private final JFieldRef isSet;
    private final MajorType type;
    private boolean isConstant;
    private final boolean singularRepeated;
    private final boolean isReader;

    public HoldingContainer(MajorType t, JVar holder, JFieldRef value, JFieldRef isSet) {
      this(t, holder, value, isSet, false, false);
    }

    public HoldingContainer(MajorType t, JVar holder, JFieldRef value, JFieldRef isSet, boolean singularRepeated, boolean isReader) {
      this.holder = holder;
      this.value = value;
      this.isSet = isSet;
      this.type = t;
      this.isConstant = false;
      this.singularRepeated = singularRepeated;
      this.isReader = isReader;
    }

    public HoldingContainer(HoldingContainer from) {
      this.holder = from.holder;
      this.value =from. value;
      this.isSet = from.isSet;
      this.type = from.type;
      this.isConstant = from.isConstant;
      this.singularRepeated = from.singularRepeated;
      this.isReader = from.isReader;
    }

    public boolean isReader() {
      return isReader;
    }

    public boolean isSingularRepeated() {
      return singularRepeated;
    }

    public HoldingContainer setConstant(boolean isConstant) {
      this.isConstant = isConstant;
      return this;
    }

    public JFieldRef f(String name) {
      return holder.ref(name);
    }

    public boolean isConstant() {
      return isConstant;
    }

    public JVar getHolder() {
      return holder;
    }

    public JFieldRef getValue() {
      return value;
    }

    public MajorType getMajorType() {
      return type;
    }

    public JFieldRef getIsSet() {
      Preconditions.checkNotNull(isSet,
          "You cannot access the isSet variable when operating on a non-nullable output value.");
      return isSet;
    }

    public boolean isOptional() {
      return type.getMode() == DataMode.OPTIONAL;
    }

    public boolean isRepeated() {
      return type.getMode() == DataMode.REPEATED;
    }

    public TypeProtos.MinorType getMinorType() {
      return type.getMinorType();
    }

    /**
     * Convert holder to a string for debugging use.
     */
    @Override
    public String toString() {
      DebugStringBuilder buf = new DebugStringBuilder(this);
      if (isConstant()) {
        buf.append("const ");
      }
      buf.append(holder.type().name())
        .append(" ")
        .append(holder.name())
        .append(", ")
        .append(type.getMode().name())
        .append(" ")
        .append(type.getMinorType().name())
        .append(", ");
      holder.generate(buf.formatter());
      if (value != null) {
        buf.append(", ");
        value.generate(buf.formatter());
      }
      return buf.toString();
    }
  }

  public JType getHolderType(MajorType t) {
    return TypeHelper.getHolderType(model, t.getMinorType(), t.getMode());
  }
}
