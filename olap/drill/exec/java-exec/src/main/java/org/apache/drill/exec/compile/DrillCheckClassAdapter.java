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

import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * There's a bug in ASM's CheckClassAdapter. It chokes on classes that
 * have the access bits for ACC_PROTECTED, ACC_PRIVATE, or ACC_FINAL set;
 * it appears to be assuming that it is only called on top-level classes.
 * However, when we compile classes, nested classes come out as byte arrays
 * that are otherwise indistinguishable from top-level classes', except for
 * the possible appearance of those access bits.
 *
 * <p>In order to get around this, we use the DrillCheckClassAdapter
 * instead. This strips off the offending bits before passing the class
 * to CheckClassAdapter, and then restores them before passing the class
 * on to whatever delegate the DrillCheckClassAdapter is created with.
 */
public class DrillCheckClassAdapter extends RetargetableClassVisitor {
  /*
   * CheckClassAdapter has final methods, and it's constructor checks that
   * getClass() == CheckClassAdapter.class, so you can't derive from it even to
   * reset the (protected) delegate. The delegates can't be initialized and set up
   * before the call to super(), so we're left with this implementation strategy.
   */
  private final InnerClassAccessStripper accessStripper; // removes the access bits

  /*
   * This inner class is used to restore the access bits that the
   * accessStripper has removed.
   */
  private class AccessRestorer extends ClassVisitor {
    public AccessRestorer(final int api, final ClassVisitor cv) {
      super(api, cv);
    }

    @Override
    public void visit(final int version, final int access, final String name,
        final String signature, final String superName,
        final String[] interfaces) {
      super.visit(version, accessStripper.getOriginalAccess(), name, signature,
          superName, interfaces);
    }
  }

  /**
   * See {@link org.objectweb.asm.util.CheckClassAdapter#CheckClassAdapter(ClassVisitor)}.
   */
  public DrillCheckClassAdapter(final ClassVisitor cv) {
    this(CompilationConfig.ASM_API_VERSION, cv, false);
  }

  /**
   * See {@link org.objectweb.asm.util.CheckClassAdapter#CheckClassAdapter(ClassVisitor)}.
   * @param api the api version
   */
  public DrillCheckClassAdapter(final int api, final ClassVisitor cv) {
    this(api, cv, false);
  }

  /**
   * See {@link org.objectweb.asm.util.CheckClassAdapter#CheckClassAdapter(ClassVisitor, boolean)}.
   * @param api the api version
   */
  protected DrillCheckClassAdapter(final int api, final ClassVisitor cv,
      final boolean checkDataFlow) {
    super(api);

    /*
     * We set up a chain of class visitors:
     * this -> InnerAccessStripper -> CheckClassAdapter -> AccessRestorer -> cv
     * Note the AccessRestorer references accessStripper to get the original
     * access bits; the inner class could not be constructed before the call to
     * super(), hence the need to set the delegate after that.
     */
    accessStripper = new InnerClassAccessStripper(api, new CheckClassAdapter(
        new AccessRestorer(api, cv), checkDataFlow));
    setDelegate(accessStripper);
  }

  /**
   * See {@link org.objectweb.asm.util.CheckClassAdapter#verify(ClassReader, boolean, PrintWriter)}.
   */
  public static void verify(final ClassReader cr, final boolean dump,
      final PrintWriter pw) {
    /*
     * For plain verification, we don't need to restore the original access
     * bytes the way we do when the check adapter is used as part of a chain, so
     * we can just strip it and use the ASM version directly.
     */
    final ClassWriter classWriter = new ClassWriter(0);
    cr.accept(new InnerClassAccessStripper(CompilationConfig.ASM_API_VERSION,
        classWriter), ClassReader.SKIP_DEBUG);
    final ClassReader strippedCr = new ClassReader(classWriter.toByteArray());
    CheckClassAdapter.verify(strippedCr, dump, pw);
  }
}
