/**
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.stack.RepoTag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Wraps the information required to create repositories from a command.  This was added
 * as a top level command object.
 */
public class CommandRepository {

  @SerializedName("repositories")
  @JsonProperty("repositories")
  private List<Repository> m_repositories = new ArrayList<>();

  @SerializedName("repoVersion")
  @JsonProperty("repoVersion")
  private String m_repoVersion;

  @SerializedName("repoVersionId")
  @JsonProperty("repoVersionId")
  private long m_repoVersionId;

  @SerializedName("stackName")
  @JsonProperty("stackName")
  private String m_stackName;

  @SerializedName("repoFileName")
  @JsonProperty("repoFileName")
  private String m_repoFileName;

  @SerializedName("feature")
  @JsonProperty("feature")
  private final CommandRepositoryFeature feature = new CommandRepositoryFeature();

  /**
   * Provides {@link CommandRepository} feature
   *
   * @return {@link CommandRepositoryFeature}
   */
  public CommandRepositoryFeature getFeature(){
    return feature;
  }

  /**
   * {@code true} if Ambari believes that this repository has reported back it's
   * version after distribution.
   */
  @SerializedName("resolved")
  @JsonProperty("resolved")
  private boolean m_resolved;

  /**
   * @param version the repo version
   */
  public void setRepoVersion(String version) {
    m_repoVersion = version;
  }


  public String getRepoVersion(){
    return m_repoVersion;
  }

  /**
   * @param id the repository id
   */
  public void setRepositoryVersionId(long id) {
    m_repoVersionId = id;
  }

  /**
   * @param name the stack name
   */
  public void setStackName(String name) {
    m_stackName = name;
  }

  /**
   * @param repositories the repositories if sourced from the stack instead of the repo_version.
   */
  public void setRepositories(Collection<RepositoryInfo> repositories) {
    m_repositories = new ArrayList<>();

    for (RepositoryInfo info : repositories) {
      m_repositories.add(new Repository(info));
    }
  }

  /**
   * @param osType        the OS type for the repositories
   * @param repositories  the repository entities that should be processed into a file
   */
  public void setRepositories(String osType, Collection<RepoDefinitionEntity> repositories) {
    m_repositories = new ArrayList<>();

    for (RepoDefinitionEntity entity : repositories) {
      m_repositories.add(new Repository(osType, entity));
    }
  }

  /**
   * @return the repositories that the command should process into a file.
   */
  public Collection<Repository> getRepositories() {
    return m_repositories;
  }

  /**
   * Sets a uniqueness on the repo ids.
   *
   * @param suffix  the repo id suffix
   */
  public void setUniqueSuffix(String suffix) {
    for (Repository repo : m_repositories) {
      repo.m_repoId = repo.m_repoId + suffix;
    }
  }

  /**
   * Sets fields for non-managed
   */
  public void setNonManaged() {
    for (Repository repo : m_repositories) {
      repo.m_baseUrl = null;
      repo.m_mirrorsList = null;
      repo.m_ambariManaged = false;
    }
  }

  public long getRepoVersionId() {
    return m_repoVersionId;
  }

  /**
   * Gets whether this repository has had its version resolved.
   *
   * @param resolved
   *          {@code true} to mark this repository as being resolved.
   */
  public void setResolved(boolean resolved) {
    m_resolved = resolved;
  }

  /**
   * Update repository id to be consistent with old format
   *
   * @param repoVersion
   */
  @Deprecated
  @Experimental(feature= ExperimentalFeature.PATCH_UPGRADES)
  public void setLegacyRepoId(String repoVersion){
    for (Repository repo : m_repositories) {
      repo.m_repoId = String.format("%s-%s", repo.getRepoName(), repoVersion);
    }
  }

  /**
   * Sets filename for the repo
   *
   * @param stackName  name of the stack
   * @param repoVersion repository version
   */
  @Deprecated
  @Experimental(feature= ExperimentalFeature.PATCH_UPGRADES)
  public void setLegacyRepoFileName(String stackName, String repoVersion) {
    this.m_repoFileName = String.format("%s-%s", stackName, repoVersion);
  }

