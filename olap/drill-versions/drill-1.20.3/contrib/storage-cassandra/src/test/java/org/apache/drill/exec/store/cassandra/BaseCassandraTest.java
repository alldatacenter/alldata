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

import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.CassandraContainer;

public class BaseCassandraTest extends ClusterTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    TestCassandraSuite.initCassandra();
    initCassandraPlugin(TestCassandraSuite.cassandra);
  }

  private static void initCassandraPlugin(CassandraContainer<?> cassandra) throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));

    CassandraStorageConfig config = new CassandraStorageConfig(
        cassandra.getHost(),
        cassandra.getMappedPort(CassandraContainer.CQL_PORT),
        cassandra.getUsername(),
        cassandra.getPassword(),
        null);
    config.setEnabled(true);
    cluster.defineStoragePlugin("cassandra", config);
  }

  @AfterClass
  public static void tearDownCassandra() {
    if (TestCassandraSuite.isRunningSuite()) {
      TestCassandraSuite.tearDownCluster();
    }
  }
}
