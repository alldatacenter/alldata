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

import java.util.HashMap;

import org.apache.drill.exec.compile.CompilationConfig;
import org.apache.drill.exec.compile.bytecode.ValueHolderIden.ValueHolderSub;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class InstructionModifier extends MethodVisitor {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InstructionModifier.class);

  /* Map from old (reference) local variable index to new local variable information. */
  private final IntObjectHashMap<ValueHolderIden.ValueHolderSub> oldToNew = new IntObjectHashMap<>();

  private final IntIntHashMap oldLocalToFirst = new IntIntHashMap();

  private final DirectSorter adder;
  private int lastLineNumber = 0; // the last line number seen
  private final TrackingInstructionList list;
  private final String name;
  private final String desc;
  private final String signature;

  private int stackIncrease = 0; // how much larger we have to make the stack

  public InstructionModifier(final int access, final String name, final String desc,
      final String signature, final String[] exceptions, final TrackingInstructionList list,
      final MethodVisitor inner) {
    super(CompilationConfig.ASM_API_VERSION, new DirectSorter(access, desc, inner));
    this.name = name;
    this.desc = desc;
    this.signature = signature;
    this.list = list;
    this.adder = (DirectSorter) mv;
  }

  public int getLastLineNumber() {
    return lastLineNumber;
  }

  private static ReplacingBasicValue filterReplacement(final BasicValue basicValue) {
    if (basicValue instanceof ReplacingBasicValue) {
      final ReplacingBasicValue replacingValue = (ReplacingBasicValue) basicValue;
      if (replacingValue.isReplaceable()) {
        return replacingValue;
      }
    }

    return null;
  }

  private ReplacingBasicValue getLocal(final int var) {
    final BasicValue basicValue = list.currentFrame.getLocal(var);
    return filterReplacement(basicValue);
  }

  /**
   * Peek at a value in the current frame's stack, counting down from the top.
   *
   * @param depth how far down to peek; 0 is the top of the stack, 1 is the
   *   first element down, etc
   * @return the value on the stack, or null if it isn't a ReplacingBasciValue
   */
  private ReplacingBasicValue peekFromTop(final int depth) {
    Preconditions.checkArgument(depth >= 0);
    final Frame<BasicValue> frame = list.currentFrame;
    final BasicValue basicValue = frame.getStack((frame.getStackSize() - 1) - depth);
    return filterReplacement(basicValue);
  }

  /**
   * Get the value of a function return if it is a ReplacingBasicValue.
   *
   * <p>Assumes that we're in the middle of processing an INVOKExxx instruction.
   *
   * @return the value that will be on the top of the stack after the function returns
   */
  private ReplacingBasicValue getFunctionReturn() {
    final Frame<BasicValue> nextFrame = list.nextFrame;
    final BasicValue basicValue = nextFrame.getStack(nextFrame.getStackSize() - 1);
    return filterReplacement(basicValue);
  }

  @Override
  public void visitInsn(final int opcode) {
    switch (opcode) {
    case Opcodes.DUP:
      /*
       * Pattern:
       *   BigIntHolder out5 = new BigIntHolder();
       *
       * Original bytecode:
       *   NEW org/apache/drill/exec/expr/holders/BigIntHolder
       *   DUP
       *   INVOKESPECIAL org/apache/drill/exec/expr/holders/BigIntHolder.<init> ()V
       *   ASTORE 6 # the index of the out5 local variable (which is a reference)
       *
       * Desired replacement:
       *   ICONST_0
       *   ISTORE 12
       *
       *   In the original, the holder's objectref will be used twice: once for the
       *   constructor call, and then to be stored. Since the holder has been replaced
       *   with one or more locals to hold its members, we don't allocate or store it.
       *   he NEW and the ASTORE are replaced via some stateless changes elsewhere; here
       *   we need to eliminate the DUP.
       *
       * TODO: there may be other reasons for a DUP to appear in the instruction stream,
       * such as reuse of a common subexpression that the compiler optimizer has
       * eliminated. This pattern may also be used for non-holders. We need to be
       * more certain of the source of the DUP, and whether the surrounding context matches
       * the above.
       */
      if (peekFromTop(0) != null) {
        return; // don't emit the DUP
      }
      break;

    case Opcodes.DUP_X1: {
      /*
       * There are a number of patterns that lead to this instruction being generated. Here
       * are some examples:
       *
       * Pattern:
       *   129:        out.start = out.end = text.end;
       *
       * Original bytecode:
       *   L9
       *    LINENUMBER 129 L9
       *    ALOAD 7
       *    ALOAD 7
       *    ALOAD 8
       *    GETFIELD org/apache/drill/exec/expr/holders/VarCharHolder.end : I
       *    DUP_X1
       *    PUTFIELD org/apache/drill/exec/expr/holders/VarCharHolder.end : I
       *    PUTFIELD org/apache/drill/exec/expr/holders/VarCharHolder.start : I
       *
       * Desired replacement:
       *   L9
       *    LINENUMBER 129 L9
       *    ILOAD 17
       *    DUP
       *    ISTORE 14
       *    ISTORE 13
       *
       *    At this point, the ALOAD/GETFIELD and ALOAD/PUTFIELD combinations have
       *    been replaced by the ILOAD and ISTOREs. However, there is still the DUP_X1
       *    in the instruction stream. In this case, it is duping the fetched holder
       *    member so that it can be stored again. We still need to do that, but because
       *    the ALOADed objectrefs are no longer on the stack, we don't need to duplicate
       *    the value lower down in the stack anymore, but can instead DUP it where it is.
       *    (Similarly, if the fetched field was a long or double, the same applies for
       *    the expected DUP2_X1.)
       *
       * There's also a similar pattern for zeroing values:
       * Pattern:
       *   170:            out.start = out.end = 0;
       *
       * Original bytecode:
       *   L20
       *    LINENUMBER 170 L20
       *    ALOAD 13
       *    ALOAD 13
       *    ICONST_0
       *    DUP_X1
       *    PUTFIELD org/apache/drill/exec/expr/holders/VarCharHolder.end : I
       *    PUTFIELD org/apache/drill/exec/expr/holders/VarCharHolder.start : I
       *
       * Desired replacement:
       *   L20
       *    LINENUMBER 170 L20
       *    ICONST_0
       *    DUP
       *    ISTORE 17
       *    ISTORE 16
       *
       *
       * There's also another pattern that involves DUP_X1
       * Pattern:
       *   1177:                   out.buffer.setByte(out.end++, currentByte);
       *
       * We're primarily interested in the out.end++ -- the post-increment of
       * a holder member.
       *
       * Original bytecode:
       *    L694
       *     LINENUMBER 1177 L694
       *     ALOAD 212
       *     GETFIELD org/apache/drill/exec/expr/holders/VarCharHolder.buffer : Lio/netty/buffer/DrillBuf;
       *     ALOAD 212
       *     DUP
       * >   GETFIELD org/apache/drill/exec/expr/holders/VarCharHolder.end : I
       * >   DUP_X1
       * >   ICONST_1
       * >   IADD
       * >   PUTFIELD org/apache/drill/exec/expr/holders/VarCharHolder.end : I
       *     ILOAD 217
       *     INVOKEVIRTUAL io/netty/buffer/DrillBuf.setByte (II)Lio/netty/buffer/ByteBuf;
       *     POP
       *
       * This fragment includes the entirety of the line 1177 above, but we're only concerned with
       * the lines marked with '>' on the left; the rest were kept for context, because it is the
       * use of the pre-incremented value as a function argument that is generating the DUP_X1 --
       * the DUP_X1 is how the value is preserved before incrementing.
       *
       * The common element in these patterns is that the stack has an objectref and a value that will
       * be stored via a PUTFIELD. The value has to be used in other contexts, so it is to be DUPed, and
       * stashed away under the objectref. In the case where the objectref belongs to a holder that will
       * be gone as a result of scalar replacement, then the objectref won't be present, so we don't need
       * the _X1 option.
       *
       * If we're replacing the holder under the value being duplicated, then we don't need to put the
       * DUPed value back under it, because it won't be present in the stack. We can just use a plain DUP.
       */
      final ReplacingBasicValue rbValue = peekFromTop(1);
      if (rbValue != null) {
        super.visitInsn(Opcodes.DUP);
        return;
      }
      break;
    }

    case Opcodes.DUP2_X1: {
      /*
       * See the comments above for DUP_X1, which also apply here; this just handles long and double
       * values, which are twice as large, in the same way.
       */
      if (peekFromTop(0) != null) {
        throw new IllegalStateException("top of stack should be 2nd part of a long or double");
      }
      final ReplacingBasicValue rbValue = peekFromTop(2);
      if (rbValue != null) {
        super.visitInsn(Opcodes.DUP2);
        return;
      }
      break;
    }
    }

    // if we get here, emit the original instruction
    super.visitInsn(opcode);
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    /*
     * This includes NEW, NEWARRAY, CHECKCAST, or INSTANCEOF.
     *
     * TODO: aren't we just trying to eliminate NEW (and possibly NEWARRAY)?
     * It looks like we'll currently pick those up because we'll only have
     * replaced the values for those, but we might find other reasons to replace
     * things, in which case this will be too broad.
     */
    final ReplacingBasicValue r = getFunctionReturn();
    if (r != null) {
      final ValueHolderSub sub = r.getIden().getHolderSub(adder);
      oldToNew.put(r.getIndex(), sub);
    } else {
      super.visitTypeInsn(opcode, type);
    }
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    lastLineNumber = line;
    super.visitLineNumber(line, start);
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    ReplacingBasicValue v;
    if (opcode == Opcodes.ASTORE && (v = peekFromTop(0)) != null) {
      final ValueHolderSub from = oldToNew.get(v.getIndex());

      final ReplacingBasicValue current = getLocal(var);
      // if local var is set, then transfer to it to the existing holders in the local position.
      if (current != null) {
        final ValueHolderSub newSub = oldToNew.get(current.getIndex());
        if (newSub.iden() == from.iden()) {
          final int targetFirst = newSub.first();
          from.transfer(this, targetFirst);
          return;
        }
      }

      // if local var is not set, then check map to see if existing holders are mapped to local var.
      if (oldLocalToFirst.containsKey(var)) {
        final ValueHolderSub sub = oldToNew.get(oldLocalToFirst.get(var));
        if (sub.iden() == from.iden()) {
          // if they are, then transfer to that.
          from.transfer(this, sub.first());
          return;
        }
      }

      // map from variables to global space for future use.
      oldLocalToFirst.put(var, v.getIndex());
      return;
    } else if (opcode == Opcodes.ALOAD && (v = getLocal(var)) != null) {
      /*
       * Not forwarding this removes a now unnecessary ALOAD for a holder. The required LOAD/STORE
       * sequences will be generated by the ASTORE code above.
       */
      return;
    }

    super.visitVarInsn(opcode, var);
  }

  void directVarInsn(final int opcode, final int var) {
    adder.directVarInsn(opcode, var);
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    super.visitMaxs(maxStack + stackIncrease, maxLocals);
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    int stackDepth = 0;
    ReplacingBasicValue value;
    switch (opcode) {
    case Opcodes.PUTFIELD:
      value = peekFromTop(stackDepth++);
      if (value != null) {
        if (filterReplacement(value) == null) {
          super.visitFieldInsn(opcode, owner, name, desc);
          return;
        } else {
          /*
           * We are trying to store a replaced variable in an external context,
           * we need to generate an instance and transfer it out.
           */
          final ValueHolderSub sub = oldToNew.get(value.getIndex());
          final int additionalStack = sub.transferToExternal(adder, owner, name, desc);
          if (additionalStack > stackIncrease) {
            stackIncrease = additionalStack;
          }
          return;
        }
      }
      // $FALL-THROUGH$

    case Opcodes.GETFIELD:
      // if falling through from PUTFIELD, this gets the 2nd item down
      value = peekFromTop(stackDepth);
      if (value != null) {
        if (filterReplacement(value) != null) {
          final ValueHolderSub sub = oldToNew.get(value.getIndex());
          if (sub != null) {
            sub.addInsn(name, this, opcode);
            return;
          }
        }
      }
      /* FALLTHROUGH */
    }

    // if we get here, emit the field reference as-is
    super.visitFieldInsn(opcode, owner, name, desc);
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
    /*
     * This method was deprecated in the switch from api version ASM4 to ASM5.
     * If we ever go back (via CompilationConfig.ASM_API_VERSION), we need to
     * duplicate the work from the other overloaded version of this method.
     */
    assert CompilationConfig.ASM_API_VERSION == Opcodes.ASM4;
    throw new RuntimeException("this method is deprecated");
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
    // this version of visitMethodInsn() came after ASM4
    assert CompilationConfig.ASM_API_VERSION != Opcodes.ASM4;

    final int argCount = Type.getArgumentTypes(desc).length;
    if (opcode != Opcodes.INVOKESTATIC) {
      final ReplacingBasicValue thisRef = peekFromTop(argCount);

      if (thisRef != null) {
        /*
         * If the this reference is a holder, we need to initialize the variables
         * that replaced it; that amounts to replacing its constructor call with
         * variable initializations.
         */
        if (name.equals("<init>")) {
          oldToNew.get(thisRef.getIndex()).init(adder);
          return;
        } else {
          /*
           * This is disallowed because the holder variables are being ignored in
           * favor of the locals we've replaced them with.
           */
          throw new IllegalStateException("You can't call a method on a value holder.");
        }
      }
    }

    /*
     * If we got here, we're not calling a method on a holder.
     *
     * Does the function being called return a holder?
     */
    if (Type.getReturnType(desc) != Type.VOID_TYPE) {
      ReplacingBasicValue functionReturn = getFunctionReturn();
      if (functionReturn != null) {
        /*
         * The return of this method is an actual instance of the object we're escaping.
         * Update so that it gets mapped correctly.
         */
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        functionReturn.markFunctionReturn();
        return;
      }
    }

    /*
     * Holders can't be passed as arguments to methods, because their contents aren't
     * maintained; we use the locals instead. Therefore, complain if any arguments are holders.
     */
    for (int argDepth = argCount - 1; argDepth >= 0; --argDepth) {
      ReplacingBasicValue argValue = peekFromTop(argDepth);
      if (argValue != null) {
        throw new IllegalStateException(
            String.format("Holder types are not allowed to be passed between methods. " +
                "Ran across problem attempting to invoke method '%s' on line number %d",
                name, lastLineNumber));
      }
    }

    // if we get here, emit this function call as it was
    super.visitMethodInsn(opcode, owner, name, desc, itf);
  }

  @Override
  public void visitEnd() {
    if (logger.isTraceEnabled()) {
      final StringBuilder sb = new StringBuilder();
      sb.append("InstructionModifier ");
      sb.append(name);
      sb.append(' ');
      sb.append(signature);
      sb.append('\n');
      if ((desc != null) && !desc.isEmpty()) {
        sb.append("  desc: ");
        sb.append(desc);
        sb.append('\n');
      }

      int idenId = 0; // used to generate unique ids for the ValueHolderIden's
      int itemCount = 0; // counts up the number of items found
      final HashMap<ValueHolderIden, Integer> seenIdens = new HashMap<>(); // iden -> idenId
      sb.append(" .oldToNew:\n");
      for (final IntObjectCursor<ValueHolderIden.ValueHolderSub> ioc : oldToNew) {
        final ValueHolderIden iden = ioc.value.iden();
        if (!seenIdens.containsKey(iden)) {
          seenIdens.put(iden, ++idenId);
          sb.append("ValueHolderIden[" + idenId + "]:\n");
          iden.dump(sb);
        }

        sb.append("  " + ioc.key + " => " + ioc.value + '[' + seenIdens.get(iden) + "]\n");
        ++itemCount;
      }

      sb.append(" .oldLocalToFirst:\n");
      for (final IntIntCursor iic : oldLocalToFirst) {
        sb.append("  " + iic.key + " => " + iic.value + '\n');
        ++itemCount;
      }

      if (itemCount > 0) {
        logger.debug(sb.toString());
      }
    }

    super.visitEnd();
  }
}
