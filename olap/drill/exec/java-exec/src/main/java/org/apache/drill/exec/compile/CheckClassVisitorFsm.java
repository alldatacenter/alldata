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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

/**
 * A ClassVisitor that verifies the required call sequence described in
 * http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/ClassVisitor.html .
 *
 * <p>
 * There is no CheckAnnotationVisitorFsm at this time. There is no
 * CheckFieldVisitorFsm at this time.
 */
public class CheckClassVisitorFsm extends ClassVisitor {
  /*
   * From
   * http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/ClassVisitor.html
   * "A visitor to visit a Java class. The methods of this class must be called
   * in the following order: visit [ visitSource ] [ visitOuterClass ] (
   * visitAnnotation | visitTypeAnnotation | visitAttribute )* ( visitInnerClass
   * | visitField | visitMethod )* visitEnd."
   */
  private final static FsmDescriptor fsmDescriptor = createFsmDescriptor();

  private static FsmDescriptor createFsmDescriptor() {
    final HashMap<String, Character> tokenMap = new HashMap<>();
    tokenMap.put("visit", 'v');
    tokenMap.put("visitSource", 'S');
    tokenMap.put("visitOuterClass", 'O');
    tokenMap.put("visitAnnotation", 'A');
    tokenMap.put("visitTypeAnnotation", 'T');
    tokenMap.put("visitAttribute", 'R');
    tokenMap.put("visitInnerClass", 'I');
    tokenMap.put("visitField", 'F');
    tokenMap.put("visitMethod", 'M');
    tokenMap.put("visitEnd", 'E');

    return new FsmDescriptor(tokenMap, "vS?+O?+(A|T|R)*+(I|F|M)*+E", "visitEnd");
  }

  private final FsmCursor fsmCursor;

  /**
   * See {@link org.objectweb.asm.ClassVisitor#ClassVisitor(int, ClassVisitor)}.
   */
  public CheckClassVisitorFsm(final int api, final ClassVisitor cv) {
    super(api, cv);
    fsmCursor = fsmDescriptor.createCursor();
  }

  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName, final String[] interfaces) {
    fsmCursor.transition("visit");
    super.visit(version, access, name, signature, superName, interfaces);
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
  public void visitAttribute(final Attribute attr) {
    fsmCursor.transition("visitAttribute");
    super.visitAttribute(attr);
  }

  @Override
  public void visitEnd() {
    fsmCursor.transition("visitEnd");
    fsmCursor.reset();
    super.visitEnd();
  }

  @Override
  public FieldVisitor visitField(final int access, final String name,
      final String desc, final String signature, final Object value) {
    fsmCursor.transition("visitField");
    final FieldVisitor fieldVisitor = super.visitField(access, name, desc,
        signature, value);
    return fieldVisitor; // TODO: add CheckFieldVisitorFsm
  }

  @Override
  public void visitInnerClass(final String name, final String outerName,
      final String innerName, final int access) {
    fsmCursor.transition("visitInnerClass");
    super.visitInnerClass(name, outerName, innerName, access);
  }

  @Override
  public MethodVisitor visitMethod(final int access, final String name,
      final String desc, final String signature, final String[] exceptions) {
    fsmCursor.transition("visitMethod");
    final MethodVisitor methodVisitor = super.visitMethod(access, name, desc,
        signature, exceptions);
    return new CheckMethodVisitorFsm(api, methodVisitor);
  }

  @Override
  public void visitOuterClass(final String owner, final String name,
      final String desc) {
    fsmCursor.transition("visitOuterClass");
    super.visitOuterClass(owner, name, desc);
  }

  @Override
  public void visitSource(final String source, final String debug) {
    fsmCursor.transition("visitSource");
    super.visitSource(source, debug);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(final int typeRef,
      final TypePath typePath, final String desc, final boolean visible) {
    fsmCursor.transition("visitTypeAnnotation");
    final AnnotationVisitor annotationVisitor = super.visitTypeAnnotation(
        typeRef, typePath, desc, visible);
    return annotationVisitor; // TODO: add CheckAnnotationVisitorFsm
  }
}
