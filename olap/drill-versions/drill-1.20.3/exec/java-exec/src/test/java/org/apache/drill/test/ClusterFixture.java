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

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;
import static org.apache.drill.exec.util.StoragePluginTestUtils.ROOT_SCHEMA;
import static org.apache.drill.exec.util.StoragePluginTestUtils.TMP_SCHEMA;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ZookeeperHelper;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.store.SchemaFactory;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.exec.store.StoragePluginRegistryImpl;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.exec.store.mock.MockStorageEngineConfig;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.exec.store.sys.store.provider.ZookeeperPersistentStoreProvider;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.drill.test.DrillTestWrapper.TestServices;
import org.apache.hadoop.fs.FileSystem;

/**
 * Test fixture to start a Drillbit with provide options, create a client, and
 * execute queries. Can be used in JUnit tests, or in ad-hoc programs. Provides
 * a builder to set the necessary embedded Drillbit and client options, then
 * creates the requested Drillbit and client.
 * TODO: To support User Impersonation add Configuration dfsConf and set hadoop.proxyuser settings
 * similar to {@link org.apache.drill.exec.impersonation.BaseTestImpersonation}
 */
public class ClusterFixture extends BaseFixture implements AutoCloseable {
  public static final int MAX_WIDTH_PER_NODE = 2;

  @SuppressWarnings("serial")
  public static final Properties TEST_CONFIGURATIONS = new Properties() {
    {
      // Properties here mimic those in drill-root/pom.xml, Surefire plugin
      // configuration. They allow tests to run successfully in Eclipse.
      put(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, false);

      // The CTTAS function requires that the default temporary workspace be
      // writable. By default, the default temporary workspace points to
      // dfs.tmp. But, the test setup marks dfs.tmp as read-only. To work
      // around this, tests are supposed to use dfs. So, we need to
      // set the default temporary workspace to dfs.tmp.
      put(ExecConstants.DEFAULT_TEMPORARY_WORKSPACE, DFS_TMP_SCHEMA);
      put(ExecConstants.HTTP_ENABLE, false);
      put("drill.catastrophic_to_standard_out", true);

      // Verbose errors.
      put(ExecConstants.ENABLE_VERBOSE_ERRORS_KEY, true);

      // See Drillbit.close. The Drillbit normally waits a specified amount
      // of time for ZK registration to drop. But, embedded Drillbits normally
      // don't use ZK, so no need to wait.
      put(ExecConstants.ZK_REFRESH, 0);

      // This is just a test, no need to be heavy-duty on threads.
      // This is the number of server and client RPC threads. The
      // production default is DEFAULT_SERVER_RPC_THREADS.
      put(ExecConstants.BIT_SERVER_RPC_THREADS, 2);

      // No need for many scanners except when explicitly testing that
      // behavior. Production default is DEFAULT_SCAN_THREADS
      put(ExecConstants.SCAN_THREADPOOL_SIZE, 4);

      // Define a useful root location for the ZK persistent
      // storage. Profiles will go here when running in distributed
      // mode.
      put(ZookeeperPersistentStoreProvider.DRILL_EXEC_SYS_STORE_PROVIDER_ZK_BLOBROOT,
          "/tmp/drill/tests");
    }
  };

  public static final String DEFAULT_BIT_NAME = "drillbit";

  private final Map<String, Drillbit> bits = new HashMap<>();
  private Drillbit defaultDrillbit;
  private boolean ownsZK;
  private ZookeeperHelper zkHelper;
  private RemoteServiceSet serviceSet;
  protected List<ClientFixture> clients = new ArrayList<>();
  protected RestClientFixture restClientFixture;
  private boolean usesZk;
  private Properties clientProps;
  private final ClusterFixtureBuilder builder;

  ClusterFixture(ClusterFixtureBuilder builder) {
    this.builder = Preconditions.checkNotNull(builder);

    setClientProps();
    configureZk();
    try {
      createConfig();
      allocator = RootAllocatorFactory.newRoot(config);
      startDrillbits();
      applyOptions();
    } catch (Exception e) {

      // Translate exceptions to unchecked to avoid cluttering
      // tests. Failures will simply fail the test itself.
      throw new IllegalStateException("Cluster fixture setup failed", e);
    }
  }

