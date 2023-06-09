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

import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.materialize.QueryDataPackage;
import org.apache.drill.exec.physical.impl.materialize.QueryWritableBatch;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.GeneralRPCProtos.RpcMode;
import org.apache.drill.exec.proto.UserBitShared.QueryResult;
import org.apache.drill.exec.proto.UserBitShared.UserCredentials;
import org.apache.drill.exec.proto.UserProtos.BitToUserHandshake;
import org.apache.drill.exec.proto.UserProtos.HandshakeStatus;
import org.apache.drill.exec.proto.UserProtos.Property;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.SaslSupport;
import org.apache.drill.exec.proto.UserProtos.UserProperties;
import org.apache.drill.exec.proto.UserProtos.UserToBitHandshake;
import org.apache.drill.exec.rpc.AbstractServerConnection;
import org.apache.drill.exec.rpc.BasicServer;
import org.apache.drill.exec.rpc.OutOfMemoryHandler;
import org.apache.drill.exec.rpc.OutboundRpcMessage;
import org.apache.drill.exec.rpc.ProtobufLengthDecoder;
import org.apache.drill.exec.rpc.RpcConstants;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.rpc.security.ServerAuthenticationHandler;
import org.apache.drill.exec.rpc.security.plain.PlainFactory;
import org.apache.drill.exec.rpc.user.UserServer.BitToUserConnection;
import org.apache.drill.exec.rpc.user.security.UserAuthenticationException;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.ssl.SSLConfig;
import org.apache.drill.exec.ssl.SSLConfigBuilder;
import org.apache.drill.exec.work.user.UserWorker;
import org.apache.hadoop.security.HadoopKerberosName;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserServer extends BasicServer<RpcType, BitToUserConnection> {
  private static final Logger logger = LoggerFactory.getLogger(UserServer.class);
  private static final String SERVER_NAME = "Apache Drill Server";

  private final UserConnectionConfig config;
  private final SSLConfig sslConfig;
  private Channel sslChannel;
  private final UserWorker userWorker;
  private static final ConcurrentHashMap<BitToUserConnection, BitToUserConnectionConfig> userConnectionMap;

  // Initialize the singleton map during startup
  static {
    userConnectionMap = new ConcurrentHashMap<>();
  }

  /**
   * Serialize {@link org.apache.drill.exec.proto.UserProtos.BitToUserHandshake} instance without password
   * @param inbound handshake instance for serialization
   * @return String of serialized object
   */
  private String serializeUserToBitHandshakeWithoutPassword(UserToBitHandshake inbound) {
    StringBuilder sb = new StringBuilder();
    sb.append("rpc_version: ");
    sb.append(inbound.getRpcVersion());
    sb.append("\ncredentials:\n\t");
    sb.append(inbound.getCredentials());
    sb.append("properties:");
    List<Property> props = inbound.getProperties().getPropertiesList();
    for (Property p: props) {
      if (!p.getKey().equalsIgnoreCase("password")) {
        sb.append("\n\tproperty:\n\t\t");
        sb.append("key: \"");
        sb.append(p.getKey());
        sb.append("\"\n\t\tvalue: \"");
        sb.append(p.getValue());
        sb.append("\"");
      }
    }
    sb.append("\nsupport_complex_types: ");
    sb.append(inbound.getSupportComplexTypes());
    sb.append("\nsupport_timeout: ");
    sb.append(inbound.getSupportTimeout());
    sb.append("sasl_support: ");
    sb.append(inbound.getSaslSupport());
    sb.append("\nclient_infos:\n\t");
    sb.append(inbound.getClientInfos().toString().replace("\n", "\n\t"));
    return sb.toString();
  }

  public UserServer(BootStrapContext context, BufferAllocator allocator, EventLoopGroup eventLoopGroup,
                    UserWorker worker) throws DrillbitStartupException {
    super(UserRpcConfig.getMapping(context.getConfig(), context.getExecutor()),
        allocator.getAsByteBufAllocator(),
        eventLoopGroup);
    this.config = new UserConnectionConfig(allocator, context, new UserServerRequestHandler(worker));
    this.sslChannel = null;
    try {
      this.sslConfig = new SSLConfigBuilder()
          .config(context.getConfig())
          .mode(SSLConfig.Mode.SERVER)
          .initializeSSLContext(true)
          .validateKeyStore(true)
          .build();
      logger.info("Rpc server configured to use TLS protocol '{}'", sslConfig.getProtocol());
    } catch (DrillException e) {
      throw new DrillbitStartupException(e.getMessage(), e.getCause());
    }
    this.userWorker = worker;

    // Initialize Singleton instance of UserRpcMetrics.
    ((UserRpcMetrics)UserRpcMetrics.getInstance()).initialize(config.isEncryptionEnabled(), allocator);
  }

  @Override
  protected void setupSSL(ChannelPipeline pipe) {

    SSLEngine sslEngine = sslConfig.createSSLEngine(config.getAllocator(), null, 0);
    // Add SSL handler into pipeline
    pipe.addFirst(RpcConstants.SSL_HANDLER, new SslHandler(sslEngine));
    logger.debug("SSL communication between client and server is enabled.");
    logger.debug(sslConfig.toString());

  }

  @Override
  protected boolean isSslEnabled() {
    return sslConfig.isUserSslEnabled();
  }

  @Override
  public void setSslChannel(Channel c) {
    sslChannel = c;
  }

  @Override
  protected void closeSSL(){
    if(isSslEnabled() && sslChannel != null){
      sslChannel.close();
    }
  }

  @Override
  protected MessageLite getResponseDefaultInstance(int rpcType) throws RpcException {
    // a user server only expects acknowledgments on messages it creates.
    switch (rpcType) {
    case RpcType.ACK_VALUE:
      return Ack.getDefaultInstance();
    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Access to set of active connection details for this instance of the Drillbit
   * @return Active connection set
   */
  public static Set<Entry<BitToUserConnection, BitToUserConnectionConfig>> getUserConnections() {
    return userConnectionMap.entrySet();
  }

  /**
   * Represents a client connection accepted by Foreman Drillbit's UserServer
   * from a DrillClient. This connection is used to get hold of
   * {@link UserSession} which stores all session related information like
   * session options changed over the lifetime of this connection. There is a
   * 1:1 mapping between a BitToUserConnection and a UserSession. This
   * connection object is also used to send query data and result back to the
   * client submitted as part of the session tied to this connection.
   */
  public class BitToUserConnection extends AbstractServerConnection<BitToUserConnection>
      implements UserClientConnection {

    private UserSession session;
    private UserToBitHandshake inbound;
    private String authenticatedUser;

    BitToUserConnection(SocketChannel channel) {
      super(channel, config, !config.isAuthEnabled()
          ? config.getMessageHandler()
          : new ServerAuthenticationHandler<>(config.getMessageHandler(),
          RpcType.SASL_MESSAGE_VALUE, RpcType.SASL_MESSAGE));

      // Increase the connection count here since at this point it means that we already have the TCP connection.
      // Later when connection fails for any reason then we will decrease the counter based on Netty's connection close
      // handler.
      incConnectionCounter();
    }

    void disableReadTimeout() {
      getChannel().pipeline().remove(RpcConstants.TIMEOUT_HANDLER);
    }

    void setHandshake(final UserToBitHandshake inbound) {
      this.inbound = inbound;
    }

    @Override
    public void finalizeSaslSession() throws IOException {
      final String authorizationID = getSaslServer().getAuthorizationID();
      final String userName = new HadoopKerberosName(authorizationID).getShortName();
      finalizeSession(userName);
      logger.info("User {} logged in from {}", authenticatedUser, getRemoteAddress());
    }

    /**
     * Sets the user on the session, and finalizes the session.
     *
     * @param userName user name to set on the session
     */
    void finalizeSession(String userName) {
      // create a session
      session = UserSession.Builder.newBuilder()
          .withCredentials(UserCredentials.newBuilder()
              .setUserName(userName)
              .build())
          .withOptionManager(userWorker.getSystemOptions())
          .withUserProperties(inbound.getProperties())
          .setSupportComplexTypes(inbound.getSupportComplexTypes())
          .build();

      this.authenticatedUser = userName;
      // if inbound impersonation is enabled and a target is mentioned
      final String targetName = session.getTargetUserName();
      if (config.getImpersonationManager() != null && targetName != null) {
        config.getImpersonationManager().replaceUserOnSession(targetName, session);
      }
    }

    @Override
    public UserSession getSession() { return session; }

    @Override
    protected Logger getLogger() { return logger; }

    @Override
    public void sendResult(final RpcOutcomeListener<Ack> listener, final QueryResult result) {
      logger.trace("Sending result to client with {}", result);
      send(listener, this, RpcType.QUERY_RESULT, result, Ack.class, true);
    }

    @Override
    public void sendData(final RpcOutcomeListener<Ack> listener, final QueryDataPackage data) {
      QueryWritableBatch result = data.toWritableBatch();
      logger.trace("Sending data to client with {}", result);
      send(listener, this, RpcType.QUERY_DATA, result.getHeader(), Ack.class, false, result.getBuffers());
    }

    @Override
    public Future<Void> getClosureFuture() {
      return getChannel().closeFuture()
          .addListener(future -> cleanup());
    }

    @Override
    public SocketAddress getRemoteAddress() {
      return getChannel().remoteAddress();
    }

    @Override
    public void channelClosed(RpcException ex) {
      // log the logged out event only when authentication is enabled
      if (config.isAuthEnabled()) {
        logger.info("User {} logged out from {}", authenticatedUser, getRemoteAddress());
      }
      super.channelClosed(ex);
    }

    private void cleanup() {
      if (session != null) {
        session.close();
      }
    }

    @Override
    public void close() {
      cleanup();
      super.close();
    }

    @Override
    public void incConnectionCounter() {
      UserRpcMetrics.getInstance().addConnectionCount();
    }

    @Override
    public void decConnectionCounter() {
      UserRpcMetrics.getInstance().decConnectionCount();
      //Removing entry in connection map (sys table)
      userConnectionMap.remove(this);
    }
  }

  @Override
  protected BitToUserConnection initRemoteConnection(SocketChannel channel) {
    super.initRemoteConnection(channel);
    return registerAndGetConnection(channel);
  }

  private BitToUserConnection registerAndGetConnection(SocketChannel channel) {
    BitToUserConnection bit2userConn = new BitToUserConnection(channel);
    if (bit2userConn != null) {
      userConnectionMap.put(bit2userConn, new BitToUserConnectionConfig());
    }
    return bit2userConn;
  }

  @Override
  protected ServerHandshakeHandler<UserToBitHandshake> getHandshakeHandler(final BitToUserConnection connection) {

    return new ServerHandshakeHandler<UserToBitHandshake>(RpcType.HANDSHAKE, UserToBitHandshake.PARSER){

      @Override
      protected void consumeHandshake(ChannelHandlerContext ctx, UserToBitHandshake inbound) throws Exception {
        BitToUserHandshake handshakeResp = getHandshakeResponse(inbound);
        OutboundRpcMessage msg = new OutboundRpcMessage(RpcMode.RESPONSE, this.handshakeType, coordinationId, handshakeResp);
        ctx.writeAndFlush(msg);

        if (handshakeResp.getStatus() != HandshakeStatus.SUCCESS &&
            handshakeResp.getStatus() != HandshakeStatus.AUTH_REQUIRED) {
          // If handling handshake results in an error, throw an exception to terminate the connection.
          throw new RpcException("Handshake request failed: " + handshakeResp.getErrorMessage());
        }
      }

      @Override
      public BitToUserHandshake getHandshakeResponse(UserToBitHandshake inbound) throws Exception {
        if (logger.isTraceEnabled()) {
          logger.trace("Handling handshake from user to bit. {}", serializeUserToBitHandshakeWithoutPassword(inbound));
        }
        // if timeout is unsupported or is set to false, disable timeout.
        if (!inbound.hasSupportTimeout() || !inbound.getSupportTimeout()) {
          connection.disableReadTimeout();
          logger.warn("Timeout Disabled as client {} doesn't support it.", connection.getName());
        }

        BitToUserHandshake.Builder respBuilder = BitToUserHandshake.newBuilder()
            .setRpcVersion(UserRpcConfig.RPC_VERSION)
            .setServerInfos(UserRpcUtils.getRpcEndpointInfos(SERVER_NAME))
            .addAllSupportedMethods(UserRpcConfig.SUPPORTED_SERVER_METHODS);

        try {
          if (inbound.getRpcVersion() != UserRpcConfig.RPC_VERSION) {
            final String errMsg = String.format("Invalid rpc version. Expected %d, actual %d.",
                UserRpcConfig.RPC_VERSION, inbound.getRpcVersion());

            return handleFailure(respBuilder, HandshakeStatus.RPC_VERSION_MISMATCH, errMsg, null);
          }

          connection.setHandshake(inbound);

          if (!config.isAuthEnabled()) {
            connection.finalizeSession(inbound.getCredentials().getUserName());
            respBuilder.setStatus(HandshakeStatus.SUCCESS);
            return respBuilder.build();
          }

          // If sasl_support field is absent in handshake message then treat the client as < 1.10 client
          final boolean clientSupportsSasl = inbound.hasSaslSupport();

          // saslSupportOrdinal will be set to UNKNOWN_SASL_SUPPORT, if sasl_support field in handshake is set to a
          // value which is unknown to this server. We will treat those clients as one which knows SASL protocol.
          final int saslSupportOrdinal = (clientSupportsSasl) ? inbound.getSaslSupport().ordinal()
                                                              : SaslSupport.UNKNOWN_SASL_SUPPORT.ordinal();

          // Check if client doesn't support SASL or only supports SASL_AUTH and server has encryption enabled
          if ((!clientSupportsSasl || saslSupportOrdinal == SaslSupport.SASL_AUTH.ordinal())
              && config.isEncryptionEnabled()) {
            throw new UserAuthenticationException("The server doesn't allow client without encryption support." +
                " Please upgrade your client or talk to your system administrator.");
          }

          if (!clientSupportsSasl) { // for backward compatibility < 1.10
            final String userName = inbound.getCredentials().getUserName();
            if (logger.isTraceEnabled()) {
              logger.trace("User {} on connection {} is likely using an older client.",
                  userName, connection.getRemoteAddress());
            }
            try {
              String password = "";
              final UserProperties props = inbound.getProperties();
              for (int i = 0; i < props.getPropertiesCount(); i++) {
                Property prop = props.getProperties(i);
                if (DrillProperties.PASSWORD.equalsIgnoreCase(prop.getKey())) {
                  password = prop.getValue();
                  break;
                }
              }
              final PlainFactory plainFactory;
              try {
                plainFactory = (PlainFactory) config.getAuthProvider()
                    .getAuthenticatorFactory(PlainFactory.SIMPLE_NAME);
              } catch (final SaslException e) {
                throw new UserAuthenticationException("The server no longer supports username/password" +
                    " based authentication. Please talk to your system administrator.");
              }
              plainFactory.getAuthenticator()
                  .authenticate(userName, password);
              connection.changeHandlerTo(config.getMessageHandler());
              connection.finalizeSession(userName);
              respBuilder.setStatus(HandshakeStatus.SUCCESS);
              logger.info("Authenticated {} from {} successfully using PLAIN", userName,
                connection.getRemoteAddress());
              return respBuilder.build();
            } catch (UserAuthenticationException ex) {
              return handleFailure(respBuilder, HandshakeStatus.AUTH_FAILED, ex.getMessage(), ex);
            }
          }

          // Offer all the configured mechanisms to client. If certain mechanism doesn't support encryption
          // like PLAIN, those should fail during the SASL handshake negotiation.
          respBuilder.addAllAuthenticationMechanisms(config.getAuthProvider().getAllFactoryNames());

          // set the encrypted flag in handshake message. For older clients this field is optional so will be ignored
          respBuilder.setEncrypted(connection.isEncryptionEnabled());
          respBuilder.setMaxWrappedSize(connection.getMaxWrappedSize());

          // for now, this means PLAIN credentials will be sent over twice
          // (during handshake and during sasl exchange)
          respBuilder.setStatus(HandshakeStatus.AUTH_REQUIRED);
          return respBuilder.build();
        } catch (Exception e) {
          return handleFailure(respBuilder, HandshakeStatus.UNKNOWN_FAILURE, e.getMessage(), e);
        }
      }
    };
  }

  /**
   * Complete building the given builder for <i>BitToUserHandshake</i> message with given status and error details.
   *
   * @param respBuilder Instance of {@link org.apache.drill.exec.proto.UserProtos.BitToUserHandshake} builder which
   *                    has RPC version field already set.
   * @param status  Status of handling handshake request.
   * @param errMsg  Error message.
   * @param exception Optional exception.
   * @return
   */
  private static BitToUserHandshake handleFailure(BitToUserHandshake.Builder respBuilder, HandshakeStatus status,
      String errMsg, Exception exception) {
    final String errorId = UUID.randomUUID().toString();

    if (exception != null) {
      logger.error("Error {} in Handling handshake request: {}, {}", errorId, status, errMsg, exception);
    } else {
      logger.error("Error {} in Handling handshake request: {}, {}", errorId, status, errMsg);
    }

    return respBuilder
        .setStatus(status)
        .setErrorId(errorId)
        .setErrorMessage(errMsg)
        .build();
  }

  @Override
  protected ProtobufLengthDecoder getDecoder(BufferAllocator allocator, OutOfMemoryHandler outOfMemoryHandler) {
    return new UserProtobufLengthDecoder(allocator, outOfMemoryHandler);
  }

  /**
   * User Connection's config for System Table access
   */
  public class BitToUserConnectionConfig {
    private final DateTime established;
    private final boolean isAuthEnabled;
    private final boolean isEncryptionEnabled;
    private final boolean isSSLEnabled;

    public BitToUserConnectionConfig() {
      established = new DateTime(); //Current Joda-based Time
      isAuthEnabled = config.isAuthEnabled();
      isEncryptionEnabled = config.isEncryptionEnabled();
      isSSLEnabled = config.isSSLEnabled();
    }

    public boolean isAuthEnabled() {
      return isAuthEnabled;
    }

    public boolean isEncryptionEnabled() {
      return isEncryptionEnabled;
    }

    public boolean isSSLEnabled() {
      return isSSLEnabled;
    }

    public DateTime getEstablished() {
      return established;
    }
  }
}