  /**
   * Sets filename for the repo
   *
   * @param stackName  name of the stack
   * @param repoVersionId repository version id
   */
  public void setRepoFileName(String stackName, Long repoVersionId) {
    this.m_repoFileName = String.format("ambari-%s-%s", stackName.toLowerCase(), repoVersionId.toString());
  }

  /**
   * Minimal information about repository feature
   */
  public static class CommandRepositoryFeature {

    /**
     * Repository is pre-installed on the host
     */
    @SerializedName("preInstalled")
    @JsonProperty("preInstalled")
    private Boolean m_isPreInstalled = false;

    /**
     * Indicates if any operation with the packages should be scoped to this repository only.
     *
     * Currently affecting: getting available packages from the repository
     */
    @SerializedName("scoped")
    @JsonProperty("scoped")
    private boolean m_isScoped = true;

    public void setIsScoped(boolean isScoped){
      this.m_isScoped = isScoped;
    }

    public void setPreInstalled(String isPreInstalled) {
      this.m_isPreInstalled = isPreInstalled.equalsIgnoreCase("true");
    }
  }

  /**
   * Minimal information required to generate repo files on the agent.  These are copies
   * of the repository objects from repo versions that can be changed for URL overrides, etc.
   */
  public static class Repository {

    @SerializedName("baseUrl")
    @JsonProperty("baseUrl")
    private String m_baseUrl;

    @SerializedName("repoId")
    @JsonProperty("repoId")
    private String m_repoId;

    @SerializedName("ambariManaged")
    @JsonProperty("ambariManaged")
    private boolean m_ambariManaged = true;

    @SerializedName("repoName")
    @JsonProperty("repoName")
    private final String m_repoName;

    @SerializedName("distribution")
    @JsonProperty("distribution")
    private final String m_distribution;

    @SerializedName("components")
    @JsonProperty("components")
    private final String m_components;

    @SerializedName("mirrorsList")
    @JsonProperty("mirrorsList")
    private String m_mirrorsList;

    @SerializedName("applicableServices")
    @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
      comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
    private List<String> m_applicableServices;

    @SerializedName("tags")
    @JsonProperty("tags")
    private Set<RepoTag> m_tags;


    private transient String m_osType;

    private Repository(RepositoryInfo info) {
      m_baseUrl = info.getBaseUrl();
      m_osType = info.getOsType();
      m_repoId = info.getRepoId();
      m_repoName = info.getRepoName();
      m_distribution = info.getDistribution();
      m_components = info.getComponents();
      m_mirrorsList = info.getMirrorsList();
      m_applicableServices = info.getApplicableServices();
      m_tags = info.getTags();
    }

    private Repository(String osType, RepoDefinitionEntity entity) {
      m_baseUrl = entity.getBaseUrl();
      m_repoId = entity.getRepoID();
      m_repoName = entity.getRepoName();
      m_distribution = entity.getDistribution();
      m_components = entity.getComponents();
      m_mirrorsList = entity.getMirrors();
      m_applicableServices = entity.getApplicableServices();
      m_osType = osType;
      m_tags = entity.getTags();
    }

    public void setRepoId(String repoId){
      m_repoId = repoId;
    }

    public void setBaseUrl(String url) {
      m_baseUrl = url;
    }

    public String getRepoName() {
      return m_repoName;
    }

    public String getRepoId() {
      return m_repoId;
    }

    public String getBaseUrl() {
      return m_baseUrl;
    }

    public boolean isAmbariManaged() {
      return m_ambariManaged;
    }

    @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
      comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
    public void setApplicableServices(List<String> applicableServices) {
      m_applicableServices = applicableServices;
    }

    @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
      comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
    public List<String> getApplicableServices() {
      return m_applicableServices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return new ToStringBuilder(null)
          .append("os", m_osType)
          .append("name", m_repoName)
          .append("distribution", m_distribution)
          .append("components", m_components)
          .append("id", m_repoId)
          .append("baseUrl", m_baseUrl)
          .append("applicableServices", (m_applicableServices != null? StringUtils.join(m_applicableServices, ",") : ""))
          .toString();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CommandRepository that = (CommandRepository) o;

    return m_repoVersionId == that.m_repoVersionId;
  }

  @Override
  public int hashCode() {
    return (int) (m_repoVersionId ^ (m_repoVersionId >>> 32));
  }
}