  /**
   * Set the client properties to be used by client fixture.
   */
  private void setClientProps() {
    clientProps = builder.clientProps;
  }

  public Properties getClientProps() {
    return clientProps;
  }

  private void configureZk() {

    // Start ZK if requested.
    String zkConnect;
    if (builder.zkHelper != null) {

      // Case where the test itself started ZK and we're only using it.
      zkHelper = builder.zkHelper;
      ownsZK = false;
    } else if (builder.localZkCount > 0) {

      // Case where we need a local ZK just for this test cluster.
      zkHelper = new ZookeeperHelper();
      zkHelper.startZookeeper(builder.localZkCount);
      ownsZK = true;
    }
    if (zkHelper != null) {
      zkConnect = zkHelper.getConnectionString();

      // When using ZK, we need to pass in the connection property as
      // a config property. But, we can only do that if we are passing
      // in config properties defined at run time. Drill does not allow
      // combining locally-set properties and a config file: it is one
      // or the other.
      if (builder.configBuilder().hasResource()) {
        throw new IllegalArgumentException("Cannot specify a local ZK while using an external config file.");
      }
      builder.configProperty(ExecConstants.ZK_CONNECTION, zkConnect);

      // Forced to disable this, because currently we leak memory which is a known issue for query cancellations.
      // Setting this causes unit tests to fail.
      builder.configProperty(ExecConstants.RETURN_ERROR_FOR_FAILURE_IN_CANCELLED_FRAGMENTS, true);
    }
  }

  private void createConfig() throws Exception {

    // Create a config
    // Because of the way DrillConfig works, we can set the ZK
    // connection string only if a property set is provided.
    config = builder.configBuilder.build();

    if (builder.usingZk) {

      // Distribute drillbit using ZK (in-process or external)
      serviceSet = null;
      usesZk = true;
    } else {

      // Embedded Drillbit.
      serviceSet = RemoteServiceSet.getLocalServiceSet();
    }
  }

  private void startDrillbits() throws Exception {

    // Start the Drillbits.
    Preconditions.checkArgument(builder.bitCount > 0);
    int bitCount = builder.bitCount;
    for (int i = 0; i < bitCount; i++) {
      Drillbit bit = new Drillbit(config, builder.configBuilder.getDefinitions(), serviceSet);
      bit.run();

      // Bit name and registration.
      String name;
      if (builder.bitNames != null && i < builder.bitNames.length) {
        name = builder.bitNames[i];
      } else {

        // Name the Drillbit by default. Most tests use one Drillbit,
        // so make the name simple: "drillbit." Only add a numeric suffix
        // when the test creates multiple bits.
        if (bitCount == 1) {
          name = DEFAULT_BIT_NAME;
        } else {
          name = DEFAULT_BIT_NAME + Integer.toString(i + 1);
        }
      }
      bits.put(name, bit);

      // Remember the first Drillbit, this is the default one returned from
      // drillbit().
      if (i == 0) {
        defaultDrillbit = bit;
      }
      configureStoragePlugins(bit);
    }
  }

  private void configureStoragePlugins(Drillbit bit) throws Exception {

    // Skip plugins if not running in test mode.
    if (builder.dirTestWatcher == null) {
      return;
    }
    // Create the dfs name space
    builder.dirTestWatcher.newDfsTestTmpDir();

    final StoragePluginRegistry pluginRegistry = bit.getContext().getStorage();
    StoragePluginTestUtils.configureFormatPlugins(pluginRegistry);

    StoragePluginTestUtils.updateSchemaLocation(
        StoragePluginTestUtils.DFS_PLUGIN_NAME, pluginRegistry,
        builder.dirTestWatcher.getDfsTestTmpDir(), TMP_SCHEMA);
    StoragePluginTestUtils.updateSchemaLocation(
        StoragePluginTestUtils.DFS_PLUGIN_NAME,
        pluginRegistry, builder.dirTestWatcher.getRootDir(), ROOT_SCHEMA);
    StoragePluginTestUtils.updateSchemaLocation(
        StoragePluginTestUtils.DFS_PLUGIN_NAME, pluginRegistry,
        builder.dirTestWatcher.getRootDir(), SchemaFactory.DEFAULT_WS_NAME);

    // Create the mock data plugin
    MockStorageEngineConfig config = MockStorageEngineConfig.INSTANCE;
    config.setEnabled(true);
    pluginRegistry.put(MockStorageEngineConfig.NAME, config);
  }

