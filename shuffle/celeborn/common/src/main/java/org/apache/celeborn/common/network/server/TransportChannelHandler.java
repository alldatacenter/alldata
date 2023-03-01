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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportResponseHandler;
import org.apache.celeborn.common.network.protocol.RequestMessage;
import org.apache.celeborn.common.network.protocol.ResponseMessage;
import org.apache.celeborn.common.network.util.NettyUtils;

/**
 * The single Transport-level Channel handler which is used for delegating requests to the {@link
 * TransportRequestHandler} and responses to the {@link TransportResponseHandler}.
 *
 * <p>All channels created in the transport layer are bidirectional. When the Client initiates a
 * Netty Channel with a RequestMessage (which gets handled by the Server's RequestHandler), the
 * Server will produce a ResponseMessage (handled by the Client's ResponseHandler). However, the
 * Server also gets a handle on the same Channel, so it may then begin to send RequestMessages to
 * the Client. This means that the Client also needs a RequestHandler and the Server needs a
 * ResponseHandler, for the Client's responses to the Server's requests.
 *
 * <p>This class also handles timeouts from a {@link io.netty.handler.timeout.IdleStateHandler}. We
 * consider a connection timed out if there are outstanding fetch or RPC requests but no traffic on
 * the channel for at least `requestTimeoutMs`. Note that this is duplex traffic; we will not
 * timeout if the client is continuously sending but getting no responses, for simplicity.
 */
public class TransportChannelHandler extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(TransportChannelHandler.class);

  private final TransportClient client;
  private final TransportResponseHandler responseHandler;
  private final TransportRequestHandler requestHandler;
  private final long requestTimeoutNs;
  private final boolean closeIdleConnections;

  public TransportChannelHandler(
      TransportClient client,
      TransportResponseHandler responseHandler,
      TransportRequestHandler requestHandler,
      long requestTimeoutMs,
      boolean closeIdleConnections) {
    this.client = client;
    this.responseHandler = responseHandler;
    this.requestHandler = requestHandler;
    this.requestTimeoutNs = requestTimeoutMs * 1000L * 1000;
    this.closeIdleConnections = closeIdleConnections;
  }

  public TransportClient getClient() {
    return client;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn(
        "Exception in connection from " + NettyUtils.getRemoteAddress(ctx.channel()), cause);
    requestHandler.exceptionCaught(cause);
    responseHandler.exceptionCaught(cause);
    ctx.close();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    try {
      requestHandler.channelActive();
    } catch (RuntimeException e) {
      logger.error("Exception from request handler while channel is active", e);
    }
    try {
      responseHandler.channelActive();
    } catch (RuntimeException e) {
      logger.error("Exception from response handler while channel is active", e);
    }
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    try {
      requestHandler.channelInactive();
    } catch (RuntimeException e) {
      logger.error("Exception from request handler while channel is inactive", e);
    }
    try {
      responseHandler.channelInactive();
    } catch (RuntimeException e) {
      logger.error("Exception from response handler while channel is inactive", e);
    }
    super.channelInactive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object request) throws Exception {
    if (request instanceof RequestMessage) {
      requestHandler.handle((RequestMessage) request);
    } else if (request instanceof ResponseMessage) {
      responseHandler.handle((ResponseMessage) request);
    } else {
      ctx.fireChannelRead(request);
    }
  }

  /** Triggered based on events from an {@link io.netty.handler.timeout.IdleStateHandler}. */
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      synchronized (this) {
        boolean isActuallyOverdue =
            System.nanoTime() - responseHandler.getTimeOfLastRequestNs() > requestTimeoutNs;
        if (e.state() == IdleState.ALL_IDLE && isActuallyOverdue) {
          if (responseHandler.numOutstandingRequests() > 0) {
            String address = NettyUtils.getRemoteAddress(ctx.channel());
            logger.error(
                "Connection to {} has been quiet for {} ms while there are outstanding "
                    + "requests.",
                address,
                requestTimeoutNs / 1000 / 1000);
          }
          if (closeIdleConnections) {
            // While CloseIdleConnections is enable, we also close idle connection
            client.timeOut();
            ctx.close();
          }
        }
      }
    }
    ctx.fireUserEventTriggered(evt);
  }

  public TransportResponseHandler getResponseHandler() {
    return responseHandler;
  }
}
