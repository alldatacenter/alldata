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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;


/**
 * Captures various agent requests that it sends as part of requests
 */

@Singleton
public class AgentRequests {
  private static final Logger LOG = LoggerFactory.getLogger(HeartbeatMonitor.class);
  private final Map<String, Map<String, Boolean>> requiresExecCmdDetails = new HashMap<>();
  private final Object _lock = new Object();

  /**
   * Creates a holder for agent requests
   */
  public AgentRequests() {
  }

  public void setExecutionDetailsRequest(String host, String component, String requestExecutionCmd) {
    if (StringUtils.isNotBlank(requestExecutionCmd)) {
      Map<String, Boolean> perHostRequiresExecCmdDetails = getPerHostRequiresExecCmdDetails(host);
      if (Boolean.TRUE.toString().toUpperCase().equals(requestExecutionCmd.toUpperCase())) {
        LOG.info("Setting need for exec command to " + requestExecutionCmd + " for " + component);
        perHostRequiresExecCmdDetails.put(component, Boolean.TRUE);
      } else {
        perHostRequiresExecCmdDetails.put(component, Boolean.FALSE);
      }
    }
  }

  public Boolean shouldSendExecutionDetails(String host, String component) {

    Map<String, Boolean> perHostRequiresExecCmdDetails = getPerHostRequiresExecCmdDetails(host);
    if (perHostRequiresExecCmdDetails != null && perHostRequiresExecCmdDetails.containsKey(component)) {
      LOG.debug("Sending exec command details for {}", component);
      return perHostRequiresExecCmdDetails.get(component);
    }

    return Boolean.FALSE;
  }

  private Map<String, Boolean> getPerHostRequiresExecCmdDetails(String host) {
    if (!requiresExecCmdDetails.containsKey(host)) {
      synchronized (_lock) {
        if (!requiresExecCmdDetails.containsKey(host)) {
          requiresExecCmdDetails.put(host, new HashMap<>());
        }
      }
    }

    return requiresExecCmdDetails.get(host);
  }

  @Override
  public String toString() {
    return new StringBuilder().append("requiresExecCmdDetails: ").append(requiresExecCmdDetails).toString();
  }
}
