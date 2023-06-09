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
package org.apache.drill.jdbc;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.drill.exec.store.SchemaFactory;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.jdbc.impl.DrillConnectionImpl;
import org.apache.drill.jdbc.impl.DrillConnectionUtils;
import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.calcite.linq4j.Ord;
import org.apache.drill.categories.JdbcTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.logical.LogicalPlan;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.jdbc.test.Hook;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

// TODO:  Document this, especially what writers of unit tests need to know
//   (e.g., the reusing of connections, the automatic interception of test
//   failures and resetting of connections, etc.).
@Category(JdbcTest.class)
public class JdbcTestBase extends ExecTest {
  @SuppressWarnings("unused")
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JdbcTestBase.class);

  @Rule
  public final TestRule watcher = new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
      reset();
    }
  };

  private static CachingConnectionFactory factory;

  @BeforeClass
  public static void setUpTestCase() {
    factory = new SingleConnectionCachingFactory(
        info -> {
          Connection connection = DriverManager.getConnection(info.getUrl(), info.getParamsAsProperties());
          updateSchemaLocations(connection);
          return connection;
        });
  }

  /**
   * Creates a {@link java.sql.Connection connection} using default parameters.
   * @param url connection URL
   * @throws SQLException if connection fails
   */
  protected static Connection connect(String url) throws SQLException {
    return connect(url, getDefaultProperties());
  }

  protected static Connection connect() throws SQLException {
    return connect("jdbc:drill:zk=local", getDefaultProperties());
  }

  /**
   * Creates a {@link java.sql.Connection connection} using the given parameters.
   * @param url connection URL
   * @param info connection info
   * @throws SQLException if connection fails
   */
  protected static Connection connect(String url, Properties info) throws SQLException {
    final Connection conn = factory.getConnection(new ConnectionInfo(url, info));
    changeSchemaIfSupplied(conn, info);
    return conn;
  }

  private static void updateSchemaLocations(Connection conn) {
    if (conn instanceof DrillConnectionImpl) {
      StoragePluginRegistry storage = DrillConnectionUtils.getStorage((DrillConnectionImpl) conn);
      try {
        StoragePluginTestUtils.configureFormatPlugins(storage);
        StoragePluginTestUtils.updateSchemaLocation(StoragePluginTestUtils.DFS_PLUGIN_NAME, storage,
            dirTestWatcher.getDfsTestTmpDir(), StoragePluginTestUtils.TMP_SCHEMA);
        StoragePluginTestUtils.updateSchemaLocation(StoragePluginTestUtils.DFS_PLUGIN_NAME, storage,
            dirTestWatcher.getRootDir(), StoragePluginTestUtils.ROOT_SCHEMA);
        StoragePluginTestUtils.updateSchemaLocation(StoragePluginTestUtils.DFS_PLUGIN_NAME, storage,
            dirTestWatcher.getRootDir(), SchemaFactory.DEFAULT_WS_NAME);
      } catch (StoragePluginRegistry.PluginException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Changes schema of the given connection if the field "schema" is present in {@link java.util.Properties info}.
   * Does nothing otherwise.
   */
  protected static void changeSchemaIfSupplied(Connection conn, Properties info) {
    final String schema = info.getProperty("schema", null);
    if (!Strings.isNullOrEmpty(schema)) {
      changeSchema(conn, schema);
    }
  }

  // TODO:  Purge nextUntilEnd(...) and calls when remaining fragment race
  // conditions are fixed (not just DRILL-2245 fixes).
  ///**
  // * Calls {@link ResultSet#next} on given {@code ResultSet} until it returns
  // * false.  (For TEMPORARY workaround for query cancelation race condition.)
  // */
  //private static void nextUntilEnd(final ResultSet resultSet) throws SQLException {
  //  while (resultSet.next()) {
  //  }
  //}

  protected static void changeSchema(Connection conn, String schema) {
    final String query = String.format("use %s", schema);
    try (Statement s = conn.createStatement()) {
      @SuppressWarnings("unused")
      ResultSet r = s.executeQuery(query);
      // TODO:  Purge nextUntilEnd(...) and calls when remaining fragment
      // race conditions are fixed (not just DRILL-2245 fixes).
      // nextUntilEnd(r);
    } catch (SQLException e) {
      throw new RuntimeException("unable to change schema", e);
    }
  }

  /**
   * Resets the factory closing all of the active connections.
   */
  protected static void reset() {
    try {
      factory.closeConnections();
    } catch (SQLException e) {
      throw new RuntimeException("error while closing connection factory", e);
    }
  }

  @AfterClass
  public static void tearDownTestCase() throws Exception {
    factory.closeConnections();
  }

  /**
   * Returns default bag of properties that is passed to JDBC connection.
   * By default, includes options to:
   * - turn off the web server
   * - indicate DrillConnectionImpl to set up dfs.tmp schema location to an exclusive dir just for this test jvm
   */
  public static Properties getDefaultProperties() {
    final Properties properties = new Properties();

    properties.setProperty(ExecConstants.DRILL_TMP_DIR, dirTestWatcher.getTmpDir().getAbsolutePath());
    properties.setProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, "false");
    properties.setProperty(ExecConstants.HTTP_ENABLE, "false");
    return properties;
  }

  public static ModelAndSchema withModel(final String model, final String schema) {
    final Properties info = getDefaultProperties();
    info.setProperty("schema", schema);
    info.setProperty("model", "inline:" + model);
    return new ModelAndSchema(info, factory);
  }

  public static ModelAndSchema withFull(final String schema) {
    final Properties info = getDefaultProperties();
    info.setProperty("schema", schema);
    return new ModelAndSchema(info, factory);
  }

  public static ModelAndSchema withNoDefaultSchema() {
    return new ModelAndSchema(getDefaultProperties(), factory);
  }

  public static String toString(ResultSet resultSet, int expectedRecordCount) throws SQLException {
    final StringBuilder buf = new StringBuilder();
    while (resultSet.next()) {
      final ResultSetMetaData metaData = resultSet.getMetaData();
      final int n = metaData.getColumnCount();
      String sep = "";
      for (int i = 1; i <= n; i++) {
        buf.append(sep)
          .append(metaData.getColumnLabel(i))
          .append("=")
          .append(resultSet.getObject(i));
        sep = "; ";
      }
      buf.append("\n");
    }
    return buf.toString();
  }

  public static String toString(ResultSet resultSet) throws SQLException {
    StringBuilder buf = new StringBuilder();
    final List<Ord<String>> columns = columnLabels(resultSet);
    while (resultSet.next()) {
      for (Ord<String> column : columns) {
        buf.append(column.i == 1 ? "" : "; ").append(column.e).append("=").append(resultSet.getObject(column.i));
      }
      buf.append("\n");
    }
    return buf.toString();
  }

  static Set<String> toStringSet(ResultSet resultSet) throws SQLException {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    final List<Ord<String>> columns = columnLabels(resultSet);
    while (resultSet.next()) {
      StringBuilder buf = new StringBuilder();
      for (Ord<String> column : columns) {
        buf.append(column.i == 1 ? "" : "; ").append(column.e).append("=").append(resultSet.getObject(column.i));
      }
      builder.add(buf.toString());
      buf.setLength(0);
    }
    return builder.build();
  }

  static List<String> toStrings(ResultSet resultSet) throws SQLException {
    final List<String> list = new ArrayList<>();
    StringBuilder buf = new StringBuilder();
    final List<Ord<String>> columns = columnLabels(resultSet);
    while (resultSet.next()) {
      buf.setLength(0);
      for (Ord<String> column : columns) {
        buf.append(column.i == 1 ? "" : "; ").append(column.e).append("=").append(resultSet.getObject(column.i));
      }
      list.add(buf.toString());
    }
    return list;
  }

  private static List<Ord<String>> columnLabels(ResultSet resultSet) throws SQLException {
    int n = resultSet.getMetaData().getColumnCount();
    List<Ord<String>> columns = new ArrayList<>();
    for (int i = 1; i <= n; i++) {
      columns.add(Ord.of(i, resultSet.getMetaData().getColumnLabel(i)));
    }
    return columns;
  }

  public static class ModelAndSchema {
    private final Properties info;
    private final ConnectionFactoryAdapter adapter;

    public ModelAndSchema(final Properties info, final ConnectionFactory factory) {
      this.info = info;
      this.adapter = () -> factory.getConnection(
          new ConnectionInfo("jdbc:drill:zk=local", ModelAndSchema.this.info));
    }

    public TestDataConnection sql(String sql) {
      return new TestDataConnection(adapter, sql);
    }

    public <T> T withConnection(Function<Connection, T> function) throws Exception {
      try (Connection connection = adapter.createConnection()) {
        return function.apply(connection);
      }
    }
  }

  public static class TestDataConnection {
    private final ConnectionFactoryAdapter adapter;
    private final String sql;

    TestDataConnection(ConnectionFactoryAdapter adapter, String sql) {
      this.adapter = adapter;
      this.sql = sql;
    }

    /**
     * Checks that the current SQL statement returns the expected result.
     */
    public TestDataConnection returns(String expected) throws Exception {
      try (Connection connection = adapter.createConnection();
           Statement statement = connection.createStatement()) {
        ResultSet resultSet = statement.executeQuery(sql);
        expected = expected.trim();
        String result = JdbcTestBase.toString(resultSet).trim();
        resultSet.close();

        if (!expected.equals(result)) {
          Assert.fail(String.format("Generated string:\n%s\ndoes not match:\n%s", result, expected));
        }
        return this;
      }
    }

    public TestDataConnection returnsSet(Set<String> expected) throws Exception {
      try (Connection connection = adapter.createConnection();
           Statement statement = connection.createStatement()) {
        ResultSet resultSet = statement.executeQuery(sql);
        Set<String> result = JdbcTestBase.toStringSet(resultSet);
        resultSet.close();

        if (!expected.equals(result)) {
          Assert.fail(String.format("Generated set:\n%s\ndoes not match:\n%s", result, expected));
        }
        return this;
      }
    }

    /**
     * Checks that the current SQL statement returns the expected result lines. Lines are compared unordered; the test
     * succeeds if the query returns these lines in any order.
     */
    public TestDataConnection returnsUnordered(String... expecteds) throws Exception {
      try (Connection connection = adapter.createConnection();
           Statement statement = connection.createStatement()) {
        ResultSet resultSet = statement.executeQuery(sql);
        Assert.assertEquals(unsortedList(Arrays.asList(expecteds)), unsortedList(JdbcTestBase.toStrings(resultSet)));
        resultSet.close();
        return this;
      }
    }

    public TestDataConnection displayResults(int recordCount) throws Exception {
      // record count check is done in toString method

      try (Connection connection = adapter.createConnection();
           Statement statement = connection.createStatement()) {
        ResultSet resultSet = statement.executeQuery(sql);
        logger.debug(JdbcTestBase.toString(resultSet, recordCount));
        resultSet.close();
        return this;
      }
    }

    private SortedSet<String> unsortedList(List<String> strings) {
      final SortedSet<String> set = new TreeSet<>();
      for (String string : strings) {
        set.add(string + "\n");
      }
      return set;
    }

    public LogicalPlan logicalPlan() {
      final String[] plan0 = {null};
      try (Connection connection = adapter.createConnection();
           Statement statement = connection.prepareStatement(sql);
           Hook.Closeable x = Hook.LOGICAL_PLAN.add(
        (Function<String, Void>) o -> {
          plan0[0] = o;
          return null;
        })) {
        statement.close();
        final String plan = plan0[0].trim();
        return LogicalPlan.parse(PhysicalPlanReaderTestFactory.defaultLogicalPlanPersistence(DrillConfig.create()), plan);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public <T extends LogicalOperator> T planContains(final Class<T> operatorClazz) {
      return (T) logicalPlan().getSortedOperators().stream()
          .filter(input -> input.getClass().equals(operatorClazz))
          .findFirst()
          .get();
    }
  }

  private interface ConnectionFactoryAdapter {
    Connection createConnection() throws Exception;
  }

  /**
   * Test of whether tests that get connection from JdbcTest.connect(...)
   * work with resetting of connections.  If enabling this (failing) test method
   * causes other test methods to fail, something needs to be fixed.
   * (Note:  Not a guaranteed test--depends on order in which test methods are
   * run.)
   */
  @Ignore("Usually disabled; enable temporarily to check tests")
  @Test
  public void testJdbcTestConnectionResettingCompatibility() {
    fail("Intentional failure--did other test methods still run?");
  }
}
