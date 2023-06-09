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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * An ASM ClassVisitor that strips class the access bits that are only possible
 * on inner classes (ACC_PROTECTED, ACC_PRIVATE, and ACC_FINAL). These bits are
 * only stripped from the class' visit() call if the class' name contains a
 * '$'.
 *
 * <p>This visitor is meant to be used on classes that will undergo validation
 * with CheckClassAdapter. CheckClassAdapter assumes it will only be called on
 * non-inner classes, and throws an IllegalArgumentException if the class is
 * protected, private, or final. However, once classes are compiled, they appear
 * in their class files alone, and these options may be present, with no way
 * for an outside observer to tell if they were originally inner classes.
 */
public class InnerClassAccessStripper extends ClassVisitor {
  private int originalClassAccess;
  private boolean accessCaptured;

  /**
   * See {@link org.objectweb.asm.ClassVisitor#ClassVisitor(int)}.
   */
  public InnerClassAccessStripper(final int api) {
    super(api);
  }

  /**
   * See {@link org.objectweb.asm.ClassVisitor#ClassVisitor(int, ClassVisitor)}.
   */
  public InnerClassAccessStripper(final int api, final ClassVisitor cv) {
    super(api, cv);
  }

  /**
   * Return the original class' access bits.
   *
   * <p>This may only be called after {@link ClassVisitor#visit(int, int, String, String, String, String[])}
   * has been called; that's where the bits are stripped and captured.
   *
   * @return the original class bits
   * @throws IllegalStateException if visit() hasn't been called yet
   */
  public int getOriginalAccess() {
    if (!accessCaptured) {
      throw new IllegalStateException(
          "can't get original access before it is captured");
    }

    return originalClassAccess;
  }

  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName, final String[] interfaces) {
    /*
     * Record the original access bits so we can restore them before the next
     * link in the visitor chain.
     */
    originalClassAccess = access;
    accessCaptured = true;

    // If we're checking an inner class, suppress access bits that ASM chokes on.
    int checkClassAccess = access;
    if (name.indexOf('$') >= 0) {
      checkClassAccess &= ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
    }
    super.visit(version, checkClassAccess, name, signature, superName, interfaces);
  }
}
