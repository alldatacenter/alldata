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
package org.apache.drill.exec.udf.dynamic;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.common.config.ConfigConstants;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.exception.VersionMismatchException;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.registry.LocalFunctionRegistry;
import org.apache.drill.exec.expr.fn.registry.RemoteFunctionRegistry;
import org.apache.drill.exec.proto.UserBitShared.Jar;
import org.apache.drill.exec.proto.UserBitShared.Registry;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.sys.store.DataChangeVersion;
import org.apache.drill.exec.util.JarUtil;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.TestBuilder;
import org.apache.hadoop.fs.FileSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.apache.drill.test.HadoopUtils.hadoopToJavaPath;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Category({SlowTest.class, SqlFunctionTest.class})
public class TestDynamicUDFSupport extends BaseTestQuery {

  private static final String DEFAULT_JAR_NAME = "drill-custom-lower";
  private static URI fsUri;
  private static File jarsDir;
  private static File buildDirectory;
  private static JarBuilder jarBuilder;
  private static String defaultBinaryJar;
  private static String defaultSourceJar;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void buildAndStoreDefaultJars() throws IOException {
    jarsDir = dirTestWatcher.makeSubDir(Paths.get("jars"));
    buildDirectory = dirTestWatcher.makeSubDir(Paths.get("drill-udf"));

    jarBuilder = new JarBuilder("src/test/resources/drill-udf");
    defaultBinaryJar = buildJars(DEFAULT_JAR_NAME, "**/CustomLowerFunction.java", null);
    defaultSourceJar = JarUtil.getSourceName(defaultBinaryJar);

    FileUtils.copyFileToDirectory(new File(buildDirectory, defaultBinaryJar), jarsDir);
    FileUtils.copyFileToDirectory(new File(buildDirectory, defaultSourceJar), jarsDir);
  }

  @Before
  public void setupNewDrillbit() throws Exception {
    updateTestCluster(1, config);
    fsUri = getLocalFileSystem().getUri();
  }

  @After
  public void cleanup() throws Exception {
    closeClient();
    dirTestWatcher.clear();
  }

  @Test
  public void testSyntax() throws Exception {
    test("create function using jar 'jar_name.jar'");
    test("drop function using jar 'jar_name.jar'");
  }

  @Test
  public void testEnableDynamicSupport() throws Exception {
    try {
      test("alter system set `exec.udf.enable_dynamic_support` = true");
      test("create function using jar 'jar_name.jar'");
      test("drop function using jar 'jar_name.jar'");
    } finally {
      test("alter system reset `exec.udf.enable_dynamic_support`");
    }
  }

  @Test
  public void testDisableDynamicSupportCreate() throws Exception {
    try {
      test("alter system set `exec.udf.enable_dynamic_support` = false");
      String query = "create function using jar 'jar_name.jar'";
      thrown.expect(UserRemoteException.class);
      thrown.expectMessage(containsString("Dynamic UDFs support is disabled."));
      test(query);
    } finally {
      test("alter system reset `exec.udf.enable_dynamic_support`");
    }
  }

  @Test
  public void testDisableDynamicSupportDrop() throws Exception {
    try {
      test("alter system set `exec.udf.enable_dynamic_support` = false");
      String query = "drop function using jar 'jar_name.jar'";
      thrown.expect(UserRemoteException.class);
      thrown.expectMessage(containsString("Dynamic UDFs support is disabled."));
      test(query);
    } finally {
      test("alter system reset `exec.udf.enable_dynamic_support`");
    }
  }

