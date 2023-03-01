/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.network.server;

import java.io.IOException;
import java.net.SocketAddress;

import com.google.common.base.Throwables;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.protocol.*;

/**
 * A handler that processes requests from clients and writes chunk data back. Each handler is
 * attached to a single Netty channel, and keeps track of which streams have been fetched via this
 * channel, in order to clean them up if the channel is terminated (see #channelUnregistered).
 *
 * <p>The messages should have been processed by the pipeline setup by {@link TransportServer}.
 */
public class TransportRequestHandler extends MessageHandler<RequestMessage> {

  private static final Logger logger = LoggerFactory.getLogger(TransportRequestHandler.class);

  /** The Netty channel that this handler is associated with. */
  private final Channel channel;

  /** Client on the same channel allowing us to talk back to the requester. */
  private final TransportClient reverseClient;

  /** Handles all RPC messages. */
  private final BaseMessageHandler msgHandler;

  public TransportRequestHandler(
      Channel channel, TransportClient reverseClient, BaseMessageHandler msgHandler) {
    this.channel = channel;
    this.reverseClient = reverseClient;
    this.msgHandler = msgHandler;
  }

  @Override
  public void exceptionCaught(Throwable cause) {
    msgHandler.exceptionCaught(cause, reverseClient);
  }

  @Override
  public void channelActive() {
    msgHandler.channelActive(reverseClient);
  }

  @Override
  public void channelInactive() {
    msgHandler.channelInactive(reverseClient);
  }

  @Override
  public void handle(RequestMessage request) {
    if (checkRegistered(request)) {
      msgHandler.receive(reverseClient, request);
    }
  }

  private boolean checkRegistered(RequestMessage req) {
    if (!msgHandler.checkRegistered()) {
      IOException e = new IOException("Worker Not Registered!");
      if (req instanceof RpcRequest) {
        respond(new RpcFailure(((RpcRequest) req).requestId, Throwables.getStackTraceAsString(e)));
      } else if (req instanceof ChunkFetchRequest) {
        respond(
            new ChunkFetchFailure(
                ((ChunkFetchRequest) req).streamChunkSlice, Throwables.getStackTraceAsString(e)));
      } else if (req instanceof OneWayMessage) {
        logger.warn("Ignore OneWayMessage since worker is not registered!");
      }
      return false;
    }
    return true;
  }

  /**
   * Responds to a single message with some Encodable object. If a failure occurs while sending, it
   * will be logged and the channel closed.
   */
  private ChannelFuture respond(Encodable result) {
    SocketAddress remoteAddress = channel.remoteAddress();
    return channel
        .writeAndFlush(result)
        .addListener(
            future -> {
              if (future.isSuccess()) {
                logger.trace("Sent result {} to client {}", result, remoteAddress);
              } else {
                logger.warn(
                    String.format(
                        "Fail to sending result %s to %s; closing connection",
                        result, remoteAddress),
                    future.cause());
              }
            });
  }
}
