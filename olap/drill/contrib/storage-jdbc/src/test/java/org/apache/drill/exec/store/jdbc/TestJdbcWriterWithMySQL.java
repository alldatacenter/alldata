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

package org.apache.drill.exec.store.jdbc;

import org.apache.drill.categories.JdbcStorageTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder.QuerySummary;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.hadoop.fs.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JDBC storage plugin tests against MySQL.
 * Note: it requires libaio1.so library on Linux
 */

@Category(JdbcStorageTest.class)
public class TestJdbcWriterWithMySQL extends ClusterTest {
  private static final String DOCKER_IMAGE_MYSQL = "mysql:5.7.27";
  private static final String DOCKER_IMAGE_MARIADB = "mariadb:10.6.0";
  private static final Logger logger = LoggerFactory.getLogger(TestJdbcWriterWithMySQL.class);
  private static JdbcDatabaseContainer<?> jdbcContainer;
  @BeforeClass
  public static void initMysql() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    dirTestWatcher.copyResourceToRoot(Paths.get(""));

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    String osName = System.getProperty("os.name").toLowerCase();
    String mysqlDBName = "drill_mysql_test";

    DockerImageName imageName;
    if (osName.startsWith("linux") && "aarch64".equals(System.getProperty("os.arch"))) {
      imageName = DockerImageName.parse(DOCKER_IMAGE_MARIADB).asCompatibleSubstituteFor("mysql");
    } else {
      imageName = DockerImageName.parse(DOCKER_IMAGE_MYSQL);
    }

    jdbcContainer = new MySQLContainer<>(imageName)
      .withExposedPorts(3306)
      .withConfigurationOverride("mysql_config_override")
      .withUsername("mysqlUser")
      .withPassword("mysqlPass")
      .withDatabaseName(mysqlDBName)
      .withUrlParam("serverTimezone", "UTC")
      .withUrlParam("useJDBCCompliantTimezoneShift", "true")
      .withInitScript("mysql-test-data.sql");
    jdbcContainer.start();

    if (osName.startsWith("linux")) {
      JdbcDatabaseDelegate databaseDelegate = new JdbcDatabaseDelegate(jdbcContainer, "");
      ScriptUtils.runInitScript(databaseDelegate, "mysql-test-data-linux.sql");
    }

    String jdbcUrl = jdbcContainer.getJdbcUrl();
    logger.debug("JDBC URL: {}", jdbcUrl);
    JdbcStorageConfig jdbcStorageConfig = new JdbcStorageConfig("com.mysql.cj.jdbc.Driver", jdbcUrl,
      jdbcContainer.getUsername(), jdbcContainer.getPassword(), false, true, null, null, 10000);
    jdbcStorageConfig.setEnabled(true);

    cluster.defineStoragePlugin("mysql", jdbcStorageConfig);

    JdbcStorageConfig jdbcStorageConfigNoWrite = new JdbcStorageConfig("com.mysql.cj.jdbc.Driver", jdbcUrl,
      jdbcContainer.getUsername(), jdbcContainer.getPassword(), false, false, null, null, 10000);
    jdbcStorageConfigNoWrite.setEnabled(true);

    cluster.defineStoragePlugin("mysql_no_write", jdbcStorageConfigNoWrite);

