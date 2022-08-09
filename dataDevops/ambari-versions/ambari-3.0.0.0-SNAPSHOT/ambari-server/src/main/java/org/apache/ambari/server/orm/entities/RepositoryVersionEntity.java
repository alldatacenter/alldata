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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

@Entity
@Table(name = "repo_version", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"display_name"}),
    @UniqueConstraint(columnNames = {"stack_id", "version"})
})
@TableGenerator(name = "repository_version_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "repo_version_id_seq"
    )
@NamedQueries({
    @NamedQuery(
        name = "repositoryVersionByDisplayName",
        query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.displayName=:displayname"),
    @NamedQuery(
        name = "repositoryVersionByStack",
        query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.stack.stackName=:stackName AND repoversion.stack.stackVersion=:stackVersion"),
    @NamedQuery(
        name = "repositoryVersionByStackAndType",
        query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.stack.stackName=:stackName AND repoversion.stack.stackVersion=:stackVersion AND repoversion.type=:type"),
    @NamedQuery(
        name = "repositoryVersionByStackNameAndVersion",
        query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.stack.stackName=:stackName AND repoversion.version=:version"),
    @NamedQuery(
        name = "repositoryVersionsFromDefinition",
        query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.versionXsd IS NOT NULL"),
    @NamedQuery(
        name = "findRepositoryByVersion",
        query = "SELECT repositoryVersion FROM RepositoryVersionEntity repositoryVersion WHERE repositoryVersion.version = :version ORDER BY repositoryVersion.id DESC"),
    @NamedQuery(
        name = "findByServiceDesiredVersion",
        query = "SELECT repositoryVersion FROM RepositoryVersionEntity repositoryVersion WHERE repositoryVersion IN (SELECT DISTINCT sd1.desiredRepositoryVersion FROM ServiceDesiredStateEntity sd1 WHERE sd1.desiredRepositoryVersion IN ?1)") })
@StaticallyInject
public class RepositoryVersionEntity {
  @Id
  @Column(name = "repo_version_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "repository_version_id_generator")
  private Long id;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "stack_id", nullable = false)
  private StackEntity stack;

  @Column(name = "version")
  private String version;

  @Column(name = "display_name")
  private String displayName;

  /**
   * one-to-many association to {@link RepoOsEntity}
   */
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "repositoryVersionEntity", orphanRemoval = true)
  private List<RepoOsEntity> repoOsEntities = new ArrayList<>();

  @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "repositoryVersion")
  private Set<HostVersionEntity> hostVersionEntities;

  @Column(name = "repo_type", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private RepositoryType type = RepositoryType.STANDARD;

  @Lob
  @Column(name="version_xml")
  private String versionXml;

  @Transient
  private VersionDefinitionXml versionDefinition = null;

  @Column(name="version_url")
  private String versionUrl;

  @Column(name="version_xsd")
  private String versionXsd;

  @Column(name = "hidden", nullable = false)
  private short isHidden = 0;

  /**
   * Repositories can't be trusted until they have been deployed and we've
   * detected their actual version. Most of the time, things match up, but
   * editing a VDF could causes the version to be misrepresented. Once we have
   * received the correct version of the repository (normally after it's been
   * installed), then we can set this flag to {@code true}.
   */
  @Column(name = "resolved", nullable = false)
  private short resolved = 0;

  @Column(name = "legacy", nullable = false)
  private short isLegacy = 0;

  @ManyToOne
  @JoinColumn(name = "parent_id")
  private RepositoryVersionEntity parent;

  @OneToMany(mappedBy = "parent")
  private List<RepositoryVersionEntity> children;


  // ----- RepositoryVersionEntity -------------------------------------------------------

  public RepositoryVersionEntity() {

  }

  public RepositoryVersionEntity(StackEntity stack, String version,
                                 String displayName, List<RepoOsEntity> repoOsEntities) {
    this.stack = stack;
    this.version = version;
    this.displayName = displayName;
    this.repoOsEntities = repoOsEntities;
    for (RepoOsEntity repoOsEntity : repoOsEntities) {
      repoOsEntity.setRepositoryVersionEntity(this);
    }
  }

  @PreUpdate
  @PrePersist
  public void removePrefixFromVersion() {
    String stackName = stack.getStackName();
    if (version.startsWith(stackName)) {
      version = version.substring(stackName.length() + 1);
    }
  }

  /**
   * Update one-to-many relation without rebuilding the whole entity
   * @param entity many-to-one entity
   */
  public void updateHostVersionEntityRelation(HostVersionEntity entity){
    hostVersionEntities.add(entity);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the repository version's stack.
   *
   * @return the stack.
   */
  public StackEntity getStack() {
    return stack;
  }

  /**
   * Sets the repository version's stack.
   *
   * @param stack
   *          the stack to set for the repo version (not {@code null}).
   */
  public void setStack(StackEntity stack) {
    this.stack = stack;
  }

  public String getVersion() {
    return version;
  }

  /**
   * Sets the version on this repository version entity. If the version is
   * confirmed as correct, then the called should also set
   * {@link #setResolved(boolean)}.
   *
   * @param version
   */
  public void setVersion(String version) {
    this.version = version;

    // need to be called to avoid work with wrong value until entity would be persisted
    if (null != version && null != stack && null != stack.getStackName()){
      removePrefixFromVersion();
    }
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }


  public String getStackName() {
    return getStackId().getStackName();
  }

  public String getStackVersion() {
    return getStackId().getStackVersion();
  }

  public StackId getStackId() {
    if (null == stack) {
      return null;
    }

    return new StackId(stack.getStackName(), stack.getStackVersion());
  }

  /**
   * @return the type
   */
  public RepositoryType getType() {
    return type;
  }

  /**
   * @param type the repo type
   */
  public void setType(RepositoryType type) {
    this.type = type;
  }

  /**
   * @return the XML that is the basis for the version
   */
  public String getVersionXml() {
    return versionXml;
  }

  /**
   * @param xml the XML that is the basis for the version
   */
  public void setVersionXml(String xml) {
    versionXml = xml;
  }

  /**
   * @return The url used for the version.  Optional in case the XML was loaded via blob.
   */
  public String getVersionUrl() {
    return versionUrl;
  }

  /**
   * @param url the url used to load the XML.
   */
  public void setVersionUrl(String url) {
    versionUrl = url;
  }

  /**
   * @return the XSD name extracted from the XML.
   */
  public String getVersionXsd() {
    return versionXsd;
  }

  /**
   * @param xsdLocation the XSD name extracted from XML.
   */
  public void setVersionXsd(String xsdLocation) {
    versionXsd = xsdLocation;
  }

  /**
   * Parse the version XML into its object representation.  This causes the XML to be lazy-loaded
   * from storage, and will only be parsed once per request.
   * @return {@code null} if the XSD (from the XML) is not available.
   * @throws Exception
   */
  public VersionDefinitionXml getRepositoryXml() throws Exception {
    if (null == versionXsd) {
      return null;
    }

    if (null == versionDefinition) {
      versionDefinition = VersionDefinitionXml.load(getVersionXml());
    }

    return versionDefinition;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return java.util.Objects.hash(stack, version, displayName, repoOsEntities);
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

    RepositoryVersionEntity that = (RepositoryVersionEntity) object;
    return Objects.equal(stack, that.stack) && Objects.equal(version, that.version)
        && Objects.equal(displayName, that.displayName)
        && Objects.equal(repoOsEntities, that.repoOsEntities);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).add("stack", stack).add("version",
        version).add("type", type).add("hidden", isHidden == 1).toString();
  }

  /**
   * Determine if the version belongs to the stack.
   * @param stackId Stack, such as HDP-2.3
   * @param version Version number, such as 2.3.0.0, or 2.3.0.0-1234
   * @return Return true if the version starts with the digits of the stack.
   */
  public static boolean isVersionInStack(StackId stackId, String version) {
    if (null != version && !StringUtils.isBlank(version)) {
      String stackName = stackId.getStackName();
      if (version.startsWith(stackName + "-")) {
        version = version.substring(stackName.length() + 1);
      }

      String leading = stackId.getStackVersion();  // E.g, 2.3
      // In some cases during unit tests, the leading can contain 3 digits, so only the major number (first two parts) are needed.
      String[] leadingParts = leading.split("\\.");
      if (leadingParts.length > 2) {
        leading = leadingParts[0] + "." + leadingParts[1];
      }
      return version.startsWith(leading);
    }
    return false;
  }

  /**
   * @param entity parent entity
   */
  public void setParent(RepositoryVersionEntity entity) {
    parent = entity;
    parent.children.add(this);
  }

  /**
   * @return the repositories that are denoted children
   */
  public List<RepositoryVersionEntity> getChildren() {
    return children;
  }

  /**
   * @return the parentId, or {@code null} if the entity is already a parent
   */
  public Long getParentId() {
    return null == parent ? null : parent.getId();
  }

  /**
   * Gets whether this repository is hidden.
   *
   * @return
   */
  public boolean isHidden() {
    return isHidden != 0;
  }

  /**
   * Sets whether this repository is hidden. A repository can be hidden for
   * several reasons, including if it has been removed (but needs to be kept
   * around for foreign key relationships) or if it just is not longer desired
   * to see it.
   *
   * @param isHidden
   */
  public void setHidden(boolean isHidden) {
    this.isHidden = (short) (isHidden ? 1 : 0);
  }

  /**
   * Gets whether this repository has been installed and has reported back its
   * actual version.
   *
   * @return {@code true} if the version for this repository can be trusted,
   *         {@code false} otherwise.
   */
  public boolean isResolved() {
    return resolved == 1;
  }

  /**
   * Gets whether this repository is legacy
   *
   * @return
   */
  @Deprecated
  @Experimental(feature= ExperimentalFeature.PATCH_UPGRADES)
  public boolean isLegacy(){
    return isLegacy == 1;
  }

  /**
   * Sets whether this repository is legacy. Scoped for moving from old-style repository naming to new
   *
   * @param isLegacy
   */
  @Deprecated
  @Experimental(feature= ExperimentalFeature.PATCH_UPGRADES)
  public void setLegacy(boolean isLegacy){
    this.isLegacy = isLegacy ? (short) 1 : (short) 0;
  }

  /**
   * Sets whether this repository has been installed and has reported back its
   * actual version.
   *
   * @param resolved
   */
  public void setResolved(boolean resolved) {
    this.resolved = resolved ? (short) 1 : (short) 0;
  }

  public List<RepoOsEntity> getRepoOsEntities() {
    return repoOsEntities;
  }

  public void addRepoOsEntities(List<RepoOsEntity> repoOsEntities) {
    this.repoOsEntities = repoOsEntities;
    for (RepoOsEntity repoOsEntity : repoOsEntities) {
      repoOsEntity.setRepositoryVersionEntity(this);
    }
  }

  /**
   * Builds a {@link RepositoryVersion} instance type from this entity.
   *
   * @return a single POJO to represent this entity.
   */
  public RepositoryVersion getRepositoryVersion() {
    return new RepositoryVersion(getId(), getStackName(), getStackVersion(),
        getStackId().getStackId(), getVersion(), getType());
  }
}
