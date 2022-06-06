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
package org.apache.ambari.server.orm.entities;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.state.stack.RepoTag;

import com.google.common.base.Objects;

/**
 * Represents a Repository definition type.
 */
@Entity
@Table(name = "repo_definition")
@TableGenerator(name = "repo_definition_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "repo_definition_id_seq"
)
public class RepoDefinitionEntity {
  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "repo_definition_id_generator")
  private Long id;

  /**
   * CollectionTable for RepoTag enum
   */
  @Enumerated(value = EnumType.STRING)
  @ElementCollection(targetClass = RepoTag.class)
  @CollectionTable(name = "repo_tags", joinColumns = @JoinColumn(name = "repo_definition_id"))
  @Column(name = "tag")
  private Set<RepoTag> repoTags = new HashSet<>();

  /**
   * many-to-one association to {@link RepoOsEntity}
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "repo_os_id", nullable = false)
  private RepoOsEntity repoOs;

  @Column(name = "repo_name", nullable = false)
  private String repoName;

  @Column(name = "repo_id", nullable = false)
  private String repoID;

  @Column(name = "base_url", nullable = false)
  private String baseUrl;

  @Column(name = "mirrors")
  private String mirrors;

  @Column(name = "distribution")
  private String distribution;

  @Column(name = "components")
  private String components;

  @Column(name = "unique_repo", nullable = false)
  private short unique = 0;

  /**
   * CollectionTable for RepoTag enum
   */
  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  @ElementCollection(targetClass = String.class)
  @CollectionTable(name = "repo_applicable_services", joinColumns = {@JoinColumn(name = "repo_definition_id")})
  @Column(name = "service_name")
  private List<String> applicableServices = new LinkedList<>();

  public String getDistribution() {
    return distribution;
  }

  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public RepoOsEntity getRepoOs() {
    return repoOs;
  }

  public void setRepoOs(RepoOsEntity repoOs) {
    this.repoOs = repoOs;
  }

  public String getRepoName() {
    return repoName;
  }

  public void setRepoName(String repoName) {
    this.repoName = repoName;
  }

  public String getRepoID() {
    return repoID;
  }

  public void setRepoID(String repoID) {
    this.repoID = repoID;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getMirrors() {
    return mirrors;
  }

  public void setMirrors(String mirrors) {
    this.mirrors = mirrors;
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

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getComponents() {
    return components;
  }

  public void setComponents(String components) {
    this.components = components;
  }

  public boolean isUnique() {
    return unique == 1;
  }

  public void setUnique(boolean unique) {
    this.unique = (short) (unique ? 1 : 0);
  }

  public Set<RepoTag> getTags() {
    return repoTags;
  }

  public void setTags(Set<RepoTag> repoTags) {
    this.repoTags = repoTags;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return java.util.Objects.hash(repoTags, repoName, repoID, baseUrl, mirrors, distribution, components, unique, applicableServices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (this == object) {
      return true;
    }

    if (object.getClass() != getClass()) {
      return false;
    }

    RepoDefinitionEntity that = (RepoDefinitionEntity) object;
    return Objects.equal(unique, that.unique)
        && Objects.equal(repoTags, that.repoTags)
        && Objects.equal(repoName, that.repoName)
        && Objects.equal(repoID, that.repoID)
        && Objects.equal(baseUrl, that.baseUrl)
        && Objects.equal(mirrors, that.mirrors)
        && Objects.equal(distribution, that.distribution)
        && Objects.equal(components, that.components)
        && Objects.equal(applicableServices, that.applicableServices);
  }
}