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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.apache.drill.exec.compile.ClassTransformer.ClassNames;
import org.codehaus.commons.compiler.CompileException;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JDKClassCompiler extends AbstractClassCompiler {
  private static final Logger logger = LoggerFactory.getLogger(JDKClassCompiler.class);

  private final Collection<String> compilerOptions;
  private final DiagnosticListener<JavaFileObject> listener;
  private final JavaCompiler compiler;
  private final ClassLoader classLoader;

  private JDKClassCompiler(JavaCompiler compiler, ClassLoader classLoader, boolean debug) {
    super(debug);
    this.compiler = compiler;
    this.listener = new DrillDiagnosticListener();
    this.classLoader = classLoader;
    this.compilerOptions = Lists.newArrayList(this.debug ? "-g:source,lines,vars" : "-g:none");
  }

  public static JDKClassCompiler newInstance(ClassLoader classLoader, boolean debug) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new RuntimeException("JDK Java compiler not available - probably you're running Drill with a JRE and not a JDK");
    }
    return new JDKClassCompiler(compiler, classLoader, debug);
  }

  @Override
  protected byte[][] getByteCode(final ClassNames className, final String sourceCode)
      throws CompileException, IOException, ClassNotFoundException {
    return doCompile(className, sourceCode).getByteCode();
  }

  @Override
  public Map<String, byte[]> compile(final ClassNames className, final String sourceCode)
      throws CompileException, IOException, ClassNotFoundException {
    return doCompile(className, sourceCode).getClassByteCodes();
  }

  private DrillJavaFileObject doCompile(final ClassNames className, final String sourceCode)
      throws CompileException, IOException, ClassNotFoundException {
    // JavaFileManager should be closed after its usage to release all resources opened by this file manager
    try (JavaFileManager fileManager = new DrillJavaFileManager(compiler.getStandardFileManager(listener, null, Charsets.UTF_8), classLoader)) {
      // Create one Java source file in memory, which will be compiled later.
      DrillJavaFileObject compilationUnit = new DrillJavaFileObject(className.dot, sourceCode);

      CompilationTask task = compiler.getTask(null, fileManager, listener, compilerOptions, null, Collections.singleton(compilationUnit));

      // Run the compiler.
      if (!task.call()) {
        throw new CompileException("Compilation failed", null);
      } else if (!compilationUnit.isCompiled()) {
        throw new ClassNotFoundException(className + ": Class file not created by compilation.");
      }
      // all good
      return compilationUnit;
    } catch (RuntimeException rte) {
      // Unwrap the compilation exception and throw it.
      Throwable cause = rte.getCause();
      if (cause != null) {
        cause = cause.getCause();
        if (cause instanceof CompileException) {
          throw (CompileException) cause;
        }
        if (cause instanceof IOException) {
          throw (IOException) cause;
        }
      }
      throw rte;
    }
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }
}
