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
package org.apache.drill.yarn.core;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Records;

/**
 * Abstract description of a remote process launch that describes the many
 * details needed to launch a process on a remote node.
 * <p>
 * Based on <a href="https://github.com/hortonworks/simple-yarn-app">Simple YARN
 * App</a>.
 */

public class LaunchSpec {
  /**
   * List of (key, file) pairs to be localized to the node before running the
   * command. The file must exist in a distributed file system (such as HDFS)
   * visible to both the client and remote node. Typically, the path is relative
   * or absolute within the file system defined by the fs.defaultFS parameter in
   * core-site.xml.
   * <p>
   * TODO: Can the value also be a URL such as
   * <p>
   * <code>hdfs://somehost:1234//path/to/file
   * <p>
   * The key is used as (what?).
   */

  public Map<String, LocalResource> resources = new HashMap<>();

  /**
   * Defines environment variables to be set on the remote host before launching
   * the remote app. Note: do not set CLASSPATH here; use {@link #classPath}
   * instead.
   */

  public Map<String, String> env = new HashMap<>();

  /**
   * Set to the name of the OS command to run when we wish to run a non-Java
   * command.
   */

  public String command;

  /**
   * Set to the name of the Java main class (the one with the main method) when
   * we wish to run a Java command.
   */

  public String mainClass;

  /**
   * Set to the application-specific class path for the Java application. These
   * values are added to the Hadoop-provided values. These items are relative to
   * (what?), use (what variables) to refer to the localized application
   * directory.
   */

  public List<String> classPath = new ArrayList<>();

  /**
   * Optional VM arguments to pass to the JVM when running a Java class; ignored
   * when running an OS command.
   */

  public List<String> vmArgs = new ArrayList<>();

  /**
   * Arguments to the remote command.
   */

  public List<String> cmdArgs = new ArrayList<>();

  public LaunchSpec() {
  }

  /**
   * Create the command line to run on the remote node. The command can either
   * be a simple OS command (if the {@link #command} member is set) or can be a
   * Java class (if the {@link #mainClass} member is set. If the command is
   * Java, then we pass along optional Java VM arguments.
   * <p>
   * In all cases we append arguments to the command itself, and redirect stdout
   * and stderr to log files.
   *
   * @return the complete command string
   */

  public String getCommand() {
    List<String> cmd = new ArrayList<>();
    if (command != null) {
      cmd.add(command);
    } else {
      assert mainClass != null;

      // JAVA_HOME is provided by YARN.

      cmd.add(Environment.JAVA_HOME.$$() + "/bin/java");
      cmd.addAll(vmArgs);
      if (!classPath.isEmpty()) {
        cmd.add("-cp");
        cmd.add(DoYUtil.join(":", classPath));
      }
      cmd.add(mainClass);
    }
    cmd.addAll(cmdArgs);
    cmd.add("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout");
    cmd.add("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr");

    // Java 8
    // return String.join( " ", cmd );
    return DoYUtil.join(" ", cmd);
  }

  /**
   * Given this generic description of an application, create the detailed YARN
   * application submission context required to launch the application.
   *
   * @param conf
   *          the YARN configuration obtained by reading the Hadoop
   *          configuration files
   * @return the completed application launch context for the given application
   * @throws IOException
   *           if localized resources are not found in the distributed file
   *           system (such as HDFS)
   */

  public ContainerLaunchContext createLaunchContext(YarnConfiguration conf)
      throws IOException {
    // Set up the container launch context
    ContainerLaunchContext container = Records
        .newRecord(ContainerLaunchContext.class);

    // Set up the list of commands to run. Here, we assume that we run only
    // one command.

    container.setCommands(Collections.singletonList(getCommand()));

    // Add localized resources

    container.setLocalResources(resources);

    // Environment.

    container.setEnvironment(env);

    return container;
  }

  public void dump(PrintStream out) {
    if (command != null) {
      out.print("Command: ");
      out.println(command);
    }
    if (mainClass != null) {
      out.print("Main Class: ");
      out.println(mainClass);
      out.println("VM Args:");
      if (vmArgs.isEmpty()) {
        out.println("  None");
      } else {
        for (String item : vmArgs) {
          out.print("  ");
          out.println(item);
        }
      }
      out.println("Class Path:");
      if (classPath.isEmpty()) {
        out.println("  None");
      } else {
        for (String item : classPath) {
          out.print("  ");
          out.println(item);
        }
      }
    }
    out.println("Program Args:");
    if (cmdArgs.isEmpty()) {
      out.println("  None");
    } else {
      for (String item : cmdArgs) {
        out.print("  ");
        out.println(item);
      }
    }
    out.println("Environment:");
    if (env.isEmpty()) {
      out.println("  None");
    } else {
      for (String key : env.keySet()) {
        out.print("  ");
        out.print(key);
        out.print("=");
        out.println(env.get(key));
      }
    }
    out.println("Resources: ");
    if (resources.isEmpty()) {
      out.println("  None");
    } else {
      for (String key : resources.keySet()) {
        out.print("  Key: ");
        out.println(key);
        LocalResource resource = resources.get(key);
        out.print("   URL: ");
        out.println(resource.getResource().toString());
        out.print("   Size: ");
        out.println(resource.getSize());
        out.print("   Timestamp: ");
        out.println(DoYUtil.toIsoTime(resource.getTimestamp()));
        out.print("   Type: ");
        out.println(resource.getType().toString());
        out.print("   Visiblity: ");
        out.println(resource.getVisibility().toString());
      }
    }
  }
}
