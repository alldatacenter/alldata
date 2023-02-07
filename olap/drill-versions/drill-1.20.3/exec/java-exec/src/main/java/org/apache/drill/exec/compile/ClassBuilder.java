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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.compile.ClassTransformer.ClassNames;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.server.options.OptionSet;
import org.codehaus.commons.compiler.CompileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the "plain Java" method of code generation and
 * compilation. Given a {@link CodeGenerator}, obtains the generated
 * source code, compiles it with the selected compiler, loads the
 * byte-codes into a class loader and provides the resulting
 * class. Compared with the {@link ClassTransformer} mechanism,
 * this one requires the code generator to have generated a complete
 * Java class that is capable of direct compilation and loading.
 * This means the generated class must be a subclass of the template
 * so that the JVM can use normal Java inheritance to associate the
 * template and generated methods.
 * <p>
 * Here is how to use the plain Java technique to debug
 * generated code:
 * <ul>
 * <li>Set the config option <tt>drill.exec.compile.code_dir</tt>
 * to the location where you want to save the generated source
 * code.</li>
 * <li>Where you generate code (using a {@link CodeGenerator}),
 * set the "plain Java" options:<pre>
 * CodeGenerator&lt;Foo> cg = ...
 * cg.plainJavaCapable(true); // Class supports plain Java
 * cg.preferPlainJava(true); // Actually generate plain Java
 * cg.saveCodeForDebugging(true); // Save code for debugging
 * ...</pre>
 * Note that <tt>saveCodeForDebugging</tt> automatically sets the PJ
 * option if the generator is capable. Call <tt>preferPlainJava</tt>
 * only if you want to try PJ for this particular generated class
 * without saving the generated code.</li>
 * <li>In your favorite IDE, add to the code lookup path the
 * code directory saved earlier. In Eclipse, for example, you do
 * this in the debug configuration you will use to debug Drill.</li>
 * <li>Set a breakpoint in template used for the generated code.</li>
 * <li>Run Drill. The IDE will stop at your breakpoint.</li>
 * <li>Step into the generated code. Examine class field and
 * local variables. Have fun!</li>
 * </ul>
 * <p>
 * Most generated classes have been upgraded to support Plain Java
 * compilation. Once this work is complete, the calls to
 * <tt>plainJavaCapable<tt> can be removed as all generated classes
 * will be capable.
 * <p>
 * The setting to prefer plain Java is ignored for any remaining generated
 * classes not marked as plain Java capable.
 */
public class ClassBuilder {

  private static final Logger logger = LoggerFactory.getLogger(ClassBuilder.class);
  public static final String CODE_DIR_OPTION = CodeCompiler.COMPILE_BASE + ".code_dir";

  private final DrillConfig config;
  private final OptionSet options;
  private final File codeDir;

  public ClassBuilder(DrillConfig config, OptionSet optionManager) {
    this.config = config;
    options = optionManager;

    // Code can be saved per-class to enable debugging.
    // Just request the code generator to persist code,
    // point your debugger to the directory set below, and you
    // can step into the code for debugging. Code is not saved
    // be default because doing so is expensive and unnecessary.
    codeDir = new File(config.getString(CODE_DIR_OPTION));
  }

  /**
   * Given a code generator which has already generated plain Java
   * code, compile the code, create a class loader, and return the
   * resulting Java class.
   *
   * @param cg a plain Java capable code generator that has generated
   * plain Java code
   * @return the class that the code generator defines
   * @throws ClassTransformationException
   */
  public Class<?> getImplementationClass(CodeGenerator<?> cg) throws ClassTransformationException {
    try {
      return compileClass(cg);
    } catch (CompileException | ClassNotFoundException|IOException e) {
      throw new ClassTransformationException(e);
    }
  }

  /**
   * Performs the actual work of compiling the code and loading the class.
   *
   * @param cg the code generator that has built the class(es) to be generated.
   * @return the class, after code generation and (if needed) compilation.
   * @throws IOException if an error occurs when optionally writing code to disk.
   * @throws CompileException if the generated code has compile issues.
   * @throws ClassNotFoundException if the generated code references unknown classes.
   * @throws ClassTransformationException generic "something is wrong" error from
   * Drill class compilation code.
   */
  private Class<?> compileClass(CodeGenerator<?> cg) throws IOException, CompileException, ClassNotFoundException, ClassTransformationException {
    final long t1 = System.nanoTime();

    // Get the plain Java code.
    String code = cg.getGeneratedCode();

    // Get the class names (dotted, file path, etc.)
    String className = cg.getMaterializedClassName();
    ClassTransformer.ClassNames name = new ClassTransformer.ClassNames(className);

    // A key advantage of this method is that the code can be
    // saved and debugged, if needed.
    if (cg.isCodeToBeSaved()) {
      saveCode(code, name);
    }

    Class<?> compiledClass = getCompiledClass(code, className, config, options);
    logger.debug("Compiled {}: time = {} ms.",
        className,
        (System.nanoTime() - t1 + 500_000) / 1_000_000);
    return compiledClass;
  }

  public static Class<?> getCompiledClass(String code, String className,
      DrillConfig config, OptionSet options) throws CompileException, ClassNotFoundException, ClassTransformationException, IOException {
    // Compile the code and load it into a class loader.
    CachedClassLoader classLoader = new CachedClassLoader();
    ClassCompilerSelector compilerSelector = new ClassCompilerSelector(classLoader, config, options);
    ClassNames name = new ClassNames(className);
    Map<String,byte[]> results = compilerSelector.compile(name, code);
    classLoader.addClasses(results);

    // Get the class from the class loader.
    try {
      return classLoader.findClass(className);
    } catch (ClassNotFoundException e) {
      // This should never occur.
      throw new IllegalStateException("Code load failed", e);
    }
  }

  /**
   * Save code to a predefined location for debugging. To use the code
   * for debugging, make sure the save location is on your IDE's source
   * code search path. Code is saved in usual Java format with each
   * package as a directory. The provided code directory becomes a
   * source directory, as in Maven's "src/main/java".
   *
   * @param code the source code
   * @param name the class name
   */
  private void saveCode(String code, ClassNames name) {

    String pathName = name.slash + ".java";
    File codeFile = new File(codeDir, pathName);
    codeFile.getParentFile().mkdirs();
    try (final FileWriter writer = new FileWriter(codeFile)) {
      writer.write(code);
    } catch (IOException e) {
      System.err.println("Could not save: " + codeFile.getAbsolutePath());
    }
  }
}
