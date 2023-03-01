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

package org.apache.celeborn.common.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.network.client.TransportResponseHandler;
import org.apache.celeborn.common.network.protocol.MessageEncoder;
import org.apache.celeborn.common.network.server.*;
import org.apache.celeborn.common.network.util.FrameDecoder;
import org.apache.celeborn.common.network.util.TransportConf;
import org.apache.celeborn.common.network.util.TransportFrameDecoder;

/**
 * Contains the context to create a {@link TransportServer}, {@link TransportClientFactory}, and to
 * setup Netty Channel pipelines with a {@link TransportChannelHandler}.
 *
 * <p>There are two communication protocols that the TransportClient provides, control-plane RPCs
 * and data-plane "chunk fetching". The handling of the RPCs is performed outside of the scope of
 * the TransportContext (i.e., by a user-provided handler), and it is responsible for setting up
 * streams which can be streamed through the data plane in chunks using zero-copy IO.
 *
 * <p>The TransportServer and TransportClientFactory both create a TransportChannelHandler for each
 * channel. As each TransportChannelHandler contains a TransportClient, this enables server
 * processes to send messages back to the client on an existing channel.
 */
public class TransportContext {
  private static final Logger logger = LoggerFactory.getLogger(TransportContext.class);

  private final TransportConf conf;
  private final BaseMessageHandler msgHandler;
  private ChannelsLimiter channelsLimiter;
  private final boolean closeIdleConnections;

  private static final MessageEncoder ENCODER = MessageEncoder.INSTANCE;

  public TransportContext(
      TransportConf conf,
      BaseMessageHandler msgHandler,
      boolean closeIdleConnections,
      ChannelsLimiter channelsLimiter) {
    this.conf = conf;
    this.msgHandler = msgHandler;
    this.closeIdleConnections = closeIdleConnections;
    this.channelsLimiter = channelsLimiter;
  }

  public TransportContext(
      TransportConf conf, BaseMessageHandler msgHandler, boolean closeIdleConnections) {
    this(conf, msgHandler, closeIdleConnections, null);
  }

  public TransportContext(TransportConf conf, BaseMessageHandler msgHandler) {
    this(conf, msgHandler, false);
  }

  public TransportClientFactory createClientFactory() {
    return new TransportClientFactory(this);
  }

  /** Create a server which will attempt to bind to a specific host and port. */
  public TransportServer createServer(String host, int port) {
    return new TransportServer(this, host, port);
  }

  public TransportServer createServer(int port) {
    return createServer(null, port);
  }

  /** For Suite only */
  public TransportServer createServer() {
    return createServer(null, 0);
  }

  public TransportChannelHandler initializePipeline(SocketChannel channel) {
    return initializePipeline(channel, new TransportFrameDecoder());
  }

  public TransportChannelHandler initializePipeline(
      SocketChannel channel, ChannelInboundHandlerAdapter decoder) {
    try {
      if (channelsLimiter != null) {
        channel.pipeline().addLast("limiter", channelsLimiter);
      }
      TransportChannelHandler channelHandler = createChannelHandler(channel, msgHandler);
      channel
          .pipeline()
          .addLast("encoder", ENCODER)
          .addLast(FrameDecoder.HANDLER_NAME, decoder)
          .addLast(
              "idleStateHandler", new IdleStateHandler(0, 0, conf.connectionTimeoutMs() / 1000))
          .addLast("handler", channelHandler);
      return channelHandler;
    } catch (RuntimeException e) {
      logger.error("Error while initializing Netty pipeline", e);
      throw e;
    }
  }

  private TransportChannelHandler createChannelHandler(
      Channel channel, BaseMessageHandler msgHandler) {
    TransportResponseHandler responseHandler = new TransportResponseHandler(conf, channel);
    TransportClient client = new TransportClient(channel, responseHandler);
    TransportRequestHandler requestHandler =
        new TransportRequestHandler(channel, client, msgHandler);
    return new TransportChannelHandler(
        client, responseHandler, requestHandler, conf.connectionTimeoutMs(), closeIdleConnections);
  }

  public TransportConf getConf() {
    return conf;
  }

  public BaseMessageHandler getMsgHandler() {
    return msgHandler;
  }
}
