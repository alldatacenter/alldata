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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.drill.common.AutoCloseables.Closeable;
import org.apache.drill.common.concurrent.AutoCloseableLock;
import org.apache.drill.exec.proto.BitControl.CustomMessage;
import org.apache.drill.exec.proto.BitControl.RpcType;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.UserRpcException;
import org.apache.drill.exec.rpc.control.Controller.CustomMessageHandler;
import org.apache.drill.exec.rpc.control.Controller.CustomResponse;

import com.carrotsearch.hppc.IntObjectHashMap;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

public class CustomHandlerRegistry {
  // private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CustomHandlerRegistry.class);

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final AutoCloseableLock read = new AutoCloseableLock(readWriteLock.readLock());
  private final AutoCloseableLock write = new AutoCloseableLock(readWriteLock.writeLock());
  private final IntObjectHashMap<ParsingHandler<?, ?>> handlers = new IntObjectHashMap<>();
  private volatile DrillbitEndpoint endpoint;

  public CustomHandlerRegistry() {
  }

  public void setEndpoint(DrillbitEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  public <REQUEST, RESPONSE> void registerCustomHandler(int messageTypeId,
      CustomMessageHandler<REQUEST, RESPONSE> handler,
      Controller.CustomSerDe<REQUEST> requestSerde,
      Controller.CustomSerDe<RESPONSE> responseSerde) {
    Preconditions.checkNotNull(handler);
    Preconditions.checkNotNull(requestSerde);
    Preconditions.checkNotNull(responseSerde);
    try (@SuppressWarnings("unused") Closeable lock = write.open()) {
      ParsingHandler<?, ?> parsingHandler = handlers.get(messageTypeId);
      if (parsingHandler != null) {
        throw new IllegalStateException(String.format(
            "Only one handler can be registered for a given custom message type. You tried to register a handler for "
                + "the %d message type but one had already been registered.",
            messageTypeId));
      }

      parsingHandler = new ParsingHandler<REQUEST, RESPONSE>(handler, requestSerde, responseSerde);
      handlers.put(messageTypeId, parsingHandler);
    }
  }

  public Response handle(CustomMessage message, DrillBuf dBody) throws RpcException {
    final ParsingHandler<?, ?> handler;
    try (@SuppressWarnings("unused") Closeable lock = read.open()) {
      handler = handlers.get(message.getType());
    }

    if (handler == null) {
      throw new UserRpcException(
          endpoint, "Unable to handle message.",
          new IllegalStateException(String.format(
              "Unable to handle message. The message type provided [%d] did not have a registered handler.",
              message.getType())));
    }
    final CustomResponse<?> customResponse = handler.onMessage(message.getMessage(), dBody);
    @SuppressWarnings("unchecked")
    final CustomMessage responseMessage = CustomMessage.newBuilder()
        .setMessage(
            ByteString.copyFrom(((Controller.CustomSerDe<Object>) handler.getResponseSerDe())
                .serializeToSend(customResponse
                .getMessage())))
        .setType(message.getType())
        .build();
    // make sure we don't pass in a null array.
    final ByteBuf[] dBodies = customResponse.getBodies() == null ? new DrillBuf[0] : customResponse.getBodies();
    return new Response(RpcType.RESP_CUSTOM, responseMessage, dBodies);

  }

  private class ParsingHandler<REQUEST, RESPONSE> {
    private final CustomMessageHandler<REQUEST, ?> handler;
    private final Controller.CustomSerDe<REQUEST> requestSerde;
    private final Controller.CustomSerDe<RESPONSE> responseSerde;

    public ParsingHandler(
        CustomMessageHandler<REQUEST, RESPONSE> handler,
        Controller.CustomSerDe<REQUEST> requestSerde,
        Controller.CustomSerDe<RESPONSE> responseSerde) {
      super();
      this.handler = handler;
      this.requestSerde = requestSerde;
      this.responseSerde = responseSerde;
    }

    public Controller.CustomSerDe<RESPONSE> getResponseSerDe() {
      return responseSerde;
    }

    public CustomResponse<?> onMessage(ByteString pBody, DrillBuf dBody) throws UserRpcException {

      try {
        final REQUEST message = requestSerde.deserializeReceived(pBody.toByteArray());
        return handler.onMessage(message, dBody);

      } catch (Exception e) {
        throw new UserRpcException(endpoint, "Failure parsing message.", e);
      }

    }
  }
}
