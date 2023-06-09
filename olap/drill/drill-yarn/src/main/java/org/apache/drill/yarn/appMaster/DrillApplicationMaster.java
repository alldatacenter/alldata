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
package org.apache.drill.yarn.appMaster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.drill.common.util.GuavaPatcher;
import org.apache.drill.common.util.ProtobufPatcher;
import org.apache.drill.yarn.appMaster.ControllerFactory.ControllerFactoryException;
import org.apache.drill.yarn.appMaster.http.WebServer;
import org.apache.drill.yarn.core.DoyConfigException;
import org.apache.drill.yarn.core.DrillOnYarnConfig;

/**
 * Application Master for Drill. The name is visible when using the "jps"
 * command and is chosen to make sense on a busy YARN node.
 * <p>
 * To debug this AM use the customized unmanaged AM launcher in this
 * jar. (The "stock" YARN version does not give you time to attach
 * the debugger.)
 * <pre><code>
 * TARGET_JAR=/your-git-folder/drill-yarn/target/drill-yarn-1.6-SNAPSHOT.jar
 * TARGET_CLASS=org.apache.drill.yarn.appMaster.ApplicationMaster
 * LAUNCHER_JAR=$TARGET_JAR
 * LAUNCHER_CLASS=org.apache.drill.yarn.mock.UnmanagedAMLauncher
 * $HH/bin/hadoop jar $LAUNCHER_JAR \
 *   $LAUNCHER_CLASS -classpath $TARGET_JAR \
 *   -cmd "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 \
 *   $TARGET_CLASS"
 * </pre></code>
 */

public class DrillApplicationMaster {

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

  private static final Log LOG = LogFactory
      .getLog(DrillApplicationMaster.class);

  public static void main(String[] args) {
    LOG.trace("Drill Application Master starting.");

    // Load the configuration. Assumes that the user's Drill-on-YARN
    // configuration was archived along with the Drill software in
    // the $DRILL_HOME/conf directory, and that $DRILL_HOME/conf is
    // on the class-path.

    try {
      DrillOnYarnConfig.load().setAmDrillHome();
    } catch (DoyConfigException e) {
      System.err.println(e.getMessage());
      System.exit(-1);
    }

    // Build the dispatcher using the Drillbit factory. Allows inserting
    // other factories for testing, or if we need to manage a cluster of
    // processes other than Drillbits.

    // Dispatcher am = (new SimpleBatchFactory( )).build( );
    // Dispatcher am = (new MockDrillbitFactory( )).build( );
    Dispatcher dispatcher;
    try {
      dispatcher = (new DrillControllerFactory()).build();
    } catch (ControllerFactoryException e) {
      LOG.error("Setup failed, exiting: " + e.getMessage(), e);
      System.exit(-1);
      return;
    }

    // Start the Dispatcher. This will return false if this AM conflicts with
    // a running AM.

    try {
      if (!dispatcher.start()) {
        return;
      }
    } catch (Throwable e) {
      LOG.error("Fatal error, exiting: " + e.getMessage(), e);
      System.exit(-1);
    }

    // Create and start the web server. Do this after starting the AM
    // so that we don't learn about a conflict via the a web server port
    // conflict.

    WebServer webServer = new WebServer(dispatcher);
    try {
      webServer.start();
    } catch (Exception e) {
      LOG.error("Web server setup failed, exiting: " + e.getMessage(), e);
      System.exit(-1);
    }

    // Run the dispatcher until the cluster shuts down.

    try {
      dispatcher.run();
    } catch (Throwable e) {
      LOG.error("Fatal error, exiting: " + e.getMessage(), e);
      System.exit(-1);
    } finally {
      try {
        webServer.close();
      } catch (Exception e) {
        // Ignore
      }
    }
  }
}
