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
package org.apache.drill.exec.server;

import com.codahale.metrics.MetricRegistry;
import io.netty.channel.EventLoopGroup;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.alias.AliasRegistryProvider;
import org.apache.drill.exec.compile.CodeCompiler;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.registry.RemoteFunctionRegistry;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.metrics.DrillCounters;
import org.apache.drill.exec.oauth.OAuthTokenProvider;
import org.apache.drill.exec.physical.impl.OperatorCreatorRegistry;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.sql.DrillOperatorTable;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.control.Controller;
import org.apache.drill.exec.rpc.control.WorkEventBus;
import org.apache.drill.exec.rpc.data.DataConnectionCreator;
import org.apache.drill.exec.rpc.security.AuthenticatorProvider;
import org.apache.drill.exec.rpc.user.UserServer;
import org.apache.drill.exec.rpc.user.UserServer.BitToUserConnection;
import org.apache.drill.exec.rpc.user.UserServer.BitToUserConnectionConfig;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.store.SchemaFactory;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.work.foreman.rm.ResourceManager;
import org.apache.drill.exec.work.foreman.rm.ResourceManagerBuilder;
import org.apache.drill.metastore.MetastoreRegistry;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

public class DrillbitContext implements AutoCloseable {

  private final BootStrapContext context;
  private final PhysicalPlanReader reader;
  private final ClusterCoordinator coord;
  private final DataConnectionCreator connectionsPool;
  private final DrillbitEndpoint endpoint;
  private final StoragePluginRegistry storagePlugins;
  private final AliasRegistryProvider aliasRegistryProvider;
  private final OAuthTokenProvider oAuthTokenProvider;
  private final OperatorCreatorRegistry operatorCreatorRegistry;
  private final Controller controller;
  private final WorkEventBus workBus;
  private final FunctionImplementationRegistry functionRegistry;
  private final SystemOptionManager systemOptions;
  private final PersistentStoreProvider provider;
  private final CodeCompiler compiler;
  private final ScanResult classpathScan;
  private final LogicalPlanPersistence lpPersistence;
  // operator table for standard SQL operators and functions, Drill built-in UDFs
  private final DrillOperatorTable table;
  private final QueryProfileStoreContext profileStoreContext;
  private ResourceManager resourceManager;
  private final MetastoreRegistry metastoreRegistry;
  private final DrillCounters counters;

  public DrillbitContext(
      DrillbitEndpoint endpoint,
      BootStrapContext context,
      ClusterCoordinator coord,
      Controller controller,
      DataConnectionCreator connectionsPool,
      WorkEventBus workBus,
      PersistentStoreProvider provider) {
    //PersistentStoreProvider is re-used for providing Query Profile Store as well
    this(endpoint, context, coord, controller, connectionsPool, workBus, provider, provider);
  }

  public DrillbitContext(
      DrillbitEndpoint endpoint,
      BootStrapContext context,
      ClusterCoordinator coord,
      Controller controller,
      DataConnectionCreator connectionsPool,
      WorkEventBus workBus,
      PersistentStoreProvider provider,
      PersistentStoreProvider profileStoreProvider) {
    classpathScan = context.getClasspathScan();
    this.workBus = workBus;
    this.controller = checkNotNull(controller);
    this.context = checkNotNull(context);
    this.coord = coord;
    this.connectionsPool = checkNotNull(connectionsPool);
    this.endpoint = checkNotNull(endpoint);
    this.provider = provider;
    DrillConfig config = context.getConfig();
    lpPersistence = new LogicalPlanPersistence(config, classpathScan);

    storagePlugins = config.getInstance(
        ExecConstants.STORAGE_PLUGIN_REGISTRY_IMPL, StoragePluginRegistry.class, this);

    reader = new PhysicalPlanReader(config, classpathScan, lpPersistence, endpoint, storagePlugins);
    operatorCreatorRegistry = new OperatorCreatorRegistry(classpathScan);
    systemOptions = new SystemOptionManager(lpPersistence, provider, config, context.getDefinitions());
    functionRegistry = new FunctionImplementationRegistry(config, classpathScan, systemOptions);
    compiler = new CodeCompiler(config, systemOptions);

    // This operator table is built once and used for all queries which do not need dynamic UDF support.
    table = new DrillOperatorTable(functionRegistry, systemOptions);

    //This profile store context is built from the profileStoreProvider
    profileStoreContext = new QueryProfileStoreContext(config, profileStoreProvider, coord);
    this.metastoreRegistry = new MetastoreRegistry(config);
    this.aliasRegistryProvider = new AliasRegistryProvider(this);
    this.oAuthTokenProvider = new OAuthTokenProvider(this);

    this.counters = DrillCounters.getInstance();
  }

  public QueryProfileStoreContext getProfileStoreContext() {
    return profileStoreContext;
  }

