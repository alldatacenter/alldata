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
package org.apache.drill.exec.store.cassandra;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.testcontainers.containers.CassandraContainer;

@Category(SlowTest.class)
@RunWith(Suite.class)
@Suite.SuiteClasses({CassandraComplexTypesTest.class, CassandraPlanTest.class, CassandraQueryTest.class})
public class TestCassandraSuite extends BaseTest {

  protected static CassandraContainer<?> cassandra;

  private static final AtomicInteger initCount = new AtomicInteger(0);

  private static volatile boolean runningSuite = false;

  @BeforeClass
  public static void initCassandra() {
    synchronized (TestCassandraSuite.class) {
      if (initCount.get() == 0) {
        startCassandra();
      }
      initCount.incrementAndGet();
      runningSuite = true;
    }
  }

  public static boolean isRunningSuite() {
    return runningSuite;
  }

  @AfterClass
  public static void tearDownCluster() {
    synchronized (TestCassandraSuite.class) {
      if (initCount.decrementAndGet() == 0 && cassandra != null) {
        cassandra.stop();
      }
    }
  }

  private static void startCassandra() {
    cassandra = new CassandraContainer<>("cassandra")
      .withInitScript("queries.cql")
      .withStartupTimeout(Duration.ofMinutes(2))
      .withEnv("CASSANDRA_SNITCH", "GossipingPropertyFileSnitch") // Tune Cassandra options for faster startup
      .withEnv("JVM_OPTS", "-Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0");
    cassandra.start();
  }
}
