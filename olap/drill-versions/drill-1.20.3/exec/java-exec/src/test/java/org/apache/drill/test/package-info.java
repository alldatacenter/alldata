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
/**
 * <p>
 *   Provides a variety of test framework tools to simplify Drill unit
 *   tests and ad-hoc tests created while developing features. Key components
 *   include:
 * </p>
 * <ul>
 *   <li>
 *     {@link org.apache.drill.test.ClusterFixtureBuilder}: Builder pattern to create an embedded Drillbit,
 *     or cluster of Drillbits, using a specified set of configuration, session
 *     and system options.
 *   </li>
 *   <li>
 *     {@link org.apache.drill.test.ClusterFixture}: The cluster created by the builder.
 *   </li>
 *   <li>
 *     {@link org.apache.drill.test.ClientFixture}: A facade to the Drill client that provides
 *     convenience methods for setting session options, running queries and
 *     so on. A client is associated with a cluster. If tests desire, multiple
 *     clients can be created for a single cluster, though most need just one
 *     client. A builder exists for clients, but most tests get the client
 *     directly from the cluster.
 *   </li>
 *   <li>
 *     {@link org.apache.drill.test.QueryBuilder}: a builder pattern for constructing and
 *     running any form of query (SQL, logical or physical) and running the
 *     query in a wide variety of ways (just count the rows, return the
 *     results as a list, run using a listener, etc.)
 *   </li>
 *   <li>
 *     {@link org.apache.drill.test.QueryBuilder.QuerySummary QuerySummary}: a summary of a
 *     query returned from running the query. Contains the query ID, the
 *     row count, the batch count and elapsed run time.
 *   </li>
 *   <li>
 *     {@link org.apache.drill.test.ProfileParser}: A simple tool to load a query profile and
 *     provide access to the profile structure. Also prints the key parts of
 *     the profile for diagnostic purposes.
 *   </li>
 *   <li>
 *     {@link org.apache.drill.test.LogFixture}: Allows per-test changes to log settings to,
 *     say, send a particular logger to the console for easier debugging, or
 *     to suppress logging of a deliberately created failure.
 *   </li>
 *   <li>
 *     {@link org.apache.drill.test.BaseDirTestWatcher}: Creates temporary directories which are used for the following
 *     aspects of drill in unit tests:
 *     <ul>
 *       <li>
 *         The <b>drill.tmp-dir</b> property: A temp directory ({@link org.apache.drill.test.BaseDirTestWatcher#getTmpDir()}) is created and
 *         configured as the tmp directory for drill bits.
 *       </li>
 *       <li>
 *         The <b>drill.exec.sys.store.provider.local.path</b> property: A temp directory ({@link org.apache.drill.test.BaseDirTestWatcher#getStoreDir()}) is created and
 *         configured as the store directory for drillbits.
 *       </li>
 *       <li>
 *         The <b>dfs.default</b> workspace: A temp directory ({@link org.apache.drill.test.BaseDirTestWatcher#getRootDir()} is created and configured as the directory
 *         for this workspace.
 *       </li>
 *       <li>
 *         The <b>dfs.root</b> workspace: <b>dfs.root</b> is configured to use the same temp directory ad <b>dfs.default</b>.
 *       </li>
 *       <li>
 *         The <b>dfs.tmp</b> workspace: A temp directory ({@link org.apache.drill.test.BaseDirTestWatcher#getDfsTestTmpDir()}) is created and configured as the
 *         directory for this workspace.
 *       </li>
 *     </ul>
 *     By default a {@link org.apache.drill.test.BaseDirTestWatcher} is used in tests that extend {@link org.apache.drill.test.BaseTestQuery} and tests that use
 *     the {@link org.apache.drill.test.ClusterFixture}.
 *   </li>
 * </ul>
 * <h3>Usage</h3>
 * <p>
 *   A typical test using this framework looks like this:
 * </p>
 * <code><pre>
  {@literal @}org.junit.Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  {@literal @}Test
  public void exampleTest() throws Exception {
    createEmployeeCsv(dirTestWatcher.getRootDir());

    // Configure the cluster. One Drillbit by default.
    FixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        // Set up per-test specialized config and session options.
        .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true)
        .configProperty(ExecConstants.REMOVER_ENABLE_GENERIC_COPIER, true)
        .sessionOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 3L * 1024 * 1024 * 1024)
        .maxParallelization(1);

    // Launch the cluster and client.
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {

      // Run a query (using the mock data source) and print a summary.
      String sql = "SELECT id_i FROM dfs.`employee.csv` ORDER BY id_i";
      QuerySummary summary = client.queryBuilder().sql(sql).run();
      assertEquals(1_000_000, summary.recordCount());
      System.out.println(String.format("Sorted %,d records in %d batches.", summary.recordCount(), summary.batchCount()));
      System.out.println(String.format("Query Id: %s, elapsed: %d ms", summary.queryIdString(), summary.runTimeMs()));
      client.parseProfile(summary.queryIdString()).print();
    }
  }
 * </pre></code>
 * Typical usage for the logging fixture:
 * <pre><code>
 * {@literal @}Test
 * public void myTest() {
 *   LogFixtureBuilder logBuilder = LogFixture.builder()
 *          .toConsole()
 *          .disable() // Silence all other loggers
 *          .logger(ExternalSortBatch.class, Level.DEBUG);
 *   try (LogFixture logs = logBuilder.build()) {
 *     // Test code here
 *   }
 * }
 * </code></pre>
 *
 */
package org.apache.drill.test;