    if (osName.startsWith("linux")) {
      // adds storage plugin with case insensitive table names
      JdbcStorageConfig jdbcCaseSensitiveStorageConfig = new JdbcStorageConfig("com.mysql.cj.jdbc.Driver", jdbcUrl,
        jdbcContainer.getUsername(), jdbcContainer.getPassword(), true, true, null, null, 10000);
      jdbcCaseSensitiveStorageConfig.setEnabled(true);
      cluster.defineStoragePlugin("mysqlCaseInsensitive", jdbcCaseSensitiveStorageConfig);
    }
  }

  @Test
  public void testBasicCTAS() throws Exception {
    String query = "CREATE TABLE mysql.`drill_mysql_test`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM  mysql.`drill_mysql_test`.`test_table`";
    DirectRowSet results = queryBuilder().sql(testQuery).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("ID", MinorType.BIGINT, DataMode.OPTIONAL)
      .add("NAME", MinorType.BIGINT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 2L)
      .addRow(3L, 4L)
      .build();

    RowSetUtilities.verify(expected, results);

    // Now drop the table
    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`test_table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testBasicCTASWithSpacesInTableName() throws Exception {
    String query = "CREATE TABLE mysql.`drill_mysql_test`.`test table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM  mysql.`drill_mysql_test`.`test table`";
    DirectRowSet results = queryBuilder().sql(testQuery).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("ID", MinorType.BIGINT, DataMode.OPTIONAL)
      .add("NAME", MinorType.BIGINT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 2L)
      .addRow(3L, 4L)
      .build();

    RowSetUtilities.verify(expected, results);

    // Now drop the table
    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`test table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testBasicCTASWithSpacesInFieldNames() throws Exception {
    String query = "CREATE TABLE mysql.`drill_mysql_test`.`test table` (`My id`, `My name`) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM  mysql.`drill_mysql_test`.`test table`";
    DirectRowSet results = queryBuilder().sql(testQuery).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("My id", MinorType.BIGINT, DataMode.OPTIONAL)
      .add("My name", MinorType.BIGINT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 2L)
      .addRow(3L, 4L)
      .build();

    RowSetUtilities.verify(expected, results);

    // Now drop the table
    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`test table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  @Ignore("Requires local installation of MySQL")
  public void testBasicCTASWithLocalDatabase() throws Exception {
    // Local databases
    String localMySql = "jdbc:mysql://localhost:3306/?useJDBCCompliantTimezoneShift=true&serverTimezone=EST5EDT";
    JdbcStorageConfig localJdbcStorageConfig = new JdbcStorageConfig("com.mysql.cj.jdbc.Driver", localMySql,
      "root", "password", false, true, null, null, 10000);
    localJdbcStorageConfig.setEnabled(true);

    cluster.defineStoragePlugin("localMysql", localJdbcStorageConfig);

    String query = "CREATE TABLE localMysql.`drill_mysql_test`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM  localMysql.`drill_mysql_test`.`test_table`";
    DirectRowSet results = queryBuilder().sql(testQuery).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("ID", MinorType.BIGINT, DataMode.OPTIONAL)
      .add("NAME", MinorType.BIGINT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 2L)
      .addRow(3L, 4L)
      .build();

    RowSetUtilities.verify(expected, results);

    // Now drop the table
    String dropQuery = "DROP TABLE localMysql.`drill_mysql_test`.`test_table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testBasicCTASWithDataTypes() throws Exception {
    String query = "CREATE TABLE mysql.drill_mysql_test.`data_types` AS " +
      "SELECT CAST(1 AS INTEGER) AS int_field," +
      "CAST(2 AS BIGINT) AS bigint_field," +
      "CAST(3.0 AS FLOAT) AS float4_field," +
      "CAST(4.0 AS DOUBLE) AS float8_field," +
      "'5.0' AS varchar_field," +
      "CAST('2021-01-01' AS DATE) as date_field," +
      "CAST('12:00:00' AS TIME) as time_field, " +
      "CAST('2015-12-30 22:55:55.23' AS TIMESTAMP) as timestamp_field, true AS boolean_field " +
      "FROM (VALUES(1))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM  mysql.`drill_mysql_test`.`data_types`";
    DirectRowSet results = queryBuilder().sql(testQuery).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("int_field", MinorType.INT, 10)
      .addNullable("bigint_field", MinorType.BIGINT, 19)
      .addNullable("float4_field", MinorType.FLOAT8, 12)
      .addNullable("float8_field", MinorType.FLOAT8, 22)
      .addNullable("varchar_field", MinorType.VARCHAR, 38)
      .addNullable("date_field", MinorType.DATE, 10)
      .addNullable("time_field", MinorType.TIME, 10)
      .addNullable("timestamp_field", MinorType.TIMESTAMP, 19)
      .addNullable("boolean_field", MinorType.BIT)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1, 2L, 3.0, 4.0, "5.0", LocalDate.parse("2021-01-01"), LocalTime.parse("12:00"), 1451516155000L, true)
      .build();

    RowSetUtilities.verify(expected, results);

    // Now drop the table
    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`data_types`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testCTASFromFileWithNulls() throws Exception {
    String sql = "CREATE TABLE mysql.drill_mysql_test.`t1` AS SELECT int_field, float_field, varchar_field, boolean_field FROM cp.`json/dataTypes.json`";
    QuerySummary insertResults = queryBuilder().sql(sql).run();
    assertTrue(insertResults.succeeded());

    sql = "SELECT * FROM mysql.drill_mysql_test.`t1`";
    DirectRowSet results = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("int_field", MinorType.BIGINT, 19)
      .addNullable("float_field", MinorType.FLOAT8, 22)
      .addNullable("varchar_field", MinorType.VARCHAR, 38,0)
      .addNullable("boolean_field", MinorType.BIT, 1)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 1.0, "foo1", true)
      .addRow(null, null, null, null)
      .addRow(2L, 2.0, "foo2", false)
      .build();

    RowSetUtilities.verify(expected, results);

    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`t1`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testCTASFromFileWithUglyData() throws Exception {
    String sql = "CREATE TABLE mysql.drill_mysql_test.`t2` AS SELECT ugly1, ugly2 FROM cp.`json/uglyData.json`";
    QuerySummary insertResults = queryBuilder().sql(sql).run();
    assertTrue(insertResults.succeeded());

    sql = "SELECT * FROM mysql.drill_mysql_test.`t2`";
    DirectRowSet results = queryBuilder().sql(sql).rowSet();

   TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("ugly1", MinorType.VARCHAR, 38)
      .addNullable("ugly2", MinorType.VARCHAR, 38)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("O'Malley", "Abraham Lincoln's best speech started with: \"Four score and seven years ago...")
      .build();

    RowSetUtilities.verify(expected, results);

    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`t2`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testDropNonExistentTable() throws Exception {
    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`none_shall_pass`";
    try {
      queryBuilder().sql(dropQuery).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("VALIDATION ERROR: Table [none_shall_pass] not found"));
    }
  }

  @Test
  public void testBasicCTASIfNotExists() throws Exception {
    String query = "CREATE TABLE IF NOT EXISTS mysql.`drill_mysql_test`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM  mysql.`drill_mysql_test`.`test_table`";
    DirectRowSet results = queryBuilder().sql(testQuery).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("ID", MinorType.BIGINT, DataMode.OPTIONAL)
      .add("NAME", MinorType.BIGINT, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 2L)
      .addRow(3L, 4L)
      .build();

    RowSetUtilities.verify(expected, results);

    // Now drop the table
    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`test_table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testCTASWithDuplicateTable() throws Exception {
    String query = "CREATE TABLE mysql.`drill_mysql_test`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Run the query again, should fail.
    try {
      queryBuilder().sql(query).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("VALIDATION ERROR"));
    }

    // Try again with IF NOT EXISTS, Should not do anything, but not throw an exception
    query = "CREATE TABLE IF NOT EXISTS mysql.`drill_mysql_test`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    DirectRowSet results = queryBuilder().sql(query).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("ok", MinorType.BIT)
      .add("summary", MinorType.VARCHAR, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(false, "A table or view with given name [test_table] already exists in schema [mysql.drill_mysql_test]")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testWithComplexData() throws Exception {
    // JDBC Writer does not support writing complex types at this time.
    try {
      String sql = "CREATE TABLE mysql.`drill_mysql_test`.`complex` AS SELECT * FROM cp.`json/complexData.json`";
      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("DATA_WRITE ERROR: Drill does not support writing complex fields to JDBC data sources."));
    }
  }

  @Test
  public void testWithArrayField() throws Exception {
    // JDBC Writer does not support writing arrays at this time.
    try {
      String sql = "CREATE TABLE mysql.`drill_mysql_test`.`complex` AS SELECT * FROM cp.`json/repeatedData.json`";
      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("DATA_WRITE ERROR: Drill does not yet support writing arrays to JDBC. `repeated_field` is an array."));
    }
  }

  @Test
  public void testUnwritableConnection() throws Exception {
    try {
      String query = "CREATE TABLE IF NOT EXISTS mysql_no_write.`drill_mysql_test`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
      queryBuilder().sql(query).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("VALIDATION ERROR: Unable to create or drop objects. Schema [mysql_no_write.drill_mysql_test] is immutable."));
    }

    try {
      String query = "CREATE TABLE mysql_no_write.`drill_mysql_test`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
      queryBuilder().sql(query).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("VALIDATION ERROR: Unable to create or drop objects. Schema [mysql_no_write.drill_mysql_test] is immutable."));
    }
  }

  @Test
  public void testWithLargeFile() throws Exception {
    String query = "CREATE TABLE mysql.`drill_mysql_test`.test (id,first_name,last_name,email,gender,ip_address) AS " +
      "SELECT id,first_name,last_name,email,gender,ip_address FROM cp.`csv/large_csv.csvh`";
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    query = "SELECT COUNT(*) FROM mysql.`drill_mysql_test`.test";
    long rowCount = queryBuilder().sql(query).singletonLong();
    assertEquals(6000, rowCount);

    // Now drop the table
    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`test`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  @Ignore("This is a slow test.  Please run manually.")
  public void testWithReallyLongFile() throws Exception {
    Path generatedFile = null;
    try {
      generatedFile = JdbcTestUtils.generateCsvFile("csv/very_large_file.csvh", 10, 100000);
    } catch (IOException e) {
      logger.error(e.getMessage());
      fail();
    }
    // Query the table to see if the insertion was successful
    String testQuery = "SELECT COUNT(*) FROM dfs.`csv/very_large_file.csvh`";
    long resultsCount = queryBuilder().sql(testQuery).singletonLong();
    assertEquals(100000, resultsCount);

    String ctasQuery = "CREATE TABLE mysql.`drill_mysql_test`.`test_big_table` AS " +
      "SELECT * FROM dfs.`csv/very_large_file.csvh`";
    QuerySummary insertResults = queryBuilder().sql(ctasQuery).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    testQuery = "SELECT COUNT(*) FROM mysql.`drill_mysql_test`.`test_big_table`";
    resultsCount = queryBuilder().sql(testQuery).singletonLong();
    assertEquals(100000, resultsCount);

    String dropQuery = "DROP TABLE mysql.`drill_mysql_test`.`test_big_table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());

    boolean deletedFile = JdbcTestUtils.deleteCsvFile(String.valueOf(generatedFile));
    if (!deletedFile) {
      fail();
    }
  }

  @AfterClass
  public static void stopMysql() {
    if (jdbcContainer != null) {
      jdbcContainer.stop();
    }
  }
}
