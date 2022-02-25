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
package org.apache.ambari.server.agent.stomp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.HeartBeat;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.HeartBeatResponse;
import org.apache.ambari.server.agent.Register;
import org.apache.ambari.server.agent.RegistrationResponse;
import org.apache.ambari.server.agent.RegistrationStatus;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.spring.GuiceBeansConfig;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

@Controller
@SendToUser("/")
@MessageMapping("/")
@Import(GuiceBeansConfig.class)
public class HeartbeatController {
  private static Logger LOG = LoggerFactory.getLogger(HeartbeatController.class);
  private final HeartBeatHandler hh;
  private final ClustersImpl clusters;
  private final AgentSessionManager agentSessionManager;
  private final LinkedBlockingQueue queue;
  private final ThreadFactory threadFactoryExecutor = new ThreadFactoryBuilder().setNameFormat("agent-register-processor-%d").build();
  private final ThreadFactory threadFactoryTimeout = new ThreadFactoryBuilder().setNameFormat("agent-register-timeout-%d").build();
  private final ExecutorService executor;
  private final ScheduledExecutorService scheduledExecutorService;
  private final UnitOfWork unitOfWork;

  @Autowired
  private AgentsRegistrationQueue agentsRegistrationQueue;

  public HeartbeatController(Injector injector) {
    hh = injector.getInstance(HeartBeatHandler.class);
    clusters = injector.getInstance(ClustersImpl.class);
    unitOfWork = injector.getInstance(UnitOfWork.class);
    agentSessionManager = injector.getInstance(AgentSessionManager.class);

    Configuration configuration = injector.getInstance(Configuration.class);
    queue = new LinkedBlockingQueue(configuration.getAgentsRegistrationQueueSize());
    executor = new ThreadPoolExecutor(configuration.getRegistrationThreadPoolSize(),
        configuration.getRegistrationThreadPoolSize(), 0L, TimeUnit.MILLISECONDS, queue, threadFactoryExecutor);
    scheduledExecutorService = Executors.newScheduledThreadPool(1, threadFactoryTimeout);
  }

  @MessageMapping("/register")
  public CompletableFuture<RegistrationResponse> register(@Header String simpSessionId, Register message)
      throws WebApplicationException, InvalidStateTransitionException, AmbariException {
    CompletableFuture<RegistrationResponse> completableFuture = new CompletableFuture<>();

    Future<RegistrationResponse> future = executor.submit(() -> {
      try {
        unitOfWork.begin();
        RegistrationResponse response = null;
        try {
          /* Call into the heartbeat handler */
          response = hh.handleRegistration(message);
          agentSessionManager.register(simpSessionId,
              clusters.getHost(message.getHostname()));
          LOG.debug("Sending registration response " + response);
        } catch (Exception ex) {
          LOG.info(ex.getMessage(), ex);
          response = new RegistrationResponse();
          response.setResponseId(-1);
          response.setResponseStatus(RegistrationStatus.FAILED);
          response.setExitstatus(1);
          response.setLog(ex.getMessage());
          completableFuture.complete(response);
          return response;
        }
        completableFuture.complete(response);
        return response;
      } finally {
        unitOfWork.end();
      }
    });

    scheduledExecutorService.schedule(new RegistrationTimeoutTask(future, completableFuture), 8, TimeUnit.SECONDS);
    return completableFuture;
  }

  @MessageMapping("/heartbeat")
  public HeartBeatResponse heartbeat(@Header String simpSessionId, HeartBeat message) {
    try {
      unitOfWork.begin();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received Heartbeat message " + message);
      }
      HeartBeatResponse heartBeatResponse;
      try {
        if (!agentSessionManager.isRegistered(simpSessionId)) {
          //Server restarted, or unknown host.
          LOG.error(String.format("Host with [%s] sessionId not registered", simpSessionId));
          return hh.createRegisterCommand();
        }
        message.setHostname(agentSessionManager.getHost(simpSessionId).getHostName());
        heartBeatResponse = hh.handleHeartBeat(message);
        agentsRegistrationQueue.complete(simpSessionId);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Sending heartbeat response with response id " + heartBeatResponse.getResponseId());
          LOG.debug("Response details " + heartBeatResponse);
        }
      } catch (Exception e) {
        LOG.warn("Error in HeartBeat", e);
        throw new WebApplicationException(500);
      }
      return heartBeatResponse;
    } finally {
      unitOfWork.end();
    }
  }

  private class RegistrationTimeoutTask implements Runnable {
    private Future<RegistrationResponse> task;
    private CompletableFuture<RegistrationResponse> completableFuture;

    public RegistrationTimeoutTask(Future<RegistrationResponse> task, CompletableFuture<RegistrationResponse> completableFuture) {

      this.task = task;
      this.completableFuture = completableFuture;
    }

    @Override
    public void run() {
      boolean cancelled = task.cancel(false);
      if (cancelled) {
        completableFuture.cancel(false);
      }
    }
  }
}
