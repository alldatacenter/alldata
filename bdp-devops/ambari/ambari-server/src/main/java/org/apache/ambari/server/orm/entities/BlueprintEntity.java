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

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.ambari.server.state.SecurityType;


/**
 * Entity representing a Blueprint.
 */
@Table(name = "blueprint")
@NamedQuery(name = "allBlueprints",
    query = "SELECT blueprint FROM BlueprintEntity blueprint")
@Entity
public class BlueprintEntity {

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = true,
      updatable = false, unique = true, length = 100)
  private String blueprintName;

  @Basic
  @Enumerated(value = EnumType.STRING)
  @Column(name = "security_type", nullable = false, insertable = true, updatable = true)
  private SecurityType securityType = SecurityType.NONE;

  @Basic
  @Column(name = "security_descriptor_reference", nullable = true, insertable = true, updatable = true)
  private String securityDescriptorReference;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "stack_id", unique = false, nullable = false, insertable = true, updatable = false)
  private StackEntity stack;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "blueprint")
  private Collection<HostGroupEntity> hostGroups;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "blueprint")
  private Collection<BlueprintConfigEntity> configurations;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "blueprint")
  private Collection<BlueprintSettingEntity> settings;


  /**
   * Get the blueprint name.
   *
   * @return blueprint name
   */
  public String getBlueprintName() {
    return blueprintName;
  }

  /**
   * Set the blueprint name
   *
   * @param blueprintName  the blueprint name
   */
  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  /**
   * Gets the blueprint's stack.
   *
   * @return the stack.
   */
  public StackEntity getStack() {
    return stack;
  }

  /**
   * Sets the blueprint's stack.
   *
   * @param stack
   *          the stack to set for the blueprint (not {@code null}).
   */
  public void setStack(StackEntity stack) {
    this.stack = stack;
  }

  /**
   * Get the collection of associated host groups.
   *
   * @return collection of host groups
   */
  public Collection<HostGroupEntity> getHostGroups() {
    return hostGroups;
  }

  /**
   * Set the host group collection.
   *
   * @param hostGroups  collection of associated host groups
   */
  public void setHostGroups(Collection<HostGroupEntity> hostGroups) {
    this.hostGroups = hostGroups;
  }

  /**
   * Get the collection of associated configurations.
   *
   * @return collection of configurations
   */
  public Collection<BlueprintConfigEntity> getConfigurations() {
    return configurations;
  }

  /**
   * Set the configuration collection.
   *
   * @param configurations  collection of associated configurations
   */
  public void setConfigurations(Collection<BlueprintConfigEntity> configurations) {
    this.configurations = configurations;
  }

  /**
   * Get the collection of associated setting.
   *
   * @return collection of setting
   */
  public Collection<BlueprintSettingEntity> getSettings() {
    return settings;
  }

  /**
   * Set the settings collection.
   *
   * @param settings collection of associated setting
   */
  public void setSettings(Collection<BlueprintSettingEntity> settings) {
    this.settings = settings;
  }

  public SecurityType getSecurityType() {
    return securityType;
  }

  public void setSecurityType(SecurityType securityType) {
    this.securityType = securityType;
  }

  public String getSecurityDescriptorReference() {
    return securityDescriptorReference;
  }

  public void setSecurityDescriptorReference(String securityDescriptorReference) {
    this.securityDescriptorReference = securityDescriptorReference;
  }
}
