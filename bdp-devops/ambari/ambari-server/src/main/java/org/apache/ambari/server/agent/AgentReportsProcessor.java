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
package org.apache.ambari.server.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.UnitOfWork;

@Singleton
public class AgentReportsProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(AgentReportsProcessor.class);

  private final int poolSize;

  private final List<ExecutorService> executors;

  public void addAgentReport(AgentReport agentReport) {
    int hash = agentReport.getHostName().hashCode();
    hash = hash == Integer.MIN_VALUE ? 0 : hash;
    int executorNumber = Math.abs(hash) % poolSize;
    executors.get(executorNumber).execute(new AgentReportProcessingTask(agentReport));
  }

  @Inject
  private UnitOfWork unitOfWork;

  @Inject
  public AgentReportsProcessor(Configuration configuration) {

    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("agent-report-processor-%d").build();
    poolSize = configuration.getAgentsReportThreadPoolSize();
    executors = new ArrayList<>();
    for (int i = 0; i < poolSize; i++) {
      executors.add(Executors.newSingleThreadExecutor(threadFactory));
    }
  }

  private class AgentReportProcessingTask implements Runnable {

    private final AgentReport agentReport;

    public AgentReportProcessingTask(AgentReport agentReport) {
      this.agentReport = agentReport;
    }

    @Override
    public void run() {
      try {
        unitOfWork.begin();
        try {
          agentReport.process();
        } catch (AmbariException e) {
          LOG.error("Error processing agent reports", e);
        }
      } finally {
        unitOfWork.end();
      }
    }
  }
}
