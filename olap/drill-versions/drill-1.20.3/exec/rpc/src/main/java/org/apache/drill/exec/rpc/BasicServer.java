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
package org.apache.drill.exec.rpc;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.io.IOException;
import java.net.BindException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.GeneralRPCProtos.RpcMode;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

/**
 * A server is bound to a port and is responsible for responding to various type of requests. In some cases,
 * the inbound requests will generate more than one outbound request.
 *
 * @param <T> RPC type
 * @param <SC> server connection type
 */
public abstract class BasicServer<T extends EnumLite, SC extends ServerConnection<SC>> extends RpcBus<T, SC> {
  final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

  private final ServerBootstrap b;
  private volatile boolean connect = false;
  private final EventLoopGroup eventLoopGroup;

  public BasicServer(final RpcConfig rpcMapping, ByteBufAllocator alloc, EventLoopGroup eventLoopGroup) {
    super(rpcMapping);
    this.eventLoopGroup = eventLoopGroup;

    b = new ServerBootstrap()
        .channel(TransportCheck.getServerSocketChannel())
        .option(ChannelOption.SO_BACKLOG, 1000)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30*1000)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_RCVBUF, 1 << 17)
        .option(ChannelOption.SO_SNDBUF, 1 << 17)
        .group(eventLoopGroup) //
        .childOption(ChannelOption.ALLOCATOR, alloc)

        // .handler(new LoggingHandler(LogLevel.INFO))

        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
//            logger.debug("Starting initialization of server connection.");
            SC connection = initRemoteConnection(ch);
            ch.closeFuture().addListener(getCloseHandler(ch, connection));

            final ChannelPipeline pipe = ch.pipeline();
            // Make sure that the SSL handler is the first handler in the pipeline so everything is encrypted
            if (isSslEnabled()) {
              setupSSL(pipe);
            }

            pipe.addLast(RpcConstants.PROTOCOL_DECODER, getDecoder(connection.getAllocator(), getOutOfMemoryHandler()));
            pipe.addLast(RpcConstants.MESSAGE_DECODER, new RpcDecoder("s-" + rpcConfig.getName()));
            pipe.addLast(RpcConstants.PROTOCOL_ENCODER, new RpcEncoder("s-" + rpcConfig.getName()));
            pipe.addLast(RpcConstants.HANDSHAKE_HANDLER, getHandshakeHandler(connection));

            if (rpcMapping.hasTimeout()) {
              pipe.addLast(RpcConstants.TIMEOUT_HANDLER,
                  new LoggingReadTimeoutHandler(connection, rpcMapping.getTimeout()));
            }

            pipe.addLast(RpcConstants.MESSAGE_HANDLER, new InboundHandler(connection));
            pipe.addLast(RpcConstants.EXCEPTION_HANDLER, new RpcExceptionHandler<>(connection));

            connect = true;
//            logger.debug("Server connection initialization completed.");
          }
        });

//     if(TransportCheck.SUPPORTS_EPOLL){
//       b.option(EpollChannelOption.SO_REUSEPORT, true); //
//     }
  }

  // Adds a SSL handler if enabled. Required only for client and server communications, so
  // a real implementation is only available for UserServer
  protected void setupSSL(ChannelPipeline pipe) {
    throw new UnsupportedOperationException("SSL is implemented only by the User Server.");
  }

  protected boolean isSslEnabled() {
    return false;
  }

  // Save the SslChannel after the SSL handshake so it can be closed later
  public void setSslChannel(Channel c) {
    return;
  }

  protected void closeSSL() {
    return;
  }

  private class LoggingReadTimeoutHandler extends ReadTimeoutHandler {

    private final SC connection;
    private final int timeoutSeconds;
    public LoggingReadTimeoutHandler(SC connection, int timeoutSeconds) {
      super(timeoutSeconds);
      this.connection = connection;
      this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
      logger.info("RPC connection {} timed out.  Timeout was set to {} seconds. Closing connection.",
          connection.getName(), timeoutSeconds);
      super.readTimedOut(ctx);
    }

  }

  protected OutOfMemoryHandler getOutOfMemoryHandler() {
    return OutOfMemoryHandler.DEFAULT_INSTANCE;
  }

  protected abstract ProtobufLengthDecoder getDecoder(BufferAllocator allocator, OutOfMemoryHandler outOfMemoryHandler);

  protected abstract ServerHandshakeHandler<?> getHandshakeHandler(SC connection);

  protected static abstract class ServerHandshakeHandler<T extends MessageLite> extends AbstractHandshakeHandler<T> {

    public ServerHandshakeHandler(EnumLite handshakeType, Parser<T> parser) {
      super(handshakeType, parser);
    }

    @Override
    protected void consumeHandshake(ChannelHandlerContext ctx, T inbound) throws Exception {
      OutboundRpcMessage msg = new OutboundRpcMessage(RpcMode.RESPONSE, this.handshakeType, coordinationId,
          getHandshakeResponse(inbound));
      ctx.writeAndFlush(msg);
    }

    public abstract MessageLite getHandshakeResponse(T inbound) throws Exception;

  }

  protected abstract MessageLite getResponseDefaultInstance(int rpcType) throws RpcException;

  @Override
  protected void handle(SC connection, int rpcType, ByteBuf pBody, ByteBuf dBody,
                        ResponseSender sender) throws RpcException {
    connection.getCurrentHandler().handle(connection, rpcType, pBody, dBody, sender);
  }

  @Override
  protected SC initRemoteConnection(SocketChannel channel) {
    local = channel.localAddress();
    remote = channel.remoteAddress();
    return null;
  }

  public int bind(final int initialPort, boolean allowPortHunting) {
    int port = initialPort - 1;
    while (true) {
      try {
        b.bind(++port).sync();
        break;
      } catch (Exception e) {
        // TODO(DRILL-3026):  Revisit:  Exception is not (always) BindException.
        // One case is "java.io.IOException: bind() failed: Address already in
        // use".
        if (e instanceof BindException && allowPortHunting) {
          continue;
        }

        final UserException bindException =
            UserException
              .resourceError( e )
              .addContext( "Server type", getClass().getSimpleName() )
              .message( "Drillbit could not bind to port %s.", port )
              .build(logger);
        throw bindException;
      }
    }

    connect = !connect;
    logger.debug("Server of type {} started on port {}.", getClass().getSimpleName(), port);
    return port;
  }

  @Override
  public void close() throws IOException {
    try {
      Stopwatch watch = Stopwatch.createStarted();
      // this takes 1s to complete
      // known issue: https://github.com/netty/netty/issues/2545
      eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS).get();
      long elapsed = watch.elapsed(MILLISECONDS);
      if (elapsed > 500) {
        logger.info("closed eventLoopGroup " + eventLoopGroup + " in " + elapsed + " ms");
      }
      if(isSslEnabled()) {
        closeSSL();
      }
    } catch (final InterruptedException | ExecutionException e) {
      logger.warn("Failure while shutting down {}. ", this.getClass().getName(), e);

      // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
      // interruption and respond to it if it wants to.
      Thread.currentThread().interrupt();
    }
  }

}
