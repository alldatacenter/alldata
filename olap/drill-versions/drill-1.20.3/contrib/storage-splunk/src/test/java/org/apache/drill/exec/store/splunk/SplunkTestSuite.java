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

package org.apache.drill.exec.store.splunk;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicInteger;


@RunWith(Suite.class)
@Suite.SuiteClasses({
  SplunkConnectionTest.class,
  SplunkQueryBuilderTest.class,
  SplunkLimitPushDownTest.class,
  SplunkIndexesTest.class,
  SplunkPluginTest.class,
  SplunkTestSplunkUtils.class
})

@Category({SlowTest.class})
public class SplunkTestSuite extends ClusterTest {
  private static final Logger logger = LoggerFactory.getLogger(SplunkTestSuite.class);

  protected static SplunkPluginConfig SPLUNK_STORAGE_PLUGIN_CONFIG = null;
  public static final String SPLUNK_LOGIN = "admin";
  public static final String SPLUNK_PASS = "password";

  private static volatile boolean runningSuite = true;
  private static AtomicInteger initCount = new AtomicInteger(0);
  @ClassRule
  public static GenericContainer<?> splunk = new GenericContainer<>(DockerImageName.parse("splunk/splunk:8.1"))
          .withExposedPorts(8089, 8089)
          .withEnv("SPLUNK_START_ARGS", "--accept-license")
          .withEnv("SPLUNK_PASSWORD", SPLUNK_PASS);

  @BeforeClass
  public static void initSplunk() throws Exception {
    synchronized (SplunkTestSuite.class) {
      if (initCount.get() == 0) {
        startCluster(ClusterFixture.builder(dirTestWatcher));
        splunk.start();
        String hostname = splunk.getHost();
        Integer port = splunk.getFirstMappedPort();
        StoragePluginRegistry pluginRegistry = cluster.drillbit().getContext().getStorage();
        SPLUNK_STORAGE_PLUGIN_CONFIG = new SplunkPluginConfig(SPLUNK_LOGIN, SPLUNK_PASS, hostname, port, "1", "now",
                null, 4);
        SPLUNK_STORAGE_PLUGIN_CONFIG.setEnabled(true);
        pluginRegistry.put(SplunkPluginConfig.NAME, SPLUNK_STORAGE_PLUGIN_CONFIG);
        runningSuite = true;
        logger.info("Take a time to ready more Splunk events (10 sec)...");
        Thread.sleep(10000);
      }
      initCount.incrementAndGet();
      runningSuite = true;
    }
    logger.info("Initialized Splunk in Docker container");
  }

  @AfterClass
  public static void tearDownCluster() {
    synchronized (SplunkTestSuite.class) {
      if (initCount.decrementAndGet() == 0) {
        splunk.close();
      }
    }
  }

  public static boolean isRunningSuite() {
    return runningSuite;
  }
}