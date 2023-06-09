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
package org.apache.drill.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.RootAllocator;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.LogFixture.LogFixtureBuilder;
import org.apache.drill.test.QueryBuilder.QuerySummary;
import org.apache.drill.exec.physical.impl.xsort.ExternalSortBatch;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.file.JsonFileBuilder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ch.qos.logback.classic.Level;

/**
 * Example test case using the Drill cluster fixture. Your test case
 * can be stand-alone (as here) or can inherit from DrillTest if you
 * want test-by-test messages. Don't use BaseTestQuery, it will attempt
 * to set up a Drillbit for you, which is not needed here.
 * <p>
 * There is nothing magic about running these items as tests, other than
 * that JUnit makes it very easy to run one test at a time. You can also
 * just launch the test as a Java program as shown in the <tt>main()</tt>
 * method at the end of the file.
 * <p>
 * Note also that each test sets up its own Drillbit. Of course, if you
 * have a series of test that all use the same Drilbit configuration,
 * you can create your cluster fixture in a JUnit <tt>{@literal @}Before</tt>
 * method, and shut it down in <tt>{@literal @}After</tt> method.
 * <p>
 */

// Note: Test itself is ignored because this is an example, not a
// real test.

@Ignore
public class ExampleTest extends BaseTest {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExampleTest.class);

  /**
   * This test watcher creates all the temp directories that are required for an integration test with a Drillbit. The
   * {@link ClusterFixture} and {@link BaseTestQuery} classes automatically configure their Drillbits to use the temp
   * directories created by this test watcher. Please see {@link BaseDirTestWatcher} and package-info.java. Please see
   * {@link #secondTest()} for an example.
   */
  @Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  /**
   * Example of the simplest possible test case: set up a default
   * cluster (with one Drillbit), a corresponding client, run a
   * query and print the results.
   *
   * @throws Exception if anything goes wrong
   */

  @Test
  public void firstTest() throws Exception {
    try (ClusterFixture cluster = ClusterFixture.standardCluster(dirTestWatcher);
         ClientFixture client = cluster.clientFixture()) {
      client.queryBuilder().sql("SELECT * FROM `cp`.`employee.json` LIMIT 10").logCsv();
    }
  }

  /**
   * <p>
   *   Example that uses the fixture builder to build a cluster fixture. Lets
   *   you set configuration (boot-time) options, session options, system options
   *   and more.
   * </p>
   * <p>
   *   You can write test files to the {@link BaseDirTestWatcher#getRootDir()} and query them in the test.
   * </p>
   * <p>
   *   Also shows how to display the plan JSON and just run a query silently,
   *   getting just the row count, batch count and run time.
   * </p>
   * @throws Exception if anything goes wrong
   */

  @Test
  public void secondTest() throws Exception {
    try (RootAllocator allocator = new RootAllocator(100_000_000)) {
      final File tableFile = dirTestWatcher
        .getRootDir()
        .toPath()
        .resolve("employee.json")
        .toFile();

      final TupleMetadata schema = new SchemaBuilder()
        .add("id", Types.required(TypeProtos.MinorType.VARCHAR))
        .add("name", Types.required(TypeProtos.MinorType.VARCHAR))
        .buildSchema();

      final RowSet rowSet = new RowSetBuilder(allocator, schema)
        .addRow("1", "kiwi")
        .addRow("2", "watermelon")
        .build();

      new JsonFileBuilder(rowSet).build(tableFile);
      rowSet.clear();

      ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher).configProperty(ExecConstants.SLICE_TARGET, 10);

      try (ClusterFixture cluster = builder.build();
           ClientFixture client = cluster.clientFixture()) {
        String sql = "SELECT * FROM `dfs`.`test/employee.json`";
        logger.info(client.queryBuilder().sql(sql).explainJson());
        QuerySummary results = client.queryBuilder().sql(sql).run();
        logger.info(String.format("Read %d rows", results.recordCount()));
        // Usually we want to test something. Here, just test that we got
        // the 2 records.
        assertEquals(2, results.recordCount());
      }
    }
  }

  /**
   * Example test using the SQL mock data source. For now, we support just two
   * column types:
   * <ul>
   * <li>Integer: _i</li>
   * <li>String (Varchar): _sn, where n is the field width.</li>
   * </ul>
   * Row count is encoded in the table name with an optional "K" or "M"
   * suffix for bigger row count numbers.
   * <p>
   * The mock data source is defined automatically by the cluster fixture.
   * <p>
   * There is another, more sophisticated, way to generate test data using
   * a mock data source defined in a JSON file. We'll add an example for
   * that later.
   *
   * @throws Exception if anything goes wrong
   */

  @Test
  public void thirdTest() throws Exception {
    try (ClusterFixture cluster = ClusterFixture.standardCluster(dirTestWatcher);
         ClientFixture client = cluster.clientFixture()) {
      String sql = "SELECT id_i, name_s10 FROM `mock`.`employees_5`";
      client.queryBuilder().sql(sql).logCsv();
    }
  }

  /**
   * Example using custom logging. Here we run a sort with trace logging enabled
   * for just the sort class, and with logging displayed to the console.
   * <p>
   * This example also shows setting up a realistic set of options prior to
   * running a query. Note that we pass in normal Java values (don't have to
   * encode the values as a string.)
   * <p>
   * Finally, also shows defining your own ad-hoc local file workspace to
   * point to a sample data file.
   * <p>
   * Unlike the other tests, don't actually run this one. It points to
   * a location on a local machine. And, the query itself takes 23 minutes
   * to run if you had the right data file...
   *
   * @throws Exception if anything goes wrong
   */
  @Test
  public void fourthTest() throws Exception {
    LogFixtureBuilder logBuilder = LogFixture.builder()
        // Log to the console for debugging convenience
        .toConsole()
        // All debug messages in the xsort package
        .logger("org.apache.drill.exec.physical.impl.xsort", Level.DEBUG)
        // And trace messages for one class.
        .logger(ExternalSortBatch.class, Level.TRACE);

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        // Easy way to run single threaded for easy debugging
        .maxParallelization(1)
        // Set some session options
        .sessionOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY, 2L * 1024 * 1024 * 1024)
        .sessionOption(PlannerSettings.EXCHANGE.getOptionName(), true)
        .sessionOption(PlannerSettings.HASHAGG.getOptionName(), false);

    try (LogFixture logs = logBuilder.build();
         ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      setupFile();
      cluster.defineWorkspace("dfs", "data", "/tmp/drill-test", "psv");
      String sql = "select * from `dfs.data`.`example.tbl` order by columns[0]";
      QuerySummary results = client.queryBuilder().sql(sql).run();
      assertEquals( 2, results.recordCount() );
    }
  }

  // Create a local file that can be pointed to by the dfs.data plugin config.
  // In real life, you would already have the file and not need to create it
  // here.

  private void setupFile() {
    File destFile = new File( "/tmp/drill-test/example.tbl" );
    destFile.getParentFile().mkdirs();
    try (PrintWriter out = new PrintWriter(new FileWriter(destFile))) {
      out.println("20|def");
      out.println("10|abc");
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Example of a more realistic test that limits parallization, saves the query
   * profile, parses it, and displays the runtime timing results per operator.
   *
   * @throws Exception if anything goes wrong
   */
  @Test
  public void fifthTest() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .maxParallelization(1)
        .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String sql = "SELECT id_i, name_s10 FROM `mock`.`employees_10K` ORDER BY id_i";

      QuerySummary summary = client.queryBuilder().sql(sql).run();
      logger.info(String.format("Results: %,d records, %d batches, %,d ms", summary.recordCount(), summary.batchCount(), summary.runTimeMs() ) );

      logger.info("Query ID: " + summary.queryIdString());
      ProfileParser profile = client.parseProfile(summary.queryIdString());
      profile.print();
    }
  }

  /**
   * This example shows how to define a workspace that points to test files in src/main/resources.
   */
  @Test
  public void sixthTest() throws Exception {
    try (ClusterFixture cluster = ClusterFixture.standardCluster(dirTestWatcher);
         ClientFixture client = cluster.clientFixture()) {
      cluster.defineWorkspace("dfs", "resources", TestTools.TEST_RESOURCES_ABS.toFile().getAbsolutePath(), "tsv");
      client.queryBuilder().sql("SELECT * from dfs.resources.`testframework/small_test_data.tsv`").logCsv();
    }
  }

  /**
   * This example shows how to define a workspace that points to test files in the sample-data folder.
   */
  @Test
  public void seventhTest() throws Exception {
    try (ClusterFixture cluster = ClusterFixture.standardCluster(dirTestWatcher);
         ClientFixture client = cluster.clientFixture()) {
      cluster.defineWorkspace("dfs", "sampledata", TestTools.SAMPLE_DATA.toFile().getAbsolutePath(), "parquet");
      client.queryBuilder().sql("SELECT * from dfs.sampledata.`nation.parquet`").logCsv();
    }
  }

  /**
   * Example of running a specific test as Java program. Handy if you want to
   * run the test from the command line, or if your test runs so long that JUnit
   * would kill it with a timeout.
   * <p>
   * The key point is that the unit test framework has no dependencies on test
   * classes, on JUnit annotations, etc.
   *
   * @param args not used
   */
  public static void main(String args) {
    try {
      new ExampleTest().firstTest();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
