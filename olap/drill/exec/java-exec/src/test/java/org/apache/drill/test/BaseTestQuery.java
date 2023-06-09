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

import static org.apache.drill.exec.util.StoragePluginTestUtils.ROOT_SCHEMA;
import static org.apache.drill.exec.util.StoragePluginTestUtils.TMP_SCHEMA;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.UserProtos.PreparedStatementHandle;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.user.AwaitableUserResultsListener;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.store.SchemaFactory;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.exec.util.VectorUtil;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.drill.test.DrillTestWrapper.TestServices;
import org.apache.hadoop.fs.FileSystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * deprecated Use {@link ClusterTest} instead.
 *
 * But, not marked as deprecated because it is still widely used.
 */
//@Deprecated
public class BaseTestQuery extends ExecTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseTestQuery.class);

  private static final int MAX_WIDTH_PER_NODE = 2;

  @SuppressWarnings("serial")
  private static final Properties TEST_CONFIGURATIONS = new Properties() {
    {
      put(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, "false");
      put(ExecConstants.HTTP_ENABLE, "false");
      // Increasing retry attempts for testing
      put(ExecConstants.UDF_RETRY_ATTEMPTS, "10");
      put(ExecConstants.SSL_USE_HADOOP_CONF, "false");
    }
  };

  protected static DrillClient client;
  protected static Drillbit[] bits;
  protected static RemoteServiceSet serviceSet;
  protected static DrillConfig config;
  protected static BufferAllocator allocator;

  /**
   * Number of Drillbits in test cluster. Default is 1.
   *
   * Tests can update the cluster size through {@link #updateTestCluster(int, DrillConfig)}
   */
  private static int drillbitCount = 1;

  private int[] columnWidths = new int[] { 8 };

  private static ScanResult classpathScan;

  @BeforeClass
  public static void setupDefaultTestCluster() throws Exception {
    config = DrillConfig.create(cloneDefaultTestConfigProperties());
    classpathScan = ClassPathScanner.fromPrescan(config);
    openClient();
    // turns on the verbose errors in tests
    // sever side stacktraces are added to the message before sending back to the client
    test("ALTER SESSION SET `exec.errors.verbose` = true");
  }

  protected static void updateTestCluster(int newDrillbitCount, DrillConfig newConfig) {
    updateTestCluster(newDrillbitCount, newConfig, cloneDefaultTestConfigProperties());
  }

  protected static void updateTestCluster(int newDrillbitCount, DrillConfig newConfig, Properties properties) {
    Preconditions.checkArgument(newDrillbitCount > 0, "Number of Drillbits must be at least one");
    if (drillbitCount != newDrillbitCount || config != null) {
      // TODO: Currently we have to shutdown the existing Drillbit cluster before starting a new one with the given
      // Drillbit count. Revisit later to avoid stopping the cluster.
      try {
        closeClient();
        drillbitCount = newDrillbitCount;
        if (newConfig != null) {
          // For next test class, updated DrillConfig will be replaced by default DrillConfig in BaseTestQuery as part
          // of the @BeforeClass method of test class.
          config = newConfig;
        }
        openClient(properties);
      } catch (Exception e) {
        throw new RuntimeException("Failure while updating the test Drillbit cluster.", e);
      }
    }
  }

  /**
   * Useful for tests that require a DrillbitContext to get/add storage plugins, options etc.
   *
   * @return DrillbitContext of first Drillbit in the cluster.
   */
  protected static DrillbitContext getDrillbitContext() {
    Preconditions.checkState(bits != null && bits[0] != null, "Drillbits are not setup.");
    return bits[0].getContext();
  }

  protected static Properties cloneDefaultTestConfigProperties() {
    Properties props = new Properties();

    for (String propName : TEST_CONFIGURATIONS.stringPropertyNames()) {
      props.put(propName, TEST_CONFIGURATIONS.getProperty(propName));
    }

    props.setProperty(ExecConstants.DRILL_TMP_DIR, dirTestWatcher.getTmpDir().getAbsolutePath());
    props.setProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_PATH, dirTestWatcher.getStoreDir().getAbsolutePath());
    props.setProperty(ExecConstants.UDF_DIRECTORY_ROOT, dirTestWatcher.getHomeDir().getAbsolutePath());
    props.setProperty(ExecConstants.UDF_DIRECTORY_FS, FileSystem.DEFAULT_FS);

    return props;
  }

  private static void openClient() throws Exception {
    openClient(null);
  }

  private static void openClient(Properties properties) throws Exception {
    if (properties == null) {
      properties = new Properties();
    }

    allocator = RootAllocatorFactory.newRoot(config);
    serviceSet = RemoteServiceSet.getLocalServiceSet();

    dirTestWatcher.newDfsTestTmpDir();

    bits = new Drillbit[drillbitCount];
    for (int i = 0; i < drillbitCount; i++) {
      bits[i] = new Drillbit(config, serviceSet, classpathScan);
      bits[i].run();

      StoragePluginRegistry pluginRegistry = bits[i].getContext().getStorage();
      StoragePluginTestUtils.configureFormatPlugins(pluginRegistry);

      StoragePluginTestUtils.updateSchemaLocation(StoragePluginTestUtils.DFS_PLUGIN_NAME, pluginRegistry,
        dirTestWatcher.getDfsTestTmpDir(), TMP_SCHEMA);
      StoragePluginTestUtils.updateSchemaLocation(StoragePluginTestUtils.DFS_PLUGIN_NAME, pluginRegistry,
        dirTestWatcher.getRootDir(), ROOT_SCHEMA);
      StoragePluginTestUtils.updateSchemaLocation(StoragePluginTestUtils.DFS_PLUGIN_NAME, pluginRegistry,
        dirTestWatcher.getRootDir(), SchemaFactory.DEFAULT_WS_NAME);
    }

    if (!properties.containsKey(DrillProperties.DRILLBIT_CONNECTION)) {
      properties.setProperty(DrillProperties.DRILLBIT_CONNECTION,
          String.format("localhost:%s", bits[0].getUserPort()));
    }

    DrillConfig clientConfig = DrillConfig.forClient();
    client = QueryTestUtil.createClient(clientConfig,  serviceSet, MAX_WIDTH_PER_NODE, properties);
  }

  /**
   * Close the current <i>client</i> and open a new client using the given <i>properties</i>. All tests executed
   * after this method call use the new <i>client</i>.
   *
   * @param properties
   */
  public static void updateClient(Properties properties) throws Exception {
    Preconditions.checkState(bits != null && bits[0] != null, "Drillbits are not setup.");
    if (client != null) {
      client.close();
      client = null;
    }

    DrillConfig clientConfig = DrillConfig.forClient();
    client = QueryTestUtil.createClient(clientConfig, serviceSet, MAX_WIDTH_PER_NODE, properties);
  }

  /*
   * Close the current <i>client</i> and open a new client for the given user. All tests executed
   * after this method call use the new <i>client</i>.
   * @param user
   */
  public static void updateClient(String user) throws Exception {
    updateClient(user, null);
  }

  /*
   * Close the current <i>client</i> and open a new client for the given user and password credentials. Tests
   * executed after this method call use the new <i>client</i>.
   * @param user
   */
  public static void updateClient(String user, String password) throws Exception {
    Properties props = new Properties();
    props.setProperty(DrillProperties.USER, user);
    if (password != null) {
      props.setProperty(DrillProperties.PASSWORD, password);
    }
    updateClient(props);
  }

  protected static BufferAllocator getAllocator() {
    return allocator;
  }

  public static int getUserPort() {
    return bits[0].getUserPort();
  }

  public static TestBuilder newTest() {
    return testBuilder();
  }


  public static class ClassicTestServices implements TestServices {
    @Override
    public BufferAllocator allocator() {
      return allocator;
    }

    @Override
    public void test(String query) throws Exception {
      BaseTestQuery.test(query);
    }

    @Override
    public List<QueryDataBatch> testRunAndReturn(QueryType type, Object query) throws Exception {
      return BaseTestQuery.testRunAndReturn(type, query);
    }
  }

  public static TestBuilder testBuilder() {
    return new TestBuilder(new ClassicTestServices());
  }

  @AfterClass
  public static void closeClient() throws Exception {
    if (client != null) {
      client.close();
      client = null;
    }

    if (bits != null) {
      for (Drillbit bit : bits) {
        if (bit != null) {
          bit.close();
        }
      }
      bits = null;
    }

    if (serviceSet != null) {
      serviceSet.close();
      serviceSet = null;
    }
    if (allocator != null) {
      allocator.close();
      allocator = null;
    }
  }

  @AfterClass
  public static void resetDrillbitCount() {
    // some test classes assume this value to be 1 and will fail if run along other tests that increase it
    drillbitCount = 1;
  }

  protected static void runSQL(String sql) throws Exception {
    AwaitableUserResultsListener listener = new AwaitableUserResultsListener(new SilentListener());
    testWithListener(QueryType.SQL, sql, listener);
    listener.await();
  }

  protected static List<QueryDataBatch> testSqlWithResults(String sql) throws Exception{
    return testRunAndReturn(QueryType.SQL, sql);
  }

  protected static List<QueryDataBatch> testLogicalWithResults(String logical) throws Exception{
    return testRunAndReturn(QueryType.LOGICAL, logical);
  }

  protected static List<QueryDataBatch> testPhysicalWithResults(String physical) throws Exception{
    return testRunAndReturn(QueryType.PHYSICAL, physical);
  }

  public static List<QueryDataBatch>  testRunAndReturn(QueryType type, Object query) throws Exception{
    if (type == QueryType.PREPARED_STATEMENT) {
      Preconditions.checkArgument(query instanceof PreparedStatementHandle,
          "Expected an instance of PreparedStatement as input query");
      return testPreparedStatement((PreparedStatementHandle)query);
    } else {
      Preconditions.checkArgument(query instanceof String, "Expected a string as input query");
      query = QueryTestUtil.normalizeQuery((String)query);
      return client.runQuery(type, (String)query);
    }
  }

  public static List<QueryDataBatch> testPreparedStatement(PreparedStatementHandle handle) throws Exception {
    return client.executePreparedStatement(handle);
  }

  public static int testRunAndPrint(QueryType type, String query) throws Exception {
    return QueryTestUtil.testRunAndLog(client, type, query);
  }

  protected static void testWithListener(QueryType type, String query, UserResultsListener resultListener) {
    QueryTestUtil.testWithListener(client, type, query, resultListener);
  }

  public static void testNoResult(String query, Object... args) throws Exception {
    testNoResult(1, query, args);
  }

  public static void alterSession(String option, Object value) {
    String valueStr = ClusterFixture.stringify(value);
    try {
      test("ALTER SESSION SET `%s` = %s", option, valueStr);
    } catch (Exception e) {
      fail(String.format("Failed to set session option `%s` = %s, Error: %s",
          option, valueStr, e.toString()));
    }
  }

  public static void resetSessionOption(String option) {
    try {
      test("ALTER SESSION RESET `%s`", option);
    } catch (Exception e) {
      fail(String.format("Failed to reset session option `%s`, Error: %s",
          option, e.toString()));
    }
  }

  public static void resetAllSessionOptions() {
    try {
      test("ALTER SESSION RESET ALL");
    } catch (Exception e) {
      fail("Failed to reset all session options");
    }
  }

  protected static void testNoResult(int interation, String query, Object... args) throws Exception {
    query = String.format(query, args);
    logger.debug("Running query:\n--------------\n" + query);
    for (int i = 0; i < interation; i++) {
      List<QueryDataBatch> results = client.runQuery(QueryType.SQL, query);
      for (QueryDataBatch queryDataBatch : results) {
        queryDataBatch.release();
      }
    }
  }

  public static void test(String query, Object... args) throws Exception {
    QueryTestUtil.testRunAndLog(client, String.format(query, args));
  }

  public static void test(String query) throws Exception {
    QueryTestUtil.testRunAndLog(client, query);
  }

  protected static int testPhysical(String query) throws Exception{
    return testRunAndPrint(QueryType.PHYSICAL, query);
  }

  protected static int testSql(String query) throws Exception{
    return testRunAndPrint(QueryType.SQL, query);
  }

  protected static void testPhysicalFromFile(String file) throws Exception{
    testPhysical(getFile(file));
  }

  /**
   * Utility method which tests given query produces a {@link UserException} and the exception message contains
   * the given message.
   * @param testSqlQuery Test query
   * @param expectedErrorMsg Expected error message.
   */
  protected static void errorMsgTestHelper(String testSqlQuery, String expectedErrorMsg) throws Exception {
    try {
      test(testSqlQuery);
      fail("Expected a UserException when running " + testSqlQuery);
    } catch (UserException actualException) {
      try {
        assertThat("message of UserException when running " + testSqlQuery, actualException.getMessage(), containsString(expectedErrorMsg));
      } catch (AssertionError e) {
        e.addSuppressed(actualException);
        throw e;
      }
    }
  }

  /**
   * Utility method which tests given query produces a {@link UserException}
   * with {@link org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType} being DrillPBError.ErrorType.PARSE
   * the given message.
   * @param testSqlQuery Test query
   */
  protected static void parseErrorHelper(String testSqlQuery) throws Exception {
    errorMsgTestHelper(testSqlQuery, UserBitShared.DrillPBError.ErrorType.PARSE.name());
  }

  public static String getFile(String resource) throws IOException {
    URL url = Resources.getResource(resource);
    if (url == null) {
      throw new IOException(String.format("Unable to find path %s.", resource));
    }
    return Resources.toString(url, Charsets.UTF_8);
  }

  /**
   * Copy the resource (ex. file on classpath) to a physical file on FileSystem.
   * @param resource
   * @return the file path
   * @throws IOException
   */
  public static String getPhysicalFileFromResource(String resource) throws IOException {
    File file = File.createTempFile("tempfile", ".txt");
    file.deleteOnExit();
    PrintWriter printWriter = new PrintWriter(file);
    printWriter.write(BaseTestQuery.getFile(resource));
    printWriter.close();

    return file.getPath();
  }

  protected static void setSessionOption(String option, boolean value) {
    alterSession(option, value);
  }

  protected static void setSessionOption(String option, long value) {
    alterSession(option, value);
  }

  protected static void setSessionOption(String option, double value) {
    alterSession(option, value);
  }

  protected static void setSessionOption(String option, String value) {
    alterSession(option, value);
  }

  public static class SilentListener implements UserResultsListener {
    private final AtomicInteger count = new AtomicInteger();
    private QueryId queryId;

    @Override
    public void submissionFailed(UserException ex) {
      logger.debug("Query failed: " + ex.getMessage());
    }

    @Override
    public void queryCompleted(QueryState state) {
      logger.debug("Query completed successfully with row count: " + count.get());
    }

    @Override
    public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
      int rows = result.getHeader().getRowCount();
      if (result.getData() != null) {
        count.addAndGet(rows);
      }
      result.release();
    }

    @Override
    public void queryIdArrived(QueryId queryId) {
      this.queryId = queryId;
    }

    public QueryId getQueryId() {
      return queryId;
    }

    public int getRowCount() {
      return count.get();
    }
  }

  protected void setColumnWidth(int columnWidth) {
    this.columnWidths = new int[] { columnWidth };
  }

  protected void setColumnWidths(int[] columnWidths) {
    this.columnWidths = columnWidths;
  }

  protected int logResult(List<QueryDataBatch> results) {
    int rowCount = 0;
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());
    for (QueryDataBatch result : results) {
      rowCount += result.getHeader().getRowCount();
      loader.load(result.getHeader().getDef(), result.getData());
      VectorUtil.logVectorAccessibleContent(loader, columnWidths);
      loader.clear();
      result.release();
    }
    return rowCount;
  }

  protected int printResult(List<QueryDataBatch> results) throws SchemaChangeException {
    int result = PrintingUtils.printAndThrow(() -> logResult(results));
    return result;
  }

  protected static String getResultString(List<QueryDataBatch> results, String delimiter)
      throws SchemaChangeException {
    StringBuilder formattedResults = new StringBuilder();
    boolean includeHeader = true;
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());
    for (QueryDataBatch result : results) {
      loader.load(result.getHeader().getDef(), result.getData());
      if (loader.getRecordCount() <= 0) {
        continue;
      }
      VectorUtil.appendVectorAccessibleContent(loader, formattedResults, delimiter, includeHeader);
      if (!includeHeader) {
        includeHeader = false;
      }
      loader.clear();
      result.release();
    }

    return formattedResults.toString();
  }

  public class TestResultSet {

    private final List<List<String>> rows;

    public TestResultSet() {
      rows = new ArrayList<>();
    }

    public TestResultSet(List<QueryDataBatch> batches) throws SchemaChangeException {
      rows = new ArrayList<>();
      convert(batches);
    }

    public void addRow(String... cells) {
      List<String> newRow = Arrays.asList(cells);
      rows.add(newRow);
    }

    public int size() {
      return rows.size();
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (! (o instanceof TestResultSet)) {
        return false;
      }
      TestResultSet that = (TestResultSet) o;
      assertEquals(this.size(), that.size());
      for (int i = 0; i < this.rows.size(); i++) {
        assertEquals(this.rows.get(i).size(), that.rows.get(i).size());
        for (int j = 0; j < this.rows.get(i).size(); ++j) {
          assertEquals(this.rows.get(i).get(j), that.rows.get(i).get(j));
        }
      }
      return true;
    }

    private void convert(List<QueryDataBatch> batches) throws SchemaChangeException {
      RecordBatchLoader loader = new RecordBatchLoader(getAllocator());
      for (QueryDataBatch batch : batches) {
        int rc = batch.getHeader().getRowCount();
        if (batch.getData() != null) {
          loader.load(batch.getHeader().getDef(), batch.getData());
          for (int i = 0; i < rc; ++i) {
            List<String> newRow = new ArrayList<>();
            rows.add(newRow);
            for (VectorWrapper<?> vw : loader) {
              ValueVector.Accessor accessor = vw.getValueVector().getAccessor();
              Object o = accessor.getObject(i);
              newRow.add(o == null ? null : o.toString());
            }
          }
        }
        loader.clear();
        batch.release();
      }
    }
  }
}
