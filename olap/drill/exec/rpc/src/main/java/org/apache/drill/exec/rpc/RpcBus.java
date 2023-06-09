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

import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.GeneralRPCProtos.RpcMode;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Rpc Bus deals with incoming and outgoing communication and is used on both the server and the client side of a
 * system.
 *
 * @param <T> RPC type
 * @param <C> Remote connection type
 */
public abstract class RpcBus<T extends EnumLite, C extends RemoteConnection> implements Closeable {
  final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

  private static final OutboundRpcMessage PONG = new OutboundRpcMessage(RpcMode.PONG, 0, 0, Acks.OK);

  protected abstract MessageLite getResponseDefaultInstance(int rpcType) throws RpcException;

  protected abstract void handle(C connection, int rpcType, ByteBuf pBody, ByteBuf dBody, ResponseSender sender)
      throws RpcException;

  protected final RpcConfig rpcConfig;

  protected volatile SocketAddress local;
  protected volatile SocketAddress remote;


  public RpcBus(RpcConfig rpcConfig) {
    this.rpcConfig = rpcConfig;
  }

  protected void setAddresses(SocketAddress remote, SocketAddress local){
    this.remote = remote;
    this.local = local;
  }

  public <SEND extends MessageLite, RECEIVE extends MessageLite>
  DrillRpcFuture<RECEIVE> send(C connection, T rpcType, SEND protobufBody, Class<RECEIVE> clazz,
                               ByteBuf... dataBodies) {
    DrillRpcFutureImpl<RECEIVE> rpcFuture = new DrillRpcFutureImpl<RECEIVE>();
    this.send(rpcFuture, connection, rpcType, protobufBody, clazz, dataBodies);
    return rpcFuture;
  }

  public <SEND extends MessageLite, RECEIVE extends MessageLite>
  void send(RpcOutcomeListener<RECEIVE> listener, C connection, T rpcType, SEND protobufBody, Class<RECEIVE> clazz,
            ByteBuf... dataBodies) {
    send(listener, connection, rpcType, protobufBody, clazz, false, dataBodies);
  }

  public <SEND extends MessageLite, RECEIVE extends MessageLite>
  void send(RpcOutcomeListener<RECEIVE> listener, C connection, T rpcType, SEND protobufBody, Class<RECEIVE> clazz,
            boolean allowInEventLoop, ByteBuf... dataBodies) {

    Preconditions
        .checkArgument(
            allowInEventLoop || !connection.inEventLoop(),
            "You attempted to send while inside the rpc event thread.  This isn't allowed because sending will block if the channel is backed up.");

    ByteBuf pBuffer = null;
    boolean completed = false;

    try {

      if (!allowInEventLoop && !connection.blockOnNotWritable(listener)) {
        // if we're in not in the event loop and we're interrupted while blocking, skip sending this message.
        return;
      }

      assert !Arrays.asList(dataBodies).contains(null);
      assert rpcConfig.checkSend(rpcType, protobufBody.getClass(), clazz);

      Preconditions.checkNotNull(protobufBody);
      ChannelListenerWithCoordinationId futureListener = connection.createNewRpcListener(listener, clazz);
      OutboundRpcMessage m = new OutboundRpcMessage(RpcMode.REQUEST, rpcType, futureListener.getCoordinationId(), protobufBody, dataBodies);
      ChannelFuture channelFuture = connection.getChannel().writeAndFlush(m);
      channelFuture.addListener(futureListener);
      channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
      completed = true;
    } catch (Exception | AssertionError e) {
      listener.failed(new RpcException("Failure sending message.", e));
    } finally {

      if (!completed) {
        if (pBuffer != null) {
          pBuffer.release();
        }
        if (dataBodies != null) {
          for (ByteBuf b : dataBodies) {
            b.release();
          }

        }
      }
    }
  }

  protected abstract C initRemoteConnection(SocketChannel channel);

  public class ChannelClosedHandler implements GenericFutureListener<ChannelFuture> {

    final C clientConnection;
    private final Channel channel;

    public ChannelClosedHandler(C clientConnection, Channel channel) {
      this.channel = channel;
      this.clientConnection = clientConnection;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      final String msg;

      if(local!=null) {
        msg = String.format("Channel closed %s <--> %s.", local, remote);
      }else{
        msg = String.format("Channel closed %s <--> %s.", future.channel().localAddress(), future.channel().remoteAddress());
      }

      final ChannelClosedException ex = future.cause() != null ?
          new ChannelClosedException(msg, future.cause()) :
          new ChannelClosedException(msg);
      clientConnection.channelClosed(ex);
    }

  }

  protected GenericFutureListener<ChannelFuture> getCloseHandler(SocketChannel channel, C clientConnection) {
    return new ChannelClosedHandler(clientConnection, channel);
  }


  private class ResponseSenderImpl implements ResponseSender {

    private final RemoteConnection connection;
    private final int coordinationId;
    private final AtomicBoolean sent = new AtomicBoolean(false);

    public ResponseSenderImpl(RemoteConnection connection, int coordinationId) {
      this.connection = connection;
      this.coordinationId = coordinationId;
    }

