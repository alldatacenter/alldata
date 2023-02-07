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
package org.apache.drill.exec.compile;

import java.util.HashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

/**
 * A MethodVisitor that verifies the required call sequence according to
 * http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/MethodVisitor.html .
 *
 * <p>
 * There is no CheckAnnotationVisitorFsm at this time.
 */
public class CheckMethodVisitorFsm extends MethodVisitor {
  /*
   * From
   * http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/MethodVisitor.html:
   * "The methods of this class must be called in the following order: (
   * visitParameter )* [ visitAnnotationDefault ] ( visitAnnotation |
   * visitTypeAnnotation | visitAttribute )* [ visitCode ( visitFrame |
   * visitXInsn | visitLabel | visitInsnAnnotation | visitTryCatchBlock |
   * visitTryCatchBlockAnnotation | visitLocalVariable |
   * visitLocalVariableAnnotation | visitLineNumber )* visitMaxs ] visitEnd.
   */
  private final static FsmDescriptor fsmDescriptor = createFsmDescriptor();

  private static FsmDescriptor createFsmDescriptor() {
    final HashMap<String, Character> tokenMap = new HashMap<>();
    tokenMap.put("visitParameter", 'P');
    tokenMap.put("visitAnnotationDefault", 'D');
    tokenMap.put("visitAnnotation", 'A');
    tokenMap.put("visitTypeAnnotation", 'T');
    tokenMap.put("visitAttribute", 'R');
    tokenMap.put("visitCode", 'C');
    tokenMap.put("visitFrame", 'F');
    tokenMap.put("visitXInsn", 'X'); // represents all Insn calls
    tokenMap.put("visitLabel", 'L');
    tokenMap.put("visitInsnAnnotation", 'I');
    tokenMap.put("visitTryCatchBlock", 'B');
    tokenMap.put("visitTryCatchBlockAnnotation", 'E'); // "Exception"
    tokenMap.put("visitLocalVariable", 'V');
    tokenMap.put("visitLocalVariableAnnotation", 'N'); // "Note"
    tokenMap.put("visitLineNumber", '#');
    tokenMap.put("visitMaxs", 'M');
    tokenMap.put("visitEnd", 'e');

    return new FsmDescriptor(tokenMap,
        "P*+D?+(A|T|R)*+(C(F|X|L|I|B|E|V|N|\\#)*+M)?+e", "visitEnd");
  }

  private final FsmCursor fsmCursor;

  /**
   * See {@link org.objectweb.asm.MethodVisitor#MethodVisitor(int, MethodVisitor)}.
   */
  public CheckMethodVisitorFsm(final int api, final MethodVisitor mv) {
    super(api, mv);
    fsmCursor = fsmDescriptor.createCursor();
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String desc,
      final boolean visible) {
    fsmCursor.transition("visitAnnotation");
    final AnnotationVisitor annotationVisitor = super.visitAnnotation(desc,
        visible);
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    fsmCursor.transition("visitAnnotationDefault");
    final AnnotationVisitor annotationVisitor = super.visitAnnotationDefault();
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }

  @Override
  public void visitAttribute(final Attribute attr) {
    fsmCursor.transition("visitAttribute");
    super.visitAttribute(attr);
  }

  @Override
  public void visitCode() {
    fsmCursor.transition("visitCode");
    super.visitCode();
  }

  @Override
  public void visitEnd() {
    fsmCursor.transition("visitEnd");
    super.visitEnd();
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    fsmCursor.transition("visitXInsn");
    super.visitFieldInsn(opcode, owner, name, desc);
  }

  @Override
  public void visitFrame(final int type, final int nLocal,
      final Object[] local, final int nStack, final Object[] stack) {
    fsmCursor.transition("visitFrame");
    super.visitFrame(type, nLocal, local, nStack, stack);
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    fsmCursor.transition("visitXInsn");
    super.visitIincInsn(var, increment);
  }

