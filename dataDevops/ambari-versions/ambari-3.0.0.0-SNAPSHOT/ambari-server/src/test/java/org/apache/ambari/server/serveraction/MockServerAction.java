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

package org.apache.ambari.server.serveraction;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Clusters;

import com.google.inject.Inject;

/**
 * The MockServerAction is an implementation of a ServerAction strictly used to testing purposes.
 * <p/>
 * This class helps to generate several scenarios from success cases to failure cases.  The
 * force_fail command parameter can be used to generate different failure cases:
 * <ul>
 * <li>exception
 * - Causes the action to fail by throwing an AmbariException</li>
 * <li>timeout
 * - Causes the action to fail by timing out (the COMMAND_TIMEOUT value must be set to a reasonable
 * value)</li>
 * </dl>
 * <p/>
 * If not instructed to fail, this implementation will attempt to increment a "data" counter in a
 * shared data context - if available.
 */
public class MockServerAction extends AbstractServerAction {

  public static final String PAYLOAD_FORCE_FAIL = "force_fail";

  @Inject
  private Clusters clusters;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    Map<String, String> commandParameters = getCommandParameters();

    if (clusters == null) { // Ensure that the injected Clusters object exists...
      throw new AmbariException("Missing payload");
    } else if (commandParameters == null) {
      throw new AmbariException("Missing payload");
    } else if ("exception".equalsIgnoreCase(commandParameters.get(PAYLOAD_FORCE_FAIL))) {
      throw new AmbariException("Failing execution by request");
    } else if ("report".equalsIgnoreCase(commandParameters.get(PAYLOAD_FORCE_FAIL))) {
      return createCommandReport(1, HostRoleStatus.FAILED, null, "Forced fail via command", "Failing execution by request");
    } else {
      if ("timeout".equalsIgnoreCase(commandParameters.get(PAYLOAD_FORCE_FAIL))) {
        Long timeout;

        try {
          timeout = (commandParameters.containsKey(ExecutionCommand.KeyNames.COMMAND_TIMEOUT))
              ? Long.parseLong(commandParameters.get(ExecutionCommand.KeyNames.COMMAND_TIMEOUT)) * 1000 // Convert seconds to milliseconds
              : null;
        } catch (NumberFormatException e) {
          timeout = null;
        }

        if (timeout != null) {
          Thread.sleep(timeout * 10);
        }
      }

      // Test updating the shared data context...
      if (requestSharedDataContext != null) {
        Integer data = (Integer) requestSharedDataContext.get("Data");

        if (data == null) {
          data = 0;
        }

        requestSharedDataContext.put("Data", ++data);
      }

      return createCommandReport(0, HostRoleStatus.COMPLETED, null, "Success!", null);
    }
  }
}