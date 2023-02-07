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
package org.apache.drill.yarn.scripts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.drill.test.BaseTest;
import org.apache.drill.yarn.scripts.ScriptUtils.DrillbitRun;
import org.apache.drill.yarn.scripts.ScriptUtils.RunResult;
import org.apache.drill.yarn.scripts.ScriptUtils.ScriptRunner;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests to test the many ways that the Drill shell scripts can run.
 * Since it would be difficult to test options using the actual Drillbit, the
 * scripts make use of a special test fixture in runbit: the ability to pass
 * a "wrapper" script to run in place of the Drillit. That script probes stderr,
 * stdout and log files, and writes its arguments (which is the Drillbit launch
 * command) to a file. As a result, we can capture this output and analyze it
 * to ensure we are passing the right arguments to the Drillbit, and that output
 * is going to the right destinations.
 */

// Turned of by default: works only in a developer setup
@Ignore
public class TestScripts extends BaseTest {
  static ScriptUtils context;

  @BeforeClass
  public static void initialSetup() throws IOException {
    context = ScriptUtils.instance();
    context.initialSetup();
  }

  /**
   * Test the simplest case: use the $DRILL_HOME/conf directory and default log
   * location. Non-existent drill-env.sh and drill-config.sh files. Everything
   * is at its Drill-provided defaults. Then, try overriding each user-settable
   * environment variable in the environment (which simulates what YARN might
   * do.)
   */

