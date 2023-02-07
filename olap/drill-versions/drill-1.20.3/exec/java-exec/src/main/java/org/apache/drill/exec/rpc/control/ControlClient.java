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
package org.apache.drill.exec.rpc.control;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.BitControl.BitControlHandshake;
import org.apache.drill.exec.proto.BitControl.RpcType;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.BasicClient;
import org.apache.drill.exec.rpc.BitRpcUtility;
import org.apache.drill.exec.rpc.FailingRequestHandler;
import org.apache.drill.exec.rpc.OutOfMemoryHandler;
import org.apache.drill.exec.rpc.ProtobufLengthDecoder;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.RpcConnectionHandler;
import org.apache.drill.exec.rpc.RpcException;

import java.util.List;

public class ControlClient extends BasicClient<RpcType, ControlConnection, BitControlHandshake, BitControlHandshake> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControlClient.class);

  private final DrillbitEndpoint remoteEndpoint;
  private volatile ControlConnection connection;
  private final ControlConnectionManager.CloseHandlerCreator closeHandlerFactory;
  private final ControlConnectionConfig config;

  public ControlClient(ControlConnectionConfig config, DrillbitEndpoint remoteEndpoint,
                       ControlConnectionManager.CloseHandlerCreator closeHandlerFactory) {
    super(ControlRpcConfig.getMapping(config.getBootstrapContext().getConfig(),
        config.getBootstrapContext().getExecutor()),
        config.getAllocator().getAsByteBufAllocator(),
        config.getBootstrapContext().getBitLoopGroup(),
        RpcType.HANDSHAKE,
        BitControlHandshake.class,
        BitControlHandshake.PARSER);
    this.config = config;
    this.remoteEndpoint = remoteEndpoint;
    this.closeHandlerFactory = closeHandlerFactory;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected ControlConnection initRemoteConnection(SocketChannel channel) {
    super.initRemoteConnection(channel);
    connection = new ControlConnection(channel, "control client", config,
        config.getAuthMechanismToUse() == null
            ? config.getMessageHandler()
            : new FailingRequestHandler<ControlConnection>(),
        this);

    // Increase the connection count here since at this point it means that we already have the TCP connection.
    // Later when connection fails for any reason then we will decrease the counter based on Netty's connection close
    // handler.
    connection.incConnectionCounter();
    return connection;
  }

  @Override
  protected GenericFutureListener<ChannelFuture> getCloseHandler(SocketChannel ch, ControlConnection clientConnection) {
    return closeHandlerFactory.getHandler(clientConnection, super.getCloseHandler(ch, clientConnection));
  }

  @Override
  public MessageLite getResponseDefaultInstance(int rpcType) throws RpcException {
    return DefaultInstanceHandler.getResponseDefaultInstance(rpcType);
  }

  @Override
  protected void handle(ControlConnection connection, int rpcType, ByteBuf pBody, ByteBuf dBody,
                        ResponseSender sender) throws RpcException {
    connection.getCurrentHandler().handle(connection, rpcType, pBody, dBody, sender);
  }

  @Override
  protected void prepareSaslHandshake(final RpcConnectionHandler<ControlConnection> connectionHandler,
                                      List<String> serverAuthMechanisms) {
    BitRpcUtility.prepareSaslHandshake(connectionHandler, serverAuthMechanisms, connection, config, remoteEndpoint,
      this, RpcType.SASL_MESSAGE);
  }

  @Override
  protected List<String> validateHandshake(BitControlHandshake handshake) throws RpcException {
    return BitRpcUtility.validateHandshake(handshake.getRpcVersion(), handshake.getAuthenticationMechanismsList(),
      ControlRpcConfig.RPC_VERSION, connection, config, this);
  }

  @Override
  protected void finalizeConnection(BitControlHandshake handshake, ControlConnection connection) {
    connection.setEndpoint(handshake.getEndpoint());
  }

  @Override
  public ProtobufLengthDecoder getDecoder(BufferAllocator allocator) {
    return new ControlProtobufLengthDecoder(allocator, OutOfMemoryHandler.DEFAULT_INSTANCE);
  }

}
