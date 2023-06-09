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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.drill.exec.compile.ClassTransformer.ClassSet;
import org.apache.drill.exec.compile.bytecode.ValueHolderReplacementVisitor;
import org.apache.drill.exec.compile.sig.SignatureHolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves two purposes. Renames all inner classes references to the outer class to the new name. Also adds all the
 * methods and fields of the class to merge to the class that is being visited.
 */
@SuppressWarnings("unused")
class MergeAdapter extends ClassVisitor {
  private static final Logger logger = LoggerFactory.getLogger(MergeAdapter.class);
  private final ClassNode classToMerge;
  private final ClassSet set;
  private final Set<String> mergingNames = new HashSet<>();
  private final boolean hasInit;
  private String name;

  // when more mature, consider AssertionUtil.IsAssertionsEnabled()
  private static final boolean verifyBytecode = true;

  private MergeAdapter(ClassSet set, ClassVisitor cv, ClassNode cn) {
    super(CompilationConfig.ASM_API_VERSION, cv);
    this.classToMerge = cn;
    this.set = set;

    boolean hasInit = false;
    for (MethodNode methodNode : classToMerge.methods) {
      String name = methodNode.name;
      if (name.equals("<init>")) {
        continue;
      }
      if (name.equals(SignatureHolder.DRILL_INIT_METHOD)) {
        hasInit = true;
      }
      mergingNames.add(name);
    }

    this.hasInit = hasInit;
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    if (name.startsWith(set.precompiled.slash)) {
      name = name.replace(set.precompiled.slash, set.generated.slash);
      int i = name.lastIndexOf('$');
      outerName = name.substring(0, i);
    }
    super.visitInnerClass(name, outerName, innerName, access);
  }

  // visit the class
  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    // use the access and names of the impl class.
    this.name = name;
    if (!name.contains("$")) {
      access = access ^ Modifier.ABSTRACT | Modifier.FINAL;
    }
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

    // finalize all methods.

