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

public class StackServiceComponentRequest extends StackServiceRequest {

  private String componentName;

  private String recoveryEnabled;

  public StackServiceComponentRequest(String stackName, String stackVersion,
      String serviceName, String componentName) {
    this(stackName, stackVersion, serviceName, componentName, null);
  }

  public StackServiceComponentRequest(String stackName, String stackVersion,
      String serviceName, String componentName, String recoveryEnabled) {
    super(stackName, stackVersion, serviceName);
    setComponentName(componentName);
    setRecoveryEnabled(recoveryEnabled);
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * Get whether auto start is enabled. If value is null,
   * auto start value was not specified.
   *
   * @return null, "true", "false"
   */
  public String getRecoveryEnabled() {
    return recoveryEnabled;
  }

  /**
   * Set whether auto start is enabled. Null indicates unspecified.
   *
   * @param recoveryEnabled null, "true", "false"
   */
  public void setRecoveryEnabled(String recoveryEnabled) {
    this.recoveryEnabled = recoveryEnabled;
  }
}
