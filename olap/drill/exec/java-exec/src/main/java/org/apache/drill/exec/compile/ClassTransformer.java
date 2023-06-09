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

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.MergeAdapter.MergedClassResult;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.server.options.OptionSet;
import org.codehaus.commons.compiler.CompileException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * Compiles generated code, merges the resulting class with the
 * template class, and performs byte-code cleanup on the resulting
 * byte codes. The most important transform is scalar replacement
 * which replaces occurrences of non-escaping objects with a
 * collection of member variables.
 */

public class ClassTransformer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ClassTransformer.class);

  private static final int MAX_SCALAR_REPLACE_CODE_SIZE = 2*1024*1024; // 2meg

  private final ByteCodeLoader byteCodeLoader = new ByteCodeLoader();
  private final DrillConfig config;
  private final OptionSet optionManager;

  @VisibleForTesting // although we need it even if it weren't used in testing
  public enum ScalarReplacementOption {
    OFF, // scalar replacement will not ever be used
    TRY, // scalar replacement will be attempted, and if there is an error, we fall back to not using it
    ON; // scalar replacement will always be used, and any errors cause user visible errors

    /**
     * Convert a string to an enum value.
     *
     * @param s the string
     * @return an enum value
     * @throws IllegalArgumentException if the string doesn't match any of the enum values
     */
    public static ScalarReplacementOption fromString(final String s) {
      switch(s) {
      case "off":
        return OFF;
      case "try":
        return TRY;
      case "on":
        return ON;
      default:
        throw new IllegalArgumentException("Invalid ScalarReplacementOption \"" + s + "\"");
      }
    }
  }

  public ClassTransformer(final DrillConfig config, final OptionSet optionManager) {
    this.config = config;
    this.optionManager = optionManager;
  }

  public static class ClassSet {
    public final ClassSet parent;
    public final ClassNames precompiled;
    public final ClassNames generated;

    public ClassSet(ClassSet parent, String precompiled, String generated) {
      Preconditions.checkArgument(!generated.startsWith(precompiled),
          String.format("The new name of a class cannot start with the old name of a class, otherwise class renaming will cause problems. Precompiled class name %s. Generated class name %s",
              precompiled, generated));
      this.parent = parent;
      this.precompiled = new ClassNames(precompiled);
      this.generated = new ClassNames(generated);
    }

    public ClassSet getChild(String precompiled, String generated) {
      return new ClassSet(this, precompiled, generated);
    }

    public ClassSet getChild(String precompiled) {
      return new ClassSet(this, precompiled, precompiled.replace(this.precompiled.dot, this.generated.dot));
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((generated == null) ? 0 : generated.hashCode());
      result = prime * result + ((parent == null) ? 0 : parent.hashCode());
      result = prime * result + ((precompiled == null) ? 0 : precompiled.hashCode());
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
      ClassSet other = (ClassSet) obj;
      if (generated == null) {
        if (other.generated != null) {
          return false;
        }
      } else if (!generated.equals(other.generated)) {
        return false;
      }
      if (parent == null) {
        if (other.parent != null) {
          return false;
        }
      } else if (!parent.equals(other.parent)) {
        return false;
      }
      if (precompiled == null) {
        if (other.precompiled != null) {
          return false;
        }
      } else if (!precompiled.equals(other.precompiled)) {
        return false;
      }
      return true;
    }
  }

  public static class ClassNames {
    public final String dot;
    public final String slash;
    public final String clazz;

    public ClassNames(String className) {
      dot = className;
      slash = className.replace('.', DrillFileUtils.SEPARATOR_CHAR);
      clazz = DrillFileUtils.SEPARATOR_CHAR + slash + ".class";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((dot == null) ? 0 : dot.hashCode());
      result = prime * result + ((slash == null) ? 0 : slash.hashCode());
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
      ClassNames other = (ClassNames) obj;
      if (clazz == null) {
        if (other.clazz != null) {
          return false;
        }
      } else if (!clazz.equals(other.clazz)) {
        return false;
      }
      if (dot == null) {
        if (other.dot != null) {
          return false;
        }
      } else if (!dot.equals(other.dot)) {
        return false;
      }
      if (slash == null) {
        if (other.slash != null) {
          return false;
        }
      } else if (!slash.equals(other.slash)) {
        return false;
      }
      return true;
    }
  }

  public Class<?> getImplementationClass(CodeGenerator<?> cg) throws ClassTransformationException {
    final QueryClassLoader loader = new QueryClassLoader(config, optionManager);
    return getImplementationClass(loader, cg.getDefinition(),
        cg.getGeneratedCode(), cg.getMaterializedClassName());
  }

  public Class<?> getImplementationClass(
      final QueryClassLoader classLoader,
      final TemplateClassDefinition<?> templateDefinition,
      final String entireClass,
      final String materializedClassName) throws ClassTransformationException {
    // unfortunately, this hasn't been set up at construction time, so we have to do it here
    final ScalarReplacementOption scalarReplacementOption = ScalarReplacementOption.fromString(optionManager.getOption(ExecConstants.SCALAR_REPLACEMENT_VALIDATOR));

    try {
      final long t1 = System.nanoTime();
      final ClassSet set = new ClassSet(null, templateDefinition.getTemplateClassName(), materializedClassName);
      final byte[][] implementationClasses = classLoader.getClassByteCode(set.generated, entireClass);

      long totalBytecodeSize = 0;
      Map<String, Pair<byte[], ClassNode>> classesToMerge = Maps.newHashMap();
      for (byte[] clazz : implementationClasses) {
        totalBytecodeSize += clazz.length;
        final ClassNode node = AsmUtil.classFromBytes(clazz, ClassReader.EXPAND_FRAMES);
        if (!AsmUtil.isClassOk(logger, "implementationClasses", node)) {
          throw new IllegalStateException("Problem found with implementationClasses");
        }
        classesToMerge.put(node.name, Pair.of(clazz, node));
      }

      final LinkedList<ClassSet> names = Lists.newLinkedList();
      final Set<ClassSet> namesCompleted = Sets.newHashSet();
      names.add(set);

      while ( !names.isEmpty() ) {
        final ClassSet nextSet = names.removeFirst();
        if (namesCompleted.contains(nextSet)) {
          continue;
        }
        final ClassNames nextPrecompiled = nextSet.precompiled;
        final byte[] precompiledBytes = byteCodeLoader.getClassByteCodeFromPath(nextPrecompiled.clazz);
        final ClassNames nextGenerated = nextSet.generated;
        // keeps only classes that have not be merged
        Pair<byte[], ClassNode> classNodePair = classesToMerge.remove(nextGenerated.slash);
        final ClassNode generatedNode;
        if (classNodePair != null) {
          generatedNode = classNodePair.getValue();
        } else {
          generatedNode = null;
        }

        /*
         * TODO
         * We're having a problem with some cases of scalar replacement, but we want to get
         * the code in so it doesn't rot anymore.
         *
         *  Here, we use the specified replacement option. The loop will allow us to retry if
         *  we're using TRY.
         */
        MergedClassResult result = null;
        boolean scalarReplace = scalarReplacementOption != ScalarReplacementOption.OFF && entireClass.length() < MAX_SCALAR_REPLACE_CODE_SIZE;
        while(true) {
          try {
            result = MergeAdapter.getMergedClass(nextSet, precompiledBytes, generatedNode, scalarReplace);
            break;
          } catch(RuntimeException e) {
            // if we had a problem without using scalar replacement, then rethrow
            if (!scalarReplace) {
              throw e;
            }

            // if we did try to use scalar replacement, decide if we need to retry or not
            if (scalarReplacementOption == ScalarReplacementOption.ON) {
              // option is forced on, so this is a hard error
              throw e;
            }

            /*
             * We tried to use scalar replacement, with the option to fall back to not using it.
             * Log this failure before trying again without scalar replacement.
             */
            logger.info("scalar replacement failure (retrying)\n", e);
            scalarReplace = false;
          }
        }

        for (String s : result.innerClasses) {
          s = s.replace(DrillFileUtils.SEPARATOR_CHAR, '.');
          names.add(nextSet.getChild(s));
        }
        classLoader.injectByteCode(nextGenerated.dot, result.bytes);
        namesCompleted.add(nextSet);
      }

      // adds byte code of the classes that have not been merged to make them accessible for outer class
      for (Map.Entry<String, Pair<byte[], ClassNode>> clazz : classesToMerge.entrySet()) {
        classLoader.injectByteCode(clazz.getKey().replace(DrillFileUtils.SEPARATOR_CHAR, '.'), clazz.getValue().getKey());
      }
      Class<?> c = classLoader.findClass(set.generated.dot);
      if (templateDefinition.getExternalInterface().isAssignableFrom(c)) {
        logger.debug("Compiled and merged {}: bytecode size = {}, time = {} ms.",
             c.getSimpleName(),
             DrillStringUtils.readable(totalBytecodeSize),
             (System.nanoTime() - t1 + 500_000) / 1_000_000);
        return c;
      }

      throw new ClassTransformationException("The requested class did not implement the expected interface.");
    } catch (CompileException | IOException | ClassNotFoundException e) {
      throw new ClassTransformationException(String.format("Failure generating transformation classes for value: \n %s", entireClass), e);
    }
  }
}
