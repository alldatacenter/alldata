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
package org.apache.drill.exec.store.phoenix;

import static org.junit.Assert.assertFalse;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.hadoop.fs.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.LoggerFactory;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class PhoenixBaseTest extends ClusterTest {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PhoenixBaseTest.class);

  public final static String U_U_I_D = UUID.randomUUID().toString();
  private final static AtomicInteger initCount = new AtomicInteger(0);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    PhoenixTestSuite.initPhoenixQueryServer();
    if (PhoenixTestSuite.isRunningSuite()) {
      QueryServerBasicsIT.testCatalogs();
    }
    startDrillCluster();
    if (initCount.incrementAndGet() == 1) {
      createSchema(QueryServerBasicsIT.CONN_STRING);
      createTables(QueryServerBasicsIT.CONN_STRING);
      createSampleData(QueryServerBasicsIT.CONN_STRING);
    }
  }

  @AfterClass
  public static void tearDownCluster() throws Exception {
    if (!PhoenixTestSuite.isRunningSuite()) {
      PhoenixTestSuite.tearDownCluster();
    }
  }

  public static void startDrillCluster() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
    Map<String, Object> props = Maps.newHashMap();
    props.put("phoenix.query.timeoutMs", 90000);
    props.put("phoenix.query.keepAliveMs", "30000");
    StoragePluginRegistry registry = cluster.drillbit().getContext().getStorage();
    PhoenixStoragePluginConfig config = new PhoenixStoragePluginConfig(null, 0, null, null,
      QueryServerBasicsIT.CONN_STRING, null, props);
    config.setEnabled(true);
    registry.put(PhoenixStoragePluginConfig.NAME + "123", config);
    dirTestWatcher.copyResourceToRoot(Paths.get(""));
  }

  public static void createSchema(String connString) throws Exception {
    try (final Connection connection = DriverManager.getConnection(connString)) {
      logger.debug("Phoenix connection established with the specified url : {}", connString);
      assertFalse(connection.isClosed());
      connection.setAutoCommit(true);
      try (final Statement stmt = connection.createStatement()) {
        assertFalse(stmt.execute("CREATE SCHEMA IF NOT EXISTS V1"));
      }
    }
  }

  public static void createTables(String connString) throws Exception {
    try (final Connection connection = DriverManager.getConnection(connString)) {
      assertFalse(connection.isClosed());
      connection.setAutoCommit(true);
      try (final Statement stmt = connection.createStatement()) {
        String region_sql = " CREATE TABLE V1.REGION "
            + "("
            + "    R_REGIONKEY BIGINT not null,"
            + "    R_NAME      VARCHAR,"
            + "    R_COMMENT   VARCHAR"
            + "    CONSTRAINT  REGION_PK PRIMARY KEY (R_REGIONKEY)"
            + ")";
        String nation_sql = " CREATE TABLE V1.NATION "
            + "("
            + "    N_NATIONKEY BIGINT not null primary key,"
            + "    N_NAME      VARCHAR(100),"
            + "    N_REGIONKEY BIGINT,"
            + "    N_COMMENT   VARCHAR(255)"
            + ")";
        String datatype_sql = " CREATE TABLE V1.DATATYPE "
            + "("
            + "    T_UUID      VARCHAR not null primary key,"
            + "    T_VARCHAR   VARCHAR,"
            + "    T_CHAR      CHAR(5),"
            + "    T_BIGINT    BIGINT,"
            + "    T_INTEGER   INTEGER,"
            + "    T_SMALLINT  SMALLINT,"
            + "    T_TINYINT   TINYINT,"
            + "    T_DOUBLE    DOUBLE,"
            + "    T_FLOAT     FLOAT,"
            + "    T_DECIMAL   DECIMAL(4,2),"
            + "    T_DATE      DATE,"
            + "    T_TIME      TIME,"
            + "    T_TIMESTAMP TIMESTAMP,"
            + "    T_BINARY    BINARY(10),"
            + "    T_VARBINARY VARBINARY,"
            + "    T_BOOLEAN   BOOLEAN"
            + ")";
        String arrytype_sql = " CREATE TABLE V1.ARRAYTYPE "
            + "("
            + "    T_UUID      VARCHAR not null primary key,"
            + "    T_VARCHAR   VARCHAR ARRAY,"
            + "    T_CHAR      CHAR(5) ARRAY,"
            + "    T_BIGINT    BIGINT  ARRAY,"
            + "    T_INTEGER   INTEGER ARRAY,"
            + "    T_DOUBLE    DOUBLE  ARRAY,"
            + "    T_SMALLINT  SMALLINT ARRAY,"
            + "    T_TINYINT   TINYINT  ARRAY,"
            + "    T_BOOLEAN   BOOLEAN  ARRAY"
            + ")";
        assertFalse(stmt.execute(region_sql));
        assertFalse(stmt.execute(nation_sql));
        assertFalse(stmt.execute(datatype_sql));
        assertFalse(stmt.execute(arrytype_sql));
      }
    }
  }

  public static void createSampleData(String connString) throws Exception {
    final String[] paths = new String[] { "data/region.tbl", "data/nation.tbl" };
    final String[] sqls = new String[] {
        "UPSERT INTO V1.REGION VALUES(?,?,?)",
        "UPSERT INTO V1.NATION VALUES(?,?,?,?)",
        "UPSERT INTO V1.DATATYPE VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        "UPSERT INTO V1.ARRAYTYPE VALUES(?,?,ARRAY['a','b','c'],?,?,?,?,?,?)",
    };

    Path region_path = new Path(dirTestWatcher.getRootDir().getAbsolutePath(), paths[0]);
    Path nation_path = new Path(dirTestWatcher.getRootDir().getAbsolutePath(), paths[1]);
    logger.info("Loading the .tbl file : " + Arrays.toString(paths));

    List<String[]> allRows = parseTblFile(String.valueOf(region_path));
    try (final Connection connection = DriverManager.getConnection(connString)) {
      assertFalse(connection.isClosed());
      connection.setAutoCommit(false);
      // region table
      try (final PreparedStatement pstmt = connection.prepareStatement(sqls[0])) {
        for (String[] row : allRows) {
          pstmt.setLong(1, Long.valueOf(row[0]));
          pstmt.setString(2, row[1]);
          pstmt.setString(3, row[2]);
          pstmt.addBatch();
        }
        pstmt.executeBatch();
      }
      connection.commit();
      // nation table
      allRows = parseTblFile(String.valueOf(nation_path));
      try (final PreparedStatement pstmt = connection.prepareStatement(sqls[1])) {
        for (String[] row : allRows) {
          pstmt.setLong(1, Long.valueOf(row[0]));
          pstmt.setString(2, row[1]);
          pstmt.setLong(3, Long.valueOf(row[2]));
          pstmt.setString(4, row[3]);
          pstmt.addBatch();
        }
        pstmt.executeBatch();
      }
      connection.commit();
      // datatype table
      try (final PreparedStatement pstmt = connection.prepareStatement(sqls[2])) {
        pstmt.setString(1, U_U_I_D);
        pstmt.setString(2, "apache");
        pstmt.setString(3, "drill");
        pstmt.setLong(4, Long.MAX_VALUE);
        pstmt.setInt(5, Integer.MAX_VALUE);
        pstmt.setShort(6, Short.MAX_VALUE);
        pstmt.setByte(7, Byte.MAX_VALUE);
        pstmt.setDouble(8, Double.MAX_VALUE);
        pstmt.setFloat(9, Float.MAX_VALUE);
        pstmt.setBigDecimal(10, BigDecimal.valueOf(10.11));
        pstmt.setDate(11, java.sql.Date.valueOf("2021-12-12"));
        pstmt.setTime(12, java.sql.Time.valueOf("12:12:12"));
        pstmt.setTimestamp(13, java.sql.Timestamp.valueOf("2021-12-12 12:12:12"));
        pstmt.setBytes(14, "a_b_c_d_e_".getBytes());
        pstmt.setBytes(15, "12345".getBytes());
        pstmt.setBoolean(16, Boolean.TRUE);
        pstmt.addBatch();
        pstmt.executeBatch();
      }
      connection.commit();
      // arraytype table
      try (final PreparedStatement pstmt = connection.prepareStatement(sqls[3])) {
        Array t_varchar = connection.createArrayOf("VARCHAR", new String[] { "apache", "drill", "1.20" });
        @SuppressWarnings("unused")
        Array t_char = connection.createArrayOf("CHAR", new String[] { "a", "b", "c" }); // PHOENIX-6607
        Array t_bigint = connection.createArrayOf("BIGINT", new Long[] { Long.MIN_VALUE, Long.MAX_VALUE });
        Array t_integer = connection.createArrayOf("INTEGER", new Integer[] { Integer.MIN_VALUE, Integer.MAX_VALUE });
        Array t_double = connection.createArrayOf("DOUBLE", new Double[] { Double.MIN_VALUE, Double.MAX_VALUE });
        @SuppressWarnings("unused")
        Array t_float = connection.createArrayOf("FLOAT", new Float[] { Float.MIN_VALUE, Float.MAX_VALUE }); // PHOENIX-6606
        Array t_smallint = connection.createArrayOf("SMALLINT", new Short[] { Short.MIN_VALUE, Short.MAX_VALUE });
        Array t_tinyint = connection.createArrayOf("TINYINT", new Byte[] { Byte.MIN_VALUE, Byte.MAX_VALUE });
        Array t_boolean = connection.createArrayOf("BOOLEAN", new Boolean[] { Boolean.TRUE, Boolean.FALSE });
        pstmt.setString(1, U_U_I_D);
        pstmt.setArray(2, t_varchar);
        pstmt.setArray(3, t_bigint);
        pstmt.setArray(4, t_integer);
        pstmt.setArray(5, t_double);
        pstmt.setArray(6, t_smallint);
        pstmt.setArray(7, t_tinyint);
        pstmt.setArray(8, t_boolean);
        pstmt.addBatch();
        pstmt.executeBatch();
      }
      connection.commit();
      logger.info("Loaded {} rows.", allRows.size());
    }
  }

  private static List<String[]> parseTblFile(String path) throws Exception {
    CsvParserSettings settings = new CsvParserSettings();
    settings.getFormat().setDelimiter("|");
    settings.getFormat().setLineSeparator("\n");
    CsvParser parser = new CsvParser(settings);

    return parser.parseAll(getReader(path));
  }

  private static Reader getReader(String path) throws Exception {
    return new InputStreamReader(new FileInputStream(path), "UTF-8");
  }
}
