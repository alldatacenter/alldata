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
package org.apache.drill.exec.client;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkArgument;
import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;
import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkState;
import static org.apache.drill.exec.proto.UserProtos.QueryResultsMode.STREAM_FULL;
import static org.apache.drill.exec.proto.UserProtos.RunQuery.newBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.DrillAutoCloseables;
import org.apache.drill.common.Version;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.coord.zk.ZKClusterCoordinator;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.GeneralRPCProtos;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.UserProtos;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementReq;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementResp;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsReq;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsResp;
import org.apache.drill.exec.proto.UserProtos.GetColumnsReq;
import org.apache.drill.exec.proto.UserProtos.GetColumnsResp;
import org.apache.drill.exec.proto.UserProtos.GetQueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.GetSchemasReq;
import org.apache.drill.exec.proto.UserProtos.GetSchemasResp;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaReq;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaResp;
import org.apache.drill.exec.proto.UserProtos.GetTablesReq;
import org.apache.drill.exec.proto.UserProtos.GetTablesResp;
import org.apache.drill.exec.proto.UserProtos.LikeFilter;
import org.apache.drill.exec.proto.UserProtos.PreparedStatementHandle;
import org.apache.drill.exec.proto.UserProtos.QueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.RpcEndpointInfos;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.ChannelClosedException;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.DrillRpcFuture;
import org.apache.drill.exec.rpc.NamedThreadFactory;
import org.apache.drill.exec.rpc.NonTransientRpcException;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.TransportCheck;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserClient;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.exec.rpc.user.UserRpcUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;

/**
 * Thin wrapper around a UserClient that handles connect/close and transforms
 * String into ByteBuf.
 */
public class DrillClient implements Closeable, ConnectionThrottle {
  private static Logger logger = LoggerFactory.getLogger(DrillClient.class);
  public static final String DEFAULT_CLIENT_NAME = "Apache Drill Java client";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final DrillConfig config;
  private UserClient client;
  private DrillProperties properties;
  private volatile ClusterCoordinator clusterCoordinator;
  private volatile boolean connected;
  private final BufferAllocator allocator;
  private final int reconnectTimes;
  private final int reconnectDelay;
  private boolean supportComplexTypes;
  private final boolean ownsZkConnection;
  private final boolean ownsAllocator;
  private final boolean isDirectConnection; // true if the connection bypasses zookeeper and connects directly to a drillbit
  private EventLoopGroup eventLoopGroup;
  private ExecutorService executor;
  private String clientName = DEFAULT_CLIENT_NAME;

  public DrillClient() throws OutOfMemoryException {
    this(DrillConfig.create(), false);
  }

  public DrillClient(boolean isDirect) throws OutOfMemoryException {
    this(DrillConfig.create(), isDirect);
  }

  public DrillClient(String fileName) throws OutOfMemoryException {
    this(DrillConfig.create(fileName), false);
  }

  public DrillClient(DrillConfig config) throws OutOfMemoryException {
    this(config, null, false);
  }

  public DrillClient(DrillConfig config, boolean isDirect)
      throws OutOfMemoryException {
    this(config, null, isDirect);
  }

  public DrillClient(DrillConfig config, ClusterCoordinator coordinator)
    throws OutOfMemoryException {
    this(config, coordinator, null, false);
  }

  public DrillClient(DrillConfig config, ClusterCoordinator coordinator, boolean isDirect)
    throws OutOfMemoryException {
    this(config, coordinator, null, isDirect);
  }

  public DrillClient(DrillConfig config, ClusterCoordinator coordinator, BufferAllocator allocator)
      throws OutOfMemoryException {
    this(config, coordinator, allocator, false);
  }

