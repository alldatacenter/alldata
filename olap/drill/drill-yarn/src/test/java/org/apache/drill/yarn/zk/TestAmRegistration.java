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
package org.apache.drill.yarn.zk;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.curator.test.TestingServer;
import org.apache.drill.test.BaseTest;
import org.apache.drill.yarn.appMaster.AMRegistrar.AMRegistrationException;
import org.junit.Test;

public class TestAmRegistration extends BaseTest {
  private static final String TEST_CLUSTER_ID = "drillbits";
  private static final String TEST_ZK_ROOT = "drill";
  private static final String TEST_AM_HOST = "localhost";
  private static final int TEST_AM_PORT = 8048;
  private static final String TEST_APP_ID = "Application_001";

  private static final String PROBE_AM_HOST = "somehost";
  private static final String PROBE_APP_ID = "Application_002";
  private static final int PROBE_AM_PORT = 8049;
  private static final String PROBE_CLUSTER_ID = "second";
  private static final String PROBE_ZK_ROOT = "myRoot";

  @Test
  public void testAMRegistry() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();
      String connStr = server.getConnectString();
      ZKClusterCoordinatorDriver driver = new ZKClusterCoordinatorDriver()
          .setConnect(connStr, TEST_ZK_ROOT, TEST_CLUSTER_ID).build();

      // Register an AM using the above.

      driver.register(TEST_AM_HOST, TEST_AM_PORT, TEST_APP_ID);

      // Simulate a second AM for the same cluster.

      {
        ZKClusterCoordinatorDriver driver2 = new ZKClusterCoordinatorDriver()
            .setConnect(connStr, TEST_ZK_ROOT, TEST_CLUSTER_ID).build();

        // Register an AM on the same (root, cluster id).

        try {
          driver.register(PROBE_AM_HOST, PROBE_AM_PORT, PROBE_APP_ID);
          fail();
        } catch (AMRegistrationException e) {
          String msg = e.getMessage();
          assertTrue(msg.contains("Application Master already exists"));
          assertTrue(
              msg.contains(" " + TEST_ZK_ROOT + "/" + TEST_CLUSTER_ID + " "));
          assertTrue(msg.contains(" host: " + TEST_AM_HOST));
          assertTrue(msg.contains(" Application ID: " + TEST_APP_ID));
        }

        driver2.close();
      }

      {
        ZKClusterCoordinatorDriver driver2 = new ZKClusterCoordinatorDriver()
            .setConnect(connStr, TEST_ZK_ROOT, PROBE_CLUSTER_ID).build();

        // Register an AM on a different cluster id, same root.

        try {
          driver2.register(PROBE_AM_HOST, PROBE_AM_PORT, PROBE_APP_ID);
        } catch (AMRegistrationException e) {
          fail("Registration should be OK");
        }

        driver2.close();
      }

      {
        ZKClusterCoordinatorDriver driver2 = new ZKClusterCoordinatorDriver()
            .setConnect(connStr, PROBE_ZK_ROOT, TEST_CLUSTER_ID).build();

        // Register an AM on a different root.

        try {
          driver2.register(PROBE_AM_HOST, PROBE_AM_PORT, PROBE_APP_ID);
        } catch (AMRegistrationException e) {
          fail("Registration should be OK");
        }

        driver2.close();
      }

      // First AM exits.

      driver.close();

      {
        // Should be able to register an AM for the same cluster.

        ZKClusterCoordinatorDriver driver2 = new ZKClusterCoordinatorDriver()
            .setConnect(connStr, TEST_ZK_ROOT, TEST_CLUSTER_ID).build();

        // Register an AM on the same (root, cluster id).

        try {
          driver2.register(PROBE_AM_HOST, PROBE_AM_PORT, PROBE_APP_ID);
        } catch (AMRegistrationException e) {
          fail("Registration should be OK");
        }

        driver2.close();
      }

      server.stop();
    }
  }

}