  @Test
  public void testStockCombined() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    // No drill-env.sh, no distrib-env.sh

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateStockArgs();
      result.validateClassPath(ScriptUtils.stdCp);
      result.validateStdOut();
      result.validateStdErr();
      result.validateDrillLog();
    }

    // As above, but pass an argument.

    {
      String propArg = "-Dproperty=value";
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withArg(propArg).run();
      assertEquals(0, result.returnCode);
      result.validateStdOut();
      result.validateArg(propArg);
    }

    // Custom Java opts to achieve the same result

    {
      String propArg = "-Dproperty=value";
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_JAVA_OPTS", propArg).run();
      assertEquals(0, result.returnCode);
      result.validateStockArgs(); // Should not lose standard JVM args
      result.validateStdOut();
      result.validateArg(propArg);
    }

    // Custom Drillbit Java Opts to achieve the same result

    {
      String propArg = "-Dproperty2=value2";
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILLBIT_JAVA_OPTS", propArg).run();
      assertEquals(0, result.returnCode);
      result.validateStockArgs(); // Should not lose standard JVM args
      result.validateStdOut();
      result.validateArg(propArg);
    }

    // Both sets of options

    {
      String propArg = "-Dproperty=value";
      String propArg2 = "-Dproperty2=value2";
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_JAVA_OPTS", propArg)
          .addEnv("DRILLBIT_JAVA_OPTS", propArg2).run();
      assertEquals(0, result.returnCode);
      result.validateArgs(new String[] { propArg, propArg2 });
    }

    // Custom heap memory

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_HEAP", "5G").run();
      result.validateArgs(new String[] { "-Xms5G", "-Xmx5G" });
    }

    // Custom direct memory

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_MAX_DIRECT_MEMORY", "7G").run();
      result.validateArg("-XX:MaxDirectMemorySize=7G");
    }

    // Enable GC logging

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("SERVER_LOG_GC", "1").run();
      String logTail = context.testLogDir.getName() + "/drillbit.gc";
      result.validateArgRegex("-Xloggc:.*/" + logTail);
    }

    // Code cache size

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILLBIT_CODE_CACHE_SIZE", "2G").run();
      result.validateArg("-XX:ReservedCodeCacheSize=2G");
    }
  }

  /**
   * Use the "stock" setup, but add each custom bit of the class path to ensure
   * it is passed to the Drillbit.
   *
   * @throws IOException
   */

  @Test
  public void testClassPath() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    File extrasDir = context.createDir(new File(context.testDir, "extras"));
    File hadoopJar = context.makeDummyJar(extrasDir, "hadoop");
    File hbaseJar = context.makeDummyJar(extrasDir, "hbase");
    File prefixJar = context.makeDummyJar(extrasDir, "prefix");
    File cpJar = context.makeDummyJar(extrasDir, "cp");
    File extnJar = context.makeDummyJar(extrasDir, "extn");
    File toolsJar = context.makeDummyJar(extrasDir, "tools");

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_CLASSPATH_PREFIX", prefixJar.getAbsolutePath()).run();
      result.validateClassPath(prefixJar.getAbsolutePath());
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_TOOL_CP", toolsJar.getAbsolutePath()).run();
      result.validateClassPath(toolsJar.getAbsolutePath());
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("HADOOP_CLASSPATH", hadoopJar.getAbsolutePath()).run();
      result.validateClassPath(hadoopJar.getAbsolutePath());
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("HBASE_CLASSPATH", hbaseJar.getAbsolutePath()).run();
      result.validateClassPath(hbaseJar.getAbsolutePath());
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("EXTN_CLASSPATH", extnJar.getAbsolutePath()).run();
      result.validateClassPath(extnJar.getAbsolutePath());
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_CLASSPATH", cpJar.getAbsolutePath()).run();
      result.validateClassPath(cpJar.getAbsolutePath());
    }

    // Site jars not on path if not created

    File siteJars = new File(siteDir, "jars");
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN).run();
      assertFalse(result.classPathContains(siteJars.getAbsolutePath()));
    }

    // Site/jars on path if exists

    context.createDir(siteJars);
    context.makeDummyJar(siteJars, "site");

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN).run();
      result.validateClassPath(siteJars.getAbsolutePath() + "/*");
    }
  }

  /**
   * Create a custom log folder location.
   */

  @Test
  public void testLogDir() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);
    File logsDir = context.createDir(new File(context.testDir, "logs"));
    context.removeDir(new File(context.testDrillHome, "log"));

    {
      String logPath = logsDir.getAbsolutePath();
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_LOG_DIR", logPath).withLogDir(logsDir).run();
      assertEquals(0, result.returnCode);
      result.validateArgs(
          new String[] { "-Dlog.path=" + logPath + "/drillbit.log",
              "-Dlog.query.path=" + logPath + "/drillbit_queries.json", });
      result.validateStdOut();
      result.validateStdErr();
      result.validateDrillLog();
    }

  }

  /**
   * Try setting custom environment variable values in drill-env.sh in the
   * $DRILL_HOME/conf location.
   */

  @Test
  public void testDrillEnv() throws IOException {
    doEnvFileTest("drill-env.sh");
  }

  /**
   * Repeat the above test using distrib-env.sh in the $DRILL_HOME/conf
   * location.
   */

  @Test
  public void testDistribEnv() throws IOException {
    doEnvFileTest("distrib-env.sh");
  }

  /**
   * Implementation of the drill-env.sh and distrib-env.sh tests.
   */

  private void doEnvFileTest(String fileName) throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    /**
     * Set all properties in the env file.
     */

    Map<String, String> drillEnv = new HashMap<>();
    String propArg = "-Dproperty=value";
    drillEnv.put("DRILL_JAVA_OPTS", propArg);
    drillEnv.put("DRILL_HEAP", "5G");
    drillEnv.put("DRILL_MAX_DIRECT_MEMORY", "7G");
    drillEnv.put("SERVER_LOG_GC", "1");
    drillEnv.put("DRILLBIT_CODE_CACHE_SIZE", "2G");
    context.createEnvFile(new File(siteDir, fileName), drillEnv, false);

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN).run();
      assertEquals(0, result.returnCode);

      String expectedArgs[] = {
          propArg,
          "-Xms5G", "-Xmx5G",
          "-XX:MaxDirectMemorySize=7G",
          "-XX:ReservedCodeCacheSize=2G"
      };

      result.validateArgs(expectedArgs);
      String logTail = context.testLogDir.getName() + "/drillbit.gc";
      result.validateArgRegex("-Xloggc:.*/" + logTail);
    }

    // Change some drill-env.sh options in the environment.
    // The generated drill-env.sh should allow overrides.
    // (The generated form is the form that customers should use.)

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("SERVER_LOG_GC", "0")
          .addEnv("DRILL_MAX_DIRECT_MEMORY", "9G")
          .run();
      assertEquals(0, result.returnCode);
      result.validateArg("-XX:MaxDirectMemorySize=9G");
      String logTail = context.testDrillHome.getName() + "/log/drillbit.gc";
      assertFalse(result.containsArgRegex("-Xloggc:.*/" + logTail));
    }
  }

  @Test
  public void testDistribEnvWithNegativeCond() throws IOException {
    // Construct condition map
    final Map<String, String> conditions = new HashMap<>();
    conditions.put("DRILLBIT_CONTEXT", "0");
    final String expectedArgs[] = {"-XX:ReservedCodeCacheSize=1G"};
    doEnvFileWithConditionTest("distrib-env.sh", conditions, expectedArgs);
  }

  @Test
  public void testDistribEnvWithPositiveCond() throws IOException {
    // Construct condition map
    final Map<String, String> conditions = new HashMap<>();
    conditions.put("DRILLBIT_CONTEXT", "1");
    final String expectedArgs[] = {"-XX:ReservedCodeCacheSize=2G"};
    doEnvFileWithConditionTest("distrib-env.sh", conditions, expectedArgs);
  }

  /**
   * Implementation of the drill-env.sh or distrib-env.sh tests with conditions
   * guarding environment variables.
   */
  private void doEnvFileWithConditionTest(String fileName, Map<String, String> conditions,
                                          String[] expectedArgs) throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    // Set a property in the env file.
    Map<String, String> drillEnv = new HashMap<>();
    drillEnv.put("DRILLBIT_CODE_CACHE_SIZE", "2G");
    context.createEnvFileWithCondition(new File(siteDir, fileName), conditions, drillEnv, false);
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN).run();
      assertEquals(0, result.returnCode);
      result.validateArgs(expectedArgs);
    }
  }

  /**
   * Test that drill-env.sh overrides distrib-env.sh, and that the environment
   * overrides both. Assumes the basics were tested above.
   *
   * @throws IOException
   */

  @Test
  public void testDrillAndDistribEnv() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    Map<String, String> distribEnv = new HashMap<>();
    distribEnv.put("DRILL_HEAP", "5G");
    distribEnv.put("DRILL_MAX_DIRECT_MEMORY", "7G");
    context.createEnvFile(new File(siteDir, "distrib-env.sh"), distribEnv, false);

    Map<String, String> drillEnv = new HashMap<>();
    drillEnv.put("DRILL_HEAP", "6G");
    drillEnv.put("DRILL_MAX_DIRECT_MEMORY", "9G");
    context.createEnvFile(new File(siteDir, "drill-env.sh"), drillEnv, false);

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN).run();
      assertEquals(0, result.returnCode);
      String expectedArgs[] = {
          "-Xms6G", "-Xmx6G",
          "-XX:MaxDirectMemorySize=9G",
          "-XX:ReservedCodeCacheSize=1024m" // Default
      };

      result.validateArgs(expectedArgs);
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_MAX_DIRECT_MEMORY", "5G").run();
      assertEquals(0, result.returnCode);
      String expectedArgs[] = {
          "-Xms6G", "-Xmx6G",
          "-XX:MaxDirectMemorySize=5G",
          "-XX:ReservedCodeCacheSize=1024m" // Default
      };

      result.validateArgs(expectedArgs);
    }
  }

  @Test
  public void testBadSiteDir() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.removeDir(siteDir);

    // Directory does not exist.

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir).run();
      assertEquals(1, result.returnCode);
      assertTrue(result.stderr.contains("Config dir does not exist"));
    }

    // Not a directory

    context.writeFile(siteDir, "dummy");
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir).run();
      assertEquals(1, result.returnCode);
      assertTrue(result.stderr.contains("Config dir does not exist"));
    }

    // Directory exists, but drill-override.conf does not

    siteDir.delete();
    context.createDir(siteDir);
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir).run();
      assertEquals(1, result.returnCode);
      assertTrue(result.stderr.contains("Drill config file missing"));
    }
  }

  /**
   * Move configuration to a site folder out of $DRILL_HOME/conf. The site
   * folder can contain code (which is why we call it "site" and not "config".)
   * The site directory can be passed to the Drillbit in several ways.
   *
   * @throws IOException
   */

  @Test
  public void testSiteDir() throws IOException {
    context.createMockDistrib();
    File confDir = new File(context.testDrillHome, "conf");
    context.createDir(confDir);
    File siteDir = new File(context.testDir, "site");
    context.createMockConf(siteDir);

    // Dummy drill-env.sh to simulate the shipped "example" file.

    context.writeFile(new File(confDir, "drill-env.sh"),
        "#!/bin/bash\n" + "# Example file");
    File siteJars = new File(siteDir, "jars");

    Map<String, String> distribEnv = new HashMap<>();
    distribEnv.put("DRILL_HEAP", "5G");
    distribEnv.put("DRILL_MAX_DIRECT_MEMORY", "7G");
    context.createEnvFile(new File(confDir, "distrib-env.sh"), distribEnv, false);

    Map<String, String> drillEnv = new HashMap<>();
    drillEnv.put("DRILL_HEAP", "6G");
    drillEnv.put("DRILL_MAX_DIRECT_MEMORY", "9G");
    context.createEnvFile(new File(siteDir, "drill-env.sh"), drillEnv, false);

    String expectedArgs[] = {
        "-Xms6G", "-Xmx6G",
        "-XX:MaxDirectMemorySize=9G",
        "-XX:ReservedCodeCacheSize=1024m" // Default
    };

    // Site set using argument

    {
      // Use --config explicitly

      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withArg("--config")
          .withArg(siteDir.getAbsolutePath())
          .run();
      assertEquals(0, result.returnCode);
      result.validateArgs(expectedArgs);
      result.validateClassPath(siteDir.getAbsolutePath());
    }

    {
      RunResult result = new DrillbitRun()
          .withArg("--config")
          .withArg(siteDir.getAbsolutePath())
          .withArg(DrillbitRun.DRILLBIT_RUN)
          .run();
      assertEquals(0, result.returnCode);
      result.validateArgs(expectedArgs);
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir)
          .run();
      assertEquals(0, result.returnCode);
      result.validateArgs(expectedArgs);
    }

    // Site argument and argument to Drillbit

    {
      String propArg = "-Dproperty=value";
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir)
          .withArg(propArg)
          .run( );
      assertEquals(0, result.returnCode);
      result.validateArgs(expectedArgs);
      result.validateArg(propArg);
    }

    // Set as an environment variable

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .addEnv("DRILL_CONF_DIR",siteDir.getAbsolutePath())
          .run();
      assertEquals(0, result.returnCode);
      result.validateArgs(expectedArgs);
    }

    // Site jars not on path if not created

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir)
          .run();
      assertFalse(result.classPathContains(siteJars.getAbsolutePath()));
    }

    // Site/jars on path if exists

    context.createDir(siteJars);
    context.makeDummyJar(siteJars, "site");

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir)
          .run();
      assertTrue(result.classPathContains(siteJars.getAbsolutePath() + "/*"));
    }
  }

  /**
   * Test the Java library path. Three sources:
   * <ol>
   * <li>DRILL_JAVA_LIB_PATH Set in drill-env.sh</li>
   * <li>DOY_JAVA_LIB_PATH passed in from an env. var.</li>
   * <li>$DRILL_SITE/lib, if it exists.</li>
   * </ol>
   *
   * @throws IOException
   */

  @Test
  public void testJavaLibDir() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    // Stock run: no lib dir.

    String prefix = "-Djava.library.path=";
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN).run();
      assertFalse(result.containsArgRegex(prefix + ".*"));
      assertNull(result.libPath);
    }

    // Old-style argument in DRILL_JAVA_OPTS

    {
      Map<String, String> env = new HashMap<>();
      env.put("DRILL_JAVA_OPTS", prefix + "/foo/bar:/foo/mumble");
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withEnvironment(env)
          .run();
      assertTrue(result.containsArgRegex(prefix + ".*"));
      assertNotNull(result.libPath);
      assertEquals(2, result.libPath.length);
      assertEquals("/foo/bar", result.libPath[0]);
      assertEquals("/foo/mumble", result.libPath[1]);
    }

    // New-style argument in DRILL_JAVA_LIB_PATH

    {
      Map<String, String> env = new HashMap<>();
      env.put("DRILL_JAVA_LIB_PATH", "/foo/bar:/foo/mumble");
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withEnvironment(env)
          .run();
      assertTrue(result.containsArgRegex(prefix + ".*"));
      assertNotNull(result.libPath);
      assertEquals(2, result.libPath.length);
      assertEquals("/foo/bar", result.libPath[0]);
      assertEquals("/foo/mumble", result.libPath[1]);
    }

    // YARN argument in DOY_JAVA_LIB_PATH

    {
      Map<String, String> env = new HashMap<>();
      env.put("DOY_JAVA_LIB_PATH", "/foo/bar:/foo/mumble");
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withEnvironment(env)
          .run();
      assertTrue(result.containsArgRegex(prefix + ".*"));
      assertNotNull(result.libPath);
      assertEquals(2, result.libPath.length);
      assertEquals("/foo/bar", result.libPath[0]);
      assertEquals("/foo/mumble", result.libPath[1]);
    }

    // Both DRILL_JAVA_LIB_PATH and DOY_JAVA_LIB_PATH

    {
      Map<String, String> env = new HashMap<>();
      env.put("DRILL_JAVA_LIB_PATH", "/foo/bar:/foo/mumble");
      env.put("DOY_JAVA_LIB_PATH", "/doy/bar:/doy/mumble");
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withEnvironment(env)
          .run();
      assertTrue(result.containsArgRegex(prefix + ".*"));
      assertNotNull(result.libPath);
      assertEquals(4, result.libPath.length);
      assertEquals("/doy/bar", result.libPath[0]);
      assertEquals("/doy/mumble", result.libPath[1]);
      assertEquals("/foo/bar", result.libPath[2]);
      assertEquals("/foo/mumble", result.libPath[3]);
    }

    // Site directory with a lib folder

    siteDir = new File(context.testDir, "site");
    context.createMockConf(siteDir);
    File libDir = new File(siteDir, "lib");
    context.createDir(libDir);

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir)
          .run();
      assertTrue(result.containsArgRegex(prefix + ".*"));
      assertNotNull(result.libPath);
      assertEquals(1, result.libPath.length);
      assertEquals(libDir.getAbsolutePath(), result.libPath[0]);
    }

    // The whole enchilada: all three settings.

    {
      Map<String, String> env = new HashMap<>();
      env.put("DRILL_JAVA_LIB_PATH", "/foo/bar:/foo/mumble");
      env.put("DOY_JAVA_LIB_PATH", "/doy/bar:/doy/mumble");
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RUN)
          .withSite(siteDir)
          .withEnvironment(env)
          .run();
      assertTrue(result.containsArgRegex(prefix + ".*"));
      assertNotNull(result.libPath);
      assertEquals(5, result.libPath.length);
      assertEquals(libDir.getAbsolutePath(), result.libPath[0]);
      assertEquals("/doy/bar", result.libPath[1]);
      assertEquals("/doy/mumble", result.libPath[2]);
      assertEquals("/foo/bar", result.libPath[3]);
      assertEquals("/foo/mumble", result.libPath[4]);
    }
  }

  /**
   * Test running a (simulated) Drillbit as a daemon with start, status, stop.
   *
   * @throws IOException
   */

  @Test
  public void testStockDaemon() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    // No drill-env.sh, no distrib-env.sh

    File pidFile;
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_START).start();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateStockArgs();
      result.validateClassPath(ScriptUtils.stdCp);
      assertTrue(result.stdout.contains("Starting drillbit, logging"));
      assertTrue(result.log.contains("Starting drillbit on"));
      assertTrue(result.log.contains("Drill Log Message"));
      assertTrue(result.out.contains("Drill Stdout Message"));
      assertTrue(result.out.contains("Stderr Message"));
      pidFile = result.pidFile;
    }

    // Save the pid file for reuse.

    assertTrue(pidFile.exists());
    File saveDir = new File(context.testDir, "save");
    context.createDir(saveDir);
    File savedPidFile = new File(saveDir, pidFile.getName());
    context.copyFile(pidFile, savedPidFile);

    // Status should be running

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STATUS).run( );
      assertEquals(0, result.returnCode);
      assertTrue(result.stdout.contains("drillbit is running"));
    }

    // Start should refuse to start a second Drillbit.

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_START).start();
      assertEquals(1, result.returnCode);
      assertTrue(
          result.stdout.contains("drillbit is already running as process"));
    }

    // Normal start, allow normal shutdown

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STOP).run();
      assertEquals(0, result.returnCode);
      assertTrue(result.log.contains("Terminating drillbit pid"));
      assertTrue(result.stdout.contains("Stopping drillbit"));
    }

    // Status should report no drillbit (no pid file)

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STATUS).run();
      assertEquals(1, result.returnCode);
      assertTrue(result.stdout.contains("drillbit is not running"));
    }

    // Stop should report no pid file

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STOP).run();
      assertEquals(1, result.returnCode);
      assertTrue(
          result.stdout.contains("No drillbit to stop because no pid file"));
    }

    // Get nasty. Put the pid file back. But, there is no process with that pid.

    context.copyFile(savedPidFile, pidFile);

    // Status should now complain.

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STATUS).run();
      assertEquals(1, result.returnCode);
      assertTrue(result.stdout
          .contains("file is present but drillbit is not running"));
    }

    // As should stop.

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STOP).run();
      assertEquals(1, result.returnCode);
      assertTrue(
          result.stdout.contains("No drillbit to stop because kill -0 of pid"));
    }
  }

  @Test
  public void testStockDaemonWithArg() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    // As above, but pass an argument.

    {
      String propArg = "-Dproperty=value";
      DrillbitRun runner = new DrillbitRun(DrillbitRun.DRILLBIT_START);
      runner.withArg(propArg);
      RunResult result = runner.start();
      assertEquals(0, result.returnCode);
      result.validateArg(propArg);
    }

    validateAndCloseDaemon(null);
  }

  private void validateAndCloseDaemon(File siteDir) throws IOException {
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STATUS)
          .withSite(siteDir)
          .run();
      assertEquals(0, result.returnCode);
      assertTrue(result.stdout.contains("drillbit is running"));
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STOP)
          .withSite(siteDir)
          .run();
      assertEquals(0, result.returnCode);
    }
  }

  /**
   * The Daemon process creates a pid file. Verify that the DRILL_PID_DIR can be
   * set to put the pid file in a custom location. The test is done with the
   * site (conf) dir in the default location.
   *
   * @throws IOException
   */

  @Test
  public void testPidDir() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);
    File pidDir = context.createDir(new File(context.testDir, "pid"));
    Map<String, String> drillEnv = new HashMap<>();
    drillEnv.put("DRILL_PID_DIR", pidDir.getAbsolutePath());
    context.createEnvFile(new File(siteDir, "drill-env.sh"), drillEnv, false);

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_START)
          .withPidDir(pidDir)
          .start();
      assertEquals(0, result.returnCode);
      assertTrue(result.pidFile.getParentFile().equals(pidDir));
      assertTrue(result.pidFile.exists());
    }

    validateAndCloseDaemon(null);
  }

  /**
   * Test a custom site directory with the Drill daemon process. The custom
   * directory contains a drill-env.sh with a custom option. Verify that that
   * option is picked up when starting Drill.
   *
   * @throws IOException
   */

  @Test
  public void testSiteDirWithDaemon() throws IOException {
    context.createMockDistrib();

    File siteDir = new File(context.testDir, "site");
    context.createMockConf(siteDir);

    Map<String, String> drillEnv = new HashMap<>();
    drillEnv.put("DRILL_MAX_DIRECT_MEMORY", "9G");
    context.createEnvFile(new File(siteDir, "drill-env.sh"), drillEnv, false);

    // Use the -site (--config) option.

    {
      DrillbitRun runner = new DrillbitRun(DrillbitRun.DRILLBIT_START);
      runner.withSite(siteDir);
      RunResult result = runner.start();
      assertEquals(0, result.returnCode);
      result.validateArg("-XX:MaxDirectMemorySize=9G");
    }

    validateAndCloseDaemon(siteDir);

    // Set an env var.

    {
      DrillbitRun runner = new DrillbitRun(DrillbitRun.DRILLBIT_START);
      runner.addEnv("DRILL_CONF_DIR", siteDir.getAbsolutePath());
      RunResult result = runner.start();
      assertEquals(0, result.returnCode);
      result.validateArg("-XX:MaxDirectMemorySize=9G");
    }

    validateAndCloseDaemon(siteDir);
  }

  /**
   * Launch the Drill daemon using a custom log file location. The config is in
   * the default location.
   *
   * @throws IOException
   */

  @Test
  public void testLogDirWithDaemon() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);
    File logsDir = context.createDir(new File(context.testDir, "logs"));
    context.removeDir(new File(context.testDrillHome, "log"));
    Map<String, String> drillEnv = new HashMap<>();
    drillEnv.put("DRILL_LOG_DIR", logsDir.getAbsolutePath());
    context.createEnvFile(new File(siteDir, "drill-env.sh"), drillEnv, false);

    {
      DrillbitRun runner = new DrillbitRun(DrillbitRun.DRILLBIT_START);
      runner.withLogDir(logsDir);
      RunResult result = runner.start();
      assertEquals(0, result.returnCode);
      assertNotNull(result.logFile);
      assertTrue(result.logFile.getParentFile().equals(logsDir));
      assertTrue(result.logFile.exists());
      assertNotNull(result.outFile);
      assertTrue(result.outFile.getParentFile().equals(logsDir));
      assertTrue(result.outFile.exists());
    }

    validateAndCloseDaemon(null);
  }

  /**
   * Some distributions create symlinks to drillbit.sh in standard locations
   * such as /usr/bin. Because drillbit.sh uses its own location to compute
   * DRILL_HOME, it must handle symlinks. This test verifies that process.
   *
   * @throws IOException
   */

  @Test
  public void testDrillbitSymlink() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    File drillbitFile = new File(context.testDrillHome, "bin/drillbit.sh");
    File linksDir = context.createDir(new File(context.testDir, "links"));
    File link = new File(linksDir, drillbitFile.getName());
    try {
      Files.createSymbolicLink(link.toPath(), drillbitFile.toPath());
    } catch (UnsupportedOperationException e) {
      // Well. This is a system without symlinks, so we won't be testing
      // syminks here...

      return;
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_START).start();
      assertEquals(0, result.returnCode);
      assertEquals(result.pidFile.getParentFile(), context.testDrillHome);
    }
    validateAndCloseDaemon(null);
  }

  /**
   * Test the restart command of drillbit.sh
   *
   * @throws IOException
   */

  @Test
  public void testRestart() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    int firstPid;
    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_START).start();
      assertEquals(0, result.returnCode);
      firstPid = result.getPid();
    }

    // Make sure it is running.

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STATUS)
          .withSite(siteDir)
          .run();
      assertEquals(0, result.returnCode);
      assertTrue(result.stdout.contains("drillbit is running"));
    }

    // Restart. Should get new pid.

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_RESTART).start();
      assertEquals(0, result.returnCode);
      int secondPid = result.getPid();
      assertNotEquals(firstPid, secondPid);
    }

    validateAndCloseDaemon(null);
  }

  /**
   * Simulate a Drillbit that refuses to die. The stop script wait a while, then
   * forces killing.
   *
   * @throws IOException
   */

  @Test
  public void testForcedKill() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    {
      DrillbitRun runner = new DrillbitRun(DrillbitRun.DRILLBIT_START);
      runner.addEnv("PRETEND_HUNG", "1");
      RunResult result = runner.start();
      assertEquals(0, result.returnCode);
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STATUS)
          .preserveLogs()
          .run();
      assertEquals(0, result.returnCode);
      assertTrue(result.stdout.contains("drillbit is running"));
    }

    {
      RunResult result = new DrillbitRun(DrillbitRun.DRILLBIT_STOP)
          .addEnv("DRILL_STOP_TIMEOUT","5")
          .preserveLogs()
          .run();
      assertEquals(0, result.returnCode);
      assertTrue(result.stdout.contains("drillbit did not complete after"));
    }
  }

  /**
   * Verify the basics of the sqlline script, including the env vars that can be
   * customized. Also validate running in embedded mode (using drillbit memory
   * and other options.)
   *
   * @throws IOException
   */

  @Test
  public void testSqlline() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    int stockArgCount;
    {
      // Out-of-the-box sqlline

      RunResult result = new ScriptRunner("sqlline").run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateClassPath(ScriptUtils.stdCp);
      assertTrue(result.containsArgsRegex(ScriptUtils.sqlLineArgs));
      stockArgCount = result.echoArgs.size();
    }

    {
      RunResult result = new ScriptRunner("sqlline")
          .withArg("arg1")
          .withArg("arg2")
          .run( );
      assertTrue(result.containsArg("arg1"));
      assertTrue(result.containsArg("arg2"));
    }
    {
      // Change drill memory and other drill-specific
      // settings: should not affect sqlline

      Map<String, String> drillEnv = new HashMap<>();
      drillEnv.put("DRILL_JAVA_OPTS", "-Dprop=value");
      drillEnv.put("DRILL_HEAP", "5G");
      drillEnv.put("DRILL_MAX_DIRECT_MEMORY", "7G");
      drillEnv.put("SERVER_LOG_GC", "1");
      drillEnv.put("DRILLBIT_CODE_CACHE_SIZE", "2G");
      RunResult result = new ScriptRunner("sqlline")
          .withEnvironment(drillEnv)
          .run();
      assertTrue(result.containsArgsRegex(ScriptUtils.sqlLineArgs));

      // Nothing new should have been added

      assertEquals(stockArgCount, result.echoArgs.size());
    }
    {
      // Change client memory: should affect sqlline

      Map<String, String> shellEnv = new HashMap<>();
      shellEnv.put("CLIENT_GC_OPTS", "-XX:+UseG1GC");
      RunResult result = new ScriptRunner("sqlline")
          .withEnvironment(shellEnv)
          .run();
      assertTrue(result.containsArg("-XX:+UseG1GC"));
    }
    {
      // Change drill memory and other drill-specific
      // settings: then set the "magic" variable that says
      // that Drill is embedded. The scripts should now use
      // the Drillbit options.

      Map<String, String> drillEnv = new HashMap<>();
      drillEnv.put("DRILL_JAVA_OPTS", "-Dprop=value");
      drillEnv.put("DRILL_HEAP", "5G");
      drillEnv.put("DRILL_MAX_DIRECT_MEMORY", "7G");
      drillEnv.put("SERVER_LOG_GC", "1");
      drillEnv.put("DRILLBIT_CODE_CACHE_SIZE", "2G");
      drillEnv.put("DRILL_EMBEDDED", "1");
      RunResult result = new ScriptRunner("sqlline")
          .withEnvironment(drillEnv)
          .run();

      String expectedArgs[] = {
          "-Dprop=value",
          "-Xms5G", "-Xmx5G",
          "-XX:MaxDirectMemorySize=7G",
          "-XX:ReservedCodeCacheSize=2G",
      };

      result.validateArgs(expectedArgs);
      assertTrue(result.containsArg("sqlline.SqlLine"));
    }
  }

  /**
   * Test to verify no effect of DRILLBIT_CONTEXT for Sqlline.
   * @throws IOException
   */
  @Test
  public void testSqllineWithDrillbitContextEnv() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    // Test when SQLLINE_JAVA_OPTS is overriden inside a condition for
    // DRILLBIT_CONTEXT = 0, then there is no effect
    {
      // Create a condition variable to be placed in distrib-env.sh
      Map<String, String> conditions = new HashMap<>();
      conditions.put("DRILLBIT_CONTEXT", "0");

      // Create environment variable to be placed inside a condition in distrib-env.sh
      Map<String, String> drillEnv = new HashMap<>();
      drillEnv.put("SQLLINE_JAVA_OPTS", "-XX:MaxPermSize=256M");

      // Create the environment variable file overriding SQLLINE_JAVA_OPTS
      context.createEnvFileWithCondition(new File(siteDir, "distrib-env.sh"), conditions, drillEnv, true);

      // Expected value of the property
      String expectedArgs[] = {"-XX:MaxPermSize=256M"};

      // Run the test and match the output with expectedArgs
      RunResult result = new ScriptRunner("sqlline").run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateClassPath(ScriptUtils.stdCp);
      // Since by default MaxPermSize is not set anymore for Sqlline. It's removed in 1.13
      assertFalse(result.containsArgsRegex(expectedArgs));
    }

    // Test when SQLLINE_JAVA_OPTS is overriden inside a condition for
    // DRILLBIT_CONTEXT = 1, then there is no effect
    {
      Map<String, String> conditions = new HashMap<>();
      conditions.put("DRILLBIT_CONTEXT", "1");

      Map<String, String> drillEnv = new HashMap<>();
      drillEnv.put("SQLLINE_JAVA_OPTS", "-XX:MaxPermSize=256M");
      String expectedArgs[] = {"-XX:MaxPermSize=256M"};

      // Create the environment variable file overriding SQLLINE_JAVA_OPTS
      context.createEnvFileWithCondition(new File(siteDir, "distrib-env.sh"), conditions, drillEnv, true);
      RunResult result = new ScriptRunner("sqlline").run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateClassPath(ScriptUtils.stdCp);
      // Since by default MaxPermSize is not set anymore for Sqlline. It's removed in 1.13
      assertFalse(result.containsArgsRegex(expectedArgs));
    }

    // Test when SQLLINE_JAVA_OPTS is overriden without condition for
    // DRILLBIT_CONTEXT then the environment variable is updated
    {
      Map<String, String> drillEnv = new HashMap<>();
      drillEnv.put("SQLLINE_JAVA_OPTS", "-XX:MaxPermSize=256M");

      // Create the environment variable file overriding SQLLINE_JAVA_OPTS without any condition
      // around it.
      String expectedArgs[] = {"-XX:MaxPermSize=256M"};
      context.createEnvFile(new File(siteDir, "distrib-env.sh"), drillEnv, true);
      RunResult result = new ScriptRunner("sqlline").run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateClassPath(ScriptUtils.stdCp);
      assertTrue(result.containsArgsRegex(expectedArgs));
    }
  }

  /**
   * Verify that the sqlline client works with the --site option by customizing
   * items in the site directory.
   *
   * @throws IOException
   */

  @Test
  public void testSqllineSiteDir() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDir, "site");
    context.createMockConf(siteDir);

    // Dummy drill-env.sh to simulate the shipped "example" file
    // with some client-specific changes.

    context.writeFile( new File( siteDir, "drill-env.sh" ),
        "#!/bin/bash\n" +
        "# Example file\n" +
        "export SQLLINE_JAVA_OPTS=\"-XX:MaxPermSize=256M\"\n"
        );
    File siteJars = new File(siteDir, "jars");
    context.createDir(siteJars);
    context.makeDummyJar(siteJars, "site");
    {
      RunResult result = new ScriptRunner("sqlline").withSite(siteDir).run();
      assertEquals(0, result.returnCode);
      assertTrue(result.containsArg("-XX:MaxPermSize=256M"));
      result.validateClassPath(siteJars.getAbsolutePath() + "/*");
    }
  }

  /**
   * Tests the three scripts that wrap sqlline for specific purposes:
   * <ul>
   * <li>drill-conf — Wrapper for sqlline, uses drill config to find Drill.
   * Seems this one needs fixing to use a config other than the hard-coded
   * $DRILL_HOME/conf location.</li>
   * <li>drill-embedded — Starts a drill "embedded" in SqlLine, using a local
   * ZK.</li>
   * <li>drill-localhost — Wrapper for sqlline, uses a local ZK.</li>
   * </ul>
   *
   * Of these, drill-embedded runs an embedded Drillbit and so should use the
   * Drillbit memory options. The other two are clients, but with simple default
   * options for finding the Drillbit.
   * <p>
   * Because the scripts are simple wrappers, all we do is verify that the right
   * "extra" options are set, not the fundamentals (which were already covered
   * in the sqlline tests.)
   *
   * @throws IOException
   */

  @Test
  public void testSqllineWrappers() throws IOException {
    context.createMockDistrib();
    File siteDir = new File(context.testDrillHome, "conf");
    context.createMockConf(siteDir);

    {
      // drill-conf: just adds a stub JDBC connect string.

      RunResult result = new ScriptRunner("drill-conf")
          .withArg("arg1")
          .run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateClassPath(ScriptUtils.stdCp);
      assertTrue(result.containsArgsRegex(ScriptUtils.sqlLineArgs));
      assertTrue(result.containsArg("-u"));
      assertTrue(result.containsArg("jdbc:drill:"));
      assertTrue(result.containsArg("arg1"));
    }

    {
      // drill-localhost: Adds a JDBC connect string to a drillbit
      // on the localhost

      RunResult result = new ScriptRunner("drill-localhost")
          .withArg("arg1")
          .run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateClassPath(ScriptUtils.stdCp);
      assertTrue(result.containsArgsRegex(ScriptUtils.sqlLineArgs));
      assertTrue(result.containsArg("-u"));
      assertTrue(result.containsArg("jdbc:drill:drillbit=localhost"));
      assertTrue(result.containsArg("arg1"));
    }

    {
      // drill-embedded: Uses drillbit startup options and
      // connects to the embedded drillbit.

      RunResult result = new ScriptRunner("drill-embedded")
          .withArg("arg1")
          .run();
      assertEquals(0, result.returnCode);
      result.validateJava();
      result.validateClassPath(ScriptUtils.stdCp);
      assertTrue(result.containsArgsRegex(ScriptUtils.sqlLineArgs));
      assertTrue(result.containsArg("-u"));
      assertTrue(result.containsArg("jdbc:drill:zk=local"));
      assertTrue(result.containsArg("-Xms4G"));
      assertTrue(result.containsArg("-XX:MaxDirectMemorySize=8G"));
      assertTrue(result.containsArg("arg1"));
    }
  }
}