    public void send(Response r) {
      assert rpcConfig.checkResponseSend(r.rpcType, r.pBody.getClass());
      sendOnce();
      OutboundRpcMessage outMessage = new OutboundRpcMessage(RpcMode.RESPONSE, r.rpcType, coordinationId,
          r.pBody, r.dBodies);
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Adding message to outbound buffer. {}", outMessage);
        logger.debug("Sending response with Sender {}", System.identityHashCode(this));
      }
      connection.getChannel().writeAndFlush(outMessage);
    }

    /**
     * Ensures that each sender is only used once.
     */
    private void sendOnce() {
      if (!sent.compareAndSet(false, true)) {
        throw new IllegalStateException("Attempted to utilize a sender multiple times.");
      }
    }

    void sendFailure(UserRpcException e){
      sendOnce();
      UserException uex = UserException.systemError(e)
          .addIdentity(e.getEndpoint())
          .build(logger);

      logger.error("Unexpected Error while handling request message", e);

      OutboundRpcMessage outMessage = new OutboundRpcMessage(
          RpcMode.RESPONSE_FAILURE,
          0,
          coordinationId,
          uex.getOrCreatePBError(false)
          );

      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Adding message to outbound buffer. {}", outMessage);
      }
      connection.getChannel().writeAndFlush(outMessage);
    }

  }

  private static void retainByteBuf(ByteBuf buf) {
    if (buf != null) {
      buf.retain();
    }
  }

  private static void releaseByteBuf(ByteBuf buf) {
    if (buf != null) {
      buf.release();
    }
  }

  protected class InboundHandler extends MessageToMessageDecoder<InboundRpcMessage> {

    private final C connection;

    public InboundHandler(C connection) {
      super();
      Preconditions.checkNotNull(connection);
      this.connection = connection;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final InboundRpcMessage msg, final List<Object> output)
            throws Exception {
      if (!ctx.channel().isOpen()) {
        return;
      }
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Received message {}", msg);
      }
      final Channel channel = connection.getChannel();
      final Stopwatch watch = Stopwatch.createStarted();

      try {

        switch (msg.mode) {
        case REQUEST: {
          final ResponseSenderImpl sender = new ResponseSenderImpl(connection, msg.coordinationId);
          retainByteBuf(msg.pBody);
          retainByteBuf(msg.dBody);
          try {
            handle(connection, msg.rpcType, msg.pBody, msg.dBody, sender);
          } catch (UserRpcException e) {
            sender.sendFailure(e);
          } finally {
            releaseByteBuf(msg.pBody);
            releaseByteBuf(msg.dBody);
          }
          break;
        }

        case RESPONSE: {
          retainByteBuf(msg.pBody);
          retainByteBuf(msg.dBody);
          try {
            final MessageLite defaultResponse = getResponseDefaultInstance(msg.rpcType);
            assert rpcConfig.checkReceive(msg.rpcType, defaultResponse.getClass());
            final RpcOutcome<?> rpcFuture = connection.getAndRemoveRpcOutcome(msg.rpcType, msg.coordinationId,
                defaultResponse.getClass());
            final Parser<?> parser = defaultResponse.getParserForType();
            final Object value = parser.parseFrom(new ByteBufInputStream(msg.pBody, msg.pBody.readableBytes()));
            rpcFuture.set(value, msg.dBody);
            if (RpcConstants.EXTRA_DEBUGGING) {
              logger.debug("Updated rpc future {} with value {}", rpcFuture, value);
            }
          } catch (Exception ex) {
            logger.error("Failure while handling response.", ex);
            throw ex;
          } finally {
            releaseByteBuf(msg.pBody);
            releaseByteBuf(msg.dBody);
          }
          break;
        }

        case RESPONSE_FAILURE: {
          DrillPBError failure = DrillPBError.parseFrom(new ByteBufInputStream(msg.pBody, msg.pBody.readableBytes()));
          connection.recordRemoteFailure(msg.coordinationId, failure);
          if (RpcConstants.EXTRA_DEBUGGING) {
            logger.debug("Updated rpc future with coordinationId {} with failure {}", msg.coordinationId, failure);
          }
          break;
        }

        case PING:
          channel.writeAndFlush(PONG);
          break;

        case PONG:
          ctx.fireUserEventTriggered(RpcMode.PONG);
          break;

        default:
          throw new UnsupportedOperationException();
        }
      } finally {
        long time = watch.elapsed(TimeUnit.MILLISECONDS);
        long delayThreshold = Integer.parseInt(System.getProperty("drill.exec.rpcDelayWarning", "500"));
        if (time > delayThreshold) {
          logger.warn(String.format(
              "Message of mode %s of rpc type %d took longer than %dms.  Actual duration was %dms.",
              msg.mode, msg.rpcType, delayThreshold, time));
        }
        msg.release();
      }
    }
  }

  public static <T> T get(ByteBuf pBody, Parser<T> parser) throws RpcException {
    try {
      ByteBufInputStream is = new ByteBufInputStream(pBody);
      return parser.parseFrom(is);
    } catch (InvalidProtocolBufferException e) {
      throw new RpcException(
          String.format("Failure while decoding message with parser of type. %s",
              parser.getClass().getCanonicalName()), e);
    }
  }
}
