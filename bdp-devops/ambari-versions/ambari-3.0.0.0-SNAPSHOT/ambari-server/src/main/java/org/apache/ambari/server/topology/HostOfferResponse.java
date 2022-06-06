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

package org.apache.ambari.server.topology;

import java.util.List;
import java.util.concurrent.Executor;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.topology.tasks.TopologyHostTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Response to a host offer.
 */
final class HostOfferResponse {

  public enum Answer {ACCEPTED, DECLINED_PREDICATE, DECLINED_DONE}

  private static final Logger LOG = LoggerFactory.getLogger(HostOfferResponse.class);
  static final HostOfferResponse DECLINED_DUE_TO_PREDICATE = new HostOfferResponse(Answer.DECLINED_PREDICATE);
  static final HostOfferResponse DECLINED_DUE_TO_DONE = new HostOfferResponse(Answer.DECLINED_DONE);

  private final Answer answer;
  private final String hostGroupName;
  private final long hostRequestId;
  private final List<TopologyHostTask> tasks;

  static HostOfferResponse createAcceptedResponse(long hostRequestId, String hostGroupName, List<TopologyHostTask> tasks) {
    return new HostOfferResponse(Answer.ACCEPTED, hostRequestId, hostGroupName, tasks);
  }

  private HostOfferResponse(Answer answer) {
    this(answer, -1, null, null);
  }

  private HostOfferResponse(Answer answer, long hostRequestId, String hostGroupName, List<TopologyHostTask> tasks) {
    this.answer = answer;
    this.hostRequestId = hostRequestId;
    this.hostGroupName = hostGroupName;
    this.tasks = tasks;
  }

  public Answer getAnswer() {
    return answer;
  }

  public long getHostRequestId() {
    return hostRequestId;
  }

  //todo: for now assumes a host was added
  //todo: perhaps a topology modification object that modifies a passed in topology structure?
  public String getHostGroupName() {
    return hostGroupName;
  }

  void executeTasks(Executor executor, final String hostName, final ClusterTopology topology, final AmbariContext ambariContext) {
    if (answer != Answer.ACCEPTED) {
      LOG.warn("Attempted to execute tasks for declined host offer", answer);
    } else {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          for (TopologyHostTask task : tasks) {
            try {
              LOG.info("Running task for accepted host offer for hostname = {}, task = {}", hostName, task.getType());
              task.run();
            } catch (Exception e) {
              HostRequest hostRequest = task.getHostRequest();
              LOG.error("{} task for host {} failed due to", task.getType(), hostRequest.getHostName(), e);
              hostRequest.markHostRequestFailed(HostRoleStatus.ABORTED, e, ambariContext.getPersistedTopologyState());
              break;
            }
          }
        }
      });
    }
  }

}