  public DrillClient(DrillConfig config, ClusterCoordinator coordinator, BufferAllocator allocator, boolean isDirect) {
    // if isDirect is true, the client will connect directly to the drillbit instead of
    // going thru the zookeeper
    this.isDirectConnection = isDirect;
    this.ownsZkConnection = coordinator == null && !isDirect;
    this.ownsAllocator = allocator == null;
    this.allocator = ownsAllocator ? RootAllocatorFactory.newRoot(config) : allocator;
    this.config = config;
    this.clusterCoordinator = coordinator;
    this.reconnectTimes = config.getInt(ExecConstants.BIT_RETRY_TIMES);
    this.reconnectDelay = config.getInt(ExecConstants.BIT_RETRY_DELAY);
    this.supportComplexTypes = config.getBoolean(ExecConstants.CLIENT_SUPPORT_COMPLEX_TYPES);
  }

  public DrillConfig getConfig() {
    return config;
  }

  @Override
  public void setAutoRead(boolean enableAutoRead) {
    client.setAutoRead(enableAutoRead);
  }

  /**
   * Sets the client name.
   *
   * If not set, default is {@code DrillClient#DEFAULT_CLIENT_NAME}.
   *
   * @param name the client name
   *
   * @throws IllegalStateException if called after a connection has been established.
   * @throws NullPointerException if client name is null
   */
  public void setClientName(String name) {
    if (connected) {
      throw new IllegalStateException("Attempted to modify client connection property after connection has been established.");
    }
    this.clientName = checkNotNull(name, "client name should not be null");
  }

  /**
   * Sets whether the application is willing to accept complex types (Map,
   * Arrays) in the returned result set. Default is {@code true}. If set to
   * {@code false}, the complex types are returned as JSON encoded VARCHAR type.
   *
   * @throws IllegalStateException
   *           if called after a connection has been established.
   */
  public void setSupportComplexTypes(boolean supportComplexTypes) {
    if (connected) {
      throw new IllegalStateException("Attempted to modify client connection property after connection has been established.");
    }
    this.supportComplexTypes = supportComplexTypes;
  }

  /**
   * Connects the client to a Drillbit server
   *
   * @throws RpcException
   */
  public void connect() throws RpcException {
    connect(null, new Properties());
  }

  /**
   * Start's a connection from client to server
   * @param props - not null {@link Properties} filled with connection url parameters
   * @throws RpcException
   */
  public void connect(Properties props) throws RpcException {
    connect(null, props);
  }

  /**
   * Populates the endpointlist with drillbits information provided in the connection string by client.
   * For direct connection we can have connection string with drillbit property as below:
   * <dl>
   *   <dt>drillbit=ip</dt>
   *   <dd>use the ip specified as the Foreman ip with default port in config file</dd>
   *   <dt>drillbit=ip:port</dt>
   *   <dd>use the ip and port specified as the Foreman ip and port</dd>
   *   <dt>drillbit=ip1:port1,ip2:port2,...</dt>
   *   <dd>randomly select the ip and port pair from the specified list as the Foreman ip and port.</dd>
   * </dl>
   *
   * @param drillbits string with drillbit value provided in connection string
   * @param defaultUserPort string with default userport of drillbit specified in config file
   * @return list of drillbit endpoints parsed from connection string
   * @throws InvalidConnectionInfoException if the connection string has invalid or no drillbit information
   */
  static List<DrillbitEndpoint> parseAndVerifyEndpoints(String drillbits, String defaultUserPort)
                                throws InvalidConnectionInfoException {
    // If no drillbits is provided then throw exception
    drillbits = drillbits.trim();
    if (drillbits.isEmpty()) {
      throw new InvalidConnectionInfoException("No drillbit information specified in the connection string");
    }

    final List<DrillbitEndpoint> endpointList = new ArrayList<>();
    final String[] connectInfo = drillbits.split(",");

    // Fetch ip address and port information for each drillbit and populate the list
    for (String drillbit : connectInfo) {

      // Trim all the empty spaces and check if the entry is empty string.
      // Ignore the empty ones.
      drillbit = drillbit.trim();

      if (!drillbit.isEmpty()) {
        // Verify if we have only ":" or only ":port" pattern
        if (drillbit.charAt(0) == ':') {
          // Invalid drillbit information
          throw new InvalidConnectionInfoException("Malformed connection string with drillbit hostname or " +
                                                     "hostaddress missing for an entry: " + drillbit);
        }

        // We are now sure that each ip:port entry will have both the values atleast once.
        // Split each drillbit connection string to get ip address and port value
        final String[] drillbitInfo = drillbit.split(":");

        // Check if we have more than one port
        if (drillbitInfo.length > 2) {
          throw new InvalidConnectionInfoException("Malformed connection string with more than one port in a " +
                                                     "drillbit entry: " + drillbit);
        }

        // At this point we are sure that drillbitInfo has atleast hostname or host address
        // trim all the empty spaces which might be present in front of hostname or
        // host address information
        final String ipAddress = drillbitInfo[0].trim();
        String port = defaultUserPort;

        if (drillbitInfo.length == 2) {
          // We have a port value also given by user. trim all the empty spaces between : and port value before
          // validating the correctness of value.
          port = drillbitInfo[1].trim();
        }

        try {
          final DrillbitEndpoint endpoint = DrillbitEndpoint.newBuilder()
                                            .setAddress(ipAddress)
                                            .setUserPort(Integer.parseInt(port))
                                            .build();

          endpointList.add(endpoint);
        } catch (NumberFormatException e) {
          throw new InvalidConnectionInfoException("Malformed port value in entry: " + ipAddress + ":" + port + " " +
                                                     "passed in connection string");
        }
      }
    }
    if (endpointList.size() == 0) {
      throw new InvalidConnectionInfoException("No valid drillbit information specified in the connection string");
    }
    return endpointList;
  }

