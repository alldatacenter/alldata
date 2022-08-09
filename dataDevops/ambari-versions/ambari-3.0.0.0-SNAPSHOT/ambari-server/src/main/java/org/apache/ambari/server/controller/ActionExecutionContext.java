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


package org.apache.ambari.server.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.state.StackId;

/**
 * The context required to create tasks and stages for a custom action
 */
public class ActionExecutionContext {
  private final String clusterName;
  private final String actionName;
  private List<RequestResourceFilter> resourceFilters;
  private RequestOperationLevel operationLevel;
  private Map<String, String> parameters;
  private TargetHostType targetType;
  private Short timeout;
  private String expectedServiceName;
  private String expectedComponentName;
  private boolean hostsInMaintenanceModeExcluded = true;
  private boolean allowRetry = false;
  private StackId stackId;

  /**
   * If {@code true}, instructs Ambari not to worry about whether or not the
   * command is valid. This is used in cases where we might have to schedule
   * commands ahead of time for components which are not yet installed.
   */
  private boolean isFutureCommand = false;

  private List<ExecutionCommandVisitor> m_visitors = new ArrayList<>();

  /**
   * {@code true} if slave/client component failures should be automatically
   * skipped. This will only automatically skip the failure if the task is
   * skippable to begin with.
   */
  private boolean autoSkipFailures = false;

  /**
   * Create an ActionExecutionContext to execute an action from a request
   */
  public ActionExecutionContext(String clusterName, String actionName,
      List<RequestResourceFilter> resourceFilters,
      Map<String, String> parameters, TargetHostType targetType,
      Short timeout, String expectedServiceName,
      String expectedComponentName) {

    this.clusterName = clusterName;
    this.actionName = actionName;
    this.resourceFilters = resourceFilters;
    this.parameters = parameters;
    this.targetType = targetType;
    this.timeout = timeout;
    this.expectedServiceName = expectedServiceName;
    this.expectedComponentName = expectedComponentName;
  }

  public ActionExecutionContext(String clusterName, String actionName,
                                List<RequestResourceFilter> resourceFilters) {
    this.clusterName = clusterName;
    this.actionName = actionName;
    this.resourceFilters = resourceFilters;
  }

