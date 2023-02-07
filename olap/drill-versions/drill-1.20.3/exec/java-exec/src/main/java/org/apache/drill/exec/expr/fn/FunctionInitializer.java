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
package org.apache.drill.exec.expr.fn;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.util.DrillFileUtils;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.mortbay.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To avoid the cost of initializing all functions up front, this class contains
 * all information required to initializing a function when it is used.
 */
public class FunctionInitializer {
  private static final Logger logger = LoggerFactory.getLogger(FunctionInitializer.class);

  private final String className;
  private final ClassLoader classLoader;
  private Map<String, String> methods;
  private List<String> imports;
  private volatile boolean isLoaded;

  /**
   * @param className
   *          the fully qualified name of the class implementing the function
   * @param classLoader
   *          class loader associated with the function, is unique for each jar
   *          that holds function to prevent classpath collisions during loading
   *          an unloading jars
   */
  public FunctionInitializer(String className, ClassLoader classLoader) {
    this.className = className;
    this.classLoader = classLoader;
  }

  /**
   * @return returns class loader
   */
  public ClassLoader getClassLoader() { return classLoader; }

  /**
   * @return the fully qualified name of the class implementing the function
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return the imports of this class (for java code gen)
   */
  public List<String> getImports() {
    loadFunctionBody();
    return imports;
  }

  /**
   * @param methodName method name
   * @return the content of the method (for java code gen inlining)
   */
  public String getMethod(String methodName) {
    loadFunctionBody();
    return methods.get(methodName);
  }

  /**
   * Loads function body: methods (for instance, eval, setup, reset) and imports.
   * Loading is done once per class instance upon first function invocation.
   * Double-checked locking is used to avoid concurrency issues
   * when two threads are trying to load the function body at the same time.
   */
  private void loadFunctionBody() {
    if (isLoaded) {
      return;
    }

    synchronized (this) {
      if (isLoaded) {
        return;
      }

      logger.trace("Getting function body for the {}", className);
      try {
        final Class<?> clazz = Class.forName(className, true, classLoader);
        final CompilationUnit cu = convertToCompilationUnit(clazz);

        methods = MethodGrabbingVisitor.getMethods(cu, clazz);
        imports = ImportGrabber.getImports(cu);
        isLoaded = true;

      } catch (IOException | ClassNotFoundException e) {
        throw UserException.functionError(e)
            .message("Failure reading Function class.")
            .addContext("Function Class", className)
            .build(logger);
      }
    }
  }

  /**
   * Using class name generates path to class source code (*.java),
   * reads its content as string and parses it into {@link org.codehaus.janino.Java.CompilationUnit}.
   *
   * @param clazz function class
   * @return compilation unit
   * @throws IOException if did not find class or could not load it
   */
  @VisibleForTesting
  CompilationUnit convertToCompilationUnit(Class<?> clazz) throws IOException {
    String path = clazz.getName();
    path = path.replaceFirst("\\$.*", "");
    path = path.replace(".", DrillFileUtils.SEPARATOR);
    path = "/" + path + ".java";

    logger.trace("Loading function code from the {}", path);
    try (InputStream is = clazz.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException(String.format(
            "Failure trying to locate source code for class %s, tried to read on classpath location %s", clazz.getName(),
            path));
      }
      String body = IO.toString(is);

      // TODO: Hack to remove annotations so Janino doesn't choke. Need to reconsider this problem...
      body = body.replaceAll("@\\w+(?:\\([^\\\\]*?\\))?", "");
      try {
        return new Parser(new Scanner(null, new StringReader(body))).parseCompilationUnit();
      } catch (CompileException e) {
          throw new IOException(String.format("Failure while loading class %s.", clazz.getName()), e);
      }

    }

  }

}