  /**
   * Start's a connection from client to server
   * @param connect - Zookeeper connection string provided at connection URL
   * @param props - not null {@link Properties} filled with connection url parameters
   * @throws RpcException
   */
  public synchronized void connect(String connect, Properties props) throws RpcException {
    if (connected) {
      return;
    }

    properties = DrillProperties.createFromProperties(props);
    final List<DrillbitEndpoint> endpoints = new ArrayList<>();

    if (isDirectConnection) {
      // Populate the endpoints list with all the drillbit information provided in the connection string
      endpoints.addAll(parseAndVerifyEndpoints(properties.getProperty(DrillProperties.DRILLBIT_CONNECTION),
                                               config.getString(ExecConstants.INITIAL_USER_PORT)));
    } else {
      if (ownsZkConnection) {
        try {
          this.clusterCoordinator = new ZKClusterCoordinator(this.config, connect);
          this.clusterCoordinator.start(10000);
        } catch (Exception e) {
          throw new RpcException("Failure setting up ZK for client.", e);
        }
      }
      // Gets the drillbit endpoints that are ONLINE and excludes the drillbits that are
      // in QUIESCENT state. This avoids the clients connecting to drillbits that are
      // shutting down thereby avoiding reducing the chances of query failures.
      endpoints.addAll(clusterCoordinator.getOnlineEndPoints());
      // Make sure we have at least one endpoint in the list
      checkState(!endpoints.isEmpty(), "No active Drillbit endpoint found from ZooKeeper. Check connection parameters?");
    }

    // shuffle the collection then get the first endpoint
    Collections.shuffle(endpoints);

    eventLoopGroup = createEventLoop(config.getInt(ExecConstants.CLIENT_RPC_THREADS), "Client-");
    executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(),
        new NamedThreadFactory("drill-client-executor-")) {
      @Override
      protected void afterExecute(final Runnable r, final Throwable t) {
        if (t != null) {
          logger.error("{}.run() leaked an exception.", r.getClass().getName(), t);
        }
        super.afterExecute(r, t);
      }
    };

    final String connectTriesConf = properties.getProperty(DrillProperties.TRIES, "5");
    int connectTriesVal;
    try {
      connectTriesVal = Math.min(endpoints.size(), Integer.parseInt(connectTriesConf));
    } catch (NumberFormatException e) {
      throw new InvalidConnectionInfoException("Invalid tries value: " + connectTriesConf + " specified in " +
                                               "connection string");
    }

