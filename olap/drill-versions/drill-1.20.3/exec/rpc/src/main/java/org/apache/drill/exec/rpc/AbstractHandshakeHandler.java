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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

public abstract class AbstractHandshakeHandler<T extends MessageLite> extends MessageToMessageDecoder<InboundRpcMessage> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractHandshakeHandler.class);

  protected final EnumLite handshakeType;
  protected final Parser<T> parser;
  protected volatile int coordinationId;

  public AbstractHandshakeHandler(EnumLite handshakeType, Parser<T> parser) {
    super();
    this.handshakeType = handshakeType;
    this.parser = parser;
  }


  @Override
  protected void decode(ChannelHandlerContext ctx, InboundRpcMessage inbound, List<Object> outputs) throws Exception {
    try {
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Received handshake {}", inbound);
      }
      this.coordinationId = inbound.coordinationId;
      ctx.channel().pipeline().remove(this);
      if (inbound.rpcType != handshakeType.getNumber()) {
        throw new RpcException(String.format("Handshake failure.  Expected %s[%d] but received number [%d]",
            handshakeType, handshakeType.getNumber(), inbound.rpcType));
      }

      T msg = parser.parseFrom(inbound.getProtobufBodyAsIS());
      consumeHandshake(ctx, msg);
    } finally {
      // Consuming a handshake may result in exceptions, so make sure to release the message buffers.
      inbound.pBody.release();
      if (inbound.dBody != null) {
        inbound.dBody.release();
      }
    }
  }

  protected abstract void consumeHandshake(ChannelHandlerContext ctx, T msg) throws Exception;

}
