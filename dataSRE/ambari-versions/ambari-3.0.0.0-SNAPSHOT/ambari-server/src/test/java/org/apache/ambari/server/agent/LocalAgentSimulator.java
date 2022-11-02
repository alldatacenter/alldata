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
package org.apache.ambari.server.agent;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to simulate the agent.
 */
public class LocalAgentSimulator implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(HeartBeatHandler.class);

  private Thread agentThread = null;
  private volatile boolean shouldRun = true;
  private final HeartBeatHandler handler;
  private long sleepTime = 500;
  private long responseId = 1;

  private String hostname = "localhost";
  private String agentVersion = "1.3.0";

  public LocalAgentSimulator(HeartBeatHandler hbh) {
    this.handler = hbh;
  }

  public LocalAgentSimulator(HeartBeatHandler hbh, String hostname, long sleepTime) {
    this(hbh);
    this.sleepTime = sleepTime;
    this.hostname  = hostname;
  }

  //Can be used to control exact number of heartbeats,
  //Default is -1 which means keep heartbeating continuously
  private volatile int numberOfHeartbeats = -1;
  private int currentHeartbeatCount = 0;
  private volatile boolean shouldSendRegistration = true;

  private volatile Register nextRegistration = null;
  private volatile HeartBeat nextHeartbeat = null;
  private volatile RegistrationResponse lastRegistrationResponse = null;
  private volatile HeartBeatResponse lastHeartBeatResponse = null;

  public void start() {
    agentThread = new Thread(this);
    agentThread.start();
  }

  public void shutdown() {
    shouldRun = false;
    agentThread.interrupt();
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        if (shouldSendRegistration) {
          sendRegistration();
        } else if (numberOfHeartbeats > 0
            && (currentHeartbeatCount < numberOfHeartbeats)) {
          sendHeartBeat();
        }
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
      } catch (Exception ex) {
        LOG.info("Exception received ", ex);
        throw new RuntimeException(ex);
      }
    }
  }

  private void sendRegistration() {
    Register reg;
    if (nextRegistration != null) {
      reg = nextRegistration;
    } else {
      reg = new Register();
      reg.setTimestamp(System.currentTimeMillis());
      reg.setHostname(this.hostname);
      reg.setAgentVersion(this.agentVersion);
      reg.setPrefix(Configuration.PREFIX_DIR);
    }
    RegistrationResponse response;
    try {
      response = handler.handleRegistration(reg);
    } catch (AmbariException | InvalidStateTransitionException e) {
      LOG.info("Registration failed", e);
      return;
    }
    this.responseId = response.getResponseId();
    this.lastRegistrationResponse  = response;
    this.shouldSendRegistration = false;
    this.nextRegistration = null;
  }

  private void sendHeartBeat() throws AmbariException {
    HeartBeat hb;
    if (nextHeartbeat != null) {
      hb = nextHeartbeat;
    } else {
      hb = new HeartBeat();
      hb.setResponseId(responseId);
      hb.setHostname(hostname);
      hb.setTimestamp(System.currentTimeMillis());
    }
    HeartBeatResponse response = handler.handleHeartBeat(hb);
    this.responseId = response.getResponseId();
    this.lastHeartBeatResponse = response;
    this.nextHeartbeat = null;
  }

  /**
   * After this value is set, the agent will send only those many heartbeats.
   * A value of 0 means no heartbeats and -1 means keep sending continuously.
   * @param numberOfHeartbeats
   */
  public void setNumberOfHeartbeats(int numberOfHeartbeats) {
    this.numberOfHeartbeats = numberOfHeartbeats;
    currentHeartbeatCount = 0;
  }

  public void setShouldSendRegistration(boolean shouldSendRegistration) {
    this.shouldSendRegistration = shouldSendRegistration;
  }

  public RegistrationResponse getLastRegistrationResponse() {
    return lastRegistrationResponse;
  }

  public HeartBeatResponse getLastHeartBeatResponse() {
    return lastHeartBeatResponse;
  }

  public void setNextRegistration(Register nextRegistration) {
    this.nextRegistration = nextRegistration;
  }

  public void setNextHeartbeat(HeartBeat nextHeartbeat) {
    this.nextHeartbeat = nextHeartbeat;
  }
}