  @Override
  public void visitInsn(final int opcode) {
    fsmCursor.transition("visitXInsn");
    super.visitInsn(opcode);
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(final int typeRef,
      final TypePath typePath, final String desc, final boolean visible) {
    fsmCursor.transition("visitInsnAnnotation");
    final AnnotationVisitor annotationVisitor = super.visitInsnAnnotation(
        typeRef, typePath, desc, visible);
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    fsmCursor.transition("visitXInsn");
    super.visitIntInsn(opcode, operand);
  }

  @Override
  public void visitInvokeDynamicInsn(final String name, final String desc,
      final Handle bsm, final Object... bsmArgs) {
    fsmCursor.transition("visitXInsn");
    super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    fsmCursor.transition("visitXInsn");
    super.visitJumpInsn(opcode, label);
  }

  @Override
  public void visitLabel(final Label label) {
    fsmCursor.transition("visitLabel");
    super.visitLabel(label);
  }

  @Override
  public void visitLdcInsn(final Object cst) {
    fsmCursor.transition("visitXInsn");
    super.visitLdcInsn(cst);
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    fsmCursor.transition("visitLineNumber");
    super.visitLineNumber(line, start);
  }

  @Override
  public void visitLocalVariable(final String name, final String desc,
      final String signature, final Label start, final Label end,
      final int index) {
    fsmCursor.transition("visitLocalVariable");
    super.visitLocalVariable(name, desc, signature, start, end, index);
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(final int typeRef,
      final TypePath typePath, final Label[] start, final Label[] end,
      final int[] index, final String desc, final boolean visible) {
    fsmCursor.transition("visitLocalVariableAnnotation");
    final AnnotationVisitor annotationVisitor = super
        .visitLocalVariableAnnotation(typeRef, typePath, start, end, index,
            desc, visible);
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
      final Label[] labels) {
    fsmCursor.transition("visitXInsn");
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    fsmCursor.transition("visitMaxs");
    super.visitMaxs(maxStack, maxLocals);
  }

  @Deprecated
  @Override
  public void visitMethodInsn(final int opcode, final String owner,
      final String name, final String desc) {
    fsmCursor.transition("visitXInsn");
    super.visitMethodInsn(opcode, owner, name, desc);
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner,
      final String name, final String desc, final boolean itf) {
    fsmCursor.transition("visitXInsn");
    super.visitMethodInsn(opcode, owner, name, desc, itf);
  }

  @Override
  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    fsmCursor.transition("visitXInsn");
    super.visitMultiANewArrayInsn(desc, dims);
  }

  @Override
  public void visitParameter(final String name, final int access) {
    fsmCursor.transition("visitParameter");
    super.visitParameter(name, access);
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(final int parameter,
      final String desc, final boolean visible) {
    fsmCursor.transition("visitParameterAnnotation");
    final AnnotationVisitor annotationVisitor = super.visitParameterAnnotation(
        parameter, desc, visible);
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }

  @Override
  public void visitTableSwitchInsn(final int min, final int max,
      final Label dflt, final Label... labels) {
    fsmCursor.transition("visitXInsn");
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(final int typeRef,
      final TypePath typePath, final String desc, final boolean visible) {
    fsmCursor.transition("visitTryCatchAnnotation");
    final AnnotationVisitor annotationVisitor = super.visitTryCatchAnnotation(
        typeRef, typePath, desc, visible);
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }

  @Override
  public void visitTryCatchBlock(final Label start, final Label end,
      final Label handler, final String type) {
    fsmCursor.transition("visitTryCatchBlock");
    super.visitTryCatchBlock(start, end, handler, type);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(final int typeRef,
      final TypePath typePath, final String desc, final boolean visible) {
    fsmCursor.transition("visitTypeAnnotation");
    final AnnotationVisitor annotationVisitor = super.visitTypeAnnotation(
        typeRef, typePath, desc, visible);
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    fsmCursor.transition("visitXInsn");
    super.visitTypeInsn(opcode, type);
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    fsmCursor.transition("visitXInsn");
    super.visitVarInsn(opcode, var);
  }
}