  private void applyOptions() throws Exception {

    // Apply system options
    if (builder.systemOptions != null) {
      for (ClusterFixtureBuilder.RuntimeOption option : builder.systemOptions) {
        clientFixture().alterSystem(option.key, option.value);
      }
    }

    // Apply session options.
    if (builder.sessionOptions != null) {
      for (ClusterFixtureBuilder.RuntimeOption option : builder.sessionOptions) {
        clientFixture().alterSession(option.key, option.value);
      }
    }
  }

  public Drillbit drillbit() { return defaultDrillbit; }
  public Drillbit drillbit(String name) { return bits.get(name); }
  public Collection<Drillbit> drillbits() { return bits.values(); }
  public RemoteServiceSet serviceSet() { return serviceSet; }

  public ClientFixture.ClientBuilder clientBuilder() {
    return new ClientFixture.ClientBuilder(this);
  }

  public ClientFixture.ClientBuilder clientBuilder(Properties properties) {
    return new ClientFixture.ClientBuilder(this, properties);
  }

  public RestClientFixture.Builder restClientBuilder() {
    return new RestClientFixture.Builder(this);
  }

  public ClientFixture clientFixture() {
    if (clients.isEmpty()) {
      clientBuilder()
        .property(DrillProperties.DRILLBIT_CONNECTION, String.format("localhost:%s", drillbit().getUserPort()))
        .build();
    }
    return clients.get(0);
  }

  /**
   * It can be used to add one more client for {@link ClusterFixture}. <br>
   * Note: {@link ClusterTest#client}
   *
   * @param properties client Properties (clientProps)
   * @return new ClientFixture for current ClusterFixture
   */
  public ClientFixture addClientFixture(Properties properties) {
    return clientBuilder(properties)
      .property(DrillProperties.DRILLBIT_CONNECTION, String.format("localhost:%s", drillbit().getUserPort()))
      .build();
  }

  /**
   * Create a test client for a specific host and port.
   *
   * @param host host, must be one of those created by this
   * fixture
   * @param port post, must be one of those created by this
   * fixture
   * @return a test client. Client will be closed when this cluster
   * fixture closes, or can be closed early
   */
  public ClientFixture client(String host, int port) {
    return clientBuilder()
      .property(DrillProperties.DRILLBIT_CONNECTION, String.format("%s:%d", host, port))
      .build();
  }

  public RestClientFixture restClientFixture() {
    if (restClientFixture == null) {
      restClientFixture = restClientBuilder().build();
    }

    return restClientFixture;
  }

  public DrillClient client() {
    return clientFixture().client();
  }

  public ClientFixture client(int number) {
    return clients.get(number);
  }

