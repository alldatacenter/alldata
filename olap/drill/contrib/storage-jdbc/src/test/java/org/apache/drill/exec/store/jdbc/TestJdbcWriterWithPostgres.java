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
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(JdbcStorageTest.class)
public class TestJdbcWriterWithPostgres extends ClusterTest {

  private static final String DOCKER_IMAGE_POSTGRES_X86 = "postgres:12.8-alpine3.14";
  private static JdbcDatabaseContainer<?> jdbcContainer;

  @BeforeClass
  public static void initPostgres() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    dirTestWatcher.copyResourceToRoot(Paths.get(""));

    String postgresDBName = "drill_postgres_test";
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    DockerImageName imageName = DockerImageName.parse(DOCKER_IMAGE_POSTGRES_X86);
    jdbcContainer = new PostgreSQLContainer<>(imageName)
      .withUsername("postgres")
      .withPassword("password")
      .withDatabaseName(postgresDBName)
      .withInitScript("postgres-test-data.sql");
    jdbcContainer.start();

    JdbcStorageConfig jdbcStorageConfig =
      new JdbcStorageConfig("org.postgresql.Driver",
        jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword(),
        true, true,null, null, 10000);
    jdbcStorageConfig.setEnabled(true);
    cluster.defineStoragePlugin("pg", jdbcStorageConfig);

