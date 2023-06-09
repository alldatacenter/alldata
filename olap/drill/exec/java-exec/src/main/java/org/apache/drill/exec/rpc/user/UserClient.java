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
package org.apache.drill.exec.rpc.user;

import org.apache.drill.common.concurrent.AbstractCheckedFuture;
import org.apache.drill.common.concurrent.CheckedFuture;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.base.Throwables;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.apache.drill.common.KerberosUtil;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.client.InvalidConnectionInfoException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult;
import org.apache.drill.exec.proto.UserBitShared.SaslMessage;
import org.apache.drill.exec.proto.UserBitShared.UserCredentials;
import org.apache.drill.exec.proto.UserProtos.BitToUserHandshake;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementResp;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsResp;
import org.apache.drill.exec.proto.UserProtos.GetColumnsResp;
import org.apache.drill.exec.proto.UserProtos.GetQueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.GetSchemasResp;
import org.apache.drill.exec.proto.UserProtos.GetServerMetaResp;
import org.apache.drill.exec.proto.UserProtos.GetTablesResp;
import org.apache.drill.exec.proto.UserProtos.QueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.RpcEndpointInfos;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.proto.UserProtos.SaslSupport;
import org.apache.drill.exec.proto.UserProtos.UserToBitHandshake;
import org.apache.drill.exec.rpc.AbstractClientConnection;
import org.apache.drill.exec.rpc.Acks;
import org.apache.drill.exec.rpc.BasicClient;
import org.apache.drill.exec.rpc.ConnectionMultiListener;
import org.apache.drill.exec.rpc.DrillRpcFuture;
import org.apache.drill.exec.rpc.NonTransientRpcException;
import org.apache.drill.exec.rpc.OutOfMemoryHandler;
import org.apache.drill.exec.rpc.ProtobufLengthDecoder;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.RpcConnectionHandler;
import org.apache.drill.exec.rpc.RpcConstants;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.security.AuthStringUtil;
import org.apache.drill.exec.rpc.security.AuthenticatorFactory;
import org.apache.drill.exec.rpc.security.ClientAuthenticatorProvider;
import org.apache.drill.exec.rpc.security.SaslProperties;
import org.apache.drill.exec.rpc.security.plain.PlainFactory;
import org.apache.drill.exec.ssl.SSLConfig;
import org.apache.drill.exec.ssl.SSLConfigBuilder;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;