    // If the value provided in the connection string is <=0 then override with 1 since we want to try connecting
    // at least once
    connectTriesVal = Math.max(1, connectTriesVal);

    int triedEndpointIndex = 0;
    DrillbitEndpoint endpoint;

    while (triedEndpointIndex < connectTriesVal) {
      endpoint = endpoints.get(triedEndpointIndex);

      // Set in both props and properties since props is passed to UserClient
      // TODO: Logically here it's doing putIfAbsent, please change to use that api once JDK 8 is minimum required
      // version
      if (!properties.containsKey(DrillProperties.SERVICE_HOST)) {
        properties.setProperty(DrillProperties.SERVICE_HOST, endpoint.getAddress());
        props.setProperty(DrillProperties.SERVICE_HOST, endpoint.getAddress());
      }

      // Note: the properties member is a DrillProperties instance which lower cases names of
      // properties. That does not work too well with properties that are mixed case.
      // For user client severla properties are mixed case so we do not use the properties member
      // but instead pass the props parameter.
      client = new UserClient(clientName, config, props, supportComplexTypes, allocator, eventLoopGroup, executor, endpoint);
      logger.debug("Connecting to server {}:{}", endpoint.getAddress(), endpoint.getUserPort());

      try {
        connect(endpoint);
        connected = true;
        logger.info("Successfully connected to server {}:{}", endpoint.getAddress(), endpoint.getUserPort());
        break;
      } catch (NonTransientRpcException ex) {
        logger.error("Connection to {}:{} failed with error {}. Not retrying anymore", endpoint.getAddress(),
                     endpoint.getUserPort(), ex.getMessage());
        throw ex;
      } catch (RpcException ex) {
        ++triedEndpointIndex;
        logger.error("Attempt {}: Failed to connect to server {}:{}", triedEndpointIndex, endpoint.getAddress(),
                     endpoint.getUserPort());

        // Throw exception when we have exhausted all the tries without having a successful connection
        if (triedEndpointIndex == connectTriesVal) {
          throw ex;
        }

        // Close the connection here to avoid calling close twice in case when all tries are exhausted.
        // Since DrillClient.close is also calling client.close
        client.close();
      }
    }
  }

  protected static EventLoopGroup createEventLoop(int size, String prefix) {
    return TransportCheck.createEventLoopGroup(size, prefix);
  }

  public synchronized boolean reconnect() {
    if (client.isActive()) {
      return true;
    }
    int retry = reconnectTimes;
    while (retry > 0) {
      retry--;
      try {
        Thread.sleep(this.reconnectDelay);
        // Gets the drillbit endpoints that are ONLINE and excludes the drillbits that are
        // in QUIESCENT state. This avoids the clients connecting to drillbits that are
        // shutting down thereby reducing the chances of query failures.
        final ArrayList<DrillbitEndpoint> endpoints = new ArrayList<>(clusterCoordinator.getOnlineEndPoints());
        if (endpoints.isEmpty()) {
          continue;
        }
        client.close();
        Collections.shuffle(endpoints);
        connect(endpoints.iterator().next());
        return true;
      } catch (Exception e) {
      }
    }
    return false;
  }

  private void connect(DrillbitEndpoint endpoint) throws RpcException {
    client.connect(endpoint, properties, getUserCredentials());
    logger.info("Foreman drillbit is {}", endpoint.getAddress());
  }

  public BufferAllocator getAllocator() {
    return allocator;
  }

  /**
   * Closes this client's connection to the server
   */
  @Override
  public void close() {
    if (client != null) {
      client.close();
    }
    if (ownsAllocator && allocator != null) {
      DrillAutoCloseables.closeNoChecked(allocator);
    }
    if (ownsZkConnection) {
      if (clusterCoordinator != null) {
        try {
          clusterCoordinator.close();
          clusterCoordinator = null;
        } catch (Exception e) {
          logger.warn("Error while closing Cluster Coordinator.", e);
        }
      }
    }
    if (eventLoopGroup != null) {
      eventLoopGroup.shutdownGracefully();
      eventLoopGroup = null;
    }

    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }

    connected = false;
  }

  /**
   * Return the server infos. Only available after connecting
   *
   * The result might be null if the server doesn't provide the informations.
   *
   * @return the server informations, or null if not connected or if the server
   *         doesn't provide the information
   * @deprecated use {@code DrillClient#getServerVersion()}
   */
  @Deprecated
  public RpcEndpointInfos getServerInfos() {
    return client != null ? client.getServerInfos() : null;
  }

  /**
   * Return the server name. Only available after connecting
   *
   * The result might be null if the server doesn't provide the name information.
   *
   * @return the server name, or null if not connected or if the server
   *         doesn't provide the name
   * @return The server name.
   */
  public String getServerName() {
    return (client != null && client.getServerInfos() != null) ? client.getServerInfos().getName() : null;
  }

  /**
   * Return the server version. Only available after connecting
   *
   * The result might be null if the server doesn't provide the version information.
   *
   * @return the server version, or null if not connected or if the server
   *         doesn't provide the version
   * @return The server version.
   */
  public Version getServerVersion() {
    return (client != null && client.getServerInfos() != null) ? UserRpcUtils.getVersion(client.getServerInfos()) : null;
  }

  /**
   * Get server meta information
   *
   * Get meta information about the server like the the available functions
   * or the identifier quoting string used by the current session
   *
   * @return a future to the server meta response
   */
  public DrillRpcFuture<GetServerMetaResp> getServerMeta() {
    return client.send(RpcType.GET_SERVER_META, GetServerMetaReq.getDefaultInstance(), GetServerMetaResp.class);
  }

  /**
   * Returns the list of methods supported by the server based on its advertised information.
   *
   * @return an immutable set of capabilities
   */
  public Set<ServerMethod> getSupportedMethods() {
    return client != null ? ServerMethod.getSupportedMethods(client.getSupportedMethods(), client.getServerInfos()) : null;
  }

  /**
   * Submits a string based query plan for execution and returns the result batches. Supported query types are:
   * <p><ul>
   *  <li>{@link QueryType#LOGICAL}
   *  <li>{@link QueryType#PHYSICAL}
   *  <li>{@link QueryType#SQL}
   * </ul>
   *
   * @param type Query type
   * @param plan Query to execute
   * @return a handle for the query result
   * @throws RpcException
   */
  public List<QueryDataBatch> runQuery(QueryType type, String plan) throws RpcException {
    checkArgument(type == QueryType.LOGICAL || type == QueryType.PHYSICAL || type == QueryType.SQL,
        String.format("Only query types %s, %s and %s are supported in this API",
            QueryType.LOGICAL, QueryType.PHYSICAL, QueryType.SQL));
    final UserProtos.RunQuery query = newBuilder().setResultsMode(STREAM_FULL).setType(type).setPlan(plan).build();
    final ListHoldingResultsListener listener = new ListHoldingResultsListener(query);
    client.submitQuery(listener, query);
    return listener.getResults();
  }

  /**
   * API to just plan a query without execution
   *
   * @param type
   * @param query
   * @param isSplitPlan
   *          option to tell whether to return single or split plans for a query
   * @return list of PlanFragments that can be used later on in
   *         {@link #runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType,
   *         java.util.List, org.apache.drill.exec.rpc.user.UserResultsListener)}
   *         to run a query without additional planning
   */
  public DrillRpcFuture<QueryPlanFragments> planQuery(QueryType type, String query, boolean isSplitPlan) {
    GetQueryPlanFragments runQuery = GetQueryPlanFragments.newBuilder().setQuery(query).setType(type).setSplitPlan(isSplitPlan).build();
    return client.planQuery(runQuery);
  }

  /**
   * Run query based on list of fragments that were supposedly produced during query planning phase. Supported
   * query type is {@link QueryType#EXECUTION}
   * @param type
   * @param planFragments
   * @param resultsListener
   * @throws RpcException
   */
  public void runQuery(QueryType type, List<PlanFragment> planFragments, UserResultsListener resultsListener)
      throws RpcException {
    // QueryType can be only executional
    checkArgument((QueryType.EXECUTION == type), "Only EXECUTION type query is supported with PlanFragments");
    // setting Plan on RunQuery will be used for logging purposes and therefore can not be null
    // since there is no Plan string provided we will create a JsonArray out of individual fragment Plans
    ArrayNode jsonArray = objectMapper.createArrayNode();
    for (PlanFragment fragment : planFragments) {
      try {
        jsonArray.add(objectMapper.readTree(fragment.getFragmentJson()));
      } catch (IOException e) {
        logger.error("Exception while trying to read PlanFragment JSON for {}", fragment.getHandle().getQueryId(), e);
        throw new RpcException(e);
      }
    }
    final String fragmentsToJsonString;
    try {
      fragmentsToJsonString = objectMapper.writeValueAsString(jsonArray);
    } catch (JsonProcessingException e) {
      logger.error("Exception while trying to get JSONString from Array of individual Fragments Json for %s", e);
      throw new RpcException(e);
    }
    final UserProtos.RunQuery query = newBuilder().setType(type).addAllFragments(planFragments)
        .setPlan(fragmentsToJsonString)
        .setResultsMode(STREAM_FULL).build();
    client.submitQuery(resultsListener, query);
  }

  /**
   * Helper method to generate the UserCredentials message from the properties.
   */
  private UserBitShared.UserCredentials getUserCredentials() {
    String userName = properties.getProperty(DrillProperties.USER);
    if (Strings.isNullOrEmpty(userName)) {
      userName = "anonymous"; // if username is not propagated as one of the properties
    }
    return UserBitShared.UserCredentials.newBuilder()
      .setUserName(userName)
      .build();
  }

  public DrillRpcFuture<Ack> cancelQuery(QueryId id) {
    if(logger.isDebugEnabled()) {
      logger.debug("Cancelling query {}", QueryIdHelper.getQueryId(id));
    }
    return client.send(RpcType.CANCEL_QUERY, id, Ack.class);
  }

  public DrillRpcFuture<Ack> resumeQuery(final QueryId queryId) {
    if(logger.isDebugEnabled()) {
      logger.debug("Resuming query {}", QueryIdHelper.getQueryId(queryId));
    }
    return client.send(RpcType.RESUME_PAUSED_QUERY, queryId, Ack.class);
  }

  /**
   * Get the list of catalogs in {@code INFORMATION_SCHEMA.CATALOGS} table satisfying the given filters.
   *
   * @param catalogNameFilter Filter on {@code catalog name}. Pass null to apply no filter.
   * @return The list of catalogs in {@code INFORMATION_SCHEMA.CATALOGS} table satisfying the given filters.
   */
  public DrillRpcFuture<GetCatalogsResp> getCatalogs(LikeFilter catalogNameFilter) {
    final GetCatalogsReq.Builder reqBuilder = GetCatalogsReq.newBuilder();
    if (catalogNameFilter != null) {
      reqBuilder.setCatalogNameFilter(catalogNameFilter);
    }

    return client.send(RpcType.GET_CATALOGS, reqBuilder.build(), GetCatalogsResp.class);
  }

  /**
   * Get the list of schemas in {@code INFORMATION_SCHEMA.SCHEMATA} table satisfying the given filters.
   *
   * @param catalogNameFilter Filter on {@code catalog name}. Pass null to apply no filter.
   * @param schemaNameFilter Filter on {@code schema name}. Pass null to apply no filter.
   * @return The list of schemas in {@code INFORMATION_SCHEMA.SCHEMATA} table satisfying the given filters.
   */
  public DrillRpcFuture<GetSchemasResp> getSchemas(LikeFilter catalogNameFilter, LikeFilter schemaNameFilter) {
    final GetSchemasReq.Builder reqBuilder = GetSchemasReq.newBuilder();
    if (catalogNameFilter != null) {
      reqBuilder.setCatalogNameFilter(catalogNameFilter);
    }

    if (schemaNameFilter != null) {
      reqBuilder.setSchemaNameFilter(schemaNameFilter);
    }

    return client.send(RpcType.GET_SCHEMAS, reqBuilder.build(), GetSchemasResp.class);
  }

  /**
   * Get the list of tables in {@code INFORMATION_SCHEMA.TABLES} table satisfying the given filters.
   *
   * @param catalogNameFilter Filter on {@code catalog name}. Pass null to apply no filter.
   * @param schemaNameFilter Filter on {@code schema name}. Pass null to apply no filter.
   * @param tableNameFilter Filter in {@code table name}. Pass null to apply no filter.
   * @param tableTypeFilter Filter in {@code table type}. Pass null to apply no filter
   * @return The list of tables in {@code INFORMATION_SCHEMA.TABLES} table satisfying the given filters.
   */
  public DrillRpcFuture<GetTablesResp> getTables(LikeFilter catalogNameFilter, LikeFilter schemaNameFilter,
      LikeFilter tableNameFilter, List<String> tableTypeFilter) {
    final GetTablesReq.Builder reqBuilder = GetTablesReq.newBuilder();
    if (catalogNameFilter != null) {
      reqBuilder.setCatalogNameFilter(catalogNameFilter);
    }

    if (schemaNameFilter != null) {
      reqBuilder.setSchemaNameFilter(schemaNameFilter);
    }

    if (tableNameFilter != null) {
      reqBuilder.setTableNameFilter(tableNameFilter);
    }

    if (tableTypeFilter != null) {
      reqBuilder.addAllTableTypeFilter(tableTypeFilter);
    }

    return client.send(RpcType.GET_TABLES, reqBuilder.build(), GetTablesResp.class);
  }

  /**
   * Get the list of columns in {@code INFORMATION_SCHEMA.COLUMNS} table satisfying the given filters.
   *
   * @param catalogNameFilter Filter on {@code catalog name}. Pass null to apply no filter.
   * @param schemaNameFilter Filter on {@code schema name}. Pass null to apply no filter.
   * @param tableNameFilter Filter in {@code table name}. Pass null to apply no filter.
   * @param columnNameFilter Filter in {@code column name}. Pass null to apply no filter.
   * @return The list of columns in {@code INFORMATION_SCHEMA.COLUMNS} table satisfying the given filters.
   */
  public DrillRpcFuture<GetColumnsResp> getColumns(LikeFilter catalogNameFilter, LikeFilter schemaNameFilter,
      LikeFilter tableNameFilter, LikeFilter columnNameFilter) {
    final GetColumnsReq.Builder reqBuilder = GetColumnsReq.newBuilder();
    if (catalogNameFilter != null) {
      reqBuilder.setCatalogNameFilter(catalogNameFilter);
    }

    if (schemaNameFilter != null) {
      reqBuilder.setSchemaNameFilter(schemaNameFilter);
    }

    if (tableNameFilter != null) {
      reqBuilder.setTableNameFilter(tableNameFilter);
    }

    if (columnNameFilter != null) {
      reqBuilder.setColumnNameFilter(columnNameFilter);
    }

    return client.send(RpcType.GET_COLUMNS, reqBuilder.build(), GetColumnsResp.class);
  }

  /**
   * Create a prepared statement for given the {@code query}.
   *
   * @param query
   * @return The prepared statement for given the {@code query}.
   */
  public DrillRpcFuture<CreatePreparedStatementResp> createPreparedStatement(final String query) {
    final CreatePreparedStatementReq req =
        CreatePreparedStatementReq.newBuilder()
            .setSqlQuery(query)
            .build();

    return client.send(RpcType.CREATE_PREPARED_STATEMENT, req, CreatePreparedStatementResp.class);
  }

  /**
   * Execute the given prepared statement.
   *
   * @param preparedStatementHandle Prepared statement handle returned in response to
   *                                {@link #createPreparedStatement(String)}.
   * @param resultsListener {@link UserResultsListener} instance for listening for query results.
   */
  public void executePreparedStatement(final PreparedStatementHandle preparedStatementHandle,
      final UserResultsListener resultsListener) {
    final RunQuery runQuery = newBuilder()
        .setResultsMode(STREAM_FULL)
        .setType(QueryType.PREPARED_STATEMENT)
        .setPreparedStatementHandle(preparedStatementHandle)
        .build();
    client.submitQuery(resultsListener, runQuery);
  }

  /**
   * Execute the given prepared statement and return the results.
   *
   * @param preparedStatementHandle Prepared statement handle returned in response to
   *                                {@link #createPreparedStatement(String)}.
   * @return List of {@link QueryDataBatch}s. It is responsibility of the caller to release query data batches.
   * @throws RpcException
   */
  @VisibleForTesting
  public List<QueryDataBatch> executePreparedStatement(final PreparedStatementHandle preparedStatementHandle)
      throws RpcException {
    final RunQuery runQuery = newBuilder()
        .setResultsMode(STREAM_FULL)
        .setType(QueryType.PREPARED_STATEMENT)
        .setPreparedStatementHandle(preparedStatementHandle)
        .build();

    final ListHoldingResultsListener resultsListener = new ListHoldingResultsListener(runQuery);

    client.submitQuery(resultsListener, runQuery);

    return resultsListener.getResults();
  }

  /**
   * Submits a Logical plan for direct execution (bypasses parsing)
   *
   * @param  plan  the plan to execute
   */
  public void runQuery(QueryType type, String plan, UserResultsListener resultsListener) {
    client.submitQuery(resultsListener, newBuilder().setResultsMode(STREAM_FULL).setType(type).setPlan(plan).build());
  }

  /**
   * @return true if client has connection and it is connected, false otherwise
   */
  public boolean connectionIsActive() {
    return client.isActive();
  }

  /**
   * Verify connection with request-answer.
   *
   * @param timeoutSec time in seconds to wait answer receiving. If 0 then won't wait.
   * @return true if {@link GeneralRPCProtos.RpcMode#PONG PONG} received until timeout, false otherwise
   */
  public boolean hasPing(long timeoutSec) throws DrillRuntimeException {
    if (timeoutSec < 0) {
      timeoutSec = 0;
    }
    return client.hasPing(timeoutSec);
  }

  private class ListHoldingResultsListener implements UserResultsListener {
    private final Vector<QueryDataBatch> results = new Vector<>();
    private final SettableFuture<List<QueryDataBatch>> future = SettableFuture.create();
    private final UserProtos.RunQuery query;

    public ListHoldingResultsListener(UserProtos.RunQuery query) {
      logger.debug( "Listener created for query \"{}\"", query );
      this.query = query;
    }

    @Override
    public void submissionFailed(UserException ex) {
      // or  !client.isActive()
      if (ex.getCause() instanceof ChannelClosedException) {
        if (reconnect()) {
          try {
            client.submitQuery(this, query);
          } catch (Exception e) {
            fail(e);
          }
        } else {
          fail(ex);
        }
      } else {
        fail(ex);
      }
    }

    @Override
    public void queryCompleted(QueryState state) {
      future.set(results);
    }

    private void fail(Exception ex) {
      logger.debug("Submission failed.", ex);
      future.setException(ex);
      future.set(results);
    }

    @Override
    public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
      logger.debug("Result arrived:  Row count: {}", result.getHeader().getRowCount());
      logger.trace("Result batch: {}", result);
      results.add(result);
    }

    public List<QueryDataBatch> getResults() throws RpcException{
      try {
        return future.get();
      } catch (Throwable t) {
        /*
         * Since we're not going to return the result to the caller
         * to clean up, we have to do it.
         */
        for(final QueryDataBatch queryDataBatch : results) {
          queryDataBatch.release();
        }

        throw RpcException.mapException(t);
      }
    }

    @Override
    public void queryIdArrived(QueryId queryId) {
      if (logger.isDebugEnabled()) {
        logger.debug("Query ID arrived: {}", QueryIdHelper.getQueryId(queryId));
      }
    }
  }
}
