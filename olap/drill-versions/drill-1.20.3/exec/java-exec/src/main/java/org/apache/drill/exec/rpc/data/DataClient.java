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
package org.apache.drill.exec.rpc.data;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.BitData.BitClientHandshake;
import org.apache.drill.exec.proto.BitData.BitServerHandshake;
import org.apache.drill.exec.proto.BitData.RpcType;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.BasicClient;
import org.apache.drill.exec.rpc.BitRpcUtility;
import org.apache.drill.exec.rpc.OutOfMemoryHandler;
import org.apache.drill.exec.rpc.ProtobufLengthDecoder;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.RpcConnectionHandler;
import org.apache.drill.exec.rpc.RpcException;

import java.util.List;

public class DataClient extends BasicClient<RpcType, DataClientConnection, BitClientHandshake, BitServerHandshake> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataClient.class);

  private final DrillbitEndpoint remoteEndpoint;
  private volatile DataClientConnection connection;
  private final DataConnectionManager.CloseHandlerCreator closeHandlerFactory;
  private final DataConnectionConfig config;

  public DataClient(DrillbitEndpoint remoteEndpoint, DataConnectionConfig config,
                    DataConnectionManager.CloseHandlerCreator closeHandlerFactory) {
    super(
        DataRpcConfig.getMapping(config.getBootstrapContext().getConfig(),
            config.getBootstrapContext().getExecutor()),
        config.getAllocator().getAsByteBufAllocator(),
        config.getBootstrapContext().getBitClientLoopGroup(),
        RpcType.HANDSHAKE,
        BitServerHandshake.class,
        BitServerHandshake.PARSER);

    this.remoteEndpoint = remoteEndpoint;
    this.config = config;
    this.closeHandlerFactory = closeHandlerFactory;
  }

  @Override
  protected DataClientConnection initRemoteConnection(SocketChannel channel) {
    super.initRemoteConnection(channel);
    connection = new DataClientConnection(channel, this, config.getEncryptionCtxt());

    // Increase the connection count here since at this point it means that we already have the TCP connection.
    // Later when connection fails for any reason then we will decrease the counter based on Netty's connection close
    // handler.
    connection.incConnectionCounter();
    return connection;
  }

  @Override
  protected GenericFutureListener<ChannelFuture>
  getCloseHandler(SocketChannel ch, DataClientConnection clientConnection) {
    return closeHandlerFactory.getHandler(clientConnection, super.getCloseHandler(ch, clientConnection));
  }

  @Override
  public MessageLite getResponseDefaultInstance(int rpcType) throws RpcException {
    return DataDefaultInstanceHandler.getResponseDefaultInstanceClient(rpcType);
  }

  @Override
  protected void handle(DataClientConnection connection, int rpcType, ByteBuf pBody, ByteBuf dBody,
                        ResponseSender sender) throws RpcException {
    throw new UnsupportedOperationException("DataClient is unidirectional by design.");
  }

  BufferAllocator getAllocator() {
    return config.getAllocator();
  }

  @Override
  protected void prepareSaslHandshake(final RpcConnectionHandler<DataClientConnection> connectionHandler, List<String> serverAuthMechanisms) {
    BitRpcUtility.prepareSaslHandshake(connectionHandler, serverAuthMechanisms, connection, config, remoteEndpoint,
      this, RpcType.SASL_MESSAGE);
  }

    @Override
    protected List<String> validateHandshake(BitServerHandshake handshake) throws RpcException {
      return BitRpcUtility.validateHandshake(handshake.getRpcVersion(), handshake.getAuthenticationMechanismsList(),
        DataRpcConfig.RPC_VERSION, connection, config, this);
    }

  @Override
  public ProtobufLengthDecoder getDecoder(BufferAllocator allocator) {
    return new DataProtobufLengthDecoder.Client(allocator, OutOfMemoryHandler.DEFAULT_INSTANCE);
  }
}
