/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.configuration.spring;

import org.apache.ambari.server.agent.stomp.AgentsRegistrationQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageBuilder;

public class AgentRegisteringQueueChecker extends ChannelInterceptorAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(AgentsRegistrationQueue.class);

  @Autowired
  private AgentsRegistrationQueue agentsRegistrationQueue;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor headerAccessor= StompHeaderAccessor.wrap(message);
    String sessionId = headerAccessor.getHeader("simpSessionId").toString();
    if (SimpMessageType.CONNECT_ACK.equals(headerAccessor.getMessageType())
        && !agentsRegistrationQueue.offer(sessionId)) {
      StompHeaderAccessor headerAccessorError = StompHeaderAccessor.create(StompCommand.ERROR);
      headerAccessorError.setHeader("simpSessionId", sessionId);
      headerAccessorError.setHeader("simpConnectMessage", headerAccessor.getHeader("simpConnectMessage").toString());
      headerAccessorError.setMessage("Connection not allowed");

      return MessageBuilder.createMessage(new byte[0], headerAccessorError.getMessageHeaders());
    } else if (SimpMessageType.DISCONNECT_ACK.equals(headerAccessor.getMessageType())) {
      agentsRegistrationQueue.complete(sessionId);
    }
    return message;
  }
}