  @Test
  public void testAbsentBinaryInStaging() throws Exception {
    Path staging = hadoopToJavaPath(getDrillbitContext().getRemoteFunctionRegistry().getStagingArea());

    String summary = String.format("File %s does not exist on file system %s",
        staging.resolve(defaultBinaryJar).toUri().getPath(), fsUri);

    testBuilder()
        .sqlQuery("create function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, summary)
        .go();
  }

  @Test
  public void testAbsentSourceInStaging() throws Exception {
    Path staging = hadoopToJavaPath(getDrillbitContext().getRemoteFunctionRegistry().getStagingArea());
    copyJar(jarsDir.toPath(), staging, defaultBinaryJar);

    String summary = String.format("File %s does not exist on file system %s",
        staging.resolve(defaultSourceJar).toUri().getPath(), fsUri);

    testBuilder()
        .sqlQuery("create function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, summary)
        .go();
  }

  @Test
  public void testJarWithoutMarkerFile() throws Exception {
    String jarName = "drill-no-marker";
    String jar = buildAndCopyJarsToStagingArea(jarName, null, "**/dummy.conf");

    String summary = "Marker file %s is missing in %s";

    testBuilder()
        .sqlQuery("create function using jar '%s'", jar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format(summary,
            ConfigConstants.DRILL_JAR_MARKER_FILE_RESOURCE_PATHNAME, jar))
        .go();
  }

  @Test
  public void testJarWithoutFunctions() throws Exception {
    String jarName = "drill-no-functions";
    String jar = buildAndCopyJarsToStagingArea(jarName, "**/CustomLowerDummyFunction.java", null);

    String summary = "Jar %s does not contain functions";

    testBuilder()
        .sqlQuery("create function using jar '%s'", jar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format(summary, jar))
        .go();
  }

  @Test
  public void testSuccessfulRegistration() throws Exception {
    copyDefaultJarsToStagingArea();

    String summary = "The following UDFs in jar %s have been registered:\n" +
        "[custom_lower(VARCHAR-REQUIRED)]";

    testBuilder()
        .sqlQuery("create function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format(summary, defaultBinaryJar))
        .go();

    RemoteFunctionRegistry remoteFunctionRegistry = getDrillbitContext().getRemoteFunctionRegistry();
    FileSystem fs = remoteFunctionRegistry.getFs();

    assertFalse("Staging area should be empty", fs.listFiles(remoteFunctionRegistry.getStagingArea(), false).hasNext());
    assertFalse("Temporary area should be empty", fs.listFiles(remoteFunctionRegistry.getTmpArea(), false).hasNext());

    Path path = hadoopToJavaPath(remoteFunctionRegistry.getRegistryArea());

    assertTrue("Binary should be present in registry area",
      path.resolve(defaultBinaryJar).toFile().exists());
    assertTrue("Source should be present in registry area",
      path.resolve(defaultBinaryJar).toFile().exists());

    Registry registry = remoteFunctionRegistry.getRegistry(new DataChangeVersion());
    assertEquals("Registry should contain one jar", registry.getJarList().size(), 1);
    assertEquals(registry.getJar(0).getName(), defaultBinaryJar);
  }

  @Test
  public void testDuplicatedJarInRemoteRegistry() throws Exception {
    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);
    copyDefaultJarsToStagingArea();

    String summary = "Jar with %s name has been already registered";

    testBuilder()
        .sqlQuery("create function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format(summary, defaultBinaryJar))
        .go();
  }

  @Test
  public void testDuplicatedJarInLocalRegistry() throws Exception {
    String jarName = "drill-custom-upper";
    String jar = buildAndCopyJarsToStagingArea(jarName, "**/CustomUpperFunction.java", null);

    test("create function using jar '%s'", jar);
    test("select custom_upper('A') from (values(1))");

    copyJarsToStagingArea(buildDirectory.toPath(), jar,JarUtil.getSourceName(jar));

    String summary = "Jar with %s name has been already registered";

    testBuilder()
        .sqlQuery("create function using jar '%s'", jar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format(summary, jar))
        .go();
  }

  @Test
  public void testDuplicatedFunctionsInRemoteRegistry() throws Exception {
    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);

    String jarName = "drill-custom-lower-copy";
    String jar = buildAndCopyJarsToStagingArea(jarName, "**/CustomLowerFunction.java", null);

    String summary = "Found duplicated function in %s: custom_lower(VARCHAR-REQUIRED)";

    testBuilder()
        .sqlQuery("create function using jar '%s'", jar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format(summary, defaultBinaryJar))
        .go();
  }

  @Test
  public void testDuplicatedFunctionsInLocalRegistry() throws Exception {
    String jarName = "drill-lower";
    String jar = buildAndCopyJarsToStagingArea(jarName, "**/LowerFunction.java", null);

    String summary = "Found duplicated function in %s: lower(VARCHAR-REQUIRED)";

    testBuilder()
        .sqlQuery("create function using jar '%s'", jar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format(summary, LocalFunctionRegistry.BUILT_IN))
        .go();
  }

  @Test
  public void testSuccessfulRegistrationAfterSeveralRetryAttempts() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();
    Path registryPath = hadoopToJavaPath(remoteFunctionRegistry.getRegistryArea());
    Path stagingPath = hadoopToJavaPath(remoteFunctionRegistry.getStagingArea());
    Path tmpPath = hadoopToJavaPath(remoteFunctionRegistry.getTmpArea());

    copyDefaultJarsToStagingArea();

    doThrow(new VersionMismatchException("Version mismatch detected", 1))
            .doThrow(new VersionMismatchException("Version mismatch detected", 1))
            .doCallRealMethod()
            .when(remoteFunctionRegistry).updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    String summary = "The following UDFs in jar %s have been registered:\n" +
            "[custom_lower(VARCHAR-REQUIRED)]";

    testBuilder()
            .sqlQuery("create function using jar '%s'", defaultBinaryJar)
            .unOrdered()
            .baselineColumns("ok", "summary")
            .baselineValues(true, String.format(summary, defaultBinaryJar))
            .go();

    verify(remoteFunctionRegistry, times(3))
            .updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    assertTrue("Staging area should be empty", ArrayUtils.isEmpty(stagingPath.toFile().listFiles()));
    assertTrue("Temporary area should be empty", ArrayUtils.isEmpty(tmpPath.toFile().listFiles()));

    assertTrue("Binary should be present in registry area",
      registryPath.resolve(defaultBinaryJar).toFile().exists());
    assertTrue("Source should be present in registry area",
      registryPath.resolve(defaultSourceJar).toFile().exists());

    Registry registry = remoteFunctionRegistry.getRegistry(new DataChangeVersion());
    assertEquals("Registry should contain one jar", registry.getJarList().size(), 1);
    assertEquals(registry.getJar(0).getName(), defaultBinaryJar);
  }

  @Test
  public void testSuccessfulUnregistrationAfterSeveralRetryAttempts() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();
    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);

