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

import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.UserRpcException;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

/**
 * Service that allows one Drillbit to communicate with another. Internally manages whether each particular bit is a
 * server or a client depending on who initially made the connection. If no connection exists, the Controller is responsible for
 * making a connection. TODO: Controller should automatically straight route local BitCommunication rather than connecting to its
 * self.
 */
public interface Controller extends AutoCloseable {

  /**
   * Get a Bit to Bit communication tunnel. If the BitCom doesn't have a tunnel attached to the node already, it will
   * start creating one. This create the connection asynchronously.
   *
   * @param node
   * @return
   */
  public ControlTunnel getTunnel(DrillbitEndpoint node);

  public DrillbitEndpoint start(DrillbitEndpoint partialEndpoint, boolean allowPortHunting)
      throws DrillbitStartupException;

  /**
   * Register a new handler for custom message types. Should be done before any messages. This is threadsafe as this
   * method manages locking internally.
   * @param messageTypeId
   *          The type of message id to handle. This corresponds to the CustomMessage.type field. Note that only a
   *          single handler for a particular type of message can be registered within a particular Drillbit.
   * @param handler
   *          The handler that should be used to handle this type of message.
   * @param parser
   *          The parser used to handle the types of messages the handler above handles.
   */
  public <REQUEST extends MessageLite, RESPONSE extends MessageLite> void registerCustomHandler(int messageTypeId,
      CustomMessageHandler<REQUEST, RESPONSE> handler, Parser<REQUEST> parser);

  /**
   * Register a new handler for custom message types. Should be done before any messages. This is threadsafe as this
   * method manages locking internally.
   * @param messageTypeId
   *          The type of message id to handle. This corresponds to the CustomMessage.type field. Note that only a
   *          single handler for a particular type of message can be registered within a particular Drillbit.
   * @param handler
   *          The handler that should be used to handle this type of message.
   * @param requestSerde
   *          CustomSerDe for incoming requests.
   * @param responseSerde
   *          CustomSerDe for serializing responses.
   */
  public <REQUEST, RESPONSE> void registerCustomHandler(int messageTypeId,
      CustomMessageHandler<REQUEST, RESPONSE> handler,
      CustomSerDe<REQUEST> requestSerde,
      CustomSerDe<RESPONSE> responseSerde);

  /**
   * Defines how the Controller should handle custom messages. Implementations need to be threadsafe.
   *
   * @param <REQUEST>
   *          The type of request message.
   * @param <RESPONSE>
   *          The type of the response message.
   */
  public interface CustomMessageHandler<REQUEST, RESPONSE> {

    /**
     * Handle an incoming message.
     * @param pBody
     *          The protobuf body message object of type REQUEST that was sent.
     * @param dBody
     *          An optional byte body that was sent along with the structured message.
     * @return The response that should be sent to the message sender.
     * @throws UserRpcException
     *           throw this exception if there is an RPC failure that should be communicated to the sender.
     */
    public CustomResponse<RESPONSE> onMessage(REQUEST pBody, DrillBuf dBody) throws UserRpcException;

  }



  /**
   * A simple interface that describes the nature of the response to the custom incoming message.
   *
   * @param <RESPONSE>
   *          The type of message that the respopnse contains. Must be a protobuf message type.
   */
  public interface CustomResponse<RESPONSE> {

    /**
     * The structured portion of the response.
     * @return A protobuf message of type RESPONSE
     */
    public RESPONSE getMessage();

    /**
     * The optional unstructured portion of the message.
     * @return null or one or more unstructured bodies.
     */
    public ByteBuf[] getBodies();
  }

  /**
   * Interface for defining how to serialize and deserialize custom message for consumer who want to use something other
   * than Protobuf messages.
   */
  public interface CustomSerDe<MSG> {
    public byte[] serializeToSend(MSG send);

    public MSG deserializeReceived(byte[] bytes) throws Exception;
  }
}
