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

public class StackServiceRequest extends StackVersionRequest {

  private String serviceName;
  private String credentialStoreSupported;
  private String credentialStoreEnabled;

  public StackServiceRequest(String stackName, String stackVersion,
                             String serviceName) {
    this(stackName, stackVersion, serviceName, null, null);
  }

  public StackServiceRequest(String stackName, String stackVersion,
                             String serviceName, String credentialStoreSupported, String credentialStoreEnabled) {
    super(stackName, stackVersion);

    this.setServiceName(serviceName);
    this.setCredentialStoreSupported(credentialStoreSupported);
    this.setCredentialStoreEnabled(credentialStoreEnabled);
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Get whether credential store is supported. If value is null,
   * this property was not specified.
   *
   * @return null, "true", "false"
   */
  public String getCredentialStoreSupported() {
    return credentialStoreSupported;
  }

  /**
   * Set whether credential store is supported. Null indicates unspecified.
   *
   * @param credentialStoreSupported null, "true", "false"
   */
  public void setCredentialStoreSupported(String credentialStoreSupported) {
    this.credentialStoreSupported = credentialStoreSupported;
  }

  /**
   * Get whether credential store is enabled. If value is null,
   * this property was not specified.
   *
   * @return null, "true", "false"
   */
  public String getCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  /**
   * Set whether credential store is supported. Null indicates unspecified.
   *
   * @param credentialStoreEnabled null, "true", "false"
   */
  public void setCredentialStoreEnabled(String credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }

}