    reset(remoteFunctionRegistry);
    doThrow(new VersionMismatchException("Version mismatch detected", 1))
            .doThrow(new VersionMismatchException("Version mismatch detected", 1))
            .doCallRealMethod()
            .when(remoteFunctionRegistry).updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    String summary = "The following UDFs in jar %s have been unregistered:\n" +
            "[custom_lower(VARCHAR-REQUIRED)]";

    testBuilder()
            .sqlQuery("drop function using jar '%s'", defaultBinaryJar)
            .unOrdered()
            .baselineColumns("ok", "summary")
            .baselineValues(true, String.format(summary, defaultBinaryJar))
            .go();

    verify(remoteFunctionRegistry, times(3))
            .updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    FileSystem fs = remoteFunctionRegistry.getFs();

    assertFalse("Registry area should be empty", fs.listFiles(remoteFunctionRegistry.getRegistryArea(), false).hasNext());
    assertEquals("Registry should be empty",
        remoteFunctionRegistry.getRegistry(new DataChangeVersion()).getJarList().size(), 0);
  }

  @Test
  public void testExceedRetryAttemptsDuringRegistration() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();
    Path registryPath = hadoopToJavaPath(remoteFunctionRegistry.getRegistryArea());
    Path stagingPath = hadoopToJavaPath(remoteFunctionRegistry.getStagingArea());
    Path tmpPath = hadoopToJavaPath(remoteFunctionRegistry.getTmpArea());

    copyDefaultJarsToStagingArea();

    doThrow(new VersionMismatchException("Version mismatch detected", 1))
        .when(remoteFunctionRegistry).updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    String summary = "Failed to update remote function registry. Exceeded retry attempts limit.";

    testBuilder()
        .sqlQuery("create function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, summary)
        .go();

    verify(remoteFunctionRegistry, times(remoteFunctionRegistry.getRetryAttempts() + 1))
        .updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    assertTrue("Binary should be present in staging area",
            stagingPath.resolve(defaultBinaryJar).toFile().exists());
    assertTrue("Source should be present in staging area",
            stagingPath.resolve(defaultSourceJar).toFile().exists());

    assertTrue("Registry area should be empty", ArrayUtils.isEmpty(registryPath.toFile().listFiles()));
    assertTrue("Temporary area should be empty", ArrayUtils.isEmpty(tmpPath.toFile().listFiles()));

    assertEquals("Registry should be empty",
        remoteFunctionRegistry.getRegistry(new DataChangeVersion()).getJarList().size(), 0);
  }

  @Test
  public void testExceedRetryAttemptsDuringUnregistration() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();
    Path registryPath = hadoopToJavaPath(remoteFunctionRegistry.getRegistryArea());

    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);

    reset(remoteFunctionRegistry);
    doThrow(new VersionMismatchException("Version mismatch detected", 1))
        .when(remoteFunctionRegistry).updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    String summary = "Failed to update remote function registry. Exceeded retry attempts limit.";

    testBuilder()
        .sqlQuery("drop function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, summary)
        .go();

    verify(remoteFunctionRegistry, times(remoteFunctionRegistry.getRetryAttempts() + 1))
        .updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    assertTrue("Binary should be present in registry area",
      registryPath.resolve(defaultBinaryJar).toFile().exists());
    assertTrue("Source should be present in registry area",
      registryPath.resolve(defaultSourceJar).toFile().exists());

    Registry registry = remoteFunctionRegistry.getRegistry(new DataChangeVersion());
    assertEquals("Registry should contain one jar", registry.getJarList().size(), 1);
    assertEquals(registry.getJar(0).getName(), defaultBinaryJar);
  }

  @Test
  public void testLazyInit() throws Exception {
    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString("No match found for function signature custom_lower(<CHARACTER>)"));
    test("select custom_lower('A') from (values(1))");

    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);
    testBuilder()
        .sqlQuery("select custom_lower('A') as res from (values(1))")
        .unOrdered()
        .baselineColumns("res")
        .baselineValues("a")
        .go();

    Path localUdfDirPath = hadoopToJavaPath((org.apache.hadoop.fs.Path)FieldUtils.readField(
      getDrillbitContext().getFunctionImplementationRegistry(), "localUdfDir", true));

    assertTrue("Binary should exist in local udf directory",
      localUdfDirPath.resolve(defaultBinaryJar).toFile().exists());
    assertTrue("Source should exist in local udf directory",
      localUdfDirPath.resolve(defaultSourceJar).toFile().exists());
  }

  @Test
  public void testLazyInitWhenDynamicUdfSupportIsDisabled() throws Exception {
    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString("No match found for function signature custom_lower(<CHARACTER>)"));
    test("select custom_lower('A') from (values(1))");

    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);

    try {
      testBuilder()
          .sqlQuery("select custom_lower('A') as res from (values(1))")
          .optionSettingQueriesForTestQuery("alter system set `exec.udf.enable_dynamic_support` = false")
          .unOrdered()
          .baselineColumns("res")
          .baselineValues("a")
          .go();
    } finally {
      test("alter system reset `exec.udf.enable_dynamic_support`");
    }
  }

  @Test
  public void testOverloadedFunctionPlanningStage() throws Exception {
    String jarName = "drill-custom-abs";
    String jar = buildAndCopyJarsToStagingArea(jarName, "**/CustomAbsFunction.java", null);

    test("create function using jar '%s'", jar);

    testBuilder()
        .sqlQuery("select abs('A', 'A') as res from (values(1))")
        .unOrdered()
        .baselineColumns("res")
        .baselineValues("ABS was overloaded. Input: A, A")
        .go();
  }

  @Test
  public void testOverloadedFunctionExecutionStage() throws Exception {
    String jarName = "drill-custom-log";
    String jar = buildAndCopyJarsToStagingArea(jarName, "**/CustomLogFunction.java", null);

    test("create function using jar '%s'", jar);

    testBuilder()
        .sqlQuery("select log('A') as res from (values(1))")
        .unOrdered()
        .baselineColumns("res")
        .baselineValues("LOG was overloaded. Input: A")
        .go();
  }

  @Test
  public void testDropFunction() throws Exception {
    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);
    test("select custom_lower('A') from (values(1))");

    Path localUdfDirPath = hadoopToJavaPath((org.apache.hadoop.fs.Path)FieldUtils.readField(
        getDrillbitContext().getFunctionImplementationRegistry(), "localUdfDir", true));

    assertTrue("Binary should exist in local udf directory",
      localUdfDirPath.resolve(defaultBinaryJar).toFile().exists());
    assertTrue("Source should exist in local udf directory",
      localUdfDirPath.resolve(defaultSourceJar).toFile().exists());

    String summary = "The following UDFs in jar %s have been unregistered:\n" +
        "[custom_lower(VARCHAR-REQUIRED)]";

    testBuilder()
        .sqlQuery("drop function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format(summary, defaultBinaryJar))
        .go();

    try {
      test("select custom_lower('A') from (values(1))");
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("No match found for function signature custom_lower(<CHARACTER>)"));
    }

    RemoteFunctionRegistry remoteFunctionRegistry = getDrillbitContext().getRemoteFunctionRegistry();
    Path registryPath = hadoopToJavaPath(remoteFunctionRegistry.getRegistryArea());

    assertEquals("Remote registry should be empty",
        remoteFunctionRegistry.getRegistry(new DataChangeVersion()).getJarList().size(), 0);

    assertFalse("Binary should not be present in registry area",
      registryPath.resolve(defaultBinaryJar).toFile().exists());
    assertFalse("Source should not be present in registry area",
      registryPath.resolve(defaultSourceJar).toFile().exists());

    assertFalse("Binary should not be present in local udf directory",
      localUdfDirPath.resolve(defaultBinaryJar).toFile().exists());
    assertFalse("Source should not be present in local udf directory",
      localUdfDirPath.resolve(defaultSourceJar).toFile().exists());
  }

  @Test
  public void testReRegisterTheSameJarWithDifferentContent() throws Exception {
    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);
    testBuilder()
        .sqlQuery("select custom_lower('A') as res from (values(1))")
        .unOrdered()
        .baselineColumns("res")
        .baselineValues("a")
        .go();
    test("drop function using jar '%s'", defaultBinaryJar);

    Thread.sleep(1000);

    buildAndCopyJarsToStagingArea(DEFAULT_JAR_NAME, "**/CustomLowerFunctionV2.java", null);

    test("create function using jar '%s'", defaultBinaryJar);
    testBuilder()
        .sqlQuery("select custom_lower('A') as res from (values(1))")
        .unOrdered()
        .baselineColumns("res")
        .baselineValues("a_v2")
        .go();
  }

  @Test
  public void testDropAbsentJar() throws Exception {
    String summary = "Jar %s is not registered in remote registry";

    testBuilder()
        .sqlQuery("drop function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format(summary, defaultBinaryJar))
        .go();
  }

  @Test
  public void testRegistrationFailDuringRegistryUpdate() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();
    Path registryPath = hadoopToJavaPath(remoteFunctionRegistry.getRegistryArea());
    Path stagingPath = hadoopToJavaPath(remoteFunctionRegistry.getStagingArea());
    Path tmpPath = hadoopToJavaPath(remoteFunctionRegistry.getTmpArea());

    final String errorMessage = "Failure during remote registry update.";
    doAnswer(invocation -> {
      assertTrue("Binary should be present in registry area",
          registryPath.resolve(defaultBinaryJar).toFile().exists());
      assertTrue("Source should be present in registry area",
          registryPath.resolve(defaultSourceJar).toFile().exists());
      throw new RuntimeException(errorMessage);
    }).when(remoteFunctionRegistry).updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    copyDefaultJarsToStagingArea();

    testBuilder()
        .sqlQuery("create function using jar '%s'", defaultBinaryJar)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, errorMessage)
        .go();

    assertTrue("Registry area should be empty", ArrayUtils.isEmpty(registryPath.toFile().listFiles()));
    assertTrue("Temporary area should be empty", ArrayUtils.isEmpty(tmpPath.toFile().listFiles()));

    assertTrue("Binary should be present in staging area", stagingPath.resolve(defaultBinaryJar).toFile().exists());
    assertTrue("Source should be present in staging area", stagingPath.resolve(defaultSourceJar).toFile().exists());
  }

  @Test
  public void testConcurrentRegistrationOfTheSameJar() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    doAnswer(invocation -> {
      String result = (String) invocation.callRealMethod();
      latch2.countDown();
      latch1.await();
      return result;
    })
        .doCallRealMethod()
        .doCallRealMethod()
        .when(remoteFunctionRegistry).addToJars(anyString(), any(RemoteFunctionRegistry.Action.class));


    final String query = String.format("create function using jar '%s'", defaultBinaryJar);

    Thread thread = new Thread(new SimpleQueryRunner(query));
    thread.start();
    latch2.await();

    try {
      String summary = "Jar with %s name is used. Action: REGISTRATION";

      testBuilder()
          .sqlQuery(query)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(false, String.format(summary, defaultBinaryJar))
          .go();

      testBuilder()
          .sqlQuery("drop function using jar '%s'", defaultBinaryJar)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(false, String.format(summary, defaultBinaryJar))
          .go();

    } finally {
      latch1.countDown();
      thread.join();
    }
  }

  @Test
  public void testConcurrentRemoteRegistryUpdateWithDuplicates() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final CountDownLatch latch3 = new CountDownLatch(1);

    doAnswer(invocation -> {
      latch3.countDown();
      latch1.await();
      invocation.callRealMethod();
      latch2.countDown();
      return null;
    }).doAnswer(invocation -> {
      latch1.countDown();
      latch2.await();
      invocation.callRealMethod();
      return null;
    })
        .when(remoteFunctionRegistry).updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    final String jar1 = defaultBinaryJar;
    copyDefaultJarsToStagingArea();

    final String copyJarName = "drill-custom-lower-copy";
    final String jar2 = buildAndCopyJarsToStagingArea(copyJarName, "**/CustomLowerFunction.java", null);

    final String query = "create function using jar '%s'";

    Thread thread1 = new Thread(new TestBuilderRunner(
        testBuilder()
        .sqlQuery(query, jar1)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true,
            String.format("The following UDFs in jar %s have been registered:\n" +
            "[custom_lower(VARCHAR-REQUIRED)]", jar1))
    ));

    Thread thread2 = new Thread(new TestBuilderRunner(
        testBuilder()
            .sqlQuery(query, jar2)
            .unOrdered()
            .baselineColumns("ok", "summary")
            .baselineValues(false,
                String.format("Found duplicated function in %s: custom_lower(VARCHAR-REQUIRED)", jar1))
    ));

    thread1.start();
    latch3.await();
    thread2.start();

    thread1.join();
    thread2.join();

    DataChangeVersion version = new DataChangeVersion();
    Registry registry = remoteFunctionRegistry.getRegistry(version);
    assertEquals("Remote registry version should match", 2, version.getVersion());
    List<Jar> jarList = registry.getJarList();
    assertEquals("Only one jar should be registered", 1, jarList.size());
    assertEquals("Jar name should match", jar1, jarList.get(0).getName());

    verify(remoteFunctionRegistry, times(2)).updateRegistry(any(Registry.class), any(DataChangeVersion.class));
  }

  @Test
  public void testConcurrentRemoteRegistryUpdateForDifferentJars() throws Exception {
    RemoteFunctionRegistry remoteFunctionRegistry = spyRemoteFunctionRegistry();
    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(2);

    doAnswer(invocation -> {
      latch2.countDown();
      latch1.await();
      invocation.callRealMethod();
      return null;
    })
        .when(remoteFunctionRegistry).updateRegistry(any(Registry.class), any(DataChangeVersion.class));

    final String jar1 = defaultBinaryJar;
    copyDefaultJarsToStagingArea();

    final String upperJarName = "drill-custom-upper";
    final String jar2 = buildAndCopyJarsToStagingArea(upperJarName, "**/CustomUpperFunction.java", null);

    final String query = "create function using jar '%s'";

    Thread thread1 = new Thread(new TestBuilderRunner(
        testBuilder()
            .sqlQuery(query, jar1)
            .unOrdered()
            .baselineColumns("ok", "summary")
            .baselineValues(true,
                String.format("The following UDFs in jar %s have been registered:\n" +
                    "[custom_lower(VARCHAR-REQUIRED)]", jar1))
    ));


    Thread thread2 = new Thread(new TestBuilderRunner(
        testBuilder()
            .sqlQuery(query, jar2)
            .unOrdered()
            .baselineColumns("ok", "summary")
            .baselineValues(true, String.format("The following UDFs in jar %s have been registered:\n" +
                "[custom_upper(VARCHAR-REQUIRED)]", jar2))
    ));

    thread1.start();
    thread2.start();

    latch2.await();
    latch1.countDown();

    thread1.join();
    thread2.join();

    DataChangeVersion version = new DataChangeVersion();
    Registry registry = remoteFunctionRegistry.getRegistry(version);
    assertEquals("Remote registry version should match", 3, version.getVersion());

    List<Jar> actualJars = registry.getJarList();
    List<String> expectedJars = Lists.newArrayList(jar1, jar2);

    assertEquals("Only one jar should be registered", 2, actualJars.size());
    for (Jar jar : actualJars) {
      assertTrue("Jar should be present in remote function registry", expectedJars.contains(jar.getName()));
    }

    verify(remoteFunctionRegistry, times(3)).updateRegistry(any(Registry.class), any(DataChangeVersion.class));
  }

  @Test
  public void testLazyInitConcurrent() throws Exception {
    FunctionImplementationRegistry functionImplementationRegistry = spyFunctionImplementationRegistry();
    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final String query = "select custom_lower('A') from (values(1))";

    doAnswer(invocation -> {
      latch1.await();
      boolean result = (boolean) invocation.callRealMethod();
      assertTrue("syncWithRemoteRegistry() should return true", result);
      latch2.countDown();
      return true;
    })
        .doAnswer(invocation -> {
          latch1.countDown();
          latch2.await();
          boolean result = (boolean) invocation.callRealMethod();
          assertTrue("syncWithRemoteRegistry() should return true", result);
          return true;
        })
        .when(functionImplementationRegistry).syncWithRemoteRegistry(anyInt());

    SimpleQueryRunner simpleQueryRunner = new SimpleQueryRunner(query);
    Thread thread1 = new Thread(simpleQueryRunner);
    Thread thread2 = new Thread(simpleQueryRunner);

    thread1.start();
    thread2.start();

    thread1.join();
    thread2.join();

    verify(functionImplementationRegistry, times(2)).syncWithRemoteRegistry(anyInt());
    LocalFunctionRegistry localFunctionRegistry = (LocalFunctionRegistry)FieldUtils.readField(
        functionImplementationRegistry, "localFunctionRegistry", true);
    assertEquals("Sync function registry version should match", 2, localFunctionRegistry.getVersion());
  }

  @Test
  public void testLazyInitNoReload() throws Exception {
    FunctionImplementationRegistry functionImplementationRegistry = spyFunctionImplementationRegistry();
    copyDefaultJarsToStagingArea();
    test("create function using jar '%s'", defaultBinaryJar);

    doAnswer(invocation -> {
      boolean result = (boolean) invocation.callRealMethod();
      assertTrue("syncWithRemoteRegistry() should return true", result);
      return true;
    })
        .doAnswer(invocation -> {
          boolean result = (boolean) invocation.callRealMethod();
          assertFalse("syncWithRemoteRegistry() should return false", result);
          return false;
        })
        .when(functionImplementationRegistry).syncWithRemoteRegistry(anyInt());

    test("select custom_lower('A') from (values(1))");

    try {
      test("select unknown_lower('A') from (values(1))");
      fail();
    } catch (UserRemoteException e){
      assertThat(e.getMessage(), containsString("No match found for function signature unknown_lower(<CHARACTER>)"));
    }

    verify(functionImplementationRegistry, times(2)).syncWithRemoteRegistry(anyInt());
    LocalFunctionRegistry localFunctionRegistry = (LocalFunctionRegistry)FieldUtils.readField(
        functionImplementationRegistry, "localFunctionRegistry", true);
    assertEquals("Sync function registry version should match", 2, localFunctionRegistry.getVersion());
  }

  private static String buildJars(String jarName, String includeFiles, String includeResources) {
    return jarBuilder.build(jarName, buildDirectory.getAbsolutePath(), includeFiles, includeResources);
  }

  private void copyDefaultJarsToStagingArea() throws IOException {
    copyJarsToStagingArea(jarsDir.toPath(), defaultBinaryJar, defaultSourceJar);
  }

  private String buildAndCopyJarsToStagingArea(String jarName, String includeFiles, String includeResources) throws IOException {
    String binaryJar = buildJars(jarName, includeFiles, includeResources);
    copyJarsToStagingArea(buildDirectory.toPath(), binaryJar, JarUtil.getSourceName(binaryJar));
    return binaryJar;
  }

  private void copyJarsToStagingArea(Path src, String binaryName, String sourceName) throws IOException {
    RemoteFunctionRegistry remoteFunctionRegistry = getDrillbitContext().getRemoteFunctionRegistry();

    final Path path = hadoopToJavaPath(remoteFunctionRegistry.getStagingArea());

    copyJar(src, path, binaryName);
    copyJar(src, path, sourceName);
  }

  private void copyJar(Path src, Path dest, String name) throws IOException {
    final File destFile = dest.resolve(name).toFile();
    FileUtils.deleteQuietly(destFile);
    FileUtils.copyFile(src.resolve(name).toFile(), destFile);
  }

  private RemoteFunctionRegistry spyRemoteFunctionRegistry() throws IllegalAccessException {
    FunctionImplementationRegistry functionImplementationRegistry =
        getDrillbitContext().getFunctionImplementationRegistry();
    RemoteFunctionRegistry remoteFunctionRegistry = functionImplementationRegistry.getRemoteFunctionRegistry();
    RemoteFunctionRegistry spy = spy(remoteFunctionRegistry);
    FieldUtils.writeField(functionImplementationRegistry, "remoteFunctionRegistry", spy, true);
    return spy;
  }

  private FunctionImplementationRegistry spyFunctionImplementationRegistry() throws IllegalAccessException {
    DrillbitContext drillbitContext = getDrillbitContext();
    FunctionImplementationRegistry spy = spy(drillbitContext.getFunctionImplementationRegistry());
    FieldUtils.writeField(drillbitContext, "functionRegistry", spy, true);
    return spy;
  }

  private static class SimpleQueryRunner implements Runnable {

    private final String query;

    SimpleQueryRunner(String query) {
      this.query = query;
    }

    @Override
    public void run() {
      try {
        test(query);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class TestBuilderRunner implements Runnable {

    private final TestBuilder testBuilder;

    TestBuilderRunner(TestBuilder testBuilder) {
      this.testBuilder = testBuilder;
    }

    @Override
    public void run() {
      try {
        testBuilder.go();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