    // skip all abstract methods as they should have implementations.
    if ((access & Modifier.ABSTRACT) != 0 || mergingNames.contains(name)) {
      return null;
    }
    if (signature != null) {
      signature = signature.replace(set.precompiled.slash, set.generated.slash);
    }

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (name.equals("<init>") && hasInit) {
      return new DrillInitMethodVisitor(this.name, mv);
    }
    return mv;
  }

  @Override
  public void visitEnd() {
    // add all the fields of the class we're going to merge.
    // Special handling for nested classes. Drill uses non-static nested
    // "inner" classes in some templates. Prior versions of Drill would
    // create the generated nested classes as static, then this line
    // would copy the "this$0" field to convert the static nested class
    // into a non-static inner class. However, that approach is not
    // compatible with plain-old Java compilation. Now, Drill generates
    // the nested classes as non-static inner classes. As a result, we
    // do not want to copy the hidden fields; we'll end up with two if
    // we do.
    classToMerge.fields.stream()
        .filter(field -> !field.name.startsWith("this$"))
        .forEach(field -> field.accept(this));

    // add all the methods that we to include.
    for (MethodNode mn : classToMerge.methods) {
      if (mn.name.equals("<init>")) {
        continue;
      }

      String[] exceptions = new String[mn.exceptions.size()];
      mn.exceptions.toArray(exceptions);
      MethodVisitor mv = cv.visitMethod(mn.access | Modifier.FINAL, mn.name, mn.desc, mn.signature, exceptions);
      if (verifyBytecode) {
        mv = new CheckMethodVisitorFsm(api, mv);
      }

      mn.instructions.resetLabels();

      // mn.accept(new RemappingMethodAdapter(mn.access, mn.desc, mv, new
      // SimpleRemapper("org.apache.drill.exec.compile.ExampleTemplate", "Bunky")));
      ClassSet top = set;
      while (top.parent != null) {
        top = top.parent;
      }
      mn.accept(new MethodRemapper(mv,
          new SimpleRemapper(top.precompiled.slash, top.generated.slash)));

    }
    super.visitEnd();
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    return super.visitField(access, name, desc, signature, value);
  }

  public static class MergedClassResult {
    public final byte[] bytes;
    public final Collection<String> innerClasses;

    public MergedClassResult(byte[] bytes, Collection<String> innerClasses) {
      this.bytes = bytes;
      this.innerClasses = innerClasses;
    }
  }

  public static MergedClassResult getMergedClass(final ClassSet set, final byte[] precompiledClass,
      ClassNode generatedClass, final boolean scalarReplace) {
    if (verifyBytecode) {
      if (!AsmUtil.isClassBytesOk(logger, "precompiledClass", precompiledClass)) {
        throw new IllegalStateException("Problem found in precompiledClass");
      }
      if ((generatedClass != null) && !AsmUtil.isClassOk(logger, "generatedClass", generatedClass)) {
        throw new IllegalStateException("Problem found in generatedClass");
      }
    }

    /*
     * Setup adapters for merging, remapping class names and class writing. This is done in
     * reverse order of how they will be evaluated.
     */
    final RemapClasses re = new RemapClasses(set);
    try {
      if (scalarReplace && generatedClass != null) {
        if (logger.isDebugEnabled()) {
          AsmUtil.logClass(logger, "generated " + set.generated.dot, generatedClass);
        }

        final ClassNode generatedMerged = new ClassNode();
        ClassVisitor mergeGenerator = generatedMerged;
        if (verifyBytecode) {
          mergeGenerator = new DrillCheckClassAdapter(CompilationConfig.ASM_API_VERSION,
              new CheckClassVisitorFsm(CompilationConfig.ASM_API_VERSION, generatedMerged), true);
        }

        /*
         * Even though we're effectively transforming-creating a new class in mergeGenerator,
         * there's no way to pass in ClassWriter.COMPUTE_MAXS, which would save us from having
         * to figure out stack size increases on our own. That gets handled by the
         * InstructionModifier (from inside ValueHolderReplacement > ScalarReplacementNode).
         */
        generatedClass.accept(new ValueHolderReplacementVisitor(mergeGenerator, verifyBytecode));
        if (verifyBytecode) {
          if (!AsmUtil.isClassOk(logger, "generatedMerged", generatedMerged)) {
            throw new IllegalStateException("Problem found with generatedMerged");
          }
        }
        generatedClass = generatedMerged;
      }

      final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
      ClassVisitor writerVisitor = writer;
      if (verifyBytecode) {
        writerVisitor = new DrillCheckClassAdapter(CompilationConfig.ASM_API_VERSION,
            new CheckClassVisitorFsm(CompilationConfig.ASM_API_VERSION, writerVisitor), true);
      }
      ClassVisitor remappingAdapter = new ClassRemapper(writerVisitor, re);
      if (verifyBytecode) {
        remappingAdapter = new DrillCheckClassAdapter(CompilationConfig.ASM_API_VERSION,
            new CheckClassVisitorFsm(CompilationConfig.ASM_API_VERSION, remappingAdapter), true);
      }

      ClassVisitor visitor = remappingAdapter;
      if (generatedClass != null) {
        visitor = new MergeAdapter(set, remappingAdapter, generatedClass);
      }
      ClassReader tReader = new ClassReader(precompiledClass);
      tReader.accept(visitor, ClassReader.SKIP_FRAMES);
      byte[] outputClass = writer.toByteArray();
      if (logger.isDebugEnabled()) {
        AsmUtil.logClassFromBytes(logger, "merged " + set.generated.dot, outputClass);
      }

      // enable when you want all the generated merged class files to also be written to disk.
//      try {
//        File destDir = new File( "/tmp/scratch/drill-generated-classes" );
//        destDir.mkdirs();
//        Files.write(outputClass, new File(destDir, String.format("%s-output.class", set.generated.dot)));
//      } catch (IOException e) {
//        // Ignore;
//      }

      return new MergedClassResult(outputClass, re.getInnerClasses());
    } catch (Error | RuntimeException e) {
      logger.error("Failure while merging classes.", e);
      AsmUtil.logClass(logger, "generatedClass", generatedClass);
      throw e;
    }
  }

  private static class RemapClasses extends Remapper {
    private final Set<String> innerClasses = new HashSet<>();
    private final ClassSet top;
    private final ClassSet current;

    public RemapClasses(ClassSet set) {
      current = set;
      ClassSet top = set;
      while (top.parent != null) {
        top = top.parent;
      }
      this.top = top;
    }

    @Override
    public String map(final String typeName) {
      // remap the names of all classes that start with the old class name.
      if (typeName.startsWith(top.precompiled.slash)) {
        // write down all the sub classes.
        if (typeName.startsWith(current.precompiled.slash + "$")) {
          innerClasses.add(typeName);
        }

        return typeName.replace(top.precompiled.slash, top.generated.slash);
      }

      return typeName;
    }

    public Set<String> getInnerClasses() {
      return innerClasses;
    }
  }
}