    JdbcStorageConfig unWritableJdbcStorageConfig =
      new JdbcStorageConfig("org.postgresql.Driver",
        jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword(),
        true, false,null, null, 10000);
    unWritableJdbcStorageConfig.setEnabled(true);
    cluster.defineStoragePlugin("pg_unwritable", unWritableJdbcStorageConfig);

  }

  @AfterClass
  public static void stopPostgres() {
    if (jdbcContainer != null) {
      jdbcContainer.stop();
    }
  }

  @Test
  public void testBasicCTAS() throws Exception {
    String query = "CREATE TABLE pg.`public`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM pg.`public`.`test_table`";
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
    String dropQuery = "DROP TABLE pg.`public`.`test_table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testBasicCTASWithDataTypes() throws Exception {
    String query = "CREATE TABLE pg.public.`data_types` AS " +
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
    String testQuery = "SELECT * FROM  pg.`public`.`data_types`";
    DirectRowSet results = queryBuilder().sql(testQuery).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("int_field", MinorType.INT, 10)
      .addNullable("bigint_field", MinorType.BIGINT, 19)
      .addNullable("float4_field", MinorType.FLOAT8, 17, 17)
      .addNullable("float8_field", MinorType.FLOAT8, 17, 17)
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
    String dropQuery = "DROP TABLE pg.`public`.`data_types`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testBasicCTASWithSpacesInTableName() throws Exception {
    String query = "CREATE TABLE pg.public.`test table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM pg.public.`test table`";
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
    String dropQuery = "DROP TABLE pg.public.`test table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  // The insert limit for Postgres is 1000 rows per INSERT query
  @Test
  public void testWithLargeFile() throws Exception {
    String query = "CREATE TABLE pg.public.test (id,first_name,last_name,email,gender,ip_address) AS " +
      "SELECT id,first_name,last_name,email,gender,ip_address FROM cp.`csv/large_csv.csvh`";
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    query = "SELECT COUNT(*) FROM pg.public.test";
    long rowCount = queryBuilder().sql(query).singletonLong();
    assertEquals(6000, rowCount);

    // Now drop the table
    String dropQuery = "DROP TABLE pg.public.`test`";
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
      fail();
    }
    // Query the table to see if the insertion was successful
    String testQuery = "SELECT COUNT(*) FROM dfs.`csv/very_large_file.csvh`";
    long resultsCount = queryBuilder().sql(testQuery).singletonLong();
    assertEquals(100000, resultsCount);

    String ctasQuery = "CREATE TABLE pg.public.`test_big_table` AS " +
      "SELECT * FROM dfs.`csv/very_large_file.csvh`";
    QuerySummary insertResults = queryBuilder().sql(ctasQuery).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    testQuery = "SELECT COUNT(*) FROM pg.public.`test_big_table`";
    resultsCount = queryBuilder().sql(testQuery).singletonLong();
    assertEquals(100000, resultsCount);

    String dropQuery = "DROP TABLE pg.public.`test_big_table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());

    boolean deletedFile = JdbcTestUtils.deleteCsvFile(String.valueOf(generatedFile));
    if (!deletedFile) {
      fail();
    }
  }

  @Test
  public void testBasicCTASWithSpacesInFieldNames() throws Exception {
    String query = "CREATE TABLE pg.public.`test table` (`My id`, `My name`) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM pg.public.`test table`";
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
    String dropQuery = "DROP TABLE pg.public.`test table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testCTASFromFileWithNulls() throws Exception {
    String sql = "CREATE TABLE pg.public.`t1` AS SELECT int_field, float_field, varchar_field, boolean_field FROM cp.`json/dataTypes.json`";
    QuerySummary insertResults = queryBuilder().sql(sql).run();
    assertTrue(insertResults.succeeded());

    sql = "SELECT * FROM pg.public.`t1`";
    DirectRowSet results = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("int_field", MinorType.BIGINT, 19)
      .addNullable("float_field", MinorType.FLOAT8, 22)
      .addNullable("varchar_field", MinorType.VARCHAR, 38)
      .addNullable("boolean_field", MinorType.BIT, 1)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 1.0, "foo1", true)
      .addRow(null, null, null, null)
      .addRow(2L, 2.0, "foo2", false)
      .build();

    RowSetUtilities.verify(expected, results);

    String dropQuery = "DROP TABLE pg.`public`.`t1`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testDropNonExistentTable() throws Exception {
    String dropQuery = "DROP TABLE pg.`public`.`none_shall_pass`";
    try {
      queryBuilder().sql(dropQuery).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("VALIDATION ERROR: Table [none_shall_pass] not found"));
    }
  }

  @Test
  public void testBasicCTASIfNotExists() throws Exception {
    String query = "CREATE TABLE IF NOT EXISTS pg.`public`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    // Create the table and insert the values
    QuerySummary insertResults = queryBuilder().sql(query).run();
    assertTrue(insertResults.succeeded());

    // Query the table to see if the insertion was successful
    String testQuery = "SELECT * FROM  pg.`public`.`test_table`";
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
    String dropQuery = "DROP TABLE pg.`public`.`test_table`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testCTASWithDuplicateTable() throws Exception {
    String query = "CREATE TABLE pg.`public`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
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
    query = "CREATE TABLE IF NOT EXISTS pg.`public`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
    DirectRowSet results = queryBuilder().sql(query).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("ok", MinorType.BIT)
      .add("summary", MinorType.VARCHAR, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(false, "A table or view with given name [test_table] already exists in schema [pg.public]")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testWithComplexData() throws Exception {
    // JDBC Writer does not support writing complex types at this time.
    try {
      String sql = "CREATE TABLE pg.`public`.`complex` AS SELECT * FROM cp.`json/complexData.json`";
      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("DATA_WRITE ERROR: Drill does not support writing complex fields to JDBC data sources."));
    }
  }

  @Test
  public void testCTASFromFileWithUglyData() throws Exception {
    String sql = "CREATE TABLE pg.public.`t2` AS SELECT ugly1, ugly2 FROM cp.`json/uglyData.json`";
    QuerySummary insertResults = queryBuilder().sql(sql).run();
    assertTrue(insertResults.succeeded());

    sql = "SELECT * FROM  pg.public.`t2`";
    DirectRowSet results = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("ugly1", MinorType.VARCHAR, 38)
      .addNullable("ugly2", MinorType.VARCHAR, 38)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("O'Malley", "Abraham Lincoln's best speech started with: \"Four score and seven years ago...")
      .build();

    RowSetUtilities.verify(expected, results);

    String dropQuery = "DROP TABLE  pg.public.`t2`";
    QuerySummary dropResults = queryBuilder().sql(dropQuery).run();
    assertTrue(dropResults.succeeded());
  }

  @Test
  public void testWithArrayField() throws Exception {
    // JDBC Writer does not support writing arrays at this time.
    try {
      String sql = "CREATE TABLE pg.`public`.`complex` AS SELECT * FROM cp.`json/repeatedData.json`";
      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("DATA_WRITE ERROR: Drill does not yet support writing arrays to JDBC. `repeated_field` is an array."));
    }
  }

  @Test
  public void testUnwritableConnection() throws Exception {
    try {
      String query = "CREATE TABLE IF NOT EXISTS pg_unwritable.public.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
      queryBuilder().sql(query).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("VALIDATION ERROR: Unable to create or drop objects. Schema [pg_unwritable.public] is immutable."));
    }

    try {
      String query = "CREATE TABLE pg_unwritable.`public`.`test_table` (ID, NAME) AS SELECT * FROM (VALUES(1,2), (3,4))";
      queryBuilder().sql(query).run();
      fail();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains("VALIDATION ERROR: Unable to create or drop objects. Schema [pg_unwritable.public] is immutable."));
    }
  }
}
