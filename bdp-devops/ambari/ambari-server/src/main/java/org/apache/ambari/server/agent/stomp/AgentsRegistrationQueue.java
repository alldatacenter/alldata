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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;

/**
 * Simultaneous processing a lot of registering/topology/metadata etc. requests from agents during
 * agent registration can cause response timeout on agents' side. So it is allowed to process simultaneously requests
 * only from limited number of agents with session ids from {@link registrationQueue}. Queue has limited capacity,
 * session id can able be appeared in queue with agent connecting to server and releases with first heartbeat or disconnect from
 * server.
 */
public class AgentsRegistrationQueue {
  private static final Logger LOG = LoggerFactory.getLogger(AgentsRegistrationQueue.class);
  private final BlockingQueue<String> registrationQueue;
  private final ThreadFactory threadFactoryExecutor = new ThreadFactoryBuilder().setNameFormat("agents-queue-%d").build();
  private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, threadFactoryExecutor);

  public AgentsRegistrationQueue(Injector injector) {
    Configuration configuration = injector.getInstance(Configuration.class);
    registrationQueue = new ArrayBlockingQueue<>(configuration.getAgentsRegistrationQueueSize());
  }

  public boolean offer(String sessionId) {
    boolean offered = registrationQueue.offer(sessionId);
    scheduledExecutorService.schedule(new CompleteJob(sessionId, registrationQueue), 60, TimeUnit.SECONDS);
    return offered;
  }

  public void complete(String sessionId) {
    registrationQueue.remove(sessionId);
  }

  private class CompleteJob implements Runnable {
    private String sessionId;
    private BlockingQueue<String> registrationQueue;

    public CompleteJob(String sessionId, BlockingQueue<String> registrationQueue) {
      this.sessionId = sessionId;
      this.registrationQueue = registrationQueue;
    }

    @Override
    public void run() {
      registrationQueue.remove(sessionId);
    }
  }
}
