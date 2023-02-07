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
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.BitData.BitClientHandshake;
import org.apache.drill.exec.proto.BitData.BitServerHandshake;
import org.apache.drill.exec.proto.BitData.RpcType;
import org.apache.drill.exec.proto.UserBitShared.RpcChannel;
import org.apache.drill.exec.rpc.BasicServer;
import org.apache.drill.exec.rpc.OutOfMemoryHandler;
import org.apache.drill.exec.rpc.ProtobufLengthDecoder;
import org.apache.drill.exec.rpc.RpcException;

public class DataServer extends BasicServer<RpcType, DataServerConnection> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataServer.class);

  private final DataConnectionConfig config;

  public DataServer(DataConnectionConfig config) {
    super(
        DataRpcConfig.getMapping(config.getBootstrapContext().getConfig(),
            config.getBootstrapContext().getExecutor()),
        config.getAllocator().getAsByteBufAllocator(),
        config.getBootstrapContext().getBitLoopGroup());
    this.config = config;
  }

  @Override
  public MessageLite getResponseDefaultInstance(int rpcType) throws RpcException {
    return DataDefaultInstanceHandler.getResponseDefaultInstanceServer(rpcType);
  }

  @Override
  protected GenericFutureListener<ChannelFuture> getCloseHandler(SocketChannel ch, DataServerConnection connection) {
    return new ProxyCloseHandler(super.getCloseHandler(ch, connection));
  }

  @Override
  protected DataServerConnection initRemoteConnection(SocketChannel channel) {
    super.initRemoteConnection(channel);
    final DataServerConnection dataServerConnection = new DataServerConnection(channel, config);

    // Increase the connection count here since at this point it means that we already have the TCP connection.
    // Later when connection fails for any reason then we will decrease the counter based on Netty's connection close
    // handler.
    dataServerConnection.incConnectionCounter();
    return dataServerConnection;
  }

  @Override
  protected ServerHandshakeHandler<BitClientHandshake> getHandshakeHandler(final DataServerConnection connection) {
    return new ServerHandshakeHandler<BitClientHandshake>(RpcType.HANDSHAKE, BitClientHandshake.PARSER) {

      @Override
      public MessageLite getHandshakeResponse(BitClientHandshake inbound) throws Exception {
        // logger.debug("Handling handshake from other bit. {}", inbound);
        if (inbound.getRpcVersion() != DataRpcConfig.RPC_VERSION) {
          throw new RpcException(String.format("Invalid rpc version.  Expected %d, actual %d.",
              inbound.getRpcVersion(), DataRpcConfig.RPC_VERSION));
        }
        if (inbound.getChannel() != RpcChannel.BIT_DATA) {
          throw new RpcException(String.format("Invalid NodeMode.  Expected BIT_DATA but received %s.",
              inbound.getChannel()));
        }

        final BitServerHandshake.Builder builder = BitServerHandshake.newBuilder();
        builder.setRpcVersion(DataRpcConfig.RPC_VERSION);
        if (config.getAuthMechanismToUse() != null) {
          builder.addAllAuthenticationMechanisms(config.getAuthProvider().getAllFactoryNames());
        }

        return builder.build();
      }

    };
  }

  private class ProxyCloseHandler implements GenericFutureListener<ChannelFuture> {

    private volatile GenericFutureListener<ChannelFuture> handler;

    public ProxyCloseHandler(GenericFutureListener<ChannelFuture> handler) {
      super();
      this.handler = handler;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      handler.operationComplete(future);
    }

  }

  @Override
  protected OutOfMemoryHandler getOutOfMemoryHandler() {
    return new OutOfMemoryHandler() {
      @Override
      public void handle() {
        logger.error("Out of memory in RPC layer.");
      }
    };
  }

  @Override
  protected ProtobufLengthDecoder getDecoder(BufferAllocator allocator, OutOfMemoryHandler outOfMemoryHandler) {
    return new DataProtobufLengthDecoder.Server(allocator, outOfMemoryHandler);
  }

}