  /**
   * Return a JDBC connection to the default (first) Drillbit.
   * Note that this code requires special setup of the test code.
   * Tests in the "exec" package do not normally have visibility
   * to the Drill JDBC driver. So, the test must put that code
   * on the class path manually in order for this code to load the
   * JDBC classes. The caller is responsible for closing the JDBC
   * connection before closing the cluster. (An enhancement is to
   * do the close automatically as is done for clients.)
   *
   * @return a JDBC connection to the default Drillbit
   */
  public Connection jdbcConnection() {
    try {
      Class.forName("org.apache.drill.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
    String connStr = "jdbc:drill:";
    if (usesZK()) {
      connStr += "zk=" + zkHelper.getConnectionString();
    } else {
      DrillbitEndpoint ep = drillbit().getContext().getEndpoint();
      connStr += "drillbit=" + ep.getAddress() + ":" + ep.getUserPort();
    }
    try {
      return DriverManager.getConnection(connStr);
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Close the clients, Drillbits, allocator and
   * Zookeeper. Checks for exceptions. If an exception occurs,
   * continues closing, suppresses subsequent exceptions, and
   * throws the first exception at completion of close. This allows
   * the test code to detect any state corruption which only shows
   * itself when shutting down resources (memory leaks, for example.)
   */
  @Override
  public void close() throws Exception {
    Exception ex = null;

    // Close clients. Clients remove themselves from the client
    // list.
    while (!clients.isEmpty()) {
      ex = safeClose(clients.get(0), ex);
    }

    for (Drillbit bit : drillbits()) {
      ex = safeClose(bit, ex);
    }
    bits.clear();
    ex = safeClose(serviceSet, ex);
    serviceSet = null;
    ex = safeClose(allocator, ex);
    allocator = null;
    if (zkHelper != null && ownsZK) {
      try {
        zkHelper.stopZookeeper();
      } catch (Exception e) {
        ex = ex == null ? e : ex;
      }
    }
    zkHelper = null;

    if (ex != null) {
      throw ex;
    }
  }

  /**
   * Shutdown the drillbit given the name of the drillbit.
   */
  public void closeDrillbit(final String drillbitName) throws Exception {
    Exception ex = null;
    for (Drillbit bit : drillbits()) {
      if (bit.equals(bits.get(drillbitName))) {
        try {
          bit.close();
        } catch (Exception e) {
          ex = ex == null ? e :ex;
        }
      }
    }
    if (ex != null) {
      throw ex;
    }
  }

  /**
   * Close a resource, suppressing the exception, and keeping
   * only the first exception that may occur. We assume that only
   * the first is useful, any others are probably down-stream effects
   * of that first one.
   *
   * @param item Item to be closed
   * @param ex exception to be returned if none thrown here
   * @return the first exception found
   */
  private Exception safeClose(AutoCloseable item, Exception ex) {
    try {
      if (item != null) {
        item.close();
      }
    } catch (Exception e) {
      ex = ex == null ? e : ex;
    }
    return ex;
  }

  public void defineStoragePlugin(String name, StoragePluginConfig config) {
    try {
      for (Drillbit drillbit : drillbits()) {
        StoragePluginRegistryImpl registry = (StoragePluginRegistryImpl) drillbit.getContext().getStorage();
        registry.put(name, config);
      }
    } catch (PluginException e) {
      throw new IllegalStateException("Plugin definition failed", e);
    }
  }

  /**
   * Define a workspace within an existing storage plugin. Useful for
   * pointing to local file system files outside the Drill source tree.
   *
   * @param pluginName name of the plugin like "dfs".
   * @param schemaName name of the new schema
   * @param path directory location (usually local)
   * @param defaultFormat default format for files in the schema
   */
  public void defineWorkspace(String pluginName, String schemaName, String path,
      String defaultFormat) {
    defineWorkspace(pluginName, schemaName, path, defaultFormat, null);
  }

  public void defineWorkspace(String pluginName, String schemaName, String path,
      String defaultFormat, FormatPluginConfig format) {
    defineWorkspace(pluginName, schemaName, path, defaultFormat, format, true);
  }

  public void defineImmutableWorkspace(String pluginName, String schemaName, String path,
      String defaultFormat, FormatPluginConfig format) {
    defineWorkspace(pluginName, schemaName, path, defaultFormat, format, false);
  }

  private void defineWorkspace(String pluginName, String schemaName, String path,
      String defaultFormat, FormatPluginConfig format, boolean writable) {
    for (Drillbit bit : drillbits()) {
      try {
        defineWorkspace(bit, pluginName, schemaName, path, defaultFormat, format, writable);
      } catch (PluginException e) {
        // This functionality is supposed to work in tests. Change
        // exception to unchecked to make test code simpler.

        throw new IllegalStateException(String.format(
            "Failed to define a workspace for plugin %s, schema %s, path %s, default format %s",
            pluginName, schemaName, path, defaultFormat), e);
      }
    }
  }

  private void defineWorkspace(Drillbit drillbit, String pluginName,
      String schemaName, String path, String defaultFormat, FormatPluginConfig format, boolean writable)
      throws PluginException {
    final StoragePluginRegistry pluginRegistry = drillbit.getContext().getStorage();
    final FileSystemConfig pluginConfig = (FileSystemConfig) pluginRegistry.getStoredConfig(pluginName);
    final WorkspaceConfig newTmpWSConfig = new WorkspaceConfig(path, writable, defaultFormat, false);

    Map<String, WorkspaceConfig> newWorkspaces = new HashMap<>();
    Optional.ofNullable(pluginConfig.getWorkspaces())
      .ifPresent(newWorkspaces::putAll);
    newWorkspaces.put(schemaName, newTmpWSConfig);

    Map<String, FormatPluginConfig> newFormats = new HashMap<>();
    Optional.ofNullable(pluginConfig.getFormats())
      .ifPresent(newFormats::putAll);
    Optional.ofNullable(format)
      .ifPresent(f -> newFormats.put(defaultFormat, f));

    updatePlugin(pluginRegistry, pluginName, pluginConfig, newWorkspaces, newFormats);
  }

  public void defineFormat(String pluginName, String name, FormatPluginConfig config) {
    defineFormats(pluginName, ImmutableMap.of(name, config));
  }

  public void defineFormats(String pluginName, Map<String, FormatPluginConfig> formats) {
    for (Drillbit bit : drillbits()) {
      try {
        defineFormats(bit, pluginName, formats);
      } catch (PluginException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void defineFormats(Drillbit drillbit,
                             String pluginName,
                             Map<String, FormatPluginConfig> formats) throws PluginException {
    StoragePluginRegistry pluginRegistry = drillbit.getContext().getStorage();
    FileSystemConfig pluginConfig = (FileSystemConfig) pluginRegistry.copyConfig(pluginName);
    pluginConfig.getFormats().putAll(formats);
    pluginRegistry.put(pluginName, pluginConfig);
  }

  private void updatePlugin(StoragePluginRegistry pluginRegistry,
                            String pluginName,
                            FileSystemConfig pluginConfig,
                            Map<String, WorkspaceConfig> newWorkspaces,
                            Map<String, FormatPluginConfig> newFormats) throws PluginException {
    FileSystemConfig newPluginConfig = new FileSystemConfig(
      pluginConfig.getConnection(),
      pluginConfig.getConfig(),
      newWorkspaces == null ? pluginConfig.getWorkspaces() : newWorkspaces,
      newFormats == null ? pluginConfig.getFormats() : newFormats,
      PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    newPluginConfig.setEnabled(pluginConfig.isEnabled());

    pluginRegistry.put(pluginName, newPluginConfig);
  }

  public static final String EXPLAIN_PLAN_TEXT = "text";
  public static final String EXPLAIN_PLAN_JSON = "json";

  public static ClusterFixtureBuilder builder(BaseDirTestWatcher dirTestWatcher) {
    ClusterFixtureBuilder builder = new ClusterFixtureBuilder(dirTestWatcher)
         .sessionOption(ExecConstants.MAX_WIDTH_PER_NODE_KEY, MAX_WIDTH_PER_NODE);
    Properties props = new Properties();
    props.putAll(ClusterFixture.TEST_CONFIGURATIONS);
    props.setProperty(ExecConstants.DRILL_TMP_DIR, dirTestWatcher.getTmpDir().getAbsolutePath());
    props.setProperty(ExecConstants.UDF_DIRECTORY_ROOT, dirTestWatcher.getHomeDir().getAbsolutePath());
    props.setProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_PATH, dirTestWatcher.getStoreDir().getAbsolutePath());
    props.setProperty(ExecConstants.UDF_DIRECTORY_FS, FileSystem.DEFAULT_FS);
    // ALTER SESSION profiles are seldom interesting
    props.setProperty(ExecConstants.SKIP_ALTER_SESSION_QUERY_PROFILE, Boolean.TRUE.toString());

    builder.configBuilder.configProps(props);
    return builder;
  }

  /**
   * Return a cluster builder without any of the usual defaults. Use
   * this only for special cases. Your code is responsible for all the
   * odd bits that must be set to get the setup right. See
   * {@link ClusterFixture#TEST_CONFIGURATIONS} for details. Note that
   * you are often better off using the defaults, then replacing selected
   * properties with the values you prefer.
   *
   * @return a fixture builder with no default properties set
   */
  public static ClusterFixtureBuilder bareBuilder(BaseDirTestWatcher dirTestWatcher) {
    return new ClusterFixtureBuilder(dirTestWatcher);
  }

  /**
   * Shim class to allow the {@link TestBuilder} class to work with the
   * cluster fixture.
   */
  public static class FixtureTestServices implements TestServices {

    private final ClientFixture client;

    public FixtureTestServices(ClientFixture client) {
      this.client = client;
    }

    @Override
    public BufferAllocator allocator() {
      return client.allocator();
    }

    @Override
    public void test(String query) throws Exception {
      client.runQueriesAndLog(query);
    }

    @Override
    public List<QueryDataBatch> testRunAndReturn(QueryType type, Object query)
        throws Exception {
      return client.queryBuilder().query(type, (String) query).results();
    }
  }

  /**
   * Return a cluster fixture built with standard options. This is a short-cut
   * for simple tests that don't need special setup.
   *
   * @param dirTestWatcher directory test watcher
   * @return a cluster fixture with standard options
   */
  public static ClusterFixture standardCluster(BaseDirTestWatcher dirTestWatcher) {
    return builder(dirTestWatcher).build();
  }

  /**
   * Convert a Java object (typically a boxed scalar) to a string
   * for use in SQL. Quotes strings but just converts others to
   * string format. If value to encode is null, return null.
   *
   * @param value the value to encode
   * @return the SQL-acceptable string equivalent
   */
  public static String stringify(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof String) {
      return "'" + value + "'";
    }

    return value.toString();
  }

  public static String getResource(String resource) throws IOException {

    // Unlike the Java routines, Guava does not like a leading slash.
    final URL url = Resources.getResource(trimSlash(resource));
    if (url == null) {
      throw new IOException(
          String.format("Unable to find resource %s.", resource));
    }
    return Resources.toString(url, Charsets.UTF_8);
  }

  /**
   * Load a resource file, returning the resource as a string.
   * "Hides" the checked exception as unchecked, which is fine
   * in a test as the unchecked exception will fail the test
   * without unnecessary error fiddling.
   *
   * @param resource path to the resource
   * @return the resource contents as a string
   */
  public static String loadResource(String resource) {
    try {
      return getResource(resource);
    } catch (IOException e) {
      throw new IllegalStateException("Resource not found: " + resource, e);
    }
  }

  /**
   * Guava likes paths to resources without an initial slash, the JDK
   * needs a slash. Normalize the path when needed.
   *
   * @param path resource path with optional leading slash
   * @return same path without the leading slash
   */
  public static String trimSlash(String path) {
    if (path == null) {
      return path;
    } else if (path.startsWith("/")) {
      return path.substring(1);
    } else {
      return path;
    }
  }

  /**
   * Create a temporary data directory which will be removed when the
   * cluster closes, and register it as a "dfs" name space.
   *
   * @param key The name to use for the directory and the name space.
   * Access the directory as "dfs.<key>".
   * @param defaultFormat The default storage format for the workspace.
   * @param formatPluginConfig The format plugin config.
   * @return location of the directory which can be used to create
   * temporary input files
   */
  public File makeDataDir(String key, String defaultFormat, FormatPluginConfig formatPluginConfig) {
    File dir = builder.dirTestWatcher.makeSubDir(Paths.get(key));
    defineWorkspace("dfs", key, dir.getAbsolutePath(), defaultFormat, formatPluginConfig);
    return dir;
  }

  public File getDrillTempDir() {
    return new File(URI.create(config.getString(ExecConstants.SYS_STORE_PROVIDER_LOCAL_PATH)).getPath());
  }

  public boolean usesZK() {
    return usesZk;
  }

  /**
   * Returns the directory that holds query profiles. Valid only for an
   * embedded Drillbit with local cluster coordinator &ndash; the normal
   * case for unit tests.
   *
   * @return query profile directory
   */
  public File getProfileDir() {
    File baseDir;
    if (usesZk) {
      baseDir = new File(config.getString(ZookeeperPersistentStoreProvider.DRILL_EXEC_SYS_STORE_PROVIDER_ZK_BLOBROOT));
    } else {
      baseDir = getDrillTempDir();
    }
    return new File(baseDir, "profiles");
  }

  public StoragePluginRegistry storageRegistry() {
    return drillbit().getContext().getStorage();
  }

  public StoragePluginRegistry storageRegistry(String name) {
    return drillbit(name).getContext().getStorage();
  }
}