import javax.net.ssl.SSLEngine;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UserClient
    extends BasicClient<RpcType, UserClient.UserToBitConnection, UserToBitHandshake, BitToUserHandshake> {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UserClient.class);

  private final BufferAllocator allocator;
  private final QueryResultHandler queryResultHandler = new QueryResultHandler();
  private final String clientName;
  private final boolean supportComplexTypes;

  private RpcEndpointInfos serverInfos = null;
  private Set<RpcType> supportedMethods = null;

  private SSLConfig sslConfig;
  private DrillbitEndpoint endpoint;

  private DrillProperties properties;

  public UserClient(String clientName, DrillConfig config, Properties properties, boolean supportComplexTypes,
      BufferAllocator allocator, EventLoopGroup eventLoopGroup, Executor eventExecutor,
      DrillbitEndpoint endpoint) throws NonTransientRpcException {
    super(UserRpcConfig.getMapping(config, eventExecutor), allocator.getAsByteBufAllocator(),
        eventLoopGroup, RpcType.HANDSHAKE, BitToUserHandshake.class, BitToUserHandshake.PARSER);
    this.endpoint = endpoint; // save the endpoint; it might be needed by SSL init.
    this.clientName = clientName;
    this.allocator = allocator;
    this.supportComplexTypes = supportComplexTypes;
    try {
      this.sslConfig = new SSLConfigBuilder().properties(properties).mode(SSLConfig.Mode.CLIENT)
          .initializeSSLContext(true).validateKeyStore(false).build();
    } catch (DrillException e) {
      throw new InvalidConnectionInfoException(e.getMessage());
    }

    // Keep a copy of properties in UserClient
    this.properties = DrillProperties.createFromProperties(properties);
  }

  @Override protected void setupSSL(ChannelPipeline pipe,
      ConnectionMultiListener.SSLHandshakeListener sslHandshakeListener) {

    String peerHost = endpoint.getAddress();
    int peerPort = endpoint.getUserPort();
    SSLEngine sslEngine = sslConfig.createSSLEngine(allocator, peerHost, peerPort);

    // Add SSL handler into pipeline
    SslHandler sslHandler = new SslHandler(sslEngine);
    sslHandler.setHandshakeTimeoutMillis(sslConfig.getHandshakeTimeout());

    // Add a listener for SSL Handshake complete. The Drill client handshake will be enabled only
    // after this is done.
    sslHandler.handshakeFuture().addListener(sslHandshakeListener);
    pipe.addFirst(RpcConstants.SSL_HANDLER, sslHandler);
    logger.debug(sslConfig.toString());
  }

  @Override protected boolean isSslEnabled() {
    return sslConfig.isUserSslEnabled();
  }

  public RpcEndpointInfos getServerInfos() {
    return serverInfos;
  }

  public Set<RpcType> getSupportedMethods() {
    return supportedMethods;
  }

  public void submitQuery(UserResultsListener resultsListener, RunQuery query) {
    send(queryResultHandler.getWrappedListener(resultsListener), RpcType.RUN_QUERY, query, QueryId.class);
  }

  /**
   * Connects, and if required, authenticates. This method blocks until both operations are complete.
   *
   * @param endpoint    endpoint to connect to
   * @param properties  properties
   * @param credentials credentials
   * @throws RpcException if either connection or authentication fails
   */
  public void connect(final DrillbitEndpoint endpoint, final DrillProperties properties,
      final UserCredentials credentials) throws RpcException {
    final UserToBitHandshake.Builder hsBuilder =
        UserToBitHandshake.newBuilder()
            .setRpcVersion(UserRpcConfig.RPC_VERSION)
            .setSupportListening(true)
            .setSupportComplexTypes(supportComplexTypes)
            .setSupportTimeout(true).setCredentials(credentials)
            .setClientInfos(UserRpcUtils.getRpcEndpointInfos(clientName))
            .setSaslSupport(SaslSupport.SASL_PRIVACY)
            .setProperties(properties.serializeForServer());

    // Only used for testing purpose
    if (properties.containsKey(DrillProperties.TEST_SASL_LEVEL)) {
      hsBuilder.setSaslSupport(
          SaslSupport.valueOf(Integer.parseInt(properties.getProperty(DrillProperties.TEST_SASL_LEVEL))));
    }

    try {
      if (sslConfig.isUserSslEnabled()) {
        connect(hsBuilder.build(), endpoint).checkedGet(sslConfig.getHandshakeTimeout(), TimeUnit.MILLISECONDS);
      } else {
        connect(hsBuilder.build(), endpoint).checkedGet();
      }
    } // Treat all authentication related exception as NonTransientException, since in those cases retry by client
    // should not happen
    catch(TimeoutException e) {
      String msg = new StringBuilder().append("Connecting to the server timed out. This is sometimes due to a " +
        "mismatch in the SSL configuration between" + " client and server. [ Exception: ").append(e.getMessage())
        .append("]").toString();
      throw new NonTransientRpcException(msg);
    } catch (SaslException e) {
      throw new NonTransientRpcException(e);
    } catch (RpcException e) {
      throw e;
    } catch (Exception e) {
      throw new RpcException(e);
    }
  }

  /**
   * Validate that security requirements from client and Drillbit side is compatible. For example: It verifies if one
   * side needs authentication / encryption then other side is also configured to support that security properties.
   * @param properties - DrillClient connection parameters
   * @param serverAuthMechs - list of auth mechanisms supported by server
   * @throws NonTransientRpcException - When DrillClient security requirements doesn't match Drillbit side of security
   *                                    configurations.
   */
  private void validateSaslCompatibility(DrillProperties properties, List<String> serverAuthMechs)
    throws NonTransientRpcException {

    final boolean clientNeedsEncryption = properties.containsKey(DrillProperties.SASL_ENCRYPT)
        && Boolean.parseBoolean(properties.getProperty(DrillProperties.SASL_ENCRYPT));

    final boolean serverAuthConfigured = (serverAuthMechs != null);

    // Check if client needs encryption and server is not configured for encryption.
    if (clientNeedsEncryption && !connection.isEncryptionEnabled()) {
      throw new NonTransientRpcException(
          "Client needs encrypted connection but server is not configured for encryption." +
              " Please contact your administrator. [Warn: It may be due to wrong config or a security attack in" +
              " progress.]");
    }

    // Check if client needs encryption and server doesn't support any security mechanisms.
    if (clientNeedsEncryption && !serverAuthConfigured) {
      throw new NonTransientRpcException(
          "Client needs encrypted connection but server doesn't support any security mechanisms." +
              " Please contact your administrator. [Warn: It may be due to wrong config or a security attack in" +
              " progress.]");
    }

    // Check if client needs authentication and server doesn't support any security mechanisms.
    if (clientNeedsAuthExceptPlain(properties) && !serverAuthConfigured) {
      throw new NonTransientRpcException(
          "Client needs authentication but server doesn't support any security mechanisms." +
              " Please contact your administrator. [Warn: It may be due to wrong config or a security attack in" +
              " progress.]");
    }
  }

  /**
   * Determine if client needs authenticated connection or not. It checks for following as an indication of
   * requiring authentication from client side:
   * 1) Auth mechanism except PLAIN is provided in connection properties with DrillProperties.AUTH_MECHANISM parameter.
   * 2) A service principal is provided in connection properties in which case we treat AUTH_MECHANISM as Kerberos
   * @param props - DrillClient connection parameters
   * @return - true  - If any of above 2 checks is successful.
   *         - false - If all the above 2 checks fails.
   */
  private boolean clientNeedsAuthExceptPlain(DrillProperties props) {

    boolean clientNeedsAuth = false;
    final String authMechanism =  props.getProperty(DrillProperties.AUTH_MECHANISM);
    if (!Strings.isNullOrEmpty(authMechanism) && !authMechanism.equalsIgnoreCase(PlainFactory.SIMPLE_NAME)) {
      clientNeedsAuth = true;
    }

    clientNeedsAuth |= !Strings.isNullOrEmpty(props.getProperty(DrillProperties.SERVICE_PRINCIPAL));
    return clientNeedsAuth;
  }

  private CheckedFuture<Void, IOException> connect(final UserToBitHandshake handshake,
      final DrillbitEndpoint endpoint) {
    final SettableFuture<Void> connectionSettable = SettableFuture.create();
    final CheckedFuture<Void, IOException> connectionFuture =
        new AbstractCheckedFuture<Void, IOException>(connectionSettable) {
          @Override protected IOException mapException(Exception e) {
            if (e instanceof SaslException) {
              return (SaslException)e;
            } else if (e instanceof ExecutionException) {
              final Throwable cause = Throwables.getRootCause(e);
              if (cause instanceof SaslException) {
                return (SaslException)cause;
              }
            }
            return RpcException.mapException(e);
          }
        };

    final RpcConnectionHandler<UserToBitConnection> connectionHandler =
        new RpcConnectionHandler<UserToBitConnection>() {
          @Override public void connectionSucceeded(UserToBitConnection connection) {
            connectionSettable.set(null);
          }

          @Override public void connectionFailed(FailureType type, Throwable t) {
            // Don't wrap NonTransientRpcException inside RpcException, since called should not retry to connect in
            // this case
            if (t instanceof NonTransientRpcException || t instanceof SaslException) {
              connectionSettable.setException(t);
            } else if (t instanceof RpcException) {
              final Throwable cause = t.getCause();
              if (cause instanceof SaslException) {
                connectionSettable.setException(cause);
                return;
              }
              connectionSettable.setException(t);
            } else {
              connectionSettable.setException(
                new RpcException(String.format("%s : %s", type.name(), t.getMessage()), t));
            }
          }
        };

    connectAsClient(queryResultHandler.getWrappedConnectionHandler(connectionHandler), handshake,
        endpoint.getAddress(), endpoint.getUserPort());

    return connectionFuture;
  }

  /**
   * Get's the authenticator factory for the mechanism required by client if it's supported on the server side too.
   * Otherwise it throws {@link SaslException}
   * @param properties - client connection properties
   * @param serverAuthMechanisms - list of authentication mechanisms supported by server
   * @return - {@link AuthenticatorFactory} for the mechanism required by client for authentication
   * @throws SaslException - In case of failure
   */
  private AuthenticatorFactory getAuthenticatorFactory(final DrillProperties properties,
                                                       List<String> serverAuthMechanisms) throws SaslException {
    final Set<String> mechanismSet = AuthStringUtil.asSet(serverAuthMechanisms);

    // first, check if a certain mechanism must be used
    String authMechanism = properties.getProperty(DrillProperties.AUTH_MECHANISM);
    if (authMechanism != null) {
      if (!ClientAuthenticatorProvider.getInstance().containsFactory(authMechanism)) {
        throw new SaslException(String.format("Unknown mechanism: %s", authMechanism));
      }
      if (!mechanismSet.contains(authMechanism.toUpperCase())) {
        throw new SaslException(String
            .format("Server does not support authentication using: %s. [Details: %s]", authMechanism,
                connection.getEncryptionCtxtString()));
      }
      return ClientAuthenticatorProvider.getInstance().getAuthenticatorFactory(authMechanism);
    }

    // check if Kerberos is supported, and the service principal is provided
    if (mechanismSet.contains(KerberosUtil.KERBEROS_SIMPLE_NAME) && properties
        .containsKey(DrillProperties.SERVICE_PRINCIPAL)) {
      return ClientAuthenticatorProvider.getInstance()
          .getAuthenticatorFactory(KerberosUtil.KERBEROS_SIMPLE_NAME);
    }

    // check if username/password is supported, and username/password are provided
    if (mechanismSet.contains(PlainFactory.SIMPLE_NAME) && properties.containsKey(DrillProperties.USER)
        && !Strings.isNullOrEmpty(properties.getProperty(DrillProperties.PASSWORD))) {
      return ClientAuthenticatorProvider.getInstance().getAuthenticatorFactory(PlainFactory.SIMPLE_NAME);
    }

    throw new SaslException(String
        .format("Server requires authentication using %s. Insufficient credentials?. " + "[Details: %s]. ",
          mechanismSet, connection.getEncryptionCtxtString()));
  }

  protected <SEND extends MessageLite, RECEIVE extends MessageLite> void send(
      RpcOutcomeListener<RECEIVE> listener, RpcType rpcType, SEND protobufBody, Class<RECEIVE> clazz,
      boolean allowInEventLoop, ByteBuf... dataBodies) {
    super.send(listener, connection, rpcType, protobufBody, clazz, allowInEventLoop, dataBodies);
  }

  @Override protected MessageLite getResponseDefaultInstance(int rpcType) throws RpcException {
    switch (rpcType) {
      case RpcType.ACK_VALUE:
        return Ack.getDefaultInstance();
      case RpcType.HANDSHAKE_VALUE:
        return BitToUserHandshake.getDefaultInstance();
      case RpcType.QUERY_HANDLE_VALUE:
        return QueryId.getDefaultInstance();
      case RpcType.QUERY_RESULT_VALUE:
        return QueryResult.getDefaultInstance();
      case RpcType.QUERY_DATA_VALUE:
        return QueryData.getDefaultInstance();
      case RpcType.QUERY_PLAN_FRAGMENTS_VALUE:
        return QueryPlanFragments.getDefaultInstance();
      case RpcType.CATALOGS_VALUE:
        return GetCatalogsResp.getDefaultInstance();
      case RpcType.SCHEMAS_VALUE:
        return GetSchemasResp.getDefaultInstance();
      case RpcType.TABLES_VALUE:
        return GetTablesResp.getDefaultInstance();
      case RpcType.COLUMNS_VALUE:
        return GetColumnsResp.getDefaultInstance();
      case RpcType.PREPARED_STATEMENT_VALUE:
        return CreatePreparedStatementResp.getDefaultInstance();
      case RpcType.SASL_MESSAGE_VALUE:
        return SaslMessage.getDefaultInstance();
      case RpcType.SERVER_META_VALUE:
        return GetServerMetaResp.getDefaultInstance();
    }
    throw new RpcException(String.format("Unable to deal with RpcType of %d", rpcType));
  }

  @Override protected void handle(UserToBitConnection connection, int rpcType, ByteBuf pBody, ByteBuf dBody,
      ResponseSender sender) throws RpcException {
    if (!isAuthComplete()) {
      // Remote should not be making any requests before authenticating, drop connection
      throw new RpcException(String.format("Request of type %d is not allowed without authentication. "
              + "Remote on %s must authenticate before making requests. Connection dropped.", rpcType,
          connection.getRemoteAddress()));
    }
    switch (rpcType) {
      case RpcType.QUERY_DATA_VALUE:
        queryResultHandler.batchArrived(connection, pBody, dBody);
        sender.send(new Response(RpcType.ACK, Acks.OK));
        break;
      case RpcType.QUERY_RESULT_VALUE:
        queryResultHandler.resultArrived(pBody);
        sender.send(new Response(RpcType.ACK, Acks.OK));
        break;
      default:
        throw new RpcException(String.format("Unknown Rpc Type %d. ", rpcType));
    }
  }

  @Override
  protected void prepareSaslHandshake(final RpcConnectionHandler<UserToBitConnection> connectionHandler,
                                      List<String> serverAuthMechanisms) {
    try {
      final Map<String, String> saslProperties = properties.stringPropertiesAsMap();

      // Set correct QOP property and Strength based on server needs encryption or not.
      // If ChunkMode is enabled then negotiate for buffer size equal to wrapChunkSize,
      // If ChunkMode is disabled then negotiate for MAX_WRAPPED_SIZE buffer size.
      saslProperties.putAll(
        SaslProperties.getSaslProperties(connection.isEncryptionEnabled(), connection.getMaxWrappedSize()));

      final AuthenticatorFactory factory = getAuthenticatorFactory(properties, serverAuthMechanisms);
      final String mechanismName = factory.getSimpleName();
      logger.trace("Will try to authenticate to server using {} mechanism with encryption context {}",
        mechanismName, connection.getEncryptionCtxtString());

      // Update the thread context class loader to current class loader
      // See DRILL-6063 for detailed description
      final ClassLoader oldThreadCtxtCL = Thread.currentThread().getContextClassLoader();
      final ClassLoader newThreadCtxtCL = this.getClass().getClassLoader();
      Thread.currentThread().setContextClassLoader(newThreadCtxtCL);
      final UserGroupInformation ugi = factory.createAndLoginUser(saslProperties);
      // Reset the thread context class loader to original one
      Thread.currentThread().setContextClassLoader(oldThreadCtxtCL);

      startSaslHandshake(connectionHandler, saslProperties, ugi, factory, RpcType.SASL_MESSAGE);
    } catch (IOException e) {
      logger.error("Failed while doing setup for starting SASL handshake for connection {}", connection.getName());
      final Exception ex = new RpcException(String.format("Failed to initiate authentication for connection %s",
        connection.getName()), e);
      connectionHandler.connectionFailed(RpcConnectionHandler.FailureType.AUTHENTICATION, ex);
    }
  }

  @Override protected List<String> validateHandshake(BitToUserHandshake inbound) throws RpcException {
    //    logger.debug("Handling handshake from bit to user. {}", inbound);
    List<String> serverAuthMechanisms = null;

    if (inbound.hasServerInfos()) {
      serverInfos = inbound.getServerInfos();
    }
    supportedMethods = Sets.immutableEnumSet(inbound.getSupportedMethodsList());

    switch (inbound.getStatus()) {
      case SUCCESS:
        break;
      case AUTH_REQUIRED:
        serverAuthMechanisms = ImmutableList.copyOf(inbound.getAuthenticationMechanismsList());
        setAuthComplete(false);
        connection.setEncryption(inbound.hasEncrypted() && inbound.getEncrypted());

        if (inbound.hasMaxWrappedSize()) {
          connection.setMaxWrappedSize(inbound.getMaxWrappedSize());
        }
        logger.trace(String
            .format("Server requires authentication with encryption context %s before proceeding.",
                connection.getEncryptionCtxtString()));
        break;
      case AUTH_FAILED:
      case RPC_VERSION_MISMATCH:
      case UNKNOWN_FAILURE:
        final String errMsg = String
            .format("Status: %s, Error Id: %s, Error message: %s", inbound.getStatus(),
                inbound.getErrorId(), inbound.getErrorMessage());
        logger.error(errMsg);
        throw new NonTransientRpcException(errMsg);
    }

    // Before starting SASL handshake validate if both client and server are compatible in their security
    // requirements for the connection
    validateSaslCompatibility(properties, serverAuthMechanisms);
    return serverAuthMechanisms;
  }

  @Override protected UserToBitConnection initRemoteConnection(SocketChannel channel) {
    super.initRemoteConnection(channel);
    return new UserToBitConnection(channel);
  }

  public class UserToBitConnection extends AbstractClientConnection {

    UserToBitConnection(SocketChannel channel) {

      // by default connection is not set for encryption. After receiving handshake msg from server we set the
      // isEncryptionEnabled, useChunkMode and chunkModeSize correctly.
      super(channel, "user client");
    }

    @Override public BufferAllocator getAllocator() {
      return allocator;
    }

    @Override protected Logger getLogger() {
      return logger;
    }

    @Override public void incConnectionCounter() {
      // no-op
    }

    @Override public void decConnectionCounter() {
      // no-op
    }
  }

  @Override public ProtobufLengthDecoder getDecoder(BufferAllocator allocator) {
    return new UserProtobufLengthDecoder(allocator, OutOfMemoryHandler.DEFAULT_INSTANCE);
  }

  /**
   * planQuery is an API to plan a query without query execution
   *
   * @param req - data necessary to plan query
   * @return list of PlanFragments that can later on be submitted for execution
   */
  public DrillRpcFuture<QueryPlanFragments> planQuery(GetQueryPlanFragments req) {
    return send(RpcType.GET_QUERY_PLAN_FRAGMENTS, req, QueryPlanFragments.class);
  }
}
