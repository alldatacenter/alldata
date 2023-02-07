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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.exec.udf.dynamic.JarBuilder;
import org.apache.drill.exec.util.JarUtil;
import org.apache.drill.test.BaseTest;
import org.codehaus.janino.Java.CompilationUnit;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(SqlFunctionTest.class)
public class FunctionInitializerTest extends BaseTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String CLASS_NAME = "org.apache.drill.udf.dynamic.CustomLowerFunction";
  private static URLClassLoader classLoader;

  @BeforeClass
  public static void init() throws Exception {
    File buildDirectory = temporaryFolder.getRoot();
    String binaryName = "drill-custom-lower";

    JarBuilder jarBuilder = new JarBuilder("src/test/resources/drill-udf");
    String binaryJar = jarBuilder.build(binaryName, buildDirectory.getAbsolutePath(), "**/CustomLowerFunction.java", null);

    URL[] urls = {
      Paths.get(buildDirectory.getPath(), binaryJar).toUri().toURL(),
      Paths.get(buildDirectory.getPath(), JarUtil.getSourceName(binaryJar)).toUri().toURL()};
    classLoader = new URLClassLoader(urls);
  }

  @Test
  public void testGetImports() {
    FunctionInitializer functionInitializer = new FunctionInitializer(CLASS_NAME, classLoader);
    List<String> actualImports = functionInitializer.getImports();

    List<String> expectedImports = Lists.newArrayList(
        "import io.netty.buffer.DrillBuf;",
        "import org.apache.drill.exec.expr.DrillSimpleFunc;",
        "import org.apache.drill.exec.expr.annotations.FunctionTemplate;",
        "import org.apache.drill.exec.expr.annotations.Output;",
        "import org.apache.drill.exec.expr.annotations.Param;",
        "import org.apache.drill.exec.expr.holders.VarCharHolder;",
        "import javax.inject.Inject;"
    );

    assertEquals("List of imports should match", expectedImports, actualImports);
  }

  @Test
  public void testGetMethod() {
    FunctionInitializer functionInitializer = new FunctionInitializer(CLASS_NAME, classLoader);
    String actualMethod = functionInitializer.getMethod("eval");
    assertTrue("Method body should match", actualMethod.contains("CustomLowerFunction_eval:"));
  }

  @Test
  public void testConcurrentFunctionBodyLoad() throws Exception {
    final AtomicInteger counter = new AtomicInteger();
    final FunctionInitializer functionInitializer = new FunctionInitializer(CLASS_NAME, classLoader) {
      @Override
      CompilationUnit convertToCompilationUnit(Class<?> clazz) throws IOException {
        counter.incrementAndGet();
        return super.convertToCompilationUnit(clazz);
      }
    };

    int threadsNumber = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadsNumber);

    try {
      List<Future<String>> results = executor.invokeAll(Collections.nCopies(threadsNumber,
        (Callable<String>) () -> functionInitializer.getMethod("eval")));

      final Set<String> uniqueResults = new HashSet<>();
      for (Future<String> result : results) {
        uniqueResults.add(result.get());
      }

      assertEquals("All threads should have received the same result", 1, uniqueResults.size());
      assertEquals("Number of function body loads should match", 1, counter.intValue());

    } finally {
      executor.shutdownNow();
    }
  }
}
