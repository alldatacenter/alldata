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

package org.apache.ambari.server.controller.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains stages associated with a request.
 */
public class RequestStageContainer {
  /**
   * Request id
   */
  private Long id;

  /**
   * Request stages
   */
  private List<Stage> stages;

  /**
   * Request Factory used to create Request instance
   */
  private RequestFactory requestFactory;

  /**
   * Action Manager
   */
  private ActionManager actionManager;

  private String requestContext = null;

  private ExecuteActionRequest actionRequest = null;

  private String clusterHostInfo = null;

  /**
   * Logger
   */
  private final static Logger LOG =
      LoggerFactory.getLogger(RequestStageContainer.class);

  /**
   * Constructor.
   *
   * @param id       request id
   * @param stages   stages
   * @param factory  request factory
   * @param manager  action manager
   */
  public RequestStageContainer(Long id, List<Stage> stages, RequestFactory factory, ActionManager manager) {
    this(id, stages, factory, manager, null);
  }

  /**
   * Constructor.
   *
   * @param id            request id
   * @param stages        stages
   * @param factory       request factory
   * @param manager       action manager
   * @param actionRequest action request
   */
  public RequestStageContainer(Long id, List<Stage> stages, RequestFactory factory, ActionManager manager,
                               ExecuteActionRequest actionRequest) {
    this.id = id;
    this.stages = stages == null ? new ArrayList<>() : stages;
    this.requestFactory = factory;
    this.actionManager = manager;
    this.actionRequest = actionRequest;
    this.clusterHostInfo = "{}";
  }

  /**
   * Get the request id.
   *
   * @return request id
   */
  public Long getId()  {
    return id;
  }

  public void setClusterHostInfo(String clusterHostInfo){
    this.clusterHostInfo = clusterHostInfo;
  }

  /**
   * Add stages to request.
   *
   * @param stages stages to add
   */
  public void addStages(List<Stage> stages) {
    if (stages != null) {
      this.stages.addAll(stages);
    }
  }

  /**
   * Get request stages.
   *
   * @return  list of stages
   */
  public List<Stage> getStages() {
    return stages;
  }

  /**
   * Get the stage id of the last stage.
   *
   * @return stage id of the last stage or -1 if no stages present
   */
  public long getLastStageId() {
    return stages.isEmpty() ? -1 : stages.get(stages.size() - 1).getStageId();
  }

  /**
   * Sets the context for the request (optional operation)
   *
   * @param context the new context
   */
  public void setRequestContext(String context) {
    requestContext = context;
  }

  /**
   * Determine the projected state for a host component from the existing stages.
   *
   * @param host       host name
   * @param component  component name
   *
   * @return the projected state of a host component after all stages successfully complete
   *         or null if the host component state is not modified in the current stages
   */
  public State getProjectedState(String host, String component) {
    RoleCommand lastCommand = null;

    ListIterator<Stage> iterator = stages.listIterator(stages.size());
    while (lastCommand == null && iterator.hasPrevious()) {
      Stage stage = iterator.previous();

      Map<String, Map<String, HostRoleCommand>> stageCommands = stage.getHostRoleCommands();
      if (stageCommands != null) {
        Map<String, HostRoleCommand> hostCommands = stageCommands.get(host);
        if (hostCommands != null) {
          HostRoleCommand roleCommand = hostCommands.get(component);
          if (roleCommand != null && roleCommand.getRoleCommand() != RoleCommand.SERVICE_CHECK) {
            lastCommand = roleCommand.getRoleCommand();
          }
        }
      }
    }

    State resultingState = null;
    if (lastCommand != null) {
      switch(lastCommand) {
        case INSTALL:
        case STOP:
          resultingState = State.INSTALLED;
          break;
        case START:
          resultingState = State.STARTED;
          break;
        case UNINSTALL:
          resultingState = State.INIT;
          break;
        default:
          resultingState = State.UNKNOWN;
      }
    }
    return resultingState;
  }

  /**
   * Persist the stages.
   */
  public void persist() throws AmbariException {
    if (!stages.isEmpty()) {
      Request request = (null == actionRequest)
          ? requestFactory.createNewFromStages(stages, clusterHostInfo)
          : requestFactory.createNewFromStages(stages, clusterHostInfo, actionRequest);

      if (null != requestContext) {
        request.setRequestContext(requestContext);
      }

      if (request != null) { //request can be null at least in JUnit
        request.setUserName(AuthorizationHelper.getAuthenticatedName());
      }

      if (request != null && request.getStages()!= null && !request.getStages().isEmpty()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Triggering Action Manager, request={}", request);
        }
        actionManager.sendActions(request, actionRequest);
      }
    }
  }

  /**
   * Build a request status response.
   *
   * @return a {@link org.apache.ambari.server.controller.RequestStatusResponse} for the request
   */
  public RequestStatusResponse getRequestStatusResponse() {
    RequestStatusResponse response = null;

    if (! stages.isEmpty()) {
      response = new RequestStatusResponse(id);
      List<HostRoleCommand> hostRoleCommands =
          actionManager.getRequestTasks(id);

      response.setRequestContext(actionManager.getRequestContext(id));
      List<ShortTaskStatus> tasks = new ArrayList<>();

      for (HostRoleCommand hostRoleCommand : hostRoleCommands) {
        tasks.add(new ShortTaskStatus(hostRoleCommand));
      }
      response.setTasks(tasks);
    }
    return response;
  }
}
