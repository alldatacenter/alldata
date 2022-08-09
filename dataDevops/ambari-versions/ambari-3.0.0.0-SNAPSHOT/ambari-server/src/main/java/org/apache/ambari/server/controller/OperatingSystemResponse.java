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

public class OperatingSystemResponse {
  private String stackName;
  private String stackVersion;
  private String osType;
  private Long repositoryVersionId;
  private String versionDefinitionId;
  private boolean ambariManagedRepos = true;

  public OperatingSystemResponse(String osType) {
    setOsType(osType);
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  public Long getRepositoryVersionId() {
    return repositoryVersionId;
  }

  public void setRepositoryVersionId(Long repositoryVersionId) {
    this.repositoryVersionId = repositoryVersionId;
  }

  /**
   * @param id the version definition id
   */
  public void setVersionDefinitionId(String id) {
    versionDefinitionId = id;
  }

  /**
   * @return the version definition id
   */
  public String getVersionDefinitionId() {
    return versionDefinitionId;
  }

  /**
   * @param managed {@code true} if ambari is managing the repositories for the OS
   */
  public void setAmbariManagedRepos(boolean managed) {
    ambariManagedRepos = managed;
  }

  /**
   * @return {@code true} if ambari is managing the repositories for the OS
   */
  public boolean isAmbariManagedRepos() {
    return ambariManagedRepos;
  }
}