  /**
   * Starts the resource manager. Must be called separately from the
   * constructor after the system property mechanism is initialized
   * since the builder will consult system options to determine the
   * proper RM to use.
   */

  public void startRM() {
    resourceManager = new ResourceManagerBuilder(this).build();
  }

  public FunctionImplementationRegistry getFunctionImplementationRegistry() {
    return functionRegistry;
  }

  public WorkEventBus getWorkBus() {
    return workBus;
  }

  /**
   * @return the system options manager. It is important to note that this manager only contains options at the
   * "system" level and not "session" level.
   */
  public SystemOptionManager getOptionManager() {
    return systemOptions;
  }

  public DrillbitEndpoint getEndpoint() {
    return endpoint;
  }

  public DrillConfig getConfig() {
    return context.getConfig();
  }

  public Collection<DrillbitEndpoint> getAvailableBits() {
    return coord.getAvailableEndpoints();
  }

  public Collection<DrillbitEndpoint> getBits() {
    return coord.getOnlineEndPoints();
  }

  public boolean isOnline(DrillbitEndpoint endpoint) {
    return endpoint.getState().equals(DrillbitEndpoint.State.ONLINE);
  }

  public boolean isForeman(DrillbitEndpoint endpoint) {
    DrillbitEndpoint foreman = getEndpoint();
    if (endpoint.getAddress().equals(foreman.getAddress()) &&
            endpoint.getUserPort() == foreman.getUserPort()) {
      return true;
    }
    return false;
  }

  public boolean isForemanOnline() {
    Collection<DrillbitEndpoint> dbs = getAvailableBits();
    for (DrillbitEndpoint db : dbs) {
      if( isForeman(db)) {
        if (isOnline(db)) {
          return true;
        }
      }
    }
    return false;
  }

  public BufferAllocator getAllocator() {
    return context.getAllocator();
  }

  public OperatorCreatorRegistry getOperatorCreatorRegistry() {
    return operatorCreatorRegistry;
  }

  public StoragePluginRegistry getStorage() {
    return this.storagePlugins;
  }

  public AliasRegistryProvider getAliasRegistryProvider() {
    return aliasRegistryProvider;
  }

  public OAuthTokenProvider getoAuthTokenProvider() { return oAuthTokenProvider; }

  public EventLoopGroup getBitLoopGroup() {
    return context.getBitLoopGroup();
  }

  public DataConnectionCreator getDataConnectionsPool() {
    return connectionsPool;
  }

  public Controller getController() {
    return controller;
  }

  public MetricRegistry getMetrics() {
    return context.getMetrics();
  }

  public PhysicalPlanReader getPlanReader() {
    return reader;
  }

  public PersistentStoreProvider getStoreProvider() {
    return provider;
  }

  public SchemaFactory getSchemaFactory() {
    return storagePlugins.getSchemaFactory();
  }

  public ClusterCoordinator getClusterCoordinator() {
    return coord;
  }

  public CodeCompiler getCompiler() {
    return compiler;
  }

  public ExecutorService getExecutor() {
    return context.getExecutor();
  }
  public ExecutorService getScanExecutor() {
    return context.getScanExecutor();
  }
  public ExecutorService getScanDecodeExecutor() {
    return context.getScanDecodeExecutor();
  }

  public LogicalPlanPersistence getLpPersistence() {
    return lpPersistence;
  }

  public ScanResult getClasspathScan() {
    return classpathScan;
  }

  public RemoteFunctionRegistry getRemoteFunctionRegistry() {
    return functionRegistry.getRemoteFunctionRegistry();
  }

  /**
   * Use the operator table built during startup when "exec.udf.use_dynamic" option
   * is set to false.
   * This operator table has standard SQL functions, operators and drill
   * built-in user defined functions (UDFs).
   * It does not include dynamic user defined functions (UDFs) that get added/removed
   * at run time.
   * This operator table is meant to be used for high throughput,
   * low latency operational queries, for which cost of building operator table is
   * high, both in terms of CPU and heap memory usage.
   *
   * @return - Operator table
   */
  public DrillOperatorTable getOperatorTable() {
    return table;
  }

  public AuthenticatorProvider getAuthProvider() {
    return context.getAuthProvider();
  }

  public Set<Entry<BitToUserConnection, BitToUserConnectionConfig>> getUserConnections() {
    return UserServer.getUserConnections();
  }

  @Override
  public void close() throws Exception {
    getOptionManager().close();
    getFunctionImplementationRegistry().close();
    getRemoteFunctionRegistry().close();
    getCompiler().close();
    getMetastoreRegistry().close();
    getAliasRegistryProvider().close();
    getoAuthTokenProvider().close();
  }

  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  public MetastoreRegistry getMetastoreRegistry() {
    return metastoreRegistry;
  }

  public DrillCounters getCounters() {
    return counters;
  }
}
