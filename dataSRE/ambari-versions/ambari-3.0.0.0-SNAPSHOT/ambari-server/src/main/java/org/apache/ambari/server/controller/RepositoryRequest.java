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

public class RepositoryRequest extends OperatingSystemRequest {

  private String repoId;
  private String baseUrl;
  private String mirrorsList;
  private boolean verify = true;
  private Long clusterVersionId = null;
  private String repoName = null;

  public RepositoryRequest(String stackName, String stackVersion, String osType, String repoId, String repoName) {
    super(stackName, stackVersion, osType);
    setRepoId(repoId);
    setRepoName(repoName);
  }

  public String getRepoId() {
    return repoId;
  }

  public void setRepoId(String repoId) {
    this.repoId = repoId;
  }

  /**
   * Gets the base URL for the repo.
   *
   * @return the url
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Sets the base URL for the repo.
   *
   * @param url the base URL.
   */
  public void setBaseUrl(String url) {
    baseUrl = url;
  }

  /**
   * @return <code>true</code> if the base url should be verified.  Default is <code>true</code>.
   */
  public boolean isVerifyBaseUrl() {
    return verify;
  }

  /**
   * @param verifyUrl <code>true</code> to verify  the base url
   */
  public void setVerifyBaseUrl(boolean verifyUrl) {
    verify = verifyUrl;
  }

  /**
   * @param id the cluster version id for the request
   */
  public void setClusterVersionId(Long id) {
    clusterVersionId = id;
  }

  /**
   * @return the cluster version id for the request
   */
  public Long getClusterVersionId() {
    return clusterVersionId;
  }

  /**
   * @return name of the repository (without version identifier)
   */
  public String getRepoName() {
    return repoName;
  }

  /**
   * @param repoName name of the repository (without version identifier)
   */
  public void setRepoName(String repoName) {
    this.repoName = repoName;
  }

  @Override
  public String toString() {
    return "RepositoryRequest [repoId=" + repoId + ", baseUrl=" + baseUrl
        + ", verify=" + verify + ", getOsType()=" + getOsType()
        + ", getRepositoryVersionId()=" + getRepositoryVersionId()
        + ", getStackVersion()=" + getStackVersion() + ", getStackName()="
        + getStackName() + ", getRepoName()=" + getRepoName() + "]";
  }



  /**
   * Gets the mirrors list for the repo.
   *
   * @return the mirrors list
   */
  public String getMirrorsList() {
    return mirrorsList;
  }

  /**
   * Sets the mirrors list for the repo.
   *
   * @param mirrorsList the mirrors list
   */
  public void setMirrorsList(String mirrorsList) {
    this.mirrorsList = mirrorsList;
  }
}