  public ActionExecutionContext(String clusterName, String commandName,
                                List<RequestResourceFilter> resourceFilters,
                                Map<String, String> parameters) {
    this.clusterName = clusterName;
    actionName = commandName;
    this.resourceFilters = resourceFilters;
    this.parameters = parameters;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getActionName() {
    return actionName;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public TargetHostType getTargetType() {
    return targetType;
  }

  public Short getTimeout() {
    return timeout;
  }

  public void setTimeout(Short timeout) {
    this.timeout = timeout;
  }

  public List<RequestResourceFilter> getResourceFilters() {
    return resourceFilters;
  }

  public RequestOperationLevel getOperationLevel() {
    return operationLevel;
  }

  public void setOperationLevel(RequestOperationLevel operationLevel) {
    this.operationLevel = operationLevel;
  }

  public String getExpectedServiceName() {
    return expectedServiceName;
  }

  public String getExpectedComponentName() {
    return expectedComponentName;
  }

  /**
   * Gets whether the action can be retried if it failed. The default is
   * {@code true)}.
   *
   * @return {@code true} if the action can be retried if it fails.
   */
  public boolean isRetryAllowed() {
    return allowRetry;
  }

  /**
   * Sets whether the action can be retried if it fails.
   *
   * @param allowRetry
   *          {@code true} if the action can be retried if it fails.
   */
  public void setRetryAllowed(boolean allowRetry){
    this.allowRetry = allowRetry;
  }

  /**
   * Gets whether skippable actions that failed are automatically skipped.
   *
   * @return the autoSkipFailures
   */
  public boolean isFailureAutoSkipped() {
    return autoSkipFailures;
  }

  /**
   * Sets whether skippable action that failed are automatically skipped.
   *
   * @param autoSkipFailures
   *          {@code true} to automatically skip failures which are marked as
   *          skippable.
   */
  public void setAutoSkipFailures(boolean autoSkipFailures) {
    this.autoSkipFailures = autoSkipFailures;
  }

  /**
   * Gets the stack to use for generating stack-associated values for a
   * command. In some cases the cluster's stack is not the correct one to use,
   * such as when distributing a repository.
   *
   * @return the stack to use when generating
   *         stack-specific content for the command.
   */
  public StackId getStackId() {
    return stackId;
  }

  /**
   * Sets the stack to use for generating stack-associated values for a
   * command. In some cases the cluster's stack is not the correct one to use,
   * such as when distributing a repository.
   *
   * @param stackId
   *          the stackId to use for stack-based properties on the command.
   */
  public void setStackId(StackId stackId) {
    this.stackId = stackId;
  }


  /**
   * Adds a command visitor that will be invoked after a command is created.  Provides access
   * to the command.
   *
   * @param visitor the visitor
   */
  public void addVisitor(ExecutionCommandVisitor visitor) {
    m_visitors.add(visitor);
  }

  @Override
  public String toString() {
    return "ActionExecutionContext{" +
      "clusterName='" + clusterName + '\'' +
      ", actionName='" + actionName + '\'' +
      ", resourceFilters=" + resourceFilters +
      ", operationLevel=" + operationLevel +
      ", parameters=" + parameters +
      ", targetType=" + targetType +
      ", timeout=" + timeout +
      ", isMaintenanceModeHostExcluded=" + hostsInMaintenanceModeExcluded +
      ", allowRetry=" + allowRetry +
      ", autoSkipFailures=" + autoSkipFailures +
      '}';
  }

  /**
   * Gets whether hosts in maintenance mode should be excluded from the command.
   *
   * @return {@code true} to exclude any hosts in maintenance mode from the
   *         command, {@code false} to include hosts which are in maintenance
   *         mode.
   */
  public boolean isMaintenanceModeHostExcluded() {
    return hostsInMaintenanceModeExcluded;
  }

  /**
   * Sets whether hosts in maintenance mode should be excluded from the command.
   *
   * @param excluded
   *          {@code true} to exclude any hosts in maintenance mode from the
   *          command, {@code false} to include hosts which are in maintenance
   *          mode.
   */
  public void setMaintenanceModeHostExcluded(boolean excluded) {
    hostsInMaintenanceModeExcluded = excluded;
  }

  /**
   * Called as a way to post-process the command after it has been created and various objects
   * have been set.
   *
   * @param command the command
   */
  public void visitAll(ExecutionCommand command) {
    for (ExecutionCommandVisitor visitor : m_visitors) {
      visitor.visit(command);
    }
  }

  /**
   *
   * Interface that allows a final attempt to setting values on an {@link ExecutionCommand}
   *
   */
  public interface ExecutionCommandVisitor {
    void visit(ExecutionCommand command);
  }

  /**
   * Gets whether Ambari should skip all kinds of command verifications while
   * scheduling since this command runs in the future and might not be
   * considered "valid".
   * <p/>
   * A use case for this would be during an upgrade where trying to schedule
   * commands for a component which has yet to be added to the cluster (since
   * it's added as part of the upgrade).
   *
   * @return {@code true} if the command is scheduled to run in the future.
   */
  public boolean isFutureCommand() {
    return isFutureCommand;
  }

  /**
   * Sets whether Ambari should skip all kinds of command verifications while
   * scheduling since this command runs in the future and might not be
   * considered "valid".
   * <p/>
   * A use case for this would be during an upgrade where trying to schedule
   * commands for a component which has yet to be added to the cluster (since
   * it's added as part of the upgrade).
   *
   * @param isFutureCommand
   *          {@code true} to have Ambari skip verification of things like
   *          component hosts while scheduling commands.
   */
  public void setIsFutureCommand(boolean isFutureCommand) {
    this.isFutureCommand = isFutureCommand;
  }

}