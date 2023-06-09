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
package org.apache.drill.yarn.client;


import org.apache.drill.common.util.GuavaPatcher;
import org.apache.drill.common.util.ProtobufPatcher;
import org.apache.drill.yarn.core.DoyConfigException;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.log4j.BasicConfigurator;

/**
 * Client for the Drill-on-YARN integration. See YARN documentation
 * for the role of a YARN client.
 * <p>
 * The client needs configuration information from drill-on-yarn.conf,
 * the directory of which must be in the class path. It is put there
 * by the drill-on-yarn.sh script.
 * <p>
 * The client also requires a debugging configuration file to be given
 * explicitly as follows:<br>
 * -Dlogback.configurationFile=/path/to/yarn-client-log.xml<br>
 * The drillbit itself uses the default logging config file name of
 * logback.xml; which contains references to system properties that are
 * not defined in this client. The result of not including the log
 * configuration file is that you'll see various var.name_IS_UNDEFINED
 * files in the directory from which you launched the client.
 * <p>
 * The client accepts a command, creates a command object for that
 * command, and executes it. There are a few main commands (start, stop),
 * along with some management commands (status, resize), and a few commands
 * mostly used for debugging and diagnosis (upload,etc.) Some commands
 * are very similar, so a single command object may handle multiple user
 * commands.
 * <p>
 * The client requires a working distributed file system (DFS), the
 * configuration of which is given either implicitly, or in the Hadoop
 * configuration files. Similarly, the client requires a working YARN
 * deployment, again with either implicit configuration or configuration
 * given in the Hadoop configuration. The Hadoop configuration must be
 * on the class path when launching this client.
 *
 * <h3>Debugging</h3>
 * <p>
 * To debug this class, add two or three directories to your class path:
 * <ul>
 * <li>$DRILL_CONF_DIR (if using a separate site directory)</li>
 * <li>$HADOOP_HOME/etc/hadoop</li>
 * <li>$DRILL_HOME/conf</li>
 * </ul>
 * Note that these MUST be in the order listed since $DRILL_HOME/conf
 * contains, by default, a version of core-site.xml that probably is
 * NOT the one you want to use for YARN. For YARN, you want the one
 * in $HADOOP_HOME/etc/hadoop.
 * <p>
 * Also, set the following VM argument:<br>
 * -Dlogback.configurationFile=/path/to/drill/conf/yarn-client-log.xml<br>
 * or<br>
 * -Dlogback.configurationFile=/path/to/drill-site/yarn-client-log.xml<br>
 */

public class DrillOnYarn {

  static {
    /*
     * Drill-on-YARN uses Hadoop dependencies that use older version of protobuf,
     * and override some methods that became final in recent protobuf versions.
     * This code removes these final modifiers.
     */
    ProtobufPatcher.patch();
    /*
     * Some libraries, such as Hadoop or HBase, depend on incompatible versions of Guava.
     * This code adds back some methods to so that the libraries can work with single Guava version.
     */
    GuavaPatcher.patch();
  }

  public static void main(String argv[]) {
    BasicConfigurator.configure();
    ClientContext.init();
    run(argv);
  }

  public static void run(String argv[]) {
    ClientContext context = ClientContext.instance();

    // Parse command-line options.

    CommandLineOptions opts = new CommandLineOptions();
    if (!opts.parse(argv)) {
      opts.usage();
      context.exit(-1);
    }
    if (opts.getCommand() == null) {
      opts.usage();
      context.exit(-1);
    }

    // Load configuration.

    try {
      DrillOnYarnConfig.load().setClientPaths();
    } catch (DoyConfigException e) {
      ClientContext.err.println(e.getMessage());
      context.exit(-1);
    }

    // Create the required command object.

    ClientCommand cmd;
    switch (opts.getCommand()) {
      case UPLOAD:
        cmd = new StartCommand(true, false);
        break;
      case START:
        cmd = new StartCommand(true, true);
        break;
      // Removed at QA request. QA wants a "real" restart. Also, upload of the
      // archive is fast enough that a "start without upload" option is not really
      // needed.
//    case RESTART:
//      cmd = new StartCommand(false, true);
//      break;
      case DESCRIBE:
        cmd = new PrintConfigCommand();
        break;
      case STATUS:
        cmd = new StatusCommand();
        break;
      case STOP:
        cmd = new StopCommand();
        break;
      case CLEAN:
        cmd = new CleanCommand();
        break;
      case RESIZE:
        cmd = new ResizeCommand();
        break;
      default:
        cmd = new HelpCommand();
    }

    // Run the command.

    cmd.setOpts(opts);
    try {
      cmd.run();
    } catch (ClientException e) {
      displayError(opts, e);
      context.exit(1);
    }
  }

  private static void displayError(CommandLineOptions opts, ClientException e) {

    // Show the Drill-provided explanation of the error.

    ClientContext.err.println(e.getMessage());

    // Add the underlying exception information, if any.

    Throwable parent = e;
    Throwable cause = e.getCause();
    while (cause != null && cause != parent) {
      ClientContext.err.print("  Caused by: ");
      ClientContext.err.println(cause.getMessage());
      parent = cause;
      cause = cause.getCause();
    }

    // Include the full stack trace if requested.

    if (opts.verbose) {
      ClientContext.err.println("Full stack trace:");
      e.printStackTrace(ClientContext.err);
    }
  }
}