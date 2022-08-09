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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.stomp.AmbariSubscriptionRegistry;
import org.apache.ambari.server.api.AmbariSendToMethodReturnValueHandler;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.support.SendToMethodReturnValueHandler;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.google.inject.Injector;

@Configuration
public class RootStompConfig {

  @Autowired
  private SimpMessagingTemplate brokerTemplate;

  private final ServletContext servletContext;

  private final org.apache.ambari.server.configuration.Configuration configuration;

  public RootStompConfig(ServletContext servletContext, Injector injector) {
    this.servletContext = servletContext;
    configuration = injector.getInstance(org.apache.ambari.server.configuration.Configuration.class);
  }

  @Bean
  public DefaultMessageEmitter defaultMessageEmitter(Injector injector) {
    org.apache.ambari.server.configuration.Configuration configuration =
        injector.getInstance(org.apache.ambari.server.configuration.Configuration.class);
    return new DefaultMessageEmitter(injector.getInstance(AgentSessionManager.class),
        brokerTemplate,
        injector.getInstance(AmbariEventPublisher.class),
        configuration.getExecutionCommandsRetryCount(),
        configuration.getExecutionCommandsRetryInterval());
  }

  @Bean
  public DefaultHandshakeHandler handshakeHandler() {

    return new DefaultHandshakeHandler(
        new JettyRequestUpgradeStrategy(new WebSocketServerFactory(servletContext)));
  }

  @Autowired
  public void configureRegistryCacheSize(SimpleBrokerMessageHandler simpleBrokerMessageHandler) throws NoSuchFieldException, IllegalAccessException {
    AmbariSubscriptionRegistry defaultSubscriptionRegistry =
        new AmbariSubscriptionRegistry(configuration.getSubscriptionRegistryCacheSize());
    simpleBrokerMessageHandler.setSubscriptionRegistry(defaultSubscriptionRegistry);
  }

  @Autowired
  public void configureGlobal(SimpAnnotationMethodMessageHandler messageHandler) {
    List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(messageHandler.getReturnValueHandlers());
    List<HandlerMethodReturnValueHandler> changedHandlers = new ArrayList<>();

    boolean handlerReplaced = false;
    for (HandlerMethodReturnValueHandler handler : handlers) {
      if (handler instanceof SendToMethodReturnValueHandler && !handlerReplaced) {
        SendToMethodReturnValueHandler sendHandler = (SendToMethodReturnValueHandler)handler;
        AmbariSendToMethodReturnValueHandler ambariSendToMethodReturnValueHandler =
          new AmbariSendToMethodReturnValueHandler(brokerTemplate, true);
        ambariSendToMethodReturnValueHandler.setHeaderInitializer(sendHandler.getHeaderInitializer());
        changedHandlers.add(ambariSendToMethodReturnValueHandler);
        handlerReplaced = true;
      } else {
        changedHandlers.add(handler);
      }
    }
    messageHandler.setReturnValueHandlers(null);
    messageHandler.setReturnValueHandlers(changedHandlers);
  }

  @ControllerAdvice
  public static class ExceptionHandlingAdvice{
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlingAdvice.class);

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/")
    public ErrorMessage handle(Exception e) {

      //LOG.error("Exception caught while processing message: " + e.getMessage(), e);
      return new ErrorMessage(e);
    }


  }

}
