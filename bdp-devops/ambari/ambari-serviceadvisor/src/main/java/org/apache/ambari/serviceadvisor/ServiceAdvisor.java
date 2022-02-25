/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.serviceadvisor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Class that can be called either through its jar or using its run method.
 * The goal is to invoke a Service Advisor.
 * Right now, it is backward compatible by invoking the python script and does not know which service is affected.
 */
public class ServiceAdvisor {
  protected static Log LOG = LogFactory.getLog(ServiceAdvisor.class);

  private static String USAGE = "Usage: java -jar serviceadvisor.jar [ACTION] [HOSTS_FILE.json] [SERVICES_FILE.json] [OUTPUT.txt] [ERRORS.txt]";
  private static String PYTHON_STACK_ADVISOR_SCRIPT = "/var/lib/ambari-server/resources/scripts/stack_advisor.py";

  /**
   * Entry point for calling this class through its jar.
   * @param args
   */
  public static void main(String[] args) {
    if (args.length != 5) {
      System.err.println(String.format("Wrong number of arguments. %s", USAGE));
      System.exit(1);
    }

    String action = args[0];
    String hostsFile = args[1];
    String servicesFile = args[2];
    String outputFile = args[3];
    String errorFile = args[4];

    int exitCode = run(action, hostsFile, servicesFile, outputFile, errorFile);
    System.exit(exitCode);
  }

  public static int run(String action, String hostsFile, String servicesFile, String outputFile, String errorFile) {
    LOG.info(String.format("ServiceAdvisor. Received arguments. Action: %s, Hosts File: %s, Services File: %s", action, hostsFile, servicesFile));
    int returnCode = -1;

    try {
      ServiceAdvisorCommandType commandType = ServiceAdvisorCommandType.getEnum(action);

      // TODO, load each Service's Service Advisor at Start Time and call it instead of Python command below.

      ProcessBuilder builder = preparePythonShellCommand(commandType, hostsFile, servicesFile, outputFile, errorFile);
      returnCode = launchProcess(builder);
    } catch (IllegalArgumentException e) {
      List<ServiceAdvisorCommandType> values = EnumUtils.getEnumList(ServiceAdvisorCommandType.class);
      List<String> stringValues = new ArrayList<String>();
      for (ServiceAdvisorCommandType value : values) {
        stringValues.add(value.toString());
      }
      LOG.error("ServiceAdvisor. Illegal Argument. Action must be one of " + StringUtils.join(stringValues.toArray(), ", "));
      return -1;
    }  catch (Exception e) {
      LOG.error("ServiceAdvisor. Failed with " + e.getMessage());
      return -1;
    }
    return returnCode;
  }

  /**
   * Generate a process to invoke a Python command for the old-style Stack Advisor.
   * @param commandType Command Type
   * @param hostsFile hosts.json file
   * @param servicesFile services.json file
   * @param outputFile output.txt
   * @param errorFile error.txt
   * @return Process that can launch.
   */
  private static ProcessBuilder preparePythonShellCommand(ServiceAdvisorCommandType commandType, String hostsFile, String servicesFile, String outputFile, String errorFile) {
    List<String> builderParameters = new ArrayList<String>();

    if (System.getProperty("os.name").contains("Windows")) {
      builderParameters.add("cmd");
      builderParameters.add("/c");
    } else {
      builderParameters.add("sh");
      builderParameters.add("-c");
    }

    StringBuilder commandString = new StringBuilder();
    commandString.append(PYTHON_STACK_ADVISOR_SCRIPT + " ");

    commandString.append(commandType.toString()).append(" ");
    commandString.append(hostsFile).append(" ");
    commandString.append(servicesFile).append(" ");
    commandString.append("1> ");
    commandString.append(outputFile).append(" ");
    commandString.append("2>");
    commandString.append(errorFile).append(" ");

    builderParameters.add(commandString.toString());

    LOG.info("ServiceAdvisor. Python command is: " + builderParameters.toString());

    return new ProcessBuilder(builderParameters);
  }

  /**
   * Launch a process, wait for it to finish, and return its exit code.
   * @param builder Process Builder
   * @return Exit Code
   * @throws Exception
   */
  private static int launchProcess(ProcessBuilder builder) throws Exception {
    int exitCode = -1;
    Process process = null;
    try {
      process = builder.start();
      exitCode = process.waitFor();
    } catch (Exception ioe) {
      String message = "Error executing Service Advisor: ";
      LOG.error(message, ioe);
      throw new Exception(message + ioe.getMessage());
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
    return exitCode;
  }
}