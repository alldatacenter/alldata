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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;

/**
 * Analyzer that allows us to inject additional functionality into ASMs basic analysis.
 *
 * <p>We need to be able to keep track of local variables that are assigned to each other
 * so that we can infer their replaceability (for scalar replacement). In order to do that,
 * we need to know when local variables are assigned (with the old value being overwritten)
 * so that we can associate them with the new value, and hence determine whether they can
 * also be replaced, or not.
 *
 * <p>In order to capture the assignment operation, we have to provide our own Frame<>, but
 * ASM doesn't provide a direct way to do that. Here, we use the Analyzer's newFrame() methods
 * as factories that will provide our own derivative of Frame<> which we use to detect
 */
public class MethodAnalyzer<V extends Value> extends Analyzer <V> {

  // list of method instructions which is analyzed
  private InsnList insnList;

  public MethodAnalyzer(Interpreter<V> interpreter) {
    super(interpreter);
  }

  @Override
  protected Frame<V> newFrame(int maxLocals, int maxStack) {
    return new AssignmentTrackingFrame<>(maxLocals, maxStack);
  }

  @Override
  protected Frame<V> newFrame(Frame<? extends V> src) {
    return new AssignmentTrackingFrame<>(src);
  }

  @Override
  protected void newControlFlowEdge(int insnIndex, int successorIndex) {
    AssignmentTrackingFrame<V> oldFrame = (AssignmentTrackingFrame<V>) getFrames()[insnIndex];
    AbstractInsnNode insn = insnList.get(insnIndex);
    if (insn.getType() == AbstractInsnNode.LABEL) {
      // checks whether current label corresponds to the end of conditional block to restore previous
      // local variables set
      if (insn.equals(oldFrame.labelsStack.peekFirst())) {
        oldFrame.localVariablesSet.pop();
        oldFrame.labelsStack.pop();
      }
    }
  }

  @Override
  public Frame<V>[] analyze(String owner, MethodNode method) throws AnalyzerException {
    insnList = method.instructions;
    return super.analyze(owner, method);
  }

  /**
   * Custom Frame<> that captures setLocal() calls in order to associate values
   * that are assigned to the same local variable slot. Also it controls stack to determine whether
   * object was assigned to the value declared outside of conditional block.
   *
   * <p>Since this is almost a pass-through, the constructors' arguments match
   * those from Frame<>.
   */
  private static class AssignmentTrackingFrame<V extends Value> extends Frame<V> {

    // represents stack of variable sets declared inside current code block
    private Deque<BitSet> localVariablesSet;

    // stack of LabelNode instances which correspond to the end of conditional block
    private Deque<LabelNode> labelsStack;

    /**
     * Constructor.
     *
     * @param nLocals the number of locals the frame should have
     * @param nStack the maximum size of the stack the frame should have
     */
    public AssignmentTrackingFrame(int nLocals, int nStack) {
      super(nLocals, nStack);
      localVariablesSet = new ArrayDeque<>();
      localVariablesSet.push(new BitSet());
      labelsStack = new ArrayDeque<>();
    }

    /**
     * Copy constructor.
     *
     * @param src the frame being copied
     */
    public AssignmentTrackingFrame(Frame<? extends V> src) {
      super(src);
      // localVariablesSet and labelsStack aren't copied from the src frame
      // since the branch-sensitive analysis was already done for src frame
      localVariablesSet = new ArrayDeque<>();
      localVariablesSet.push(new BitSet());
      labelsStack = new ArrayDeque<>();
    }

    @Override
    public void setLocal(int i, V value) {
      /*
       * If we're replacing one ReplacingBasicValue with another, we need to
       * associate them together so that they will have the same replaceability
       * attributes. We also track the local slot the new value will be stored in.
       */
      if (value instanceof ReplacingBasicValue) {
        ReplacingBasicValue replacingValue = (ReplacingBasicValue) value;
        replacingValue.setFrameSlot(i);
        V localValue = getLocal(i);
        BitSet currentLocalVars = localVariablesSet.element();
        if (localValue instanceof ReplacingBasicValue) {
          if (!currentLocalVars.get(i)) {
            // value is assigned to object declared outside of conditional block
            replacingValue.setAssignedInConditionalBlock();
          }
          ReplacingBasicValue localReplacingValue = (ReplacingBasicValue) localValue;
          localReplacingValue.associate(replacingValue);
        } else {
          currentLocalVars.set(i);
        }
      }

      super.setLocal(i, value);
    }

    @Override
    public void initJumpTarget(int opcode, LabelNode target) {
      if (target != null) {
        switch (opcode) {
          case IFEQ:
          case IFNE:
          case IFLT:
          case IFGE:
          case IFGT:
          case IFLE:
          case IF_ICMPEQ:
          case IF_ICMPNE:
          case IF_ICMPLT:
          case IF_ICMPGE:
          case IF_ICMPGT:
          case IF_ICMPLE:
          case IF_ACMPEQ:
          case IF_ACMPNE:
          case IFNULL:
          case IFNONNULL:
            // for the case when conditional block is handled, creates new variables set
            // to store local variables declared inside current conditional block and
            // stores its target LabelNode to restore previous variables set after conditional block is ended
            localVariablesSet.push(new BitSet());
            labelsStack.push(target);
        }
      }
    }

    @Override
    public void clearStack() {
      super.clearStack();
      localVariablesSet = null;
      labelsStack = null;
    }
  }
}
