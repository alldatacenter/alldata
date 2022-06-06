/*
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

import org.apache.ambari.server.api.stomp.NamedTasksSubscriptions;
import org.apache.ambari.server.api.stomp.TestController;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.events.listeners.requests.STOMPUpdateListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import com.google.inject.Injector;

@Configuration
@EnableWebSocketMessageBroker
@ComponentScan(basePackageClasses = {TestController.class})
@Import(RootStompConfig.class)
public class ApiStompConfig extends AbstractWebSocketMessageBrokerConfigurer {
  private final String HEARTBEAT_THREAD_NAME = "ws-heartbeat-thread-";
  private final int HEARTBEAT_POOL_SIZE = 1;
  private final org.apache.ambari.server.configuration.Configuration configuration;

  public ApiStompConfig(Injector injector) {
    configuration = injector.getInstance(org.apache.ambari.server.configuration.Configuration.class);
  }

  @Bean
  public STOMPUpdateListener requestSTOMPListener(Injector injector) {
    return new STOMPUpdateListener(injector, DefaultMessageEmitter.DEFAULT_API_EVENT_TYPES);
  }

  @Bean
  public NamedTasksSubscriptions namedTasksSubscribtions(Injector injector) {
    return injector.getInstance(NamedTasksSubscriptions.class);
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/v1")
      .setAllowedOrigins("*")
      .withSockJS().setHeartbeatTime(configuration.getAPIHeartbeatInterval());
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(HEARTBEAT_POOL_SIZE);
    taskScheduler.setThreadNamePrefix(HEARTBEAT_THREAD_NAME);
    taskScheduler.initialize();

    registry.setPreservePublishOrder(true).enableSimpleBroker("/").setTaskScheduler(taskScheduler)
        .setHeartbeatValue(new long[]{configuration.getAPIHeartbeatInterval(), configuration.getAPIHeartbeatInterval()});
  }
}
