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

import java.util.List;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.state.stack.RepoTag;

public class RepositoryResponse {

  private String stackName;
  private String stackVersion;
  private String baseUrl;
  private String osType;
  private String repoId;
  private String repoName;
  private String distribution;
  private String components;
  private String mirrorsList;
  private String defaultBaseUrl;
  private Long repositoryVersionId;
  private String versionDefinitionId;
  private Long clusterVersionId;
  private boolean unique;
  private Set<RepoTag> tags;

  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  private List<String> applicableServices;

  public RepositoryResponse(String baseUrl, String osType, String repoId,
                            String repoName, String distribution, String components,
                            String mirrorsList, String defaultBaseUrl,
                            Set<RepoTag> repoTags, List<String> applicableServices) {
    setBaseUrl(baseUrl);
    setOsType(osType);
    setRepoId(repoId);
    setRepoName(repoName);
    setDistribution(distribution);
    setComponents(components);
    setMirrorsList(mirrorsList);
    setDefaultBaseUrl(defaultBaseUrl);
    setTags(repoTags);
    setApplicableServices(applicableServices);
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

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  public String getRepoId() {
    return repoId;
  }

  public void setRepoId(String repoId) {
    this.repoId = repoId;
  }

  public String getRepoName() {
    return repoName;
  }

  public void setRepoName(String repoName) {
    this.repoName = repoName;
  }

  public String getDistribution() {
    return distribution;
  }

  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public String getComponents() {
    return components;
  }

  public void setComponents(String components) {
    this.components = components;
  }

  public String getMirrorsList() {
    return mirrorsList;
  }

  public void setMirrorsList(String mirrorsList) {
    this.mirrorsList = mirrorsList;
  }

  public String getDefaultBaseUrl() {
    return defaultBaseUrl;
  }

  public void setDefaultBaseUrl(String url) {
    this.defaultBaseUrl = url;
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
   * @param id the cluster version id for the response
   */
  public void setClusterVersionId(Long id) {
    clusterVersionId = id;
  }

  /**
   * @return the cluster version id for the response
   */
  public Long getClusterVersionId() {
    return clusterVersionId;
  }

  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }


  /**
   * @return the repo tags
   */
  public Set<RepoTag> getTags() {
    return tags;
  }

  /**
   * @param repoTags    the repo tags
   */
  public void setTags(Set<RepoTag> repoTags) {
    tags = repoTags;
  }

  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  public List<String> getApplicableServices() {
    return applicableServices;
  }

  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  public void setApplicableServices(List<String> applicableServices) {
    this.applicableServices = applicableServices;
  }
}
