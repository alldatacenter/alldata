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
package org.apache.drill.exec.compile.bytecode;

import java.util.List;

import org.apache.drill.exec.compile.CompilationConfig;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

public class ReplacingInterpreter extends BasicInterpreter {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReplacingInterpreter.class);

  private final String className; // fully qualified internal class name
  private int index = 0;
  private final List<ReplacingBasicValue> valueList;

  public ReplacingInterpreter(final String className, final List<ReplacingBasicValue> valueList) {
    super(CompilationConfig.ASM_API_VERSION);
    this.className = className;
    this.valueList = valueList;
  }

  @Override
  public BasicValue newValue(final Type t) {
    if (t != null) {
      final ValueHolderIden iden = HOLDERS.get(t.getDescriptor());
      if (iden != null) {
        final ReplacingBasicValue v = ReplacingBasicValue.create(t, iden, index++, valueList);
        v.markFunctionReturn();
        return v;
      }

      // We need to track use of the "this" objectref
      if ((t.getSort() == Type.OBJECT) && className.equals(t.getInternalName())) {
        final ReplacingBasicValue rbValue = ReplacingBasicValue.create(t, null, 0, valueList);
        rbValue.setThis();
        return rbValue;
      }
    }

    return super.newValue(t);
  }

  @Override
  public BasicValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
    if (insn.getOpcode() == Opcodes.NEW) {
      final TypeInsnNode t = (TypeInsnNode) insn;

      // if this is for a holder class, we'll replace it
      final ValueHolderIden iden = HOLDERS.get(t.desc);
      if (iden != null) {
        return ReplacingBasicValue.create(Type.getObjectType(t.desc), iden, index++, valueList);
      }
    }

    return super.newOperation(insn);
  }

  @Override
  public void returnOperation(AbstractInsnNode insn, BasicValue value, BasicValue expected) {
    if (value instanceof ReplacingBasicValue) {
      ((ReplacingBasicValue) value).markFunctionReturn();
    }
  }

  @Override
  public BasicValue unaryOperation(final AbstractInsnNode insn, final BasicValue value)
      throws AnalyzerException {
    /*
     * We're looking for the assignment of an operator member variable that's a holder to a local
     * objectref. If we spot that, we can't replace the local objectref (at least not
     * until we do the work to replace member variable holders).
     *
     * Note that a GETFIELD does not call newValue(), as would happen for a local variable, so we're
     * emulating that here.
     */
    if ((insn.getOpcode() == Opcodes.GETFIELD) && (value instanceof ReplacingBasicValue)) {
      final ReplacingBasicValue possibleThis = (ReplacingBasicValue) value;
      if (possibleThis.isThis()) {
        final FieldInsnNode fieldInsn = (FieldInsnNode) insn;
        if (HOLDERS.get(fieldInsn.desc) != null) {
          final BasicValue fetchedField = super.unaryOperation(insn, value);
          final ReplacingBasicValue replacingValue =
              ReplacingBasicValue.create(fetchedField.getType(), null, -1, valueList);
          replacingValue.setAssignedToMember();
          return replacingValue;
        }
      }
    }

    return super.unaryOperation(insn,  value);
  }

  @Override
  public BasicValue binaryOperation(final AbstractInsnNode insn,
      final BasicValue value1, final BasicValue value2) throws AnalyzerException {
    /*
     * We're looking for the assignment of a local holder objectref to a member variable.
     * If we spot that, then the local holder can't be replaced, since we don't (yet)
     * have the mechanics to replace the member variable with the holder's members or
     * to assign all of them when this happens.
     */
    if (insn.getOpcode() == Opcodes.PUTFIELD) {
      if (value2.isReference() && (value1 instanceof ReplacingBasicValue)) {
        final ReplacingBasicValue possibleThis = (ReplacingBasicValue) value1;
        if (possibleThis.isThis() && (value2 instanceof ReplacingBasicValue)) {
          // if this is a reference for a holder class, we can't replace it
          if (HOLDERS.get(value2.getType().getDescriptor()) != null) {
            final ReplacingBasicValue localRef = (ReplacingBasicValue) value2;
            localRef.setAssignedToMember();
          }
        }
      }
    }

    return super.binaryOperation(insn, value1, value2);
  }

  @Override
  public BasicValue naryOperation(final AbstractInsnNode insn,
      final List<? extends BasicValue> values) throws AnalyzerException {
    if (insn instanceof MethodInsnNode) {
      boolean skipOne = insn.getOpcode() != Opcodes.INVOKESTATIC;

      // Note if the argument is a holder, and is used as a function argument
      for(BasicValue value : values) {
        // if non-static method, skip over the receiver
        if (skipOne) {
          skipOne = false;
          continue;
        }

        if (value instanceof ReplacingBasicValue) {
          final ReplacingBasicValue argument = (ReplacingBasicValue) value;
          argument.setFunctionArgument();
        }
      }
    }

    return super.naryOperation(insn,  values);
  }

  @Override
  public BasicValue ternaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2, BasicValue value3) throws AnalyzerException {
    // prevents scalar replacement for the case when a holder is stored to the array element
    if (insn.getOpcode() == AASTORE && value3 instanceof ReplacingBasicValue) {
      ReplacingBasicValue argument = (ReplacingBasicValue) value3;
      argument.setAssignedToMember();
    }
    return super.ternaryOperation(insn, value1, value2, value3);
  }

  private static String desc(Class<?> c) {
    final Type t = Type.getType(c);
    return t.getDescriptor();
  }

  static {
    ImmutableMap.Builder<String, ValueHolderIden> builder = ImmutableMap.builder();
    ImmutableSet.Builder<String> setB = ImmutableSet.builder();
    for (Class<?> c : ScalarReplacementTypes.CLASSES) {
      String desc = desc(c);
      setB.add(desc);
      String desc2 = desc.substring(1, desc.length() - 1);
      ValueHolderIden vhi = new ValueHolderIden(c);
      builder.put(desc, vhi);
      builder.put(desc2, vhi);
    }
    HOLDER_DESCRIPTORS = setB.build();
    HOLDERS = builder.build();
  }

  private final static ImmutableMap<String, ValueHolderIden> HOLDERS;
  public final static ImmutableSet<String> HOLDER_DESCRIPTORS;
}
