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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class ScriptUtils {

  private static ScriptUtils instance = new ScriptUtils();
  public File distribDir;
  public File javaHome;
  public File testDir;
  public File testDrillHome;
  public File testSiteDir;
  public File testLogDir;
  public boolean externalLogDir;

  /**
   * Out-of-the-box command-line arguments when launching sqlline.
   * Order is not important here (though it is to Java.)
   */

  public static String sqlLineArgs[] = makeSqlLineArgs( );

  private static String[] makeSqlLineArgs( ) {
    String args[] = {
      "-Dlog.path=/.*/drill/log/sqlline\\.log",
      "-Dlog.query.path=/.*/drill/log/sqlline_queries\\.json",
      "sqlline\\.SqlLine",
      "-d",
      "org\\.apache\\.drill\\.jdbc\\.Driver",
      "--maxWidth=10000",
      "--color=true"
    };

    // Special handling if this machine happens to have the default
    // /var/log/drill log location.

    if ( new File( "/var/log/drill" ).exists() ) {
      args[ 0 ] = "-Dlog\\.path=/var/log/drill/sqlline\\.log";
      args[ 1 ] = "-Dlog\\.query\\.path=/var/log/drill/sqlline_queries\\.json";
    }
    return args;
  }

  public static final boolean USE_SOURCE = true;
  public static final String TEMP_DIR = "/tmp";
  public static boolean useSource = USE_SOURCE;

  private ScriptUtils() {
    String drillScriptsDir = System.getProperty("drillScriptDir");
    assertNotNull(drillScriptsDir);
    distribDir = new File(drillScriptsDir);
    javaHome = new File(System.getProperty("java.home"));
  }

  public static ScriptUtils instance() {
    return instance;
  }

  public ScriptUtils fromSource(String sourceDir) {
    useSource = true;
    return this;
  }

  public ScriptUtils fromDistrib(String distrib) {
    distribDir = new File(distrib);
    useSource = false;
    return this;
  }

  /**
   * Out-of-the-box command-line arguments when launching Drill.
   * Order is not important here (though it is to Java.)
   */

  public static String[] stdArgs = buildStdArgs();

  private static String[] buildStdArgs() {
    String[] args = {
      "-Xms4G",
      "-Xmx4G",
      "-XX:MaxDirectMemorySize=8G",
      "-XX:ReservedCodeCacheSize=1G",
      "-Ddrill\\.exec\\.enable-epoll=false",
      "-XX:\\+UseG1GC",
      "org\\.apache\\.drill\\.exec\\.server\\.Drillbit",
      "-Dlog\\.path=/.*/script-test/drill/log/drillbit\\.log",
      "-Dlog\\.query\\.path=/.*/script-test/drill/log/drillbit_queries\\.json",
    };

    // Special handling if this machine happens to have the default
    // /var/log/drill log location.

    if ( new File( "/var/log/drill" ).exists() ) {
      args[ args.length-2 ] = "-Dlog\\.path=/var/log/drill/drillbit\\.log";
      args[ args.length-1 ] = "-Dlog\\.query\\.path=/var/log/drill/drillbit_queries\\.json";
    }
    return args;
  };

  /**
   * Out-of-the-box class-path before any custom additions.
   */

  static String stdCp[] =
  {
    "conf",
    "jars/*",
    "jars/ext/*",
    "jars/3rdparty/*",
    "jars/classb/*"
  };

  /**
   * Directories to create to simulate a Drill distribution.
   */

  static String distribDirs[] = {
      "bin",
      "jars",
      "jars/3rdparty",
      "jars/ext",
      "conf"
  };

  /**
   * Out-of-the-box Jar directories.
   */

  static String jarDirs[] = {
      "jars",
      "jars/3rdparty",
      "jars/ext",
  };

  /**
   * Scripts we must copy from the source tree to create a simulated
   * Drill bin directory.
   */

  public static String scripts[] = {
      "drill-config.sh",
      "drill-embedded",
      "drill-localhost",
      "drill-on-yarn.sh",
      "drillbit.sh",
      "drill-conf",
      //dumpcat
      //hadoop-excludes.txt
      "runbit",
      "sqlline",
      //sqlline.bat
      //submit_plan
      "yarn-drillbit.sh",
      "auto-setup.sh"
  };

  /**
   * Create the basic test directory. Tests add or remove details.
   */

  public void initialSetup() throws IOException {
    File tempDir = new File(TEMP_DIR);
    testDir = new File(tempDir, "script-test");
    testDrillHome = new File(testDir, "drill");
    testSiteDir = new File(testDir, "site");
    File varLogDrill = new File( "/var/log/drill" );
    if ( varLogDrill.exists() ) {
      testLogDir = varLogDrill;
      externalLogDir = true;
    } else {
      testLogDir = new File(testDrillHome, "log");
    }
    if (testDir.exists()) {
      FileUtils.forceDelete(testDir);
    }
    testDir.mkdirs();
    testSiteDir.mkdir();
    testLogDir.mkdir();
  }

  public void createMockDistrib() throws IOException {
    if (ScriptUtils.useSource) {
      buildFromSource();
    } else {
      buildFromDistrib();
    }
  }

  /**
   * Build the Drill distribution directory directly from sources.
   */

  private void buildFromSource() throws IOException {
    createMockDirs();
    copyScripts(ScriptUtils.instance().distribDir);
  }

  /**
   * Build the shell of a Drill distribution directory by creating the required
   * directory structure.
   */

  private void createMockDirs() throws IOException {
    if (testDrillHome.exists()) {
      FileUtils.forceDelete(testDrillHome);
    }
    testDrillHome.mkdir();
    for (String path : ScriptUtils.distribDirs) {
      File subDir = new File(testDrillHome, path);
      subDir.mkdirs();
    }
    for (String path : ScriptUtils.jarDirs) {
      makeDummyJar(new File(testDrillHome, path), "dist");
    }
  }

  /**
   * The tests should not require jar files, but we simulate them to be a bit
   * more realistic. Since we dont' run Java, the jar files can be simulated.
   */

  public File makeDummyJar(File dir, String prefix) throws IOException {
    String jarName = "";
    if (prefix != null) {
      jarName += prefix + "-";
    }
    jarName += dir.getName() + ".jar";
    File jarFile = new File(dir, jarName);
    writeFile(jarFile, "Dummy jar");
    return jarFile;
  }

  /**
   * Create a simple text file with the given contents.
   */

  public void writeFile(File file, String contents) throws IOException {
    try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
      out.println(contents);
    }
  }

  public void writeEnvFile(PrintWriter out, String key, String value, boolean overrideValue) {
    out.print("export ");
    out.print(key);
    out.print("=");

    if (!overrideValue) {
      out.print("${");
      out.print(key);
      out.print(":-");
    }
    out.print("\"");
    out.print(value);
    out.print("\"");

    if (!overrideValue) {
      out.print("}");
    }
    out.println();
  }

  /**
   * Create a drill-env.sh or distrib-env.sh file with the given environment in
   * the recommended format.
   * different formats based on overrideValue flag
   *
   * @param file - File instance to set environment variables in
   * @param env - Environment to be placed inside File
   * @param overrideValue - true - Set environment value such that it overrides previously set value
   *                      - false - Set environment value in recommended format.
   */
  public void createEnvFile(File file, Map<String, String> env, boolean overrideValue)
      throws IOException {
    try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
      out.println("#!/bin/bash");
      for (String key : env.keySet()) {
        String value = env.get(key);
        writeEnvFile(out, key, value, overrideValue);
      }
    }
  }

  /**
   * Creates a drill-env.sh or distrib-env.sh file with the given environment under
   * a given condition. If size of env map is smaller than condition map then last
   * env entry is repeated for rest of conditions.
   *
   * @param file - File instance to set environment and condition in
   * @param condition - Conditions to guard environment variable
   * @param env - Environment to be placed inside File
   * @param overrideValue - true - Set environment value such that it overrides previously set value
   *                      - false - Set environment value in recommended format.
   *
   */
  public void createEnvFileWithCondition(File file, Map<String, String> condition,
                                         Map<String, String> env, boolean overrideValue) throws IOException {
    if (env.size() == 0 || condition.size() == 0) {
      return;
    }

    final Iterator envIterator = env.entrySet().iterator();
    Map.Entry currentEnv = (Map.Entry) envIterator.next();

    try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
      out.println("#!/bin/bash");

      for (String condKey : condition.keySet()) {
        String condValue = condition.get(condKey);
        out.print("if [ \"$");
        out.print(condKey);
        out.print("\" = \"");
        out.print(condValue);
        out.println("\" ]; then");

        final String envKey = currentEnv.getKey().toString();
        final String envValue = currentEnv.getValue().toString();
        writeEnvFile(out, envKey, envValue, overrideValue);

        out.println("fi");
        out.println();

        if (envIterator.hasNext()) {
          currentEnv = (Map.Entry) envIterator.next();
        }
      }
    }
  }

  /**
   * Copy the standard scripts from source location to the mock distribution
   * directory.
   */

  private void copyScripts(File sourceDir) throws IOException {
    File binDir = new File(testDrillHome, "bin");
    for (String script : ScriptUtils.scripts) {
      File source = new File(sourceDir, script);
      File dest = new File(binDir, script);
      copyFile(source, dest);
      dest.setExecutable(true);
    }

    // Create the "magic" wrapper script that simulates the Drillbit and
    // captures the output we need for testing.

    String wrapper = "wrapper.sh";
    File dest = new File(binDir, wrapper);
    try (InputStream is = getClass().getResourceAsStream("/" + wrapper)) {
      Files.copy(is, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    dest.setExecutable(true);
  }

  private void buildFromDistrib() {
    // TODO Auto-generated method stub

  }

  /**
   * Consume the input from a stream, specifically the stderr or stdout stream
   * from a process.
   *
   * @link http://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes
   * -without-blocki
   */

  private static class StreamGobbler extends Thread {
    InputStream is;
    public StringBuilder buf = new StringBuilder();

    private StreamGobbler(InputStream is) {
      this.is = is;
    }

    @Override
    public void run() {
      try {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null) {
          buf.append(line);
          buf.append("\n");
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  /**
   * Handy run result class to capture the information we need for testing and
   * to do various kinds of validation on it.
   */

  public static class RunResult {
    File logDir;
    File logFile;
    String stdout;
    String stderr;
    List<String> echoArgs;
    int returnCode;
    String classPath[];
    String libPath[];
    String log;
    public File pidFile;
    public File outFile;
    String out;

    /**
     * Split the class path into strings for easier validation.
     */

    public void analyze() {
      if (echoArgs == null) {
        return;
      }
      for (int i = 0; i < echoArgs.size(); i++) {
        String arg = echoArgs.get(i);
        if (arg.equals("-cp")) {
          classPath = Pattern.compile(":").split((echoArgs.get(i + 1)));
          break;
        }
      }
      String probe = "-Djava.library.path=";
      for (int i = 0; i < echoArgs.size(); i++) {
        String arg = echoArgs.get(i);
        if (arg.startsWith(probe)) {
          assertNull(libPath);
          libPath = Pattern.compile(":").split((arg.substring(probe.length())));
          break;
        }
      }
    }

    /**
     * Read the log file, if any, generated by the process.
     */

    public void loadLog() throws IOException {
      log = loadFile(logFile);
    }

    private String loadFile(File file) throws IOException {
      StringBuilder buf = new StringBuilder();
      try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
        String line;
        while ((line = reader.readLine()) != null) {
          buf.append(line);
          buf.append("\n");
        }
        return buf.toString();
      } catch (FileNotFoundException e) {
        return null;
      }
    }

    /**
     * Validate that the first argument invokes Java correctly.
     */

    public void validateJava() {
      assertNotNull(echoArgs);
      String java = instance.javaHome + "/bin/java";
      List<String> actual = echoArgs;
      assertEquals(java, actual.get(0));
    }

    public boolean containsArg(String arg) {
      for (String actual : echoArgs) {
        if (actual.equals(arg)) {
          return true;
        }
      }
      return false;
    }

    public void validateStockArgs() {
      for (String arg : ScriptUtils.stdArgs) {
        assertTrue("Argument not found: " + arg + " in " + echoArgs, containsArgRegex(arg));
      }
    }

    public void validateArg(String arg) {
      validateArgs(Collections.singletonList(arg));
    }

    public void validateArgs(String args[]) {
      validateArgs(Arrays.asList(args));
    }

    public void validateArgs(List<String> args) {
      validateJava();
      for (String arg : args) {
        assertTrue(containsArg(arg));
      }
    }

    public void validateArgRegex(String arg) {
      assertTrue(containsArgRegex(arg));
    }

    public void validateArgsRegex(List<String> args) {
      assertTrue(containsArgsRegex(args));
    }

    public boolean containsArgsRegex(List<String> args) {
      for (String arg : args) {
        if (!containsArgRegex(arg)) {
          return false;
        }
      }
      return true;
    }

    public boolean containsArgsRegex(String args[]) {
      for (String arg : args) {
        if (!containsArgRegex(arg)) {
          return false;
        }
      }
      return true;
    }

    public boolean containsArgRegex(String arg) {
      for (String actual : echoArgs) {
        if (actual.matches(arg)) {
          return true;
        }
      }
      return false;
    }

    public void validateClassPath(String expectedCP) {
      assertTrue(classPathContains(expectedCP));
    }

    public void validateClassPath(String expectedCP[]) {
      assertTrue(classPathContains(expectedCP));
    }

    public boolean classPathContains(String expectedCP[]) {
      for (String entry : expectedCP) {
        if (!classPathContains(entry)) {
          return false;
        }
      }
      return true;
    }

    public boolean classPathContains(String expectedCP) {
      if (classPath == null) {
        fail("No classpath returned");
      }
      String tail = "/" + instance.testDir.getName() + "/"
          + instance.testDrillHome.getName() + "/";
      String expectedPath;
      if (expectedCP.startsWith("/")) {
        expectedPath = expectedCP;
      } else {
        expectedPath = tail + expectedCP;
      }
      for (String entry : classPath) {
        if (entry.endsWith(expectedPath)) {
          return true;
        }
      }
      return false;
    }

    public void loadOut() throws IOException {
      out = loadFile(outFile);
    }

    /**
     * Ensure that the Drill log file contains at least the sample message
     * written by the wrapper.
     */

    public void validateDrillLog() {
      assertNotNull(log);
      assertTrue(log.contains("Drill Log Message"));
    }

    /**
     * Validate that the stdout contained the expected message.
     */

    public void validateStdOut() {
      assertTrue(stdout.contains("Starting drillbit on"));
    }

    /**
     * Validate that the stderr contained the sample error message from the
     * wrapper.
     */

    public void validateStdErr() {
      assertTrue(stderr.contains("Stderr Message"));
    }

    public int getPid() throws IOException {
      try (BufferedReader reader = new BufferedReader(new FileReader(pidFile))) {
        return Integer.parseInt(reader.readLine());
      }
      finally { }
    }

  }

  /**
   * The "business end" of the tests: runs drillbit.sh and captures results.
   */

  public static class ScriptRunner {
    // Drillbit commands

    public static String DRILLBIT_RUN = "run";
    public static String DRILLBIT_START = "start";
    public static String DRILLBIT_STATUS = "status";
    public static String DRILLBIT_STOP = "stop";
    public static String DRILLBIT_RESTART = "restart";

    public File cwd = instance.testDir;
    public File drillHome = instance.testDrillHome;
    public String script;
    public List<String> args = new ArrayList<>();
    public Map<String, String> env = new HashMap<>();
    public File logDir;
    public File pidFile;
    public File outputFile;
    public boolean preserveLogs;

    public ScriptRunner(String script) {
      this.script = script;
    }

    public ScriptRunner(String script, String cmd) {
      this(script);
      args.add(cmd);
    }

    public ScriptRunner(String script, String cmdArgs[]) {
      this(script);
      for (String arg : cmdArgs) {
        args.add(arg);
      }
    }

    public ScriptRunner withArg(String arg) {
      args.add(arg);
      return this;
    }

    public ScriptRunner withSite(File siteDir) {
      if (siteDir != null) {
        args.add("--site");
        args.add(siteDir.getAbsolutePath());
      }
      return this;
    }

    public ScriptRunner withEnvironment(Map<String, String> env) {
      if (env != null) {
        this.env.putAll(env);
      }
      return this;
    }

    public ScriptRunner addEnv(String key, String value) {
      env.put(key, value);
      return this;
    }

    public ScriptRunner withLogDir(File logDir) {
      this.logDir = logDir;
      return this;
    }

    public ScriptRunner preserveLogs() {
      preserveLogs = true;
      return this;
    }

    public RunResult run() throws IOException {
      File binDir = new File(drillHome, "bin");
      File scriptFile = new File(binDir, script);
      assertTrue(scriptFile.exists());
      outputFile = new File(instance.testDir, "output.txt");
      outputFile.delete();
      if (logDir == null) {
        logDir = instance.testLogDir;
      }
      if (!preserveLogs) {
        cleanLogs(logDir);
      }

      Process proc = startProcess(scriptFile);
      RunResult result = runProcess(proc);
      if (result.returnCode == 0) {
        captureOutput(result);
        captureLog(result);
      }
      return result;
    }

    private void cleanLogs(File logDir) throws IOException {
      if ( logDir == instance.testLogDir  &&  instance.externalLogDir ) {
        return;
      }
      if (logDir.exists()) {
        FileUtils.forceDelete(logDir);
      }
    }

    private Process startProcess(File scriptFile) throws IOException {
      outputFile.delete();
      List<String> cmd = new ArrayList<>();
      cmd.add(scriptFile.getAbsolutePath());
      cmd.addAll(args);
      ProcessBuilder pb = new ProcessBuilder().command(cmd).directory(cwd);
      Map<String, String> pbEnv = pb.environment();
      pbEnv.clear();
      pbEnv.putAll(env);
      File binDir = new File(drillHome, "bin");
      File wrapperCmd = new File(binDir, "wrapper.sh");

      // Set the magic wrapper to capture output.

      pbEnv.put("_DRILL_WRAPPER_", wrapperCmd.getAbsolutePath());
      pbEnv.put("JAVA_HOME", instance.javaHome.getAbsolutePath());
      return pb.start();
    }

    private RunResult runProcess(Process proc) {
      StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());
      StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());
      outputGobbler.start();
      errorGobbler.start();

      try {
        proc.waitFor();
      } catch (InterruptedException e) {
        // Won't occur.
      }

      RunResult result = new RunResult();
      result.stderr = errorGobbler.buf.toString();
      result.stdout = outputGobbler.buf.toString();
      result.returnCode = proc.exitValue();
      return result;
    }

    private void captureOutput(RunResult result) throws IOException {
      // Capture the Java arguments which the wrapper script wrote to a file.

      try (BufferedReader reader = new BufferedReader(new FileReader(outputFile))) {
         result.echoArgs = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
          result.echoArgs.add(line);
        }
        result.analyze();
      } catch (FileNotFoundException e) {
        // noop
      }
    }

    private void captureLog(RunResult result) throws IOException {
      result.logDir = logDir;
      result.logFile = new File(logDir, "drillbit.log");
      if (result.logFile.exists()) {
        result.loadLog();
      } else {
        result.logFile = null;
      }
    }
  }

  public static class DrillbitRun extends ScriptRunner {
    public File pidDir;

    public DrillbitRun() {
      super("drillbit.sh");
    }

    public DrillbitRun(String cmd) {
      super("drillbit.sh", cmd);
    }

    public DrillbitRun withPidDir(File pidDir) {
      this.pidDir = pidDir;
      return this;
    }

    public DrillbitRun asDaemon() {
      addEnv("KEEP_RUNNING", "1");
      return this;
    }

    public RunResult start() throws IOException {
      if (pidDir == null) {
        pidDir = drillHome;
      }
      pidFile = new File(pidDir, "drillbit.pid");
      // pidFile.delete();
      asDaemon();
      RunResult result = run();
      if (result.returnCode == 0) {
        capturePidFile(result);
        captureDrillOut(result);
      }
      return result;
    }

    private void capturePidFile(RunResult result) {
      assertTrue(pidFile.exists());
      result.pidFile = pidFile;
    }

    private void captureDrillOut(RunResult result) throws IOException {
      // Drillbit.out

      result.outFile = new File(result.logDir, "drillbit.out");
      if (result.outFile.exists()) {
        result.loadOut();
      } else {
        result.outFile = null;
      }
    }

  }

  /**
   * Build a "starter" conf or site directory by creating a mock
   * drill-override.conf file.
   */

  public void createMockConf(File siteDir) throws IOException {
    createDir(siteDir);
    File override = new File(siteDir, "drill-override.conf");
    writeFile(override, "# Dummy override");
  }

  public void removeDir(File dir) throws IOException {
    if (dir.exists()) {
      FileUtils.forceDelete(dir);
    }
  }

  /**
   * Remove, then create a directory.
   */

  public File createDir(File dir) throws IOException {
    removeDir(dir);
    dir.mkdirs();
    return dir;
  }

  public void copyFile(File source, File dest) throws IOException {
    Files.copy(source.toPath(), dest.toPath(),
        StandardCopyOption.REPLACE_EXISTING);
  }

}
